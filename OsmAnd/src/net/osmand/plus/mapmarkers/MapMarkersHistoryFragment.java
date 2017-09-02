package net.osmand.plus.mapmarkers;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapmarkers.adapters.MapMarkersHistoryAdapter;

public class MapMarkersHistoryFragment extends Fragment {

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final RecyclerView recyclerView = new RecyclerView(getContext());
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		final MapActivity mapActivity = (MapActivity) getActivity();

		recyclerView.setAdapter(new MapMarkersHistoryAdapter(mapActivity.getMyApplication()));

		return recyclerView;
	}
}
