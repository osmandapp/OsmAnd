package net.osmand.plus.mapillary;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.osmand.AndroidNetworkUtils;
import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MapillaryImageRow {

	private static final int IMAGES_LIMIT = 15;

	private MenuBuilder menuBuilder;
	private View view;
	private OsmandApplication app;
	private List<MapillaryImageInfo> imageInfoList = new ArrayList<>();
	private boolean updatingInfo;
	private boolean downloadingImages;
	private boolean updatingInfoInterrupted;
	private boolean downloadingImagesInterrupted;
	private ViewsPagerAdapter pagerAdapter;
	private int dp10;
	private boolean updated;

	public MapillaryImageRow(MenuBuilder menuBuilder, View view) {
		this.menuBuilder = menuBuilder;
		this.view = view;
		this.app = menuBuilder.getApp();
		this.dp10 = AndroidUtils.dpToPx(app, 10f);
	}

	public MapillaryImageRow(MapillaryImageRow imageRow, View view) {
		this(imageRow.menuBuilder, view);
		this.updatingInfo = imageRow.updatingInfo;
		this.downloadingImages = imageRow.downloadingImages;
		this.updatingInfoInterrupted = imageRow.updatingInfoInterrupted;
		this.downloadingImagesInterrupted = imageRow.downloadingImagesInterrupted;
		this.imageInfoList.addAll(imageRow.imageInfoList);
		this.updated = true;
	}

	public MenuBuilder getMenuBuilder() {
		return menuBuilder;
	}

	public View getView() {
		return view;
	}

	public void build() {
		ViewPager viewPager = new ViewPager(view.getContext());
		ViewPager.LayoutParams params = new ViewPager.LayoutParams();
		params.width = ViewGroup.LayoutParams.MATCH_PARENT;
		params.height = (int) app.getResources().getDimension(R.dimen.mapillary_card_height) + dp10 + dp10;
		viewPager.setLayoutParams(params);
		viewPager.setPageMargin(dp10);
		viewPager.setPadding(dp10, dp10, dp10, dp10);
		viewPager.setClipToPadding(false);
		pagerAdapter = new ViewsPagerAdapter();
		viewPager.setAdapter(pagerAdapter);
		((LinearLayout) view).addView(viewPager);

		if (!updated) {
			new GetImageInfoTask().execute();
		} else {
			update();
		}
	}

	public void update() {
		if (updatingInfoInterrupted || updatingInfo) {
			new GetImageInfoTask().execute();
		} else if ((downloadingImagesInterrupted || downloadingImages) && imageInfoList.size() > 0) {
			new DownloadImagesTask().execute();
		}
	}

	private int itemsCount() {
		return updatingInfo ? 1 : imageInfoList.size() + 1;
	}

	private View createPageView(ViewGroup container, int position) {
		View v;
		if (updatingInfo && position == 0) {
			v = LayoutInflater.from(view.getContext()).inflate(R.layout.mapillary_context_menu_progress, null);
		} else if (position == itemsCount() - 1) {
			v = LayoutInflater.from(view.getContext()).inflate(R.layout.mapillary_context_menu_action, null);
			v.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// todo open mapillary
				}
			});
		} else {
			v = LayoutInflater.from(view.getContext()).inflate(R.layout.mapillary_context_menu_image, null);
			ImageView image = (ImageView) v.findViewById(R.id.image);
			TextView watermark = (TextView) v.findViewById(R.id.watermark);
			ProgressBar progress = (ProgressBar) v.findViewById(R.id.progress);
			if (position < imageInfoList.size()) {
				MapillaryImageInfo info = imageInfoList.get(position);
				if (info != null) {
					if (Algorithms.isEmpty(info.getUserName())) {
						watermark.setText("mapillary.com");
					} else {
						watermark.setText("@" + info.getUserName() + " | mapillary.com");
					}
					if (info.isDownloading()) {
						progress.setVisibility(View.VISIBLE);
						image.setImageBitmap(null);
					} else {
						progress.setVisibility(View.GONE);
						image.setImageBitmap(info.getBitmap());
					}
					v.findViewById(R.id.image_card).setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							// todo open image
						}
					});
				}
			}
		}
		return v;
	}

	private class ViewsPagerAdapter extends PagerAdapter {

		@Override
		public float getPageWidth(int position) {
			return 0.8f;
		}

		@Override
		public int getItemPosition(Object object) {
			return POSITION_NONE;
		}

		@Override
		public int getCount() {
			return itemsCount();
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {

			View view = createPageView(container, position);
			container.addView(view, 0);

			return view;
		}

		@Override
		public void destroyItem(ViewGroup collection, int position, Object view) {
			collection.removeView((View) view);
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}
	}

	private class GetImageInfoTask extends AsyncTask<Void, MapillaryImageInfo, List<MapillaryImageInfo>> {

		@Override
		protected void onPreExecute() {
			updatingInfoInterrupted = false;
			if (!menuBuilder.isHidden()) {
				updatingInfo = true;
				pagerAdapter.notifyDataSetChanged();
			}
		}

		@Override
		protected List<MapillaryImageInfo> doInBackground(Void... params) {
			// https://a.mapillary.com/v3/images/?lookat=12.9981086701,55.6075236275&closeto=13.0006076843,55.6089295863&radius=20&client_id=LXJVNHlDOGdMSVgxZG5mVzlHQ3ZqQTo0NjE5OWRiN2EzNTFkNDg4
			List<MapillaryImageInfo> result = new ArrayList<>();
			try {
				final Map<String, String> pms = new LinkedHashMap<>();
				//pms.put("lookat", Version.getFullVersion(app));
				pms.put("closeto", "" + menuBuilder.getLatLon().getLongitude() + "," + menuBuilder.getLatLon().getLatitude());
				pms.put("radius", "20");
				pms.put("client_id", "LXJVNHlDOGdMSVgxZG5mVzlHQ3ZqQTo0NjE5OWRiN2EzNTFkNDg4");
				String response = AndroidNetworkUtils.sendRequest(app, "https://a.mapillary.com/v3/images", pms,
						"Requesting mapillary images...", false, false);

				if (!Algorithms.isEmpty(response)) {
					JSONObject obj = new JSONObject(response);
					JSONArray images = obj.getJSONArray("features");
					if (images.length() > 0) {
						for (int i = 0; i < images.length(); i++) {
							if (menuBuilder.isHidden()) {
								updatingInfoInterrupted = true;
								break;
							}
							if (i > IMAGES_LIMIT) {
								break;
							}
							try {
								JSONObject imgObj = (JSONObject) images.get(i);
								if (imgObj != JSONObject.NULL) {
									result.add(new MapillaryImageInfo(imgObj));
								}
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return result;
		}

		@Override
		protected void onPostExecute(List<MapillaryImageInfo> infoList) {
			updatingInfo = false;
			if (!menuBuilder.isHidden()) {
				imageInfoList.addAll(infoList);
				pagerAdapter.notifyDataSetChanged();
				if (imageInfoList.size() > 0) {
					new DownloadImagesTask().execute();
				}
			}
		}
	}

	private class DownloadImagesTask extends AsyncTask<Void, Void, Void> {

		private static final String urlTemplate = "https://d1cuyjsrcm0gby.cloudfront.net/{key}/thumb-640.jpg?origin=osmand";

		@Override
		protected void onPreExecute() {
			downloadingImagesInterrupted = false;
			if (!menuBuilder.isHidden()) {
				downloadingImages = true;
				for (MapillaryImageInfo imageInfo : imageInfoList) {
					if (imageInfo.getBitmap() == null) {
						imageInfo.setDownloading(true);
					}
				}
				pagerAdapter.notifyDataSetChanged();
			}
		}

		@Override
		protected Void doInBackground(Void... params) {
			for (MapillaryImageInfo imageInfo : imageInfoList) {
				if (menuBuilder.isHidden()) {
					downloadingImagesInterrupted = true;
					break;
				}
				if (imageInfo.isDownloading()) {
					String url = urlTemplate.replace("{key}", imageInfo.getKey());
					Bitmap bitmap = AndroidNetworkUtils.downloadImage(app, url);
					imageInfo.setBitmap(bitmap);
					imageInfo.setDownloading(false);
					publishProgress();
				}
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			if (!menuBuilder.isHidden()) {
				pagerAdapter.notifyDataSetChanged();
			}
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			downloadingImages = false;
			if (!menuBuilder.isHidden()) {
				pagerAdapter.notifyDataSetChanged();
			}
		}
	}
}