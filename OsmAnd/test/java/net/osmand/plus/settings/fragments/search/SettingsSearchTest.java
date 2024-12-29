package net.osmand.plus.settings.fragments.search;

import static net.osmand.plus.settings.fragments.search.SettingsSearchTestFactory.searchQueryAndExpectedSearchResult;

import android.content.Context;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.accessibility.AccessibilityPlugin;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.plugins.weather.WeatherPlugin;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.test.common.AndroidTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;
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
						{"RecalculateRoute", searchQueryAndExpectedSearchResult(R.string.recalculate_route)},

						{"AnnouncementTimeBottomSheet: title", searchQueryAndExpectedSearchResult(R.string.announcement_time_title)},
						{"AnnouncementTimeBottomSheet: description", searchQueryAndExpectedSearchResult(R.string.announcement_time_descr)},
						{"AnnouncementTimeBottomSheet: time intervals", searchQueryAndExpectedSearchResult(R.string.announcement_time_intervals)},

						{"FuelTankCapacityBottomSheet: title", searchQueryAndExpectedSearchResult(R.string.fuel_tank_capacity)},
						{"FuelTankCapacityBottomSheet: description", searchQueryAndExpectedSearchResult(R.string.fuel_tank_capacity_description)},

						{"RecalculateRouteInDeviationBottomSheet: title", searchQueryAndExpectedSearchResult(R.string.recalculate_route_in_deviation)},
						{"RecalculateRouteInDeviationBottomSheet: description", searchQueryAndExpectedSearchResult(R.string.select_distance_route_will_recalc)},
						{"RecalculateRouteInDeviationBottomSheet: longDescription", searchQueryAndExpectedSearchResult(R.string.recalculate_route_distance_promo)},

						{"ScreenTimeoutBottomSheet: description", searchQueryAndExpectedSearchResult(R.string.system_screen_timeout_descr)},

						{"GoodsRestrictionsBottomSheet: title", searchQueryAndExpectedSearchResult(R.string.routing_attr_goods_restrictions_name)},
						{"GoodsRestrictionsBottomSheet: goods_delivery_desc", searchQueryAndExpectedSearchResult(R.string.goods_delivery_desc)},
						{"GoodsRestrictionsBottomSheet: goods_delivery_desc_2", searchQueryAndExpectedSearchResult(R.string.goods_delivery_desc_2)},
						{"GoodsRestrictionsBottomSheet: goods_delivery_desc_3", searchQueryAndExpectedSearchResult(R.string.goods_delivery_desc_3)},
						{"GoodsRestrictionsBottomSheet: goods_delivery_desc_4", searchQueryAndExpectedSearchResult(R.string.goods_delivery_desc_4)},

						{"SendAnalyticsBottomSheetDialogFragment: description", searchQueryAndExpectedSearchResult(R.string.make_osmand_better_descr)},

						{"ProfileAppearanceFragment: view_angle_description", searchQueryAndExpectedSearchResult(R.string.view_angle_description)},
						{"ProfileAppearanceFragment: location_radius_description", searchQueryAndExpectedSearchResult(R.string.location_radius_description)},

						{"RouteParametersFragment: title", searchQueryAndExpectedSearchResult(R.string.route_recalculation_dist_title)},

						{"ResetProfilePrefsBottomSheet: title", searchQueryAndExpectedSearchResult(R.string.reset_all_profile_settings)},
						{"ResetProfilePrefsBottomSheet: description", searchQueryAndExpectedSearchResult(R.string.reset_all_profile_settings_descr)},
						{"ResetProfilePrefsBottomSheet: reset_confirmation_descr", searchQueryAndExpectedSearchResult("Tapping Reset discards all your changes")},

						{"GeneralProfileSettingsFragment", searchQueryAndExpectedSearchResult(R.string.distance_during_navigation)},

						{"DistanceDuringNavigationBottomSheet: description", searchQueryAndExpectedSearchResult("Choose how distance information is displayed in navigation widgets")},

						{"VehicleParametersFragment: SimpleSingleSelectionBottomSheet, description", searchQueryAndExpectedSearchResult(R.string.routing_attr_motor_type_description)},

						{"VoiceLanguageBottomSheetFragment: language_description", searchQueryAndExpectedSearchResult(R.string.language_description)},
						{"VoiceLanguageBottomSheetFragment: tts_description", searchQueryAndExpectedSearchResult(R.string.tts_description)},
						{"VoiceLanguageBottomSheetFragment: recorded_description", searchQueryAndExpectedSearchResult(R.string.recorded_description)},

						{"WakeTimeBottomSheet: description", searchQueryAndExpectedSearchResult(context -> context.getString(R.string.turn_screen_on_wake_time_descr, context.getString(R.string.keep_screen_on)))},
						{"WakeTimeBottomSheet: keep_screen_on", searchQueryAndExpectedSearchResult(R.string.keep_screen_on)},
						{"WakeTimeBottomSheet: timeoutDescription", searchQueryAndExpectedSearchResult(context -> context.getString(R.string.screen_timeout_descr, context.getString(R.string.system_screen_timeout)))},

						{"SelectNavProfileBottomSheet: header", searchQueryAndExpectedSearchResult(R.string.select_nav_profile_dialog_message)},

						{"SelectDefaultProfileBottomSheet: description", searchQueryAndExpectedSearchResult(R.string.profile_by_default_description)},
						{"SelectDefaultProfileBottomSheet: car profile", searchQueryAndExpectedSearchResult(ApplicationMode.CAR.toHumanString())},

						{"SelectBaseProfileBottomSheet: title", searchQueryAndExpectedSearchResult(R.string.select_base_profile_dialog_title)},
						{"SelectBaseProfileBottomSheet: longDescription", searchQueryAndExpectedSearchResult(R.string.select_base_profile_dialog_message)},

						{"ConfigureScreenFragment", searchQueryAndExpectedSearchResult(R.string.configure_screen_widgets_descr)},

						{
								"search_map_rendering_engine_v1_find_map_rendering_engine",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.map_rendering_engine_v1);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context) {
										return List.of(context.getString(R.string.map_rendering_engine));
									}
								}},
						{
								"search_map_rendering_engine_v2_find_map_rendering_engine",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.map_rendering_engine_v2);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context) {
										return List.of(context.getString(R.string.map_rendering_engine));
									}
								}},

						{
								"search_ApplicationMode_find_SelectCopyAppModeBottomSheet",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return ApplicationMode.PEDESTRIAN.toHumanString();
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context) {
										return List.of(
												String.format(
														"Path: Driving > %s",
														context.getString(R.string.copy_from_other_profile)));
									}
								}
						},

						{
								"shouldSearchAndFind_ResetProfilePrefsBottomSheet_within_AccessibilityPlugin",
								new SettingsSearchWithPluginTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.reset_all_profile_settings_descr);
									}

									@Override
									protected Class<? extends OsmandPlugin> getPluginClass() {
										return AccessibilityPlugin.class;
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context, final OsmandPlugin osmandPlugin) {
										return List.of(String.format("Path: Driving > %s > Reset plugin settings to default", osmandPlugin.getName()));
									}
								}
						},

						{
								"shouldSearchAndFind_ResetProfilePrefsBottomSheet_within_AudioVideoNotesPlugin",
								new SettingsSearchWithPluginTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.reset_all_profile_settings_descr);
									}

									@Override
									protected Class<? extends OsmandPlugin> getPluginClass() {
										return AudioVideoNotesPlugin.class;
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context, final OsmandPlugin osmandPlugin) {
										return List.of(String.format("Path: Driving > %s > Reset plugin settings to default", osmandPlugin.getName()));
									}
								}
						},

						{
								"shouldSearchAndFind_ResetProfilePrefsBottomSheet_within_OsmandMonitoringPlugin",
								new SettingsSearchWithPluginTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.reset_all_profile_settings_descr);
									}

									@Override
									protected Class<? extends OsmandPlugin> getPluginClass() {
										return OsmandMonitoringPlugin.class;
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context, final OsmandPlugin osmandPlugin) {
										return List.of(String.format("Path: Driving > %s > Reset plugin settings to default", osmandPlugin.getName()));
									}
								}
						},

						{
								"shouldSearchAndFind_ResetProfilePrefsBottomSheet_within_WeatherPlugin",
								new SettingsSearchWithPluginTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.reset_all_profile_settings_descr);
									}

									@Override
									protected Class<? extends OsmandPlugin> getPluginClass() {
										return WeatherPlugin.class;
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context, final OsmandPlugin osmandPlugin) {
										return List.of(String.format("Path: Driving > %s > Reset plugin settings to default", osmandPlugin.getName()));
									}
								}
						},

						{
								"shouldSearchAndFind_SelectCopyAppModeBottomSheet_within_OsmandMonitoringPlugin",
								new SettingsSearchWithPluginTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return ApplicationMode.PEDESTRIAN.toHumanString();
									}

									@Override
									protected Class<? extends OsmandPlugin> getPluginClass() {
										return OsmandMonitoringPlugin.class;
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context, final OsmandPlugin osmandPlugin) {
										return List.of(
												String.format(
														"Path: Driving > %s > %s",
														osmandPlugin.getName(),
														context.getString(R.string.copy_from_other_profile)));
									}
								}
						},

						{
								"shouldSearchAndFind_SelectCopyAppModeBottomSheet_within_AccessibilityPlugin",
								new SettingsSearchWithPluginTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return ApplicationMode.PEDESTRIAN.toHumanString();
									}

									@Override
									protected Class<? extends OsmandPlugin> getPluginClass() {
										return AccessibilityPlugin.class;
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context, final OsmandPlugin osmandPlugin) {
										return List.of(
												String.format(
														"Path: Driving > %s > %s",
														osmandPlugin.getName(),
														context.getString(R.string.copy_from_other_profile)));
									}
								}
						},

						{
								"shouldSearchAndFind_SelectCopyAppModeBottomSheet_within_AudioVideoNotesPlugin",
								new SettingsSearchWithPluginTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return ApplicationMode.PEDESTRIAN.toHumanString();
									}

									@Override
									protected Class<? extends OsmandPlugin> getPluginClass() {
										return AudioVideoNotesPlugin.class;
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context, final OsmandPlugin osmandPlugin) {
										return List.of(
												String.format(
														"Path: Driving > %s > %s",
														osmandPlugin.getName(),
														context.getString(R.string.copy_from_other_profile)));
									}
								}
						},

						{
								"shouldSearchAndFind_SelectCopyAppModeBottomSheet_within_WeatherPlugin",
								new SettingsSearchWithPluginTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return ApplicationMode.PEDESTRIAN.toHumanString();
									}

									@Override
									protected Class<? extends OsmandPlugin> getPluginClass() {
										return WeatherPlugin.class;
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context, final OsmandPlugin osmandPlugin) {
										return List.of(
												String.format(
														"Path: Driving > %s > %s",
														osmandPlugin.getName(),
														context.getString(R.string.copy_from_other_profile)));
									}
								}
						},

						{
								"shouldSearchAndFind_LocationInterpolationBottomSheet_title",
								new SettingsSearchWithPluginTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.location_interpolation_percent);
									}

									@Override
									protected Class<? extends OsmandPlugin> getPluginClass() {
										return OsmandDevelopmentPlugin.class;
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context, final OsmandPlugin osmandPlugin) {
										return List.of(context.getString(R.string.location_interpolation_percent));
									}
								}
						},

						{
								"shouldSearchAndFind_LocationInterpolationBottomSheet_description",
								new SettingsSearchWithPluginTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.location_interpolation_percent);
									}

									@Override
									protected Class<? extends OsmandPlugin> getPluginClass() {
										return OsmandDevelopmentPlugin.class;
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context, final OsmandPlugin osmandPlugin) {
										return List.of(context.getString(R.string.location_interpolation_percent_desc));
									}
								}
						},

						{
								"shouldSearchAndFindProfileAppearanceSettings4EachApplicationMode",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return "profile appearance";
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context) {
										return Stream
												.of("Driving", "Moped")
												.map(applicationMode -> String.format("Path: %s > Profile appearance", applicationMode))
												.toList();
									}
								}
						},

						{
								"shouldSearchAndFindSpeedCameraSettings4EachApplicationMode",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return "speed cameras";
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context) {
										return Stream
												.of("Driving", "Truck")
												.map(applicationMode -> String.format("Path: %s > Navigation settings > Screen alerts > Speed cameras", applicationMode))
												.toList();
									}
								}
						},
				});
	}

	@Rule
	public ActivityScenarioRule<MapActivity> mActivityScenarioRule = new ActivityScenarioRule<>(MapActivity.class);

	@Test
	public void testSearchAndFind() {
		settingsSearchTest.testSearchAndFind(app);
	}
}
