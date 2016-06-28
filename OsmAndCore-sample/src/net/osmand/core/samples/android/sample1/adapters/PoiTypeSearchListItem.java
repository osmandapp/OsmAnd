package net.osmand.core.samples.android.sample1.adapters;

import android.graphics.drawable.Drawable;

import net.osmand.core.samples.android.sample1.SampleApplication;
import net.osmand.core.samples.android.sample1.search.objects.PoiTypeSearchObject;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiFilter;
import net.osmand.osm.PoiType;

public class PoiTypeSearchListItem extends SearchListItem {

	private AbstractPoiType poiType;
	private String typeName;

	public PoiTypeSearchListItem(SampleApplication app, PoiTypeSearchObject poiTypeObject) {
		super(app, poiTypeObject);

		poiType = app.getPoiTypesHelper().fetchPoiType(poiTypeObject);
		if (poiType != null) {
			if (poiType instanceof PoiCategory) {
				typeName = "Category";
			} else if (poiType instanceof PoiFilter) {
				PoiFilter filter = (PoiFilter) poiType;
				if (filter.getPoiCategory() != null) {
					typeName = ((PoiFilter) poiType).getPoiCategory().getTranslation();
				} else {
					typeName = "Filter";
				}
			} else if (poiType instanceof PoiType) {
				PoiType type = (PoiType) poiType;
				if (type.getCategory() != null) {
					typeName = type.getCategory().getTranslation();
				} else if (type.getParentType() != null) {
					typeName = type.getTranslation();
				}
			} else {
				typeName = "Poi type";
			}
		} else {
			typeName = poiTypeObject.getObjectType().name();
		}
	}

	@Override
	public String getName() {
		return poiType != null ? poiType.getTranslation() : "Unresolved";
	}

	@Override
	public String getTypeName() {
		return typeName;
	}

	@Override
	public Drawable getIcon() {
		return null;
	}
}
