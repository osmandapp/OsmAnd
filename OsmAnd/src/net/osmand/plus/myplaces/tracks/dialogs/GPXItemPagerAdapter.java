package net.osmand.plus.myplaces.tracks.dialogs;

import static net.osmand.gpx.GpxParameter.JOIN_SEGMENTS;
import static net.osmand.plus.charts.ChartUtils.CHART_LABEL_COUNT;
import static net.osmand.plus.charts.GPXDataSetType.ALTITUDE;
import static net.osmand.plus.charts.GPXDataSetType.SLOPE;
import static net.osmand.plus.charts.GPXDataSetType.SPEED;
import static net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu.ChartPointLayer.GPX;
import static net.osmand.plus.myplaces.tracks.GPXTabItemType.GPX_TAB_ITEM_ALTITUDE;
import static net.osmand.plus.myplaces.tracks.GPXTabItemType.GPX_TAB_ITEM_GENERAL;
import static net.osmand.plus.myplaces.tracks.GPXTabItemType.GPX_TAB_ITEM_NO_ALTITUDE;
import static net.osmand.plus.myplaces.tracks.GPXTabItemType.GPX_TAB_ITEM_SPEED;
import static net.osmand.plus.track.helpers.GpxDisplayGroup.getTrackDisplayGroup;

import android.content.Context;
import android.graphics.Matrix;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;

