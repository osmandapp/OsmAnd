package net.osmand.plus.myplaces.tracks;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.shared.gpx.TrackItem;
import net.osmand.shared.gpx.data.TracksGroup;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;

import java.util.ArrayList;
import java.util.List;

public class VisibleTracksGroup implements TracksGroup {

	private final OsmandApplication app;
	private final GpxSelectionHelper selectedGpxHelper;

	public VisibleTracksGroup(@NonNull OsmandApplication app) {
		this.app = app;
		this.selectedGpxHelper = app.getSelectedGpxHelper();
	}

	@NonNull
	@Override
	public List<TrackItem> getTrackItems() {
		List<TrackItem> trackItems = new ArrayList<>();
		for (SelectedGpxFile selectedGpxFile : selectedGpxHelper.getSelectedGPXFiles()) {
			TrackItem trackItem = new TrackItem(selectedGpxFile.getGpxFile());
			trackItems.add(trackItem);
		}
		return trackItems;
	}

	@NonNull
	@Override
	public String getName() {
		return app.getString(R.string.shared_string_visible_on_map);
	}

	@NonNull
	@Override
	public String toString() {
		return getName();
	}
}
