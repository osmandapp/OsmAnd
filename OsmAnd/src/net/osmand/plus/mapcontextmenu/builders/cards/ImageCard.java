package net.osmand.plus.mapcontextmenu.builders.cards;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatButton;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.osmand.AndroidNetworkUtils;
import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapillary.MapillaryContributeCard;
import net.osmand.plus.mapillary.MapillaryImageCard;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class ImageCard extends AbstractCard {

	public static String TYPE_MAPILLARY_PHOTO = "mapillary-photo";
	public static String TYPE_MAPILLARY_CONTRIBUTE = "mapillary-contribute";

	protected String type;
	// Image location
	protected LatLon location;
	// (optional) Image's camera angle in range  [0, 360]
	protected double ca = Double.NaN;
	// Date When bitmap was captured
	protected Date timestamp;
	// Image key
	protected String key;
	// Image title
	protected String title;
	// User name
	protected String userName;
	// Image viewer url
	protected String url;
	// Image bitmap url
	protected String imageUrl;
	// Image high resolution bitmap url
	protected String imageHiresUrl;
	// true if external browser should to be opened, open webview otherwise
	protected boolean externalLink;

	protected int topIconId;
	protected int buttonIconId;
	protected String buttonText;
	protected int buttonIconColor;
	protected int buttonColor;
	protected int buttonTextColor;

	private int defaultCardLayoutId = R.layout.context_menu_card_image;

	protected Drawable icon;
	protected Drawable buttonIcon;
	protected OnClickListener onClickListener;
	protected OnClickListener onButtonClickListener;

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

	private boolean downloading;
	private boolean downloaded;
	private Bitmap bitmap;
	private float bearingDiff = Float.NaN;
	private float distance = Float.NaN;

	public ImageCard(MapActivity mapActivity, JSONObject imageObject) {
		super(mapActivity);
		if (imageObject != null) {
			try {
				if (imageObject.has("type")) {
					this.type = imageObject.getString("type");
				}
				if (imageObject.has("ca") && !imageObject.isNull("ca")) {
					this.ca = imageObject.getDouble("ca");
				}
				if (imageObject.has("lat") && imageObject.has("lon")
						&& !imageObject.isNull("lat") && !imageObject.isNull("lon")) {
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
				if (imageObject.has("title") && !imageObject.isNull("title")) {
					this.title = imageObject.getString("title");
				}
				if (imageObject.has("username") && !imageObject.isNull("username")) {
					this.userName = imageObject.getString("username");
				}
				if (imageObject.has("url") && !imageObject.isNull("url")) {
					this.url = imageObject.getString("url");
				}
				if (imageObject.has("imageUrl") && !imageObject.isNull("imageUrl")) {
					this.imageUrl = imageObject.getString("imageUrl");
				}
				if (imageObject.has("imageHiresUrl") && !imageObject.isNull("imageHiresUrl")) {
					this.imageHiresUrl = imageObject.getString("imageHiresUrl");
				}
				if (imageObject.has("externalLink") && !imageObject.isNull("externalLink")) {
					this.externalLink = imageObject.getBoolean("externalLink");
				}
				if (imageObject.has("topIcon") && !imageObject.isNull("topIcon")) {
					this.topIconId = AndroidUtils.getDrawableId(getMyApplication(), imageObject.getString("topIcon"));
				}
				if (imageObject.has("buttonIcon") && !imageObject.isNull("buttonIcon")) {
					this.buttonIconId = AndroidUtils.getDrawableId(getMyApplication(), imageObject.getString("buttonIcon"));
				}
				if (imageObject.has("buttonText") && !imageObject.isNull("buttonText")) {
					this.buttonText = imageObject.getString("buttonText");
				}
				if (imageObject.has("buttonIconColor") && !imageObject.isNull("buttonIconColor")) {
					try {
						this.buttonIconColor = Algorithms.parseColor(imageObject.getString("buttonIconColor"));
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					}
				}
				if (imageObject.has("buttonColor") && !imageObject.isNull("buttonColor")) {
					try {
						this.buttonColor = Algorithms.parseColor(imageObject.getString("buttonColor"));
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					}
				}
				if (imageObject.has("buttonTextColor") && !imageObject.isNull("buttonTextColor")) {
					try {
						this.buttonTextColor = Algorithms.parseColor(imageObject.getString("buttonTextColor"));
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static ImageCard createCard(MapActivity mapActivity, JSONObject imageObject) {
		ImageCard imageCard = null;
		try {
			if (imageObject.has("type")) {
				String type = imageObject.getString("type");
				if (TYPE_MAPILLARY_PHOTO.equals(type)) {
					imageCard = new MapillaryImageCard(mapActivity, imageObject);
				} else if (TYPE_MAPILLARY_CONTRIBUTE.equals(type)) {
					imageCard = new MapillaryContributeCard(mapActivity, imageObject);
				} else {
					imageCard = new UrlImageCard(mapActivity, imageObject);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return imageCard;
	}

	public double getCa() {
		return ca;
	}

	public String getKey() {
		return key;
	}

	public String getTitle() {
		return title;
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

	public String getImageHiresUrl() {
		return imageHiresUrl;
	}

	public boolean isExternalLink() {
		return externalLink;
	}

	public int getTopIconId() {
		return topIconId;
	}

	public int getButtonIconId() {
		return buttonIconId;
	}

	public String getButtonText() {
		return buttonText;
	}

	public int getButtonIconColor() {
		return buttonIconColor;
	}

	public int getButtonColor() {
		return buttonColor;
	}

	public int getButtonTextColor() {
		return buttonTextColor;
	}

	public int getDefaultCardLayoutId() {
		return defaultCardLayoutId;
	}

	@Override
	public int getCardLayoutId() {
		return defaultCardLayoutId;
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
			TextView urlTextView = (TextView) view.findViewById(R.id.url);
			TextView watermarkTextView = (TextView) view.findViewById(R.id.watermark);
			ProgressBar progress = (ProgressBar) view.findViewById(R.id.progress);
			AppCompatButton button = (AppCompatButton) view.findViewById(R.id.button);

			boolean night = getMyApplication().getDaynightHelper().isNightModeForMapControls();
			AndroidUtils.setBackground(getMapActivity(), view.findViewById(R.id.card_background), night,
					R.drawable.context_menu_card_light, R.drawable.context_menu_card_dark);

			if (icon == null && topIconId != 0) {
				icon = getMyApplication().getUIUtilities().getIcon(topIconId);
			}
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
				MenuBuilder.execute(new DownloadImageTask());
			} else {
				progress.setVisibility(View.GONE);
				image.setImageBitmap(bitmap);
				if (bitmap == null) {
					urlTextView.setVisibility(View.VISIBLE);
					urlTextView.setText(getUrl());
				} else {
					urlTextView.setVisibility(View.GONE);
				}
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

			if (!Algorithms.isEmpty(buttonText)) {
				button.setText(buttonText);
			}
			if (buttonIcon == null && buttonIconId != 0) {
				if (buttonIconColor != 0) {
					buttonIcon = getMyApplication().getUIUtilities().getPaintedIcon(buttonIconId, buttonIconColor);
				} else {
					buttonIcon = getMyApplication().getUIUtilities().getIcon(buttonIconId);
				}
			}
			button.setCompoundDrawablesWithIntrinsicBounds(buttonIcon, null, null, null);
			if (buttonColor != 0) {
				button.setSupportBackgroundTintList(ColorStateList.valueOf(buttonColor));
			}
			if (buttonTextColor != 0) {
				button.setTextColor(buttonTextColor);
			}
			if (onButtonClickListener != null) {
				button.setVisibility(View.VISIBLE);
				button.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						onButtonClickListener.onClick(v);
					}
				});
			} else {
				button.setVisibility(View.GONE);
				button.setOnClickListener(null);
			}
		}
	}

	public static class GetImageCardsTask extends AsyncTask<Void, Void, List<ImageCard>> {

		private MapActivity mapActivity;
		private OsmandApplication app;
		private LatLon latLon;
		private Map<String, String> params;
		private GetImageCardsListener listener;
		private List<ImageCard> result;

		public interface GetImageCardsListener {
			void onPostProcess(List<ImageCard> cardList);

			void onFinish(List<ImageCard> cardList);
		}

		public GetImageCardsTask(@NonNull MapActivity mapActivity, LatLon latLon,
								 @Nullable Map<String, String> params, GetImageCardsListener listener) {
			this.mapActivity = mapActivity;
			this.app = mapActivity.getMyApplication();
			this.latLon = latLon;
			this.params = params;
			this.listener = listener;
		}

		@Override
		protected List<ImageCard> doInBackground(Void... params) {
			List<ImageCard> result = new ArrayList<>();
			try {
				final Map<String, String> pms = new LinkedHashMap<>();
				pms.put("lat", "" + (float) latLon.getLatitude());
				pms.put("lon", "" + (float) latLon.getLongitude());
				Location myLocation = app.getLocationProvider().getLastKnownLocation();
				if (myLocation != null) {
					pms.put("mloc", "" + (float) myLocation.getLatitude() + "," + (float) myLocation.getLongitude());
				}
				pms.put("app", Version.isPaidVersion(app) ? "paid" : "free");
				String preferredLang = app.getSettings().MAP_PREFERRED_LOCALE.get();
				if (Algorithms.isEmpty(preferredLang)) {
					preferredLang = app.getLanguage();
				}
				if (!Algorithms.isEmpty(preferredLang)) {
					pms.put("lang", preferredLang);
				}
				if (this.params != null) {
					pms.putAll(this.params);
				}
				String response = AndroidNetworkUtils.sendRequest(app, "https://osmand.net/api/cm_place", pms,
						"Requesting location images...", false, false);

				if (!Algorithms.isEmpty(response)) {
					JSONObject obj = new JSONObject(response);
					JSONArray images = obj.getJSONArray("features");
					if (images.length() > 0) {
						for (int i = 0; i < images.length(); i++) {
							try {
								JSONObject imageObject = (JSONObject) images.get(i);
								if (imageObject != JSONObject.NULL) {
									ImageCard imageCard = ImageCard.createCard(mapActivity, imageObject);
									if (imageCard != null) {
										result.add(imageCard);
									}
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
			if (listener != null) {
				listener.onPostProcess(result);
			}
			return result;
		}

		@Override
		protected void onPostExecute(List<ImageCard> cardList) {
			result = cardList;
			if (listener != null) {
				listener.onFinish(result);
			}
		}
	}

	private class DownloadImageTask extends AsyncTask<Void, Void, Bitmap> {

		@Override
		protected void onPreExecute() {
			downloading = true;
			update();
		}

		@Override
		protected Bitmap doInBackground(Void... params) {
			return AndroidNetworkUtils.downloadImage(getMyApplication(), imageUrl);
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			downloading = false;
			downloaded = true;
			ImageCard.this.bitmap = bitmap;
			if (bitmap != null && Algorithms.isEmpty(getImageHiresUrl())) {
				ImageCard.this.imageHiresUrl = getUrl();
			}
			update();
		}
	}
}
