package net.osmand.plus.configmap;

import static net.osmand.plus.dashboard.DashboardType.COORDINATE_GRID;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;

public class GridZoomLevelsController extends ZoomLevelsController {

	public static final int MIN_LIMIT = 4;
	public static final int MAX_LIMIT = 19;

	private final CommonPreference<Integer> minZoomPreference;
	private final CommonPreference<Integer> maxZoomPreference;

	public GridZoomLevelsController(@NonNull OsmandApplication app) {
		super(app, createInitialLimits(app));
		OsmandSettings settings = app.getSettings();
		this.minZoomPreference = settings.COORDINATE_GRID_MIN_ZOOM;
		this.maxZoomPreference = settings.COORDINATE_GRID_MAX_ZOOM;
	}

	@Override
	public void onBackPressed(@NonNull MapActivity mapActivity) {
		mapActivity.getSupportFragmentManager().popBackStack();
		mapActivity.getDashboard().setDashboardVisibility(true, COORDINATE_GRID, false);
	}

	@Override
	@NonNull
	public String getDialogTitle() {
		return getString(R.string.shared_string_zoom_levels);
	}

	@NonNull
	@Override
	public String getDialogSummary() {
		return getString(R.string.terrain_slider_description);
	}

	@Override
	public void onApplyChanges() {
		minZoomPreference.set((int) selectedLimits.min());
		maxZoomPreference.set((int) selectedLimits.max());
	}

	@Override
	public void onResetToDefault() {
		minZoomPreference.resetToDefault();
		maxZoomPreference.resetToDefault();
	}

	@NonNull
	private static Limits createInitialLimits(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		return new Limits(settings.COORDINATE_GRID_MIN_ZOOM.get(), settings.COORDINATE_GRID_MAX_ZOOM.get());
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
