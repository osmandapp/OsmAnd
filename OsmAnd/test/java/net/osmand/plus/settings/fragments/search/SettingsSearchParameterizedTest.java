package net.osmand.plus.settings.fragments.search;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static net.osmand.plus.settings.fragments.search.SearchButtonClick.clickSearchButton;
import static net.osmand.plus.settings.fragments.search.SettingsSearchTest.hasSearchResultWithSubstring;
import static net.osmand.plus.settings.fragments.search.SettingsSearchTest.searchResultsView;
import static net.osmand.plus.settings.fragments.search.SettingsSearchTest.searchView;

import androidx.annotation.StringRes;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
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
	public @StringRes int searchQueryId;

	@Parameterized.Parameters(name = "{0}")
	public static Iterable<Object[]> data() {
		return Arrays.asList(
				new Object[][]{
						{"RecalculateRoute", R.string.recalculate_route},
						{"AnnouncementTimeBottomSheet_title", R.string.announcement_time_title},
						{"AnnouncementTimeBottomSheet_description", R.string.announcement_time_descr},
						{"FuelTankCapacityBottomSheet_description", R.string.fuel_tank_capacity_description},
						{"RecalculateRouteInDeviationBottomSheet_title", R.string.recalculate_route_in_deviation},
						{"RecalculateRouteInDeviationBottomSheet_description", R.string.select_distance_route_will_recalc},
						{"RecalculateRouteInDeviationBottomSheet_longDescription", R.string.recalculate_route_distance_promo},
						{"ScreenTimeoutBottomSheet_description", R.string.system_screen_timeout_descr},
						{"GoodsRestrictionsBottomSheet_title", R.string.routing_attr_goods_restrictions_name},
						{"GoodsRestrictionsBottomSheet: goods_delivery_desc", R.string.goods_delivery_desc},
						{"GoodsRestrictionsBottomSheet: goods_delivery_desc_2", R.string.goods_delivery_desc_2},
						{"GoodsRestrictionsBottomSheet: goods_delivery_desc_3", R.string.goods_delivery_desc_3},
						{"GoodsRestrictionsBottomSheet: goods_delivery_desc_4", R.string.goods_delivery_desc_4},
				});
	}

	@Rule
	public ActivityScenarioRule<MapActivity> mActivityScenarioRule = new ActivityScenarioRule<>(MapActivity.class);

	@Test
	public void testSearchAndFind() {
		testSearchAndFind(app.getResources().getString(searchQueryId));
	}

	private void testSearchAndFind(final String searchQuery) {
		// Given
		clickSearchButton(app);

		// When
		onView(searchView()).perform(replaceText(searchQuery), closeSoftKeyboard());

		// Then
		onView(searchResultsView()).check(matches(hasSearchResultWithSubstring(searchQuery)));
	}
}
