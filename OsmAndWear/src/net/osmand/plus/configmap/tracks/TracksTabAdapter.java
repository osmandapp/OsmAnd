package net.osmand.plus.configmap.tracks;

import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import net.osmand.plus.OsmandApplication;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TracksTabAdapter extends FragmentStatePagerAdapter {

	private final OsmandApplication app;
	private final List<TrackTab> trackTabs = new ArrayList<>();

	public TracksTabAdapter(@NonNull OsmandApplication app, @NonNull FragmentManager manager, @NonNull List<TrackTab> tabs) {
		super(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
		this.app = app;
		trackTabs.addAll(tabs);
	}

	public void setTrackTabs(@NonNull Map<String, TrackTab> tabs) {
		trackTabs.clear();
		trackTabs.addAll(tabs.values());
		notifyDataSetChanged();
	}

	@Override
	public int getItemPosition(@NonNull Object object) {
		if (object instanceof TrackItemsFragment) {
			TrackItemsFragment fragment = (TrackItemsFragment) object;
			int index = trackTabs.indexOf(fragment.getTrackTab());
			return index >= 0 ? index : POSITION_NONE;
		}
		return POSITION_NONE;
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
		return trackTabs.get(position).getName(app);
	}

	@Override
	public Parcelable saveState() {
		return null;
	}
}