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
	public Function<Context, String> searchQueryProvider;

	@Parameterized.Parameters(name = "{0}")
	public static Iterable<Object[]> data() {
		return Arrays.asList(
				new Object[][]{
						{"RecalculateRoute", searchQuery(R.string.recalculate_route)},

						{"AnnouncementTimeBottomSheet: title", searchQuery(R.string.announcement_time_title)},
						{"AnnouncementTimeBottomSheet: description", searchQuery(R.string.announcement_time_descr)},
						{"AnnouncementTimeBottomSheet: time intervals", searchQuery(R.string.announcement_time_intervals)},

						{"FuelTankCapacityBottomSheet: title", searchQuery(R.string.fuel_tank_capacity)},
						{"FuelTankCapacityBottomSheet: description", searchQuery(R.string.fuel_tank_capacity_description)},

						{"RecalculateRouteInDeviationBottomSheet: title", searchQuery(R.string.recalculate_route_in_deviation)},
						{"RecalculateRouteInDeviationBottomSheet: description", searchQuery(R.string.select_distance_route_will_recalc)},
						{"RecalculateRouteInDeviationBottomSheet: longDescription", searchQuery(R.string.recalculate_route_distance_promo)},

						{"ScreenTimeoutBottomSheet: description", searchQuery(R.string.system_screen_timeout_descr)},

						{"GoodsRestrictionsBottomSheet: title", searchQuery(R.string.routing_attr_goods_restrictions_name)},
						{"GoodsRestrictionsBottomSheet: goods_delivery_desc", searchQuery(R.string.goods_delivery_desc)},
						{"GoodsRestrictionsBottomSheet: goods_delivery_desc_2", searchQuery(R.string.goods_delivery_desc_2)},
						{"GoodsRestrictionsBottomSheet: goods_delivery_desc_3", searchQuery(R.string.goods_delivery_desc_3)},
						{"GoodsRestrictionsBottomSheet: goods_delivery_desc_4", searchQuery(R.string.goods_delivery_desc_4)},

						{"SendAnalyticsBottomSheetDialogFragment: description", searchQuery(R.string.make_osmand_better_descr)},

						{"ProfileAppearanceFragment: view_angle_description", searchQuery(R.string.view_angle_description)},
						{"ProfileAppearanceFragment: location_radius_description", searchQuery(R.string.location_radius_description)},

						{"RouteParametersFragment: title", searchQuery(R.string.route_recalculation_dist_title)},

						{"ResetProfilePrefsBottomSheet: title", searchQuery(R.string.reset_all_profile_settings)},
						{"ResetProfilePrefsBottomSheet: description", searchQuery(R.string.reset_all_profile_settings_descr)},
						{"ResetProfilePrefsBottomSheet: reset_confirmation_descr", searchQuery("Tapping Reset discards all your changes")},

						{"GeneralProfileSettingsFragment", searchQuery(R.string.distance_during_navigation)},

						{"DistanceDuringNavigationBottomSheet: description", searchQuery("Choose how distance information is displayed in navigation widgets")},

						{"VehicleParametersFragment: SimpleSingleSelectionBottomSheet, description", searchQuery(R.string.routing_attr_motor_type_description)},

						{"VoiceLanguageBottomSheetFragment: language_description", searchQuery(R.string.language_description)},
						{"VoiceLanguageBottomSheetFragment: tts_description", searchQuery(R.string.tts_description)},
						{"VoiceLanguageBottomSheetFragment: recorded_description", searchQuery(R.string.recorded_description)},

						{"WakeTimeBottomSheet: description", searchQuery(context -> context.getString(R.string.turn_screen_on_wake_time_descr, context.getString(R.string.keep_screen_on)))},
						{"WakeTimeBottomSheet: keep_screen_on", searchQuery(R.string.keep_screen_on)},
						{"WakeTimeBottomSheet: timeoutDescription", searchQuery(context -> context.getString(R.string.screen_timeout_descr, context.getString(R.string.system_screen_timeout)))},

						{"SelectNavProfileBottomSheet: header", searchQuery(R.string.select_nav_profile_dialog_message)},

						{"SelectDefaultProfileBottomSheet: description", searchQuery(R.string.profile_by_default_description)},
						{"SelectDefaultProfileBottomSheet: car profile", searchQuery(ApplicationMode.CAR.toHumanString())},
				});
	}

	@Rule
	public ActivityScenarioRule<MapActivity> mActivityScenarioRule = new ActivityScenarioRule<>(MapActivity.class);

	@Test
	public void testSearchAndFind() {
		testSearchAndFind(searchQueryProvider.apply(app));
	}

	private void testSearchAndFind(final String searchQuery) {
		// Given
		clickSearchButton(app);

		// When
		onView(searchView()).perform(replaceText(searchQuery), closeSoftKeyboard());

		// Then
		onView(searchResultsView()).check(matches(hasSearchResultWithSubstring(searchQuery)));
	}

	private static Function<Context, String> searchQuery(final @StringRes int id) {
		return context -> context.getString(id);
	}

	private static Function<Context, String> searchQuery(final String str) {
		return context -> str;
	}

	private static Function<Context, String> searchQuery(final Function<Context, String> searchQueryProvider) {
		return searchQueryProvider;
	}
}
