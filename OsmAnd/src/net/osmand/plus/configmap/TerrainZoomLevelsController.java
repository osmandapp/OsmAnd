package net.osmand.plus.configmap;

import static net.osmand.plus.dashboard.DashboardType.TERRAIN;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.plugins.srtm.SRTMPlugin;

public class TerrainZoomLevelsController extends ZoomLevelsController {

	private final SRTMPlugin plugin;
	private boolean applyChanges = false;

	public TerrainZoomLevelsController(@NonNull OsmandApplication app,
	                                   @NonNull SRTMPlugin plugin) {
		super(app, createInitialLimits(plugin), createSupportedLimits());
		this.plugin = plugin;
	}

	@Override
	public void onCloseScreen(@NonNull MapActivity activity) {
		setSavedZoomLimits(applyChanges ? selectedLimits : initialLimits);

		activity.getSupportFragmentManager().popBackStack();
		activity.getDashboard().setDashboardVisibility(true, TERRAIN, false);
	}

	@Override
	public void onResetToDefault() {
		plugin.resetZoomLevelsToDefault();
		selectedLimits = getSavedZoomLimits();
	}

	@Override
	public void onApplyChanges() {
		applyChanges = true;
		selectedLimits = getSavedZoomLimits();
	}

	@Override
	protected void setSavedZoomLimits(@NonNull Limits<Integer> limits) {
		plugin.setTerrainZoomValues(limits.min(), limits.max(), plugin.getTerrainMode());
	}

	@Override
	@NonNull
	protected Limits<Integer> getSavedZoomLimits() {
		return new Limits<>(plugin.getTerrainMinZoom(), plugin.getTerrainMaxZoom());
	}

	@NonNull
	private static Limits<Integer> createInitialLimits(@NonNull SRTMPlugin plugin) {
		return new Limits<>(plugin.getTerrainMinZoom(), plugin.getTerrainMaxZoom());
	}

	@NonNull
	private static Limits<Integer> createSupportedLimits() {
		return new Limits<>(SRTMPlugin.TERRAIN_MIN_SUPPORTED_ZOOM, SRTMPlugin.TERRAIN_MAX_SUPPORTED_ZOOM);
	}

	public static void showDialog(@NonNull FragmentActivity activity, @NonNull SRTMPlugin plugin) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		TerrainZoomLevelsController controller = new TerrainZoomLevelsController(app, plugin);

		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(controller.getProcessId(), controller);

		FragmentManager manager = activity.getSupportFragmentManager();
		ZoomLevelsFragment.showInstance(manager);
	}
}
