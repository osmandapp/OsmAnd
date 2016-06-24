package net.osmand.core.samples.android.sample1.search.objects;

import net.osmand.core.jni.StreetGroup;

public class CitySearchObject extends StreetGroupSearchObject {

	public CitySearchObject(StreetGroup streetGroup) {
		super(SearchObjectType.CITY, streetGroup);
	}
}
