package net.osmand.plus.measurementtool;

import static net.osmand.plus.measurementtool.MeasurementToolFragment.getSuggestedFileName;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.LocationsHolder;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.routing.GpxApproximator;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.router.RoutePlannerFrontEnd.GpxRouteApproximation;
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
	private GpxApproximator approximator;
	@NonNull
	private final GpxApproximationParams params;
	private final Map<LocationsHolder, GpxRouteApproximation> resultMap = new HashMap<>();
	private GpxApproximationListener listener;


	public GpxApproximationHelper(@NonNull OsmandApplication app,
	                              @NonNull GpxApproximationParams params) {
		this.app = app;
		this.params = params;
	}

	public boolean calculateGpxApproximation(boolean newCalculation) {
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
		gpxApproximator.calculateGpxApproximation(new ResultMatcher<GpxRouteApproximation>() {
			@Override
			public boolean publish(GpxRouteApproximation gpxApproximation) {
				app.runInUIThread(() -> {
					if (!gpxApproximator.isCancelled()) {
						if (gpxApproximation != null) {
							resultMap.put(gpxApproximator.getLocationsHolder(), gpxApproximation);
						}
						if (!calculateGpxApproximation(false)) {
							notifyOnApproximationFinished();
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

	public GpxApproximator createApproximator(@NonNull LocationsHolder locationsHolder) {
		GpxApproximator approximator = null;
		try {
			approximator = new GpxApproximator(app, getAppMode(), getDistanceThreshold(), locationsHolder);
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

	public void notifyOnApproximationFinished() {
		if (listener != null) {
			processApproximationResults();
		}
	}

	private void processApproximationResults() {
		List<GpxRouteApproximation> approximations = new ArrayList<>();
		List<List<WptPt>> points = new ArrayList<>();
		for (LocationsHolder locationsHolder : params.getLocationsHolders()) {
			GpxRouteApproximation approximation = resultMap.get(locationsHolder);
			if (approximation != null) {
				approximations.add(approximation);
				points.add(locationsHolder.getWptPtList());
			}
		}
		listener.processApproximationResults(approximations, points);
	}

	public void setAppMode(@Nullable ApplicationMode appMode, boolean recalculate) {
		if (params.setAppMode(appMode) && recalculate) {
			calculateGpxApproximation(true);
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
			calculateGpxApproximation(true);
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

	public static void approximateGpxSilently(@NonNull OsmandApplication app,
	                                          @NonNull GPXFile gpxFile,
	                                          @NonNull GpxApproximationParams params,
	                                          @NonNull CallbackWithObject<GPXFile> callback) {
		GpxData gpxData = new GpxData(gpxFile);
		MeasurementEditingContext ctx = new MeasurementEditingContext(app);
		ctx.setGpxData(gpxData);
		ctx.setAppMode(params.getAppMode());
		ctx.addPoints();
		params.setTrackPoints(ctx.getSegmentsPoints());

		GpxApproximationHelper helper = new GpxApproximationHelper(app, params);
		helper.setListener(new GpxApproximationListener() {
			@Override
			public void processApproximationResults(@NonNull List<GpxRouteApproximation> approximations,
			                                        @NonNull List<List<WptPt>> points) {
				for (int i = 0; i < approximations.size(); i++) {
					GpxRouteApproximation approximation = approximations.get(i);
					List<WptPt> segment = points.get(i);
					ctx.setPoints(approximation, segment, params.getAppMode(), false);
				}
				String trackName = getSuggestedFileName(app, ctx.getGpxData());
				callback.processResult(ctx.exportGpx(trackName));
			}
		});

		if (helper.canApproximate()) {
			helper.calculateGpxApproximation(true);
		} else {
			callback.processResult(gpxFile);
		}
	}
}
