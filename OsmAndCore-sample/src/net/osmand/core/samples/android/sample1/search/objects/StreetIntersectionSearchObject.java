package net.osmand.core.samples.android.sample1.search.objects;

import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QStringStringHash;
import net.osmand.core.jni.StreetIntersection;

public class StreetIntersectionSearchObject extends SearchPositionObject {

	public StreetIntersectionSearchObject(StreetIntersection streetIntersection) {
		super(SearchObjectType.STREET_INTERSECTION, streetIntersection);
	}

	public StreetIntersection getStreetIntersection() {
		return (StreetIntersection) getInternalObject();
	}

	@Override
	public PointI getPosition31() {
		return getStreetIntersection().getPosition31();
	}

	@Override
	public String getNativeName() {
		return getStreetIntersection().getNativeName();
	}

	@Override
	protected QStringStringHash getLocalizedNames() {
		return getStreetIntersection().getLocalizedNames();
	}
}