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
			boolean overlapFixed = ButtonPositionSize.computeNonOverlap(1, new ArrayList<>(map.values()));
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

	@NonNull
	public static List<ButtonPositionSize> defaultLayoutExample() {
		List<ButtonPositionSize> lst = new ArrayList<>();

		lst.add(new ButtonPositionSize("map_left_widgets_panel").fromLongValue(343901601804L));
		lst.add(new ButtonPositionSize("map_right_widgets_panel").fromLongValue(343903830029L));
		lst.add(new ButtonPositionSize("top_widgets_panel").fromLongValue(343602233395L));
		lst.add(new ButtonPositionSize("map_bottom_widgets_panel").fromLongValue(412326428723L));
		lst.add(new ButtonPositionSize("map.view.layers").fromLongValue(103552253958L));
		lst.add(new ButtonPositionSize("map.view.quick_search").fromLongValue(69192581574L));
		lst.add(new ButtonPositionSize("map.view.compass").fromLongValue(103787134982L));
		lst.add(new ButtonPositionSize("map.view.zoom_out").fromLongValue(171802624007L));
		lst.add(new ButtonPositionSize("map.view.zoom_id").fromLongValue(172071059463L));
		lst.add(new ButtonPositionSize("map.view.back_to_loc").fromLongValue(137442951687L));
		lst.add(new ButtonPositionSize("map.view.menu").fromLongValue(138080354311L));
		lst.add(new ButtonPositionSize("map.view.route_planning").fromLongValue(138080354823L));
		lst.add(new ButtonPositionSize("map.view.map_3d").fromLongValue(172238898375L));
		lst.add(new ButtonPositionSize("quick_actions").fromLongValue(103083213639L));
		lst.add(new ButtonPositionSize("quick_actions_1729770291750").fromLongValue(103385203975L));
		lst.add(new ButtonPositionSize("quick_actions_1729770294329").fromLongValue(103653639943L));
		lst.add(new ButtonPositionSize("quick_actions_1729770296201").fromLongValue(103418626951L));

//		Pos map_left_widgets_panel x=(left ->0 ), y=(top ->9 ), w=12, h= 4 value = 343901601804 {main}
//		Pos map_right_widgets_panel x=(right->0 ), y=(top ->9 ), w=13, h= 8 value = 343903830029 {main}
//		Pos top_widgets_panel x=(left ->0 ), y=(top ->0 ), w=51, h= 9 value = 343602233395 {main}
//		Pos map_bottom_widgets_panel x=(left ->0 ), y=(bott->0 ), w=51, h=18 value = 412326428723 {main}
//		Pos map.view.layers x=(left ->0 ), y=(top ->14+), w= 6, h= 6 value = 103552253958 {main}
//		Pos map.view.quick_search x=(left ->7+), y=(top ->14 ), w= 6, h= 6 value = 69192581574 {main}
//		Pos map.view.compass x=(left ->0 ), y=(top ->21+), w= 6, h= 6 value = 103787134982 {main}
//		Pos map.view.zoom_out x=(right->0 ), y=(bott->0+), w= 7, h= 7 value = 171802624007 {main}
//		Pos map.view.zoom_id x=(right->0 ), y=(bott->8+), w= 7, h= 7 value = 172071059463 {main}
//		Pos map.view.back_to_loc x=(right->8+), y=(bott->0 ), w= 7, h= 7 value = 137442951687 {main}
//		Pos map.view.menu x=(left ->0+), y=(bott->19 ), w= 7, h= 7 value = 138080354311 {main}
//		Pos map.view.route_planning x=(left ->8+), y=(bott->19 ), w= 7, h= 7 value = 138080354823 {main}
//		Pos map.view.map_3d x=(right->19+), y=(bott->13+), w= 7, h= 7 value = 172238898375 {main}
//		Pos quick_actions x=(right->13+), y=(top ->0+), w= 7, h= 7 value = 103083213639 {main}
//		Pos quick_actions_1729770291750 x=(right->20+), y=(top ->9+), w= 7, h= 7 value = 103385203975 {main}
//		Pos quick_actions_1729770294329 x=(right->28+), y=(top ->17+), w= 7, h= 7 value = 103653639943 {main}
//		Pos quick_actions_1729770296201 x=(left ->14+), y=(top ->10+), w= 7, h= 7 value = 103418626951 {main}

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