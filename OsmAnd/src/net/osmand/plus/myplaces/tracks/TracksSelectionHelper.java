package net.osmand.plus.myplaces.tracks;

import static net.osmand.plus.track.helpers.GpxSelectionHelper.CURRENT_TRACK;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.configmap.tracks.viewholders.TrackViewHolder.TrackSelectionListener;
import net.osmand.plus.myplaces.tracks.dialogs.viewholders.TrackFolderViewHolder.FolderSelectionListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.helpers.GpxSelectionHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TracksSelectionHelper implements TrackSelectionListener, FolderSelectionListener {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final GpxSelectionHelper selectionHelper;

	private final Set<TrackItem> allTrackItems = new HashSet<>();
	private final Set<TrackItem> selectedTrackItems = new HashSet<>();

	public TracksSelectionHelper(@NonNull OsmandApplication app, @NonNull List<TrackItem> items) {
		this.app = app;
		this.settings = app.getSettings();
		this.selectionHelper = app.getSelectedGpxHelper();
		allTrackItems.addAll(items);
	}

	public boolean isAllItemsSelected() {
		return selectedTrackItems.containsAll(allTrackItems);
	}

	@NonNull
	public List<TrackItem> getSelectedTrackItems() {
		return new ArrayList<>(selectedTrackItems);
	}

	public int getSelectedItemsCount() {
		return selectedTrackItems.size();
	}

	public void clearSelectedItems() {
		selectedTrackItems.clear();
	}

	public void selectAllItems() {
		selectedTrackItems.addAll(allTrackItems);
	}

	public boolean isFolderSelected(@NonNull TrackFolder folder) {
		return selectedTrackItems.containsAll(folder.getFlattenedTrackItems());
	}

	@Override
	public boolean isTrackItemSelected(@NonNull TrackItem trackItem) {
		return selectedTrackItems.contains(trackItem);
	}

	@Override
	public void onTrackItemsSelected(@NonNull Set<TrackItem> trackItems, boolean selected) {
		if (selected) {
			selectedTrackItems.addAll(trackItems);
		} else {
			selectedTrackItems.removeAll(trackItems);
		}
	}

	@Override
	public void onFolderSelected(@NonNull TrackFolder folder) {
		List<TrackItem> items = folder.getFlattenedTrackItems();
		if (isFolderSelected(folder)) {
			items.forEach(selectedTrackItems::remove);
		} else {
			selectedTrackItems.addAll(items);
		}
	}

	public void saveTracksVisibility() {
		selectionHelper.clearAllGpxFilesToShow(true);

		Map<String, Boolean> selectedFileNames = new HashMap<>();
		for (TrackItem trackItem : selectedTrackItems) {
			String path = trackItem.isShowCurrentTrack() ? CURRENT_TRACK : trackItem.getPath();
			selectedFileNames.put(path, true);
		}
		selectionHelper.runSelection(selectedFileNames, null);
	}
}
