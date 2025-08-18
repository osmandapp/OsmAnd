package net.osmand.plus.myplaces.tracks.dialogs;

import static net.osmand.plus.track.helpers.GpxDisplayGroup.getTrackDisplayGroup;
import static net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType.TRACK_SEGMENT;
import static net.osmand.shared.gpx.GpxParameter.SPLIT_INTERVAL;
import static net.osmand.shared.gpx.GpxParameter.SPLIT_TYPE;

import android.app.Dialog;
import android.os.Bundle;
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
import androidx.annotation.StringRes;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.SelectedGpxPoint;
import net.osmand.plus.myplaces.tracks.dialogs.SplitSegmentsAdapter.SplitAdapterListener;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.track.GpxSplitParams;
import net.osmand.plus.track.GpxSplitType;
import net.osmand.plus.track.SplitTrackAsyncTask.SplitTrackListener;
import net.osmand.plus.track.helpers.GpxDisplayGroup;
import net.osmand.plus.track.helpers.GpxDisplayHelper;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.track.helpers.TrackDisplayGroup;
import net.osmand.plus.track.helpers.TrackDisplayHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.MapLayers;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.shared.gpx.GpxDbHelper;
import net.osmand.shared.gpx.GpxDbHelper.GpxDataItemCallback;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.shared.io.KFile;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

import gnu.trove.list.array.TIntArrayList;

public class SplitSegmentDialogFragment extends BaseFullScreenDialogFragment implements SplitAdapterListener {

	public static final String TAG = "SPLIT_SEGMENT_DIALOG_FRAGMENT";

	private TrackDisplayHelper displayHelper;
	private GpxDbHelper gpxDbHelper;
	@Nullable
	private GpxDataItem gpxDataItem;
	private TrkSegment segment;
	private GpxDisplayItem displayItem;
	private SelectedGpxFile selectedGpxFile;

	private final List<String> options = new ArrayList<>();
	private final List<Double> distanceSplit = new ArrayList<>();
	private final TIntArrayList timeSplit = new TIntArrayList();
	private final GpxDisplayItemType[] filterTypes = {TRACK_SEGMENT};

	private long modifiedTime = -1;
	private List<GpxDisplayGroup> displayGroups;

	private View headerView;
	private ListView listView;
	private ProgressBar progressBar;
	private SplitSegmentsAdapter adapter;

	private int selectedSplitInterval;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		gpxDbHelper = app.getGpxDbHelper();
		GpxFile gpxFile = getGpx();
		if (gpxFile != null) {
			GpxSelectionHelper gpxSelectionHelper = app.getSelectedGpxHelper();
			selectedGpxFile = gpxSelectionHelper.getSelectedFileByPath(gpxFile.getPath());
			if (selectedGpxFile == null) {
				selectedGpxFile = new SelectedGpxFile();
				selectedGpxFile.setGpxFile(gpxFile, app);
			}

			GpxDataItemCallback callback = new GpxDataItemCallback() {
				@Override
				public boolean isCancelled() {
					return !isAdded();
				}

				@Override
				public void onGpxDataItemReady(@NonNull GpxDataItem item) {
					gpxDataItem = item;
				}
			};
			String filePath = selectedGpxFile.getGpxFile().getPath();
			gpxDataItem = gpxDbHelper.getItem(new KFile(filePath), callback);
			displayHelper.updateDisplayGroups();
		}
		if (shouldDismiss()) {
			dismiss();
		}
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflate(R.layout.split_segments_layout, container, false);

		Toolbar toolbar = view.findViewById(R.id.split_interval_toolbar);
		TextView title = toolbar.findViewById(R.id.title);
		title.setTextAppearance(nightMode ? R.style.TextAppearance_AppCompat_Widget_ActionBar_Title : R.style.Widget_Styled_LightActionBarTitle);

		toolbar.setNavigationIcon(getIcon(AndroidUtils.getNavigationIconResId(app)));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(v -> {
			dismiss();
		});

		progressBar = view.findViewById(R.id.progress_bar);

		listView = view.findViewById(R.id.list);
		listView.setBackgroundColor(ColorUtilities.getActivityBgColor(app, nightMode));
		listView.setDivider(null);
		listView.setDividerHeight(0);

		adapter = new SplitSegmentsAdapter(requireActivity(), new ArrayList<>(), displayItem, this);
		headerView = view.findViewById(R.id.header_layout);

		ImageView splitImage = headerView.findViewById(R.id.header_split_image);
		splitImage.setImageDrawable(getIcon(R.drawable.ic_action_split_interval, nightMode ? 0 : R.color.icon_color_default_light));

		listView.addHeaderView(inflate(R.layout.gpx_split_segments_empty_header, listView, false));
		listView.addFooterView(inflate(R.layout.list_shadow_footer, listView, false));

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

