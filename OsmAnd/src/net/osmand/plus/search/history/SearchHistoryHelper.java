package net.osmand.plus.search.history;

import static net.osmand.plus.settings.enums.HistorySource.SEARCH;
import static net.osmand.search.core.SearchCoreFactory.SEARCH_AMENITY_TYPE_PRIORITY;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
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

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SearchHistoryHelper {

	private static final Log log = PlatformUtil.getLog(SearchHistoryHelper.class);

	private static final int HISTORY_LIMIT = 1500;

	private final OsmandApplication app;
	private final SearchHistoryDBHelper dbHelper;
	private final Map<PointDescription, HistoryEntry> map = new ConcurrentHashMap<>();

	private List<HistoryEntry> loadedEntries;

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
			HistorySource source) {
		addNewItemToHistory(new HistoryEntry(latitude, longitude, name, source));
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
		if (loadedEntries == null) {
			checkLoadedEntries();
		}
		List<HistoryEntry> entries = new ArrayList<>();
		for (HistoryEntry entry : loadedEntries) {
			PointDescription description = entry.getName();

			boolean exists = isPointDescriptionExists(description);
			if ((includeDeleted || exists) && (source == null || entry.getSource() == source)) {
				if (!onlyPoints || (!description.isPoiType() && !description.isCustomPoiFilter())) {
					entries.add(entry);
				}
			}
		}
		return entries;
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

		SearchPhrase phrase = SearchPhrase.emptyPhrase();
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
					searchResult.object.getClass(),
					searchResult.relatedObject,
					searchResult.relatedObject.getClass(),
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

	private void remove(PointDescription pd) {
		HistoryEntry model = map.get(pd);
		if (model != null && checkLoadedEntries().remove(model)) {
			if (pd.isCustomPoiFilter()) {
				app.getPoiFilters().markHistory(pd.getName(), false);
			}
			loadedEntries = CollectionUtils.removeFromList(loadedEntries, model);
			map.remove(pd);
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
				map.put(he.getName(), he);
			}
		}
		return dbHelper;
	}

	private void addNewItemToHistory(HistoryEntry model) {
		if (app.getSettings().SEARCH_HISTORY.get()) {
			checkLoadedEntries();
			if (map.containsKey(model.getName())) {
				model = map.get(model.getName());
				model.markAsAccessed(System.currentTimeMillis());
				dbHelper.update(model);
			} else {
				loadedEntries = CollectionUtils.addToList(loadedEntries, model);
				map.put(model.getName(), model);
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
		if (map.containsKey(name)) {
			HistoryEntry oldModel = map.remove(name);
			historyEntries.remove(oldModel);
			dbHelper.remove(model);
		}
		historyEntries.add(model);
		loadedEntries = historyEntries;

		map.put(name, model);
		dbHelper.add(model);
	}

	@Nullable
	public HistoryEntry getEntryByName(@Nullable PointDescription pd) {
		return pd != null ? map.get(pd) : null;
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
					custom.setFilterByName(additional.getKeyName().replace('_', ':').toLowerCase());

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
}
