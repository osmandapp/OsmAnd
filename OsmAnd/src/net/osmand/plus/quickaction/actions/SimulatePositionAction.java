package net.osmand.plus.quickaction.actions;


import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.osmand.CallbackWithObject;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.plus.OsmAndLocationSimulation;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.quickaction.CreateEditActionDialog;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.quickaction.SelectTrackFileDialogFragment;
import net.osmand.plus.track.helpers.GPXDatabase;
import net.osmand.plus.track.helpers.GpxDbHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.multistatetoggle.RadioItem;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.util.Algorithms;

import java.io.File;

public class SimulatePositionAction extends QuickAction implements CreateEditActionDialog.FileSelected {
	public static final QuickActionType TYPE = new QuickActionType(45, "simulation.position", SimulatePositionAction.class)
			.nameRes(R.string.simulate_your_position)
			.iconRes(R.drawable.ic_action_simulate_position).nonEditable()
			.category(QuickActionType.NAVIGATION)
			.nameActionRes(R.string.quick_action_start_stop_title);

	public static final String KEY_USE_SELECTED_GPX_FILE = "use_selected_gpx_file";
	public static final String KEY_GPX_FILE_PATH = "gpx_file_path";
	private transient String selectedGpxFilePath;

	private transient TextToggleButton trackToggleButton;


	public SimulatePositionAction() {
		super(TYPE);
	}

	public SimulatePositionAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		OsmAndLocationSimulation sim = mapActivity.getMyApplication().getLocationProvider().getLocationSimulation();
		if (sim.isRouteAnimating()) {
			sim.stop();
		} else {
			unselectGpxFileIfMissing();

			if (!shouldUseSelectedGpxFile()) {
				CallbackWithObject<GPXFile[]> callbackWithObject = result -> {
					startSimulation(mapActivity, result[0]);
					return true;
				};
				GpxUiHelper.selectSingleGPXFile(mapActivity, true, callbackWithObject);

			} else {
				getGpxFile(getSelectedGpxFilePath(true), mapActivity, gpxFile -> {
					startSimulation(mapActivity, gpxFile);
					return true;
				});
			}
		}
	}

	private void startSimulation(MapActivity mapActivity, GPXFile gpxFile) {
		OsmandApplication app = mapActivity.getMyApplication();
		OsmAndLocationSimulation sim = app.getLocationProvider().getLocationSimulation();
		sim.startAnimationThread(app, OsmAndLocationSimulation.getSimulatedLocationsForGpx(app, 0, gpxFile),
				true, (float) app.getSettings().SIMULATE_POSITION_SPEED.get());
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		View root = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_simulate_position, parent, false);
		parent.addView(root);

		unselectGpxFileIfMissing();
		setupTrackToggleButton(root, mapActivity);
	}

	private void setupTrackToggleButton(@NonNull View container, @NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		boolean night = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		LinearLayout trackToggle = container.findViewById(R.id.track_toggle);
		trackToggleButton = new TextToggleButton(app, trackToggle, night);

		TextToggleButton.TextRadioItem alwaysAskButton = new TextToggleButton.TextRadioItem(app.getString(R.string.confirm_every_run));
		TextToggleButton.TextRadioItem selectTrackButton = new TextToggleButton.TextRadioItem(app.getString(R.string.shared_string_select));

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
	private RadioItem.OnRadioItemClickListener getOnTrackToggleButtonClicked(@NonNull View container, boolean alwaysAsk,
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
			GPXDatabase.GpxDataItem gpxDataItem = app.getGpxDbHelper().getItem(file, new GpxDbHelper.GpxDataItemCallback() {
				@Override
				public boolean isCancelled() {
					return false;
				}

				@Override
				public void onGpxDataItemReady(GPXDatabase.GpxDataItem item) {
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

		TextView title = trackInfoContainer.findViewById(R.id.title);
		title.setText(gpxName);
		title.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);

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
		SelectTrackFileDialogFragment.showInstance(mapActivity.getSupportFragmentManager(), getDialog(mapActivity));
	}

	@Nullable
	private CreateEditActionDialog getDialog(@NonNull MapActivity mapActivity) {
		Fragment fragment = mapActivity.getFragment(CreateEditActionDialog.TAG);
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

	@Override
	public boolean isActionWithSlash(@NonNull OsmandApplication app) {
		return app.getLocationProvider().getLocationSimulation().isRouteAnimating();

	}

	public void onGpxFileSelected(@NonNull View container, @NonNull MapActivity mapActivity, @NonNull String gpxFilePath) {
		selectedGpxFilePath = gpxFilePath;
		setupTrackToggleButton(container, mapActivity);
	}
}
