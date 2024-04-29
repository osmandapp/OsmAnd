package net.osmand;

import net.osmand.map.OsmandRegions;

import java.io.IOException;

public class PlatformResources {

	private static OsmandRegions osmandRegions;

	public static OsmandRegions getOsmandRegions() {
		if (osmandRegions == null) {
			osmandRegions = new OsmandRegions();
		}
		return osmandRegions;
	}

	public static OsmandRegions getInitializedOsmandRegions() throws IOException {
		OsmandRegions osmandRegions = getOsmandRegions();
		if (!osmandRegions.isInitialized()) {
			osmandRegions.prepareFile();
		}
		return osmandRegions;
	}

}
