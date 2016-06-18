package net.osmand.core.samples.android.sample1.search;

import net.osmand.core.jni.Amenity;
import net.osmand.core.jni.DecodedCategoryList;
import net.osmand.core.jni.DecodedValueList;
import net.osmand.core.jni.QStringList;
import net.osmand.core.jni.QStringStringHash;
import net.osmand.util.Algorithms;

import java.util.HashMap;
import java.util.Map;

public class AmenitySearchItem extends SearchItem {

	private String category;
	private String subcategory;
	private Map<String, String> values = new HashMap<>();

	public AmenitySearchItem(Amenity amenity) {
		super(amenity.getPosition31());

		setNativeName(amenity.getNativeName());
		addLocalizedNames(amenity.getLocalizedNames());

		DecodedCategoryList catList = amenity.getDecodedCategories();
		if (catList.size() > 0) {
			Amenity.DecodedCategory decodedCategory = catList.get(0);
			category = decodedCategory.getCategory();
			subcategory = decodedCategory.getSubcategory();
		}

		DecodedValueList decodedValueList = amenity.getDecodedValues();
		if (decodedValueList.size() > 0) {
			for (int i = 0; i < decodedValueList.size(); i++) {
				Amenity.DecodedValue decodedValue = decodedValueList.get(i);
				String tag = decodedValue.getDeclaration().getTagName();
				String value = decodedValue.getValue().toString();
				values.put(tag, value);
			}
		}
	}

	public String getCategory() {
		return category;
	}

	public String getSubcategory() {
		return subcategory;
	}

	public Map<String, String> getValues() {
		return values;
	}

	@Override
	public String getType() {
		return Algorithms.capitalizeFirstLetterAndLowercase(subcategory);
	}
}
