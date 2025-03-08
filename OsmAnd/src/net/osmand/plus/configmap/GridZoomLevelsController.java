package net.osmand.plus.configmap;

import static net.osmand.plus.dashboard.DashboardType.COORDINATE_GRID;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;

public class GridZoomLevelsController extends ZoomLevelsController {

	public static final int MIN_ZOOM = 4;
	public static final int MAX_ZOOM = 19;

	private final CommonPreference<Integer> minZoomPreference;
	private final CommonPreference<Integer> maxZoomPreference;

	public GridZoomLevelsController(@NonNull OsmandApplication app) {
		super(app, createInitialLimits(app));
		OsmandSettings settings = app.getSettings();
		this.minZoomPreference = settings.COORDINATE_GRID_MIN_ZOOM;
		this.maxZoomPreference = settings.COORDINATE_GRID_MAX_ZOOM;
	}

	@Override
	public void onCloseScreen(@NonNull MapActivity activity) {
		activity.getSupportFragmentManager().popBackStack();
		activity.getDashboard().setDashboardVisibility(true, COORDINATE_GRID, false);
	}

	@Override
	public void onApplyChanges() {
		minZoomPreference.set(selectedLimits.min());
		maxZoomPreference.set(selectedLimits.max());
	}

	@Override
	public void onResetToDefault() {
		minZoomPreference.resetToDefault();
		maxZoomPreference.resetToDefault();
	}

	@NonNull
	private static Limits<Integer> createInitialLimits(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		return new Limits<>(settings.COORDINATE_GRID_MIN_ZOOM.get(), settings.COORDINATE_GRID_MAX_ZOOM.get());
	}

	public static void showDialog(@NonNull FragmentActivity activity) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		GridZoomLevelsController controller = new GridZoomLevelsController(app);

		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(controller.getProcessId(), controller);

		FragmentManager manager = activity.getSupportFragmentManager();
		ZoomLevelsFragment.showInstance(manager);
	}
}
