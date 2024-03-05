package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu.ChartPointLayer.ROUTE;
import static net.osmand.plus.views.mapwidgets.WidgetType.ELEVATION_PROFILE;

import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.mikephil.charting.charts.ElevationChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis.AxisDependency;
import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.ViewPortHandler;

import net.osmand.Location;
import net.osmand.StateChangedListener;
import net.osmand.data.LatLon;
import net.osmand.gpx.ElevationDiffsCalculator;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXUtilities.TrkSegment;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.charts.ChartUtils;
import net.osmand.plus.charts.GPXDataSetAxisType;
import net.osmand.plus.charts.GPXDataSetType;
import net.osmand.plus.charts.GPXHighlight;
import net.osmand.plus.charts.OrderedLineDataSet;
import net.osmand.plus.charts.TrackChartPoints;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu;
import net.osmand.plus.measurementtool.graph.BaseCommonChartAdapter;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public class ElevationProfileWidget extends MapWidget {

	private static final String SHOW_SLOPE_PREF_ID = "show_slope_elevation_widget";

	private final CommonPreference<Boolean> showSlopePreference;

	private static final int MAX_DISTANCE_TO_SHOW_IM_METERS = 10_000;

	private View uphillView;
	private View downhillView;
	private View gradeView;
	private ElevationChart chart;

	private GpxDisplayItem gpxItem;
	private TrkSegment segment;
	private GPXFile gpx;
	private float toMetersMultiplier;
	private Location myLocation;
	private List<WptPt> allPoints;

	private boolean showSlopes;
	private RouteCalculationResult route;
	private int firstVisiblePointIndex = -1;
	private int lastVisiblePointIndex = -1;
	private OrderedLineDataSet slopeDataSet;

	@Nullable
	private TrackChartPoints trackChartPoints;

	private boolean movedToLocation;

	private static Matrix lastStateMatrix;
	private static String lastRoute;
	private static boolean lastChartLinkedToLocation;

	private final StateChangedListener<Boolean> linkedToLocationListener = change -> {
		if (change) {
			movedToLocation = true;
			lastChartLinkedToLocation = true;
		}
	};

	public ElevationProfileWidget(@NonNull MapActivity mapActivity, @Nullable String customId) {
		super(mapActivity, ELEVATION_PROFILE);
		this.showSlopePreference = registerShowSlopePref(customId);
		settings.MAP_LINKED_TO_LOCATION.addListener(linkedToLocationListener);
		updateVisibility(false);
		setupStatisticBlocks();
	}

	public Boolean shouldShowSlope(@NonNull ApplicationMode appMode) {
		return showSlopePreference.getModeValue(appMode);
	}

	public void setShouldShowSlope(@NonNull ApplicationMode appMode, boolean shouldShowSlope) {
		showSlopePreference.setModeValue(appMode, shouldShowSlope);
	}

	@NonNull
	private CommonPreference<Boolean> registerShowSlopePref(@Nullable String customId) {
		String prefId = Algorithms.isEmpty(customId) ? SHOW_SLOPE_PREF_ID : SHOW_SLOPE_PREF_ID + customId;
		return settings.registerBooleanPreference(prefId, false)
				.makeProfile()
				.cache();
	}

	private void restoreLastState() {
		if (chart != null && lastStateMatrix != null && route != null) {
			if (Algorithms.stringsEqual(lastRoute, route.toString())) {
				chart.getViewPortHandler().refresh(new Matrix(lastStateMatrix), chart, false);
			} else {
				lastStateMatrix = null;
			}
			if (lastChartLinkedToLocation) {
				movedToLocation = true;
			}
		}
	}

	private void storeLastState(boolean chartLinkedToLocation) {
		if (chart != null) {
			lastStateMatrix = new Matrix(chart.getViewPortHandler().getMatrixTouch());
		}
		lastChartLinkedToLocation = chartLinkedToLocation;
	}

	@Override
	protected int getLayoutId() {
		return R.layout.elevation_profile_widget;
	}

	private void setupStatisticBlocks() {
		uphillView = setupStatisticBlock(R.id.uphill_widget,
				R.string.shared_string_uphill,
				R.drawable.ic_action_ascent_arrow_16);

		downhillView = setupStatisticBlock(R.id.downhill_widget,
				R.string.shared_string_downhill,
				R.drawable.ic_action_descent_arrow_16);

		gradeView = setupStatisticBlock(R.id.grade_widget,
				R.string.shared_string_grade,
				R.drawable.ic_action_percent_16);
	}

	private View setupStatisticBlock(int viewId, int textId, int iconId) {
		View blockView = view.findViewById(viewId);

		TextView title = blockView.findViewById(R.id.title);
		title.setText(textId);

		TextView text = blockView.findViewById(R.id.widget_text);
		text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

		ImageView icon = blockView.findViewById(R.id.image);
		icon.setImageResource(iconId);

		return blockView;
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		boolean visible = mapActivity.getWidgetsVisibilityHelper().shouldShowElevationProfileWidget();
		updateVisibility(visible);
		if (visible) {
			updateInfoImpl();
		}
	}

	private void updateInfoImpl() {
		boolean settingsUpdated = updateSettings();
		if (settingsUpdated) {
			setupChart();
		}
		boolean chartUpdated = updateChart(settingsUpdated);
		if (chartUpdated) {
			updateWidgets();
		}
		if (settingsUpdated) {
			view.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
					restoreLastState();
				}
			});
		}
	}

	private boolean updateSettings() {
		RouteCalculationResult route = app.getRoutingHelper().getRoute();
		boolean routeChanged = this.route != route;
		this.route = route;
		lastRoute = route.toString();
		boolean showSlopes = showSlopePreference.get();
		boolean slopesChanged = showSlopes != this.showSlopes;
		this.showSlopes = showSlopes;
		return routeChanged || slopesChanged;
	}

	private void setupChart() {
		gpx = GpxUiHelper.makeGpxFromLocations(route.getImmutableAllLocations(), app);
		GPXTrackAnalysis analysis = gpx.getAnalysis(0);
		allPoints = gpx.getAllSegmentsPoints();
		gpxItem = GpxUiHelper.makeGpxDisplayItem(app, gpx, ROUTE, analysis);
		firstVisiblePointIndex = -1;
		lastVisiblePointIndex = -1;
		slopeDataSet = null;

		chart = view.findViewById(R.id.line_chart);
		UiUtilities iconsCache = app.getUIUtilities();
		ApplicationMode appMode = app.getSettings().getApplicationMode();
		int profileColor = appMode.getProfileColor(isNightMode());
		Drawable markerIcon = iconsCache.getPaintedIcon(R.drawable.ic_action_location_color, profileColor);
		ChartUtils.setupElevationChart(chart, 24f, 16f, true, markerIcon);
		chart.setHighlightPerTapEnabled(false);
		chart.setHighlightPerDragEnabled(false);
		BaseCommonChartAdapter chartAdapter = new BaseCommonChartAdapter(app, chart, true);

		if (analysis.hasElevationData()) {
			List<ILineDataSet> dataSets = new ArrayList<>();
			OrderedLineDataSet elevationDataSet = ChartUtils.createGPXElevationDataSet(app, chart, analysis,
					GPXDataSetType.ALTITUDE, GPXDataSetAxisType.DISTANCE, false, true, false);
			dataSets.add(elevationDataSet);

			if (showSlopes) {
				OrderedLineDataSet slopeDataSet = ChartUtils.createGPXSlopeDataSet(app, chart, analysis,
						GPXDataSetType.SLOPE, GPXDataSetAxisType.DISTANCE, elevationDataSet.getEntries(), true, true, false);
				if (slopeDataSet != null) {
					dataSets.add(slopeDataSet);
				}
				this.slopeDataSet = slopeDataSet;
			}

			chartAdapter.updateContent(new LineData(dataSets), gpxItem);
			toMetersMultiplier = ((OrderedLineDataSet) dataSets.get(0)).getDivX();

			setupZoom(chart);
			chart.setVisibility(View.VISIBLE);
		} else {
			chart.setVisibility(View.GONE);
		}
		segment = TrackDetailsMenu.getTrackSegment(chart, gpxItem);
		chart.setOnChartGestureListener(new OnChartGestureListener() {
			boolean hasTranslated;
			float highlightDrawX = -1;

			@Override
			public void onChartGestureStart(MotionEvent me, ChartGesture lastPerformedGesture) {
				hasTranslated = false;
				Highlight[] highlighted = chart.getHighlighted();
				boolean setupDrawX = false;
				if (highlighted != null && highlighted.length > 0) {
					for (Highlight highlight : highlighted) {
						if (highlight != locationHighlight) {
							highlightDrawX = highlight.getDrawX();
							setupDrawX = true;
							break;
						}
					}
				}
				if (!setupDrawX) {
					highlightDrawX = -1;
				}
			}

			@Override
			public void onChartGestureEnd(MotionEvent me, ChartGesture lastPerformedGesture) {
				gpxItem.chartMatrix = new Matrix(chart.getViewPortHandler().getMatrixTouch());
				storeLastState(false);
				app.runInUIThread(() -> updateWidgets());
			}

			@Override
			public void onChartLongPressed(MotionEvent me) {
			}

			@Override
			public void onChartDoubleTapped(MotionEvent me) {
			}

			@Override
			public void onChartSingleTapped(MotionEvent me) {
				Highlight locationHighlight = ElevationProfileWidget.this.locationHighlight;
				Highlight touchHighlight = chart.getHighlightByTouchPoint(me.getX(), me.getY());
				if (touchHighlight != null) {
					touchHighlight = createHighlight(touchHighlight.getX(), false);
				}

				if (locationHighlight != null && touchHighlight != null) {
					chart.highlightValues(new Highlight[] {locationHighlight, touchHighlight});
				} else if (locationHighlight != null) {
					chart.highlightValue(locationHighlight, true);
				} else if (touchHighlight != null) {
					chart.highlightValue(touchHighlight, true);
				}
			}

			@Override
			public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {
			}

			@Override
			public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
				app.runInUIThread(() -> updateWidgets());
			}

			@Override
			public void onChartTranslate(MotionEvent me, float dX, float dY) {
				hasTranslated = true;
				if (highlightDrawX != -1) {
					Highlight h = chart.getHighlightByTouchPoint(highlightDrawX, 0f);
					if (h != null) {
						h = createHighlight(h.getX(), false);
						if (locationHighlight != null) {
							chart.highlightValues(new Highlight[] {locationHighlight, h});
						} else {
							chart.highlightValue(h, true);
						}
					}
				}
				app.runInUIThread(() -> updateWidgets());
			}
		});
	}

	private Highlight locationHighlight;

	private boolean updateChart(boolean forceUpdate) {
		Location location = app.getLocationProvider().getLastKnownLocation();
		if (!forceUpdate && myLocation != null && MapUtils.areLatLonEqual(myLocation, location)) {
			return false;
		}
		myLocation = location;
		if (location == null) {
			gpxItem.chartHighlightPos = -1f;
			refreshHighlights(null);
			return true;
		}
		LineData lineData = chart.getLineData();
		List<ILineDataSet> ds = lineData != null ? lineData.getDataSets() : null;
		RouteCalculationResult route = this.route;
		TrkSegment segment = this.segment;
		if (Algorithms.isEmpty(ds) || gpxItem == null || route == null || segment == null) {
			return false;
		}
		float distanceFromStart = route.getDistanceFromStart();
		if (distanceFromStart == 0) {
			gpxItem.chartHighlightPos = -1f;
			refreshHighlights(null);
			return true;
		}
		float minVisibleX = chart.getLowestVisibleX();
		float maxVisibleX = chart.getHighestVisibleX();
		float twentyPercent = ((maxVisibleX - minVisibleX) / 5);
		float startMoveChartPosition = minVisibleX + twentyPercent;
		float pos = distanceFromStart / ((OrderedLineDataSet) ds.get(0)).getDivX();

		boolean movedToLocation = this.movedToLocation;
		if (pos >= minVisibleX && pos <= maxVisibleX || movedToLocation) {
			if (pos >= startMoveChartPosition) {
				float nextVisibleX = pos - twentyPercent;
				moveViewToX(chart, nextVisibleX);
			} else if (movedToLocation) {
				moveViewToX(chart, Math.max(pos - twentyPercent, chart.getXChartMin()));
			}
			if (movedToLocation) {
				this.movedToLocation = false;
			}
			gpxItem.chartHighlightPos = pos;
			Highlight newLocationHighlight = createHighlight(pos, true);
			refreshHighlights(newLocationHighlight);
			storeLastState(true);
		}
		return true;
	}

	private void refreshHighlights(@Nullable Highlight newLocationHighlight) {
		Highlight[] highlighted = chart.getHighlighted();
		int replaceIndex = -1;
		if (highlighted != null) {
			for (int i = 0; i < highlighted.length; i++) {
				Highlight highlight = highlighted[i];
				if (highlight == locationHighlight) {
					replaceIndex = i;
					break;
				}
			}
		}
		locationHighlight = newLocationHighlight;
		if (replaceIndex != -1) {
			if (newLocationHighlight != null) {
				highlighted[replaceIndex] = newLocationHighlight;
			} else {
				Highlight[] newHighlighted = new Highlight[highlighted.length - 1];
				int k = 0;
				for (int i = 0; i < highlighted.length; i++) {
					if (i != replaceIndex) {
						newHighlighted[k++] = highlighted[i];
					}
				}
				highlighted = newHighlighted;
			}
		} else if (newLocationHighlight != null) {
			if (highlighted == null) {
				highlighted = new Highlight[] {newLocationHighlight};
			} else {
				Highlight[] newHighlighted = new Highlight[highlighted.length + 1];
				newHighlighted[0] = newLocationHighlight;
				System.arraycopy(highlighted, 0, newHighlighted, 1, highlighted.length);
				highlighted = newHighlighted;
			}
		}
		chart.highlightValues(highlighted);
	}

	private Highlight createHighlight(float x, boolean location) {
		return new GPXHighlight(x, 0, location);
	}

	private boolean updateWidgets() {
		double minVisibleX = chart.getLowestVisibleX();
		double maxVisibleX = chart.getHighestVisibleX();
		float highlightPosition = gpxItem != null ? gpxItem.chartHighlightPos : -1f;
		if (highlightPosition > minVisibleX && highlightPosition < maxVisibleX) {
			minVisibleX = highlightPosition;
		}
		updateTrackChartPoints();
		double fromDistance = minVisibleX * toMetersMultiplier;
		double toDistance = maxVisibleX * toMetersMultiplier;
		List<WptPt> points = this.allPoints;
		int firstPointIndex = gpx.getPointIndexByDistance(points, fromDistance);
		int lastPointIndex = gpx.getPointIndexByDistance(points, toDistance);
		if (firstVisiblePointIndex == firstPointIndex && lastVisiblePointIndex == lastPointIndex) {
			return false;
		}
		firstVisiblePointIndex = firstPointIndex;
		lastVisiblePointIndex = lastPointIndex;
		firstPointIndex = Math.max(0, firstPointIndex - 1);
		lastPointIndex = Math.min(points.size() - 1, lastPointIndex + 1);
		if (lastPointIndex > firstPointIndex) {
			int pointsCount = lastPointIndex - firstPointIndex + 1;
			ElevationDiffsCalculator elevationDiffsCalc = new ElevationDiffsCalculator() {
				@Override
				public double getPointDistance(int index) {
					return points.get(index).distance;
				}

				@Override
				public double getPointElevation(int index) {
					return points.get(index).ele;
				}

				@Override
				public int getPointsCount() {
					return pointsCount;
				}
			};
			elevationDiffsCalc.calculateElevationDiffs();
			String uphill = OsmAndFormatter.getFormattedAlt(elevationDiffsCalc.getDiffElevationUp(), app);
			updateTextWidget(uphillView, uphill);
			String downhill = OsmAndFormatter.getFormattedAlt(elevationDiffsCalc.getDiffElevationDown(), app);
			updateTextWidget(downhillView, downhill);
		}
		int maxGrade = calculateMaxGrade();
		String maxGradeStr;
		if (maxGrade == Integer.MAX_VALUE) {
			maxGradeStr = "--";
		} else {
			maxGradeStr = maxGrade + " %";
		}
		updateTextWidget(gradeView, maxGradeStr);
		return true;
	}

	private void updateTrackChartPoints() {
		Highlight highlight = getSelectedHighlight();
		if (highlight != null) {
			TrackChartPoints trackChartPoints = getTrackChartPoints();
			LatLon location = TrackDetailsMenu.getLocationAtPos(chart, gpxItem, segment, highlight.getX(), true);
			if (location != null) {
				trackChartPoints.setHighlightedPoint(location);
			}
			if (gpxItem.chartPointLayer == ROUTE) {
				mapActivity.getMapLayers().getRouteLayer().setTrackChartPoints(trackChartPoints);
			}
			if (location != null) {
				mapActivity.refreshMap();
			}
		}
	}

	@NonNull
	private TrackChartPoints getTrackChartPoints() {
		TrackChartPoints trackChartPoints = this.trackChartPoints;
		if (trackChartPoints == null) {
			trackChartPoints = new TrackChartPoints();
			int segmentColor = segment != null ? segment.getColor(0) : 0;
			trackChartPoints.setSegmentColor(segmentColor);
			trackChartPoints.setGpx(gpxItem.group.getGpxFile());
			this.trackChartPoints = trackChartPoints;
		}
		return trackChartPoints;
	}

	@Nullable
	private Highlight getSelectedHighlight() {
		Highlight[] highlighted = chart.getHighlighted();
		if (!Algorithms.isEmpty(highlighted)) {
			for (Highlight highlight : highlighted) {
				if (highlight instanceof GPXHighlight && !((GPXHighlight) highlight).shouldShowLocationIcon()) {
					return highlight;
				}
			}
		}
		return null;
	}

	private void updateTextWidget(View container, String text) {
		String[] split = text.split(" ");
		if (split.length == 2) {
			((TextView) container.findViewById(R.id.widget_text)).setText(split[0]);
			((TextView) container.findViewById(R.id.widget_text_small)).setText(split[1]);
		} else {
			((TextView) container.findViewById(R.id.widget_text)).setText(text);
			((TextView) container.findViewById(R.id.widget_text_small)).setText("");
		}
	}

	private int calculateMaxGrade() {
		OrderedLineDataSet slopeDataSet = this.slopeDataSet;
		if (slopeDataSet == null) {
			return Integer.MAX_VALUE;
		}
		float minVisibleX = chart.getLowestVisibleX();
		float maxVisibleX = chart.getHighestVisibleX();
		float highlightPosition = gpxItem != null ? gpxItem.chartHighlightPos : -1f;
		if (highlightPosition > minVisibleX && highlightPosition < maxVisibleX) {
			minVisibleX = highlightPosition;
		}
		int firstEntryIndex = slopeDataSet.getEntryIndex(minVisibleX, Float.NaN, DataSet.Rounding.CLOSEST);
		int lastEntryIndex = slopeDataSet.getEntryIndex(maxVisibleX, Float.NaN, DataSet.Rounding.CLOSEST);
		if (firstEntryIndex == -1 || lastEntryIndex == -1) {
			return Integer.MAX_VALUE;
		}
		float maxValue = 0;
		for (int i = firstEntryIndex; i <= lastEntryIndex; i++) {
			Entry e = slopeDataSet.getEntryForIndex(i);
			float v = e.getY();
			if (Math.abs(v) > Math.abs(maxValue)) {
				maxValue = v;
			}
		}
		return (int) (maxValue + 0.5);
	}

	private void setupZoom(LineChart chart) {
		chart.fitScreen();
		float maxValue = chart.getXChartMax() * toMetersMultiplier;
		if (maxValue > MAX_DISTANCE_TO_SHOW_IM_METERS) {
			float scaleX = maxValue / MAX_DISTANCE_TO_SHOW_IM_METERS;
			chart.zoom(scaleX, 1.0f, 0, 0);
			chart.scrollTo(0, 0);
		}
	}

	private static void moveViewToX(LineChart chart, float nextVisibleX) {
		ViewPortHandler handler = chart.getViewPortHandler();
		Transformer transformer = chart.getTransformer(AxisDependency.LEFT);

		float[] pts = new float[2];
		pts[0] = nextVisibleX;
		pts[1] = 0f;
		transformer.pointValuesToPixel(pts);

		Matrix save = new Matrix();
		save.reset();
		save.set(handler.getMatrixTouch());

		float x = pts[0] - handler.offsetLeft();
		float y = pts[1] - handler.offsetTop();
		save.postTranslate(-x, -y);
		handler.refresh(save, chart, false);
	}
}