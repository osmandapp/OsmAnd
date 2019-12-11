package net.osmand.plus.avoidroads;

import android.annotation.SuppressLint;
import android.os.AsyncTask;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AvoidRoadsHelper {

	private static final Log LOG = PlatformUtil.getLog(AvoidRoadsHelper.class);

	private static int[] foundObjCount;
	private static boolean saveResultToFile = true;

	private final Map<Long, Location> roadsToAvoid;
	private final List<Location> parsedPoints;

	private final OsmandApplication app;
	private final ApplicationMode appMode;
	private final RDOSearchCompleteCallback rdoSearchCompleteCallback;

	private ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

//	String inputFileName = "points_500.json";
	String inputFileName = "point_100.json";
//	String inputFileName = "points_10.json";

	public AvoidRoadsHelper(final OsmandApplication app) {
		this.app = app;
		this.appMode = app.getSettings().getApplicationMode();
		this.roadsToAvoid = new LinkedHashMap<>();
		this.parsedPoints = new ArrayList<>();
		foundObjCount = new int[] {0, 0};

		rdoSearchCompleteCallback = new RDOSearchCompleteCallback() {
			@Override
			public void onRDOSearchComplete() {
				if (saveResultToFile) {
					File out = new File (app.getAppPath(IndexConstants.AVOID_ROADS_DIR).getAbsolutePath() + "/"
							+ inputFileName.substring(0, inputFileName.lastIndexOf(".")) + "_out.json");
					saveRoadsToJson(roadsToAvoid, out);
				}
			}

			@Override
			public void onPointsParsed(List<Location> result) {
				parsedPoints.addAll(result);
				convertPointsToRDO(parsedPoints);

			}
		};
	}

	public void testRun() {
		File in = new File(app.getAppPath(IndexConstants.AVOID_ROADS_DIR).getAbsolutePath()  + "/" + inputFileName);
		LOG.debug(String.format("Input json: %s", in.getAbsolutePath()));
		if (in.exists()) {
			parsePointsFromJson(in.getAbsolutePath());
		}
	}

	@SuppressLint("StaticFieldLeak")
	private void saveRoadsToJson(final Map<Long, Location> roadsToAvoid, final File out) {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... voids) {
				FileWriter fileWriter = null;
				if (out.exists()) {
					out.delete();
				}
				try {
					Gson gson = new Gson();
					fileWriter = new FileWriter(out, true);
					gson.toJson(new RoadsToAvoid (roadsToAvoid), fileWriter);
					fileWriter.close();
					LOG.info(String.format("File saved: %s ", out.getAbsolutePath()));
				} catch (Exception e) {
					//inform user about error
					LOG.error("Error writing file");
				} finally {
					if (fileWriter != null) {
						try {
							fileWriter.close();
						} catch (IOException e) {
						}
					}
				}
				return null;
			}
		}.executeOnExecutor(singleThreadExecutor);

	}

	public void convertPointsToRDO(List<Location> parsedPoints) {

		this.roadsToAvoid.clear();

		app.getLocationProvider().getMultipleRouteSegmentsIds(parsedPoints, appMode, false, new ResultMatcher<Map<Long, Location>>() {
			@Override
			public boolean publish(Map<Long, Location> result) {

				if (result == null || result.isEmpty()) {
					LOG.error("Error! No valid result");
				} else {
					roadsToAvoid.putAll(result);
					LOG.debug(String.format("Found %d road ids", result.size()));

					rdoSearchCompleteCallback.onRDOSearchComplete();

				}
				return true;
			}
			@Override
			public boolean isCancelled() {
				return false;
			}
		});

	}

	@SuppressLint("StaticFieldLeak")
	private void parsePointsFromJson(final String pointsJson) {

		new AsyncTask<String, Void, List<Location>>() {
			@Override
			protected List<Location> doInBackground(String... file) {
				FileReader fr = null;
				try {
					List<Location> result = new ArrayList<>();
					fr = new FileReader(pointsJson);
					Gson gson = new Gson();
					GeoJSON geoJSON = gson.fromJson(fr, GeoJSON.class);
					for (Point o : geoJSON.points) {
						Location ll = new Location("geoJSON");
						ll.setLatitude(o.geo.coordinates.get(1));
						ll.setLongitude(o.geo.coordinates.get(0));
						result.add(ll);
					}
					LOG.debug(String.format("Points parsed: %d", result.size()));
					return result;
				} catch (Exception e) {
					LOG.error("Error reading json file!");
				} finally {
					if (fr != null) {
						try {
							fr.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(List<Location> result) {
				if (!Algorithms.isEmpty(result)) {
					rdoSearchCompleteCallback.onPointsParsed(result);
				}
			}
		}.executeOnExecutor(singleThreadExecutor);
	}




	interface RDOSearchCompleteCallback {
		void onRDOSearchComplete();
		void onPointsParsed(List<Location> result);
	}

	class GeoJSON {

		@SerializedName("type")
		@Expose
		String type;
		@SerializedName("features")
		@Expose
		List<Point> points = null;

	}

	class Point {

		@SerializedName("type")
		@Expose
		String type;
		@SerializedName("geometry")
		@Expose
		Geometry geo;
	}


	class Geometry {

		@SerializedName("type")
		@Expose
		String type;
		@SerializedName("coordinates")
		@Expose
		List<Double> coordinates = null;

	}

	class RoadsToAvoid {
		@SerializedName("avoid_roads")
		@Expose
		List<RoadToAvoid> roadsToAvoid;

		public RoadsToAvoid(Map<Long, Location> roads) {
			this.roadsToAvoid = new ArrayList<>();
			for (Map.Entry<Long, Location> road : roads.entrySet()) {
				roadsToAvoid.add(new RoadToAvoid (road.getKey(), road.getValue().getLatitude(), road.getValue().getLongitude()));
			}
		}
	}

	class RoadToAvoid {

		@SerializedName("road_id")
		@Expose
		long roadId;
		@SerializedName("lat")
		@Expose
		double lat;
		@SerializedName("lon")
		@Expose
		double lon;

		RoadToAvoid(long roadId, double lat, double lon) {
			this.roadId = roadId;
			this.lat = lat;
			this.lon = lon;
		}
	}

}
