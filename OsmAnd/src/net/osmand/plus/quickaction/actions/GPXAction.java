package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.GeocodingLookupService.AddressLookupRequest;
import static net.osmand.plus.quickaction.QuickActionIds.GPX_ACTION_ID;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;

import net.osmand.CallbackWithObject;
import net.osmand.data.BackgroundType;
import net.osmand.data.LatLon;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXUtilities;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.ColorDialogs;
import net.osmand.plus.mapcontextmenu.editors.WptPtEditor;
import net.osmand.plus.mapcontextmenu.editors.WptPtEditor.OnDismissListener;
import net.osmand.plus.quickaction.CreateEditActionDialog;
import net.osmand.plus.quickaction.CreateEditActionDialog.FileSelected;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.quickaction.SelectTrackFileDialogFragment;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.track.helpers.GpxDataItem;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.PointImageDrawable;
import net.osmand.plus.widgets.multistatetoggle.RadioItem.OnRadioItemClickListener;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;
import net.osmand.util.Algorithms;

import java.io.File;

public class GPXAction extends QuickAction implements FileSelected {

	public static final QuickActionType TYPE = new QuickActionType(GPX_ACTION_ID, "gpx.add", GPXAction.class)
			.nameRes(R.string.quick_action_add_gpx)
			.iconRes(R.drawable.ic_action_gnew_label_dark)
			.category(QuickActionType.CREATE_CATEGORY);

	public static final String KEY_USE_SELECTED_GPX_FILE = "use_selected_gpx_file";
	public static final String KEY_GPX_FILE_PATH = "gpx_file_path";

	public static final String KEY_USE_PREDEFINED_WPT_APPEARANCE = "dialog";
	public static final String KEY_WPT_NAME = "name";
	public static final String KEY_WPT_ADDRESS = "wpt_address";
	public static final String KEY_WPT_DESCRIPTION = "wpt_description";
	public static final String KEY_WPT_COLOR = "wpt_color";
	public static final String KEY_WPT_ICON = "wpt_icon";
	public static final String KEY_WPT_BACKGROUND_TYPE = "wpt_background_type";

	public static final String KEY_CATEGORY_NAME = "category_name";
	public static final String KEY_CATEGORY_COLOR = "category_color";

	private transient String selectedGpxFilePath;
	private transient WptPt predefinedWaypoint;
	@ColorInt
	private transient int predefinedCategoryColor;

	private transient TextToggleButton trackToggleButton;
	private transient TextToggleButton appearanceToggleButton;

	public GPXAction() {
		super(TYPE);
	}

	public GPXAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		unselectGpxFileIfMissing();

