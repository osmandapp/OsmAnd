package net.osmand.plus.mapcontextmenu.editors.icon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.mapcontextmenu.editors.icon.data.IconSearchResult;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class EditorIconSearcherTest {

	private static final long POI_TYPES_INIT_TIMEOUT_MS = 30_000;
	private static final long SEARCH_TIMEOUT_SEC = 10;

	private Instrumentation instrumentation;
	private OsmandApplication app;
	private EditorIconSearcher searcher;

	@Before
	public void setUp() throws InterruptedException {
		instrumentation = InstrumentationRegistry.getInstrumentation();
		app = (OsmandApplication) instrumentation.getTargetContext().getApplicationContext();
		long deadline = System.currentTimeMillis() + POI_TYPES_INIT_TIMEOUT_MS;
		while (!app.getPoiTypes().isInit() && System.currentTimeMillis() < deadline) {
			Thread.sleep(50);
		}
		assertTrue("POI types are not initialized", app.getPoiTypes().isInit());
		EditorIconController controller = new EditorIconController(app);
		controller.initIconCategories();
		searcher = new EditorIconSearcher(app, controller);
	}

	@Test
	public void iconWithoutPoiTypeIsFoundByNameAndKey() throws InterruptedException {
		for (String query : new String[] {"wagon", "special", "special_wagon", "special wagon"}) {
			IconSearchResult wagon = find(search(query), "special_wagon");
			assertNotNull("special_wagon is not found by " + query, wagon);
			assertEquals("Wagon", wagon.getIconName());
			assertEquals("transportation", wagon.getCategoryKey());
		}
	}

	@Test
	public void poiIconIsFoundByTranslation() throws InterruptedException {
		PoiType cafe = app.getPoiTypes().getPoiTypeByKey("cafe");
		List<String> cafeIconKeys = new ArrayList<>();
		EditorIconUtils.retrieveIconKey(cafe, cafeIconKeys::add);
		assertEquals(1, cafeIconKeys.size());
		String cafeIconKey = cafeIconKeys.get(0);

		IconSearchResult result = find(search("cafe"), cafeIconKey);
		assertNotNull(cafeIconKey + " is not found by cafe", result);
		assertEquals(cafe.getTranslation(), result.getIconName());
	}

	@Test
	public void newSearchDropsResultOfPreviousOne() throws InterruptedException {
		List<String> queries = new ArrayList<>();
		CountDownLatch latch = new CountDownLatch(1);
		instrumentation.runOnMainSync(() -> {
			searcher.search("wagon", results -> queries.add("wagon"));
			searcher.search("cafe", results -> {
				queries.add("cafe");
				latch.countDown();
			});
		});
		assertTrue(latch.await(SEARCH_TIMEOUT_SEC, TimeUnit.SECONDS));
		instrumentation.waitForIdleSync();
		assertEquals(List.of("cafe"), queries);
	}

	@Test
	public void cancelledSearchDeliversNothing() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		instrumentation.runOnMainSync(() -> {
			searcher.search("wagon", results -> latch.countDown());
			searcher.cancel();
		});
		assertFalse(latch.await(3, TimeUnit.SECONDS));
	}

	@NonNull
	private List<IconSearchResult> search(@NonNull String query) throws InterruptedException {
		List<IconSearchResult> found = new ArrayList<>();
		CountDownLatch latch = new CountDownLatch(1);
		instrumentation.runOnMainSync(() -> searcher.search(query, results -> {
			found.addAll(results);
			latch.countDown();
		}));
		assertTrue("No results for " + query, latch.await(SEARCH_TIMEOUT_SEC, TimeUnit.SECONDS));
		return found;
	}

	@Nullable
	private IconSearchResult find(@NonNull List<IconSearchResult> results, @NonNull String iconKey) {
		for (IconSearchResult result : results) {
			if (iconKey.equals(result.getIconKey())) {
				return result;
			}
		}
		return null;
	}
}
