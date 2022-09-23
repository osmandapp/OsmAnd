package net.osmand.plus.configmap;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;

public class TracksTabAdapter extends FragmentStateAdapter {
	ArrayList<TrackGroup> trackGroups;

	public TracksTabAdapter(@NonNull Fragment fragment, ArrayList<TrackGroup> trackGroups) {
		super(fragment);
		this.trackGroups = trackGroups;
	}

	@NonNull
	@Override
	public Fragment createFragment(int position) {
		TracksTreeFragment fragment = new TracksTreeFragment();

		fragment.setGroupName(trackGroups.get(position).groupName);

		return fragment;
	}

	@Override
	public int getItemCount() {
		return trackGroups.size();
	}
}
