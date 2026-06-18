package net.osmand.plus.gallery.attached.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.myplaces.favorites.FavoritesListener;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.myplaces.favorites.add.AddFavoriteOptions;
import net.osmand.plus.myplaces.favorites.add.AddFavoriteResult;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.audionotes.Recording;
import net.osmand.plus.settings.mediastorage.MediaStorageHelper;
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

public class AttachedMediaDataHelper {

	private static final Log LOG = PlatformUtil.getLog(AttachedMediaDataHelper.class);

	public static final String MEDIA_FAVORITES_GROUP = "media";

	private final OsmandApplication app;
	private final MediaStorageHelper mediaStorageHelper;

	public AttachedMediaDataHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.mediaStorageHelper = new MediaStorageHelper(app);
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