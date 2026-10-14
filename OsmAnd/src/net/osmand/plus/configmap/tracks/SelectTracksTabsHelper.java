package net.osmand.plus.configmap.tracks;

import static net.osmand.plus.track.helpers.GpxSelectionHelper.CURRENT_TRACK;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.shared.gpx.TrackItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SelectTracksTabsHelper extends TrackTabsHelper {

	private final Set<String> initialPaths;
	private final Set<TrackItem> visibleTrackItems = new HashSet<>();
	private Set<String> selectedPaths;

	public SelectTracksTabsHelper(@NonNull OsmandApplication app, @NonNull Collection<String> paths) {
		super(app);
		this.initialPaths = new HashSet<>(paths);
	}

	@Override
	public void processVisibleTracks() {
		ItemsSelectionHelper<TrackItem> helper = getItemsSelectionHelper();
		if (selectedPaths == null) {
			selectedPaths = initialPaths;
		} else {
			selectedPaths = new HashSet<>(getSelectedPaths());
		}
		super.processVisibleTracks();
		visibleTrackItems.clear();
		visibleTrackItems.addAll(helper.getSelectedItems());

		helper.setOriginalSelectedItems(getTrackItems(initialPaths));
		helper.setSelectedItems(getTrackItems(selectedPaths));
	}

	@NonNull
	@Override
	protected Set<TrackItem> getVisibleTrackItems() {
		return visibleTrackItems;
	}

	@NonNull
	public List<String> getSelectedPaths() {
		List<String> paths = new ArrayList<>();
		for (TrackItem item : getItemsSelectionHelper().getSelectedItems()) {
			paths.add(getTrackPath(item));
		}
		return paths;
	}

	@NonNull
	private Set<TrackItem> getTrackItems(@NonNull Set<String> paths) {
		Set<TrackItem> items = new HashSet<>();
		for (TrackItem item : getItemsSelectionHelper().getAllItems()) {
			if (paths.contains(getTrackPath(item))) {
				items.add(item);
			}
		}
		return items;
	}

	@NonNull
	private static String getTrackPath(@NonNull TrackItem item) {
		return item.isShowCurrentTrack() ? CURRENT_TRACK : item.getPath();
	}
}
