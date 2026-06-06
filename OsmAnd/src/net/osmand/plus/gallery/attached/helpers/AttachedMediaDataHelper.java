package net.osmand.plus.gallery.attached.helpers;

import static net.osmand.IndexConstants.AV_INDEX_DIR;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.myplaces.favorites.add.AddFavoriteOptions;
import net.osmand.plus.myplaces.favorites.add.AddFavoriteResult;
import net.osmand.plus.plugins.audionotes.Recording;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.track.helpers.save.SaveGpxHelper;
import net.osmand.shared.gpx.primitives.Link;
import net.osmand.shared.gpx.primitives.Linkable;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.shared.media.LinkMediaFactory;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AttachedMediaDataHelper {

	private static final Log LOG = PlatformUtil.getLog(AttachedMediaDataHelper.class);

	public static final String MEDIA_FAVORITES_GROUP = "media";

	private final OsmandApplication app;

	public AttachedMediaDataHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public void addRecordingLink(@NonNull Linkable target, @Nullable Recording recording, @Nullable Runnable onMediaChanged) {
		if (recording != null) {
			addMediaLinks(target, Collections.singletonList(createRecordingLink(recording)), onMediaChanged);
		}
	}

	public void addMediaLinks(@NonNull Linkable target, @NonNull List<Link> links,
	                          @Nullable Runnable onMediaChanged) {
		if (links.isEmpty()) return;

		for (int i = 0; i < links.size(); i++) {
			target.addLink(links.get(i));
		}

		if (target instanceof FavouritePoint) {
			app.getFavoritesHelper().saveCurrentPointsIntoFile(true);
		} else if (target instanceof WptPt wpt) {
			SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedGPXFile(wpt);
			if (selectedGpxFile != null) {
				SaveGpxHelper.saveGpx(selectedGpxFile.getGpxFile());
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

	public void convertRecordingsToFavorites(@NonNull Collection<Recording> recordingsToConvert) {
		if (recordingsToConvert.isEmpty()) {
			return;
		}

		FavouritesHelper favouritesHelper = app.getFavoritesHelper();
		Set<String> usedFavoriteNames = getMediaFavoriteNames(favouritesHelper);
		Set<String> existingRecordingLinks = getMediaRecordingLinks(favouritesHelper);
		boolean changed = false;
		AddFavoriteOptions options = new AddFavoriteOptions();
		for (Recording recording : recordingsToConvert) {
			if (!recording.getFile().exists()) {
				continue;
			}
			String href = getRecordingHref(recording);
			if (!existingRecordingLinks.contains(href)) {
				FavouritePoint favorite = createMediaFavorite(recording, usedFavoriteNames);
				favorite.addLink(createRecordingLink(recording));
				if (favouritesHelper.addFavourite(favorite, options) == AddFavoriteResult.ADDED) {
					existingRecordingLinks.add(href);
					changed = true;
				}
			}
		}

		if (changed) {
			favouritesHelper.sortAll();
			favouritesHelper.saveCurrentPointsIntoFile(false);
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
					if (!Algorithms.isEmpty(link.getHref())) {
						res.add(link.getHref());
					}
				}
			}
		}
		return res;
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
		return LinkMediaFactory.createInternalUri(AV_INDEX_DIR + recording.getFileName());
	}
}