package net.osmand.plus.plugins.development;

import static net.osmand.plus.OsmAndLocationSimulation.LocationSimulationListener;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.IndexConstants;
import net.osmand.gpx.GPXFile;
import net.osmand.plus.DialogListItemAdapter;
import net.osmand.plus.OsmAndLocationSimulation;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class SimulatePositionFragment extends BaseOsmAndFragment {

	public static final String TAG = SimulatePositionFragment.class.getSimpleName();

	public static final String TRACK_FILE_NAME = "track_file_name";

	private OsmandApplication app;
	private OsmandSettings settings;
	private OsmAndLocationSimulation simulation;

	private ImageView trackIcon;
	private ImageView speedIcon;
	private ImageView startIcon;
	private LinearLayout speedButton;
	private LinearLayout startButton;

	private LinearLayout trackItem;
	private LinearLayout speedItem;
	private LinearLayout startItem;

	@Nullable
	private GPXFile gpxFile;

	private boolean nightMode;
	private boolean usedOnMap;
	private LocationSimulationListener simulationListener;

	public void setGpxFile(@Nullable GPXFile gpxFile) {
		this.gpxFile = gpxFile;
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), true);
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		app = requireMyApplication();
		settings = app.getSettings();
		simulation = app.getLocationProvider().getLocationSimulation();

		if (simulation.isRouteAnimating() && gpxFile == null) {
			gpxFile = simulation.getGpxFile();
		} else if (simulation.isRouteAnimating() && gpxFile != null && simulation.getGpxFile() != gpxFile) {
			simulation.stop();
		} else if (!simulation.isRouteAnimating()) {
			app.getSettings().SIMULATE_POSITION_SPEED.set(1);
		}

		simulationListener = simulating -> app.runInUIThread(SimulatePositionFragment.this::updateCard);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		if (gpxFile != null) {
			outState.putString(TRACK_FILE_NAME, gpxFile.path);
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		nightMode = usedOnMap ? app.getDaynightHelper().isNightModeForMapControls() : !settings.isLightContent();
		View view = UiUtilities.getInflater(getContext(), nightMode).inflate(R.layout.simulate_position_fragment, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);

		if (gpxFile == null && savedInstanceState != null) {
			String path = savedInstanceState.getString(TRACK_FILE_NAME);
			MapActivity mapActivity = (MapActivity) requireActivity();
			TrackMenuFragment.loadSelectedGpxFile(mapActivity, path, false, result -> {
				gpxFile = result.getGpxFile();
				setupCard(view);
				return true;
			});
		}

		setupToolbar(view);
		setupCard(view);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		simulation.addSimulationListener(simulationListener);
	}

	@Override
	public void onPause() {
		super.onPause();
		simulation.removeSimulationListener(simulationListener);
	}

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);

		TextView tvTitle = toolbar.findViewById(R.id.toolbar_title);
		tvTitle.setText(R.string.simulate_your_location);

		toolbar.findViewById(R.id.close_button).setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
		AndroidUiHelper.updateVisibility(toolbar.findViewById(R.id.toolbar_subtitle), false);
	}

	private void setupCard(@NonNull View view) {
		trackItem = view.findViewById(R.id.track);
		speedItem = view.findViewById(R.id.speed);
		startItem = view.findViewById(R.id.start);

		trackIcon = trackItem.findViewById(R.id.icon);
		speedIcon = speedItem.findViewById(R.id.icon);
		startIcon = startItem.findViewById(R.id.icon);

		LinearLayout trackButton = trackItem.findViewById(R.id.button_container);
		trackIcon.getDrawable().setTint(ColorUtilities.getDefaultIconColor(app, nightMode));
		trackButton.setOnClickListener(v -> GpxUiHelper.selectGPXFile(requireActivity(), false, false, result -> {
			gpxFile = result[0];
			updateCard();
			return true;
		}, nightMode));
		TextView trackTextview = trackItem.findViewById(R.id.title);
		trackTextview.setText(R.string.shared_string_gpx_track);

		speedButton = speedItem.findViewById(R.id.button_container);
		speedButton.setOnClickListener(v -> showMovementSpeedDialog(requireActivity(), nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme, nightMode));
		TextView speedTextview = speedItem.findViewById(R.id.title);
		speedTextview.setText(R.string.shared_string_speed);

		AndroidUiHelper.updateVisibility(speedItem.findViewById(R.id.short_divider), false);
		AndroidUiHelper.updateVisibility(speedItem.findViewById(R.id.long_divider), true);

		startButton = startItem.findViewById(R.id.button_container);
		startButton.setOnClickListener(v -> startStopSimulation());

		AndroidUiHelper.updateVisibility(startItem.findViewById(R.id.short_divider), false);

		updateCard();
	}

	private void startStopSimulation() {
		if (!simulation.isRouteAnimating()) {
			if (gpxFile != null) {
				simulation.startAnimationThread(app, gpxFile, 0, true, (float) app.getSettings().SIMULATE_POSITION_SPEED.get());
			}
		} else {
			simulation.stop();
		}
		updateCard();
	}

	private void updateCard() {
		trackIcon.getDrawable().setTint(gpxFile != null
				? ColorUtilities.getActiveIconColor(app, nightMode)
				: ColorUtilities.getDefaultIconColor(app, nightMode));

		boolean enabled = gpxFile != null;
		if (enabled) {
			TextView trackDescription = trackItem.findViewById(R.id.description);
			AndroidUiHelper.updateVisibility(trackDescription, true);
			String name = gpxFile.path;
			int i = name.lastIndexOf('/');
			if (i >= 0) {
				name = name.substring(i + 1);
			}
			if (name.toLowerCase().endsWith(IndexConstants.GPX_FILE_EXT)) {
				name = name.substring(0, name.length() - 4);
			}
			name = name.replace('_', ' ');
			trackDescription.setText(name);
		}
		speedButton.setEnabled(enabled);
		startButton.setEnabled(enabled);

		TextView speedDescription = speedItem.findViewById(R.id.description);
		AndroidUiHelper.updateVisibility(speedDescription, true);
		speedDescription.setText(getSpeedString(app.getSettings().SIMULATE_POSITION_SPEED.get()));

		speedIcon.setImageDrawable(app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_speed,
				gpxFile != null ? ColorUtilities.getActiveIconColor(app, nightMode) : ColorUtilities.getSecondaryIconColor(app, nightMode)));


		TextView startDescription = startItem.findViewById(R.id.description);
		if (simulation.isRouteAnimating()) {
			AndroidUiHelper.updateVisibility(startDescription, true);
			startDescription.setText(getString(R.string.shared_string_in_progress));
		} else {
			AndroidUiHelper.updateVisibility(startDescription, false);
		}

		TextView startTextview = startItem.findViewById(R.id.title);
		startTextview.setText(simulation.isRouteAnimating() ? R.string.shared_string_control_stop : R.string.shared_string_control_start);
		startIcon.setImageDrawable(app.getUIUtilities().getPaintedIcon(simulation.isRouteAnimating() ? R.drawable.ic_action_stop  : R.drawable.ic_play_dark,
				gpxFile != null ? ColorUtilities.getActiveIconColor(app, nightMode) : ColorUtilities.getSecondaryIconColor(app, nightMode)));
	}

	protected void showMovementSpeedDialog(Activity activity, int themeRes, boolean nightMode) {
		int selectedProfileColor = settings.APPLICATION_MODE.get().getProfileColor(nightMode);

		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(activity, themeRes));
		builder.setTitle(R.string.movement_speed);
		int[] txtValues = {1, 2, 3, 4};
		int selectedSpeed = app.getSettings().SIMULATE_POSITION_SPEED.get();
		int selectedSpeedId = -1;
		String[] txtNames = new String[txtValues.length];
		for (int i = 0; i < txtValues.length; i++) {
			if (selectedSpeed == txtValues[i]) {
				selectedSpeedId = i;
			}
			txtNames[i] = getSpeedString(txtValues[i]);
		}
		DialogListItemAdapter dialogAdapter = DialogListItemAdapter.createSingleChoiceAdapter(
				txtNames, nightMode, selectedSpeedId, app, selectedProfileColor, themeRes, v -> {
					int which = (int) v.getTag();
					settings.SIMULATE_POSITION_SPEED.set(txtValues[which]);
					if (simulation.isRouteAnimating()) {
						simulation.stop();
					}
					updateCard();
				});
		builder.setAdapter(dialogAdapter, null);
		dialogAdapter.setDialog(builder.show());
	}

	private String getSpeedString(int speed) {
		return speed == 1 ? getString(R.string.shared_string_original) : "x" + speed;
	}

	public static void showInstance(@NonNull FragmentManager manager, @Nullable GPXFile gpxFile, boolean usedOnMap) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			SimulatePositionFragment fragment = new SimulatePositionFragment();
			fragment.setGpxFile(gpxFile);
			fragment.usedOnMap = usedOnMap;
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
