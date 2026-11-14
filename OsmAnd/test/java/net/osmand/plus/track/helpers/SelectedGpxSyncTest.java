package net.osmand.plus.track.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.Instrumentation;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.IndexConstants;
import net.osmand.plus.AppInitializeListener;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.backup.items.GlobalSettingsItem;
import net.osmand.plus.settings.backend.backup.items.GpxSettingsItem;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.track.GpxSelectionParams;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

@RunWith(AndroidJUnit4.class)
public class SelectedGpxSyncTest {

	private static final String GPX = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
			+ "<gpx version=\"1.1\" creator=\"OsmAnd\" xmlns=\"http://www.topografix.com/GPX/1/1\">"
			+ "<trk><trkseg><trkpt lat=\"50.45\" lon=\"30.52\"/><trkpt lat=\"50.46\" lon=\"30.53\"/></trkseg></trk></gpx>";

	private final List<File> files = new ArrayList<>();
	private Instrumentation instrumentation;
	private OsmandApplication app;
	private OsmandSettings settings;
	private GpxSelectionHelper helper;
	private String originalValue;
	private long originalEditTime;

	@Before
	public void setUp() throws InterruptedException {
		instrumentation = InstrumentationRegistry.getInstrumentation();
		Context context = instrumentation.getTargetContext();
		app = (OsmandApplication) context.getApplicationContext();
		waitForAppInitialization();
		settings = app.getSettings();
		helper = app.getSelectedGpxHelper();
		originalValue = settings.SELECTED_GPX.get();
		originalEditTime = settings.getLastGlobalPreferencesEditTime();
		instrumentation.runOnMainSync(() -> helper.clearAllGpxFilesToShow(false));
	}

	private void waitForAppInitialization() throws InterruptedException {
		AppInitializer initializer = app.getAppInitializer();
		CountDownLatch initialized = new CountDownLatch(1);
		AppInitializeListener listener = new AppInitializeListener() {
			@Override
			public void onFinish(@NonNull AppInitializer init) {
				initialized.countDown();
			}
		};
		instrumentation.runOnMainSync(() -> initializer.addListener(listener));
		try {
			// Startup can update shared settings before this test takes its timestamp baseline.
			assertTrue("App initialization did not finish", initialized.await(180, TimeUnit.SECONDS));
		} finally {
			instrumentation.runOnMainSync(() -> initializer.removeListener(listener));
		}
		waitForMainThread();
	}

	@After
	public void tearDown() {
		if (helper == null) {
			return;
		}
		instrumentation.runOnMainSync(() -> helper.clearAllGpxFilesToShow(false));
		for (File file : files) {
			file.delete();
		}
		settings.SELECTED_GPX.set(originalValue);
		instrumentation.runOnMainSync(() -> {
			helper.loadGPXTracks(null);
			helper.startPendingGpxRestore();
		});
		settings.setLastGlobalPreferencesEditTime(originalEditTime);
	}

	@Test
	public void missingSelectionsSurviveStartupAndUnreadableImport() throws Exception {
		File local = createGpx("local");
		String missing = missingPath();
		// Unescaped slashes and no hidden_groups: equivalent to what Android writes, but not identical.
		String value = "[{\"file\":\"" + local.getAbsolutePath() + "\"},{\"file\":\"" + missing + "\"}]";
		settings.SELECTED_GPX.set(value);
		long editTime = System.currentTimeMillis() - 60_000L;
		settings.setLastGlobalPreferencesEditTime(editTime);

		instrumentation.runOnMainSync(() -> {
			helper.loadGPXTracks(null);
			helper.startPendingGpxRestore();
		});
		waitFor(() -> helper.getSelectedFileByPath(local.getAbsolutePath()) != null);
		waitForMainThread();

		assertEquals(value, settings.SELECTED_GPX.get());
		assertEquals(editTime, settings.getLastGlobalPreferencesEditTime());
		assertTrue(helper.isPendingSelection(missing));

		// A value Android cannot read, such as the iOS list, leaves the current selections as they are.
		importSelections("Travel/A.gpx,B.gpx");
		assertEquals("Travel/A.gpx,B.gpx", settings.SELECTED_GPX.get());
		assertNotNull(helper.getSelectedFileByPath(local.getAbsolutePath()));
		assertTrue(helper.isPendingSelection(missing));

		File other = createGpx("other");
		selectAndSave(other);
		assertEquals(paths(local.getAbsolutePath(), missing, other.getAbsolutePath()), storedPaths());
		assertTrue(settings.getLastGlobalPreferencesEditTime() > editTime);

		SelectedGpxFile selected = helper.getSelectedFileByPath(local.getAbsolutePath());
		instrumentation.runOnMainSync(() -> helper.selectGpxFile(selected.getGpxFile(),
				GpxSelectionParams.newInstance().hideFromMap().syncGroup().saveSelection()));
		assertEquals(paths(missing, other.getAbsolutePath()), storedPaths());
	}

