package net.osmand.plus.routepreparationmenu.cards;

import android.graphics.Matrix;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture;
import com.github.mikephil.charting.listener.OnChartGestureListener;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetAxisType;
import net.osmand.plus.helpers.GpxUiHelper.OrderedLineDataSet;
import net.osmand.plus.routing.RoutingHelper;

import java.util.ArrayList;
import java.util.List;

public class RouteStatisticCard extends BaseCard {

	private GPXFile gpx;
	private GpxDisplayItem gpxItem;
	@Nullable
	private OrderedLineDataSet slopeDataSet;
	@Nullable
	private OrderedLineDataSet elevationDataSet;
	private View.OnTouchListener onTouchListener;

	public RouteStatisticCard(MapActivity mapActivity, GPXFile gpx, View.OnTouchListener onTouchListener) {
		super(mapActivity);
		this.gpx = gpx;
		this.onTouchListener = onTouchListener;
		makeGpxDisplayItem();
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.route_info_header;
	}

	@Override
	protected void updateContent() {
		OsmandApplication app = getMyApplication();
		RoutingHelper routingHelper = app.getRoutingHelper();

		((ImageView) view.findViewById(R.id.distance_icon)).setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_route_distance));
		((ImageView) view.findViewById(R.id.time_icon)).setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_time_span));

		int dist = routingHelper.getLeftDistance();
		int time = routingHelper.getLeftTime();
		int hours = time / (60 * 60);
		int minutes = (time / 60) % 60;
		TextView distanceTv = (TextView) view.findViewById(R.id.distance);
		String text = OsmAndFormatter.getFormattedDistance(dist, app);
		SpannableStringBuilder distanceStr = new SpannableStringBuilder(text);
		int spaceIndex = text.indexOf(" ");
		if (spaceIndex != -1) {
			distanceStr.setSpan(new ForegroundColorSpan(ContextCompat.getColor(app, nightMode ? R.color.primary_text_dark : R.color.primary_text_light)), 0, spaceIndex, 0);
		}
		distanceTv.setText(distanceStr);
		SpannableStringBuilder timeStr = new SpannableStringBuilder();
		if (hours > 0) {
			timeStr.append(String.valueOf(hours)).append(" ").append(app.getString(R.string.osmand_parking_hour)).append(" ");
		}
		if (minutes > 0) {
			timeStr.append(String.valueOf(minutes)).append(" ").append(app.getString(R.string.osmand_parking_minute));
		}
		spaceIndex = timeStr.toString().lastIndexOf(" ");
		if (spaceIndex != -1) {
			timeStr.setSpan(new ForegroundColorSpan(ContextCompat.getColor(app, nightMode ? R.color.primary_text_dark : R.color.primary_text_light)), 0, spaceIndex, 0);
		}
		TextView timeTv = (TextView) view.findViewById(R.id.time);
		timeTv.setText(timeStr);

		TextView arriveTimeTv = (TextView) view.findViewById(R.id.time_desc);
		String arriveStr = app.getString(R.string.arrive_at_time, OsmAndFormatter.getFormattedTime(time, true));
		arriveTimeTv.setText(arriveStr);

		GPXTrackAnalysis analysis = gpx.getAnalysis(0);

		buildHeader(analysis);

		((TextView) view.findViewById(R.id.average_text)).setText(OsmAndFormatter.getFormattedAlt(analysis.avgElevation, app));

		String min = OsmAndFormatter.getFormattedAlt(analysis.minElevation, app);
		String max = OsmAndFormatter.getFormattedAlt(analysis.maxElevation, app);
		((TextView) view.findViewById(R.id.range_text)).setText(min + " - " + max);

		String asc = OsmAndFormatter.getFormattedAlt(analysis.diffElevationUp, app);
		String desc = OsmAndFormatter.getFormattedAlt(analysis.diffElevationDown, app);
		((TextView) view.findViewById(R.id.descent_text)).setText(desc);
		((TextView) view.findViewById(R.id.ascent_text)).setText(asc);

		((ImageView) view.findViewById(R.id.average_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_altitude_average));
		((ImageView) view.findViewById(R.id.range_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_altitude_average));
		((ImageView) view.findViewById(R.id.descent_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_altitude_descent));
		((ImageView) view.findViewById(R.id.ascent_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_altitude_ascent));

		if (isTransparentBackground()) {
			view.setBackgroundDrawable(null);
		}
	}

	@Nullable
	public OrderedLineDataSet getSlopeDataSet() {
		return slopeDataSet;
	}

	@Nullable
	public OrderedLineDataSet getElevationDataSet() {
		return elevationDataSet;
	}

	private void makeGpxDisplayItem() {
		String groupName = getMyApplication().getString(R.string.current_route);
		GpxSelectionHelper.GpxDisplayGroup group = getMyApplication().getSelectedGpxHelper().buildGpxDisplayGroup(gpx, 0, groupName);
		if (group != null && group.getModifiableList().size() > 0) {
			gpxItem = group.getModifiableList().get(0);
			if (gpxItem != null) {
				gpxItem.route = true;
			}
		}
	}

	private void buildHeader(GPXTrackAnalysis analysis) {
		final LineChart mChart = (LineChart) view.findViewById(R.id.chart);
		GpxUiHelper.setupGPXChart(mChart, 4, 24f, 16f, !nightMode, true);
		mChart.setOnTouchListener(onTouchListener);

		if (analysis.hasElevationData) {
			List<ILineDataSet> dataSets = new ArrayList<>();
			OrderedLineDataSet slopeDataSet = null;
			OrderedLineDataSet elevationDataSet = GpxUiHelper.createGPXElevationDataSet(app, mChart, analysis,
					GPXDataSetAxisType.DISTANCE, false, true);
			if (elevationDataSet != null) {
				dataSets.add(elevationDataSet);
				slopeDataSet = GpxUiHelper.createGPXSlopeDataSet(app, mChart, analysis,
						GPXDataSetAxisType.DISTANCE, elevationDataSet.getValues(), true, true);
			}
			if (slopeDataSet != null) {
				dataSets.add(slopeDataSet);
			}
			this.elevationDataSet = elevationDataSet;
			this.slopeDataSet = slopeDataSet;

			LineData data = new LineData(dataSets);
			mChart.setData(data);

			mChart.setOnChartGestureListener(new OnChartGestureListener() {

				float highlightDrawX = -1;

				@Override
				public void onChartGestureStart(MotionEvent me, ChartGesture lastPerformedGesture) {
					if (mChart.getHighlighted() != null && mChart.getHighlighted().length > 0) {
						highlightDrawX = mChart.getHighlighted()[0].getDrawX();
					} else {
						highlightDrawX = -1;
					}
				}

				@Override
				public void onChartGestureEnd(MotionEvent me, ChartGesture lastPerformedGesture) {
					gpxItem.chartMatrix = new Matrix(mChart.getViewPortHandler().getMatrixTouch());
					Highlight[] highlights = mChart.getHighlighted();
					if (highlights != null && highlights.length > 0) {
						gpxItem.chartHighlightPos = highlights[0].getX();
					} else {
						gpxItem.chartHighlightPos = -1;
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
					if (highlightDrawX != -1) {
						Highlight h = mChart.getHighlightByTouchPoint(highlightDrawX, 0f);
						if (h != null) {
							mChart.highlightValue(h);
						}
					}
				}
			});
			mChart.setVisibility(View.VISIBLE);
		} else {
			mChart.setVisibility(View.GONE);
		}
	}
}