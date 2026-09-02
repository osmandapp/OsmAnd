package net.osmand.plus.plugins.panoramax;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A single picture of the Panoramax "pictures" vector tile layer.
 *
 * Panoramax property names differ from Mapillary ones, see {@link PanoramaxImage#setData(Map)}:
 * heading instead of compass_angle, ts instead of captured_at, account_id instead of
 * organization_id, and panoramas are flagged by type = "equirectangular" rather than by a
 * boolean. Property values also differ in kind - ids are UUID strings, not numbers.
 */
public class PanoramaxImage {

	public static final String IMAGE_ID_KEY = "id";
	public static final String TIMESTAMP_KEY = "ts";
	public static final String HEADING_KEY = "heading";
	public static final String ACCOUNT_ID_KEY = "account_id";
	public static final String SEQUENCE_ID_KEY = "first_sequence";
	public static final String TYPE_KEY = "type";
	// The sequences layer dates its lines with a plain day instead of a timestamp.
	public static final String DATE_KEY = "date";

	public static final String TYPE_EQUIRECTANGULAR = "equirectangular";

	/**
	 * Timestamps arrive as "2024-07-18 07:48:24.248+00": a space separator instead of the ISO-8601
	 * 'T', a variable number of fractional digits, and a two digit UTC offset. Neither
	 * Instant.parse() nor any single SimpleDateFormat pattern accepts that.
	 *
	 * The fractional part is the trap. Across a sample of 1249 pictures from one tile the counts
	 * were 3 digits 1037x, 2 digits 125x, none 44x, 6 digits 26x, 1 digit 11x, 5 digits 5x and
	 * 4 digits 1x, so a fixed ".SSS" pattern would mishandle roughly one timestamp in six. Every
	 * variable part is therefore optional here, and seconds and the offset are optional too
	 * because the API documents them as such.
	 */
	private static final Pattern TIMESTAMP_PATTERN = Pattern.compile(
			"^(\\d{4})-(\\d{2})-(\\d{2})(?:[T ](\\d{2}):(\\d{2})(?::(\\d{2}))?(?:\\.(\\d+))?)?\\s*(Z|[+-]\\d{2}(?::?\\d{2})?)?$");

	// Image location
	private final double latitude;
	private final double longitude;
	// Camera heading. -1 if not found.
	private double compassAngle = -1;
	// When the image was captured, expressed as UTC epoch time in milliseconds. 0 if not found.
	private long capturedAt;
	private String imageId;
	private boolean panoramicImage;
	private String sequenceId;
	// Can be absent
	private String accountId;

	public PanoramaxImage(double latitude, double longitude, double compassAngle, long capturedAt,
	                      String imageId, boolean panoramicImage, String sequenceId, String accountId) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.compassAngle = compassAngle;
		this.capturedAt = capturedAt;
		this.imageId = imageId;
		this.panoramicImage = panoramicImage;
		this.sequenceId = sequenceId;
		this.accountId = accountId;
	}

	public PanoramaxImage(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	/**
	 * Unlike the Mapillary counterpart this does not fail when an optional property is absent.
	 * Panoramax is federated and instances are free to emit only the properties the tile
	 * specification makes mandatory, so only the image id is actually required here.
	 */
	public boolean setData(Map<?, ?> userData) {
		if (userData == null) {
			return false;
		}
		Object id = userData.get(IMAGE_ID_KEY);
		if (id == null) {
			return false;
		}
		this.imageId = id.toString();
		this.capturedAt = parseCaptureTime(userData);
		Object heading = userData.get(HEADING_KEY);
		this.compassAngle = heading instanceof Number ? ((Number) heading).doubleValue() : -1;
		Object sequenceId = userData.get(SEQUENCE_ID_KEY);
		this.sequenceId = sequenceId != null ? sequenceId.toString() : null;
		Object accountId = userData.get(ACCOUNT_ID_KEY);
		this.accountId = accountId != null ? accountId.toString() : null;
		Object type = userData.get(TYPE_KEY);
		this.panoramicImage = type != null && TYPE_EQUIRECTANGULAR.equalsIgnoreCase(type.toString());
		return true;
	}

	/**
	 * Reads the capture time of a tile feature.
	 *
	 * The two layers date their features differently: "pictures" carries "ts" with a full
	 * timestamp, "sequences" carries "date" with a plain day. Both are filtered against the same
	 * date range, so the caller must not assume either key. Reading only "ts" would score every
	 * sequence as 0 and make the whole overview disappear as soon as a date filter is set.
	 */
	public static long parseCaptureTime(Map<?, ?> userData) {
		if (userData == null) {
			return 0;
		}
		Object timestamp = userData.get(TIMESTAMP_KEY);
		return timestamp != null ? parseTimestamp(timestamp) : parseTimestamp(userData.get(DATE_KEY));
	}

	/**
	 * @return the capture time as UTC epoch milliseconds, or 0 when absent or unparsable.
	 * A timestamp without an offset is read as UTC, which is what the API documents.
	 */
	public static long parseTimestamp(Object value) {
		if (value == null) {
			return 0;
		}
		if (value instanceof Number) {
			return ((Number) value).longValue();
		}
		Matcher matcher = TIMESTAMP_PATTERN.matcher(value.toString().trim());
		if (!matcher.matches()) {
			return 0;
		}
		try {
			int year = Integer.parseInt(matcher.group(1));
			int month = Integer.parseInt(matcher.group(2));
			int day = Integer.parseInt(matcher.group(3));
			int hour = matcher.group(4) != null ? Integer.parseInt(matcher.group(4)) : 0;
			int minute = matcher.group(5) != null ? Integer.parseInt(matcher.group(5)) : 0;
			int second = matcher.group(6) != null ? Integer.parseInt(matcher.group(6)) : 0;

			// Keep millisecond resolution whatever the precision on the wire: pad "1" to "100" and
			// truncate "123456" to "123" rather than reading either as a raw number of milliseconds.
			int millis = 0;
			String fraction = matcher.group(7);
			if (fraction != null) {
				String normalized = (fraction + "000").substring(0, 3);
				millis = Integer.parseInt(normalized);
			}

			LocalDateTime dateTime = LocalDateTime.of(year, month, day, hour, minute, second);
			return dateTime.toInstant(parseOffset(matcher.group(8))).toEpochMilli() + millis;
		} catch (RuntimeException e) {
			return 0;
		}
	}

	/**
	 * Accepts "Z", "+HH", "+HHMM" and "+HH:MM". A missing offset is read as UTC, which is what
	 * the API documents. Uses a total number of seconds because ZoneOffset.ofHoursMinutes()
	 * rejects mixed signs.
	 */
	private static ZoneOffset parseOffset(String offset) {
		if (offset == null || "Z".equalsIgnoreCase(offset)) {
			return ZoneOffset.UTC;
		}
		int sign = offset.charAt(0) == '-' ? -1 : 1;
		String digits = offset.substring(1).replace(":", "");
		int hours = Integer.parseInt(digits.substring(0, 2));
		int minutes = digits.length() >= 4 ? Integer.parseInt(digits.substring(2, 4)) : 0;
		return ZoneOffset.ofTotalSeconds(sign * (hours * 3600 + minutes * 60));
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public double getCompassAngle() {
		return compassAngle;
	}

	public long getCapturedAt() {
		return capturedAt;
	}

	public String getImageId() {
		return imageId;
	}

	public boolean isPanoramicImage() {
		return panoramicImage;
	}

	public String getSKey() {
		return sequenceId;
	}

	public String getAccountId() {
		return accountId;
	}
}
