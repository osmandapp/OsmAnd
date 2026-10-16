package net.osmand.test.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Debug;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.Location;
import net.osmand.NativeLibrary;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteSubregion;
import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.render.NativeOsmandLibrary;
import net.osmand.plus.routing.RoutingEnvironment;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.router.GeneralRouter;
import net.osmand.router.NativeTransportRoutingResult;
import net.osmand.router.RouteCalculationProgress;
import net.osmand.router.RouteResultPreparation.RouteCalcResult;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingContext;
import net.osmand.router.TransportRoutingConfiguration;
import net.osmand.test.common.AndroidTest;
import net.osmand.util.MapUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Device checks for ROUTING_LOCK in {@link NativeLibrary} (OsmAnd-Issues#3396, item 1): native routing,
 * public transport, GPX approximation and native route tile loading share one file descriptor per map,
 * so only one of them may read at a time; a calculation cancelled while it waits for the lock must not
 * enter native code, and the memory guard must not charge the lock holder's memory to the waiting one.
 * The Kyiv maps must be installed; observations go to logcat under {@link #TAG}.
 * {@code -e stressSeconds N} sets the length of the stress case.
 */
@RunWith(AndroidJUnit4.class)
public class NativeRoutingLockDeviceTest extends AndroidTest {

	static final String TAG = "NativeRoutingLockTest";

	// Kyiv: Maidan Nezalezhnosti -> Velyka Vasylkivska near Olimpiiska -> Velyka Vasylkivska near Lybidska
	static final LatLon START = new LatLon(50.45045, 30.52345);
	static final LatLon MID = new LatLon(50.44210, 30.51685);
	static final LatLon END = new LatLon(50.42960, 30.51720);
	// public transport: Maidan -> Livoberezhna
	static final LatLon PT_END = new LatLon(50.45180, 30.59850);

	static final String TASK_CLASS = "net.osmand.plus.routing.RouteRecalculationHelper$RouteRecalculationTask";
	static final long MB = 1 << 20;

	private Object routingLock;

	@Before
	public void setupLock() throws Exception {
		Field f = NativeLibrary.class.getDeclaredField("ROUTING_LOCK");
		f.setAccessible(true);
		routingLock = f.get(null);
		assertNotNull("ROUTING_LOCK is missing", routingLock);
		assertFalse("SAFE_MODE is on, native routing is disabled", settings.SAFE_MODE.get());
		assertNotNull("native library is not loaded", NativeOsmandLibrary.getLoadedLibrary());
	}

	/**
	 * Pedestrian never uses HH routing (RouteCalculationMethod.canProfileUseFastRouting), so a route with an
	 * intermediate point loads its tiles from Java: addSegment -> findRouteSegment -> loadTileData ->
	 * loadRouteRegion. Those native reads must wait for the lock like the planners do.
	 */
	@Test
	public void tileLoadsWaitForRoutingLockHeldByAnotherCalculation() throws Exception {
		RoutingHelper rh = app.getRoutingHelper();
		RoutingEnvironment env = rh.getRoutingEnvironment(app, ApplicationMode.PEDESTRIAN, START, END);
		assertNotNull(env);
		RoutingContext ctx = env.getCtx();
		assertNotNull("routing context has no native library", ctx.nativeLib);
		LockHolder holder = new LockHolder("test-other-calculation");
		SpyLibrary spy = new SpyLibrary(holder);
		ctx.nativeLib = spy;

		AtomicReference<RouteCalcResult> result = new AtomicReference<>();
		AtomicReference<Throwable> error = new AtomicReference<>();
		Thread routeThread = new Thread(() -> {
			try {
				result.set(env.getRouter().searchRoute(ctx, START, END, Collections.singletonList(MID)));
			} catch (Throwable t) {
				error.set(t);
			}
		}, "test-route");
		routeThread.start();
		assertTrue("route never reached needRequestPrivateAccessRouting",
				spy.privateAccessChecked.await(60, TimeUnit.SECONDS));
		// the private-access check released the lock; another calculation (PT, GPX) takes it now
		holder.acquire();
		spy.continueAfterPrivateAccess.countDown();

		long deadline = System.currentTimeMillis() + 60_000;
		while (spy.tileLoads.get() == 0 && routeThread.isAlive() && System.currentTimeMillis() < deadline) {
			Thread.sleep(20);
		}
		Thread.sleep(2000); // time enough for all tile loads of three points if they do not wait
		int unlockedLoads = spy.tileLoadsCompletedWhileLockHeldElsewhere.get();
		int loads = spy.tileLoads.get();
		String top = topFrames(routeThread.getStackTrace(), 2);
		log("tiles: while another thread held ROUTING_LOCK the route thread completed " + unlockedLoads
				+ " loadRouteRegion reads (" + loads + " calls), then was " + routeThread.getState() + " at " + top);

		holder.release();
		routeThread.join(180_000);
		assertNull("route failed: " + error.get(), error.get());
		assertNotNull(result.get());
		assertTrue("route failed: " + result.get().getError(), result.get().isCorrect());
		log("tiles: route after release: " + signature(result.get()));
		assertTrue("route never reached loadRouteRegion", loads > 0);
		assertEquals("loadRouteRegion read the shared descriptor " + unlockedLoads
				+ " times while another thread held ROUTING_LOCK", 0, unlockedLoads);
	}

	/**
	 * stopCalculation() while the route waits for the lock: once the lock is free the cancelled calculation
	 * must return without running native routing. The wait itself is not cancellable (the thread stays
	 * blocked until the holder is done); that is the declared cost of the lock.
	 */
	@Test
	public void cancelledRouteDoesNotEnterNativeRoutingAfterTheLockIsFreed() throws Exception {
		RoutingHelper rh = app.getRoutingHelper();
		ApplicationMode origMode = rh.getAppMode();
		LockHolder holder = new LockHolder("test-gpx-approximation");
		holder.acquire(); // a GPX approximation or PT calculation holds the lock
		try {
			rh.setAppMode(ApplicationMode.CAR);
			rh.setFinalAndCurrentLocation(END, Collections.singletonList(MID), location(START));
			Thread worker = awaitRouteTaskThread(30_000);
			log("cancel: navigation route is waiting: " + worker.getState() + " at " + topFrames(worker.getStackTrace(), 2));

			rh.stopCalculationImmediately();
			Thread.sleep(500);
			log("cancel: after stopCalculation: isRouteBeingCalculated=" + rh.isRouteBeingCalculated()
					+ ", routing thread " + worker.getState() + " at " + topFrames(worker.getStackTrace(), 2));

			holder.release();
			long released = System.currentTimeMillis();
			boolean enteredNative = false;
			boolean taskRunning = true;
			while (taskRunning && System.currentTimeMillis() < released + 120_000) {
				StackTraceElement[] st = worker.getStackTrace();
				taskRunning = hasFrame(st, TASK_CLASS, "run");
				if (hasFrame(st, "NativeLibrary", "nativeRouting") || hasFrame(st, "NativeLibrary", "nativeNeedRequestPrivateAccessRouting")) {
					enteredNative = true;
				}
				Thread.sleep(5);
			}
			long took = System.currentTimeMillis() - released;
			log("cancel: cancelled task finished " + took + " ms after the lock was released, entered native routing: " + enteredNative);
			assertFalse("the cancelled task still ran native routing after the lock was released", enteredNative);
			assertFalse("the cancelled task did not finish within 120 s", taskRunning);
		} finally {
			holder.release();
			rh.clearCurrentRoute(null, null);
			rh.setAppMode(origMode);
		}
	}

	/**
	 * NativeRoutingMemoryGuard measures the process-wide native heap. While a route waits for the lock, that
	 * heap belongs to the calculation holding it, so the guard must not cancel the waiting route for it.
	 * The other calculation's memory is stood in for by bitmaps, which live in the native heap since Android 8.
	 */
	@Test
	public void memoryGuardIgnoresMemoryWhileRouteWaitsForTheLock() throws Exception {
		RoutingHelper rh = app.getRoutingHelper();
		ApplicationMode origMode = rh.getAppMode();
		ActivityManager.MemoryInfo info = memoryInfo(app);
		long budget = guardBudget(info);
		assumeTrue("needs about " + (budget + 512 * MB) / MB + " MB of free RAM to exceed the guard budget",
				info.availMem > budget + 512 * MB);
		List<Bitmap> ballast = new ArrayList<>();
		LockHolder holder = new LockHolder("test-pt-calculation");
		holder.acquire();
		try {
			rh.setAppMode(ApplicationMode.CAR);
			rh.setFinalAndCurrentLocation(END, Collections.singletonList(MID), location(START));
			Thread worker = awaitRouteTaskThread(30_000);
			log("guard: route waiting on the lock, native heap " + Debug.getNativeHeapAllocatedSize() / MB
					+ " MB, guard budget " + budget / MB + " MB");

			long target = budget + 128 * MB;
			while (Debug.getNativeHeapAllocatedSize() < target && ballast.size() < 24) {
				try {
					ballast.add(Bitmap.createBitmap(8192, 8192, Bitmap.Config.ARGB_8888));
				} catch (OutOfMemoryError e) {
					log("guard: bitmap allocation failed after " + ballast.size() + " bitmaps: " + e);
					break;
				}
			}
			long peak = Debug.getNativeHeapAllocatedSize();
			log("guard: native heap pushed to " + peak / MB + " MB with " + ballast.size() + " bitmaps");
			assumeTrue("could not push the native heap over the guard budget", peak > budget);
			Thread.sleep(1500); // the guard polls every 200 ms
			for (Bitmap b : ballast) {
				b.recycle();
			}
			ballast.clear();
			System.gc();
			Thread.sleep(500);
			long afterFree = Debug.getNativeHeapAllocatedSize();
			log("guard: memory released: native heap " + afterFree / MB + " MB, routing thread still " + worker.getState()
					+ " at " + topFrames(worker.getStackTrace(), 2));
			assertTrue("native heap did not drop back under the budget", afterFree < budget);

			holder.release();
			long released = System.currentTimeMillis();
			while (rh.isRouteBeingCalculated() && System.currentTimeMillis() < released + 180_000) {
				Thread.sleep(50);
			}
			long took = System.currentTimeMillis() - released;
			boolean calculated = rh.isRouteCalculated();
			log("guard: route finished " + took + " ms after the lock was released: calculated=" + calculated
					+ ", error=" + rh.getLastRouteCalcError());
			assertTrue("the route that waited for the lock was not calculated: " + rh.getLastRouteCalcError(), calculated);
		} finally {
			for (Bitmap b : ballast) {
				b.recycle();
			}
			holder.release();
			rh.clearCurrentRoute(null, null);
			rh.setAppMode(origMode);
		}
	}

	/** The gate must not disable the guard: a route over budget inside native code is still stopped. */
	@Test
	public void memoryGuardStillStopsRouteOverBudgetInsideNativeCode() throws Exception {
		RoutingHelper rh = app.getRoutingHelper();
		ApplicationMode origMode = rh.getAppMode();
		ActivityManager.MemoryInfo info = memoryInfo(app);
		long budget = guardBudget(info);
		assumeTrue("needs about " + (budget + 512 * MB) / MB + " MB of free RAM to exceed the guard budget",
				info.availMem > budget + 512 * MB);
		List<Bitmap> ballast = new ArrayList<>();
		LockHolder holder = new LockHolder("test-pt-calculation");
		holder.acquire();
		try {
			rh.setAppMode(ApplicationMode.CAR);
			rh.setFinalAndCurrentLocation(END, Collections.singletonList(MID), location(START));
			awaitRouteTaskThread(30_000);
			holder.release(); // the route runs native code now; its memory is stood in for by bitmaps
			long released = System.currentTimeMillis();
			while (rh.isRouteBeingCalculated() && Debug.getNativeHeapAllocatedSize() < budget + 128 * MB && ballast.size() < 24) {
				try {
					ballast.add(Bitmap.createBitmap(8192, 8192, Bitmap.Config.ARGB_8888));
				} catch (OutOfMemoryError e) {
					log("guard2: bitmap allocation failed after " + ballast.size() + " bitmaps: " + e);
					break;
				}
			}
			long peak = Debug.getNativeHeapAllocatedSize();
			boolean stillRunning = rh.isRouteBeingCalculated();
			log("guard2: native heap " + peak / MB + " MB with " + ballast.size() + " bitmaps, route still running: " + stillRunning);
			assumeTrue("the route finished before the heap went over budget", stillRunning && peak > budget);
			while (rh.isRouteBeingCalculated() && System.currentTimeMillis() < released + 180_000) {
				Thread.sleep(50);
			}
			String error = rh.getLastRouteCalcError();
			String oom = app.getString(R.string.route_calculation_out_of_memory);
			log("guard2: route finished " + (System.currentTimeMillis() - released) + " ms after the lock was released: calculated="
					+ rh.isRouteCalculated() + ", error=" + error);
			assertTrue("the guard did not stop a route over budget inside native code: calculated=" + rh.isRouteCalculated()
					+ ", error=" + error, !rh.isRouteCalculated() && error != null && error.contains(oom));
		} finally {
			for (Bitmap b : ballast) {
				b.recycle();
			}
			holder.release();
			rh.clearCurrentRoute(null, null);
			rh.setAppMode(origMode);
		}
	}

	/** Pedestrian route with an intermediate point against native public transport, both in a loop. */
	@Test
	public void pedestrianRouteWithIntermediateAgainstPublicTransportRouting() throws Exception {
		RoutingHelper rh = app.getRoutingHelper();
		NativeOsmandLibrary lib = NativeOsmandLibrary.getLoadedLibrary();
		initNativeFiles(lib, START, MID, END, PT_END);

		String routeBase = signature(pedestrianRoute(rh));
		int ptBase = publicTransport(lib).length;
		log("stress: baseline alone: pedestrian route " + routeBase + ", public transport results " + ptBase);
		assertTrue("public transport found no route alone", ptBase > 0);

		long seconds = stressSeconds();
		long end = System.currentTimeMillis() + seconds * 1000;
		AtomicInteger routeIters = new AtomicInteger(), ptIters = new AtomicInteger();
		AtomicInteger routeMismatch = new AtomicInteger(), ptMismatch = new AtomicInteger();
		AtomicReference<Throwable> error = new AtomicReference<>();
		Thread routes = new Thread(() -> {
			try {
				while (System.currentTimeMillis() < end && error.get() == null) {
					String s = signature(pedestrianRoute(rh));
					routeIters.incrementAndGet();
					if (!routeBase.equals(s)) {
						routeMismatch.incrementAndGet();
						Log.w(TAG, "stress: pedestrian route differs: " + s + " vs " + routeBase);
					}
				}
			} catch (Throwable t) {
				error.set(t);
			}
		}, "test-routes");
		Thread pt = new Thread(() -> {
			try {
				while (System.currentTimeMillis() < end && error.get() == null) {
					int n = publicTransport(lib).length;
					ptIters.incrementAndGet();
					if (n != ptBase) {
						ptMismatch.incrementAndGet();
						Log.w(TAG, "stress: public transport results differ: " + n + " vs " + ptBase);
					}
				}
			} catch (Throwable t) {
				error.set(t);
			}
		}, "test-pt");
		routes.start();
		pt.start();
		routes.join(600_000);
		pt.join(600_000);
		log("stress: " + seconds + " s: pedestrian routes " + routeIters + " (" + routeMismatch + " wrong), public transport "
				+ ptIters + " (" + ptMismatch + " wrong), error " + error.get());
		assertNull("a calculation threw: " + error.get(), error.get());
		assertEquals("results differed from the same request made alone", 0, routeMismatch.get() + ptMismatch.get());
	}

	// ---------------------------------------------------------------------------------------------

	private RouteCalcResult pedestrianRoute(RoutingHelper rh) throws Exception {
		RoutingEnvironment env = rh.getRoutingEnvironment(app, ApplicationMode.PEDESTRIAN, START, END);
		assertNotNull(env);
		RouteCalcResult res = env.getRouter().searchRoute(env.getCtx(), START, END, Collections.singletonList(MID));
		assertTrue("pedestrian route failed: " + res.getError(), res.isCorrect());
		return res;
	}

	private NativeTransportRoutingResult[] publicTransport(NativeOsmandLibrary lib) {
		ApplicationMode mode = ApplicationMode.PUBLIC_TRANSPORT;
		RoutingConfiguration.Builder config = app.getRoutingConfigForMode(mode);
		GeneralRouter prouter = config.getRouter(mode.getRoutingProfile());
		assertNotNull(prouter);
		Map<String, String> params = new TreeMap<>();
		TransportRoutingConfiguration cfg = new TransportRoutingConfiguration(prouter, params);
		NativeTransportRoutingResult[] res = lib.runNativePTRouting(
				MapUtils.get31TileNumberX(START.getLongitude()), MapUtils.get31TileNumberY(START.getLatitude()),
				MapUtils.get31TileNumberX(PT_END.getLongitude()), MapUtils.get31TileNumberY(PT_END.getLatitude()),
				cfg, new RouteCalculationProgress());
		return res == null ? new NativeTransportRoutingResult[0] : res;
	}

	private void initNativeFiles(NativeOsmandLibrary lib, LatLon... points) {
		int left = Integer.MAX_VALUE, right = 0, top = Integer.MAX_VALUE, bottom = 0;
		for (LatLon p : points) {
			int x = MapUtils.get31TileNumberX(p.getLongitude());
			int y = MapUtils.get31TileNumberY(p.getLatitude());
			left = Math.min(left, x);
			right = Math.max(right, x);
			top = Math.min(top, y);
			bottom = Math.max(bottom, y);
		}
		app.getResourceManager().getRenderer().checkInitialized(15, lib, left, right, bottom, top);
	}

	private static long stressSeconds() {
		String s = InstrumentationRegistry.getArguments().getString("stressSeconds");
		return s == null ? 120 : Long.parseLong(s);
	}

	private static String signature(RouteCalcResult r) {
		double dist = 0;
		for (RouteSegmentResult s : r.getList()) {
			dist += s.getDistance();
		}
		return r.getList().size() + " segments / " + Math.round(dist) + " m";
	}

	private static Location location(LatLon l) {
		Location loc = new Location("test", l.getLatitude(), l.getLongitude());
		loc.setTime(System.currentTimeMillis());
		return loc;
	}

	private static ActivityManager.MemoryInfo memoryInfo(Context ctx) {
		ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
		ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
		am.getMemoryInfo(info);
		return info;
	}

	/** Same formula as NativeRoutingMemoryGuard.getBudget() on r5.4. */
	private static long guardBudget(ActivityManager.MemoryInfo info) {
		long budget = Math.min(3072 * MB, info.totalMem - 1024 * MB);
		long floor = Math.max(512 * MB, info.totalMem / 2);
		return Math.min(3072 * MB, Math.max(floor, budget));
	}

	/** The route calculation thread, once it is blocked in NativeLibrary. */
	@NonNull
	private static Thread awaitRouteTaskThread(long timeoutMs) throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMs;
		Thread last = null;
		while (System.currentTimeMillis() < deadline) {
			for (Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()) {
				if (hasFrame(e.getValue(), TASK_CLASS, "run")) {
					last = e.getKey();
					if (e.getKey().getState() == Thread.State.BLOCKED && hasFrame(e.getValue(), "NativeLibrary", null)) {
						return e.getKey();
					}
				}
			}
			Thread.sleep(20);
		}
		fail("routing thread did not block in NativeLibrary within " + timeoutMs + " ms; last seen "
				+ (last == null ? "none" : last.getState() + " at " + topFrames(last.getStackTrace(), 4)));
		throw new IllegalStateException();
	}

	private static boolean hasFrame(StackTraceElement[] st, @Nullable String classPart, @Nullable String method) {
		for (StackTraceElement f : st) {
			if ((classPart == null || f.getClassName().contains(classPart))
					&& (method == null || method.equals(f.getMethodName()))) {
				return true;
			}
		}
		return false;
	}

	private static String topFrames(StackTraceElement[] st, int n) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < Math.min(n, st.length); i++) {
			if (i > 0) {
				sb.append(" <- ");
			}
			String cls = st[i].getClassName();
			sb.append(cls.substring(cls.lastIndexOf('.') + 1)).append('.').append(st[i].getMethodName());
		}
		return sb.toString();
	}

	private static void log(String s) {
		Log.i(TAG, s);
	}

	/** Holds ROUTING_LOCK on its own thread, standing in for a running PT or GPX calculation. */
	private final class LockHolder {
		final CountDownLatch acquired = new CountDownLatch(1);
		final CountDownLatch release = new CountDownLatch(1);
		final AtomicBoolean holding = new AtomicBoolean();
		final Thread thread;

		LockHolder(String name) {
			thread = new Thread(() -> {
				synchronized (routingLock) {
					holding.set(true);
					acquired.countDown();
					try {
						release.await(10, TimeUnit.MINUTES);
					} catch (InterruptedException ignored) {
					}
					holding.set(false);
				}
			}, name);
			thread.setDaemon(true);
		}

		void acquire() throws InterruptedException {
			thread.start();
			assertTrue("could not take ROUTING_LOCK", acquired.await(30, TimeUnit.SECONDS));
		}

		void release() throws InterruptedException {
			release.countDown();
			thread.join(10_000);
		}
	}

	/** The real library with counters; the native methods are static, so a second instance is equivalent. */
	private final class SpyLibrary extends NativeOsmandLibrary {
		final LockHolder other;
		final AtomicInteger tileLoads = new AtomicInteger();
		final AtomicInteger tileLoadsCompletedWhileLockHeldElsewhere = new AtomicInteger();
		final CountDownLatch privateAccessChecked = new CountDownLatch(1);
		final CountDownLatch continueAfterPrivateAccess = new CountDownLatch(1);

		SpyLibrary(LockHolder other) {
			this.other = other;
		}

		@Override
		public boolean needRequestPrivateAccessRouting(RoutingContext ctx, int[] x31Coordinates, int[] y31Coordinates) {
			boolean res = super.needRequestPrivateAccessRouting(ctx, x31Coordinates, y31Coordinates);
			privateAccessChecked.countDown();
			try {
				continueAfterPrivateAccess.await(60, TimeUnit.SECONDS);
			} catch (InterruptedException ignored) {
			}
			return res;
		}

		@Override
		public NativeRouteSearchResult loadRouteRegion(RouteSubregion sub, boolean loadObjects, RouteCalculationProgress progress) {
			tileLoads.incrementAndGet();
			NativeRouteSearchResult res = super.loadRouteRegion(sub, loadObjects, progress);
			// a read that finished while the other thread still holds the lock went past it
			if (other.holding.get()) {
				tileLoadsCompletedWhileLockHeldElsewhere.incrementAndGet();
			}
			return res;
		}
	}
}
