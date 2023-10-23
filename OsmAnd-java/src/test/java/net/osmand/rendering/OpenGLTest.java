package net.osmand.rendering;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.router.RouteResultPreparationTest;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class OpenGLTest {

	private Map<Integer, Integer> DISTANCES_TABLE;
	private final int DISTANCE_ZOOM = 15;
	private final int MAX_ZOOM = 21;
	private final int MAX_DISTANCE_IN_METERS = 300;//for 15 zoom
	protected Log log = PlatformUtil.getLog(OpenGLTest.class);
	private final String PATH_TO_RESOURCES = "src/test/resources/rendering/";
	private final String TES_JSON = "/test_3d_rendering.json";
	private CummulativeException cummulativeException;

	@Test
	public void testRendering() {
		String eyepiecePath = System.getProperty("eyepiece");
		if (eyepiecePath == null) {
			eyepiecePath = System.getenv("eyepiece");
		}
		if (eyepiecePath == null || eyepiecePath.isEmpty()) {
			return;
		}
		List<String> commands = generateCommands(eyepiecePath);
		for (String cmd : commands) {
			try {
				runCommand(cmd);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		initDistanceTable();
		try {
			test();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private void initDistanceTable() {
		if (DISTANCES_TABLE == null) {
			DISTANCES_TABLE = new HashMap<>();
			for (int i = 0; i <= MAX_ZOOM; i++) {
				double coef = Math.pow(2, DISTANCE_ZOOM - i);
				DISTANCES_TABLE.put(i, (int) (coef * MAX_DISTANCE_IN_METERS));
			}
		}
	}

	private List<String> generateCommands(String eyepiecePath) {
		Reader reader = new InputStreamReader(Objects.requireNonNull(RouteResultPreparationTest.class.getResourceAsStream(TES_JSON)));
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonArray arr = gson.fromJson(reader, JsonArray.class);
		LinkedList<String> res = new LinkedList<>();
		for (int i = 0; i < arr.size(); i++) {
			EyepieceParams params = new EyepieceParams();
			JsonObject o = (JsonObject) arr.get(i);
			JsonObject center = o.getAsJsonObject("center");
			assert(center != null);
			params.latitude = center.getAsJsonPrimitive("latitude").getAsDouble();
			params.longitude = center.getAsJsonPrimitive("longitude").getAsDouble();

			parseVisibilityZoom(o.getAsJsonArray("icons"), params);
			parseVisibilityZoom(o.getAsJsonArray("textOnPath"), params);
			parseVisibilityZoom(o.getAsJsonArray("text"), params);

			JsonPrimitive eyepieceParams = o.getAsJsonPrimitive("eyepieceParams");
			if (eyepieceParams != null) {
				params.commandParams = eyepieceParams.getAsString();
			}

			JsonPrimitive testName = o.getAsJsonPrimitive("testName");
			assert(testName != null);
			params.testName = testName.getAsString();
			res.add(params.getCommand(eyepiecePath));
		}
		return res;
	}

	private void parseVisibilityZoom(JsonArray arr, EyepieceParams params) {
		if (arr == null) {
			return;
		}
		for (int i = 0; i < arr.size(); i++) {
			JsonObject obj = (JsonObject) arr.get(i);
			JsonObject zooms = obj.getAsJsonObject("visibilityZoom");
			Set<Map.Entry<String, JsonElement>> set = zooms.entrySet();
			for (Map.Entry<String, JsonElement> s : set) {
				int z = Integer.parseInt(s.getKey());
				params.registerZoom(z);
			}
		}
	}

	private static String getMapName(String testName) {
		String shortName = testName.substring(0, Math.min(testName.length(), 10));
		return shortName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
	}

	private void runCommand(String cmd) throws IOException {
		System.out.println("\n");
		System.out.println(cmd);
		System.out.println("\n");
		Process proc = Runtime.getRuntime().exec(cmd);
		BufferedReader stdInput = new BufferedReader(new
				InputStreamReader(proc.getInputStream()));

		BufferedReader stdError = new BufferedReader(new
				InputStreamReader(proc.getErrorStream()));

		String s = null;
		while ((s = stdInput.readLine()) != null) {
			System.out.println(s);
		}

		StringBuilder errors = new StringBuilder();
		while ((s = stdError.readLine()) != null) {
			System.out.println(s);
			if (!s.startsWith("WARNING")) {
				errors.append(s);
			}
		}
		if (!errors.toString().isEmpty()) {
			throw new RuntimeException(s);
		}
	}

	private void test() throws FileNotFoundException {
		cummulativeException = new CummulativeException();
		Reader reader = new InputStreamReader(Objects.requireNonNull(RouteResultPreparationTest.class.getResourceAsStream(TES_JSON)));
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonArray arr = gson.fromJson(reader, JsonArray.class);
		for (int i = 0; i < arr.size(); i++) {
			JsonObject o = (JsonObject) arr.get(i);
			JsonPrimitive testNamePrimitive = o.getAsJsonPrimitive("testName");
			assert(testNamePrimitive != null);
			String testName = testNamePrimitive.getAsString();
			cummulativeException.setCurrentTestName(testName);
			List<RenderedInfo> renderedInfo = parseRenderedJsonForMap(testName);
			if (renderedInfo.size() == 0) {
				throw new RuntimeException("File(s) is empty for test:" + testName);
			}
			List<TestInfo> testInfo = parseTestJson(o);
			compareTestAndRenderedInfo(testInfo, renderedInfo);
		}
		if (cummulativeException.hasExceptions()) {
			throw new IllegalStateException(cummulativeException.getFormatExceptions());
		}
	}

	private List<RenderedInfo> parseRenderedJsonForMap(String testName) throws FileNotFoundException {
		File jsonDir = new File(PATH_TO_RESOURCES + "/mapdata");
		assert(jsonDir.isDirectory());
		final String mapName = getMapName(testName);
		File[] jsonFiles = jsonDir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.getName().endsWith(".json") && pathname.getName().startsWith(mapName);
			}
		});
		if (jsonFiles.length == 0) {
			throw new RuntimeException("File(s) not found:" + mapName + "000x.json for test:" + testName);
		}
		List<RenderedInfo> renderedInfo = new ArrayList<>();
		for (File f : jsonFiles) {
			Gson gson = new Gson();
			JsonArray arr = gson.fromJson(new JsonReader(new FileReader(f)), JsonArray.class);
			renderedInfo.addAll(parseRenderedJson(arr));
		}
		return renderedInfo;
	}

	private List<RenderedInfo> parseRenderedJson(JsonArray arr) {
		if (arr == null) {
			return null;
		}
		List<RenderedInfo> info = new ArrayList();
		for (int i = 0; i < arr.size(); i++) {
			RenderedInfo ri = new RenderedInfo();
			JsonObject obj = (JsonObject) arr.get(i);
			String cl = obj.getAsJsonPrimitive("class").getAsString();
			String type = obj.getAsJsonPrimitive("type").getAsString();
			ri.setType(cl, type);
			ri.zoom = obj.getAsJsonPrimitive("zoom").getAsInt();
			ri.id = obj.getAsJsonPrimitive("id").getAsLong();
			double lat = obj.getAsJsonPrimitive("lat").getAsDouble();
			double lon = obj.getAsJsonPrimitive("lon").getAsDouble();
			ri.center = new LatLon(lat, lon);

			JsonObject start = obj.getAsJsonObject("startPoint");
			JsonObject end = obj.getAsJsonObject("endPoint");
			if (start != null && end != null) {
				lat = start.getAsJsonPrimitive("lat").getAsDouble();
				lon = start.getAsJsonPrimitive("lon").getAsDouble();
				ri.startPoint = new LatLon(lat, lon);
				lat = end.getAsJsonPrimitive("lat").getAsDouble();
				lon = end.getAsJsonPrimitive("lon").getAsDouble();
				ri.endPoint = new LatLon(lat, lon);
			}
			JsonPrimitive content = obj.getAsJsonPrimitive("content");
			if (content != null) {
				ri.content = content.getAsString();
			}
			info.add(ri);
		}
		return info;
	}

	private List<TestInfo> parseTestJson(JsonObject testJsonObj) {
		List<TestInfo> result = new ArrayList<>();
		parseTestJsonArr(testJsonObj.getAsJsonArray("icons"), result, RenderedType.ICON);
		parseTestJsonArr(testJsonObj.getAsJsonArray("text"), result, RenderedType.TEXT_ON_POINT);
		parseTestJsonArr(testJsonObj.getAsJsonArray("textOnPath"), result, RenderedType.TEXT_ON_LINE);
		return result;
	}

	private void parseTestJsonArr(JsonArray arr, List<TestInfo> result, RenderedType type) {
		if (arr == null) {
			return;
		}
		for (int i = 0; i < arr.size(); i++) {
			TestInfo testInfo = new TestInfo();
			JsonObject obj = (JsonObject) arr.get(i);
			if (obj.getAsJsonPrimitive("osmId") == null) {
				throw new RuntimeException("osmId not found");
			}
			if (obj.getAsJsonObject("visibilityZoom") == null) {
				throw new RuntimeException("visibilityZoom not found");
			}

			try {
				testInfo.id = obj.getAsJsonPrimitive("osmId").getAsLong();
			} catch (NumberFormatException e) {
				throw new RuntimeException("osmId is empty");
			}
			JsonObject zooms = obj.getAsJsonObject("visibilityZoom");
			Set<Map.Entry<String, JsonElement>> set = zooms.entrySet();
			for (Map.Entry<String, JsonElement> s : set) {
				int z = Integer.parseInt(s.getKey());
				String v = s.getValue().getAsString();
				boolean visible = "true".equals(v) || "yes".equals(v);
				if (visible) {
					testInfo.addVisibleZoom(z);
				} else {
					testInfo.addInVisibleZoom(z);
				}
			}

			JsonPrimitive lat = obj.getAsJsonPrimitive("latitude");
			JsonPrimitive lon = obj.getAsJsonPrimitive("longitude");
			if (lat != null && lon != null) {
				testInfo.center = new LatLon(lat.getAsDouble(), lon.getAsDouble());
			}

			lat = obj.getAsJsonPrimitive("lat");
			lon = obj.getAsJsonPrimitive("lon");
			if (lat != null && lon != null) {
				testInfo.center = new LatLon(lat.getAsDouble(), lon.getAsDouble());
			}

			JsonPrimitive name = obj.getAsJsonPrimitive("name");
			if (name != null) {
				testInfo.text = name.getAsString();
			}

			JsonObject startPoint = obj.getAsJsonObject("startPoint");
			JsonObject endPoint = obj.getAsJsonObject("endPoint");
			if (startPoint != null && endPoint != null) {
				lat = startPoint.getAsJsonPrimitive("latitude");
				lon = startPoint.getAsJsonPrimitive("longitude");
				assert (lat != null && lon != null);
				testInfo.startPoint = new LatLon(lat.getAsDouble(), lon.getAsDouble());
				lat = endPoint.getAsJsonPrimitive("latitude");
				lon = endPoint.getAsJsonPrimitive("longitude");
				assert (lat != null && lon != null);
				testInfo.endPoint = new LatLon(lat.getAsDouble(), lon.getAsDouble());
			}

			testInfo.type = type;

			result.add(testInfo);
		}
	}

	private void compareTestAndRenderedInfo(List<TestInfo> testInfo, List<RenderedInfo> renderedInfo) {
		for (TestInfo t : testInfo) {
			checkOsmIdAndText(renderedInfo, t);
		}
	}

	private void checkOsmIdAndText(List<RenderedInfo> renderedInfo, TestInfo testInfo) {
		HashSet<Integer> checkedZooms = testInfo.visibleZooms;
		checkedZooms.addAll(testInfo.inVisibleZooms);
		for (RenderedInfo info : renderedInfo) {
			int zoom = info.zoom;
			if (!checkedZooms.contains(zoom)) {
				continue;
			}
			if (info.id == testInfo.id && testInfo.visibleZooms.contains(zoom)) {
				checkedZooms.remove(zoom);
				continue;
			}
			if (info.id == testInfo.id && testInfo.inVisibleZooms.contains(zoom)) {
				cummulativeException.addException("osmId:" + testInfo.id + " must be not visible on zoom:" + zoom);
				checkedZooms.remove(zoom);
				continue;
			}
			if (testInfo.type == RenderedType.TEXT_ON_LINE || testInfo.type == RenderedType.TEXT_ON_POINT) {
				if (info.content != null && info.content.equals(testInfo.text)) {
					LatLon c = null;
					LatLon c2 = null;
					if (testInfo.center != null) {
						c = testInfo.center;
					} else if (testInfo.startPoint != null && testInfo.endPoint != null) {
						c = MapUtils.calculateMidPoint(testInfo.startPoint, testInfo.endPoint);
					}
					if (info.startPoint != null && info.endPoint != null) {
						c2 = MapUtils.calculateMidPoint(info.startPoint, info.endPoint);
					} else if (info.center != null) {
						c2 = info.center;
					}
					if (c != null && c2 != null) {
						double dist = MapUtils.getDistance(c, c2);
						if (dist <= DISTANCES_TABLE.get(zoom)) {
							if (testInfo.inVisibleZooms.contains(zoom)) {
								cummulativeException.addException("text:" + testInfo.text + " must be not visible on zoom:" + zoom);
							}
						} else {
							cummulativeException.addException("text:" + testInfo.text + " is visible on zoom:" + zoom +
									", but too far from test location. Found location " + info.id + " " + c2.getLatitude() + " " + c2.getLongitude() +
									". Distance " + (int) dist + " meters");
						}
						checkedZooms.remove(zoom);
					}
				}
			}
			if (checkedZooms.size() == 0) {
				break;
			}
		}
		checkedZooms.removeAll(testInfo.inVisibleZooms);
		if (checkedZooms.size() > 0) {
			String name = testInfo.text != null ? " name:\"" + testInfo.text + "\"" : "";
			cummulativeException.addException("osmId:" + testInfo.id + name + " must be visible on zooms:" + checkedZooms.toString());
		}
	}

	private enum RenderedType {
		ICON,
		TEXT_ON_POINT,
		TEXT_ON_LINE
	}

	private class RenderedInfo {
		RenderedType type;
		String content;
		long id;
		LatLon center;
		LatLon startPoint;
		LatLon endPoint;
		int zoom;
		public void setType(String cl, String type) {
			if (cl.equals("icon")) {
				this.type = RenderedType.ICON;
			} else if (cl.equals("caption")) {
				if (type.equals("billboard")) {
					this.type = RenderedType.TEXT_ON_POINT;
				} else {
					this.type = RenderedType.TEXT_ON_LINE;
				}
			}
		}
	}

	private class TestInfo {
		long id;
		LatLon center;
		LatLon startPoint;
		LatLon endPoint;
		HashSet<Integer> visibleZooms = new HashSet<>();
		HashSet<Integer> inVisibleZooms = new HashSet<>();
		String text;
		RenderedType type;
		void addVisibleZoom(int zoom) {
			visibleZooms.add(zoom);
		}
		void addInVisibleZoom(int zoom) {
			inVisibleZooms.add(zoom);
		}
	}

	private class CummulativeException {
		Map<String, List<String>> exceptions;
		String currentTestName = "";

		void addException(String e) {
			if (exceptions == null) {
				exceptions = new HashMap<>();
			}
			if (currentTestName.isEmpty()) {
				throw new RuntimeException("Set test name");
			}
			if (exceptions.get(currentTestName) == null) {
				exceptions.put(currentTestName, new ArrayList<String>());
			}
			List<String> explist = exceptions.get(currentTestName);
			explist.add(e);
		}

		void setCurrentTestName(String testName) {
			currentTestName = testName;
		}

		boolean hasExceptions() {
			return exceptions.size() > 0;
		}

		String getFormatExceptions() {
			String res = "\n";
			for (Map.Entry<String, List<String>> entry : exceptions.entrySet()) {
				res += ">>>>" + entry.getKey() + "\n";
				for (String s : entry.getValue()) {
					res += "\t\t" + s + "\n";
				}
				res += "\n";
			}
			return res;
		}
	}

	private class EyepieceParams {
		String testName = "";
		int minZoom = 21;
		int maxZoom = 0;
		double latitude;
		double longitude;
		String commandParams = "";

		void registerZoom(int zoom) {
			minZoom = Math.min(minZoom, zoom);
			maxZoom = Math.max(maxZoom, zoom);
		}

		String getCommand(String eyepiecePath) {
			assert(minZoom < maxZoom);
			StringBuilder builder = new StringBuilder();
			builder.append(eyepiecePath + " -verbose ");
			if (!commandParams.contains("-obfsPath")) {
				builder.append("-obfsPath=" + PATH_TO_RESOURCES + "maps/ ");
			}
			if (!commandParams.contains("-geotiffPath")) {
				builder.append("-geotiffPath=" + PATH_TO_RESOURCES + "geotiffs/ ");
			}
			if (!commandParams.contains("-cachePath")) {
				builder.append("-cachePath=" + PATH_TO_RESOURCES + "cache/ ");
			}
			if (!commandParams.contains("-outputRasterWidth")) {
				builder.append("-outputRasterWidth=1024 ");
			}
			if (!commandParams.contains("-outputRasterHeight")) {
				builder.append("-outputRasterHeight=768 ");
			}
			if (!commandParams.contains("-outputImageFilename")) {
				builder.append("-outputImageFilename=" + PATH_TO_RESOURCES + "mapimage/" + getMapName(testName) + " ");
			}
			if (!commandParams.contains("-outputJSONFilename")) {
				builder.append("-outputJSONFilename=" + PATH_TO_RESOURCES + "mapdata/" + getMapName(testName) + " ");
			}
			if (!commandParams.contains("-latLon") && !commandParams.contains("-endLatLon")) {
				builder.append("-latLon=" + latitude + ":" + longitude + " ");
			}
			if (!commandParams.contains("-zoom") && !commandParams.contains("-endZoom")) {
				builder.append("-zoom=" + minZoom + " -endZoom=" + maxZoom + " ");
			}
			if (!commandParams.contains("-frames")) {
				int frames = maxZoom - minZoom + 1;
				builder.append("-frames=" + frames +  " ");
			}
			builder.append(commandParams);
			return builder.toString();
		}
	}

}