	private void updateHeader() {
		View splitIntervalView = headerView.findViewById(R.id.split_interval_view);

		if (getGpx() != null && !getGpx().isShowCurrentTrack() && adapter.getCount() > 0) {
			setupSplitIntervalView(splitIntervalView);
			if (options.isEmpty()) {
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
					List<GpxDisplayGroup> groups = getDisplayGroups();
					if (!groups.isEmpty()) {
						updateSplit(groups, selectedGpxFile);
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
		if (isAdded() && !shouldDismiss()) {
			adapter.clear();
			adapter.setNotifyOnChange(false);
			adapter.add(displayItem);
			adapter.addAll(getSplitSegments());
			adapter.notifyDataSetChanged();

			listView.setSelection(0);
			headerView.setTranslationY(0);
			updateHeader();
		}
	}

	private void updateSplit(@NonNull List<GpxDisplayGroup> groups, @NonNull SelectedGpxFile selectedGpxFile) {
		double splitInterval = 0;
		GpxSplitType splitType = GpxSplitType.NO_SPLIT;
		if (selectedSplitInterval == 1) {
			splitType = GpxSplitType.UPHILL_DOWNHILL;
			splitInterval = 1;
		} else if (distanceSplit.get(selectedSplitInterval) > 1) {
			splitType = GpxSplitType.DISTANCE;
			splitInterval = distanceSplit.get(selectedSplitInterval);
		} else if (timeSplit.get(selectedSplitInterval) > 1) {
			splitType = GpxSplitType.TIME;
			splitInterval = timeSplit.get(selectedSplitInterval);
		}
		saveNewSplit(splitType, splitInterval);

		SplitTrackListener listener = getSplitTrackListener(selectedGpxFile);
		GpxSplitParams params = new GpxSplitParams(splitType, splitInterval, false);

		app.getGpxDisplayHelper().splitTrackAsync(selectedGpxFile, groups, params, listener);
	}

	private void saveNewSplit(@NonNull GpxSplitType splitType, double splitInterval) {
		if (gpxDataItem != null) {
			gpxDataItem.setParameter(SPLIT_TYPE, splitType.getType());
			gpxDataItem.setParameter(SPLIT_INTERVAL, splitInterval);
			gpxDbHelper.updateDataItem(gpxDataItem);
		}
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
					selectedGpxFile.setSplitGroups(groups, app);
				}
				updateContent();
			}
		};
	}

