package net.osmand.plus.routing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.LocationsHolder;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.measurementtool.GpxApproximationFragment;
import net.osmand.plus.routing.RouteProvider.RoutingEnvironment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.router.RouteCalculationProgress;
import net.osmand.router.RoutePlannerFrontEnd;
import net.osmand.router.RoutePlannerFrontEnd.GpxPoint;
import net.osmand.router.RoutePlannerFrontEnd.GpxRouteApproximation;
import net.osmand.search.core.SearchResult;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class GpxApproximator {

	protected static final Log log = PlatformUtil.getLog(GpxApproximator.class);

	private OsmandApplication ctx;
	private RoutingHelper routingHelper;

	private RoutingEnvironment env;
	private GpxRouteApproximation gctx;
	private ApplicationMode mode;
	private LocationsHolder locationsHolder;
	private List<GpxPoint> points;
	private LatLon start;
	private LatLon end;
	private double pointApproximation = 50;

	private ThreadPoolExecutor singleThreadedExecutor;
	private GpxApproximationProgressCallback approximationProgress;
	private Future<?> currentApproximationTask;

	public interface GpxApproximationProgressCallback {

		void start();

		void updateProgress(int progress);

		void finish();
	}

	public GpxApproximator(@NonNull OsmandApplication ctx, @NonNull LocationsHolder locationsHolder) throws IOException {
		this.ctx = ctx;
		this.locationsHolder = locationsHolder;
		this.routingHelper = ctx.getRoutingHelper();
		this.mode = ApplicationMode.CAR;
		if (locationsHolder.getSize() > 1) {
			start = locationsHolder.getLatLon(0);
			end = locationsHolder.getLatLon(locationsHolder.getSize() - 1);
			prepareEnvironment(ctx, mode);
		}
		init();
	}

	public GpxApproximator(@NonNull OsmandApplication ctx, @NonNull ApplicationMode mode, double pointApproximation, @NonNull LocationsHolder locationsHolder) throws IOException {
		this.ctx = ctx;
		this.locationsHolder = locationsHolder;
		this.pointApproximation = pointApproximation;
		this.routingHelper = ctx.getRoutingHelper();
		this.mode = mode;
		if (locationsHolder.getSize() > 1) {
			start = locationsHolder.getLatLon(0);
			end = locationsHolder.getLatLon(locationsHolder.getSize() - 1);
			prepareEnvironment(ctx, mode);
		}
		init();
	}

	private void init() {
		singleThreadedExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
	}

	private void prepareEnvironment(@NonNull OsmandApplication ctx, @NonNull ApplicationMode mode) throws IOException {
		this.env = routingHelper.getRoutingEnvironment(ctx, mode, start, end);
	}

	private GpxRouteApproximation getNewGpxApproximationContext() {
		GpxRouteApproximation newContext = new GpxRouteApproximation(env.getCtx());
		newContext.ctx.calculationProgress = new RouteCalculationProgress();
		newContext.MINIMUM_POINT_APPROXIMATION = pointApproximation;
		return newContext;
	}

	private List<GpxPoint> getPoints() {
		if (points == null) {
			points = routingHelper.generateGpxPoints(env, getNewGpxApproximationContext(), locationsHolder);
		}
		List<GpxPoint> points = new ArrayList<>(this.points.size());
		for (GpxPoint p : this.points) {
			points.add(new GpxPoint(p));
		}
		return points;
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

	public double getPointApproximation() {
		return pointApproximation;
	}

	public void setPointApproximation(double pointApproximation) {
		this.pointApproximation = pointApproximation;
	}

	public LocationsHolder getLocationsHolder() {
		return locationsHolder;
	}

	public GpxApproximationProgressCallback getApproximationProgress() {
		return approximationProgress;
	}

	public void setApproximationProgress(GpxApproximationProgressCallback approximationProgress) {
		this.approximationProgress = approximationProgress;
	}

	public void cancelApproximation() {
		if (gctx != null) {
			gctx.ctx.calculationProgress.isCancelled = true;
		}
	}

	public void calculateGpxApproximation(@NonNull final ResultMatcher<GpxRouteApproximation> resultMatcher) {
		if (gctx != null) {
			gctx.ctx.calculationProgress.isCancelled = true;
		}
		final GpxRouteApproximation gctx = getNewGpxApproximationContext();
		this.gctx = gctx;
		startProgress();
		updateProgress(gctx);
		if (currentApproximationTask != null) {
			currentApproximationTask.cancel(true);
		}
		currentApproximationTask = singleThreadedExecutor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					routingHelper.calculateGpxApproximation(env, gctx, getPoints(), resultMatcher);
				} catch (Exception e) {
					resultMatcher.publish(null);
					log.error(e.getMessage(), e);
				}
			}
		});
	}

	private void startProgress() {
		final GpxApproximationProgressCallback approximationProgress = this.approximationProgress;
		if (approximationProgress != null) {
			approximationProgress.start();
		}
	}

	private void finishProgress() {
		final GpxApproximationProgressCallback approximationProgress = this.approximationProgress;
		if (approximationProgress != null) {
			approximationProgress.finish();
		}
	}

	private void updateProgress(@NonNull final GpxRouteApproximation gctx) {
		final GpxApproximationProgressCallback approximationProgress = this.approximationProgress;
		if (approximationProgress != null) {
			ctx.runInUIThread(new Runnable() {

				@Override
				public void run() {
					RouteCalculationProgress calculationProgress = gctx.ctx.calculationProgress;
					if (!gctx.result.isEmpty() && GpxApproximator.this.gctx == gctx) {
						finishProgress();
					}
					if (gctx.result.isEmpty() && calculationProgress != null && !calculationProgress.isCancelled) {
						float pr = calculationProgress.getLinearProgress();
						approximationProgress.updateProgress((int) pr);
						if (GpxApproximator.this.gctx != gctx) {
							// different calculation started
						} else {
							updateProgress(gctx);
						}
					}
				}
			}, 300);
		}
	}
}
