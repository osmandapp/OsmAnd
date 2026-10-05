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
 * calculated and cancels the calculation (the native loop polls isCancelled) when it grows
 * more than the budget, so the user gets an error instead of a crash and a restart loop.
 * Completed calculations in crash reports grow native memory by 112 MB (p99), 724 MB at most;
 * a pedestrian A* route needs about 1 GB for 40 km, 2.8 GB for 90 km and 5.9 GB for 110 km.
 */
class NativeRoutingMemoryGuard {

	private static final Log log = PlatformUtil.getLog(NativeRoutingMemoryGuard.class);

	private static final long MB = 1 << 20;
	private static final long MIN_BUDGET = 1024 * MB;
	private static final long MAX_BUDGET = 4096 * MB;
	private static final double BUDGET_RAM_SHARE = 0.3;
	private static final long CHECK_INTERVAL_MS = 200;

	private final RouteCalculationProgress progress;
	private final long budget;
	private final long startAllocated;
	private volatile boolean running = true;
	private volatile boolean exceeded;
	private volatile long peakGrowth;

	private NativeRoutingMemoryGuard(@NonNull RouteCalculationProgress progress, long budget) {
		this.progress = progress;
		this.budget = budget;
		this.startAllocated = Debug.getNativeHeapAllocatedSize();
	}

	@NonNull
	static NativeRoutingMemoryGuard start(@NonNull Context ctx, @NonNull RouteCalculationProgress progress) {
		NativeRoutingMemoryGuard guard = new NativeRoutingMemoryGuard(progress, getBudget(ctx));
		Thread thread = new Thread(guard::watch, "RoutingMemoryGuard");
		thread.setDaemon(true);
		thread.start();
		return guard;
	}

	private static long getBudget(@NonNull Context ctx) {
		long totalMem = 0;
		ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
		if (am != null) {
			ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
			am.getMemoryInfo(info);
			totalMem = info.totalMem;
		}
		return Math.max(MIN_BUDGET, Math.min(MAX_BUDGET, (long) (totalMem * BUDGET_RAM_SHARE)));
	}

	private void watch() {
		while (running && !progress.isCancelled) {
			long growth = Debug.getNativeHeapAllocatedSize() - startAllocated;
			peakGrowth = Math.max(peakGrowth, growth);
			if (growth > budget) {
				exceeded = true;
				progress.isCancelled = true;
				log.error("Route calculation stopped: native memory grew by " + growth / MB
						+ " MB, budget " + budget / MB + " MB");
				return;
			}
			try {
				Thread.sleep(CHECK_INTERVAL_MS);
			} catch (InterruptedException e) {
				return;
			}
		}
	}

	boolean isExceeded() {
		return exceeded;
	}

	/**
	 * Stops watching. Returns true if the guard cancelled the calculation; the cancel flag is then
	 * cleared, otherwise RouteRecalculationTask drops the result silently as a user cancel.
	 */
	boolean stop() {
		if (!running) {
			return exceeded;
		}
		running = false;
		if (exceeded) {
			progress.isCancelled = false;
		}
		log.info("Route calculation native memory growth " + peakGrowth / MB + " MB, budget " + budget / MB + " MB");
		return exceeded;
	}
}
