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
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.router.GeneralRouter;
import net.osmand.util.Algorithms;

import java.util.List;

public class MapRouteCalculationProgressListener implements RouteCalculationProgressListener {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final RoutingHelper routingHelper;
	private final MapActivity activity;

	public MapRouteCalculationProgressListener(@NonNull MapActivity activity) {
		this.activity = activity;
		this.app = activity.getMyApplication();
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
		});
	}

	@Override
	public void onRequestPrivateAccessRouting() {
		app.runInUIThread(() -> {
			ApplicationMode routingProfile = routingHelper.getAppMode();
			if (AndroidUtils.isActivityNotDestroyed(activity)
					&& !settings.FORCE_PRIVATE_ACCESS_ROUTING_ASKED.getModeValue(routingProfile)) {
				List<ApplicationMode> modes = ApplicationMode.values(app);
				for (ApplicationMode mode : modes) {
					if (!getAllowPrivatePreference(mode).getModeValue(mode)) {
						settings.FORCE_PRIVATE_ACCESS_ROUTING_ASKED.setModeValue(mode, true);
					}
				}
				OsmandPreference<Boolean> allowPrivate = getAllowPrivatePreference(routingProfile);
				if (!allowPrivate.getModeValue(routingProfile)) {
					AlertDialog.Builder dlg = new AlertDialog.Builder(activity);
					dlg.setMessage(R.string.private_access_routing_req);
					dlg.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> {
						for (ApplicationMode mode : modes) {
							OsmandPreference<Boolean> preference = getAllowPrivatePreference(mode);
							if (!preference.getModeValue(mode)) {
								preference.setModeValue(mode, true);
							}
						}
						routingHelper.onSettingsChanged(null, true);
					});
					dlg.setNegativeButton(R.string.shared_string_no, null);
					dlg.show();
				}
			}
		});
	}

	@NonNull
	private OsmandPreference<Boolean> getAllowPrivatePreference(@NonNull ApplicationMode appMode) {
		String derivedProfile = appMode.getDerivedProfile();
		CommonPreference<Boolean> allowPrivate =
				settings.getCustomRoutingBooleanProperty(GeneralRouter.ALLOW_PRIVATE, false);
		CommonPreference<Boolean> allowPrivateForTruck =
				settings.getCustomRoutingBooleanProperty(GeneralRouter.ALLOW_PRIVATE_FOR_TRUCK, false);
		return Algorithms.objectEquals(derivedProfile, "truck") ? allowPrivateForTruck : allowPrivate;
	}

	@Override
	public void onCalculationFinish() {
		app.runInUIThread(() -> {
			activity.getMapRouteInfoMenu().routeCalculationFinished();
			activity.getDashboard().routeCalculationFinished();

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