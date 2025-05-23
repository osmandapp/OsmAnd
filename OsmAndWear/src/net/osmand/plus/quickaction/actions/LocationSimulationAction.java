package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.CreateEditActionDialog.FileSelected;
import static net.osmand.plus.quickaction.CreateEditActionDialog.TAG;
import static net.osmand.plus.quickaction.QuickActionIds.LOCATION_SIMULATION_ACTION_ID;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.google.android.material.slider.Slider;

import net.osmand.CallbackWithObject;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.plus.simulation.OsmAndLocationSimulation;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.quickaction.CreateEditActionDialog;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.track.SelectTrackTabsFragment;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.multistatetoggle.RadioItem.OnRadioItemClickListener;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;
import net.osmand.shared.gpx.GpxHelper;
import net.osmand.util.Algorithms;

import java.io.File;

public class LocationSimulationAction extends QuickAction implements FileSelected {

	public static final QuickActionType TYPE = new QuickActionType(LOCATION_SIMULATION_ACTION_ID, "location.simulation", LocationSimulationAction.class)
			.nameRes(R.string.quick_action_location_by_gpx)
			.iconRes(R.drawable.ic_action_start_navigation).nonEditable()
			.category(QuickActionType.NAVIGATION)
			.nameActionRes(R.string.shared_string_simulate);

	public static final String KEY_USE_SELECTED_GPX_FILE = "use_selected_gpx_file";
	public static final String KEY_GPX_FILE_PATH = "gpx_file_path";

	public static final String KEY_SIMULATION_SPEEDUP = "simulation_speedup";
	public static final String KEY_SIMULATION_CUTOFF = "simulation_cutoff";

	private static final int MIN_SPEEDUP = 1;
	private static final int MAX_SPEEDUP = 8;
	private static final int MIN_CUTOFF_DISTANCE = 0;
	private static final int MAX_CUTOFF_DISTANCE = 10;
	public static final int CUTOFF_STEP_SIZE = 10;
	public static final float SPEEDUP_STEP_SIZE = 0.5f;

	private transient String selectedGpxFilePath;

	private transient TextToggleButton trackToggleButton;
	private float cutOffValue = MIN_CUTOFF_DISTANCE;
	private float speedUpValue = MIN_SPEEDUP;

	public LocationSimulationAction() {
		super(TYPE);
	}

