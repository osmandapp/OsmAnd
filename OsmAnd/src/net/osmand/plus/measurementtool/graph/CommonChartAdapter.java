package net.osmand.plus.measurementtool.graph;

import android.graphics.Matrix;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.mikephil.charting.charts.ElevationChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.tracks.dialogs.GPXItemPagerAdapter;
import net.osmand.plus.myplaces.tracks.GPXTabItemType;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.utils.UiUtilities;

import java.util.HashMap;
import java.util.Map;

public class CommonChartAdapter extends BaseChartAdapter<ElevationChart, LineData, GpxDisplayItem> {

	private Highlight highlight;
	private final Map<String, ExternalValueSelectedListener> externalValueSelectedListeners = new HashMap<>();
	private ExternalGestureListener externalGestureListener;

	private GPXTabItemType gpxGraphType;

	public CommonChartAdapter(OsmandApplication app, ElevationChart chart, boolean usedOnMap) {
		super(app, chart, usedOnMap);
	}

	@Override
	protected void prepareChartView() {
		super.prepareChartView();

		chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
			@Override
			public void onValueSelected(Entry e, Highlight h) {
				highlight = h;
				for (ExternalValueSelectedListener listener : externalValueSelectedListeners.values()) {
					listener.onValueSelected(e, h);
				}
			}

			@Override
			public void onNothingSelected() {
				for (ExternalValueSelectedListener listener : externalValueSelectedListeners.values()) {
					listener.onNothingSelected();
				}
			}
		});

		chart.setOnChartGestureListener(new OnChartGestureListener() {
			boolean hasTranslated;
			float highlightDrawX = -1;

			@Override
			public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
				hasTranslated = false;
				if (chart.getHighlighted() != null && chart.getHighlighted().length > 0) {
					highlightDrawX = chart.getHighlighted()[0].getDrawX();
				} else {
					highlightDrawX = -1;
				}
				if (externalGestureListener != null) {
					externalGestureListener.onChartGestureStart(me, lastPerformedGesture);
				}
			}

			@Override
			public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
				GpxDisplayItem gpxItem = getGpxItem();
				gpxItem.chartMatrix = new Matrix(chart.getViewPortHandler().getMatrixTouch());
				Highlight[] highlights = chart.getHighlighted();
				if (highlights != null && highlights.length > 0) {
					gpxItem.chartHighlightPos = highlights[0].getX();
				} else {
					gpxItem.chartHighlightPos = -1;
				}
				if (externalGestureListener != null) {
					externalGestureListener.onChartGestureEnd(me, lastPerformedGesture, hasTranslated);
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
						chart.highlightValue(h, true);
					}
				}
			}
		});
	}

	@Override
	protected void attachBottomInfo() {
		if (additionalData == null || additionalData.analysis == null || gpxGraphType == null) {
			AndroidUiHelper.updateVisibility(bottomInfoContainer, false);
			return;
		}
		AndroidUiHelper.updateVisibility(bottomInfoContainer, true);

		GPXTrackAnalysis analysis = additionalData.analysis;

		if (gpxGraphType == GPXTabItemType.GPX_TAB_ITEM_GENERAL) {
			attachGeneralStatistics(analysis);
		} else if (gpxGraphType == GPXTabItemType.GPX_TAB_ITEM_ALTITUDE) {
			attachAltitudeStatistics(analysis);
		} else if (gpxGraphType == GPXTabItemType.GPX_TAB_ITEM_SPEED) {
			attachSpeedStatistics(analysis);
		}
	}

	private void attachGeneralStatistics(@NonNull GPXTrackAnalysis analysis) {
		LayoutInflater inflater = createThemedInflater();
		View generalStatistics = inflater.inflate(R.layout.gpx_item_general_statistics, bottomInfoContainer, false);
		GPXItemPagerAdapter.updateGeneralTabInfo(generalStatistics, app, analysis, false, false);
		GPXItemPagerAdapter.setupGeneralStatisticsIcons(generalStatistics, app.getUIUtilities());

		boolean timeDefined = analysis.getTimeSpan() > 0;
		AndroidUiHelper.updateVisibility(generalStatistics.findViewById(R.id.list_divider), timeDefined);
		AndroidUiHelper.updateVisibility(generalStatistics.findViewById(R.id.bottom_line_blocks), timeDefined);
		if (timeDefined) {
			GPXItemPagerAdapter.setupTimeSpanStatistics(generalStatistics, analysis);
		}

		bottomInfoContainer.addView(generalStatistics);
	}

	private void attachAltitudeStatistics(@NonNull GPXTrackAnalysis analysis) {
		LayoutInflater inflater = createThemedInflater();
		View altitudeStatistics = inflater.inflate(R.layout.gpx_item_altitude_statistics, bottomInfoContainer, false);
		GPXItemPagerAdapter.updateAltitudeTabInfo(altitudeStatistics, app, analysis);
		GPXItemPagerAdapter.setupAltitudeStatisticsIcons(altitudeStatistics, app.getUIUtilities());
		bottomInfoContainer.addView(altitudeStatistics);
	}

	private void attachSpeedStatistics(@NonNull GPXTrackAnalysis analysis) {
		LayoutInflater inflater = createThemedInflater();
		View speedStatistics = inflater.inflate(R.layout.gpx_item_speed_statistics, bottomInfoContainer, false);
		GPXItemPagerAdapter.updateSpeedTabInfo(speedStatistics, app, analysis, false, false);
		GPXItemPagerAdapter.setupSpeedStatisticsIcons(speedStatistics, app.getUIUtilities());
		bottomInfoContainer.addView(speedStatistics);
	}

	private LayoutInflater createThemedInflater() {
		return LayoutInflater.from(UiUtilities.getThemedContext(app, isNightMode()));
	}

	public void setGpxGraphType(@Nullable GPXTabItemType gpxGraphType) {
		this.gpxGraphType = gpxGraphType;
	}

	public void addValueSelectedListener(String key, ExternalValueSelectedListener listener) {
		this.externalValueSelectedListeners.put(key, listener);
	}

	public void setExternalGestureListener(ExternalGestureListener listener) {
		this.externalGestureListener = listener;
	}

	@Override
	public void highlight(Highlight h) {
		super.highlight(h);
		chart.highlightValue(highlight);
	}

	public GpxDisplayItem getGpxItem() {
		return additionalData;
	}
}
