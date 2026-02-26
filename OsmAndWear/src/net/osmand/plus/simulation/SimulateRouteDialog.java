package net.osmand.plus.simulation;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.slider.Slider;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.track.GpxDialogs;
import net.osmand.plus.utils.UiUtilities;

public class SimulateRouteDialog {

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @NonNull OsmAndLocationSimulation simulation,
	                                @Nullable Runnable runnable) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(activity, nightMode));
		builder.setTitle(R.string.animate_route);

		View view = activity.getLayoutInflater().inflate(R.layout.simulate_route, null);
		((TextView) view.findViewById(R.id.MinSpeedup)).setText("1");
		((TextView) view.findViewById(R.id.MaxSpeedup)).setText("4");

		Slider speedup = view.findViewById(R.id.Speedup);
		speedup.setValueTo(3);

		ApplicationMode appMode = app.getSettings().getApplicationMode();
		int profileColor = appMode.getProfileColor(nightMode);
		UiUtilities.setupSlider(speedup, nightMode, profileColor, true);

		builder.setView(view);
		builder.setPositiveButton(R.string.shared_string_ok, (dialog, which) -> {
			boolean mode = app.getDaynightHelper().isNightMode(activity instanceof MapActivity);
			GpxDialogs.selectGPXFile(activity, false, false, result -> {
				simulation.startSimulationThread(app, result[0], 0, true, speedup.getValue() + 1);
				if (runnable != null) {
					runnable.run();
				}
				return true;
			}, mode);
		});
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.show();
	}
}