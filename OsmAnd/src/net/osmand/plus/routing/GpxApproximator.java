package net.osmand.plus.routing;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.LocationsHolder;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.measurementtool.GpxApproximationListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.router.GpxRouteApproximation;
import net.osmand.router.RouteCalculationProgress;
import net.osmand.router.RoutePlannerFrontEnd.GpxPoint;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

public class GpxApproximator {

	public static final int DEFAULT_POINT_APPROXIMATION = 50;

	protected static final Log log = PlatformUtil.getLog(GpxApproximator.class);

	private final OsmandApplication ctx;
	private final RoutingHelper routingHelper;

	private RoutingEnvironment env;
	private GpxRouteApproximation gctx;
	private ApplicationMode mode;
	private final LocationsHolder locationsHolder;
	private List<GpxPoint> points;
	private LatLon start;
	private LatLon end;
	private float pointApproximation = DEFAULT_POINT_APPROXIMATION;
	private GpxApproximationListener listener;
	private Runnable approximationTask;

	private static final ThreadPoolExecutor SINGLE_THREAD_EXECUTOR = new ThreadPoolExecutor(1, 1, 0L, MILLISECONDS, new LinkedBlockingQueue<>());

	public GpxApproximator(@NonNull OsmandApplication ctx, @NonNull LocationsHolder locationsHolder) throws IOException {
		this.ctx = ctx;
		this.locationsHolder = locationsHolder;
		this.routingHelper = ctx.getRoutingHelper();
		this.mode = ApplicationMode.CAR;
		initEnvironment(mode, locationsHolder);
	}

	public GpxApproximator(@NonNull OsmandApplication ctx, @NonNull ApplicationMode mode, float pointApproximation, @NonNull LocationsHolder locationsHolder) throws IOException {
		this.ctx = ctx;
		this.locationsHolder = locationsHolder;
		this.pointApproximation = pointApproximation;
		this.routingHelper = ctx.getRoutingHelper();
		this.mode = mode;
		initEnvironment(mode, locationsHolder);
	}

	private void initEnvironment(@NonNull ApplicationMode mode, @NonNull LocationsHolder locationsHolder) throws IOException {
		if (locationsHolder.getSize() > 1) {
			start = locationsHolder.getLatLon(0);
			end = locationsHolder.getLatLon(locationsHolder.getSize() - 1);
			prepareEnvironment(ctx, mode);
		}
	}

	private void prepareEnvironment(@NonNull OsmandApplication ctx, @NonNull ApplicationMode mode) throws IOException {
		this.env = routingHelper.getRoutingEnvironment(ctx, mode, start, end);
	}

	public GpxRouteApproximation getNewGpxApproximationContext() {
		GpxRouteApproximation newContext = new GpxRouteApproximation(env.getCtx());
		newContext.ctx.calculationProgress = new RouteCalculationProgress();
		newContext.ctx.config.minPointApproximation = pointApproximation;
		return newContext;
	}

	private List<GpxPoint> getPoints() {
		if (points == null) {
			points = routingHelper.generateGpxPoints(env, getNewGpxApproximationContext(), locationsHolder);
		}
		List<GpxPoint> pointsCopy = new ArrayList<>(this.points.size());
		for (GpxPoint p : this.points) {
			pointsCopy.add(new GpxPoint(p));
		}
		return pointsCopy;
	}

	public ApplicationMode getMode() {
		return mode;
	}

	public void setMode(ApplicationMode mode) throws IOException {
		if (this.mode != mode) {
			this.mode = mode;
			prepareEnvironment(ctx, mode);
		}
	}

	public void setPointApproximation(float pointApproximation) {
		this.pointApproximation = pointApproximation;
	}

	public LocationsHolder getLocationsHolder() {
		return locationsHolder;
	}

	public void setApproximationListener(@Nullable GpxApproximationListener listener) {
		this.listener = listener;
	}

	public boolean isCancelled() {
		return gctx != null && gctx.ctx.calculationProgress.isCancelled;
	}

	public void cancelApproximation() {
		if (gctx != null) {
			gctx.ctx.calculationProgress.isCancelled = true;
		}
	}

	public void calculateGpxApproximationAsync(@NonNull ResultMatcher<GpxRouteApproximation> resultMatcher) {
		if (gctx != null) {
			gctx.ctx.calculationProgress.isCancelled = true;
		}
		GpxRouteApproximation gctx = getNewGpxApproximationContext();
		this.gctx = gctx;
		notifyOnStart();
		notifyUpdateProgress(gctx);
		approximationTask = () -> {
			calculateGpxApproximationSync(gctx, resultMatcher);
			approximationTask = null;
		};
		SINGLE_THREAD_EXECUTOR.submit(approximationTask);
	}

	public void calculateGpxApproximationSync(@NonNull GpxRouteApproximation gctx,
	                                          @NonNull ResultMatcher<GpxRouteApproximation> matcher) {
		try {
			routingHelper.calculateGpxApproximation(env, gctx, getPoints(), matcher, false);
		} catch (Exception e) {
			matcher.publish(null);
			log.error(e.getMessage(), e);
		}
	}

	private void notifyOnStart() {
		if (listener != null) {
			listener.onSegmentApproximationStarted(this);
		}
	}

	private void notifyOnUpdateProgress(float progress) {
		if (listener != null) {
			listener.updateApproximationProgress(this, (int) progress);
		}
	}

	private void notifyOnFinish() {
		if (listener != null) {
			listener.onSegmentApproximationFinished(this);
		}
	}

	private void notifyUpdateProgress(@NonNull GpxRouteApproximation gctx) {
		if (listener != null) {
			ctx.runInUIThread(() -> {
				RouteCalculationProgress progressInfo = gctx.ctx.calculationProgress;
				if (approximationTask == null && GpxApproximator.this.gctx == gctx) {
					notifyOnFinish();
				}
				if (approximationTask != null && progressInfo != null && !progressInfo.isCancelled) {
					notifyOnUpdateProgress(progressInfo.getApproximationProgress());
					if (GpxApproximator.this.gctx == gctx) {
						notifyUpdateProgress(gctx);
					}
				}
			}, 300);
		}
	}
}
