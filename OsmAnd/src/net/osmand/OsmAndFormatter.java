package net.osmand;

import java.text.MessageFormat;

import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.OsmandSettings.MetricsConstants;
import android.content.Context;

public class OsmAndFormatter {
	private final static float METERS_IN_KILOMETER = 1000f;
	private final static float METERS_IN_ONE_MILE = 1609.344f; // 1609.344
	private final static float YARDS_IN_ONE_METER = 1.0936f;
	private final static float FOOTS_IN_ONE_METER = YARDS_IN_ONE_METER * 3f;
	
	public static double calculateRoundedDist(double distInMeters, Context ctx) {
		OsmandSettings settings = OsmandSettings.getOsmandSettings(ctx);
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
	
	public static String getFormattedDistance(float meters, Context ctx) {
		OsmandSettings settings = OsmandSettings.getOsmandSettings(ctx);
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
			return (int) (meters / mainUnitInMeters) + " " + ctx.getString(mainUnitStr); //$NON-NLS-1$
		} else if (meters > 9.99f * mainUnitInMeters) {
			return MessageFormat.format("{0,number,#.#} " + ctx.getString(mainUnitStr), ((float) meters) / mainUnitInMeters); //$NON-NLS-1$ 
		} else if (meters > 0.999f * mainUnitInMeters) {
			return MessageFormat.format("{0,number,#.##} " + ctx.getString(mainUnitStr), ((float) meters) / mainUnitInMeters); //$NON-NLS-1$
		} else {
			if (mc == MetricsConstants.KILOMETERS_AND_METERS) {
				return ((int) meters) + " " + ctx.getString(R.string.m); //$NON-NLS-1$
			} else if (mc == MetricsConstants.MILES_AND_YARDS) {
				int yards = (int) (meters * YARDS_IN_ONE_METER);
				return yards + " " + ctx.getString(R.string.yard); //$NON-NLS-1$
			} else if (mc == MetricsConstants.MILES_AND_FOOTS) {
				int foots = (int) (meters * FOOTS_IN_ONE_METER);
				return foots + " " + ctx.getString(R.string.foot); //$NON-NLS-1$
			}
			return ((int) meters) + " " + ctx.getString(R.string.m); //$NON-NLS-1$
		}
	}
	
	public static String getFormattedSpeed(float metersperseconds, Context ctx) {
		OsmandSettings settings = OsmandSettings.getOsmandSettings(ctx);
		MetricsConstants mc = settings.METRIC_SYSTEM.get();
		float kmh = metersperseconds * 3.6f;
		if(mc == MetricsConstants.KILOMETERS_AND_METERS){
			return ((int) kmh) + ctx.getString(R.string.km_h);
		} else {
			return ((int) (kmh * METERS_IN_KILOMETER / METERS_IN_ONE_MILE)) + ctx.getString(R.string.mile_per_hour);
		}
	}
	
	public static String toPublicString(AmenityType t, Context ctx) {
		switch (t) {
		case SUSTENANCE:
			return ctx.getString(R.string.amenity_type_sustenance);
		case EDUCATION:
			return ctx.getString(R.string.amenity_type_education);
		case TRANSPORTATION:
			return ctx.getString(R.string.amenity_type_transportation);
		case FINANCE:
			return ctx.getString(R.string.amenity_type_finance);
		case HEALTHCARE:
			return ctx.getString(R.string.amenity_type_healthcare);
		case ENTERTAINMENT:
			return ctx.getString(R.string.amenity_type_entertainment);
		case TOURISM:
			return ctx.getString(R.string.amenity_type_tourism);
		case HISTORIC:
			return ctx.getString(R.string.amenity_type_historic);
		case NATURAL:
			return ctx.getString(R.string.amenity_type_natural);
		case SHOP:
			return ctx.getString(R.string.amenity_type_shop);
		case LEISURE:
			return ctx.getString(R.string.amenity_type_leisure);
		case SPORT:
			return ctx.getString(R.string.amenity_type_sport);
		case BARRIER:
			return ctx.getString(R.string.amenity_type_barrier);
		case LANDUSE:
			return ctx.getString(R.string.amenity_type_landuse);
		case MAN_MADE:
			return ctx.getString(R.string.amenity_type_manmade);
		case OFFICE:
			return ctx.getString(R.string.amenity_type_office);
		case EMERGENCY:
			return ctx.getString(R.string.amenity_type_emergency);
		case MILITARY:
			return ctx.getString(R.string.amenity_type_military);
		case ADMINISTRATIVE:
			return ctx.getString(R.string.amenity_type_administrative);
		case GEOCACHE:
			return ctx.getString(R.string.amenity_type_geocache);
		case OTHER:
			return ctx.getString(R.string.amenity_type_other);
		}
		return "";
	}

	
	public static String getPoiSimpleFormat(Amenity amenity, Context ctx, boolean en){
		return toPublicString(amenity.getType(), ctx) + " : " + getPoiStringWithoutType(amenity, en); //$NON-NLS-1$
	}
	
	public static String getPoiStringWithoutType(Amenity amenity, boolean en){
		String n = amenity.getName(en);
		if(n.length() == 0){
			return amenity.getSubType();
		}
		return amenity.getSubType() + " " + n; //$NON-NLS-1$
	}
}
