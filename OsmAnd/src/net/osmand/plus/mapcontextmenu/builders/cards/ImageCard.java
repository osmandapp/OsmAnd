package net.osmand.plus.mapcontextmenu.builders.cards;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.gallery.tasks.DownloadImageTask;
import net.osmand.plus.mapcontextmenu.gallery.tasks.DownloadImageTask.DownloadImageListener;
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
	private static final int THUMBNAIL_WIDTH = 12;
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
			ImageView image = view.findViewById(R.id.image);
			ImageView iconImageView = view.findViewById(R.id.icon);
			TextView urlTextView = view.findViewById(R.id.url);
			TextView watermarkTextView = view.findViewById(R.id.watermark);
			ProgressBar progress = view.findViewById(R.id.progress);
			AppCompatButton button = view.findViewById(R.id.button);

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
				MenuBuilder.execute(new DownloadImageTask(getMyApplication(), imageUrl, getDownloadImageListener()));
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
				view.findViewById(R.id.image_card).setOnClickListener(v -> onClickListener.onClick(v));
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
				button.setOnClickListener(v -> onButtonClickListener.onClick(v));
			} else {
				button.setVisibility(View.GONE);
				button.setOnClickListener(null);
			}
		}
	}

	private DownloadImageListener getDownloadImageListener() {
		return new DownloadImageListener() {
			@Override
			public void onStartDownloading() {
				downloading = true;
				update();
			}

			@Override
			public void onFinishDownloading(Bitmap bitmap) {
				downloading = false;
				downloaded = true;
				ImageCard.this.bitmap = bitmap;
				if (bitmap != null && Algorithms.isEmpty(getImageHiresUrl())) {
					ImageCard.this.imageHiresUrl = getUrl();
				}
				update();
			}
		};
	}
}
