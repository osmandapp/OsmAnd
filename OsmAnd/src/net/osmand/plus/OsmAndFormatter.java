package net.osmand.plus;

import static net.osmand.data.PointDescription.getLocationOlcName;

import android.content.Context;
import android.text.format.DateUtils;

import com.jwetherell.openmap.common.LatLonPoint;
import com.jwetherell.openmap.common.UTMPoint;
import java.text.DecimalFormatSymbols;
import net.osmand.LocationConvert;
import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.data.City.CityType;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandSettings.AngularConstants;
import net.osmand.plus.OsmandSettings.MetricsConstants;
import net.osmand.plus.OsmandSettings.SpeedConstants;
import net.osmand.util.Algorithms;

import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map.Entry;

public class OsmAndFormatter {
	public final static float METERS_IN_KILOMETER = 1000f;
	public final static float METERS_IN_ONE_MILE = 1609.344f; // 1609.344
	public final static float METERS_IN_ONE_NAUTICALMILE = 1852f; // 1852
	
	public final static float YARDS_IN_ONE_METER = 1.0936f;
	public final static float FEET_IN_ONE_METER = YARDS_IN_ONE_METER * 3f;
	private static final DecimalFormat fixed2 = new DecimalFormat("0.00");
	private static final DecimalFormat fixed1 = new DecimalFormat("0.0");
	private static final SimpleDateFormat SIMPLE_TIME_OF_DAY_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());
	private static final String[] localDaysStr = getLettersStringArray(DateFormatSymbols.getInstance().getShortWeekdays(), 3);

	public static final int FORMAT_DEGREES_SHORT = 6;
	public static final int FORMAT_DEGREES = LocationConvert.FORMAT_DEGREES;
	public static final int FORMAT_MINUTES = LocationConvert.FORMAT_MINUTES;
	public static final int FORMAT_SECONDS = LocationConvert.FORMAT_SECONDS;
	public static final int UTM_FORMAT = LocationConvert.UTM_FORMAT;
	public static final int OLC_FORMAT = LocationConvert.OLC_FORMAT;
	private static final char DELIMITER_DEGREES = '°';
	private static final char DELIMITER_MINUTES = '′';
	private static final char DELIMITER_SECONDS = '″';

	private static final char NORTH = 'N';
	private static final char SOUTH = 'S';
	private static final char WEST = 'W';
	private static final char EAST = 'E';

	{
		fixed2.setMinimumFractionDigits(2);
		fixed1.setMinimumFractionDigits(1);
		fixed1.setMinimumIntegerDigits(1);
		fixed2.setMinimumIntegerDigits(1);
	}

	public static String getFormattedDuration(int seconds, OsmandApplication ctx) {
		int hours = seconds / (60 * 60);
		int minutes = (seconds / 60) % 60;
		if (hours > 0) {
			return hours + " "
					+ ctx.getString(R.string.osmand_parking_hour)
					+ (minutes > 0 ? " " + minutes + " "
					+ ctx.getString(R.string.osmand_parking_minute) : "");
		} else {
			return minutes + " " + ctx.getString(R.string.osmand_parking_minute);
		}
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

	public static String getFormattedTime(long seconds, boolean useCurrentTime) {
		Calendar calendar = Calendar.getInstance();
		if (useCurrentTime) {
			calendar.setTimeInMillis(System.currentTimeMillis() + seconds * 1000);
		} else {
			calendar.setTimeInMillis(seconds * 1000);
		}
		if (isSameDay(calendar, Calendar.getInstance())) {
			return SIMPLE_TIME_OF_DAY_FORMAT.format(calendar.getTime());
		} else {
			return SIMPLE_TIME_OF_DAY_FORMAT.format(calendar.getTime()) + " " + localDaysStr[calendar.get(Calendar.DAY_OF_WEEK)];
		}
	}

	public static String getFormattedTimeShort(long seconds) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(seconds * 1000);
		return SIMPLE_TIME_OF_DAY_FORMAT.format(calendar.getTime());
	}

	public static String getFormattedDate(Context context, long milliseconds) {
		return DateUtils.formatDateTime(context, milliseconds, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL);
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
		} else if (mc == MetricsConstants.NAUTICAL_MILES) {
			mainUnitInMeter = 1;
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
			return fixed1.format(((float) meters) / mainUnitInMeters) + " " + ctx.getString(mainUnitStr); 
		} else {
			return fixed2.format(((float) meters) / mainUnitInMeters) + " " + ctx.getString(mainUnitStr);
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
		while(bearing < -180.0) {
			bearing += 360;
		}
		while(bearing > 360.0) {
			bearing -= 360;
		}
		
		if (app.getSettings().ANGULAR_UNITS.get() == AngularConstants.MILLIRADS) {
			return Math.round(bearing * 17.4533) + " " + AngularConstants.MILLIRADS.getUnitSymbol();
		} else if (app.getSettings().ANGULAR_UNITS.get() == AngularConstants.DEGREES360) {
			if (bearing < 0) {
				return (359 + Math.round(bearing)) + AngularConstants.DEGREES360.getUnitSymbol();
			} else {
				return Math.round(bearing) + AngularConstants.DEGREES360.getUnitSymbol();
			}
		} else {
			return Math.round(bearing) + AngularConstants.DEGREES.getUnitSymbol();
		}

	}

	public static String getFormattedDistance(float meters, OsmandApplication ctx) {
		return getFormattedDistance(meters, ctx, true);
	}

	public static String getFormattedDistance(float meters, OsmandApplication ctx, boolean forceTrailingZeros) {
		String format1 = forceTrailingZeros ? "{0,number,0.0} " : "{0,number,0.#} ";
		String format2 = forceTrailingZeros ? "{0,number,0.00} " : "{0,number,0.##} ";

		OsmandSettings settings = ctx.getSettings();
		MetricsConstants mc = settings.METRIC_SYSTEM.get();
		int mainUnitStr;
		float mainUnitInMeters;
		if (mc == MetricsConstants.KILOMETERS_AND_METERS) {
			mainUnitStr = R.string.km;
			mainUnitInMeters = METERS_IN_KILOMETER;
		} else if (mc == MetricsConstants.NAUTICAL_MILES) {
			mainUnitStr = R.string.nm;
			mainUnitInMeters = METERS_IN_ONE_NAUTICALMILE;
		} else {
			mainUnitStr = R.string.mile;
			mainUnitInMeters = METERS_IN_ONE_MILE;
		}

		if (meters >= 100 * mainUnitInMeters) {
			return (int) (meters / mainUnitInMeters + 0.5) + " " + ctx.getString(mainUnitStr); //$NON-NLS-1$
		} else if (meters > 9.99f * mainUnitInMeters) {
			return MessageFormat.format(format1 + ctx.getString(mainUnitStr), ((float) meters) / mainUnitInMeters).replace('\n', ' '); //$NON-NLS-1$
		} else if (meters > 0.999f * mainUnitInMeters) {
			return MessageFormat.format(format2 + ctx.getString(mainUnitStr), ((float) meters) / mainUnitInMeters).replace('\n', ' '); //$NON-NLS-1$
		} else if (mc == MetricsConstants.MILES_AND_FEET && meters > 0.249f * mainUnitInMeters) {
			return MessageFormat.format(format2 + ctx.getString(mainUnitStr), ((float) meters) / mainUnitInMeters).replace('\n', ' '); //$NON-NLS-1$
		} else if (mc == MetricsConstants.MILES_AND_METERS && meters > 0.249f * mainUnitInMeters) {
			return MessageFormat.format(format2 + ctx.getString(mainUnitStr), ((float) meters) / mainUnitInMeters).replace('\n', ' '); //$NON-NLS-1$
		} else if (mc == MetricsConstants.MILES_AND_YARDS && meters > 0.249f * mainUnitInMeters) {
			return MessageFormat.format(format2 + ctx.getString(mainUnitStr), ((float) meters) / mainUnitInMeters).replace('\n', ' '); //$NON-NLS-1$
		} else if (mc == MetricsConstants.NAUTICAL_MILES && meters > 0.99f * mainUnitInMeters) {
			return MessageFormat.format(format2 + ctx.getString(mainUnitStr), ((float) meters) / mainUnitInMeters).replace('\n', ' '); //$NON-NLS-1$
		} else {
			if (mc == MetricsConstants.KILOMETERS_AND_METERS || mc == MetricsConstants.MILES_AND_METERS) {
				return ((int) (meters + 0.5)) + " " + ctx.getString(R.string.m); //$NON-NLS-1$
			} else if (mc == MetricsConstants.MILES_AND_FEET) {
				int feet = (int) (meters * FEET_IN_ONE_METER + 0.5);
				return feet + " " + ctx.getString(R.string.foot); //$NON-NLS-1$
			} else if (mc == MetricsConstants.MILES_AND_YARDS) {
				int yards = (int) (meters * YARDS_IN_ONE_METER + 0.5);
				return yards + " " + ctx.getString(R.string.yard); //$NON-NLS-1$
			}
			return ((int) (meters + 0.5)) + " " + ctx.getString(R.string.m); //$NON-NLS-1$
		}
	}

	public static String getFormattedAlt(double alt, OsmandApplication ctx) {
		OsmandSettings settings = ctx.getSettings();
		MetricsConstants mc = settings.METRIC_SYSTEM.get();
		boolean useFeet = (mc == MetricsConstants.MILES_AND_FEET) || (mc == MetricsConstants.MILES_AND_YARDS);
		if (!useFeet) {
			return ((int) (alt + 0.5)) + " " + ctx.getString(R.string.m);
		} else {
			return ((int) (alt * FEET_IN_ONE_METER + 0.5)) + " " + ctx.getString(R.string.foot);
		}
	}
	
	public static String getFormattedSpeed(float metersperseconds, OsmandApplication ctx) {
		OsmandSettings settings = ctx.getSettings();
		SpeedConstants mc = settings.SPEED_SYSTEM.get();
		ApplicationMode am = settings.getApplicationMode();
		float kmh = metersperseconds * 3.6f;
		if (mc == SpeedConstants.KILOMETERS_PER_HOUR) {
			// e.g. car case and for high-speeds: Display rounded to 1 km/h (5% precision at 20 km/h)
			if (kmh >= 20 || am.hasFastSpeed()) {
				return ((int) Math.round(kmh)) + " " + mc.toShortString(ctx);
			}
			// for smaller values display 1 decimal digit x.y km/h, (0.5% precision at 20 km/h)
			int kmh10 = (int) Math.round(kmh * 10f);
			return (kmh10 / 10f) + " " + mc.toShortString(ctx);
		} else if (mc == SpeedConstants.MILES_PER_HOUR) {
			float mph = kmh * METERS_IN_KILOMETER / METERS_IN_ONE_MILE;
			if (mph >= 20 || am.hasFastSpeed()) {
				return ((int) Math.round(mph)) + " " + mc.toShortString(ctx);
			} else {
				int mph10 = (int) Math.round(mph * 10f);
				return (mph10 / 10f) + " " + mc.toShortString(ctx);
			}
		} else if (mc == SpeedConstants.NAUTICALMILES_PER_HOUR) {
			float mph = kmh * METERS_IN_KILOMETER / METERS_IN_ONE_NAUTICALMILE;
			if (mph >= 20 || am.hasFastSpeed()) {
				return ((int) Math.round(mph)) + " " + mc.toShortString(ctx);
			} else {
				int mph10 = (int) Math.round(mph * 10f);
				return (mph10 / 10f) + " " + mc.toShortString(ctx);
			}
		} else if (mc == SpeedConstants.MINUTES_PER_KILOMETER) {
			if (metersperseconds < 0.111111111) {
				return "-" + mc.toShortString(ctx);
			}
			float minperkm = METERS_IN_KILOMETER / (metersperseconds * 60);
			if (minperkm >= 10) {
				return ((int) Math.round(minperkm)) + " " + mc.toShortString(ctx);
			} else {
				int mph10 = (int) Math.round(minperkm * 10f);
				return (mph10 / 10f) + " " + mc.toShortString(ctx);
			}
		} else if (mc == SpeedConstants.MINUTES_PER_MILE) {
			if (metersperseconds < 0.111111111) {
				return "-" + mc.toShortString(ctx);
			}
			float minperm = (METERS_IN_ONE_MILE) / (metersperseconds * 60);
			if (minperm >= 10) {
				return ((int) Math.round(minperm)) + " " + mc.toShortString(ctx);
			} else {
				int mph10 = (int) Math.round(minperm * 10f);
				return (mph10 / 10f) + " " + mc.toShortString(ctx);
			}
		} else /*if (mc == SpeedConstants.METERS_PER_SECOND) */ {
			if (metersperseconds >= 10) {
				return ((int) Math.round(metersperseconds)) + " " + SpeedConstants.METERS_PER_SECOND.toShortString(ctx);
			}
			// for smaller values display 1 decimal digit x.y km/h, (0.5% precision at 20 km/h)
			int kmh10 = (int) Math.round(metersperseconds * 10f);
			return (kmh10 / 10f) + " " + SpeedConstants.METERS_PER_SECOND.toShortString(ctx);
		}
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

	public static String getPoiStringWithoutType(Amenity amenity, String locale, boolean transliterate) {
		PoiCategory pc = amenity.getType();
		PoiType pt = pc.getPoiTypeByKeyName(amenity.getSubType());
		String nm = amenity.getSubType();
		if (pt != null) {
			nm = pt.getTranslation();
		} else if(nm != null){
			nm = Algorithms.capitalizeFirstLetterAndLowercase(nm.replace('_', ' '));
		}
		String n = amenity.getName(locale, transliterate);
		if (n.indexOf(nm) != -1) {
			// type is contained in name e.g.
			// n = "Bakery the Corner"
			// type = "Bakery"
			// no need to repeat this
			return n;
		}
		if (n.length() == 0) {
			return nm;
		}
		return nm + " " + n; //$NON-NLS-1$
	}

	public static String getAmenityDescriptionContent(OsmandApplication ctx, Amenity amenity, boolean shortDescription) {
		StringBuilder d = new StringBuilder();
		if(amenity.getType().isWiki()) {
			return "";
		}
		MapPoiTypes poiTypes = ctx.getPoiTypes();
		for(Entry<String, String>  e : amenity.getAdditionalInfo().entrySet()) {
			String key = e.getKey();
			String vl = e.getValue();
			if(key.startsWith("name:")) {
				continue;
			} else if(vl.length() >= 150) {
				if(shortDescription) {
					continue;
				}
			} else if(Amenity.OPENING_HOURS.equals(key)) {
				d.append(ctx.getString(R.string.opening_hours) + ": ");
			} else if(Amenity.PHONE.equals(key)) {
				d.append(ctx.getString(R.string.phone) + ": ");
			} else if(Amenity.WEBSITE.equals(key)) {
				d.append(ctx.getString(R.string.website) + ": ");
			} else {
				AbstractPoiType pt = poiTypes.getAnyPoiAdditionalTypeByKey(e.getKey());
				if (pt != null) {
					if(pt instanceof PoiType && !((PoiType) pt).isText()) {
						vl = pt.getTranslation();
					} else {
						vl = pt.getTranslation() + ": " + amenity.unzipContent(e.getValue());
					}
				} else {
					vl = Algorithms.capitalizeFirstLetterAndLowercase(e.getKey()) +
					 ": " + amenity.unzipContent(e.getValue());
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

	private static boolean isSameDay(Calendar cal1, Calendar cal2) {
		return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
				cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
				cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
	}
	
	public static String getFormattedCoordinates(double lat, double lon, int outputFormat) {
		StringBuilder result = new StringBuilder();
		if (outputFormat == FORMAT_DEGREES_SHORT) {
			result.append(formatCoordinate(lat, outputFormat)).append(" ").append(formatCoordinate(lon, outputFormat));
		} else if (outputFormat == FORMAT_DEGREES || outputFormat == FORMAT_MINUTES || outputFormat == FORMAT_SECONDS) {
			result
				.append(formatCoordinate(lat, outputFormat)).append(" ")
				.append(lat > 0 ? NORTH : SOUTH).append(", ")
				.append(formatCoordinate(lon, outputFormat)).append(" ")
				.append(lon > 0 ? EAST : WEST);
		}  else if (outputFormat == UTM_FORMAT) {
			UTMPoint pnt = new UTMPoint(new LatLonPoint(lat, lon));
			result
				.append(pnt.zone_number)
				.append(pnt.zone_letter).append(" ")
				.append((long) pnt.easting).append(" ")
				.append((long) pnt.northing);
		} else if (outputFormat == OLC_FORMAT) {
			String r;
			try {
				r = getLocationOlcName(lat, lon);
			} catch (RuntimeException e) {
				r = "0, 0";
			}
			result.append(r);
		}
		return result.toString();
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
}