	private void setupSplitIntervalView(@NonNull View view) {
		TextView title = view.findViewById(R.id.split_interval_title);
		TextView text = view.findViewById(R.id.split_interval_text);
		ImageView img = view.findViewById(R.id.split_interval_arrow);
		boolean nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.APP);
		int colorId;
		List<GpxDisplayGroup> groups = getDisplayGroups();
		if (!groups.isEmpty()) {
			colorId = ColorUtilities.getPrimaryTextColorId(nightMode);
		} else {
			colorId = ColorUtilities.getSecondaryTextColorId(nightMode);
		}
		int color = app.getColor(colorId);
		title.setTextColor(color);
		String titleText = getString(R.string.shared_string_split_by);
		title.setText(getString(R.string.ltr_or_rtl_combine_via_colon, titleText, ""));
		text.setTextColor(color);
		img.setImageDrawable(getIcon(R.drawable.ic_action_arrow_drop_down, colorId));
	}

	private void updateSplitIntervalView(View view) {
		TextView text = view.findViewById(R.id.split_interval_text);
		if (selectedSplitInterval == 0) {
			text.setText(getString(R.string.shared_string_none));
		} else if (selectedSplitInterval == 1) {
			text.setText(getString(R.string.uphill_downhill_split));
		} else {
			text.setText(options.get(selectedSplitInterval));
		}
	}

	@Nullable
	private GpxFile getGpx() {
		if (displayHelper != null) {
			return displayHelper.getGpx();
		} else {
			return null;
		}
	}

	private void addLabelOption(@StringRes int resId){
		options.add(app.getString(resId));
		distanceSplit.add(-1d);
		timeSplit.add(-1);
	}

	private void prepareSplitIntervalAdapterData() {
		List<GpxDisplayGroup> groups = getDisplayGroups();

		addLabelOption(R.string.shared_string_none);
		addLabelOption(R.string.uphill_downhill_split);

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

		GpxDisplayGroup group = groups.get(0);
		TrackDisplayGroup trackGroup = getTrackDisplayGroup(group);
		if (trackGroup != null && trackGroup.isSplitUphillDownhill()) {
			selectedSplitInterval = 1;
		}
	}

	@NonNull
	private List<GpxDisplayGroup> getDisplayGroups() {
		GpxFile gpxFile = selectedGpxFile.getGpxFile();
		if (gpxFile.getModifiedTime() != modifiedTime) {
			modifiedTime = gpxFile.getModifiedTime();
			GpxDisplayHelper displayHelper = app.getGpxDisplayHelper();
			List<GpxDisplayGroup> collectedGroup = displayHelper.collectDisplayGroups(selectedGpxFile, gpxFile, true, true);
			displayGroups = TrackDisplayHelper.filterGroups(collectedGroup, filterTypes);
		}
		return displayGroups;
	}

	private void addOptionSplit(int value, boolean distance, List<GpxDisplayGroup> model) {
		GpxDisplayGroup group = model.get(0);
		TrackDisplayGroup trackGroup = getTrackDisplayGroup(group);
		if (distance) {
			double dvalue = OsmAndFormatter.calculateRoundedDist(value, app);
			options.add(OsmAndFormatter.getFormattedDistance((float) dvalue, app));
			distanceSplit.add(dvalue);
			timeSplit.add(-1);
			if (trackGroup != null && Math.abs(trackGroup.getSplitDistance() - dvalue) < 1) {
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
			if (trackGroup != null && trackGroup.getSplitTime() == value) {
				selectedSplitInterval = distanceSplit.size() - 1;
			}
		}
	}

	@NonNull
	private List<GpxDisplayItem> getSplitSegments() {
		List<GpxDisplayItem> splitSegments = new ArrayList<>();
		List<GpxDisplayGroup> result;
		if (!Algorithms.isEmpty(selectedGpxFile.getSplitGroups(app))) {
			result = selectedGpxFile.getSplitGroups(app);
		} else {
			result = displayHelper.getGpxFile(true);
		}

		if (result != null && !result.isEmpty() && !segment.getPoints().isEmpty()) {
			for (GpxDisplayGroup group : result) {
				TrackDisplayGroup trackGroup = getTrackDisplayGroup(group);
				if (trackGroup != null) {
					splitSegments.addAll(collectDisplayItemsFromGroup(trackGroup));
				}
			}
		}
		return splitSegments;
	}

	private List<GpxDisplayItem> collectDisplayItemsFromGroup(@NonNull TrackDisplayGroup group) {
		List<GpxDisplayItem> splitSegments = new ArrayList<>();
		boolean generalTrack = displayItem.isGeneralTrack();
		boolean generalGroup = group.isGeneralTrack();
		if ((group.isSplitDistance() || group.isSplitTime()) && (!generalGroup && !generalTrack || generalGroup && generalTrack)) {
			boolean itemsForSelectedSegment = false;
			for (GpxDisplayItem item : group.getDisplayItems()) {
				itemsForSelectedSegment = segment.getPoints().get(0).equals(item.locationStart) || itemsForSelectedSegment;
				if (itemsForSelectedSegment) {
					splitSegments.add(item);
				}
				if (segment.getPoints().get(segment.getPoints().size() - 1).equals(item.locationEnd)) {
					break;
				}
			}
		}
		return splitSegments;
	}

	private boolean hasFilterType(GpxDisplayItemType filterType) {
		for (GpxDisplayItemType type : filterTypes) {
			if (type == filterType) {
				return true;
			}
		}
		return false;
	}

	private boolean shouldDismiss() {
		return displayHelper == null || selectedGpxFile == null || displayItem == null || segment == null;
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull TrackDisplayHelper helper,
	                                @NonNull GpxDisplayItem item, @NonNull TrkSegment segment) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			SplitSegmentDialogFragment fragment = new SplitSegmentDialogFragment();
			fragment.displayItem = item;
			fragment.segment = segment;
			fragment.displayHelper = helper;
			fragment.setRetainInstance(true);
			fragment.show(manager, TAG);
		}
	}

	@Override
	public void onOpenSegment(@NonNull GpxDisplayItem currentGpxDisplayItem) {
		SelectedGpxFile selectedGpxFile;
		selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(currentGpxDisplayItem.group.getGpxFile().getPath());

		if (selectedGpxFile != null) {
			dismiss();
			MapLayers mapLayers = app.getOsmandMap().getMapLayers();
			WptPt wptPt = currentGpxDisplayItem.getLabelPoint();

			SelectedGpxPoint gpxPoint = new SelectedGpxPoint(selectedGpxFile, wptPt);
			LatLon latLon = new LatLon(wptPt.getLatitude(), wptPt.getLongitude());
			PointDescription pointDescription = mapLayers.getGpxLayer().getObjectName(gpxPoint);
			mapLayers.getContextMenuLayer().showContextMenu(latLon, pointDescription, gpxPoint, null);
		}
	}
}
