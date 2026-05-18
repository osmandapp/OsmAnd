package net.osmand.plus.measurementtool;

import static net.osmand.plus.measurementtool.MeasurementToolFragment.getSuggestedFileName;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.LocationsHolder;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.routing.GpxApproximator;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.router.GpxRouteApproximation;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GpxApproximationHelper {

	private static final Log LOG = PlatformUtil.getLog(GpxApproximationHelper.class);

	private final OsmandApplication app;
	private final GpxApproximationParams params;
	private final Map<LocationsHolder, GpxRouteApproximation> resultMap = new HashMap<>();

	private GpxApproximator approximator;
	private GpxApproximationListener listener;


	public GpxApproximationHelper(@NonNull OsmandApplication app, @NonNull GpxApproximationParams params) {
		this.app = app;
		this.params = params;
	}

	public boolean calculateGpxApproximationAsync(boolean newCalculation) {
		if (newCalculation) {
			if (approximator != null) {
				approximator.cancelApproximation();
				approximator = null;
			}
			resultMap.clear();
			notifyOnNewCalculation();
		}
		GpxApproximator gpxApproximator = null;
		for (LocationsHolder locationsHolder : params.getLocationsHolders()) {
			if (!resultMap.containsKey(locationsHolder)) {
				gpxApproximator = createApproximator(locationsHolder);
				break;
			}
		}
		if (gpxApproximator != null) {
			try {
				this.approximator = gpxApproximator;
				gpxApproximator.setMode(getAppMode());
				gpxApproximator.setPointApproximation(getDistanceThreshold());
				approximateGpx(gpxApproximator);
				return true;
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
		}
		return false;
	}

	private void approximateGpx(@NonNull GpxApproximator gpxApproximator) {
		notifyOnApproximationStarted();
		gpxApproximator.calculateGpxApproximationAsync(new ResultMatcher<>() {
			@Override
			public boolean publish(GpxRouteApproximation gpxApproximation) {
				app.runInUIThread(() -> {
					if (!gpxApproximator.isCancelled()) {
						resultMap.put(gpxApproximator.getLocationsHolder(), gpxApproximation);
						if (!calculateGpxApproximationAsync(false)) {
							notifyOnApproximationFinished(processApproximationResults());
						}
					}
				});
				return true;
			}

			@Override
			public boolean isCancelled() {
				return false;
			}
		});
	}

	public GpxApproximator createApproximator(@NonNull LocationsHolder holder) {
		GpxApproximator approximator = null;
		try {
			approximator = new GpxApproximator(app, getAppMode(), getDistanceThreshold(), holder);
			approximator.setApproximationListener(listener);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		return approximator;
	}

	public void setListener(@Nullable GpxApproximationListener listener) {
		this.listener = listener;
	}

	public void notifyOnNewCalculation() {
		if (listener != null) {
			listener.onNewApproximation();
		}
	}

	public void notifyOnApproximationStarted() {
		if (listener != null) {
			listener.onApproximationStarted();
		}
	}

	public void notifyOnApproximationFinished(@NonNull Pair<List<GpxRouteApproximation>, List<List<WptPt>>> pair) {
		if (listener != null) {
			listener.processApproximationResults(pair.first, pair.second);
		}
	}

	@NonNull
	private Pair<List<GpxRouteApproximation>, List<List<WptPt>>> processApproximationResults() {
		List<GpxRouteApproximation> approximations = new ArrayList<>();
		List<List<WptPt>> points = new ArrayList<>();
		for (LocationsHolder locationsHolder : params.getLocationsHolders()) {
			GpxRouteApproximation approximation = resultMap.get(locationsHolder);
			if (approximation != null) {
				approximations.add(approximation);
				points.add(SharedUtil.kWptPtList(locationsHolder.getWptPtList()));
			}
		}
		return Pair.create(approximations, points);
	}

	public void setAppMode(@Nullable ApplicationMode appMode, boolean recalculate) {
		if (params.setAppMode(appMode) && recalculate) {
			calculateGpxApproximationAsync(true);
		}
	}

	@NonNull
	public ApplicationMode getAppMode() {
		return params.getAppMode();
	}

	@NonNull
	public String getModeKey() {
		ApplicationMode appMode = getAppMode();
		return appMode.getStringKey();
	}

	public int getDistanceThreshold() {
		return params.getDistanceThreshold();
	}

	public void setDistanceThreshold(int threshold, boolean recalculate) {
		if (params.setDistanceThreshold(threshold) && recalculate) {
			calculateGpxApproximationAsync(true);
		}
	}

	public boolean isSameApproximator(@NonNull GpxApproximator approximator) {
		return this.approximator == approximator;
	}

	public boolean canApproximate() {
		return !Algorithms.isEmpty(params.getLocationsHolders());
	}

	public void cancelApproximationIfPossible() {
		if (approximator != null) {
			approximator.cancelApproximation();
		}
	}

	public static void approximateGpxAsync(@NonNull OsmandApplication app,
	                                       @NonNull GpxFile gpxFile,
	                                       @NonNull GpxApproximationParams params,
	                                       @NonNull CallbackWithObject<GpxFile> callback) {
		MeasurementEditingContext context = createEditingContext(app, gpxFile, params);
		GpxApproximationHelper helper = new GpxApproximationHelper(app, params);
		helper.setListener(new GpxApproximationListener() {
			@Override
			public void processApproximationResults(@NonNull List<GpxRouteApproximation> approximations,
			                                        @NonNull List<List<WptPt>> points) {
				GpxFile approximatedGpx = createApproximatedGpx(app, context, params, approximations, points);
				callback.processResult(approximatedGpx);
			}
		});

		if (helper.canApproximate()) {
			helper.calculateGpxApproximationAsync(true);
		} else {
			callback.processResult(gpxFile);
		}
	}

	@NonNull
	public static GpxFile approximateGpxSync(@NonNull OsmandApplication app, @NonNull GpxFile gpxFile,
	                                         @NonNull GpxApproximationParams params) {
		MeasurementEditingContext context = createEditingContext(app, gpxFile, params);
		GpxApproximationHelper helper = new GpxApproximationHelper(app, params);
		if (helper.canApproximate()) {
			Pair<List<GpxRouteApproximation>, List<List<WptPt>>> pair = helper.calculateGpxApproximationSync();
			GpxFile approximatedGpx = createApproximatedGpx(app, context, params, pair.first, pair.second);
			if (approximatedGpx != null && approximatedGpx.isAttachedToRoads()) {
				return approximatedGpx;
			}
		}
		return gpxFile;
	}

	@NonNull
	public Pair<List<GpxRouteApproximation>, List<List<WptPt>>> calculateGpxApproximationSync() {
		for (LocationsHolder holder : params.getLocationsHolders()) {
			GpxApproximator approximator = createApproximator(holder);
			if (approximator != null) {
				try {
					GpxRouteApproximation gctx = approximator.getNewGpxApproximationContext();
					approximator.calculateGpxApproximationSync(gctx, new ResultMatcher<>() {
						@Override
						public boolean publish(GpxRouteApproximation approximation) {
							resultMap.put(approximator.getLocationsHolder(), approximation);
							return true;
						}

						@Override
						public boolean isCancelled() {
							return false;
						}
					});
				} catch (Exception e) {
					LOG.error(e.getMessage(), e);
				}
			}
		}
		return processApproximationResults();
	}

	@Nullable
	public static GpxFile createApproximatedGpx(@NonNull OsmandApplication app,
	                                            @NonNull MeasurementEditingContext context,
	                                            @NonNull GpxApproximationParams params,
	                                            @NonNull List<GpxRouteApproximation> approximations,
	                                            @NonNull List<List<WptPt>> points) {
		for (int i = 0; i < approximations.size(); i++) {
			GpxRouteApproximation approximation = approximations.get(i);
			List<WptPt> segment = points.get(i);
			context.setPoints(approximation, segment, params.getAppMode(), false);
		}
		String trackName = getSuggestedFileName(app, context.getGpxData());
		return context.exportGpx(trackName);
	}

	@NonNull
	private static MeasurementEditingContext createEditingContext(@NonNull OsmandApplication app,
	                                                              @NonNull GpxFile gpxFile,
	                                                              @NonNull GpxApproximationParams params) {
		MeasurementEditingContext ctx = new MeasurementEditingContext(app);
		ctx.setGpxData(new GpxData(gpxFile));
		ctx.setAppMode(params.getAppMode());
		ctx.addPoints();
		params.setTrackPoints(ctx.getSegmentsPoints());

		return ctx;
	}
}
