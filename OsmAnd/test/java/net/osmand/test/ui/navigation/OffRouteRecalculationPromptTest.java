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
import net.osmand.plus.shared.SharedUtil;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.WptPt;
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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Rides beside the route (#25544) with a bicycle, on foot, by car and by motorcycle, and checks
 * when "Route recalculated" is announced. The rider keeps the direction of the original route and
 * moves sideways to the offset of the profile, above its recalculation distance.
 * <p>
 * Needs Germany_berlin_europe_2.obf in the app folder, and for the BRouter cases
 * the BRouter app with the E10_N50.rd5 segment. Missing data skips the case.
 * <p>
 * The GPX cases ride a track instead, one point per second, for example r25544_offset.gpx from #25544:
 * <p>
 * adb push r25544_offset.gpx /data/local/tmp/
 * adb shell am instrument -w -e gpx /data/local/tmp/r25544_offset.gpx
 * -e class net.osmand.test.ui.navigation.OffRouteRecalculationPromptTest#brouterGpxRide
 * net.osmand.plus.test/androidx.test.runner.AndroidJUnitRunner
 * <p>
 * Remove @Ignore before running the cases.
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
	private static final String GPX_ARGUMENT = "gpx";

	private static final Profile BICYCLE = new Profile(ApplicationMode.BICYCLE, "bicycle", 20 / 3.6f, 30, 45,
			new LatLon(52.521513, 13.416514), new LatLon(52.515300, 13.453830)); // the ride from #25544
	// between its 15 m recalculation distance and the ~22 m off-route prompt distance,
	// so the test checks "Route recalculated" and not "off route"
	private static final Profile PEDESTRIAN = new Profile(ApplicationMode.PEDESTRIAN, "pedestrian", 5 / 3.6f, 15, 20,
			new LatLon(52.536500, 13.417500), new LatLon(52.543000, 13.402000)); // between houses in Prenzlauer Berg
	private static final Profile CAR = new Profile(ApplicationMode.CAR, "car", 60 / 3.6f, 50, 75,
			new LatLon(52.504000, 13.276000), new LatLon(52.473000, 13.403000)); // city and the A100 motorway
	private static final Profile MOTORCYCLE = new Profile(ApplicationMode.MOTORCYCLE, "motorcycle", 50 / 3.6f, 50, 75,
			new LatLon(52.548000, 13.428000), new LatLon(52.487000, 13.424000)); // across the city

	// 15 s of suppressed prompts plus one more recalculation
	private static final long MAX_SILENT_RECALCULATION_MS = 25_000;
	private static final long ANNOUNCEMENT_TAIL_MS = 5000;
	// long car routes on slow emulators
	private static final long ROUTE_TIMEOUT_MS = 180_000;
	private static final int MAP_ZOOM = 17;

	private static final String ROUTE_RECALC = "route_recalc";
	private static final String OFF_ROUTE = "off_route";
	private static final String BACK_ON_ROUTE = "back_on_route";

	// seconds and the share of the profile offset (0 on the route, 1 the full offset),
	// the offset changes linearly within a phase
	private static final int[][] LONG_DEVIATION = {
			{20, 0}, {5, 1}, {60, 1}, {5, 0}, {20, 0}};
	// the ride from the issue: beside the route almost to the destination
	private static final int[][] WHOLE_ROUTE = {
			{20, 0}, {5, 1}, {420, 1}, {5, 0}, {20, 0}};
	// deviation, return and a second deviation within 60 s of the last recalculation
	private static final int[][] TWO_DEVIATIONS = {
			{20, 0}, {5, 1}, {35, 1}, {5, 0}, {20, 0}, {5, 1}, {35, 1}, {5, 0}, {15, 0}};
	// short deviation, return and a short deviation with a single recalculation 15-60 s later
	private static final int[][] SHORT_DEVIATIONS = {
			{20, 0}, {5, 1}, {8, 1}, {5, 0}, {25, 0}, {5, 1}, {6, 1}, {5, 0}, {15, 0}};

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
	private final Location[] fedLocations = new Location[3];
	private String caseName;
	private String rideDescription;
	private Ride ride;
	private Profile profile;

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
	}

	@After
	public void tearDown() {
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
		screenshotExecutor.shutdown();
		try {
			screenshotExecutor.awaitTermination(30, TimeUnit.SECONDS);
		} catch (InterruptedException ignored) {
		}
		super.cleanUp();
	}

	@Test
	public void osmandLongDeviation() throws Throwable {
		ride(BICYCLE, RouteService.OSMAND, "osmand_long", LONG_DEVIATION);
	}

	@Test
	public void osmandWholeRoute() throws Throwable {
		ride(BICYCLE, RouteService.OSMAND, "osmand_whole", WHOLE_ROUTE);
	}

	@Test
	public void osmandTwoDeviations() throws Throwable {
		ride(BICYCLE, RouteService.OSMAND, "osmand_two", TWO_DEVIATIONS);
	}

	@Test
	public void osmandShortDeviations() throws Throwable {
		ride(BICYCLE, RouteService.OSMAND, "osmand_short", SHORT_DEVIATIONS);
	}

	@Test
	public void brouterLongDeviation() throws Throwable {
		ride(BICYCLE, RouteService.BROUTER, "brouter_long", LONG_DEVIATION);
	}

	@Test
	public void brouterWholeRoute() throws Throwable {
		ride(BICYCLE, RouteService.BROUTER, "brouter_whole", WHOLE_ROUTE);
	}

	@Test
	public void brouterTwoDeviations() throws Throwable {
		ride(BICYCLE, RouteService.BROUTER, "brouter_two", TWO_DEVIATIONS);
	}

	@Test
	public void brouterShortDeviations() throws Throwable {
		ride(BICYCLE, RouteService.BROUTER, "brouter_short", SHORT_DEVIATIONS);
	}

	@Test
	public void osmandPedestrianTwoDeviations() throws Throwable {
		ride(PEDESTRIAN, RouteService.OSMAND, "osmand_pedestrian_two", TWO_DEVIATIONS);
	}

	@Test
	public void brouterPedestrianTwoDeviations() throws Throwable {
		ride(PEDESTRIAN, RouteService.BROUTER, "brouter_pedestrian_two", TWO_DEVIATIONS);
	}

	@Test
	public void osmandPedestrianWholeRoute() throws Throwable {
		ride(PEDESTRIAN, RouteService.OSMAND, "osmand_pedestrian_whole", WHOLE_ROUTE);
	}

	@Test
	public void brouterPedestrianWholeRoute() throws Throwable {
		ride(PEDESTRIAN, RouteService.BROUTER, "brouter_pedestrian_whole", WHOLE_ROUTE);
	}

	@Test
	public void osmandCarTwoDeviations() throws Throwable {
		ride(CAR, RouteService.OSMAND, "osmand_car_two", TWO_DEVIATIONS);
	}

	@Test
	public void brouterCarTwoDeviations() throws Throwable {
		ride(CAR, RouteService.BROUTER, "brouter_car_two", TWO_DEVIATIONS);
	}

	@Test
	public void osmandCarWholeRoute() throws Throwable {
		ride(CAR, RouteService.OSMAND, "osmand_car_whole", WHOLE_ROUTE);
	}

	@Test
	public void brouterCarWholeRoute() throws Throwable {
		ride(CAR, RouteService.BROUTER, "brouter_car_whole", WHOLE_ROUTE);
	}

	@Test
	public void osmandMotorcycleTwoDeviations() throws Throwable {
		ride(MOTORCYCLE, RouteService.OSMAND, "osmand_motorcycle_two", TWO_DEVIATIONS);
	}

	@Test
	public void brouterMotorcycleTwoDeviations() throws Throwable {
		ride(MOTORCYCLE, RouteService.BROUTER, "brouter_motorcycle_two", TWO_DEVIATIONS);
	}

	@Test
	public void osmandMotorcycleWholeRoute() throws Throwable {
		ride(MOTORCYCLE, RouteService.OSMAND, "osmand_motorcycle_whole", WHOLE_ROUTE);
	}

	@Test
	public void brouterMotorcycleWholeRoute() throws Throwable {
		ride(MOTORCYCLE, RouteService.BROUTER, "brouter_motorcycle_whole", WHOLE_ROUTE);
	}

	@Test
	public void osmandGpxRide() throws Throwable {
		rideGpx(RouteService.OSMAND, "osmand_gpx");
	}

	@Test
	public void brouterGpxRide() throws Throwable {
		rideGpx(RouteService.BROUTER, "brouter_gpx");
	}

	private void ride(@NonNull Profile profile, @NonNull RouteService routeService, @NonNull String caseName,
	                  @NonNull int[][] phases) throws Throwable {
		ride(profile, routeService, caseName, profile.offset + " m beside the route",
				path -> Ride.generated(path, phases, profile));
	}

	private void rideGpx(@NonNull RouteService routeService, @NonNull String caseName) throws Throwable {
		String gpxPath = InstrumentationRegistry.getArguments().getString(GPX_ARGUMENT);
		Assume.assumeTrue("No -e " + GPX_ARGUMENT + " argument", gpxPath != null);
		List<LatLon> points = loadGpxPoints(gpxPath);
		ride(BICYCLE, routeService, caseName, new File(gpxPath).getName(), path -> Ride.fromPoints(path, points));
	}

	private void ride(@NonNull Profile profile, @NonNull RouteService routeService, @NonNull String caseName,
	                  @NonNull String rideDescription, @NonNull Function<List<Location>, Ride> rideFactory) throws Throwable {
		if (routeService == RouteService.BROUTER) {
			Assume.assumeTrue("BRouter is not installed", isPackageInstalled(BROUTER_PACKAGE));
		}
		this.profile = profile;
		this.caseName = caseName;
		this.rideDescription = rideDescription;
		runShellCommand("rm -f " + TARGET_DIR + "/" + caseName + "_*");

		previousAppMode = settings.getApplicationMode();
		saveAndSet(settings.ROUTE_RECALCULATION_DISTANCE, profile.recalculationDistance);
		saveAndSet((CommonPreference<Boolean>) settings.SPEAK_ROUTE_RECALCULATION, true);
		saveAndSet((CommonPreference<Boolean>) settings.SPEAK_ROUTE_DEVIATION, true);
		saveAndSet((CommonPreference<Boolean>) settings.VOICE_MUTE, false);
		saveAndSet((CommonPreference<String>) settings.VOICE_PROVIDER, "en-tts");
		saveAndSet(settings.ROUTE_SERVICE, routeService);
		saveAndSet(settings.AUTO_ZOOM_MAP, false);
		// the speed cameras sheet is shown on the first route planning and covers the map
		saveAndSet((CommonPreference<Boolean>) settings.SPEED_CAMERAS_ALERT_SHOWED, true);

		initVoice();
		skipAppStartDialogs(app);

		ride = rideFactory.apply(calculateRoute());
		startNavigation();
		feedLocations();

		writeTimeline();
		checkTimeline();
	}

	@NonNull
	private List<LatLon> loadGpxPoints(@NonNull String gpxPath) {
		// the app cannot read /data/local/tmp, the shell copies the track to the app cache
		File file = new File(app.getExternalCacheDir(), new File(gpxPath).getName());
		runShellCommand("cp " + gpxPath + " " + file.getAbsolutePath());
		GpxFile gpxFile = SharedUtil.loadGpxFile(file);
		file.delete();
		List<LatLon> points = new ArrayList<>();
		for (WptPt point : gpxFile.getAllSegmentsPoints()) {
			points.add(new LatLon(point.getLat(), point.getLon()));
		}
		assertTrue("No track points in " + gpxPath, points.size() > 1);
		return points;
	}

	private void initVoice() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		instrumentation.runOnMainSync(() -> {
			settings.setApplicationMode(profile.mode);
			app.initVoiceCommandPlayer(app, profile.mode, latch::countDown, false, false, true, false);
		});
		assertTrue("Voice player is not initialized", latch.await(30, TimeUnit.SECONDS));
		assertTrue("Voice player is not initialized", app.getPlayer() != null);
	}

	@NonNull
	private List<Location> calculateRoute() throws Throwable {
		Location start = createLocation(profile.start, 0, 0);
		instrumentation.runOnMainSync(() -> app.getLocationProvider().setCustomLocation(start, TimeUnit.MINUTES.toMillis(1)));
		instrumentation.runOnMainSync(() -> {
			app.getTargetPointsHelper().navigateToPoint(profile.end, false, -1);
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
		assertTrue("The route is calculated for another profile: " + routingHelper.getAppMode(), routingHelper.getAppMode() == profile.mode);
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

	private void feedLocations() {
		rideStartTime = SystemClock.elapsedRealtime();
		synchronized (fedLocations) {
			fedLocations[0] = null;
			fedLocations[1] = null;
			fedLocations[2] = null;
		}
		for (int second = 0; second < ride.locations.size(); second++) {
			Location location = new Location(ride.locations.get(second));
			location.setTime(System.currentTimeMillis());
			synchronized (fedLocations) {
				fedLocations[2] = fedLocations[1];
				fedLocations[1] = fedLocations[0];
				fedLocations[0] = location;
			}
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
		Location[] locations;
		synchronized (fedLocations) {
			locations = fedLocations.clone();
		}
		// the app checks with the location of the moment the calculation ended, which is one of the latest
		// locations, so the route counts as backward only when all agree
		boolean backward = RoutingHelperUtils.isRouteAgainstMovement(location, routingHelper.getRoute());
		if (backward) {
			for (Location element : locations) {
				if (element != null && !RoutingHelperUtils.isRouteAgainstMovement(element, routingHelper.getRoute())) {
					backward = false;
					break;
				}
			}
		}
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
		// the new route from the rider and the rest of the original route from the rider's position along it
		int second = (int) Math.min(time / 1000, ride.progress.size() - 1);
		int originalRemaining = (int) (ride.pathLength - ride.progress.get(second));
		recalculations.add(new RecalculationEvent(time, newRoute, backward, announced,
				routingHelper.getRoute().getWholeDistance(), originalRemaining));
		if (!announced) {
			showEvent("Silent recalculation" + (backward ? " (backward route)" : ""), "silent_recalc");
		}
	}

	private void checkTimeline() {
		List<String> errors = new ArrayList<>();
		for (long[] deviation : getDeviations()) {
			long start = deviation[0];
			long end = deviation[1] + ANNOUNCEMENT_TAIL_MS;
			String name = String.format(Locale.US, "Deviation %d-%d s", start / 1000, deviation[1] / 1000);
			List<RecalculationEvent> deviationRecalculations = getRecalculations(start, end);
			if (deviationRecalculations.isEmpty() && deviation[1] - deviation[0] > MAX_SILENT_RECALCULATION_MS) {
				errors.add(name + ": no recalculation");
			}
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
				if (event.time >= start && event.time <= end && isRoutePrompt(event)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean isRoutePrompt(@NonNull VoiceEvent event) {
		return event.commands.startsWith(ROUTE_RECALC) || event.commands.startsWith(OFF_ROUTE);
	}

	// the longest time from a silent recalculation to the next route_recalc or off_route prompt in a deviation
	private long getLongestSilence(long start, long end) {
		long longest = 0;
		for (RecalculationEvent recalculation : getRecalculations(start, end)) {
			if (recalculation.announced) {
				continue;
			}
			long next = end;
			synchronized (voiceEvents) {
				for (VoiceEvent event : voiceEvents) {
					if (event.time >= recalculation.time && event.time < next && isRoutePrompt(event)) {
						next = event.time;
					}
				}
			}
			longest = Math.max(longest, next - recalculation.time);
		}
		return longest;
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

	// time intervals when the rider is farther from the original route than the recalculation distance
	@NonNull
	private List<long[]> getDeviations() {
		List<long[]> deviations = new ArrayList<>();
		long start = -1;
		for (int second = 0; second < ride.offsets.size(); second++) {
			boolean deviated = ride.offsets.get(second) > profile.recalculationDistance;
			if (deviated && start < 0) {
				start = second * 1000L;
			} else if (!deviated && start >= 0) {
				deviations.add(new long[] {start, second * 1000L});
				start = -1;
			}
		}
		if (start >= 0) {
			deviations.add(new long[] {start, ride.offsets.size() * 1000L});
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
	private static Location locationAt(@NonNull List<Location> path, double distance, double offset, float speed) {
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
				return createLocation(shifted, bearing, speed);
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
		if (screenshotExecutor.isShutdown()) {
			return;
		}
		String fileName = String.format(Locale.US, "%s_%03ds_%s.png", caseName, elapsed() / 1000, fileSuffix);
		app.showShortToastMessage(message);
		try {
			screenshotExecutor.execute(() -> {
				// navigation start zooms the map out after a while, so the zoom is set for every screenshot
				app.runInUIThread(() -> app.getOsmandMap().getMapView().setIntZoom(MAP_ZOOM));
				SystemClock.sleep(500);
				runShellCommand("screencap -p " + TARGET_DIR + "/" + fileName);
			});
		} catch (RejectedExecutionException ignored) {
			// the executor is stopped in tearDown, the event is not needed then
		}
	}

	private void writeTimeline() {
		StringBuilder text = new StringBuilder();
		text.append("# ").append(caseName).append(", ").append(profile.mode.getRouteService())
				.append(", ").append(profile.name).append(" ").append(Math.round(profile.speed * 3.6f)).append(" km/h")
				.append(", recalculation distance ").append((int) profile.recalculationDistance).append(" m")
				.append(", ").append(rideDescription).append("\n");
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
						+ (event.announced ? ", announced" : ", silent")
						+ ", length " + event.length + " m, original remaining " + event.originalRemaining + " m";
				rows.add(new Object[] {event.time, "route", details});
			}
		}
		for (long[] deviation : getDeviations()) {
			rows.add(new Object[] {deviation[0], "deviation", "start, offset > " + (int) profile.recalculationDistance + " m"});
			long silence = getLongestSilence(deviation[0], deviation[1] + ANNOUNCEMENT_TAIL_MS);
			rows.add(new Object[] {deviation[1], "deviation", "end, longest silent route change " + silence / 1000 + " s"});
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
		file.delete();
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
		preferenceStates.add(new PreferenceState<>(preference, profile.mode));
		preference.setModeValue(profile.mode, value);
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

	private static final class Profile {

		final ApplicationMode mode;
		final String name;
		final float speed;
		final float recalculationDistance;
		final int offset;
		final LatLon start;
		final LatLon end;

		Profile(@NonNull ApplicationMode mode, @NonNull String name, float speed, float recalculationDistance,
		        int offset, @NonNull LatLon start, @NonNull LatLon end) {
			this.mode = mode;
			this.name = name;
			this.speed = speed;
			this.recalculationDistance = recalculationDistance;
			this.offset = offset;
			this.start = start;
			this.end = end;
		}
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
		private final int length;
		private final int originalRemaining;

		RecalculationEvent(long time, boolean newRoute, boolean backward, boolean announced,
		                   int length, int originalRemaining) {
			this.time = time;
			this.newRoute = newRoute;
			this.backward = backward;
			this.announced = announced;
			this.length = length;
			this.originalRemaining = originalRemaining;
		}
	}

	// one location per second, its distance from the original route and the progress along it
	private static class Ride {

		private final List<Location> locations = new ArrayList<>();
		private final List<Double> offsets = new ArrayList<>();
		private final List<Double> progress = new ArrayList<>();
		private final double pathLength;

		private Ride(@NonNull List<Location> path) {
			double length = 0;
			for (int i = 1; i < path.size(); i++) {
				length += path.get(i - 1).distanceTo(path.get(i));
			}
			pathLength = length;
		}

		private void add(@NonNull Location location, double offset, double distance) {
			locations.add(location);
			offsets.add(offset);
			progress.add(distance);
		}

		@NonNull
		static Ride generated(@NonNull List<Location> path, @NonNull int[][] phases, @NonNull Profile profile) {
			Ride ride = new Ride(path);
			int seconds = 0;
			for (int[] phase : phases) {
				seconds += phase[0];
			}
			assertTrue("Route is too short: " + (int) ride.pathLength + " m", ride.pathLength > profile.speed * seconds);
			for (int second = 0; second <= seconds; second++) {
				double offset = offsetAt(phases, second) * profile.offset;
				ride.add(locationAt(path, profile.speed * second, offset, profile.speed), offset, profile.speed * second);
			}
			return ride;
		}

		@NonNull
		static Ride fromPoints(@NonNull List<Location> path, @NonNull List<LatLon> points) {
			Ride ride = new Ride(path);
			for (int i = 0; i < points.size(); i++) {
				// the direction to the next point, points are one second apart
				LatLon from = points.get(i == points.size() - 1 ? i - 1 : i);
				LatLon to = points.get(i == points.size() - 1 ? i : i + 1);
				Location fromLocation = new Location("", from.getLatitude(), from.getLongitude());
				Location toLocation = new Location("", to.getLatitude(), to.getLongitude());
				Location location = createLocation(points.get(i), fromLocation.bearingTo(toLocation), fromLocation.distanceTo(toLocation));

				double offset = Double.MAX_VALUE;
				double distance = 0;
				double passed = 0;
				for (int j = 1; j < path.size(); j++) {
					Location a = path.get(j - 1);
					Location b = path.get(j);
					double d = MapUtils.getOrthogonalDistance(location.getLatitude(), location.getLongitude(),
							a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
					double segment = a.distanceTo(b);
					if (d < offset) {
						offset = d;
						distance = passed + segment * MapUtils.getProjectionCoeff(location.getLatitude(), location.getLongitude(),
								a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
					}
					passed += segment;
				}
				ride.add(location, offset, distance);
			}
			return ride;
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