import com.github.mikephil.charting.charts.ElevationChart;
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
import net.osmand.gpx.GPXUtilities.Track;
import net.osmand.gpx.GPXUtilities.TrkSegment;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.charts.ChartUtils;
import net.osmand.plus.charts.GPXDataSetAxisType;
import net.osmand.plus.charts.GPXDataSetType;
import net.osmand.plus.charts.OrderedLineDataSet;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.tracks.GPXTabItemType;
import net.osmand.plus.track.helpers.GpxDataItem;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.track.helpers.GpxUtils;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.track.helpers.TrackDisplayGroup;
import net.osmand.plus.track.helpers.TrackDisplayHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.CustomRadioButtonType;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;
import net.osmand.plus.views.controls.PagerSlidingTabStrip.CustomTabProvider;
import net.osmand.plus.views.controls.WrapContentHeightViewPager;
import net.osmand.plus.views.controls.WrapContentHeightViewPager.ViewAtPositionInterface;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.util.Algorithms;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GPXItemPagerAdapter extends PagerAdapter implements CustomTabProvider, ViewAtPositionInterface {

	private static final int[] SINGLE_TAB_LAYOUT_ID = {R.layout.center_button_container};
	private static final int[] DOUBLE_TABS_LAYOUT_IDS = {R.layout.left_button_container, R.layout.right_button_container};
	private static final int[] TRIPLE_TABS_LAYOUT_IDS = {R.layout.left_button_container, R.layout.center_button_container, R.layout.right_button_container};

	private final OsmandApplication app;
	private final UiUtilities iconsCache;

	private final TrackDisplayHelper displayHelper;
	private final Map<GPXTabItemType, List<ILineDataSet>> dataSetsMap = new HashMap<>();

	private WptPt selectedWpt;
	private TrkSegment segment;
	private GpxDisplayItem gpxItem;
	private GPXTrackAnalysis analysis;
	private GPXTabItemType[] tabTypes;

	private final SparseArray<View> views = new SparseArray<>();
	private final SegmentActionsListener actionsListener;

	private final boolean nightMode;
	private boolean chartClicked;
	private final boolean showEmptyAltitudeTab;
	private boolean hideStatistics;
	private boolean hideJoinGapsBottomButtons;
	private int chartHMargin;

	public void setChartHMargin(int chartHMargin) {
		this.chartHMargin = chartHMargin;
	}

	private boolean isShowCurrentTrack() {
		return displayHelper.getGpx() != null && displayHelper.getGpx().showCurrentTrack;
	}

	public void setHideStatistics(boolean hideStatistics) {
		this.hideStatistics = hideStatistics;
	}

	public void setHideJoinGapsBottomButtons(boolean hideJoinGapsBottomButtons) {
		this.hideJoinGapsBottomButtons = hideJoinGapsBottomButtons;
	}

	public GPXItemPagerAdapter(@NonNull OsmandApplication app,
	                           @Nullable GpxDisplayItem gpxItem,
	                           @NonNull TrackDisplayHelper displayHelper,
	                           @NonNull SegmentActionsListener actionsListener,
	                           boolean nightMode,
	                           boolean showEmptyAltitudeTab) {
		this.app = app;
		this.gpxItem = gpxItem;
		this.displayHelper = displayHelper;
		this.nightMode = nightMode;
		this.showEmptyAltitudeTab = showEmptyAltitudeTab;
		this.actionsListener = actionsListener;
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
				gpxItem = GpxUiHelper.makeGpxDisplayItem(app, gpxFile, GPX, analysis);
			}
		} else if (getFilteredGpxFile() != null) {
			GPXFile gpxFile = getFilteredGpxFile();
			analysis = gpxFile.getAnalysis(System.currentTimeMillis());
			gpxItem = GpxUiHelper.makeGpxDisplayItem(app, gpxFile, GPX, analysis);
		} else if (gpxItem != null) {
			analysis = gpxItem.analysis;
		}
	}

	@Nullable
	private GPXFile getFilteredGpxFile() {
		String gpxPath = displayHelper.getGpx() != null ? displayHelper.getGpx().path : null;
		if (gpxPath != null) {
			SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(gpxPath);
			if (selectedGpxFile != null && selectedGpxFile.getFilteredSelectedGpxFile() != null) {
				return selectedGpxFile.getFilteredSelectedGpxFile().getGpxFile();
			}
		}
		return null;
	}

	private void fetchTabTypes() {
		List<GPXTabItemType> tabTypeList = new ArrayList<>();
		boolean showCurrentTrack = isShowCurrentTrack();
		if (showCurrentTrack) {
			if (analysis != null && (analysis.hasElevationData() || analysis.hasSpeedData())) {
				tabTypeList.add(GPXTabItemType.GPX_TAB_ITEM_GENERAL);
			}
		} else {
			tabTypeList.add(GPXTabItemType.GPX_TAB_ITEM_GENERAL);
		}
		if (analysis != null) {
			if (analysis.hasElevationData()) {
				tabTypeList.add(GPX_TAB_ITEM_ALTITUDE);
			} else if (showEmptyAltitudeTab && !showCurrentTrack) {
				tabTypeList.add(GPX_TAB_ITEM_NO_ALTITUDE);
			}
			if (analysis.isSpeedSpecified()) {
				tabTypeList.add(GPX_TAB_ITEM_SPEED);
			}
		}
		tabTypes = tabTypeList.toArray(new GPXTabItemType[0]);
	}

	private List<ILineDataSet> getDataSets(LineChart chart, GPXTabItemType tabType,
	                                       GPXDataSetType firstType, GPXDataSetType secondType) {
		List<ILineDataSet> dataSets = dataSetsMap.get(tabType);
		boolean withoutGaps = true;
		if (isShowCurrentTrack()) {
			GPXFile gpxFile = displayHelper.getGpx();
			withoutGaps = !app.getSavingTrackHelper().getCurrentTrack().isJoinSegments() && gpxFile != null
					&& (Algorithms.isEmpty(gpxFile.tracks) || gpxFile.tracks.get(0).generalTrack);
		} else if (gpxItem != null) {
			GpxDataItem item = displayHelper.getGpxDataItem();
			boolean joinSegments = item != null ? item.getParameter(JOIN_SEGMENTS) : false;
			withoutGaps = gpxItem.isGeneralTrack() && joinSegments;
		}
		if (chart != null && analysis != null) {
			dataSets = ChartUtils.getDataSets(chart, app, analysis, firstType, secondType, withoutGaps);
			if (!Algorithms.isEmpty(dataSets)) {
				dataSetsMap.remove(tabType);
			}
			dataSetsMap.put(tabType, dataSets);
		}
		return dataSets;
	}

	@Nullable
	private TrkSegment getTrackSegment(LineChart chart) {
		if (segment == null) {
			LineData lineData = chart.getLineData();
			List<ILineDataSet> ds = lineData != null ? lineData.getDataSets() : null;
			if (!Algorithms.isEmpty(ds)) {
				segment = getSegmentForAnalysis(gpxItem, analysis);
			}
		}
		return segment;
	}

	@Nullable
	private WptPt getPoint(LineChart chart, float pos) {
		LineData lineData = chart.getLineData();
		List<ILineDataSet> dataSets = lineData != null ? lineData.getDataSets() : null;
		TrkSegment segment = getTrackSegment(chart);
		if (!Algorithms.isEmpty(dataSets) && segment != null) {
			GPXFile gpxFile = gpxItem.group.getGpxFile();
			boolean joinSegments = displayHelper.isJoinSegments();
			if (gpxItem.chartAxisType == GPXDataSetAxisType.TIME) {
				float time = pos * 1000;
				return GpxUtils.getSegmentPointByTime(segment, gpxFile, time, false, joinSegments);
			} else {
				OrderedLineDataSet dataSet = (OrderedLineDataSet) dataSets.get(0);
				float distance = dataSet.getDivX() * pos;
				return GpxUtils.getSegmentPointByDistance(segment, gpxFile, distance, false, joinSegments);
			}
		}
		return null;
	}

	@Override
	public int getCount() {
		return tabTypes.length;
	}

	@Override
	public CharSequence getPageTitle(int position) {
		return tabTypes[position].toHumanString(app);
	}

	@Override
	public int getItemPosition(@NonNull Object object) {
		View view = (View) object;
		GPXTabItemType tabType = (GPXTabItemType) view.getTag();
		int index = Arrays.asList(tabTypes).indexOf(tabType);
		return index >= 0 ? index : POSITION_NONE;
	}

	@NonNull
	@Override
	public Object instantiateItem(@NonNull ViewGroup container, int position) {
		GPXTabItemType tabType = tabTypes[position];
		View view = getViewForTab(container, tabType);
		view.setTag(tabType);
		if (analysis != null && gpxItem != null) {
			ElevationChart chart = view.findViewById(R.id.chart);
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
				case GPX_TAB_ITEM_NO_ALTITUDE:
					setupNoAltitudeTab(view);
					break;
			}
		}
		container.addView(view, 0);
		views.put(position, view);
		return view;
	}

	private View getViewForTab(@NonNull ViewGroup container, @NonNull GPXTabItemType tabType) {
		LayoutInflater inflater = LayoutInflater.from(container.getContext());
		View view = inflater.inflate(tabType.getLayoutId(), container, false);
		if (hideJoinGapsBottomButtons) {
			AndroidUiHelper.setVisibility(View.GONE,
					view.findViewById(R.id.gpx_join_gaps_container),
					view.findViewById(R.id.details_divider),
					view.findViewById(R.id.details_view)
			);
		}
		if (hideStatistics) {
			AndroidUiHelper.setVisibility(View.GONE,
					view.findViewById(R.id.top_line_blocks),
					view.findViewById(R.id.list_divider),
					view.findViewById(R.id.bottom_line_blocks));
		}
		return view;
	}

	private void setupSpeedTab(View view, com.github.mikephil.charting.charts.ElevationChart chart, int position) {
		if (analysis != null && analysis.isSpeedSpecified()) {
			if (analysis.hasSpeedData()) {
				ChartUtils.setupElevationChart(chart);
				chart.setData(new LineData(getDataSets(chart, GPX_TAB_ITEM_SPEED, SPEED, null)));
				updateChart(chart);
				chart.setVisibility(View.VISIBLE);
			} else {
				chart.setVisibility(View.GONE);
			}
			if (!hideStatistics) {
				setupSpeedStatisticsIcons(view, iconsCache);
				setupJoinGapsInfo(view);
			}
		} else {
			chart.setVisibility(View.GONE);
			view.findViewById(R.id.top_line_blocks).setVisibility(View.GONE);
			view.findViewById(R.id.list_divider).setVisibility(View.GONE);
			view.findViewById(R.id.bottom_line_blocks).setVisibility(View.GONE);
		}
		if (!hideStatistics) {
			updateJoinGapsInfo(view, position);
			updateActionButtonsRow(view, GPX_TAB_ITEM_SPEED, false);
		}
	}

	public static void setupSpeedStatisticsIcons(@NonNull View container, @NonNull UiUtilities iconsCache) {
		((ImageView) container.findViewById(R.id.average_icon))
				.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_speed_16));
		((ImageView) container.findViewById(R.id.max_icon))
				.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_max_speed_16));
		((ImageView) container.findViewById(R.id.time_moving_icon))
				.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_time_moving_16));
		((ImageView) container.findViewById(R.id.distance_icon))
				.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_distance_16));
	}

	private void setupOptionsPopupMenu(TextView overflowMenu, boolean confirmDeletion) {
		overflowMenu.setVisibility(View.VISIBLE);
		overflowMenu.setOnClickListener(view ->
				actionsListener.showOptionsPopupMenu(view, getTrkSegment(), confirmDeletion, gpxItem));
	}

	private void setupNoAltitudeTab(@NonNull View view) {
		DialogButton buttonView = view.findViewById(R.id.button_action);
		buttonView.setOnClickListener(v -> actionsListener.openGetAltitudeBottomSheet(gpxItem));

		if (!hideStatistics) {
			updateActionButtonsRow(view, GPX_TAB_ITEM_GENERAL, true);
		}
	}

	private void setupAltitudeTab(View view, ElevationChart chart, int position) {
		if (analysis != null) {
			if (analysis.hasElevationData()) {
				ChartUtils.setupElevationChart(chart);
				chart.setData(new LineData(getDataSets(chart, GPX_TAB_ITEM_ALTITUDE, ALTITUDE, SLOPE)));
				updateChart(chart);
				chart.setVisibility(View.VISIBLE);
			} else {
				chart.setVisibility(View.GONE);
			}
			if (!hideStatistics) {
				setupAltitudeStatisticsIcons(view, iconsCache);
				setupJoinGapsInfo(view);
			}
		} else {
			chart.setVisibility(View.GONE);
			view.findViewById(R.id.top_line_blocks).setVisibility(View.GONE);
			view.findViewById(R.id.list_divider).setVisibility(View.GONE);
			view.findViewById(R.id.bottom_line_blocks).setVisibility(View.GONE);
		}
		if (!hideStatistics) {
			updateJoinGapsInfo(view, position);
			updateActionButtonsRow(view, GPX_TAB_ITEM_ALTITUDE, false);
		}
	}

	public static void setupAltitudeStatisticsIcons(@NonNull View container, @NonNull UiUtilities iconsCache) {
		((ImageView) container.findViewById(R.id.average_icon))
				.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_altitude_average_16));
		((ImageView) container.findViewById(R.id.range_icon))
				.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_altitude_average_16));
		((ImageView) container.findViewById(R.id.ascent_icon))
				.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_altitude_ascent_16));
		((ImageView) container.findViewById(R.id.descent_icon))
				.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_altitude_descent_16));
	}

	private void setupGeneralTab(View view, ElevationChart chart, int position) {
		if (analysis != null) {
			if (analysis.hasElevationData() || analysis.hasSpeedData()) {
				ChartUtils.setupElevationChart(chart);
				chart.setData(new LineData(getDataSets(chart, GPXTabItemType.GPX_TAB_ITEM_GENERAL, ALTITUDE, SPEED)));
				updateChart(chart);
				chart.setVisibility(View.VISIBLE);
			} else {
				chart.setVisibility(View.GONE);
			}
			if (!hideStatistics) {
				setupGeneralStatisticsIcons(view, iconsCache);
				setupJoinGapsInfo(view);

				if (analysis.getTimeSpan() > 0) {
					setupTimeSpanStatistics(view, analysis);
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
		if (!hideStatistics) {
			updateJoinGapsInfo(view, position);
			updateActionButtonsRow(view, GPX_TAB_ITEM_GENERAL, true);
		}
	}

	public static void setupGeneralStatisticsIcons(@NonNull View container, @NonNull UiUtilities iconsCache) {
		((ImageView) container.findViewById(R.id.distance_icon))
				.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_distance_16));
		((ImageView) container.findViewById(R.id.duration_icon))
				.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_time_span_16));
		((ImageView) container.findViewById(R.id.start_time_icon))
				.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_time_start_16));
		((ImageView) container.findViewById(R.id.end_time_icon))
				.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_time_end_16));
	}

	public static void setupTimeSpanStatistics(@NonNull View container, @NonNull GPXTrackAnalysis analysis) {
		DateFormat timeFormat = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
		DateFormat dateFormat = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM);

		Date start = new Date(analysis.getStartTime());
		((TextView) container.findViewById(R.id.start_time_text)).setText(timeFormat.format(start));
		((TextView) container.findViewById(R.id.start_date_text)).setText(dateFormat.format(start));
		Date end = new Date(analysis.getEndTime());
		((TextView) container.findViewById(R.id.end_time_text)).setText(timeFormat.format(end));
		((TextView) container.findViewById(R.id.end_date_text)).setText(dateFormat.format(end));
	}

	private void setupChart(View view, LineChart chart) {
		if (chart == null) {
			return;
		}
		ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) chart.getLayoutParams();
		AndroidUtils.setMargins(params, chartHMargin, params.topMargin, chartHMargin, params.bottomMargin);
		chart.setHighlightPerDragEnabled(chartClicked);
		chart.setOnClickListener(v -> {
			if (!chartClicked) {
				chartClicked = true;
				if (selectedWpt != null) {
					actionsListener.onPointSelected(segment, selectedWpt.lat, selectedWpt.lon);
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

	@Override
	public View getCustomTabView(@NonNull ViewGroup parent, int position) {
		int layoutId;
		int count = getCount();
		if (count == 1) {
			layoutId = SINGLE_TAB_LAYOUT_ID[position];
		} else if (count == 2) {
			layoutId = DOUBLE_TABS_LAYOUT_IDS[position];
		} else {
			layoutId = TRIPLE_TABS_LAYOUT_IDS[position];
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
			int dimenId = hideStatistics ? R.dimen.context_menu_buttons_bottom_height : R.dimen.dialog_button_height;
			params.height = app.getResources().getDimensionPixelSize(dimenId);
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

	private void updateChart(int position) {
		View view = getViewAtPosition(position);
		if (view != null) {
			updateChart(view.findViewById(R.id.chart));
		}
	}

	private void updateActionButtonsRow(@NonNull View view, @NonNull GPXTabItemType tabType, boolean confirmDeletion) {
		view.findViewById(R.id.analyze_on_map).setOnClickListener(v -> openAnalyzeOnMap(tabType));

		TextView overflowMenu = view.findViewById(R.id.overflow_menu);
		TrackDisplayGroup trackGroup = getTrackDisplayGroup(gpxItem.group);
		if (trackGroup == null || !trackGroup.isGeneralTrack()) {
			setupOptionsPopupMenu(overflowMenu, confirmDeletion);
		} else {
			overflowMenu.setVisibility(View.GONE);
		}
	}

	private void setupJoinGapsInfo(@NonNull View view) {
		view.findViewById(R.id.gpx_join_gaps_container).setOnClickListener(v -> {
			if (displayHelper.setJoinSegments(!displayHelper.isJoinSegments())) {
				actionsListener.updateContent();
				for (int i = 0; i < getCount(); i++) {
					View itemView = getViewAtPosition(i);
					updateJoinGapsInfo(itemView, i);
				}
			}
		});
	}

	private void updateJoinGapsInfo(@Nullable View view, int position) {
		if (view != null && gpxItem != null) {
			GPXTabItemType tabType = tabTypes[position];
			boolean generalTrack = gpxItem.isGeneralTrack();
			boolean joinSegments = displayHelper.isJoinSegments();

			boolean showJoinGapsSwitch = !hideJoinGapsBottomButtons && generalTrack
					&& analysis != null && tabType.equals(GPXTabItemType.GPX_TAB_ITEM_GENERAL);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.gpx_join_gaps_container), showJoinGapsSwitch);

			CompoundButton compoundButton = view.findViewById(R.id.gpx_join_gaps_switch);
			if (compoundButton != null) {
				compoundButton.setChecked(joinSegments);
			}
			if (analysis != null) {
				if (tabType == GPX_TAB_ITEM_GENERAL) {
					updateGeneralTabInfo(view, app, analysis, joinSegments, generalTrack);
				} else if (tabType == GPX_TAB_ITEM_ALTITUDE) {
					updateAltitudeTabInfo(view, app, analysis);
				} else if (tabType == GPX_TAB_ITEM_SPEED) {
					updateSpeedTabInfo(view, app, analysis, joinSegments, generalTrack);
				}
			}
		}
	}

	public static void updateGeneralTabInfo(@NonNull View container, @NonNull OsmandApplication app,
	                                        @NonNull GPXTrackAnalysis analysis,
	                                        boolean joinSegments, boolean generalTrack) {
		float totalDistance = !joinSegments && generalTrack ? analysis.totalDistanceWithoutGaps : analysis.getTotalDistance();
		float timeSpan = !joinSegments && generalTrack ? analysis.timeSpanWithoutGaps : analysis.getTimeSpan();

		TextView distanceText = container.findViewById(R.id.distance_text);
		TextView durationText = container.findViewById(R.id.duration_text);

		distanceText.setText(OsmAndFormatter.getFormattedDistance(totalDistance, app));
		durationText.setText(Algorithms.formatDuration((int) (timeSpan / 1000), app.accessibilityEnabled()));
	}

	public static void updateAltitudeTabInfo(@NonNull View container, @NonNull OsmandApplication app,
	                                         @NonNull GPXTrackAnalysis analysis) {
		String min = OsmAndFormatter.getFormattedAlt(analysis.getMinElevation(), app);
		String max = OsmAndFormatter.getFormattedAlt(analysis.getMaxElevation(), app);

		TextView averageAltitudeText = container.findViewById(R.id.average_text);
		TextView altitudeRangeText = container.findViewById(R.id.range_text);
		TextView ascentText = container.findViewById(R.id.ascent_text);
		TextView descentText = container.findViewById(R.id.descent_text);

		averageAltitudeText.setText(OsmAndFormatter.getFormattedAlt(analysis.getAvgElevation(), app));
		altitudeRangeText.setText(String.format("%s - %s", min, max));
		ascentText.setText(OsmAndFormatter.getFormattedAlt(analysis.getDiffElevationUp(), app));
		descentText.setText(OsmAndFormatter.getFormattedAlt(analysis.getDiffElevationDown(), app));
	}

	public static void updateSpeedTabInfo(@NonNull View container, @NonNull OsmandApplication app,
	                                      @NonNull GPXTrackAnalysis analysis,
	                                      boolean joinSegments, boolean generalTrack) {
		long timeMoving = !joinSegments && generalTrack ? analysis.timeMovingWithoutGaps : analysis.getTimeMoving();
		float totalDistanceMoving = !joinSegments && generalTrack ?
				analysis.totalDistanceMovingWithoutGaps : analysis.getTotalDistanceMoving();

		TextView averageSpeedText = container.findViewById(R.id.average_text);
		TextView maxSpeedText = container.findViewById(R.id.max_text);
		TextView timeMovingText = container.findViewById(R.id.time_moving_text);
		TextView distanceText = container.findViewById(R.id.distance_text);

		averageSpeedText.setText(OsmAndFormatter.getFormattedSpeed(analysis.getAvgSpeed(), app));
		maxSpeedText.setText(OsmAndFormatter.getFormattedSpeed(analysis.getMaxSpeed(), app));
		timeMovingText.setText(Algorithms.formatDuration((int) (timeMoving / 1000), app.accessibilityEnabled()));
		distanceText.setText(OsmAndFormatter.getFormattedDistance(totalDistanceMoving, app));
	}

	private void updateChart(LineChart chart) {
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
		notifyDataSetChanged();

		int count = getCount();
		if (count > 0 && views.size() == count) {
			updateGraphTab(position);
		}
	}

	private void updateGraphTab(int position) {
		GPXDataSetType firstType = tabTypes[position] == GPX_TAB_ITEM_SPEED ? SPEED : ALTITUDE;
		GPXDataSetType secondType = null;
		if (tabTypes[position] == GPX_TAB_ITEM_ALTITUDE) {
			secondType = SLOPE;
		} else if (tabTypes[position] == GPX_TAB_ITEM_GENERAL) {
			secondType = SPEED;
		}

		View container = getViewAtPosition(position);

		ElevationChart chart = container.findViewById(R.id.chart);
		List<ILineDataSet> dataSets = getDataSets(chart, tabTypes[position], firstType, secondType);
		boolean isEmptyDataSets = Algorithms.isEmpty(dataSets);
		AndroidUiHelper.updateVisibility(chart, !isEmptyDataSets);
		chart.clear();
		if (!isEmptyDataSets) {
			chart.setData(new LineData(dataSets));
		}
		if (chart.getAxisRight().getLabelCount() != CHART_LABEL_COUNT
				|| chart.getAxisLeft().getLabelCount() != CHART_LABEL_COUNT) {
			ChartUtils.setupElevationChart(chart);
		}
		updateChart(chart);

		updateJoinGapsInfo(container, position);
	}

	public int getTabIndex(@NonNull GPXTabItemType tab) {
		for (int i = 0; i < tabTypes.length; i++) {
			if (tabTypes[i] == tab) {
				return i;
			}
		}
		return -1;
	}

	private TrkSegment getTrkSegment() {
		for (Track track : gpxItem.group.getGpxFile().tracks) {
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

	@Nullable
	public static TrkSegment getSegmentForAnalysis(GpxDisplayItem gpxItem, GPXTrackAnalysis analysis) {
		for (Track track : gpxItem.group.getGpxFile().tracks) {
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

	public static View createGpxTabsView(ViewGroup root, boolean nightMode) {
		Context context = root.getContext();
		View tabsView = UiUtilities.getInflater(context, nightMode)
				.inflate(R.layout.gpx_list_item_tab_content, root, false);
		setupGpxTabsView(tabsView, nightMode);
		return tabsView;
	}

	public static void setupGpxTabsView(View tabsView, boolean nightMode) {
		Context context = tabsView.getContext();

		PagerSlidingTabStrip tabLayout = tabsView.findViewById(R.id.sliding_tabs);
		tabLayout.setTabBackground(AndroidUtils.resolveAttribute(context, R.attr.btn_bg_border_inactive));
		tabLayout.setDividerWidth(AndroidUtils.dpToPx(context, 1));
		tabLayout.setDividerColor(ColorUtilities.getStrokedButtonsOutlineColor(context, nightMode));
		tabLayout.setIndicatorHeight(0);
		tabLayout.setShouldExpand(true);

		WrapContentHeightViewPager pager = tabsView.findViewById(R.id.pager);
		pager.setSwipeable(false);
		pager.setOffscreenPageLimit(2);
	}
}