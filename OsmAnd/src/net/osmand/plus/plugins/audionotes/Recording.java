package net.osmand.plus.plugins.audionotes;

import static net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin.IMG_EXTENSION;
import static net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin.MPEG4_EXTENSION;
import static net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin.THREEGP_EXTENSION;

import android.content.Context;
import android.media.MediaPlayer;

import androidx.annotation.NonNull;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.text.DateFormat;

public class Recording {

	private static final Log log = PlatformUtil.getLog(Recording.class);

	private static final char SPLIT_DESC = ' ';

	private File file;

	private double lat;
	private double lon;
	private long duration = -1;
	private boolean available = true;

	public Recording(@NonNull File file) {
		this.file = file;
	}

	public void setFile(@NonNull File file) {
		this.file = file;
	}

	public double getLatitude() {
		return lat;
	}

	public void setLatitude(double lat) {
		this.lat = lat;
	}

	public double getLongitude() {
		return lon;
	}

	public void setLongitude(double lon) {
		this.lon = lon;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public boolean isAvailable() {
		return available;
	}

	public void setAvailable(boolean available) {
		this.available = available;
	}

	private void updateInternalDescription() {
		if (duration == -1) {
			duration = 0;
			if (!isPhoto()) {
				MediaPlayer mediaPlayer = new MediaPlayer();
				try {
					mediaPlayer.setDataSource(file.getAbsolutePath());
					mediaPlayer.prepare();
					duration = mediaPlayer.getDuration();
					available = true;
				} catch (Exception e) {
					log.error("Error reading recording " + file.getAbsolutePath(), e);
					available = false;
				}
			}
		}
	}

	public File getFile() {
		return file;
	}

	public long getLastModified() {
		return file.lastModified();
	}

	public boolean setName(String name) {
		File directory = file.getParentFile();
		String fileName = getFileName();
		File to = new File(directory, name + SPLIT_DESC + getOtherName(fileName));
		if (file.renameTo(to)) {
			file = to;
			return true;
		}
		return false;
	}

	public boolean setLocation(LatLon latLon) {
		File directory = file.getParentFile();
		lat = latLon.getLatitude();
		lon = latLon.getLongitude();
		if (directory != null) {
			File to = AudioVideoNotesPlugin.getBaseFileName(lat, lon, directory, Algorithms.getFileExtension(file));
			if (file.renameTo(to)) {
				file = to;
				return true;
			}
		}
		return false;
	}

	public String getFileName() {
		return file.getName();
	}

	public String getDescriptionName(String fileName) {
		int hashInd = fileName.lastIndexOf(SPLIT_DESC);
		//backward compatibility
		if (fileName.indexOf('.') - fileName.indexOf('_') > 12 &&
				hashInd < fileName.indexOf('_')) {
			hashInd = fileName.indexOf('_');
		}
		if (hashInd == -1) {
			return null;
		} else {
			return fileName.substring(0, hashInd);
		}
	}

	public String getOtherName(String fileName) {
		String descriptionName = getDescriptionName(fileName);
		if (descriptionName != null) {
			return fileName.substring(descriptionName.length() + 1); // SPLIT_DESC
		} else {
			return fileName;
		}
	}

	public static String formatDateTime(Context ctx, long dateTime) {
		DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(ctx);
		DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(ctx);
		return dateFormat.format(dateTime) + " " + timeFormat.format(dateTime);
	}

	public String getName(Context ctx, boolean includingType) {
		String fileName = file.getName();
		String desc = getDescriptionName(fileName);
		if (desc != null) {
			return desc;
		} else if (this.isAudio() || this.isVideo() || this.isPhoto()) {
			if (includingType) {
				return getType(ctx) + " " + formatDateTime(ctx, file.lastModified());
			} else {
				return formatDateTime(ctx, file.lastModified());
			}
		}
		return "";
	}

	public String getType(@NonNull Context ctx) {
		if (this.isAudio()) {
			return ctx.getResources().getString(R.string.shared_string_audio);
		} else if (this.isVideo()) {
			return ctx.getResources().getString(R.string.shared_string_video);
		} else if (this.isPhoto()) {
			return ctx.getResources().getString(R.string.shared_string_photo);
		} else {
			return "";
		}
	}

	public String getSearchHistoryType() {
		if (isPhoto()) {
			return PointDescription.POINT_TYPE_PHOTO_NOTE;
		} else if (isVideo()) {
			return PointDescription.POINT_TYPE_VIDEO_NOTE;
		} else {
			return PointDescription.POINT_TYPE_PHOTO_NOTE;
		}
	}

	public boolean isPhoto() {
		return file.getName().endsWith(IMG_EXTENSION);
	}

	public boolean isVideo() {
		return file.getName().endsWith(MPEG4_EXTENSION);
	}

	public boolean isAudio() {
		return file.getName().endsWith(THREEGP_EXTENSION);
	}

	private String convertDegToExifRational(double l) {
		if (l < 0) {
			l = -l;
		}
		String s = ((int) l) + "/1,"; // degrees
		l = (l - ((int) l)) * 60.0;
		s += (int) l + "/1,"; // minutes
		l = (l - ((int) l)) * 60000.0;
		s += (int) l + "/1000"; // seconds
		// log.info("deg rational: " + s);
		return s;
	}

	@SuppressWarnings("rawtypes")
	public void updatePhotoInformation(double lat, double lon, Location loc,
			double rot) throws IOException {
		try {
			Class exClass = Class.forName("android.media.ExifInterface");

			Constructor c = exClass.getConstructor(String.class);
			Object exInstance = c.newInstance(file.getAbsolutePath());
			Method setAttribute = exClass.getMethod("setAttribute", String.class, String.class);
			setAttribute.invoke(exInstance, "GPSLatitude", convertDegToExifRational(lat));
			setAttribute.invoke(exInstance, "GPSLatitudeRef", lat > 0 ? "N" : "S");
			setAttribute.invoke(exInstance, "GPSLongitude", convertDegToExifRational(lon));
			setAttribute.invoke(exInstance, "GPSLongitudeRef", lon > 0 ? "E" : "W");
			if (!Double.isNaN(rot)) {
				setAttribute.invoke(exInstance, "GPSImgDirectionRef", "T");
				while (rot < 0) {
					rot += 360;
				}
				while (rot > 360) {
					rot -= 360;
				}
				int abs = (int) (Math.abs(rot) * 100.0);
				String rotString = abs + "/100";
				setAttribute.invoke(exInstance, "GPSImgDirection", rotString);
			}
			if (loc != null && loc.hasAltitude()) {
				double alt = loc.getAltitude();
				String altString = (int) (Math.abs(alt) * 100.0) + "/100";
				setAttribute.invoke(exInstance, "GPSAltitude", altString);
				setAttribute.invoke(exInstance, "GPSAltitudeRef", alt < 0 ? "1" : "0");
			}
			Method saveAttributes = exClass.getMethod("saveAttributes");
			saveAttributes.invoke(exInstance);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@SuppressWarnings("rawtypes")
	private int getExifOrientation() {
		int orientation = 0;
		try {
			Class exClass = Class.forName("android.media.ExifInterface");
			Constructor c = exClass.getConstructor(String.class);
			Object exInstance = c.newInstance(file.getAbsolutePath());
			Method getAttributeInt = exClass.getMethod("getAttributeInt", String.class, Integer.TYPE);
			orientation = (Integer) getAttributeInt.invoke(exInstance, "Orientation", 1);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return orientation;
	}

	public int getBitmapRotation() {
		return switch (getExifOrientation()) {
			case 3 -> 180;
			case 6 -> 90;
			case 8 -> 270;
			default -> 0;
		};
	}

	public String getDescription(Context ctx) {
		String time = AndroidUtils.formatDateTime(ctx, file.lastModified());
		if (isPhoto()) {
			return ctx.getString(R.string.recording_photo_description, "", time).trim();
		}
		updateInternalDescription();
		return ctx.getString(R.string.recording_description, "", getDuration(ctx, true), time)
				.trim();
	}

	public String getSmallDescription(Context ctx) {
		String time = AndroidUtils.formatDateTime(ctx, file.lastModified());
		if (isPhoto()) {
			return time;
		}
		updateInternalDescription();
		return time + " " + getDuration(ctx, true);

	}

	public String getExtendedDescription(Context ctx) {
		DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(ctx);
		String date = dateFormat.format(file.lastModified());
		String size = AndroidUtils.formatSize(ctx, file.length());
		if (isPhoto()) {
			return date + " • " + size;
		}
		updateInternalDescription();
		return date + " • " + size + " • " + getDuration(ctx, false);
	}

	public String getTypeWithDuration(Context ctx) {
		StringBuilder res = new StringBuilder(getType(ctx));
		if (isAudio() || isVideo()) {
			updateInternalDescription();
			res.append(", ").append(getDuration(ctx, false));
		}
		return res.toString();
	}

	public String getPlainDuration(boolean accessibilityEnabled) {
		updateInternalDescription();
		if (duration > 0) {
			int d = (int) (duration / 1000);
			return Algorithms.formatDuration(d, accessibilityEnabled);
		} else {
			return "";
		}
	}

	private String getDuration(Context ctx, boolean addRoundBrackets) {
		StringBuilder additional = new StringBuilder();
		if (duration > 0) {
			int d = (int) (duration / 1000);
			additional.append(addRoundBrackets ? "(" : "");
			additional.append(Algorithms.formatDuration(d, ((OsmandApplication) ctx.getApplicationContext()).accessibilityEnabled()));
			additional.append(addRoundBrackets ? ")" : "");
		}
		if (!available) {
			additional.append("[").append(ctx.getString(R.string.recording_unavailable)).append("]");
		}
		return additional.toString();
	}

	public static String getNameForMultimediaFile(@NonNull OsmandApplication app,
			@NonNull String fileName, long lastModified) {
		if (fileName.endsWith(IMG_EXTENSION)) {
			return app.getString(R.string.shared_string_photo) + " " + formatDateTime(app, lastModified);
		} else if (fileName.endsWith(MPEG4_EXTENSION)) {
			return app.getString(R.string.shared_string_video) + " " + formatDateTime(app, lastModified);
		} else if (fileName.endsWith(THREEGP_EXTENSION)) {
			return app.getString(R.string.shared_string_audio) + " " + formatDateTime(app, lastModified);
		}
		return "";
	}
}
