package net.osmand.plus.gallery.helpers;

import static net.osmand.IndexConstants.AV_INDEX_DIR;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.FavouritePoint;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.audionotes.Recording;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.track.helpers.save.SaveGpxHelper;
import net.osmand.shared.gpx.primitives.Link;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.shared.media.LinkMediaFactory;

import java.util.Collections;
import java.util.List;

public class AttachedMediaDataHelper {

	private final OsmandApplication app;

	public AttachedMediaDataHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public void addRecordingLink(@Nullable Object object, @Nullable Recording recording, @Nullable Runnable onMediaChanged) {
		if (recording != null) {
			addMediaLinks(object, Collections.singletonList(createRecordingLink(recording)), onMediaChanged);
		}
	}

	public void addMediaLinks(@Nullable Object object, @NonNull List<Link> links, @Nullable Runnable onMediaChanged) {
		if (links.isEmpty()) {
			return;
		}
		if (object instanceof FavouritePoint point) {
			for (int i = 0; i < links.size(); i++) {
				point.addLink(links.get(i));
			}
			app.getFavoritesHelper().saveCurrentPointsIntoFile(true);
		} else if (object instanceof WptPt wpt) {
			for (int i = 0; i < links.size(); i++) {
				wpt.addLink(links.get(i));
			}
			SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedGPXFile(wpt);
			if (selectedGpxFile != null) {
				SaveGpxHelper.saveGpx(selectedGpxFile.getGpxFile());
			}
		} else {
			return;
		}
		if (onMediaChanged != null) {
			onMediaChanged.run();
		}
	}

	@Nullable
	public List<Link> getMediaLinks(@Nullable Object object) {
		if (object instanceof FavouritePoint point) {
			return point.getLinks();
		} else if (object instanceof WptPt wpt) {
			return wpt.getLinks();
		}
		return null;
	}

	@NonNull
	public Link createRecordingLink(@NonNull Recording recording) {
		return new Link(getRecordingHref(recording), recording.getName(app, false), getRecordingMimeType(recording));
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