package net.osmand.plus.render;

import static net.osmand.IndexConstants.BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT;
import static net.osmand.plus.wikivoyage.data.TravelGpx.ACTIVITY_TYPE;
import static net.osmand.render.RenderingRulesStorage.ORDER_RULES;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.StateChangedListener;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiSubType;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.AppInitializer.AppInitializeListener;
import net.osmand.plus.AppInitializer.InitEvents;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.download.ReloadIndexesTask;
import net.osmand.plus.download.ReloadIndexesTask.ReloadIndexesListener;
import net.osmand.plus.render.RendererRegistry.IRendererLoadedEventListener;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.settings.backend.CommonPreference;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.render.RenderingRule;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParserException;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TravelRendererHelper implements IRendererLoadedEventListener {

	private static final Log log = PlatformUtil.getLog(TravelRendererHelper.class);
	private static final String FILE_PREFERENCE_PREFIX = "travel_file_";
	private static final String ROUTE_TYPE_PREFERENCE_PREFIX = "travel_route_type_";

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final ResourceManager resourceManager;
	private final RendererRegistry rendererRegistry;
	private StateChangedListener<Boolean> listener;

	private final Map<String, CommonPreference<Boolean>> filesProps = new LinkedHashMap<>();
	private final Map<String, CommonPreference<Boolean>> routeTypesProps = new LinkedHashMap<>();

	public TravelRendererHelper(OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();
		resourceManager = app.getResourceManager();
		rendererRegistry = app.getRendererRegistry();
		addListeners();
	}

	private void addListeners() {
		addAppInitListener();
		addShowTravelPrefListener();
		rendererRegistry.addRendererLoadedEventListener(this);
	}

	private void addShowTravelPrefListener() {
		listener = change -> updateTravelVisibility();
		settings.SHOW_TRAVEL.addListener(listener);
	}

	private void addAppInitListener() {
		if (app.isApplicationInitializing()) {
			app.getAppInitializer().addListener(new AppInitializeListener() {
				@Override
				public void onStart(AppInitializer init) {
				}

				@Override
				public void onProgress(AppInitializer init, InitEvents event) {
					if (event == InitEvents.MAPS_INITIALIZED) {
						updateVisibilityPrefs();
					}
				}

				@Override
				public void onFinish(AppInitializer init) {
				}
			});
		} else {
			updateVisibilityPrefs();
		}
	}

	public void updateVisibilityPrefs() {
		updateFilesVisibility();
		updateTravelVisibility();
		updateRouteTypesVisibility();
	}

	public void updateFilesVisibility() {
		for (BinaryMapIndexReader reader : resourceManager.getTravelMapRepositories()) {
			String fileName = reader.getFile().getName();
			CommonPreference<Boolean> pref = getFileProperty(fileName);
			updateFileVisibility(fileName, pref.get());
		}
		reloadIndexes();
	}

	public void updateTravelVisibility() {
		MapRenderRepositories renderer = resourceManager.getRenderer();
		if (settings.SHOW_TRAVEL.get()) {
			renderer.removeHiddenFileExtension(BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT);
		} else {
			renderer.addHiddenFileExtension(BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT);
		}
		reloadIndexes();
	}

	public void updateRouteTypesVisibility() {
		RenderingRulesStorage renderer = rendererRegistry.getCurrentSelectedRenderer();
		List<PoiSubType> routesTypes = resourceManager.searchPoiSubTypesByPrefix(ACTIVITY_TYPE);
		for (PoiSubType type : routesTypes) {
			CommonPreference<Boolean> pref = getRouteTypeProperty(type.name);
			if (renderer != null) {
				boolean selected = pref.get();
				String attrName = type.name.replace(ACTIVITY_TYPE + "_", "");
				updateRouteTypeVisibility(renderer, attrName, selected);
			}
		}
	}

	public void updateFileVisibility(String fileName, boolean hidden) {
		MapRenderRepositories renderer = resourceManager.getRenderer();
		if (hidden) {
			renderer.removeHiddenFileName(fileName);
		} else {
			renderer.addHiddenFileName(fileName);
		}
	}

	private void reloadIndexes() {
		ReloadIndexesListener listener = new ReloadIndexesListener() {
			@Override
			public void reloadIndexesStarted() {
			}

			@Override
			public void reloadIndexesFinished(List<String> warnings) {
				app.getOsmandMap().refreshMap();
			}
		};
		ReloadIndexesTask reloadIndexesTask = new ReloadIndexesTask(app, listener);
		reloadIndexesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public CommonPreference<Boolean> getFileProperty(@NonNull String fileName) {
		if (filesProps.containsKey(fileName)) {
			return filesProps.get(fileName);
		}
		String prefId = FILE_PREFERENCE_PREFIX + fileName.replace(BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT, "");
		CommonPreference<Boolean> pref = settings.registerBooleanPreference(prefId, true).makeProfile();
		filesProps.put(fileName, pref);
		return pref;
	}

	public CommonPreference<Boolean> getRouteTypeProperty(@NonNull String routeType) {
		if (routeTypesProps.containsKey(routeType)) {
			return routeTypesProps.get(routeType);
		}
		String prefId = ROUTE_TYPE_PREFERENCE_PREFIX + routeType;
		CommonPreference<Boolean> pref = settings.registerBooleanPreference(prefId, true).makeProfile();
		routeTypesProps.put(routeType, pref);
		return pref;
	}

	public void updateRouteTypeVisibility(RenderingRulesStorage storage, String name, boolean selected) {
		Map<String, String> attrsMap = new LinkedHashMap<>();
		attrsMap.put("order", "-1");
		attrsMap.put("tag", "route");
		attrsMap.put("value", "segment");
		attrsMap.put("additional", "route_activity_type=" + name);

		if (selected) {
			int key = storage.getTagValueKey("route", "segment");
			RenderingRule insert = storage.getRule(ORDER_RULES, key);
			if (insert != null) {
				RenderingRule selectedRule = null;
				for (RenderingRule renderingRule : insert.getIfElseChildren()) {
					if (Algorithms.objectEquals(renderingRule.getAttributes(), attrsMap)) {
						selectedRule = renderingRule;
						break;
					}
				}
				insert.removeIfElseChildren(selectedRule);
			}
		} else {
			try {
				RenderingRule rule = new RenderingRule(attrsMap, false, storage);
				rule.storeAttributes(attrsMap);
				storage.registerTopLevel(rule, null, attrsMap, ORDER_RULES, true);
			} catch (XmlPullParserException e) {
				log.error(e);
			}
		}
		NativeOsmandLibrary nativeLib = !settings.SAFE_MODE.get() ? NativeOsmandLibrary.getLibrary(storage, app) : null;
		if (nativeLib != null) {
			nativeLib.clearCachedRenderingRulesStorage();
		}
	}

	@Override
	public void onRendererLoaded(String name, RenderingRulesStorage rules, InputStream source) {
		for (Map.Entry<String, CommonPreference<Boolean>> entry : routeTypesProps.entrySet()) {
			boolean selected = entry.getValue().get();
			String attrName = entry.getKey().replace(ACTIVITY_TYPE + "_", "");
			updateRouteTypeVisibility(rules, attrName, selected);
		}
	}
}
