package net.osmand.plus.download;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.data.Amenity;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.download.DownloadSearchHelper.SearchSection;
import net.osmand.plus.download.DownloadSearchHelper.SectionHeader;
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Checks the content of the "Maps & Resources" search results screen.
 * <p>
 * {@link #searchResultsMatchExamples()} compares the rows against the examples listed in
 * {@code test/assets/download/search_examples.json}, {@link #europeAndUsSearchIsUnambiguous()}
 * runs the same search over the whole Europe and United States region trees.
 * <p>
 * Both write what they found to {@code download_search_report.html} in the app external files
 * directory, so the screens can be reviewed without taking screenshots. Run the tests with
 * {@code adb shell am instrument} to keep the file - {@code connectedAndroidTest} uninstalls the
 * app, and the report goes away with it.
 */
@RunWith(AndroidJUnit4.class)
public class DownloadSearchHelperTest extends AndroidTest {

	private static final String EXAMPLES_ASSET = "download/search_examples.json";
	private static final String REPORT_FILE = "download_search_report.html";

	private static final String EUROPE_ID = "europe";
	private static final String US_ID = "northamerica_us";

	private static final List<String> AMBIGUOUS_QUERIES = Arrays.asList(
			"berlin", "york", "washington", "georgia", "london", "hamburg", "new hampshire");

	private static final List<ReportScreen> REPORT = new ArrayList<>();

	private OsmandRegions regions;

	@Before
	public void setupRegions() {
		regions = app.getRegions();
		assertTrue("World regions are not initialized", regions.isInitialized());
		// region names are localized, the examples are written in English
		assumeTrue("Test requires an English locale", "en".equals(app.getLanguage()));
	}

	@Test
	public void searchResultsMatchExamples() throws Exception {
		JSONObject json = new JSONObject(readAsset(EXAMPLES_ASSET));
		DownloadResources indexes = prepareIndexes(toIndexItems(toStringList(json.getJSONArray("resources"))));

		JSONArray examples = json.getJSONArray("examples");
		assertTrue("No examples to check", examples.length() > 0);
		for (int i = 0; i < examples.length(); i++) {
			JSONObject example = examples.getJSONObject(i);
			String name = example.getString("name");
			String query = example.getString("query");
			boolean showGroup = example.optBoolean("showGroup", true);

			DownloadSearchHelper helper = createHelper(showGroup);
			List<Object> rows = helper.search(indexes, query, readCities(example.optJSONArray("cities"), indexes));
			REPORT.add(new ReportScreen("Examples", query, name, toReportRows(helper, rows)));

			List<String> expected = toStringList(example.getJSONArray("screen"));
			List<String> actual = new ArrayList<>();
			for (Object row : rows) {
				actual.add(render(helper, row));
			}
			assertEquals(name + " (query: \"" + query + "\")", expected, actual);
		}
	}

	@Test
	public void europeAndUsSearchIsUnambiguous() {
		DownloadResources europe = prepareIndexes(collectRegionMaps(EUROPE_ID));
		DownloadResources us = prepareIndexes(collectRegionMaps(US_ID));
		DownloadResources both = prepareIndexes(collectRegionMaps(EUROPE_ID, US_ID));

		checkQueries("Europe", europe);
		checkQueries("United States", us);
		checkQueries("Europe + United States", both);
	}

	@Test
	public void cityIsHiddenWhenItsMapIsAlreadyListed() {
		DownloadResources europe = prepareIndexes(collectRegionMaps(EUROPE_ID));
		IndexItem berlinMap = findByBasename(europe, "germany_berlin_europe");
		DownloadSearchHelper helper = createHelper(true);

		List<Object> withoutCity = helper.search(europe, "berlin", Collections.emptyList());
		List<Object> withCity = helper.search(europe, "berlin",
				Collections.singletonList(new CityItem("Berlin", cityAmenity(), berlinMap)));

		assertEquals("A city pointing to a listed map must not add a row",
				renderAll(helper, withoutCity), renderAll(helper, withCity));
	}

	private void checkQueries(@NonNull String catalog, @NonNull DownloadResources indexes) {
		DownloadSearchHelper helper = createHelper(true);
		for (String query : AMBIGUOUS_QUERIES) {
			List<Object> rows = helper.search(indexes, query, Collections.emptyList());
			REPORT.add(new ReportScreen(catalog, query, null, toReportRows(helper, rows)));
			assertSectionsAreWellFormed(catalog, query, rows);
			assertNoDuplicateMaps(catalog, query, rows);
			assertRowsAreDistinguishable(catalog, query, rows);
		}
	}

	/** Headers come once, in a fixed order, and only in front of the rows they describe. */
	private void assertSectionsAreWellFormed(@NonNull String catalog, @NonNull String query,
	                                         @NonNull List<Object> rows) {
		String where = catalog + " / \"" + query + "\": ";
		List<SearchSection> headers = new ArrayList<>();
		SearchSection current = null;
		for (Object row : rows) {
			if (row instanceof SectionHeader header) {
				assertFalse(where + "duplicate " + header.getSection() + " header",
						headers.contains(header.getSection()));
				headers.add(header.getSection());
				current = header.getSection();
			} else {
				assertNotNull(where + "row before the first header", current);
				assertEquals(where + "row is in the wrong section", current, DownloadSearchHelper.getSection(row));
			}
		}
		if (headers.size() == 2) {
			assertEquals(where + "sections are in the wrong order",
					Arrays.asList(SearchSection.REGIONS, SearchSection.MAPS), headers);
		}
	}

	/** The same file must not be offered twice in the "Maps" section. */
	private void assertNoDuplicateMaps(@NonNull String catalog, @NonNull String query,
	                                   @NonNull List<Object> rows) {
		Set<String> files = new HashSet<>();
		for (Object row : rows) {
			String fileName = getFileName(row);
			if (fileName != null) {
				assertTrue(catalog + " / \"" + query + "\": " + fileName + " is listed twice",
						files.add(fileName));
			}
		}
	}

	/** Two rows of the same section must never show exactly the same two lines. */
	private void assertRowsAreDistinguishable(@NonNull String catalog, @NonNull String query,
	                                          @NonNull List<Object> rows) {
		DownloadSearchHelper helper = createHelper(true);
		Set<String> seen = new HashSet<>();
		for (Object row : rows) {
			if (row instanceof SectionHeader) {
				seen.clear();
				continue;
			}
			String text = render(helper, row);
			assertTrue(catalog + " / \"" + query + "\": two rows read as \"" + text + "\"", seen.add(text));
		}
	}

	@Test
	public void countryRegionIsResolvedForNestedRegions() {
		assertEquals("Germany", countryOf("germany_berlin_europe"));
		assertEquals("Germany", countryOf("germany_europe"));
		assertEquals("United States", countryOf("us_new-hampshire_northamerica"));
		// Russia is placed directly under the world, not under a continent
		assertEquals("Russia", countryOf("russia_moscow_asia"));
		assertNull(DownloadSearchHelper.getCountryRegion(regions.getWorldRegion()));
		assertNull(DownloadSearchHelper.getCountryName(null));
	}

	@Test
	public void conditionsAreParsedAsAndOfOrs() {
		List<List<String>> conditions = DownloadSearchHelper.parseConditions("new hampshire, berlin");
		assertEquals(Arrays.asList(Arrays.asList("new", "hampshire"), Arrays.asList("berlin")), conditions);

		assertTrue(DownloadSearchHelper.isMatch(conditions, false, "new hampshire"));
		assertTrue(DownloadSearchHelper.isMatch(conditions, false, "brandenburg, berlin"));
		assertFalse(DownloadSearchHelper.isMatch(conditions, false, "hampshire"));
		assertFalse(DownloadSearchHelper.isMatch(conditions, false, "germany"));
	}

	@NonNull
	private DownloadSearchHelper createHelper(boolean showGroup) {
		return new DownloadSearchHelper(app, regions, showGroup,
				Collections.singletonList(DownloadActivityType.NORMAL_FILE.getTag()));
	}

	@Nullable
	private String countryOf(@NonNull String downloadName) {
		WorldRegion region = regions.getRegionDataByDownloadName(downloadName);
		assertNotNull("Unknown region " + downloadName, region);
		return DownloadSearchHelper.getCountryName(region);
	}

	@NonNull
	private IndexItem findByBasename(@NonNull DownloadResources indexes, @NonNull String basename) {
		for (IndexItem item : indexes.getIndexItems((List<DownloadActivityType>) null)) {
			if (basename.equalsIgnoreCase(item.getBasename())) {
				return item;
			}
		}
		throw new AssertionError("No map for " + basename);
	}

	@NonNull
	private Amenity cityAmenity() {
		Amenity amenity = new Amenity();
		amenity.setSubType("city");
		return amenity;
	}

	@NonNull
	private List<String> renderAll(@NonNull DownloadSearchHelper helper, @NonNull List<Object> rows) {
		List<String> rendered = new ArrayList<>();
		for (Object row : rows) {
			rendered.add(render(helper, row));
		}
		return rendered;
	}

	@NonNull
	private String render(@NonNull DownloadSearchHelper helper, @NonNull Object row) {
		if (row instanceof SectionHeader header) {
			return "[" + header.getTitle(app) + "]";
		}
		String title = helper.getTitle(row);
		String subtitle = helper.getSubtitle(row);
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
	private List<ReportRow> toReportRows(@NonNull DownloadSearchHelper helper, @NonNull List<Object> rows) {
		List<ReportRow> reportRows = new ArrayList<>();
		for (Object row : rows) {
			if (row instanceof SectionHeader header) {
				reportRows.add(new ReportRow(header.getTitle(app), null, null));
			} else {
				reportRows.add(new ReportRow(null, helper.getTitle(row), helper.getSubtitle(row)));
			}
		}
		return reportRows;
	}

	@NonNull
	private DownloadResources prepareIndexes(@NonNull List<IndexItem> items) {
		DownloadResources resources = new DownloadResources(app);
		assertTrue(resources.prepareData(items));
		return resources;
	}

	@NonNull
	private List<IndexItem> toIndexItems(@NonNull List<String> fileNames) {
		List<IndexItem> items = new ArrayList<>();
		for (String fileName : fileNames) {
			items.add(createIndexItem(fileName));
		}
		return items;
	}

	/** Builds the standard map of every downloadable region under the given roots. */
	@NonNull
	private List<IndexItem> collectRegionMaps(@NonNull String... rootRegionIds) {
		Set<String> fileNames = new LinkedHashSet<>();
		for (String regionId : rootRegionIds) {
			WorldRegion root = regions.getRegionData(regionId);
			assertNotNull("Unknown region " + regionId, root);
			collectRegionMaps(root, fileNames);
		}
		List<IndexItem> items = new ArrayList<>();
		for (String fileName : fileNames) {
			items.add(createIndexItem(fileName));
		}
		assertFalse("No maps collected", items.isEmpty());
		return items;
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
	private IndexItem createIndexItem(@NonNull String fileName) {
		return new IndexItem(fileName, null, 0, "0", 0, 0,
				DownloadActivityType.NORMAL_FILE, true, null);
	}

	@NonNull
	private List<CityItem> readCities(@Nullable JSONArray json, @NonNull DownloadResources indexes)
			throws JSONException {
		List<CityItem> cities = new ArrayList<>();
		if (json != null) {
			for (int i = 0; i < json.length(); i++) {
				JSONObject object = json.getJSONObject(i);
				Amenity amenity = new Amenity();
				amenity.setSubType(object.optString("subType", "city"));
				String mapFileName = object.optString("map", null);
				IndexItem indexItem = mapFileName != null ? indexes.getIndexItem(mapFileName) : null;
				assertTrue("Unknown map " + mapFileName, mapFileName == null || indexItem != null);
				cities.add(new CityItem(object.getString("name"), amenity, indexItem));
			}
		}
		return cities;
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
			out.write(SearchReport.build(REPORT).getBytes(StandardCharsets.UTF_8));
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
