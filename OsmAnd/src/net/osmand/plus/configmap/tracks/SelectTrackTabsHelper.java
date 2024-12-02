package net.osmand.plus.configmap.tracks;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.shared.gpx.data.TrackFolder;

public class SelectTrackTabsHelper extends TrackTabsHelper {

	public SelectTrackTabsHelper(@NonNull OsmandApplication app) {
		super(app);
	}

	@Override
	protected void updateTrackTabs(@NonNull TrackFolder rootFolder) {
		trackTabs.clear();
		trackTabs.put(TrackTabType.ON_MAP.name(), getTracksOnMapTab());
		trackTabs.put(TrackTabType.ALL.name(), getAllTracksTab());
		trackTabs.put(TrackTabType.FOLDERS.name(), getFoldersTab(rootFolder));
	}
}
