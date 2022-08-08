package net.osmand.plus.weather;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.chooseplan.button.PurchasingUtils;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.dashboard.DashboardOnMap.DashboardType;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper;
import net.osmand.plus.settings.backend.preferences.EnumStringPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.fragments.BaseSettingsFragment.SettingsScreenType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.weather.units.CloudConstants;
import net.osmand.plus.weather.units.PrecipConstants;
import net.osmand.plus.weather.units.PressureConstants;
import net.osmand.plus.weather.units.TemperatureConstants;
import net.osmand.plus.weather.units.WindConstants;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_WEATHER;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.WEATHER_ID;
import static net.osmand.plus.views.mapwidgets.WidgetType.WX_AIR_PRESSURE_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WX_CLOUDS_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WX_PRECIPITATION_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WX_TEMPERATURE_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WX_WIND_WIDGET;
import static net.osmand.plus.weather.WeatherInfoType.CLOUDS;
import static net.osmand.plus.weather.WeatherInfoType.PRECIPITATION;
import static net.osmand.plus.weather.WeatherInfoType.PRESSURE;
import static net.osmand.plus.weather.WeatherInfoType.TEMPERATURE;
import static net.osmand.plus.weather.WeatherInfoType.WIND;

public class WeatherPlugin extends OsmandPlugin {

	public static int DEFAULT_TRANSPARENCY = 50;

	// todo replace with preferences
	private boolean isWeatherEnabled = false;
	private WeatherInfoType currentConfigureLayer = null;
	private Set<WeatherInfoType> enabledLayers = new HashSet<>();
	private Map<WeatherInfoType, Integer> layersTransparency = new HashMap<>();
	private boolean contoursEnabled = false;
	private Integer contoursTranparency;
	private WeatherInfoType selectedContoursType = TEMPERATURE;

