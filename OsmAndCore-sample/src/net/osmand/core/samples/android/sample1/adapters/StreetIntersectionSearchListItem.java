package net.osmand.core.samples.android.sample1.adapters;

import net.osmand.core.jni.Street;
import net.osmand.core.jni.StreetGroup;
import net.osmand.core.jni.StreetIntersection;
import net.osmand.core.samples.android.sample1.MapUtils;
import net.osmand.core.samples.android.sample1.SampleApplication;
import net.osmand.core.samples.android.sample1.search.objects.StreetIntersectionSearchObject;

public class StreetIntersectionSearchListItem extends SearchListPositionItem {
	private String nameStr;
	private String typeStr;

	public StreetIntersectionSearchListItem(SampleApplication app, StreetIntersectionSearchObject intersectionObject) {
		super(app, intersectionObject);

		nameStr = intersectionObject.getName(MapUtils.LANGUAGE);

		StreetIntersection streetIntersection = intersectionObject.getBaseObject();

		Street street = streetIntersection.getStreet();
		if (street != null) {
			StreetGroup streetGroup = street.getStreetGroup();
			if (streetGroup != null) {
				typeStr = streetGroup.getNativeName() + ", " + street.getNativeName();
			} else {
				typeStr = street.getNativeName();
			}
		} else {
			typeStr = "Street intersection";
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