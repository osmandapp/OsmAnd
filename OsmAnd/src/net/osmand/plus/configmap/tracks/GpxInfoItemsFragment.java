package net.osmand.plus.configmap.tracks;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.track.helpers.GPXInfo;
import net.osmand.plus.utils.UiUtilities;

import java.util.Set;

public class GpxInfoItemsFragment extends BaseOsmAndFragment {

	public static final String TAG = GpxInfoItemsFragment.class.getSimpleName();

	private OsmandApplication app;

	public TrackTab trackTab;
	private TracksAdapter adapter;

	private boolean nightMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		nightMode = isNightMode(true);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		inflater = UiUtilities.getInflater(requireContext(), nightMode);
		View view = inflater.inflate(R.layout.gpx_info_items_fragment, container, false);

		TracksFragment fragment = (TracksFragment) requireParentFragment();
		adapter = new TracksAdapter(app, trackTab, fragment, nightMode);

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(app));
		recyclerView.setAdapter(adapter);

		return view;
	}

	public void onGpxInfosSelected(@NonNull Set<GPXInfo> gpxInfos) {
		adapter.onGpxInfosSelected(gpxInfos);
	}

	public void updateContent() {
		adapter.notifyDataSetChanged();
	}
}