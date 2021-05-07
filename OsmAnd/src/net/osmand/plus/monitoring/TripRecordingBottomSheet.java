package net.osmand.plus.monitoring;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.other.TrackChartPoints;
import net.osmand.plus.myplaces.GPXItemPagerAdapter;
import net.osmand.plus.myplaces.GPXTabItemType;
import net.osmand.plus.myplaces.SegmentActionsListener;
import net.osmand.plus.myplaces.SegmentGPXAdapter;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.GpxBlockStatisticsBuilder;
import net.osmand.plus.track.TrackAppearanceFragment;
import net.osmand.plus.track.TrackDisplayHelper;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;
import net.osmand.plus.views.controls.WrapContentHeightViewPager;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static net.osmand.AndroidUtils.getSecondaryTextColorId;
import static net.osmand.AndroidUtils.setPadding;
import static net.osmand.plus.UiUtilities.CompoundButtonType.GLOBAL;
import static net.osmand.plus.myplaces.GPXTabItemType.GPX_TAB_ITEM_ALTITUDE;
import static net.osmand.plus.myplaces.GPXTabItemType.GPX_TAB_ITEM_GENERAL;
import static net.osmand.plus.myplaces.GPXTabItemType.GPX_TAB_ITEM_SPEED;

public class TripRecordingBottomSheet extends MenuBottomSheetDialogFragment implements SegmentActionsListener {

	public static final String TAG = TripRecordingBottomSheet.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(TripRecordingBottomSheet.class);
	public static final String UPDATE_TRACK_ICON = "update_track_icon";
	public static final String UPDATE_DYNAMIC_ITEMS = "update_dynamic_items";
	private static final int GPS_UPDATE_INTERVAL = 1000;
	public static final GPXTabItemType[] INIT_TAB_ITEMS =
			new GPXTabItemType[] {GPX_TAB_ITEM_GENERAL, GPX_TAB_ITEM_ALTITUDE, GPX_TAB_ITEM_SPEED};

	private OsmandApplication app;
	private OsmandSettings settings;
	private SavingTrackHelper helper;
	private OsmandMonitoringPlugin plugin;

	private View statusContainer;
	private AppCompatImageView trackAppearanceIcon;

	private TrackChartPoints trackChartPoints;
	private GPXItemPagerAdapter graphsAdapter;
	private int graphTabPosition = 0;
	private ViewGroup segmentsTabs;

	private GpxBlockStatisticsBuilder blockStatisticsBuilder;
	private SelectedGpxFile selectedGpxFile;
	private final Handler handler = new Handler();
	private Runnable updatingGPS;
	private Runnable updatingGraph;

	private GPXFile getGPXFile() {
		return selectedGpxFile.getGpxFile();
	}

	private boolean hasDataToSave() {
		return helper.hasDataToSave();
	}

	private boolean searchingGPS() {
		return app.getLocationProvider().getLastKnownLocation() == null;
	}

