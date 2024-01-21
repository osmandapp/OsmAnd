package net.osmand.plus.myplaces.tracks.dialogs;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.gpx.GPXUtilities;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXUtilities.TrkSegment;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.GpxSplitParams;
import net.osmand.plus.track.GpxSplitType;
import net.osmand.plus.track.SplitTrackAsyncTask.SplitTrackListener;
import net.osmand.plus.track.helpers.GpxDisplayGroup;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.track.helpers.TrackDisplayHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import gnu.trove.list.array.TIntArrayList;

public class SplitSegmentDialogFragment extends DialogFragment {

	public static final String TAG = "SPLIT_SEGMENT_DIALOG_FRAGMENT";

	private OsmandApplication app;
	private TrackDisplayHelper displayHelper;

	private SplitSegmentsAdapter adapter;
	private View headerView;

	private GpxDisplayItem gpxItem;
	private final GpxDisplayItemType[] filterTypes = {GpxDisplayItemType.TRACK_SEGMENT};
	private GPXUtilities.TrkSegment trkSegment;

	private final List<String> options = new ArrayList<>();
	private final List<Double> distanceSplit = new ArrayList<>();
	private final TIntArrayList timeSplit = new TIntArrayList();
	private int selectedSplitInterval;
	private UiUtilities ic;
	private int minMaxSpeedLayoutWidth;
	private Paint minMaxSpeedPaint;
	private Rect minMaxSpeedTextBounds;
	private ListView listView;
	private ProgressBar progressBar;
	private boolean joinSegments;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (displayHelper == null || gpxItem == null || trkSegment == null) {
			dismiss();
			return;
		}
		FragmentActivity activity = requireActivity();
		app = (OsmandApplication) activity.getApplication();
		ic = app.getUIUtilities();
		boolean isLightTheme = app.getSettings().isLightContent();
		int themeId = isLightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		boolean nightMode = !app.getSettings().isLightContent();
		listView.setBackgroundColor(ColorUtilities.getActivityBgColor(app, nightMode));
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		minMaxSpeedPaint = new Paint();
		minMaxSpeedPaint.setTextSize(getResources().getDimension(R.dimen.default_split_segments_data));
		minMaxSpeedPaint.setTypeface(FontCache.getFont(getContext(), "ui-fonts/Roboto-Medium.ttf"));
		minMaxSpeedPaint.setStyle(Paint.Style.FILL);
		minMaxSpeedTextBounds = new Rect();

		AppCompatActivity trackActivity = (AppCompatActivity) getActivity();
		View view = trackActivity.getLayoutInflater().inflate(R.layout.split_segments_layout, container, false);

		Toolbar toolbar = view.findViewById(R.id.split_interval_toolbar);
		TextView titleTextView = toolbar.findViewById(R.id.title);
		boolean nightMode = !app.getSettings().isLightContent();
		titleTextView.setTextAppearance(nightMode ?
				R.style.TextAppearance_AppCompat_Widget_ActionBar_Title :
				R.style.Widget_Styled_LightActionBarTitle);
		ActionBar trackActivityActionBar = trackActivity.getSupportActionBar();
		if (trackActivityActionBar != null) {
			titleTextView.setText(trackActivityActionBar.getTitle());
		}
		Drawable icBack = ic.getIcon(AndroidUtils.getNavigationIconResId(app));
		toolbar.setNavigationIcon(icBack);
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(v -> {
			dismiss();
		});

		progressBar = view.findViewById(R.id.progress_bar);

		listView = view.findViewById(R.id.list);
		listView.setDivider(null);
		listView.setDividerHeight(0);

		adapter = new SplitSegmentsAdapter(new ArrayList<GpxDisplayItem>());
		headerView = view.findViewById(R.id.header_layout);
		((ImageView) headerView.findViewById(R.id.header_split_image)).setImageDrawable(ic.getIcon(R.drawable.ic_action_split_interval, app.getSettings().isLightContent() ? R.color.icon_color_default_light : 0));

