package net.osmand.plus.myplaces.tracks.dialogs;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.utils.AndroidUtils;

public class TrackFolderFragment extends BaseTrackFolderFragment {

	public static final String TAG = TrackFolderFragment.class.getSimpleName();

	public static void showInstance(@NonNull FragmentManager manager, @NonNull TrackFolder folder) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			TrackFolderFragment fragment = new TrackFolderFragment();
			fragment.trackFolder = folder;
			manager.beginTransaction()
					.replace(android.R.id.content, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}