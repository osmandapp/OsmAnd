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
import net.osmand.util.Algorithms;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_WEATHER;
import static net.osmand.plus.views.mapwidgets.WidgetType.AV_NOTES_ON_REQUEST;
import static net.osmand.plus.views.mapwidgets.WidgetType.AV_NOTES_RECORD_AUDIO;
import static net.osmand.plus.views.mapwidgets.WidgetType.AV_NOTES_RECORD_VIDEO;
import static net.osmand.plus.views.mapwidgets.WidgetType.AV_NOTES_TAKE_PHOTO;
import static net.osmand.plus.views.mapwidgets.WidgetType.WX_AIR_PRESSURE_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WX_CLOUDS_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WX_PRECIPITATION_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WX_TEMPERATURE_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WX_WIND_WIDGET;

public class WeatherPlugin extends OsmandPlugin {

	public WeatherPlugin(@NonNull OsmandApplication app) {
		super(app);

		ApplicationMode[] noAppMode = {};
		ApplicationMode.regWidgetVisibility(WX_TEMPERATURE_WIDGET, noAppMode);
		ApplicationMode.regWidgetVisibility(WX_PRECIPITATION_WIDGET, noAppMode);
		ApplicationMode.regWidgetVisibility(WX_WIND_WIDGET, noAppMode);
		ApplicationMode.regWidgetVisibility(WX_CLOUDS_WIDGET, noAppMode);
		ApplicationMode.regWidgetVisibility(WX_AIR_PRESSURE_WIDGET, noAppMode);

		EnumStringPreference weatherTemp = (EnumStringPreference) registerEnumStringPreference("map_settings_weather_temp", TemperatureConstants.CELSIUS, TemperatureConstants.values(), TemperatureConstants.class).makeProfile();
		EnumStringPreference weatherPressure = (EnumStringPreference) registerEnumStringPreference("map_settings_weather_pressure", PressureConstants.MILLIMETERS_OF_MERCURY, PressureConstants.values(), PressureConstants.class).makeProfile();
		EnumStringPreference weatherWind = (EnumStringPreference) registerEnumStringPreference("map_settings_weather_wind", TemperatureConstants.CELSIUS, TemperatureConstants.values(), TemperatureConstants.class).makeProfile();
		EnumStringPreference weatherCloud = (EnumStringPreference) registerEnumStringPreference("map_settings_weather_cloud", TemperatureConstants.CELSIUS, TemperatureConstants.values(), TemperatureConstants.class).makeProfile();
		EnumStringPreference weatherPrecipitation = (EnumStringPreference) registerEnumStringPreference("map_settings_weather_precip", TemperatureConstants.CELSIUS, TemperatureConstants.values(), TemperatureConstants.class).makeProfile();
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

	private WidgetType[] getWidgetTypes() {
		return new WidgetType[] {
				WX_TEMPERATURE_WIDGET,
				WX_PRECIPITATION_WIDGET,
				WX_WIND_WIDGET,
				WX_CLOUDS_WIDGET,
				WX_AIR_PRESSURE_WIDGET
		};
	}

}
