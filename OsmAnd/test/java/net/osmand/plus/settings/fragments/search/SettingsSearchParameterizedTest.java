package net.osmand.plus.settings.fragments.search;

import static net.osmand.plus.settings.fragments.search.SettingsSearchTestFactory.searchQueryAndResult;

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
import java.util.Optional;
import java.util.stream.Stream;

@LargeTest
@RunWith(Parameterized.class)
public class SettingsSearchParameterizedTest extends AndroidTest {

	@Parameterized.Parameter(value = 0)
	public String description;

	@Parameterized.Parameter(value = 1)
	public SettingsSearchTestTemplate settingsSearchTestTemplate;

	@Parameterized.Parameters(name = "{0}")
	public static Iterable<Object[]> data() {
		return Arrays.asList(
				new Object[][]{
						{"RecalculateRoute", searchQueryAndResult(R.string.recalculate_route)},

						{"AnnouncementTimeBottomSheet: title", searchQueryAndResult(R.string.announcement_time_title)},
						{"AnnouncementTimeBottomSheet: description", searchQueryAndResult(R.string.announcement_time_descr)},
						{"AnnouncementTimeBottomSheet: time intervals", searchQueryAndResult(R.string.announcement_time_intervals)},

						{"FuelTankCapacityBottomSheet: title", searchQueryAndResult(R.string.fuel_tank_capacity)},
						{"FuelTankCapacityBottomSheet: description", searchQueryAndResult(R.string.fuel_tank_capacity_description)},

						{"RecalculateRouteInDeviationBottomSheet: title", searchQueryAndResult(R.string.recalculate_route_in_deviation)},
						{"RecalculateRouteInDeviationBottomSheet: description", searchQueryAndResult(R.string.select_distance_route_will_recalc)},
						{"RecalculateRouteInDeviationBottomSheet: longDescription", searchQueryAndResult(R.string.recalculate_route_distance_promo)},

						{"ScreenTimeoutBottomSheet: description", searchQueryAndResult(R.string.system_screen_timeout_descr)},

						{"GoodsRestrictionsBottomSheet: title", searchQueryAndResult(R.string.routing_attr_goods_restrictions_name)},
						{"GoodsRestrictionsBottomSheet: goods_delivery_desc", searchQueryAndResult(R.string.goods_delivery_desc)},
						{"GoodsRestrictionsBottomSheet: goods_delivery_desc_2", searchQueryAndResult(R.string.goods_delivery_desc_2)},
						{"GoodsRestrictionsBottomSheet: goods_delivery_desc_3", searchQueryAndResult(R.string.goods_delivery_desc_3)},
						{"GoodsRestrictionsBottomSheet: goods_delivery_desc_4", searchQueryAndResult(R.string.goods_delivery_desc_4)},

						{"SendAnalyticsBottomSheetDialogFragment: description", searchQueryAndResult(R.string.make_osmand_better_descr)},

						{"ProfileAppearanceFragment: view_angle_description", searchQueryAndResult(R.string.view_angle_description)},
						{"ProfileAppearanceFragment: location_radius_description", searchQueryAndResult(R.string.location_radius_description)},

						{"RouteParametersFragment: title", searchQueryAndResult(R.string.route_recalculation_dist_title)},

						{"ResetProfilePrefsBottomSheet: title", searchQueryAndResult(R.string.reset_all_profile_settings)},
						{"ResetProfilePrefsBottomSheet: description", searchQueryAndResult(R.string.reset_all_profile_settings_descr)},
						{"ResetProfilePrefsBottomSheet: reset_confirmation_descr", searchQueryAndResult("Tapping Reset discards all your changes")},

						{"GeneralProfileSettingsFragment", searchQueryAndResult(R.string.distance_during_navigation)},

						{"DistanceDuringNavigationBottomSheet: description", searchQueryAndResult("Choose how distance information is displayed in navigation widgets")},

						{"VehicleParametersFragment: SimpleSingleSelectionBottomSheet, description", searchQueryAndResult(R.string.routing_attr_motor_type_description)},

						{"VoiceLanguageBottomSheetFragment: language_description", searchQueryAndResult(R.string.language_description)},
						{"VoiceLanguageBottomSheetFragment: tts_description", searchQueryAndResult(R.string.tts_description)},
						{"VoiceLanguageBottomSheetFragment: recorded_description", searchQueryAndResult(R.string.recorded_description)},

						{"WakeTimeBottomSheet: description", searchQueryAndResult(context -> context.getString(R.string.turn_screen_on_wake_time_descr, context.getString(R.string.keep_screen_on)))},
						{"WakeTimeBottomSheet: keep_screen_on", searchQueryAndResult(R.string.keep_screen_on)},
						{"WakeTimeBottomSheet: timeoutDescription", searchQueryAndResult(context -> context.getString(R.string.screen_timeout_descr, context.getString(R.string.system_screen_timeout)))},

						{"SelectNavProfileBottomSheet: header", searchQueryAndResult(R.string.select_nav_profile_dialog_message)},

						{"SelectDefaultProfileBottomSheet: description", searchQueryAndResult(R.string.profile_by_default_description)},
						{"SelectDefaultProfileBottomSheet: car profile", searchQueryAndResult(ApplicationMode.CAR.toHumanString())},

						{"SelectBaseProfileBottomSheet: title", searchQueryAndResult(R.string.select_base_profile_dialog_title)},
						{"SelectBaseProfileBottomSheet: longDescription", searchQueryAndResult(R.string.select_base_profile_dialog_message)},

						{"ConfigureScreenFragment", searchQueryAndResult(R.string.configure_screen_widgets_descr)},

						{"search_map_rendering_engine_v1_find_map_rendering_engine", searchQueryAndResult(R.string.map_rendering_engine_v1, R.string.map_rendering_engine)},
						{"search_map_rendering_engine_v2_find_map_rendering_engine", searchQueryAndResult(R.string.map_rendering_engine_v2, R.string.map_rendering_engine)},

						{
								"search_ApplicationMode_find_SelectCopyAppModeBottomSheet",
								searchQueryAndResult(
										context -> ApplicationMode.PEDESTRIAN.toHumanString(),
										(context, osmandPlugin) -> List.of(String.format("Path: Driving > %s", context.getString(R.string.copy_from_other_profile))))
						},

						{
								"shouldSearchAndFind_ResetProfilePrefsBottomSheet_within_AccessibilityPlugin",
								new SettingsSearchTestTemplate() {

									@Override
									protected String getSearchQuery(final Context context) {
										return context.getString(R.string.reset_all_profile_settings_descr);
									}

									@Override
									protected Optional<Class<? extends OsmandPlugin>> getPluginClass() {
										return Optional.of(AccessibilityPlugin.class);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context, final Optional<OsmandPlugin> osmandPlugin) {
										return List.of(String.format("Path: Driving > %s > Reset plugin settings to default", osmandPlugin.orElseThrow().getName()));
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
									protected Optional<Class<? extends OsmandPlugin>> getPluginClass() {
										return Optional.of(AudioVideoNotesPlugin.class);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context, final Optional<OsmandPlugin> osmandPlugin) {
										return List.of(String.format("Path: Driving > %s > Reset plugin settings to default", osmandPlugin.orElseThrow().getName()));
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
									protected Optional<Class<? extends OsmandPlugin>> getPluginClass() {
										return Optional.of(OsmandMonitoringPlugin.class);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context, final Optional<OsmandPlugin> osmandPlugin) {
										return List.of(String.format("Path: Driving > %s > Reset plugin settings to default", osmandPlugin.orElseThrow().getName()));
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
									protected Optional<Class<? extends OsmandPlugin>> getPluginClass() {
										return Optional.of(WeatherPlugin.class);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context, final Optional<OsmandPlugin> osmandPlugin) {
										return List.of(String.format("Path: Driving > %s > Reset plugin settings to default", osmandPlugin.orElseThrow().getName()));
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
									protected Optional<Class<? extends OsmandPlugin>> getPluginClass() {
										return Optional.of(OsmandMonitoringPlugin.class);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context, final Optional<OsmandPlugin> osmandPlugin) {
										return List.of(
												String.format(
														"Path: Driving > %s > %s",
														osmandPlugin.orElseThrow().getName(),
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
									protected Optional<Class<? extends OsmandPlugin>> getPluginClass() {
										return Optional.of(AccessibilityPlugin.class);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context, final Optional<OsmandPlugin> osmandPlugin) {
										return List.of(
												String.format(
														"Path: Driving > %s > %s",
														osmandPlugin.orElseThrow().getName(),
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
									protected Optional<Class<? extends OsmandPlugin>> getPluginClass() {
										return Optional.of(AudioVideoNotesPlugin.class);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context, final Optional<OsmandPlugin> osmandPlugin) {
										return List.of(
												String.format(
														"Path: Driving > %s > %s",
														osmandPlugin.orElseThrow().getName(),
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
									protected Optional<Class<? extends OsmandPlugin>> getPluginClass() {
										return Optional.of(WeatherPlugin.class);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context, final Optional<OsmandPlugin> osmandPlugin) {
										return List.of(
												String.format(
														"Path: Driving > %s > %s",
														osmandPlugin.orElseThrow().getName(),
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
									protected Optional<Class<? extends OsmandPlugin>> getPluginClass() {
										return Optional.of(OsmandDevelopmentPlugin.class);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context, final Optional<OsmandPlugin> osmandPlugin) {
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
									protected Optional<Class<? extends OsmandPlugin>> getPluginClass() {
										return Optional.of(OsmandDevelopmentPlugin.class);
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context, final Optional<OsmandPlugin> osmandPlugin) {
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
									protected Optional<Class<? extends OsmandPlugin>> getPluginClass() {
										return Optional.empty();
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context, final Optional<OsmandPlugin> osmandPlugin) {
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
									protected Optional<Class<? extends OsmandPlugin>> getPluginClass() {
										return Optional.empty();
									}

									@Override
									protected List<String> getExpectedSearchResults(final Context context, final Optional<OsmandPlugin> osmandPlugin) {
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
		settingsSearchTestTemplate.testSearchAndFind(app);
	}
}
