package net.osmand.plus.configmap.tracks;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TracksTabAdapter extends FragmentStateAdapter {

	private final List<TrackTab> trackTabs = new ArrayList<>();

	public TracksTabAdapter(@NonNull TracksFragment fragment, @NonNull List<TrackTab> tabs) {
		super(fragment);
		trackTabs.addAll(tabs);
	}

	public void setTrackTabs(@NonNull Map<String, TrackTab> tabs) {
		trackTabs.clear();
		trackTabs.addAll(tabs.values());
		notifyDataSetChanged();
	}

	@NonNull
	@Override
	public Fragment createFragment(int position) {
		GpxInfoItemsFragment fragment = new GpxInfoItemsFragment();
		fragment.trackTab = trackTabs.get(position);
		fragment.setRetainInstance(true);
		return fragment;
	}

	@Override
	public int getItemCount() {
		return trackTabs.size();
	}
}