package net.osmand.plus.configmap.tracks;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TracksTabAdapter extends FragmentStateAdapter {

	private final List<TrackTab> trackTabs = new ArrayList<>();

	public TracksTabAdapter(@NonNull TracksFragment fragment) {
		super(fragment);
	}

	public void setTrackTabs(@NonNull Map<String, TrackTab> tabs) {
		trackTabs.clear();
		trackTabs.addAll(tabs.values());
		notifyDataSetChanged();
	}

	@NonNull
	@Override
	public Fragment createFragment(int position) {
		TracksTreeFragment fragment = new TracksTreeFragment();
		fragment.trackTab = trackTabs.get(position);
		return fragment;
	}

	@Override
	public int getItemCount() {
		return trackTabs.size();
	}
}