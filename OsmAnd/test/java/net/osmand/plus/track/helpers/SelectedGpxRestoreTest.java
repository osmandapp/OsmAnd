package net.osmand.plus.track.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.IndexConstants;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.test.common.AndroidTest;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileWriter;

/** Restoring the selected tracks must not rewrite selected_gpx: a write moves the Global Settings edit time. */
@RunWith(AndroidJUnit4.class)
public class SelectedGpxRestoreTest extends AndroidTest {

	private static final long EDIT_TIME = 1700000000000L;
	private static final String GPX = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><gpx version=\"1.1\" creator=\"OsmAnd\""
			+ " xmlns=\"http://www.topografix.com/GPX/1/1\"><trk><trkseg><trkpt lat=\"50.45\" lon=\"30.52\"/>"
			+ "<trkpt lat=\"50.46\" lon=\"30.53\"/></trkseg></trk></gpx>";

	private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
	private String savedSelection;
	private long savedEditTime;
	private File track;

	@Test
	public void restoreKeepsSelectionOfAnotherDevice() throws Exception {
		track = new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), "selected_gpx_restore_test.gpx");
		track.getParentFile().mkdirs();
		try (FileWriter writer = new FileWriter(track)) {
			writer.write(GPX);
		}
		String missing = new File(track.getParentFile(), "selected_gpx_restore_missing.gpx").getAbsolutePath();
		// A track missing here and a local one, with unescaped "/" and no hidden_groups, unlike Android writes them
		String stored = "[{\"file\":\"" + missing + "\"},{\"file\":\"" + track.getAbsolutePath() + "\"}]";

		restore(stored);
		long end = System.currentTimeMillis() + 20_000;
		while (app.getSelectedGpxHelper().getSelectedFileByPath(track.getAbsolutePath()) == null) {
			assertTrue("Local track not shown", System.currentTimeMillis() < end);
			Thread.sleep(100);
		}
		instrumentation.runOnMainSync(() -> { });

		assertEquals(stored, settings.SELECTED_GPX.get());
		assertEquals(EDIT_TIME, settings.getLastGlobalPreferencesEditTime());
	}

	@Test
	public void restoreKeepsUnreadableSelection() {
		String stored = "Travel/A.gpx,B.gpx"; // as the iOS app stores it

		restore(stored);

		assertEquals(stored, settings.SELECTED_GPX.get());
		assertEquals(EDIT_TIME, settings.getLastGlobalPreferencesEditTime());
	}

	private void restore(String stored) {
		savedSelection = settings.SELECTED_GPX.get();
		savedEditTime = settings.getLastGlobalPreferencesEditTime();
		settings.SELECTED_GPX.set(stored);
		settings.setLastGlobalPreferencesEditTime(EDIT_TIME);
		loadSelection();
	}

	private void loadSelection() {
		GpxSelectionHelper helper = app.getSelectedGpxHelper();
		instrumentation.runOnMainSync(() -> {
			helper.loadGPXTracks(null);
			helper.startPendingGpxRestore();
		});
	}

	@After
	public void restoreSelection() {
		GpxSelectionHelper helper = app.getSelectedGpxHelper();
		SelectedGpxFile shown = track != null ? helper.getSelectedFileByPath(track.getAbsolutePath()) : null;
		if (shown != null) {
			instrumentation.runOnMainSync(() -> helper.selectGpxFile(shown.getGpxFile(),
					GpxSelectionParams.newInstance().hideFromMap()));
		}
		if (track != null) {
			track.delete();
		}
		if (savedSelection != null) {
			settings.SELECTED_GPX.set(savedSelection);
			settings.setLastGlobalPreferencesEditTime(savedEditTime);
			// The test replaced the helper's pending and recently visible tracks: load the device's own again
			loadSelection();
		}
	}
}
