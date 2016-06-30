package net.osmand.core.samples.android.sample1.search.objects;

import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QStringStringHash;
import net.osmand.core.jni.Street;

public class StreetSearchObject extends SearchPositionObject<Street> {

	public StreetSearchObject(Street street) {
		super(SearchObjectType.STREET, street);
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
