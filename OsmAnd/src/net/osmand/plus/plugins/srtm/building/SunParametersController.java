package net.osmand.plus.plugins.srtm.building;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.dashboard.DashboardType;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.srtm.SRTMPlugin;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;

import java.util.Arrays;
import java.util.List;

public class SunParametersController extends BaseDialogController {

	private static final String PROCESS_ID = "select_sun_parameters";

	private static final int MIN_AZIMUTH = 0;
	private static final int MAX_AZIMUTH = 360;
	private static final int MIN_ALTITUDE = 0;
	private static final int MAX_ALTITUDE = 90;

	private final SRTMPlugin plugin;
	/* the screen the panel was opened from: 3D Relief or 3D buildings */
	private final DashboardType returnTo;
	private final int initialAzimuth;
	private final int initialAltitude;
	private final SunLightMode initialMode;
	private final long initialTime;

	private int azimuth;
	private int altitude;
	private SunLightMode mode;
	private long time;
	private boolean applyChanges = false;

	public SunParametersController(@NonNull OsmandApplication app, @NonNull SRTMPlugin plugin,
	                               @NonNull DashboardType returnTo) {
		super(app);
		this.plugin = plugin;
		this.returnTo = returnTo;
		this.initialAzimuth = plugin.HILLSHADE_SUN_AZIMUTH.get();
		this.initialAltitude = plugin.HILLSHADE_SUN_ANGLE.get();
		this.azimuth = initialAzimuth;
		this.altitude = initialAltitude;
		this.initialMode = plugin.SUN_LIGHT_MODE.get();
		this.initialTime = plugin.SUN_LIGHT_TIME.get();
		this.mode = initialMode;
		this.time = initialTime;
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	@NonNull
	public DashboardType getReturnTo() {
		return returnTo;
	}

	public void setAzimuth(int value) {
		azimuth = value;
		plugin.HILLSHADE_SUN_AZIMUTH.set(value);
	}

	public void setAltitude(int value) {
		altitude = value;
		plugin.HILLSHADE_SUN_ANGLE.set(value);
	}

	@NonNull
	public SunLightMode getMode() {
		return mode;
	}

	public void setMode(@NonNull SunLightMode mode) {
		this.mode = mode;
		plugin.SUN_LIGHT_MODE.set(mode);
	}

	/** Date and time of {@link SunLightMode#DATE}, now until one is chosen. */
	public long getTime() {
		return plugin.getSunLight().getDateTime();
	}

	public void setTime(long time) {
		this.time = time;
		plugin.SUN_LIGHT_TIME.set(time);
	}

	@Nullable
	public SunLight.Angles getSunAngles() {
		return plugin.getSunLight().getAngles();
	}

	public void onApplyChanges() {
		applyChanges = true;
	}

	@NonNull
	public List<OsmandPreference<?>> getPreviewPreferences() {
		return Arrays.asList(plugin.HILLSHADE_SUN_AZIMUTH, plugin.HILLSHADE_SUN_ANGLE,
				plugin.SUN_LIGHT_MODE, plugin.SUN_LIGHT_TIME);
	}

	@Override
	public boolean finishProcessIfNeeded(@Nullable FragmentActivity activity) {
		if (super.finishProcessIfNeeded(activity)) {
			setAzimuth(applyChanges ? azimuth : initialAzimuth);
			setAltitude(applyChanges ? altitude : initialAltitude);
			setTime(applyChanges ? time : initialTime);
			setMode(applyChanges ? mode : initialMode);
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
		return initialAzimuth != azimuth || initialAltitude != altitude
				|| initialMode != mode || initialTime != time;
	}

	public static void showDialog(@NonNull OsmandApplication app, @NonNull FragmentManager manager,
	                              @NonNull DashboardType returnTo) {
		SRTMPlugin plugin = PluginsHelper.getPlugin(SRTMPlugin.class);
		if (plugin != null) {
			DialogManager dialogManager = app.getDialogManager();
			dialogManager.register(PROCESS_ID, new SunParametersController(app, plugin, returnTo));

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