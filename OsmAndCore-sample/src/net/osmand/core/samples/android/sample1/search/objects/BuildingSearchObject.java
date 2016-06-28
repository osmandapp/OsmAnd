package net.osmand.core.samples.android.sample1.search.objects;

import net.osmand.core.jni.Building;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QStringStringHash;

public class BuildingSearchObject extends SearchPositionObject {

	public BuildingSearchObject(Building building) {
		super(SearchObjectType.BUILDING, building);
	}

	public Building getBuilding() {
		return (Building) getInternalObject();
	}

	@Override
	public PointI getPosition31() {
		return getBuilding().getPosition31();
	}

	@Override
	public String getNativeName() {
		return getBuilding().getNativeName();
	}

	@Override
	protected QStringStringHash getLocalizedNames() {
		return getBuilding().getLocalizedNames();
	}
}
