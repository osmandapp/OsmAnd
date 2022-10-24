package net.osmand.plus.plugins.weather;

import static com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.MeasurementDataType.TEMPERATURE;
import static net.osmand.IndexConstants.WEATHER_INDEX_DIR;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_WEATHER;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.WEATHER_ID;
import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_AIR_PRESSURE_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_CLOUDS_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_PRECIPITATION_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_TEMPERATURE_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_WIND_WIDGET;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.android.MapRendererContext;
import net.osmand.core.jni.BandIndexGeoBandSettingsHash;
import net.osmand.core.jni.GeoBandSettings;
import net.osmand.core.jni.WeatherBand;
import net.osmand.core.jni.WeatherTileResourcesManager;
import net.osmand.core.jni.ZoomLevelDoubleListHash;
import net.osmand.core.jni.ZoomLevelStringListHash;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.chooseplan.button.PurchasingUtils;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.dashboard.DashboardOnMap.DashboardType;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.weather.WeatherRasterLayer.WeatherLayer;
import net.osmand.plus.plugins.weather.units.CloudConstants;
import net.osmand.plus.plugins.weather.units.PrecipConstants;
import net.osmand.plus.plugins.weather.units.PressureConstants;
import net.osmand.plus.plugins.weather.units.TemperatureConstants;
import net.osmand.plus.plugins.weather.units.WindConstants;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper;
import net.osmand.plus.settings.backend.preferences.EnumStringPreference;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.fragments.BaseSettingsFragment.SettingsScreenType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetInfoCreator;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class WeatherPlugin extends OsmandPlugin {

	public final OsmandPreference<Boolean> WX_ENABLED;
	public final ListStringPreference WX_ENABLED_LAYERS;
	public final ListStringPreference WX_LAYERS_TRANSPARENCY;

	public final OsmandPreference<Boolean> WX_CONTOURS_ENABLED;
	public final OsmandPreference<Integer> WX_CONTOURS_TRANSPARENCY;
	public final EnumStringPreference<WeatherInfoType> WX_CONTOURS_TYPE;

	public final EnumStringPreference<TemperatureConstants> WX_UNIT_TEMPERATURE;
	public final EnumStringPreference<PrecipConstants> WX_UNIT_PRECIPITATION;
	public final EnumStringPreference<WindConstants> WX_UNIT_WIND;
	public final EnumStringPreference<CloudConstants> WX_UNIT_CLOUDS;
	public final EnumStringPreference<PressureConstants> WX_UNIT_PRESSURE;

	public static int DEFAULT_TRANSPARENCY = 50;

	private WeatherInfoType currentConfigureLayer = null;

	private WeatherRasterLayer weatherLayerLow;
	private WeatherRasterLayer weatherLayerHigh;
	private static final float ZORDER_RASTER_LOW = 0.8f;
	private static final float ZORDER_RASTER_HIGH = 0.81f;
	private final AtomicInteger bandsSettingsVersion = new AtomicInteger(0);

	public WeatherPlugin(@NonNull OsmandApplication app) {
		super(app);

		ApplicationMode[] noAppMode = {};
		WidgetsAvailabilityHelper.regWidgetVisibility(WEATHER_TEMPERATURE_WIDGET, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(WEATHER_PRECIPITATION_WIDGET, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(WEATHER_WIND_WIDGET, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(WEATHER_CLOUDS_WIDGET, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(WEATHER_AIR_PRESSURE_WIDGET, noAppMode);

		WX_ENABLED = registerBooleanPreference("map_setting_wx_enabled", true).makeProfile();
		WX_ENABLED_LAYERS = (ListStringPreference) registerListStringPreference("map_setting_wx_enabled_layers", null, ",").makeProfile();
		WX_LAYERS_TRANSPARENCY = (ListStringPreference) registerListStringPreference("map_setting_wx_layers_transparency", null, ",").makeProfile();

		WX_CONTOURS_ENABLED = registerBooleanPreference("map_setting_wx_contours_enabled", true).makeProfile();
		WX_CONTOURS_TRANSPARENCY = registerIntPreference("map_setting_wx_contours_transparency", DEFAULT_TRANSPARENCY).makeProfile();
		WX_CONTOURS_TYPE = (EnumStringPreference<WeatherInfoType>) registerEnumStringPreference(
				"map_setting_wx_contours_type", TEMPERATURE, WeatherInfoType.values(), WeatherInfoType.class).makeProfile();

		WX_UNIT_TEMPERATURE = (EnumStringPreference<TemperatureConstants>) registerEnumStringPreference(
				"map_settings_weather_temp", TemperatureConstants.CELSIUS, TemperatureConstants.values(), TemperatureConstants.class).makeProfile();
		WX_UNIT_PRESSURE = (EnumStringPreference<PressureConstants>) registerEnumStringPreference(
				"map_settings_weather_pressure", PressureConstants.MILLIMETERS_OF_MERCURY, PressureConstants.values(), PressureConstants.class).makeProfile();
		WX_UNIT_WIND = (EnumStringPreference<WindConstants>) registerEnumStringPreference(
				"map_settings_weather_wind", WindConstants.METERS_PER_SECOND, WindConstants.values(), WindConstants.class).makeProfile();
		WX_UNIT_CLOUDS = (EnumStringPreference<CloudConstants>) registerEnumStringPreference(
				"map_settings_weather_cloud", CloudConstants.PERCENT, CloudConstants.values(), CloudConstants.class).makeProfile();
		WX_UNIT_PRECIPITATION = (EnumStringPreference<PrecipConstants>) registerEnumStringPreference(
				"map_settings_weather_precip", PrecipConstants.MILIMETERS, PrecipConstants.values(), PrecipConstants.class).makeProfile();
	}

	@Override
	public String getId() {
		return PLUGIN_WEATHER;
	}

	@Override
	public String getName() {
		return app.getString(R.string.shared_string_weather);
	}

	@Override
	public CharSequence getDescription() {
		return app.getString(R.string.weather_plugin_description);
	}

	@Override
	public String getPrefsDescription() {
		return app.getString(R.string.weather_prefs_descr);
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_action_umbrella;
	}

	@Override
	public boolean isPaid() {
		return true;
	}

	@Override
	public boolean isLocked() {
		return !InAppPurchaseHelper.isOsmAndProAvailable(app);
	}

	@Override
	public SettingsScreenType getSettingsScreenType() {
		return SettingsScreenType.WEATHER_SETTINGS;
	}

	@Override
	public void createWidgets(@NonNull MapActivity mapActivity, @NonNull List<MapWidgetInfo> widgetInfos, @NonNull ApplicationMode appMode) {
		WidgetInfoCreator creator = new WidgetInfoCreator(app, appMode);

		MapWidget temperatureWidget = createMapWidgetForParams(mapActivity, WEATHER_TEMPERATURE_WIDGET);
		if (temperatureWidget != null) {
			widgetInfos.add(creator.createWidgetInfo(temperatureWidget));
		}
		MapWidget precipitationWidget = createMapWidgetForParams(mapActivity, WEATHER_PRECIPITATION_WIDGET);
		if (precipitationWidget != null) {
			widgetInfos.add(creator.createWidgetInfo(precipitationWidget));
		}
		MapWidget windWidget = createMapWidgetForParams(mapActivity, WEATHER_WIND_WIDGET);
		if (windWidget != null) {
			widgetInfos.add(creator.createWidgetInfo(windWidget));
		}
		MapWidget cloudsWidget = createMapWidgetForParams(mapActivity, WEATHER_CLOUDS_WIDGET);
		if (cloudsWidget != null) {
			widgetInfos.add(creator.createWidgetInfo(cloudsWidget));
		}
		MapWidget airPressureWidget = createMapWidgetForParams(mapActivity, WEATHER_AIR_PRESSURE_WIDGET);
		if (airPressureWidget != null) {
			widgetInfos.add(creator.createWidgetInfo(airPressureWidget));
		}
	}

	@Nullable
	@Override
	protected MapWidget createMapWidgetForParams(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType) {
		switch (widgetType) {
			case WEATHER_TEMPERATURE_WIDGET:
				return new TemperatureWidget(mapActivity, this);
			case WEATHER_PRECIPITATION_WIDGET:
				return new PrecipitationWidget(mapActivity, this);
			case WEATHER_WIND_WIDGET:
				return new WindWidget(mapActivity, this);
			case WEATHER_CLOUDS_WIDGET:
				return new CloudsWidget(mapActivity, this);
			case WEATHER_AIR_PRESSURE_WIDGET:
				return new AirPressureWidget(mapActivity, this);
		}
		return null;
	}

	@Override
	protected void registerLayerContextMenuActions(@NonNull ContextMenuAdapter adapter,
	                                               @NonNull MapActivity mapActivity,
	                                               @NonNull List<RenderingRuleProperty> customRules) {
		if (isLocked()) {
			PurchasingUtils.createPromoItem(adapter, mapActivity, OsmAndFeature.WEATHER,
					WEATHER_ID,
					R.string.shared_string_weather,
					R.string.explore_weather_forecast);
		} else {
			OsmandApplication app = mapActivity.getMyApplication();
			OsmandSettings settings = app.getSettings();
			ApplicationMode appMode = settings.getApplicationMode();
			ItemClickListener listener = (uiAdapter, view, item, isChecked) -> {
				DashboardOnMap dashboard = mapActivity.getDashboard();
				int[] coordinates = AndroidUtils.getCenterViewCoordinates(view);
				dashboard.setDashboardVisibility(true, DashboardType.WEAHTER, coordinates);
				return false;
			};
			boolean selected = isAnyDataVisible(appMode);
			adapter.addItem(new ContextMenuItem(WEATHER_ID)
					.setTitleId(R.string.shared_string_weather, mapActivity)
					.setDescription(selected ? getWeatherTypesSummary(getEnabledLayers(appMode)) : null)
					.setSecondaryDescription(selected ? app.getString(R.string.shared_string_on) : null)
					.setLayout(R.layout.configure_map_item_with_additional_right_desc)
					.setSelected(selected)
					.setColor(app, selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
					.setIcon(R.drawable.ic_action_umbrella)
					.setListener(listener));
		}
	}

	@Nullable
	public String getWeatherTypesSummary(@NonNull List<WeatherInfoType> types) {
		if (!Algorithms.isEmpty(types)) {
			List<String> titles = new ArrayList<>();
			for (WeatherInfoType layer : WeatherInfoType.values()) {
				if (types.contains(layer)) {
					titles.add(app.getString(layer.getTitleId()));
				}
			}
			return TextUtils.join(", ", titles);
		}
		return null;
	}

	@Override
	public void updateLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();

		// Weather layers available for opengl only
		if (!updateBandsSettings()) {
			return;
		}

		if (isActive()) {
			if (weatherLayerLow == null || weatherLayerHigh == null) {
				createLayers();
			}
			if (!mapView.isLayerExists(weatherLayerLow)) {
				mapView.addLayer(weatherLayerLow, ZORDER_RASTER_LOW);
			}
			if (!mapView.isLayerExists(weatherLayerHigh)) {
				mapView.addLayer(weatherLayerHigh, ZORDER_RASTER_HIGH);
			}
			mapView.refreshMap();
		} else {
			mapView.removeLayer(weatherLayerLow);
			mapView.removeLayer(weatherLayerHigh);
		}
	}

	@Nullable
	public WeatherTileResourcesManager getWeatherResourcesManager() {
		MapRendererContext mapContext = NativeCoreContext.getMapRendererContext();
		return mapContext != null ? mapContext.getWeatherTileResourcesManager() : null;
	}

	public boolean updateBandsSettings() {
		WeatherTileResourcesManager weatherResourcesManager = getWeatherResourcesManager();
		if (weatherResourcesManager == null) {
			return false;
		}

		ApplicationMode appMode = app.getSettings().getApplicationMode();
		Map<WeatherInfoType, Integer> transparencies = getLayersTransparencies(appMode);

		BandIndexGeoBandSettingsHash bandSettings = new BandIndexGeoBandSettingsHash();

		// TODO: read unit, unitFormatGeneral and unitFormatPrecise from OsmAnd settings
		String cloudUnit = "%";
		String cloudUnitFormatGeneral = "%d"; // For countour lines
		String cloudUnitFormatPrecise = "%d"; // For widgets
		Integer cloudTransparencyObj = transparencies.get(WeatherInfoType.CLOUDS);
		float cloudTransparency = cloudTransparencyObj != null ? cloudTransparencyObj / 100f : 0.5f;
		String cloudColorProfilePath = new File(app.getAppPath(WEATHER_INDEX_DIR), "cloud_color.txt").getAbsolutePath();
		GeoBandSettings cloudBandSettings = new GeoBandSettings(cloudUnit, cloudUnitFormatGeneral,
				cloudUnitFormatPrecise, "%", cloudTransparency, cloudColorProfilePath,
				"", new ZoomLevelDoubleListHash(), new ZoomLevelStringListHash());
		bandSettings.set((short) WeatherBand.Cloud.swigValue(), cloudBandSettings);

		String tempUnit = "°C";
		String tempUnitFormatGeneral = "%d";
		String tempUnitFormatPrecise = "%d";
		Integer tempTransparencyObj = transparencies.get(WeatherInfoType.TEMPERATURE);
		float tempTransparency = tempTransparencyObj != null ? tempTransparencyObj / 100f : 0.5f;
		String tempColorProfilePath = new File(app.getAppPath(WEATHER_INDEX_DIR), "temperature_color.txt").getAbsolutePath();
		GeoBandSettings tempBandSettings = new GeoBandSettings(tempUnit, tempUnitFormatGeneral,
				tempUnitFormatPrecise, "°C", tempTransparency, tempColorProfilePath,
				"", new ZoomLevelDoubleListHash(), new ZoomLevelStringListHash());
		bandSettings.set((short) WeatherBand.Temperature.swigValue(), tempBandSettings);

		String pressureUnit = "hPa";
		String pressureUnitFormatGeneral = "%d";
		String pressureUnitFormatPrecise = "%d";
		Integer pressureTransparencyObj = transparencies.get(WeatherInfoType.PRESSURE);
		float pressureTransparency = pressureTransparencyObj != null ? pressureTransparencyObj / 100f : 0.5f;
		String pressureColorProfilePath = new File(app.getAppPath(WEATHER_INDEX_DIR), "pressure_color.txt").getAbsolutePath();
		GeoBandSettings pressureBandSettings = new GeoBandSettings(pressureUnit, pressureUnitFormatGeneral,
				pressureUnitFormatPrecise, "Pa", pressureTransparency, pressureColorProfilePath,
				"", new ZoomLevelDoubleListHash(), new ZoomLevelStringListHash());
		bandSettings.set((short) WeatherBand.Pressure.swigValue(), pressureBandSettings);

		String windUnit = "m/s";
		String windUnitFormatGeneral = "%d";
		String windUnitFormatPrecise = "%d";
		Integer windTransparencyObj = transparencies.get(WeatherInfoType.WIND);
		float windTransparency = windTransparencyObj != null ? windTransparencyObj / 100f : 0.5f;
		String windColorProfilePath = new File(app.getAppPath(WEATHER_INDEX_DIR), "wind_color.txt").getAbsolutePath();
		GeoBandSettings windBandSettings = new GeoBandSettings(windUnit, windUnitFormatGeneral,
				windUnitFormatPrecise, "m/s", windTransparency, windColorProfilePath,
				"", new ZoomLevelDoubleListHash(), new ZoomLevelStringListHash());
		bandSettings.set((short) WeatherBand.WindSpeed.swigValue(), windBandSettings);

		String precipUnit = "mm";
		String precipUnitFormatGeneral = "%d";
		String precipUnitFormatPrecise = "%d";
		Integer precipTransparencyObj = transparencies.get(WeatherInfoType.PRECIPITATION);
		float precipTransparency = precipTransparencyObj != null ? precipTransparencyObj / 100f : 0.5f;
		String precipColorProfilePath = new File(app.getAppPath(WEATHER_INDEX_DIR), "precip_color.txt").getAbsolutePath();
		GeoBandSettings precipBandSettings = new GeoBandSettings(precipUnit, precipUnitFormatGeneral,
				precipUnitFormatPrecise, "kg/(m^2 s)", precipTransparency, precipColorProfilePath,
				"", new ZoomLevelDoubleListHash(), new ZoomLevelStringListHash());
		bandSettings.set((short) WeatherBand.Precipitation.swigValue(), precipBandSettings);

		weatherResourcesManager.setBandSettings(bandSettings);
		bandsSettingsVersion.incrementAndGet();
		return true;
	}

	public int getBandsSettingsVersion() {
		return bandsSettingsVersion.get();
	}

	private void createLayers() {
		weatherLayerLow = new WeatherRasterLayer(app, WeatherLayer.LOW);
		weatherLayerHigh = new WeatherRasterLayer(app, WeatherLayer.HIGH);
	}

	public void setWeatherEnabled(@NonNull ApplicationMode appMode, boolean enable) {
		WX_ENABLED.setModeValue(appMode, enable);
	}

	public boolean isWeatherEnabled(@NonNull ApplicationMode appMode) {
		return WX_ENABLED.getModeValue(appMode);
	}

	public boolean isAnyDataVisible(@NonNull ApplicationMode appMode) {
		boolean isAnyLayerEnabled = !Algorithms.isEmpty(getEnabledLayers(appMode));
		boolean isContoursEnabled = isContoursEnabled(appMode);
		return isWeatherEnabled(appMode) && (isAnyLayerEnabled || isContoursEnabled);
	}

	public boolean isContoursEnabled(@NonNull ApplicationMode appMode) {
		return WX_CONTOURS_ENABLED.getModeValue(appMode);
	}

	public void setContoursEnabled(@NonNull ApplicationMode appMode, boolean enabled) {
		WX_CONTOURS_ENABLED.setModeValue(appMode, enabled);
	}

	public int getContoursTransparency(@NonNull ApplicationMode appMode) {
		Integer value = WX_CONTOURS_TRANSPARENCY.getModeValue(appMode);
		return value != null ? value : DEFAULT_TRANSPARENCY;
	}

	public void setContoursTransparency(@NonNull ApplicationMode appMode, @NonNull Integer transparency) {
		WX_CONTOURS_TRANSPARENCY.setModeValue(appMode, transparency);
	}

	@NonNull
	public WeatherInfoType getSelectedContoursType(@NonNull ApplicationMode appMode) {
		return WX_CONTOURS_TYPE.getModeValue(appMode);
	}

	public void setSelectedContoursType(@NonNull ApplicationMode appMode, @NonNull WeatherInfoType contoursType) {
		WX_CONTOURS_TYPE.setModeValue(appMode, contoursType);
	}

	public void setCurrentConfigureLayer(@Nullable WeatherInfoType layer) {
		this.currentConfigureLayer = layer;
	}

	@Nullable
	public WeatherInfoType getCurrentConfiguredLayer() {
		return currentConfigureLayer;
	}

	@NonNull
	public List<WeatherInfoType> getEnabledLayers(@NonNull ApplicationMode appMode) {
		Set<WeatherInfoType> result = new HashSet<>();
		String storedValue = WX_ENABLED_LAYERS.getModeValue(appMode);
		if (!Algorithms.isEmpty(storedValue)) {
			for (WeatherInfoType type : WeatherInfoType.values()) {
				if (storedValue.contains(type.name())) {
					result.add(type);
				}
			}
		}
		return new ArrayList<>(result);
	}

	public void toggleLayerEnable(@NonNull ApplicationMode appMode, @NonNull WeatherInfoType layer, boolean enable) {
		List<WeatherInfoType> enabledLayers = getEnabledLayers(appMode);
		if (enable) {
			enabledLayers.add(layer);
		} else {
			enabledLayers.remove(layer);
		}
		List<String> valueToSave = new ArrayList<>();
		for (WeatherInfoType type : enabledLayers) {
			valueToSave.add(type.name());
		}
		WX_ENABLED_LAYERS.setModeValues(appMode, valueToSave);
	}

	public boolean isLayerEnabled(@NonNull ApplicationMode appMode, @NonNull WeatherInfoType layer) {
		return getEnabledLayers(appMode).contains(layer);
	}

	public int getLayerTransparency(@NonNull ApplicationMode appMode, @NonNull WeatherInfoType layer) {
		Integer value = getLayersTransparencies(appMode).get(layer);
		return value != null ? value : DEFAULT_TRANSPARENCY;
	}

	public void setLayerTransparency(@NonNull ApplicationMode appMode, @NonNull WeatherInfoType layer, @NonNull Integer transparency) {
		Map<WeatherInfoType, Integer> transparencies = getLayersTransparencies(appMode);
		transparencies.put(layer, transparency);
		List<String> valuesToSave = new ArrayList<>();
		for (Entry<WeatherInfoType, Integer> layerTransp : transparencies.entrySet()) {
			valuesToSave.add(layerTransp.getKey().name() + ":" + layerTransp.getValue());
		}
		WX_LAYERS_TRANSPARENCY.setModeValues(appMode, valuesToSave);
		updateBandsSettings();
	}

	@NonNull
	public Map<WeatherInfoType, Integer> getLayersTransparencies(@NonNull ApplicationMode appMode) {
		Map<WeatherInfoType, Integer> transparencies = new HashMap<>();
		List<String> storedValues = WX_LAYERS_TRANSPARENCY.getStringsListForProfile(appMode);
		if (!Algorithms.isEmpty(storedValues)) {
			for (String value : storedValues) {
				try {
					String[] bundle = value.split(":");
					WeatherInfoType type = WeatherInfoType.valueOf(bundle[0].trim());
					Integer transparency = Integer.parseInt(bundle[1].trim());
					transparencies.put(type, transparency);
				} catch (Exception e) {
					// implement logs
				}
			}
		}
		return transparencies;
	}

	@NonNull
	public Enum<?> getSelectedLayerUnit(@NonNull ApplicationMode appMode, @NonNull WeatherInfoType layer) {
		EnumStringPreference preference = getUnitsPreference(layer);
		return (Enum<?>) preference.getModeValue(appMode);
	}

	public void setSelectedLayerUnit(@NonNull ApplicationMode appMode, @NonNull WeatherInfoType layer, @NonNull Enum<?> value) {
		EnumStringPreference preference = getUnitsPreference(layer);
		preference.setModeValue(appMode, value);
	}

	@Nullable
	public EnumStringPreference getUnitsPreference(@NonNull WeatherInfoType layer) {
		switch (layer) {
			case TEMPERATURE:
				return WX_UNIT_TEMPERATURE;
			case PRESSURE:
				return WX_UNIT_PRESSURE;
			case WIND:
				return WX_UNIT_WIND;
			case CLOUDS:
				return WX_UNIT_CLOUDS;
			case PRECIPITATION:
				return WX_UNIT_PRECIPITATION;
			default:
				return null;
		}
	}

	@NonNull
	public String[] getUnitsPreferencesIds() {
		return new String[] {
				WX_UNIT_TEMPERATURE.getId(),
				WX_UNIT_PRECIPITATION.getId(),
				WX_UNIT_WIND.getId(),
				WX_UNIT_CLOUDS.getId(),
				WX_UNIT_PRESSURE.getId()
		};
	}
}