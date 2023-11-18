package net.osmand.plus.views;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;

import net.osmand.core.android.MapRendererView;
import net.osmand.data.RotatedTileBox;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXTrackAnalysis.TrackPointsAnalyser;
import net.osmand.gpx.PointAttributes;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.charts.ChartUtils;
import net.osmand.plus.charts.GPXDataSetAxisType;
import net.osmand.plus.charts.GPXDataSetType;
import net.osmand.plus.charts.OrderedLineDataSet;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.AutoZoomMap;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.Zoom.ComplexZoom;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AutoZoomBySpeedUtils {

	public static final float ZOOM_PER_SECOND = 0.1f;
	public static final int MIN_ZOOM_DURATION_MILLIS = 1500;
	public static final long FIXED_ZOOM_DURATION_MILLIS = 1000;
	private static final int SHOW_DRIVING_SECONDS = 60;

	@Nullable
	public static ComplexZoom calculateAutoZoomBySpeed(@NonNull OsmandApplication app, @NonNull RotatedTileBox tb, float speed) {
		OsmandSettings settings = app.getSettings();

		float zoomProximityCoeff = settings.AUTO_ZOOM_MAP_SCALE.get().coefficient;
		float zoomDelta = defineZoomFromSpeed(tb, speed, zoomProximityCoeff);
		if (Math.abs(zoomDelta) < 0.5) {
			return null;
		}

		if (zoomDelta >= 2) {
			zoomDelta -= 1;
		} else if (zoomDelta <= -2) {
			zoomDelta += 1;
		}
		double targetZoom = Math.min(tb.getZoom() + tb.getZoomFloatPart() + zoomDelta, settings.AUTO_ZOOM_MAP_SCALE.get().maxZoom);
		targetZoom = Math.round(targetZoom * 3) / 3f;
		int newIntegerZoom = (int) Math.round(targetZoom);
		float zPart = (float) (targetZoom - newIntegerZoom);
		return newIntegerZoom > 0 ? new ComplexZoom(newIntegerZoom, zPart) : null;
	}

	private static float defineZoomFromSpeed(@NonNull RotatedTileBox tb, float speed, float zoomProximityCoeff) {
		if (speed < 7f / 3.6) {
			return 0;
		}
		double visibleDist = tb.getDistance(tb.getCenterPixelX(), 0, tb.getCenterPixelX(), tb.getCenterPixelY());
		float time = speed < 83f / 3.6 ? 60 : 75;
		double distToSee = speed * time / zoomProximityCoeff;
		float currentZoom = (float) (tb.getZoom() + tb.getZoomFloatPart() + tb.getZoomAnimation());
		return Zoom.fromDistanceRatio(visibleDist, distToSee, currentZoom);
	}

	@Nullable
	public static ComplexZoom autoZoomBySpeed(@NonNull OsmandApplication app, @NonNull RotatedTileBox tileBox, float speed) {
		if (speed < 7 / 3.6) {
			return null;
		}

		OsmandMapTileView mapView = app.getOsmandMap().getMapView();

		AutoZoomMap autoZoomScale = app.getSettings().AUTO_ZOOM_MAP_SCALE.get();
		double visibleDist = tileBox.getDistance(tileBox.getCenterPixelX(), 0, tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
		float distanceToDrive = speed * SHOW_DRIVING_SECONDS / autoZoomScale.coefficient;
		float currentZoom = (float) (tileBox.getZoom() + tileBox.getZoomFloatPart() + tileBox.getZoomAnimation());
		float expectedZoom = Zoom.fromDistanceRatio(visibleDist, distanceToDrive, currentZoom);

		int minZoom = mapView.getMinZoom();
		int maxZoom = Math.min(mapView.getMaxZoom(), autoZoomScale.maxZoom);
		Zoom boundedZoom = Zoom.checkZoomBounds(expectedZoom, minZoom, maxZoom);
		return new ComplexZoom(boundedZoom.getBaseZoom(), boundedZoom.getZoomFloatPart());
	}

	@Nullable
	public static TrackPointsAnalyser getTrackPointsAnalyser(@NonNull OsmandApplication app) {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		MapRendererView mapRenderer = mapView.getMapRenderer();
		RotatedTileBox tileBox = mapView.getRotatedTileBox();

		if (mapRenderer == null) {
			return null;
		}

		return (analysis, point, attributes) -> {
			ComplexZoom autoZoom = AutoZoomBySpeedUtils.autoZoomBySpeed(app, tileBox, attributes.speed);

			if (autoZoom != null) {
				tileBox.setZoomAndAnimation(autoZoom.base, 0.0, autoZoom.floatPart);
			}

			float expectedZoom = (float) (tileBox.getZoom() + tileBox.getZoomFloatPart() + tileBox.getZoomAnimation());
			attributes.setAttributeValue(PointAttributes.DEV_ZOOM, expectedZoom);
			if (!analysis.hasData(PointAttributes.DEV_ZOOM)) {
				analysis.setHasData(PointAttributes.DEV_ZOOM, true);
			}
		};
	}

	public static void addAvailableGPXDataSetTypes(@NonNull OsmandApplication app,
			@NonNull GPXTrackAnalysis analysis,
			@NonNull List<GPXDataSetType[]> availableTypes) {
		boolean cameraElevationAvailable = app.getOsmandMap().getMapView().hasMapRenderer()
				&& analysis.hasData(PointAttributes.DEV_ZOOM);
		if (cameraElevationAvailable) {
			if (analysis.hasSpeedData()) {
				availableTypes.add(new GPXDataSetType[] {GPXDataSetType.ZOOM_NON_ANIMATED, GPXDataSetType.SPEED});
				availableTypes.add(new GPXDataSetType[] {GPXDataSetType.ZOOM_ANIMATED, GPXDataSetType.SPEED});
			}
			availableTypes.add(new GPXDataSetType[] {GPXDataSetType.ZOOM_NON_ANIMATED, GPXDataSetType.ZOOM_ANIMATED});
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
				if (analysis.hasData(PointAttributes.DEV_ZOOM)) {
					return getZoomDataSet(app, chart, analysis, analysis.pointAttributes, graphType,
							chartAxisType, calcWithoutGaps, useRightAxis);
				}
			case ZOOM_ANIMATED:
				if (analysis.hasData(PointAttributes.DEV_ZOOM)) {
					List<PointAttributes> animatedAttributes = simulateZoomAttributesAnimation(analysis.pointAttributes);
					return getZoomDataSet(app, chart, analysis, animatedAttributes, graphType,
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
		YAxis yAxis = ChartUtils.getYAxis(chart, textColor, useRightAxis);
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
	private static List<PointAttributes> simulateZoomAttributesAnimation(@NonNull List<PointAttributes> original) {
		List<PointAttributes> result = new ArrayList<>();

		float currentZoom = original.get(0).zoom;
		PointAttributes overridenCurrentAttribute = null;

		for (int i = 1; i < original.size(); i++) {
			PointAttributes currentAttributes = overridenCurrentAttribute != null
					? overridenCurrentAttribute
					: original.get(i);

			float zoomToAnimate = currentAttributes.zoom;

			PointAttributes currentAnimatedAttributes = new PointAttributes(
					currentAttributes.distance,
					currentAttributes.timeDiff,
					currentAttributes.firstPoint,
					currentAttributes.lastPoint
			);
			currentAnimatedAttributes.zoom = currentZoom;
			result.add(currentAnimatedAttributes);

			if (i + 1 == original.size()) {
				break;
			}

			PointAttributes nextAttributes = original.get(i + 1);

			float zoomDelta = zoomToAnimate - currentZoom;
			float allowedZoomDelta = ZOOM_PER_SECOND * nextAttributes.timeDiff;

			if (Math.abs(zoomDelta) < allowedZoomDelta) {
				float secondsToZoom = Math.abs(zoomDelta) / ZOOM_PER_SECOND;
				float offsetN = secondsToZoom / nextAttributes.timeDiff;

				PointAttributes intermediateAttribute = new PointAttributes(
						nextAttributes.distance * offsetN,
						secondsToZoom,
						nextAttributes.firstPoint,
						false
				);
				intermediateAttribute.zoom = zoomToAnimate;
				result.add(intermediateAttribute);

				overridenCurrentAttribute = new PointAttributes(
						nextAttributes.distance * (1.0f - offsetN),
						nextAttributes.timeDiff - secondsToZoom,
						false,
						nextAttributes.lastPoint
				);
				overridenCurrentAttribute.zoom = nextAttributes.zoom;

				currentZoom = zoomToAnimate;
			} else {
				currentZoom += Math.signum(zoomDelta) * Math.min(Math.abs(zoomDelta), allowedZoomDelta);
				overridenCurrentAttribute = null;
			}
		}

		return result;
	}
}