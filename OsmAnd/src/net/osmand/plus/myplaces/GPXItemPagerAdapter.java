package net.osmand.plus.myplaces;

import android.annotation.SuppressLint;
import android.graphics.Matrix;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import net.osmand.AndroidUtils;
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
import net.osmand.plus.UiUtilities.CustomRadioButtonType;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetAxisType;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetType;
import net.osmand.plus.helpers.GpxUiHelper.LineGraphType;
import net.osmand.plus.helpers.GpxUiHelper.OrderedLineDataSet;
import net.osmand.plus.track.TrackDisplayHelper;
import net.osmand.plus.views.controls.PagerSlidingTabStrip.CustomTabProvider;
import net.osmand.plus.views.controls.WrapContentHeightViewPager.ViewAtPositionInterface;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.osmand.plus.helpers.GpxUiHelper.LineGraphType.ALTITUDE;
import static net.osmand.plus.helpers.GpxUiHelper.LineGraphType.SLOPE;
import static net.osmand.plus.helpers.GpxUiHelper.LineGraphType.SPEED;
import static net.osmand.plus.myplaces.GPXTabItemType.GPX_TAB_ITEM_ALTITUDE;
import static net.osmand.plus.myplaces.GPXTabItemType.GPX_TAB_ITEM_GENERAL;
import static net.osmand.plus.myplaces.GPXTabItemType.GPX_TAB_ITEM_SPEED;

public class GPXItemPagerAdapter extends PagerAdapter implements CustomTabProvider, ViewAtPositionInterface {

	private static final int CHART_LABEL_COUNT = 4;

	private OsmandApplication app;
	private UiUtilities iconsCache;
	private TrackDisplayHelper displayHelper;
	private Map<GPXTabItemType, List<ILineDataSet>> dataSetsMap = new HashMap<>();

	private WptPt selectedWpt;
	private TrkSegment segment;
	private GpxDisplayItem gpxItem;
	private GPXTrackAnalysis analysis;
	private GPXTabItemType[] tabTypes;

	private SparseArray<View> views = new SparseArray<>();
	private SegmentActionsListener actionsListener;

	private boolean chartClicked;
	private boolean nightMode;
	private boolean onlyGraphs;
	private int chartHMargin = 0;

	public void setChartHMargin(int chartHMargin) {
		this.chartHMargin = chartHMargin;
	}

	private boolean isShowCurrentTrack() {
		return displayHelper.getGpx() != null && displayHelper.getGpx().showCurrentTrack;
	}

	public GPXItemPagerAdapter(@NonNull OsmandApplication app,
							   @Nullable GpxDisplayItem gpxItem,
							   @NonNull TrackDisplayHelper displayHelper,
							   boolean nightMode,
							   @NonNull SegmentActionsListener actionsListener,
							   boolean onlyGraphs) {
		super();
		this.app = app;
		this.gpxItem = gpxItem;
		this.displayHelper = displayHelper;
		this.nightMode = nightMode;
		this.actionsListener = actionsListener;
		this.onlyGraphs = onlyGraphs;
		iconsCache = app.getUIUtilities();

		updateAnalysis();
		fetchTabTypes();
	}

	private void updateAnalysis() {
		analysis = null;
		if (isShowCurrentTrack()) {
			GPXFile gpxFile = displayHelper.getGpx();
			if (gpxFile != null && !gpxFile.isEmpty()) {
				analysis = gpxFile.getAnalysis(0);
				gpxItem = GpxUiHelper.makeGpxDisplayItem(app, gpxFile, false);
			}
		} else {
			if (gpxItem != null) {
				analysis = gpxItem.analysis;
			}
		}
	}

	private void fetchTabTypes() {
		List<GPXTabItemType> tabTypeList = new ArrayList<>();
		if (isShowCurrentTrack()) {
			if (analysis != null && (analysis.hasElevationData || analysis.hasSpeedData)) {
				tabTypeList.add(GPXTabItemType.GPX_TAB_ITEM_GENERAL);
			}
		} else {
			tabTypeList.add(GPXTabItemType.GPX_TAB_ITEM_GENERAL);
		}
		if (analysis != null) {
			if (analysis.hasElevationData) {
				tabTypeList.add(GPX_TAB_ITEM_ALTITUDE);
			}
			if (analysis.isSpeedSpecified()) {
				tabTypeList.add(GPX_TAB_ITEM_SPEED);
			}
		}
		tabTypes = tabTypeList.toArray(new GPXTabItemType[0]);
	}