	private boolean wasTrackMonitored() {
		return settings.SAVE_GLOBAL_TRACK_TO_GPX.get();
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (!fragmentManager.isStateSaved()) {
			TripRecordingBottomSheet fragment = new TripRecordingBottomSheet();
			fragment.show(fragmentManager, TAG);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();
		settings = app.getSettings();
		helper = app.getSavingTrackHelper();
		plugin = OsmandPlugin.getPlugin(OsmandMonitoringPlugin.class);
		selectedGpxFile = helper.getCurrentTrack();
		LayoutInflater inflater = UiUtilities.getInflater(getContext(), nightMode);
		final FragmentManager fragmentManager = getFragmentManager();

		View itemView = inflater.inflate(R.layout.trip_recording_fragment, null, false);
		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(itemView)
				.create());

		statusContainer = itemView.findViewById(R.id.status_container);
		updateStatus();

		LinearLayout showTrackContainer = itemView.findViewById(R.id.show_track_on_map);
		trackAppearanceIcon = showTrackContainer.findViewById(R.id.additional_button_icon);
		createShowTrackItem(showTrackContainer, trackAppearanceIcon, ItemType.SHOW_TRACK.getTitleId(),
				TripRecordingBottomSheet.this, nightMode, new Runnable() {
					@Override
					public void run() {
						hide();
					}
				});

		segmentsTabs = itemView.findViewById(R.id.segments_container);
		createSegmentsTabs(segmentsTabs);

		RecyclerView statBlocks = itemView.findViewById(R.id.block_statistics);
		blockStatisticsBuilder = new GpxBlockStatisticsBuilder(app, selectedGpxFile, nightMode);
		blockStatisticsBuilder.setBlocksView(statBlocks);
		blockStatisticsBuilder.setBlocksClickable(false);
		blockStatisticsBuilder.setTabItem(GPX_TAB_ITEM_GENERAL);
		blockStatisticsBuilder.initStatBlocks(null,
				ContextCompat.getColor(app, getActiveTextColorId(nightMode)));

		CardView cardLeft = itemView.findViewById(R.id.button_left);
		createItem(cardLeft, ItemType.CLOSE);
		cardLeft.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		final CardView cardCenterLeft = itemView.findViewById(R.id.button_center_left);
		createItem(cardCenterLeft, wasTrackMonitored() ? ItemType.PAUSE : ItemType.RESUME);
		cardCenterLeft.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean wasTrackMonitored = !wasTrackMonitored();
				if (!wasTrackMonitored) {
					blockStatisticsBuilder.stopUpdatingStatBlocks();
					stopUpdatingGraph();
				} else {
					blockStatisticsBuilder.runUpdatingStatBlocksIfNeeded();
					runUpdatingGraph();
				}
				settings.SAVE_GLOBAL_TRACK_TO_GPX.set(wasTrackMonitored);
				updateStatus();
				createItem(cardCenterLeft, wasTrackMonitored ? ItemType.PAUSE : ItemType.RESUME);
			}
		});

		CardView cardCenterRight = itemView.findViewById(R.id.button_center_right);
		createItem(cardCenterRight, ItemType.FINISH);
		cardCenterRight.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null && plugin != null && hasDataToSave()) {
					plugin.saveCurrentTrack(null, mapActivity);
					app.getNotificationHelper().refreshNotifications();
					dismiss();
				}
			}
		});

		CardView cardRight = itemView.findViewById(R.id.button_right);
		createItem(cardRight, ItemType.OPTIONS);
		cardRight.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (fragmentManager != null) {
					TripRecordingOptionsBottomSheet.showInstance(fragmentManager, TripRecordingBottomSheet.this);
				}
			}
		});

	}

	@Override
	public void onResume() {
		super.onResume();
		blockStatisticsBuilder.runUpdatingStatBlocksIfNeeded();
		runUpdatingGPS();
		runUpdatingGraph();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapLayers().getGpxLayer().setTrackChartPoints(trackChartPoints);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		blockStatisticsBuilder.stopUpdatingStatBlocks();
		stopUpdatingGPS();
		stopUpdatingGraph();
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

	private void stopUpdatingGPS() {
		handler.removeCallbacks(updatingGPS);
	}

	private void runUpdatingGPS() {
		updatingGPS = new Runnable() {
			@Override
			public void run() {
				int interval = app.getSettings().SAVE_GLOBAL_TRACK_INTERVAL.get();
				updateStatus();
				handler.postDelayed(this, Math.max(GPS_UPDATE_INTERVAL, interval));
			}
		};
		handler.post(updatingGPS);
	}

	private void stopUpdatingGraph() {
		handler.removeCallbacks(updatingGraph);
	}

	private void runUpdatingGraph() {
		updatingGraph = new Runnable() {
			@Override
			public void run() {
				int interval = app.getSettings().SAVE_GLOBAL_TRACK_INTERVAL.get();
				graphsAdapter.updateGraph(graphTabPosition);
				AndroidUiHelper.updateVisibility(segmentsTabs, graphsAdapter.isTabsVisible());
				handler.postDelayed(this, Math.max(GPS_UPDATE_INTERVAL, interval));
			}
		};
		handler.post(updatingGraph);
	}

	private void restartUpdatingGraph() {
		stopUpdatingGraph();
		runUpdatingGraph();
	}

	private void createSegmentsTabs(ViewGroup viewGroup) {
		View segmentView = SegmentGPXAdapter.createGpxTabsView(null, viewGroup, this, nightMode);
		AndroidUiHelper.setVisibility(View.GONE, segmentView.findViewById(R.id.list_item_divider));
		WrapContentHeightViewPager pager = segmentView.findViewById(R.id.pager);
		PagerSlidingTabStrip tabLayout = segmentView.findViewById(R.id.sliding_tabs);
		tabLayout.setDividerWidth(AndroidUtils.dpToPx(app, 1));
		tabLayout.setDividerColor(ContextCompat.getColor(app, nightMode ?
				R.color.stroked_buttons_and_links_outline_dark : R.color.stroked_buttons_and_links_outline_light));
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
		GPXFile gpxFile = getGPXFile();
		File file = new File(gpxFile.path);
		displayHelper.setFile(file);
		displayHelper.setGpxDataItem(app.getGpxDbHelper().getItem(file));
		displayHelper.setGpx(gpxFile);

		graphsAdapter = new GPXItemPagerAdapter(app, null, displayHelper, nightMode, this, true);
		graphsAdapter.setChartHMargin(getResources().getDimensionPixelSize(R.dimen.content_padding));

		pager.setAdapter(graphsAdapter);
		tabLayout.setViewPager(pager);

		viewGroup.addView(segmentView);
	}

	private void updateStatus() {
		TextView statusTitle = statusContainer.findViewById(R.id.text_status);
		AppCompatImageView statusIcon = statusContainer.findViewById(R.id.icon_status);
		ItemType status = searchingGPS() ? ItemType.SEARCHING_GPS : !wasTrackMonitored() ? ItemType.ON_PAUSE : ItemType.RECORDING;
		Integer titleId = status.getTitleId();
		if (titleId != null) {
			statusTitle.setText(titleId);
		}
		int colorText = status.equals(ItemType.SEARCHING_GPS) ? getSecondaryTextColorId(nightMode) : getOsmandIconColorId(nightMode);
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
										   Integer showTrackId, final Fragment target,
										   final boolean nightMode, final Runnable hideOnClickButtonAppearance) {
		FragmentActivity activity = target.getActivity();
		if (!(activity instanceof MapActivity)) {
			return;
		}
		final MapActivity mapActivity = (MapActivity) activity;
		final OsmandApplication app = mapActivity.getMyApplication();
		final CardView buttonShowTrack = showTrackContainer.findViewById(R.id.compound_container);
		final CardView buttonAppearance = showTrackContainer.findViewById(R.id.additional_button_container);

		TextView showTrackTextView = buttonShowTrack.findViewById(R.id.title);
		if (showTrackId != null) {
			showTrackTextView.setText(showTrackId);
		}
		final CompoundButton showTrackCompound = buttonShowTrack.findViewById(R.id.compound_button);
		showTrackCompound.setChecked(app.getSelectedGpxHelper().getSelectedCurrentRecordingTrack() != null);
		UiUtilities.setupCompoundButton(showTrackCompound, nightMode, GLOBAL);

		setShowTrackItemBackground(buttonShowTrack, showTrackCompound.isChecked(), nightMode);
		buttonShowTrack.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean checked = !showTrackCompound.isChecked();
				showTrackCompound.setChecked(checked);
				app.getSelectedGpxHelper().selectGpxFile(app.getSavingTrackHelper().getCurrentGpx(), checked, false);
				setShowTrackItemBackground(buttonShowTrack, checked, nightMode);
				createItem(app, nightMode, buttonAppearance, ItemType.APPEARANCE, checked, null);
			}
		});

		updateTrackIcon(app, trackAppearanceIcon);
		createItem(app, nightMode, buttonAppearance, ItemType.APPEARANCE, showTrackCompound.isChecked(), null);
		buttonAppearance.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (showTrackCompound.isChecked()) {
					hideOnClickButtonAppearance.run();
					SelectedGpxFile selectedGpxFile = app.getSavingTrackHelper().getCurrentTrack();
					TrackAppearanceFragment.showInstance(mapActivity, selectedGpxFile, target);
				}
			}
		});
	}

	public static void setShowTrackItemBackground(View view, boolean checked, boolean nightMode) {
		Drawable background = AppCompatResources.getDrawable(view.getContext(),
				checked ? getActiveTransparentBackgroundId(nightMode) : getInactiveStrokedBackgroundId(nightMode));
		view.setBackgroundDrawable(background);
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
			setTextColorActive(context, title, nightMode, type);
		}
		setItemBackgroundActive(context, nightMode, view);
	}

	public static void setItemBackground(Context context, boolean nightMode, View view, boolean enabled) {
		if (view instanceof CardView) {
			int colorId = enabled ? getActiveTransparentColorId(nightMode) : getInactiveButtonColorId(nightMode);
			((CardView) view).setCardBackgroundColor(AndroidUtils.createPressedColorStateList(
					context, colorId, getActiveTextColorId(nightMode)
			));
			return;
		}
		Drawable background = AppCompatResources.getDrawable(context, getInactiveButtonBackgroundId(nightMode));
		if (background != null && enabled) {
			DrawableCompat.setTintList(background, AndroidUtils.createPressedColorStateList(
					context, getInactiveButtonColorId(nightMode), getActiveTextColorId(nightMode)
			));
		} else {
			UiUtilities.tintDrawable(background, ContextCompat.getColor(context, getInactiveButtonColorId(nightMode)));
		}
		view.setBackgroundDrawable(background);
	}

	public static void setItemBackgroundActive(Context context, boolean nightMode, View view) {
		if (view instanceof CardView) {
			((CardView) view).setCardBackgroundColor(ContextCompat.getColor(context, getActiveTextColorId(nightMode)));
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
			int activeColorId = type.isNegative() ? R.color.color_osm_edit_delete : getActiveTextColorId(nightMode);
			int normalColorId = enabled ? activeColorId : getSecondaryTextColorId(nightMode);
			ColorStateList textColorStateList = AndroidUtils.createPressedColorStateList(context, normalColorId, getPressedColorId(nightMode));
			tv.setTextColor(textColorStateList);
		}
	}

	protected static void setTextColorActive(Context context, TextView tv, boolean nightMode, ItemType type) {
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

	public interface DismissTargetFragment {
		void dismissTarget();
	}

	@ColorRes
	public static int getActiveTextColorId(boolean nightMode) {
		return nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
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
	public static int getActiveButtonColorId(boolean nightMode) {
		return nightMode ? R.color.active_buttons_and_links_bg_pressed_dark : R.color.active_buttons_and_links_bg_pressed_light;
	}

	@ColorRes
	public static int getInactiveButtonColorId(boolean nightMode) {
		return nightMode ? R.color.inactive_buttons_and_links_bg_dark : R.color.inactive_buttons_and_links_bg_light;
	}

	@ColorRes
	public static int getOsmandIconColorId(boolean nightMode) {
		return nightMode ? R.color.icon_color_osmand_dark : R.color.icon_color_osmand_light;
	}

	@ColorRes
	public static int getActiveTransparentColorId(boolean nightMode) {
		return nightMode ? R.color.switch_button_active_dark : R.color.switch_button_active_light;
	}

	@ColorRes
	public static int getPressedColorId(boolean nightMode) {
		return nightMode ? R.color.active_buttons_and_links_text_dark : R.color.active_buttons_and_links_text_light;
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
	protected void setupHeightAndBackground(final View mainView) {
		final Activity activity = getActivity();
		if (activity == null) {
			return;
		}
		if (AndroidUiHelper.isOrientationPortrait(activity)) {
			super.setupHeightAndBackground(mainView);
			return;
		}

		mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				ViewTreeObserver obs = mainView.getViewTreeObserver();
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					obs.removeOnGlobalLayoutListener(this);
				} else {
					obs.removeGlobalOnLayoutListener(this);
				}
				final View contentView = mainView.findViewById(R.id.scroll_view);
				contentView.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
				contentView.requestLayout();
				boolean showTopShadow = AndroidUtils.getScreenHeight(activity) - AndroidUtils.getStatusBarHeight(activity)
						- mainView.getHeight() >= AndroidUtils.dpToPx(activity, 8);
				drawTopShadow(showTopShadow);
			}
		});
	}

	@Override
	protected boolean hideButtonsContainer() {
		return true;
	}

	@Nullable
	public MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		}
		return null;
	}
}
