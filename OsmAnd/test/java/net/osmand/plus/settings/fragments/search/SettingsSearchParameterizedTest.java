package net.osmand.plus.settings.fragments.search;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static net.osmand.plus.settings.fragments.search.SearchButtonClick.clickSearchButton;
import static net.osmand.plus.settings.fragments.search.SearchQueryAndResultFactory.searchQueryAndResult;
import static net.osmand.plus.settings.fragments.search.SettingsSearchTest.hasSearchResultWithSubstring;
import static net.osmand.plus.settings.fragments.search.SettingsSearchTest.searchResultsView;
import static net.osmand.plus.settings.fragments.search.SettingsSearchTest.searchView;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.test.common.AndroidTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

@LargeTest
@RunWith(Parameterized.class)
public class SettingsSearchParameterizedTest extends AndroidTest {

	@Parameterized.Parameter(value = 0)
	public String description;

	@Parameterized.Parameter(value = 1)
	public SearchQueryAndResult searchQueryAndResult;

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

						{"test_search_map_rendering_engine_v1_find_map_rendering_engine", searchQueryAndResult(R.string.map_rendering_engine_v1, R.string.map_rendering_engine)},
						{"test_search_map_rendering_engine_v2_find_map_rendering_engine", searchQueryAndResult(R.string.map_rendering_engine_v2, R.string.map_rendering_engine)}
				});
	}

	@Rule
	public ActivityScenarioRule<MapActivity> mActivityScenarioRule = new ActivityScenarioRule<>(MapActivity.class);

	@Test
	public void testSearchAndFind() {
		testSearchAndFind(searchQueryAndResult.getSearchQuery(app), searchQueryAndResult.getSearchResult(app));
	}

	private void testSearchAndFind(final String searchQuery, final String searchResultExpected) {
		// Given
		clickSearchButton(app);

		// When
		onView(searchView()).perform(replaceText(searchQuery), closeSoftKeyboard());

		// Then
		onView(searchResultsView()).check(matches(hasSearchResultWithSubstring(searchResultExpected)));
	}
}
