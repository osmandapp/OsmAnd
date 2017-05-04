package net.osmand.plus.mapcontextmenu.builders.cards;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.osmand.AndroidNetworkUtils;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.text.DateFormat.FULL;

public abstract class ImageCard extends AbstractCard {

	private static final int IMAGES_LIMIT = 15;

	private String type;
	// Image location
	private LatLon location;
	// (optional) Image's camera angle in range  [0, 360].
	private double ca = Double.NaN;
	// Date When bitmap was captured.
	private Date timestamp;
	// Image key.
	private String key;
	// User name
	private String userName;
	// Image viewer url
	private String url;
	// Image bitmap url
	private String imageUrl;

	private OsmandApplication app;
	private int defaultImageLayoutId = R.layout.context_menu_card_image;

	protected Drawable icon;
	protected OnClickListener onClickListener;

	private static final DateFormat DATE_FORMAT = SimpleDateFormat.getDateTimeInstance(FULL, FULL, Locale.US); //"yyyy-MM-dd'T'HH:mm:ss");
	private boolean downloading;
	private boolean downloaded;
	private Bitmap bitmap;
	private float bearingDiff = Float.NaN;
	private float distance = Float.NaN;

	public interface ImageCardFactory<T> {
		T createCard(OsmandApplication app, JSONObject imageObject);
	}

