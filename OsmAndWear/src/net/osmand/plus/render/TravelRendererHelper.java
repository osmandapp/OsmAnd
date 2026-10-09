package net.osmand.plus.render;

import static net.osmand.IProgress.EMPTY_PROGRESS;
import static net.osmand.IndexConstants.BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.WIKIVOYAGE_INDEX_DIR;
import static net.osmand.osm.MapPoiTypes.ROUTE_ARTICLE;
import static net.osmand.osm.MapPoiTypes.ROUTE_ARTICLE_POINT;
import static net.osmand.osm.MapPoiTypes.ROUTE_TRACK;
import static net.osmand.plus.wikivoyage.data.TravelGpx.ACTIVITY_TYPE;
import static net.osmand.render.RenderingRulesStorage.LINE_RULES;
import static net.osmand.render.RenderingRulesStorage.ORDER_RULES;
import static net.osmand.render.RenderingRulesStorage.POINT_RULES;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.StateChangedListener;
import net.osmand.core.android.MapRendererContext;
import net.osmand.osm.MapPoiTypes;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.render.RendererRegistry.IRendererLoadedEventListener;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.render.RenderingRule;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParserException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TravelRendererHelper implements IRendererLoadedEventListener {

	private static final Log log = PlatformUtil.getLog(TravelRendererHelper.class);
	private static final String FILE_PREFERENCE_PREFIX = "travel_file_";
	private static final String ROUTE_TYPE_PREFERENCE_PREFIX = "travel_route_type_";
	private static final String ROUTE_POINT_CATEGORY_PREFERENCE_PREFIX = "travel_route_point_category_";
	private static final String ROUTE_ARTICLE_POINTS_PREFERENCE = "travel_route_article_points_preference";
	private static final String ROUTE_ARTICLES_PREFERENCE = "travel_route_articles_preference";
	private static final String ROUTE_TRACKS_PREFERENCE = "travel_route_tracks_preference";
	private static final String ROUTE_TRACKS_AS_POI_PREFERENCE = "travel_route_tracks_as_poi_preference";

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final ResourceManager resourceManager;
	private final RendererRegistry rendererRegistry;
	private StateChangedListener<Boolean> listener;

	private final Map<String, CommonPreference<Boolean>> filesVisibilityProperties = new LinkedHashMap<>();
	private StateChangedListener<Boolean> fileVisibilityPropertiesListener;
	private final List<OnFileVisibilityChangeListener> fileVisibilityListeners = new ArrayList<>();
	private final Map<String, CommonPreference<Boolean>> routeTypesProps = new LinkedHashMap<>();
	private final Map<String, CommonPreference<Boolean>> routePointCategoriesProps = new LinkedHashMap<>();

	private PoiUIFilter routeArticleFilter;
	private PoiUIFilter routeArticlePointsFilter;
	private PoiUIFilter routeTrackFilter;

	public interface OnFileVisibilityChangeListener {
		void fileVisibilityChanged();
	}

	public TravelRendererHelper(@NonNull OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();
		resourceManager = app.getResourceManager();
		rendererRegistry = app.getRendererRegistry();
		addListeners();
	}

	public void addFileVisibilityListener(@NonNull OnFileVisibilityChangeListener listener) {
		fileVisibilityListeners.add(listener);
	}

	public void removeFileVisibilityListener(@NonNull OnFileVisibilityChangeListener listener) {
		fileVisibilityListeners.remove(listener);
	}

	private void addListeners() {
		addShowTravelPrefListener();
		rendererRegistry.addRendererLoadedEventListener(this);
		fileVisibilityPropertiesListener = change -> {
			for (OnFileVisibilityChangeListener listener : fileVisibilityListeners) {
				listener.fileVisibilityChanged();
			}
		};
	}

	private void addShowTravelPrefListener() {
		listener = change -> updateTravelVisibility();
		settings.SHOW_TRAVEL.addListener(listener);
	}

	public void updateVisibilityPrefs() {
		updateFilesVisibility();
		updateTravelVisibility();
		updateRouteTypesVisibility();
	}

	public void updateFilesVisibility() {
		for (String fileName : resourceManager.getTravelRepositoryNames()) {
			CommonPreference<Boolean> pref = getFileVisibilityProperty(fileName);
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
		MapRendererContext rendererContext = NativeCoreContext.getMapRendererContext();
		if (rendererContext != null) {
			if (settings.SHOW_TRAVEL.get()) {
				rendererContext.addDirectory(app.getAppPath(WIKIVOYAGE_INDEX_DIR).getAbsolutePath());
			} else {
				rendererContext.removeDirectory(app.getAppPath(WIKIVOYAGE_INDEX_DIR).getAbsolutePath());
			}
		}
		reloadIndexes();
	}

	public void updateRouteTypesVisibility() {
		RenderingRulesStorage renderer = rendererRegistry.getCurrentSelectedRenderer();
		if (renderer != null) {
			renderer = renderer.copy();
		}
		boolean showTracks = getRouteTracksProperty().get();
		boolean renderedChanged = false;
		List<String> routesTypes = resourceManager.searchPoiSubTypesByPrefix(ACTIVITY_TYPE);
		for (String type : routesTypes) {
			CommonPreference<Boolean> pref = getRouteTypeProperty(type);
			if (renderer != null) {
				boolean selected = showTracks && pref.get();
				String attrName = type.replace(ACTIVITY_TYPE + "_", "");
				renderedChanged |= updateRouteTypeVisibility(renderer, attrName, selected, false);
			}
		}
		if (renderedChanged) {
			app.getRendererRegistry().updateRenderer(renderer);
		}
	}

	public void updateFileVisibility(String fileName, boolean visible) {
		MapRenderRepositories renderer = resourceManager.getRenderer();
		if (visible) {
			renderer.removeHiddenFileName(fileName);
		} else {
			renderer.addHiddenFileName(fileName);
		}
	}

	private void reloadIndexes() {
		app.getResourceManager().reloadIndexesAsync(EMPTY_PROGRESS, warnings -> app.getOsmandMap().refreshMap());
	}

	public CommonPreference<Boolean> getFileVisibilityProperty(@NonNull String fileName) {
		if (filesVisibilityProperties.containsKey(fileName)) {
			return filesVisibilityProperties.get(fileName);
		}
		String prefId = FILE_PREFERENCE_PREFIX + fileName.replace(BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT, "");
		CommonPreference<Boolean> pref = settings.registerBooleanPreference(prefId, true).makeProfile();
		pref.addListener(fileVisibilityPropertiesListener);
		filesVisibilityProperties.put(fileName, pref);
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

	public CommonPreference<Boolean> getRoutePointCategoryProperty(@NonNull String pointCategory) {
		if (routePointCategoriesProps.containsKey(pointCategory)) {
			return routePointCategoriesProps.get(pointCategory);
		}
		String prefId = ROUTE_POINT_CATEGORY_PREFERENCE_PREFIX + pointCategory;
		CommonPreference<Boolean> pref = settings.registerBooleanPreference(prefId, true).makeProfile();
		routePointCategoriesProps.put(pointCategory, pref);
		return pref;
	}

	public CommonPreference<Boolean> getRouteArticlesProperty() {
		return settings.registerBooleanPreference(ROUTE_ARTICLES_PREFERENCE, true).makeProfile();
	}

	public CommonPreference<Boolean> getRouteArticlePointsProperty() {
		return settings.registerBooleanPreference(ROUTE_ARTICLE_POINTS_PREFERENCE, true).makeProfile();
	}

	public CommonPreference<Boolean> getRouteTracksProperty() {
		return settings.registerBooleanPreference(ROUTE_TRACKS_PREFERENCE, true).makeProfile();
	}

	public CommonPreference<Boolean> getRouteTracksAsPoiProperty() {
		return settings.registerBooleanPreference(ROUTE_TRACKS_AS_POI_PREFERENCE, true).makeProfile();
	}

	@Nullable
	public PoiUIFilter getRouteArticleFilter() {
		if (routeArticleFilter == null) {
			updateRouteArticleFilter();
		}
		return routeArticleFilter;
	}

	@Nullable
	public PoiUIFilter getRouteArticlePointsFilter() {
		if (routeArticlePointsFilter == null) {
			updateRouteArticlePointsFilter();
		}
		return routeArticlePointsFilter;
	}

	@Nullable
	public PoiUIFilter getRouteTrackFilter() {
		if (routeTrackFilter == null && app.getPoiTypes().isInit()) {
			updateRouteTrackFilter();
		}
		return routeTrackFilter;
	}

	public void updateRouteArticleFilter() {
		if (app.getPoiTypes().isInit()) {
			routeArticleFilter = app.getPoiFilters().getFilterById(PoiUIFilter.STD_PREFIX + ROUTE_ARTICLE);
		}
	}

	public void updateRouteArticlePointsFilter() {
		if (!app.getPoiTypes().isInit()) {
			return;
		}
		PoiUIFilter routeArticlePointsFilter = app.getPoiFilters().getFilterById(PoiUIFilter.STD_PREFIX + ROUTE_ARTICLE_POINT);
		if (routeArticlePointsFilter != null) {
			Set<String> selectedCategories = new HashSet<>();
			List<String> categories = app.getResourceManager().searchPoiSubTypesByPrefix(MapPoiTypes.CATEGORY);
			for (String category : categories) {
				CommonPreference<Boolean> prop = getRoutePointCategoryProperty(category);
				if (prop.get()) {
					selectedCategories.add(category.replace('_', ':').toLowerCase());
				}
			}
			routeArticlePointsFilter.setFilterByName(TextUtils.join(" ", selectedCategories));
		}
		this.routeArticlePointsFilter = routeArticlePointsFilter;
	}

	public void updateRouteTrackFilter() {
		routeTrackFilter = app.getPoiFilters().getFilterById(PoiUIFilter.STD_PREFIX + ROUTE_TRACK);
	}

	public boolean updateRouteTypeVisibility(RenderingRulesStorage storage, String name, boolean selected) {
		return updateRouteTypeVisibility(storage, name, selected, true);
	}

	private boolean updateRouteTypeVisibility(RenderingRulesStorage storage, String name, boolean selected, boolean cloneStorage) {
		Map<String, String> attrsMap = new LinkedHashMap<>();
		attrsMap.put("order", "-1");
		attrsMap.put("tag", "route");
		attrsMap.put("value", "segment");
		attrsMap.put("additional", "route_activity_type=" + name);

		storage = cloneStorage ? storage.copy() : storage;
		boolean changed = false;

		int key = storage.getTagValueKey("route", "segment");
		RenderingRule lineSegmentRule = storage.getRule(LINE_RULES, key);
		if (lineSegmentRule != null && lineSegmentRule.getAttributes() != null) {
			Map<String, String> attributes = new HashMap<>(lineSegmentRule.getAttributes());
			attributes.put("minzoom", "14");
			lineSegmentRule.init(attributes);
			lineSegmentRule.storeAttributes(attributes);
			changed = true;
		}

		if (selected) {
			RenderingRule orderSegmentRule = storage.getRule(ORDER_RULES, key);
			if (orderSegmentRule != null) {
				RenderingRule activityRule = null;
				for (RenderingRule renderingRule : orderSegmentRule.getIfElseChildren()) {
					if (Algorithms.objectEquals(renderingRule.getAttributes(), attrsMap)) {
						activityRule = renderingRule;
						break;
					}
				}
				orderSegmentRule.removeIfElseChildren(activityRule);
				changed = true;
			}
		} else {
			try {
				RenderingRule rule = new RenderingRule(attrsMap, false, storage);
				rule.storeAttributes(attrsMap);
				storage.registerTopLevel(rule, null, attrsMap, ORDER_RULES, true);
				changed = true;
			} catch (XmlPullParserException e) {
				log.error(e);
			}
		}

		attrsMap = new LinkedHashMap<>();
		attrsMap.put("order", "-2");
		attrsMap.put("tag", "route");
		attrsMap.put("value", "point");
		attrsMap.put("additional", "route_activity_type=" + name);

		key = storage.getTagValueKey("route", "point");
		RenderingRule pointSegmentRule = storage.getRule(POINT_RULES, key);
		if (pointSegmentRule != null && pointSegmentRule.getAttributes() != null) {
			Map<String, String> attributes = new HashMap<>(pointSegmentRule.getAttributes());
			attributes.put("minzoom", "3");
			pointSegmentRule.init(attributes);
			pointSegmentRule.storeAttributes(attributes);
			changed = true;
		}

		if (selected) {
			RenderingRule orderSegmentRule = storage.getRule(ORDER_RULES, key);
			if (orderSegmentRule != null) {
				RenderingRule activityRule = null;
				for (RenderingRule renderingRule : orderSegmentRule.getIfElseChildren()) {
					if (Algorithms.objectEquals(renderingRule.getAttributes(), attrsMap)) {
						activityRule = renderingRule;
						break;
					}
				}
				orderSegmentRule.removeIfElseChildren(activityRule);
				changed = true;
			}
		} else {
			try {
				RenderingRule rule = new RenderingRule(attrsMap, false, storage);
				rule.storeAttributes(attrsMap);
				storage.registerTopLevel(rule, null, attrsMap, ORDER_RULES, true);
				changed = true;
			} catch (XmlPullParserException e) {
				log.error(e);
			}
		}

		if (changed && cloneStorage) {
			app.getRendererRegistry().updateRenderer(storage);
		}
		return changed;
	}

	@Override
	public void onRendererLoaded(String name, RenderingRulesStorage rules, InputStream source) {
		for (Map.Entry<String, CommonPreference<Boolean>> entry : routeTypesProps.entrySet()) {
			boolean selected = entry.getValue().get();
			String attrName = entry.getKey().replace(ACTIVITY_TYPE + "_", "");
			updateRouteTypeVisibility(rules, attrName, selected, false);
		}
	}
}