	private List<ILineDataSet> getDataSets(LineChart chart, GPXTabItemType tabType,
										   LineGraphType firstType, LineGraphType secondType) {
		List<ILineDataSet> dataSets = dataSetsMap.get(tabType);
		boolean withoutGaps = true;
		if (isShowCurrentTrack()) {
			GPXFile gpxFile = displayHelper.getGpx();
			withoutGaps = !app.getSavingTrackHelper().getCurrentTrack().isJoinSegments() && gpxFile != null
					&& (Algorithms.isEmpty(gpxFile.tracks) || gpxFile.tracks.get(0).generalTrack);
		} else if (gpxItem != null) {
			GpxDataItem gpxDataItem = displayHelper.getGpxDataItem();
			withoutGaps = gpxItem.isGeneralTrack() && gpxDataItem != null && !gpxDataItem.isJoinSegments();
		}
		if (chart != null && analysis != null) {
			dataSets = GpxUiHelper.getDataSets(chart, app, analysis, firstType, secondType, withoutGaps);
			if (!Algorithms.isEmpty(dataSets)) {
				dataSetsMap.remove(tabType);
			}
			dataSetsMap.put(tabType, dataSets);
		}
		return dataSets;
	}

	private TrkSegment getTrackSegment(LineChart chart) {
		if (segment == null) {
			LineData lineData = chart.getLineData();
			List<ILineDataSet> ds = lineData != null ? lineData.getDataSets() : null;
			if (ds != null && ds.size() > 0) {
				segment = getSegmentForAnalysis(gpxItem, analysis);
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
					if (p.time - analysis.startTime >= time) {
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
		LineChart chart = view.findViewById(R.id.chart);
		ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) chart.getLayoutParams();
		AndroidUtils.setMargins(lp, chartHMargin, lp.topMargin, chartHMargin, lp.bottomMargin);
		if (analysis != null) {
			setupChart(view, chart);
			switch (tabType) {
				case GPX_TAB_ITEM_GENERAL:
					setupGeneralTab(view, chart, position);
					break;
				case GPX_TAB_ITEM_ALTITUDE:
					setupAltitudeTab(view, chart, position);
					break;
				case GPX_TAB_ITEM_SPEED:
					setupSpeedTab(view, chart, position);
					break;
			}
		}
		container.addView(view, 0);
		views.put(position, view);
		return view;
	}

	private View getViewForTab(@NonNull ViewGroup container, @NonNull GPXTabItemType tabType) {
		LayoutInflater inflater = LayoutInflater.from(container.getContext());
		int layoutResId;
		if (tabType == GPX_TAB_ITEM_ALTITUDE) {
			layoutResId = R.layout.gpx_item_altitude;
		} else if (tabType == GPX_TAB_ITEM_SPEED) {
			layoutResId = R.layout.gpx_item_speed;
		} else {
			layoutResId = R.layout.gpx_item_general;
		}
		View view = inflater.inflate(layoutResId, container, false);
		if (onlyGraphs) {
			AndroidUiHelper.setVisibility(View.GONE,
					view.findViewById(R.id.gpx_join_gaps_container),
					view.findViewById(R.id.top_line_blocks),
					view.findViewById(R.id.list_divider),
					view.findViewById(R.id.bottom_line_blocks),
					view.findViewById(R.id.details_divider),
					view.findViewById(R.id.details_view)
			);
		}
		return view;
	}

	private void setupSpeedTab(View view, LineChart chart, int position) {
		if (analysis != null && analysis.isSpeedSpecified()) {
			if (analysis.hasSpeedData) {
				GpxUiHelper.setupGPXChart(app, chart, CHART_LABEL_COUNT);
				chart.setData(new LineData(getDataSets(chart, GPX_TAB_ITEM_SPEED, SPEED, null)));
				updateChart(chart);
				chart.setVisibility(View.VISIBLE);
			} else {
				chart.setVisibility(View.GONE);
			}
			if (!onlyGraphs) {
				((ImageView) view.findViewById(R.id.average_icon))
						.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_speed));
				((ImageView) view.findViewById(R.id.max_icon))
						.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_max_speed));
				((ImageView) view.findViewById(R.id.time_moving_icon))
						.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_time_moving_16));
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
							actionsListener.updateContent();
							for (int i = 0; i < getCount(); i++) {
								View view = getViewAtPosition(i);
								updateJoinGapsInfo(view, i);
							}
						}
					}
				});
			}
		} else {
			chart.setVisibility(View.GONE);
			view.findViewById(R.id.top_line_blocks).setVisibility(View.GONE);
			view.findViewById(R.id.list_divider).setVisibility(View.GONE);
			view.findViewById(R.id.bottom_line_blocks).setVisibility(View.GONE);
		}
		if (!onlyGraphs) {
			updateJoinGapsInfo(view, position);
			view.findViewById(R.id.analyze_on_map).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openAnalyzeOnMap(GPX_TAB_ITEM_SPEED);
				}
			});
			TextView overflowMenu = view.findViewById(R.id.overflow_menu);
			if (!gpxItem.group.getTrack().generalTrack) {
				setupOptionsPopupMenu(overflowMenu, false);
			} else {
				overflowMenu.setVisibility(View.GONE);
			}
		}
	}

	private void setupOptionsPopupMenu(TextView overflowMenu, final boolean confirmDeletion) {
		overflowMenu.setVisibility(View.VISIBLE);
		overflowMenu.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				actionsListener.showOptionsPopupMenu(view, getTrkSegment(), confirmDeletion, gpxItem);
			}
		});
	}

	private void setupAltitudeTab(View view, LineChart chart, int position) {
		if (analysis != null) {
			if (analysis.hasElevationData) {
				GpxUiHelper.setupGPXChart(app, chart, CHART_LABEL_COUNT);
				chart.setData(new LineData(getDataSets(chart, GPX_TAB_ITEM_ALTITUDE, ALTITUDE, SLOPE)));
				updateChart(chart);
				chart.setVisibility(View.VISIBLE);
			} else {
				chart.setVisibility(View.GONE);
			}
			if (!onlyGraphs) {
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
							actionsListener.updateContent();
							for (int i = 0; i < getCount(); i++) {
								View view = getViewAtPosition(i);
								updateJoinGapsInfo(view, i);
							}
						}
					}
				});
			}
		} else {
			chart.setVisibility(View.GONE);
			view.findViewById(R.id.top_line_blocks).setVisibility(View.GONE);
			view.findViewById(R.id.list_divider).setVisibility(View.GONE);
			view.findViewById(R.id.bottom_line_blocks).setVisibility(View.GONE);
		}
		if (!onlyGraphs) {
			updateJoinGapsInfo(view, position);
			view.findViewById(R.id.analyze_on_map).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openAnalyzeOnMap(GPX_TAB_ITEM_ALTITUDE);
				}
			});
			TextView overflowMenu = view.findViewById(R.id.overflow_menu);
			if (!gpxItem.group.getTrack().generalTrack) {
				setupOptionsPopupMenu(overflowMenu, false);
			} else {
				overflowMenu.setVisibility(View.GONE);
			}
		}
	}

	private void setupGeneralTab(View view, LineChart chart, int position) {
		if (analysis != null) {
			if (analysis.hasElevationData || analysis.hasSpeedData) {
				GpxUiHelper.setupGPXChart(app, chart, CHART_LABEL_COUNT);
				chart.setData(new LineData(getDataSets(chart, GPXTabItemType.GPX_TAB_ITEM_GENERAL, ALTITUDE, SPEED)));
				updateChart(chart);
				chart.setVisibility(View.VISIBLE);
			} else {
				chart.setVisibility(View.GONE);
			}
			if (!onlyGraphs) {
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
					view.findViewById(R.id.bottom_line_blocks).setVisibility(View.GONE);
				}
			}
		} else {
			chart.setVisibility(View.GONE);
			view.findViewById(R.id.top_line_blocks).setVisibility(View.GONE);
			view.findViewById(R.id.list_divider).setVisibility(View.GONE);
			view.findViewById(R.id.bottom_line_blocks).setVisibility(View.GONE);
		}
		if (!onlyGraphs) {
			updateJoinGapsInfo(view, position);
			view.findViewById(R.id.analyze_on_map).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openAnalyzeOnMap(GPXTabItemType.GPX_TAB_ITEM_GENERAL);
				}
			});
			TextView overflowMenu = view.findViewById(R.id.overflow_menu);
			if (!gpxItem.group.getTrack().generalTrack) {
				setupOptionsPopupMenu(overflowMenu, true);
			} else {
				overflowMenu.setVisibility(View.GONE);
			}
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
						actionsListener.onPointSelected(segment, selectedWpt.lat, selectedWpt.lon);
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
					actionsListener.onPointSelected(segment, wpt.lat, wpt.lon);
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
							actionsListener.onPointSelected(segment, wpt.lat, wpt.lon);
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

	int singleTabLayoutId[] = {R.layout.center_button_container};
	int doubleTabsLayoutIds[] = {R.layout.left_button_container, R.layout.right_button_container};
	int tripleTabsLayoutIds[] = {R.layout.left_button_container, R.layout.center_button_container, R.layout.right_button_container};

	@Override
	public View getCustomTabView(@NonNull ViewGroup parent, int position) {
		int layoutId;
		int count = getCount();
		if (count == 1) {
			layoutId = singleTabLayoutId[position];
		} else if (count == 2) {
			layoutId = doubleTabsLayoutIds[position];
		} else {
			layoutId = tripleTabsLayoutIds[position];
		}
		ViewGroup tab = (ViewGroup) UiUtilities.getInflater(parent.getContext(), nightMode).inflate(layoutId, parent, false);
		tab.setTag(tabTypes[position].name());
		TextView title = (TextView) tab.getChildAt(0);
		if (title != null) {
			title.setText(getPageTitle(position));
		}
		return tab;
	}

	@Override
	public void select(View tab) {
		GPXTabItemType tabType = GPXTabItemType.valueOf((String) tab.getTag());
		int index = Arrays.asList(tabTypes).indexOf(tabType);
		View parent = (View) tab.getParent();
		UiUtilities.updateCustomRadioButtons(app, parent, nightMode, getCustomRadioButtonType(index));
	}

	@Override
	public void deselect(View tab) {

	}

	@Override
	public void tabStylesUpdated(View tabsContainer, int currentPosition) {
		if (getCount() > 0) {
			ViewGroup.MarginLayoutParams params = (MarginLayoutParams) tabsContainer.getLayoutParams();
			params.height = app.getResources().getDimensionPixelSize(!onlyGraphs ? R.dimen.dialog_button_height : R.dimen.context_menu_buttons_bottom_height);
			tabsContainer.setLayoutParams(params);
			UiUtilities.updateCustomRadioButtons(app, tabsContainer, nightMode, getCustomRadioButtonType(currentPosition));
		}
	}

	private CustomRadioButtonType getCustomRadioButtonType(int index) {
		int count = getCount();
		CustomRadioButtonType type = CustomRadioButtonType.CENTER;
		if (count == 2) {
			type = index > 0 ? CustomRadioButtonType.END : CustomRadioButtonType.START;
		} else if (count == 3) {
			if (index == 0) {
				type = CustomRadioButtonType.START;
			} else if (index == 2) {
				type = CustomRadioButtonType.END;
			}
		}
		return type;
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
			GPXTabItemType tabType = tabTypes[position];
			boolean generalTrack = gpxItem.isGeneralTrack();
			boolean joinSegments = displayHelper.isJoinSegments();
			boolean visible = generalTrack && analysis != null && tabType.equals(GPXTabItemType.GPX_TAB_ITEM_GENERAL);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.gpx_join_gaps_container), visible);
			((SwitchCompat) view.findViewById(R.id.gpx_join_gaps_switch)).setChecked(joinSegments);
			if (analysis != null) {
				if (tabType.equals(GPXTabItemType.GPX_TAB_ITEM_GENERAL)) {
					float totalDistance = !joinSegments && generalTrack ? analysis.totalDistanceWithoutGaps : analysis.totalDistance;
					float timeSpan = !joinSegments && generalTrack ? analysis.timeSpanWithoutGaps : analysis.timeSpan;

					((TextView) view.findViewById(R.id.distance_text)).setText(OsmAndFormatter.getFormattedDistance(totalDistance, app));
					((TextView) view.findViewById(R.id.duration_text)).setText(Algorithms.formatDuration((int) (timeSpan / 1000), app.accessibilityEnabled()));
				} else if (tabType.equals(GPX_TAB_ITEM_SPEED)) {
					long timeMoving = !joinSegments && generalTrack ? analysis.timeMovingWithoutGaps : analysis.timeMoving;
					float totalDistanceMoving = !joinSegments && generalTrack ? analysis.totalDistanceMovingWithoutGaps : analysis.totalDistanceMoving;

					((TextView) view.findViewById(R.id.time_moving_text)).setText(Algorithms.formatDuration((int) (timeMoving / 1000), app.accessibilityEnabled()));
					((TextView) view.findViewById(R.id.distance_text)).setText(OsmAndFormatter.getFormattedDistance(totalDistanceMoving, app));
				}
			}
		}
	}

	void updateChart(LineChart chart) {
		if (chart != null && !chart.isEmpty()) {
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
				}
			} else {
				chart.highlightValue(null);
			}
		}
	}

	public boolean isTabsVisible() {
		GPXFile gpxFile = displayHelper.getGpx();
		if (gpxFile != null && getCount() > 0 && views.size() > 0) {
			for (int i = 0; i < getCount(); i++) {
				LineChart lc = getViewAtPosition(i).findViewById(R.id.chart);
				if (!lc.isEmpty() && !gpxFile.isEmpty()) {
					return true;
				}
			}
		}
		return false;
	}

	public void updateGraph(int position) {
		updateAnalysis();
		fetchTabTypes();
		if (getCount() > 0 && views.size() > 0) {
			LineGraphType firstType = tabTypes[position] == GPX_TAB_ITEM_SPEED ? SPEED : ALTITUDE;
			LineGraphType secondType = null;
			if (tabTypes[position] == GPX_TAB_ITEM_ALTITUDE) {
				secondType = SLOPE;
			} else if (tabTypes[position] == GPX_TAB_ITEM_GENERAL) {
				secondType = SPEED;
			}
			LineChart chart = getViewAtPosition(position).findViewById(R.id.chart);
			List<ILineDataSet> dataSets = getDataSets(chart, tabTypes[position], firstType, secondType);
			boolean isEmptyDataSets = Algorithms.isEmpty(dataSets);
			AndroidUiHelper.updateVisibility(chart, !isEmptyDataSets);
			chart.clear();
			if (!isEmptyDataSets) {
				chart.setData(new LineData(dataSets));
			}
			if (chart.getAxisRight().getLabelCount() != CHART_LABEL_COUNT
					|| chart.getAxisLeft().getLabelCount() != CHART_LABEL_COUNT) {
				GpxUiHelper.setupGPXChart(app, chart, CHART_LABEL_COUNT);
			}
			updateChart(chart);
		}
		notifyDataSetChanged();
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
		List<ILineDataSet> dataSets = getDataSets(null, tabType, null, null);
		prepareGpxItemChartTypes(gpxItem, dataSets);
		actionsListener.openAnalyzeOnMap(gpxItem);
	}

	public static void prepareGpxItemChartTypes(GpxDisplayItem gpxItem, List<ILineDataSet> dataSets) {
		WptPt wpt = null;
		gpxItem.chartTypes = null;
		if (dataSets != null && dataSets.size() > 0) {
			gpxItem.chartTypes = new GPXDataSetType[dataSets.size()];
			for (int i = 0; i < dataSets.size(); i++) {
				OrderedLineDataSet orderedDataSet = (OrderedLineDataSet) dataSets.get(i);
				gpxItem.chartTypes[i] = orderedDataSet.getDataSetType();
			}
			if (gpxItem.chartHighlightPos != -1) {
				TrkSegment segment = getSegmentForAnalysis(gpxItem, gpxItem.analysis);
				if (segment != null) {
					OrderedLineDataSet dataSet = (OrderedLineDataSet) dataSets.get(0);
					float distance = gpxItem.chartHighlightPos * dataSet.getDivX();
					for (WptPt p : segment.points) {
						if (p.distance >= distance) {
							wpt = p;
							break;
						}
					}
				}
			}
		}
		if (wpt != null) {
			gpxItem.locationOnMap = wpt;
		} else {
			gpxItem.locationOnMap = gpxItem.locationStart;
		}
	}

	public static TrkSegment getSegmentForAnalysis(GpxDisplayItem gpxItem, GPXTrackAnalysis analysis) {
		for (Track track : gpxItem.group.getGpx().tracks) {
			for (TrkSegment segment : track.segments) {
				int size = segment.points.size();
				if (size > 0 && segment.points.get(0).equals(analysis.locationStart)
						&& segment.points.get(size - 1).equals(analysis.locationEnd)) {
					return segment;
				}
			}
		}
		return null;
	}
}
