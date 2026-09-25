package net.osmand.plus.myplaces.tracks.tasks;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.CallbackWithObject;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.shared.gpx.TrackItem;
import net.osmand.shared.gpx.data.TrackFolder;
import net.osmand.shared.gpx.data.TracksGroup;
import net.osmand.plus.utils.FileUtils;
import net.osmand.shared.io.KFile;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MoveTrackFoldersTask extends BaseLoadAsyncTask<Void, Void, Void> {

	private final File destinationFolder;
	private final Set<TrackItem> trackItems;
	private final Set<TracksGroup> tracksGroups;
	private final Set<TrackItem> existingTrackItems = new HashSet<>();
	private final CallbackWithObject<Set<TrackItem>> callback;


	public MoveTrackFoldersTask(@NonNull FragmentActivity activity,
	                            @NonNull File destinationFolder,
	                            @NonNull Set<TrackItem> trackItems,
	                            @NonNull Set<TracksGroup> tracksGroups,
	                            @Nullable CallbackWithObject<Set<TrackItem>> callback) {
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
		KFile src = trackFolder.getDirFile();
		if (!Algorithms.objectEquals(src, destinationFolder)) {
			File dest = new File(destinationFolder, src.name());
			if (src.renameTo(dest.getAbsolutePath())) {
				dest.setLastModified(System.currentTimeMillis());

				List<File> files = new ArrayList<>();
				for (TrackItem trackItem : trackFolder.getFlattenedTrackItems()) {
					KFile file = trackItem.getFile();
					if (file != null) {
						files.add(SharedUtil.jFile(file));
					}
				}
				FileUtils.updateMovedGpxFiles(app, files, SharedUtil.jFile(src), dest);
			}
		}
	}

	private void moveTracks(@NonNull Collection<TrackItem> trackItems) {
		for (TrackItem trackItem : trackItems) {
			KFile src = trackItem.getFile();
			if (src != null) {
				File dest = new File(destinationFolder, src.name());
				if (dest.exists()) {
					existingTrackItems.add(trackItem);
				} else {
					FileUtils.renameGpxFile(app, SharedUtil.jFile(src), dest);
				}
			}
		}
	}

	@Override
	protected void onPostExecute(Void result) {
		hideProgress();

		if (callback != null) {
			callback.processResult(existingTrackItems);
		}
	}
}