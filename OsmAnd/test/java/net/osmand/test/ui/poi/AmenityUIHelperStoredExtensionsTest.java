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
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AmenityExtensionsHelper;
import net.osmand.plus.mapcontextmenu.builders.AmenityUIHelper;
import net.osmand.plus.mapcontextmenu.builders.rows.AmenityInfoRow;
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
	private static final String AMENITY_ONLY_KEY = "unknown:amenity:field";
	private static final String STORED_AMENITY_KEY = "osm_tag_top_index_brand";
	private static final String NORMALIZED_AMENITY_KEY = "top_index_brand";

	@Rule
	public ActivityTestRule<MapActivity> activityRule =
			new ActivityTestRule<>(MapActivity.class, true, false);

	private MapActivity mapActivity;

	@Before
	public void setUp() {
		mapActivity = activityRule.launchActivity(null);
	}

	@Test
	public void knownColonKeyUsesExistingPoiResolution() {
		Map<String, String> extensions = Collections.singletonMap(KNOWN_COLON_KEY, "+380441234567");

		Map<String, AmenityInfoRow> rows = buildRows(extensions, extensions.keySet());

		AmenityInfoRow row = rows.get(KNOWN_COLON_KEY);
		assertNotNull(row);
		assertEquals("authentication_phone_call_number", row.name);
		assertTrue(row.isText);
	}

	@Test
	public void unknownStoredKeyUsesGenericFallback() {
		Map<String, String> extensions = Collections.singletonMap(CUSTOM_KEY, "United States");

		Map<String, AmenityInfoRow> rows = buildRows(extensions, extensions.keySet());

		AmenityInfoRow row = rows.get(CUSTOM_KEY);
		assertNotNull(row);
		assertEquals("country", row.name);
		assertEquals("United States", row.text);
	}

	@Test
	public void unknownAmenityOnlyKeyIsNotShown() {
		Map<String, String> extensions = Collections.singletonMap(AMENITY_ONLY_KEY, "internal value");

		Map<String, AmenityInfoRow> rows = buildRows(extensions, Collections.emptySet());

		assertFalse(rows.containsKey(AMENITY_ONLY_KEY));
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
