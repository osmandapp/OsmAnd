package net.osmand.core.samples.android.sample1.adapters;

import android.graphics.drawable.Drawable;

import net.osmand.core.jni.Amenity.DecodedCategory;
import net.osmand.core.jni.Amenity.DecodedValue;
import net.osmand.core.jni.DecodedCategoryList;
import net.osmand.core.jni.DecodedValueList;
import net.osmand.core.jni.QStringList;
import net.osmand.core.jni.QStringStringHash;
import net.osmand.core.samples.android.sample1.MapUtils;
import net.osmand.core.samples.android.sample1.SampleApplication;
import net.osmand.core.samples.android.sample1.search.objects.PoiSearchObject;
import net.osmand.data.Amenity;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.util.Algorithms;

import java.util.HashMap;
import java.util.Map;

public class PoiSearchListItem extends SearchListPositionItem {

	private Amenity amenity;
	private String nameStr;
	private String typeStr;

	public PoiSearchListItem(SampleApplication app, PoiSearchObject poiObject) {
		super(app, poiObject);
		amenity = parseAmenity(poiObject);
		nameStr = amenity.getName(MapUtils.LANGUAGE);
		typeStr = getTypeStr();
	}

	private Amenity parseAmenity(PoiSearchObject poiObject) {

		String categoryName = "";
		String subcategoryName = "";
		Map<String, String> values = new HashMap<>();

		net.osmand.core.jni.Amenity coreAmenity = poiObject.getBaseObject();
		DecodedCategoryList catList = coreAmenity.getDecodedCategories();
		if (catList.size() > 0) {
			DecodedCategory decodedCategory = catList.get(0);
			categoryName = decodedCategory.getCategory();
			subcategoryName = decodedCategory.getSubcategory();
		}

		DecodedValueList decodedValueList = coreAmenity.getDecodedValues();
		if (decodedValueList.size() > 0) {
			for (int i = 0; i < decodedValueList.size(); i++) {
				DecodedValue decodedValue = decodedValueList.get(i);
				String tag = decodedValue.getDeclaration().getTagName();
				String value = decodedValue.getValue();
				values.put(tag, value);
			}
		}

		MapPoiTypes poiTypes = app.getPoiTypes();

		Amenity a = new Amenity();
		PoiCategory category = poiTypes.getPoiCategoryByName(categoryName);
		a.setType(category);
		a.setSubType(subcategoryName);
		a.setName(poiObject.getNativeName());

		QStringStringHash localizedNamesMap = coreAmenity.getLocalizedNames();
		QStringList locNamesKeys = localizedNamesMap.keys();
		for (int i = 0; i < locNamesKeys.size(); i++) {
			String key = locNamesKeys.get(i);
			String val = localizedNamesMap.get(key);
			a.setName(key, val);
		}

		a.setAdditionalInfo(values);

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
	public String getTypeName() {
		return typeStr;
	}

	private String getTypeStr() {
		/*
		PoiCategory pc = amenity.getTypeName();
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
			drawable = app.getIconsCache().getMapIcon(st.getOsmTag() + "_" + st.getOsmValue());
		}
		return drawable;
	}
}
