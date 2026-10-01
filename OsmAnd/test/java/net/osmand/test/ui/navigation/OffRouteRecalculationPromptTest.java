package net.osmand.test.ui.navigation;

import static net.osmand.plus.simulation.SimulationProvider.SIMULATED_PROVIDER;
import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.pm.PackageManager;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.IProgress;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.ValueHolder;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.IRouteInformationListener;
import net.osmand.plus.routing.RouteService;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.RoutingHelperUtils;
import net.osmand.plus.routing.VoiceRouter.VoiceMessageListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.test.common.AndroidTest;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Rides a bicycle beside the route (#25544) and checks when "Route recalculated" is announced.
 * The rider keeps the direction of the original route and moves sideways to 45 m,
 * above the 30 m recalculation distance and below the off-route prompt distance.
 * <p>
 * Needs Germany_berlin_europe_2.obf in the app folder, and for the BRouter cases
 * the BRouter app with the E10_N50.rd5 segment. Missing data skips the case.
 * <p>
 * Run command to pull the timelines and screenshots to a git-ignored directory:
 * <p>
 * mkdir -p build/screenshots/off_route_prompts
 * adb pull /data/local/tmp/osmand/screenshots/off_route_prompts/. build/screenshots/off_route_prompts/
 */
@Ignore("Manual scenario test, needs the Berlin map and BRouter")
@LargeTest
@RunWith(AndroidJUnit4.class)
public class OffRouteRecalculationPromptTest extends AndroidTest {

	private static final Log LOG = PlatformUtil.getLog(OffRouteRecalculationPromptTest.class);

	private static final String TARGET_DIR = "/data/local/tmp/osmand/screenshots/off_route_prompts";
	private static final String BERLIN_MAP = "Germany_berlin_europe_2.obf";
	private static final String BROUTER_PACKAGE = "btools.routingapp";

	private static final ApplicationMode MODE = ApplicationMode.BICYCLE;
	private static final LatLon START = new LatLon(52.521513, 13.416514);
	private static final LatLon END = new LatLon(52.515300, 13.453830);

	private static final float RECALCULATION_DISTANCE = 30;
	private static final float SPEED = 20 / 3.6f;
	private static final int OFFSET = 45;
	// 15 s of suppressed prompts plus one more recalculation
	private static final long MAX_SILENT_RECALCULATION_MS = 25_000;
	private static final long ANNOUNCEMENT_TAIL_MS = 5000;
	private static final long ROUTE_TIMEOUT_MS = 90_000;
	// the 45 m offset is visible on screenshots
	private static final int MAP_ZOOM = 17;

	private static final String ROUTE_RECALC = "route_recalc";
	private static final String OFF_ROUTE = "off_route";
	private static final String BACK_ON_ROUTE = "back_on_route";

	// seconds and target offset in meters, the offset changes linearly within a phase
	private static final int[][] LONG_DEVIATION = {
			{20, 0}, {5, OFFSET}, {60, OFFSET}, {5, 0}, {20, 0}};
	// the ride from the issue: beside the route almost to the destination
	private static final int[][] WHOLE_ROUTE = {
			{20, 0}, {5, OFFSET}, {420, OFFSET}, {5, 0}, {20, 0}};
	// deviation, return and a second deviation within 60 s of the last recalculation
	private static final int[][] TWO_DEVIATIONS = {
			{20, 0}, {5, OFFSET}, {35, OFFSET}, {5, 0}, {20, 0}, {5, OFFSET}, {35, OFFSET}, {5, 0}, {15, 0}};
	// short deviation, return and a short deviation with a single recalculation 15-60 s later
	private static final int[][] SHORT_DEVIATIONS = {
			{20, 0}, {5, OFFSET}, {8, OFFSET}, {5, 0}, {25, 0}, {5, OFFSET}, {6, OFFSET}, {5, 0}, {15, 0}};

	@Rule
	public ActivityScenarioRule<MapActivity> scenarioRule = new ActivityScenarioRule<>(MapActivity.class);

	private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
	private final List<PreferenceState<?>> preferenceStates = new ArrayList<>();
	private final List<VoiceEvent> voiceEvents = Collections.synchronizedList(new ArrayList<>());
	private final List<RecalculationEvent> recalculations = Collections.synchronizedList(new ArrayList<>());
	private final ExecutorService screenshotExecutor = Executors.newSingleThreadExecutor();

	private ApplicationMode previousAppMode;
	private VoiceMessageListener voiceListener;
	private IRouteInformationListener routeListener;
	private volatile long rideStartTime;
	private String caseName;

	@Before
	@Override
	public void setup() {
		super.setup();
		runShellCommand("mkdir -p " + TARGET_DIR);
		runShellCommand("chmod 777 " + TARGET_DIR);

		File berlinMap = new File(app.getAppPath(null), BERLIN_MAP);
		Assume.assumeTrue("No " + BERLIN_MAP, berlinMap.exists());
		// reload only for a map copied while the app was running: reloading on every run
		// may race with the start up indexing and fail the route calculation
		if (!app.getResourceManager().getIndexFileNames().containsKey(BERLIN_MAP)) {
			app.getResourceManager().reloadIndexes(IProgress.EMPTY_PROGRESS, new ArrayList<>());
		}

		previousAppMode = settings.getApplicationMode();
		saveAndSet(settings.ROUTE_RECALCULATION_DISTANCE, RECALCULATION_DISTANCE);
		saveAndSet((CommonPreference<Boolean>) settings.SPEAK_ROUTE_RECALCULATION, true);
		saveAndSet((CommonPreference<Boolean>) settings.SPEAK_ROUTE_DEVIATION, true);
		saveAndSet((CommonPreference<Boolean>) settings.VOICE_MUTE, false);
		saveAndSet((CommonPreference<String>) settings.VOICE_PROVIDER, "en-tts");
		saveAndSet(settings.ROUTE_SERVICE, RouteService.OSMAND);
		saveAndSet(settings.AUTO_ZOOM_MAP, false);
		// the speed cameras sheet is shown on the first route planning and covers the map
		saveAndSet((CommonPreference<Boolean>) settings.SPEED_CAMERAS_ALERT_SHOWED, true);
	}

	@After
	public void tearDown() {
		screenshotExecutor.shutdown();
		try {
			screenshotExecutor.awaitTermination(30, TimeUnit.SECONDS);
		} catch (InterruptedException ignored) {
		}
		instrumentation.runOnMainSync(() -> {
			RoutingHelper routingHelper = app.getRoutingHelper();
			if (voiceListener != null) {
				routingHelper.getVoiceRouter().removeVoiceMessageListener(voiceListener);
			}
			if (routeListener != null) {
				routingHelper.removeListener(routeListener);
			}
			app.stopNavigation();
			app.getTargetPointsHelper().clearAllPoints(true);
			for (PreferenceState<?> state : preferenceStates) {
				state.restore();
			}
			preferenceStates.clear();
			if (previousAppMode != null) {
				settings.setApplicationMode(previousAppMode);
			}
		});
		super.cleanUp();
	}

	@Test
	public void osmandLongDeviation() throws Throwable {
		ride(RouteService.OSMAND, "osmand_long", LONG_DEVIATION);
	}

	@Test
	public void osmandWholeRoute() throws Throwable {
		ride(RouteService.OSMAND, "osmand_whole", WHOLE_ROUTE);
	}

	@Test
	public void osmandTwoDeviations() throws Throwable {
		ride(RouteService.OSMAND, "osmand_two", TWO_DEVIATIONS);
	}

	@Test
	public void osmandShortDeviations() throws Throwable {
		ride(RouteService.OSMAND, "osmand_short", SHORT_DEVIATIONS);
	}

	@Test
	public void brouterLongDeviation() throws Throwable {
		ride(RouteService.BROUTER, "brouter_long", LONG_DEVIATION);
	}

	@Test
	public void brouterWholeRoute() throws Throwable {
		ride(RouteService.BROUTER, "brouter_whole", WHOLE_ROUTE);
	}

	@Test
	public void brouterTwoDeviations() throws Throwable {
		ride(RouteService.BROUTER, "brouter_two", TWO_DEVIATIONS);
	}

	@Test
	public void brouterShortDeviations() throws Throwable {
		ride(RouteService.BROUTER, "brouter_short", SHORT_DEVIATIONS);
	}

	private void ride(@NonNull RouteService routeService, @NonNull String caseName, @NonNull int[][] phases) throws Throwable {
		if (routeService == RouteService.BROUTER) {
			Assume.assumeTrue("BRouter is not installed", isPackageInstalled(BROUTER_PACKAGE));
		}
		this.caseName = caseName;
		runShellCommand("rm -f " + TARGET_DIR + "/" + caseName + "_*");
		MODE.setRouteService(routeService);
		initVoice();
		skipAppStartDialogs(app);

		List<Location> path = calculateRoute();
		startNavigation();
		feedLocations(path, phases);

		writeTimeline(phases);
		checkTimeline(phases);
	}

	private void initVoice() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		instrumentation.runOnMainSync(() -> {
			settings.setApplicationMode(MODE);
			app.initVoiceCommandPlayer(app, MODE, latch::countDown, false, false, true, false);
		});
		assertTrue("Voice player is not initialized", latch.await(30, TimeUnit.SECONDS));
		assertTrue("Voice player is not initialized", app.getPlayer() != null);
	}

	@NonNull
	private List<Location> calculateRoute() throws Throwable {
		Location start = createLocation(START, 0, 0);
		instrumentation.runOnMainSync(() -> app.getLocationProvider().setCustomLocation(start, TimeUnit.MINUTES.toMillis(1)));
		instrumentation.runOnMainSync(() -> {
			app.getTargetPointsHelper().navigateToPoint(END, false, -1);
			app.getOsmandMap().getMapActions().enterRoutePlanningModeGivenGpx(null, null, null, true, false);
		});
		RoutingHelper routingHelper = app.getRoutingHelper();
		long deadline = SystemClock.elapsedRealtime() + ROUTE_TIMEOUT_MS;
		boolean retried = false;
		while (!routingHelper.isRouteCalculated() || routingHelper.isRouteBeingCalculated()) {
			if (SystemClock.elapsedRealtime() > deadline) {
				fail("Route is not calculated: " + routingHelper.getLastRouteCalcError());
			}
			String error = routingHelper.getLastRouteCalcError();
			if (!retried && error != null && !routingHelper.isRouteBeingCalculated()) {
				// right after the app start reading a map region may fail once
				// (IndexOutOfBoundsException in BinaryMapRouteReaderAdapter), it is not what this test checks
				LOG.warn("Route calculation failed, retrying: " + error);
				retried = true;
				instrumentation.runOnMainSync(() -> app.getTargetPointsHelper().updateRouteAndRefresh(true));
			}
			SystemClock.sleep(500);
		}
		assertTrue("Route is calculated for " + routingHelper.getAppMode(), routingHelper.getAppMode() == MODE);
		return new ArrayList<>(routingHelper.getRoute().getImmutableAllLocations());
	}

	private void startNavigation() {
		RoutingHelper routingHelper = app.getRoutingHelper();
		voiceListener = (listCommands, played) -> {
			if (!listCommands.isEmpty() && rideStartTime != 0) {
				String command = listCommands.get(0);
				voiceEvents.add(new VoiceEvent(elapsed(), String.join(" ", listCommands)));
				if (command.equals(ROUTE_RECALC) || command.equals(OFF_ROUTE) || command.equals(BACK_ON_ROUTE)) {
					showEvent("Voice: " + command, "voice_" + command);
				}
			}
		};
		routeListener = new IRouteInformationListener() {
			@Override
			public void newRouteIsCalculated(boolean newRoute, ValueHolder<Boolean> showToast) {
				onRouteRecalculated(newRoute);
			}

			@Override
			public void routeWasCancelled() {
			}

			@Override
			public void routeWasFinished() {
			}
		};
		instrumentation.runOnMainSync(() -> {
			routingHelper.getVoiceRouter().addVoiceMessageListener(voiceListener);
			routingHelper.addListener(routeListener);
			app.getOsmandMap().getMapActions().startNavigation();
		});
		assertTrue("Navigation is not started", routingHelper.isFollowingMode());
	}

	private void feedLocations(@NonNull List<Location> path, @NonNull int[][] phases) {
		double pathLength = 0;
		for (int i = 1; i < path.size(); i++) {
			pathLength += path.get(i - 1).distanceTo(path.get(i));
		}
		int seconds = 0;
		for (int[] phase : phases) {
			seconds += phase[0];
		}
		assertTrue("Route is too short: " + (int) pathLength + " m", pathLength > SPEED * seconds);

		rideStartTime = SystemClock.elapsedRealtime();
		for (int second = 0; second <= seconds; second++) {
			Location location = locationAt(path, SPEED * second, offsetAt(phases, second));
			app.runInUIThread(() -> app.getLocationProvider().setCustomLocation(location, 10_000));
			long next = rideStartTime + (second + 1) * 1000L;
			SystemClock.sleep(Math.max(0, next - SystemClock.elapsedRealtime()));
		}
		SystemClock.sleep(ANNOUNCEMENT_TAIL_MS);
	}

	private void onRouteRecalculated(boolean newRoute) {
		RoutingHelper routingHelper = app.getRoutingHelper();
		Location location = routingHelper.getLastFixedLocation();
		if (location == null || rideStartTime == 0) {
			return;
		}
		// the check RouteRecalculationHelper.setNewRoute suppresses the prompt with
		boolean backward = RoutingHelperUtils.isRouteAgainstMovement(location, routingHelper.getRoute());
		long time = elapsed();
		boolean announced;
		synchronized (voiceEvents) {
			announced = false;
			for (VoiceEvent event : voiceEvents) {
				if (!event.attributed && event.commands.startsWith(ROUTE_RECALC)) {
					event.attributed = true;
					announced = true;
				}
			}
		}
		recalculations.add(new RecalculationEvent(time, newRoute, backward, announced));
		if (!announced) {
			showEvent("Silent recalculation" + (backward ? " (backward route)" : ""), "silent_recalc");
		}
	}

	private void checkTimeline(@NonNull int[][] phases) {
		List<String> errors = new ArrayList<>();
		for (long[] deviation : getDeviations(phases)) {
			long start = deviation[0];
			long end = deviation[1] + ANNOUNCEMENT_TAIL_MS;
			String name = String.format(Locale.US, "Deviation %d-%d s", start / 1000, deviation[1] / 1000);
			List<RecalculationEvent> deviationRecalculations = getRecalculations(start, end);
			if (deviationRecalculations.isEmpty()) {
				continue;
			}
			RecalculationEvent first = deviationRecalculations.get(0);
			if (end - first.time > MAX_SILENT_RECALCULATION_MS && !hasPrompt(start, end)) {
				errors.add(name + ": the route changes without route_recalc or off_route");
			}
			// a single backward route is expected to be followed by a forward one, so it stays silent
			if (first.backward && first.announced) {
				errors.add(name + ": the first backward recalculation is announced at " + first.time / 1000 + " s");
			}
		}
		if (!errors.isEmpty()) {
			fail(caseName + "\n" + String.join("\n", errors));
		}
	}

	private boolean hasPrompt(long start, long end) {
		synchronized (voiceEvents) {
			for (VoiceEvent event : voiceEvents) {
				if (event.time >= start && event.time <= end
						&& (event.commands.startsWith(ROUTE_RECALC) || event.commands.startsWith(OFF_ROUTE))) {
					return true;
				}
			}
		}
		return false;
	}

	@NonNull
	private List<RecalculationEvent> getRecalculations(long start, long end) {
		List<RecalculationEvent> result = new ArrayList<>();
		synchronized (recalculations) {
			for (RecalculationEvent event : recalculations) {
				if (event.time >= start && event.time <= end) {
					result.add(event);
				}
			}
		}
		return result;
	}

	// time intervals when the target offset is above the recalculation distance
	@NonNull
	private static List<long[]> getDeviations(@NonNull int[][] phases) {
		List<long[]> deviations = new ArrayList<>();
		int seconds = 0;
		for (int[] phase : phases) {
			seconds += phase[0];
		}
		long start = -1;
		for (int second = 0; second <= seconds; second++) {
			boolean deviated = offsetAt(phases, second) > RECALCULATION_DISTANCE;
			if (deviated && start < 0) {
				start = second * 1000L;
			} else if (!deviated && start >= 0) {
				deviations.add(new long[] {start, second * 1000L});
				start = -1;
			}
		}
		return deviations;
	}

	private static double offsetAt(@NonNull int[][] phases, int second) {
		double offset = 0;
		int phaseStart = 0;
		for (int[] phase : phases) {
			int duration = phase[0];
			if (second <= phaseStart + duration) {
				return offset + (phase[1] - offset) * (second - phaseStart) / duration;
			}
			offset = phase[1];
			phaseStart += duration;
		}
		return offset;
	}

	// the point at a distance along the path, shifted to the right of the path
	@NonNull
	private static Location locationAt(@NonNull List<Location> path, double distance, double offset) {
		double passed = 0;
		for (int i = 1; i < path.size(); i++) {
			Location from = path.get(i - 1);
			Location to = path.get(i);
			double segment = from.distanceTo(to);
			if (passed + segment >= distance || i == path.size() - 1) {
				float bearing = from.bearingTo(to);
				double along = Math.min(segment, Math.max(0, distance - passed));
				LatLon onPath = MapUtils.rhumbDestinationPoint(from.getLatitude(), from.getLongitude(), along, bearing);
				LatLon shifted = MapUtils.rhumbDestinationPoint(onPath, offset, bearing + 90);
				return createLocation(shifted, bearing, SPEED);
			}
			passed += segment;
		}
		throw new IllegalArgumentException("Empty path");
	}

	@NonNull
	private static Location createLocation(@NonNull LatLon latLon, float bearing, float speed) {
		Location location = new Location(SIMULATED_PROVIDER, latLon.getLatitude(), latLon.getLongitude());
		location.setTime(System.currentTimeMillis());
		location.setAccuracy(5);
		if (speed > 0) {
			location.setBearing(bearing);
			location.setSpeed(speed);
		}
		return location;
	}

	private long elapsed() {
		return SystemClock.elapsedRealtime() - rideStartTime;
	}

	private void showEvent(@NonNull String message, @NonNull String fileSuffix) {
		String fileName = String.format(Locale.US, "%s_%03ds_%s.png", caseName, elapsed() / 1000, fileSuffix);
		app.showShortToastMessage(message);
		screenshotExecutor.execute(() -> {
			// navigation start zooms the map out after a while, so the zoom is set for every screenshot
			app.runInUIThread(() -> app.getOsmandMap().getMapView().setIntZoom(MAP_ZOOM));
			SystemClock.sleep(500);
			runShellCommand("screencap -p " + TARGET_DIR + "/" + fileName);
		});
	}

	private void writeTimeline(@NonNull int[][] phases) {
		StringBuilder text = new StringBuilder();
		text.append("# ").append(caseName).append(", ").append(MODE.getRouteService())
				.append(", recalculation distance ").append((int) RECALCULATION_DISTANCE).append(" m")
				.append(", offset ").append(OFFSET).append(" m\n");
		text.append("time_s\tevent\tdetails\n");
		List<Object[]> rows = new ArrayList<>();
		synchronized (voiceEvents) {
			for (VoiceEvent event : voiceEvents) {
				rows.add(new Object[] {event.time, "voice", event.commands});
			}
		}
		synchronized (recalculations) {
			for (RecalculationEvent event : recalculations) {
				String details = (event.newRoute ? "new route" : "recalculation")
						+ (event.backward ? ", backward" : ", forward")
						+ (event.announced ? ", announced" : ", silent");
				rows.add(new Object[] {event.time, "route", details});
			}
		}
		for (long[] deviation : getDeviations(phases)) {
			rows.add(new Object[] {deviation[0], "deviation", "start, offset > " + (int) RECALCULATION_DISTANCE + " m"});
			rows.add(new Object[] {deviation[1], "deviation", "end"});
		}
		rows.sort((o1, o2) -> Long.compare((long) o1[0], (long) o2[0]));
		for (Object[] row : rows) {
			text.append(String.format(Locale.US, "%.1f\t%s\t%s\n", (long) row[0] / 1000f, row[1], row[2]));
		}
		LOG.info(caseName + " timeline\n" + text);
		// the shell can read the external cache but not the internal one
		File file = new File(app.getExternalCacheDir(), caseName + "_timeline.tsv");
		try (FileWriter writer = new FileWriter(file)) {
			writer.write(text.toString());
		} catch (IOException e) {
			LOG.error("Failed writing timeline", e);
		}
		runShellCommand("cp " + file.getAbsolutePath() + " " + TARGET_DIR + "/");
	}

	private boolean isPackageInstalled(@NonNull String packageName) {
		try {
			app.getPackageManager().getPackageInfo(packageName, 0);
			return true;
		} catch (PackageManager.NameNotFoundException e) {
			return false;
		}
	}

	private <T> void saveAndSet(@NonNull CommonPreference<T> preference, @Nullable T value) {
		preferenceStates.add(new PreferenceState<>(preference, MODE));
		preference.setModeValue(MODE, value);
	}

	@NonNull
	private String runShellCommand(@NonNull String command) {
		StringBuilder output = new StringBuilder();
		try {
			UiAutomation uiAutomation = instrumentation.getUiAutomation();
			try (ParcelFileDescriptor pfd = uiAutomation.executeShellCommand(command)) {
				if (pfd != null) {
					try (InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(pfd)) {
						byte[] buf = new byte[1024];
						int read;
						while ((read = is.read(buf)) != -1) {
							output.append(new String(buf, 0, read, StandardCharsets.UTF_8));
						}
					}
				}
			}
		} catch (Exception e) {
			LOG.error("Failed executing shell command: " + command, e);
		}
		return output.toString().trim();
	}

	private static class VoiceEvent {

		private final long time;
		private final String commands;
		private boolean attributed;

		VoiceEvent(long time, @NonNull String commands) {
			this.time = time;
			this.commands = commands;
		}
	}

	private static class RecalculationEvent {

		private final long time;
		private final boolean newRoute;
		private final boolean backward;
		private final boolean announced;

		RecalculationEvent(long time, boolean newRoute, boolean backward, boolean announced) {
			this.time = time;
			this.newRoute = newRoute;
			this.backward = backward;
			this.announced = announced;
		}
	}

	private static class PreferenceState<T> {

		private final CommonPreference<T> preference;
		private final ApplicationMode appMode;
		private final boolean wasSet;
		private final T value;

		PreferenceState(@NonNull CommonPreference<T> preference, @NonNull ApplicationMode appMode) {
			this.preference = preference;
			this.appMode = appMode;
			this.wasSet = preference.isGlobal() ? preference.isSet() : preference.isSetForMode(appMode);
			this.value = preference.getModeValue(appMode);
		}

		void restore() {
			if (wasSet) {
				preference.setModeValue(appMode, value);
			} else {
				preference.resetModeToDefault(appMode);
			}
		}
	}
}
