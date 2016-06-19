package net.osmand.core.samples.android.sample1.adapters;

import android.graphics.drawable.Drawable;

import net.osmand.core.jni.OsmAndCore;
import net.osmand.core.samples.android.sample1.MapUtils;
import net.osmand.core.samples.android.sample1.SampleApplication;
import net.osmand.core.samples.android.sample1.search.AmenitySearchItem;
import net.osmand.data.Amenity;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.util.Algorithms;

import java.util.Map.Entry;

public class AmenitySearchListItem extends SearchListItem {

	private Amenity amenity;
	private String nameStr;
	private String typeStr;

	public AmenitySearchListItem(SampleApplication app, AmenitySearchItem searchItem) {
		super(app, searchItem);
		amenity = parseAmenity(searchItem);
		nameStr = amenity.getName(MapUtils.LANGUAGE);
		typeStr = getTypeStr();
	}

	private Amenity parseAmenity(AmenitySearchItem searchItem) {

		MapPoiTypes poiTypes = app.getPoiTypes();

		Amenity a = new Amenity();
		PoiCategory category = poiTypes.getPoiCategoryByName(searchItem.getCategory());
		a.setType(category);
		a.setSubType(searchItem.getSubcategory());
		a.setName(searchItem.getNativeName());
		for (Entry<String, String> entry : searchItem.getLocalizedNames().entrySet()) {
			a.setName(entry.getKey(), entry.getValue());
		}
		a.setAdditionalInfo(searchItem.getValues());

		return a;
	}

	public Amenity getAmenity() {
		return amenity;
	}

	@Override
	public String getName() {
		return nameStr;
	}

	@Override
	public String getType() {
		return typeStr;
	}

	private String getTypeStr() {
		/*
		PoiCategory pc = amenity.getType();
		PoiType pt = pc.getPoiTypeByKeyName(amenity.getSubType());
		String typeStr = amenity.getSubType();
		if (pt != null) {
			typeStr = pt.getTranslation();
		} else if (typeStr != null) {
			typeStr = Algorithms.capitalizeFirstLetterAndLowercase(typeStr.replace('_', ' '));
		}
		return typeStr;
		*/
		return Algorithms.capitalizeFirstLetterAndLowercase(amenity.getSubType().replace('_', ' '));
	}

	@Override
	public Drawable getIcon() {
		Drawable drawable = null;
		PoiType st = amenity.getType().getPoiTypeByKeyName(amenity.getSubType());
		if (st != null) {
			drawable = app.getIconsCache().getIcon("mx_" + st.getIconKeyName());
			if (drawable == null) {
				drawable = app.getIconsCache().getIcon("mx_" + st.getOsmTag() + "_" + st.getOsmValue());
			}
		}
		return drawable;
	}
}
