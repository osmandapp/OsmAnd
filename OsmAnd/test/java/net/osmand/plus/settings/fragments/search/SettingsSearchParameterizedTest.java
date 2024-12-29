package net.osmand.plus.settings.fragments.search;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static net.osmand.plus.settings.fragments.search.SearchButtonClick.clickSearchButton;
import static net.osmand.plus.settings.fragments.search.SettingsSearchTest.hasSearchResultWithSubstring;
import static net.osmand.plus.settings.fragments.search.SettingsSearchTest.searchResultsView;
import static net.osmand.plus.settings.fragments.search.SettingsSearchTest.searchView;

import android.content.Context;

import androidx.annotation.StringRes;
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
import java.util.function.Function;

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
						{"RecalculateRoute", SearchQueryAndResult.from(R.string.recalculate_route)},

						{"AnnouncementTimeBottomSheet: title", SearchQueryAndResult.from(R.string.announcement_time_title)},
						{"AnnouncementTimeBottomSheet: description", SearchQueryAndResult.from(R.string.announcement_time_descr)},
						{"AnnouncementTimeBottomSheet: time intervals", SearchQueryAndResult.from(R.string.announcement_time_intervals)},

						{"FuelTankCapacityBottomSheet: title", SearchQueryAndResult.from(R.string.fuel_tank_capacity)},
						{"FuelTankCapacityBottomSheet: description", SearchQueryAndResult.from(R.string.fuel_tank_capacity_description)},

						{"RecalculateRouteInDeviationBottomSheet: title", SearchQueryAndResult.from(R.string.recalculate_route_in_deviation)},
						{"RecalculateRouteInDeviationBottomSheet: description", SearchQueryAndResult.from(R.string.select_distance_route_will_recalc)},
						{"RecalculateRouteInDeviationBottomSheet: longDescription", SearchQueryAndResult.from(R.string.recalculate_route_distance_promo)},

						{"ScreenTimeoutBottomSheet: description", SearchQueryAndResult.from(R.string.system_screen_timeout_descr)},

						{"GoodsRestrictionsBottomSheet: title", SearchQueryAndResult.from(R.string.routing_attr_goods_restrictions_name)},
						{"GoodsRestrictionsBottomSheet: goods_delivery_desc", SearchQueryAndResult.from(R.string.goods_delivery_desc)},
						{"GoodsRestrictionsBottomSheet: goods_delivery_desc_2", SearchQueryAndResult.from(R.string.goods_delivery_desc_2)},
						{"GoodsRestrictionsBottomSheet: goods_delivery_desc_3", SearchQueryAndResult.from(R.string.goods_delivery_desc_3)},
						{"GoodsRestrictionsBottomSheet: goods_delivery_desc_4", SearchQueryAndResult.from(R.string.goods_delivery_desc_4)},

						{"SendAnalyticsBottomSheetDialogFragment: description", SearchQueryAndResult.from(R.string.make_osmand_better_descr)},

						{"ProfileAppearanceFragment: view_angle_description", SearchQueryAndResult.from(R.string.view_angle_description)},
						{"ProfileAppearanceFragment: location_radius_description", SearchQueryAndResult.from(R.string.location_radius_description)},

						{"RouteParametersFragment: title", SearchQueryAndResult.from(R.string.route_recalculation_dist_title)},

						{"ResetProfilePrefsBottomSheet: title", SearchQueryAndResult.from(R.string.reset_all_profile_settings)},
						{"ResetProfilePrefsBottomSheet: description", SearchQueryAndResult.from(R.string.reset_all_profile_settings_descr)},
						{"ResetProfilePrefsBottomSheet: reset_confirmation_descr", SearchQueryAndResult.from("Tapping Reset discards all your changes")},

						{"GeneralProfileSettingsFragment", SearchQueryAndResult.from(R.string.distance_during_navigation)},

						{"DistanceDuringNavigationBottomSheet: description", SearchQueryAndResult.from("Choose how distance information is displayed in navigation widgets")},

						{"VehicleParametersFragment: SimpleSingleSelectionBottomSheet, description", SearchQueryAndResult.from(R.string.routing_attr_motor_type_description)},

						{"VoiceLanguageBottomSheetFragment: language_description", SearchQueryAndResult.from(R.string.language_description)},
						{"VoiceLanguageBottomSheetFragment: tts_description", SearchQueryAndResult.from(R.string.tts_description)},
						{"VoiceLanguageBottomSheetFragment: recorded_description", SearchQueryAndResult.from(R.string.recorded_description)},

						{"WakeTimeBottomSheet: description", SearchQueryAndResult.from(context -> context.getString(R.string.turn_screen_on_wake_time_descr, context.getString(R.string.keep_screen_on)))},
						{"WakeTimeBottomSheet: keep_screen_on", SearchQueryAndResult.from(R.string.keep_screen_on)},
						{"WakeTimeBottomSheet: timeoutDescription", SearchQueryAndResult.from(context -> context.getString(R.string.screen_timeout_descr, context.getString(R.string.system_screen_timeout)))},

						{"SelectNavProfileBottomSheet: header", SearchQueryAndResult.from(R.string.select_nav_profile_dialog_message)},

						{"SelectDefaultProfileBottomSheet: description", SearchQueryAndResult.from(R.string.profile_by_default_description)},
						{"SelectDefaultProfileBottomSheet: car profile", SearchQueryAndResult.from(ApplicationMode.CAR.toHumanString())},

						{"SelectBaseProfileBottomSheet: title", SearchQueryAndResult.from(R.string.select_base_profile_dialog_title)},
						{"SelectBaseProfileBottomSheet: longDescription", SearchQueryAndResult.from(R.string.select_base_profile_dialog_message)},

						{"ConfigureScreenFragment", SearchQueryAndResult.from(R.string.configure_screen_widgets_descr)},

						{"test_search_map_rendering_engine_v1_find_map_rendering_engine", SearchQueryAndResult.from(R.string.map_rendering_engine_v1, R.string.map_rendering_engine)},
						{"test_search_map_rendering_engine_v2_find_map_rendering_engine", SearchQueryAndResult.from(R.string.map_rendering_engine_v2, R.string.map_rendering_engine)}
				});
	}

	@Rule
	public ActivityScenarioRule<MapActivity> mActivityScenarioRule = new ActivityScenarioRule<>(MapActivity.class);

	@Test
	public void testSearchAndFind() {
		testSearchAndFind(
				searchQueryAndResult.searchQueryProvider().apply(app),
				searchQueryAndResult.searchResultProvider().apply(app));
	}

	private void testSearchAndFind(final String searchQuery, final String searchResult) {
		// Given
		clickSearchButton(app);

		// When
		onView(searchView()).perform(replaceText(searchQuery), closeSoftKeyboard());

		// Then
		onView(searchResultsView()).check(matches(hasSearchResultWithSubstring(searchResult)));
	}

	public record SearchQueryAndResult(Function<Context, String> searchQueryProvider,
									   Function<Context, String> searchResultProvider) {

		public static SearchQueryAndResult from(final @StringRes int id) {
			return from(context -> context.getString(id));
		}

		public static SearchQueryAndResult from(final @StringRes int queryId,
												final @StringRes int resultId) {
			return new SearchQueryAndResult(
					context -> context.getString(queryId),
					context -> context.getString(resultId));
		}

		public static SearchQueryAndResult from(final String str) {
			return from(context -> str);
		}

		public static SearchQueryAndResult from(final Function<Context, String> searchQueryProvider) {
			return new SearchQueryAndResult(searchQueryProvider, searchQueryProvider);
		}
	}
}
