package net.osmand.rendering;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
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

public class OpenGLTest {

	private Map<Integer, Integer> DISTANCES_TABLE;
	private final int DISTANCE_ZOOM = 15;
	private final int MAX_ZOOM = 21;
	private final int MAX_DISTANCE_IN_METERS = 400;//for 15 zoom
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
		TestEntry[] arr = gson.fromJson(reader, TestEntry[].class);
		LinkedList<String> res = new LinkedList<>();
		for (TestEntry testEntry : arr) {
			EyepieceParams params = new EyepieceParams();
			assert(testEntry.center != null);
			params.latitude = testEntry.center.latitude;
			params.longitude = testEntry.center.longitude;

			parseVisibilityZoom(testEntry.icons, params);
			parseVisibilityZoom(testEntry.textOnPath, params);
			parseVisibilityZoom(testEntry.text, params);

			if (testEntry.eyepieceParams != null) {
				params.commandParams = testEntry.eyepieceParams;
			}
			assert(testEntry.testName != null);
			params.testName = testEntry.testName;
			res.add(params.getCommand(eyepiecePath));
		}
		return res;
	}

	private void parseVisibilityZoom(List<TestEntryUnit> unitList, EyepieceParams params) {
		if (unitList == null) {
			return;
		}
		for (TestEntryUnit testEntryUnit : unitList) {
			if (testEntryUnit.visibilityZoom == null) {
				continue;
			}
			for (Map.Entry<Integer, Boolean> entry : testEntryUnit.visibilityZoom.entrySet()) {
				params.registerZoom(entry.getKey());
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
		TestEntry[] arr = gson.fromJson(reader, TestEntry[].class);
		for (TestEntry testEntry : arr) {
			assert(testEntry.testName != null);
			cummulativeException.setCurrentTestName(testEntry.testName);
			List<RenderedInfo> renderedInfo = parseRenderedJsonForMap(testEntry.testName);
			if (renderedInfo.size() == 0) {
				throw new RuntimeException("File(s) is empty for test:" + testEntry.testName);
			}
			List<TestInfo> testInfo = parseTestJson(testEntry);
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
			RenderEntry[] arr = gson.fromJson(new JsonReader(new FileReader(f)), RenderEntry[].class);
			renderedInfo.addAll(parseRenderedJson(arr));
		}
		return renderedInfo;
	}

	private List<RenderedInfo> parseRenderedJson(RenderEntry[] arr) {
		if (arr == null) {
			return null;
		}
		List<RenderedInfo> info = new ArrayList();
		for (RenderEntry renderEntry : arr) {
			RenderedInfo ri = new RenderedInfo();
			assert(renderEntry.cl != null && renderEntry.type != null);
			ri.setType(renderEntry.cl, renderEntry.type);
			ri.zoom = renderEntry.zoom;
			ri.id = renderEntry.id;
			ri.center = new LatLon(renderEntry.lat, renderEntry.lon);
			if (renderEntry.startPoint != null && renderEntry.endPoint != null) {
				ri.startPoint = new LatLon(renderEntry.startPoint.lat, renderEntry.startPoint.lon);
				ri.endPoint = new LatLon(renderEntry.endPoint.lat, renderEntry.endPoint.lon);
			}
			ri.content = renderEntry.content;
			info.add(ri);
		}
		return info;
	}

	private List<TestInfo> parseTestJson(TestEntry testEntry) {
		List<TestInfo> result = new ArrayList<>();
		parseTestJsonArr(testEntry.icons, result, RenderedType.ICON);
		parseTestJsonArr(testEntry.text, result, RenderedType.TEXT_ON_POINT);
		parseTestJsonArr(testEntry.textOnPath, result, RenderedType.TEXT_ON_LINE);
		return result;
	}

	private void parseTestJsonArr(List<TestEntryUnit> unitList, List<TestInfo> result, RenderedType type) {
		if (unitList == null) {
			return;
		}
		for (TestEntryUnit testEntryUnit : unitList) {
			TestInfo testInfo = new TestInfo();
			if (testEntryUnit.visibilityZoom == null) {
				throw new RuntimeException("visibilityZoom not found");
			}

			if (testEntryUnit.osmId != null) {
				testInfo.id = testEntryUnit.osmId;
			}
			for (Map.Entry<Integer, Boolean> entry : testEntryUnit.visibilityZoom.entrySet()) {
				int z = entry.getKey();
				boolean visible = entry.getValue();
				if (visible) {
					testInfo.addVisibleZoom(z);
				} else {
					testInfo.addInVisibleZoom(z);
				}
			}

			if (testEntryUnit.latitude != null && testEntryUnit.longitude != null) {
				testInfo.center = new LatLon(testEntryUnit.latitude, testEntryUnit.longitude);
			}

			testInfo.text = testEntryUnit.name;
			if (testEntryUnit.startPoint != null && testEntryUnit.endPoint != null) {
				testInfo.startPoint = new LatLon(testEntryUnit.startPoint.latitude, testEntryUnit.startPoint.longitude);
				testInfo.endPoint = new LatLon(testEntryUnit.endPoint.latitude, testEntryUnit.endPoint.longitude);
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
		String name = testInfo.text != null ? " name:\"" + testInfo.text + "\"" : "";
		String type = testInfo.type == RenderedType.ICON ? "icon " : "text ";
		for (RenderedInfo info : renderedInfo) {
			int zoom = info.zoom;
			if (!checkedZooms.contains(zoom)) {
				continue;
			}
			boolean isEqualText = testInfo.type == RenderedType.TEXT_ON_LINE || testInfo.type == RenderedType.TEXT_ON_POINT;
			isEqualText &= info.content != null && info.content.equals(testInfo.text);
			if (isEqualText) {
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
							cummulativeException.addException("text \"" + testInfo.text + "\" must be not visible on zoom:" + zoom);
						}
					} else {
						cummulativeException.addException("text \"" + testInfo.text + "\" is visible on zoom:" + zoom +
								", but too far from test location. Found location " +
								String.format("%,.5f", c2.getLatitude()) + " " + String.format("%,.5f", c2.getLongitude()) +
								" (" + info.id + ") " +
								". Distance " + (int) dist + " meters. Maximum distance for zoom=" + zoom + " is " + DISTANCES_TABLE.get(zoom) + " meters");
					}
					checkedZooms.remove(zoom);
					continue;
				} else if (c == null) {
					//lat and lon is not set in the test, just check visibility
					if (testInfo.visibleZooms.contains(zoom)) {
						checkedZooms.remove(zoom);
						continue;
					} else if (testInfo.inVisibleZooms.contains(zoom)) {
						cummulativeException.addException("text \"" + testInfo.text + "\" must be not visible on zoom:" + zoom);
					}
				}
			}
			if (testInfo.type == RenderedType.ICON || isEqualText) {
				if (info.id == testInfo.id && testInfo.visibleZooms.contains(zoom)) {
					checkedZooms.remove(zoom);
					continue;
				}

				if (info.id == testInfo.id && testInfo.inVisibleZooms.contains(zoom)) {
					cummulativeException.addException(type + "osmId:" + testInfo.id + name + " must be not visible on zoom:" + zoom);
					checkedZooms.remove(zoom);
					continue;
				}
			}
			if (checkedZooms.size() == 0) {
				break;
			}
		}
		checkedZooms.removeAll(testInfo.inVisibleZooms);
		if (checkedZooms.size() > 0) {
			cummulativeException.addException(type + "osmId:" + testInfo.id + name + " must be visible on zooms:" + checkedZooms.toString());
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
		long id = -1;
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
		Map<String, List<String>> exceptions = new HashMap<>();
		String currentTestName = "";

		void addException(String e) {
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

	private class TestEntry {
		@SerializedName("testName")
		public String testName;
		@SerializedName("description")
		public String description;
		@SerializedName("center")
		public Coordinates center;
		@SerializedName("icons")
		public List<TestEntryUnit> icons;
		@SerializedName("text")
		public List<TestEntryUnit> text;
		@SerializedName("textOnPath")
		public List<TestEntryUnit> textOnPath;
		@SerializedName("eyepieceParams")
		public String eyepieceParams;
	}

	private class TestEntryUnit {
		@SerializedName("osmId")
		public Long osmId;
		@SerializedName("name")
		public String name;
		@SerializedName("latitude")
		public Double latitude;
		@SerializedName("longitude")
		public Double longitude;
		@SerializedName("visibilityZoom")
		Map<Integer, Boolean> visibilityZoom;
		@SerializedName("startPoint")
		Coordinates startPoint;
		@SerializedName("endPoint")
		Coordinates endPoint;
	}

	private class Coordinates {
		@SerializedName("latitude")
		public Double latitude;
		@SerializedName("longitude")
		public Double longitude;
	}

	private class Coords {
		@SerializedName("lat")
		public Double lat;
		@SerializedName("lon")
		public Double lon;
	}

	private class RenderEntry {
		@SerializedName("class")
		public String cl;
		@SerializedName("type")
		public String type;
		@SerializedName("zoom")
		public Integer zoom;
		@SerializedName("id")
		public Long id;
		@SerializedName("lat")
		public Double lat;
		@SerializedName("lon")
		public Double lon;
		@SerializedName("startPoint")
		public Coords startPoint;
		@SerializedName("endPoint")
		public Coords endPoint;
		@SerializedName("content")
		public String content;
	}

}
