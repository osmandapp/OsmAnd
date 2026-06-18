package net.osmand.plus.settings.mediastorage.task;

import android.os.AsyncTask;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.mediastorage.MediaSource;
import net.osmand.plus.settings.mediastorage.MediaStorageHelper;
import net.osmand.plus.settings.mediastorage.MediaStorageLocation;
import net.osmand.plus.settings.mediastorage.task.CollectMediaFilesTask.MediaFilesCollection;
import net.osmand.plus.utils.FileUtils;
import net.osmand.shared.gpx.primitives.Link;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollectMediaFilesTask extends AsyncTask<Void, Void, MediaFilesCollection> {

	private static final Log LOG = PlatformUtil.getLog(CollectMediaFilesTask.class);

	public record MediaFilesCollection(@NonNull List<MediaSource> sources, @NonNull Pair<Long, Long> filesSize) {}

	private final OsmandApplication app;
	private final MediaStorageHelper helper;
	private final MediaStorageLocation storage;
	private final CallbackWithObject<MediaFilesCollection> callback;

	public CollectMediaFilesTask(@NonNull OsmandApplication app, @NonNull MediaStorageLocation storage,
								 @NonNull CallbackWithObject<MediaFilesCollection> callback) {
		this.app = app;
		this.helper = new MediaStorageHelper(app);
		this.storage = storage;
		this.callback = callback;
	}

	@Override
	protected MediaFilesCollection doInBackground(Void... params) {
		logDebug("Collect media migration sources started: storage=" + storage.getStorageType() + ", manualUri=" + storage.getManualUri());
		return collectMediaFiles();
	}

	@NonNull
	private MediaFilesCollection collectMediaFiles() {
		List<MediaSource> sources = new ArrayList<>();
		Set<String> addedSourceIds = new HashSet<>();
		collectLinkedMediaFiles(sources, addedSourceIds);
		logDebug("Collect media migration sources finished: sources=" + sources.size());
		return new MediaFilesCollection(sources, calculateFilesSize(sources));
	}

	@NonNull
	private Pair<Long, Long> calculateFilesSize(@NonNull List<MediaSource> sources) {
		long filesSize = 0;
		long estimatedSize = 0;
		for (MediaSource source : sources) {
			long length = source.getLength();
			filesSize += length;
			estimatedSize += length + FileUtils.APPROXIMATE_FILE_SIZE_BYTES;
		}
		return new Pair<>(filesSize, estimatedSize);
	}

	private void collectLinkedMediaFiles(@NonNull List<MediaSource> sources, @NonNull Set<String> addedSourceIds) {
		for (FavouritePoint point : app.getFavoritesHelper().getFavouritePoints()) {
			collectLinkedMediaFilesFromLinks(sources, addedSourceIds, point.getLinks());
		}
	}

	private void collectLinkedMediaFilesFromLinks(@NonNull List<MediaSource> sources, @NonNull Set<String> addedSourceIds, @Nullable List<Link> links) {
		if (!Algorithms.isEmpty(links)) {
			for (Link link : links) {
				MediaSource source = helper.resolveManagedMediaSource(storage, link.getHref());
				if (source != null) {
					if (addedSourceIds.add(source.getId())) {
						sources.add(source);
						logDebug("Collected media migration source: id=" + source.getId() + ", fileName=" + source.getFileName()
								+ ", dirType=" + source.getDirType() + ", length=" + source.getLength());
					}
				} else {
					logDebug("Skipped unresolved media link during migration: href=" + link.getHref());
				}
			}
		}
	}

	private static void logDebug(@NonNull String message) {
		if (PluginsHelper.isDevelopment()) {
			LOG.debug(message);
		}
	}

	@Override
	protected void onPostExecute(MediaFilesCollection collection) {
		callback.processResult(collection);
	}
}