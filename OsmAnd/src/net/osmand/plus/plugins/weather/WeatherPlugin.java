package net.osmand.plus.plugins.weather;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_WEATHER;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.WEATHER_ID;
import static net.osmand.plus.chooseplan.OsmAndFeature.WEATHER;
import static net.osmand.plus.plugins.weather.WeatherSettings.WEATHER_PRESSURE_CONTOURS_LINES_ATTR;
import static net.osmand.plus.plugins.weather.WeatherSettings.WEATHER_TEMP_CONTOUR_LINES_ATTR;
import static net.osmand.plus.settings.fragments.BaseSettingsFragment.SettingsScreenType.WEATHER_SETTINGS;
import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_AIR_PRESSURE_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_CLOUDS_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_PRECIPITATION_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_TEMPERATURE_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_WIND_WIDGET;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
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
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.EnumStringPreference;
import net.osmand.plus.settings.fragments.BaseSettingsFragment.SettingsScreenType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetInfoCreator;
import net.osmand.plus.views.mapwidgets.WidgetType;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class WeatherPlugin extends OsmandPlugin {

	private static final Log log = PlatformUtil.getLog(WeatherPlugin.class);

	private WeatherInfoType currentConfigureLayer = null;

	private WeatherRasterLayer weatherLayerLow;
	private WeatherRasterLayer weatherLayerHigh;
	private static final float ZORDER_RASTER_LOW = 0.8f;
	private static final float ZORDER_RASTER_HIGH = 0.81f;
	private WeatherContourLayer weatherContourLayer;
	private static final float ZORDER_CONTOURS = 0.82f;

	private WeatherHelper weatherHelper;
	private WeatherSettings weatherSettings;


	public WeatherPlugin(@NonNull OsmandApplication app) {
		super(app);
		weatherHelper = app.getWeatherHelper();
		weatherSettings = weatherHelper.getWeatherSettings();

		ApplicationMode[] noAppMode = {};
		WidgetsAvailabilityHelper.regWidgetVisibility(WEATHER_TEMPERATURE_WIDGET, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(WEATHER_PRECIPITATION_WIDGET, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(WEATHER_WIND_WIDGET, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(WEATHER_CLOUDS_WIDGET, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(WEATHER_AIR_PRESSURE_WIDGET, noAppMode);
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
		return app.useOpenGlRenderer() && super.isEnabled();
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
	protected List<QuickActionType> getQuickActionTypes() {
		List<QuickActionType> action = new ArrayList<>();
		action.add(ShowHideTemperatureLayerAction.TYPE);
		action.add(ShowHideWindLayerAction.TYPE);
		action.add(ShowHideAirPressureLayerAction.TYPE);
		action.add(ShowHidePrecipitationLayerAction.TYPE);
		action.add(ShowHideCloudLayerAction.TYPE);
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
			OsmandApplication app = mapActivity.getMyApplication();
			ApplicationMode appMode = app.getSettings().getApplicationMode();

			boolean selected = isWeatherEnabled(appMode);
			adapter.addItem(new ContextMenuItem(WEATHER_ID)
					.setTitleId(R.string.shared_string_weather, mapActivity)
					.setDescription(selected ? getWeatherTypesSummary(getEnabledLayers(appMode)) : null)
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
				dashboard.setDashboardVisibility(true, DashboardType.WEAHTER, coordinates);
				return false;
			}

			@Override
			public boolean onContextMenuClick(@Nullable OnDataChangeUiAdapter uiAdapter,
			                                  @Nullable View view, @NotNull ContextMenuItem item,
			                                  boolean isChecked) {
				weatherSettings.WX_ENABLED.set(isChecked);
				item.setSelected(weatherSettings.WX_ENABLED.get());
				item.setColor(app, weatherSettings.WX_ENABLED.get() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
				item.setDescription(isChecked ? getWeatherTypesSummary(getEnabledLayers(app.getSettings().getApplicationMode())) : null);
				if (uiAdapter != null) {
					uiAdapter.onDataSetChanged();
				}
				mapActivity.refreshMapComplete();
				return true;
			}
		};
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

	private void createLayers() {
		weatherLayerLow = new WeatherRasterLayer(app, WeatherLayer.LOW);
		weatherLayerHigh = new WeatherRasterLayer(app, WeatherLayer.HIGH);
		weatherContourLayer = new WeatherContourLayer(app);
	}

	public void setWeatherEnabled(@NonNull ApplicationMode appMode, boolean enable) {
		weatherSettings.WX_ENABLED.setModeValue(appMode, enable);
	}

	public boolean isWeatherEnabled(@NonNull ApplicationMode appMode) {
		return weatherSettings.WX_ENABLED.getModeValue(appMode);
	}

	public boolean isAnyDataVisible(@NonNull ApplicationMode appMode) {
		boolean isAnyLayerEnabled = !Algorithms.isEmpty(getEnabledLayers(appMode));
		boolean isContoursEnabled = isContoursEnabled(appMode);
		return isWeatherEnabled(appMode) && (isAnyLayerEnabled || isContoursEnabled);
	}

	public boolean isContoursEnabled(@NonNull ApplicationMode appMode) {
		return weatherSettings.WX_CONTOURS_ENABLED.getModeValue(appMode);
	}

	public void setContoursEnabled(@NonNull ApplicationMode appMode, boolean enabled) {
		weatherSettings.WX_CONTOURS_ENABLED.setModeValue(appMode, enabled);

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
		Integer value = weatherSettings.WX_CONTOURS_TRANSPARENCY.getModeValue(appMode);
		return value != null ? value : WeatherSettings.DEFAULT_TRANSPARENCY;
	}

	public void setContoursTransparency(@NonNull ApplicationMode appMode, @NonNull Integer transparency) {
		weatherSettings.WX_CONTOURS_TRANSPARENCY.setModeValue(appMode, transparency);
	}

	@NonNull
	public WeatherInfoType getSelectedContoursType(@NonNull ApplicationMode appMode) {
		return weatherSettings.WX_CONTOURS_TYPE.getModeValue(appMode);
	}

	public void setSelectedContoursType(@NonNull ApplicationMode appMode, @NonNull WeatherInfoType contoursType) {
		weatherSettings.WX_CONTOURS_TYPE.setModeValue(appMode, contoursType);
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
		String storedValue = weatherSettings.WX_ENABLED_LAYERS.getModeValue(appMode);
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
		weatherSettings.WX_ENABLED_LAYERS.setModeValues(appMode, valueToSave);
	}

	public boolean isLayerEnabled(@NonNull ApplicationMode appMode, @NonNull WeatherInfoType layer) {
		return getEnabledLayers(appMode).contains(layer);
	}

	public int getLayerTransparency(@NonNull ApplicationMode appMode, @NonNull WeatherInfoType layer) {
		Integer value = getLayersTransparencies(appMode).get(layer);
		return value != null ? value : weatherSettings.DEFAULT_TRANSPARENCY;
	}

	public void setLayerTransparency(@NonNull ApplicationMode appMode, @NonNull WeatherInfoType layer, @NonNull Integer transparency) {
		Map<WeatherInfoType, Integer> transparencies = getLayersTransparencies(appMode);
		transparencies.put(layer, transparency);
		List<String> valuesToSave = new ArrayList<>();
		for (Entry<WeatherInfoType, Integer> layerTransp : transparencies.entrySet()) {
			valuesToSave.add(layerTransp.getKey().name() + ":" + layerTransp.getValue());
		}
		weatherSettings.WX_LAYERS_TRANSPARENCY.setModeValues(appMode, valuesToSave);
		weatherHelper.updateBandsSettings();
	}

	@NonNull
	public Map<WeatherInfoType, Integer> getLayersTransparencies(@NonNull ApplicationMode appMode) {
		Map<WeatherInfoType, Integer> transparencies = new HashMap<>();
		List<String> storedValues = weatherSettings.WX_LAYERS_TRANSPARENCY.getStringsListForProfile(appMode);
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
				return weatherSettings.weatherTempUnit;
			case PRESSURE:
				return weatherSettings.weatherPressureUnit;
			case WIND:
				return weatherSettings.weatherWindUnit;
			case CLOUDS:
				return weatherSettings.weatherCloudUnit;
			case PRECIPITATION:
				return weatherSettings.weatherPrecipUnit;
			default:
				return null;
		}
	}

	@NonNull
	public String[] getUnitsPreferencesIds() {
		return new String[] {
				weatherSettings.weatherTempUnit.getId(),
				weatherSettings.weatherPrecipUnit.getId(),
				weatherSettings.weatherWindUnit.getId(),
				weatherSettings.weatherCloudUnit.getId(),
				weatherSettings.weatherPressureUnit.getId()
		};
	}
}