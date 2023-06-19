package net.osmand.plus.myplaces.tracks.tasks;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.myplaces.tracks.VisibleTracksGroup;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.data.TracksGroup;
import net.osmand.plus.utils.FileUtils;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.Collection;
import java.util.Set;

public class DeleteTracksTask extends AsyncTask<Void, File, Void> {

	private final OsmandApplication app;
	private final Set<TrackItem> trackItems;
	private final Set<TracksGroup> tracksGroups;
	@Nullable
	private final GpxFilesDeletionListener listener;

	private int totalFiles;
	private int deletedFiles;

	public DeleteTracksTask(@NonNull OsmandApplication app, @Nullable Set<TrackItem> trackItems,
	                        @Nullable Set<TracksGroup> tracksGroups, @Nullable GpxFilesDeletionListener listener) {
		this.app = app;
		this.trackItems = trackItems;
		this.tracksGroups = tracksGroups;
		this.listener = listener;
	}

	@Override
	protected Void doInBackground(Void... params) {
		if (!Algorithms.isEmpty(trackItems)) {
			deleteTrackItems(trackItems);
		}
		if (!Algorithms.isEmpty(tracksGroups)) {
			deleteTrackGroups(tracksGroups);
		}
		return null;
	}

	private void deleteTrackItems(@NonNull Collection<TrackItem> trackItems) {
		for (TrackItem trackItem : trackItems) {
			if (isCancelled()) {
				break;
			}
			File file = trackItem.getFile();
			if (file != null && file.exists()) {
				totalFiles++;
				if (FileUtils.removeGpxFile(app, file)) {
					deletedFiles++;
					publishProgress(file);
				}
			}
		}
	}

	private void deleteTrackGroups(@NonNull Set<TracksGroup> tracksGroups) {
		for (TracksGroup tracksGroup : tracksGroups) {
			if (isCancelled()) {
				break;
			}
			if (tracksGroup instanceof TrackFolder) {
				totalFiles++;
				TrackFolder trackFolder = (TrackFolder) tracksGroup;
				deleteTrackItems(trackFolder.getFlattenedTrackItems());

				File dirFile = trackFolder.getDirFile();
				if (Algorithms.removeAllFiles(dirFile)) {
					deletedFiles++;
					publishProgress(dirFile);
				}
			} else if (tracksGroup instanceof VisibleTracksGroup) {
				VisibleTracksGroup visibleTracksGroup = (VisibleTracksGroup) tracksGroup;
				deleteTrackItems(visibleTracksGroup.getTrackItems());
			}
		}
	}


	@Override
	protected void onProgressUpdate(File... values) {
		if (listener != null) {
			listener.onGpxFilesDeleted(values);
		}
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.onGpxFilesDeletionStarted();
		}
	}

	@Override
	protected void onPostExecute(Void result) {
		app.showToastMessage(R.string.local_index_items_deleted, deletedFiles, totalFiles);

		if (listener != null) {
			listener.onGpxFilesDeletionFinished();
		}
	}

	public interface GpxFilesDeletionListener {
		default void onGpxFilesDeletionStarted() {

		}

		default void onGpxFilesDeleted(File... values) {
		}

		default void onGpxFilesDeletionFinished() {

		}
	}
}
