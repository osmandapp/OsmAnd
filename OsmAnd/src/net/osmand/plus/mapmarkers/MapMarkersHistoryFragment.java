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
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapmarkers.adapters.MapMarkersHistoryAdapter;

public class MapMarkersHistoryFragment extends Fragment {

	MapMarkersHistoryAdapter adapter;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final RecyclerView recyclerView = new RecyclerView(getContext());
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		final MapActivity mapActivity = (MapActivity) getActivity();

		adapter = new MapMarkersHistoryAdapter(mapActivity.getMyApplication());
		adapter.setAdapterListener(new MapMarkersHistoryAdapter.MapMarkersHistoryAdapterListener() {
			@Override
			public void onItemClick(View view) {
				int pos = recyclerView.indexOfChild(view);
				MapMarker marker = adapter.getItem(pos);
				mapActivity.getMyApplication().getSettings().setMapLocationToShow(marker.getLatitude(), marker.getLongitude(),
						15, new PointDescription(PointDescription.POINT_TYPE_LOCATION, marker.getPointDescription(mapActivity).getName()),
						false, null);
				MapActivity.launchMapActivityMoveToTop(mapActivity);
				((DialogFragment) getParentFragment()).dismiss();
			}
		});
		recyclerView.setAdapter(adapter);

		return recyclerView;
	}

	void updateAdapter() {
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}
}
