package net.osmand.plus.myplaces;

import android.annotation.SuppressLint;
import android.graphics.Matrix;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.viewpager.widget.PagerAdapter;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.GPXUtilities.Track;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetAxisType;
import net.osmand.plus.helpers.GpxUiHelper.LineGraphType;
import net.osmand.plus.helpers.GpxUiHelper.OrderedLineDataSet;
import net.osmand.plus.track.TrackDisplayHelper;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;
import net.osmand.plus.views.controls.PagerSlidingTabStrip.CustomTabProvider;
import net.osmand.plus.views.controls.WrapContentHeightViewPager.ViewAtPositionInterface;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.osmand.plus.helpers.GpxUiHelper.LineGraphType.ALTITUDE;
import static net.osmand.plus.helpers.GpxUiHelper.LineGraphType.SLOPE;
import static net.osmand.plus.helpers.GpxUiHelper.LineGraphType.SPEED;

public class GPXItemPagerAdapter extends PagerAdapter implements CustomTabProvider, ViewAtPositionInterface {

	private OsmandApplication app;
	private UiUtilities iconsCache;
	private TrackDisplayHelper displayHelper;
	private Map<GPXTabItemType, List<ILineDataSet>> dataSetsMap = new HashMap<>();

	private WptPt selectedWpt;
	private TrkSegment segment;
	private GpxDisplayItem gpxItem;
	private GPXTabItemType[] tabTypes;

	private PagerSlidingTabStrip tabs;
	private SparseArray<View> views = new SparseArray<>();
	private SegmentActionsListener actionsListener;

	private boolean chartClicked;


	public GPXItemPagerAdapter(@NonNull PagerSlidingTabStrip tabs,
							   @NonNull GpxDisplayItem gpxItem,
							   @NonNull TrackDisplayHelper displayHelper,
							   @NonNull SegmentActionsListener actionsListener) {
		super();
		this.tabs = tabs;
		this.gpxItem = gpxItem;
		this.displayHelper = displayHelper;
		this.actionsListener = actionsListener;
		app = (OsmandApplication) tabs.getContext().getApplicationContext();
		iconsCache = app.getUIUtilities();
		fetchTabTypes();
	}

	private void fetchTabTypes() {
		List<GPXTabItemType> tabTypeList = new ArrayList<>();
		tabTypeList.add(GPXTabItemType.GPX_TAB_ITEM_GENERAL);
		if (gpxItem != null && gpxItem.analysis != null) {
			if (gpxItem.analysis.hasElevationData) {
				tabTypeList.add(GPXTabItemType.GPX_TAB_ITEM_ALTITUDE);
			}
			if (gpxItem.analysis.isSpeedSpecified()) {
				tabTypeList.add(GPXTabItemType.GPX_TAB_ITEM_SPEED);
			}
		}
		tabTypes = tabTypeList.toArray(new GPXTabItemType[0]);
	}

	private List<ILineDataSet> getDataSets(LineChart chart, GPXTabItemType tabType,
										   LineGraphType firstType, LineGraphType secondType) {
		List<ILineDataSet> dataSets = dataSetsMap.get(tabType);
		if (dataSets == null && chart != null) {
			GPXTrackAnalysis analysis = gpxItem.analysis;
			GpxDataItem gpxDataItem = displayHelper.getGpxDataItem();
			boolean calcWithoutGaps = gpxItem.isGeneralTrack() && gpxDataItem != null && !gpxDataItem.isJoinSegments();
			dataSets = GpxUiHelper.getDataSets(chart, app, analysis, firstType, secondType, calcWithoutGaps);
			dataSetsMap.put(tabType, dataSets);
		}
		return dataSets;
	}

	private TrkSegment getTrackSegment(LineChart chart) {
		if (segment == null) {
			LineData lineData = chart.getLineData();
			List<ILineDataSet> ds = lineData != null ? lineData.getDataSets() : null;
			if (ds != null && ds.size() > 0) {
				for (GPXUtilities.Track t : gpxItem.group.getGpx().tracks) {
					for (TrkSegment s : t.segments) {
						if (s.points.size() > 0 && s.points.get(0).equals(gpxItem.analysis.locationStart)) {
							segment = s;
							break;
						}
					}
					if (segment != null) {
						break;
					}
				}
			}
		}
		return segment;
	}