		listView.addHeaderView(trackActivity.getLayoutInflater().inflate(R.layout.gpx_split_segments_empty_header, null, false));
		listView.addFooterView(trackActivity.getLayoutInflater().inflate(R.layout.list_shadow_footer, null, false));

		listView.setOnScrollListener(new AbsListView.OnScrollListener() {
			int previousYPos = -1;

			@Override
			public void onScrollStateChanged(AbsListView absListView, int i) {
				if (i == SCROLL_STATE_IDLE) {
					previousYPos = -1;
				}
			}

			@Override
			public void onScroll(AbsListView absListView, int i, int i1, int i2) {
				View c = absListView.getChildAt(0);
				if (c != null) {
					int currentYPos = -c.getTop() + absListView.getFirstVisiblePosition() * c.getHeight();
					if (previousYPos == -1) {
						previousYPos = currentYPos;
					}

					float yTranslationToSet = headerView.getTranslationY() + (previousYPos - currentYPos);
					if (yTranslationToSet < 0 && yTranslationToSet > -headerView.getHeight()) {
						headerView.setTranslationY(yTranslationToSet);
					} else if (yTranslationToSet < -headerView.getHeight()) {
						headerView.setTranslationY(-headerView.getHeight());
					} else if (yTranslationToSet > 0) {
						headerView.setTranslationY(0);
					}

					previousYPos = currentYPos;
				}
			}
		});

		listView.setAdapter(adapter);

