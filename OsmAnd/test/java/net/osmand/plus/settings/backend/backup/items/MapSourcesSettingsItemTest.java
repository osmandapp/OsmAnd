package net.osmand.plus.settings.backend.backup.items;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.IndexConstants;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.settings.backend.backup.exporttype.MapSourcesExportType;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class MapSourcesSettingsItemTest {

	private static final String TEST_SOURCE_PREFIX = "map_sources_backup_test_";

	private final List<File> filesToDelete = new ArrayList<>();
	private final List<SQLiteTileSource> sourcesToClose = new ArrayList<>();

	private OsmandApplication app;
	private OsmandSettings settings;
	private boolean stateHashWasSet;
	private boolean modifiedTimeWasSet;
	private String previousStateHash;
	private long previousModifiedTime;

	@Before
	public void setup() {
		Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
		app = (OsmandApplication) targetContext.getApplicationContext();
		settings = app.getSettings();
		stateHashWasSet = settings.MAP_SOURCES_BACKUP_STATE_HASH.isSet();
		modifiedTimeWasSet = settings.MAP_SOURCES_LOCAL_MODIFIED_TIME.isSet();
		previousStateHash = settings.MAP_SOURCES_BACKUP_STATE_HASH.get();
		previousModifiedTime = settings.MAP_SOURCES_LOCAL_MODIFIED_TIME.get();
		settings.MAP_SOURCES_BACKUP_STATE_HASH.resetToDefault();
		settings.MAP_SOURCES_LOCAL_MODIFIED_TIME.resetToDefault();
	}

	@After
	public void cleanup() {
		if (stateHashWasSet) {
			settings.MAP_SOURCES_BACKUP_STATE_HASH.set(previousStateHash);
		} else {
			settings.MAP_SOURCES_BACKUP_STATE_HASH.resetToDefault();
		}
		if (modifiedTimeWasSet) {
			settings.MAP_SOURCES_LOCAL_MODIFIED_TIME.set(previousModifiedTime);
		} else {
			settings.MAP_SOURCES_LOCAL_MODIFIED_TIME.resetToDefault();
		}
		for (SQLiteTileSource source : sourcesToClose) {
			source.closeConnection();
		}
		for (File file : filesToDelete) {
			Algorithms.removeAllFiles(file);
		}
	}

	@Test
	public void emptyStateHashMatchesSha256() {
		assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
				MapSourcesSettingsItem.getStateHash(Collections.emptyList()));
	}

	@Test
	public void stateHashDoesNotDependOnSourceOrder() {
		ITileSource first = createSource("First", "https://first.example/{0}/{1}/{2}");
		ITileSource second = createSource("Second", "https://second.example/{0}/{1}/{2}");

		String forwardHash = MapSourcesSettingsItem.getStateHash(Arrays.asList(first, second));
		String reverseHash = MapSourcesSettingsItem.getStateHash(Arrays.asList(second, first));

		assertEquals(forwardHash, reverseHash);
	}

	@Test
	public void jsonWriterPreservesSourceOrder() throws Exception {
		ITileSource first = createSource("First", "https://first.example/{0}/{1}/{2}");
		ITileSource second = createSource("Second", "https://second.example/{0}/{1}/{2}");
		MapSourcesSettingsItem item = new MapSourcesSettingsItem(app, Arrays.asList(second, first));

		JSONObject json = item.writeItemsToJson(new JSONObject());

		assertEquals("Second", json.getJSONArray("items").getJSONObject(0).getString("name"));
		assertEquals("First", json.getJSONArray("items").getJSONObject(1).getString("name"));
	}

	@Test
	public void stateHashChangesWithExportedSourceConfiguration() {
		TileSourceTemplate source = createSource("Source", "https://example.com/{0}/{1}/{2}");
		String initialHash = MapSourcesSettingsItem.getStateHash(Collections.singletonList(source));

		source.setReferer("https://osmand.net");

		assertNotEquals(initialHash, MapSourcesSettingsItem.getStateHash(Collections.singletonList(source)));
	}

	@Test
	public void stateHashNormalizesMissingAndEmptyJsonStrings() {
		TileSourceTemplate missingReferer = createSource("Source", "https://example.com/{0}/{1}/{2}");
		TileSourceTemplate emptyReferer = createSource("Source", "https://example.com/{0}/{1}/{2}");
		emptyReferer.setReferer("");

		assertEquals(MapSourcesSettingsItem.getStateHash(Collections.singletonList(missingReferer)),
				MapSourcesSettingsItem.getStateHash(Collections.singletonList(emptyReferer)));
	}

	@Test
	public void jsonRoundTripPreservesDirectorySourceState() throws Exception {
		TileSourceTemplate source = createSource("Directory", "https://example.com/{0}/{1}/{2}.jpg");
		source.setEllipticYTile(true);
		source.setInvertedYTile(true);
		source.setReferer("https://osmand.net");
		source.setUserAgent("OsmAnd test");
		source.setRule("template:1");

		MapSourcesSettingsItem original = new MapSourcesSettingsItem(app, Collections.singletonList(source));
		JSONObject json = original.writeItemsToJson(new JSONObject());
		MapSourcesSettingsItem restored = new MapSourcesSettingsItem(app, json);

		assertEquals(MapSourcesSettingsItem.getStateHash(original.getItems()),
				MapSourcesSettingsItem.getStateHash(restored.getItems()));
	}

	@Test
	public void sqliteDatabaseRoundTripPreservesBackupState() {
		String name = TEST_SOURCE_PREFIX + System.nanoTime();
		File file = app.getAppPath(IndexConstants.TILES_INDEX_DIR + name + IndexConstants.SQLITE_EXT);
		filesToDelete.add(file);
		SQLiteTileSource original = SQLiteTileSource.fromBackup(app, name, 3, 16,
				"https://tiles.example/{0}/{1}/{2}.jpg", "1-4", true, true,
				"https://example.com/a'b", "OsmAnd test", true, 3_600_000L,
				true, "template:1", ".jpg", 512, 32, 24_000);
		String originalHash = MapSourcesSettingsItem.getStateHash(Collections.singletonList(original));

		original.createDataBase();
		SQLiteTileSource restored = new SQLiteTileSource(app, file, Collections.emptyList());
		try {
			assertEquals(originalHash,
					MapSourcesSettingsItem.getStateHash(Collections.singletonList(restored)));
		} finally {
			restored.closeDB();
		}
	}

	@Test
	public void restoredSqliteTileSizeRemainsAutoDetectable() throws Exception {
		String name = TEST_SOURCE_PREFIX + System.nanoTime();
		File file = app.getAppPath(IndexConstants.TILES_INDEX_DIR + name + IndexConstants.SQLITE_EXT);
		filesToDelete.add(file);
		SQLiteTileSource.fromBackup(app, name, 1, 20, "https://example.com/{0}/{1}/{2}", "",
				false, false, "", "", false, -1, false, "", ".png", 256, 16, -1).createDataBase();
		SQLiteTileSource restored = new SQLiteTileSource(app, file, Collections.emptyList());
		try {
			String initialHash = MapSourcesSettingsItem.getStateHash(Collections.singletonList(restored));
			assertEquals(256, restored.getTileSize());

			decodeTile(restored, 512);

			assertEquals(512, restored.getTileSize());
			assertEquals(initialHash, MapSourcesSettingsItem.getStateHash(Collections.singletonList(restored)));
		} finally {
			restored.closeDB();
		}
	}

	@Test
	public void legacySqliteConstructorLeavesTileSizeAutoDetectable() throws Exception {
		String name = TEST_SOURCE_PREFIX + System.nanoTime();
		File file = app.getAppPath(IndexConstants.TILES_INDEX_DIR + name + IndexConstants.SQLITE_EXT);
		filesToDelete.add(file);
		new SQLiteTileSource(app, name, 1, 20, "https://example.com/{0}/{1}/{2}", "", false,
				false, "", "", false, -1, false, "").createDataBase();
		SQLiteTileSource restored = new SQLiteTileSource(app, file, Collections.emptyList());
		try {
			restored.initDatabaseIfNeeded();
			assertEquals(256, restored.getTileSize());

			decodeTile(restored, 512);

			assertEquals(512, restored.getTileSize());
		} finally {
			restored.closeDB();
		}
	}

	@Test
	public void autoDetectedSqliteTileSizeDoesNotChangeStateHash() throws Exception {
		SQLiteTileSource source = createLegacySqliteSource(null, 16);
		try {
			String initialHash = MapSourcesSettingsItem.getStateHash(Collections.singletonList(source));
			assertEquals(256, source.getTileSize());

			decodeTile(source, 512);

			assertEquals(512, source.getTileSize());
			assertEquals(initialHash, MapSourcesSettingsItem.getStateHash(Collections.singletonList(source)));
			JSONObject json = new MapSourcesSettingsItem(app, Collections.singletonList(source))
					.writeItemsToJson(new JSONObject());
			assertEquals(512, json.getJSONArray("items").getJSONObject(0).getInt("tileSize"));
		} finally {
			source.closeDB();
		}
	}

	@Test
	public void sqliteMetadataDefaultsAreNormalizedBeforeBackup() throws Exception {
		SQLiteTileSource source = createLegacySqliteSource(null, 0);
		try {
			MapSourcesSettingsItem original = new MapSourcesSettingsItem(app, Collections.singletonList(source));
			JSONObject json = original.writeItemsToJson(new JSONObject());
			MapSourcesSettingsItem restored = new MapSourcesSettingsItem(app, json);

			assertEquals(".png", source.getTileFormat());
			assertEquals(16, source.getBitDensity());
			assertEquals(MapSourcesSettingsItem.getStateHash(original.getItems()),
					MapSourcesSettingsItem.getStateHash(restored.getItems()));
		} finally {
			source.closeDB();
		}
	}

	@Test
	public void fileTimestampChangeDoesNotChangeInitializedLogicalTime() {
		TileSourceTemplate source = createSource(TEST_SOURCE_PREFIX + System.nanoTime(), "https://example.com/{0}/{1}/{2}");
		MapSourcesSettingsItem item = new MapSourcesSettingsItem(app, Collections.singletonList(source));
		installItem(item);
		File metainfo = new File(app.getAppPath(IndexConstants.TILES_INDEX_DIR + source.getName()), ".metainfo");
		long baseline = System.currentTimeMillis() - 60_000L;
		item.setLocalModifiedTime(baseline);

		assertTrue(metainfo.setLastModified(System.currentTimeMillis()));

		assertEquals(baseline, getLocalItem().getLocalModifiedTime());
	}

	@Test
	public void sqliteFileTimestampChangeDoesNotChangeInitializedLogicalTime() {
		SQLiteTileSource source = createLegacySqliteSource(null, 16);
		try {
			File file = app.getAppPath(IndexConstants.TILES_INDEX_DIR + source.getName() + IndexConstants.SQLITE_EXT);
			MapSourcesSettingsItem item = new MapSourcesSettingsItem(app, Collections.singletonList(source));
			long baseline = System.currentTimeMillis() - 60_000L;
			item.setLocalModifiedTime(baseline);

			assertTrue(file.setLastModified(System.currentTimeMillis()));

			assertEquals(baseline, getLocalItem().getLocalModifiedTime());
		} finally {
			source.closeDB();
		}
	}

	@Test
	public void sourceConfigurationChangeAdvancesLogicalTime() {
		TileSourceTemplate source = createSource(TEST_SOURCE_PREFIX + System.nanoTime(), "https://example.com/{0}/{1}/{2}");
		MapSourcesSettingsItem item = new MapSourcesSettingsItem(app, Collections.singletonList(source));
		installItem(item);
		long baseline = System.currentTimeMillis() - 60_000L;
		item.setLocalModifiedTime(baseline);

		source.setReferer("https://osmand.net");
		assertTrue(settings.installTileSource(source));

		assertTrue(getLocalItem().getLocalModifiedTime() > baseline);
	}

	@Test
	public void cloudModifiedTimeRemainsStableWhenStateDoesNotChange() {
		TileSourceTemplate source = createSource(TEST_SOURCE_PREFIX + System.nanoTime(), "https://example.com/{0}/{1}/{2}");
		MapSourcesSettingsItem item = new MapSourcesSettingsItem(app, Collections.singletonList(source));
		installItem(item);
		long remoteTime = System.currentTimeMillis() - 60_000L;

		item.setLocalModifiedTime(remoteTime);

		assertEquals(remoteTime, getLocalItem().getLocalModifiedTime());
	}

	@Test
	public void unreadDownloadDoesNotChangeBackupState() throws Exception {
		TileSourceTemplate source = createSource(TEST_SOURCE_PREFIX + System.nanoTime(), "https://example.com/{0}/{1}/{2}");
		MapSourcesSettingsItem item = new MapSourcesSettingsItem(app, Collections.singletonList(source));
		installItem(item);
		long baseline = System.currentTimeMillis() - 120_000L;
		item.setLocalModifiedTime(baseline);
		String stateHash = settings.MAP_SOURCES_BACKUP_STATE_HASH.get();
		MapSourcesSettingsItem unread = new MapSourcesSettingsItem(app,
				new JSONObject().put("type", "MAP_SOURCES").put("file", "map_sources.json"));

		unread.setLocalModifiedTime(baseline + 60_000L);

		assertEquals(stateHash, settings.MAP_SOURCES_BACKUP_STATE_HASH.get());
		assertEquals(baseline, settings.MAP_SOURCES_LOCAL_MODIFIED_TIME.get().longValue());
		assertEquals(baseline, getLocalItem().getLocalModifiedTime());
	}

	@Test
	public void emptyDownloadDoesNotInitializeBackupState() throws Exception {
		MapSourcesSettingsItem empty = new MapSourcesSettingsItem(app, new JSONObject().put("items", new JSONArray()));

		empty.setLocalModifiedTime(System.currentTimeMillis());

		assertFalse(settings.MAP_SOURCES_BACKUP_STATE_HASH.isSet());
		assertFalse(settings.MAP_SOURCES_LOCAL_MODIFIED_TIME.isSet());
	}

	@Test
	public void failedJsonReadDoesNotAcknowledgePartialState() throws Exception {
		MapSourcesSettingsItem original = new MapSourcesSettingsItem(app, Collections.singletonList(
				createSource("Source", "https://example.com/{0}/{1}/{2}")));
		JSONObject json = original.writeItemsToJson(new JSONObject());
		json.getJSONArray("items").put("invalid source");
		MapSourcesSettingsItem remote = new MapSourcesSettingsItem(app,
				new JSONObject().put("type", "MAP_SOURCES").put("file", "map_sources.json"));
		settings.MAP_SOURCES_BACKUP_STATE_HASH.set("previous-state");
		settings.MAP_SOURCES_LOCAL_MODIFIED_TIME.set(100L);

		try {
			remote.readItemsFromJson(json);
			fail("Invalid source must fail the entire read");
		} catch (IllegalArgumentException expected) {
			assertTrue(remote.getItems().isEmpty());
		}
		remote.setLocalModifiedTime(200L);

		assertEquals("previous-state", settings.MAP_SOURCES_BACKUP_STATE_HASH.get());
		assertEquals(100L, settings.MAP_SOURCES_LOCAL_MODIFIED_TIME.get().longValue());
		remote.readItemsFromJson(original.writeItemsToJson(new JSONObject()));
		assertEquals(1, remote.getItems().size());
	}

	@Test
	public void restoredSqliteNumericUrlDoesNotCreateLocalChange() throws Exception {
		assertInstalledStateKeepsRemoteTime(true, "https://example.com/{0}/{1}/{2}", 60, ".png");
	}

	@Test
	public void restoredSqliteUrlNormalizationDoesNotCreateLocalChange() throws Exception {
		assertInstalledStateKeepsRemoteTime(true, "https://example.com/{z}/{x}/{y}", 60, ".png");
	}

	@Test
	public void restoredSqliteZeroExpirationDoesNotCreateLocalChange() throws Exception {
		assertInstalledStateKeepsRemoteTime(true, "https://example.com/{0}/{1}/{2}", 0, ".png");
	}

	@Test
	public void restoredDirectoryWithoutExtensionDoesNotCreateLocalChange() throws Exception {
		assertInstalledStateKeepsRemoteTime(false, "https://example.com/{0}/{1}/{2}", 60, null);
	}

	@Test
	public void restoringSubsetPreservesLocalAdditions() {
		MapSourcesSettingsItem existing = new MapSourcesSettingsItem(app, Collections.singletonList(
				createSource(TEST_SOURCE_PREFIX + System.nanoTime(), "https://local.example/{0}/{1}/{2}")));
		installItem(existing);
		MapSourcesSettingsItem imported = new MapSourcesSettingsItem(app, Collections.singletonList(
				createSource(TEST_SOURCE_PREFIX + System.nanoTime(), "https://remote.example/{0}/{1}/{2}")));
		installItem(imported);
		long remoteTime = System.currentTimeMillis() - 60_000L;

		imported.setLocalModifiedTime(remoteTime);

		assertEquals(MapSourcesSettingsItem.getStateHash(imported.getItems()), settings.MAP_SOURCES_BACKUP_STATE_HASH.get());
		long localTime = getLocalItem().getLocalModifiedTime();
		assertTrue(localTime > remoteTime);
		assertEquals(localTime, getLocalItem().getLocalModifiedTime());
	}

	@Test
	public void failedDirectoryInstallationCanBeRetried() throws IOException {
		TileSourceTemplate source = createSource(TEST_SOURCE_PREFIX + System.nanoTime(), "https://example.com/{0}/{1}/{2}");
		MapSourcesSettingsItem item = new MapSourcesSettingsItem(app, Collections.singletonList(source));
		File blocker = app.getAppPath(IndexConstants.TILES_INDEX_DIR + source.getName());
		filesToDelete.add(blocker);
		assertTrue(blocker.createNewFile());

		item.apply();

		assertFalse(item.isAppliedLocally());
		assertTrue(item.getAppliedItems().isEmpty());
		assertTrue(blocker.delete());
		item.apply();
		assertTrue(item.isAppliedLocally());
		assertEquals(1, item.getAppliedItems().size());
	}

	@Test
	public void failedSqliteOpenIsNotApplied() {
		String name = TEST_SOURCE_PREFIX + System.nanoTime();
		SQLiteTileSource source = new SQLiteTileSource(app, name, 1, 20, "https://example.com/{0}/{1}/{2}", "",
				false, false, "", "", false, -1, false, "");
		MapSourcesSettingsItem item = new MapSourcesSettingsItem(app, Collections.singletonList(source));
		File blocker = app.getAppPath(IndexConstants.TILES_INDEX_DIR + name + IndexConstants.SQLITE_EXT);
		filesToDelete.add(blocker);
		assertTrue(blocker.mkdirs());

		item.apply();

		assertFalse(item.isAppliedLocally());
		assertTrue(item.getAppliedItems().isEmpty());
	}

	@Test
	public void failedSqliteWriteIsNotApplied() {
		String name = TEST_SOURCE_PREFIX + System.nanoTime();
		SQLiteTileSource source = new SQLiteTileSource(app, name, 1, 20, "https://example.com/{0}/{1}/{2}", "",
				false, false, "", "", false, -1, false, "");
		MapSourcesSettingsItem item = new MapSourcesSettingsItem(app, Collections.singletonList(source));
		File file = app.getAppPath(IndexConstants.TILES_INDEX_DIR + name + IndexConstants.SQLITE_EXT);
		filesToDelete.add(file);
		SQLiteConnection db = app.getSQLiteAPI().getOrCreateDatabase(file.getAbsolutePath(), true);
		assertNotNull(db);
		try {
			db.execSQL("CREATE TABLE tiles (unexpected int)");
		} finally {
			db.close();
		}

		item.apply();

		assertFalse(item.isAppliedLocally());
		assertTrue(item.getAppliedItems().isEmpty());
	}

	@Test
	public void partialInstallationIsNotApplied() throws IOException {
		TileSourceTemplate installed = createSource(TEST_SOURCE_PREFIX + System.nanoTime(), "https://first.example/{0}/{1}/{2}");
		TileSourceTemplate failed = createSource(TEST_SOURCE_PREFIX + System.nanoTime(), "https://second.example/{0}/{1}/{2}");
		MapSourcesSettingsItem item = new MapSourcesSettingsItem(app, Arrays.asList(installed, failed));
		File blocker = app.getAppPath(IndexConstants.TILES_INDEX_DIR + failed.getName());
		filesToDelete.add(app.getAppPath(IndexConstants.TILES_INDEX_DIR + installed.getName()));
		filesToDelete.add(blocker);
		assertTrue(blocker.createNewFile());

		item.apply();

		assertFalse(item.isAppliedLocally());
		assertEquals(Collections.singletonList(installed), item.getAppliedItems());
	}

	@Test
	public void emptySourcesAreOmittedFromExport() {
		assertTrue(app.getFileSettingsHelper().getFilteredSettingsItems(
				Collections.singletonMap(ExportType.MAP_SOURCES, Collections.emptyList()),
				Collections.singletonList(ExportType.MAP_SOURCES), Collections.emptyList(), true).isEmpty());
	}

	@Test
	public void getNewItemsReturnsIndependentMutableList() {
		TileSourceTemplate source = createSource("Source", "https://example.com/{0}/{1}/{2}");
		MapSourcesSettingsItem item = new MapSourcesSettingsItem(app, Collections.singletonList(source));

		List<ITileSource> newItems = item.getNewItems();
		newItems.clear();
		newItems.add(createSource("Other", "https://other.example/{0}/{1}/{2}"));

		assertEquals(Collections.singletonList(source), item.getItems());
		assertEquals(Collections.singletonList(source), item.getNewItems());
	}

	@Test
	public void replacingExistingSourceIsApplied() {
		TileSourceTemplate source = createSource(TEST_SOURCE_PREFIX + System.nanoTime(), "https://example.com/{0}/{1}/{2}");
		installItem(new MapSourcesSettingsItem(app, Collections.singletonList(source)));
		MapSourcesSettingsItem replacement = new MapSourcesSettingsItem(app, Collections.singletonList(source));
		replacement.setShouldReplace(true);

		installItem(replacement);

		assertTrue(replacement.isAppliedLocally());
		assertEquals(1, replacement.getAppliedItems().size());
	}

	@Test
	public void renamedSqliteImportDoesNotAcknowledgeExistingSource() {
		String name = TEST_SOURCE_PREFIX + System.nanoTime();
		SQLiteTileSource existing = new SQLiteTileSource(app, name, 1, 20, "https://local.example/{0}/{1}/{2}", "",
				false, false, "", "", false, -1, false, "");
		installItem(new MapSourcesSettingsItem(app, Collections.singletonList(existing)));
		SQLiteTileSource incoming = new SQLiteTileSource(app, name, 1, 20, "https://remote.example/{0}/{1}/{2}", "",
				false, false, "", "", false, -1, false, "");
		MapSourcesSettingsItem imported = new MapSourcesSettingsItem(app, Collections.singletonList(incoming));
		filesToDelete.add(app.getAppPath(IndexConstants.TILES_INDEX_DIR + name + "_1" + IndexConstants.SQLITE_EXT));
		imported.processDuplicateItems();
		imported.apply();
		assertTrue(imported.isAppliedLocally());
		assertEquals(name + "_1", imported.getAppliedItems().get(0).getName());
		long remoteTime = System.currentTimeMillis() - 60_000L;

		imported.setLocalModifiedTime(remoteTime);

		assertEquals(MapSourcesSettingsItem.getStateHash(imported.getAppliedItems()),
				settings.MAP_SOURCES_BACKUP_STATE_HASH.get());
		assertTrue(getLocalItem().getLocalModifiedTime() > remoteTime);
	}

	@Test
	public void firstInitializationPreservesLegacyModifiedTime() throws IOException {
		TileSourceTemplate source = createSource(TEST_SOURCE_PREFIX + System.nanoTime(), "https://example.com/{0}/{1}/{2}");
		File metainfo = createMetainfoFile(source.getName());
		assertTrue(metainfo.setLastModified(System.currentTimeMillis() - 60_000L));
		long legacyTime = metainfo.lastModified();
		MapSourcesSettingsItem item = new MapSourcesSettingsItem(app, Collections.singletonList(source));

		assertEquals(legacyTime, item.getLocalModifiedTime());
		assertNotEquals("", settings.MAP_SOURCES_BACKUP_STATE_HASH.get());
		assertEquals(legacyTime, settings.MAP_SOURCES_LOCAL_MODIFIED_TIME.get().longValue());
	}

	@Test
	public void interruptedFirstInitializationReusesStoredModifiedTime() throws IOException {
		TileSourceTemplate source = createSource(TEST_SOURCE_PREFIX + System.nanoTime(), "https://example.com/{0}/{1}/{2}");
		File metainfo = createMetainfoFile(source.getName());
		assertTrue(metainfo.setLastModified(System.currentTimeMillis()));
		// The time was stored, the hash was not: the previous initialization was interrupted.
		long interruptedTime = System.currentTimeMillis() - 120_000L;
		settings.MAP_SOURCES_LOCAL_MODIFIED_TIME.set(interruptedTime);
		MapSourcesSettingsItem item = new MapSourcesSettingsItem(app, Collections.singletonList(source));

		assertEquals(interruptedTime, item.getLocalModifiedTime());
		assertNotEquals("", settings.MAP_SOURCES_BACKUP_STATE_HASH.get());
	}

	private void assertInstalledStateKeepsRemoteTime(boolean sqlite, String url, long expire, String ext) throws Exception {
		TileSourceTemplate source = createSource(TEST_SOURCE_PREFIX + System.nanoTime(), url);
		MapSourcesSettingsItem original = new MapSourcesSettingsItem(app, Collections.singletonList(source));
		JSONObject json = original.writeItemsToJson(new JSONObject());
		JSONObject sourceJson = json.getJSONArray("items").getJSONObject(0);
		sourceJson.put("sql", sqlite);
		sourceJson.put("url", url);
		sourceJson.put("expire", expire);
		if (ext == null) {
			sourceJson.remove("ext");
		} else {
			sourceJson.put("ext", ext);
		}
		MapSourcesSettingsItem restored = new MapSourcesSettingsItem(app, json);
		installItem(restored);
		long remoteTime = System.currentTimeMillis() - 60_000L;

		restored.setLocalModifiedTime(remoteTime);

		MapSourcesSettingsItem local = getLocalItem();
		assertEquals(remoteTime, local.getLocalModifiedTime());
		ITileSource installed = null;
		for (ITileSource candidate : local.getItems()) {
			if (source.getName().equals(candidate.getName())) {
				installed = candidate;
				break;
			}
		}
		assertNotNull(installed);
		assertEquals(sqlite ? url.replace("{z}", "{0}").replace("{x}", "{1}").replace("{y}", "{2}") : url,
				installed.getUrlTemplate());
		if (ext == null) {
			assertEquals(".jpg", installed.getTileFormat());
		}
		if (sqlite && expire == 0) {
			assertEquals(-1, installed.getExpirationTimeMinutes());
		}
		assertEquals(remoteTime, getLocalItem().getLocalModifiedTime());
	}

	private void installItem(MapSourcesSettingsItem item) {
		List<File> installedFiles = new ArrayList<>();
		for (ITileSource source : item.getItems()) {
			String suffix = source instanceof SQLiteTileSource ? IndexConstants.SQLITE_EXT : "";
			File file = app.getAppPath(IndexConstants.TILES_INDEX_DIR + source.getName() + suffix);
			filesToDelete.add(file);
			installedFiles.add(source instanceof SQLiteTileSource ? file : new File(file, ".metainfo"));
		}
		item.processDuplicateItems();
		item.apply();
		assertTrue("Source installation failed", item.isAppliedLocally());
		for (File file : installedFiles) {
			assertTrue("Source was not installed: " + file, file.isFile());
		}
	}

	private MapSourcesSettingsItem getLocalItem() {
		List<ITileSource> sources = new MapSourcesExportType().fetchExportData(app, false);
		for (ITileSource source : sources) {
			if (source instanceof SQLiteTileSource) {
				sourcesToClose.add((SQLiteTileSource) source);
			}
		}
		return new MapSourcesSettingsItem(app, sources);
	}

	private File createMetainfoFile(String sourceName) throws IOException {
		File sourceDirectory = app.getAppPath(IndexConstants.TILES_INDEX_DIR + sourceName);
		filesToDelete.add(sourceDirectory);
		assertTrue(sourceDirectory.exists() || sourceDirectory.mkdirs());
		File metainfo = new File(sourceDirectory, ".metainfo");
		assertTrue(metainfo.exists() || metainfo.createNewFile());
		return metainfo;
	}

	private SQLiteTileSource createLegacySqliteSource(String tileFormat, int bitDensity) {
		String name = TEST_SOURCE_PREFIX + System.nanoTime();
		File file = app.getAppPath(IndexConstants.TILES_INDEX_DIR + name + IndexConstants.SQLITE_EXT);
		filesToDelete.add(file);
		SQLiteConnection db = app.getSQLiteAPI().getOrCreateDatabase(file.getAbsolutePath(), true);
		assertNotNull(db);
		try {
			db.execSQL("CREATE TABLE tiles (x int, y int, z int, s int, image blob, PRIMARY KEY (x,y,z,s))");
			db.execSQL("CREATE TABLE info(tilenumbering, minzoom, maxzoom, url, ext, img_density)");
			db.execSQL("INSERT INTO info (tilenumbering, minzoom, maxzoom, url, ext, img_density) "
					+ "VALUES (?, ?, ?, ?, ?, ?)", new Object[] {"simple", 1, 20,
					"https://example.com/{0}/{1}/{2}", tileFormat, bitDensity});
		} finally {
			db.close();
		}
		return new SQLiteTileSource(app, file, Collections.emptyList());
	}

	private static void decodeTile(SQLiteTileSource source, int tileSize) throws IOException {
		Bitmap bitmap = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
			Bitmap decoded = source.getImage(output.toByteArray(), new String[] {"0", "0", "1"});
			assertNotNull(decoded);
			decoded.recycle();
		} finally {
			bitmap.recycle();
			output.close();
		}
	}

	private static TileSourceTemplate createSource(String name, String url) {
		TileSourceTemplate source = new TileSourceTemplate(name, url, ".png", 20, 1, 256, 16, 12_000);
		source.setRandoms("1-4");
		source.setExpirationTimeMinutes(60);
		return source;
	}
}
