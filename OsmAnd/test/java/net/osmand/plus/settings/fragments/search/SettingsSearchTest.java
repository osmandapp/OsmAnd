package net.osmand.plus.settings.fragments.search;

import android.content.Context;
import android.os.AsyncTask;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import net.osmand.map.TileSourceManager;

import static net.osmand.map.TileSourceManager.TileSourceTemplate;
import static net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin.createDownloadTileSourceTemplatesTask;
import static net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin.waitFor;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.accessibility.AccessibilityPlugin;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.plugins.weather.WeatherPlugin;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.DayNightMode;
import net.osmand.test.common.AndroidTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@LargeTest
@RunWith(Parameterized.class)
public class SettingsSearchTest extends AndroidTest {

	@Parameterized.Parameter(value = 0)
	public String description;

	@Parameterized.Parameter(value = 1)
	public ISettingsSearchTest settingsSearchTest;

	@Parameterized.Parameters(name = "{0}")
	public static Iterable<Object[]> data() {
		return Arrays.asList(
				new Object[][]{
						{
								"RecalculateRoute",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.recalculate_route);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"AnnouncementTimeBottomSheet: title",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.announcement_time_title);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"AnnouncementTimeBottomSheet: description",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.announcement_time_descr);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"AnnouncementTimeBottomSheet: time intervals",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.announcement_time_intervals);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"FuelTankCapacityBottomSheet: title",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.fuel_tank_capacity);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"FuelTankCapacityBottomSheet: description",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.fuel_tank_capacity_description);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"RecalculateRouteInDeviationBottomSheet: title",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.recalculate_route_in_deviation);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"RecalculateRouteInDeviationBottomSheet: description",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.select_distance_route_will_recalc);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"RecalculateRouteInDeviationBottomSheet: longDescription",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.recalculate_route_distance_promo);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"ScreenTimeoutBottomSheet: description",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.system_screen_timeout_descr);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"GoodsRestrictionsBottomSheet: title",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.routing_attr_goods_restrictions_name);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"GoodsRestrictionsBottomSheet: goods_delivery_desc",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.goods_delivery_desc);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"GoodsRestrictionsBottomSheet: goods_delivery_desc_2",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.goods_delivery_desc_2);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"GoodsRestrictionsBottomSheet: goods_delivery_desc_3",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.goods_delivery_desc_3);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"GoodsRestrictionsBottomSheet: goods_delivery_desc_4",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.goods_delivery_desc_4);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"SendAnalyticsBottomSheetDialogFragment: description",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.make_osmand_better_descr);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"ProfileAppearanceFragment: view_angle_description",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.view_angle_description);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"ProfileAppearanceFragment: location_radius_description",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.location_radius_description);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"RouteParametersFragment: title",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.route_recalculation_dist_title);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"ResetProfilePrefsBottomSheet: title",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.reset_all_profile_settings);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"ResetProfilePrefsBottomSheet: description",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.reset_all_profile_settings_descr);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"ResetProfilePrefsBottomSheet: reset_confirmation_descr",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return "Tapping Reset discards all your changes";
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"GeneralProfileSettingsFragment",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.distance_during_navigation);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"DistanceDuringNavigationBottomSheet: description",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return "Choose how distance information is displayed in navigation widgets";
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"VehicleParametersFragment: SimpleSingleSelectionBottomSheet, description",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.routing_attr_motor_type_description);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"VoiceLanguageBottomSheetFragment: language_description",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.language_description);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"VoiceLanguageBottomSheetFragment: tts_description",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.tts_description);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"VoiceLanguageBottomSheetFragment: recorded_description",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.recorded_description);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"WakeTimeBottomSheet: description",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.turn_screen_on_wake_time_descr, context.getString(R.string.keep_screen_on));
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"WakeTimeBottomSheet: keep_screen_on",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.keep_screen_on);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"WakeTimeBottomSheet: timeoutDescription",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.screen_timeout_descr, context.getString(R.string.system_screen_timeout));
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"SelectNavProfileBottomSheet: header",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.select_nav_profile_dialog_message);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"SelectDefaultProfileBottomSheet: description",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.profile_by_default_description);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"SelectDefaultProfileBottomSheet: car profile",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return ApplicationMode.CAR.toHumanString();
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"SelectBaseProfileBottomSheet: title",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.select_base_profile_dialog_title);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"SelectBaseProfileBottomSheet: longDescription",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.select_base_profile_dialog_message);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"ConfigureScreenFragment",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.configure_screen_widgets_descr);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"search_map_rendering_engine_v1_find_map_rendering_engine",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.map_rendering_engine_v1);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(context.getString(R.string.map_rendering_engine));
									}
								}
						},
						{
								"search_map_rendering_engine_v2_find_map_rendering_engine",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.map_rendering_engine_v2);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(context.getString(R.string.map_rendering_engine));
									}
								}
						},
						{
								"search_ApplicationMode_find_SelectCopyAppModeBottomSheet",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return ApplicationMode.PEDESTRIAN.toHumanString();
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(
												String.format(
														"Path: Driving > %s",
														context.getString(R.string.copy_from_other_profile)));
									}
								}
						},
						{
								"shouldSearchAndFind_ResetProfilePrefsBottomSheet_within_AccessibilityPlugin",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.reset_all_profile_settings_descr);
									}

									@Override
									protected Set<Class<? extends OsmandPlugin>> getEnabledPluginClasses() {
										return Set.of(AccessibilityPlugin.class);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(String.format("Path: Driving > %s > Reset plugin settings to default", Iterables.getOnlyElement(enabledPlugins).getName()));
									}
								}
						},
						{
								"shouldSearchAndFind_ResetProfilePrefsBottomSheet_within_AudioVideoNotesPlugin",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.reset_all_profile_settings_descr);
									}

									@Override
									protected Set<Class<? extends OsmandPlugin>> getEnabledPluginClasses() {
										return Set.of(AudioVideoNotesPlugin.class);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(String.format("Path: Driving > %s > Reset plugin settings to default", Iterables.getOnlyElement(enabledPlugins).getName()));
									}
								}
						},
						{
								"shouldSearchAndFind_ResetProfilePrefsBottomSheet_within_OsmandMonitoringPlugin",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.reset_all_profile_settings_descr);
									}

									@Override
									protected Set<Class<? extends OsmandPlugin>> getEnabledPluginClasses() {
										return Set.of(OsmandMonitoringPlugin.class);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(String.format("Path: Driving > %s > Reset plugin settings to default", Iterables.getOnlyElement(enabledPlugins).getName()));
									}
								}
						},
						{
								"shouldSearchAndFind_ResetProfilePrefsBottomSheet_within_WeatherPlugin",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.reset_all_profile_settings_descr);
									}

									@Override
									protected Set<Class<? extends OsmandPlugin>> getEnabledPluginClasses() {
										return Set.of(WeatherPlugin.class);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(String.format("Path: Driving > %s > Reset plugin settings to default", Iterables.getOnlyElement(enabledPlugins).getName()));
									}
								}
						},
						{
								"shouldSearchAndFind_SelectCopyAppModeBottomSheet_within_OsmandMonitoringPlugin",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return ApplicationMode.PEDESTRIAN.toHumanString();
									}

									@Override
									protected Set<Class<? extends OsmandPlugin>> getEnabledPluginClasses() {
										return Set.of(OsmandMonitoringPlugin.class);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(
												String.format(
														"Path: Driving > %s > %s",
														Iterables.getOnlyElement(enabledPlugins).getName(),
														context.getString(R.string.copy_from_other_profile)));
									}
								}
						},
						{
								"shouldSearchAndFind_SelectCopyAppModeBottomSheet_within_AccessibilityPlugin",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return ApplicationMode.PEDESTRIAN.toHumanString();
									}

									@Override
									protected Set<Class<? extends OsmandPlugin>> getEnabledPluginClasses() {
										return Set.of(AccessibilityPlugin.class);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(
												String.format(
														"Path: Driving > %s > %s",
														Iterables.getOnlyElement(enabledPlugins).getName(),
														context.getString(R.string.copy_from_other_profile)));
									}
								}
						},
						{
								"shouldSearchAndFind_SelectCopyAppModeBottomSheet_within_AudioVideoNotesPlugin",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return ApplicationMode.PEDESTRIAN.toHumanString();
									}

									@Override
									protected Set<Class<? extends OsmandPlugin>> getEnabledPluginClasses() {
										return Set.of(AudioVideoNotesPlugin.class);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(
												String.format(
														"Path: Driving > %s > %s",
														Iterables.getOnlyElement(enabledPlugins).getName(),
														context.getString(R.string.copy_from_other_profile)));
									}
								}
						},
						{
								"shouldSearchAndFind_SelectCopyAppModeBottomSheet_within_WeatherPlugin",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return ApplicationMode.PEDESTRIAN.toHumanString();
									}

									@Override
									protected Set<Class<? extends OsmandPlugin>> getEnabledPluginClasses() {
										return Set.of(WeatherPlugin.class);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(
												String.format(
														"Path: Driving > %s > %s",
														Iterables.getOnlyElement(enabledPlugins).getName(),
														context.getString(R.string.copy_from_other_profile)));
									}
								}
						},
						{
								"shouldSearchAndFind_LocationInterpolationBottomSheet_title",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.location_interpolation_percent);
									}

									@Override
									protected Set<Class<? extends OsmandPlugin>> getEnabledPluginClasses() {
										return Set.of(OsmandDevelopmentPlugin.class);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(context.getString(R.string.location_interpolation_percent));
									}
								}
						},
						{
								"shouldSearchAndFind_LocationInterpolationBottomSheet_description",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.location_interpolation_percent);
									}

									@Override
									protected Set<Class<? extends OsmandPlugin>> getEnabledPluginClasses() {
										return Set.of(OsmandDevelopmentPlugin.class);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(context.getString(R.string.location_interpolation_percent_desc));
									}
								}
						},
						{
								"shouldSearchAndFindProfileAppearanceSettings4EachEnabledApplicationMode",
								new SettingsSearchTestTemplate() {

									@Override
									protected void initializeTest(final OsmandApplication app) {
										Stream
												.of(ApplicationMode.CAR, ApplicationMode.MOPED)
												.forEach(applicationMode -> ApplicationMode.changeProfileAvailability(applicationMode, true, app));
									}

									@Override
									protected String getSearchQuery(final Context context) {
										return "profile appearance";
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return Stream
												.of("Driving", "Moped")
												.map(applicationMode -> String.format("Path: %s > Profile appearance", applicationMode))
												.toList();
									}
								}
						},
						{
								"shouldNotFindProfileAppearanceSettings4DisabledApplicationModes",
								new SettingsSearchTestTemplate() {

									@Override
									protected void initializeTest(final OsmandApplication app) {
										ApplicationMode.changeProfileAvailability(ApplicationMode.CAR, false, app);
									}

									@Override
									protected String getSearchQuery(final Context context) {
										return "profile appearance";
									}

									@Override
									protected List<String> getForbiddenSearchResults(final Context context,
																					 final Set<OsmandPlugin> enabledPlugins,
																					 final Set<OsmandPlugin> disabledPlugins) {
										return List.of("Path: Driving > Profile appearance");
									}
								}
						},
						{
								"shouldSearchAndFindSpeedCameraSettings4EachApplicationMode",
								new SettingsSearchTestTemplate() {

									@Override
									protected void initializeTest(final OsmandApplication app) {
										Stream
												.of(ApplicationMode.CAR, ApplicationMode.TRUCK)
												.forEach(applicationMode -> ApplicationMode.changeProfileAvailability(applicationMode, true, app));
									}

									@Override
									protected String getSearchQuery(final Context context) {
										return "speed cameras";
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return Stream
												.of("Driving", "Truck")
												.map(applicationMode -> String.format("Path: %s > Navigation settings > Screen alerts > Speed cameras", applicationMode))
												.toList();
									}
								}
						},
						{
								"shouldSearchAndFind_ConfigureMenuRootFragment_description",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.ui_customization_description, context.getString(R.string.prefs_plugins));
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"shouldSearchAndFind_ConfigureScreenFragment_ConfigureMap_TextSize",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.text_size);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"shouldSearchAndFind_ConfigureScreenFragment_ConfigureMap_DetailsBottomSheet_WaterWayTunnels",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.rendering_attr_showWaterwayTunnels_name);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"shouldSearchAndFind_ConfigureScreenFragment_ConfigureMap_TransportLinesFragment_TransportStops",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.rendering_attr_transportStops_name);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"shouldSearchAndFind_ConfigureScreenFragment_ConfigureMap_MapStyles_WinterAndSki",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.winter_and_ski_renderer);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"shouldSearchAndFind_ConfigureScreenFragment_ConfigureMap_RoadStyles_highContrastRoads",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.rendering_value_highContrastRoads_name);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"shouldSearchAndFind_ConfigureScreenFragment_ConfigureMap_MapLanguage_Afrikaans",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.lang_af);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"shouldSearchAndFind_ConfigureScreenFragment_ConfigureMap_Hide_HouseNumbers",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.rendering_attr_hideHouseNumbers_name);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"shouldSearchAndFind_ConfigureScreenFragment_ConfigureMap_MapMode_sunrise",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return DayNightMode.AUTO.toHumanString(context);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}
								}
						},
						{
								"shouldSearchAndFind_ConfigureScreenFragment_ConfigureMap_MapSource_onlineTiles_onlineMapsPluginActive",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return getOnlineTiles();
									}

									@Override
									protected Set<Class<? extends OsmandPlugin>> getEnabledPluginClasses() {
										return Set.of(OsmandRasterMapsPlugin.class);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}

									private static String getOnlineTiles() {
										return TileSourceManager.getMapnikSource().getName();
									}
								}
						},
						{
								"shouldSearchAndFind_ConfigureScreenFragment_ConfigureMap_MapSource_onlineTiles_onlineMapsPluginInactive",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return getOnlineTiles();
									}

									@Override
									protected Set<Class<? extends OsmandPlugin>> getDisabledPluginClasses() {
										return Set.of(OsmandRasterMapsPlugin.class);
									}

									@Override
									protected List<String> getForbiddenSearchResults(final Context context,
																					 final Set<OsmandPlugin> enabledPlugins,
																					 final Set<OsmandPlugin> disabledPlugins) {
										return List.of(getSearchQuery(context));
									}

									private static String getOnlineTiles() {
										return TileSourceManager.getMapnikSource().getName();
									}
								}
						},
						{
								"shouldSearchAndFind_ConfigureScreenFragment_ConfigureMap_MapSource_nonInstalledTileSource",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return "Microsoft Maps";
									}

									@Override
									protected Set<Class<? extends OsmandPlugin>> getEnabledPluginClasses() {
										return Set.of(OsmandRasterMapsPlugin.class);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context,
																					final Set<OsmandPlugin> enabledPlugins,
																					final Set<OsmandPlugin> disabledPlugins) {

										return List.of(
												"Path: Driving > Configure map > Map source… > Add more…",
												getSearchQuery(context));
									}
								}
						},
				});
	}

	@Rule
	public ActivityScenarioRule<MapActivity> activityScenarioRule = new ActivityScenarioRule<>(MapActivity.class);

	@Test
	public void testSearchAndFind() {
		settingsSearchTest.testSearchAndFind(app);
	}
}