		if (!shouldUseSelectedGpxFile()) {
			addWaypoint(null, mapActivity);
		} else {
			getGpxFile(getSelectedGpxFilePath(true), mapActivity, gpxFile -> {
				addWaypoint(gpxFile, mapActivity);
				return true;
			});
		}
	}

	private void addWaypoint(@Nullable GPXFile gpxFile, @NonNull MapActivity mapActivity) {
		LatLon latLon = getMapLocation(mapActivity);
		boolean usePredefinedWaypoint = Boolean.parseBoolean(getParams().get(KEY_USE_PREDEFINED_WPT_APPEARANCE));
		if (usePredefinedWaypoint) {
			WptPt wptPt = createWaypoint();
			wptPt.lat = latLon.getLatitude();
			wptPt.lon = latLon.getLongitude();
			wptPt.time = System.currentTimeMillis();

			String categoryName = getParams().get(KEY_CATEGORY_NAME);
			int categoryColor = getColorFromParams(KEY_CATEGORY_COLOR, 0);

			if (Algorithms.isBlank(wptPt.name) && Algorithms.isBlank(wptPt.getAddress())) {
				lookupAddress(latLon, mapActivity, foundAddress -> {
					wptPt.name = foundAddress;
					mapActivity.getContextMenu().addWptPt(wptPt, categoryName, categoryColor, true, gpxFile);
					return true;
				});
			} else {
				if (Algorithms.isBlank(wptPt.name)) {
					wptPt.name = wptPt.getAddress();
					wptPt.setAddress(null);
				}
				mapActivity.getContextMenu().addWptPt(wptPt, categoryName, categoryColor, true, gpxFile);
			}
		} else {
			WptPt wptPt = new WptPt(latLon.getLatitude(), latLon.getLongitude(), System.currentTimeMillis(),
					Double.NaN, 0, Double.NaN);
			mapActivity.getContextMenu()
					.addWptPt(wptPt, null, 0, false, gpxFile);
		}
	}

	private void lookupAddress(@NonNull LatLon latLon,
	                           @NonNull MapActivity mapActivity,
	                           @NonNull CallbackWithObject<String> onAddressDetermined) {
		Dialog progressDialog = new ProgressDialog(mapActivity);
		progressDialog.setCancelable(false);
		progressDialog.setTitle(R.string.search_address);
		progressDialog.show();

		AddressLookupRequest lookupRequest = new AddressLookupRequest(latLon, address -> {
			progressDialog.dismiss();
			onAddressDetermined.processResult(address);
		}, null);
		mapActivity.getMyApplication().getGeocodingLookupService().lookupAddress(lookupRequest);
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		View root = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_add_gpx, parent, false);
		parent.addView(root);

		unselectGpxFileIfMissing();
		setupTrackToggleButton(root, mapActivity);
		setupWaypointAppearanceToggle(root, mapActivity);

		boolean editingWaypointTemplate = mapActivity.getFragmentsHelper().getFragment(WptPtEditor.TAG) != null;
		if (editingWaypointTemplate) {
			hideDialogWhileEditingTemplate(mapActivity);
		}
	}

	private void setupTrackToggleButton(@NonNull View container, @NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		boolean night = isNightMode(mapActivity);
		LinearLayout trackToggle = container.findViewById(R.id.track_toggle);
		trackToggleButton = new TextToggleButton(app, trackToggle, night);

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
				R.drawable.btn_solid_border_light, R.drawable.btn_solid_border_dark);

		selectAnotherTrackButtonContainer.setOnClickListener(v -> showSelectTrackFileDialog(mapActivity));
	}

	private void setupGpxTrackInfo(@NonNull View container, @NonNull OsmandApplication app) {
		String gpxFilePath = getSelectedGpxFilePath(false);
		if (gpxFilePath == null) {
			return;
		}

		View trackInfoContainer = container.findViewById(R.id.selected_track_file_container);

		boolean currentTrack = gpxFilePath.isEmpty();
		File file = new File(gpxFilePath);
		String gpxName = currentTrack ? app.getString(R.string.current_track) : GpxUiHelper.getGpxTitle(file.getName());
		SelectedGpxFile selectedGpxFile = currentTrack
				? app.getSavingTrackHelper().getCurrentTrack()
				: app.getSelectedGpxHelper().getSelectedFileByPath(gpxFilePath);
		if (selectedGpxFile != null) {
			setupGpxTrackInfo(trackInfoContainer, gpxName, selectedGpxFile.getTrackAnalysis(app), app);
		} else {
			GpxDataItem gpxDataItem = app.getGpxDbHelper().getItem(file, item -> {
				if (item.getAnalysis() != null) {
					setupGpxTrackInfo(trackInfoContainer, gpxName, item.getAnalysis(), app);
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

		TextView title = trackInfoContainer.findViewById(R.id.title);
		title.setText(gpxName);
		title.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);

		ImageView trackIcon = trackInfoContainer.findViewById(R.id.icon);
		trackIcon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_polygom_dark));

		ImageView distanceIcon = trackInfoContainer.findViewById(R.id.distance_icon);
		TextView distanceText = trackInfoContainer.findViewById(R.id.distance);
		distanceIcon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_distance_16));
		distanceText.setText(OsmAndFormatter.getFormattedDistance(analysis.getTotalDistance(), app));

		ImageView waypointsIcon = trackInfoContainer.findViewById(R.id.points_icon);
		TextView waypointsCountText = trackInfoContainer.findViewById(R.id.points_count);
		waypointsIcon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_waypoint_16));
		waypointsCountText.setText(String.valueOf(analysis.getWptPoints()));

		ImageView timeIcon = trackInfoContainer.findViewById(R.id.time_icon);
		if (analysis.isTimeSpecified()) {
			AndroidUiHelper.updateVisibility(timeIcon, true);
			timeIcon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_time_16));

			TextView timeText = trackInfoContainer.findViewById(R.id.time);
			int duration = analysis.getDurationInSeconds();
			timeText.setText(Algorithms.formatDuration(duration, app.accessibilityEnabled()));
		} else {
			AndroidUiHelper.updateVisibility(timeIcon, false);
		}
	}

	private void showSelectTrackFileDialog(@NonNull MapActivity mapActivity) {
		SelectTrackFileDialogFragment.showInstance(mapActivity.getSupportFragmentManager(), getDialog(mapActivity));
	}

	private void setupWaypointAppearanceToggle(@NonNull View container, @NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		boolean night = isNightMode(mapActivity);
		boolean usePredefinedAppearance = predefinedWaypoint != null
				|| Boolean.parseBoolean(getParams().get(KEY_USE_PREDEFINED_WPT_APPEARANCE));
		LinearLayout appearanceToggle = container.findViewById(R.id.appearance_toggle);
		appearanceToggleButton = new TextToggleButton(app, appearanceToggle, night);

		TextRadioItem alwaysAskButton = new TextRadioItem(app.getString(R.string.confirm_every_run));
		TextRadioItem predefinedAppearanceButton = new TextRadioItem(app.getString(R.string.shared_string_predefined));

		alwaysAskButton.setOnClickListener(getOnAppearanceToggleButtonClicked(container, true, mapActivity));
		predefinedAppearanceButton.setOnClickListener(getOnAppearanceToggleButtonClicked(container, false, mapActivity));

		appearanceToggleButton.setItems(alwaysAskButton, predefinedAppearanceButton);
		TextRadioItem selectedItem = usePredefinedAppearance ? predefinedAppearanceButton : alwaysAskButton;
		appearanceToggleButton.setSelectedItem(selectedItem);
		updateAppearanceBottomInfo(container, !usePredefinedAppearance, mapActivity);
	}

	@NonNull
	private OnRadioItemClickListener getOnAppearanceToggleButtonClicked(@NonNull View container,
	                                                                    boolean alwaysAsk,
	                                                                    @NonNull MapActivity mapActivity) {
		return (radioItem, view) -> {
			if (alwaysAsk) {
				updateAppearanceBottomInfo(container, true, mapActivity);
			} else {
				if (hasPredefinedWaypointAppearance()) {
					updateAppearanceBottomInfo(container, false, mapActivity);
				} else {
					showPredefineWaypointAppearanceDialog(container, mapActivity);
					return false;
				}
			}
			return true;
		};
	}

	private void updateAppearanceBottomInfo(@NonNull View container, boolean alwaysAsk, @NonNull MapActivity mapActivity) {
		Context context = container.getContext();
		boolean night = isNightMode(mapActivity);

		AndroidUiHelper.updateVisibility(container.findViewById(R.id.always_ask_waypoint_appearance), alwaysAsk);
		AndroidUiHelper.updateVisibility(container.findViewById(R.id.predefined_appearance_container), !alwaysAsk);

		if (!alwaysAsk) {
			WptPt waypoint = createWaypoint();

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
			int categoryColor = getCategoryColor();
			int categoryColorToDisplay = categoryColor != 0
					? categoryColor
					: ColorUtilities.getDefaultIconColor(context, night);
			ColorStateList categoryIconTint = ColorStateList.valueOf(categoryColorToDisplay);
			TextViewCompat.setCompoundDrawableTintList(predefinedCategoryName, categoryIconTint);

			View editAppearanceButtonContainer = container.findViewById(R.id.edit_appearance_button_container);
			View editAppearanceButton = container.findViewById(R.id.edit_appearance_button);

			AndroidUtils.setBackground(context, editAppearanceButtonContainer, night,
					R.drawable.ripple_oval_light, R.drawable.ripple_oval_dark);
			AndroidUtils.setBackground(context, editAppearanceButton, night,
					R.drawable.btn_oval_blue, R.drawable.btn_oval_orange);

			editAppearanceButtonContainer.setOnClickListener(v ->
					showPredefineWaypointAppearanceDialog(container, mapActivity));
		}
	}

	private void showPredefineWaypointAppearanceDialog(@NonNull View container, @NonNull MapActivity mapActivity) {
		WptPtEditor waypointEditor = mapActivity.getContextMenu().getWptPtPointEditor();
		if (waypointEditor != null) {
			WptPt source = hasPredefinedWaypointAppearance()
					? createWaypoint()
					: null;
			String gpxFilePath = getSelectedGpxFilePath(true);
			waypointEditor.setOnDismissListener(showDialogAfterTemplateEditing(mapActivity));
			waypointEditor.setOnWaypointTemplateAddedListener((waypoint, categoryColor) -> {
				predefinedWaypoint = waypoint;
				predefinedCategoryColor = categoryColor;
				setupWaypointAppearanceToggle(container, mapActivity);
			});

			if (gpxFilePath == null) {
				int categoryColor = getColorFromParams(KEY_CATEGORY_COLOR, 0);
				waypointEditor.addWaypointTemplate(source, categoryColor);
				hideDialog(mapActivity);
			} else {
				getGpxFile(gpxFilePath, mapActivity, gpxFile -> {
					waypointEditor.addWaypointTemplate(source, gpxFile);
					hideDialog(mapActivity);
					return true;
				});
			}
		}
	}

	private void hideDialog(@NonNull MapActivity mapActivity) {
		CreateEditActionDialog dialog = getDialog(mapActivity);
		if (dialog != null) {
			dialog.hide();
		}
	}

	@Nullable
	private CreateEditActionDialog getDialog(@NonNull MapActivity mapActivity) {
		Fragment fragment = mapActivity.getFragmentsHelper().getFragment(CreateEditActionDialog.TAG);
		return fragment instanceof CreateEditActionDialog
				? ((CreateEditActionDialog) fragment)
				: null;
	}

	private boolean shouldUseSelectedGpxFile() {
		boolean useSelectedGpxFile = Boolean.parseBoolean(getParams().get(KEY_USE_SELECTED_GPX_FILE));
		String gpxFilePath = getSelectedGpxFilePath(false);
		boolean gpxFileExist = gpxFilePath != null && (gpxFilePath.isEmpty() || new File(gpxFilePath).exists());
		return (useSelectedGpxFile || this.selectedGpxFilePath != null) && gpxFileExist;
	}

	private boolean hasPredefinedWaypointAppearance() {
		return predefinedWaypoint != null || getParams().containsKey(KEY_WPT_COLOR)
				|| !Algorithms.isBlank(getParams().get(KEY_WPT_NAME))
				|| !Algorithms.isBlank(getParams().get(KEY_CATEGORY_NAME));
	}

	@NonNull
	private WptPt createWaypoint() {
		WptPt waypoint = new WptPt();
		waypoint.name = predefinedWaypoint != null ? predefinedWaypoint.name : getParams().get(KEY_WPT_NAME);
		waypoint.setAddress(predefinedWaypoint != null ? predefinedWaypoint.getAddress() : getParams().get(KEY_WPT_ADDRESS));
		waypoint.desc = predefinedWaypoint != null ? predefinedWaypoint.desc : getParams().get(KEY_WPT_DESCRIPTION);
		waypoint.category = predefinedWaypoint != null ? predefinedWaypoint.category : getParams().get(KEY_CATEGORY_NAME);
		waypoint.setColor(predefinedWaypoint != null ? predefinedWaypoint.getColor() : getWaypointColorFromParams());
		waypoint.setIconName(predefinedWaypoint != null ? predefinedWaypoint.getIconName() : getIconNameFromParams());
		waypoint.setBackgroundType(predefinedWaypoint != null ? predefinedWaypoint.getBackgroundType() : getBackgroundTypeFromParams());
		return waypoint;
	}

	@ColorInt
	private int getWaypointColorFromParams() {
		return getColorFromParams(KEY_WPT_COLOR, ColorDialogs.pallette[0]);
	}

	@NonNull
	private String getIconNameFromParams() {
		String iconName = getParams().get(KEY_WPT_ICON);
		return Algorithms.isEmpty(iconName) || !RenderingIcons.containsBigIcon(iconName)
				? GPXUtilities.DEFAULT_ICON_NAME
				: iconName;
	}

	@NonNull
	private String getBackgroundTypeFromParams() {
		String backgroundType = getParams().get(KEY_WPT_BACKGROUND_TYPE);
		return BackgroundType.getByTypeName(backgroundType, BackgroundType.CIRCLE).getTypeName();
	}

	@ColorInt
	private int getCategoryColor() {
		return predefinedWaypoint != null
				? predefinedCategoryColor
				: getColorFromParams(KEY_CATEGORY_COLOR, 0);
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
		boolean useSelectedGpxFile = trackToggleButton.getSelectedItemIndex() == 1;
		getParams().put(KEY_USE_SELECTED_GPX_FILE, String.valueOf(useSelectedGpxFile));
		if (selectedGpxFilePath != null) {
			getParams().put(KEY_GPX_FILE_PATH, selectedGpxFilePath);
		}

		boolean usePredefinedTemplate = appearanceToggleButton.getSelectedItemIndex() == 1;
		getParams().put(KEY_USE_PREDEFINED_WPT_APPEARANCE, String.valueOf(usePredefinedTemplate));
		if (predefinedWaypoint != null) {
			getParams().put(KEY_WPT_NAME, predefinedWaypoint.name);
			getParams().put(KEY_WPT_ADDRESS, predefinedWaypoint.getAddress());
			getParams().put(KEY_WPT_DESCRIPTION, predefinedWaypoint.desc);
			getParams().put(KEY_WPT_COLOR, String.valueOf(predefinedWaypoint.getColor()));
			getParams().put(KEY_WPT_ICON, predefinedWaypoint.getIconName());
			getParams().put(KEY_WPT_BACKGROUND_TYPE, predefinedWaypoint.getBackgroundType());
			getParams().put(KEY_CATEGORY_NAME, predefinedWaypoint.category);
			getParams().put(KEY_CATEGORY_COLOR, String.valueOf(predefinedCategoryColor));
		}

		return true;
	}

	private String getSelectedGpxFilePath(boolean paramsOnly) {
		return paramsOnly || selectedGpxFilePath == null ? getParams().get(KEY_GPX_FILE_PATH) : selectedGpxFilePath;
	}

	private void unselectGpxFileIfMissing() {
		String gpxFilePath = getSelectedGpxFilePath(true);
		boolean gpxFileMissing = !Algorithms.isEmpty(gpxFilePath) && !new File(gpxFilePath).exists();
		if (gpxFileMissing) {
			getParams().put(KEY_USE_SELECTED_GPX_FILE, String.valueOf(false));
			getParams().remove(KEY_GPX_FILE_PATH);
		}
	}

	private void getGpxFile(@NonNull String gpxFilePath,
	                        @NonNull MapActivity mapActivity,
	                        @NonNull CallbackWithObject<GPXFile> onGpxFileAvailable) {
		OsmandApplication app = mapActivity.getMyApplication();
		if (gpxFilePath.isEmpty()) {
			onGpxFileAvailable.processResult(app.getSavingTrackHelper().getCurrentGpx());
		} else {
			SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(gpxFilePath);
			if (selectedGpxFile != null) {
				onGpxFileAvailable.processResult(selectedGpxFile.getGpxFile());
			} else {
				CallbackWithObject<GPXFile[]> onGpxFileLoaded = gpxFiles -> {
					onGpxFileAvailable.processResult(gpxFiles[0]);
					return true;
				};
				String gpxFileName = Algorithms.getFileWithoutDirs(gpxFilePath);
				File gpxFileDir = new File(gpxFilePath.replace("/" + gpxFileName, ""));
				GpxUiHelper.loadGPXFileInDifferentThread(mapActivity, onGpxFileLoaded, gpxFileDir,
						null, gpxFileName);
			}
		}
	}

	private void hideDialogWhileEditingTemplate(@NonNull MapActivity mapActivity) {
		CreateEditActionDialog dialog = getDialog(mapActivity);
		View view = dialog == null ? null : dialog.getView();
		if (view != null) {
			view.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
					WptPtEditor wptPtEditor = mapActivity.getContextMenu().getWptPtPointEditor();
					if (wptPtEditor != null) {
						wptPtEditor.setOnDismissListener(showDialogAfterTemplateEditing(mapActivity));
						hideDialog(mapActivity);
					}
				}
			});
		}
	}

	@NonNull
	private OnDismissListener showDialogAfterTemplateEditing(@NonNull MapActivity mapActivity) {
		return () -> {
			CreateEditActionDialog dialog = getDialog(mapActivity);
			if (dialog != null) {
				dialog.show();
			}
		};
	}

	public void onGpxFileSelected(@NonNull View container, @NonNull MapActivity mapActivity, @NonNull String gpxFilePath) {
		selectedGpxFilePath = gpxFilePath;
		setupTrackToggleButton(container, mapActivity);
	}

	private boolean isNightMode(@NonNull MapActivity mapActivity) {
		return mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
	}
}