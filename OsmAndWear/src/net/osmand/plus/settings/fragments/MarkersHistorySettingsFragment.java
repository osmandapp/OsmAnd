package net.osmand.plus.settings.fragments;

import android.util.Pair;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.plus.R;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.mapmarkers.ItineraryDataHelper;
import net.osmand.plus.mapmarkers.MapMarker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarkersHistorySettingsFragment extends HistoryItemsFragment {

	public static final String TAG = MarkersHistorySettingsFragment.class.getSimpleName();

	@Override
	protected void setupWarningCard(@NonNull View view) {
		super.setupWarningCard(view);
		TextView warning = warningCard.findViewById(R.id.title);
		warning.setText(getString(R.string.is_disabled, getString(R.string.map_markers_history)));
	}

	@Override
	protected void updateHistoryItems() {
		clearItems();

		List<Pair<Long, MapMarker>> pairs = new ArrayList<>();
		for (MapMarker marker : app.getMapMarkersHelper().getMapMarkersHistory()) {
			pairs.add(new Pair<>(marker.visitedDate, marker));
		}

		Map<Integer, List<MapMarker>> markerGroups = new HashMap<>();
		HistoryAdapter.createHistoryGroups(pairs, markerGroups, items);
		for (Map.Entry<Integer, List<MapMarker>> entry : markerGroups.entrySet()) {
			itemsGroups.put(entry.getKey(), new ArrayList<>(entry.getValue()));
		}
	}

	@Override
	protected void setupToolbar(@NonNull View view) {
		super.setupToolbar(view);

		View toolbarContainer = view.findViewById(R.id.toolbar);

		TextView title = toolbarContainer.findViewById(R.id.toolbar_title);
		title.setText(R.string.map_markers_history);

		toolbarContainer.findViewById(R.id.toolbar_switch_container).setOnClickListener(view1 -> {
			boolean checked = !settings.MAP_MARKERS_HISTORY.get();
			settings.MAP_MARKERS_HISTORY.set(checked);
			updateToolbarSwitch(toolbarContainer);
			updateDisabledItems();

			Fragment target = getTargetFragment();
			if (target instanceof OnPreferenceChanged) {
				((OnPreferenceChanged) target).onPreferenceChanged(settings.SEARCH_HISTORY.getId());
			}
		});
	}

	@Override
	protected void shareItems() {
		List<MapMarker> mapMarkers = getSelectedMarkers();
		ItineraryDataHelper dataHelper = app.getMapMarkersHelper().getDataHelper();
		GpxFile gpxFile = dataHelper.generateGpx(mapMarkers, true);
		GpxUiHelper.saveAndShareGpx(app, gpxFile);
	}

	@Override
	protected void deleteSelectedItems() {
		List<MapMarker> mapMarkers = getSelectedMarkers();
		app.getMapMarkersHelper().removeMarkers(mapMarkers);
	}

	private List<MapMarker> getSelectedMarkers() {
		List<MapMarker> mapMarkers = new ArrayList<>();
		for (Object item : selectedItems) {
			if (item instanceof MapMarker) {
				mapMarkers.add((MapMarker) item);
			}
		}
		return mapMarkers;
	}

	@Override
	protected boolean isHistoryEnabled() {
		return settings.MAP_MARKERS_HISTORY.get();
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			MarkersHistorySettingsFragment fragment = new MarkersHistorySettingsFragment();
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}
}
