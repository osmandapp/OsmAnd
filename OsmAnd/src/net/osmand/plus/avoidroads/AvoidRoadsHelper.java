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
import net.osmand.binary.RouteDataObject;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AvoidRoadsHelper {

	private static final Log LOG = PlatformUtil.getLog(AvoidRoadsHelper.class);

	public static final int FROM_URL = 101;
	public static final int FROM_STORAGE = 100;

	private static boolean saveResultToFile = true;

	private final Map<Long, Location> roadsToAvoid;
	private final List<Location> parsedPoints;

	private final OsmandApplication app;
	private final ApplicationMode appMode;
	private final CompletionCallback completionCallback;

	private ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

//	String inputFileName = "points_500.json";
//	String inputFileName = "point_100.json";
	String inputFileName = "points_10.json";

	public AvoidRoadsHelper(final OsmandApplication app) {
		this.app = app;
		this.appMode = app.getSettings().getApplicationMode();
		this.roadsToAvoid = new LinkedHashMap<>();
		this.parsedPoints = new ArrayList<>();

		completionCallback = new CompletionCallback() {
			@Override
			public void onRDOSearchComplete() {

				if (saveResultToFile) {
					File out = new File (app.getAppPath(IndexConstants.AVOID_ROADS_DIR).getAbsolutePath() + "/"
							+ "processed_ids.json");
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
//		File in = new File(app.getAppPath(IndexConstants.AVOID_ROADS_DIR).getAbsolutePath()  + "/" + inputFileName);
//		LOG.debug(String.format("Input json: %s", in.getAbsolutePath()));
//		if (in.exists()) {
//			loadJson(in.getAbsolutePath(), FROM_STORAGE);
//		}
		//String url = "https://gist.githubusercontent.com/MadWasp79/1238d8878792572e343eb2e296c3c7f5/raw/494f872425993797c3a3bc79a4ec82039db6ee46/point_100.json";
		String url = "https://gist.githubusercontent.com/MadWasp79/45f362ea48e9e8edd1593113593993c5/raw/6e817fb3bc7eaeaa3eda24847fde4855eb22485d/points_500.json";
		LOG.debug(String.format("Loading json from url: %s", url));
		loadJson(url, FROM_URL);
	}

	public void testRunDownload() {
		String url = "https://gist.githubusercontent.com/MadWasp79/1238d8878792572e343eb2e296c3c7f5/raw/494f872425993797c3a3bc79a4ec82039db6ee46/point_100.json";
		LOG.debug(String.format("Loading json from url: %s", url));
		loadJson(url, FROM_URL);
	}


	public void convertPointsToRDO(final List<Location> parsedPoints) {
		final Map<Long, Location> avoidedRoads = new HashMap<>();
		final long timeStart = System.currentTimeMillis();
		final int[] count = {0};
		this.roadsToAvoid.clear();
//		for (final Location point : parsedPoints) {
//
//			app.getLocationProvider().getRouteSegment(point, appMode, false, new ResultMatcher<RouteDataObject>() {
//				@Override
//				public boolean publish(RouteDataObject result) {
//					count[0]++;
//					if (result == null) {
//						LOG.error("Error! Find no result for point []");
//					} else {
//						avoidedRoads.put(result.id, point);
//					}
//					if (count[0] == parsedPoints.size()) {
//						completionCallback.onRDOSearchComplete();
//					}
//					if (count[0]%10 == 0) {
//						app.showShortToastMessage(String.format("Found %d roads", count[0]));
//					}
//					return true;
//				}
//
//				@Override
//				public boolean isCancelled() {
//					return false;
//				}
//			});
//		}

		app.getLocationProvider().getMultipleRouteSegmentsIds(parsedPoints, appMode, false,
				new ResultMatcher<Map<Long, Location>>() {
			@Override
			public boolean publish(Map<Long, Location> result) {

				if (result == null || result.isEmpty()) {
					LOG.error("Error! No valid result");
				} else {
					roadsToAvoid.putAll(result);
					LOG.debug(String.format("Found %d road ids", result.size()));
					completionCallback.onRDOSearchComplete();
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
	private void loadJson(final String path, final int source) {
		new AsyncTask<Void, Void, List<Location>>() {
			@Override
			protected List<Location> doInBackground(Void... voids) {
				InputStream is = null;
				try {
					switch(source) {
						case FROM_STORAGE:
							is = new FileInputStream(path);
							break;
						case FROM_URL:
							URLConnection connection = NetworkUtils.getHttpURLConnection(path);
							is = connection.getInputStream();
							break;
					}
					List<Location> result = new ArrayList<>();
					parsePointsFromJson(is, result);
					return result;
				} catch (Exception e) {
					LOG.error("Error reading json !");
				} finally {
					if (is != null) {
						try {
							is.close();
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
					completionCallback.onPointsParsed(result);
				}
			}
		}.executeOnExecutor(singleThreadExecutor);
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

	private void parsePointsFromJson(InputStream is, List<Location> result) {
		Gson gson = new Gson();
		GeoJSON geoJSON = gson.fromJson(new BufferedReader(new InputStreamReader(is)), GeoJSON.class);
		double minlat = 0 , maxlat = 0, minlon = 0, maxlon= 0;
		boolean first = true;
		for (Point o : geoJSON.points) {
			Location ll = new Location("geoJSON");
			double lat = o.geo.coordinates.get(1);
			double lon = o.geo.coordinates.get(0);
			if(first) {
				minlat = maxlat = lat;
				minlon = maxlon = lon;
				first = false;
			} else {
				minlat = Math.min(minlat, lat);
				minlon = Math.min(minlon, lon);
				maxlat = Math.max(maxlat, lat);
				maxlon = Math.max(maxlon, lon);
			}
			ll.setLatitude(lat);
			ll.setLongitude(lon);
			result.add(ll);
		}
		QuadRect qr = new QuadRect(minlon, minlat, maxlon, maxlat);
		QuadTree<Location> qt = new QuadTree<Location>(qr, 8, 0.55f);
		for(Location l : result) {
			qt.insert(l, (float)l.getLongitude(), (float) l.getLatitude());
		}
		qt.queryInBox(qr, result);

		app.showShortToastMessage(String.format("Loaded and parsed %d avoid points from JSON. Starting segment search.", result.size()));
		LOG.debug(String.format("Points parsed: %d", result.size()));
	}


	interface CompletionCallback {
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