	private WptPt getPoint(LineChart chart, float pos) {
		WptPt wpt = null;
		LineData lineData = chart.getLineData();
		List<ILineDataSet> ds = lineData != null ? lineData.getDataSets() : null;
		if (ds != null && ds.size() > 0) {
			TrkSegment segment = getTrackSegment(chart);
			OrderedLineDataSet dataSet = (OrderedLineDataSet) ds.get(0);
			if (gpxItem.chartAxisType == GPXDataSetAxisType.TIME) {
				float time = pos * 1000;
				for (WptPt p : segment.points) {
					if (p.time - gpxItem.analysis.startTime >= time) {
						wpt = p;
						break;
					}
				}
			} else {
				float distance = pos * dataSet.getDivX();
				double totalDistance = 0;
				for (int i = 0; i < segment.points.size(); i++) {
					WptPt currentPoint = segment.points.get(i);
					if (i != 0) {
						WptPt previousPoint = segment.points.get(i - 1);
						totalDistance += MapUtils.getDistance(previousPoint.lat, previousPoint.lon, currentPoint.lat, currentPoint.lon);
					}
					if (currentPoint.distance >= distance || Math.abs(totalDistance - distance) < 0.1) {
						wpt = currentPoint;
						break;
					}
				}
			}
		}
		return wpt;
	}

	@Override
	public int getCount() {
		return tabTypes.length;
	}

	@Override
	public CharSequence getPageTitle(int position) {
		return tabTypes[position].toHumanString(app);
	}

	@NonNull
	@Override
	public Object instantiateItem(@NonNull ViewGroup container, int position) {
		GPXTabItemType tabType = tabTypes[position];
		View view = getViewForTab(container, tabType);
		GPXFile gpxFile = displayHelper.getGpx();
		if (gpxFile != null && gpxItem != null) {
			GPXTrackAnalysis analysis = gpxItem.analysis;
			LineChart chart = view.findViewById(R.id.chart);
			setupChart(view, chart);

			switch (tabType) {
				case GPX_TAB_ITEM_GENERAL:
					setupGeneralTab(view, chart, analysis, gpxFile, position);
					break;
				case GPX_TAB_ITEM_ALTITUDE:
					setupAltitudeTab(view, chart, analysis, gpxFile, position);
					break;
				case GPX_TAB_ITEM_SPEED:
					setupSpeedTab(view, chart, analysis, gpxFile, position);
					break;
			}
		}
		container.addView(view, 0);
		views.put(position, view);
		return view;
	}

	private View getViewForTab(@NonNull ViewGroup container, @NonNull GPXTabItemType tabType) {
		LayoutInflater inflater = LayoutInflater.from(container.getContext());
		switch (tabType) {
			case GPX_TAB_ITEM_ALTITUDE:
				return inflater.inflate(R.layout.gpx_item_altitude, container, false);
			case GPX_TAB_ITEM_SPEED:
				return inflater.inflate(R.layout.gpx_item_speed, container, false);
			case GPX_TAB_ITEM_GENERAL:
			default:
				return inflater.inflate(R.layout.gpx_item_general, container, false);
		}
	}