	public LocationSimulationAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		OsmandDevelopmentPlugin plugin = PluginsHelper.getActivePlugin(OsmandDevelopmentPlugin.class);
		if (plugin != null) {
			unselectGpxFileIfMissing();
			speedUpValue = getFloatFromParams(KEY_SIMULATION_SPEEDUP, MIN_SPEEDUP);
			cutOffValue = getFloatFromParams(KEY_SIMULATION_CUTOFF, MIN_CUTOFF_DISTANCE);

			if (!shouldUseSelectedGpxFile()) {
				OsmAndLocationSimulation sim = mapActivity.getMyApplication().getLocationProvider().getLocationSimulation();
				if (sim.isRouteAnimating()) {
					sim.startStopGpxAnimation(mapActivity);
				} else {
					CallbackWithObject<String> onFileSelect = gpxFilePath -> {
						getGpxFile(gpxFilePath, mapActivity, gpxFile -> {
							startStopSimulation(gpxFile, mapActivity);
							return true;
						});
						return true;
					};
					showSelectTrackFileDialog(mapActivity, onFileSelect);
				}
			} else {
				getGpxFile(getSelectedGpxFilePath(true), mapActivity, gpxFile -> {
					startStopSimulation(gpxFile, mapActivity);
					return true;
				});
			}
		}

	}

	@Override
	public int getActionNameRes() {
		return super.getActionNameRes();
	}

	private void startStopSimulation(@Nullable GpxFile gpxFile, @NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		OsmAndLocationSimulation sim = app.getLocationProvider().getLocationSimulation();
		if (sim.isRouteAnimating()) {
			sim.startStopGpxAnimation(mapActivity);
		} else if (gpxFile != null && gpxFile.hasTrkPt()) {
			sim.startSimulationThread(app, gpxFile, (int) cutOffValue, true, speedUpValue);
		}
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		View root = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_simulate_location, parent, false);
		parent.addView(root);
		OsmandApplication app = mapActivity.getMyApplication();
		unselectGpxFileIfMissing();
		setupSpeedUpSlider(root, app);
		setupCutOffSlider(root, app, MAX_CUTOFF_DISTANCE);
		setupTrackToggleButton(root, mapActivity);
	}

	private void setupTrackToggleButton(@NonNull View container, @NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		boolean night = isNightMode(app);
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
					showSelectTrackFileDialog(mapActivity, null);
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

		selectAnotherTrackButtonContainer.setOnClickListener(v -> showSelectTrackFileDialog(mapActivity, null));
	}

	private void setupGpxTrackInfo(@NonNull View container, @NonNull OsmandApplication app) {
		String gpxFilePath = getSelectedGpxFilePath(false);
		if (gpxFilePath == null) {
			return;
		}

		View trackInfoContainer = container.findViewById(R.id.selected_track_file_container);

		boolean currentTrack = gpxFilePath.isEmpty();
		File file = new File(gpxFilePath);
		String gpxName = currentTrack ? app.getString(R.string.current_track) : GpxHelper.INSTANCE.getGpxTitle(file.getName());
		SelectedGpxFile selectedGpxFile = currentTrack
				? app.getSavingTrackHelper().getCurrentTrack()
				: app.getSelectedGpxHelper().getSelectedFileByPath(gpxFilePath);
		if (selectedGpxFile != null) {
			setupGpxTrackInfo(trackInfoContainer, gpxName, selectedGpxFile.getTrackAnalysis(app), app);
		} else {
			GpxDataItem gpxDataItem = app.getGpxDbHelper().getItem(SharedUtil.kFile(file), item -> {
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
	                               @NonNull GpxTrackAnalysis analysis,
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

		setupCutOffSlider(trackInfoContainer.getRootView(), app, (int) (analysis.getTotalDistance() / CUTOFF_STEP_SIZE) * CUTOFF_STEP_SIZE);
	}

	private void showSelectTrackFileDialog(@NonNull MapActivity mapActivity, CallbackWithObject<String> onFileSelect) {
		SelectTrackTabsFragment.showInstance(mapActivity.getSupportFragmentManager(), onFileSelect != null ? onFileSelect : getDialog(mapActivity));
	}

	private void setupSpeedUpSlider(@NonNull View container, @NonNull OsmandApplication app) {
		int min = MIN_SPEEDUP;
		int max = MAX_SPEEDUP;
		speedUpValue = getFloatFromParams(KEY_SIMULATION_SPEEDUP, MIN_SPEEDUP);

		Slider slider = container.findViewById(R.id.speed_slider);
		TextView title = container.findViewById(R.id.speed_title);
		TextView minSpeed = container.findViewById(R.id.min_speed);
		TextView maxSpeed = container.findViewById(R.id.max_speed);

		minSpeed.setText(String.valueOf(min));
		maxSpeed.setText(String.valueOf(max));

		title.setText(getSpeedUpTitle(app, speedUpValue));
		slider.setStepSize(SPEEDUP_STEP_SIZE);
		slider.setValueFrom(min);
		slider.setValueTo(max);
		slider.setValue(speedUpValue);
		slider.addOnChangeListener((s, val, fromUser) -> {
			speedUpValue = val;
			title.setText(getSpeedUpTitle(app, speedUpValue));
		});
		boolean nightMode = isNightMode(app);
		int defaultColor = ColorUtilities.getActiveIconColor(container.getContext(), nightMode);
		UiUtilities.setupSlider(slider, nightMode, defaultColor);
	}

	@NonNull
	private String getSpeedUpTitle(@NonNull OsmandApplication app, float value) {
		return getTitle(app, R.string.location_simulation_speedup, String.valueOf(value));
	}

	@NonNull
	private String getCutOffTitle(@NonNull OsmandApplication app, float value) {
		return getTitle(app, R.string.location_simulation_cutoff, OsmAndFormatter.getFormattedDistance(value, app));
	}

	@Override
	public String getActionText(@NonNull OsmandApplication app) {
		OsmAndLocationSimulation sim = app.getLocationProvider().getLocationSimulation();
		if (sim.isRouteAnimating()) {
			return app.getString(R.string.stop_navigation_service);
		}
		return app.getString(R.string.simulate_location_by_gpx);
	}

	@NonNull
	private String getTitle(@NonNull OsmandApplication app, @StringRes int titleId, String value) {
		return app.getString(R.string.ltr_or_rtl_combine_via_colon, app.getString(titleId), value);
	}

	private void setupCutOffSlider(@NonNull View container, @NonNull OsmandApplication app, int max) {
		int min = MIN_CUTOFF_DISTANCE;
		cutOffValue = getFloatFromParams(KEY_SIMULATION_CUTOFF, MIN_CUTOFF_DISTANCE);
		cutOffValue = Math.min(cutOffValue, max);

		Slider slider = container.findViewById(R.id.cut_slider);
		TextView title = container.findViewById(R.id.cut_title);
		TextView minValue = container.findViewById(R.id.min_cut);
		TextView maxValue = container.findViewById(R.id.max_cut);

		minValue.setText(OsmAndFormatter.getFormattedDistance(min, app));
		maxValue.setText(OsmAndFormatter.getFormattedDistance(max, app));

		title.setText(getCutOffTitle(app, cutOffValue));
		slider.setStepSize(CUTOFF_STEP_SIZE);
		slider.setValueFrom(min);
		slider.setValueTo(max);
		slider.setValue(cutOffValue);
		slider.addOnChangeListener((s, val, fromUser) -> {
			cutOffValue = val;
			title.setText(getCutOffTitle(app, cutOffValue));
		});
		boolean nightMode = isNightMode(app);
		int defaultColor = ColorUtilities.getActiveIconColor(container.getContext(), nightMode);
		UiUtilities.setupSlider(slider, nightMode, defaultColor);
	}

	@Nullable
	private CreateEditActionDialog getDialog(@NonNull MapActivity mapActivity) {
		Fragment fragment = mapActivity.getFragmentsHelper().getFragment(TAG);
		return fragment instanceof CreateEditActionDialog ? ((CreateEditActionDialog) fragment) : null;
	}

	private boolean shouldUseSelectedGpxFile() {
		boolean useSelectedGpxFile = Boolean.parseBoolean(getParams().get(KEY_USE_SELECTED_GPX_FILE));
		String gpxFilePath = getSelectedGpxFilePath(false);
		boolean gpxFileExist = gpxFilePath != null && (gpxFilePath.isEmpty() || new File(gpxFilePath).exists());
		return (useSelectedGpxFile || this.selectedGpxFilePath != null) && gpxFileExist;
	}

	private float getFloatFromParams(@NonNull String key, float defaultValue) {
		String floatStr = getParams().get(key);
		if (Algorithms.isEmpty(floatStr)) {
			return defaultValue;
		}
		try {
			return Float.parseFloat(floatStr);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	@Override
	public boolean fillParams(@NonNull View root, @NonNull MapActivity mapActivity) {
		boolean useSelectedGpxFile = trackToggleButton.getSelectedItemIndex() == 1;
		getParams().put(KEY_USE_SELECTED_GPX_FILE, String.valueOf(useSelectedGpxFile));
		if (selectedGpxFilePath != null) {
			getParams().put(KEY_GPX_FILE_PATH, selectedGpxFilePath);
		}
		getParams().put(KEY_SIMULATION_SPEEDUP, String.valueOf(speedUpValue));
		getParams().put(KEY_SIMULATION_CUTOFF, String.valueOf(cutOffValue));
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
	                        @NonNull CallbackWithObject<GpxFile> onGpxFileAvailable) {
		OsmandApplication app = mapActivity.getMyApplication();
		if (gpxFilePath.isEmpty()) {
			onGpxFileAvailable.processResult(app.getSavingTrackHelper().getCurrentGpx());
		} else {
			SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(gpxFilePath);
			if (selectedGpxFile != null) {
				onGpxFileAvailable.processResult(selectedGpxFile.getGpxFile());
			} else {
				CallbackWithObject<GpxFile[]> onGpxFileLoaded = gpxFiles -> {
					onGpxFileAvailable.processResult(gpxFiles[0]);
					return true;
				};
				String gpxFileName = Algorithms.getFileWithoutDirs(gpxFilePath);
				File gpxFileDir = new File(gpxFilePath.replace("/" + gpxFileName, ""));
				GpxUiHelper.loadGPXFileInDifferentThread(mapActivity, onGpxFileLoaded, gpxFileDir, null, gpxFileName);
			}
		}
	}

	public void onGpxFileSelected(@NonNull View container, @NonNull MapActivity mapActivity, @NonNull String gpxFilePath) {
		selectedGpxFilePath = gpxFilePath;
		setupTrackToggleButton(container, mapActivity);
	}

	private boolean isNightMode(@NonNull OsmandApplication app) {
		return app.getDaynightHelper().isNightModeForMapControls();
	}
}