package net.osmand.core.samples.android.sample1.search.objects;

import net.osmand.core.jni.Amenity;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QStringStringHash;

public class PoiSearchObject extends SearchPositionObject<Amenity> {

	public PoiSearchObject(Amenity amenity) {
		super(SearchObjectType.POI, amenity);
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
