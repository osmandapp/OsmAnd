package net.osmand.plus.weather;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.EnumStringPreference;
import net.osmand.plus.settings.fragments.BaseSettingsFragment.SettingsScreenType;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.weather.units.CloudConstants;
import net.osmand.plus.weather.units.PrecipConstants;
import net.osmand.plus.weather.units.PressureConstants;
import net.osmand.plus.weather.units.TemperatureConstants;
import net.osmand.plus.weather.units.WindConstants;
import net.osmand.util.Algorithms;

import java.util.List;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_WEATHER;
import static net.osmand.plus.views.mapwidgets.WidgetType.WX_AIR_PRESSURE_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WX_CLOUDS_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WX_PRECIPITATION_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WX_TEMPERATURE_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WX_WIND_WIDGET;

public class WeatherPlugin extends OsmandPlugin {

	public static final String PREFERENCE_ID_TEMPERATURE = "map_settings_weather_temp";
	public static final String PREFERENCE_ID_PRESSURE = "map_settings_weather_pressure";
	public static final String PREFERENCE_ID_WIND = "map_settings_weather_wind";
	public static final String PREFERENCE_ID_CLOUDS = "map_settings_weather_cloud";
	public static final String PREFERENCE_ID_PRECIP = "map_settings_weather_precip";

	public WeatherPlugin(@NonNull OsmandApplication app) {
		super(app);

		ApplicationMode[] noAppMode = {};
		ApplicationMode.regWidgetVisibility(WX_TEMPERATURE_WIDGET, noAppMode);
		ApplicationMode.regWidgetVisibility(WX_PRECIPITATION_WIDGET, noAppMode);
		ApplicationMode.regWidgetVisibility(WX_WIND_WIDGET, noAppMode);
		ApplicationMode.regWidgetVisibility(WX_CLOUDS_WIDGET, noAppMode);
		ApplicationMode.regWidgetVisibility(WX_AIR_PRESSURE_WIDGET, noAppMode);

		EnumStringPreference weatherTemp = (EnumStringPreference) registerEnumStringPreference(PREFERENCE_ID_TEMPERATURE, TemperatureConstants.CELSIUS, TemperatureConstants.values(), TemperatureConstants.class).makeProfile();
		EnumStringPreference weatherPressure = (EnumStringPreference) registerEnumStringPreference(PREFERENCE_ID_PRESSURE, PressureConstants.MILLIMETERS_OF_MERCURY, PressureConstants.values(), PressureConstants.class).makeProfile();
		EnumStringPreference weatherWind = (EnumStringPreference) registerEnumStringPreference(PREFERENCE_ID_WIND, WindConstants.METERS_PER_SECOND, WindConstants.values(), WindConstants.class).makeProfile();
		EnumStringPreference weatherCloud = (EnumStringPreference) registerEnumStringPreference(PREFERENCE_ID_CLOUDS, CloudConstants.PERCENT, CloudConstants.values(), CloudConstants.class).makeProfile();
		EnumStringPreference weatherPrecipitation = (EnumStringPreference) registerEnumStringPreference(PREFERENCE_ID_PRECIP, PrecipConstants.MILIMETERS, PrecipConstants.values(), PrecipConstants.class).makeProfile();
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
//		return R.drawable.img_plugin_weather;
		return R.drawable.ic_extension_dark;
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

	@NonNull
	public static String[] getUnitsPreferencesIds() {
		return new String[]{
				PREFERENCE_ID_TEMPERATURE,
				PREFERENCE_ID_PRESSURE,
				PREFERENCE_ID_WIND,
				PREFERENCE_ID_CLOUDS,
				PREFERENCE_ID_PRECIP
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
