package net.osmand.core.samples.android.sample1.search.objects;

import net.osmand.core.jni.Amenity;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QStringStringHash;

public class PoiSearchObject extends SearchPositionObject {

	public PoiSearchObject(Amenity amenity) {
		super(SearchObjectType.POI, amenity);
	}

	public Amenity getAmenity() {
		return (Amenity) getInternalObject();
	}

	@Override
	public PointI getPosition31() {
		return getAmenity().getPosition31();
	}

	@Override
	public String getNativeName() {
		return getAmenity().getNativeName();
	}

	@Override
	protected QStringStringHash getLocalizedNames() {
		return getAmenity().getLocalizedNames();
	}
}
