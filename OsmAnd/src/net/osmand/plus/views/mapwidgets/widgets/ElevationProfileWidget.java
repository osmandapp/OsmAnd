package net.osmand.plus.views.mapwidgets.widgets;

import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetAxisType;
import net.osmand.plus.helpers.GpxUiHelper.OrderedLineDataSet;
import net.osmand.plus.measurementtool.graph.CommonGraphAdapter;
import net.osmand.plus.myplaces.GPXItemPagerAdapter;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.layers.GPXLayer;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu.MAX_DISTANCE_LOCATION_PROJECTION;

public class ElevationProfileWidget {

	private final MapActivity map;
	private final OsmandApplication app;
	private final OsmandSettings settings;

	private View view;
	private View uphillView;
	private View downhillView;
	private View slopeView;
	private LineChart chart;
	private CommonGraphAdapter graphAdapter;

	private GPXTrackAnalysis analysis;
	private GpxDisplayItem gpxItem;
	private TrkSegment segment;
	private GPXFile gpx;

	private boolean showSlopes = false;
	private boolean routeAvailable = false;
	private boolean shouldUpdateGraph = false;

	public ElevationProfileWidget(MapActivity map) {
		this.map = map;
		app = map.getMyApplication();
		settings = app.getSettings();
		initView();
	}

	private void initView() {
		this.view = map.findViewById(R.id.elevation_profile_widget_layout);
		setupStatisticBlocks();
		setupGraph();
	}

	private void setupStatisticBlocks() {
		uphillView = setupStatisticBlock(R.id.uphill_widget,
				R.string.shared_string_uphill,
				R.drawable.ic_action_ascent_arrow_16);

		downhillView = setupStatisticBlock(R.id.downhill_widget,
				R.string.shared_string_downhill,
				R.drawable.ic_action_descent_arrow_16);

		slopeView = setupStatisticBlock(R.id.slope_widget,
				R.string.shared_string_slope,
				R.drawable.ic_action_percent_16);
	}

	private View setupStatisticBlock(int viewId, int textId, int iconId) {
		View blockView = view.findViewById(viewId);

		TextView title = blockView.findViewById(R.id.title);
		title.setText(textId);

		ImageView icon = blockView.findViewById(R.id.image);
		icon.setImageResource(iconId);

		return blockView;
	}

	private void setupGraph() {
		gpx = GpxUiHelper.makeGpxFromRoute(app.getRoutingHelper().getRoute(), app);
		analysis = gpx.getAnalysis(0);
		gpxItem = GpxUiHelper.makeGpxDisplayItem(app, gpx);

		chart = (LineChart) view.findViewById(R.id.line_chart);
		GpxUiHelper.setupGPXChart(chart, 4, 24f, 16f, !isNightMode(), true);
		graphAdapter = new CommonGraphAdapter(chart, true);

		if (analysis.hasElevationData) {
			List<ILineDataSet> dataSets = new ArrayList<>();
			OrderedLineDataSet elevationDataSet = GpxUiHelper.createGPXElevationDataSet(app, chart, analysis,
					GPXDataSetAxisType.DISTANCE, false, true, false);
			dataSets.add(elevationDataSet);

			if (showSlopes) {
				OrderedLineDataSet slopeDataSet = GpxUiHelper.createGPXSlopeDataSet(app, chart, analysis,
						GPXDataSetAxisType.DISTANCE, elevationDataSet.getValues(), true, true, false);
				if (slopeDataSet != null) {
					dataSets.add(slopeDataSet);
				}
			}

			graphAdapter.updateContent(new LineData(dataSets), gpxItem);
			chart.setVisibility(View.VISIBLE);
			shouldUpdateGraph = false;
		} else {
			chart.setVisibility(View.GONE);
		}
	}

	public void updateInfo() {
		updateSettings();
		boolean visible = map.getWidgetsVisibilityHelper().shouldShowElevationProfileWidget();
		AndroidUiHelper.updateVisibility(view, visible);
		if (visible) {
			updateInfoImpl();
		}
	}

	private void updateInfoImpl() {
		String uphill = OsmAndFormatter.getFormattedAlt(calculateUphill(), app);
		String downhill = OsmAndFormatter.getFormattedAlt(calculateDownhill(), app);
		String slope = calculateSlope() + " %";

		((TextView) uphillView.findViewById(R.id.value)).setText(uphill);
		((TextView) downhillView.findViewById(R.id.value)).setText(downhill);
		((TextView) slopeView.findViewById(R.id.value)).setText(slope);

		if (shouldUpdateGraph) {
			setupGraph();
		}
		updateGraph();
	}

	private void updateSettings() {
		boolean previousShowSlopes = showSlopes;
		showSlopes = settings.SHOW_SLOPES_ON_ELEVATION_WIDGET.get();
		boolean slopesChanged = previousShowSlopes != showSlopes;

		boolean previousRouteAvailable = routeAvailable;
		routeAvailable = app.getRoutingHelper().isRouteCalculated();
		boolean routeChanged = previousRouteAvailable != routeAvailable;

		if (!shouldUpdateGraph) {
			shouldUpdateGraph = slopesChanged || routeChanged;
		}
	}

	// copy pasted from TrackDetailsMenu
	private void updateGraph() {
		Location location = app.getLocationProvider().getLastKnownLocation();
		TrkSegment segment = getTrackSegment(chart);
		LineData lineData = chart.getLineData();
		List<ILineDataSet> ds = lineData != null ? lineData.getDataSets() : null;
		if (ds != null && ds.size() > 0 && gpxItem != null && segment != null) {
			RotatedTileBox tb = map.getMapView().getCurrentRotatedTileBox();
			int mx = (int) tb.getPixXFromLatLon(location.getLatitude(), location.getLongitude());
			int my = (int) tb.getPixYFromLatLon(location.getLatitude(), location.getLongitude());
			int r = (int) (MAX_DISTANCE_LOCATION_PROJECTION * tb.getPixDensity());
			Pair<WptPt, WptPt> points = GPXLayer.findPointsNearSegment(tb, segment.points, r, mx, my);
			if (points != null) {
				LatLon latLon = tb.getLatLonFromPixel(mx, my);
				gpxItem.locationOnMap = GPXLayer.createProjectionPoint(points.first, points.second, latLon);

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
		}
	}

	// copy pasted from TrackDetailsMenu
	@Nullable
	private TrkSegment getTrackSegment(@NonNull LineChart chart) {
		TrkSegment segment = this.segment;
		if (segment == null) {
			LineData lineData = chart.getLineData();
			List<ILineDataSet> ds = lineData != null ? lineData.getDataSets() : null;
			if (ds != null && ds.size() > 0 && gpxItem != null) {
				this.segment = GPXItemPagerAdapter.getSegmentForAnalysis(gpxItem, gpxItem.analysis);
			}
		}
		return segment;
	}

	private double calculateUphill() {
		return analysis != null ? analysis.diffElevationUp : 0;
	}

	private double calculateDownhill() {
		return analysis != null ? analysis.diffElevationDown : 0;
	}

	private int calculateSlope() {
		return 0;
	}

	private boolean isNightMode() {
		return app.getDaynightHelper().isNightModeForMapControls();
	}
}
