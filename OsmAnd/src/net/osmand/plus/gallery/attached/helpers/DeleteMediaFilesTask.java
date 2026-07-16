package net.osmand.plus.gallery.attached.helpers;

import android.os.AsyncTask;

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
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.shared.gpx.primitives.Link;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.shared.media.LinkMediaFactory;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Deletes internal media files that are no longer referenced by any favorite point
 * or any waypoint of a loaded GPX file after their links were removed.
 * <p>
 * Only app-managed internal media is affected: sources are resolved with
 * {@code includeNonManagedMedia = false}, so external ({@code content://}, {@code file://}),
 * remote and non-managed files are never deleted.
 */
class DeleteMediaFilesTask extends AsyncTask<Void, Void, Integer> {

	private static final Log LOG = PlatformUtil.getLog(DeleteMediaFilesTask.class);

	private final OsmandApplication app;
	private final List<Link> links;
	@Nullable
	private final CallbackWithObject<Integer> callback;

	DeleteMediaFilesTask(@NonNull OsmandApplication app, @NonNull List<Link> links, @Nullable CallbackWithObject<Integer> callback) {
		this.app = app;
		this.links = links;
		this.callback = callback;
	}

	@Override
	protected Integer doInBackground(Void... voids) {
		return deleteSources(collectUnreferencedSources());
	}

	@NonNull
	private List<MediaSource> collectUnreferencedSources() {
		Set<String> paths = new HashSet<>();
		addInternalPaths(paths, links);
		paths.removeAll(collectReferencedPaths());

		MediaStorageHelper storageHelper = new MediaStorageHelper(app);
		MediaStorageLocation location = MediaStorageLocation.fromSettings(app);

		List<MediaSource> res = new ArrayList<>();
		for (String path : paths) {
			MediaSource source = storageHelper.resolveMediaSource(location, LinkMediaFactory.createInternalUri(path), false);
			if (source != null) {
				res.add(source);
			}
		}
		return res;
	}

	private int deleteSources(@NonNull List<MediaSource> sources) {
		int deleted = 0;
		for (MediaSource source : sources) {
			try {
				source.delete();
				deleted++;
			} catch (IOException e) {
				LOG.warn("Failed to delete media file: " + source.getId(), e);
			}
		}
		logDebug("Media files cleanup finished: candidates=" + sources.size() + ", deleted=" + deleted);
		return deleted;
	}

	@NonNull
	private Set<String> collectReferencedPaths() {
		Set<String> res = new HashSet<>();
		for (FavouritePoint point : app.getFavoritesHelper().getFavouritePoints()) {
			addInternalPaths(res, point.getLinks());
		}
		for (SelectedGpxFile selectedGpxFile : app.getSelectedGpxHelper().getSelectedGPXFiles()) {
			for (WptPt wpt : selectedGpxFile.getGpxFile().getPointsList()) {
				addInternalPaths(res, wpt.getLinks());
			}
		}
		return res;
	}

	private static void addInternalPaths(@NonNull Set<String> res, @Nullable List<Link> links) {
		if (links == null) {
			return;
		}
		for (Link link : links) {
			String href = link != null ? link.getHref() : null;
			if (!Algorithms.isEmpty(href)) {
				String path = LinkMediaFactory.getInternalPath(href.trim());
				if (path != null) {
					res.add(path);
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
	protected void onPostExecute(Integer deleted) {
		if (callback != null) {
			callback.processResult(deleted);
		}
	}
}
