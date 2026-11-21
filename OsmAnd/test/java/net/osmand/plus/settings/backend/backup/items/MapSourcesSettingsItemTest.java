package net.osmand.plus.settings.backend.backup.items;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.IndexConstants;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class MapSourcesSettingsItemTest {

	private static final String TEST_SOURCE_PREFIX = "map_sources_backup_test_";
	private static final String URL = "https://example.com/{0}/{1}/{2}.png";
	private static final long EXPIRATION_MILLIS = 60 * 60 * 1000L;

	private OsmandApplication app;
	private OsmandSettings settings;
	private String savedHash;
	private long savedTime;

	@Before
	public void setUp() {
		Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
		app = (OsmandApplication) context.getApplicationContext();
		settings = app.getSettings();
		// Start from the first run and give the app its own state back afterwards.
		savedHash = settings.MAP_SOURCES_BACKUP_STATE_HASH.get();
		savedTime = settings.MAP_SOURCES_LOCAL_MODIFIED_TIME.get();
		settings.MAP_SOURCES_BACKUP_STATE_HASH.resetToDefault();
		settings.MAP_SOURCES_LOCAL_MODIFIED_TIME.resetToDefault();
	}

	@After
	public void tearDown() {
		settings.MAP_SOURCES_BACKUP_STATE_HASH.set(savedHash);
		settings.MAP_SOURCES_LOCAL_MODIFIED_TIME.set(savedTime);
		File[] files = app.getAppPath(IndexConstants.TILES_INDEX_DIR).listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.getName().startsWith(TEST_SOURCE_PREFIX)) {
					Algorithms.removeAllFiles(file);
				}
			}
		}
	}

	@Test
	public void tileWritesDoNotChangeLocalModifiedTime() throws IOException {
		File file = createSqliteSource();
		// The first read-write open switches the database to WAL, which also writes the file.
		getLocalModifiedTime();
		assertTrue(file.setLastModified(System.currentTimeMillis() - 60_000L));
		long fileTime = file.lastModified();
		long modifiedTime = getLocalModifiedTime();

		// What the map does with a tile database: save a downloaded tile, detect the tile size
		// from it, and on close delete the expired tiles and VACUUM.
		SQLiteTileSource source = new SQLiteTileSource(app, file, Collections.emptyList());
		byte[] tile = createTile(512);
		source.insertImage(0, 0, 1, tile);
		Bitmap bitmap = source.getImage(tile, source.getTileDbParams(0, 0, 1));
		assertNotNull(bitmap);
		bitmap.recycle();
		assertEquals(512, source.getTileSize());
		source.closeDB();

		assertTrue("Tile writes must change the database file", file.lastModified() > fileTime);
		assertEquals(modifiedTime, getLocalModifiedTime());
	}

	@Test
	public void sourceEditChangesLocalModifiedTime() {
		File file = createSqliteSource();
		long modifiedTime = getLocalModifiedTime();

		SQLiteTileSource source = new SQLiteTileSource(app, file, Collections.emptyList());
		TileSourceTemplate edited = new TileSourceTemplate(source.getName(),
				"https://edited.example.com/{0}/{1}/{2}.png", ".png", 20, 1, 256, 16, 18000);
		edited.setExpirationTimeMillis(EXPIRATION_MILLIS);
		source.updateFromTileSourceTemplate(edited);
		source.closeConnection();

		assertTrue(getLocalModifiedTime() > modifiedTime);
	}

	@Test
	public void restoredSourcesKeepRemoteModifiedTime() throws JSONException {
		// Installation normalizes the SQLite url and expiration and the directory tile format.
		JSONArray items = new JSONArray()
				.put(new JSONObject().put("sql", true).put("name", TEST_SOURCE_PREFIX + System.nanoTime())
						.put("minZoom", 1).put("maxZoom", 20).put("url", "https://example.com/{z}/{x}/{y}.png")
						.put("expire", 0))
				.put(new JSONObject().put("sql", false).put("name", TEST_SOURCE_PREFIX + System.nanoTime())
						.put("minZoom", 1).put("maxZoom", 20).put("url", URL));
		MapSourcesSettingsItem remote = new MapSourcesSettingsItem(app, new JSONObject().put("items", items));
		remote.processDuplicateItems();
		remote.apply();
		long remoteTime = System.currentTimeMillis() - 60_000L;

		remote.setLocalModifiedTime(remoteTime);

		List<ITileSource> installed = getTestSources();
		closeConnections(installed);
		assertEquals(2, installed.size());
		assertEquals(remoteTime, getLocalModifiedTime());
	}

	@Test
	public void unappliedDownloadKeepsLocalModifiedTime() throws JSONException {
		createSqliteSource();
		long modifiedTime = getLocalModifiedTime();
		MapSourcesSettingsItem unread = new MapSourcesSettingsItem(app,
				new JSONObject().put("type", "MAP_SOURCES").put("file", "map_sources.json"));

		unread.setLocalModifiedTime(modifiedTime + 60_000L);

		assertEquals(modifiedTime, getLocalModifiedTime());
	}

	private File createSqliteSource() {
		String name = TEST_SOURCE_PREFIX + System.nanoTime();
		new SQLiteTileSource(app, name, 1, 20, URL, "", false, false, "", "", true,
				EXPIRATION_MILLIS, false, "").createDataBase();
		File file = app.getAppPath(IndexConstants.TILES_INDEX_DIR + name + IndexConstants.SQLITE_EXT);
		assertTrue(file.isFile());
		return file;
	}

	/**
	 * The local time of this test's sources only, read the way the backup reads all of them,
	 * so that the map sources a device has of its own do not matter.
	 */
	private long getLocalModifiedTime() {
		List<ITileSource> sources = getTestSources();
		try {
			return new MapSourcesSettingsItem(app, sources).getLocalModifiedTime();
		} finally {
			closeConnections(sources);
		}
	}

	private List<ITileSource> getTestSources() {
		List<ITileSource> sources = new ArrayList<>();
		for (Object object : ExportType.MAP_SOURCES.fetchExportData(app, false)) {
			ITileSource source = (ITileSource) object;
			if (source.getName().startsWith(TEST_SOURCE_PREFIX)) {
				sources.add(source);
			} else if (source instanceof SQLiteTileSource) {
				((SQLiteTileSource) source).closeConnection();
			}
		}
		return sources;
	}

	private static void closeConnections(List<ITileSource> sources) {
		for (ITileSource source : sources) {
			if (source instanceof SQLiteTileSource) {
				((SQLiteTileSource) source).closeConnection();
			}
		}
	}

	private static byte[] createTile(int size) {
		Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
		bitmap.recycle();
		return output.toByteArray();
	}
}
