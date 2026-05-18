package net.osmand.plus.configmap.tracks;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.shared.gpx.GpxDbHelper;
import net.osmand.shared.gpx.GpxDbHelper.GpxDataItemCallback;
import net.osmand.shared.gpx.GpxHelper;
import net.osmand.shared.gpx.SmartFolderHelper;
import net.osmand.shared.gpx.TrackItem;
import net.osmand.shared.gpx.data.TrackFolder;
import net.osmand.shared.io.KFile;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Deprecated
public class TrackFolderLoaderTask extends AsyncTask<Void, TrackItem, Void> {

	private static final Log LOG = PlatformUtil.getLog(TrackFolderLoaderTask.class);
	private static final int LOG_BATCH_SIZE = 100;
	private static final int PROGRESS_BATCH_SIZE = 70;

	private final GpxDbHelper gpxDbHelper;
	private final SmartFolderHelper smartFolderHelper;

	private final TrackFolder folder;
	private final LoadTracksListener listener;
	private long loadingTime = 0;
	private int tracksCounter = 0;
	private GpxDataItemCallback callback;

	public TrackFolderLoaderTask(@NonNull OsmandApplication app, @NonNull TrackFolder folder, @NonNull LoadTracksListener listener) {
		this.folder = folder;
		this.listener = listener;
		this.gpxDbHelper = app.getGpxDbHelper();
		this.smartFolderHelper = app.getSmartFolderHelper();
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.loadTracksStarted();
		}
	}

	@Override
	protected void onProgressUpdate(TrackItem... items) {
		if (listener != null) {
			listener.loadTracksProgress(items);
		}
	}

	@Override
	protected Void doInBackground(Void... voids) {
		long start = System.currentTimeMillis();
		LOG.info("Start loading tracks in " + folder.getDirName());

		folder.clearData();
		loadingTime = System.currentTimeMillis();

		List<TrackItem> progress = new ArrayList<>();
		loadGPXFolder(folder, progress);

		if (!progress.isEmpty()) {
			publishProgress(progress.toArray(new TrackItem[0]));
		}
		if (listener != null) {
			listener.tracksLoaded(folder);
		}
		LOG.info("Finished loading tracks. Took " + (System.currentTimeMillis() - start) + "ms");
		return null;
	}

	private void loadGPXFolder(@NonNull TrackFolder rootFolder, @NonNull List<TrackItem> progress) {
		Deque<TrackFolder> folders = new ArrayDeque<>();
		folders.push(rootFolder);

		while (!folders.isEmpty()) {
			TrackFolder folder = folders.pop();
			KFile dir = folder.getDirFile();
			List<KFile> files = dir.listFiles();
			if (Algorithms.isEmpty(files)) {
				continue;
			}
			List<TrackItem> trackItems = new ArrayList<>();
			List<TrackFolder> subFolders = new ArrayList<>();

			for (KFile file : files) {
				if (isCancelled()) {
					return;
				}
				if (file.isDirectory()) {
					TrackFolder subfolder = new TrackFolder(file, folder);
					subFolders.add(subfolder);
					folders.push(subfolder); // Add subfolder to the queue for processing
				} else if (GpxHelper.INSTANCE.isGpxFile(file)) {
					TrackItem item = new TrackItem(file);
					item.setDataItem(getDataItem(item, file));
					trackItems.add(item);

					progress.add(item);
					if (progress.size() > PROGRESS_BATCH_SIZE) {
						publishProgress(progress.toArray(new TrackItem[0]));
						progress.clear();
					}
					tracksCounter++;
					if (tracksCounter % LOG_BATCH_SIZE == 0) {
						long endTime = System.currentTimeMillis();
						LOG.info("Loading " + LOG_BATCH_SIZE + " tracks. Took " + (endTime - loadingTime) + "ms");
						loadingTime = endTime;
					}
				}
			}
			folder.setTrackItems(trackItems);
			folder.setSubFolders(subFolders);
			smartFolderHelper.addTrackItemsToSmartFolder(trackItems);
		}
		for (TrackFolder folder : rootFolder.getFlattenedSubFolders()) {
			folder.resetCachedData();
		}
		rootFolder.resetCachedData();
	}

	@Nullable
	private GpxDataItem getDataItem(@NonNull TrackItem trackItem, @NonNull KFile file) {
		if (callback == null) {
			callback = new GpxDataItemCallback() {
				@Override
				public boolean isCancelled() {
					return TrackFolderLoaderTask.this.isCancelled();
				}

				@Override
				public void onGpxDataItemReady(@NonNull GpxDataItem item) {
					trackItem.setDataItem(item);
				}
			};
		}
		return gpxDbHelper.getItem(file, callback);
	}

	@Override
	protected void onPostExecute(Void unused) {
		if (listener != null) {
			listener.loadTracksFinished(folder);
		}
		smartFolderHelper.notifyUpdateListeners();
	}

	public interface LoadTracksListener {

		@UiThread
		default void loadTracksStarted() {
		}

		@UiThread
		default void loadTracksProgress(@NonNull TrackItem[] items) {
		}

		@WorkerThread
		default void tracksLoaded(@NonNull TrackFolder folder) {
		}

		@UiThread
		void loadTracksFinished(@NonNull TrackFolder folder);
	}
}
