package net.osmand.plus.avoidroads;

import static net.osmand.IndexConstants.AVOID_ROADS_FILE_EXT;
import static net.osmand.IndexConstants.ROUTING_PROFILES_DIR;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.shared.data.KQuadRect;
import net.osmand.shared.data.KQuadTree;
import net.osmand.shared.routing.DirectionPoint;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DirectionPointsHelper {

	private static final Log log = PlatformUtil.getLog(DirectionPointsHelper.class);

	private final OsmandApplication app;
	private final ListStringPreference preference;

	public DirectionPointsHelper(@NonNull OsmandApplication app) {
		this.app = app;

		preference = app.getSettings().registerStringListPreference("avoid_roads_files", null, ",");
		preference.makeProfile().cache();
	}

	@NonNull
	public List<File> collectAvoidRoadsFiles() {
		List<File> avoidRoadsFiles = new ArrayList<>();
		File dir = app.getAppPath(ROUTING_PROFILES_DIR);
		File[] files = dir.listFiles();
		if (!Algorithms.isEmpty(files)) {
			for (File file : files) {
				if (file.isFile() && file.getName().endsWith(AVOID_ROADS_FILE_EXT)) {
					avoidRoadsFiles.add(file);
				}
			}
		}
		return avoidRoadsFiles;
	}

	@Nullable
	public KQuadTree<DirectionPoint> getDirectionPoints(@NonNull ApplicationMode mode) {
		KQuadTree<DirectionPoint> directionPoints = null;

		List<String> selectedFiles = getSelectedFilesForMode(mode);
		if (!Algorithms.isEmpty(selectedFiles)) {
			File[] files = app.getAppPath(ROUTING_PROFILES_DIR).listFiles();
			if (!Algorithms.isEmpty(files)) {
				for (File file : files) {
					String fileName = file.getName();
					if (fileName.endsWith(AVOID_ROADS_FILE_EXT) && selectedFiles.contains(fileName)) {
						if (directionPoints == null) {
							KQuadRect rect = new KQuadRect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
							directionPoints = new KQuadTree<>(rect, 15, 0.5f);
						}
						try {
							DirectionPointsTask.parseDirectionPointsForFile(file, directionPoints);
						} catch (JSONException | IOException e) {
							log.error("Error parsing file: " + fileName, e);
						}
					}
				}
			}
		}
		return directionPoints;
	}

	public void getDirectionPointsForFileAsync(@NonNull File file, @Nullable CallbackWithObject<KQuadTree<DirectionPoint>> callback) {
		OsmAndTaskManager.executeTask(new DirectionPointsTask(file, callback));
	}

	public void setSelectedFilesForMode(@NonNull ApplicationMode appMode, @NonNull List<String> enabledFiles) {
		preference.setModeValues(appMode, enabledFiles);
	}

	@Nullable
	public List<String> getSelectedFilesForMode(@NonNull ApplicationMode appMode) {
		return preference.getStringsListForProfile(appMode);
	}
}
