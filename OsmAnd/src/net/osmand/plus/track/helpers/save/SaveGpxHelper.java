package net.osmand.plus.track.helpers.save;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.myplaces.tracks.tasks.SaveCurrentTrackTask;
import net.osmand.shared.gpx.GpxFile;

import java.io.File;
import java.util.Objects;

public class SaveGpxHelper {

	public static void saveGpx(@NonNull GpxFile gpx) {
		saveGpx(gpx, null);
	}

	public static void saveGpx(@NonNull GpxFile gpx, @Nullable SaveGpxListener listener) {
		saveGpx(new File(gpx.getPath()), gpx, listener);
	}

	public static void saveGpx(@NonNull File file, @NonNull GpxFile gpx, @Nullable SaveGpxListener listener) {
		OsmAndTaskManager.executeTask(new SaveGpxAsyncTask(Objects.requireNonNull(file),
				Objects.requireNonNull(gpx), listener));
	}

	// TODO Do we need this? OsmandMonitoringPlugin.saveCurrentTrack() should be enough
	public static void saveCurrentTrack(@NonNull OsmandApplication app, @NonNull GpxFile gpx, @NonNull SaveGpxListener listener) {
		OsmAndTaskManager.executeTask(new SaveCurrentTrackTask(app, Objects.requireNonNull(gpx), listener));
	}
}