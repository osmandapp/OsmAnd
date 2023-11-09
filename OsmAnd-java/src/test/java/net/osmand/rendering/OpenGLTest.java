package net.osmand.rendering;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;

import net.osmand.data.LatLon;
import net.osmand.router.RouteResultPreparationTest;
import net.osmand.util.Algorithms;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class OpenGLTest {

	private static final String TEST_JSON = "/test_opengl_rendering.json";
	private static final String EYEPIECE_PROPERTY = "eyepiece";
	private static final String RESOURCES_PROPERTY = "openglTestResources";
	private CummulativeException cummulativeException;

	@Test
	public void testRendering() {
		String eyepiecePath = System.getProperty(EYEPIECE_PROPERTY);
		if (Algorithms.isEmpty(eyepiecePath)) {
			eyepiecePath = System.getenv(EYEPIECE_PROPERTY);
		}
		if (Algorithms.isEmpty(eyepiecePath)) {
			return;
		}

		String resourcesPath = System.getProperty(RESOURCES_PROPERTY);
		if (Algorithms.isEmpty(resourcesPath)) {
			resourcesPath = System.getenv(RESOURCES_PROPERTY);
		}
		if (Algorithms.isEmpty(resourcesPath)) {
			return;
		}

		List<String> commands = generateCommands(eyepiecePath, resourcesPath);
		for (String cmd : commands) {
			try {
				runCommand(cmd);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		try {
			test(resourcesPath);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private List<String> generateCommands(String eyepiecePath, String resourcesPath) {
		Reader reader = new InputStreamReader(Objects.requireNonNull(RouteResultPreparationTest.class.getResourceAsStream(TEST_JSON)));
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		TestEntry[] arr = gson.fromJson(reader, TestEntry[].class);
		LinkedList<String> res = new LinkedList<>();
		for (TestEntry testEntry : arr) {
			if (testEntry.ignore) {
				continue;
			}

			EyepieceParams params = new EyepieceParams();
			assert(testEntry.center != null);
			params.latitude = testEntry.center.lat;
			params.longitude = testEntry.center.lon;

			parseVisibilityZoom(testEntry.icons, params);
			parseVisibilityZoom(testEntry.textOnPath, params);
			parseVisibilityZoom(testEntry.billboardText, params);

			if (testEntry.eyepieceParams != null) {
				params.commandParams = testEntry.eyepieceParams;
			}
			assert(testEntry.testName != null);
			params.testName = testEntry.testName;
			res.add(params.getCommand(eyepiecePath, resourcesPath));
		}
		return res;
	}

	private void parseVisibilityZoom(List<TestEntryUnit> unitList, EyepieceParams params) {
		if (unitList == null) {
			return;
		}
		for (TestEntryUnit testEntryUnit : unitList) {
			if (testEntryUnit.visibleInstancesByZoom == null) {
				continue;
			}
			for (Integer zoom : testEntryUnit.visibleInstancesByZoom.keySet()) {
				params.registerZoom(zoom);
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
			if (!s.startsWith("WARNING") && !s.startsWith("QSocketNotifier:")) {
				errors.append(s);
			}
		}
		if (!errors.toString().isEmpty()) {
			throw new RuntimeException(s);
		}
	}

	private void test(String resourcesPath) throws FileNotFoundException {
		cummulativeException = new CummulativeException();
		Reader reader = new InputStreamReader(Objects.requireNonNull(RouteResultPreparationTest.class.getResourceAsStream(TEST_JSON)));
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		TestEntry[] arr = gson.fromJson(reader, TestEntry[].class);
		for (TestEntry testEntry : arr) {
			if (testEntry.ignore) {
				continue;
			}

			assert(testEntry.testName != null);
			cummulativeException.setCurrentTestName(testEntry.testName);
			List<RenderedInfo> renderedInfo = parseRenderedJsonForMap(testEntry.testName, resourcesPath);
			if (renderedInfo.size() == 0) {
				throw new RuntimeException("File(s) is empty for test:" + testEntry.testName);
			}
			List<TestInfo> testInfo = parseTestJson(testEntry);
			compareExpectedAndRenderedInfo(testInfo, renderedInfo);
		}
		if (cummulativeException.hasExceptions()) {
			throw new IllegalStateException(cummulativeException.getFormatExceptions());
		}
	}

	private List<RenderedInfo> parseRenderedJsonForMap(String testName, String resourcesPath) throws FileNotFoundException {
		File jsonDir = new File(resourcesPath + "/mapdata");
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
		parseTestJsonArr(testEntry.billboardText, result, RenderedType.TEXT_ON_POINT);
		parseTestJsonArr(testEntry.textOnPath, result, RenderedType.TEXT_ON_LINE);
		return result;
	}

	private void parseTestJsonArr(List<TestEntryUnit> unitList, List<TestInfo> result, RenderedType type) {
		if (unitList == null) {
			return;
		}
		for (TestEntryUnit testEntryUnit : unitList) {
			TestInfo testInfo = new TestInfo();
			if (testEntryUnit.visibleInstancesByZoom == null) {
				throw new RuntimeException("visibleInstancesByZoom not found");
			}

			if (testEntryUnit.osmId != null) {
				testInfo.id = testEntryUnit.osmId;
			}

			for (Map.Entry<Integer, String> entry : testEntryUnit.visibleInstancesByZoom.entrySet()) {
				int zoom = entry.getKey();
				String visibleInstancesRangeStr = entry.getValue();
				Range visibleInstancesRange = Range.parse(visibleInstancesRangeStr);
				testInfo.visibleInstancesByZoom.put(zoom, visibleInstancesRange);
			}

			testInfo.content = testEntryUnit.content;
			testInfo.type = type;

			result.add(testInfo);
		}
	}

	private void compareExpectedAndRenderedInfo(List<TestInfo> testInfo, List<RenderedInfo> renderedInfo) {
		for (TestInfo expected : testInfo) {
			Map<Integer, Integer> renderedSymbolsByZoom = getRenderedInstancesByZoom(expected, renderedInfo);
			compareExpectedAndRenderedInstancesCount(expected, renderedSymbolsByZoom);
		}
	}

	private Map<Integer, Integer> getRenderedInstancesByZoom(TestInfo expected, List<RenderedInfo> renderedSymbols) {
		Map<Integer, Integer> renderedInstancesByZoom = new HashMap<>();

		for (RenderedInfo renderedSymbolInfo : renderedSymbols) {
			assert (expected.id != -1 || expected.content != null);
			if (expected.id != -1 && expected.id != renderedSymbolInfo.id) {
				continue;
			}

			if (expected.type != renderedSymbolInfo.type) {
				continue;
			}

			if (!Algorithms.isEmpty(expected.content) && !expected.content.equals(renderedSymbolInfo.content)) {
				continue;
			}

			int symbolZoom = renderedSymbolInfo.zoom;
			if (expected.visibleInstancesByZoom.containsKey(symbolZoom)) {
				Integer count = renderedInstancesByZoom.get(symbolZoom);
				count = count == null ? 1 : count + 1;
				renderedInstancesByZoom.put(symbolZoom, count);
			}
		}

		return renderedInstancesByZoom;
	}

	private void compareExpectedAndRenderedInstancesCount(TestInfo expected, Map<Integer, Integer> renderedSymbolsByZoom) {
		for (Map.Entry<Integer, Range> expectedSymbolsOnZoom : expected.visibleInstancesByZoom.entrySet()) {
			int zoom = expectedSymbolsOnZoom.getKey();
			Range expectedRange = expectedSymbolsOnZoom.getValue();

			int actualCount = renderedSymbolsByZoom.containsKey(zoom)
					? renderedSymbolsByZoom.get(zoom)
					: 0;

			if (actualCount < expectedRange.min || actualCount > expectedRange.max) {
				StringBuilder error = new StringBuilder();
				error.append(expected.type.name.substring(0, 1).toUpperCase(Locale.US));
				error.append(expected.type.name.substring(1));
				if (!Algorithms.isEmpty(expected.content)) {
					error.append(String.format(Locale.US, " 'content':'%s'", expected.content));
				}
				if (expected.id != -1) {
					error.append(String.format(Locale.US, " 'osmId':'%d'", expected.id));
				}
				error.append(String.format(Locale.US, " is rendered %d times on %d zoom, expected %s times",
						actualCount, zoom, expectedRange.str));
				cummulativeException.addException(error.toString());
			}
		}
	}

	private enum RenderedType {

		ICON("billboard icon"),
		TEXT_ON_POINT("billboard text"),
		TEXT_ON_LINE("text on path");

		private final String name;

		RenderedType(String name) {
			this.name = name;
		}
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
		Map<Integer, Range> visibleInstancesByZoom = new HashMap<>();
		String content;
		RenderedType type;
	}

	private static class Range {
		public final String str;
		public final int min;
		public final int max;

		private Range(String str, int min, int max) {
			this.str = str;
			this.min = min;
			this.max = max;
		}

		public static Range parse(String str) {
			int min;
			int max;
			if (str.contains(":")) {
				String[] split = str.split(":");
				if (split.length == 0 || split.length > 2) {
					throw new RuntimeException("invalid visible instances range");
				}

				try {
					min = split[0].isEmpty() ? 0 : Integer.parseInt(split[0]);
					max = split.length == 1 ? Integer.MAX_VALUE : Integer.parseInt(split[1]);
				} catch (NumberFormatException e) {
					throw new RuntimeException("invalid visible instances range");
				}
			} else {
				int count = Integer.parseInt(str);
				min = count;
				max = count;
			}

			return new Range(str, min, max);
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

		String getCommand(String eyepiecePath, String resourcesPath) {
			assert(minZoom <= maxZoom);
			StringBuilder builder = new StringBuilder();
			builder.append(eyepiecePath + " -verbose ");
			if (!commandParams.contains("-obfsPath")) {
				builder.append("-obfsPath=" + resourcesPath + "/maps/ ");
			}
			if (!commandParams.contains("-geotiffPath")) {
				builder.append("-geotiffPath=" + resourcesPath + "/geotiffs/ ");
			}
			if (!commandParams.contains("-cachePath")) {
				builder.append("-cachePath=" + resourcesPath + "/cache/ ");
			}
			if (!commandParams.contains("-outputRasterWidth")) {
				builder.append("-outputRasterWidth=1024 ");
			}
			if (!commandParams.contains("-outputRasterHeight")) {
				builder.append("-outputRasterHeight=768 ");
			}
			if (!commandParams.contains("-outputImageFilename")) {
				builder.append("-outputImageFilename=" + resourcesPath + "/mapimage/" + getMapName(testName) + " ");
			}
			if (!commandParams.contains("-outputJSONFilename")) {
				builder.append("-outputJSONFilename=" + resourcesPath + "/mapdata/" + getMapName(testName) + " ");
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
		@SerializedName("issue")
		public String issue;
		@SerializedName("ignore")
		public boolean ignore;
		@SerializedName("center")
		public LatitudeLongitude center;
		@SerializedName("icons")
		public List<TestEntryUnit> icons;
		@SerializedName("billboardText")
		public List<TestEntryUnit> billboardText;
		@SerializedName("textOnPath")
		public List<TestEntryUnit> textOnPath;
		@SerializedName("eyepieceParams")
		public String eyepieceParams;
	}

	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	private class TestEntryUnit {
		@SerializedName("osmId")
		public Long osmId;
		@SerializedName("content")
		public String content;
		@SerializedName("visibleInstancesByZoom")
		Map<Integer, String> visibleInstancesByZoom;
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
		public LatitudeLongitude startPoint;
		@SerializedName("endPoint")
		public LatitudeLongitude endPoint;
		@SerializedName("content")
		public String content;
	}

	private class LatitudeLongitude {
		@SerializedName("lat")
		public Double lat;
		@SerializedName("lon")
		public Double lon;
	}
}
