package net.osmand.test.junit;

import static net.osmand.plus.NavigationService.USED_BY_GPX;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/**
 * Regression test for issue #25861 - {@code ForegroundServiceDidNotStartInTimeException} /
 * silently lost GPX track when the process is re-created in the background.
 *
 * <p>{@code OsmandApplication.onCreate()} unconditionally runs {@code AppInitializer}, whose
 * {@code saveGPXTracks()} step calls {@code startNavigationService(USED_BY_GPX)} whenever
 * {@code SAVE_GLOBAL_TRACK_TO_GPX} is set. That happens on <i>every</i> cold start of the
 * process, including the ones Android triggers with no UI at all - a sticky service restart,
 * a broadcast, a widget update - after the recording process was killed with the screen off.
 *
 * <p>Until commit 51c7ff78c9 ("Attempt navigation foreground service start even from
 * background (A3)") {@code startNavigationService()} skipped the call in that case, because it
 * was guarded by {@code isAppInForeground()}. Since that commit the call is attempted from the
 * background too, and Android then refuses to grant the service the while-in-use location
 * capability: {@code Service.startForeground(.., FOREGROUND_SERVICE_TYPE_LOCATION)} throws
 * {@code SecurityException} ("Foreground service started from background can not have
 * location/camera/microphone access"), the service never enters the foreground state and the
 * recording does not resume - and where the platform does not clear its own
 * {@code startForegroundService()} deadline the whole process is killed with
 * {@code ForegroundServiceDidNotStartInTimeException}.
 *
 * <p>The instrumentation runs without an activity, so the application really is in the
 * background here - exactly the state the crashing cold start is in.
 */
@RunWith(AndroidJUnit4.class)
public class NavigationServiceBackgroundStartTest {

	private OsmandApplication app;

	@Before
	public void setup() {
		Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
		app = (OsmandApplication) targetContext.getApplicationContext();
	}

	/**
	 * The precondition of the whole test: no activity has been started, so the application is
	 * in the background and {@code startForegroundService()} cannot produce a working
	 * foreground service.
	 */
	@Test
	public void applicationIsInBackgroundDuringInstrumentation() {
		assertFalse("the test needs the app to be in the background", app.isAppInForeground());
	}

	/**
	 * A background cold start must not ask the system to start the navigation foreground
	 * service. The system accepts the call (the app is exempt from the background-start
	 * restriction), then denies the location capability the service needs, which both loses the
	 * recording and leaves the platform's {@code startForegroundService()} deadline running.
	 */
	@Test
	public void doesNotStartForegroundServiceWhileInBackground() {
		assertFalse("the test needs the app to be in the background", app.isAppInForeground());
		assertTrue("the test needs the location permission granted",
				OsmAndLocationProvider.isLocationPermissionAvailable(app));

		RecordingContext context = new RecordingContext(app);
		app.startNavigationService(context, USED_BY_GPX);
		waitForMainThread();

		assertTrue("startForegroundService() was called while the app was in the background: "
				+ context.startedForegroundServices, context.startedForegroundServices.isEmpty());
	}

	/** {@code startNavigationService()} posts the actual call, so let the main looper drain. */
	private void waitForMainThread() {
		InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
		});
	}

	/** A {@link Context} that records service starts instead of performing them. */
	private static class RecordingContext extends ContextWrapper {

		final List<String> startedForegroundServices = new ArrayList<>();
		final List<String> startedServices = new ArrayList<>();

		RecordingContext(@NonNull Context base) {
			super(base);
		}

		@Override
		public ComponentName startForegroundService(Intent service) {
			startedForegroundServices.add(String.valueOf(service.getComponent()));
			return service.getComponent();
		}

		@Override
		public ComponentName startService(Intent service) {
			startedServices.add(String.valueOf(service.getComponent()));
			return service.getComponent();
		}
	}
}
