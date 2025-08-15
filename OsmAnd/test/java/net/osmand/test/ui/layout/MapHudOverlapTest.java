package net.osmand.test.ui.layout;

import static net.osmand.shared.grid.ButtonPositionSize.CELL_SIZE_DP;
import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;

import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.MapButtonsHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.controls.MapHudLayout;
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonState;
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState;
import net.osmand.shared.grid.ButtonPositionSize;
import net.osmand.test.common.AndroidTest;
import net.osmand.test.common.BaseIdlingResource;

import org.apache.commons.logging.Log;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Map;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class MapHudOverlapTest extends AndroidTest {

	private static final Log LOG = PlatformUtil.getLog(MapHudOverlapTest.class);

	@Rule
	public ActivityScenarioRule<MapActivity> scenarioRule = new ActivityScenarioRule<>(MapActivity.class);

	private ObserveButtonsOverlappingIdlingResource idlingResource;

	@Before
	@Override
	public void setup() {
		super.setup();

		MapButtonsHelper buttonsHelper = app.getMapButtonsHelper();
		for (QuickActionButtonState buttonState : buttonsHelper.getQuickActionButtonsStates()) {
			buttonsHelper.setQuickActionFabState(buttonState, true);
		}
		for (MapButtonState buttonState : buttonsHelper.getDefaultButtonsStates()) {
			buttonState.getVisibilityPref().resetToDefault();
		}
	}

	@After
	public void cleanUp() {
		super.cleanUp();
		if (idlingResource != null) {
			unregisterIdlingResources(idlingResource);
		}
	}

	@Test
	public void test() throws Throwable {
		skipAppStartDialogs(app);

		idlingResource = new ObserveButtonsOverlappingIdlingResource(app);
		registerIdlingResources(idlingResource);

		Espresso.onIdle();
	}

	private class ObserveButtonsOverlappingIdlingResource extends BaseIdlingResource {

		private static final int CHECKS_COUNT = 45;
		private static final int CHECK_INTERVAL_MS = 1000;

		private final MapHudLayout mapHudLayout;

		private int checksCounter;

		public ObserveButtonsOverlappingIdlingResource(@NonNull OsmandApplication app) {
			super(app);
			mapHudLayout = app.getOsmandMap().getMapLayers().getMapControlsLayer().getMapHudLayout();
			startHandler();
		}

		private void startHandler() {
			Handler handler = new Handler(Looper.getMainLooper());
			handler.postDelayed(() -> {
				checksCounter++;

				checkOverlap();

				if (isIdleNow()) {
					notifyIdleTransition();
				} else {
					startHandler();
				}
			}, CHECK_INTERVAL_MS);
		}

		private void checkOverlap() {
			LOG.info("--------START--------");
			Map<View, ButtonPositionSize> map = mapHudLayout.collectPositions();
			for (ButtonPositionSize b : map.values()) {
				LOG.info(b + " value = " + b.toLongValue());
			}
			LOG.info("--------");

			float dpToPx = AndroidUtils.dpToPxF(app, 1);
			int width = Math.round(mapHudLayout.getWidth() / dpToPx / CELL_SIZE_DP);
			int height = Math.round(mapHudLayout.getAdjustedHeight() / dpToPx / CELL_SIZE_DP);

			boolean overlapFixed = ButtonPositionSize.Companion.computeNonOverlap(1, new ArrayList<>(map.values()), width, height);
			if (!overlapFixed) {
				throw new AssertionError("Relayout is broken");
			}
			for (ButtonPositionSize b : map.values()) {
				LOG.info(b + " value = " + b.toLongValue());
			}
			LOG.info("--------END--------");
		}

		@Override
		public boolean isIdleNow() {
			return checksCounter >= CHECKS_COUNT;
		}
	}
}