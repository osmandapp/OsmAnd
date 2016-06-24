package net.osmand.core.samples.android.sample1.search.objects;

import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QStringStringHash;
import net.osmand.core.jni.StreetGroup;

public abstract class StreetGroupSearchObject extends SearchPositionObject {

	public StreetGroupSearchObject(SearchObjectType type, StreetGroup streetGroup) {
		super(type, streetGroup);
	}

	public StreetGroup getStreetGroup() {
		return (StreetGroup) getInternalObject();
	}

	@Override
	public PointI getPosition31() {
		return getStreetGroup().getPosition31();
	}

	@Override
	public String getNativeName() {
		return getStreetGroup().getNativeName();
	}

	@Override
	protected QStringStringHash getLocalizedNames() {
		return getStreetGroup().getLocalizedNames();
	}
}