	public WeatherPlugin(@NonNull OsmandApplication app) {
		super(app);

		ApplicationMode[] noAppMode = {};
		WidgetsAvailabilityHelper.regWidgetVisibility(WX_TEMPERATURE_WIDGET, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(WX_PRECIPITATION_WIDGET, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(WX_WIND_WIDGET, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(WX_CLOUDS_WIDGET, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(WX_AIR_PRESSURE_WIDGET, noAppMode);

		EnumStringPreference weatherTemp = (EnumStringPreference) registerEnumStringPreference(TEMPERATURE.getUnitsPrefId(), TemperatureConstants.CELSIUS, TemperatureConstants.values(), TemperatureConstants.class).makeProfile();
		EnumStringPreference weatherPressure = (EnumStringPreference) registerEnumStringPreference(PRESSURE.getUnitsPrefId(), PressureConstants.MILLIMETERS_OF_MERCURY, PressureConstants.values(), PressureConstants.class).makeProfile();
		EnumStringPreference weatherWind = (EnumStringPreference) registerEnumStringPreference(WIND.getUnitsPrefId(), WindConstants.METERS_PER_SECOND, WindConstants.values(), WindConstants.class).makeProfile();
		EnumStringPreference weatherCloud = (EnumStringPreference) registerEnumStringPreference(CLOUDS.getUnitsPrefId(), CloudConstants.PERCENT, CloudConstants.values(), CloudConstants.class).makeProfile();
		EnumStringPreference weatherPrecipitation = (EnumStringPreference) registerEnumStringPreference(PRECIPITATION.getUnitsPrefId(), PrecipConstants.MILIMETERS, PrecipConstants.values(), PrecipConstants.class).makeProfile();
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

	@Nullable
	@Override
	public Drawable getAssetResourceImage() {
		// todo replace with appropriate resource
		return super.getAssetResourceImage();
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
	public SettingsScreenType getSettingsScreenType() {
		return SettingsScreenType.WEATHER;
	}

	@Override
	public void createWidgets(@NonNull MapActivity mapActivity, @NonNull List<MapWidgetInfo> widgetInfos, @NonNull ApplicationMode appMode) {
		MapWidgetRegistry widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();

		MapWidget temperatureWidget = createMapWidgetForParams(mapActivity, WX_TEMPERATURE_WIDGET);
		widgetInfos.add(widgetRegistry.createWidgetInfo(temperatureWidget));

		MapWidget precipitationWidget = createMapWidgetForParams(mapActivity, WX_PRECIPITATION_WIDGET);
		widgetInfos.add(widgetRegistry.createWidgetInfo(precipitationWidget));

		MapWidget windWidget = createMapWidgetForParams(mapActivity, WX_WIND_WIDGET);
		widgetInfos.add(widgetRegistry.createWidgetInfo(windWidget));

		MapWidget cloudsWidget = createMapWidgetForParams(mapActivity, WX_CLOUDS_WIDGET);
		widgetInfos.add(widgetRegistry.createWidgetInfo(cloudsWidget));

		MapWidget airPressureWidget = createMapWidgetForParams(mapActivity, WX_AIR_PRESSURE_WIDGET);
		widgetInfos.add(widgetRegistry.createWidgetInfo(airPressureWidget));
	}

	@Nullable
	@Override
	protected MapWidget createMapWidgetForParams(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType) {
		if (Algorithms.equalsToAny(widgetType, getWidgetTypes())) {
			return new WeatherWidget(mapActivity, widgetType);
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
			boolean selected = isWeatherEnabled(appMode);
			adapter.addItem(new ContextMenuItem(WEATHER_ID)
					.setTitleId(R.string.shared_string_weather, mapActivity)
					.setDescription(selected ? getEnabledLayersSummary() : null)
					.setSecondaryDescription(selected ? app.getString(R.string.shared_string_on) : null)
					.setLayout(R.layout.configure_map_item_with_additional_right_desc)
					.setSelected(selected)
					.setColor(app, selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
					.setIcon(R.drawable.ic_action_umbrella)
					.setListener(listener));
		}
	}

	@NonNull
	public String getEnabledLayersSummary() {
		if (!Algorithms.isEmpty(enabledLayers)) {
			List<String> titles = new ArrayList<>();
			for (WeatherInfoType layer : WeatherInfoType.values()) {
				if (enabledLayers.contains(layer)) {
					titles.add(app.getString(layer.getTitleId()));
				}
			}
			return TextUtils.join(", ", titles);
		}
		return null;
	}

	@NonNull
	public String getEnabledContoursSummary(@NonNull Context ctx) {
		return selectedContoursType.toHumanString(ctx);
	}

	public void setWeatherEnabled(@NonNull ApplicationMode appMode, boolean isWeatherEnabled) {
		this.isWeatherEnabled = isWeatherEnabled;
	}

	public boolean isWeatherEnabled(@NonNull ApplicationMode appMode) {
		return isWeatherEnabled;
	}

	public boolean isContoursEnabled(@NonNull ApplicationMode appMode) {
		return contoursEnabled;
	}

	public void setContoursEnabled(@NonNull ApplicationMode appMode, boolean enabled) {
		this.contoursEnabled = enabled;
	}

	public int getContoursTransparency(@NonNull ApplicationMode appMode) {
		return contoursTranparency != null ? contoursTranparency : DEFAULT_TRANSPARENCY;
	}

	public void setContoursTransparency(@NonNull ApplicationMode appMode, @NonNull Integer transparency) {
		if (transparency != null) {
			// add
			contoursTranparency = transparency;
		} else {
			// remove
			contoursTranparency = null;
		}
	}

	@NonNull
	public WeatherInfoType getSelectedContoursType(@NonNull ApplicationMode appMode) {
		return selectedContoursType;
	}

	public void setSelectedContoursType(@NonNull ApplicationMode appMode, @NonNull WeatherInfoType contoursType) {
		this.selectedContoursType = contoursType;
	}

	public void setCurrentConfigureLayer(@Nullable WeatherInfoType layer) {
		this.currentConfigureLayer = layer;
	}

	@Nullable
	public WeatherInfoType getCurrentConfiguredLayer() {
		return currentConfigureLayer;
	}

	public void toggleLayerEnable(@NonNull ApplicationMode appMode, @NonNull WeatherInfoType layer, boolean enable) {
		if (enable) {
			enabledLayers.add(layer);
		} else {
			enabledLayers.remove(layer);
		}
	}

	public boolean isLayerEnabled(@NonNull ApplicationMode appMode, @NonNull WeatherInfoType layer) {
		return enabledLayers.contains(layer);
	}

	public int getLayerTransparency(@NonNull ApplicationMode appMode, @NonNull WeatherInfoType layer) {
		Integer transparency = layersTransparency.get(layer);
		return transparency != null ? transparency : DEFAULT_TRANSPARENCY;
	}

	public void setLayerTransparency(@NonNull ApplicationMode appMode, @NonNull WeatherInfoType layer, @NonNull Integer transparency) {
		if (transparency != null) {
			layersTransparency.put(layer, transparency);
		} else {
			layersTransparency.remove(layer);
		}
	}

	@NonNull
	public Enum<?> getSelectedLayerUnit(@NonNull ApplicationMode appMode, @NonNull WeatherInfoType layer) {
		OsmandPreference<?> preference = app.getSettings().getPreference(layer.getUnitsPrefId());
		return (Enum<?>) preference.getModeValue(appMode);
	}

	public void setSelectedLayerUnit(@NonNull ApplicationMode appMode, @NonNull WeatherInfoType layer, @NonNull Enum<?> value) {
		OsmandPreference<Enum> preference = (OsmandPreference<Enum>) app.getSettings().getPreference(layer.getUnitsPrefId());
		preference.setModeValue(appMode, value);
	}

	@NonNull
	public static String[] getUnitsPreferencesIds() {
		return new String[]{
				TEMPERATURE.getUnitsPrefId(),
				PRECIPITATION.getUnitsPrefId(),
				WIND.getUnitsPrefId(),
				CLOUDS.getUnitsPrefId(),
				PRESSURE.getUnitsPrefId()
		};
	}

	@NonNull
	private static WidgetType[] getWidgetTypes() {
		return new WidgetType[]{
				WX_TEMPERATURE_WIDGET,
				WX_PRECIPITATION_WIDGET,
				WX_WIND_WIDGET,
				WX_CLOUDS_WIDGET,
				WX_AIR_PRESSURE_WIDGET
		};
	}

}
