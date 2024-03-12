package net.osmand.plus.views;

import android.graphics.PointF;
import android.view.MotionEvent;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;

import net.osmand.Location;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.MapRendererState;
import net.osmand.core.jni.PointI;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXTrackAnalysis.TrackPointsAnalyser;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.gpx.PointAttributes;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.charts.ChartUtils;
import net.osmand.plus.charts.GPXDataSetAxisType;
import net.osmand.plus.charts.GPXDataSetType;
import net.osmand.plus.charts.OrderedLineDataSet;
import net.osmand.plus.helpers.MapDisplayPositionManager;
import net.osmand.plus.routing.RouteCalculationResult.NextDirectionInfo;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.AutoZoomMap;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.OsmandMapTileView.ManualZoomListener;
import net.osmand.plus.views.OsmandMapTileView.TouchListener;
import net.osmand.plus.views.Zoom.ComplexZoom;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import static net.osmand.gpx.PointAttributes.DEV_ANIMATED_ZOOM;
import static net.osmand.gpx.PointAttributes.DEV_INTERPOLATION_OFFSET_N;
import static net.osmand.gpx.PointAttributes.DEV_RAW_ZOOM;

public class AutoZoomBySpeedHelper implements ManualZoomListener, TouchListener {

	public static final float ZOOM_PER_SECOND = 0.1f;
	public static final float ZOOM_PER_MILLIS = ZOOM_PER_SECOND / 1000f;
	public static final int MIN_ZOOM_DURATION_MILLIS = 1500;
	private static final int SHOW_DRIVING_SECONDS_V2 = 45;
	private static final float MIN_AUTO_ZOOM_SPEED = 7 / 3.6f;

	private static final float FOCUS_PIXEL_RATIO_X = 0.5f;
	private static final float FOCUS_PIXEL_RATIO_Y = 1 / 3f;

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final SpeedFilter speedFilter;

	@Nullable
	private OsmandMapTileView tileView;

	@Nullable
	private RouteDirectionInfo nextTurnInFocus;

