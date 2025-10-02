package net.osmand.plus.helpers;

import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RouteCalculationProgressListener;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.AndroidUtils;

public class MapRouteCalculationProgressListener implements RouteCalculationProgressListener {

	public static final String TAG = "route_calculation_progress";

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final RoutingHelper routingHelper;
	private final MapActivity activity;

	public MapRouteCalculationProgressListener(@NonNull MapActivity activity) {
		this.activity = activity;
		this.app = activity.getApp();
		this.settings = app.getSettings();
		this.routingHelper = app.getRoutingHelper();
	}

	@Override
	public void onCalculationStart() {
		app.runInUIThread(() -> {
			ProgressBar progressBar = activity.findViewById(R.id.map_horizontal_progress);
			activity.setupRouteCalculationProgressBar(progressBar);
			activity.getMapRouteInfoMenu().routeCalculationStarted();

			if (routingHelper.isPublicTransportMode() || !routingHelper.isOsmandRouting()) {
				activity.getDashboard().updateRouteCalculationProgress(0);
			}
		});
	}

	@Override
	public void onUpdateCalculationProgress(int progress) {
		app.runInUIThread(() -> {
			activity.getMapRouteInfoMenu().updateRouteCalculationProgress(progress);
			activity.getDashboard().updateRouteCalculationProgress(progress);
			activity.updateProgress(progress);
			app.getDialogManager().notifyOnProgress(TAG, progress);
		});
	}

	@Override
	public void onRequestPrivateAccessRouting() {
		app.runInUIThread(() -> {
			ApplicationMode routingProfile = routingHelper.getAppMode();
			if (AndroidUtils.isActivityNotDestroyed(activity) && !app.getOsmandMap().getMapView().isCarView()
					&& !settings.FORCE_PRIVATE_ACCESS_ROUTING_ASKED.getModeValue(routingProfile)) {
				settings.setPrivateAccessRoutingAsked();
				OsmandPreference<Boolean> allowPrivate = settings.getAllowPrivatePreference(routingProfile);
				if (!allowPrivate.getModeValue(routingProfile)) {
					AlertDialog.Builder dlg = new AlertDialog.Builder(activity);
					dlg.setMessage(R.string.private_access_routing_req);
					dlg.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> {
						settings.setAllowPrivateAccessAllModes(true);
						routingHelper.onSettingsChanged(null, true);
					});
					dlg.setNegativeButton(R.string.shared_string_no, null);
					dlg.show();
				}
			}
		});
	}

	@Override
	public void onCalculationFinish() {
		app.runInUIThread(() -> {
			activity.getMapRouteInfoMenu().routeCalculationFinished();
			activity.getDashboard().routeCalculationFinished();
			app.getDialogManager().notifyOnProgress(TAG, 100);

			ProgressBar progressBar = activity.findViewById(R.id.map_horizontal_progress);
			AndroidUiHelper.updateVisibility(progressBar, false);

			// for voice navigation. (routingAppMode may have changed.)
			ApplicationMode routingAppMode = routingHelper.getAppMode();
			if (routingAppMode != null && settings.AUDIO_MANAGER_STREAM.getModeValue(routingAppMode) != null) {
				activity.setVolumeControlStream(settings.AUDIO_MANAGER_STREAM.getModeValue(routingAppMode));
			}
		});
	}
}