package net.osmand.plus.gallery.attached.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.gallery.data.GalleryKey;
import net.osmand.plus.myplaces.favorites.FavoritesListener;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.myplaces.favorites.add.AddFavoriteOptions;
import net.osmand.plus.myplaces.favorites.add.AddFavoriteResult;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.plugins.audionotes.Recording;
import net.osmand.plus.settings.mediastorage.MediaSource;
import net.osmand.plus.settings.mediastorage.MediaStorageHelper;
import net.osmand.plus.settings.mediastorage.MediaStorageLocation;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.track.helpers.save.SaveGpxHelper;
import net.osmand.shared.gpx.primitives.Link;
import net.osmand.shared.gpx.primitives.Linkable;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.shared.media.LinkMediaFactory;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.IdentityHashMap;
import java.io.File;
import net.osmand.shared.media.MediaProvider;

public class AttachedMediaDataHelper {

	private static final Log LOG = PlatformUtil.getLog(AttachedMediaDataHelper.class);

	public static final String MEDIA_FAVORITES_GROUP = "media";

	private final OsmandApplication app;
	private final MediaStorageHelper mediaStorageHelper;

	public AttachedMediaDataHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.mediaStorageHelper = new MediaStorageHelper(app);
	}

	@NonNull
	public List<Link> collectMediaLinks(@NonNull Collection<FavoriteGroup> groups) {
		List<Link> res = new ArrayList<>();
		for (FavoriteGroup group : groups) {
			for (FavouritePoint point : group.getPoints()) {
				List<Link> links = point.getLinks();
				if (links != null) {
					for (Link link : links) {
						if (link != null && !Algorithms.isEmpty(link.getHref())) {
							res.add(link);
						}
					}
				}
			}
		}
		return res;
	}

	@Nullable
	public MediaSource resolveExportMediaSource(@Nullable String href, boolean includeNonManagedMedia) {
		MediaStorageLocation location = MediaStorageLocation.fromSettings(app);
		return includeNonManagedMedia ? mediaStorageHelper.resolveMediaSource(location, href, true) : mediaStorageHelper.resolveManagedMediaSource(location, href);
	}

	public void addMediaLinks(@NonNull Linkable target, @NonNull List<Link> links, @Nullable Runnable onMediaChanged) {
		if (links.isEmpty()) {
			logDebug("Attached media add skipped: empty links");
			return;
		}
		logDebug("Attached media add: target=" + target.getClass().getSimpleName() + ", links=" + links.size());

		for (int i = 0; i < links.size(); i++) {
			target.addLink(links.get(i));
		}

		if (target instanceof FavouritePoint) {
			app.getFavoritesHelper().saveCurrentPointsIntoFile(true);
			logDebug("Attached media links saved to favorites: links=" + links.size());
		} else if (target instanceof WptPt wpt) {
			SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedGPXFile(wpt);
			if (selectedGpxFile != null) {
				SaveGpxHelper.saveGpx(selectedGpxFile.getGpxFile());
				logDebug("Attached media links saved to Gpx: links=" + links.size());
			}
		} else {
			LOG.warn("Unsupported Linkable type, links not persisted: " + target.getClass().getName());
			return;
		}
		if (onMediaChanged != null) {
			onMediaChanged.run();
		}
		notifyMediaChanged(target);
	}

	public void removeMediaLinks(@NonNull Linkable target, @NonNull List<Link> links, @Nullable CallbackWithObject<Boolean> callback) {
		removeMediaLinks(target, links, true, callback);
	}

	public void removeMediaLinks(@NonNull Linkable target, @NonNull List<Link> links,
	                             boolean deleteUnreferencedFiles, @Nullable CallbackWithObject<Boolean> callback) {
		if (links.isEmpty()) {
			logDebug("Attached media remove skipped: empty links");
			notifyResult(callback, true);
			return;
		}
		logDebug("Attached media remove: target=" + target + ", links=" + links.size());
		List<Link> originals = new ArrayList<>(target.getLinks() != null ? target.getLinks() : List.of());

		for (Link link : links) {
			target.removeLink(link);
		}
		saveTarget(target, success -> {
			if (Boolean.TRUE.equals(success) && deleteUnreferencedFiles) {
				OsmAndTaskManager.executeTask(new DeleteMediaFilesTask(app, links, null));
			}
			if (!Boolean.TRUE.equals(success)) {
				for (Link link : new ArrayList<>(target.getLinks() != null ? target.getLinks() : List.of())) target.removeLink(link);
				for (Link link : originals) target.addLink(link);
			} else {
				notifyMediaChanged(target);
			}
			notifyResult(callback, Boolean.TRUE.equals(success));
			return true;
		});
	}

	private void saveTarget(@NonNull Linkable target, @Nullable CallbackWithObject<Boolean> callback) {
		if (target instanceof FavouritePoint) {
			app.getFavoritesHelper().saveCurrentPointsIntoFile(true, new FavoritesListener() {
				@Override
				public void onSavingFavoritesFinished(boolean success) {
					notifyResult(callback, success);
				}
			});
		} else if (target instanceof WptPt wpt) {
			SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedGPXFile(wpt);
			if (selectedGpxFile != null) {
				SaveGpxHelper.saveGpx(selectedGpxFile.getGpxFile(), error -> notifyResult(callback, error == null));
			} else {
				notifyResult(callback, false);
			}
		} else {
			LOG.warn("Unsupported Linkable type, links not persisted: " + target.getClass().getName());
			notifyResult(callback, false);
		}
	}

	/** Saves replacement links before renaming an internal file, restoring the links on failure. */
	public void renameMedia(@Nullable Recording recording, @NonNull String href, @NonNull String name,
	                        @NonNull Map<Linkable, List<Link>> linksByTarget,
	                        @NonNull CallbackWithObject<Boolean> callback) {
		String path = LinkMediaFactory.getInternalPath(href);
		if (path == null || name.trim().isEmpty() || name.equals(".") || name.equals("..")
				|| name.matches(".*[\\\\/:*?\"<>|\\p{Cntrl}].*")) {
			notifyResult(callback, false);
			return;
		}
		File source = MediaProvider.resolveInternalMediaFile(app.getAppPath().getAbsolutePath(), path);
		String extension = Algorithms.getFileNameExtension(source.getName());
		String newName = recording != null ? name + " " + recording.getOtherName(source.getName())
				: name + (extension.isEmpty() ? "" : "." + extension);
		File destination = new File(source.getParentFile(), newName);
		if (source.equals(destination)) {
			notifyResult(callback, true);
			return;
		}
		if (!source.isFile() || destination.exists()) {
			notifyResult(callback, false);
			return;
		}
		Map<Link, Link> originals = new IdentityHashMap<>();
		String newHref = mediaStorageHelper.createMediaFileHref(destination);
		for (List<Link> links : linksByTarget.values()) {
			for (Link link : links) {
				originals.putIfAbsent(link, new Link(link));
				link.setHref(newHref);
				link.setText(newName);
			}
		}
		List<Linkable> targets = new ArrayList<>(linksByTarget.keySet());
		saveTargets(targets, 0, true, saved -> {
			boolean renamed = Boolean.TRUE.equals(saved) && !destination.exists()
					&& (recording != null ? recording.setName(name) : source.renameTo(destination));
			if (renamed) {
				mediaStorageHelper.scanMediaFile(source);
				mediaStorageHelper.scanMediaFile(destination);
				for (Linkable target : targets) notifyMediaChanged(target);
				notifyResult(callback, true);
			} else {
				for (Map.Entry<Link, Link> entry : originals.entrySet()) {
					entry.getKey().setHref(entry.getValue().getHref());
					entry.getKey().setText(entry.getValue().getText());
				}
				saveTargets(targets, 0, true, restored -> {
					if (!Boolean.TRUE.equals(restored)) LOG.warn("Failed to restore media links after rename failure");
					notifyResult(callback, false);
					return true;
				});
			}
			return true;
		});
	}

	private void notifyMediaChanged(@NonNull Linkable target) {
		app.runInUIThread(() -> {
			GalleryKey key = null;
			if (target instanceof FavouritePoint point) {
				key = new GalleryKey.Favorite(point.getKey());
			} else if (target instanceof WptPt point) {
				SelectedGpxFile selected = app.getSelectedGpxHelper().getSelectedGPXFile(point);
				if (selected != null) key = new GalleryKey.Waypoint(selected.getGpxFile().getPath(), point.getKey());
			}
			if (key != null) app.getGalleryHelper().notifyAttachedMediaChanged(java.util.Collections.singleton(key));
		});
	}

	private void saveTargets(@NonNull List<Linkable> targets, int index, boolean success,
	                         @NonNull CallbackWithObject<Boolean> callback) {
		if (index == targets.size()) {
			notifyResult(callback, success);
			return;
		}
		saveTarget(targets.get(index), saved -> {
			app.runInUIThread(() -> saveTargets(targets, index + 1, success && Boolean.TRUE.equals(saved), callback));
			return true;
		});
	}

	private static void notifyResult(@Nullable CallbackWithObject<Boolean> callback, boolean success) {
		if (callback != null) {
			callback.processResult(success);
		}
	}

	@NonNull
	public Link createRecordingLink(@NonNull Recording recording) {
		return new Link(getRecordingHref(recording), recording.getName(app, false), getRecordingMimeType(recording));
	}

	public void convertRecordingsToFavorites(@NonNull Collection<Recording> recordings) {
		if (recordings.isEmpty()) {
			logDebug("Media recordings conversion skipped: empty recordings");
			return;
		}

		logDebug("Media recordings conversion requested: recordings=" + recordings.size());
		FavouritesHelper favouritesHelper = app.getFavoritesHelper();
		if (!favouritesHelper.isFavoritesLoaded()) {
			List<Recording> pendingRecordings = new ArrayList<>(recordings);
			favouritesHelper.addListener(new FavoritesListener() {
				@Override
				public void onFavoritesLoaded() {
					favouritesHelper.removeListener(this);
					convertLoadedRecordingsToFavorites(favouritesHelper, pendingRecordings);
				}
			});
			return;
		}
		convertLoadedRecordingsToFavorites(favouritesHelper, recordings);
	}

	private void convertLoadedRecordingsToFavorites(@NonNull FavouritesHelper favouritesHelper, @NonNull Collection<Recording> recordings) {
		Set<String> usedFavoriteNames = getMediaFavoriteNames(favouritesHelper);
		Set<String> existingRecordingLinks = getMediaRecordingLinks(favouritesHelper);
		boolean changed = false;
		AddFavoriteOptions options = new AddFavoriteOptions();
		for (Recording recording : recordings) {
			if (!recording.getFile().exists()) {
				continue;
			}
			String key = mediaLinkKey(getRecordingHref(recording));
			if (key != null && !existingRecordingLinks.contains(key)) {
				FavouritePoint favorite = createMediaFavorite(recording, usedFavoriteNames);
				favorite.addLink(createRecordingLink(recording));
				if (favouritesHelper.addFavourite(favorite, options) == AddFavoriteResult.ADDED) {
					existingRecordingLinks.add(key);
					changed = true;
				}
			}
		}

		if (changed) {
			favouritesHelper.sortAll();
			favouritesHelper.saveCurrentPointsIntoFile(false);
			logDebug("Media recordings converted to favorites: recordings=" + recordings.size());
		}
	}

	private static void logDebug(@NonNull String message) {
		if (PluginsHelper.isDevelopment()) {
			LOG.debug(message);
		}
	}

	@NonNull
	private FavouritePoint createMediaFavorite(@NonNull Recording recording, @NonNull Set<String> usedFavoriteNames) {
		return new FavouritePoint(recording.getLatitude(), recording.getLongitude(),
				getUniqueMediaFavoriteName(recording, usedFavoriteNames), MEDIA_FAVORITES_GROUP);
	}

	@NonNull
	private Set<String> getMediaFavoriteNames(@NonNull FavouritesHelper favouritesHelper) {
		Set<String> res = new HashSet<>();
		for (FavouritePoint point : favouritesHelper.getFavouritePoints()) {
			if (MEDIA_FAVORITES_GROUP.equals(point.getCategory())) {
				res.add(point.getName());
			}
		}
		return res;
	}

	@NonNull
	private Set<String> getMediaRecordingLinks(@NonNull FavouritesHelper favouritesHelper) {
		Set<String> res = new HashSet<>();
		for (FavouritePoint point : favouritesHelper.getFavouritePoints()) {
			List<Link> links = point.getLinks();
			if (links != null) {
				for (Link link : links) {
					String key = mediaLinkKey(link.getHref());
					if (key != null) {
						res.add(key);
					}
				}
			}
		}
		return res;
	}

	@Nullable
	private String mediaLinkKey(@Nullable String href) {
		if (Algorithms.isEmpty(href)) {
			return null;
		}
		String internalPath = LinkMediaFactory.getInternalPath(href);
		if (internalPath != null) {
			String name = LinkMediaFactory.getInternalMediaFileName(internalPath);
			if (!Algorithms.isEmpty(name)) {
				return "internal:" + name;
			}
		}
		return href;
	}

	@NonNull
	private String getUniqueMediaFavoriteName(@NonNull Recording recording, @NonNull Set<String> usedFavoriteNames) {
		String baseName = recording.getName(app, true);
		if (Algorithms.isEmpty(baseName)) {
			baseName = recording.getFileName();
		}
		String name = baseName;
		int index = 2;
		while (usedFavoriteNames.contains(name)) {
			name = baseName + " (" + index++ + ")";
		}
		usedFavoriteNames.add(name);
		return name;
	}

	@NonNull
	private String getRecordingMimeType(@NonNull Recording recording) {
		if (recording.isPhoto()) {
			return "image/jpeg";
		} else if (recording.isVideo()) {
			return "video/mp4";
		} else if (recording.isAudio()) {
			return "audio/3gpp";
		}
		return "*/*";
	}

	@NonNull
	private String getRecordingHref(@NonNull Recording recording) {
		return mediaStorageHelper.createMediaFileHref(recording.getFile());
	}
}