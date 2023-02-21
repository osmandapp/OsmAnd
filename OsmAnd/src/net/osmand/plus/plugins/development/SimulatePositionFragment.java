package net.osmand.plus.plugins.development;

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
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.IndexConstants;
import net.osmand.gpx.GPXFile;
import net.osmand.plus.DialogListItemAdapter;
import net.osmand.plus.OsmAndLocationSimulation;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class SimulatePositionFragment extends BaseOsmAndFragment {
	public static final String TAG = SimulatePositionFragment.class.getSimpleName();

	private OsmandApplication app;
	private OsmandSettings settings;

	private boolean nightMode;
	private Toolbar toolbar;
	private ImageView navigationIcon;

	private View view;
	private AppCompatImageView trackIcon;
	private AppCompatImageView speedIcon;
	private AppCompatImageView startIcon;
	private LinearLayout speedButton;
	private LinearLayout startButton;

	@Nullable
	private static GPXFile gpxFile = null;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		settings = app.getSettings();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		nightMode = !settings.isLightContent();
		view = UiUtilities.getInflater(getContext(), nightMode).inflate(R.layout.simulate_position_fragment, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);

		toolbar = view.findViewById(R.id.toolbar);
		navigationIcon = toolbar.findViewById(R.id.close_button);

		setupToolbar();
		setupCard();

		return view;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		OsmAndLocationSimulation sim = app.getLocationProvider().getLocationSimulation();
		if (!sim.isRouteAnimating()) {
			gpxFile = null;
		}
	}

	private void setupCard() {
		trackIcon = view.findViewById(R.id.track_icon);
		speedIcon = view.findViewById(R.id.speed_icon);
		startIcon = view.findViewById(R.id.start_icon);

		LinearLayout trackButton = view.findViewById(R.id.track_button_container);
		trackIcon.getDrawable().setTint(ColorUtilities.getDefaultIconColor(app, nightMode));
		trackButton.setOnClickListener(view -> GpxUiHelper.selectGPXFile(requireActivity(), false, false, result -> {
			gpxFile = result[0];
			updateCard();
			return true;
		}, nightMode));

		speedButton = view.findViewById(R.id.speed_button_container);
		speedButton.setOnClickListener(view -> showMovementSpeedDialog(requireActivity(), nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme, nightMode));

		startButton = view.findViewById(R.id.start_button_container);
		startButton.setOnClickListener(view -> {
			startStopSimulation();
		});

		updateCard();
	}

	private void startStopSimulation() {
		OsmAndLocationSimulation sim = app.getLocationProvider().getLocationSimulation();
		if (!sim.isRouteAnimating()) {
			if (gpxFile != null) {
				sim.startAnimationThread(app, OsmAndLocationSimulation.getSimulatedLocationsForGpx(app, 0, gpxFile),
						true, (float) app.getSettings().SIMULATE_POSITION_SPEED.get());
			}
		} else {
			sim.stop();
		}
		updateCard();
	}

	public void setGpxFile(OsmandApplication app, GPXFile gpxFile) {
		OsmAndLocationSimulation sim = app.getLocationProvider().getLocationSimulation();
		if (sim.isRouteAnimating() && SimulatePositionFragment.gpxFile != gpxFile) {
			sim.stop();
		}
		SimulatePositionFragment.gpxFile = gpxFile;
	}

	private void updateCard() {
		trackIcon.getDrawable().setTint(gpxFile != null
				? ColorUtilities.getActiveIconColor(app, nightMode)
				: ColorUtilities.getDefaultIconColor(app, nightMode));

		if (gpxFile != null) {
			TextView trackDescription = view.findViewById(R.id.track_description);
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

			speedButton.setEnabled(true);
			startButton.setEnabled(true);
		} else {
			speedButton.setEnabled(false);
			startButton.setEnabled(false);
		}


		TextView speedDescription = view.findViewById(R.id.speed_description);
		AndroidUiHelper.updateVisibility(speedDescription, true);
		speedDescription.setText(getSpeedString(app.getSettings().SIMULATE_POSITION_SPEED.get()));

		speedIcon.getDrawable().setTint(gpxFile != null
				? ColorUtilities.getActiveIconColor(app, nightMode)
				: ColorUtilities.getSecondaryIconColor(app, nightMode));

		OsmAndLocationSimulation sim = app.getLocationProvider().getLocationSimulation();

		TextView startDescription = view.findViewById(R.id.start_description);

		if (sim.isRouteAnimating()) {
			AndroidUiHelper.updateVisibility(startDescription, true);
			startDescription.setText(getString(R.string.shared_string_in_progress));
		} else {
			AndroidUiHelper.updateVisibility(startDescription, false);
		}

		startIcon.setImageDrawable(app.getUIUtilities().getPaintedIcon(sim.isRouteAnimating() ? R.drawable.ic_action_rec_stop : R.drawable.ic_play_dark,
				gpxFile != null ? ColorUtilities.getActiveIconColor(app, nightMode) : ColorUtilities.getSecondaryIconColor(app, nightMode)));
	}

	protected void showMovementSpeedDialog(Activity activity, int themeRes, boolean nightMode) {
		OsmandSettings settings = app.getSettings();
		int selectedProfileColor = settings.APPLICATION_MODE.get().getProfileColor(nightMode);

		AlertDialog.Builder b = new AlertDialog.Builder(new ContextThemeWrapper(activity, themeRes));
		b.setTitle(R.string.movement_speed);
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
					app.getSettings().SIMULATE_POSITION_SPEED.set(txtValues[which]);
					OsmAndLocationSimulation sim = app.getLocationProvider().getLocationSimulation();
					if (sim.isRouteAnimating()) {
						sim.stop();
					}
					updateCard();
				});
		b.setAdapter(dialogAdapter, null);
		dialogAdapter.setDialog(b.show());
	}

	private String getSpeedString(int speed) {
		return speed == 1 ? getString(R.string.shared_string_original) : "x" + speed;
	}

	private void setupToolbar() {
		TextView tvTitle = toolbar.findViewById(R.id.toolbar_title);
		tvTitle.setText(R.string.simulate_your_location);

		updateToolbarNavigationIcon();

		AndroidUiHelper.updateVisibility(toolbar.findViewById(R.id.toolbar_subtitle), false);
	}

	private void updateToolbarNavigationIcon() {
		navigationIcon.setOnClickListener(view -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), !nightMode);
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	public static void showInstance(@NonNull FragmentActivity activity) {
		showInstance(activity, null);
	}

	public static void showInstance(@NonNull FragmentActivity activity, @Nullable GPXFile gpxFile) {
		FragmentManager fm = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fm, TAG)) {

			SimulatePositionFragment fragment = new SimulatePositionFragment();
			if (gpxFile != null) {
				fragment.setGpxFile((OsmandApplication) activity.getApplication(), gpxFile);
			}
			fm.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
