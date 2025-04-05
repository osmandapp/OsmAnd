package net.osmand.plus.configmap;

import static net.osmand.plus.dashboard.DashboardType.COORDINATE_GRID;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.views.layers.CoordinatesGridLayerSettings;
import net.osmand.plus.settings.backend.ApplicationMode;

public class GridZoomLevelsController extends ZoomLevelsController {

	private final CoordinatesGridLayerSettings gridLayerSettings;
	private final ApplicationMode appMode;
	private boolean applyChanges = false;

	public GridZoomLevelsController(@NonNull OsmandApplication app,
	                                @NonNull CoordinatesGridLayerSettings gridLayerSettings) {
		super(app, gridLayerSettings.getZoomLevels(), gridLayerSettings.getSupportedZoomLevels());
		this.appMode = app.getSettings().getApplicationMode();
		this.gridLayerSettings = gridLayerSettings;
	}

	@Override
	public void onCloseScreen(@NonNull MapActivity activity) {
		setSavedZoomLimits(applyChanges ? selectedLimits : initialLimits);

		activity.getSupportFragmentManager().popBackStack();
		activity.getDashboard().setDashboardVisibility(true, COORDINATE_GRID, false);
	}

	@Override
	public void onResetToDefault() {
		gridLayerSettings.resetZoomLevels(appMode);
		selectedLimits = getSavedZoomLimits();
	}

	@Override
	public void onApplyChanges() {
		applyChanges = true;
		selectedLimits = getSavedZoomLimits();
	}

	protected void setSavedZoomLimits(@NonNull Limits<Integer> limits) {
		gridLayerSettings.setZoomLevels(appMode, limits);
	}

	@Override
	@NonNull
	protected Limits<Integer> getSavedZoomLimits() {
		return gridLayerSettings.getZoomLevelsWithRestrictions(appMode);
	}

	public static void showDialog(@NonNull FragmentActivity activity,
	                              @NonNull CoordinatesGridLayerSettings gridLayerSettings) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		GridZoomLevelsController controller = new GridZoomLevelsController(app, gridLayerSettings);

		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(controller.getProcessId(), controller);

		FragmentManager manager = activity.getSupportFragmentManager();
		ZoomLevelsFragment.showInstance(manager);
	}
}
