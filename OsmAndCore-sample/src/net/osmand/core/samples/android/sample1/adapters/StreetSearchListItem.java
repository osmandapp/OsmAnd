package net.osmand.core.samples.android.sample1.adapters;

import net.osmand.core.jni.ObfAddressStreetGroupSubtype;
import net.osmand.core.jni.StreetGroup;
import net.osmand.core.samples.android.sample1.MapUtils;
import net.osmand.core.samples.android.sample1.SampleApplication;
import net.osmand.core.samples.android.sample1.search.objects.StreetSearchObject;

public class StreetSearchListItem extends SearchListPositionItem {
	private String nameStr;
	private String typeStr;

	public StreetSearchListItem(SampleApplication app, StreetSearchObject searchItem) {
		super(app, searchItem);

		nameStr = searchItem.getName(MapUtils.LANGUAGE);

		StreetGroup streetGroup = searchItem.getStreet().getStreetGroup();
		if (streetGroup != null) {
			typeStr = streetGroup.getNativeName() + " — " + getTypeStr(streetGroup);
		} else {
			typeStr = "Street";
		}
	}

	private String getTypeStr(StreetGroup streetGroup) {
		String typeStr;
		if (streetGroup.getSubtype() != ObfAddressStreetGroupSubtype.Unknown) {
			typeStr = streetGroup.getSubtype().name();
		} else {
			typeStr = streetGroup.getType().name();
		}
		return typeStr;
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
