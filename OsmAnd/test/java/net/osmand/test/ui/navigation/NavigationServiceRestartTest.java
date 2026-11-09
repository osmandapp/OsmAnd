package net.osmand.test.ui.navigation;

import static net.osmand.plus.NavigationService.USED_BY_GPX;
import static net.osmand.plus.NavigationService.USED_BY_NAVIGATION;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import android.Manifest;
import android.app.UiAutomation;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.plus.NavigationService;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.test.common.AndroidTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

/**
 * Play crash {@code OsmandApplication.lambda$startNavigationService$1} /
 * {@code ForegroundServiceDidNotStartInTimeException} (5.4.6 - 5.4.9).
 *
 * <p>With the battery usage set to "Restricted" ({@code RUN_ANY_IN_BACKGROUND} denied) Android
 * takes NavigationService out of the foreground state as soon as the app goes to the background,
 * but keeps the service running. When the app is reopened, navigation restore (or a new track
 * recording) calls {@code startForegroundService()} again; {@code onStartCommand()} took the
 * {@code isUsed()} branch and returned without {@code startForeground()}, and the platform killed
 * the process 10-30 s later. Field logcats show exactly this sequence.
 *
 * <p>Without the fix the run aborts with "Process crashed"; with the fix the service is back in
 * the foreground. Ignored because it takes about a minute and kills the process when it fails;
 * run it by hand to recheck.
 */
@Ignore("Manual check: ~1 min, kills the process without the fix")
@LargeTest
@RunWith(AndroidJUnit4.class)
public class NavigationServiceRestartTest extends AndroidTest {

	private static final long TIMEOUT_MS = 30_000;
	private static final long CRASH_WINDOW_MS = 40_000;
	private static final String[] PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION,
			Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.POST_NOTIFICATIONS};

	private String packageName;

	@Before
	@Override
	public void setup() {
		packageName = InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName();
		UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
		for (String permission : PERMISSIONS) {
			automation.grantRuntimePermission(packageName, permission);
		}
		super.setup();
	}

	@After
	public void restore() {
		shell("cmd appops set " + packageName + " RUN_ANY_IN_BACKGROUND allow");
		runOnMainSync(() -> {
			OsmandMonitoringPlugin plugin = PluginsHelper.getPlugin(OsmandMonitoringPlugin.class);
			if (plugin != null) {
				plugin.stopRecording();
			}
			NavigationService service = app.getNavigationService();
			if (service != null) {
				service.stopIfNeeded(app, USED_BY_GPX | USED_BY_NAVIGATION);
			}
		});
	}

	@Test
	public void startAgainAfterRestrictedAppReturnsFromBackground() {
		// Not ActivityScenario: it loses track of the activity once it is reopened from the shell.
		openMapActivity();
		waitFor("app in foreground", app::isAppInForeground);
		shell("cmd appops set " + packageName + " RUN_ANY_IN_BACKGROUND ignore");

		// Trip recording starts the service with the GPX notification it needs for startForeground().
		OsmandMonitoringPlugin plugin = PluginsHelper.getPlugin(OsmandMonitoringPlugin.class);
		assertNotNull(plugin);
		runOnMainSync(() -> {
			PluginsHelper.enablePlugin(null, app, plugin, true);
			plugin.startRecording(null);
		});
		waitFor("service in foreground", () -> foregroundType() != 0);

		shell("input keyevent KEYCODE_HOME");
		waitFor("service taken out of the foreground", () -> foregroundType() == 0);

		openMapActivity();
		waitFor("app in foreground again", app::isAppInForeground);
		runOnMainSync(() -> app.startNavigationService(USED_BY_NAVIGATION));

		// Without the fix the platform kills the process inside this window.
		SystemClock.sleep(CRASH_WINDOW_MS);
		assertNotNull("service was stopped", app.getNavigationService());
		assertNotEquals("service did not return to the foreground", 0, foregroundType());
	}

	private void openMapActivity() {
		shell("am start -n " + packageName + "/" + MapActivity.class.getName());
	}

	private int foregroundType() {
		AtomicInteger type = new AtomicInteger();
		runOnMainSync(() -> {
			NavigationService service = app.getNavigationService();
			type.set(service != null ? service.getForegroundServiceType() : 0);
		});
		return type.get();
	}

	private void waitFor(@NonNull String what, @NonNull BooleanSupplier condition) {
		long end = SystemClock.uptimeMillis() + TIMEOUT_MS;
		while (!condition.getAsBoolean()) {
			if (SystemClock.uptimeMillis() > end) {
				throw new AssertionError("timed out waiting for " + what);
			}
			SystemClock.sleep(200);
		}
	}

	private void shell(@NonNull String command) {
		UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
		ParcelFileDescriptor pfd = automation.executeShellCommand(command);
		try (FileInputStream in = new ParcelFileDescriptor.AutoCloseInputStream(pfd)) {
			byte[] buffer = new byte[1024];
			while (in.read(buffer) != -1) {
				// wait for the command to finish
			}
		} catch (IOException e) {
			throw new AssertionError(command, e);
		}
	}

	private void runOnMainSync(@NonNull Runnable runnable) {
		InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable);
	}
}
