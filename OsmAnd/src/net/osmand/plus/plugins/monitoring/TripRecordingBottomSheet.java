package net.osmand.plus.plugins.monitoring;

import static net.osmand.plus.myplaces.tracks.GPXTabItemType.GPX_TAB_ITEM_ALTITUDE;
import static net.osmand.plus.myplaces.tracks.GPXTabItemType.GPX_TAB_ITEM_GENERAL;
import static net.osmand.plus.myplaces.tracks.GPXTabItemType.GPX_TAB_ITEM_SPEED;
import static net.osmand.plus.myplaces.tracks.dialogs.GPXItemPagerAdapter.createGpxTabsView;
import static net.osmand.plus.utils.AndroidUtils.setPadding;
import static net.osmand.plus.utils.ColorUtilities.getActiveTransparentColorId;
import static net.osmand.plus.utils.UiUtilities.CompoundButtonType.GLOBAL;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.PlatformUtil;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.data.LatLon;
import net.osmand.plus.charts.ChartModeBottomSheet;
import net.osmand.plus.charts.GPXDataSetType;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.SideMenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.charts.TrackChartPoints;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.tracks.GPXTabItemType;
import net.osmand.plus.myplaces.tracks.dialogs.GPXItemPagerAdapter;
import net.osmand.plus.myplaces.tracks.dialogs.SegmentActionsListener;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.GpxBlockStatisticsBuilder;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.fragments.TrackAppearanceFragment;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.track.helpers.TrackDisplayHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;
import net.osmand.plus.views.controls.WrapContentHeightViewPager;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TripRecordingBottomSheet extends SideMenuBottomSheetDialogFragment implements SegmentActionsListener {

	public static final String TAG = TripRecordingBottomSheet.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(TripRecordingBottomSheet.class);
	public static final String UPDATE_TRACK_ICON = "update_track_icon";
	public static final String UPDATE_DYNAMIC_ITEMS = "update_dynamic_items";
	public static final GPXTabItemType[] INIT_TAB_ITEMS =
			{GPX_TAB_ITEM_GENERAL, GPX_TAB_ITEM_ALTITUDE, GPX_TAB_ITEM_SPEED};

	private SavingTrackHelper helper;
	private OsmandMonitoringPlugin plugin;

	private View statusContainer;
	private AppCompatImageView trackAppearanceIcon;

	private TrackChartPoints trackChartPoints;
	private GPXItemPagerAdapter graphsAdapter;
	private int graphTabPosition;
	private ViewGroup segmentsTabs;

	private GpxBlockStatisticsBuilder blockStatisticsBuilder;
	private SelectedGpxFile selectedGpxFile;

	private TripRecordingUpdatesHandler handler;

	private GpxFile getGPXFile() {
		return selectedGpxFile.getGpxFile();
	}

	private boolean hasDataToSave() {
		return helper.hasDataToSave();
	}

	private boolean searchingGPS() {
		return app.getLocationProvider().getLastKnownLocation() == null;
	}

	private boolean isRecordingTrack() {
		return settings.SAVE_GLOBAL_TRACK_TO_GPX.get();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		helper = app.getSavingTrackHelper();
		plugin = PluginsHelper.getPlugin(OsmandMonitoringPlugin.class);
		selectedGpxFile = helper.getCurrentTrack();
		handler = new TripRecordingUpdatesHandler(app, this::updateStatus, () -> {
			graphsAdapter.updateGraph(graphTabPosition);
			boolean showSegmentsTab = false;
			if (graphsAdapter.isUseSingleMainTab()) {
				List<GPXDataSetType[]> availableYAxis = new ArrayList<>();
				availableYAxis.addAll(ChartModeBottomSheet.getAvailableDefaultYTypes(selectedGpxFile.getTrackAnalysis(app)));
				availableYAxis.addAll(ChartModeBottomSheet.getAvailableSensorYTypes(selectedGpxFile.getTrackAnalysis(app)));
				if (!Algorithms.isEmpty(availableYAxis)) {
					showSegmentsTab = !Algorithms.isEmpty(availableYAxis) || graphsAdapter.isTabsVisible();
				}
			} else {
				showSegmentsTab = graphsAdapter.isTabsVisible();
			}
			AndroidUiHelper.updateVisibility(segmentsTabs, showSegmentsTab);
		});
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		View itemView = inflate(R.layout.trip_recording_fragment);
		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(itemView)
				.create());

		statusContainer = itemView.findViewById(R.id.status_container);
		updateStatus();

		LinearLayout showTrackContainer = itemView.findViewById(R.id.show_track_on_map);
		trackAppearanceIcon = showTrackContainer.findViewById(R.id.additional_button_icon);
		createShowTrackItem(showTrackContainer, trackAppearanceIcon, ItemType.SHOW_TRACK.getTitleId(),
				this, nightMode, this::hide);

		segmentsTabs = itemView.findViewById(R.id.segments_container);
		createSegmentsTabs(segmentsTabs);

		RecyclerView statBlocks = itemView.findViewById(R.id.block_statistics);
		blockStatisticsBuilder = new GpxBlockStatisticsBuilder(app, selectedGpxFile, nightMode);
		blockStatisticsBuilder.setShowShortStat(true);
		blockStatisticsBuilder.setBlocksView(statBlocks, false);
		blockStatisticsBuilder.setBlocksClickable(false);
		blockStatisticsBuilder.setTabItem(GPX_TAB_ITEM_GENERAL);
		blockStatisticsBuilder.initStatBlocks(
				null, ColorUtilities.getActiveColor(app, nightMode), null);
	}

	@Nullable
	@Override
	public List<Integer> getScrollableViewIds() {
		return null;
	}

	@Nullable
	public List<Integer> getBottomContainersIds() {
		List<Integer> ids = new ArrayList<>();
		ids.add(R.id.quadruple_bottom_buttons);
		return ids;
	}

	@Override
	protected void setupBottomButtons(ViewGroup view) {
		int contentPadding = getDimensionPixelSize(R.dimen.content_padding);
		View buttonsContainer = inflate(R.layout.preference_button_with_icon_quadruple);
		buttonsContainer.setPadding(contentPadding, contentPadding, contentPadding, contentPadding);
		view.addView(buttonsContainer);

		setupCloseButton(buttonsContainer);
		setupResumePauseButton(buttonsContainer);
		setupFinishButton(buttonsContainer);
		setupOptionsButton(buttonsContainer);
	}

	private void setupCloseButton(View container) {
		CardView closeButton = container.findViewById(R.id.button_left);
		createItem(closeButton, ItemType.CLOSE);
		closeButton.setOnClickListener(v -> dismiss());
	}

	private void setupResumePauseButton(View container) {
		CardView resumePauseButton = container.findViewById(R.id.button_center_left);
		createItem(resumePauseButton, isRecordingTrack() ? ItemType.PAUSE : ItemType.RESUME);
		resumePauseButton.setOnClickListener(v -> {
			boolean isRecordingTrack = isRecordingTrack();
			if (isRecordingTrack) {
				blockStatisticsBuilder.stopUpdatingStatBlocks();
				handler.stopChartUpdates();
			} else {
				blockStatisticsBuilder.runUpdatingStatBlocksIfNeeded();
				handler.startChartUpdatesIfNotRunning();
			}
			if (plugin != null) {
				plugin.pauseOrResumeRecording();
			}
			updateStatus();
			createItem(resumePauseButton, !isRecordingTrack ? ItemType.PAUSE : ItemType.RESUME);
		});
	}

	private void setupFinishButton(View container) {
		CardView finishButton = container.findViewById(R.id.button_center_right);
		createItem(finishButton, ItemType.FINISH);
		finishButton.setOnClickListener(v -> {
			if (plugin != null && plugin.finishRecording()) {
				dismiss();
			}
		});
	}

	private void setupOptionsButton(View container) {
		CardView optionsButton = container.findViewById(R.id.button_right);
		createItem(optionsButton, ItemType.OPTIONS);
		optionsButton.setOnClickListener(v -> {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				TripRecordingOptionsBottomSheet.showInstance(fragmentManager, this);
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		blockStatisticsBuilder.runUpdatingStatBlocksIfNeeded();
		handler.startGpsUpdatesIfNotRunning();
		handler.startChartUpdatesIfNotRunning();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapLayers().getGpxLayer().setTrackChartPoints(trackChartPoints);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		blockStatisticsBuilder.stopUpdatingStatBlocks();
		handler.stopGpsUpdates();
		handler.stopChartUpdates();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapLayers().getGpxLayer().setTrackChartPoints(null);
		}
	}

	public void show(String... keys) {
		Dialog dialog = getDialog();
		if (dialog != null) {
			dialog.show();
		}
		for (String key : keys) {
			if (key.equals(UPDATE_TRACK_ICON)) {
				updateTrackIcon(app, trackAppearanceIcon);
			}
			if (key.equals(UPDATE_DYNAMIC_ITEMS)) {
				blockStatisticsBuilder.restartUpdatingStatBlocks();
				restartUpdatingGraph();
			}
		}
	}

	public void hide() {
		Dialog dialog = getDialog();
		if (dialog != null) {
			dialog.hide();
		}
	}

	private void restartUpdatingGraph() {
		handler.stopChartUpdates();
		handler.startChartUpdatesIfNotRunning();
	}

	private void createSegmentsTabs(ViewGroup viewGroup) {
		View segmentView = createGpxTabsView(viewGroup, nightMode);
		AndroidUiHelper.setVisibility(View.GONE, segmentView.findViewById(R.id.list_item_divider));
		WrapContentHeightViewPager pager = segmentView.findViewById(R.id.pager);
		PagerSlidingTabStrip tabLayout = segmentView.findViewById(R.id.sliding_tabs);
		tabLayout.setDividerWidth(AndroidUtils.dpToPx(app, 1));
		tabLayout.setDividerColor(ColorUtilities.getStrokedButtonsOutlineColor(app, nightMode));
		tabLayout.setOnTabReselectedListener(new PagerSlidingTabStrip.OnTabReselectedListener() {
			@Override
			public void onTabSelected(int position) {
				graphTabPosition = position;
				blockStatisticsBuilder.setTabItem(INIT_TAB_ITEMS[graphTabPosition]);
				blockStatisticsBuilder.restartUpdatingStatBlocks();
			}

			@Override
			public void onTabReselected(int position) {
				graphTabPosition = position;
				blockStatisticsBuilder.setTabItem(INIT_TAB_ITEMS[graphTabPosition]);
			}
		});

		TrackDisplayHelper displayHelper = new TrackDisplayHelper(app);
		GpxFile gpxFile = getGPXFile();
		File file = new File(gpxFile.getPath());
		displayHelper.setFile(file);
		displayHelper.setSelectedGpxFile(selectedGpxFile);
		displayHelper.setGpxDataItem(app.getGpxDbHelper().getItem(SharedUtil.kFile(file)));
		displayHelper.setGpx(gpxFile);

		graphsAdapter = new GPXItemPagerAdapter(app, null, displayHelper, this, nightMode, false, getMapActivity());
		graphsAdapter.setHideStatistics(true);
		graphsAdapter.setHideJoinGapsBottomButtons(true);
		graphsAdapter.setUseSingleMainTab(true);
		graphsAdapter.setAxisPreferences(settings.TRIP_RECORDING_X_AXIS, settings.TRIP_RECORDING_Y_AXIS);

		pager.setAdapter(graphsAdapter);
		tabLayout.setViewPager(pager);
		tabLayout.setVisibility(View.GONE);

		viewGroup.addView(segmentView);
	}

	private void updateStatus() {
		TextView statusTitle = statusContainer.findViewById(R.id.text_status);
		AppCompatImageView statusIcon = statusContainer.findViewById(R.id.icon_status);
		ItemType status = searchingGPS() ? ItemType.SEARCHING_GPS : !isRecordingTrack() ? ItemType.ON_PAUSE : ItemType.RECORDING;
		Integer titleId = status.getTitleId();
		if (titleId != null) {
			statusTitle.setText(titleId);
		}
		int colorText = status.equals(ItemType.SEARCHING_GPS) ? ColorUtilities.getSecondaryTextColorId(nightMode) : getOsmandIconColorId(nightMode);
		statusTitle.setTextColor(ContextCompat.getColor(app, colorText));
		Integer iconId = status.getIconId();
		if (iconId != null) {
			int colorDrawable = ContextCompat.getColor(app,
					status.equals(ItemType.SEARCHING_GPS) ? getSecondaryIconColorId(nightMode) : getOsmandIconColorId(nightMode));
			Drawable statusDrawable = UiUtilities.tintDrawable(AppCompatResources.getDrawable(app, iconId), colorDrawable);
			statusIcon.setImageDrawable(statusDrawable);
		}
	}

	public static void updateTrackIcon(OsmandApplication app, AppCompatImageView appearanceIcon) {
		if (appearanceIcon != null) {
			OsmandSettings settings = app.getSettings();
			String width = settings.CURRENT_TRACK_WIDTH.get();
			boolean showArrows = settings.CURRENT_TRACK_SHOW_ARROWS.get();
			int color = settings.CURRENT_TRACK_COLOR.get();
			Drawable appearanceDrawable = TrackAppearanceFragment.getTrackIcon(app, width, showArrows, color);
			int marginTrackIconH = app.getResources().getDimensionPixelSize(R.dimen.content_padding_small);
			UiUtilities.setMargins(appearanceIcon, marginTrackIconH, 0, marginTrackIconH, 0);
			appearanceIcon.setImageDrawable(appearanceDrawable);
		}
	}

	public static void createShowTrackItem(LinearLayout showTrackContainer, AppCompatImageView trackAppearanceIcon,
	                                       Integer showTrackId, Fragment target,
	                                       boolean nightMode, Runnable hideOnClickButtonAppearance) {
		FragmentActivity activity = target.getActivity();
		if (activity == null) {
			AndroidUiHelper.updateVisibility(showTrackContainer, false);
			return;
		}
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		GpxSelectionHelper gpxSelectionHelper = app.getSelectedGpxHelper();
		CardView buttonShowTrack = showTrackContainer.findViewById(R.id.compound_container);
		CardView buttonAppearance = showTrackContainer.findViewById(R.id.additional_button_container);

		TextView showTrackTextView = buttonShowTrack.findViewById(R.id.title);
		if (showTrackId != null) {
			showTrackTextView.setText(showTrackId);
		}

		SelectedGpxFile selectedGpxFile = app.getSavingTrackHelper().getCurrentTrack();
		boolean showCurrentTrack = gpxSelectionHelper.getSelectedCurrentRecordingTrack() != null;

		CompoundButton showTrackCompound = buttonShowTrack.findViewById(R.id.compound_button);
		showTrackCompound.setChecked(showCurrentTrack);
		UiUtilities.setupCompoundButton(showTrackCompound, nightMode, GLOBAL);

		setShowTrackItemBackground(buttonShowTrack, showTrackCompound.isChecked(), nightMode);
		buttonShowTrack.setOnClickListener(v -> {
			boolean checked = !showTrackCompound.isChecked();
			showTrackCompound.setChecked(checked);
			GpxSelectionParams params = GpxSelectionParams.newInstance().syncGroup().saveSelection();
			if (checked) {
				params.showOnMap().selectedByUser().addToMarkers().addToHistory();
			} else {
				params.hideFromMap();
			}
			gpxSelectionHelper.selectGpxFile(selectedGpxFile.getGpxFile(), params);
			setShowTrackItemBackground(buttonShowTrack, checked, nightMode);
			createItem(app, nightMode, buttonAppearance, ItemType.APPEARANCE, checked, null);
		});

		updateTrackIcon(app, trackAppearanceIcon);
		createItem(app, nightMode, buttonAppearance, ItemType.APPEARANCE, showTrackCompound.isChecked(), null);
		if (activity instanceof MapActivity) {
			buttonAppearance.setOnClickListener(v -> {
				if (showTrackCompound.isChecked()) {
					hideOnClickButtonAppearance.run();
					TrackAppearanceFragment.showInstance((MapActivity) activity, selectedGpxFile, target);
				}
			});
		}
	}

	public static void setShowTrackItemBackground(View view, boolean checked, boolean nightMode) {
		Drawable background = AppCompatResources.getDrawable(view.getContext(),
				checked ? getActiveTransparentBackgroundId(nightMode) : getInactiveStrokedBackgroundId(nightMode));
		view.setBackground(background);
	}

	private void createItem(View view, ItemType type) {
		createItem(app, nightMode, view, type, true, null);
	}

	public static View createItem(Context context, boolean nightMode, LayoutInflater inflater, ItemType type) {
		return createItem(context, nightMode, inflater, type, true, null);
	}

	public static View createItem(Context context, boolean nightMode, LayoutInflater inflater, ItemType type, boolean enabled, String description) {
		View button = inflater.inflate(R.layout.bottom_sheet_button_with_icon, null);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
		int horizontal = context.getResources().getDimensionPixelSize(R.dimen.content_padding);
		params.setMargins(horizontal, 0, horizontal, 0);
		button.setLayoutParams(params);
		createItem(context, nightMode, button, type, enabled, description);
		return button;
	}

	public static void createItem(Context context, boolean nightMode, View view, ItemType type, boolean enabled, @Nullable String description) {
		view.setTag(type);

		AppCompatImageView icon = view.findViewById(R.id.icon);
		if (icon != null) {
			setTintedIcon(context, icon, enabled, nightMode, type);
		}

		TextView title = view.findViewById(R.id.button_text);
		Integer titleId = type.getTitleId();
		if (title != null && titleId != null) {
			title.setText(titleId);
			setTextColor(context, title, enabled, nightMode, type);
		}

		TextViewEx desc = view.findViewById(R.id.desc);
		if (desc != null) {
			boolean isShowDesc = !Algorithms.isBlank(description);
			int marginDesc = isShowDesc ? 0 : context.getResources().getDimensionPixelSize(R.dimen.context_menu_padding_margin_medium);
			AndroidUiHelper.updateVisibility(desc, isShowDesc);
			if (title != null) {
				UiUtilities.setMargins(title, 0, marginDesc, 0, marginDesc);
			}
			desc.setText(description);
			setTextColor(context, desc, false, nightMode, type);
		}

		setItemBackground(context, nightMode, view, enabled);
	}

	public static void createItemActive(Context context, boolean nightMode, View view, ItemType type) {
		view.setTag(type);
		AppCompatImageView icon = view.findViewById(R.id.icon);
		if (icon != null) {
			setTintedIconActive(context, icon, nightMode, type);
		}
		TextView title = view.findViewById(R.id.button_text);
		Integer titleId = type.getTitleId();
		if (title != null && titleId != null) {
			title.setText(titleId);
			setTextColorActive(context, title, nightMode);
		}
		setItemBackgroundActive(context, nightMode, view);
	}

	public static void setItemBackground(Context context, boolean nightMode, View view, boolean enabled) {
		if (view instanceof CardView) {
			int colorId = enabled
					? getActiveTransparentColorId(nightMode)
					: ColorUtilities.getInactiveButtonsAndLinksColorId(nightMode);
			((CardView) view).setCardBackgroundColor(AndroidUtils.createPressedColorStateList(
					context, colorId, ColorUtilities.getActiveColorId(nightMode)
			));
			return;
		}
		Drawable background = AppCompatResources.getDrawable(context, getInactiveButtonBackgroundId(nightMode));
		if (background != null && enabled) {
			int inactiveColorId = ColorUtilities.getInactiveButtonsAndLinksColorId(nightMode);
			int activeColorId = ColorUtilities.getActiveColorId(nightMode);
			DrawableCompat.setTintList(background, AndroidUtils.createPressedColorStateList(
					context, inactiveColorId, activeColorId));
		} else {
			UiUtilities.tintDrawable(background, ColorUtilities.getInactiveButtonsAndLinksColor(context, nightMode));
		}
		view.setBackground(background);
	}

	public static void setItemBackgroundActive(Context context, boolean nightMode, View view) {
		if (view instanceof CardView) {
			((CardView) view).setCardBackgroundColor(ContextCompat.getColor(context, ColorUtilities.getActiveColorId(nightMode)));
		}
	}

	public enum ItemType {
		SHOW_TRACK(R.string.shared_string_show_on_map, null),
		APPEARANCE(null, null),
		SEARCHING_GPS(R.string.searching_gps, R.drawable.ic_action_gps_info),
		RECORDING(R.string.recording_default_name, R.drawable.ic_action_track_recordable),
		ON_PAUSE(R.string.on_pause, R.drawable.ic_pause),
		CLEAR_DATA(R.string.clear_recorded_data, R.drawable.ic_action_delete_dark),
		START_NEW_SEGMENT(R.string.gpx_start_new_segment, R.drawable.ic_action_new_segment),
		SAVE(R.string.trip_recording_save_and_continue, R.drawable.ic_action_save_to_file),
		PAUSE(R.string.shared_string_pause, R.drawable.ic_pause),
		RESUME(R.string.shared_string_resume, R.drawable.ic_play_dark),
		STOP(R.string.shared_string_control_stop, R.drawable.ic_action_rec_stop),
		STOP_AND_DISCARD(R.string.track_recording_stop_without_saving, R.drawable.ic_action_rec_stop),
		STOP_AND_SAVE(R.string.track_recording_save_and_stop, R.drawable.ic_action_save_to_file),
		START_ONLINE(R.string.live_monitoring_start, R.drawable.ic_world_globe_dark),
		STOP_ONLINE(R.string.live_monitoring_stop, R.drawable.ic_action_offline),
		CANCEL(R.string.shared_string_cancel, R.drawable.ic_action_close),
		CLOSE(R.string.shared_string_close, R.drawable.ic_action_close),
		START_RECORDING(R.string.shared_string_control_start, R.drawable.ic_action_direction_movement),
		SETTINGS(R.string.shared_string_settings, R.drawable.ic_action_settings),
		FINISH(R.string.shared_string_finish, R.drawable.ic_action_point_destination),
		OPTIONS(R.string.shared_string_options, R.drawable.ic_overflow_menu_with_background);

		@StringRes
		private final Integer titleId;
		@DrawableRes
		private final Integer iconId;
		private static final List<ItemType> negative = Arrays.asList(CLEAR_DATA, STOP, STOP_AND_DISCARD);

		ItemType(@Nullable @StringRes Integer titleId, @Nullable @DrawableRes Integer iconId) {
			this.titleId = titleId;
			this.iconId = iconId;
		}

		@Nullable
		public Integer getTitleId() {
			return titleId;
		}

		@Nullable
		public Integer getIconId() {
			return iconId;
		}

		public boolean isNegative() {
			return negative.contains(this);
		}
	}

	protected static void setTextColor(Context context, TextView tv, boolean enabled, boolean nightMode, ItemType type) {
		if (tv != null) {
			int activeColorId = type.isNegative() ? R.color.color_osm_edit_delete : ColorUtilities.getActiveColorId(nightMode);
			int normalColorId = enabled ? activeColorId : ColorUtilities.getSecondaryTextColorId(nightMode);
			ColorStateList textColorStateList = AndroidUtils.createPressedColorStateList(context, normalColorId, getPressedColorId(nightMode));
			tv.setTextColor(textColorStateList);
		}
	}

	protected static void setTextColorActive(Context context, TextView tv, boolean nightMode) {
		if (tv != null) {
			tv.setTextColor(ContextCompat.getColor(context, getPressedColorId(nightMode)));
		}
	}

	protected static void setTintedIcon(Context context, AppCompatImageView iv, boolean enabled, boolean nightMode, ItemType type) {
		Integer iconId = type.getIconId();
		if (iv != null && iconId != null) {
			Drawable icon = AppCompatResources.getDrawable(context, iconId);
			int activeColorId = type.isNegative() ? R.color.color_osm_edit_delete : getActiveIconColorId(nightMode);
			int normalColorId = enabled ? activeColorId : getSecondaryIconColorId(nightMode);
			ColorStateList iconColorStateList = AndroidUtils.createPressedColorStateList(context, normalColorId, getPressedColorId(nightMode));
			if (icon != null) {
				DrawableCompat.setTintList(icon, iconColorStateList);
			}
			iv.setImageDrawable(icon);
			if (type.iconId == R.drawable.ic_action_rec_stop) {
				int stopSize = iv.getResources().getDimensionPixelSize(R.dimen.bottom_sheet_icon_margin_large);
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(stopSize, stopSize);
				iv.setLayoutParams(params);
				View container = (View) iv.getParent();
				setPadding(container, container.getPaddingLeft(), container.getTop(),
						context.getResources().getDimensionPixelSize(R.dimen.content_padding_half), container.getBottom());
			}
		}
	}

	protected static void setTintedIconActive(Context context, AppCompatImageView iv, boolean nightMode, ItemType type) {
		Integer iconId = type.getIconId();
		if (iv != null && iconId != null) {
			Drawable icon = AppCompatResources.getDrawable(context, iconId);
			if (icon != null) {
				DrawableCompat.setTint(icon, ContextCompat.getColor(context, getPressedColorId(nightMode)));
			}
			iv.setImageDrawable(icon);
		}
	}

	@Override
	public void onPointSelected(TrkSegment segment, double lat, double lon) {
		if (trackChartPoints == null) {
			trackChartPoints = new TrackChartPoints();
			trackChartPoints.setGpx(getGPXFile());
		}
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			int segmentColor = segment != null ? segment.getColor(0) : 0;
			trackChartPoints.setSegmentColor(segmentColor);
			trackChartPoints.setHighlightedPoint(new LatLon(lat, lon));
			mapActivity.getMapLayers().getGpxLayer().setTrackChartPoints(trackChartPoints);
			mapActivity.refreshMap();
		}
	}

	@Override
	public void updateContent() {
	}

	@Override
	public void onChartTouch() {
	}

	@Override
	public void scrollBy(int px) {
	}

	@Override
	public void openSplitInterval(GpxDisplayItem gpxItem, TrkSegment trkSegment) {
	}

	@Override
	public void showOptionsPopupMenu(View view, TrkSegment segment, boolean confirmDeletion, GpxDisplayItem gpxItem) {
	}

	@Override
	public void openAnalyzeOnMap(@NonNull GpxDisplayItem gpxItem) {
	}

	@Override
	public void openGetAltitudeBottomSheet(@NonNull GpxDisplayItem gpxItem) {

	}

	public interface DismissTargetFragment {
		void dismissTarget();
	}

	@ColorRes
	public static int getActiveIconColorId(boolean nightMode) {
		return nightMode ? R.color.icon_color_active_dark : R.color.icon_color_active_light;
	}

	@ColorRes
	public static int getSecondaryIconColorId(boolean nightMode) {
		return nightMode ? R.color.icon_color_secondary_dark : R.color.icon_color_secondary_light;
	}

	@ColorRes
	public static int getOsmandIconColorId(boolean nightMode) {
		return nightMode ? R.color.icon_color_osmand_dark : R.color.icon_color_osmand_light;
	}

	@ColorRes
	public static int getPressedColorId(boolean nightMode) {
		return ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);
	}

	@DrawableRes
	public static int getActiveTransparentBackgroundId(boolean nightMode) {
		return nightMode ? R.drawable.btn_background_active_transparent_dark : R.drawable.btn_background_active_transparent_light;
	}

	@DrawableRes
	public static int getInactiveStrokedBackgroundId(boolean nightMode) {
		return nightMode ? R.drawable.btn_background_stroked_inactive_dark : R.drawable.btn_background_stroked_inactive_light;
	}

	@DrawableRes
	public static int getInactiveButtonBackgroundId(boolean nightMode) {
		return nightMode ? R.drawable.btn_background_inactive_dark : R.drawable.btn_background_inactive_light;
	}

	@Override
	protected void setupHeightAndBackground(@Nullable View mainView, @NonNull Insets sysBars) {
		Activity activity = getActivity();
		if (activity == null || mainView == null) {
			return;
		}
		if (AndroidUiHelper.isOrientationPortrait(activity)) {
			super.setupHeightAndBackground(mainView, sysBars);
			return;
		}
		mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				ViewTreeObserver obs = mainView.getViewTreeObserver();
				obs.removeOnGlobalLayoutListener(this);
				View contentView = mainView.findViewById(R.id.scroll_view);
				contentView.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
				contentView.requestLayout();
				boolean showTopShadow = AndroidUtils.getScreenHeight(activity) - sysBars.top
						- mainView.getHeight() >= AndroidUtils.dpToPx(activity, 8);
				drawTopShadow(showTopShadow);
			}
		});
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			TripRecordingBottomSheet fragment = new TripRecordingBottomSheet();
			fragment.show(fragmentManager, TAG);
		}
	}
}