package net.osmand.test.ui.map;

import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.accessibility.AccessibilityPlugin;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.test.common.AndroidTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Pinch zoom magnification is an independent setting: it keeps working while the
 * Accessibility plugin is disabled (#26631).
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class PinchZoomMagnificationTest extends AndroidTest {

	private static final double LAT = 50.4501;
	private static final double LON = 30.5234;
	private static final int ZOOM = 15;
	private static final float START_HALF_DISTANCE = 150;
	private static final float END_HALF_DISTANCE = 300;
	private static final int MOVE_STEPS = 40;
	private static final long STEP_DELAY_MS = 8;

	@Rule
	public ActivityScenarioRule<MapActivity> scenarioRule = new ActivityScenarioRule<>(MapActivity.class);

	private AccessibilityPlugin plugin;
	private boolean pluginWasActive;
	private boolean magnificationWasEnabled;

	@Before
	@Override
	public void setup() {
		super.setup();
		skipAppStartDialogs(app);
		plugin = PluginsHelper.getPlugin(AccessibilityPlugin.class);
		assertNotNull(plugin);
		pluginWasActive = PluginsHelper.isActive(AccessibilityPlugin.class);
		magnificationWasEnabled = settings.ACCESSIBILITY_PINCH_ZOOM_MAGNIFICATION.get();
		assertNotNull("Pinch zoom magnification needs the OpenGL renderer", getMapView().getMapRenderer());
	}

	@After
	@Override
	public void cleanUp() {
		getScenario().onActivity(activity -> {
			setPluginEnabled(activity, pluginWasActive);
			settings.ACCESSIBILITY_PINCH_ZOOM_MAGNIFICATION.set(magnificationWasEnabled);
		});
		super.cleanUp();
	}

	@Test
	public void magnificationKeepsWorkingAfterPluginIsDisabled() {
		getScenario().onActivity(activity -> {
			setPluginEnabled(activity, true);
			settings.ACCESSIBILITY_PINCH_ZOOM_MAGNIFICATION.set(true);
		});
		assertMagnified(pinchInAndGetMagnificationScale());

		// Same call as the plugin switch on the Plugins screen
		getScenario().onActivity(activity -> setPluginEnabled(activity, false));
		assertMagnified(pinchInAndGetMagnificationScale());
	}

	@Test
	public void magnificationIsAppliedOnResumeWithPluginDisabled() {
		getScenario().onActivity(activity -> {
			setPluginEnabled(activity, false);
			settings.ACCESSIBILITY_PINCH_ZOOM_MAGNIFICATION.set(true);
		});
		// After the update posted by the preference listener, leave the flag stale
		// so that only resuming the map can restore it
		getScenario().onActivity(activity -> getMapView().setPinchZoomMagnificationEnabled(false));
		getScenario().moveToState(Lifecycle.State.STARTED);
		getScenario().moveToState(Lifecycle.State.RESUMED);
		assertMagnified(pinchInAndGetMagnificationScale());
	}

	@Test
	public void noMagnificationWhenPreferenceIsDisabled() {
		getScenario().onActivity(activity -> {
			setPluginEnabled(activity, true);
			settings.ACCESSIBILITY_PINCH_ZOOM_MAGNIFICATION.set(false);
		});
		assertEquals(1f, pinchInAndGetMagnificationScale(), 0.001f);
	}

	@NonNull
	private ActivityScenario<MapActivity> getScenario() {
		return scenarioRule.getScenario();
	}

	@NonNull
	private OsmandMapTileView getMapView() {
		return app.getOsmandMap().getMapView();
	}

	private void setPluginEnabled(@NonNull Activity activity, boolean enabled) {
		if (PluginsHelper.isActive(AccessibilityPlugin.class) != enabled) {
			PluginsHelper.enablePlugin(activity, app, plugin, enabled);
		}
	}

	private static void assertMagnified(float scale) {
		assertTrue("Map is not magnified during the pinch, scale " + scale, scale > 1.5f);
	}

	/**
	 * Pinches in twice the finger distance and returns the magnification scale
	 * while both fingers are still down.
	 */
	private float pinchInAndGetMagnificationScale() {
		OsmandMapTileView mapView = getMapView();
		AtomicReference<float[]> center = new AtomicReference<>();
		getScenario().onActivity(activity -> {
			app.getMapViewTrackingUtilities().setMapLinkedToLocation(false);
			mapView.setLatLon(LAT, LON);
			mapView.setIntZoom(ZOOM);
			mapView.refreshMap();
			View view = mapView.getView();
			assertNotNull(view);
			int[] location = new int[2];
			view.getLocationOnScreen(location);
			center.set(new float[] {location[0] + view.getWidth() / 2f, location[1] + view.getHeight() * 0.4f});
		});
		SystemClock.sleep(1000);

		float x = center.get()[0];
		float y = center.get()[1];
		long downTime = SystemClock.uptimeMillis();
		injectPinch(downTime, MotionEvent.ACTION_DOWN, x, y, START_HALF_DISTANCE, 1);
		injectPinch(downTime, pointerAction(MotionEvent.ACTION_POINTER_DOWN), x, y, START_HALF_DISTANCE, 2);
		for (int i = 1; i <= MOVE_STEPS; i++) {
			float halfDistance = START_HALF_DISTANCE + (END_HALF_DISTANCE - START_HALF_DISTANCE) * i / MOVE_STEPS;
			injectPinch(downTime, MotionEvent.ACTION_MOVE, x, y, halfDistance, 2);
		}
		AtomicReference<Float> scale = new AtomicReference<>();
		InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> scale.set(mapView.getMagnificationScale()));

		injectPinch(downTime, pointerAction(MotionEvent.ACTION_POINTER_UP), x, y, END_HALF_DISTANCE, 2);
		injectPinch(downTime, MotionEvent.ACTION_UP, x, y, END_HALF_DISTANCE, 1);
		SystemClock.sleep(1000);
		return scale.get();
	}

	private static int pointerAction(int action) {
		return action | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
	}

	private static void injectPinch(long downTime, int action, float x, float y, float halfDistance, int pointers) {
		MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[pointers];
		MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[pointers];
		for (int i = 0; i < pointers; i++) {
			properties[i] = new MotionEvent.PointerProperties();
			properties[i].id = i;
			properties[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
			coords[i] = new MotionEvent.PointerCoords();
			coords[i].x = i == 0 ? x - halfDistance : x + halfDistance;
			coords[i].y = y;
			coords[i].pressure = 1f;
			coords[i].size = 1f;
		}
		MotionEvent event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, pointers,
				properties, coords, 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
		boolean injected = InstrumentationRegistry.getInstrumentation().getUiAutomation().injectInputEvent(event, true);
		event.recycle();
		assertTrue("Input injection failed", injected);
		SystemClock.sleep(STEP_DELAY_MS);
	}
}
