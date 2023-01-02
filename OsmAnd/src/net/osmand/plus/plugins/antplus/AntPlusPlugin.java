package net.osmand.plus.plugins.antplus;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_ANT_PLUS;
import static net.osmand.plus.views.mapwidgets.WidgetType.ANT_BICYCLE_CADENCE;
import static net.osmand.plus.views.mapwidgets.WidgetType.ANT_BICYCLE_DISTANCE;
import static net.osmand.plus.views.mapwidgets.WidgetType.ANT_BICYCLE_POWER;
import static net.osmand.plus.views.mapwidgets.WidgetType.ANT_BICYCLE_SPEED;
import static net.osmand.plus.views.mapwidgets.WidgetType.ANT_HEART_RATE;

import android.app.Activity;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.antplus.antdevices.AntBikeCadenceDevice;
import net.osmand.plus.plugins.antplus.antdevices.AntBikeDistanceDevice;
import net.osmand.plus.plugins.antplus.antdevices.AntBikePowerDevice;
import net.osmand.plus.plugins.antplus.antdevices.AntBikeSpeedDevice;
import net.osmand.plus.plugins.antplus.antdevices.AntHeartRateDevice;
import net.osmand.plus.plugins.antplus.devices.CommonDevice;
import net.osmand.plus.plugins.antplus.devices.CommonDevice.IPreferenceFactory;
import net.osmand.plus.plugins.antplus.widgets.BikeCadenceTextWidget;
import net.osmand.plus.plugins.antplus.widgets.BikeDistanceTextWidget;
import net.osmand.plus.plugins.antplus.widgets.BikePowerTextWidget;
import net.osmand.plus.plugins.antplus.widgets.BikeSpeedTextWidget;
import net.osmand.plus.plugins.antplus.widgets.HeartRateTextWidget;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetInfoCreator;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;

import org.apache.commons.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class AntPlusPlugin extends OsmandPlugin implements IPreferenceFactory {

	private static final Log log = PlatformUtil.getLog(AntPlusPlugin.class);

	private final DevicesHelper devicesHelper;

	public AntPlusPlugin(OsmandApplication app) {
		super(app);
		devicesHelper = new DevicesHelper(app, this);
	}

	@Override
	public String getId() {
		return PLUGIN_ANT_PLUS;
	}

	@Override
	public String getName() {
		return app.getString(R.string.external_sensors_plugin_name);
	}

	@Override
	public boolean isPaid() {
		return true;
	}

	@Override
	public boolean isLocked() {
		return !Version.isPaidVersion(app);
	}

	@Override
	public CharSequence getDescription() {
		return app.getString(R.string.external_sensors_plugin_description);
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_action_external_sensor;
	}

	@Override
	public Drawable getAssetResourceImage() {
		return app.getUIUtilities().getIcon(R.drawable.osmand_development);
	}

	@Nullable
	@Override
	public SettingsScreenType getSettingsScreenType() {
		return SettingsScreenType.ANT_PLUS_SETTINGS;
	}

	@Nullable
	@Override
	public OsmAndFeature getOsmAndFeature() {
		return OsmAndFeature.EXTERNAL_SENSORS_SUPPORT;
	}

	@Nullable
	<T extends CommonDevice<?>> T getDevice(@NonNull Class<T> clz) {
		return devicesHelper.getDevice(clz);
	}

	@NonNull
	List<CommonDevice<?>> getDevices() {
		return devicesHelper.getDevices();
	}

	@Override
	protected void attachAdditionalInfoToRecordedTrack(Location location, JSONObject json) {
		for (CommonDevice<?> device : devicesHelper.getDevices()) {
			if (device.isEnabled() && device.shouldWriteGpx() && device.getAntDevice().isConnected()) {
				try {
					device.writeDataToJson(json);
				} catch (JSONException e) {
					log.error(e);
				}
			}
		}
	}

	@Override
	public void mapActivityCreate(@NonNull MapActivity activity) {
		devicesHelper.setActivity(activity);
	}

	@Override
	public void mapActivityDestroy(@NonNull MapActivity activity) {
		devicesHelper.setActivity(null);
	}

	@Override
	public boolean init(@NonNull OsmandApplication app, Activity activity) {
		devicesHelper.setActivity(activity);
		devicesHelper.connectAntDevices(activity);
		return true;
	}

	@Override
	public void disable(@NonNull OsmandApplication app) {
		super.disable(app);
		devicesHelper.disconnectAntDevices();
	}

	@Override
	public void createWidgets(@NonNull MapActivity mapActivity, @NonNull List<MapWidgetInfo> widgetsInfos, @NonNull ApplicationMode appMode) {
		WidgetInfoCreator creator = new WidgetInfoCreator(app, appMode);

		MapWidget heartRateWidget = createMapWidgetForParams(mapActivity, ANT_HEART_RATE);
		widgetsInfos.add(creator.createWidgetInfo(heartRateWidget));

		MapWidget bikePowerWidget = createMapWidgetForParams(mapActivity, ANT_BICYCLE_POWER);
		widgetsInfos.add(creator.createWidgetInfo(bikePowerWidget));

		MapWidget bikeCadenceWidget = createMapWidgetForParams(mapActivity, ANT_BICYCLE_CADENCE);
		widgetsInfos.add(creator.createWidgetInfo(bikeCadenceWidget));

		MapWidget bikeSpeedWidget = createMapWidgetForParams(mapActivity, ANT_BICYCLE_SPEED);
		widgetsInfos.add(creator.createWidgetInfo(bikeSpeedWidget));

		MapWidget bikeDistanceWidget = createMapWidgetForParams(mapActivity, ANT_BICYCLE_DISTANCE);
		widgetsInfos.add(creator.createWidgetInfo(bikeDistanceWidget));
	}

	@Override
	protected MapWidget createMapWidgetForParams(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType) {
		switch (widgetType) {
			case ANT_HEART_RATE:
				AntHeartRateDevice heartRateDevice = devicesHelper.getAntDevice(AntHeartRateDevice.class);
				return heartRateDevice != null ? new HeartRateTextWidget(mapActivity, heartRateDevice) : null;
			case ANT_BICYCLE_POWER:
				AntBikePowerDevice powerDevice = devicesHelper.getAntDevice(AntBikePowerDevice.class);
				return powerDevice != null ? new BikePowerTextWidget(mapActivity, powerDevice) : null;
			case ANT_BICYCLE_CADENCE:
				AntBikeCadenceDevice cadenceDevice = devicesHelper.getAntDevice(AntBikeCadenceDevice.class);
				return cadenceDevice != null ? new BikeCadenceTextWidget(mapActivity, cadenceDevice) : null;
			case ANT_BICYCLE_SPEED:
				AntBikeSpeedDevice speedDevice = devicesHelper.getAntDevice(AntBikeSpeedDevice.class);
				return speedDevice != null ? new BikeSpeedTextWidget(mapActivity, speedDevice) : null;
			case ANT_BICYCLE_DISTANCE:
				AntBikeDistanceDevice distanceDevice = devicesHelper.getAntDevice(AntBikeDistanceDevice.class);
				return distanceDevice != null ? new BikeDistanceTextWidget(mapActivity, distanceDevice) : null;
		}
		return null;
	}

	@Override
	public CommonPreference<Boolean> registerBooleanPref(@NonNull String prefId, boolean defValue) {
		return registerBooleanPreference(prefId, defValue).makeGlobal().makeShared();
	}

	@Override
	public CommonPreference<Integer> registerIntPref(@NonNull String prefId, int defValue) {
		return registerIntPreference(prefId, defValue).makeGlobal().makeShared();
	}
}
