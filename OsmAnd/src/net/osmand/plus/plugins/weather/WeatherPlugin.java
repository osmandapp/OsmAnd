package net.osmand.plus.plugins.weather;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_WEATHER_FORECAST_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_WEATHER;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.WEATHER_ID;
import static net.osmand.plus.AppInitEvents.INDEX_REGION_BOUNDARIES;
import static net.osmand.plus.AppInitEvents.NATIVE_OPEN_GL_INITIALIZED;
import static net.osmand.plus.chooseplan.OsmAndFeature.WEATHER;
import static net.osmand.plus.download.DownloadActivityType.WEATHER_FORECAST;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_CLOUD;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_NOTHING;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_PRECIPITATION;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_PRESSURE;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_TEMPERATURE;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_WIND_SPEED;
import static net.osmand.plus.plugins.weather.WeatherSettings.WEATHER_CLOUD_CONTOURS_LINES_ATTR;
import static net.osmand.plus.plugins.weather.WeatherSettings.WEATHER_PRECIPITATION_CONTOURS_LINES_ATTR;
import static net.osmand.plus.plugins.weather.WeatherSettings.WEATHER_PRESSURE_CONTOURS_LINES_ATTR;
import static net.osmand.plus.plugins.weather.WeatherSettings.WEATHER_TEMP_CONTOUR_LINES_ATTR;
import static net.osmand.plus.plugins.weather.WeatherSettings.WEATHER_WIND_CONTOURS_LINES_ATTR;
import static net.osmand.plus.settings.fragments.SettingsScreenType.WEATHER_SETTINGS;
import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_AIR_PRESSURE_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_CLOUDS_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_PRECIPITATION_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_TEMPERATURE_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_WIND_WIDGET;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererContext;
import net.osmand.plus.AppInitEvents;
import net.osmand.plus.AppInitializeListener;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.chooseplan.button.PurchasingUtils;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.dashboard.DashboardType;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.weather.WeatherBand.WeatherBandType;
import net.osmand.plus.plugins.weather.WeatherRasterLayer.WeatherLayer;
import net.osmand.plus.plugins.weather.actions.OpenWeatherAction;
import net.osmand.plus.plugins.weather.actions.ShowHideAirPressureLayerAction;
import net.osmand.plus.plugins.weather.actions.ShowHideCloudLayerAction;
import net.osmand.plus.plugins.weather.actions.ShowHidePrecipitationLayerAction;
import net.osmand.plus.plugins.weather.actions.ShowHideTemperatureLayerAction;
import net.osmand.plus.plugins.weather.actions.ShowHideWindLayerAction;
import net.osmand.plus.plugins.weather.dialogs.WeatherForecastFragment;
import net.osmand.plus.plugins.weather.enums.WeatherSource;
import net.osmand.plus.plugins.weather.units.WeatherUnit;
import net.osmand.plus.plugins.weather.widgets.WeatherWidget;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.views.layers.DownloadedRegionsLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetInfoCreator;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.OnRowItemClick;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WeatherPlugin extends OsmandPlugin {

	private static final Log log = PlatformUtil.getLog(WeatherPlugin.class);

	private static final float ZORDER_RASTER_LOW = 0.8f;
	private static final float ZORDER_RASTER_HIGH = 0.81f;
	private static final float ZORDER_CONTOURS = 0.82f;

	private final WeatherHelper weatherHelper;
	private final WeatherSettings weatherSettings;

	private WeatherRasterLayer weatherLayerLow;
	private WeatherRasterLayer weatherLayerHigh;
	private WeatherContourLayer weatherContourLayer;

	@Nullable
	private Date forecastDate;

	@WeatherBandType
	private short currentConfigureBand = WEATHER_BAND_NOTHING;

	public WeatherPlugin(@NonNull OsmandApplication app) {
		super(app);
		weatherHelper = app.getWeatherHelper();
		weatherSettings = weatherHelper.getWeatherSettings();

		for (WeatherBand weatherBand : weatherHelper.getWeatherBands()) {
			CommonPreference<? extends WeatherUnit> preference = weatherBand.getBandUnitPref();
			if (preference != null) {
				pluginPreferences.add(preference);
			}
		}

		ApplicationMode[] noAppMode = {};
		WidgetsAvailabilityHelper.regWidgetVisibility(WEATHER_TEMPERATURE_WIDGET, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(WEATHER_PRECIPITATION_WIDGET, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(WEATHER_WIND_WIDGET, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(WEATHER_CLOUDS_WIDGET, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(WEATHER_AIR_PRESSURE_WIDGET, noAppMode);

		app.getAppInitializer().addListener(new AppInitializeListener() {
			@Override
			public void onProgress(@NonNull AppInitializer init, @NonNull AppInitEvents event) {
				if (event == NATIVE_OPEN_GL_INITIALIZED) {
					updateMapPresentationEnvironment();
					updateLayers(app, null);

					if (weatherHelper.shouldUpdateForecastCache()) {
						weatherHelper.updateForecastCache();
					}
				} else if (event == INDEX_REGION_BOUNDARIES) {
					clearOutdatedCache();
				}
			}
		});
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
	public boolean init(@NonNull OsmandApplication app, @Nullable Activity activity) {
		if (!app.getAppInitializer().isAppInitializing()) {
			updateMapPresentationEnvironment();

			if (weatherHelper.shouldUpdateForecastCache()) {
				weatherHelper.updateForecastCache();
			}
		}
		return super.init(app, activity);
	}

	private void updateMapPresentationEnvironment() {
		MapRendererContext mapRenderer = NativeCoreContext.getMapRendererContext();
		if (weatherHelper.getWeatherResourcesManager() == null && mapRenderer != null) {
			updateMapPresentationEnvironment(mapRenderer);
		}
	}


	private void clearOutdatedCache() {
		if (weatherHelper.getWeatherResourcesManager() != null) {
			weatherHelper.clearOutdatedCache();
		} else {
			log.error("Tile Resources Manager isn't initialized");
		}
	}

	@Override
	public boolean isEnabled() {
		return WeatherUtils.isWeatherSupported(app) && super.isEnabled();
	}

	@Override
	public boolean isEnableByDefault() {
		return !Version.isHMDBuild();
	}

	@Override
	public CharSequence getDescription(boolean linksEnabled) {
		String infoUrl = app.getString(R.string.weather_global_forecast_system);
		String description = app.getString(R.string.weather_plugin_description, infoUrl);
		return linksEnabled ? UiUtilities.createUrlSpannable(app, description, infoUrl) : description;
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
		return !InAppPurchaseUtils.isWeatherAvailable(app);
	}

	@Nullable
	@Override
	public SettingsScreenType getSettingsScreenType() {
		return WEATHER_SETTINGS;
	}

	@Nullable
	@Override
	public OsmAndFeature getOsmAndFeature() {
		return WEATHER;
	}

	public void setCurrentConfigureBand(@WeatherBandType short currentConfigureBand) {
		this.currentConfigureBand = currentConfigureBand;
	}

	@WeatherBandType
	public short getCurrentConfigureBand() {
		return currentConfigureBand;
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
	public WeatherWidget createMapWidgetForParams(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		return switch (widgetType) {
			case WEATHER_TEMPERATURE_WIDGET ->
					new WeatherWidget(mapActivity, widgetType, customId, widgetsPanel, WEATHER_BAND_TEMPERATURE);
			case WEATHER_PRECIPITATION_WIDGET ->
					new WeatherWidget(mapActivity, widgetType, customId, widgetsPanel, WEATHER_BAND_PRECIPITATION);
			case WEATHER_WIND_WIDGET ->
					new WeatherWidget(mapActivity, widgetType, customId, widgetsPanel, WEATHER_BAND_WIND_SPEED);
			case WEATHER_CLOUDS_WIDGET ->
					new WeatherWidget(mapActivity, widgetType, customId, widgetsPanel, WEATHER_BAND_CLOUD);
			case WEATHER_AIR_PRESSURE_WIDGET ->
					new WeatherWidget(mapActivity, widgetType, customId, widgetsPanel, WEATHER_BAND_PRESSURE);
			default -> null;
		};
	}

	@Nullable
	@Override
	public WeatherWidget createMapWidgetForParams(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType) {
		return createMapWidgetForParams(mapActivity, widgetType, null, null);
	}

	@Override
	protected List<QuickActionType> getQuickActionTypes() {
		List<QuickActionType> action = new ArrayList<>();
		action.add(ShowHideTemperatureLayerAction.TYPE);
		action.add(ShowHideWindLayerAction.TYPE);
		action.add(ShowHideAirPressureLayerAction.TYPE);
		action.add(ShowHidePrecipitationLayerAction.TYPE);
		action.add(ShowHideCloudLayerAction.TYPE);
		action.add(OpenWeatherAction.TYPE);
		return action;
	}

	@Override
	protected void registerLayerContextMenuActions(@NonNull ContextMenuAdapter adapter,
	                                               @NonNull MapActivity mapActivity,
	                                               @NonNull List<RenderingRuleProperty> customRules) {
		if (!isEnabled()) {
			return;
		}
		if (isLocked()) {
			PurchasingUtils.createPromoItem(adapter, mapActivity, OsmAndFeature.WEATHER,
					WEATHER_ID,
					R.string.shared_string_weather,
					R.string.explore_weather_forecast);
		} else {
			boolean selected = isWeatherEnabled();
			adapter.addItem(new ContextMenuItem(WEATHER_ID)
					.setTitleId(R.string.shared_string_weather, mapActivity)
					.setDescription(selected ? getWeatherTypesSummary(weatherHelper.getVisibleBands()) : null)
					.setSecondaryDescription(selected ? app.getString(R.string.shared_string_on) : null)
					.setSelected(selected)
					.setColor(app, selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
					.setIcon(R.drawable.ic_action_umbrella)
					.setSecondaryIcon(R.drawable.ic_action_additional_option)
					.setListener(getPropertyItemClickListener(mapActivity)));
		}
	}

	public ItemClickListener getPropertyItemClickListener(@NonNull MapActivity mapActivity) {
		return new OnRowItemClick() {

			@Override
			public boolean onRowItemClick(@NonNull OnDataChangeUiAdapter uiAdapter,
			                              @NonNull View view, @NonNull ContextMenuItem item) {
				DashboardOnMap dashboard = mapActivity.getDashboard();
				int[] coordinates = AndroidUtils.getCenterViewCoordinates(view);
				dashboard.setDashboardVisibility(true, DashboardType.WEATHER, coordinates);
				return false;
			}

			@Override
			public boolean onContextMenuClick(@Nullable OnDataChangeUiAdapter uiAdapter,
			                                  @Nullable View view, @NotNull ContextMenuItem item,
			                                  boolean isChecked) {
				weatherSettings.weatherEnabled.set(isChecked);
				item.setSelected(isChecked);
				item.setColor(app, isChecked ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
				item.setDescription(isChecked ? getWeatherTypesSummary(weatherHelper.getVisibleBands()) : null);
				if (uiAdapter != null) {
					uiAdapter.onDataSetChanged();
				}
				mapActivity.refreshMapComplete();
				return true;
			}
		};
	}

	@Nullable
	public String getWeatherTypesSummary(@NonNull List<WeatherBand> weatherBands) {
		if (!Algorithms.isEmpty(weatherBands)) {
			List<String> titles = new ArrayList<>();
			for (WeatherBand weatherBand : weatherBands) {
				titles.add(weatherBand.getMeasurementName());
			}
			return TextUtils.join(", ", titles);
		}
		return null;
	}

	@Override
	public void registerLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();

		if (weatherLayerLow != null) {
			mapView.removeLayer(weatherLayerLow);
		}
		if (weatherLayerHigh != null) {
			mapView.removeLayer(weatherLayerHigh);
		}
		if (weatherContourLayer != null) {
			mapView.removeLayer(weatherContourLayer);
		}
		createLayers();
	}


	@Override
	public void updateMapPresentationEnvironment(@NonNull MapRendererContext mapRendererContext) {
		weatherHelper.updateMapPresentationEnvironment(mapRendererContext);
	}

	@Override
	public void updateLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();

		// Weather layers available for opengl only
		if (!weatherHelper.updateBandsSettings()) {
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
			if (mapView.isLayerExists(weatherLayerLow)) {
				mapView.removeLayer(weatherLayerLow);
			}
			if (mapView.isLayerExists(weatherLayerHigh)) {
				mapView.removeLayer(weatherLayerHigh);
			}
			if (mapView.isLayerExists(weatherContourLayer)) {
				mapView.removeLayer(weatherContourLayer);
			}
		}
	}

	@Override
	public void registerOptionsMenuItems(MapActivity mapActivity, ContextMenuAdapter helper) {
		if (isActive()) {
			helper.addItem(new ContextMenuItem(DRAWER_WEATHER_FORECAST_ID)
					.setTitleId(R.string.shared_string_weather, mapActivity)
					.setIcon(R.drawable.ic_action_umbrella)
					.setListener((uiAdapter, view, item, isChecked) -> {
						app.logEvent("weatherForecastOpen");
						WeatherForecastFragment.showInstance(mapActivity.getSupportFragmentManager());
						return true;
					}));
		}
	}

	private void createLayers() {
		weatherLayerLow = new WeatherRasterLayer(app, WeatherLayer.LOW);
		weatherLayerHigh = new WeatherRasterLayer(app, WeatherLayer.HIGH);
		weatherContourLayer = new WeatherContourLayer(app);
		updateLayersDate(false, false);
	}

	public void setWeatherEnabled(boolean enable) {
		weatherSettings.weatherEnabled.set(enable);
	}

	public boolean isWeatherEnabled() {
		return weatherSettings.weatherEnabled.get();
	}

	public boolean isAnyDataVisible() {
		boolean isContoursEnabled = isAnyWeatherContourLinesEnabled();
		boolean isAnyLayerEnabled = weatherHelper.hasVisibleBands();
		return isWeatherEnabled() && (isAnyLayerEnabled || isContoursEnabled);
	}

	public boolean isContoursEnabled() {
		return weatherSettings.weatherContoursEnabled.get();
	}

	public WeatherSource getWeatherSource() {
		return WeatherSource.Companion.getWeatherSourceBySettingsValue(weatherSettings.weatherSource.get());
	}

	public void setWeatherSource(WeatherSource source) {
		weatherSettings.weatherSource.set(source.getSettingValue());
		weatherHelper.updateWeatherSource();
	}

	public boolean isAnyWeatherContourLinesEnabled() {
		return isWeatherContourLinesEnabled(WEATHER_TEMP_CONTOUR_LINES_ATTR)
				|| isWeatherContourLinesEnabled(WEATHER_PRESSURE_CONTOURS_LINES_ATTR)
				|| isWeatherContourLinesEnabled(WEATHER_CLOUD_CONTOURS_LINES_ATTR)
				|| isWeatherContourLinesEnabled(WEATHER_WIND_CONTOURS_LINES_ATTR)
				|| isWeatherContourLinesEnabled(WEATHER_PRECIPITATION_CONTOURS_LINES_ATTR);
	}

	public boolean isWeatherContourLinesEnabled(@NonNull String attr) {
		return settings.getCustomRenderBooleanProperty(attr).get();
	}

	public void setContoursEnabled(boolean enabled) {
		weatherSettings.weatherContoursEnabled.set(enabled);
		if (!isAnyWeatherContourLinesEnabled()) {
			setContoursType(WeatherContour.TEMPERATURE);
		}
	}

	public int getContoursTransparency() {
		Integer value = weatherSettings.weatherContoursTransparency.get();
		return value != null ? value : WeatherSettings.DEFAULT_TRANSPARENCY;
	}

	public void setContoursTransparency(@NonNull Integer transparency) {
		weatherSettings.weatherContoursTransparency.set(transparency);
	}

	@NonNull
	public WeatherContour getSelectedContoursType() {
		return weatherSettings.weatherContoursType.get();
	}

	public void setSelectedContoursType(@NonNull WeatherContour contoursType) {
		weatherSettings.weatherContoursType.set(contoursType);
		setContoursType(contoursType);
	}

	@Nullable
	public WeatherContour getSelectedForecastContoursType() {
		return weatherSettings.weatherForecastContoursType.get();
	}

	public void setSelectedForecastContoursType(@Nullable WeatherContour contoursType) {
		weatherSettings.weatherForecastContoursType.set(contoursType);
		setContoursType(contoursType);
	}

	public void setContoursType(@Nullable WeatherContour contoursType) {
		CommonPreference<Boolean> temperaturePref = settings.getCustomRenderBooleanProperty(WEATHER_TEMP_CONTOUR_LINES_ATTR);
		CommonPreference<Boolean> pressurePref = settings.getCustomRenderBooleanProperty(WEATHER_PRESSURE_CONTOURS_LINES_ATTR);
		CommonPreference<Boolean> cloudPref = settings.getCustomRenderBooleanProperty(WEATHER_CLOUD_CONTOURS_LINES_ATTR);
		CommonPreference<Boolean> windPref = settings.getCustomRenderBooleanProperty(WEATHER_WIND_CONTOURS_LINES_ATTR);
		CommonPreference<Boolean> precipitationPref = settings.getCustomRenderBooleanProperty(WEATHER_PRECIPITATION_CONTOURS_LINES_ATTR);

		String attrName = contoursType != null ? contoursType.getAttrName() : null;

		temperaturePref.set(WEATHER_TEMP_CONTOUR_LINES_ATTR.equals(attrName));
		pressurePref.set(WEATHER_PRESSURE_CONTOURS_LINES_ATTR.equals(attrName));
		cloudPref.set(WEATHER_CLOUD_CONTOURS_LINES_ATTR.equals(attrName));
		windPref.set(WEATHER_WIND_CONTOURS_LINES_ATTR.equals(attrName));
		precipitationPref.set(WEATHER_PRECIPITATION_CONTOURS_LINES_ATTR.equals(attrName));

		updateMapSettings();
	}

	private void updateMapSettings() {
		MapRendererContext mapContext = NativeCoreContext.getMapRendererContext();
		if (mapContext != null) {
			mapContext.updateMapSettings(true);
		}
		weatherHelper.updateBandsSettings();
	}

	public boolean hasCustomForecast() {
		return forecastDate != null;
	}

	public void setForecastDate(@Nullable Date date, boolean forAnimation, boolean resetPeriod) {
		forecastDate = date;
		updateLayersDate(forAnimation, resetPeriod);
	}

	public void prepareForDayAnimation(@NonNull Date date) {
		forecastDate = date;
		long time = forecastDate.getTime();
		if (weatherLayerLow != null) {
			weatherLayerLow.setDateTime(time, true, false);
		}
		if (weatherLayerHigh != null) {
			weatherLayerHigh.setDateTime(time, true, false);
		}
	}

	private void updateLayersDate(boolean forAnimation, boolean resetPeriod) {
		long time = forecastDate != null ? forecastDate.getTime() : System.currentTimeMillis();
		if (weatherLayerLow != null) {
			weatherLayerLow.setDateTime(time, forAnimation, resetPeriod);
		}
		if (weatherLayerHigh != null) {
			weatherLayerHigh.setDateTime(time, forAnimation, resetPeriod);
		}
		if (weatherContourLayer != null) {
			weatherContourLayer.setDateTime(time);
		}
	}

	@Override
	protected boolean layerShouldBeDisabled(@NonNull OsmandMapLayer layer) {
		return hasCustomForecast() && layer instanceof DownloadedRegionsLayer;
	}

	public void onIndexItemDownloaded(@NonNull IndexItem item, boolean updatingFile) {
		if (item.getType() == WEATHER_FORECAST) {
			weatherHelper.updateForecastCache(item.getTargetFile(app).getAbsolutePath());
		}
	}
}