	public ImageCard(OsmandApplication app, JSONObject imageObject) {
		this.app = app;
		try {
			if (imageObject.has("type")) {
				this.type = imageObject.getString("type");
			}
			if (imageObject.has("ca") && !imageObject.isNull("ca")) {
				this.ca = imageObject.getDouble("ca");
			}
			if (imageObject.has("lat") && imageObject.has("lon")) {
				double latitude = imageObject.getDouble("lat");
				double longitude = imageObject.getDouble("lon");
				this.location = new LatLon(latitude, longitude);
			}
			if (imageObject.has("timestamp")) {
				try {
					this.timestamp = DATE_FORMAT.parse(imageObject.getString("timestamp"));
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
			if (imageObject.has("key")) {
				this.key = imageObject.getString("key");
			}
			if (imageObject.has("username")) {
				this.userName = imageObject.getString("username");
			}
			if (imageObject.has("url")) {
				this.url = imageObject.getString("url");
			}
			if (imageObject.has("imageUrl")) {
				this.imageUrl = imageObject.getString("imageUrl");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public double getCa() {
		return ca;
	}

	public String getKey() {
		return key;
	}

	public String getType() {
		return type;
	}

	public LatLon getLocation() {
		return location;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public String getUserName() {
		return userName;
	}

	public String getUrl() {
		return url;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public int getDefaultImageLayoutId() {
		return defaultImageLayoutId;
	}

	@Override
	public int getCardLayoutId() {
		return defaultImageLayoutId;
	}

	public Drawable getIcon() {
		return icon;
	}

	public OnClickListener getOnClickListener() {
		return onClickListener;
	}


	public boolean isDownloading() {
		return downloading;
	}

	public void setDownloading(boolean downloading) {
		this.downloading = downloading;
	}

	public Bitmap getBitmap() {
		return bitmap;
	}

	public void setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;
	}

	public float getBearingDiff() {
		return bearingDiff;
	}

	public void setBearingDiff(float bearingDiff) {
		this.bearingDiff = bearingDiff;
	}

	public float getDistance() {
		return distance;
	}

	public void setDistance(float distance) {
		this.distance = distance;
	}

	public void update() {
		if (view != null) {
			ImageView image = (ImageView) view.findViewById(R.id.image);
			ImageView iconImageView = (ImageView) view.findViewById(R.id.icon);
			TextView watermarkTextView = (TextView) view.findViewById(R.id.watermark);
			ProgressBar progress = (ProgressBar) view.findViewById(R.id.progress);
			if (icon == null) {
				iconImageView.setVisibility(View.GONE);
			} else {
				iconImageView.setImageDrawable(icon);
				iconImageView.setVisibility(View.VISIBLE);
			}
			if (Algorithms.isEmpty(userName)) {
				watermarkTextView.setVisibility(View.GONE);
			} else {
				watermarkTextView.setText("@" + userName);
				watermarkTextView.setVisibility(View.VISIBLE);
			}
			if (downloading) {
				progress.setVisibility(View.VISIBLE);
				image.setImageBitmap(null);
			} else if (!downloaded) {
				execute(new DownloadImageTask());
			} else {
				progress.setVisibility(View.GONE);
				image.setImageBitmap(bitmap);
			}
			if (onClickListener != null) {
				view.findViewById(R.id.image_card).setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						onClickListener.onClick(v);
					}
				});
			} else {
				view.findViewById(R.id.image_card).setOnClickListener(null);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static <P> void execute(AsyncTask<P, ?, ?> task, P... requests) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, requests);
		} else {
			task.execute(requests);
		}
	}

	public static class GetImageCardsTask<I extends ImageCard> extends AsyncTask<Void, Void, List<I>> {

		private OsmandApplication app;
		private LatLon latLon;
		private Listener<I> listener;
		private ImageCardFactory<I> imageCardFactory;
		private List<I> result;

		public interface Listener<T extends ImageCard> {
			void onFinish(List<T> cardList);
		}

		public GetImageCardsTask(@NonNull ImageCardFactory<I> imageCardFactory,
								 @NonNull OsmandApplication app, LatLon latLon, Listener<I> listener) {
			this.imageCardFactory = imageCardFactory;
			this.app = app;
			this.latLon = latLon;
			this.listener = listener;
		}

		@Override
		protected List<I> doInBackground(Void... params) {
			List<I> result = new ArrayList<>();
			try {
				final Map<String, String> pms = new LinkedHashMap<>();
				pms.put("lat", "" + latLon.getLatitude());
				pms.put("lon", "" + latLon.getLongitude());
				Location myLocation = app.getLocationProvider().getLastKnownLocation();
				if (myLocation != null) {
					pms.put("myLocation", "" + myLocation.getLatitude() + "," + myLocation.getLongitude());
				}
				pms.put("app", Version.isPaidVersion(app) ? "paid" : "free");
				String response = AndroidNetworkUtils.sendRequest(app, "http://osmand.net/api/cm_place.php", pms,
						"Requesting location images...", false, false);

				if (!Algorithms.isEmpty(response)) {
					JSONObject obj = new JSONObject(response);
					JSONArray images = obj.getJSONArray("features");
					if (images.length() > 0) {
						for (int i = 0; i < images.length(); i++) {
							try {
								JSONObject imageObject = (JSONObject) images.get(i);
								if (imageObject != JSONObject.NULL) {
									result.add(imageCardFactory.createCard(app, imageObject));
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
			final Location loc = new Location("");
			loc.setLatitude(latLon.getLatitude());
			loc.setLongitude(latLon.getLongitude());
			sortImagesInfo(result, loc);
			if (result.size() > IMAGES_LIMIT) {
				result = result.subList(0, IMAGES_LIMIT);
			}
			return result;
		}

		@Override
		protected void onPostExecute(List<I> cardList) {
			result = cardList;
			if (listener != null) {
				listener.onFinish(result);
			}
		}
	}

	private static <T extends ImageCard> void sortImagesInfo(List<T> cardList, @NonNull final Location location) {
		List<T> targetCards = new ArrayList<>();
		List<T> otherCards = new ArrayList<>();
		for (T c : cardList) {
			if (c.getLocation() != null) {
				Location l = new Location("");
				l.setLatitude(c.getLocation().getLatitude());
				l.setLongitude(c.getLocation().getLongitude());
				c.setDistance(l.distanceTo(location));
				if (!Double.isNaN(c.getCa())) {
					float bearingDiff = Math.abs(MapUtils.unifyRotationDiff(
							MapUtils.unifyRotationTo360((float) c.getCa()), l.bearingTo(location)));
					c.setBearingDiff(bearingDiff);
					if (bearingDiff < 30) {
						targetCards.add(c);
					} else {
						otherCards.add(c);
					}
				} else {
					otherCards.add(c);
				}
			}
		}
		Collections.sort(targetCards, new Comparator<ImageCard>() {

			@Override
			public int compare(ImageCard i1, ImageCard i2) {
				return Float.compare(i1.bearingDiff, i2.bearingDiff);
			}
		});
		Collections.sort(otherCards, new Comparator<ImageCard>() {

			@Override
			public int compare(ImageCard i1, ImageCard i2) {
				return Float.compare(i1.distance, i2.distance);
			}
		});
		cardList.clear();
		cardList.addAll(targetCards);
		cardList.addAll(otherCards);
	}

	private class DownloadImageTask extends AsyncTask<Void, Void, Bitmap> {

		@Override
		protected void onPreExecute() {
			downloading = true;
			update();
		}

		@Override
		protected Bitmap doInBackground(Void... params) {
			return AndroidNetworkUtils.downloadImage(app, imageUrl);
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			downloading = false;
			downloaded = true;
			ImageCard.this.bitmap = bitmap;
			update();
		}
	}
}
