package net.osmand.plus.download;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.IndexConstants;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.ui.DownloadSearchUIModel;
import net.osmand.plus.download.ui.DownloadSearchUIModel.SectionHeader;
import net.osmand.test.common.AndroidTest;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Checks the content of the "Maps & Resources" search results screen.
 * <p>
 * Nothing on the screen is simulated: the regions, their names and the list of downloadable maps
 * come from the real {@code regions.ocbf} and the cities come from the real
 * {@code World_basemap_mini.obf}. Both are shipped with the app and unpacked on start, so no map
 * has to be downloaded and the rows are the ones a user sees. The only thing the tests build
 * themselves is the {@link IndexItem} of every map - its size and timestamp come from
 * {@code indexes.xml} at runtime and neither of them is shown on this screen.
 * <p>
 * Every test runs against the whole catalog - every map the region tree offers.
 * {@link #searchResultsMatchExamples()} compares the rows against the screens written down in
 * {@code test/assets/download/search_examples.json}, {@link #searchIsUnambiguousOnTheFullCatalog()}
 * checks the rules that must hold for any query on a few more ambiguous ones.
 * <p>
 * Both write what they found to {@code download_search_report.html} in the app external files
 * directory, so the screens can be reviewed without taking screenshots. Run the tests with
 * {@code adb shell am instrument} to keep the file - {@code connectedAndroidTest} uninstalls the
 * app, and the report goes away with it.
 */
@RunWith(AndroidJUnit4.class)
public class DownloadSearchUIModelTest extends AndroidTest {

	private static final String EXAMPLES_ASSET = "download/search_examples.json";
	private static final String REPORT_FILE = "download_search_report.html";
	private static final String BASEMAP_FILE =
			WorldRegion.WORLD_BASEMAP_MINI + IndexConstants.BINARY_MAP_INDEX_EXT;

	/** Broad queries, to check the rules on more rows than the examples spell out. */
	private static final List<String> AMBIGUOUS_QUERIES = Arrays.asList(
			"san", "santa", "port", "york", "kyiv", "utrecht");

	private static final List<ReportScreen> REPORT = new ArrayList<>();

	/** Built once, the region tree holds a few thousand maps. */
	private static DownloadResources fullCatalog;

	/** Searching the basemap is by far the slowest part, and several tests use the same queries. */
	private static final Map<String, List<CityItem>> CITIES = new HashMap<>();

	private OsmandRegions regions;

	@Before
	public void setupSearchData() {
		regions = app.getRegions();
		assertTrue("World regions are not initialized", regions.isInitialized());
		// region names are localized, the examples are written in English
		assumeTrue("Test requires an English locale", "en".equals(app.getLanguage()));
		// cities are shown under the name the request is matched against, keep it at the default
		settings.MAP_PREFERRED_LOCALE.set("");
		// the basemap is a bundled asset, the app unpacks it on every start
		assertTrue("The world basemap is missing",
				new File(app.getAppPath(IndexConstants.MAPS_PATH), BASEMAP_FILE).exists());
	}

	@Test
	public void searchResultsMatchExamples() throws Exception {
		DownloadResources indexes = fullCatalog();
		JSONArray examples = new JSONObject(readAsset(EXAMPLES_ASSET)).getJSONArray("examples");
		assertTrue("No examples to check", examples.length() > 0);
		for (int i = 0; i < examples.length(); i++) {
			JSONObject example = examples.getJSONObject(i);
			String name = example.getString("name");
			String query = example.getString("query");

			DownloadSearchUIModel model = createModel(example.optBoolean("showGroup", true));
			List<Object> rows = model.search(indexes, query, searchCities(model, query));
			REPORT.add(new ReportScreen("Examples", query, name, toReportRows(model, rows)));

			assertEquals(name + " (query: \"" + query + "\")",
					toStringList(example.getJSONArray("screen")), renderAll(model, rows));
			assertScreenIsWellFormed(query, rows);
		}
	}

	@Test
	public void searchIsUnambiguousOnTheFullCatalog() throws IOException {
		DownloadResources indexes = fullCatalog();
		DownloadSearchUIModel model = createModel(true);
		for (String query : AMBIGUOUS_QUERIES) {
			List<Object> rows = model.search(indexes, query, searchCities(model, query));
			REPORT.add(new ReportScreen("Ambiguous queries", query, null, toReportRows(model, rows)));
			assertScreenIsWellFormed(query, rows);
		}
	}

	/** Rules that hold for any query, whatever the shipped region data happens to contain. */
	private void assertScreenIsWellFormed(@NonNull String query, @NonNull List<Object> rows) {
		assertSectionsAreWellFormed(query, rows);
		assertNoDuplicateMaps(query, rows);
		assertRowsAreDistinguishable(query, rows);
	}

	@Test
	public void cityIsHiddenWhenItsMapIsAlreadyListed() throws IOException {
		DownloadResources indexes = fullCatalog();
		DownloadSearchUIModel model = createModel(true);
		List<CityItem> cities = searchCities(model, "berlin");
		assertFalse("The basemap knows no Berlin", cities.isEmpty());

		List<Object> rows = model.search(indexes, "berlin", cities);
		List<String> withoutCities = renderAll(model, model.search(indexes, "berlin", Collections.emptyList()));
		List<String> withCities = renderAll(model, rows);

		assertTrue("Berlin, Germany is listed as a map on its own", withoutCities.contains("Berlin | Germany"));
		// the basemap knows a Berlin in Germany too, and it must not offer the same file again
		assertScreenIsWellFormed("berlin", rows);
		assertTrue("The cities may only add rows", withCities.containsAll(withoutCities));
	}

	/**
	 * The screens in the examples are what the code produces, so they cannot tell whether it
	 * produces the right thing. These cities and the country each of them is in are written down
	 * from the map, not from a test run.
	 */
	@Test
	public void citiesResolveToTheMapThatCoversThem() {
		String[][] cities = {
				// name, latitude, longitude, the country the city is in
				{"Berlin", "52.5200", "13.4050", "Germany"},
				{"Hamburg", "53.5511", "9.9937", "Germany"},
				{"Berlin", "44.4687", "-71.1851", "United States"},
				{"Washington", "38.9072", "-77.0369", "United States"},
				{"New York", "40.7128", "-74.0060", "United States"},
				{"San Francisco", "37.7749", "-122.4194", "United States"},
				{"Kyiv", "50.4501", "30.5234", "Ukraine"},
				{"Lviv", "49.8397", "24.0297", "Ukraine"},
				{"Utrecht", "52.0907", "5.1214", "Netherlands"},
				{"Amsterdam", "52.3676", "4.9041", "Netherlands"},
				{"York", "53.9591", "-1.0815", "United Kingdom"},
				{"Santo Domingo", "18.4861", "-69.9312", "Dominican Republic"},
				{"Havana", "23.1136", "-82.3666", "Cuba"},
				{"Geneva", "46.2044", "6.1432", "Switzerland"},
		};
		List<CityItem> items = new ArrayList<>();
		for (String[] city : cities) {
			items.add(cityAt(city[0], Double.parseDouble(city[1]), Double.parseDouble(city[2])));
		}
		resolve(items);

		for (int i = 0; i < cities.length; i++) {
			String[] expected = cities[i];
			IndexItem item = items.get(i).getIndexItem();
			assertNotNull(expected[0] + " is covered by no map", item);
			WorldRegion country = regionOf(item).getCountryRegion();
			assertNotNull(expected[0] + " resolved to " + item.getBasename() + ", which is in no country",
					country);
			assertEquals(expected[0] + " resolved to " + item.getBasename(),
					expected[3], country.getLocaleName());
		}
	}

	/**
	 * The rows are built by walking the region tree in memory. {@code regions.ocbf} answers the
	 * same question through its own quad tree, which is a separate implementation over the same
	 * data, so the two must name the same map for every city.
	 */
	@Test
	public void cityResolutionAgreesWithTheRegionFile() throws IOException {
		DownloadSearchUIModel model = createModel(true);
		List<CityItem> cities = new ArrayList<>();
		for (String query : Arrays.asList("san", "port", "york")) {
			cities.addAll(searchCities(model, query));
		}
		assertTrue("Nothing to cross check", cities.size() > 100);
		resolve(cities);

		for (CityItem city : cities) {
			LatLon location = city.getAmenity().getLocation();
			Entry<WorldRegion, BinaryMapDataObject> smallest =
					regions.getSmallestBinaryMapDataObjectAt(location);
			if (smallest == null) {
				continue;
			}
			assertNotNull(city.getName() + " at " + location + " is covered by no map", city.getIndexItem());
			assertEquals(city.getName() + " at " + location,
					smallest.getKey().getRegionId(), regionOf(city.getIndexItem()).getRegionId());
		}
	}

	/** Runs one search that resolves every city, with a request no region can match. */
	private void resolve(@NonNull List<CityItem> cities) {
		createModel(true).search(fullCatalog(), "-", cities);
	}

	@NonNull
	private List<CityItem> searchCities(@NonNull DownloadSearchUIModel model, @NonNull String query)
			throws IOException {
		List<CityItem> cities = CITIES.get(query);
		if (cities == null) {
			cities = model.searchCities(query);
			CITIES.put(query, cities);
		}
		// the rows keep the resolved map, hand out a copy so that the tests do not share it
		List<CityItem> copy = new ArrayList<>();
		for (CityItem city : cities) {
			copy.add(new CityItem(city.getName(), city.getAmenity(), null));
		}
		return copy;
	}

	@NonNull
	private CityItem cityAt(@NonNull String name, double latitude, double longitude) {
		Amenity amenity = new Amenity();
		amenity.setSubType("city");
		amenity.setLocation(latitude, longitude);
		return new CityItem(name, amenity, null);
	}

	@NonNull
	private WorldRegion regionOf(@NonNull IndexItem item) {
		WorldRegion region = regions.getRegionDataByDownloadName(item.getBasename().toLowerCase(Locale.US));
		assertNotNull("No region for " + item.getBasename(), region);
		return region;
	}

	@Test
	public void conditionsAreParsedAsAndOfOrs() {
		List<List<String>> conditions = DownloadSearchUIModel.parseConditions("new hampshire, berlin");
		assertEquals(Arrays.asList(Arrays.asList("new", "hampshire"), Arrays.asList("berlin")), conditions);

		assertTrue(DownloadSearchUIModel.isMatch(conditions, false, "new hampshire"));
		assertTrue(DownloadSearchUIModel.isMatch(conditions, false, "brandenburg, berlin"));
		assertFalse(DownloadSearchUIModel.isMatch(conditions, false, "hampshire"));
		assertFalse(DownloadSearchUIModel.isMatch(conditions, false, "germany"));
	}

	/** Headers come once, in a fixed order, and only in front of the rows they describe. */
	private void assertSectionsAreWellFormed(@NonNull String query, @NonNull List<Object> rows) {
		String where = "\"" + query + "\": ";
		List<Integer> headers = new ArrayList<>();
		Integer current = null;
		for (Object row : rows) {
			if (row instanceof SectionHeader header) {
				assertFalse(where + "duplicate header", headers.contains(header.getTitleId()));
				headers.add(header.getTitleId());
				current = header.getTitleId();
			} else {
				assertNotNull(where + "row before the first header", current);
				int expected = DownloadSearchUIModel.isRegion(row) ? R.string.regions : R.string.shared_string_maps;
				assertEquals(where + "row is in the wrong section", expected, current.intValue());
			}
		}
		if (headers.size() == 2) {
			assertEquals(where + "sections are in the wrong order",
					Arrays.asList(R.string.regions, R.string.shared_string_maps), headers);
		}
	}

	/** The same file must not be offered twice in the "Maps" section. */
	private void assertNoDuplicateMaps(@NonNull String query, @NonNull List<Object> rows) {
		Set<String> files = new HashSet<>();
		for (Object row : rows) {
			String fileName = getFileName(row);
			if (fileName != null) {
				assertTrue("\"" + query + "\": " + fileName + " is listed twice", files.add(fileName));
			}
		}
	}

	/** Two rows of the same section must never show exactly the same two lines. */
	private void assertRowsAreDistinguishable(@NonNull String query, @NonNull List<Object> rows) {
		DownloadSearchUIModel model = createModel(true);
		Set<String> seen = new HashSet<>();
		for (Object row : rows) {
			if (row instanceof SectionHeader) {
				seen.clear();
				continue;
			}
			String text = render(model, row);
			assertTrue("\"" + query + "\": two rows read as \"" + text + "\"", seen.add(text));
		}
	}

	@NonNull
	private DownloadSearchUIModel createModel(boolean showGroup) {
		return new DownloadSearchUIModel(app, regions, showGroup,
				Collections.singletonList(DownloadActivityType.NORMAL_FILE.getTag()));
	}

	@NonNull
	private List<String> renderAll(@NonNull DownloadSearchUIModel model, @NonNull List<Object> rows) {
		List<String> rendered = new ArrayList<>();
		for (Object row : rows) {
			rendered.add(render(model, row));
		}
		return rendered;
	}

	@NonNull
	private String render(@NonNull DownloadSearchUIModel model, @NonNull Object row) {
		if (row instanceof SectionHeader header) {
			return "[" + header.getTitle(app) + "]";
		}
		String title = model.getTitle(row);
		String subtitle = model.getSubtitle(row);
		return subtitle == null ? title : title + " | " + subtitle;
	}

	@Nullable
	private String getFileName(@NonNull Object row) {
		if (row instanceof DownloadItem item) {
			return item.getFileName();
		}
		if (row instanceof CityItem city && city.getIndexItem() != null) {
			return city.getIndexItem().getFileName();
		}
		return null;
	}

	@NonNull
	private List<ReportRow> toReportRows(@NonNull DownloadSearchUIModel model, @NonNull List<Object> rows) {
		List<ReportRow> reportRows = new ArrayList<>();
		for (Object row : rows) {
			if (row instanceof SectionHeader header) {
				reportRows.add(new ReportRow(header.getTitle(app), null, null));
			} else {
				reportRows.add(new ReportRow(null, model.getTitle(row), model.getSubtitle(row)));
			}
		}
		return reportRows;
	}

	/** Every map the app offers, taken from the region tree the app itself is shipped with. */
	@NonNull
	private DownloadResources fullCatalog() {
		if (fullCatalog == null) {
			Set<String> fileNames = new LinkedHashSet<>();
			collectRegionMaps(regions.getWorldRegion(), fileNames);
			assertFalse("No maps collected", fileNames.isEmpty());
			fullCatalog = prepareIndexes(new ArrayList<>(fileNames));
		}
		return fullCatalog;
	}

	private void collectRegionMaps(@NonNull WorldRegion region, @NonNull Set<String> fileNames) {
		String downloadName = region.getRegionDownloadName();
		if (region.isRegionMapDownload() && !Algorithms.isEmpty(downloadName)) {
			fileNames.add(Algorithms.capitalizeFirstLetter(downloadName) + "_2.obf.zip");
		}
		for (WorldRegion subregion : region.getSubregions()) {
			collectRegionMaps(subregion, fileNames);
		}
	}

	@NonNull
	private DownloadResources prepareIndexes(@NonNull List<String> fileNames) {
		List<IndexItem> items = new ArrayList<>();
		for (String fileName : fileNames) {
			items.add(new IndexItem(fileName, null, 0, "0", 0, 0,
					DownloadActivityType.NORMAL_FILE, true, null));
		}
		DownloadResources resources = new DownloadResources(app);
		assertTrue(resources.prepareData(items));
		return resources;
	}

	@NonNull
	private List<String> toStringList(@NonNull JSONArray array) throws JSONException {
		List<String> list = new ArrayList<>();
		for (int i = 0; i < array.length(); i++) {
			list.add(array.getString(i));
		}
		return list;
	}

	@NonNull
	private String readAsset(@NonNull String name) throws IOException {
		try (InputStream is = testContext.getAssets().open(name)) {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			byte[] buffer = new byte[8192];
			int read;
			while ((read = is.read(buffer)) != -1) {
				os.write(buffer, 0, read);
			}
			return os.toString("UTF-8");
		}
	}

	@AfterClass
	public static void writeReport() throws IOException {
		fullCatalog = null;
		CITIES.clear();
		if (REPORT.isEmpty()) {
			return;
		}
		Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
		OsmandApplication app = (OsmandApplication) targetContext.getApplicationContext();
		File dir = app.getExternalFilesDir(null);
		if (dir == null) {
			return;
		}
		File report = new File(dir, REPORT_FILE);
		try (FileOutputStream out = new FileOutputStream(report)) {
			out.write(DownloadSearchScreenReport.build(REPORT).getBytes(StandardCharsets.UTF_8));
		}
		REPORT.clear();
		System.out.println("Search results report: " + report.getAbsolutePath());
	}

	static class ReportScreen {

		final String catalog;
		final String query;
		final String note;
		final List<ReportRow> rows;

		ReportScreen(@NonNull String catalog, @NonNull String query, @Nullable String note,
		             @NonNull List<ReportRow> rows) {
			this.catalog = catalog;
			this.query = query;
			this.note = note;
			this.rows = rows;
		}
	}

	static class ReportRow {

		final String header;
		final String title;
		final String subtitle;

		ReportRow(@Nullable String header, @Nullable String title, @Nullable String subtitle) {
			this.header = header;
			this.title = title;
			this.subtitle = subtitle;
		}
	}
}
