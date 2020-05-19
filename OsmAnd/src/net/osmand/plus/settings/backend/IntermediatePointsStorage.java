package net.osmand.plus.settings.backend;

import net.osmand.data.LatLon;

import java.util.List;

class IntermediatePointsStorage extends SettingsMapPointsStorage {

	private OsmandSettings osmandSettings;

	public IntermediatePointsStorage(OsmandSettings osmandSettings) {
		this.osmandSettings = osmandSettings;
		pointsKey = OsmandSettings.INTERMEDIATE_POINTS;
		descriptionsKey = OsmandSettings.INTERMEDIATE_POINTS_DESCRIPTION;
	}

	@Override
	public boolean savePoints(List<LatLon> ps, List<String> ds) {
		boolean res = super.savePoints(ps, ds);
		osmandSettings.backupTargetPoints();
		return res;
	}
}
