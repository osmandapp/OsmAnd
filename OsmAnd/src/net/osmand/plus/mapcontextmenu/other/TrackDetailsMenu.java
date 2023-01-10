package net.osmand.plus.mapcontextmenu.other;

import static net.osmand.plus.helpers.GpxUiHelper.HOUR_IN_MILLIS;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXUtilities.TrkSegment;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.Location;
import net.osmand.core.android.MapRendererView;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetAxisType;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetType;
import net.osmand.plus.helpers.GpxUiHelper.OrderedLineDataSet;
import net.osmand.plus.myplaces.ui.GPXItemPagerAdapter;
import net.osmand.plus.track.GpxMarkerView;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.GPXLayer;
import net.osmand.plus.views.mapwidgets.TopToolbarController;
import net.osmand.plus.views.mapwidgets.TopToolbarView;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrackDetailsMenu {

	public static final int MAX_DISTANCE_LOCATION_PROJECTION = 20; // in meters

	@Nullable
	private MapActivity mapActivity;
	@Nullable
	private GpxDisplayItem gpxItem;
	@Nullable
	private SelectedGpxFile selectedGpxFile;
	@Nullable
	private TrackDetailsBarController toolbarController;
	@Nullable
	private TrkSegment segment;
	@Nullable
	private TrackChartPoints trackChartPoints;
	@Nullable
	private List<LatLon> xAxisPoints;
	private int topMarginPx;
	private boolean visible;
	private boolean hidding;
	private Location myLocation;

	private boolean fitTrackOnMapForbidden;

	@Nullable
	public MapActivity getMapActivity() {
		return mapActivity;
	}

	public void setMapActivity(@Nullable MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		if (mapActivity != null) {
			if (topMarginPx == 0) {
				topMarginPx = AndroidUtils.dpToPx(mapActivity, 48f);
			}
		}
	}

	@Nullable
	public GpxDisplayItem getGpxItem() {
		return gpxItem;
	}

	public void setGpxItem(@NonNull GpxDisplayItem gpxItem) {
		this.gpxItem = gpxItem;
	}

	@Nullable
	public SelectedGpxFile getSelectedGpxFile() {
		return selectedGpxFile;
	}

	public void setSelectedGpxFile(@NonNull SelectedGpxFile selectedGpxFile) {
		this.selectedGpxFile = selectedGpxFile;
	}

	public boolean isVisible() {
		return visible;
	}

	public void show() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && getGpxItem() != null) {
			visible = true;
			TrackDetailsMenuFragment.showInstance(mapActivity);
		}
	}

	public void dismiss(boolean backPressed) {
		TrackDetailsMenuFragment fragment = getMenuFragment();
		if (fragment != null) {
			fragment.dismiss(backPressed);
		}
	}

	public void hide(boolean backPressed) {
		TrackDetailsMenuFragment fragment = getMenuFragment();
		if (fragment != null) {
			hidding = true;
			fragment.dismiss(backPressed);
		} else {
			reset();
		}
	}

	public void updateMyLocation(View mainView, Location location) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			LineChart chart = mainView.findViewById(R.id.chart);
			GpxDisplayItem gpxItem = getGpxItem();
			TrkSegment segment = getTrackSegment(chart);
			LineData lineData = chart.getLineData();
			List<ILineDataSet> ds = lineData != null ? lineData.getDataSets() : null;
			if (ds != null && ds.size() > 0 && gpxItem != null && segment != null) {
				MapRendererView mapRenderer = mapActivity.getMapView().getMapRenderer();
				RotatedTileBox tb = mapActivity.getMapView().getCurrentRotatedTileBox();
				PointF pixel = NativeUtilities.getPixelFromLatLon(mapRenderer, tb, location.getLatitude(), location.getLongitude());
				int mx = (int) pixel.x;
				int my = (int) pixel.y;
				int r = (int) (MAX_DISTANCE_LOCATION_PROJECTION * tb.getPixDensity());
				Pair<WptPt, WptPt> points = GPXLayer.findPointsNearSegment(
						mapRenderer, tb, segment.points, r, mx, my);
				if (points != null) {
					LatLon latLon = NativeUtilities.getLatLonFromPixel(mapRenderer, tb, mx, my);
					if (latLon != null) {
						gpxItem.locationOnMap = GPXLayer.createProjectionPoint(points.first, points.second, latLon);
					}

					float pos;
					if (gpxItem.chartAxisType == GPXDataSetAxisType.TIME ||
							gpxItem.chartAxisType == GPXDataSetAxisType.TIMEOFDAY) {
						pos = gpxItem.locationOnMap.time / 1000f;
					} else {
						double totalDistance = 0;
						int index = segment.points.indexOf(points.first);
						if (index != -1) {
							WptPt previousPoint = null;
							for (int i = 0; i < index; i++) {
								WptPt currentPoint = segment.points.get(i);
								if (previousPoint != null) {
									totalDistance += MapUtils.getDistance(previousPoint.lat, previousPoint.lon, currentPoint.lat, currentPoint.lon);
								}
								previousPoint = currentPoint;
							}
							totalDistance += MapUtils.getDistance(gpxItem.locationOnMap.lat, gpxItem.locationOnMap.lon, points.first.lat, points.first.lon);
						}
						pos = (float) (totalDistance / ((OrderedLineDataSet) ds.get(0)).getDivX());
					}

					float lowestVisibleX = chart.getLowestVisibleX();
					float highestVisibleX = chart.getHighestVisibleX();
					float nextVisibleX = lowestVisibleX + (pos - gpxItem.chartHighlightPos);
					float oneFourthDiff = (highestVisibleX - lowestVisibleX) / 4f;
					if (pos > oneFourthDiff) {
						nextVisibleX = pos - oneFourthDiff;
					}
					gpxItem.chartHighlightPos = pos;

					chart.moveViewToX(nextVisibleX);
					chart.highlightValue(gpxItem.chartHighlightPos, 0);
				}
				myLocation = location;
			}
		}
	}

	protected Location getMyLocation() {
		return myLocation;
	}

	public void update() {
		TrackDetailsMenuFragment fragment = getMenuFragment();
		if (fragment != null) {
			fragment.updateInfo();
		}
	}

	private TrackDetailsMenuFragment getMenuFragment() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			TrackDetailsMenuFragment fragment = (TrackDetailsMenuFragment) mapActivity.getSupportFragmentManager()
					.findFragmentByTag(TrackDetailsMenuFragment.TAG);
			if (fragment != null && !fragment.isDetached()) {
				return fragment;
			}
		}
		return null;
	}

	public void onShow() {
		MapActivity mapActivity = getMapActivity();
		GpxDisplayItem gpxItem = getGpxItem();
		if (mapActivity != null && gpxItem != null) {
			OsmandApplication app = mapActivity.getMyApplication();
			GPXFile groupGpx = gpxItem.group.getGpxFile();
			if (gpxItem.chartPointLayer == ChartPointLayer.GPX) {
				gpxItem.wasHidden = app.getSelectedGpxHelper().getSelectedFileByPath(groupGpx.path) == null;
				app.getSelectedGpxHelper().setGpxFileToDisplay(groupGpx);
			}
			boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
			if (!portrait) {
				mapActivity.getMapView().setMapPositionX(1);
			} else {
				TrackDetailsBarController toolbarController = new TrackDetailsBarController();
				this.toolbarController = toolbarController;
				if (gpxItem.group != null) {
					toolbarController.setTitle(gpxItem.group.getGpxName());
				} else {
					toolbarController.setTitle(mapActivity.getString(R.string.rendering_category_details));
				}
				toolbarController.setOnBackButtonClickListener(v -> {
					MapActivity activity = getMapActivity();
					if (activity != null) {
						activity.onBackPressed();
					}
				});
				int navigationIconResId = AndroidUtils.getNavigationIconResId(mapActivity);
				toolbarController.setBackBtnIconIds(navigationIconResId, navigationIconResId);
				toolbarController.setOnCloseButtonClickListener(v -> hide(false));
				mapActivity.showTopToolbar(toolbarController);
			}
			mapActivity.refreshMap();
			mapActivity.getMapLayers().getContextMenuLayer().enterGpxDetailsMode();
		}
	}

	public void onDismiss() {
		GpxDisplayItem gpxItem = getGpxItem();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (gpxItem != null && gpxItem.chartPointLayer == ChartPointLayer.GPX
					&& gpxItem.wasHidden && gpxItem.group != null) {
				GpxSelectionParams params = GpxSelectionParams.newInstance()
						.hideFromMap().syncGroup().saveSelection();
				GpxSelectionHelper helper = mapActivity.getMyApplication().getSelectedGpxHelper();
				helper.selectGpxFile(gpxItem.group.getGpxFile(), params);
			}
			TrackDetailsBarController toolbarController = this.toolbarController;
			if (toolbarController != null) {
				mapActivity.hideTopToolbar(toolbarController);
			}
			mapActivity.getMapLayers().getContextMenuLayer().exitGpxDetailsMode();
			mapActivity.getMapLayers().getGpxLayer().setTrackChartPoints(null);
			mapActivity.getMapLayers().getMapInfoLayer().setTrackChartPoints(null);
			mapActivity.getMapView().setMapPositionX(0);
			mapActivity.refreshMap();
		}
		if (hidding) {
			hidding = false;
			visible = false;
			reset();
		}
	}

	public void updateInfo(View main, boolean forceFitTrackOnMap) {
		updateView(main, forceFitTrackOnMap);
	}

	@Nullable
	private TrkSegment getTrackSegment(@NonNull LineChart chart) {
		if (segment == null) {
			segment = getTrackSegment(chart, getGpxItem());
		}
		return segment;
	}

	public static TrkSegment getTrackSegment(@NonNull LineChart chart,
	                                         @Nullable GpxDisplayItem gpxItem) {
		LineData lineData = chart.getLineData();
		List<ILineDataSet> ds = lineData != null ? lineData.getDataSets() : null;
		if (ds != null && ds.size() > 0 && gpxItem != null) {
			return GPXItemPagerAdapter.getSegmentForAnalysis(gpxItem, gpxItem.analysis);
		}
		return null;
	}

	@Nullable
	private LatLon getLocationAtPos(LineChart chart, float pos) {
		GpxDisplayItem gpxItem = getGpxItem();
		TrkSegment segment = getTrackSegment(chart);
		boolean joinSegments = selectedGpxFile != null && selectedGpxFile.isJoinSegments();
		return getLocationAtPos(chart, gpxItem, segment, pos, joinSegments);
	}

	@Nullable
	public static LatLon getLocationAtPos(LineChart chart, GpxDisplayItem gpxItem, TrkSegment segment,
	                                      float pos, boolean joinSegments) {
		WptPt point = null;
		LineData lineData = chart.getLineData();
		List<ILineDataSet> ds = lineData != null ? lineData.getDataSets() : null;
		if (!Algorithms.isEmpty(ds) && gpxItem != null && segment != null) {
			OrderedLineDataSet dataSet = (OrderedLineDataSet) ds.get(0);
			GPXFile gpxFile = gpxItem.group.getGpxFile();
			if (gpxItem.chartAxisType == GPXDataSetAxisType.TIME ||
					gpxItem.chartAxisType == GPXDataSetAxisType.TIMEOFDAY) {
				float time = pos * 1000;
				point = GpxUiHelper.getSegmentPointByTime(segment, gpxFile, time, true, joinSegments);
			} else {
				float distance = pos * dataSet.getDivX();
				point = GpxUiHelper.getSegmentPointByDistance(segment, gpxFile, distance, true,
						joinSegments);
			}
		}
		return point == null ? null : new LatLon(point.lat, point.lon);
	}

	private QuadRect getRect(LineChart chart, float startPos, float endPos) {
		double left = 0, right = 0;
		double top = 0, bottom = 0;
		LineData lineData = chart.getLineData();
		List<ILineDataSet> ds = lineData != null ? lineData.getDataSets() : null;
		GpxDisplayItem gpxItem = getGpxItem();
		if (ds != null && ds.size() > 0 && gpxItem != null) {
			TrkSegment segment = getTrackSegment(chart);
			if (segment != null) {
				OrderedLineDataSet dataSet = (OrderedLineDataSet) ds.get(0);
				if (gpxItem.chartAxisType == GPXDataSetAxisType.TIME || gpxItem.chartAxisType == GPXDataSetAxisType.TIMEOFDAY) {
					float startTime = startPos * 1000;
					float endTime = endPos * 1000;
					for (WptPt p : segment.points) {
						if (p.time - gpxItem.analysis.startTime >= startTime && p.time - gpxItem.analysis.startTime <= endTime) {
							if (left == 0 && right == 0) {
								left = p.getLongitude();
								right = p.getLongitude();
								top = p.getLatitude();
								bottom = p.getLatitude();
							} else {
								left = Math.min(left, p.getLongitude());
								right = Math.max(right, p.getLongitude());
								top = Math.max(top, p.getLatitude());
								bottom = Math.min(bottom, p.getLatitude());
							}
						}
					}
				} else {
					float startDistance = startPos * dataSet.getDivX();
					float endDistance = endPos * dataSet.getDivX();
					double previousSplitDistance = 0;
					for (int i = 0; i < segment.points.size(); i++) {
						WptPt currentPoint = segment.points.get(i);
						if (i != 0) {
							WptPt previousPoint = segment.points.get(i - 1);
							if (currentPoint.distance < previousPoint.distance) {
								previousSplitDistance += previousPoint.distance;
							}
						}
						if (previousSplitDistance + currentPoint.distance >= startDistance && previousSplitDistance + currentPoint.distance <= endDistance) {
							if (left == 0 && right == 0) {
								left = currentPoint.getLongitude();
								right = currentPoint.getLongitude();
								top = currentPoint.getLatitude();
								bottom = currentPoint.getLatitude();
							} else {
								left = Math.min(left, currentPoint.getLongitude());
								right = Math.max(right, currentPoint.getLongitude());
								top = Math.max(top, currentPoint.getLatitude());
								bottom = Math.min(bottom, currentPoint.getLatitude());
							}
						}
					}
				}
			}
		}
		return new QuadRect(left, top, right, bottom);
	}

	private void fitTrackOnMap(LineChart chart, LatLon location, boolean forceFit) {
		QuadRect rect = getRect(chart, chart.getLowestVisibleX(), chart.getHighestVisibleX());
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && rect.left != 0 && rect.right != 0) {
			RotatedTileBox tb = mapActivity.getMapView().getCurrentRotatedTileBox().copy();
			int tileBoxWidthPx = 0;
			int tileBoxHeightPx = 0;

			boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
			if (!portrait) {
				int width = getFragmentWidth();
				tileBoxWidthPx = width != -1 ? tb.getPixWidth() - width : 0;
			} else {
				int height = getFragmentHeight();
				tileBoxHeightPx = height != -1 ? tb.getPixHeight() - height : 0;
			}
			if (tileBoxHeightPx > 0 || tileBoxWidthPx > 0) {
				if (forceFit) {
					mapActivity.getMapView().fitRectToMap(rect.left, rect.right, rect.top, rect.bottom,
							tileBoxWidthPx, tileBoxHeightPx, topMarginPx);
				} else if (location != null &&
						!mapActivity.getMapView().getTileBox(tileBoxWidthPx, tileBoxHeightPx, topMarginPx).containsLatLon(location)) {
					boolean animating = mapActivity.getMapView().getAnimatedDraggingThread().isAnimating();
					mapActivity.getMapView().fitLocationToMap(location.getLatitude(), location.getLongitude(),
							mapActivity.getMapView().getZoom(), tileBoxWidthPx, tileBoxHeightPx, topMarginPx, !animating);
				} else {
					mapActivity.refreshMap();
				}
			}
		}
	}

	protected int getFragmentWidth() {
		TrackDetailsMenuFragment fragment = getMenuFragment();
		if (fragment != null) {
			return fragment.getWidth();
		}
		return -1;
	}

	protected int getFragmentHeight() {
		TrackDetailsMenuFragment fragment = getMenuFragment();
		if (fragment != null) {
			return fragment.getHeight();
		}
		return -1;
	}

	public void refreshChart(LineChart chart, boolean forceFit, boolean recalculateXAxis) {
		refreshChart(chart, true, forceFit, recalculateXAxis);
	}

	public void refreshChart(LineChart chart, boolean fitTrackOnMap, boolean forceFit, boolean recalculateXAxis) {
		MapActivity mapActivity = getMapActivity();
		GpxDisplayItem gpxItem = getGpxItem();
		if (mapActivity == null || gpxItem == null) {
			return;
		}

		Highlight[] highlights = chart.getHighlighted();
		LatLon location = null;

		TrackChartPoints trackChartPoints = this.trackChartPoints;
		if (trackChartPoints == null) {
			trackChartPoints = new TrackChartPoints();
			TrkSegment segment = getTrackSegment(chart);
			int segmentColor = segment != null ? segment.getColor(0) : 0;
			trackChartPoints.setSegmentColor(segmentColor);
			trackChartPoints.setGpx(gpxItem.group.getGpxFile());
			this.trackChartPoints = trackChartPoints;
		}

		float minimumVisibleXValue = chart.getLowestVisibleX();
		float maximumVisibleXValue = chart.getHighestVisibleX();

		if (highlights != null && highlights.length > 0) {
			if (minimumVisibleXValue != 0 && maximumVisibleXValue != 0) {
				if (highlights[0].getX() < minimumVisibleXValue) {
					float difference = (maximumVisibleXValue - minimumVisibleXValue) * 0.1f;
					gpxItem.chartHighlightPos = minimumVisibleXValue + difference;
					chart.highlightValue(minimumVisibleXValue + difference, 0);
				} else if (highlights[0].getX() > maximumVisibleXValue) {
					float difference = (maximumVisibleXValue - minimumVisibleXValue) * 0.1f;
					gpxItem.chartHighlightPos = maximumVisibleXValue - difference;
					chart.highlightValue(maximumVisibleXValue - difference, 0);
				} else {
					gpxItem.chartHighlightPos = highlights[0].getX();
				}
			} else {
				gpxItem.chartHighlightPos = highlights[0].getX();
			}
			location = getLocationAtPos(chart, gpxItem.chartHighlightPos);
			if (location != null) {
				trackChartPoints.setHighlightedPoint(location);
			}
		} else {
			gpxItem.chartHighlightPos = -1;
		}
		if (recalculateXAxis && shouldShowXAxisPoints()) {
			trackChartPoints.setXAxisPoints(getXAxisPoints(chart));
		}
		if (gpxItem.chartPointLayer == ChartPointLayer.ROUTE) {
			mapActivity.getMapLayers().getRouteLayer().setTrackChartPoints(trackChartPoints);
		} else if (gpxItem.chartPointLayer == ChartPointLayer.GPX) {
			mapActivity.getMapLayers().getGpxLayer().setTrackChartPoints(trackChartPoints);
		} else if (gpxItem.chartPointLayer == ChartPointLayer.MEASUREMENT_TOOL) {
			mapActivity.getMapLayers().getMeasurementToolLayer().setTrackChartPoints(trackChartPoints);
		}
		if (location != null) {
			mapActivity.refreshMap();
		}
		if (!fitTrackOnMapForbidden && fitTrackOnMap) {
			fitTrackOnMap(chart, location, forceFit);
		}
	}

	public boolean shouldShowXAxisPoints() {
		return true;
	}

	public void reset() {
		segment = null;
		trackChartPoints = null;
	}

	private List<LatLon> getXAxisPoints(LineChart chart) {
		float[] entries = chart.getXAxis().mEntries;
		LineData lineData = chart.getLineData();
		float maxXValue = lineData != null ? lineData.getXMax() : -1;
		if (entries.length >= 2 && lineData != null) {
			float interval = entries[1] - entries[0];
			if (interval > 0) {
				List<LatLon> xAxisPoints = new ArrayList<>();
				float currentPointEntry = interval;
				while (currentPointEntry < maxXValue) {
					LatLon location = getLocationAtPos(chart, currentPointEntry);
					if (location != null) {
						xAxisPoints.add(location);
					}
					currentPointEntry += interval;
				}
				this.xAxisPoints = xAxisPoints;
			}
		}
		return xAxisPoints;
	}

	private void updateView(View parentView, boolean forceFitTrackOnMap) {
		MapActivity mapActivity = getMapActivity();
		GpxDisplayItem gpxItem = getGpxItem();
		if (mapActivity == null || gpxItem == null) {
			return;
		}
		OsmandApplication app = mapActivity.getMyApplication();
		UiUtilities ic = app.getUIUtilities();
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		GPXTrackAnalysis analysis = gpxItem.analysis;
		if (analysis == null || gpxItem.chartTypes == null) {
			parentView.setVisibility(View.GONE);
			if (analysis != null && analysis.isBoundsCalculated()) {
				mapActivity.getMapView()
						.fitRectToMap(analysis.left, analysis.right, analysis.top, analysis.bottom, 0, 0, 0);
			}
			return;
		}

		LineChart chart = parentView.findViewById(R.id.chart);
		chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
			@Override
			public void onValueSelected(Entry e, Highlight h) {
				refreshChart(chart, false, false);
			}

			@Override
			public void onNothingSelected() {

			}
		});

		chart.setOnChartGestureListener(new OnChartGestureListener() {
			boolean hasTranslated;
			float highlightDrawX = -1;

			@Override
			public void onChartGestureStart(MotionEvent me, ChartGesture lastPerformedGesture) {
				hasTranslated = false;
				if (chart.getHighlighted() != null && chart.getHighlighted().length > 0) {
					highlightDrawX = chart.getHighlighted()[0].getDrawX();
				} else {
					highlightDrawX = -1;
				}
				MapActivity mapActivity = getMapActivity();
				if (lastPerformedGesture != ChartGesture.NONE && mapActivity != null
						&& mapActivity.getMapViewTrackingUtilities().isMapLinkedToLocation()) {
					mapActivity.getMapViewTrackingUtilities().setMapLinkedToLocation(false);
				}
			}

			@Override
			public void onChartGestureEnd(MotionEvent me, ChartGesture lastPerformedGesture) {
				GpxDisplayItem gpxItem = getGpxItem();
				if (gpxItem != null) {
					if ((lastPerformedGesture == ChartGesture.DRAG && hasTranslated) ||
							lastPerformedGesture == ChartGesture.X_ZOOM ||
							lastPerformedGesture == ChartGesture.Y_ZOOM ||
							lastPerformedGesture == ChartGesture.PINCH_ZOOM ||
							lastPerformedGesture == ChartGesture.DOUBLE_TAP ||
							lastPerformedGesture == ChartGesture.ROTATE) {

						gpxItem.chartMatrix = new Matrix(chart.getViewPortHandler().getMatrixTouch());
						refreshChart(chart, false, true);
					}
				}
			}

			@Override
			public void onChartLongPressed(MotionEvent me) {
			}

			@Override
			public void onChartDoubleTapped(MotionEvent me) {
			}

			@Override
			public void onChartSingleTapped(MotionEvent me) {
			}

			@Override
			public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {
			}

			@Override
			public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
			}

			@Override
			public void onChartTranslate(MotionEvent me, float dX, float dY) {
				hasTranslated = true;
				if (highlightDrawX != -1) {
					Highlight h = chart.getHighlightByTouchPoint(highlightDrawX, 0f);
					if (h != null) {
						chart.highlightValue(h);
						refreshChart(chart, false, false);
					}
				}
			}
		});

		Context themedContext = UiUtilities.getThemedContext(mapActivity, nightMode);
		boolean useHours = analysis.timeSpan != 0 && analysis.timeSpan / HOUR_IN_MILLIS > 0;
		GpxMarkerView markerView = new GpxMarkerView(themedContext, analysis.startTime, useHours);
		GpxUiHelper.setupGPXChart(chart, markerView, 24, 16, true);

		List<ILineDataSet> dataSets = new ArrayList<>();
		if (gpxItem.chartTypes != null && gpxItem.chartTypes.length > 0) {
			for (int i = 0; i < gpxItem.chartTypes.length; i++) {
				OrderedLineDataSet dataSet = null;
				boolean withoutGaps = selectedGpxFile != null && (!selectedGpxFile.isJoinSegments() && gpxItem.isGeneralTrack());
				switch (gpxItem.chartTypes[i]) {
					case ALTITUDE:
						dataSet = GpxUiHelper.createGPXElevationDataSet(app, chart, analysis,
								gpxItem.chartAxisType, false, true, withoutGaps);
						break;
					case SPEED:
						dataSet = GpxUiHelper.createGPXSpeedDataSet(app, chart, analysis,
								gpxItem.chartAxisType, gpxItem.chartTypes.length > 1, true, withoutGaps);
						break;
					case SLOPE:
						boolean useRightAxis = gpxItem.chartTypes[0] != GPXDataSetType.SLOPE;
						dataSet = GpxUiHelper.createGPXSlopeDataSet(app, chart, analysis,
								gpxItem.chartAxisType, null, useRightAxis, true, withoutGaps);
						break;
				}
				if (dataSet != null) {
					dataSets.add(dataSet);
				}
			}
		}

		Collections.sort(dataSets, (ds1, ds2) -> {
			OrderedLineDataSet dataSet1 = (OrderedLineDataSet) ds1;
			OrderedLineDataSet dataSet2 = (OrderedLineDataSet) ds2;
			return Float.compare(dataSet2.getPriority(), dataSet1.getPriority());
		});
		chart.setData(new LineData(dataSets));
		updateChart(chart);

		View yAxis = parentView.findViewById(R.id.y_axis);
		ImageView yAxisIcon = parentView.findViewById(R.id.y_axis_icon);
		TextView yAxisTitle = parentView.findViewById(R.id.y_axis_title);
		View yAxisArrow = parentView.findViewById(R.id.y_axis_arrow);
		List<GPXDataSetType[]> availableTypes = getAvailableYTypes(analysis);

		yAxisIcon.setImageDrawable(GPXDataSetType.getImageDrawable(app, gpxItem.chartTypes));
		yAxisTitle.setText(GPXDataSetType.getName(app, gpxItem.chartTypes));
		if (availableTypes.size() > 0) {
			yAxis.setOnClickListener(v -> {
				AnalyzeBottomSheet.showInstance(mapActivity.getSupportFragmentManager());
			});
			yAxisArrow.setVisibility(View.VISIBLE);
		} else {
			yAxis.setOnClickListener(null);
			yAxis.setBackgroundResource(0);
			yAxisArrow.setVisibility(View.GONE);
		}

		View xAxis = parentView.findViewById(R.id.x_axis);
		ImageView xAxisIcon = parentView.findViewById(R.id.x_axis_icon);
		TextView xAxisTitle = parentView.findViewById(R.id.x_axis_title);
		View xAxisArrow = parentView.findViewById(R.id.x_axis_arrow);
		if (gpxItem.chartAxisType == GPXDataSetAxisType.TIME) {
			xAxisIcon.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_time));
			xAxisTitle.setText(app.getString(R.string.shared_string_time));
		} else if (gpxItem.chartAxisType == GPXDataSetAxisType.TIMEOFDAY) {
			xAxisIcon.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_time_span));
			xAxisTitle.setText(app.getString(R.string.time_of_day));
		} else {
			xAxisIcon.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_marker_dark));
			xAxisTitle.setText(app.getString(R.string.distance));
		}
		if (analysis.isTimeSpecified()) {
			xAxis.setOnClickListener(v -> {
				AnalyzeBottomSheet bottomSheet = new AnalyzeBottomSheet();
				bottomSheet.show(mapActivity.getSupportFragmentManager(), AnalyzeBottomSheet.TAG);
			});
			xAxisArrow.setVisibility(View.VISIBLE);
		} else {
			xAxis.setOnClickListener(null);
			xAxis.setBackgroundResource(0);
			xAxisArrow.setVisibility(View.GONE);
		}

		refreshChart(chart, forceFitTrackOnMap, true);
	}

	public List<GPXDataSetAxisType> getAvailableXTypes(GPXTrackAnalysis analysis) {
		List<GPXDataSetAxisType> availableTypes = new ArrayList<>();

		for (GPXDataSetAxisType type : GPXDataSetAxisType.values()) {
			if (type == GPXDataSetAxisType.TIME || type == GPXDataSetAxisType.TIMEOFDAY) {
				if (analysis.isTimeSpecified()) {
					availableTypes.add(type);
				}
			} else {
				availableTypes.add(type);
			}
		}

		return availableTypes;
	}

	public List<GPXDataSetType[]> getAvailableYTypes(GPXTrackAnalysis analysis) {
		List<GPXDataSetType[]> availableTypes = new ArrayList<>();

		if (analysis.hasElevationData) {
			availableTypes.add(new GPXDataSetType[] {GPXDataSetType.ALTITUDE});
			availableTypes.add(new GPXDataSetType[] {GPXDataSetType.SLOPE});
		}
		if (analysis.hasSpeedData) {
			availableTypes.add(new GPXDataSetType[] {GPXDataSetType.SPEED});
		}
		if (analysis.hasElevationData) {
			availableTypes.add(new GPXDataSetType[] {GPXDataSetType.ALTITUDE, GPXDataSetType.SLOPE});
		}
		if (analysis.hasElevationData && analysis.hasSpeedData) {
			availableTypes.add(new GPXDataSetType[] {GPXDataSetType.ALTITUDE, GPXDataSetType.SPEED});
		}
		if (analysis.hasElevationData && analysis.hasSpeedData) {
			availableTypes.add(new GPXDataSetType[] {GPXDataSetType.SLOPE, GPXDataSetType.SPEED});
		}
		return availableTypes;
	}

	public AxisSelectedListener getAxisSelectedListener() {
		return new AxisSelectedListener() {
			@Override
			public void onXAxisSelected(GPXDataSetAxisType type) {
				fitTrackOnMapForbidden = true;
				GpxDisplayItem item = getGpxItem();
				if (item != null) {
					item.chartAxisType = type;
					item.chartHighlightPos = -1;
					item.chartMatrix = null;
					update();
				}
				fitTrackOnMapForbidden = false;
			}

			@Override
			public void onYAxisSelected(GPXDataSetType[] type) {
				fitTrackOnMapForbidden = true;
				GpxDisplayItem item = getGpxItem();
				if (item != null) {
					item.chartTypes = type;
					update();
				}
				fitTrackOnMapForbidden = false;
			}
		};
	}

	private void updateChart(LineChart chart) {
		GpxDisplayItem gpxItem = getGpxItem();
		chart.notifyDataSetChanged();
		chart.invalidate();
		if (gpxItem != null) {
			if (gpxItem.chartMatrix != null) {
				chart.getViewPortHandler().refresh(new Matrix(gpxItem.chartMatrix), chart, true);
			}
			if (gpxItem.chartHighlightPos != -1) {
				chart.highlightValue(gpxItem.chartHighlightPos, 0);
			} else if (gpxItem.locationOnMap != null) {
				LineData lineData = chart.getLineData();
				List<ILineDataSet> ds = lineData != null ? lineData.getDataSets() : null;
				if (ds != null && ds.size() > 0) {
					OrderedLineDataSet dataSet = (OrderedLineDataSet) ds.get(0);
					gpxItem.chartHighlightPos = (float) (gpxItem.locationOnMap.distance / dataSet.getDivX());
					chart.highlightValue(gpxItem.chartHighlightPos, 0);
				}
			} else {
				chart.highlightValue(null);
			}
		}
	}

	public enum ChartPointLayer {
		GPX,
		ROUTE,
		MEASUREMENT_TOOL
	}

	private static class TrackDetailsBarController extends TopToolbarController {

		TrackDetailsBarController() {
			super(TopToolbarControllerType.TRACK_DETAILS);
			setBackBtnIconClrIds(0, 0);
			setRefreshBtnIconClrIds(0, 0);
			setCloseBtnIconClrIds(0, 0);
			setTitleTextClrIds(R.color.text_color_tab_active_light, R.color.text_color_tab_active_dark);
			setDescrTextClrIds(R.color.text_color_tab_active_light, R.color.text_color_tab_active_dark);
			setBgIds(R.drawable.gradient_toolbar, R.drawable.gradient_toolbar,
					R.drawable.gradient_toolbar, R.drawable.gradient_toolbar);
		}

		@Override
		public void updateToolbar(@NonNull TopToolbarView toolbarView) {
			super.updateToolbar(toolbarView);
			AndroidUiHelper.updateVisibility(toolbarView.getShadowView(), false);
		}

		@Override
		public int getStatusBarColor(Context context, boolean nightMode) {
			return NO_COLOR;
		}
	}
}