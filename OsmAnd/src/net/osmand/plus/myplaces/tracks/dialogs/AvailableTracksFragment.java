package net.osmand.plus.myplaces.tracks.dialogs;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;

import android.os.AsyncTask;
import android.os.AsyncTask.Status;

import androidx.annotation.NonNull;

import net.osmand.plus.configmap.tracks.TrackFolderLoaderTask;
import net.osmand.plus.configmap.tracks.TrackFolderLoaderTask.LoadTracksListener;
import net.osmand.plus.configmap.tracks.TrackItemsFragment;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.utils.FileUtils;

import java.io.File;

public class AvailableTracksFragment extends BaseTrackFolderFragment implements LoadTracksListener {

	public static final String TAG = TrackItemsFragment.class.getSimpleName();

	private TrackFolderLoaderTask trackFolderLoader;


	@Override
	public void onResume() {
		super.onResume();

		if (trackFolder == null && (trackFolderLoader == null || trackFolderLoader.getStatus() != Status.RUNNING)) {
			reloadTracks();
		}
	}

	private void reloadTracks() {
		File gpxDir = FileUtils.getExistingDir(app, GPX_INDEX_DIR);
		trackFolderLoader = new TrackFolderLoaderTask(app, gpxDir, this);
		trackFolderLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	public void loadTracksStarted() {
		showProgressBar();
	}

	@Override
	public void loadTracksFinished(@NonNull TrackFolder folder) {
		hideProgressBar();
		trackFolder = folder;
		adapter.updateFolder(trackFolder);
	}

	public void showProgressBar() {
		if (getActivity() != null) {
			((MyPlacesActivity) getActivity()).setSupportProgressBarIndeterminateVisibility(true);
		}
	}

	public void hideProgressBar() {
		if (getActivity() != null) {
			((MyPlacesActivity) getActivity()).setSupportProgressBarIndeterminateVisibility(false);
		}
	}
}