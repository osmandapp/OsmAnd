package net.osmand.plus.settings.backend.storages;

import androidx.annotation.NonNull;

import net.osmand.data.LatLon;
import net.osmand.plus.settings.backend.OsmandSettings;

import java.util.List;

public class IntermediatePointsStorage extends SettingsMapPointsStorage {

	public static final String INTERMEDIATE_POINTS = "intermediate_points";
	public static final String INTERMEDIATE_POINTS_DESCRIPTION = "intermediate_points_description";

	public IntermediatePointsStorage(@NonNull OsmandSettings settings) {
		super(settings);
	}

	@NonNull
	@Override
	protected String getPointsKey() {
		return INTERMEDIATE_POINTS;
	}

	@NonNull
	@Override
	protected String getDescriptionsKey() {
		return INTERMEDIATE_POINTS_DESCRIPTION;
	}

	@Override
	public boolean savePoints(@NonNull List<LatLon> points, @NonNull List<String> descriptions) {
		boolean res = super.savePoints(points, descriptions);
		getSettings().backupTargetPoints();
		return res;
	}
}