	@Test
	public void importedSelectionIsTheBaseOfTheNextSave() throws Exception {
		File kept = createGpx("kept");
		File deselected = createGpx("deselected");
		File imported = createGpx("imported");
		String missing = missingPath();
		selectAndSave(kept);
		selectAndSave(deselected);
		String value = new JSONArray()
				.put(new JSONObject().put("file", kept.getAbsolutePath()))
				.put(new JSONObject().put("file", missing))
				.put(new JSONObject().put("file", imported.getAbsolutePath()))
				.toString();

		importSelections(value);

		assertEquals(value, settings.SELECTED_GPX.get());
		assertNotNull(helper.getSelectedFileByPath(kept.getAbsolutePath()));
		assertNull(helper.getSelectedFileByPath(deselected.getAbsolutePath()));
		waitFor(() -> helper.getSelectedFileByPath(imported.getAbsolutePath()) != null
				&& !helper.isPendingSelection(kept.getAbsolutePath()));
		assertTrue(helper.isPendingSelection(missing));

		File next = createGpx("next");
		selectAndSave(next);
		assertEquals(paths(kept.getAbsolutePath(), missing, imported.getAbsolutePath(),
				next.getAbsolutePath()), storedPaths());
	}

	private void importSelections(String value) throws Exception {
		byte[] json = new JSONObject().put("selected_gpx", value).toString().getBytes(StandardCharsets.UTF_8);
		new GlobalSettingsItem(settings).getReader().readFromStream(new ByteArrayInputStream(json), null, null);
		waitForMainThread();
	}

	@Test
	public void downloadedGpxKeepsPendingImportedAttributesOnTheNextSave() throws Exception {
		verifyDownloadedGpx(false);
	}

	@Test
	public void downloadedGpxRefreshesGeometryAfterImportedSelectionAlreadyRestored() throws Exception {
		verifyDownloadedGpx(true);
	}

