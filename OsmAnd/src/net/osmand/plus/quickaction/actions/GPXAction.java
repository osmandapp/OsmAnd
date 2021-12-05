package net.osmand.plus.quickaction.actions;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.data.FavouritePoint.BackgroundType;
import net.osmand.data.LatLon;
import net.osmand.plus.ColorUtilities;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GeocodingLookupService;
import net.osmand.plus.GpxDbHelper.GpxDataItemCallback;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.PointImageDrawable;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.ColorDialogs;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.widgets.multistatetoggle.RadioItem.OnRadioItemClickListener;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;
import net.osmand.util.Algorithms;

import java.io.File;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.widget.TextViewCompat;

public class GPXAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(6, "gpx.add", GPXAction.class)
			.nameRes(R.string.quick_action_add_gpx)
			.iconRes(R.drawable.ic_action_gnew_label_dark)
			.category(QuickActionType.CREATE_CATEGORY);

	private static final String KEY_USE_SELECTED_GPX_FILE = "use_selected_gpx_file";
	private static final String KEY_GPX_FILE_PATH = "gpx_file_path";

	private static final String KEY_USE_PREDEFINED_WPT_APPEARANCE = "use_predefined_appearance";
	private static final String KEY_PREDEFINED_WPT_NAME = "name";
	private static final String KEY_PREDEFINED_WPT_DESCRIPTION = "predefined_wpt_description";
	private static final String KEY_PREDEFINED_WPT_COLOR = "predefined_wpt_color";
	private static final String KEY_PREDEFINED_WPT_ICON = "predefined_wpt_icon";
	private static final String KEY_PREDEFINED_WPT_BACKGROUND_TYPE = "predefined_wpt_background_type";

	private static final String KEY_PREDEFINED_CATEGORY_NAME = "category_name";
	private static final String KEY_PREDEFINED_CATEGORY_COLOR = "category_color";

	public GPXAction() {
		super(TYPE);
	}

	public GPXAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull final MapActivity mapActivity) {

		final LatLon latLon = mapActivity.getMapView()
				.getCurrentRotatedTileBox()
				.getCenterLatLon();

		final String title = getParams().get(KEY_PREDEFINED_WPT_NAME);

		if (title == null || title.isEmpty()) {

			final Dialog progressDialog = new ProgressDialog(mapActivity);
			progressDialog.setCancelable(false);
			progressDialog.setTitle(R.string.search_address);
			progressDialog.show();

			GeocodingLookupService.AddressLookupRequest lookupRequest = new GeocodingLookupService.AddressLookupRequest(latLon,

					new GeocodingLookupService.OnAddressLookupResult() {

						@Override
						public void geocodingDone(String address) {

							progressDialog.dismiss();
							mapActivity.getContextMenu().addWptPt(latLon, address,
									getParams().get(KEY_PREDEFINED_CATEGORY_NAME),
									Integer.valueOf(getParams().get(KEY_PREDEFINED_CATEGORY_COLOR)),
									false);
						}

					}, null);

			mapActivity.getMyApplication().getGeocodingLookupService().lookupAddress(lookupRequest);

		} else mapActivity.getContextMenu().addWptPt(latLon, title,
				getParams().get(KEY_PREDEFINED_CATEGORY_NAME),
				Integer.valueOf(getParams().get(KEY_PREDEFINED_CATEGORY_COLOR)), false);
	}

	@Override
	public void drawUI(@NonNull final ViewGroup parent, @NonNull final MapActivity mapActivity) {
		View root = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_add_gpx, parent, false);
		parent.addView(root);

		setupTrackToggleButton(root, mapActivity);
		setupWaypointAppearanceToggle(root, mapActivity);
	}

	private void setupTrackToggleButton(@NonNull View container, @NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		boolean night = app.getDaynightHelper().isNightModeForMapControls();
		LinearLayout trackToggle = container.findViewById(R.id.track_toggle);
		TextToggleButton trackToggleButton = new TextToggleButton(app, trackToggle, night);

		TextRadioItem alwaysAskButton = new TextRadioItem(app.getString(R.string.confirm_every_run));
		TextRadioItem selectTrackButton = new TextRadioItem(app.getString(R.string.shared_string_select));

		alwaysAskButton.setOnClickListener(getOnTrackToggleButtonClicked(container, true, mapActivity));
		selectTrackButton.setOnClickListener(getOnTrackToggleButtonClicked(container, false, mapActivity));

		trackToggleButton.setItems(alwaysAskButton, selectTrackButton);
		trackToggleButton.setSelectedItem(shouldUseSelectedGpxFile() ? selectTrackButton : alwaysAskButton);
		updateTrackBottomInfo(container, !shouldUseSelectedGpxFile());

		setupSelectAnotherTrackButton(container, night, mapActivity);
		if (shouldUseSelectedGpxFile()) {
			setupGpxTrackInfo(container, app);
		}
	}

	@NonNull
	private OnRadioItemClickListener getOnTrackToggleButtonClicked(@NonNull View container, boolean alwaysAsk,
	                                                               @NonNull MapActivity mapActivity) {
		return (radioItem, view) -> {
			if (alwaysAsk) {
				updateTrackBottomInfo(container, true);
			} else {
				if (shouldUseSelectedGpxFile()) {
					updateTrackBottomInfo(container, false);
				} else {
					showSelectTrackFileDialog(mapActivity);
					return false;
				}
			}
			return true;
		};
	}

	private void updateTrackBottomInfo(@NonNull View container, boolean alwaysAsk) {
		AndroidUiHelper.updateVisibility(container.findViewById(R.id.always_ask_track_file), alwaysAsk);
		AndroidUiHelper.updateVisibility(container.findViewById(R.id.selected_track_file_container), !alwaysAsk);
	}

	private void setupSelectAnotherTrackButton(@NonNull View container, boolean night, @NonNull MapActivity mapActivity) {
		View selectAnotherTrackButtonContainer = container.findViewById(R.id.select_another_track_button_container);
		View selectAnotherTrackButton = container.findViewById(R.id.select_another_track_button);

		AndroidUtils.setBackground(container.getContext(), selectAnotherTrackButtonContainer, night,
				R.drawable.ripple_light, R.drawable.ripple_dark);
		AndroidUtils.setBackground(container.getContext(), selectAnotherTrackButton, night,
				R.drawable.btn_solid_border_light, R.drawable.btn_solid_border_light);

		selectAnotherTrackButtonContainer.setOnClickListener(v -> showSelectTrackFileDialog(mapActivity));
	}

	private void setupGpxTrackInfo(@NonNull final View container, @NonNull final OsmandApplication app) {
		String gpxFilePath = getParams().get(KEY_GPX_FILE_PATH);
		if (gpxFilePath == null) {
			return;
		}

		View trackInfoContainer = container.findViewById(R.id.selected_track_file_container);

		File file = new File(gpxFilePath);
		String gpxName = GpxUiHelper.getGpxTitle(file.getName());
		SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(gpxFilePath);
		if (selectedGpxFile != null) {
			setupGpxTrackInfo(trackInfoContainer, gpxName, selectedGpxFile.getTrackAnalysis(app), app);
		} else {
			GpxDataItem gpxDataItem = app.getGpxDbHelper().getItem(file, new GpxDataItemCallback() {
				@Override
				public boolean isCancelled() {
					return false;
				}

				@Override
				public void onGpxDataItemReady(GpxDataItem item) {
					if (item != null && item.getAnalysis() != null) {
						setupGpxTrackInfo(trackInfoContainer, gpxName, item.getAnalysis(), app);
					}
				}
			});

			if (gpxDataItem != null && gpxDataItem.getAnalysis() != null) {
				setupGpxTrackInfo(trackInfoContainer, gpxName, gpxDataItem.getAnalysis(), app);
			}
		}
	}

	private void setupGpxTrackInfo(@NonNull View trackInfoContainer,
	                               @NonNull String gpxName,
	                               @NonNull GPXTrackAnalysis analysis,
	                               @NonNull OsmandApplication app) {
		UiUtilities iconsCache = app.getUIUtilities();

		TextView name = trackInfoContainer.findViewById(R.id.name);
		name.setText(gpxName);
		name.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);

		ImageView trackIcon = trackInfoContainer.findViewById(R.id.icon);
		trackIcon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_polygom_dark));

		ImageView distanceIcon = trackInfoContainer.findViewById(R.id.distance_icon);
		TextView distanceText = trackInfoContainer.findViewById(R.id.distance);
		distanceIcon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_distance_16));
		distanceText.setText(OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));

		ImageView waypointsIcon = trackInfoContainer.findViewById(R.id.points_icon);
		TextView waypointsCountText = trackInfoContainer.findViewById(R.id.points_count);
		waypointsIcon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_waypoint_16));
		waypointsCountText.setText(String.valueOf(analysis.wptPoints));

		ImageView timeIcon = trackInfoContainer.findViewById(R.id.time_icon);
		if (analysis.isTimeSpecified()) {
			AndroidUiHelper.updateVisibility(timeIcon, true);
			timeIcon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_time_16));

			TextView timeText = trackInfoContainer.findViewById(R.id.time);
			int duration = (int) (analysis.timeSpan / 1000);
			timeText.setText(Algorithms.formatDuration(duration, app.accessibilityEnabled()));
		} else {
			AndroidUiHelper.updateVisibility(timeIcon, false);
		}
	}

	private void showSelectTrackFileDialog(@NonNull MapActivity mapActivity) {
		// TODO
	}

	private void setupWaypointAppearanceToggle(@NonNull View container, @NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		boolean night = app.getDaynightHelper().isNightModeForMapControls();
		boolean usePredefinedAppearance = Boolean.parseBoolean(getParams().get(KEY_USE_PREDEFINED_WPT_APPEARANCE));
		LinearLayout appearanceToggle = container.findViewById(R.id.appearance_toggle);
		TextToggleButton appearanceToggleButton = new TextToggleButton(app, appearanceToggle, night);

		TextRadioItem alwaysAskButton = new TextRadioItem(app.getString(R.string.confirm_every_run));
		TextRadioItem predefinedAppearanceButton = new TextRadioItem(app.getString(R.string.shared_string_predefined));

		alwaysAskButton.setOnClickListener(getOnAppearanceToggleButtonClicked(container, true, night));
		predefinedAppearanceButton.setOnClickListener(getOnAppearanceToggleButtonClicked(container, false, night));

		appearanceToggleButton.setItems(alwaysAskButton, predefinedAppearanceButton);
		TextRadioItem selectedItem = usePredefinedAppearance ? predefinedAppearanceButton : alwaysAskButton;
		appearanceToggleButton.setSelectedItem(selectedItem);
		updateAppearanceBottomInfo(container, !usePredefinedAppearance, night);
	}

	@NonNull
	private OnRadioItemClickListener getOnAppearanceToggleButtonClicked(@NonNull View container, boolean alwaysAsk, boolean night) {
		return (radioItem, view) -> {
			if (alwaysAsk) {
				updateAppearanceBottomInfo(container, true, night);
			} else {
				if (hasPredefinedWaypointAppearance()) {
					updateAppearanceBottomInfo(container, false, night);
				} else {
					// todo: open edit favorite dialog
					return false;
				}
			}
			return true;
		};
	}

	private void updateAppearanceBottomInfo(@NonNull View container, boolean alwaysAsk, boolean night) {
		Context context = container.getContext();

		AndroidUiHelper.updateVisibility(container.findViewById(R.id.always_ask_waypoint_appearance), alwaysAsk);
		AndroidUiHelper.updateVisibility(container.findViewById(R.id.predefined_appearance_container), !alwaysAsk);

		if (!alwaysAsk) {
			WptPt waypoint = createWaypointFromParams();

			ImageView predefinedIcon = container.findViewById(R.id.predefined_icon);
			Drawable icon = PointImageDrawable.getFromWpt(context, waypoint.getColor(),
					false, waypoint);
			predefinedIcon.setImageDrawable(icon);

			TextView predefinedTitle = container.findViewById(R.id.predefined_title);
			String waypointName = waypoint.name;
			String title = Algorithms.isEmpty(waypointName) ? context.getString(R.string.address) : waypointName;
			predefinedTitle.setText(title);

			TextView predefinedCategoryName = container.findViewById(R.id.predefined_category_name);
			String categoryName = Algorithms.isEmpty(waypoint.category) ? "" : waypoint.category;
			predefinedCategoryName.setText(categoryName);
			ColorStateList categoryIconTint = ColorStateList.valueOf(getCategoryColorFromParams(context, night));
			TextViewCompat.setCompoundDrawableTintList(predefinedCategoryName, categoryIconTint);

			View editAppearanceButtonContainer = container.findViewById(R.id.edit_appearance_button_container);
			View editAppearanceButton = container.findViewById(R.id.edit_appearance_button);

			AndroidUtils.setBackground(context, editAppearanceButtonContainer, night,
					R.drawable.ripple_oval_light, R.drawable.ripple_oval_dark);
			AndroidUtils.setBackground(context, editAppearanceButton, night,
					R.drawable.btn_oval_blue, R.drawable.btn_oval_orange);
		}
	}

	private boolean shouldUseSelectedGpxFile() {
		boolean useSelectedGpxFile = Boolean.parseBoolean(getParams().get(KEY_USE_SELECTED_GPX_FILE));
		String gpxFilePath = getParams().get(KEY_GPX_FILE_PATH);
		boolean gpxFileExist = gpxFilePath != null && (gpxFilePath.isEmpty() || new File(gpxFilePath).exists());
		return useSelectedGpxFile && gpxFileExist;
	}

	private boolean hasPredefinedWaypointAppearance() {
		return getParams().containsKey(KEY_PREDEFINED_WPT_COLOR);
	}

	@NonNull
	private WptPt createWaypointFromParams() {
		WptPt waypoint = new WptPt();
		waypoint.name = getParams().get(KEY_PREDEFINED_WPT_NAME);
		waypoint.desc = getParams().get(KEY_PREDEFINED_WPT_DESCRIPTION);
		waypoint.category = getParams().get(KEY_PREDEFINED_CATEGORY_NAME);
		waypoint.setColor(getWaypointColorFromParams());
		waypoint.setIconName(getIconNameFromParams());
		waypoint.setBackgroundType(getBackgroundTypeFromParams());
		return waypoint;
	}

	@ColorInt
	private int getWaypointColorFromParams() {
		return getColorFromParams(KEY_PREDEFINED_WPT_COLOR, ColorDialogs.pallette[0]);
	}

	@NonNull
	private String getIconNameFromParams() {
		String iconName = getParams().get(KEY_PREDEFINED_WPT_ICON);
		return Algorithms.isEmpty(iconName) || !RenderingIcons.containsBigIcon(iconName)
				? GPXUtilities.DEFAULT_ICON_NAME
				: iconName;
	}

	@NonNull
	private String getBackgroundTypeFromParams() {
		String backgroundType = getParams().get(KEY_PREDEFINED_WPT_BACKGROUND_TYPE);
		return BackgroundType.getByTypeName(backgroundType, BackgroundType.CIRCLE).getTypeName();
	}

	@ColorInt
	private int getCategoryColorFromParams(@NonNull Context context, boolean night) {
		int defaultColor = ColorUtilities.getDefaultIconColor(context, night);
		return getColorFromParams(KEY_PREDEFINED_CATEGORY_COLOR, defaultColor);
	}

	@ColorInt
	private int getColorFromParams(@NonNull String key, @ColorInt int defaultColor) {
		String colorStr = getParams().get(key);
		if (Algorithms.isEmpty(colorStr)) {
			return defaultColor;
		}
		try {
			return Integer.parseInt(colorStr);
		} catch (NumberFormatException e) {
			return defaultColor;
		}
	}

	@Override
	public boolean fillParams(@NonNull View root, @NonNull MapActivity mapActivity) {
		return true;
	}
}