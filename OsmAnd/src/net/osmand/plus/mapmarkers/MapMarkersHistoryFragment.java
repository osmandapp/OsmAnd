package net.osmand.plus.mapmarkers;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.data.PointDescription;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapmarkers.adapters.MapMarkersHistoryAdapter;

public class MapMarkersHistoryFragment extends Fragment implements MapMarkersHelper.MapMarkerChangedListener {

	MapMarkersHistoryAdapter adapter;
	OsmandApplication app;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		app = getMyApplication();
		final MapActivity mapActivity = (MapActivity) getActivity();

		Fragment historyMarkerMenuFragment = mapActivity.getSupportFragmentManager().findFragmentByTag(HistoryMarkerMenuBottomSheetDialogFragment.TAG);
		if (historyMarkerMenuFragment != null) {
			((HistoryMarkerMenuBottomSheetDialogFragment) historyMarkerMenuFragment).setListener(createHistoryMarkerMenuListener());
		}

		final RecyclerView recyclerView = new RecyclerView(getContext());
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

		adapter = new MapMarkersHistoryAdapter(mapActivity.getMyApplication());
		adapter.setAdapterListener(new MapMarkersHistoryAdapter.MapMarkersHistoryAdapterListener() {
			@Override
			public void onItemClick(View view) {
				int pos = recyclerView.getChildAdapterPosition(view);
				Object item = adapter.getItem(pos);
				if (item instanceof MapMarker) {
					MapMarker marker = (MapMarker) item;
					HistoryMarkerMenuBottomSheetDialogFragment fragment = new HistoryMarkerMenuBottomSheetDialogFragment();
					Bundle arguments = new Bundle();
					arguments.putInt(HistoryMarkerMenuBottomSheetDialogFragment.MARKER_POSITION, pos);
					arguments.putString(HistoryMarkerMenuBottomSheetDialogFragment.MARKER_NAME, marker.getName(mapActivity));
					arguments.putInt(HistoryMarkerMenuBottomSheetDialogFragment.MARKER_COLOR_INDEX, marker.colorIndex);
					arguments.putLong(HistoryMarkerMenuBottomSheetDialogFragment.MARKER_VISITED_DATE, marker.visitedDate);
					fragment.setArguments(arguments);
					fragment.setListener(createHistoryMarkerMenuListener());
					fragment.show(mapActivity.getSupportFragmentManager(), HistoryMarkerMenuBottomSheetDialogFragment.TAG);
				}
			}
		});
		recyclerView.setAdapter(adapter);

		app.getMapMarkersHelper().addListener(this);

		return recyclerView;
	}

	private HistoryMarkerMenuBottomSheetDialogFragment.HistoryMarkerMenuFragmentListener createHistoryMarkerMenuListener() {
		return new HistoryMarkerMenuBottomSheetDialogFragment.HistoryMarkerMenuFragmentListener() {
			@Override
			public void onMakeMarkerActive(int pos) {
				Object item = adapter.getItem(pos);
				if (item instanceof MapMarker) {
					app.getMapMarkersHelper().restoreMarkerFromHistory((MapMarker) item, 0);
					adapter.notifyItemRemoved(pos);
				}
			}

			@Override
			public void onDeleteMarker(int pos) {

			}
		};
	}

	@Override
	public void onDestroy() {
		app.getMapMarkersHelper().removeListener(this);
		super.onDestroy();
	}

	void updateAdapter() {
		adapter.createHeaders();
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	public OsmandApplication getMyApplication() {
		return (OsmandApplication)getActivity().getApplication();
	}

	@Override
	public void onMapMarkerChanged(MapMarker mapMarker) {
		updateAdapter();
	}

	@Override
	public void onMapMarkersChanged() {
		updateAdapter();
	}
}
