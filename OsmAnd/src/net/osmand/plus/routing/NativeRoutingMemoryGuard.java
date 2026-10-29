package net.osmand.plus.routing;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.router.RouteCalculationProgress;

import org.apache.commons.logging.Log;

/**
 * Native routing (A* and HH) has no limit on its search graph, only on the road tile cache.
 * A long route without HH (pedestrian, truck, HH fallback) grows native memory to 4-10 GB and
 * the process dies inside the allocator. The guard watches the native heap while a route is
 * calculated and cancels the calculation (the native loop polls isCancelled) when it reaches
 * the budget, so the user gets an error instead of a crash and a restart loop.
 */
class NativeRoutingMemoryGuard {

	private static final Log log = PlatformUtil.getLog(NativeRoutingMemoryGuard.class);

	private static final long MB = 1 << 20;
	private static final long MIN_BUDGET = 512 * MB;
	private static final long MAX_BUDGET = 3072 * MB;
	private static final long RESERVED_RAM = 1024 * MB;
	private static final long CHECK_INTERVAL_MS = 200;

	private final RouteCalculationProgress progress;
	private final long budget;
	private Thread thread;
	private volatile boolean exceeded;
	private volatile long peakAllocated;

	private NativeRoutingMemoryGuard(@NonNull RouteCalculationProgress progress, long budget) {
		this.progress = progress;
		this.budget = budget;
	}

	@NonNull
	static NativeRoutingMemoryGuard start(@NonNull Context ctx, @NonNull RouteCalculationProgress progress) {
		NativeRoutingMemoryGuard guard = new NativeRoutingMemoryGuard(progress, getBudget(ctx));
		guard.thread = new Thread(guard::watch, "RoutingMemoryGuard");
		guard.thread.setDaemon(true);
		guard.thread.start();
		return guard;
	}

	/**
	 * Device RAM minus 1 GB for the system and the rest of the app, never more than 3 GB and never
	 * less than half of the device RAM. The half-RAM floor only changes the 1-2 GB band: there the
	 * reserve alone leaves less than a single public transport route needs on a dense network
	 * (measured 485 MB of growth for a 4.7 km trip in Hamburg), so the flat 512 MB floor stopped
	 * ordinary routes. Below 1 GB the floor stays 512 MB, and from 2 GB up the reserve already
	 * leaves more than half of RAM, so every other device keeps the budget it has today.
	 */
	private static long getBudget(@NonNull Context ctx) {
		long totalMem = 0;
		ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
		if (am != null) {
			ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
			am.getMemoryInfo(info);
			totalMem = info.totalMem;
		}
		long budget = Math.min(MAX_BUDGET, totalMem - RESERVED_RAM);
		long floor = Math.max(MIN_BUDGET, totalMem / 2);
		return Math.min(MAX_BUDGET, Math.max(floor, budget));
	}

	private void watch() {
		while (!progress.isCancelled) {
			long allocated = Debug.getNativeHeapAllocatedSize();
			peakAllocated = Math.max(peakAllocated, allocated);
			if (allocated > budget) {
				exceeded = true;
				progress.memoryLimitExceeded = true;
				progress.isCancelled = true;
				log.error("Route calculation stopped: native heap " + allocated / MB
						+ " MB, budget " + budget / MB + " MB");
				break;
			}
			try {
				Thread.sleep(CHECK_INTERVAL_MS);
			} catch (InterruptedException e) {
				break;
			}
		}
		log.info("Route calculation native heap peak " + peakAllocated / MB + " MB, budget " + budget / MB + " MB");
	}

	boolean isExceeded() {
		return exceeded;
	}

	/**
	 * Stops watching and waits for the watcher, so the flags do not change after this returns.
	 * Returns true if the guard cancelled the calculation; isCancelled stays set, and
	 * RouteRecalculationTask tells this stop from a requested one by memoryLimitExceeded.
	 */
	boolean stop() {
		thread.interrupt();
		try {
			thread.join();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return exceeded;
	}
}
