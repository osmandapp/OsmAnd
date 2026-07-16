package net.osmand.plus.media;

import static android.media.MediaMetadataRetriever.METADATA_KEY_LOCATION;
import static net.osmand.shared.media.MediaFileNameFormat.isManagedMediaFileName;

import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.shared.media.domain.MediaType;
import net.osmand.util.Algorithms;
import net.osmand.util.GeoParsedPoint;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public final class MediaMetadataUtils {

	private static final Log log = PlatformUtil.getLog(MediaMetadataUtils.class);

	private static final String MEDIA_LOCATION_PROVIDER_PREFIX = "media_";
	private static final String EXIF_LOCATION_PROVIDER = MEDIA_LOCATION_PROVIDER_PREFIX + "exif";
	private static final String METADATA_LOCATION_PROVIDER = MEDIA_LOCATION_PROVIDER_PREFIX + "metadata";
	private static final String FILE_NAME_LOCATION_PROVIDER = MEDIA_LOCATION_PROVIDER_PREFIX + "file_name";

	/**
	 * Returns the photo's EXIF coordinates with optional altitude and heading,
	 * or {@code null} when the photo has no EXIF coordinates.
	 */
	@Nullable
	public static Location getPhotoInformation(@NonNull File file) throws IOException {
		return getPhotoInformation(new ExifInterface(file.getAbsolutePath()));
	}

	@Nullable
	public static Location getPhotoInformation(@NonNull InputStream inputStream) throws IOException {
		return getPhotoInformation(new ExifInterface(inputStream));
	}

	@Nullable
	private static Location getPhotoInformation(@NonNull ExifInterface exif) {
		float[] latLon = new float[2];
		if (!exif.getLatLong(latLon)) {
			return null;
		}

		Location location = new Location(EXIF_LOCATION_PROVIDER, latLon[0], latLon[1]);
		double altitude = exif.getAltitude(Double.NaN);
		if (Double.isFinite(altitude)) {
			location.setAltitude(altitude);
		}
		double heading = exif.getAttributeDouble(ExifInterface.TAG_GPS_IMG_DIRECTION, Double.NaN);
		if (Double.isFinite(heading)) {
			location.setBearing((float) heading);
		}
		return location;
	}

	public static int getExifOrientation(@NonNull File file) {
		int orientation = 0;
		try {
			ExifInterface exif = new ExifInterface(file.getAbsolutePath());
			orientation = exif.getAttributeInt("Orientation", 1);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return orientation;
	}

	public static void updatePhotoInformation(@NonNull File file, double lat, double lon, Location loc, double rot) {
		try {
			ExifInterface exif = new ExifInterface(file.getAbsolutePath());
			exif.setAttribute("GPSLatitude", convertDegToExifRational(lat));
			exif.setAttribute("GPSLatitudeRef", lat > 0 ? "N" : "S");
			exif.setAttribute("GPSLongitude", convertDegToExifRational(lon));
			exif.setAttribute("GPSLongitudeRef", lon > 0 ? "E" : "W");
			if (!Double.isNaN(rot)) {
				exif.setAttribute("GPSImgDirectionRef", "T");
				while (rot < 0) {
					rot += 360;
				}
				while (rot > 360) {
					rot -= 360;
				}
				int abs = (int) (Math.abs(rot) * 100.0);
				String rotString = abs + "/100";
				exif.setAttribute("GPSImgDirection", rotString);
			}
			if (loc != null && loc.hasAltitude()) {
				double alt = loc.getAltitude();
				String altString = (int) (Math.abs(alt) * 100.0) + "/100";
				exif.setAttribute("GPSAltitude", altString);
				exif.setAttribute("GPSAltitudeRef", alt < 0 ? "1" : "0");
			}
			exif.saveAttributes();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public static void setMediaRecorderLocation(@NonNull MediaRecorder recorder, double lat, double lon) {
		if (MapUtils.isValidLatLon(lat, lon)) {
			recorder.setLocation((float) lat, (float) lon);
		}
	}

	@Nullable
	public static Location getLocation(@NonNull File file, @NonNull String legacyFileName) {
		MediaType mediaType = MediaType.fromFileName(file.getName());
		Location location = getLocationFromLegacyFileName(legacyFileName);
		if (location == null && mediaType == MediaType.PHOTO) {
			try {
				location = getPhotoInformation(file);
			} catch (IOException e) {
				log.error("Error reading photo location from " + file.getAbsolutePath(), e);
			}
		}
		return location != null ? location : getMediaLocation(file);
	}

	@Nullable
	@SuppressWarnings("resource")
	public static Location getMediaLocation(@NonNull File file) {
		MediaMetadataRetriever retriever = new MediaMetadataRetriever();
		try {
			retriever.setDataSource(file.getAbsolutePath());
			return parseMediaLocation(retriever.extractMetadata(METADATA_KEY_LOCATION));
		} catch (RuntimeException e) {
			log.error("Error reading media location from " + file.getAbsolutePath(), e);
			return null;
		} finally {
			try {
				retriever.release();
			} catch (IOException e) {
				log.error("Error releasing media metadata retriever", e);
			}
		}
	}

	@Nullable
	public static Location parseMediaLocation(@Nullable String value) {
		if (value == null) {
			return null;
		}
		String[] coordinates = value.replace("/", "").split("(?=[+-])");
		if (coordinates.length < 2 || coordinates.length > 3) {
			return null;
		}
		double lat = Algorithms.parseDoubleSilently(coordinates[0], Double.NaN);
		double lon = Algorithms.parseDoubleSilently(coordinates[1], Double.NaN);
		if (!MapUtils.isValidLatLon(lat, lon)) {
			return null;
		}
		Location location = new Location(METADATA_LOCATION_PROVIDER, lat, lon);
		if (coordinates.length == 3) {
			double altitude = Algorithms.parseDoubleSilently(coordinates[2], Double.NaN);
			if (Double.isFinite(altitude)) {
				location.setAltitude(altitude);
			}
		}
		return location;
	}

	@Nullable
	public static Location getLocationFromLegacyFileName(@NonNull String fileName) {
		String name = Algorithms.getFileNameWithoutExtension(fileName);
		int indexSeparator = name.lastIndexOf('.');
		if (indexSeparator < 0 || !isManagedMediaFileName(fileName.replace('@', '~'))) {
			return null;
		}
		String shortLink = name.substring(0, indexSeparator);
		int nameSeparator = shortLink.lastIndexOf(' ');
		if (nameSeparator >= 0) {
			shortLink = shortLink.substring(nameSeparator + 1);
		}
		String typePrefix = MediaType.fromFileName(fileName).getTypeName() + "_";
		if (shortLink.startsWith(typePrefix)) {
			shortLink = shortLink.substring(typePrefix.length());
		}
		if (Algorithms.isEmpty(shortLink)) {
			return null;
		}
		GeoParsedPoint point = MapUtils.decodeShortLinkString(shortLink);
		return new Location(FILE_NAME_LOCATION_PROVIDER, point.getLatitude(), point.getLongitude());
	}

	public static String convertDegToExifRational(double coordinate) {
		if (coordinate < 0) {
			coordinate = -coordinate;
		}
		String s = ((int) coordinate) + "/1,"; // degrees
		coordinate = (coordinate - ((int) coordinate)) * 60.0;
		s += (int) coordinate + "/1,"; // minutes
		coordinate = (coordinate - ((int) coordinate)) * 60000.0;
		s += (int) coordinate + "/1000"; // seconds
		// log.info("deg rational: " + s);
		return s;
	}
}