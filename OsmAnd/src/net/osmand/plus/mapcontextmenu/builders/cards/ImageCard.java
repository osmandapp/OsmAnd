package net.osmand.plus.mapcontextmenu.builders.cards;

import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public abstract class ImageCard extends AbstractCard {
	private static final Log LOG = PlatformUtil.getLog(ImageCard.class);
	private static final int THUMBNAIL_WIDTH = 160;
	private static final int GALLERY_FULL_SIZE_WIDTH = 1280;

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

	private final int defaultCardLayoutId = R.layout.context_menu_card_image;

	protected Drawable icon;

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

	private boolean imageDownloadFailed = false;
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
					String timeStampObject = imageObject.getString("timestamp");
					try {
						this.timestamp = DATE_FORMAT.parse(timeStampObject);
					} catch (ParseException parseDateFormat) {
						try {
							this.timestamp = new Date(Long.parseLong(timeStampObject));
						} catch (NumberFormatException e) {
							LOG.error("Failed to parse date " + timeStampObject);
						}
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
					String topIcon = imageObject.getString("topIcon");
					this.topIconId = AndroidUtils.getDrawableId(app, topIcon);
				}
				if (imageObject.has("buttonIcon") && !imageObject.isNull("buttonIcon")) {
					this.buttonIconId = AndroidUtils.getDrawableId(app, imageObject.getString("buttonIcon"));
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

	@Nullable
	public String getThumbnailUrl() {
		if (Algorithms.isEmpty(getImageHiresUrl())) {
			return null;
		}
		return getImageHiresUrl() + "?width=" + THUMBNAIL_WIDTH;
	}

	@Nullable
	public String getGalleryFullSizeUrl() {
		if (Algorithms.isEmpty(getImageHiresUrl())) {
			return null;
		}
		return getImageHiresUrl() + "?width=" + GALLERY_FULL_SIZE_WIDTH;
	}

	public int getTopIconId() {
		return topIconId;
	}

	public String getButtonText() {
		return buttonText;
	}

	@Override
	public int getCardLayoutId() {
		return defaultCardLayoutId;
	}

	public Drawable getIcon() {
		return icon;
	}

	public void markImageDownloadFailed(boolean imageDownloadFailed) {
		this.imageDownloadFailed = imageDownloadFailed;
	}

	public boolean isImageDownloadFailed() {
		return imageDownloadFailed;
	}

	public float getDistance() {
		return distance;
	}

	public void setDistance(float distance) {
		this.distance = distance;
	}
}