		return view;
	}

	@Override
	public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		updateContent();
	}

	@Override
	public void onDestroyView() {
		Dialog dialog = getDialog();
		if (dialog != null && getRetainInstance()) {
			dialog.setDismissMessage(null);
		}
		super.onDestroyView();
	}

	public void setGpxItem(GpxDisplayItem gpxItem) {
		this.gpxItem = gpxItem;
	}

	public void setTrkSegment(GPXUtilities.TrkSegment trkSegment) {
		this.trkSegment = trkSegment;
	}

	public void setJoinSegments(boolean joinSegments) {
		this.joinSegments = joinSegments;
	}

	private void updateHeader() {
		View splitIntervalView = headerView.findViewById(R.id.split_interval_view);

		if (getGpx() != null && !getGpx().showCurrentTrack && adapter.getCount() > 0) {
			setupSplitIntervalView(splitIntervalView);
			if (options.size() == 0) {
				prepareSplitIntervalAdapterData();
			}
			updateSplitIntervalView(splitIntervalView);
			splitIntervalView.setOnClickListener(v -> {
				ListPopupWindow popup = new ListPopupWindow(v.getContext());
				popup.setAnchorView(splitIntervalView);
				popup.setContentWidth(AndroidUtils.dpToPx(app, 200f));
				popup.setModal(true);
				popup.setDropDownGravity(Gravity.END | Gravity.TOP);
				popup.setVerticalOffset(AndroidUtils.dpToPx(app, -48f));
				popup.setHorizontalOffset(AndroidUtils.dpToPx(app, -6f));
				popup.setAdapter(new ArrayAdapter<>(v.getContext(),
						R.layout.popup_list_text_item, options));
				popup.setOnItemClickListener((parent, view, position, id) -> {
					selectedSplitInterval = position;
					GpxSelectionParams params = GpxSelectionParams.getDefaultSelectionParams();
					SelectedGpxFile sf = app.getSelectedGpxHelper().selectGpxFile(getGpx(), params);
					List<GpxDisplayGroup> groups = getDisplayGroups();
					if (groups.size() > 0) {
						updateSplit(groups, sf);
					}
					popup.dismiss();
					updateSplitIntervalView(splitIntervalView);
				});
				popup.show();
			});
			splitIntervalView.setVisibility(View.VISIBLE);
		} else {
			splitIntervalView.setVisibility(View.GONE);
		}
	}

	public void updateContent() {
		if (getActivity() != null) {
			adapter.clear();
			adapter.setNotifyOnChange(false);
			adapter.add(gpxItem);
			List<GpxDisplayItem> splitSegments = getSplitSegments();
			adapter.addAll(splitSegments);
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			headerView.setTranslationY(0);
			updateHeader();
		}
	}

	private void updateSplit(@NonNull List<GpxDisplayGroup> groups, @NonNull SelectedGpxFile selectedGpxFile) {
		double splitInterval = 0;
		GpxSplitType splitType = GpxSplitType.NO_SPLIT;
		if (distanceSplit.get(selectedSplitInterval) > 0) {
			splitType = GpxSplitType.DISTANCE;
			splitInterval = distanceSplit.get(selectedSplitInterval);
		} else if (timeSplit.get(selectedSplitInterval) > 0) {
			splitType = GpxSplitType.TIME;
			splitInterval = timeSplit.get(selectedSplitInterval);
		}
		SplitTrackListener listener = getSplitTrackListener(selectedGpxFile);
		GpxSplitParams params = new GpxSplitParams(splitType, splitInterval, joinSegments);

		app.getGpxDisplayHelper().splitTrackAsync(selectedGpxFile, groups, params, listener);
	}

	@NonNull
	private SplitTrackListener getSplitTrackListener(@NonNull SelectedGpxFile selectedGpxFile) {
		return new SplitTrackListener() {
			@Override
			public void trackSplittingStarted() {
				AndroidUiHelper.updateVisibility(progressBar, true);
			}

			@Override
			public void trackSplittingFinished(boolean success) {
				AndroidUiHelper.updateVisibility(progressBar, false);
				if (success) {
					List<GpxDisplayGroup> groups = getDisplayGroups();
					selectedGpxFile.setDisplayGroups(groups, app);
				}
				updateContent();
			}
		};
	}

	private void setupSplitIntervalView(@NonNull View view) {
		TextView title = view.findViewById(R.id.split_interval_title);
		TextView text = view.findViewById(R.id.split_interval_text);
		ImageView img = view.findViewById(R.id.split_interval_arrow);
		boolean nightMode = !app.getSettings().isLightContent();
		int colorId;
		List<GpxDisplayGroup> groups = getDisplayGroups();
		if (groups.size() > 0) {
			colorId = ColorUtilities.getPrimaryTextColorId(nightMode);
		} else {
			colorId = ColorUtilities.getSecondaryTextColorId(nightMode);
		}
		int color = app.getColor(colorId);
		title.setTextColor(color);
		String titleText = getString(R.string.gpx_split_interval);
		title.setText(getString(R.string.ltr_or_rtl_combine_via_colon, titleText, ""));
		text.setTextColor(color);
		img.setImageDrawable(ic.getIcon(R.drawable.ic_action_arrow_drop_down, colorId));
	}

	private void updateSplitIntervalView(View view) {
		TextView text = view.findViewById(R.id.split_interval_text);
		if (selectedSplitInterval == 0) {
			text.setText(getString(R.string.shared_string_none));
		} else {
			text.setText(options.get(selectedSplitInterval));
		}
	}

	@Nullable
	private GPXFile getGpx() {
		return displayHelper.getGpx();
	}

	private void prepareSplitIntervalAdapterData() {
		List<GpxDisplayGroup> groups = getDisplayGroups();

		options.add(app.getString(R.string.shared_string_none));
		distanceSplit.add(-1d);
		timeSplit.add(-1);
		addOptionSplit(30, true, groups); // 50 feet, 20 yards, 20
		// m
		addOptionSplit(60, true, groups); // 100 feet, 50 yards,
		// 50 m
		addOptionSplit(150, true, groups); // 200 feet, 100 yards,
		// 100 m
		addOptionSplit(300, true, groups); // 500 feet, 200 yards,
		// 200 m
		addOptionSplit(600, true, groups); // 1000 feet, 500 yards,
		// 500 m
		addOptionSplit(1500, true, groups); // 2000 feet, 1000 yards, 1 km
		addOptionSplit(3000, true, groups); // 1 mi, 2 km
		addOptionSplit(6000, true, groups); // 2 mi, 5 km
		addOptionSplit(15000, true, groups); // 5 mi, 10 km

		addOptionSplit(15, false, groups);
		addOptionSplit(30, false, groups);
		addOptionSplit(60, false, groups);
		addOptionSplit(120, false, groups);
		addOptionSplit(150, false, groups);
		addOptionSplit(300, false, groups);
		addOptionSplit(600, false, groups);
		addOptionSplit(900, false, groups);
		addOptionSplit(1800, false, groups);
		addOptionSplit(3600, false, groups);
	}

	@NonNull
	private List<GpxDisplayGroup> getDisplayGroups() {
		return displayHelper.getDisplayGroups(filterTypes);
	}

	private void addOptionSplit(int value, boolean distance, List<GpxDisplayGroup> model) {
		if (distance) {
			double dvalue = OsmAndFormatter.calculateRoundedDist(value, app);
			options.add(OsmAndFormatter.getFormattedDistance((float) dvalue, app));
			distanceSplit.add(dvalue);
			timeSplit.add(-1);
			if (Math.abs(model.get(0).getSplitDistance() - dvalue) < 1) {
				selectedSplitInterval = distanceSplit.size() - 1;
			}
		} else {
			if (value < 60) {
				options.add(value + " " + app.getString(R.string.int_seconds));
			} else if (value % 60 == 0) {
				options.add((value / 60) + " " + app.getString(R.string.int_min));
			} else {
				options.add((value / 60f) + " " + app.getString(R.string.int_min));
			}
			distanceSplit.add(-1d);
			timeSplit.add(value);
			if (model.get(0).getSplitTime() == value) {
				selectedSplitInterval = distanceSplit.size() - 1;
			}
		}
	}

	@NonNull
	private List<GpxDisplayItem> getSplitSegments() {
		List<GpxDisplayItem> splitSegments = new ArrayList<>();
		List<GpxDisplayGroup> result = displayHelper.getGpxFile(true);
		if (result != null && result.size() > 0 && trkSegment.points.size() > 0) {
			for (GpxDisplayGroup group : result) {
				splitSegments.addAll(collectDisplayItemsFromGroup(group));
			}
		}
		return splitSegments;
	}

	private List<GpxDisplayItem> collectDisplayItemsFromGroup(GpxDisplayGroup group) {
		List<GpxDisplayItem> splitSegments = new ArrayList<>();
		boolean generalTrack = gpxItem.isGeneralTrack();
		boolean generalGroup = group.isGeneralTrack();
		if ((group.isSplitDistance() || group.isSplitTime()) && (!generalGroup && !generalTrack || generalGroup && generalTrack)) {
			boolean itemsForSelectedSegment = false;
			for (GpxDisplayItem item : group.getDisplayItems()) {
				itemsForSelectedSegment = trkSegment.points.get(0).equals(item.locationStart) || itemsForSelectedSegment;
				if (itemsForSelectedSegment) {
					splitSegments.add(item);
				}
				if (trkSegment.points.get(trkSegment.points.size() - 1).equals(item.locationEnd)) {
					break;
				}
			}
		}
		return splitSegments;
	}

	protected boolean hasFilterType(GpxDisplayItemType filterType) {
		for (GpxDisplayItemType type : filterTypes) {
			if (type == filterType) {
				return true;
			}
		}
		return false;
	}

	private class SplitSegmentsAdapter extends ArrayAdapter<GpxDisplayItem> {

		SplitSegmentsAdapter(List<GpxDisplayItem> items) {
			super(requireActivity(), 0, items);
		}

		ColorStateList defaultTextColor;

		@NonNull
		@Override
		public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
			GpxDisplayItem currentGpxDisplayItem = getItem(position);
			FragmentActivity trackActivity = requireActivity();
			if (convertView == null) {
				convertView = trackActivity.getLayoutInflater().inflate(R.layout.gpx_split_segment_fragment, parent, false);
			}
			convertView.setOnClickListener(null);
			boolean nightMode = !app.getSettings().isLightContent();
			int activeColorId = ColorUtilities.getActiveColorId(nightMode);
			TextView overviewTextView = convertView.findViewById(R.id.overview_text);
			ImageView overviewImageView = convertView.findViewById(R.id.overview_image);
			if (position == 0) {
				overviewImageView.setImageDrawable(ic.getIcon(R.drawable.ic_action_time_span_16, app.getSettings().isLightContent() ? R.color.gpx_split_segment_icon_color : 0));
				if (defaultTextColor == null) {
					defaultTextColor = overviewTextView.getTextColors();
				}
				overviewTextView.setTextColor(defaultTextColor);
				overviewTextView.setText(app.getString(R.string.shared_string_overview));
				if (currentGpxDisplayItem != null) {
					String overview = getString(R.string.shared_string_overview);
					String points = String.valueOf(currentGpxDisplayItem.analysis.points);
					overviewTextView.setText(getString(R.string.ltr_or_rtl_combine_with_brackets, overview, points));

					String timeSpan = getString(R.string.shared_string_time_span);
					String formattedDuration = Algorithms.formatDuration(currentGpxDisplayItem.analysis.getDurationInSeconds(), app.accessibilityEnabled());
					TextView tvDuration = convertView.findViewById(R.id.fragment_count_text);
					tvDuration.setText(getString(R.string.ltr_or_rtl_combine_via_colon, timeSpan, formattedDuration));
				}
			} else {
				if (currentGpxDisplayItem != null && currentGpxDisplayItem.analysis != null) {
					overviewTextView.setTextColor(app.getColor(activeColorId));
					if (currentGpxDisplayItem.group.isSplitDistance()) {
						overviewImageView.setImageDrawable(ic.getIcon(R.drawable.ic_action_track_16, activeColorId));
						overviewTextView.setText("");
						double metricStart = currentGpxDisplayItem.analysis.metricEnd - currentGpxDisplayItem.analysis.totalDistance;
						overviewTextView.append(OsmAndFormatter.getFormattedDistance((float) metricStart, app));
						overviewTextView.append(" - ");
						overviewTextView.append(OsmAndFormatter.getFormattedDistance((float) currentGpxDisplayItem.analysis.metricEnd, app));
						overviewTextView.append("  (" + currentGpxDisplayItem.analysis.points + ")");
					} else if (currentGpxDisplayItem.group.isSplitTime()) {
						overviewImageView.setImageDrawable(ic.getIcon(R.drawable.ic_action_time_span_16, activeColorId));
						overviewTextView.setText("");
						double metricStart = currentGpxDisplayItem.analysis.metricEnd - (currentGpxDisplayItem.analysis.timeSpan / 1000);
						overviewTextView.append(OsmAndFormatter.getFormattedDuration((int) metricStart, app));
						overviewTextView.append(" - ");
						overviewTextView.append(OsmAndFormatter.getFormattedDuration((int) currentGpxDisplayItem.analysis.metricEnd, app));
						overviewTextView.append("  (" + currentGpxDisplayItem.analysis.points + ")");
					}
					((TextView) convertView.findViewById(R.id.fragment_count_text)).setText(app.getString(R.string.of, position, adapter.getCount() - 1));
				}
			}

			((ImageView) convertView.findViewById(R.id.start_time_image))
					.setImageDrawable(ic.getIcon(R.drawable.ic_action_time_start_16, app.getSettings().isLightContent() ? R.color.gpx_split_segment_icon_color : 0));
			((ImageView) convertView.findViewById(R.id.end_time_image))
					.setImageDrawable(ic.getIcon(R.drawable.ic_action_time_end_16, app.getSettings().isLightContent() ? R.color.gpx_split_segment_icon_color : 0));
			((ImageView) convertView.findViewById(R.id.average_altitude_image))
					.setImageDrawable(ic.getIcon(R.drawable.ic_action_altitude_average_16, app.getSettings().isLightContent() ? R.color.gpx_split_segment_icon_color : 0));
			((ImageView) convertView.findViewById(R.id.altitude_range_image))
					.setImageDrawable(ic.getIcon(R.drawable.ic_action_altitude_range_16, app.getSettings().isLightContent() ? R.color.gpx_split_segment_icon_color : 0));
			((ImageView) convertView.findViewById(R.id.ascent_descent_image))
					.setImageDrawable(ic.getIcon(R.drawable.ic_action_altitude_descent_ascent_16, app.getSettings().isLightContent() ? R.color.gpx_split_segment_icon_color : 0));
			((ImageView) convertView.findViewById(R.id.moving_time_image))
					.setImageDrawable(ic.getIcon(R.drawable.ic_action_time_moving_16, app.getSettings().isLightContent() ? R.color.gpx_split_segment_icon_color : 0));
			((ImageView) convertView.findViewById(R.id.average_speed_image))
					.setImageDrawable(ic.getIcon(R.drawable.ic_action_speed_16, app.getSettings().isLightContent() ? R.color.gpx_split_segment_icon_color : 0));
			((ImageView) convertView.findViewById(R.id.max_speed_image))
					.setImageDrawable(ic.getIcon(R.drawable.ic_action_max_speed_16, app.getSettings().isLightContent() ? R.color.gpx_split_segment_icon_color : 0));

			if (currentGpxDisplayItem != null) {
				GPXTrackAnalysis analysis = currentGpxDisplayItem.analysis;
				if (analysis != null) {
					ImageView distanceOrTimeSpanImageView = convertView.findViewById(R.id.distance_or_timespan_image);
					TextView distanceOrTimeSpanValue = convertView.findViewById(R.id.distance_or_time_span_value);
					TextView distanceOrTimeSpanText = convertView.findViewById(R.id.distance_or_time_span_text);
					if (position == 0) {
						distanceOrTimeSpanImageView.setImageDrawable(ic.getIcon(R.drawable.ic_action_track_16, app.getSettings().isLightContent() ? R.color.gpx_split_segment_icon_color : 0));
						float totalDistance = !joinSegments && gpxItem.isGeneralTrack() ? analysis.totalDistanceWithoutGaps : analysis.totalDistance;
						distanceOrTimeSpanValue.setText(OsmAndFormatter.getFormattedDistance(totalDistance, app));
						distanceOrTimeSpanText.setText(app.getString(R.string.distance));
					} else {
						if (currentGpxDisplayItem.group.isSplitDistance()) {
							distanceOrTimeSpanImageView.setImageDrawable(ic.getIcon(R.drawable.ic_action_time_span_16, app.getSettings().isLightContent() ? R.color.gpx_split_segment_icon_color : 0));
							if (analysis.getDurationInMs() > 0) {
								distanceOrTimeSpanValue.setText(Algorithms.formatDuration(analysis.getDurationInSeconds(), app.accessibilityEnabled()));
							} else {
								distanceOrTimeSpanValue.setText("-");
							}
							distanceOrTimeSpanText.setText(app.getString(R.string.shared_string_time_span));
						} else if (currentGpxDisplayItem.group.isSplitTime()) {
							distanceOrTimeSpanImageView.setImageDrawable(ic.getIcon(R.drawable.ic_action_track_16, app.getSettings().isLightContent() ? R.color.gpx_split_segment_icon_color : 0));
							distanceOrTimeSpanValue.setText(OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));
							distanceOrTimeSpanText.setText(app.getString(R.string.distance));
						}
					}

					TextView startTimeValue = convertView.findViewById(R.id.start_time_value);
					TextView startDateValue = convertView.findViewById(R.id.start_date_value);
					TextView endTimeValue = convertView.findViewById(R.id.end_time_value);
					TextView endDateValue = convertView.findViewById(R.id.end_date_value);
					if (analysis.timeSpan > 0) {
						DateFormat tf = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
						DateFormat df = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM);

						Date start = new Date(analysis.startTime);
						startTimeValue.setText(tf.format(start));
						startDateValue.setText(df.format(start));

						Date end = new Date(analysis.endTime);
						endTimeValue.setText(tf.format(end));
						endDateValue.setText(df.format(end));
					} else {
						startTimeValue.setText("-");
						startDateValue.setText("-");
						endTimeValue.setText("-");
						endDateValue.setText("-");
					}

					View elevationDivider = convertView.findViewById(R.id.elevation_divider);
					View elevationSection = convertView.findViewById(R.id.elevation_layout);
					if (analysis.hasElevationData()) {
						elevationDivider.setVisibility(View.VISIBLE);
						elevationSection.setVisibility(View.VISIBLE);

						((TextView) convertView.findViewById(R.id.average_altitude_value))
								.setText(OsmAndFormatter.getFormattedAlt(analysis.avgElevation, app));

						String min = OsmAndFormatter.getFormattedAlt(analysis.minElevation, app);
						String max = OsmAndFormatter.getFormattedAlt(analysis.maxElevation, app);
						String min_max_elevation = min.substring(0, min.indexOf(" ")).concat("/").concat(max);
						if (min_max_elevation.length() > 9) {
							(convertView.findViewById(R.id.min_altitude_value))
									.setVisibility(View.VISIBLE);
							(convertView.findViewById(R.id.max_altitude_value))
									.setVisibility(View.VISIBLE);
							((TextView) convertView.findViewById(R.id.min_altitude_value))
									.setText(min);
							((TextView) convertView.findViewById(R.id.max_altitude_value))
									.setText(max);
							(convertView.findViewById(R.id.min_max_altitude_value))
									.setVisibility(View.GONE);
						} else {
							(convertView.findViewById(R.id.min_max_altitude_value))
									.setVisibility(View.VISIBLE);
							((TextView) convertView.findViewById(R.id.min_max_altitude_value))
									.setText(min_max_elevation);
							(convertView.findViewById(R.id.min_altitude_value))
									.setVisibility(View.GONE);
							(convertView.findViewById(R.id.max_altitude_value))
									.setVisibility(View.GONE);
						}

						TextView ascentValue = convertView.findViewById(R.id.ascent_value);
						TextView descentValue = convertView.findViewById(R.id.descent_value);
						TextView ascentDescentValue = convertView.findViewById(R.id.ascent_descent_value);

						String asc = OsmAndFormatter.getFormattedAlt(analysis.diffElevationUp, app);
						String desc = OsmAndFormatter.getFormattedAlt(analysis.diffElevationDown, app);
						String asc_desc = asc.substring(0, asc.indexOf(" ")).concat("/").concat(desc);
						if (asc_desc.length() > 9) {
							ascentValue.setVisibility(View.VISIBLE);
							descentValue.setVisibility(View.VISIBLE);
							ascentValue.setText(asc);
							descentValue.setText(desc);
							ascentDescentValue.setVisibility(View.GONE);
						} else {
							ascentDescentValue.setVisibility(View.VISIBLE);
							ascentDescentValue.setText(asc_desc);
							ascentValue.setVisibility(View.GONE);
							descentValue.setVisibility(View.GONE);
						}

					} else {
						elevationDivider.setVisibility(View.GONE);
						elevationSection.setVisibility(View.GONE);
					}

					View speedDivider = convertView.findViewById(R.id.speed_divider);
					View speedSection = convertView.findViewById(R.id.speed_layout);
					if (analysis.hasSpeedData()) {
						speedDivider.setVisibility(View.VISIBLE);
						speedSection.setVisibility(View.VISIBLE);

						((TextView) convertView.findViewById(R.id.moving_time_value))
								.setText(Algorithms.formatDuration((int) (analysis.timeMoving / 1000), app.accessibilityEnabled()));
						((TextView) convertView.findViewById(R.id.average_speed_value))
								.setText(OsmAndFormatter.getFormattedSpeed(analysis.avgSpeed, app));

						String maxSpeed = OsmAndFormatter.getFormattedSpeed(analysis.maxSpeed, app);
						String minSpeed = OsmAndFormatter.getFormattedSpeed(analysis.minSpeed, app);
						String maxMinSpeed;
						if (maxSpeed.contains(" ")) {
							maxMinSpeed = maxSpeed.substring(0, maxSpeed.indexOf(" ")).concat("/").concat(minSpeed);
						} else {
							maxMinSpeed = maxSpeed.substring(0, maxSpeed.indexOf("-")).concat("/").concat(minSpeed);
						}

						if (minMaxSpeedLayoutWidth == 0) {
							DisplayMetrics metrics = new DisplayMetrics();
							trackActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
							int screenWidth = metrics.widthPixels;
							int widthWithoutSidePadding = screenWidth - AndroidUtils.dpToPx(trackActivity, 32);
							int singleLayoutWidth = widthWithoutSidePadding / 3;
							int twoLayouts = 2 * (singleLayoutWidth + AndroidUtils.dpToPx(trackActivity, 3));
							minMaxSpeedLayoutWidth = widthWithoutSidePadding - twoLayouts - AndroidUtils.dpToPx(trackActivity, 28);
						}

						minMaxSpeedPaint.getTextBounds(maxMinSpeed, 0, maxMinSpeed.length(), minMaxSpeedTextBounds);
						int minMaxStringWidth = minMaxSpeedTextBounds.width();

						if (analysis.minSpeed == 0) {
							(convertView.findViewById(R.id.max_speed_value))
									.setVisibility(View.VISIBLE);
							(convertView.findViewById(R.id.min_speed_value))
									.setVisibility(View.GONE);
							((TextView) convertView.findViewById(R.id.max_speed_value))
									.setText(maxSpeed);
							(convertView.findViewById(R.id.max_min_speed_value))
									.setVisibility(View.GONE);
							((TextView) convertView.findViewById(R.id.max_min_speed_text))
									.setText(app.getString(R.string.shared_string_max));
						} else if (minMaxStringWidth > minMaxSpeedLayoutWidth) {
							(convertView.findViewById(R.id.max_speed_value))
									.setVisibility(View.VISIBLE);
							(convertView.findViewById(R.id.min_speed_value))
									.setVisibility(View.VISIBLE);
							((TextView) convertView.findViewById(R.id.max_speed_value))
									.setText(maxSpeed);
							((TextView) convertView.findViewById(R.id.min_speed_value))
									.setText(minSpeed);
							(convertView.findViewById(R.id.max_min_speed_value))
									.setVisibility(View.GONE);
							((TextView) convertView.findViewById(R.id.max_min_speed_text))
									.setText(app.getString(R.string.max_min));
						} else {
							(convertView.findViewById(R.id.max_min_speed_value))
									.setVisibility(View.VISIBLE);
							((TextView) convertView.findViewById(R.id.max_min_speed_value))
									.setText(maxMinSpeed);
							(convertView.findViewById(R.id.max_speed_value))
									.setVisibility(View.GONE);
							(convertView.findViewById(R.id.min_speed_value))
									.setVisibility(View.GONE);
							((TextView) convertView.findViewById(R.id.max_min_speed_text))
									.setText(app.getString(R.string.max_min));
						}
					} else {
						speedDivider.setVisibility(View.GONE);
						speedSection.setVisibility(View.GONE);
					}
				}
			}
			return convertView;
		}
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager, @NonNull TrackDisplayHelper displayHelper,
	                                   @NonNull GpxDisplayItem gpxItem, @NonNull TrkSegment trkSegment) {
		try {
			SplitSegmentDialogFragment fragment = new SplitSegmentDialogFragment();
			fragment.setGpxItem(gpxItem);
			fragment.setTrkSegment(trkSegment);
			fragment.setRetainInstance(true);
			fragment.displayHelper = displayHelper;
			fragment.show(fragmentManager, TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}
}