	public AutoZoomBySpeedHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		this.speedFilter = new SpeedFilter();
	}

	public void setMapView(@Nullable OsmandMapTileView tileView) {
		if (this.tileView != null) {
			this.tileView.removeManualZoomListener(this);
			this.tileView.removeTouchListener(this);
		}
		this.tileView = tileView;
		if (tileView != null) {
			tileView.addManualZoomChangeListener(this);
			tileView.addTouchListener(this);
		}
	}

	@Nullable
	public ComplexZoom calculateAutoZoomBySpeedV1(@NonNull RotatedTileBox tb, float speed) {
		AutoZoomMap autoZoomScale = settings.AUTO_ZOOM_MAP_SCALE.get();
		float zoomDelta = defineZoomFromSpeed(tb, speed, autoZoomScale);
		if (Math.abs(zoomDelta) < 0.5) {
			return null;
		}

		if (zoomDelta >= 2) {
			zoomDelta -= 1;
		} else if (zoomDelta <= -2) {
			zoomDelta += 1;
		}
		double targetZoom = Math.min(tb.getZoom() + tb.getZoomFloatPart() + zoomDelta, autoZoomScale.maxZoom);
		targetZoom = Math.round(targetZoom * 3) / 3f;
		int newIntegerZoom = (int) Math.round(targetZoom);
		float zPart = (float) (targetZoom - newIntegerZoom);
		return newIntegerZoom > 0 ? new ComplexZoom(newIntegerZoom, zPart) : null;
	}

	private float defineZoomFromSpeed(@NonNull RotatedTileBox tb, float speed, @NonNull AutoZoomMap autoZoomScale) {
		if (speed < MIN_AUTO_ZOOM_SPEED) {
			return 0;
		}
		double visibleDist = tb.getDistance(tb.getCenterPixelX(), 0, tb.getCenterPixelX(), tb.getCenterPixelY());
		float time = speed < 83f / 3.6 ? 60 : 75;
		double distToSee = speed * time / autoZoomScale.coefficient;
		float currentZoom = (float) (tb.getZoom() + tb.getZoomFloatPart() + tb.getZoomAnimation());
		return Zoom.fromDistanceRatio(visibleDist, distToSee, currentZoom) - currentZoom;
	}

	@Nullable
	public ComplexZoom calculateZoomBySpeedToAnimate(@NonNull MapRendererView mapRenderer,
	                                                 @NonNull Location myLocation,
	                                                 @Nullable Float rotationToAnimate,
	                                                 @Nullable NextDirectionInfo nextTurn) {
		float speed = myLocation.getSpeed();
		if (speed < MIN_AUTO_ZOOM_SPEED) {
			return null;
		}

		float filteredSpeed = speedFilter.getFilteredSpeed(speed);
		if (Float.isNaN(filteredSpeed)) {
			return null;
		}

		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		AutoZoomMap autoZoomScale = settings.AUTO_ZOOM_MAP_SCALE.get();

		LatLon myLocationLatLon = new LatLon(myLocation.getLatitude(), myLocation.getLongitude());
		PointI myLocation31 = NativeUtilities.getPoint31FromLatLon(myLocationLatLon);
		float myLocationHeight = NativeUtilities.getLocationHeightOrZero(mapRenderer, myLocation31);
		PointI myLocationPixel = mapRenderer.getState().getFixedPixel();

		float showDistanceToDrive = getShowDistanceToDrive(autoZoomScale, nextTurn, filteredSpeed);
		float rotation = rotationToAnimate != null ? rotationToAnimate : mapView.getRotate();
		LatLon anotherLatLon = MapUtils.rhumbDestinationPoint(myLocationLatLon, showDistanceToDrive, rotation);

		PointI anotherLocation31 = NativeUtilities.getPoint31FromLatLon(anotherLatLon);
		float anotherLocationHeight = NativeUtilities.getLocationHeightOrZero(mapRenderer, anotherLocation31);
		PointI windowSize = mapRenderer.getState().getWindowSize();
		PointI anotherPixel = getFocusPixel(windowSize.getX(), windowSize.getY());

		float expectedSurfaceZoom = mapRenderer.getSurfaceZoomAfterPinch(
				myLocation31, myLocationHeight, myLocationPixel,
				anotherLocation31, anotherLocationHeight, anotherPixel);
		if (expectedSurfaceZoom == -1.0f) {
			return null;
		}

		int minZoom = mapView.getMinZoom();
		int maxZoom = mapView.getMaxZoom();
		Zoom boundedZoom = Zoom.checkZoomBounds(expectedSurfaceZoom, minZoom, maxZoom);
		return ComplexZoom.fromPreferredBase(boundedZoom.getBaseZoom() + boundedZoom.getZoomFloatPart(), mapView.getZoom());
	}

	@Nullable
	public Pair<ComplexZoom, Float> getAnimatedZoomParamsForChart(@NonNull MapRendererView mapRenderer, float currentZoom,
	                                                              double lat, double lon, float heading, float speed) {
		if (speed < MIN_AUTO_ZOOM_SPEED) {
			return null;
		}

		float filteredSpeed = speedFilter.getFilteredSpeed(speed);
		if (Float.isNaN(filteredSpeed)) {
			return null;
		}

		ComplexZoom autoZoom = calculateRawZoomBySpeedForChart(mapRenderer, currentZoom, lat, lon, heading, filteredSpeed);
		if (autoZoom == null) {
			return null;
		}

		return getAutoZoomParams(currentZoom, autoZoom, -1);
	}

	@Nullable
	public ComplexZoom calculateRawZoomBySpeedForChart(@NonNull MapRendererView mapRenderer, float currentZoom, double lat, double lon, float rotation, float speed) {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		MapRendererState state = mapRenderer.getState();

		AutoZoomMap autoZoomScale = settings.AUTO_ZOOM_MAP_SCALE.get();

		PointI fixedLocation31 = NativeUtilities.getPoint31FromLatLon(lat, lon);

		PointI firstLocation31 = fixedLocation31;
		float firstHeightInMeters = NativeUtilities.getLocationHeightOrZero(mapRenderer, firstLocation31);
		PointI firstPixel = state.getFixedPixel();

		float showDistanceToDrive = getShowDistanceToDrive(autoZoomScale, null, speed);
		LatLon secondLatLon = MapUtils.rhumbDestinationPoint(lat, lon, showDistanceToDrive, rotation);
		PointI secondLocation31 = NativeUtilities.getPoint31FromLatLon(secondLatLon);
		float secondHeightInMeters = NativeUtilities.getLocationHeightOrZero(mapRenderer, secondLocation31);
		PointI windowSize = state.getWindowSize();
		PointI secondPixel = getFocusPixel(windowSize.getX(), windowSize.getY());

		float expectedSurfaceZoom = mapRenderer.getSurfaceZoomAfterPinchWithParams(
				fixedLocation31, currentZoom, -rotation,
				firstLocation31, firstHeightInMeters, firstPixel,
				secondLocation31, secondHeightInMeters, secondPixel);

		if (expectedSurfaceZoom == -1.0f) {
			return null;
		}

		int minZoom = mapView.getMinZoom();
		int maxZoom = mapView.getMaxZoom();
		Zoom boundedZoom = Zoom.checkZoomBounds(expectedSurfaceZoom, minZoom, maxZoom);
		return new ComplexZoom(boundedZoom.getBaseZoom(), boundedZoom.getZoomFloatPart());
	}
	@Nullable
	public Pair<ComplexZoom, Float> getAutoZoomParams(float currentZoom, @NonNull ComplexZoom autoZoom, float fixedDurationMillis) {
		if (fixedDurationMillis > 0) {
			return new Pair<>(autoZoom, fixedDurationMillis);
		}

		float zoomDelta = autoZoom.fullZoom() - currentZoom;
		float zoomDuration = Math.abs(zoomDelta) / ZOOM_PER_MILLIS;

		if (zoomDuration < MIN_ZOOM_DURATION_MILLIS) {
			return null;
		}

		return new Pair<>(autoZoom, zoomDuration);
	}

	private float getShowDistanceToDrive(@NonNull AutoZoomMap autoZoomScale,
	                                     @Nullable NextDirectionInfo nextTurn,
	                                     float speed) {
		float showDistanceToDrive = speed * SHOW_DRIVING_SECONDS_V2 / autoZoomScale.coefficient;
		if (nextTurn != null) {
			if (nextTurnInFocus != null && nextTurnInFocus.equals(nextTurn.directionInfo)) {
				showDistanceToDrive = nextTurn.distanceTo;
			} else if (nextTurn.distanceTo < showDistanceToDrive) {
				showDistanceToDrive = nextTurn.distanceTo;
				nextTurnInFocus = nextTurn.directionInfo;
			} else {
				nextTurnInFocus = null;
			}
		} else {
			nextTurnInFocus = null;
		}

		return Math.max(showDistanceToDrive, autoZoomScale.minDistanceToDrive);
	}

	@NonNull
	private PointI getFocusPixel(int pixWidth, int pixHeight) {
		MapDisplayPositionManager displayPositionManager = app.getMapViewTrackingUtilities().getMapDisplayPositionManager();
		PointF originalRatio = new PointF(FOCUS_PIXEL_RATIO_X, FOCUS_PIXEL_RATIO_Y);
		PointF ratio = displayPositionManager.projectRatioToVisibleMapRect(originalRatio);
		if (ratio == null) {
			ratio = originalRatio;
		}
		int pixelX = (int) (ratio.x * pixWidth);
		int pixelY = (int) (ratio.y * pixHeight);
		return new PointI(pixelX, pixelY);
	}

	@Override
	public void onManualZoomChange() {
		nextTurnInFocus = null;
	}

	@Override
	public void onTouchEvent(@NonNull MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			nextTurnInFocus = null;
		}
	}

	@Nullable
	public static TrackPointsAnalyser getTrackPointsAnalyser(@NonNull OsmandApplication app) {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		MapRendererView mapRenderer = mapView.getMapRenderer();

		if (mapRenderer == null) {
			return null;
		}

		AutoZoomBySpeedHelper autoZoomBySpeedHelper = new AutoZoomBySpeedHelper(app);
		return new TrackPointsAnalyser() {

			private float currentRawZoom = mapRenderer.getZoom();

			float currentAnimatedZoom = mapRenderer.getZoom();
			private Pair<ComplexZoom, Float> prevAnimatedZoomParams;

			boolean firstPoint = true;

			@Override
			public void onAnalysePoint(GPXTrackAnalysis analysis, WptPt point, PointAttributes attributes) {
				// First point is skipped is GPS simulation
				if (firstPoint) {
					firstPoint = false;
					return;
				}

				float bearing = Float.isNaN(point.bearing) ? mapView.getRotate() : -point.bearing;

				ComplexZoom rawAutoZoom = autoZoomBySpeedHelper.calculateRawZoomBySpeedForChart(
						mapRenderer, currentRawZoom, point.lat, point.lon, bearing, attributes.speed);
				if (rawAutoZoom != null) {
					attributes.setAttributeValue(DEV_RAW_ZOOM, rawAutoZoom.fullZoom());
					currentRawZoom = rawAutoZoom.fullZoom();

					attributes.setAttributeValue(DEV_RAW_ZOOM, rawAutoZoom.fullZoom());
					if (!analysis.hasData(DEV_RAW_ZOOM)) {
						analysis.setHasData(DEV_RAW_ZOOM, true);
					}
				} else {
					attributes.setAttributeValue(DEV_RAW_ZOOM, 0);
				}

				if (prevAnimatedZoomParams != null) {
					float timeDiffMillis = attributes.timeDiff * 1000;
					float animationTime;
					if (prevAnimatedZoomParams.second < timeDiffMillis) {
						float offsetN = prevAnimatedZoomParams.second / timeDiffMillis;
						attributes.setAttributeValue(DEV_INTERPOLATION_OFFSET_N, offsetN);
						animationTime = prevAnimatedZoomParams.second;
					} else {
						animationTime = timeDiffMillis;
					}

					float zoomDeltaSign = Math.signum(prevAnimatedZoomParams.first.fullZoom() - currentAnimatedZoom);
					float zoomDelta = zoomDeltaSign * animationTime * ZOOM_PER_MILLIS;
					currentAnimatedZoom += zoomDelta;

					float leftDuration = prevAnimatedZoomParams.second - animationTime;
					prevAnimatedZoomParams = new Pair<>(prevAnimatedZoomParams.first, leftDuration);
				}

				attributes.setAttributeValue(DEV_ANIMATED_ZOOM, currentAnimatedZoom);
				if (!analysis.hasData(DEV_ANIMATED_ZOOM)) {
					analysis.setHasData(DEV_ANIMATED_ZOOM, true);
				}

				Pair<ComplexZoom, Float> zoomParams = autoZoomBySpeedHelper.getAnimatedZoomParamsForChart(
						mapRenderer, currentAnimatedZoom, point.lat, point.lon, bearing, attributes.speed);
				if (zoomParams != null) {
					prevAnimatedZoomParams = zoomParams;
				}
			}
		};
	}

	public static void addAvailableGPXDataSetTypes(@NonNull OsmandApplication app,
			@NonNull GPXTrackAnalysis analysis,
			@NonNull List<GPXDataSetType[]> availableTypes) {
		if (!app.getOsmandMap().getMapView().hasMapRenderer()) {
			return;
		}

		if (analysis.hasSpeedData()) {
			if (analysis.hasData(DEV_ANIMATED_ZOOM)) {
				availableTypes.add(new GPXDataSetType[] {GPXDataSetType.ZOOM_ANIMATED, GPXDataSetType.SPEED});
			}
		}
	}

	@Nullable
	public static OrderedLineDataSet getOrderedLineDataSet(@NonNull OsmandApplication app,
	                                                       @NonNull LineChart chart,
	                                                       @NonNull GPXTrackAnalysis analysis,
	                                                       @NonNull GPXDataSetType graphType,
	                                                       @NonNull GPXDataSetAxisType chartAxisType,
	                                                       boolean calcWithoutGaps,
	                                                       boolean useRightAxis) {
		switch (graphType) {
			case ZOOM_NON_ANIMATED:
				if (analysis.hasData(DEV_RAW_ZOOM)) {
					return getZoomDataSet(app, chart, analysis, analysis.pointAttributes, graphType,
							chartAxisType, calcWithoutGaps, useRightAxis);
				}
			case ZOOM_ANIMATED:
				if (analysis.hasData(DEV_ANIMATED_ZOOM)) {
					List<PointAttributes> processedAttributes = postProcessAttributes(analysis.pointAttributes);
					return getZoomDataSet(app, chart, analysis, processedAttributes, graphType,
							chartAxisType, calcWithoutGaps, useRightAxis);
				}
		}
		return null;
	}

	@NonNull
	private static OrderedLineDataSet getZoomDataSet(@NonNull OsmandApplication app,
	                                                 @NonNull LineChart chart,
	                                                 @NonNull GPXTrackAnalysis analysis,
	                                                 @NonNull List<PointAttributes> pointAttributes,
	                                                 @NonNull GPXDataSetType graphType,
	                                                 @NonNull GPXDataSetAxisType chartAxisType,
	                                                 boolean calcWithoutGaps,
	                                                 boolean useRightAxis) {
		OsmandSettings settings = app.getSettings();
		boolean nightMode = !settings.isLightContent();

		float divX = ChartUtils.getDivX(app, chart, analysis, chartAxisType, calcWithoutGaps);

		List<Entry> values = ChartUtils.getPointAttributeValues(graphType.getDataKey(), pointAttributes, chartAxisType, divX, 1f, Float.NaN, calcWithoutGaps);
		OrderedLineDataSet dataSet = new OrderedLineDataSet(values, "", graphType, chartAxisType, !useRightAxis);
		dataSet.setAxisValueFormatter((app1, value) -> OsmAndFormatter.formatValue(value, "", true, 2, app1).value);

		int textColor = ColorUtilities.getColor(app, graphType.getTextColorId(false));
		YAxis yAxis = ChartUtils.getAndEnableYAxis(chart, textColor, useRightAxis);
		yAxis.resetAxisMinimum();
		String mainUnitY = graphType.getMainUnitY(app);
		yAxis.setValueFormatter((value, axis) ->
				OsmAndFormatter.formatValue(value, mainUnitY, true, 2, app).value);

		dataSet.setDivX(divX);
		dataSet.setUnits(mainUnitY);

		int color = ColorUtilities.getColor(app, graphType.getFillColorId(false));
		ChartUtils.setupDataSet(app, dataSet, color, color, true, false, useRightAxis, nightMode);

		return dataSet;
	}

	@NonNull
	private static List<PointAttributes> postProcessAttributes(@NonNull List<PointAttributes> originalAttributes) {
		List<PointAttributes> result = new ArrayList<>();

		for (PointAttributes original : originalAttributes) {
			float offsetN = original.interpolationOffsetN;
			if (offsetN > 0) {
				PointAttributes intermediateAttribute = new PointAttributes(
						original.distance * offsetN,
						original.timeDiff * offsetN,
						original.firstPoint,
						false);
				intermediateAttribute.animatedZoom = original.animatedZoom;

				PointAttributes modifiedAttribute = new PointAttributes(
						original.distance * (1.0f - offsetN),
						original.timeDiff * (1.0f - offsetN),
						false,
						original.lastPoint);
				modifiedAttribute.animatedZoom = original.animatedZoom;

				result.add(intermediateAttribute);
				result.add(modifiedAttribute);
			} else {
				result.add(original);
			}
		}

		return result;
	}

	private static class SpeedFilter {

		private float speedToFilter = Float.NaN;
		private float currentSpeed = Float.NaN;

		public float getFilteredSpeed(float speed) {
			float oldSpeed = speedToFilter;
			speedToFilter = currentSpeed;
			currentSpeed = speed;

			if (Float.isNaN(oldSpeed)) {
				return speedToFilter;
			} else {
				boolean monotonous = currentSpeed >= speedToFilter && speedToFilter >= oldSpeed
						|| currentSpeed <= speedToFilter && speedToFilter <= oldSpeed;
				return monotonous ? speedToFilter : Float.NaN;
			}
		}
	}
}