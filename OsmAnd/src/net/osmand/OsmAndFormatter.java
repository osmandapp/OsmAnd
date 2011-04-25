package net.osmand;

import java.text.MessageFormat;

import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.plus.R;

import android.content.Context;

public class OsmAndFormatter {
	
	public static String getFormattedDistance(int meters, Context ctx) {
		if (meters >= 100000) {
			return meters / 1000 + " " + ctx.getString(R.string.km); //$NON-NLS-1$
		} else if (meters >= 10000) {
			return MessageFormat.format("{0,number,#.#} " + ctx.getString(R.string.km), ((float) meters) / 1000); //$NON-NLS-1$ 
		} else if (meters > 1500) {
			return MessageFormat.format("{0,number,#.#} " + ctx.getString(R.string.km), ((float) meters) / 1000); //$NON-NLS-1$ 
		} else if (meters > 900) {
			return MessageFormat.format("{0,number,#.##} " + ctx.getString(R.string.km), ((float) meters) / 1000); //$NON-NLS-1$
		} else {
			return meters + " " + ctx.getString(R.string.m); //$NON-NLS-1$ 
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
