package net.osmand.test.ui.tracks;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;

import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.configmap.tracks.appearance.DefaultAppearanceController;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.utils.FileUtils;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.shared.gpx.GpxDbHelper;
import net.osmand.shared.gpx.GpxDirItem;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxParameter;
import net.osmand.shared.gpx.data.TrackFolder;
import net.osmand.shared.gpx.enums.GpxLineStyleType;
import net.osmand.plus.track.helpers.GpxAppearanceHelper;
import net.osmand.test.common.AndroidTest;
import net.osmand.test.common.ResourcesImporter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Collections;

/**
 * Covers all three angles of the "track line style" feature for the folder
 * Default Appearance dialog: setting it through the real UI (segmented popup
 * selector), the value that gets persisted, and the value the map renderer
 * would actually use to draw a track that has no line style of its own.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class DefaultAppearanceLineStyleTest extends AndroidTest {

	private static final String UNSTYLED_TRACK_ASSET = "gpx_recalc_test.gpx";

	@Rule
	public ActivityScenarioRule<MapActivity> scenarioRule = new ActivityScenarioRule<>(MapActivity.class);

	@Test
	public void settingFolderDefaultLineStyleUpdatesUiPersistsAndAppliesOnMove() throws Throwable {
		skipAppStartDialogs(app);

		File dir = new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR),
				"line_style_test_" + System.currentTimeMillis());
		assertTrue("Failed to create test folder", dir.mkdirs());
		TrackFolder folder = new TrackFolder(SharedUtil.kFile(dir), null);

		scenarioRule.getScenario().onActivity(activity -> DefaultAppearanceController.showDialog(activity, folder));
		Espresso.onIdle();

		// --- UI: open the Line style card and pick "Dashed" from its popup selector ---
		onView(allOf(withId(R.id.card_selector), isDescendantOfA(lineStyleCardHeader())))
				.perform(click());
		onView(allOf(withId(R.id.title), withText(R.string.gpx_line_style_dashed)))
				.inRoot(isPlatformPopup())
				.perform(click());

		// the card's own selector row must reflect the new choice immediately
		onView(allOf(withId(R.id.title), isDescendantOfA(allOf(withId(R.id.card_selector), isDescendantOfA(lineStyleCardHeader())))))
				.check(matches(withText(R.string.gpx_line_style_dashed)));

		// --- Setting: save and confirm it persisted to the folder's own DB row ---
		DialogManager dialogManager = app.getDialogManager();
		DefaultAppearanceController controller =
				(DefaultAppearanceController) dialogManager.findController(DefaultAppearanceController.PROCESS_ID);
		assertNotNull("Default appearance controller was not registered", controller);

		scenarioRule.getScenario().onActivity(activity -> controller.saveChanges(activity, false));
		Espresso.onIdle();

		GpxDbHelper gpxDbHelper = app.getGpxDbHelper();
		GpxDirItem dirItem = gpxDbHelper.getGpxDirItem(SharedUtil.kFile(dir));
		assertEquals("dashed", dirItem.<String>getParameter(GpxParameter.LINE_STYLE));

		// --- Rendering: a track with no line style of its own must adopt the folder default once moved in ---
		ResourcesImporter.importGpxAssets(app, Collections.singletonList(UNSTYLED_TRACK_ASSET), null);
		File importedFile = new File(net.osmand.plus.importfiles.ImportHelper.getGpxDestinationDir(app, true), UNSTYLED_TRACK_ASSET);
		GpxDataItem beforeMove = gpxDbHelper.getItem(SharedUtil.kFile(importedFile));
		assertNotNull("Imported test track was not registered", beforeMove);
		assertNull("Test asset must not carry its own line style", beforeMove.<String>getParameter(GpxParameter.LINE_STYLE));

		File movedFile = new File(dir, UNSTYLED_TRACK_ASSET);
		assertNotNull(FileUtils.renameGpxFile(app, importedFile, movedFile));

		GpxDataItem movedItem = gpxDbHelper.getItem(SharedUtil.kFile(movedFile));
		assertNotNull(movedItem);
		assertEquals("Moved track did not adopt the folder's default line style",
				"dashed", movedItem.<String>getParameter(GpxParameter.LINE_STYLE));

		GpxFile movedGpxFile = SharedUtil.loadGpxFile(movedFile);
		GpxAppearanceHelper appearanceHelper = new GpxAppearanceHelper(app);
		GpxLineStyleType renderedStyle = appearanceHelper.getLineStyleTypeForTrack(movedGpxFile, movedItem, dirItem);
		assertEquals("Renderer would not draw the moved track as dashed",
				GpxLineStyleType.DASHED, renderedStyle);
	}

	private static org.hamcrest.Matcher<android.view.View> lineStyleCardHeader() {
		return allOf(withId(R.id.header),
				hasDescendant(allOf(withId(R.id.card_title), withText(R.string.gpx_line_style))));
	}
}
