package net.osmand.plus.mapillary;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
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
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.LockableViewPager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
	private LockableViewPager viewPager;
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
		imageInfoList.addAll(imageRow.imageInfoList);
		sortImagesInfo(imageInfoList,
				menuBuilder.getMapActivity().getMapView().getCurrentRotatedTileBox().getRotate(),
				menuBuilder.getLatLon());
		updated = true;
	}

	public MenuBuilder getMenuBuilder() {
		return menuBuilder;
	}

	public View getView() {
		return view;
	}

	public void build() {
		viewPager = new LockableViewPager(view.getContext());
		ViewPager.LayoutParams params = new ViewPager.LayoutParams();
		params.width = ViewGroup.LayoutParams.MATCH_PARENT;
		params.height = (int) app.getResources().getDimension(R.dimen.mapillary_card_height) + dp10 + dp10;
		viewPager.setLayoutParams(params);
		viewPager.setPageMargin(dp10);
		viewPager.setPadding(dp10, dp10, dp10, dp10);
		viewPager.setClipToPadding(false);
		pagerAdapter = new ViewsPagerAdapter();
		viewPager.setAdapter(pagerAdapter);
		viewPager.setSwipeLocked(imageInfoList.size() == 0);
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

		private RotatedTileBox tb;
		private LatLon menuLatLon;

		@Override
		protected void onPreExecute() {
			updatingInfoInterrupted = false;
			if (!menuBuilder.isHidden()) {
				updatingInfo = true;
				pagerAdapter.notifyDataSetChanged();
				tb = menuBuilder.getMapActivity().getMapView().getCurrentRotatedTileBox().copy();
				menuLatLon = menuBuilder.getLatLon();
			}
		}

		@Override
		protected List<MapillaryImageInfo> doInBackground(Void... params) {
			// https://a.mapillary.com/v3/images/?lookat=12.9981086701,55.6075236275&closeto=13.0006076843,55.6089295863&radius=20&client_id=LXJVNHlDOGdMSVgxZG5mVzlHQ3ZqQTo0NjE5OWRiN2EzNTFkNDg4
			List<MapillaryImageInfo> result = new ArrayList<>();
			try {
				LatLon l1 = tb.getLatLonFromPixel(0, 0);
				LatLon l2 = tb.getLatLonFromPixel(dp10 * 2, dp10 * 2);
				int radius = Math.max(20, (int) MapUtils.getDistance(l1, l2));
				radius = Math.min(500, radius);
				//float mx = tb.getPixXFromLatLon(menuLatLon.getLatitude(), menuLatLon.getLongitude());
				//float my = tb.getPixYFromLatLon(menuLatLon.getLatitude(), menuLatLon.getLongitude());
				//LatLon lookAt = tb.getLatLonFromPixel(mx, my - dp10);

				final Map<String, String> pms = new LinkedHashMap<>();
				//pms.put("lookat", "" + lookAt.getLongitude() + "," + lookAt.getLatitude());
				pms.put("closeto", "" + menuLatLon.getLongitude() + "," + menuLatLon.getLatitude());
				pms.put("radius", "" + radius);
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
			sortImagesInfo(result, tb.getRotate(), menuLatLon);
			return result;
		}

		@Override
		protected void onPostExecute(List<MapillaryImageInfo> infoList) {
			updatingInfo = false;
			if (!menuBuilder.isHidden()) {
				imageInfoList.addAll(infoList);
				pagerAdapter.notifyDataSetChanged();
				viewPager.setSwipeLocked(imageInfoList.size() == 0);
				if (imageInfoList.size() > 0) {
					new DownloadImagesTask().execute();
				}
			}
		}
	}

	private void sortImagesInfo(List<MapillaryImageInfo> infoList, final float azimuth, final LatLon menuLatLon) {
		Collections.sort(infoList, new Comparator<MapillaryImageInfo>() {

			@Override
			public int compare(MapillaryImageInfo i1, MapillaryImageInfo i2) {
				int res = 0;
				if (!Double.isNaN(i1.getCa()) && !Double.isNaN(i2.getCa()) && menuLatLon != null) {
					float a1 = Math.abs(MapUtils.unifyRotationDiff(azimuth, (float) i1.getCa()));
					float a2 = Math.abs(MapUtils.unifyRotationDiff(azimuth, (float) i2.getCa()));
					res = a1 < a2 ? -1 : (a1 == a2 ? 0 : 1);
				}
				if (res == 0 && i1.getCoordinates() != null && i2.getCoordinates() != null) {
					double d1 = MapUtils.getDistance(menuLatLon, i1.getCoordinates());
					double d2 = MapUtils.getDistance(menuLatLon, i2.getCoordinates());
					res = Double.compare(d1, d2);
				}
				return res;
			}
		});
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