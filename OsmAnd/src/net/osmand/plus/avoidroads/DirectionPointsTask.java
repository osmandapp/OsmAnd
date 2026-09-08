package net.osmand.plus.avoidroads;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.PlatformUtil;
import net.osmand.shared.data.KQuadRect;
import net.osmand.shared.data.KQuadTree;
import net.osmand.shared.routing.DirectionPoint;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

public class DirectionPointsTask extends AsyncTask<Void, Void, KQuadTree<DirectionPoint>> {

	private static final Log log = PlatformUtil.getLog(DirectionPointsHelper.class);

	private final File file;
	private final CallbackWithObject<KQuadTree<DirectionPoint>> callback;

	public DirectionPointsTask(@NonNull File file, @Nullable CallbackWithObject<KQuadTree<DirectionPoint>> callback) {
		this.file = file;
		this.callback = callback;
	}

	@Override
	protected KQuadTree<DirectionPoint> doInBackground(Void... voids) {
		KQuadRect rect = new KQuadRect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
		KQuadTree<DirectionPoint> directionPoints = new KQuadTree<>(rect, 15, 0.5f);
		try {
			parseDirectionPointsForFile(file, directionPoints);
		} catch (JSONException | IOException e) {
			log.error("Error parsing file: " + file.getName(), e);
		}
		return directionPoints;
	}

	public static void parseDirectionPointsForFile(@NonNull File file, @NonNull KQuadTree<DirectionPoint> directionPoints) throws JSONException, IOException {
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

			DirectionPoint point = new DirectionPoint(lat, lon);
			int x = MapUtils.get31TileNumberX(lon);
			int y = MapUtils.get31TileNumberY(lat);

			for (Iterator<String> iterator = properties.keys(); iterator.hasNext(); ) {
				String key = iterator.next();
				String value = properties.getString(key);
				point.putTag(key, value);
			}
			directionPoints.insert(point, new KQuadRect(x, y, x, y));
		}
	}

	@Override
	protected void onPostExecute(KQuadTree<DirectionPoint> o) {
		if (callback != null) {
			callback.processResult(o);
		}
	}
}