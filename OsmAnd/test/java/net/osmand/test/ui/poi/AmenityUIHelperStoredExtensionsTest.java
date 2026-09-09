package net.osmand.test.ui.poi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import net.osmand.data.AdditionalInfoBundle;
import net.osmand.data.Amenity;
import net.osmand.data.BackgroundType;
import net.osmand.data.FavouritePoint;
import net.osmand.data.SpecialPointType;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AmenityExtensionsHelper;
import net.osmand.plus.mapcontextmenu.builders.AmenityUIHelper;
import net.osmand.plus.mapcontextmenu.builders.rows.AmenityInfoRow;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.test.common.AndroidTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class AmenityUIHelperStoredExtensionsTest extends AndroidTest {

	private static final String KNOWN_COLON_KEY = "authentication:phone_call:number";
	private static final String CUSTOM_KEY = "test:country";
	private static final String CUSTOM_REFERENCE_KEY = "test:reference";
	private static final String CUSTOM_ROUTE_KEY = "test:route_id";
	private static final String UNKNOWN_UNPREFIXED_KEY = "unknown_point_field";
	private static final String HIDDEN_KEY = "hidden";
	private static final String VISITED_DATE_KEY = "visited_date";
	private static final String OFFSET_KEY = "offset";
	private static final String FILTER_ONLY_KEY = "osmand_socket_type2";
	private static final String AMENITY_ONLY_KEY = "unknown:amenity:field";
	private static final String STORED_AMENITY_KEY = "osm_tag_top_index_brand";
	private static final String NORMALIZED_AMENITY_KEY = "top_index_brand";

	@Rule
	public ActivityTestRule<MapActivity> activityRule =
			new ActivityTestRule<>(MapActivity.class, true, false);

	private MapActivity mapActivity;

	@Before
	public void setup() {
		super.setup();
		mapActivity = activityRule.launchActivity(null);
	}

	@Test
	public void knownColonKeyUsesExistingPoiResolution() {
		Map<String, String> extensions = Collections.singletonMap(KNOWN_COLON_KEY, "+380441234567");

		Map<String, AmenityInfoRow> rows = buildRows(extensions,
				AmenityExtensionsHelper.getStoredExtensionFallbackKeys(extensions));

		AmenityInfoRow row = rows.get(KNOWN_COLON_KEY);
		assertNotNull(row);
		assertEquals("authentication_phone_call_number", row.name);
		assertTrue(row.isText);
	}

	@Test
	public void unknownStoredKeyUsesGenericFallback() {
		Map<String, String> extensions = new HashMap<>();
		extensions.put(CUSTOM_KEY, "United States");
		extensions.put("test:state", "Virginia");
		extensions.put("test:telephone", "+1 804 828 0100");
		extensions.put("test:postcode", "23284");
		extensions.put("test:start_date", "1838");

		Map<String, AmenityInfoRow> rows = buildRows(extensions,
				AmenityExtensionsHelper.getStoredExtensionFallbackKeys(extensions));

		for (Map.Entry<String, String> entry : extensions.entrySet()) {
			AmenityInfoRow row = rows.get(entry.getKey());
			assertNotNull(entry.getKey(), row);
			assertEquals(entry.getValue(), row.text);
		}
		assertEquals("country", rows.get(CUSTOM_KEY).name);
	}

	@Test
	public void unknownAmenityOnlyKeyIsNotShown() {
		Map<String, String> extensions = Collections.singletonMap(AMENITY_ONLY_KEY, "internal value");

		Map<String, AmenityInfoRow> rows = buildRows(extensions,
				AmenityExtensionsHelper.getStoredExtensionFallbackKeys(Collections.emptyMap()));

		assertFalse(rows.containsKey(AMENITY_ONLY_KEY));
	}

	@Test
	public void internalPointFieldsAreNotShown() {
		Map<String, String> storedExtensions = new HashMap<>();
		storedExtensions.put(HIDDEN_KEY, "true");
		storedExtensions.put(VISITED_DATE_KEY, "2024-01-01T00:00:00Z");
		storedExtensions.put(OFFSET_KEY, "1");
		storedExtensions.put(CUSTOM_KEY, "United States");

		Map<String, AmenityInfoRow> rows = buildRows(storedExtensions,
				AmenityExtensionsHelper.getStoredExtensionFallbackKeys(storedExtensions));

		assertFalse(rows.containsKey(HIDDEN_KEY));
		assertFalse(rows.containsKey(VISITED_DATE_KEY));
		assertFalse(rows.containsKey(OFFSET_KEY));
		assertTrue(rows.containsKey(CUSTOM_KEY));
	}

	/**
	 * Canary: unqualified fields OsmAnd stores on a point must never enter the custom fallback.
	 */
	@Test
	public void noInternalPointFieldIsTreatedAsCustom() {
		FavouritePoint point = new FavouritePoint(50.45, 30.52, "name", "category", 100d, 1L);
		point.setVisible(false);
		point.setVisitedDate(1_704_067_200_000L);
		point.setPickupDate(1_704_067_200_000L);
		point.setCalendarEvent(true);
		point.setAddress("address");
		point.setColor(0xFFFF0000);
		point.setIconIdFromName("special_star");
		point.setBackgroundType(BackgroundType.CIRCLE);
		point.setSpecialPointType(SpecialPointType.HOME);
		point.setAmenityOriginName("origin");

		WptPt wpt = point.toWpt(app);

		Set<String> fallbackKeys =
				AmenityExtensionsHelper.getStoredExtensionFallbackKeys(wpt.getExtensionsToRead());

		assertEquals("internal point fields leaked into the custom fallback: " + fallbackKeys,
				Collections.emptySet(), fallbackKeys);
	}

	@Test
	public void unknownUnprefixedFieldDoesNotUseGenericFallback() {
		Map<String, String> extensions = Collections.singletonMap(UNKNOWN_UNPREFIXED_KEY, "value");

		Set<String> fallbackKeys = AmenityExtensionsHelper.getStoredExtensionFallbackKeys(extensions);
		Map<String, AmenityInfoRow> rows = buildRows(extensions, fallbackKeys);

		assertTrue(fallbackKeys.isEmpty());
		assertFalse(rows.containsKey(UNKNOWN_UNPREFIXED_KEY));
	}

	@Test
	public void genericFallbackShowsStoredValueAsIs() {
		Map<String, String> extensions = Collections.singletonMap(CUSTOM_REFERENCE_KEY, "abc_def");

		Map<String, AmenityInfoRow> rows = buildRows(extensions,
				AmenityExtensionsHelper.getStoredExtensionFallbackKeys(extensions));

		AmenityInfoRow row = rows.get(CUSTOM_REFERENCE_KEY);
		assertNotNull(row);
		assertEquals("abc_def", row.text);
	}

	/**
	 * A custom field whose key contains "route", "content" or "wikipedia" is still dropped by the
	 * amenity-only substring filters, which run before the fallback. Known gap of #25226.
	 */
	@Test
	public void customFieldMatchingAmenityFilterIsStillNotShown() {
		Map<String, String> extensions = Collections.singletonMap(CUSTOM_ROUTE_KEY, "1234");

		Map<String, AmenityInfoRow> rows = buildRows(extensions,
				AmenityExtensionsHelper.getStoredExtensionFallbackKeys(extensions));

		assertFalse(rows.containsKey(CUSTOM_ROUTE_KEY));
	}

	@Test
	public void knownFilterOnlyPoiFieldIsNotShown() {
		Map<String, String> extensions = Collections.singletonMap(FILTER_ONLY_KEY, "yes");

		Map<String, AmenityInfoRow> rows = buildRows(extensions,
				AmenityExtensionsHelper.getStoredExtensionFallbackKeys(extensions));

		assertFalse(rows.containsKey(FILTER_ONLY_KEY));
	}

	@Test
	public void mixedStoredAndAmenityExtensionsUseSourceAwareFallback() {
		Map<String, String> storedExtensions = new HashMap<>();
		storedExtensions.put(CUSTOM_KEY, "United States");
		storedExtensions.put(KNOWN_COLON_KEY, "+380441234567");

		Amenity amenity = new Amenity();
		amenity.setAdditionalInfo(AMENITY_ONLY_KEY, "internal value");

		AmenityExtensionsHelper extensionsHelper = new AmenityExtensionsHelper(app);
		Map<String, String> mergedExtensions =
				extensionsHelper.getUpdatedAmenityExtensions(storedExtensions, amenity);

		Map<String, AmenityInfoRow> rows = buildRows(
				mergedExtensions,
				AmenityExtensionsHelper.getStoredExtensionFallbackKeys(storedExtensions));

		assertTrue(rows.containsKey(CUSTOM_KEY));
		assertEquals("authentication_phone_call_number", rows.get(KNOWN_COLON_KEY).name);
		assertFalse(rows.containsKey(AMENITY_ONLY_KEY));
	}

	@Test
	public void storedAmenityMetadataDoesNotUseGenericFallback() {
		Map<String, String> storedExtensions =
				Collections.singletonMap(STORED_AMENITY_KEY, "Internal brand index");
		AmenityExtensionsHelper extensionsHelper = new AmenityExtensionsHelper(app);
		Map<String, String> normalizedExtensions =
				extensionsHelper.getUpdatedAmenityExtensions(storedExtensions, null);

		Map<String, AmenityInfoRow> rows = buildRows(
				normalizedExtensions,
				AmenityExtensionsHelper.getStoredExtensionFallbackKeys(storedExtensions));

		assertFalse(rows.containsKey(NORMALIZED_AMENITY_KEY));
	}

	@NonNull
	private Map<String, AmenityInfoRow> buildRows(@NonNull Map<String, String> extensions,
	                                              @NonNull Set<String> genericFallbackKeys) {
		Map<String, AmenityInfoRow> rows = new HashMap<>();
		AmenityUIHelper helper = new AmenityUIHelper(mapActivity, app.getLanguage(),
				new AdditionalInfoBundle(app.getPoiTypes(), extensions)) {
			@Override
			public void buildAmenityRow(View view, AmenityInfoRow info) {
				rows.put(info.key, info);
			}
		};
		helper.setGenericFallbackKeys(genericFallbackKeys);
		helper.buildInternal(new LinearLayout(mapActivity));
		return rows;
	}
}
