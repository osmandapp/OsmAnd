package net.osmand.plus.avoidroads;

import static net.osmand.IndexConstants.AVOID_ROADS_FILE_EXT;
import static net.osmand.IndexConstants.ROUTING_PROFILES_DIR;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.osm.edit.Node;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AvoidRoadsHelper {

	private static final Log log = PlatformUtil.getLog(AvoidRoadsHelper.class);

	private final OsmandApplication app;
	private final ListStringPreference preference;

	public AvoidRoadsHelper(@NonNull OsmandApplication app) {
		this.app = app;

		preference = app.getSettings().registerStringListPreference("avoid_roads_files", null, ",");
		preference.makeProfile().cache();
	}

	@NonNull
	public List<File> collectAvoidRoadsFiles() {
		List<File> avoidRoadsFiles = new ArrayList<>();
		File dir = app.getAppPath(ROUTING_PROFILES_DIR);
		File[] files = dir.listFiles();
		if (files != null && files.length > 0) {
			for (File file : files) {
				if (file.isFile() && file.getName().endsWith(AVOID_ROADS_FILE_EXT)) {
					avoidRoadsFiles.add(file);
				}
			}
		}
		return avoidRoadsFiles;
	}

	@Nullable
	public QuadTree<Node> getDirectionPoints(@NonNull ApplicationMode mode) {
		QuadTree<Node> directionPoints = null;

		List<String> selectedFiles = getSelectedFilesForMode(mode);
		if (!Algorithms.isEmpty(selectedFiles)) {
			File[] files = app.getAppPath(IndexConstants.ROUTING_PROFILES_DIR).listFiles();
			if (files != null && files.length > 0) {
				for (File file : files) {
					String fileName = file.getName();
					if (fileName.endsWith(AVOID_ROADS_FILE_EXT) && selectedFiles.contains(fileName)) {
						if (directionPoints == null) {
							QuadRect rect = new QuadRect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
							directionPoints = new QuadTree<>(rect, 15, 0.5f);
						}
						try {
							parseDirectionPointsForFile(file, directionPoints);
						} catch (JSONException | IOException e) {
							log.error("Error parsing file: " + fileName, e);
						}
					}
				}
			}
		}
		return directionPoints;
	}

	public void getDirectionPointsForFileAsync(@NonNull File file, @Nullable CallbackWithObject<QuadTree<Node>> callback) {
		new AsyncTask<Object, Object, QuadTree<Node>>() {

			@Override
			protected QuadTree<Node> doInBackground(Object[] objects) {
				QuadRect rect = new QuadRect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
				QuadTree<Node> directionPoints = new QuadTree<>(rect, 15, 0.5f);
				try {
					parseDirectionPointsForFile(file, directionPoints);
				} catch (JSONException | IOException e) {
					log.error("Error parsing file: " + file.getName(), e);
				}
				return directionPoints;
			}

			@Override
			protected void onPostExecute(QuadTree<Node> o) {
				if (callback != null) {
					callback.processResult(o);
				}
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
	}

	private void parseDirectionPointsForFile(@NonNull File file, @NonNull QuadTree<Node> directionPoints) throws JSONException, IOException {
		StringBuilder json = Algorithms.readFromInputStream(new FileInputStream(file));
		JSONObject jsonObject = new JSONObject(json.toString());
		JSONArray array = jsonObject.getJSONArray("features");
		for (int i = 0; i < array.length(); i++) {
			JSONObject object = array.getJSONObject(i);
			JSONObject geometry = object.getJSONObject("geometry");
			JSONObject properties = object.getJSONObject("properties");

			JSONArray coordinates = geometry.getJSONArray("coordinates");
			double lon = coordinates.getDouble(0);
			double lat = coordinates.getDouble(1);

			Node node = new Node(lat, lon, -1);
			int x = MapUtils.get31TileNumberX(lon);
			int y = MapUtils.get31TileNumberY(lat);

			for (Iterator<String> iterator = properties.keys(); iterator.hasNext(); ) {
				String key = iterator.next();
				String value = properties.getString(key);
				node.putTag(key, value);
			}
			directionPoints.insert(node, new QuadRect(x, y, x, y));
		}
	}

	public void setSelectedFilesForMode(@NonNull ApplicationMode appMode, @NonNull List<String> enabledFiles) {
		preference.setModeValues(appMode, enabledFiles);
	}

	@Nullable
	public List<String> getSelectedFilesForMode(@NonNull ApplicationMode appMode) {
		return preference.getStringsListForProfile(appMode);
	}
}
