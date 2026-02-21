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
	private GpxApproximator currentApproximator;
	private GpxApproximationListener listener;


	public GpxApproximationHelper(@NonNull OsmandApplication app, @NonNull GpxApproximationParams params) {
		this.app = app;
		this.params = params;
	}

	public void calculateGpxApproximationAsync() {
		if (currentApproximator != null) {
			currentApproximator.cancelApproximation();
			currentApproximator = null;
		}
		notifyOnNewCalculation();
		List<GpxApproximator> approximateList = new ArrayList<>();
		for (LocationsHolder locationsHolder : params.getLocationsHolders()) {
			GpxApproximator approximate = createApproximator(locationsHolder);
			if (approximate != null) {
				approximateList.add(approximate);
			}
		}
		Map<LocationsHolder, GpxRouteApproximation> approximateResult = new HashMap<>();
		notifyOnApproximationStarted();
		try {
			approximateMultipleGpxAsync(approximateList, approximateResult);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}

	private void approximateMultipleGpxAsync(List<GpxApproximator> approximationsToDo, Map<LocationsHolder, GpxRouteApproximation> approximateResult) {
		// Runs in UI thread
		if (approximationsToDo.size() > 0) {
			GpxApproximator gpxApproximator = approximationsToDo.remove(0);
			this.currentApproximator = gpxApproximator;
			gpxApproximator.calculateGpxApproximationAsync(new ResultMatcher<>() {
				@Override
				public boolean publish(GpxRouteApproximation gpxApproximation) {
					app.runInUIThread(() -> {
						// wait for first result as final
						if (!gpxApproximator.isCancelled()) {
							approximateResult.put(gpxApproximator.getLocationsHolder(), gpxApproximation);
							// call chain of leftApproximations to do
							approximateMultipleGpxAsync(approximationsToDo, approximateResult);
						}
					});
					return true;
				}
				@Override
				public boolean isCancelled() {
					return false;
				}
			});
		} else {
			notifyOnApproximationFinished(processApproximationResults(approximateResult));
		}
	}

	public GpxApproximator createApproximator(@NonNull LocationsHolder holder) {
		GpxApproximator approximator = null;
		try {
			approximator = new GpxApproximator(app, getAppMode(), getDistanceThreshold(), holder);
			approximator.setApproximationListener(listener);
			approximator.setMode(getAppMode());
			approximator.setPointApproximation(getDistanceThreshold());
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
	private Pair<List<GpxRouteApproximation>, List<List<WptPt>>> processApproximationResults(Map<LocationsHolder, GpxRouteApproximation> approximateResult) {
		List<GpxRouteApproximation> approximations = new ArrayList<>();
		List<List<WptPt>> points = new ArrayList<>();
		for (LocationsHolder locationsHolder : params.getLocationsHolders()) {
			GpxRouteApproximation approximation = approximateResult.get(locationsHolder);
			if (approximation != null) {
				approximations.add(approximation);
				points.add(SharedUtil.kWptPtList(locationsHolder.getWptPtList()));
			}
		}
		return Pair.create(approximations, points);
	}

	public void setAppMode(@Nullable ApplicationMode appMode, boolean recalculate) {
		if (params.setAppMode(appMode) && recalculate) {
			calculateGpxApproximationAsync();
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
			calculateGpxApproximationAsync();
		}
	}

	public boolean isSameApproximator(@NonNull GpxApproximator approximator) {
		return this.currentApproximator == approximator;
	}

	public boolean canApproximate() {
		return !Algorithms.isEmpty(params.getLocationsHolders());
	}

	public void cancelApproximationIfPossible() {
		if (currentApproximator != null) {
			currentApproximator.cancelApproximation();
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
			helper.calculateGpxApproximationAsync();
		} else {
			callback.processResult(gpxFile);
		}
	}

	@NonNull
	public static GpxFile approximateGpxSync(@NonNull OsmandApplication app, @NonNull GpxFile gpxFile,
	                                         @NonNull GpxApproximationParams params,
	                                         @Nullable GpxApproximationHelper helper) {
		MeasurementEditingContext context = createEditingContext(app, gpxFile, params);
		if (helper == null) {
			helper = new GpxApproximationHelper(app, params);
		}
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
		Map<LocationsHolder, GpxRouteApproximation> approximateResult = new HashMap<>();
		for (LocationsHolder holder : params.getLocationsHolders()) {
			GpxApproximator approximator = createApproximator(holder);
			if (approximator != null) {
				try {
					this.currentApproximator = approximator;
					GpxRouteApproximation gctx = approximator.getNewGpxApproximationContext();
					approximator.calculateGpxApproximationSync(gctx, new ResultMatcher<>() {
						@Override
						public boolean publish(GpxRouteApproximation approximation) {
							approximateResult.put(approximator.getLocationsHolder(), approximation);
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
		return processApproximationResults(approximateResult);
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
			context.setPoints(i, approximation, segment, params.getAppMode(), false);
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
