package net.osmand.plus.poi;

import static net.osmand.plus.poi.PoiUIFilter.TOP_WIKI_FILTER_ID;
import static net.osmand.search.core.ObjectType.POI_TYPE;

import android.util.ArraySet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import net.osmand.PlatformUtil;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.search.SearchUICore;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.TopIndexFilter;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class PoiFiltersHelper {

	private static final Log LOG = PlatformUtil.getLog(PoiFiltersHelper.class);

	private final OsmandApplication app;
	private final OsmandSettings settings;

	private NominatimPoiFilter nominatimPOIFilter;
	private NominatimPoiFilter nominatimAddressFilter;

	private PoiUIFilter searchByNamePOIFilter;
	private PoiUIFilter customPOIFilter;
	private PoiUIFilter showAllPOIFilter;
	private PoiUIFilter topWikiPoiFilter;
	private List<PoiUIFilter> cacheTopStandardFilters = null;
	private Set<PoiUIFilter> selectedPoiFilters = new TreeSet<>();
	private Set<PoiUIFilter> overwrittenPoiFilters = null;

	public PoiFiltersHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		PoiFilterDbHelper helper = openDbHelperNoPois();
		helper.doDeletion();
		helper.close();
	}

	public long getLastModifiedTime() {
		PoiFilterDbHelper helper = openDbHelperNoPois();
		long lastModifiedTime = helper.getLastModifiedTime();
		helper.close();
		return lastModifiedTime;
	}

	public void setLastModifiedTime(long lastModifiedTime) {
		PoiFilterDbHelper helper = openDbHelperNoPois();
		helper.setLastModifiedTime(lastModifiedTime);
		helper.close();
	}

	public NominatimPoiFilter getNominatimPOIFilter() {
		if (nominatimPOIFilter == null) {
			nominatimPOIFilter = new NominatimPoiFilter(app, false);
		}
		return nominatimPOIFilter;
	}

	public NominatimPoiFilter getNominatimAddressFilter() {
		if (nominatimAddressFilter == null) {
			nominatimAddressFilter = new NominatimPoiFilter(app, true);
		}
		return nominatimAddressFilter;
	}

	public void resetNominatimFilters() {
		nominatimPOIFilter = null;
		nominatimAddressFilter = null;
	}

	public PoiUIFilter getSearchByNamePOIFilter() {
		if (searchByNamePOIFilter == null) {
			PoiUIFilter filter = new SearchByNameFilter(app);
			filter.setStandardFilter(true);
			searchByNamePOIFilter = filter;
		}
		return searchByNamePOIFilter;
	}

	public PoiUIFilter getCustomPOIFilter() {
		if (customPOIFilter == null) {
			PoiUIFilter filter = new PoiUIFilter(app.getString(R.string.poi_filter_custom_filter),
					PoiUIFilter.CUSTOM_FILTER_ID, new LinkedHashMap<>(), app);
			filter.setStandardFilter(true);
			customPOIFilter = filter;
		}
		return customPOIFilter;
	}

	@Nullable
	public PoiUIFilter getTopWikiPoiFilter() {
		if (topWikiPoiFilter == null) {
			topWikiPoiFilter = PluginsHelper.getPoiFilterById(TOP_WIKI_FILTER_ID);
		}
		return topWikiPoiFilter;
	}

	public PoiUIFilter getShowAllPOIFilter() {
		if (showAllPOIFilter == null) {
			PoiUIFilter filter = new PoiUIFilter(null, app, "");
			filter.setStandardFilter(true);
			showAllPOIFilter = filter;
		}
		return showAllPOIFilter;
	}

	public void markHistory(String filterId, boolean history) {
		PoiFilterDbHelper helper = openDbHelperNoPois();
		helper.markHistory(filterId, history);
		helper.close();
	}

	public void clearHistory() {
		PoiFilterDbHelper helper = openDbHelperNoPois();
		helper.clearHistory();
		helper.close();
	}

	@Nullable
	private PoiUIFilter getFilterById(String filterId, PoiUIFilter... filters) {
		for (PoiUIFilter pf : filters) {
			if (pf != null && pf.getFilterId() != null && pf.getFilterId().equals(filterId)) {
				return pf;
			}
		}
		return null;
	}

	public PoiUIFilter getFilterById(String filterId) {
		return getFilterById(filterId, false);
	}

	@Nullable
	public PoiUIFilter getFilterById(String filterId, boolean includeDeleted) {
		if (filterId == null) {
			return null;
		}
		for (PoiUIFilter filter : getTopDefinedPoiFilters(includeDeleted)) {
			if (filter.getFilterId().equals(filterId)) {
				return filter;
			}
		}
		PoiUIFilter filter = getFilterById(filterId, getCustomPOIFilter(), getSearchByNamePOIFilter(),
				getTopWikiPoiFilter(), getShowAllPOIFilter(), getNominatimPOIFilter(), getNominatimAddressFilter());
		if (filter != null) {
			return filter;
		}
		if (filterId.startsWith(PoiUIFilter.STD_PREFIX)) {
			String typeId = filterId.substring(PoiUIFilter.STD_PREFIX.length());
			AbstractPoiType tp = app.getPoiTypes().getAnyPoiTypeByKey(typeId);
			if (tp != null) {
				PoiUIFilter lf = new PoiUIFilter(tp, app, "");
				return addTopPoiFilter(lf);
			}
			AbstractPoiType lt = app.getPoiTypes().getAnyPoiAdditionalTypeByKey(typeId);
			if (lt != null) {
				PoiUIFilter lf = new PoiUIFilter(lt, app, "");
				return addTopPoiFilter(lf);
			}
		}
		return null;
	}

	@Nullable
	public PoiUIFilter getFilter(TopIndexFilter topIndexFilter,
			Map<PoiCategory, LinkedHashSet<String>> acceptedTypes) {
		PoiUIFilter poiUIFilter = new PoiUIFilter(topIndexFilter, acceptedTypes, app);
		return addTopPoiFilter(poiUIFilter);
	}

	public void reloadAllPoiFilters() {
		showAllPOIFilter = null;
		getShowAllPOIFilter();
		setTopStandardFilters(null);
		getTopStandardFilters();
	}

	public List<PoiUIFilter> getUserDefinedPoiFilters(boolean includeDeleted) {
		ArrayList<PoiUIFilter> userDefinedFilters = new ArrayList<>();
		PoiFilterDbHelper helper = openDbHelper();
		if (helper != null) {
			List<PoiUIFilter> userDefined = helper.getFilters(helper.getReadableDatabase(), includeDeleted);
			userDefinedFilters.addAll(userDefined);
			helper.close();
		}
		return userDefinedFilters;
	}

	public List<PoiUIFilter> getSearchPoiFilters() {
		List<PoiUIFilter> result = new ArrayList<>();
		List<PoiUIFilter> filters = Arrays.asList(getCustomPOIFilter(),  // getShowAllPOIFilter(),
				getSearchByNamePOIFilter(), getNominatimPOIFilter(), getNominatimAddressFilter());
		for (PoiUIFilter f : filters) {
			if (f != null && !f.isEmpty()) {
				result.add(f);
			}
		}
		return result;
	}

	@NonNull
	public List<PoiUIFilter> getTopDefinedPoiFilters() {
		return getTopDefinedPoiFilters(false);
	}

	@NonNull
	public List<PoiUIFilter> getTopDefinedPoiFilters(boolean includeDeleted) {
		List<PoiUIFilter> result = new ArrayList<>();
		List<PoiUIFilter> standardFilters = getTopStandardFilters();
		if (standardFilters != null) {
			for (PoiUIFilter filter : standardFilters) {
				if (includeDeleted || !filter.isDeleted()) {
					result.add(filter);
				}
			}
			result.add(getShowAllPOIFilter());
		}
		return result;
	}

	@Nullable
	private List<PoiUIFilter> getTopStandardFilters() {
		// collect top standard filters if poi types are initialized
		MapPoiTypes poiTypes = app.getPoiTypes();
		if (cacheTopStandardFilters == null && poiTypes.isInit()) {
			// user defined
			List<PoiUIFilter> filters = getUserDefinedPoiFilters(true);
			// default
			for (AbstractPoiType t : poiTypes.getTopVisibleFilters()) {
				filters.add(new PoiUIFilter(t, app, ""));
			}
			PluginsHelper.registerPoiFilters(filters);
			cacheTopStandardFilters = filters;
		}
		return cacheTopStandardFilters;
	}

	private void setTopStandardFilters(@Nullable List<PoiUIFilter> topStandardFilters) {
		this.cacheTopStandardFilters = topStandardFilters;
	}

	public List<String> getPoiFilterOrders(boolean onlyActive) {
		List<String> filterOrders = new ArrayList<>();
		for (PoiUIFilter filter : getSortedPoiFilters(onlyActive)) {
			filterOrders.add(filter.getFilterId());
		}
		return filterOrders;
	}

	public List<PoiUIFilter> getSortedPoiFilters(boolean onlyActive) {
		ApplicationMode selectedAppMode = settings.getApplicationMode();
		return getSortedPoiFilters(selectedAppMode, onlyActive);
	}

	public List<PoiUIFilter> getSortedPoiFilters(@NonNull ApplicationMode appMode,
			boolean onlyActive) {
		initPoiUIFiltersState(appMode);
		List<PoiUIFilter> allFilters = new ArrayList<>();
		allFilters.addAll(getTopDefinedPoiFilters());
		allFilters.addAll(getSearchPoiFilters());
		Collections.sort(allFilters);
		if (onlyActive) {
			List<PoiUIFilter> onlyActiveFilters = new ArrayList<>();
			for (PoiUIFilter f : allFilters) {
				if (f.isActive()) {
					onlyActiveFilters.add(f);
				}
			}
			return onlyActiveFilters;
		} else {
			return allFilters;
		}
	}

	private void initPoiUIFiltersState(@NonNull ApplicationMode appMode) {
		List<PoiUIFilter> allFilters = new ArrayList<>();
		allFilters.addAll(getTopDefinedPoiFilters());
		allFilters.addAll(getSearchPoiFilters());

		refreshPoiFiltersActivation(appMode, allFilters);
		refreshPoiFiltersOrder(appMode, allFilters);

		//set up the biggest order to custom filter
		PoiUIFilter customFilter = getCustomPOIFilter();
		customFilter.setActive(true);
		customFilter.setOrder(allFilters.size());
	}

	private void refreshPoiFiltersOrder(@NonNull ApplicationMode appMode,
			List<PoiUIFilter> filters) {
		Map<String, Integer> orders = getPoiFiltersOrder(appMode);
		List<PoiUIFilter> existedFilters = new ArrayList<>();
		List<PoiUIFilter> newFilters = new ArrayList<>();
		if (orders != null) {
			//set up orders from settings
			for (PoiUIFilter filter : filters) {
				Integer order = orders.get(filter.getFilterId());
				if (order != null) {
					filter.setOrder(order);
					existedFilters.add(filter);
				} else {
					newFilters.add(filter);
				}
			}
			//make order values without spaces
			Collections.sort(existedFilters);
			for (int i = 0; i < existedFilters.size(); i++) {
				existedFilters.get(i).setOrder(i);
			}
			//set up maximum orders for new poi filters
			Collections.sort(newFilters);
			for (PoiUIFilter filter : newFilters) {
				filter.setOrder(existedFilters.size());
				existedFilters.add(filter);
			}
		} else {
			for (PoiUIFilter filter : filters) {
				filter.setOrder(PoiUIFilter.INVALID_ORDER);
			}
		}
	}

	private void refreshPoiFiltersActivation(@NonNull ApplicationMode appMode,
			List<PoiUIFilter> filters) {
		List<String> inactiveFiltersIds = getInactivePoiFiltersIds(appMode);
		if (inactiveFiltersIds != null) {
			for (PoiUIFilter filter : filters) {
				filter.setActive(!inactiveFiltersIds.contains(filter.getFilterId()));
			}
		} else {
			for (PoiUIFilter filter : filters) {
				filter.setActive(true);
			}
		}
	}

	public void saveFiltersOrder(ApplicationMode appMode, List<String> filterIds) {
		settings.POI_FILTERS_ORDER.setStringsListForProfile(appMode, filterIds);
	}

	public void saveInactiveFilters(ApplicationMode appMode, List<String> filterIds) {
		settings.INACTIVE_POI_FILTERS.setStringsListForProfile(appMode, filterIds);
	}

	public Map<String, Integer> getPoiFiltersOrder(@NonNull ApplicationMode appMode) {
		List<String> ids = settings.POI_FILTERS_ORDER.getStringsListForProfile(appMode);
		if (ids == null) {
			return null;
		}
		Map<String, Integer> result = new HashMap<>();
		for (int i = 0; i < ids.size(); i++) {
			result.put(ids.get(i), i);
		}
		return result;
	}

	public List<String> getInactivePoiFiltersIds(@NonNull ApplicationMode appMode) {
		return settings.INACTIVE_POI_FILTERS.getStringsListForProfile(appMode);
	}

	@NonNull
	private PoiFilterDbHelper openDbHelperNoPois() {
		return new PoiFilterDbHelper(app, null);
	}

	@Nullable
	private PoiFilterDbHelper openDbHelper() {
		if (!app.getPoiTypes().isInit()) {
			return null;
		}
		return new PoiFilterDbHelper(app, app.getPoiTypes());
	}

	public boolean removePoiFilter(@NonNull PoiUIFilter filter) {
		if (filter.isCustomPoiFilter() ||
				filter.getFilterId().equals(PoiUIFilter.BY_NAME_FILTER_ID) ||
				filter.getFilterId().startsWith(PoiUIFilter.STD_PREFIX)) {
			return false;
		}
		PoiFilterDbHelper helper = openDbHelper();
		if (helper == null) {
			return false;
		}
		boolean res = helper.deleteFilter(helper.getWritableDatabase(), filter, false);
		helper.close();
		return res;
	}

	public boolean createPoiFilter(@NonNull PoiUIFilter filter, boolean forHistory) {
		PoiFilterDbHelper helper = openDbHelper();
		List<PoiUIFilter> standardFilters = getTopStandardFilters();
		if (standardFilters == null || helper == null) {
			return false;
		}
		helper.deleteFilter(helper.getWritableDatabase(), filter, true);

		Set<PoiUIFilter> filtersToRemove = new HashSet<>();
		for (PoiUIFilter f : standardFilters) {
			if (Objects.equals(f.getFilterId(), filter.getFilterId())) {
				filtersToRemove.add(f);
			}
		}
		setTopStandardFilters(standardFilters = CollectionUtils.removeAllFromList(standardFilters, filtersToRemove));
		boolean res = helper.addFilter(filter, helper.getWritableDatabase(), false, forHistory);
		if (res) {
			addTopPoiFilter(filter);
			Collections.sort(standardFilters);
		}
		helper.close();
		return res;
	}

	public boolean editPoiFilter(@NonNull PoiUIFilter filter) {
		if (filter.isCustomPoiFilter()
				|| filter.getFilterId().equals(PoiUIFilter.BY_NAME_FILTER_ID)
				|| filter.getFilterId().startsWith(PoiUIFilter.STD_PREFIX)) {
			return false;
		}
		PoiFilterDbHelper helper = openDbHelper();
		if (helper != null) {
			boolean res = helper.editFilter(helper.getWritableDatabase(), filter);
			helper.close();
			return res;
		}
		return false;
	}

	@NonNull
	public Set<PoiUIFilter> getGeneralSelectedPoiFilters() {
		Set<PoiUIFilter> selectedPoiFilters = getSelectedPoiFilters();
		PoiUIFilter wiki = getTopWikiPoiFilter();
		if (isPoiFilterSelected(wiki)) {
			Set<PoiUIFilter> result = new TreeSet<>(selectedPoiFilters);
			result.remove(wiki);
			return result;
		}
		return selectedPoiFilters;
	}

	@NonNull
	public Set<PoiUIFilter> getSelectedPoiFilters() {
		return overwrittenPoiFilters != null ? overwrittenPoiFilters : selectedPoiFilters;
	}

	public void replaceSelectedPoiFilters(@NonNull PoiUIFilter filter) {
		overwrittenPoiFilters = new TreeSet<>(Set.of(filter));
	}

	public void restoreSelectedPoiFilters() {
		overwrittenPoiFilters = null;
	}

	public void addSelectedPoiFilter(PoiUIFilter filter) {
		if (filter != null) {
			Set<PoiUIFilter> selectedPoiFilters = new TreeSet<>(getSelectedPoiFilters());
			selectedPoiFilters.add(filter);
			PluginsHelper.onPrepareExtraTopPoiFilters(selectedPoiFilters);
			setSelectedPoiFilters(selectedPoiFilters);
		}
	}

	public void removeSelectedPoiFilter(PoiUIFilter filter) {
		if (filter != null) {
			Set<PoiUIFilter> selectedPoiFilters = new TreeSet<>(getSelectedPoiFilters());
			selectedPoiFilters.remove(filter);
			setSelectedPoiFilters(selectedPoiFilters);
		}
	}

	private PoiUIFilter addTopPoiFilter(@NonNull PoiUIFilter filter) {
		List<PoiUIFilter> standardFilters = getTopStandardFilters();
		if (standardFilters != null) {
			setTopStandardFilters(CollectionUtils.addToList(standardFilters, filter));
		}
		return filter;
	}

	public boolean isShowingAnyPoi() {
		return !getSelectedPoiFilters().isEmpty();
	}

	public boolean isShowingAnyGeneralPoi() {
		return !getGeneralSelectedPoiFilters().isEmpty();
	}

	public void clearGeneralSelectedPoiFilters() {
		clearSelectedPoiFilters(true);
	}

	public void clearAllSelectedPoiFilters() {
		clearSelectedPoiFilters(false);
	}

	private void clearSelectedPoiFilters(boolean saveWiki) {
		Set<PoiUIFilter> selectedPoiFilters = new ArraySet<>();
		PoiUIFilter wiki = getTopWikiPoiFilter();
		if (saveWiki && isPoiFilterSelected(wiki)) {
			selectedPoiFilters.add(wiki);
		}
		setSelectedPoiFilters(selectedPoiFilters);
	}

	private void setSelectedPoiFilters(@NonNull Set<PoiUIFilter> filters) {
		if (overwrittenPoiFilters != null) {
			overwrittenPoiFilters = filters;
		} else {
			selectedPoiFilters = filters;
			saveSelectedPoiFilters(selectedPoiFilters);
		}
	}

	public String getSelectedPoiFiltersName() {
		return getFiltersName(getSelectedPoiFilters());
	}

	public String getGeneralSelectedPoiFiltersName() {
		return getFiltersName(getGeneralSelectedPoiFilters());
	}

	public String getFiltersName(Set<PoiUIFilter> filters) {
		if (filters.isEmpty()) {
			return app.getString(R.string.shared_string_none);
		} else {
			List<String> names = new ArrayList<>();
			for (PoiUIFilter filter : filters) {
				names.add(filter.getName());
			}
			return android.text.TextUtils.join(", ", names);
		}
	}

	public boolean isPoiFiltersSelected(@NonNull Collection<PoiUIFilter> filters) {
		for (PoiUIFilter filter : filters) {
			if (!isPoiFilterSelected(filter)) {
				return false;
			}
		}
		return !filters.isEmpty();
	}

	public boolean isPoiFilterSelected(PoiUIFilter filter) {
		return filter != null && isPoiFilterSelected(filter.filterId);
	}

	public boolean isPoiFilterSelected(String filterId) {
		for (PoiUIFilter filter : getSelectedPoiFilters()) {
			if (filter.filterId.equals(filterId)) {
				return true;
			}
		}
		return false;
	}

	public void loadSelectedPoiFilters() {
		// don't deal with not loaded poi types
		if (!app.getPoiTypes().isInit()) {
			return;
		}
		Set<PoiUIFilter> selectedPoiFilters = new TreeSet<>();
		Set<String> selectedFiltersIds = settings.getSelectedPoiFilters();
		for (String filterId : selectedFiltersIds) {
			PoiUIFilter filter = getFilterById(filterId);
			if (filter != null) {
				selectedPoiFilters.add(filter);
			}
		}
		PluginsHelper.onPrepareExtraTopPoiFilters(selectedPoiFilters);
		this.selectedPoiFilters = selectedPoiFilters;
	}

	@Nullable
	public Pair<Long, Map<String, List<String>>> getCacheByResourceName(String fileName) {
		Pair<Long, Map<String, List<String>>> cache = null;
		PoiFilterDbHelper helper = openDbHelper();
		if (helper != null) {
			SQLiteConnection readableDb = helper.getReadableDatabase();
			if (readableDb != null) {
				cache = helper.getCacheByResourceName(readableDb, fileName);
			}
			helper.close();
		}
		return cache;
	}

	public void updateCacheForResource(String fileName, long lastModified,
			Map<String, List<String>> categories) {
		PoiFilterDbHelper helper = openDbHelper();
		if (helper != null) {
			SQLiteConnection readableDb = helper.getReadableDatabase();
			if (readableDb != null) {
				helper.updateCacheForResource(readableDb, fileName, lastModified, categories);
			}
			helper.close();
		}
	}

	public void insertCacheForResource(String fileName, long lastModified,
			Map<String, List<String>> categories) {
		PoiFilterDbHelper helper = openDbHelper();
		if (helper != null) {
			SQLiteConnection readableDb = helper.getReadableDatabase();
			if (readableDb != null) {
				helper.insertCacheForResource(readableDb, fileName, lastModified, categories);
			}
			helper.close();
		}
	}

	private void saveSelectedPoiFilters(@NonNull Set<PoiUIFilter> selectedPoiFilters) {
		Set<String> filters = new HashSet<>();
		for (PoiUIFilter filter : selectedPoiFilters) {
			filters.add(filter.filterId);
		}
		settings.setSelectedPoiFilters(filters);
	}

	public PoiUIFilter getShowOnMapFilter(@NonNull SearchPhrase searchPhrase) {
		PoiUIFilter filter = null;
		if (searchPhrase.isNoSelectedType() || searchPhrase.isLastWord(POI_TYPE)) {
			SearchUICore searchUICore = app.getSearchUICore().getCore();
			if (searchPhrase.isNoSelectedType()) {
				AbstractPoiType unselectedPoiType = searchUICore.getUnselectedPoiType();
				if (searchUICore.isOnlineSearch() && !Algorithms.isEmpty(searchPhrase.getFirstUnknownSearchWord())) {
					app.getPoiFilters().resetNominatimFilters();
					filter = app.getPoiFilters().getNominatimAddressFilter();
					filter.setFilterByName(searchPhrase.getUnknownSearchPhrase());
					filter.clearCurrentResults();
				} else if (unselectedPoiType != null) {
					filter = new PoiUIFilter(unselectedPoiType, app, "");
					String customName = searchUICore.getCustomNameFilter();
					if (!Algorithms.isEmpty(customName)) {
						filter.setFilterByName(customName);
					}
				} else {
					filter = app.getPoiFilters().getSearchByNamePOIFilter();
					String searchWord = searchPhrase.getUnknownWordToSearch();
					if (!Algorithms.isEmpty(searchWord)) {
						filter.setFilterByName(searchWord);
						filter.clearCurrentResults();
					}
				}
			} else if (searchPhrase.getLastSelectedWord().getResult().object instanceof AbstractPoiType) {
				if (searchPhrase.isNoSelectedType()) {
					filter = new PoiUIFilter(null, app, "");
				} else {
					AbstractPoiType abstractPoiType = (AbstractPoiType) searchPhrase.getLastSelectedWord().getResult().object;
					filter = new PoiUIFilter(abstractPoiType, app, "");
				}
				if (!Algorithms.isEmpty(searchPhrase.getFirstUnknownSearchWord())) {
					filter.setFilterByName(searchPhrase.getFirstUnknownSearchWord());
				}
			} else if (searchPhrase.getLastSelectedWord().getResult().object instanceof PoiUIFilter) {
				filter = (PoiUIFilter) searchPhrase.getLastSelectedWord().getResult().object;
				if (!Algorithms.isEmpty(searchPhrase.getFirstUnknownSearchWord())) {
					filter.setFilterByName(searchPhrase.getFirstUnknownSearchWord());
				}
			}
		}
		return filter;
	}
}