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
import net.osmand.core.jni.QListDouble;
import net.osmand.core.jni.QStringList;
import net.osmand.core.jni.WeatherTileResourcesManager;
import net.osmand.core.jni.ZoomLevel;
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
import net.osmand.plus.settings.backend.preferences.CommonPreference;
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

	public static final String WEATHER_TEMP_CONTOUR_LINES_ATTR = "weatherTempContours";
	public static final String WEATHER_PRESSURE_CONTOURS_LINES_ATTR = "weatherPressureContours";
	public static final String WEATHER_NONE_CONTOURS_LINES_VALUE = "none";

	public static int DEFAULT_TRANSPARENCY = 50;

	private WeatherInfoType currentConfigureLayer = null;

	private WeatherRasterLayer weatherLayerLow;
	private WeatherRasterLayer weatherLayerHigh;
	private static final float ZORDER_RASTER_LOW = 0.8f;
	private static final float ZORDER_RASTER_HIGH = 0.81f;
	private final AtomicInteger bandsSettingsVersion = new AtomicInteger(0);
	private WeatherContourLayer weatherContourLayer;
	private static final float ZORDER_CONTOURS = 0.82f;

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
	public boolean isEnabled() {
		return app.getSettings().USE_OPENGL_RENDER.get()
				&& !app.getSettings().OPENGL_RENDER_FAILED.get()
				&& super.isEnabled();
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
			if (weatherLayerLow == null || weatherLayerHigh == null || weatherContourLayer == null) {
				createLayers();
			}
			if (!mapView.isLayerExists(weatherLayerLow)) {
				mapView.addLayer(weatherLayerLow, ZORDER_RASTER_LOW);
			}
			if (!mapView.isLayerExists(weatherLayerHigh)) {
				mapView.addLayer(weatherLayerHigh, ZORDER_RASTER_HIGH);
			}
			if (!mapView.isLayerExists(weatherContourLayer)) {
				mapView.addLayer(weatherContourLayer, ZORDER_CONTOURS);
			}
			mapView.refreshMap();
		} else {
			mapView.removeLayer(weatherLayerLow);
			mapView.removeLayer(weatherLayerHigh);
			mapView.removeLayer(weatherContourLayer);
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
		bandSettings.set(WeatherBand.WEATHER_BAND_CLOUD, cloudBandSettings);

		String tempUnit = "°C";
		String tempUnitFormatGeneral = "%d";
		String tempUnitFormatPrecise = "%.1f";
		Integer tempTransparencyObj = transparencies.get(WeatherInfoType.TEMPERATURE);
		float tempTransparency = tempTransparencyObj != null ? tempTransparencyObj / 100f : 0.5f;
		String tempColorProfilePath = new File(app.getAppPath(WEATHER_INDEX_DIR), "temperature_color.txt").getAbsolutePath();

		String tempContourStyleName = "temperature";

		ZoomLevelDoubleListHash contourLevels = new ZoomLevelDoubleListHash();

		QListDouble ld9 = new QListDouble();
		ld9.add(-70.000000);
		ld9.add(-65.000000);
		ld9.add(-60.000000);
		ld9.add(-55.000000);
		ld9.add(-50.000000);
		ld9.add(-45.000000);
		ld9.add(-40.000000);
		ld9.add(-35.000000);
		ld9.add(-30.000000);
		ld9.add(-25.000000);
		ld9.add(-20.000000);
		ld9.add(-15.000000);
		ld9.add(-10.000000);
		ld9.add(-5.000000);
		ld9.add(0.000000);
		ld9.add(5.000000);
		ld9.add(10.000000);
		ld9.add(15.000000);
		ld9.add(20.000000);
		ld9.add(25.000000);
		ld9.add(30.000000);
		ld9.add(35.000000);
		ld9.add(40.000000);
		ld9.add(45.000000);
		ld9.add(50.000000);
		ld9.add(55.000000);
		ld9.add(60.000000);
		contourLevels.set(ZoomLevel.ZoomLevel9, ld9);
		QListDouble ld5 = new QListDouble();
		ld5.add(-70.000000);
		ld5.add(-60.000000);
		ld5.add(-50.000000);
		ld5.add(-40.000000);
		ld5.add(-30.000000);
		ld5.add(-20.000000);
		ld5.add(-10.000000);
		ld5.add(0.000000);
		ld5.add(10.000000);
		ld5.add(20.000000);
		ld5.add(30.000000);
		ld5.add(40.000000);
		ld5.add(50.000000);
		ld5.add(60.000000);
		contourLevels.set(ZoomLevel.ZoomLevel5, ld5);
		QListDouble ld10 = new QListDouble();
		ld10.add(-70.000000);
		ld10.add(-68.000000);
		ld10.add(-66.000000);
		ld10.add(-64.000000);
		ld10.add(-62.000000);
		ld10.add(-60.000000);
		ld10.add(-58.000000);
		ld10.add(-56.000000);
		ld10.add(-54.000000);
		ld10.add(-52.000000);
		ld10.add(-50.000000);
		ld10.add(-48.000000);
		ld10.add(-46.000000);
		ld10.add(-44.000000);
		ld10.add(-42.000000);
		ld10.add(-40.000000);
		ld10.add(-38.000000);
		ld10.add(-36.000000);
		ld10.add(-34.000000);
		ld10.add(-32.000000);
		ld10.add(-30.000000);
		ld10.add(-28.000000);
		ld10.add(-26.000000);
		ld10.add(-24.000000);
		ld10.add(-22.000000);
		ld10.add(-20.000000);
		ld10.add(-18.000000);
		ld10.add(-16.000000);
		ld10.add(-14.000000);
		ld10.add(-12.000000);
		ld10.add(-10.000000);
		ld10.add(-8.000000);
		ld10.add(-6.000000);
		ld10.add(-4.000000);
		ld10.add(-2.000000);
		ld10.add(0.000000);
		ld10.add(2.000000);
		ld10.add(4.000000);
		ld10.add(6.000000);
		ld10.add(8.000000);
		ld10.add(10.000000);
		ld10.add(12.000000);
		ld10.add(14.000000);
		ld10.add(16.000000);
		ld10.add(18.000000);
		ld10.add(20.000000);
		ld10.add(22.000000);
		ld10.add(24.000000);
		ld10.add(26.000000);
		ld10.add(28.000000);
		ld10.add(30.000000);
		ld10.add(32.000000);
		ld10.add(34.000000);
		ld10.add(36.000000);
		ld10.add(38.000000);
		ld10.add(40.000000);
		ld10.add(42.000000);
		ld10.add(44.000000);
		ld10.add(46.000000);
		ld10.add(48.000000);
		ld10.add(50.000000);
		ld10.add(52.000000);
		ld10.add(54.000000);
		ld10.add(56.000000);
		ld10.add(58.000000);
		ld10.add(60.000000);
		contourLevels.set(ZoomLevel.ZoomLevel10, ld10);
		QListDouble ld6 = new QListDouble();
		ld6.add(-70.000000);
		ld6.add(-65.000000);
		ld6.add(-60.000000);
		ld6.add(-55.000000);
		ld6.add(-50.000000);
		ld6.add(-45.000000);
		ld6.add(-40.000000);
		ld6.add(-35.000000);
		ld6.add(-30.000000);
		ld6.add(-25.000000);
		ld6.add(-20.000000);
		ld6.add(-15.000000);
		ld6.add(-10.000000);
		ld6.add(-5.000000);
		ld6.add(0.000000);
		ld6.add(5.000000);
		ld6.add(10.000000);
		ld6.add(15.000000);
		ld6.add(20.000000);
		ld6.add(25.000000);
		ld6.add(30.000000);
		ld6.add(35.000000);
		ld6.add(40.000000);
		ld6.add(45.000000);
		ld6.add(50.000000);
		ld6.add(55.000000);
		ld6.add(60.000000);
		contourLevels.set(ZoomLevel.ZoomLevel6, ld6);
		QListDouble ld11 = new QListDouble();
		ld11.add(-70.000000);
		ld11.add(-68.000000);
		ld11.add(-66.000000);
		ld11.add(-64.000000);
		ld11.add(-62.000000);
		ld11.add(-60.000000);
		ld11.add(-58.000000);
		ld11.add(-56.000000);
		ld11.add(-54.000000);
		ld11.add(-52.000000);
		ld11.add(-50.000000);
		ld11.add(-48.000000);
		ld11.add(-46.000000);
		ld11.add(-44.000000);
		ld11.add(-42.000000);
		ld11.add(-40.000000);
		ld11.add(-38.000000);
		ld11.add(-36.000000);
		ld11.add(-34.000000);
		ld11.add(-32.000000);
		ld11.add(-30.000000);
		ld11.add(-28.000000);
		ld11.add(-26.000000);
		ld11.add(-24.000000);
		ld11.add(-22.000000);
		ld11.add(-20.000000);
		ld11.add(-18.000000);
		ld11.add(-16.000000);
		ld11.add(-14.000000);
		ld11.add(-12.000000);
		ld11.add(-10.000000);
		ld11.add(-8.000000);
		ld11.add(-6.000000);
		ld11.add(-4.000000);
		ld11.add(-2.000000);
		ld11.add(0.000000);
		ld11.add(2.000000);
		ld11.add(4.000000);
		ld11.add(6.000000);
		ld11.add(8.000000);
		ld11.add(10.000000);
		ld11.add(12.000000);
		ld11.add(14.000000);
		ld11.add(16.000000);
		ld11.add(18.000000);
		ld11.add(20.000000);
		ld11.add(22.000000);
		ld11.add(24.000000);
		ld11.add(26.000000);
		ld11.add(28.000000);
		ld11.add(30.000000);
		ld11.add(32.000000);
		ld11.add(34.000000);
		ld11.add(36.000000);
		ld11.add(38.000000);
		ld11.add(40.000000);
		ld11.add(42.000000);
		ld11.add(44.000000);
		ld11.add(46.000000);
		ld11.add(48.000000);
		ld11.add(50.000000);
		ld11.add(52.000000);
		ld11.add(54.000000);
		ld11.add(56.000000);
		ld11.add(58.000000);
		ld11.add(60.000000);
		contourLevels.set(ZoomLevel.ZoomLevel11, ld11);
		QListDouble ld7 = new QListDouble();
		ld7.add(-70.000000);
		ld7.add(-65.000000);
		ld7.add(-60.000000);
		ld7.add(-55.000000);
		ld7.add(-50.000000);
		ld7.add(-45.000000);
		ld7.add(-40.000000);
		ld7.add(-35.000000);
		ld7.add(-30.000000);
		ld7.add(-25.000000);
		ld7.add(-20.000000);
		ld7.add(-15.000000);
		ld7.add(-10.000000);
		ld7.add(-5.000000);
		ld7.add(0.000000);
		ld7.add(5.000000);
		ld7.add(10.000000);
		ld7.add(15.000000);
		ld7.add(20.000000);
		ld7.add(25.000000);
		ld7.add(30.000000);
		ld7.add(35.000000);
		ld7.add(40.000000);
		ld7.add(45.000000);
		ld7.add(50.000000);
		ld7.add(55.000000);
		ld7.add(60.000000);
		contourLevels.set(ZoomLevel.ZoomLevel7, ld7);
		QListDouble ld12 = new QListDouble();
		ld12.add(-70.000000);
		ld12.add(-68.000000);
		ld12.add(-66.000000);
		ld12.add(-64.000000);
		ld12.add(-62.000000);
		ld12.add(-60.000000);
		ld12.add(-58.000000);
		ld12.add(-56.000000);
		ld12.add(-54.000000);
		ld12.add(-52.000000);
		ld12.add(-50.000000);
		ld12.add(-48.000000);
		ld12.add(-46.000000);
		ld12.add(-44.000000);
		ld12.add(-42.000000);
		ld12.add(-40.000000);
		ld12.add(-38.000000);
		ld12.add(-36.000000);
		ld12.add(-34.000000);
		ld12.add(-32.000000);
		ld12.add(-30.000000);
		ld12.add(-28.000000);
		ld12.add(-26.000000);
		ld12.add(-24.000000);
		ld12.add(-22.000000);
		ld12.add(-20.000000);
		ld12.add(-18.000000);
		ld12.add(-16.000000);
		ld12.add(-14.000000);
		ld12.add(-12.000000);
		ld12.add(-10.000000);
		ld12.add(-8.000000);
		ld12.add(-6.000000);
		ld12.add(-4.000000);
		ld12.add(-2.000000);
		ld12.add(0.000000);
		ld12.add(2.000000);
		ld12.add(4.000000);
		ld12.add(6.000000);
		ld12.add(8.000000);
		ld12.add(10.000000);
		ld12.add(12.000000);
		ld12.add(14.000000);
		ld12.add(16.000000);
		ld12.add(18.000000);
		ld12.add(20.000000);
		ld12.add(22.000000);
		ld12.add(24.000000);
		ld12.add(26.000000);
		ld12.add(28.000000);
		ld12.add(30.000000);
		ld12.add(32.000000);
		ld12.add(34.000000);
		ld12.add(36.000000);
		ld12.add(38.000000);
		ld12.add(40.000000);
		ld12.add(42.000000);
		ld12.add(44.000000);
		ld12.add(46.000000);
		ld12.add(48.000000);
		ld12.add(50.000000);
		ld12.add(52.000000);
		ld12.add(54.000000);
		ld12.add(56.000000);
		ld12.add(58.000000);
		ld12.add(60.000000);
		contourLevels.set(ZoomLevel.ZoomLevel12, ld12);
		QListDouble ld8 = new QListDouble();
		ld8.add(-70.000000);
		ld8.add(-65.000000);
		ld8.add(-60.000000);
		ld8.add(-55.000000);
		ld8.add(-50.000000);
		ld8.add(-45.000000);
		ld8.add(-40.000000);
		ld8.add(-35.000000);
		ld8.add(-30.000000);
		ld8.add(-25.000000);
		ld8.add(-20.000000);
		ld8.add(-15.000000);
		ld8.add(-10.000000);
		ld8.add(-5.000000);
		ld8.add(0.000000);
		ld8.add(5.000000);
		ld8.add(10.000000);
		ld8.add(15.000000);
		ld8.add(20.000000);
		ld8.add(25.000000);
		ld8.add(30.000000);
		ld8.add(35.000000);
		ld8.add(40.000000);
		ld8.add(45.000000);
		ld8.add(50.000000);
		ld8.add(55.000000);
		ld8.add(60.000000);
		contourLevels.set(ZoomLevel.ZoomLevel8, ld8);
		QListDouble ld4 = new QListDouble();
		ld4.add(-70.000000);
		ld4.add(-60.000000);
		ld4.add(-50.000000);
		ld4.add(-40.000000);
		ld4.add(-30.000000);
		ld4.add(-20.000000);
		ld4.add(-10.000000);
		ld4.add(0.000000);
		ld4.add(10.000000);
		ld4.add(20.000000);
		ld4.add(30.000000);
		ld4.add(40.000000);
		ld4.add(50.000000);
		ld4.add(60.000000);
		contourLevels.set(ZoomLevel.ZoomLevel4, ld4);

		ZoomLevelStringListHash contourTypes = new ZoomLevelStringListHash();

		QStringList sl9 = new QStringList();
		sl9.add("-30°C");
		sl9.add("-20°C");
		sl9.add("-10°C");
		sl9.add("-5°C");
		sl9.add("0°C");
		sl9.add("5°C");
		sl9.add("10°C");
		sl9.add("20°C");
		sl9.add("30°C");
		contourTypes.set(ZoomLevel.ZoomLevel9, sl9);
		QStringList sl5 = new QStringList();
		sl5.add("-30°C");
		sl5.add("-20°C");
		sl5.add("-10°C");
		sl5.add("-5°C");
		sl5.add("0°C");
		sl5.add("5°C");
		sl5.add("10°C");
		sl5.add("20°C");
		sl5.add("30°C");
		contourTypes.set(ZoomLevel.ZoomLevel5, sl5);
		QStringList sl10 = new QStringList();
		sl10.add("-30°C");
		sl10.add("-20°C");
		sl10.add("-10°C");
		sl10.add("-5°C");
		sl10.add("0°C");
		sl10.add("5°C");
		sl10.add("10°C");
		sl10.add("20°C");
		sl10.add("30°C");
		contourTypes.set(ZoomLevel.ZoomLevel10, sl10);
		QStringList sl6 = new QStringList();
		sl6.add("-30°C");
		sl6.add("-20°C");
		sl6.add("-10°C");
		sl6.add("-5°C");
		sl6.add("0°C");
		sl6.add("5°C");
		sl6.add("10°C");
		sl6.add("20°C");
		sl6.add("30°C");
		contourTypes.set(ZoomLevel.ZoomLevel6, sl6);
		QStringList sl11 = new QStringList();
		sl11.add("-30°C");
		sl11.add("-20°C");
		sl11.add("-10°C");
		sl11.add("-5°C");
		sl11.add("0°C");
		sl11.add("5°C");
		sl11.add("10°C");
		sl11.add("20°C");
		sl11.add("30°C");
		contourTypes.set(ZoomLevel.ZoomLevel11, sl11);
		QStringList sl7 = new QStringList();
		sl7.add("-30°C");
		sl7.add("-20°C");
		sl7.add("-10°C");
		sl7.add("-5°C");
		sl7.add("0°C");
		sl7.add("5°C");
		sl7.add("10°C");
		sl7.add("20°C");
		sl7.add("30°C");
		contourTypes.set(ZoomLevel.ZoomLevel7, sl7);
		QStringList sl12 = new QStringList();
		sl12.add("-30°C");
		sl12.add("-20°C");
		sl12.add("-10°C");
		sl12.add("-5°C");
		sl12.add("0°C");
		sl12.add("5°C");
		sl12.add("10°C");
		sl12.add("20°C");
		sl12.add("30°C");
		contourTypes.set(ZoomLevel.ZoomLevel12, sl12);
		QStringList sl8 = new QStringList();
		sl8.add("-30°C");
		sl8.add("-20°C");
		sl8.add("-10°C");
		sl8.add("-5°C");
		sl8.add("0°C");
		sl8.add("5°C");
		sl8.add("10°C");
		sl8.add("20°C");
		sl8.add("30°C");
		contourTypes.set(ZoomLevel.ZoomLevel8, sl8);
		QStringList sl4 = new QStringList();
		sl4.add("-30°C");
		sl4.add("-20°C");
		sl4.add("-10°C");
		sl4.add("-5°C");
		sl4.add("0°C");
		sl4.add("5°C");
		sl4.add("10°C");
		sl4.add("20°C");
		sl4.add("30°C");
		contourTypes.set(ZoomLevel.ZoomLevel4, sl4);

		GeoBandSettings tempBandSettings = new GeoBandSettings(tempUnit, tempUnitFormatGeneral,
				tempUnitFormatPrecise, "°C", tempTransparency, tempColorProfilePath,
				tempContourStyleName, contourLevels, contourTypes);
		bandSettings.set(WeatherBand.WEATHER_BAND_TEMPERATURE, tempBandSettings);

		String pressureUnit = "mmHg";
		String pressureUnitFormatGeneral = "%d";
		String pressureUnitFormatPrecise = "%d";
		Integer pressureTransparencyObj = transparencies.get(WeatherInfoType.PRESSURE);
		float pressureTransparency = pressureTransparencyObj != null ? pressureTransparencyObj / 100f : 0.5f;
		String pressureColorProfilePath = new File(app.getAppPath(WEATHER_INDEX_DIR), "pressure_color.txt").getAbsolutePath();
		String pressureContourStyleName = "pressure";

		contourLevels = new ZoomLevelDoubleListHash();

		/*
		QListDouble ld9 = new QListDouble();
		ld9.add(93324.000000);
		ld9.add(93990.600000);
		ld9.add(94657.200000);
		ld9.add(95323.800000);
		ld9.add(95990.400000);
		ld9.add(96657.000000);
		ld9.add(97323.600000);
		ld9.add(97990.200000);
		ld9.add(98656.800000);
		ld9.add(99323.400000);
		ld9.add(99990.000000);
		ld9.add(100656.600000);
		ld9.add(101323.200000);
		ld9.add(101989.800000);
		ld9.add(102656.400000);
		ld9.add(103323.000000);
		ld9.add(103989.600000);
		ld9.add(104656.200000);
		ld9.add(105322.800000);
		ld9.add(105989.400000);
		ld9.add(106656.000000);
		contourLevels.set(ZoomLevel.ZoomLevel9, ld9);
		QListDouble ld5 = new QListDouble();
		ld5.add(93324.000000);
		ld5.add(93990.600000);
		ld5.add(94657.200000);
		ld5.add(95323.800000);
		ld5.add(95990.400000);
		ld5.add(96657.000000);
		ld5.add(97323.600000);
		ld5.add(97990.200000);
		ld5.add(98656.800000);
		ld5.add(99323.400000);
		ld5.add(99990.000000);
		ld5.add(100656.600000);
		ld5.add(101323.200000);
		ld5.add(101989.800000);
		ld5.add(102656.400000);
		ld5.add(103323.000000);
		ld5.add(103989.600000);
		ld5.add(104656.200000);
		ld5.add(105322.800000);
		ld5.add(105989.400000);
		ld5.add(106656.000000);
		contourLevels.set(ZoomLevel.ZoomLevel5, ld5);
		QListDouble ld10 = new QListDouble();
		ld10.add(93324.000000);
		ld10.add(93990.600000);
		ld10.add(94657.200000);
		ld10.add(95323.800000);
		ld10.add(95990.400000);
		ld10.add(96657.000000);
		ld10.add(97323.600000);
		ld10.add(97990.200000);
		ld10.add(98656.800000);
		ld10.add(99323.400000);
		ld10.add(99990.000000);
		ld10.add(100656.600000);
		ld10.add(101323.200000);
		ld10.add(101989.800000);
		ld10.add(102656.400000);
		ld10.add(103323.000000);
		ld10.add(103989.600000);
		ld10.add(104656.200000);
		ld10.add(105322.800000);
		ld10.add(105989.400000);
		ld10.add(106656.000000);
		contourLevels.set(ZoomLevel.ZoomLevel10, ld10);
		QListDouble ld6 = new QListDouble();
		ld6.add(93324.000000);
		ld6.add(93990.600000);
		ld6.add(94657.200000);
		ld6.add(95323.800000);
		ld6.add(95990.400000);
		ld6.add(96657.000000);
		ld6.add(97323.600000);
		ld6.add(97990.200000);
		ld6.add(98656.800000);
		ld6.add(99323.400000);
		ld6.add(99990.000000);
		ld6.add(100656.600000);
		ld6.add(101323.200000);
		ld6.add(101989.800000);
		ld6.add(102656.400000);
		ld6.add(103323.000000);
		ld6.add(103989.600000);
		ld6.add(104656.200000);
		ld6.add(105322.800000);
		ld6.add(105989.400000);
		ld6.add(106656.000000);
		contourLevels.set(ZoomLevel.ZoomLevel6, ld6);
		QListDouble ld11 = new QListDouble();
		ld11.add(93324.000000);
		ld11.add(93990.600000);
		ld11.add(94657.200000);
		ld11.add(95323.800000);
		ld11.add(95990.400000);
		ld11.add(96657.000000);
		ld11.add(97323.600000);
		ld11.add(97990.200000);
		ld11.add(98656.800000);
		ld11.add(99323.400000);
		ld11.add(99990.000000);
		ld11.add(100656.600000);
		ld11.add(101323.200000);
		ld11.add(101989.800000);
		ld11.add(102656.400000);
		ld11.add(103323.000000);
		ld11.add(103989.600000);
		ld11.add(104656.200000);
		ld11.add(105322.800000);
		ld11.add(105989.400000);
		ld11.add(106656.000000);
		contourLevels.set(ZoomLevel.ZoomLevel11, ld11);
		QListDouble ld7 = new QListDouble();
		ld7.add(93324.000000);
		ld7.add(93990.600000);
		ld7.add(94657.200000);
		ld7.add(95323.800000);
		ld7.add(95990.400000);
		ld7.add(96657.000000);
		ld7.add(97323.600000);
		ld7.add(97990.200000);
		ld7.add(98656.800000);
		ld7.add(99323.400000);
		ld7.add(99990.000000);
		ld7.add(100656.600000);
		ld7.add(101323.200000);
		ld7.add(101989.800000);
		ld7.add(102656.400000);
		ld7.add(103323.000000);
		ld7.add(103989.600000);
		ld7.add(104656.200000);
		ld7.add(105322.800000);
		ld7.add(105989.400000);
		ld7.add(106656.000000);
		contourLevels.set(ZoomLevel.ZoomLevel7, ld7);
		QListDouble ld12 = new QListDouble();
		ld12.add(93324.000000);
		ld12.add(93990.600000);
		ld12.add(94657.200000);
		ld12.add(95323.800000);
		ld12.add(95990.400000);
		ld12.add(96657.000000);
		ld12.add(97323.600000);
		ld12.add(97990.200000);
		ld12.add(98656.800000);
		ld12.add(99323.400000);
		ld12.add(99990.000000);
		ld12.add(100656.600000);
		ld12.add(101323.200000);
		ld12.add(101989.800000);
		ld12.add(102656.400000);
		ld12.add(103323.000000);
		ld12.add(103989.600000);
		ld12.add(104656.200000);
		ld12.add(105322.800000);
		ld12.add(105989.400000);
		ld12.add(106656.000000);
		contourLevels.set(ZoomLevel.ZoomLevel12, ld12);
		QListDouble ld8 = new QListDouble();
		ld8.add(93324.000000);
		ld8.add(93990.600000);
		ld8.add(94657.200000);
		ld8.add(95323.800000);
		ld8.add(95990.400000);
		ld8.add(96657.000000);
		ld8.add(97323.600000);
		ld8.add(97990.200000);
		ld8.add(98656.800000);
		ld8.add(99323.400000);
		ld8.add(99990.000000);
		ld8.add(100656.600000);
		ld8.add(101323.200000);
		ld8.add(101989.800000);
		ld8.add(102656.400000);
		ld8.add(103323.000000);
		ld8.add(103989.600000);
		ld8.add(104656.200000);
		ld8.add(105322.800000);
		ld8.add(105989.400000);
		ld8.add(106656.000000);
		contourLevels.set(ZoomLevel.ZoomLevel8, ld8);
		QListDouble ld4 = new QListDouble();
		ld4.add(93324.000000);
		ld4.add(93990.600000);
		ld4.add(94657.200000);
		ld4.add(95323.800000);
		ld4.add(95990.400000);
		ld4.add(96657.000000);
		ld4.add(97323.600000);
		ld4.add(97990.200000);
		ld4.add(98656.800000);
		ld4.add(99323.400000);
		ld4.add(99990.000000);
		ld4.add(100656.600000);
		ld4.add(101323.200000);
		ld4.add(101989.800000);
		ld4.add(102656.400000);
		ld4.add(103323.000000);
		ld4.add(103989.600000);
		ld4.add(104656.200000);
		ld4.add(105322.800000);
		ld4.add(105989.400000);
		ld4.add(106656.000000);
		contourLevels.set(ZoomLevel.ZoomLevel4, ld4);
*/
		contourTypes = new ZoomLevelStringListHash();
/*
		QStringList sl9 = new QStringList();
		sl9.add("97323mmHg");
		sl9.add("98656mmHg");
		sl9.add("99990mmHg");
		sl9.add("101323mmHg");
		sl9.add("102656mmHg");
		sl9.add("103989mmHg");
		contourTypes.set(ZoomLevel.ZoomLevel9, sl9);
		QStringList sl5 = new QStringList();
		sl5.add("97323mmHg");
		sl5.add("98656mmHg");
		sl5.add("99990mmHg");
		sl5.add("101323mmHg");
		sl5.add("102656mmHg");
		sl5.add("103989mmHg");
		contourTypes.set(ZoomLevel.ZoomLevel5, sl5);
		QStringList sl10 = new QStringList();
		sl10.add("97323mmHg");
		sl10.add("98656mmHg");
		sl10.add("99990mmHg");
		sl10.add("101323mmHg");
		sl10.add("102656mmHg");
		sl10.add("103989mmHg");
		contourTypes.set(ZoomLevel.ZoomLevel10, sl10);
		QStringList sl6 = new QStringList();
		sl6.add("97323mmHg");
		sl6.add("98656mmHg");
		sl6.add("99990mmHg");
		sl6.add("101323mmHg");
		sl6.add("102656mmHg");
		sl6.add("103989mmHg");
		contourTypes.set(ZoomLevel.ZoomLevel6, sl6);
		QStringList sl11 = new QStringList();
		sl11.add("97323mmHg");
		sl11.add("98656mmHg");
		sl11.add("99990mmHg");
		sl11.add("101323mmHg");
		sl11.add("102656mmHg");
		sl11.add("103989mmHg");
		contourTypes.set(ZoomLevel.ZoomLevel11, sl11);
		QStringList sl7 = new QStringList();
		sl7.add("97323mmHg");
		sl7.add("98656mmHg");
		sl7.add("99990mmHg");
		sl7.add("101323mmHg");
		sl7.add("102656mmHg");
		sl7.add("103989mmHg");
		contourTypes.set(ZoomLevel.ZoomLevel7, sl7);
		QStringList sl12 = new QStringList();
		sl12.add("97323mmHg");
		sl12.add("98656mmHg");
		sl12.add("99990mmHg");
		sl12.add("101323mmHg");
		sl12.add("102656mmHg");
		sl12.add("103989mmHg");
		contourTypes.set(ZoomLevel.ZoomLevel12, sl12);
		QStringList sl8 = new QStringList();
		sl8.add("97323mmHg");
		sl8.add("98656mmHg");
		sl8.add("99990mmHg");
		sl8.add("101323mmHg");
		sl8.add("102656mmHg");
		sl8.add("103989mmHg");
		contourTypes.set(ZoomLevel.ZoomLevel8, sl8);
		QStringList sl4 = new QStringList();
		sl4.add("97323mmHg");
		sl4.add("98656mmHg");
		sl4.add("99990mmHg");
		sl4.add("101323mmHg");
		sl4.add("102656mmHg");
		sl4.add("103989mmHg");
		contourTypes.set(ZoomLevel.ZoomLevel4, sl4);
*/
		GeoBandSettings pressureBandSettings = new GeoBandSettings(pressureUnit, pressureUnitFormatGeneral,
				pressureUnitFormatPrecise, "Pa", pressureTransparency, pressureColorProfilePath,
				pressureContourStyleName, contourLevels, contourTypes);
		bandSettings.set(WeatherBand.WEATHER_BAND_PRESSURE, pressureBandSettings);

		String windUnit = "m/s";
		String windUnitFormatGeneral = "%d";
		String windUnitFormatPrecise = "%d";
		Integer windTransparencyObj = transparencies.get(WeatherInfoType.WIND);
		float windTransparency = windTransparencyObj != null ? windTransparencyObj / 100f : 0.5f;
		String windColorProfilePath = new File(app.getAppPath(WEATHER_INDEX_DIR), "wind_color.txt").getAbsolutePath();
		GeoBandSettings windBandSettings = new GeoBandSettings(windUnit, windUnitFormatGeneral,
				windUnitFormatPrecise, "m/s", windTransparency, windColorProfilePath,
				"", new ZoomLevelDoubleListHash(), new ZoomLevelStringListHash());
		bandSettings.set(WeatherBand.WEATHER_BAND_WIND_SPEED, windBandSettings);

		String precipUnit = "mm";
		String precipUnitFormatGeneral = "%d";
		String precipUnitFormatPrecise = "%d";
		Integer precipTransparencyObj = transparencies.get(WeatherInfoType.PRECIPITATION);
		float precipTransparency = precipTransparencyObj != null ? precipTransparencyObj / 100f : 0.5f;
		String precipColorProfilePath = new File(app.getAppPath(WEATHER_INDEX_DIR), "precip_color.txt").getAbsolutePath();
		GeoBandSettings precipBandSettings = new GeoBandSettings(precipUnit, precipUnitFormatGeneral,
				precipUnitFormatPrecise, "kg/(m^2 s)", precipTransparency, precipColorProfilePath,
				"", new ZoomLevelDoubleListHash(), new ZoomLevelStringListHash());
		bandSettings.set(WeatherBand.WEATHER_BAND_PRECIPITATION, precipBandSettings);

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
		weatherContourLayer = new WeatherContourLayer(app);
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

		RenderingRuleProperty tempContoursProp = app.getRendererRegistry().getCustomRenderingRuleProperty(WEATHER_TEMP_CONTOUR_LINES_ATTR);
		if (tempContoursProp != null) {
			CommonPreference<Boolean> pref = app.getSettings().getCustomRenderBooleanProperty(tempContoursProp.getAttrName());
			pref.set(enabled);
		}

		RenderingRuleProperty pressureContoursProp = app.getRendererRegistry().getCustomRenderingRuleProperty(WEATHER_PRESSURE_CONTOURS_LINES_ATTR);
		if (pressureContoursProp != null) {
			CommonPreference<Boolean> pref = app.getSettings().getCustomRenderBooleanProperty(pressureContoursProp.getAttrName());
			//pref.set(true);
			pref.set(false);
		}
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