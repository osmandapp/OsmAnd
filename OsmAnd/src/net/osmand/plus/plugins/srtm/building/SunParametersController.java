package net.osmand.plus.plugins.srtm.building;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.srtm.SRTMPlugin;

public class SunParametersController extends BaseDialogController {

	private static final String PROCESS_ID = "select_sun_parameters";

	private static final int MIN_AZIMUTH = 0;
	private static final int MAX_AZIMUTH = 360;
	private static final int MIN_ALTITUDE = 0;
	private static final int MAX_ALTITUDE = 90;

	private final SRTMPlugin plugin;
	private final int initialAzimuth;
	private final int initialAltitude;

	private int azimuth;
	private int altitude;
	private boolean applyChanges = false;

	public SunParametersController(@NonNull OsmandApplication app, @NonNull SRTMPlugin plugin) {
		super(app);
		this.plugin = plugin;
		this.initialAzimuth = plugin.HILLSHADE_SUN_AZIMUTH.get();
		this.initialAltitude = plugin.HILLSHADE_SUN_ANGLE.get();
		this.azimuth = initialAzimuth;
		this.altitude = initialAltitude;
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	public void setAzimuth(int value) {
		azimuth = value;
		plugin.HILLSHADE_SUN_AZIMUTH.set(value);
	}

	public void setAltitude(int value) {
		altitude = value;
		plugin.HILLSHADE_SUN_ANGLE.set(value);
	}

	public void onApplyChanges() {
		applyChanges = true;
	}

	@Override
	public boolean finishProcessIfNeeded(@Nullable FragmentActivity activity) {
		if (super.finishProcessIfNeeded(activity)) {
			setAzimuth(applyChanges ? azimuth : initialAzimuth);
			setAltitude(applyChanges ? altitude : initialAltitude);
			return true;
		}
		return false;
	}

	public float getMinAzimuth() { return MIN_AZIMUTH; }
	public float getMaxAzimuth() { return MAX_AZIMUTH; }

	public float getMinAltitude() { return MIN_ALTITUDE; }
	public float getMaxAltitude() { return MAX_ALTITUDE; }

	public int getValidAzimuth() {
		if (azimuth < MIN_AZIMUTH) return MIN_AZIMUTH;
		return Math.min(azimuth, MAX_AZIMUTH);
	}

	public int getValidAltitude() {
		if (altitude < MIN_ALTITUDE) return MIN_ALTITUDE;
		return Math.min(altitude, MAX_ALTITUDE);
	}

	protected boolean hasChanges() {
		return initialAzimuth != azimuth || initialAltitude != altitude;
	}

	public static void showDialog(@NonNull OsmandApplication app, @NonNull FragmentManager manager) {
		SRTMPlugin plugin = PluginsHelper.getPlugin(SRTMPlugin.class);
		if (plugin != null) {
			DialogManager dialogManager = app.getDialogManager();
			dialogManager.register(PROCESS_ID, new SunParametersController(app, plugin));

			if (!SunParametersFragment.showInstance(manager)) {
				dialogManager.unregister(PROCESS_ID);
			}
		}
	}

	@Nullable
	public static SunParametersController getExistedInstance(@NonNull OsmandApplication app) {
		return (SunParametersController) app.getDialogManager().findController(PROCESS_ID);
	}
}