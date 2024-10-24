package net.osmand.test.activities;

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
import net.osmand.plus.views.controls.MapHudLayout;
import net.osmand.plus.views.controls.maphudbuttons.ButtonPositionSize;
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonState;
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState;
import net.osmand.test.common.AndroidTest;
import net.osmand.test.common.BaseIdlingResource;

import org.apache.commons.logging.Log;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
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
		for (QuickActionButtonState buttonState : buttonsHelper.getButtonsStates()) {
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
			ButtonPositionSize.computeNonOverlap(1, new ArrayList<>(map.values()));
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

	@NonNull
	public static List<ButtonPositionSize> defaultLayoutExample() {
		List<ButtonPositionSize> lst = new ArrayList<>();

		lst.add(new ButtonPositionSize("top_widgets_panel").fromLongValue(343601184819L));
		lst.add(new ButtonPositionSize("map.view.layers").fromLongValue(103082491910L));
		lst.add(new ButtonPositionSize("map.view.quick_search").fromLongValue(68722819078L));
		lst.add(new ButtonPositionSize("map.view.compass").fromLongValue(103082491910L));
		lst.add(new ButtonPositionSize("map.view.zoom_out").fromLongValue(171802624007L));
		lst.add(new ButtonPositionSize("map.view.zoom_id").fromLongValue(171802624007L));
		lst.add(new ButtonPositionSize("map.view.back_to_loc").fromLongValue(137442951175L));
		lst.add(new ButtonPositionSize("map.view.menu").fromLongValue(137442820103L));
		lst.add(new ButtonPositionSize("map.view.route_planning").fromLongValue(137442820103L));
		lst.add(new ButtonPositionSize("map.view.map_3d").fromLongValue(17351639047L));
		lst.add(new ButtonPositionSize("quick_actions_1729512772882").fromLongValue(103653573767L));
		lst.add(new ButtonPositionSize("quick_actions_1729512776587").fromLongValue(103888453959L));
		lst.add(new ButtonPositionSize("quick_actions_1729512779928").fromLongValue(172037570695L));
		lst.add(new ButtonPositionSize("quick_actions_1729588947725").fromLongValue(69226725575L));

//		MapHudOverlapTest Pos top_widgets_panel x=(left ->0 ), y=(top ->0 ), w=51, h= 7 value = 343601184819
//		MapHudOverlapTest Pos map.view.layers x=(left ->0 ), y=(top ->0+), w= 6, h= 6 value = 103082491910
//		MapHudOverlapTest Pos map.view.quick_search x=(left ->0+), y=(top ->0 ), w= 6, h= 6 value = 68722819078
//		MapHudOverlapTest Pos map.view.compass x=(left ->0 ), y=(top ->0+), w= 6, h= 6 value = 103082491910
//		MapHudOverlapTest Pos map.view.zoom_out x=(right->0 ), y=(bott->0+), w= 7, h= 7 value = 171802624007
//		MapHudOverlapTest Pos map.view.zoom_id x=(right->0 ), y=(bott->0+), w= 7, h= 7 value = 171802624007
//		MapHudOverlapTest Pos map.view.back_to_loc x=(right->0+), y=(bott->0 ), w= 7, h= 7 value = 137442951175
//		MapHudOverlapTest Pos map.view.menu x=(left ->0+), y=(bott->0 ), w= 7, h= 7 value = 137442820103
//		MapHudOverlapTest Pos map.view.route_planning x=(left ->0+), y=(bott->0 ), w= 7, h= 7 value = 137442820103
//		MapHudOverlapTest Pos map.view.map_3d x=(right->0+), y=(bott->517 ), w= 7, h= 7 value = 17351639047
//		MapHudOverlapTest Pos quick_actions_1729512772882 x=(right->18 ), y=(top ->17+), w= 7, h= 7 value = 103653573767
//		MapHudOverlapTest Pos quick_actions_1729512776587 x=(right->5 ), y=(top ->24+), w= 7, h= 7 value = 103888453959
//		MapHudOverlapTest Pos quick_actions_1729512779928 x=(right->2+), y=(bott->7+), w= 7, h= 7 value = 172037570695
//		MapHudOverlapTest Pos quick_actions_1729588947725 x=(right->3 ), y=(top ->15 ), w= 7, h= 7 value = 69226725575

//		lst.add(new ButtonPositionSize("topPanel", 7, POS_FULL_WIDTH, POS_TOP).setMoveDescendantsVertical());
//
//		lst.add(new ButtonPositionSize("leftWid", 7, POS_LEFT, POS_TOP).
//				setMoveDescendantsVertical().setSize(10, 10));
//
//		lst.add(new ButtonPositionSize("zoomOut", 7, false, false).setMoveVertical());
//		lst.add(new ButtonPositionSize("zoomIn", 7, false, false).setMoveVertical());
//		lst.add(new ButtonPositionSize("myLoc", 7, false, false).setMoveHorizontal());
//
//		lst.add(new ButtonPositionSize("drawer", 7, true, false).setMoveHorizontal());
//		lst.add(new ButtonPositionSize("navigation", 7, true, false).setMoveHorizontal());
//		lst.add(new ButtonPositionSize("ruler", 10, true, false).setMoveHorizontal());
//
//		lst.add(new ButtonPositionSize("configMap", 6, true, true).setMoveHorizontal());
//		lst.add(new ButtonPositionSize("search", 6, true, true).setMoveHorizontal());
//		lst.add(new ButtonPositionSize("compass", 6, true, true).setMoveVertical());

		return lst;
	}
}