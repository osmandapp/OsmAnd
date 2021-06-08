package net.osmand.plus.settings.backend;

import net.osmand.data.LatLon;

import java.util.List;

class IntermediatePointsStorage extends SettingsMapPointsStorage {

	public IntermediatePointsStorage(OsmandSettings osmandSettings) {
		super(osmandSettings, false);
		pointsKey = OsmandSettings.INTERMEDIATE_POINTS;
		descriptionsKey = OsmandSettings.INTERMEDIATE_POINTS_DESCRIPTION;
	}

	@Override
	public boolean savePoints(List<LatLon> ps, List<String> ds) {
		boolean res = super.savePoints(ps, ds);
		getOsmandSettings().backupTargetPoints();
		return res;
	}
}