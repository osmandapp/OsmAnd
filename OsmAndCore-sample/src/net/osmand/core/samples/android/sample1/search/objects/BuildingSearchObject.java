package net.osmand.core.samples.android.sample1.search.objects;

import net.osmand.core.jni.Building;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QStringStringHash;

public class BuildingSearchObject extends SearchPositionObject<Building> {

	public BuildingSearchObject(Building building) {
		super(SearchObjectType.BUILDING, building);
	}

	@Override
	public PointI getPosition31() {
		return getBaseObject().getPosition31();
	}

	@Override
	public String getNativeName() {
		return getBaseObject().getNativeName();
	}

	@Override
	protected QStringStringHash getLocalizedNames() {
		return getBaseObject().getLocalizedNames();
	}
}
