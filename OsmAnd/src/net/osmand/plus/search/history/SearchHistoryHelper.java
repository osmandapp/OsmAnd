package net.osmand.plus.search.history;

import static net.osmand.plus.settings.enums.HistorySource.NAVIGATION;
import static net.osmand.plus.settings.enums.HistorySource.SEARCH;
import static net.osmand.search.core.SearchCoreFactory.SEARCH_AMENITY_TYPE_PRIORITY;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.data.City;
import net.osmand.data.PointDescription;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.search.QuickSearchHelper.SearchHistoryAPI;
import net.osmand.plus.settings.enums.HistorySource;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.search.SearchUICore;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchResult;
import net.osmand.util.CollectionUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class SearchHistoryHelper {

	private static final Log log = PlatformUtil.getLog(SearchHistoryHelper.class);

	private static final int HISTORY_LIMIT = 1500;

	private final OsmandApplication app;
	private final SearchHistoryDBHelper dbHelper;
	private final Map<HistoryEntryKey, HistoryEntry> map = new ConcurrentHashMap<>();

	private List<HistoryEntry> loadedEntries;

	public static class HistoryObject {
		private final Object object;
		private final SearchResult searchResult;

		private HistoryObject(@Nullable Object object, @NonNull SearchResult searchResult) {
			this.object = object;
			this.searchResult = searchResult;
		}

		@Nullable
		public Object getObject() {
			return object;
		}

		@NonNull
		public SearchResult getSearchResult() {
			return searchResult;
		}
	}

	@NonNull
	public static Object createHistoryObject(@Nullable Object object, @NonNull SearchResult searchResult) {
		return new HistoryObject(object, searchResult);
	}

	public SearchHistoryHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.dbHelper = new SearchHistoryDBHelper(app);
	}

	public long getLastModifiedTime() {
		return dbHelper.getLastModifiedTime();
	}

	public void setLastModifiedTime(long lastModifiedTime) {
		dbHelper.setLastModifiedTime(lastModifiedTime);
	}

	public void addNewItemToHistory(double latitude, double longitude, PointDescription name,
			HistorySource source, @Nullable Object object) {
		HistoryEntry entry = new HistoryEntry(latitude, longitude, name, source);
		applyObjectMetadata(entry, name, object);
		addNewItemToHistory(entry);
	}

	public void addNewItemToHistory(double latitude, double longitude, PointDescription name,
			HistorySource source) {
		addNewItemToHistory(latitude, longitude, name, source, null);
	}

	public void addNewItemToHistory(AbstractPoiType poiType, HistorySource source) {
		addNewItemToHistory(new HistoryEntry(0, 0, createPointDescription(poiType), source));
	}

	public void addNewItemToHistory(PoiUIFilter filter, HistorySource source) {
		addNewItemToHistory(new HistoryEntry(0, 0, createPointDescription(filter), source));
		if (app.getSettings().SEARCH_HISTORY.get()) {
			app.getPoiFilters().markHistory(filter.getFilterId(), true);
		}
	}

	public void addNewItemToHistory(GPXInfo gpxInfo, HistorySource source) {
		if (gpxInfo != null) {
			addNewItemToHistory(new HistoryEntry(0, 0, createPointDescription(gpxInfo), source));
		}
	}

	@NonNull
	public List<HistoryEntry> getHistoryEntries(boolean onlyPoints) {
		return getHistoryEntries(null, onlyPoints, false);
	}

	@NonNull
	public List<HistoryEntry> getHistoryEntries(@Nullable HistorySource source,
			boolean onlyPoints) {
		return getHistoryEntries(source, onlyPoints, false);
	}

	@NonNull
	public List<HistoryEntry> getHistoryEntries(@Nullable HistorySource source, boolean onlyPoints,
			boolean includeDeleted) {
		return getHistoryEntries(source, onlyPoints, includeDeleted, false);
	}

	@NonNull
	public List<HistoryEntry> getVisibleHistoryEntries(@Nullable HistorySource source, boolean onlyPoints,
			boolean includeDeleted) {
		return getHistoryEntries(source, onlyPoints, includeDeleted, true);
	}

	@NonNull
	private List<HistoryEntry> getHistoryEntries(@Nullable HistorySource source, boolean onlyPoints,
			boolean includeDeleted, boolean onlyEnabledSources) {
		if (loadedEntries == null) {
			checkLoadedEntries();
		}
		List<HistoryEntry> entries = new ArrayList<>();
		boolean searchHistoryEnabled = app.getSettings().SEARCH_HISTORY.get();
		boolean navigationHistoryEnabled = app.getSettings().NAVIGATION_HISTORY.get();

		for (HistoryEntry entry : loadedEntries) {
			PointDescription description = entry.getName();
			boolean exists = isPointDescriptionExists(description);
			boolean historyEnabled = entry.getSource() == SEARCH && searchHistoryEnabled
					|| entry.getSource() == NAVIGATION && navigationHistoryEnabled;
			if ((includeDeleted || exists)
					&& (!onlyEnabledSources || historyEnabled)
					&& (source == null || entry.getSource() == source)) {
				if (!onlyPoints || (!description.isPoiType() && !description.isCustomPoiFilter())) {
					entries.add(entry);
				}
			}
		}
		return entries;
	}

	public boolean isHistoryEnabled(@NonNull HistorySource source) {
		return switch (source) {
			case SEARCH -> app.getSettings().SEARCH_HISTORY.get();
			case NAVIGATION -> app.getSettings().NAVIGATION_HISTORY.get();
		};
	}

	private boolean isPointDescriptionExists(@NonNull PointDescription description) {
		String name = description.getName();
		if (description.isPoiType()) {
			MapPoiTypes poiTypes = app.getPoiTypes();
			return poiTypes.getAnyPoiTypeByKey(name) != null || poiTypes.getAnyPoiAdditionalTypeByKey(name) != null;
		} else if (description.isCustomPoiFilter()) {
			return app.getPoiFilters().getFilterById(name, true) != null;
		} else if (description.isGpxFile()) {
			return GpxUiHelper.getGpxInfoByFileName(app, name) != null;
		}
		return true;
	}

	@NonNull
	public List<SearchResult> getHistoryResults(@Nullable HistorySource source, boolean onlyPoints,
			boolean includeDeleted) {
		List<SearchResult> searchResults = new ArrayList<>();

		SearchPhrase phrase = SearchPhrase.emptyPhrase(app.getSearchUICore().getCore().getSearchSettings());
		for (HistoryEntry entry : getHistoryEntries(source, onlyPoints, includeDeleted)) {
			SearchResult result = SearchHistoryAPI.createSearchResult(app, entry, phrase);
			searchResults.add(result);
		}
		return searchResults;
	}

	private PointDescription createPointDescription(AbstractPoiType pt) {
		return new PointDescription(PointDescription.POINT_TYPE_POI_TYPE, pt.getKeyName());
	}

	private PointDescription createPointDescription(PoiUIFilter filter) {
		return new PointDescription(PointDescription.POINT_TYPE_CUSTOM_POI_FILTER, filter.getFilterId());
	}

	private PointDescription createPointDescription(GPXInfo gpxInfo) {
		return new PointDescription(PointDescription.POINT_TYPE_GPX_FILE, gpxInfo.getFileName());
	}

	public void remove(SearchResult searchResult) {
		HistoryEntry entry = searchResult instanceof SearchHistoryAPI.HistorySearchResult historySearchResult
				? historySearchResult.getHistoryEntry()
				: getHistoryEntry(searchResult.object);
		if (entry == null) {
			entry = getHistoryEntry(searchResult.relatedObject);
		}

		if (entry != null) {
			remove(entry);
			return;
		}

		PointDescription pd = getPointDescription(searchResult.object);
		if (pd == null) {
			pd = getPointDescription(searchResult.relatedObject);
		}

		if (pd != null) {
			remove(pd);
		} else {
			log.error(String.format(
					"Can't get PointDescription from SearchResult: %s, object: %s (%s), relatedObject: %s (%s), objectType: %s",
					searchResult,
					searchResult.object,
					searchResult.object != null ? searchResult.object.getClass() : null,
					searchResult.relatedObject,
					searchResult.relatedObject != null ? searchResult.relatedObject.getClass() : null,
					searchResult.objectType
			));
		}
	}

	private PointDescription getPointDescription(Object item) {
		PointDescription pd = null;
		if (item instanceof HistoryEntry) {
			pd = ((HistoryEntry) item).getName();
		} else if (item instanceof AbstractPoiType) {
			pd = createPointDescription((AbstractPoiType) item);
		} else if (item instanceof PoiUIFilter) {
			pd = createPointDescription((PoiUIFilter) item);
		} else if (item instanceof GPXInfo) {
			pd = createPointDescription((GPXInfo) item);
		}
		return pd;
	}

	@Nullable
	private HistoryEntry getHistoryEntry(@Nullable Object item) {
		return item instanceof HistoryEntry historyEntry ? historyEntry : null;
	}

	private void remove(@NonNull HistoryEntry entry) {
		remove(entry.getName(), entry.getSource());
	}

	private void remove(PointDescription pd) {
		checkLoadedEntries();
		List<HistoryEntry> toRemove = new ArrayList<>();
		for (HistoryEntry entry : loadedEntries) {
			if (Objects.equals(pd, entry.getName())) {
				toRemove.add(entry);
			}
		}
		for (HistoryEntry entry : toRemove) {
			remove(entry);
		}
	}

	private void remove(@NonNull PointDescription pd, @NonNull HistorySource source) {
		checkLoadedEntries();
		HistoryEntryKey key = new HistoryEntryKey(pd, source);
		HistoryEntry model = map.get(key);
		if (model != null && checkLoadedEntries().remove(model)) {
			if (pd.isCustomPoiFilter()) {
				app.getPoiFilters().markHistory(pd.getName(), false);
			}
			loadedEntries = CollectionUtils.removeFromList(loadedEntries, model);
			map.remove(key);
		}
	}

	public void removeAll() {
		checkLoadedEntries();
		if (dbHelper.removeAll()) {
			app.getPoiFilters().clearHistory();
			loadedEntries = new ArrayList<>();
			map.clear();
		}
	}

	private SearchHistoryDBHelper checkLoadedEntries() {
		if (loadedEntries == null) {
			loadedEntries = sortHistoryEntries(dbHelper.getEntries());
			for (HistoryEntry he : loadedEntries) {
				map.put(new HistoryEntryKey(he), he);
			}
		}
		return dbHelper;
	}

	private void addNewItemToHistory(HistoryEntry model) {
		if (isHistoryEnabled(model.getSource())) {
			checkLoadedEntries();
			HistoryEntryKey key = new HistoryEntryKey(model);
			if (map.containsKey(key)) {
				HistoryEntry existingModel = map.get(key);
				if (existingModel != null) {
					if (model.hasMetadata()) {
						model.fillMissingMetadataFrom(existingModel);
						existingModel.copyMetadataFrom(model);
					}
					existingModel.markAsAccessed(System.currentTimeMillis());
					dbHelper.update(existingModel);
				}
			} else {
				loadedEntries = CollectionUtils.addToList(loadedEntries, model);
				map.put(key, model);
				model.markAsAccessed(System.currentTimeMillis());
				dbHelper.add(model);
			}
			updateEntriesList();
		}
	}

	public void addItemsToHistory(List<HistoryEntry> entries) {
		for (HistoryEntry model : entries) {
			addItemToHistoryWithReplacement(model);
		}
		updateEntriesList();
	}

	public void updateEntriesList() {
		checkLoadedEntries();
		List<HistoryEntry> historyEntries = sortHistoryEntries(loadedEntries);

		while (historyEntries.size() > HISTORY_LIMIT) {
			int lastIndex = historyEntries.size() - 1;
			if (dbHelper.remove(historyEntries.get(lastIndex))) {
				historyEntries.remove(lastIndex);
			}
		}
		loadedEntries = historyEntries;
	}

	private void addItemToHistoryWithReplacement(@NonNull HistoryEntry model) {
		checkLoadedEntries();
		List<HistoryEntry> historyEntries = new ArrayList<>(loadedEntries);

		PointDescription name = model.getName();
		HistoryEntryKey key = new HistoryEntryKey(model);
		if (map.containsKey(key)) {
			HistoryEntry oldModel = map.remove(key);
			historyEntries.remove(oldModel);
			dbHelper.remove(model);
		}
		historyEntries.add(model);
		loadedEntries = historyEntries;

		map.put(key, model);
		dbHelper.add(model);
	}

	@Nullable
	public HistoryEntry getEntryByName(@Nullable PointDescription pd) {
		if (pd == null) {
			return null;
		}
		checkLoadedEntries();
		for (HistoryEntry entry : loadedEntries) {
			if (Objects.equals(pd, entry.getName())) {
				return entry;
			}
		}
		return null;
	}

	@Nullable
	public HistoryEntry getEntryByName(@Nullable PointDescription pd, @NonNull HistorySource source) {
		checkLoadedEntries();
		return pd != null ? map.get(new HistoryEntryKey(pd, source)) : null;
	}

	@NonNull
	private List<HistoryEntry> sortHistoryEntries(@NonNull List<HistoryEntry> historyEntries) {
		List<HistoryEntry> entries = new ArrayList<>(historyEntries);
		Collections.sort(entries, new HistoryEntryComparator());
		return entries;
	}

	private static class HistoryEntryComparator implements Comparator<HistoryEntry> {

		long time = System.currentTimeMillis();

		@Override
		public int compare(HistoryEntry lhs, HistoryEntry rhs) {
			double l = lhs.getRank(time);
			double r = rhs.getRank(time);
			return -Double.compare(l, r);
		}
	}

	public void selectSearchResult(@NonNull SearchResult result) {
		if (result.object instanceof AbstractPoiType) {
			addNewItemToHistory((AbstractPoiType) result.object, SEARCH);
		} else if (result.object instanceof PoiUIFilter) {
			addNewItemToHistory((PoiUIFilter) result.object, SEARCH);
		}
		SearchUICore searchUICore = app.getSearchUICore().getCore();
		if (result.object instanceof PoiType && ((PoiType) result.object).isAdditional()) {
			PoiType additional = (PoiType) result.object;
			AbstractPoiType parent = additional.getParentType();
			if (parent != null) {
				PoiUIFilter custom = app.getPoiFilters().getFilterById(PoiUIFilter.STD_PREFIX + parent.getKeyName());
				if (custom != null) {
					custom.clearFilter();
					custom.updateTypesToAccept(parent);
					custom.setFilterByName(additional.getKeyName().replace('_', ':').toLowerCase(Locale.ROOT));

					SearchPhrase phrase = searchUICore.getPhrase();
					result = new SearchResult(phrase);
					result.localeName = custom.getName();
					result.object = custom;
					result.priority = SEARCH_AMENITY_TYPE_PRIORITY;
					result.priorityDistance = 0;
					result.objectType = ObjectType.POI_TYPE;
				}
			}
		}
		searchUICore.selectSearchResult(result);
	}

	private void applyObjectMetadata(@NonNull HistoryEntry entry, @NonNull PointDescription name,
			@Nullable Object object) {
		if (object instanceof HistoryEntry historyEntry) {
			entry.copyMetadataFrom(historyEntry);
			return;
		}
		entry.setDisplayName(name.getSimpleName(app, false));
		if (!Algorithms.isEmpty(name.getTypeName())) {
			entry.setTypeName(name.getTypeName());
		}
		if (object instanceof SearchResult searchResult) {
			applySearchResultMetadata(entry, searchResult);
			object = searchResult.object;
		} else if (object instanceof HistoryObject historyObject) {
			applySearchResultMetadata(entry, historyObject.getSearchResult());
			object = historyObject.getObject();
		}
		if (object instanceof Amenity amenity) {
			applyAmenityMetadata(entry, amenity);
		}
	}

	private void applyAmenityMetadata(@NonNull HistoryEntry entry, Amenity amenity) {
		String lang = app.getSettings().MAP_PREFERRED_LOCALE.get();
		boolean transliterate = app.getSettings().MAP_TRANSLITERATE_NAMES.get();
		entry.setObjectType(ObjectType.POI);
		String displayName = Amenity.getPoiStringWithoutType(amenity, lang, transliterate);
		if (!Algorithms.isEmpty(displayName)) {
			entry.setDisplayName(displayName);
		}
		entry.setTypeName(amenity.getType() != null ? amenity.getSubTypeStr() : amenity.getSubType());
		if (amenity.getType() != null) {
			entry.setPoiCategoryKey(amenity.getType().getKeyName());
		}
		entry.setPoiSubtypeKey(amenity.getSubType());
		entry.setOpeningHours(amenity.getOpeningHours());
		entry.setPhotoUrl(amenity.getWikiIconUrl());
		entry.setOsmId(amenity.getOsmId());
	}

	private void applySearchResultMetadata(@NonNull HistoryEntry entry, @NonNull SearchResult searchResult) {
		if (searchResult.objectType != null) {
			entry.setObjectType(searchResult.objectType);
		}
		if (searchResult.object instanceof City city) {
			entry.setCityType(city.getType());
		}
		if (!Algorithms.isEmpty(searchResult.localeName)) {
			entry.setDisplayName(searchResult.localeName);
		}
		if (!Algorithms.isEmpty(searchResult.addressName)) {
			entry.setAddress(searchResult.addressName);
		}
		if (!Algorithms.isEmpty(searchResult.localeRelatedObjectName)) {
			entry.setRelatedObjectName(searchResult.localeRelatedObjectName);
		}
		if (!Algorithms.isEmpty(searchResult.alternateName)) {
			entry.setAlternateName(searchResult.alternateName);
		}
		String typeName = getSearchResultTypeName(searchResult);
		if (!Algorithms.isEmpty(typeName)) {
			entry.setTypeName(typeName);
		}
	}

	@Nullable
	private String getSearchResultTypeName(@NonNull SearchResult searchResult) {
		try {
			return net.osmand.plus.search.listitems.QuickSearchListItem.getTypeName(app, searchResult);
		} catch (RuntimeException e) {
			return searchResult.localeRelatedObjectName;
		}
	}
}
