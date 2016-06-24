package net.osmand.core.samples.android.sample1.search.objects;

import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QStringStringHash;
import net.osmand.core.jni.StreetGroup;

public class PostcodeSearchObject extends StreetGroupSearchObject {

	public PostcodeSearchObject(StreetGroup streetGroup) {
		super(SearchObjectType.POSTCODE, streetGroup);
	}
}
