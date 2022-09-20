package net.osmand.plus.plugins.antplus;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_ANT_PLUS;
import static net.osmand.plus.views.mapwidgets.WidgetType.ANT_HEART_RATE;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.antplus.antdevices.AntHeartRateDevice;
import net.osmand.plus.plugins.antplus.devices.CommonDevice;
import net.osmand.plus.plugins.antplus.devices.CommonDevice.IPreferenceFactory;
import net.osmand.plus.plugins.antplus.widgets.HeartRateTextWidget;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.fragments.BaseSettingsFragment.SettingsScreenType;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;

import java.util.List;

public class AntPlusPlugin extends OsmandPlugin implements IPreferenceFactory {

	private MapActivity mapActivity;
	private final Devices devices;

	public AntPlusPlugin(OsmandApplication app) {
		super(app);

		devices = new Devices(app, this);
	}

	@Override
	public String getId() {
		return PLUGIN_ANT_PLUS;
	}

	@Override
	public String getName() {
		return app.getString(R.string.antplus_plugin_name);
	}

	@Override
	public CharSequence getDescription() {
		return app.getString(R.string.antplus_plugin_description);
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_action_laptop;
	}

	@Override
	public Drawable getAssetResourceImage() {
		return app.getUIUtilities().getIcon(R.drawable.osmand_development);
	}

	@Override
	public SettingsScreenType getSettingsScreenType() {
		return SettingsScreenType.ANT_PLUS_SETTINGS;
	}

	@Nullable
	<T extends CommonDevice<?>> T getDevice(@NonNull Class<T> clz) {
		return devices.getDevice(clz);
	}

	@Override
	public void mapActivityCreate(@NonNull MapActivity activity) {
		super.mapActivityCreate(activity);
		this.mapActivity = activity;
		devices.setActivity(activity);
		devices.connectAntDevices(activity);
	}

	@Override
	public void mapActivityDestroy(@NonNull MapActivity activity) {
		devices.disconnectAntDevices();
		devices.setActivity(null);
		this.mapActivity = null;
		super.mapActivityDestroy(activity);
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);

		if (enabled) {
			devices.connectAntDevices(mapActivity);
		} else {
			devices.disconnectAntDevices();
		}
	}

	@Override
	public void createWidgets(@NonNull MapActivity mapActivity, @NonNull List<MapWidgetInfo> widgetsInfos, @NonNull ApplicationMode appMode) {
		MapWidgetRegistry widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
		MapWidget widget = createMapWidgetForParams(mapActivity, ANT_HEART_RATE);
		widgetsInfos.add(widgetRegistry.createWidgetInfo(widget, appMode));
	}

	@Override
	protected MapWidget createMapWidgetForParams(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType) {
		if (widgetType == ANT_HEART_RATE) {
			AntHeartRateDevice heartRateDevice = devices.getAntDevice(AntHeartRateDevice.class);
			return heartRateDevice != null ? new HeartRateTextWidget(mapActivity, heartRateDevice) : null;
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
