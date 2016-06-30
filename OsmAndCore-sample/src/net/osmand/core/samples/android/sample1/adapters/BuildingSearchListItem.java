package net.osmand.core.samples.android.sample1.adapters;

import net.osmand.core.jni.Building;
import net.osmand.core.jni.Street;
import net.osmand.core.jni.StreetGroup;
import net.osmand.core.samples.android.sample1.MapUtils;
import net.osmand.core.samples.android.sample1.SampleApplication;
import net.osmand.core.samples.android.sample1.search.objects.BuildingSearchObject;

public class BuildingSearchListItem extends SearchListPositionItem {
	private String nameStr;
	private String typeStr;

	public BuildingSearchListItem(SampleApplication app, BuildingSearchObject buildingObject) {
		super(app, buildingObject);

		nameStr = buildingObject.getName(MapUtils.LANGUAGE);

		Building building = buildingObject.getBaseObject();

		Street street = building.getStreet();
		if (street != null) {
			StreetGroup streetGroup = street.getStreetGroup();
			if (streetGroup != null) {
				typeStr = streetGroup.getNativeName() + ", " + street.getNativeName();
			} else {
				typeStr = street.getNativeName();
			}
		} else {
			typeStr = "Building";
		}
	}

	@Override
	public String getName() {
		return nameStr;
	}

	@Override
	public String getTypeName() {
		return typeStr;
	}
}