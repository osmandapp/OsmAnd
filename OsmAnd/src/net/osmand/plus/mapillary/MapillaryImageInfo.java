package net.osmand.plus.mapillary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static java.text.DateFormat.FULL;

public class MapillaryImageInfo {
	// (optional) Image's camera angle in range  [0, 360].
	private double ca = Double.NaN;
	// Camera model name.
	private String cameraMake;
	// Date When bitmap was captured.
	private Date capturedAt;
	// Image key.
	private String key;
	// Whether the bitmap is panorama ( true ), or not ( false ).
	private boolean pano;
	// (optional) Which project the bitmap belongs to. Absent if it doesn't belong to any project.
	private String projectKey;
	// User who captured the bitmap.
	private String userKey;
	// Username of who captured the bitmap.
	private String userName;

	private static final DateFormat DATE_FORMAT = SimpleDateFormat.getDateTimeInstance(FULL, FULL, Locale.US); //"yyyy-MM-dd'T'HH:mm:ss");
	private boolean downloading;
	private Bitmap bitmap;

	MapillaryImageInfo(JSONObject imgObj) {
		try {
			JSONObject props = imgObj.getJSONObject("properties");
			if (props.has("ca")) {
				this.ca = props.getDouble("ca");
			}
			if (props.has("camera_make")) {
				this.cameraMake = props.getString("camera_make");
			}
			if (props.has("captured_at")) {
				try {
					this.capturedAt = DATE_FORMAT.parse(props.getString("captured_at"));
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
			if (props.has("key")) {
				this.key = props.getString("key");
			}
			if (props.has("pano")) {
				this.pano = props.getBoolean("pano");
			}
			if (props.has("project_key")) {
				this.projectKey = props.getString("project_key");
			}
			if (props.has("user_key")) {
				this.userKey = props.getString("user_key");
			}
			if (props.has("username")) {
				this.userName = props.getString("username");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public double getCa() {
		return ca;
	}

	public String getCameraMake() {
		return cameraMake;
	}

	public Date getCapturedAt() {
		return capturedAt;
	}

	public String getKey() {
		return key;
	}

	public boolean isPano() {
		return pano;
	}

	public String getProjectKey() {
		return projectKey;
	}

	public String getUserKey() {
		return userKey;
	}

	public String getUserName() {
		return userName;
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
}