	private void verifyDownloadedGpx(boolean restoreBeforeDownload) throws Exception {
		File kept = createGpx("downloaded");
		String path = kept.getAbsolutePath();
		String group = "Imported group";
		String originalGpx = GPX.replace("<trk>",
				"<wpt lat=\"50.45\" lon=\"30.52\"><name>Marker</name><type>" + group + "</type></wpt><trk>");
		try (FileWriter writer = new FileWriter(kept)) {
			writer.write(originalGpx);
		}
		selectAndSave(kept);
		String color = "#123456";
		String value = new JSONArray().put(new JSONObject()
				.put("file", path)
				.put("color", color)
				.put("hidden_groups", group)
				.put("selected_by_user", false)).toString();
		byte[] globalJson = new JSONObject().put("selected_gpx", value).toString().getBytes(StandardCharsets.UTF_8);
		byte[] replacement = originalGpx.replace("50.46", "51.46").getBytes(StandardCharsets.UTF_8);
		long editTime = System.currentTimeMillis() - 60_000;
		GlobalSettingsItem globalItem = new GlobalSettingsItem(settings);
		globalItem.setLastModifiedTime(editTime);
		GpxSettingsItem gpxItem = new GpxSettingsItem(app, kept);
		gpxItem.setShouldReplace(true);
		gpxItem.setLastModifiedTime(kept.lastModified() - 60_000);
		FutureTask<Void> download = new FutureTask<>(() -> {
			assertEquals(!restoreBeforeDownload, helper.isPendingSelection(path));
			gpxItem.getReader().readFromStream(new ByteArrayInputStream(replacement), null, gpxItem.getFileName());
			return null;
		});
		FutureTask<Void> queueImports = new FutureTask<>(() -> {
			globalItem.getReader().readFromStream(new ByteArrayInputStream(globalJson), null, null);
			// Queue the download behind the Global Settings import, before it can queue a restore callback.
			if (!restoreBeforeDownload) {
				app.runInUIThread(download);
			}
			return null;
		});
		instrumentation.runOnMainSync(queueImports);
		queueImports.get();
		if (restoreBeforeDownload) {
			waitFor(() -> !helper.isPendingSelection(path));
			assertEquals(50.46, lastTrackLatitude(path), 0.000001);
			download.run();
		}
		download.get(20, TimeUnit.SECONDS);
		waitForMainThread();
		waitFor(() -> !helper.isPendingSelection(path));

		SelectedGpxFile shown = helper.getSelectedFileByPath(path);
		assertNotNull(shown);
		assertEquals(51.46, lastTrackLatitude(path), 0.000001);
		assertEquals(Integer.valueOf(0xff123456), shown.getGpxFile().getColor(0));
		assertTrue(shown.getGpxFile().getPointsGroups().get(group).isHidden());
		assertFalse(shown.selectedByUser);
		assertEquals(value, settings.SELECTED_GPX.get());
		assertEquals(editTime, settings.getLastGlobalPreferencesEditTime());

		File next = createGpx("after_download");
		selectAndSave(next);
		JSONArray saved = new JSONArray(settings.SELECTED_GPX.get());
		assertEquals(paths(path, next.getAbsolutePath()), storedPaths());
		for (int i = 0; i < saved.length(); i++) {
			JSONObject entry = saved.getJSONObject(i);
			if (path.equals(entry.optString("file"))) {
				assertEquals(color, entry.getString("color"));
				assertEquals(group, entry.getString("hidden_groups"));
				assertFalse(entry.getBoolean("selected_by_user"));
				return;
			}
		}
		fail("Downloaded track missing from saved selection");
	}

	private double lastTrackLatitude(String path) {
		return helper.getSelectedFileByPath(path).getGpxFile().getTracks().get(0)
				.getSegments().get(0).getPoints().get(1).getLat();
	}

	private void selectAndSave(File file) {
		instrumentation.runOnMainSync(() -> helper.selectGpxFile(SharedUtil.loadGpxFile(file),
				GpxSelectionParams.getDefaultSelectionParams()));
	}

	private File createGpx(String name) throws IOException {
		File file = new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR),
				"selected_gpx_test_" + name + "_" + System.nanoTime() + IndexConstants.GPX_FILE_EXT);
		try (FileWriter writer = new FileWriter(file)) {
			writer.write(GPX);
		}
		files.add(file);
		return file;
	}

	private String missingPath() {
		return new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR),
				"selected_gpx_test_missing_" + System.nanoTime() + IndexConstants.GPX_FILE_EXT).getAbsolutePath();
	}

	private Set<String> storedPaths() {
		Set<String> paths = new HashSet<>();
		try {
			JSONArray array = new JSONArray(settings.SELECTED_GPX.get());
			for (int i = 0; i < array.length(); i++) {
				JSONObject obj = array.getJSONObject(i);
				if (obj.has("file") && !obj.has("backup")) {
					paths.add(obj.getString("file"));
				}
			}
		} catch (Exception e) {
			fail("Unreadable selection: " + e);
		}
		return paths;
	}

	private static Set<String> paths(String... paths) {
		return new HashSet<>(Arrays.asList(paths));
	}

	private void waitForMainThread() {
		instrumentation.waitForIdleSync();
		instrumentation.runOnMainSync(() -> { });
	}

	private void waitFor(BooleanSupplier condition) throws InterruptedException {
		long end = System.currentTimeMillis() + 20_000;
		while (!condition.getAsBoolean()) {
			if (System.currentTimeMillis() > end) {
				fail("Condition not met in time");
			}
			Thread.sleep(100);
		}
		waitForMainThread();
	}
}
