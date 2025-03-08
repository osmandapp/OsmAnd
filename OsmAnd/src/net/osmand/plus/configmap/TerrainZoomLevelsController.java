package net.osmand.plus.configmap;

import static net.osmand.plus.dashboard.DashboardType.TERRAIN;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.plugins.srtm.SRTMPlugin;
import net.osmand.plus.plugins.srtm.TerrainZoomLevelsFragment;

public class TerrainZoomLevelsController extends ZoomLevelsController {

	private final SRTMPlugin plugin;

	public TerrainZoomLevelsController(@NonNull OsmandApplication app,
	                                   @NonNull SRTMPlugin plugin) {
		super(app, createInitialLimits(plugin), createSupportedLimits());
		this.plugin = plugin;
	}

	@Override
	public void onBackPressed(@NonNull MapActivity mapActivity) {
		mapActivity.getSupportFragmentManager().popBackStack();
		mapActivity.getDashboard().setDashboardVisibility(true, TERRAIN, false);
	}

	@Override
	@NonNull
	public String getDialogTitle() {
		return getString(R.string.shared_string_zoom_levels);
	}

	@NonNull
	@Override
	public String getDialogSummary() {
		return app.getString(R.string.terrain_slider_description);
	}

	@Override
	public void onApplyChanges() {
		plugin.setTerrainZoomValues((int) selectedLimits.min(), (int) selectedLimits.max(), plugin.getTerrainMode());
	}

	@Override
	public void onResetToDefault() {
		plugin.resetZoomLevelsToDefault();
	}

	@NonNull
	private static Limits<Integer> createInitialLimits(@NonNull SRTMPlugin plugin) {
		return new Limits<>(plugin.getTerrainMinZoom(), plugin.getTerrainMaxZoom());
	}

	@NonNull
	private static Limits<Integer> createSupportedLimits() {
		return new Limits<>(SRTMPlugin.TERRAIN_MIN_SUPPORTED_ZOOM, SRTMPlugin.TERRAIN_MAX_SUPPORTED_ZOOM);
	}

	private static int index = 0;

	public static void showDialog(@NonNull FragmentActivity activity, @NonNull SRTMPlugin plugin) {
		if (index % 2 == 0) {
			OsmandApplication app = (OsmandApplication) activity.getApplication();
			TerrainZoomLevelsController controller = new TerrainZoomLevelsController(app, plugin);

			DialogManager dialogManager = app.getDialogManager();
			dialogManager.register(controller.getProcessId(), controller);

			FragmentManager manager = activity.getSupportFragmentManager();
			ZoomLevelsFragment.showInstance(manager);
		} else {
			TerrainZoomLevelsFragment.showInstance(activity.getSupportFragmentManager());
		}
		index++;
	}
}
