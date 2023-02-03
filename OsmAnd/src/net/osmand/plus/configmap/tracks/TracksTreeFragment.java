package net.osmand.plus.configmap.tracks;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.configmap.tracks.TracksAdapter.TracksVisibilityListener;
import net.osmand.plus.utils.UiUtilities;

public class TracksTreeFragment extends BaseOsmAndFragment {

	public static final String TAG = TracksTreeFragment.class.getSimpleName();

	private OsmandApplication app;

	public TrackTab trackTab;
	private TracksAdapter adapter;
	private TracksVisibilityListener listener;

	private boolean nightMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		nightMode = isNightMode(false);

		Fragment fragment = getParentFragment();
		if (fragment instanceof TracksVisibilityListener) {
			listener = ((TracksVisibilityListener) fragment);
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		inflater = UiUtilities.getInflater(requireContext(), nightMode);
		View view = inflater.inflate(R.layout.tracks_tree_fragment, container, false);

		adapter = new TracksAdapter(app, trackTab, listener, nightMode);

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(app));
		recyclerView.setAdapter(adapter);

		return view;
	}

	public void onTrackItemSelected(@NonNull GPXInfo gpxInfo) {
		adapter.onTrackItemSelected(gpxInfo);
	}

	public void updateContent() {
		adapter.notifyDataSetChanged();
	}
}