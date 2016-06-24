package net.osmand.core.samples.android.sample1.search.objects;

import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QStringStringHash;
import net.osmand.core.jni.Street;

public class StreetSearchObject extends SearchPositionObject {

	public StreetSearchObject(Street street) {
		super(SearchObjectType.STREET, street);
	}

	public Street getStreet() {
		return (Street) getInternalObject();
	}

	@Override
	public PointI getPosition31() {
		return getStreet().getPosition31();
	}

	@Override
	public String getNativeName() {
		return getStreet().getNativeName();
	}

	@Override
	protected QStringStringHash getLocalizedNames() {
		return getStreet().getLocalizedNames();
	}
}
