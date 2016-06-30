package net.osmand.core.samples.android.sample1.search.objects;

import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QStringStringHash;
import net.osmand.core.jni.StreetIntersection;

public class StreetIntersectionSearchObject extends SearchPositionObject<StreetIntersection> {

	public StreetIntersectionSearchObject(StreetIntersection streetIntersection) {
		super(SearchObjectType.STREET_INTERSECTION, streetIntersection);
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