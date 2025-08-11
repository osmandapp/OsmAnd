package net.osmand.plus.utils;

import static java.util.Calendar.DAY_OF_YEAR;
import static java.util.Calendar.ERA;
import static java.util.Calendar.YEAR;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.google.openlocationcode.OpenLocationCode;
import com.jwetherell.openmap.common.LatLonPoint;
import com.jwetherell.openmap.common.MGRSPoint;
import com.jwetherell.openmap.common.ZonedUTMPoint;

import net.osmand.LocationConvert;
import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.data.City.CityType;
import net.osmand.data.LatLon;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.SwissGridApproximation;
import net.osmand.plus.helpers.LocaleHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.enums.AngularConstants;
import net.osmand.plus.settings.enums.VolumeUnit;
import net.osmand.shared.settings.enums.AltitudeMetrics;
import net.osmand.shared.settings.enums.MetricsConstants;
import net.osmand.shared.settings.enums.SpeedConstants;
import net.osmand.util.Algorithms;
import net.osmand.util.TextDirectionUtil;

import org.apache.commons.logging.Log;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class OsmAndFormatter {

	private static final Log log = PlatformUtil.getLog(OsmAndFormatter.class);

	public static final int OLC_FORMAT_PRECISION = 11; // 2.8 x 3.5 meters

	public static final float METERS_IN_KILOMETER = 1000f;
	public static final float METERS_IN_ONE_MILE = 1609.344f; // 1609.344
	public static final float METERS_IN_ONE_NAUTICALMILE = 1852f; // 1852

	public static final float YARDS_IN_ONE_METER = 1.0936f;
	public static final float FEET_IN_ONE_METER = YARDS_IN_ONE_METER * 3f;
	public static final float INCHES_IN_ONE_METER = FEET_IN_ONE_METER * 12;

	public static final float IMPERIAL_GALLONS_IN_LITER = 4.54609f;
	public static final float US_GALLONS_IN_LITER = 3.78541f;

	public static final int KILOGRAMS_IN_ONE_TON = 1000;
	public static final float POUNDS_IN_ONE_KILOGRAM = 2.2046f;

	private static final int SECONDS_IN_HOUR = 3600;
	private static final DecimalFormat fixed2 = new DecimalFormat("0.00");
	private static final DecimalFormat fixed1 = new DecimalFormat("0.0");

	private static final int[] ROUNDING_DISTANCE_BOUNDS = Algorithms.generate10BaseRoundingBounds(100, 5);

	private static boolean twelveHoursFormat;
	private static TimeFormatter fullTimeFormatter;
	private static TimeFormatter shortTimeFormatter;
	private static final String[] localDaysStr = getLettersStringArray(DateFormatSymbols.getInstance().getShortWeekdays(), 3);

	public static final float MILS_IN_DEGREE = 17.777778f;

	public static final int FORMAT_DEGREES_SHORT = 8;
	public static final int FORMAT_DEGREES = LocationConvert.FORMAT_DEGREES;
	public static final int FORMAT_MINUTES = LocationConvert.FORMAT_MINUTES;
	public static final int FORMAT_SECONDS = LocationConvert.FORMAT_SECONDS;
	public static final int UTM_FORMAT = LocationConvert.UTM_FORMAT;
	public static final int OLC_FORMAT = LocationConvert.OLC_FORMAT;
	public static final int MGRS_FORMAT = LocationConvert.MGRS_FORMAT;
	public static final int SWISS_GRID_FORMAT = LocationConvert.SWISS_GRID_FORMAT;
	public static final int SWISS_GRID_PLUS_FORMAT = LocationConvert.SWISS_GRID_PLUS_FORMAT;
	private static final char DELIMITER_DEGREES = '°';
	private static final char DELIMITER_MINUTES = '\'';
	private static final char DELIMITER_SECONDS = '″';

	private static final char NORTH = 'N';
	private static final char SOUTH = 'S';
	private static final char WEST = 'W';
	private static final char EAST = 'E';

	static {
		setTwelveHoursFormatting(false, Locale.getDefault());
		fixed2.setMinimumFractionDigits(2);
		fixed1.setMinimumFractionDigits(1);
		fixed1.setMinimumIntegerDigits(1);
		fixed2.setMinimumIntegerDigits(1);
	}

	public static void setTwelveHoursFormatting(boolean setTwelveHoursFormat, @NonNull Locale locale) {
		twelveHoursFormat = setTwelveHoursFormat;
		fullTimeFormatter = new TimeFormatter(locale, "H:mm:ss", "h:mm:ss a");
		shortTimeFormatter = new TimeFormatter(locale, "HH:mm", "h:mm a");
	}

	public static String getFormattedDuration(long seconds, @NonNull OsmandApplication app) {
		NumberFormat numberFormat = getNumberFormat(app);
		long hours = seconds / (60 * 60);
		long minutes = (seconds / 60) % 60;
		if (hours > 0) {
			return numberFormat.format(hours) + " "
					+ app.getString(R.string.osmand_parking_hour)
					+ (minutes > 0 ? " " + numberFormat.format(minutes) + " "
					+ app.getString(R.string.shared_string_minute_lowercase) : "");
		} else if (minutes > 0) {
			return numberFormat.format(minutes) + " " + app.getString(R.string.shared_string_minute_lowercase);
		} else {
			return "<" + numberFormat.format(1) + " " + app.getString(R.string.shared_string_minute_lowercase);
		}
	}

	@NonNull
	public static NumberFormat getNumberFormat(@NonNull OsmandApplication app) {
		LocaleHelper localeHelper = app.getLocaleHelper();
		Locale locale = localeHelper.getPreferredLocale() != null
				? localeHelper.getPreferredLocale() : localeHelper.getDefaultLocale();
		return NumberFormat.getInstance(locale);
	}

	public static String getFormattedDurationShort(int seconds) {
		int hours = seconds / (60 * 60);
		int minutes = (seconds / 60) % 60;
		int sec = seconds % 60;
		return hours + ":" + (minutes < 10 ? "0" + minutes : minutes) + ":" + (sec < 10 ? "0" + sec : sec);
	}

	public static String getFormattedDurationShortMinutes(int seconds) {
		int hours = seconds / (60 * 60);
		int minutes = (seconds / 60) % 60;
		return hours + ":" + (minutes < 10 ? "0" + minutes : minutes);
	}

	public static String getFormattedFullTime(long millis) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(millis);
		return fullTimeFormatter.format(calendar.getTime(), twelveHoursFormat);
	}

	public static String getFormattedTimeShort(long seconds, boolean useCurrentTime) {
		Calendar calendar = Calendar.getInstance();
		if (useCurrentTime) {
			calendar.setTimeInMillis(System.currentTimeMillis() + seconds * 1000);
		} else {
			calendar.setTimeInMillis(seconds * 1000);
		}
		Date date = calendar.getTime();
		String formattedTime = shortTimeFormatter.format(date, twelveHoursFormat);
		if (!isSameDay(calendar, Calendar.getInstance())) {
			formattedTime += " " + localDaysStr[calendar.get(Calendar.DAY_OF_WEEK)];
		}
		return formattedTime;
	}

	public static String getFormattedTimeRuntime(int seconds) {
		int totalMinutes = (seconds / 60);
		int hours = totalMinutes / 60;
		int minutes = totalMinutes % 60;
		return String.format(Locale.US, "%d:%02d", hours, minutes);
	}

	@NonNull
	public static Pair<String, String> getFormattedTime(@NonNull Context context, long time) {
		String timeFormatted;
		String period = null;

		boolean is24HourFormat = android.text.format.DateFormat.is24HourFormat(context);

		if (is24HourFormat) {
			timeFormatted = android.text.format.DateFormat.format("k:mm", time).toString();
		} else {
			timeFormatted = android.text.format.DateFormat.format("h:mm", time).toString();
			period = android.text.format.DateFormat.format("aa", time).toString();
		}

		return new Pair<>(timeFormatted, period);
	}


	public static DateFormat getDateFormat(@NonNull Context context) {
		return android.text.format.DateFormat.getDateFormat(context);
	}

	public static String getFormattedDate(Context context, long milliseconds) {
		return DateUtils.formatDateTime(context, milliseconds,
				DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL);
	}

	public static String getFormattedDateTime(Context context, long milliseconds) {
		return DateUtils.formatDateTime(context, milliseconds,
				DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_ALL);
	}

	public static long getStartOfToday() {
		return getStartOfDayForTime(System.currentTimeMillis());
	}

	public static long getStartOfDayForTime(long time) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(time);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTimeInMillis();
	}

	public static Date getTimeForTimeZone(long time, @NonNull String timeZone) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(time);
		calendar.setTimeZone(TimeZone.getTimeZone(timeZone));
		return new Date(calendar.getTimeInMillis());
	}

	public static String getFormattedTimeInterval(OsmandApplication app, double interval) {
		String unitsStr;
		double intervalInUnits;
		if (interval < 60) {
			unitsStr = app.getString(R.string.shared_string_sec);
			intervalInUnits = interval;
		} else if (interval % 60 == 0) {
			unitsStr = app.getString(R.string.int_min);
			intervalInUnits = (interval / 60);
		} else {
			unitsStr = app.getString(R.string.int_min);
			intervalInUnits = (interval / 60f);
		}
		String formattedInterval = Algorithms.isInt(intervalInUnits) ?
				String.format(Locale.US, "%d", (long) intervalInUnits) :
				String.format("%s", intervalInUnits);
		return formattedInterval + " " + unitsStr;
	}

	public static String getFormattedDistanceInterval(OsmandApplication app, double interval, OsmAndFormatterParams pms) {
		double roundedDist = calculateRoundedDist(interval, app);
		return getFormattedDistance((float) roundedDist, app, pms);
	}

	public static String getFormattedPredictionTime(@NonNull OsmandApplication app, double interpolationValue) {
		double seconds = interpolationValue / 100.0;
		String formattedValue = String.format(seconds % 1 == 0 ? "%.0f" : "%.1f", seconds);
		return app.getString(R.string.ltr_or_rtl_combine_via_space, formattedValue, app.getString(R.string.shared_string_sec));
	}

	public static double calculateRoundedDist(double distInMeters, OsmandApplication ctx) {
		OsmandSettings settings = ctx.getSettings();
		MetricsConstants mc = settings.METRIC_SYSTEM.get();
		double mainUnitInMeter = 1;
		double metersInSecondUnit = METERS_IN_KILOMETER;
		if (mc == MetricsConstants.MILES_AND_FEET) {
			mainUnitInMeter = FEET_IN_ONE_METER;
			metersInSecondUnit = METERS_IN_ONE_MILE;
		} else if (mc == MetricsConstants.MILES_AND_METERS) {
			mainUnitInMeter = 1;
			metersInSecondUnit = METERS_IN_ONE_MILE;
		} else if (mc == MetricsConstants.NAUTICAL_MILES_AND_METERS) {
			mainUnitInMeter = 1;
			metersInSecondUnit = METERS_IN_ONE_NAUTICALMILE;
		} else if (mc == MetricsConstants.NAUTICAL_MILES_AND_FEET) {
			mainUnitInMeter = FEET_IN_ONE_METER;
			metersInSecondUnit = METERS_IN_ONE_NAUTICALMILE;
		} else if (mc == MetricsConstants.MILES_AND_YARDS) {
			mainUnitInMeter = YARDS_IN_ONE_METER;
			metersInSecondUnit = METERS_IN_ONE_MILE;
		}

		// 1, 2, 5, 10, 20, 50, 100, 200, 500, 1000 ...
		int generator = 1;
		byte pointer = 1;
		double point = mainUnitInMeter;
		double roundDist = 1;
		while (distInMeters * point >= generator) {
			roundDist = (generator / point);
			if (pointer++ % 3 == 2) {
				generator = generator * 5 / 2;
			} else {
				generator *= 2;
			}

			if (point == mainUnitInMeter && metersInSecondUnit * mainUnitInMeter * 0.9f <= generator) {
				point = 1 / metersInSecondUnit;
				generator = 1;
				pointer = 1;
			}
		}
		//Miles exceptions: 2000ft->0.5mi, 1000ft->0.25mi, 1000yd->0.5mi, 500yd->0.25mi, 1000m ->0.5mi, 500m -> 0.25mi
		if (mc == MetricsConstants.MILES_AND_METERS && roundDist == 1000) {
			roundDist = 0.5f * METERS_IN_ONE_MILE;
		} else if (mc == MetricsConstants.MILES_AND_METERS && roundDist == 500) {
			roundDist = 0.25f * METERS_IN_ONE_MILE;
		} else if (mc == MetricsConstants.MILES_AND_FEET && roundDist == 2000 / (double) FEET_IN_ONE_METER) {
			roundDist = 0.5f * METERS_IN_ONE_MILE;
		} else if (mc == MetricsConstants.MILES_AND_FEET && roundDist == 1000 / (double) FEET_IN_ONE_METER) {
			roundDist = 0.25f * METERS_IN_ONE_MILE;
		} else if (mc == MetricsConstants.MILES_AND_YARDS && roundDist == 1000 / (double) YARDS_IN_ONE_METER) {
			roundDist = 0.5f * METERS_IN_ONE_MILE;
		} else if (mc == MetricsConstants.MILES_AND_YARDS && roundDist == 500 / (double) YARDS_IN_ONE_METER) {
			roundDist = 0.25f * METERS_IN_ONE_MILE;
		}
		return roundDist;
	}

	public static String getFormattedRoundDistanceKm(float meters, int digits, OsmandApplication ctx) {
		int mainUnitStr = R.string.km;
		float mainUnitInMeters = METERS_IN_KILOMETER;
		if (digits == 0) {
			return (int) (meters / mainUnitInMeters + 0.5) + " " + ctx.getString(mainUnitStr); //$NON-NLS-1$
		} else if (digits == 1) {
			return fixed1.format(meters / mainUnitInMeters) + " " + ctx.getString(mainUnitStr);
		} else {
			return fixed2.format(meters / mainUnitInMeters) + " " + ctx.getString(mainUnitStr);
		}
	}

	public static String getFormattedAlarmInfoDistance(OsmandApplication app, float meters) {
		boolean kmAndMeters = app.getSettings().METRIC_SYSTEM.get() == MetricsConstants.KILOMETERS_AND_METERS;
		int mainUnitStr = kmAndMeters ? R.string.km : R.string.mile;
		float mainUnitInMeters = kmAndMeters ? METERS_IN_KILOMETER : METERS_IN_ONE_MILE;
		DecimalFormat df = new DecimalFormat("#.#");

		return df.format(meters / mainUnitInMeters) + " " + app.getString(mainUnitStr);
	}

	public static String getFormattedAzimuth(float bearing, OsmandApplication app) {
		return getFormattedAzimuth(bearing, app.getSettings().ANGULAR_UNITS.get());
	}

	public static String getFormattedAzimuth(float bearing, AngularConstants angularConstant) {
		while (bearing < -180.0) {
			bearing += 360;
		}
		while (bearing > 360.0) {
			bearing -= 360;
		}
		switch (angularConstant) {
			case DEGREES360: {
				bearing += bearing < 0 ? 360 : 0;
				int b = Math.round(bearing);
				b = b == 360 ? 0 : b;
				return b + AngularConstants.DEGREES360.getUnitSymbol();
			}
			case MILLIRADS: {
				bearing += bearing < 0 ? 360 : 0;
				return Math.round(bearing * MILS_IN_DEGREE) + " " + AngularConstants.MILLIRADS.getUnitSymbol();
			}
			default:
				return Math.round(bearing) + AngularConstants.DEGREES.getUnitSymbol();
		}
	}

	@NonNull
	public static String getFormattedDistance(float meters, @NonNull OsmandApplication ctx) {
		return getFormattedDistance(meters, ctx, null);
	}

	@NonNull
	public static String getFormattedDistance(float meters, @NonNull OsmandApplication ctx, OsmAndFormatterParams pms) {
		MetricsConstants mc = ctx.getSettings().METRIC_SYSTEM.get();
		return getFormattedDistance(meters, ctx, pms, mc);
	}

	@NonNull
	public static String getFormattedDistance(float meters, @NonNull OsmandApplication ctx,
	                                          OsmAndFormatterParams pms, @NonNull MetricsConstants mc) {
		return getFormattedDistanceValue(meters, ctx, pms, mc).format(ctx);
	}

	public static FormattedValue getFormattedDistanceValue(float meters, @NonNull OsmandApplication ctx) {
		return getFormattedDistanceValue(meters, ctx, null, ctx.getSettings().METRIC_SYSTEM.get());
	}

	public static FormattedValue getFormattedDistanceValue(float meters, @NonNull OsmandApplication ctx, OsmAndFormatterParams pms) {
		return getFormattedDistanceValue(meters, ctx, pms, ctx.getSettings().METRIC_SYSTEM.get());
	}

	@NonNull
	public static FormattedValue getFormattedDistanceValue(float meters, @NonNull OsmandApplication ctx,
	                                                       OsmAndFormatterParams pms, @NonNull MetricsConstants mc) {
		int mainUnitStr;
		float mainUnitInMeters;
		if (pms == null) {
			pms = OsmAndFormatterParams.DEFAULT;
		}
		if (pms.isUseLowerBound() && ctx.getSettings().PRECISE_DISTANCE_NUMBERS.get()) {
			pms = OsmAndFormatterParams.DEFAULT;
		}
		if (mc == MetricsConstants.KILOMETERS_AND_METERS) {
			mainUnitStr = R.string.km;
			mainUnitInMeters = METERS_IN_KILOMETER;
		} else if (mc == MetricsConstants.NAUTICAL_MILES_AND_METERS || mc == MetricsConstants.NAUTICAL_MILES_AND_FEET) {
			mainUnitStr = R.string.nm;
			mainUnitInMeters = METERS_IN_ONE_NAUTICALMILE;
		} else {
			mainUnitStr = R.string.mile;
			mainUnitInMeters = METERS_IN_ONE_MILE;
		}

		float floatDistance = meters / mainUnitInMeters;
		boolean forceTrailingZeros = pms.forceTrailingZerosInDecimalMainUnit;
		int decimalPrecision = pms.extraDecimalPrecision;
		if(pms.forcePreciseValue) {
			return formatValue(floatDistance, mainUnitStr, forceTrailingZeros, decimalPrecision, ctx);
		} else if (meters >= 100 * mainUnitInMeters) {
			return formatValue((int) (meters / mainUnitInMeters + 0.5), mainUnitStr, forceTrailingZeros,
					0, ctx);
		} else if (meters > 9.99f * mainUnitInMeters) {
			return formatValue(floatDistance, mainUnitStr, forceTrailingZeros, decimalPrecision, ctx);
		} else if (meters > 0.999f * mainUnitInMeters) {
			return formatValue(floatDistance, mainUnitStr, forceTrailingZeros, 1 + decimalPrecision, ctx);
		} else if (mc == MetricsConstants.MILES_AND_FEET && meters > 0.249f * mainUnitInMeters) {
			return formatValue(floatDistance, mainUnitStr, forceTrailingZeros, 1 + decimalPrecision, ctx);
		} else if (mc == MetricsConstants.MILES_AND_METERS && meters > 0.249f * mainUnitInMeters) {
			return formatValue(floatDistance, mainUnitStr, forceTrailingZeros, 1 + decimalPrecision, ctx);
		} else if (mc == MetricsConstants.MILES_AND_YARDS && meters > 0.249f * mainUnitInMeters) {
			return formatValue(floatDistance, mainUnitStr, forceTrailingZeros, 1 + decimalPrecision, ctx);
		} else if (mc == MetricsConstants.NAUTICAL_MILES_AND_METERS && meters > 0.99f * mainUnitInMeters) {
			return formatValue(floatDistance, mainUnitStr, forceTrailingZeros, 1 + decimalPrecision, ctx);
		} else if (mc == MetricsConstants.NAUTICAL_MILES_AND_FEET && meters > 0.99f * mainUnitInMeters) {
			return formatValue(floatDistance, mainUnitStr, forceTrailingZeros, 1 + decimalPrecision, ctx);
		} else {

			if (mc == MetricsConstants.MILES_AND_FEET || mc == MetricsConstants.NAUTICAL_MILES_AND_FEET) {
				int feet = (int) (meters * FEET_IN_ONE_METER + 0.5);
				if (pms.isUseLowerBound()) {
					feet = Algorithms.lowerTo10BaseRoundingBounds(feet, ROUNDING_DISTANCE_BOUNDS);
				}
				return formatValue(feet, R.string.foot, forceTrailingZeros, 0, ctx);
			} else if (mc == MetricsConstants.MILES_AND_YARDS) {
				int yards = (int) (meters * YARDS_IN_ONE_METER + 0.5);
				if (pms.isUseLowerBound()) {
					yards = Algorithms.lowerTo10BaseRoundingBounds(yards, ROUNDING_DISTANCE_BOUNDS);
				}
				return formatValue(yards, R.string.yard, forceTrailingZeros, 0, ctx);
			}
			// mc == MetricsConstants.KILOMETERS_AND_METERS || mc == MetricsConstants.MILES_AND_METERS) {
			int m = (int) (meters + 0.5);
			if (pms.isUseLowerBound()) {
				m = Algorithms.lowerTo10BaseRoundingBounds(m, ROUNDING_DISTANCE_BOUNDS);
			}
			return formatValue(m, R.string.m, forceTrailingZeros, 0, ctx);
		}
	}

	@NonNull
	public static String getFormattedAlt(double alt, OsmandApplication ctx) {
		OsmandSettings settings = ctx.getSettings();
		AltitudeMetrics altitudeMetrics = settings.ALTITUDE_METRIC.get();
		return getFormattedAlt(alt, ctx, altitudeMetrics);
	}

	@NonNull
	public static String getFormattedAlt(double alt, OsmandApplication ctx, AltitudeMetrics altitudeMetrics) {
		return getFormattedAltitudeValue(alt, ctx, altitudeMetrics).format(ctx);
	}

	@NonNull
	public static FormattedValue getFormattedAltitudeValue(double altitude,
	                                                       @NonNull OsmandApplication ctx,
	                                                       @NonNull AltitudeMetrics am) {
		boolean useFeet = am.shouldUseFeet();
		FormattedValue formattedValue;
		if (useFeet) {
			int feet = (int) (altitude * FEET_IN_ONE_METER + 0.5);
			formattedValue = formatValue(feet, R.string.foot, false, 0, ctx);
		} else {
			int meters = (int) (altitude + 0.5);
			formattedValue = formatValue(meters, R.string.m, false, 0, ctx);
		}
		return formattedValue;
	}

	@NonNull
	public static String getFormattedSpeed(float metersPerSeconds, @NonNull OsmandApplication app) {
		return getFormattedSpeedValue(metersPerSeconds, app).format(app);
	}

	@NonNull
	public static String getFormattedSpeed(float metersPerSeconds, @NonNull OsmandApplication app, boolean hasFastSpeed, SpeedConstants speedFormat) {
		return getFormattedSpeedValue(metersPerSeconds, app, hasFastSpeed, speedFormat).format(app);
	}

	@NonNull
	public static FormattedValue getFormattedSpeedValue(float metersPerSeconds, @NonNull OsmandApplication app) {
		ApplicationMode mode = app.getSettings().getApplicationMode();
		return getFormattedSpeedValue(metersPerSeconds, app, mode.hasFastSpeed(), app.getSettings().SPEED_SYSTEM.getModeValue(mode));
	}

	public static float getMpSFromFormattedValue(OsmandApplication app, float value, SpeedConstants speedFormat) {
		return (value * getMetersInModeUnit(app, speedFormat) / METERS_IN_KILOMETER) / 3.6f;
	}

	public static float getMetersFromFormattedAltitudeValue(float altitude, MetricsConstants mc) {
		boolean useFeet = mc == MetricsConstants.MILES_AND_FEET || mc == MetricsConstants.MILES_AND_YARDS || mc == MetricsConstants.NAUTICAL_MILES_AND_FEET;
		if (useFeet) {
			return altitude / FEET_IN_ONE_METER;
		} else {
			return altitude;
		}
	}

	public static float convertToMeters(float distance, MetricsConstants mc) {
		float mainUnitInMeters;
		switch (mc) {
			case MILES_AND_FEET:
			case MILES_AND_METERS:
				mainUnitInMeters = METERS_IN_ONE_MILE;
				break;
			case NAUTICAL_MILES_AND_METERS:
			case NAUTICAL_MILES_AND_FEET:
				mainUnitInMeters = METERS_IN_ONE_NAUTICALMILE;
				break;
			default:
				mainUnitInMeters = METERS_IN_KILOMETER;
				break;
		}

		return distance * mainUnitInMeters;
	}

	@NonNull
	public static float getMetersInModeUnit(@NonNull OsmandApplication app, @NonNull SpeedConstants speedFormat) {
		float metersInUnit = 0f;
		switch (speedFormat) {
			case MILES_PER_HOUR:
			case MINUTES_PER_MILE:
				metersInUnit = METERS_IN_ONE_MILE;
				break;
			case KILOMETERS_PER_HOUR:
			case MINUTES_PER_KILOMETER:
				metersInUnit = METERS_IN_KILOMETER;
				break;
			case METERS_PER_SECOND:
				metersInUnit = 1f * SECONDS_IN_HOUR;
				break;
			case NAUTICALMILES_PER_HOUR:
				metersInUnit = METERS_IN_ONE_NAUTICALMILE;
				break;
			default:
				break;
		}
		return metersInUnit;
	}

	public static float convertSpeedToMetersPerSecond(float formattedValueSrc, SpeedConstants mc) {
		switch (mc) {
			case KILOMETERS_PER_HOUR:
				return formattedValueSrc / 3.6f;
			case MILES_PER_HOUR:
				return (float) (formattedValueSrc * METERS_IN_ONE_MILE / 3.6 * 1000);
			case NAUTICALMILES_PER_HOUR:
				return (float) (formattedValueSrc * METERS_IN_ONE_NAUTICALMILE / 3.6 * 1000);
			default:
				return formattedValueSrc;
		}
	}

	public static FormattedValue getFormattedFuelCapacityValue(@NonNull OsmandApplication app, @NonNull VolumeUnit volumeUnit, @NonNull ApplicationMode mode) {
		OsmandSettings settings = app.getSettings();
		boolean separateWithSpace = false;
		String textValue;
		String textUnit;
		float value = readSavedFuelTankCapacity(settings, volumeUnit, mode);
		if (value == 0.0f) {
			textValue = app.getString(R.string.shared_string_none);
			textUnit = "";
		} else {
			DecimalFormat formatter = new DecimalFormat("0.#", new DecimalFormatSymbols(Locale.US));
			textValue = formatter.format(value);
			textUnit = volumeUnit.getUnitSymbol(app);
			separateWithSpace = true;
		}
		return new FormattedValue(value, textValue, textUnit, separateWithSpace);
	}

	public static float readSavedFuelTankCapacity(@NonNull OsmandSettings settings, @NonNull VolumeUnit volumeUnit, @NonNull ApplicationMode mode) {
		OsmandPreference<Float> fuelTankCapacity = settings.FUEL_TANK_CAPACITY;
		float valueInLitres = fuelTankCapacity.getModeValue(mode);
		if (valueInLitres == 0.0f) {
			return valueInLitres;
		}

		return convertLiterToVolumeUnit(volumeUnit, valueInLitres);
	}

	public static float convertLiterToVolumeUnit(@NonNull VolumeUnit volumeUnit, float value){
		if (volumeUnit == VolumeUnit.US_GALLONS) {
			return value / US_GALLONS_IN_LITER;
		} else if (volumeUnit == VolumeUnit.IMPERIAL_GALLONS) {
			return value / IMPERIAL_GALLONS_IN_LITER;
		} else {
			return value;
		}
	}

	public static float prepareFuelTankCapacityToSave(@NonNull VolumeUnit volumeUnit, float value) {
		float preparedValueToSave = 0;
		if (value != 0.0f) {
			if (volumeUnit == VolumeUnit.US_GALLONS) {
				preparedValueToSave = value * US_GALLONS_IN_LITER;
			} else if (volumeUnit == VolumeUnit.IMPERIAL_GALLONS) {
				preparedValueToSave = value * IMPERIAL_GALLONS_IN_LITER;
			} else {
				preparedValueToSave = value;
			}
		}
		return preparedValueToSave;
	}

	@NonNull
	public static FormattedValue getFormattedSpeedValue(float metersPerSeconds, @NonNull OsmandApplication app, boolean hasFastSpeed, SpeedConstants mc) {
		String unit = mc.toShortString();
		float kmh = metersPerSeconds * 3.6f;
		if (mc == SpeedConstants.KILOMETERS_PER_HOUR) {
			// e.g. car case and for high-speeds: Display rounded to 1 km/h (5% precision at 20 km/h)
			if (kmh >= 20 || hasFastSpeed) {
				return getFormattedSpeed(Math.round(kmh), unit, app);
			}
			// for smaller values display 1 decimal digit x.y km/h, (0.5% precision at 20 km/h)
			int kmh10 = Math.round(kmh * 10f);
			return getFormattedLowSpeed(kmh10 / 10f, unit, app);
		} else if (mc == SpeedConstants.MILES_PER_HOUR) {
			float mph = kmh * METERS_IN_KILOMETER / METERS_IN_ONE_MILE;
			if (mph >= 20 || hasFastSpeed) {
				return getFormattedSpeed(Math.round(mph), unit, app);
			} else {
				int mph10 = Math.round(mph * 10f);
				return getFormattedLowSpeed(mph10 / 10f, unit, app);
			}
		} else if (mc == SpeedConstants.NAUTICALMILES_PER_HOUR) {
			float mph = kmh * METERS_IN_KILOMETER / METERS_IN_ONE_NAUTICALMILE;
			if (mph >= 20 || hasFastSpeed) {
				return getFormattedSpeed(Math.round(mph), unit, app);
			} else {
				int mph10 = Math.round(mph * 10f);
				return getFormattedLowSpeed(mph10 / 10f, unit, app);
			}
		} else if (mc == SpeedConstants.MINUTES_PER_KILOMETER) {
			if (metersPerSeconds < 0.111111111) {
				return new FormattedValue(metersPerSeconds, "-", unit, false);
			}
			float minPerKm = METERS_IN_KILOMETER / (metersPerSeconds * 60);
			if (minPerKm >= 10) {
				return getFormattedSpeed(Math.round(minPerKm), unit, app);
			} else {
				int seconds = Math.round(minPerKm * 60);
				return new FormattedValue(seconds, Algorithms.formatDuration(seconds, false), unit);
			}
		} else if (mc == SpeedConstants.MINUTES_PER_MILE) {
			if (metersPerSeconds < 0.111111111) {
				return new FormattedValue(metersPerSeconds, "-", unit, false);
			}
			float minPerM = (METERS_IN_ONE_MILE) / (metersPerSeconds * 60);
			if (minPerM >= 10) {
				return getFormattedSpeed(Math.round(minPerM), unit, app);
			} else {
				int mph10 = Math.round(minPerM * 10f);
				return getFormattedLowSpeed(mph10 / 10f, unit, app);
			}
		} else {
			String metersPerSecond = SpeedConstants.METERS_PER_SECOND.toShortString();
			if (metersPerSeconds >= 10) {
				return getFormattedSpeed(Math.round(metersPerSeconds), metersPerSecond, app);
			}
			// for smaller values display 1 decimal digit x.y km/h, (0.5% precision at 20 km/h)
			int kmh10 = Math.round(metersPerSeconds * 10f);
			return getFormattedLowSpeed(kmh10 / 10f, metersPerSecond, app);
		}
	}

	@NonNull
	public static SpeedConstants getSpeedModeForPaceMode(@NonNull SpeedConstants originalMode) {
		return switch (originalMode) {
			case MINUTES_PER_KILOMETER -> SpeedConstants.KILOMETERS_PER_HOUR;
			case MINUTES_PER_MILE -> SpeedConstants.MILES_PER_HOUR;
			default -> originalMode;
		};
	}

	@NonNull
	private static FormattedValue getFormattedSpeed(float speed, @NonNull String unit, @NonNull OsmandApplication app) {
		return formatValue(speed, unit, false, 0, app);
	}

	@NonNull
	private static FormattedValue getFormattedLowSpeed(float speed, @NonNull String unit, @NonNull OsmandApplication app) {
		return formatValue(speed, unit, true, 1, app);
	}

	@NonNull
	public static String formatInteger(int value, @NonNull String unit, @NonNull OsmandApplication app) {
		return formatIntegerValue(value, unit, app).format(app);
	}

	@NonNull
	public static FormattedValue formatIntegerValue(int value, @NonNull String unit, @NonNull OsmandApplication app) {
		return formatValue(value, unit, false, 0, app);
	}

	@NonNull
	public static FormattedValue formatValue(float value, @StringRes int unitId, boolean forceTrailingZeroes,
	                                         int decimalPlacesNumber, @NonNull OsmandApplication app) {

		return formatValue(value, app.getString(unitId), unitId, forceTrailingZeroes, decimalPlacesNumber, app);
	}

	@NonNull
	public static FormattedValue formatValue(float value, @NonNull String unit, boolean forceTrailingZeroes,
	                                         int decimalPlacesNumber, @NonNull OsmandApplication app) {
		return formatValue(value, unit, -1, forceTrailingZeroes, decimalPlacesNumber, app);
	}

	@NonNull
	public static FormattedValue formatValue(float value, @NonNull String unit, @StringRes int unitId, boolean forceTrailingZeroes,
	                                         int decimalPlacesNumber, @NonNull OsmandApplication app) {
		String pattern = "0";
		if (decimalPlacesNumber > 0) {
			char fractionDigitPattern = forceTrailingZeroes ? '0' : '#';
			char[] fractionDigitsPattern = new char[decimalPlacesNumber];
			Arrays.fill(fractionDigitsPattern, fractionDigitPattern);
			pattern += "." + String.valueOf(fractionDigitsPattern);
		}

		Locale preferredLocale = app.getLocaleHelper().getPreferredLocale();
		Locale locale = preferredLocale != null ? preferredLocale : Locale.getDefault();

		DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(locale);
		decimalFormatSymbols.setGroupingSeparator(' ');

		DecimalFormat decimalFormat = new DecimalFormat(pattern);
		decimalFormat.setDecimalFormatSymbols(decimalFormatSymbols);

		boolean fiveOrMoreDigits = Math.abs(value) >= 10_000;
		if (fiveOrMoreDigits) {
			decimalFormat.setGroupingUsed(true);
			decimalFormat.setGroupingSize(3);
		}

		MessageFormat messageFormat = new MessageFormat("{0}");
		messageFormat.setFormatByArgumentIndex(0, decimalFormat);
		String formattedValue = messageFormat.format(new Object[] {value})
				.replace('\n', ' ');
		return new FormattedValue(value, formattedValue, unit, unitId, true);
	}

	public static boolean isSameDay(long firstTime, long secondTime) {
		return isSameDay(new Date(firstTime), new Date(secondTime));
	}

	public static boolean isSameDay(@NonNull Date firstDate, @NonNull Date secondDate) {
		Calendar firstCal = Calendar.getInstance();
		firstCal.setTime(firstDate);
		Calendar secondCal = Calendar.getInstance();
		secondCal.setTime(secondDate);
		return isSameDay(firstCal, secondCal);
	}

	public static boolean isSameDay(@NonNull Calendar first, @NonNull Calendar second) {
		return first.get(ERA) == second.get(ERA)
				&& first.get(YEAR) == second.get(YEAR)
				&& first.get(DAY_OF_YEAR) == second.get(DAY_OF_YEAR);
	}

	public static String toPublicString(CityType t, Context ctx) {
		switch (t) {
			case CITY:
				return ctx.getString(R.string.city_type_city);
			case HAMLET:
				return ctx.getString(R.string.city_type_hamlet);
			case TOWN:
				return ctx.getString(R.string.city_type_town);
			case VILLAGE:
				return ctx.getString(R.string.city_type_village);
			case SUBURB:
				return ctx.getString(R.string.city_type_suburb);
			default:
				break;
		}
		return "";
	}

	public static List<String> getPoiStringsWithoutType(Amenity amenity, String locale, boolean transliterate) {
		PoiCategory pc = amenity.getType();
		PoiType pt = pc.getPoiTypeByKeyName(amenity.getSubType());
		String typeName = amenity.getSubType();
		if (pt != null) {
			typeName = pt.getTranslation();
		} else if (typeName != null) {
			typeName = Algorithms.capitalizeFirstLetterAndLowercase(typeName.replace('_', ' '));
		}
		List<String> res = new ArrayList<>();
		String localName = amenity.getName(locale, transliterate);
		addPoiString(typeName, localName, res);
		for (String name : amenity.getOtherNames(true)) {
			addPoiString(typeName, name, res);
		}
		for (String name : amenity.getAdditionalInfoValues(false)) {
			addPoiString(typeName, name, res);
		}
		return res;
	}

	private static void addPoiString(String poiTypeName, String poiName, List<String> res) {
		if (poiTypeName != null && poiName.contains(poiTypeName)) {
			res.add(poiName);
		}
		if (poiName.length() == 0) {
			res.add(poiTypeName);
		}
		res.add(poiTypeName + " " + poiName);
	}

	public static String getAmenityDescriptionContent(OsmandApplication ctx, Amenity amenity, boolean shortDescription) {
		StringBuilder d = new StringBuilder();
		if (amenity.getType().isWiki()) {
			return "";
		}
		MapPoiTypes poiTypes = ctx.getPoiTypes();
		for (String key : amenity.getAdditionalInfoKeys()) {
			String vl = amenity.getAdditionalInfo(key);
			if (key.startsWith("name:")) {
				continue;
			} else if (vl.length() >= 150) {
				if (shortDescription) {
					continue;
				}
			} else if (Amenity.OPENING_HOURS.equals(key)) {
				d.append(ctx.getString(R.string.opening_hours) + ": ");
			} else if (Amenity.PHONE.equals(key)) {
				d.append(ctx.getString(R.string.phone) + ": ");
			} else if (Amenity.WEBSITE.equals(key)) {
				d.append(ctx.getString(R.string.website) + ": ");
			} else {
				AbstractPoiType pt = poiTypes.getAnyPoiAdditionalTypeByKey(key);
				if (pt != null) {
					if (pt instanceof PoiType && !((PoiType) pt).isText()) {
						vl = pt.getTranslation();
					} else {
						vl = pt.getTranslation() + ": " + vl;
					}
				} else {
					vl = Algorithms.capitalizeFirstLetterAndLowercase(key) + ": " + vl;
				}
			}
			d.append(vl).append('\n');
		}
		return d.toString().trim();
	}

	private static String[] getLettersStringArray(String[] strings, int letters) {
		String[] newStrings = new String[strings.length];
		for (int i = 0; i < strings.length; i++) {
			if (strings[i] != null) {
				if (strings[i].length() > letters) {
					newStrings[i] = Algorithms.capitalizeFirstLetter(strings[i].substring(0, letters));
				} else {
					newStrings[i] = Algorithms.capitalizeFirstLetter(strings[i]);
				}
			}
		}
		return newStrings;
	}

	public static String getFormattedCoordinates(double lat, double lon, int outputFormat) {
		return getFormattedCoordinates(lat, lon, outputFormat, true);
	}

	public static String getFormattedCoordinates(double lat, double lon, int outputFormat, boolean forceLTR) {
		StringBuilder result = new StringBuilder();
		if (outputFormat == FORMAT_DEGREES_SHORT) {
			result.append(formatCoordinate(lat, outputFormat)).append(" ").append(formatCoordinate(lon, outputFormat));
		} else if (outputFormat == FORMAT_DEGREES || outputFormat == FORMAT_MINUTES || outputFormat == FORMAT_SECONDS) {
			result.append(formatCoordinate(lat, outputFormat)).append(" ")
					.append(lat > 0 ? NORTH : SOUTH).append(", ")
					.append(formatCoordinate(lon, outputFormat)).append(" ")
					.append(lon > 0 ? EAST : WEST);
		} else if (outputFormat == UTM_FORMAT) {
			ZonedUTMPoint utmPoint = new ZonedUTMPoint(new LatLonPoint(lat, lon));
			result.append(utmPoint.format());
		} else if (outputFormat == OLC_FORMAT) {
			String code = getOpenLocationCode(lat, lon);
			result.append(code);
		} else if (outputFormat == MGRS_FORMAT) {
			MGRSPoint pnt = new MGRSPoint(new LatLonPoint(lat, lon));
			try {
				result.append(pnt.toFlavoredString(5));
			} catch (java.lang.Error e) {
				e.printStackTrace();
			}
		} else if (outputFormat == SWISS_GRID_FORMAT) {
			double[] swissGrid = SwissGridApproximation.convertWGS84ToLV03(new LatLon(lat, lon));
			DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(Locale.US);
			formatSymbols.setDecimalSeparator('.');
			formatSymbols.setGroupingSeparator(' ');
			DecimalFormat swissGridFormat = new DecimalFormat("###,###.##", formatSymbols);
			result.append(swissGridFormat.format(swissGrid[0])).append(", ").append(swissGridFormat.format(swissGrid[1]));
		} else if (outputFormat == SWISS_GRID_PLUS_FORMAT) {
			double[] swissGrid = SwissGridApproximation.convertWGS84ToLV95(new LatLon(lat, lon));
			DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(Locale.US);
			formatSymbols.setDecimalSeparator('.');
			formatSymbols.setGroupingSeparator(' ');
			DecimalFormat swissGridFormat = new DecimalFormat("###,###.##", formatSymbols);
			result.append(swissGridFormat.format(swissGrid[0])).append(", ").append(swissGridFormat.format(swissGrid[1]));
		}
		String formattedCoordinates = result.toString();
		return forceLTR ? TextDirectionUtil.markAsLTR(formattedCoordinates) : formattedCoordinates;
	}


	private static String formatCoordinate(double coordinate, int outputType) {

		if (coordinate < -180.0 || coordinate > 180.0 || Double.isNaN(coordinate)) {
			return "Error. Wrong coordinates data!";
		}
		if ((outputType != FORMAT_DEGREES) && (outputType != FORMAT_MINUTES) && (outputType
				!= FORMAT_SECONDS) && (outputType != FORMAT_DEGREES_SHORT)) {
			return "Unknown Output Format!";
		}

		DecimalFormat degDf = new DecimalFormat("##0.00000", new DecimalFormatSymbols(Locale.US));
		DecimalFormat minDf = new DecimalFormat("00.000", new DecimalFormatSymbols(Locale.US));
		DecimalFormat secDf = new DecimalFormat("00.0", new DecimalFormatSymbols(Locale.US));

		StringBuilder sb = new StringBuilder();

		if (coordinate < 0) {
			if (outputType == FORMAT_DEGREES_SHORT) {
				sb.append('-');
			}
			coordinate = -coordinate;
		}

		if (outputType == FORMAT_DEGREES_SHORT) {
			sb.append(degDf.format(coordinate));
		} else if (outputType == FORMAT_DEGREES) {
			sb.append(degDf.format(coordinate)).append(DELIMITER_DEGREES);
		} else if (outputType == FORMAT_MINUTES) {
			sb.append(minDf.format(formatCoordinate(coordinate, sb, DELIMITER_DEGREES)))
					.append(DELIMITER_MINUTES);
		} else {
			sb.append(secDf.format(formatCoordinate(
							formatCoordinate(coordinate, sb, DELIMITER_DEGREES), sb, DELIMITER_MINUTES)))
					.append(DELIMITER_SECONDS);
		}
		return sb.toString();
	}

	private static double formatCoordinate(double coordinate, StringBuilder sb, char delimiter) {
		int deg = (int) Math.floor(coordinate);
		if (deg < 10) {
			sb.append('0');
		}
		sb.append(deg);
		sb.append(delimiter);
		coordinate -= deg;
		coordinate *= 60.0;
		return coordinate;
	}

	@NonNull
	public static String formatFps(float fps) {
		return fps > 0 ? String.format(Locale.US, "%.1f", fps) : "-";
	}

	@NonNull
	public static String getOpenLocationCode(double lat, double lon) {
		try {
			return OpenLocationCode.encode(lat, lon, OLC_FORMAT_PRECISION);
		} catch (RuntimeException e) {
			log.error("Failed to define OLC location", e);
		}
		return "0, 0";
	}
}