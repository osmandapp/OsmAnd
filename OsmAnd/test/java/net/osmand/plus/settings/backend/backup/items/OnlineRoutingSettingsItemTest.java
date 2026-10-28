package net.osmand.plus.settings.backend.backup.items;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.onlinerouting.EngineParameter;
import net.osmand.plus.onlinerouting.OnlineRoutingHelper;
import net.osmand.plus.onlinerouting.OnlineRoutingUtils;
import net.osmand.plus.onlinerouting.engine.EngineType;
import net.osmand.plus.onlinerouting.engine.OnlineRoutingEngine;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class OnlineRoutingSettingsItemTest {

	private static final String TEST_KEY_PREFIX = OnlineRoutingEngine.ONLINE_ROUTING_ENGINE_PREFIX + "test_";
	private static final String ENGINE_KEY = TEST_KEY_PREFIX + "x";
	private static final String OTHER_ENGINE_KEY = TEST_KEY_PREFIX + "y";
	private static final String ENGINE_NAME = "Test engine X";
	private static final String OTHER_ENGINE_NAME = "Test engine Y";
	private static final String URL_A = "https://example.org/a/";
	private static final String URL_B = "https://example.org/b/";

	private OsmandApplication app;
	private OnlineRoutingHelper helper;
	private OsmandSettings settings;
	private long originalGlobalEditTime;

	@Before
	public void setUp() {
		Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
		app = (OsmandApplication) context.getApplicationContext();
		helper = app.getOnlineRoutingHelper();
		settings = app.getSettings();
		originalGlobalEditTime = settings.getLastGlobalPreferencesEditTime();
		removeTestEngines();
	}

	@After
	public void tearDown() {
		removeTestEngines();
		settings.setLastGlobalPreferencesEditTime(originalGlobalEditTime);
	}

	@Test
	public void anIdenticalEngineRestoreDoesNotTouchGlobalSettings() {
		helper.saveEngine(engine(ENGINE_KEY, ENGINE_NAME, URL_A));
		String storedBefore = settings.ONLINE_ROUTING_ENGINES.get();
		long globalEditTime = markGlobalEditTime();

		restore(engine(ENGINE_KEY, ENGINE_NAME, URL_A));

		assertEquals(storedBefore, settings.ONLINE_ROUTING_ENGINES.get());
		assertEquals(globalEditTime, settings.getLastGlobalPreferencesEditTime());
		assertEquals(1, enginesNamed(ENGINE_NAME).size());
		assertEquals(URL_A, storedUrl(ENGINE_KEY));
	}

	@Test
	public void aChangedEngineIsRestored() {
		helper.saveEngine(engine(ENGINE_KEY, ENGINE_NAME, URL_A));

		restore(engine(ENGINE_KEY, ENGINE_NAME, URL_B));

		assertEquals(URL_B, storedUrl(ENGINE_KEY));
		assertEquals(1, enginesNamed(ENGINE_NAME).size());
	}

	@Test
	public void anUnrelatedEngineSurvivesTheRestore() {
		helper.saveEngine(engine(ENGINE_KEY, ENGINE_NAME, URL_A));
		helper.saveEngine(engine(OTHER_ENGINE_KEY, OTHER_ENGINE_NAME, URL_A));

		restore(engine(ENGINE_KEY, ENGINE_NAME, URL_B));

		assertEquals(URL_B, storedUrl(ENGINE_KEY));
		assertEquals(URL_A, storedUrl(OTHER_ENGINE_KEY));
		assertEquals(1, enginesNamed(OTHER_ENGINE_NAME).size());
	}

	@Test
	public void aNewEngineIsImportedOnce() {
		restore(engine(ENGINE_KEY, ENGINE_NAME, URL_A));

		assertNotNull(helper.getEngineByKey(ENGINE_KEY));
		assertEquals(1, enginesNamed(ENGINE_NAME).size());
		assertEquals(URL_A, storedUrl(ENGINE_KEY));
	}

	/** The delete is still required here: the imported engine lands under a different key. */
	@Test
	public void aNameMatchWithAnotherKeyReplacesTheExistingEngine() {
		helper.saveEngine(engine(ENGINE_KEY, ENGINE_NAME, URL_A));

		restore(engine(OTHER_ENGINE_KEY, ENGINE_NAME, URL_B));

		assertNull(helper.getEngineByKey(ENGINE_KEY));
		assertEquals(URL_B, storedUrl(OTHER_ENGINE_KEY));
		assertEquals(1, enginesNamed(ENGINE_NAME).size());
	}

	@Test
	public void anExplicitEngineEditStillUpdatesTheSharedPreference() {
		long globalEditTime = markGlobalEditTime();

		helper.saveEngine(engine(ENGINE_KEY, ENGINE_NAME, URL_A));

		assertEquals(URL_A, storedUrl(ENGINE_KEY));
		assertTrue(settings.getLastGlobalPreferencesEditTime() > globalEditTime);

		globalEditTime = markGlobalEditTime();
		helper.deleteEngine(ENGINE_KEY);

		assertNull(storedEngine(ENGINE_KEY));
		assertTrue(settings.getLastGlobalPreferencesEditTime() > globalEditTime);
	}

	private void restore(OnlineRoutingEngine... imported) {
		OnlineRoutingSettingsItem item = new OnlineRoutingSettingsItem(app, Arrays.asList(imported));
		item.setShouldReplace(true);
		item.processDuplicateItems();
		item.apply();
	}

	private long markGlobalEditTime() {
		long time = System.currentTimeMillis() - 60_000;
		settings.setLastGlobalPreferencesEditTime(time);
		return time;
	}

	private static OnlineRoutingEngine engine(String key, String name, String url) {
		Map<String, String> params = new HashMap<>();
		params.put(EngineParameter.KEY.name(), key);
		params.put(EngineParameter.CUSTOM_NAME.name(), name);
		params.put(EngineParameter.CUSTOM_URL.name(), url);
		params.put(EngineParameter.VEHICLE_KEY.name(), "car");
		return EngineType.OSRM_TYPE.newInstance(params);
	}

	private List<OnlineRoutingEngine> enginesNamed(String name) {
		List<OnlineRoutingEngine> engines = new ArrayList<>();
		for (OnlineRoutingEngine engine : helper.getOnlyCustomEngines()) {
			if (Algorithms.objectEquals(engine.getName(app), name)) {
				engines.add(engine);
			}
		}
		return engines;
	}

	/** Read back from the preference rather than the helper cache, so persistence is covered. */
	private String storedUrl(String key) {
		OnlineRoutingEngine engine = storedEngine(key);
		return engine != null ? engine.get(EngineParameter.CUSTOM_URL) : null;
	}

	private OnlineRoutingEngine storedEngine(String key) {
		for (OnlineRoutingEngine engine : storedEngines()) {
			if (Algorithms.objectEquals(engine.getStringKey(), key)) {
				return engine;
			}
		}
		return null;
	}

	private List<OnlineRoutingEngine> storedEngines() {
		List<OnlineRoutingEngine> engines = new ArrayList<>();
		String jsonString = settings.ONLINE_ROUTING_ENGINES.get();
		if (!Algorithms.isEmpty(jsonString)) {
			try {
				OnlineRoutingUtils.readFromJson(new JSONObject(jsonString), engines);
			} catch (JSONException e) {
				throw new AssertionError("Failed to read stored engines", e);
			}
		}
		return engines;
	}

	private void removeTestEngines() {
		for (OnlineRoutingEngine engine : helper.getOnlyCustomEngines()) {
			String key = engine.getStringKey();
			if (key != null && key.startsWith(TEST_KEY_PREFIX)) {
				helper.deleteEngine(key);
			}
		}
	}
}
