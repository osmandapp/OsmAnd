package net.osmand.plus.mapmarkers;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapmarkers.adapters.MapMarkersActiveAdapter;
import net.osmand.plus.mapmarkers.adapters.MapMarkersActiveAdapter.MapMarkersActiveAdapterListener;

public class MapMarkersActiveFragment extends Fragment {

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final RecyclerView recyclerView = new RecyclerView(getContext());
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		MapActivity mapActivity = (MapActivity) getActivity();
		if (mapActivity != null) {
			MapMarkersActiveAdapter adapter = new MapMarkersActiveAdapter(mapActivity);
			adapter.setAdapterListener(new MapMarkersActiveAdapterListener() {
				@Override
				public void onItemClick(View view) {
					Toast.makeText(getContext(), recyclerView.indexOfChild(view) + "", Toast.LENGTH_SHORT).show();
				}
			});
			recyclerView.setAdapter(adapter);
		}
		return recyclerView;
	}
}
