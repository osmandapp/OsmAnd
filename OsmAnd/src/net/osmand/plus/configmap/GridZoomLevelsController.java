package net.osmand.plus.configmap;

import static net.osmand.plus.dashboard.DashboardType.COORDINATE_GRID;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.views.layers.CoordinatesGridHelper;
import net.osmand.plus.settings.backend.ApplicationMode;

public class GridZoomLevelsController extends ZoomLevelsController {

	private final CoordinatesGridHelper gridHelper;
	private final ApplicationMode appMode;
	private boolean applyChanges = false;

	public GridZoomLevelsController(@NonNull OsmandApplication app,
	                                @NonNull CoordinatesGridHelper gridHelper) {
		super(app, gridHelper.getZoomLevels(), gridHelper.getSupportedZoomLevels());
		this.appMode = app.getSettings().getApplicationMode();
		this.gridHelper = gridHelper;
	}

	@Override
	public void onCloseScreen(@NonNull MapActivity activity) {
		setSavedZoomLimits(applyChanges ? selectedLimits : initialLimits);

		activity.getSupportFragmentManager().popBackStack();
		activity.getDashboard().setDashboardVisibility(true, COORDINATE_GRID, false);
	}

	@Override
	public void onResetToDefault() {
		gridHelper.resetZoomLevels(appMode);
		selectedLimits = getSavedZoomLimits();
	}

	@Override
	public void onApplyChanges() {
		applyChanges = true;
		selectedLimits = getSavedZoomLimits();
	}

	protected void setSavedZoomLimits(@NonNull Limits<Integer> limits) {
		gridHelper.setZoomLevels(appMode, limits);
	}

	@Override
	@NonNull
	protected Limits<Integer> getSavedZoomLimits() {
		return gridHelper.getZoomLevelsWithRestrictions(appMode);
	}

	public static void showDialog(@NonNull FragmentActivity activity,
	                              @NonNull CoordinatesGridHelper gridHelper) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		GridZoomLevelsController controller = new GridZoomLevelsController(app, gridHelper);

		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(controller.getProcessId(), controller);

		FragmentManager manager = activity.getSupportFragmentManager();
		ZoomLevelsFragment.showInstance(manager);
	}
}
