package net.osmand.plus;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.Map.Entry;

import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.data.City.CityType;
import net.osmand.plus.OsmandSettings.MetricsConstants;
import android.content.Context;

public class OsmAndFormatter {
	private final static float METERS_IN_KILOMETER = 1000f;
	private final static float METERS_IN_ONE_MILE = 1609.344f; // 1609.344
	private final static float YARDS_IN_ONE_METER = 1.0936f;
	private final static float FOOTS_IN_ONE_METER = YARDS_IN_ONE_METER * 3f;
	
	public static double calculateRoundedDist(double distInMeters, OsmandApplication ctx) {
		OsmandSettings settings = ctx.getSettings();
		MetricsConstants mc = settings.METRIC_SYSTEM.get();
		double mainUnitInMeter = 1;
		double metersInSecondUnit = METERS_IN_KILOMETER; 
		if (mc == MetricsConstants.MILES_AND_FOOTS) {
			mainUnitInMeter = FOOTS_IN_ONE_METER;
			metersInSecondUnit = METERS_IN_ONE_MILE;
		} else if(mc == MetricsConstants.MILES_AND_YARDS){
			mainUnitInMeter = YARDS_IN_ONE_METER;
			metersInSecondUnit = METERS_IN_ONE_MILE ;
		}
		// 1, 2, 5, 10, 20, 50, 100, 200, 500, 1000 ...
		
		int generator = 1;
		byte pointer = 1;
		double point = mainUnitInMeter;
		while(distInMeters * point > generator){
			if (pointer++ % 3 == 2) {
				generator = generator * 5 / 2;
			} else {
				generator *= 2;
			}
			if(point == mainUnitInMeter && metersInSecondUnit * mainUnitInMeter * 0.9f <= generator ){
				point = 1 / metersInSecondUnit;
				generator = 1;
				pointer = 1;
			}
		}
		
		return (generator / point);
	}
	
	public static String getFormattedDistance(float meters, OsmandApplication ctx) {
		OsmandSettings settings = ctx.getSettings();
		MetricsConstants mc = settings.METRIC_SYSTEM.get();
		int mainUnitStr;
		float mainUnitInMeters;
		if (mc == MetricsConstants.KILOMETERS_AND_METERS) {
			mainUnitStr = R.string.km;
			mainUnitInMeters = METERS_IN_KILOMETER;
		} else {
			mainUnitStr = R.string.mile;
			mainUnitInMeters = METERS_IN_ONE_MILE;
		}

		if (meters >= 100 * mainUnitInMeters) {
			return (int) (meters / mainUnitInMeters + 0.5) + " " + ctx.getString(mainUnitStr); //$NON-NLS-1$
		} else if (meters > 9.99f * mainUnitInMeters) {
			return MessageFormat.format("{0,number,#.#} " + ctx.getString(mainUnitStr), ((float) meters) / mainUnitInMeters); //$NON-NLS-1$
		} else if (meters > 0.999f * mainUnitInMeters) {
			return MessageFormat.format("{0,number,#.##} " + ctx.getString(mainUnitStr), ((float) meters) / mainUnitInMeters); //$NON-NLS-1$
		} else {
			if (mc == MetricsConstants.KILOMETERS_AND_METERS) {
				return ((int) (meters + 0.5)) + " " + ctx.getString(R.string.m); //$NON-NLS-1$
			} else if (mc == MetricsConstants.MILES_AND_FOOTS) {
				int foots = (int) (meters * FOOTS_IN_ONE_METER + 0.5);
				return foots + " " + ctx.getString(R.string.foot); //$NON-NLS-1$
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
		if (mc == MetricsConstants.KILOMETERS_AND_METERS) {
			return ((int) (alt + 0.5)) + " " + ctx.getString(R.string.m);
		} else {
			return ((int) (alt * FOOTS_IN_ONE_METER + 0.5)) + " " + ctx.getString(R.string.foot);
		}
	}
	
	public static String getFormattedSpeed(float metersperseconds, OsmandApplication ctx) {
		OsmandSettings settings = ctx.getSettings();
		MetricsConstants mc = settings.METRIC_SYSTEM.get();
		ApplicationMode am = settings.getApplicationMode();
		float kmh = metersperseconds * 3.6f;
		if (mc == MetricsConstants.KILOMETERS_AND_METERS) {
			if (kmh >= 10 || am.hasFastSpeed()) {
				// case of car
				return ((int) Math.round(kmh)) + " " + ctx.getString(R.string.km_h);
			}
			int kmh10 = (int) (kmh * 10f);
			// calculate 2.0 km/h instead of 2 km/h in order to not stress UI text lengh
			return (kmh10 / 10f) + " " + ctx.getString(R.string.km_h);
		} else {
			float mph = kmh * METERS_IN_KILOMETER / METERS_IN_ONE_MILE;
			if (mph >= 10) {
				return ((int) Math.round(mph)) + " " + ctx.getString(R.string.mile_per_hour);
			} else {
				int mph10 = (int) (mph * 10f);
				return (mph10 / 10f) + " " + ctx.getString(R.string.mile_per_hour);
			}
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

	public static String toPublicString(AmenityType t, Context ctx) {
		Class<?> cl = R.string.class;
		try {
			Field fld = cl.getField("amenity_type_"+t.getCategoryName());
			if(fld != null) {
				return ctx.getString((Integer)fld.get(null));
			}
		} catch (Exception e) {
		}
		return ctx.getString(R.string.amenity_type_user_defined);
	}

	
	public static String getPoiSimpleFormat(Amenity amenity, Context ctx, boolean en){
		return toPublicString(amenity.getType(), ctx) + " : " + getPoiStringWithoutType(amenity, en); //$NON-NLS-1$
	}
	
	public static String getPoiStringWithoutType(Amenity amenity, boolean en) {
		String type = SpecialPhrases.getSpecialPhrase(amenity.getSubType());
		String n = amenity.getName(en);
		if (n.indexOf(type) != -1) {
			// type is contained in name e.g.
			// n = "Bakery the Corner"
			// type = "Bakery"
			// no need to repeat this
			return n;
		}
		if (n.length() == 0) {
			return type;
		}
		return type + " " + n; //$NON-NLS-1$
	}

	public static String getAmenityDescriptionContent(Context ctx, Amenity amenity, boolean shortDescription) {
		StringBuilder d = new StringBuilder();
		for(Entry<String, String>  e : amenity.getAdditionalInfo().entrySet()) {
			String key = e.getKey();
			if(Amenity.DESCRIPTION.equals(key)) {
				if(amenity.getType() == AmenityType.OSMWIKI && shortDescription) {
					continue;
				}
			} else if(Amenity.OPENING_HOURS.equals(key)) {
				d.append(ctx.getString(R.string.opening_hours) + " : ");
			} else if(Amenity.PHONE.equals(key)) {
				d.append(ctx.getString(R.string.phone) + " : ");
			} else if(Amenity.WEBSITE.equals(key)) {
				if(amenity.getType() == AmenityType.OSMWIKI) {
					continue;
				}
				d.append(ctx.getString(R.string.website) + " : ");
			} else {
				d.append(e.getKey() + " : ");
			}
			d.append(e.getValue()).append('\n');
		}
		return d.toString().trim();
	}
}
