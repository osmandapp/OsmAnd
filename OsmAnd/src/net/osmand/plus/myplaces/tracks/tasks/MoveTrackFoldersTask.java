package net.osmand.plus.myplaces.tracks.tasks;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.CallbackWithObject;
import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.data.TracksGroup;
import net.osmand.plus.utils.FileUtils;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.Collection;
import java.util.Set;

public class MoveTrackFoldersTask extends BaseLoadAsyncTask<Void, Void, Void> {

	private final File destinationFolder;
	private final Set<TrackItem> trackItems;
	private final Set<TracksGroup> tracksGroups;
	private final CallbackWithObject<Void> callback;


	public MoveTrackFoldersTask(@NonNull FragmentActivity activity,
	                            @NonNull File destinationFolder,
	                            @NonNull Set<TrackItem> trackItems,
	                            @NonNull Set<TracksGroup> tracksGroups,
	                            @Nullable CallbackWithObject<Void> callback) {
		super(activity);
		this.trackItems = trackItems;
		this.tracksGroups = tracksGroups;
		this.destinationFolder = destinationFolder;
		this.callback = callback;
	}

	@Override
	protected Void doInBackground(Void... params) {
		if (!destinationFolder.exists()) {
			destinationFolder.mkdirs();
		}
		moveTracks(trackItems);
		moveTracksGroups(tracksGroups);
		return null;
	}

	private void moveTracksGroups(@NonNull Collection<TracksGroup> tracksGroups) {
		for (TracksGroup group : tracksGroups) {
			if (group instanceof TrackFolder) {
				moveTrackFolder((TrackFolder) group);
			} else {
				moveTracks(group.getTrackItems());
			}
		}
	}

	private void moveTrackFolder(@NonNull TrackFolder trackFolder) {
		File src = trackFolder.getDirFile();
		if (!Algorithms.objectEquals(src, destinationFolder)) {
			File dest = new File(destinationFolder, src.getName());
			if (src.renameTo(dest)) {
				dest.setLastModified(System.currentTimeMillis());
				updateMovedGpx(trackFolder, src, dest);
			}
		}
	}

	private void moveTracks(@NonNull Collection<TrackItem> trackItems) {
		for (TrackItem trackItem : trackItems) {
			File src = trackItem.getFile();
			if (src != null) {
				File dest = new File(destinationFolder, src.getName());
				if (!dest.exists()) {
					FileUtils.renameGpxFile(app, src, dest);
				}
			}
		}
	}

	private void updateMovedGpx(@NonNull TrackFolder trackFolder, @NonNull File srcDir, @NonNull File destDir) {
		for (TrackItem trackItem : trackFolder.getFlattenedTrackItems()) {
			String path = trackItem.getPath();
			String newPath = path.replace(srcDir.getAbsolutePath(), destDir.getAbsolutePath());

			File srcFile = trackItem.getFile();
			File destFile = new File(newPath);
			if (srcFile != null && destFile.exists()) {
				FileUtils.updateMovedGpx(app, srcFile, destFile);
			}
		}
	}

	@Override
	protected void onPostExecute(Void result) {
		hideProgress();

		if (callback != null) {
			callback.processResult(null);
		}
	}
}