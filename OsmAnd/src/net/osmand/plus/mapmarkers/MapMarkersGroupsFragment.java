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
import net.osmand.plus.mapmarkers.adapters.MapMarkersGroupsAdapter;

public class MapMarkersGroupsFragment extends Fragment {

    public static final String TAG = "MapMarkersGroupsFragment";

    private MapMarkersGroupsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final RecyclerView recyclerView = new RecyclerView(getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        final MapActivity mapActivity = (MapActivity) getActivity();

        adapter = new MapMarkersGroupsAdapter(mapActivity);
        return recyclerView;
    }

    void updateAdapter() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}
