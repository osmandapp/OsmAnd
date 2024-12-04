package net.osmand.plus.configmap.tracks;

import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class TracksTabAdapter extends FragmentStatePagerAdapter {

	private final List<TrackTab> trackTabs = new ArrayList<>();

	public TracksTabAdapter(@NonNull FragmentManager manager, @NonNull List<TrackTab> tabs) {
		super(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
		trackTabs.addAll(tabs);
	}

	public void setTrackTabs(@NonNull List<TrackTab> tabs) {
		trackTabs.clear();
		trackTabs.addAll(tabs);
		notifyDataSetChanged();
	}

	@Override
	public int getItemPosition(@NonNull Object object) {
		if (object instanceof TrackItemsFragment fragment) {
			int index = trackTabs.indexOf(fragment.getTrackTab());
			return index >= 0 ? index : POSITION_NONE;
		}
		return POSITION_NONE;
	}

	@NonNull
	public List<TrackTab> getTrackTabs() {
		return trackTabs;
	}

	@Override
	public int getCount() {
		return trackTabs.size();
	}

	@NonNull
	@Override
	public Fragment getItem(int position) {
		TrackItemsFragment fragment = new TrackItemsFragment();
		fragment.setTrackTab(trackTabs.get(position));
		fragment.setRetainInstance(true);
		return fragment;
	}

	@Override
	public CharSequence getPageTitle(int position) {
		return trackTabs.get(position).getName();
	}

	@Override
	public Parcelable saveState() {
		return null;
	}
}