	private void setupSpeedTab(View view, LineChart chart, GPXTrackAnalysis analysis, GPXFile gpxFile, int position) {
		if (analysis != null && analysis.isSpeedSpecified()) {
			if (analysis.hasSpeedData) {
				GpxUiHelper.setupGPXChart(app, chart, 4);
				chart.setData(new LineData(getDataSets(chart, GPXTabItemType.GPX_TAB_ITEM_SPEED, SPEED, null)));
				updateChart(chart);
				chart.setVisibility(View.VISIBLE);
			} else {
				chart.setVisibility(View.GONE);
			}
			((ImageView) view.findViewById(R.id.average_icon))
					.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_speed));
			((ImageView) view.findViewById(R.id.max_icon))
					.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_max_speed));
			((ImageView) view.findViewById(R.id.time_moving_icon))
					.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_time_span));
			((ImageView) view.findViewById(R.id.distance_icon))
					.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_polygom_dark));

			String avg = OsmAndFormatter.getFormattedSpeed(analysis.avgSpeed, app);
			String max = OsmAndFormatter.getFormattedSpeed(analysis.maxSpeed, app);

			((TextView) view.findViewById(R.id.average_text)).setText(avg);
			((TextView) view.findViewById(R.id.max_text)).setText(max);

			view.findViewById(R.id.gpx_join_gaps_container).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (displayHelper.setJoinSegments(!displayHelper.isJoinSegments())) {
						for (int i = 0; i < getCount(); i++) {
							View view = getViewAtPosition(i);
							updateJoinGapsInfo(view, i);
						}
					}
				}
			});
		} else {
			chart.setVisibility(View.GONE);
			view.findViewById(R.id.average_max).setVisibility(View.GONE);
			view.findViewById(R.id.list_divider).setVisibility(View.GONE);
			view.findViewById(R.id.time_distance).setVisibility(View.GONE);
		}
		updateJoinGapsInfo(view, position);
		view.findViewById(R.id.analyze_on_map).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openAnalyzeOnMap(GPXTabItemType.GPX_TAB_ITEM_SPEED);
			}
		});
		if (gpxFile.showCurrentTrack) {
			view.findViewById(R.id.split_interval).setVisibility(View.GONE);
		} else {
			view.findViewById(R.id.split_interval).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					openSplitIntervalScreen();
				}
			});
		}
		ImageView overflowMenu = view.findViewById(R.id.overflow_menu);
		if (!gpxItem.group.getTrack().generalTrack) {
			setupOptionsPopupMenu(overflowMenu, false);
		} else {
			overflowMenu.setVisibility(View.GONE);
		}
	}

	private void setupOptionsPopupMenu(ImageView overflowMenu, final boolean confirmDeletion) {
		overflowMenu.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_overflow_menu_white));
		overflowMenu.setVisibility(View.VISIBLE);
		overflowMenu.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				actionsListener.showOptionsPopupMenu(view, getTrkSegment(), confirmDeletion);
			}
		});
	}

	private void setupAltitudeTab(View view, LineChart chart, GPXTrackAnalysis analysis, GPXFile gpxFile, int position) {
		if (analysis != null) {
			if (analysis.hasElevationData) {
				GpxUiHelper.setupGPXChart(app, chart, 4);
				chart.setData(new LineData(getDataSets(chart, GPXTabItemType.GPX_TAB_ITEM_ALTITUDE, ALTITUDE, SLOPE)));
				updateChart(chart);
				chart.setVisibility(View.VISIBLE);
			} else {
				chart.setVisibility(View.GONE);
			}
			((ImageView) view.findViewById(R.id.average_icon))
					.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_altitude_average));
			((ImageView) view.findViewById(R.id.range_icon))
					.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_altitude_average));
			((ImageView) view.findViewById(R.id.ascent_icon))
					.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_altitude_ascent));
			((ImageView) view.findViewById(R.id.descent_icon))
					.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_altitude_descent));

			String min = OsmAndFormatter.getFormattedAlt(analysis.minElevation, app);
			String max = OsmAndFormatter.getFormattedAlt(analysis.maxElevation, app);
			String asc = OsmAndFormatter.getFormattedAlt(analysis.diffElevationUp, app);
			String desc = OsmAndFormatter.getFormattedAlt(analysis.diffElevationDown, app);

			((TextView) view.findViewById(R.id.average_text))
					.setText(OsmAndFormatter.getFormattedAlt(analysis.avgElevation, app));
			((TextView) view.findViewById(R.id.range_text)).setText(String.format("%s - %s", min, max));
			((TextView) view.findViewById(R.id.ascent_text)).setText(asc);
			((TextView) view.findViewById(R.id.descent_text)).setText(desc);

			view.findViewById(R.id.gpx_join_gaps_container).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (displayHelper.setJoinSegments(!displayHelper.isJoinSegments())) {
						for (int i = 0; i < getCount(); i++) {
							View view = getViewAtPosition(i);
							updateJoinGapsInfo(view, i);
						}
					}
				}
			});
		} else {
			chart.setVisibility(View.GONE);
			view.findViewById(R.id.average_range).setVisibility(View.GONE);
			view.findViewById(R.id.list_divider).setVisibility(View.GONE);
			view.findViewById(R.id.ascent_descent).setVisibility(View.GONE);
		}
		updateJoinGapsInfo(view, position);
		view.findViewById(R.id.analyze_on_map).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openAnalyzeOnMap(GPXTabItemType.GPX_TAB_ITEM_ALTITUDE);
			}
		});
		if (gpxFile.showCurrentTrack) {
			view.findViewById(R.id.split_interval).setVisibility(View.GONE);
		} else {
			view.findViewById(R.id.split_interval).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					openSplitIntervalScreen();
				}
			});
		}
		ImageView overflowMenu = view.findViewById(R.id.overflow_menu);
		if (!gpxItem.group.getTrack().generalTrack) {
			setupOptionsPopupMenu(overflowMenu, false);
		} else {
			overflowMenu.setVisibility(View.GONE);
		}
	}

	private void setupGeneralTab(View view, LineChart chart, GPXTrackAnalysis analysis, GPXFile gpxFile, int position) {
		if (analysis != null) {
			if (analysis.hasElevationData || analysis.hasSpeedData) {
				GpxUiHelper.setupGPXChart(app, chart, 4);
				chart.setData(new LineData(getDataSets(chart, GPXTabItemType.GPX_TAB_ITEM_GENERAL, ALTITUDE, SPEED)));
				updateChart(chart);
				chart.setVisibility(View.VISIBLE);
			} else {
				chart.setVisibility(View.GONE);
			}

			((ImageView) view.findViewById(R.id.distance_icon))
					.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_polygom_dark));
			((ImageView) view.findViewById(R.id.duration_icon))
					.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_time_span));
			((ImageView) view.findViewById(R.id.start_time_icon))
					.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_time_start));
			((ImageView) view.findViewById(R.id.end_time_icon))
					.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_time_end));

			view.findViewById(R.id.gpx_join_gaps_container).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (displayHelper.setJoinSegments(!displayHelper.isJoinSegments())) {
						actionsListener.updateContent();
						for (int i = 0; i < getCount(); i++) {
							View view = getViewAtPosition(i);
							updateJoinGapsInfo(view, i);
						}
					}
				}
			});
			if (analysis.timeSpan > 0) {
				DateFormat tf = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
				DateFormat df = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM);

				Date start = new Date(analysis.startTime);
				((TextView) view.findViewById(R.id.start_time_text)).setText(tf.format(start));
				((TextView) view.findViewById(R.id.start_date_text)).setText(df.format(start));
				Date end = new Date(analysis.endTime);
				((TextView) view.findViewById(R.id.end_time_text)).setText(tf.format(end));
				((TextView) view.findViewById(R.id.end_date_text)).setText(df.format(end));
			} else {
				view.findViewById(R.id.list_divider).setVisibility(View.GONE);
				view.findViewById(R.id.start_end_time).setVisibility(View.GONE);
			}
		} else {
			chart.setVisibility(View.GONE);
			view.findViewById(R.id.distance_time_span).setVisibility(View.GONE);
			view.findViewById(R.id.list_divider).setVisibility(View.GONE);
			view.findViewById(R.id.start_end_time).setVisibility(View.GONE);
		}
		updateJoinGapsInfo(view, position);
		view.findViewById(R.id.analyze_on_map).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openAnalyzeOnMap(GPXTabItemType.GPX_TAB_ITEM_GENERAL);
			}
		});
		if (gpxFile.showCurrentTrack) {
			view.findViewById(R.id.split_interval).setVisibility(View.GONE);
		} else {
			view.findViewById(R.id.split_interval).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					openSplitIntervalScreen();
				}
			});
		}
		ImageView overflowMenu = view.findViewById(R.id.overflow_menu);
		if (!gpxItem.group.getTrack().generalTrack) {
			setupOptionsPopupMenu(overflowMenu, true);
		} else {
			overflowMenu.setVisibility(View.GONE);
		}
	}

	private void setupChart(final View view, final LineChart chart) {
		chart.setHighlightPerDragEnabled(chartClicked);
		chart.setOnClickListener(new View.OnClickListener() {
			@SuppressLint("ClickableViewAccessibility")
			@Override
			public void onClick(View view) {
				if (!chartClicked) {
					chartClicked = true;
					if (selectedWpt != null) {
						actionsListener.onPointSelected(selectedWpt.lat, selectedWpt.lon);
					}
				}
			}
		});
		chart.setOnTouchListener(new View.OnTouchListener() {

			private float listViewYPos;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (chartClicked) {
					actionsListener.onChartTouch();
					if (!chart.isHighlightPerDragEnabled()) {
						chart.setHighlightPerDragEnabled(true);
					}
					switch (event.getAction()) {
						case MotionEvent.ACTION_DOWN:
							listViewYPos = event.getRawY();
							break;
						case MotionEvent.ACTION_MOVE:
							actionsListener.scrollBy(Math.round(listViewYPos - event.getRawY()));
							listViewYPos = event.getRawY();
							break;
					}
				}
				return false;
			}
		});
		chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
			@Override
			public void onValueSelected(Entry e, Highlight h) {
				WptPt wpt = getPoint(chart, h.getX());
				selectedWpt = wpt;
				if (chartClicked && wpt != null) {
					actionsListener.onPointSelected(wpt.lat, wpt.lon);
				}
			}

			@Override
			public void onNothingSelected() {

			}
		});
		chart.setOnChartGestureListener(new OnChartGestureListener() {

			float highlightDrawX = -1;

			@Override
			public void onChartGestureStart(MotionEvent me, ChartGesture lastPerformedGesture) {
				if (chart.getHighlighted() != null && chart.getHighlighted().length > 0) {
					highlightDrawX = chart.getHighlighted()[0].getDrawX();
				} else {
					highlightDrawX = -1;
				}
			}

			@Override
			public void onChartGestureEnd(MotionEvent me, ChartGesture lastPerformedGesture) {
				gpxItem.chartMatrix = new Matrix(chart.getViewPortHandler().getMatrixTouch());
				Highlight[] highlights = chart.getHighlighted();
				if (highlights != null && highlights.length > 0) {
					gpxItem.chartHighlightPos = highlights[0].getX();
				} else {
					gpxItem.chartHighlightPos = -1;
				}
				if (chartClicked) {
					for (int i = 0; i < getCount(); i++) {
						View v = getViewAtPosition(i);
						if (v != view) {
							updateChart(i);
						}
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
				if (chartClicked && highlightDrawX != -1) {
					Highlight h = chart.getHighlightByTouchPoint(highlightDrawX, 0f);
					if (h != null) {
						chart.highlightValue(h);
						WptPt wpt = getPoint(chart, h.getX());
						if (wpt != null) {
							actionsListener.onPointSelected(wpt.lat, wpt.lon);
						}
					}
				}
			}
		});
	}

	@Override
	public void destroyItem(@NonNull ViewGroup collection, int position, @NonNull Object view) {
		views.remove(position);
		collection.removeView((View) view);
	}

	@Override
	public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
		return view == object;
	}

	@Override
	public View getCustomTabView(@NonNull ViewGroup parent, int position) {
		View tab = LayoutInflater.from(parent.getContext()).inflate(R.layout.gpx_tab, parent, false);
		tab.setTag(tabTypes[position].name());
		deselect(tab);
		return tab;
	}

	@Override
	public void select(View tab) {
		GPXTabItemType tabType = GPXTabItemType.valueOf((String) tab.getTag());
		ImageView img = tab.findViewById(R.id.tab_image);
		switch (tabs.getTabSelectionType()) {
			case ALPHA:
				img.setAlpha(tabs.getTabTextSelectedAlpha());
				break;
			case SOLID_COLOR:
				img.setImageDrawable(iconsCache.getPaintedIcon(tabType.getIconId(), tabs.getTextColor()));
				break;
		}
	}

	@Override
	public void deselect(View tab) {
		GPXTabItemType tabType = GPXTabItemType.valueOf((String) tab.getTag());
		ImageView img = tab.findViewById(R.id.tab_image);
		switch (tabs.getTabSelectionType()) {
			case ALPHA:
				img.setAlpha(tabs.getTabTextAlpha());
				break;
			case SOLID_COLOR:
				img.setImageDrawable(iconsCache.getPaintedIcon(tabType.getIconId(), tabs.getTabInactiveTextColor()));
				break;
		}
	}

	@Override
	public View getViewAtPosition(int position) {
		return views.get(position);
	}

	void updateChart(int position) {
		View view = getViewAtPosition(position);
		if (view != null) {
			updateChart((LineChart) view.findViewById(R.id.chart));
		}
	}

	void updateJoinGapsInfo(View view, int position) {
		if (view != null) {
			GPXTrackAnalysis analysis = gpxItem.analysis;
			GPXTabItemType tabType = tabTypes[position];
			boolean visible = gpxItem.isGeneralTrack() && analysis != null && tabType.equals(GPXTabItemType.GPX_TAB_ITEM_GENERAL);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.gpx_join_gaps_container), visible);
			boolean joinSegments = displayHelper.isJoinSegments();
			((SwitchCompat) view.findViewById(R.id.gpx_join_gaps_switch)).setChecked(joinSegments);
			if (analysis != null) {
				if (tabType.equals(GPXTabItemType.GPX_TAB_ITEM_GENERAL)) {
					float totalDistance = !joinSegments && gpxItem.isGeneralTrack() ? analysis.totalDistanceWithoutGaps : analysis.totalDistance;
					float timeSpan = !joinSegments && gpxItem.isGeneralTrack() ? analysis.timeSpanWithoutGaps : analysis.timeSpan;

					((TextView) view.findViewById(R.id.distance_text)).setText(OsmAndFormatter.getFormattedDistance(totalDistance, app));
					((TextView) view.findViewById(R.id.duration_text)).setText(Algorithms.formatDuration((int) (timeSpan / 1000), app.accessibilityEnabled()));
				} else if (tabType.equals(GPXTabItemType.GPX_TAB_ITEM_SPEED)) {
					long timeMoving = !joinSegments && gpxItem.isGeneralTrack() ? analysis.timeMovingWithoutGaps : analysis.timeMoving;
					float totalDistanceMoving = !joinSegments && gpxItem.isGeneralTrack() ? analysis.totalDistanceMovingWithoutGaps : analysis.totalDistanceMoving;

					((TextView) view.findViewById(R.id.time_moving_text)).setText(Algorithms.formatDuration((int) (timeMoving / 1000), app.accessibilityEnabled()));
					((TextView) view.findViewById(R.id.distance_text)).setText(OsmAndFormatter.getFormattedDistance(totalDistanceMoving, app));
				}
			}
		}
	}

	void updateChart(LineChart chart) {
		if (chart != null && !chart.isEmpty()) {
			if (gpxItem.chartMatrix != null) {
				chart.getViewPortHandler().refresh(new Matrix(gpxItem.chartMatrix), chart, true);
			}
			if (gpxItem.chartHighlightPos != -1) {
				chart.highlightValue(gpxItem.chartHighlightPos, 0);
			} else {
				chart.highlightValue(null);
			}
		}
	}

	private TrkSegment getTrkSegment() {
		for (Track track : gpxItem.group.getGpx().tracks) {
			if (!track.generalTrack && !gpxItem.isGeneralTrack() || track.generalTrack && gpxItem.isGeneralTrack()) {
				for (TrkSegment segment : track.segments) {
					if (segment.points.size() > 0 && segment.points.get(0).equals(gpxItem.analysis.locationStart)) {
						return segment;
					}
				}
			}
		}
		return null;
	}

	void openAnalyzeOnMap(GPXTabItemType tabType) {
		List<ILineDataSet> ds = getDataSets(null, tabType, null, null);
		actionsListener.openAnalyzeOnMap(gpxItem, ds, tabType);
	}

	private void openSplitIntervalScreen() {
		actionsListener.openSplitInterval(gpxItem, getTrkSegment());
	}
}
