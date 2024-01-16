package net.osmand.plus.myplaces.tracks;

import android.widget.Filter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.myplaces.tracks.filters.BaseTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.CityTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.ColorTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.DateCreationTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.FilterChangedListener;
import net.osmand.plus.myplaces.tracks.filters.FilterType;
import net.osmand.plus.myplaces.tracks.filters.LengthTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.TrackFolderFilter;
import net.osmand.plus.myplaces.tracks.filters.TrackNameFilter;
import net.osmand.plus.myplaces.tracks.filters.WidthTrackFilter;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TracksSearchFilter extends Filter implements FilterChangedListener {
	public static final Log LOG = PlatformUtil.getLog(TracksSearchFilter.class);

	private List<TrackItem> trackItems;
	private CallbackWithObject<List<TrackItem>> callback;
	private List<BaseTrackFilter> currentFilters = new ArrayList<>();
	private List<FilterChangedListener> filterChangedListeners = new ArrayList<>();
	private List<TrackItem> filteredTrackItems;
	private Map<FilterType, List<TrackItem>> filterSpecificSearchResults = new HashMap<>();
	@Nullable
	private TrackFolder currentFolder;

	private OsmandApplication app;

	public TracksSearchFilter(@NonNull OsmandApplication app, @NonNull List<TrackItem> trackItems) {
		this(app, trackItems, null);
	}

	public TracksSearchFilter(@NonNull OsmandApplication app, @NonNull List<TrackItem> trackItems, @Nullable TrackFolder currentFolder) {
		this.app = app;
		this.trackItems = trackItems;
		this.currentFolder = currentFolder;
		initFilters(app);
	}

	private void initFilters(@NonNull OsmandApplication app) {
		recreateFilters();
		DateCreationTrackFilter dateFilter = (DateCreationTrackFilter) getFilterByType(FilterType.DATE_CREATION);
		if (dateFilter != null) {
			long minDate = app.getGpxDbHelper().getTracksMinCreateDate();
			long now = (new Date()).getTime();
			dateFilter.setInitialValueFrom(minDate);
			dateFilter.setInitialValueTo(now);
			dateFilter.setValueFrom(minDate);
			dateFilter.setValueTo(now);
		}
		LengthTrackFilter lengthFilter = (LengthTrackFilter) getFilterByType(FilterType.LENGTH);
		if (lengthFilter != null) {
			lengthFilter.setMaxValue((float) app.getGpxDbHelper().getTracksMaxDuration());
		}
		CityTrackFilter cityFilter = (CityTrackFilter) getFilterByType(FilterType.CITY);
		if (cityFilter != null) {
			cityFilter.setFullItemsCollection(app.getGpxDbHelper().getNearestCityList());
		}
		ColorTrackFilter colorsFilter = (ColorTrackFilter) getFilterByType(FilterType.COLOR);
		if (colorsFilter != null) {
			colorsFilter.setFullItemsCollection(app.getGpxDbHelper().getTrackColorsList());
		}
		WidthTrackFilter widthFilter = (WidthTrackFilter) getFilterByType(FilterType.WIDTH);
		if (widthFilter != null) {
			widthFilter.setFullItemsCollection(app.getGpxDbHelper().getTrackWidthList());
		}
		TrackFolderFilter folderFilter = (TrackFolderFilter) getFilterByType(FilterType.FOLDER);
		if (folderFilter != null) {
			folderFilter.setFullItemsCollection(app.getGpxDbHelper().getTrackFolders());
			folderFilter.setCurrentFolder(currentFolder);
		}

	}

	public void setCallback(@Nullable CallbackWithObject<List<TrackItem>> callback) {
		this.callback = callback;
	}

	@Override
	protected FilterResults performFiltering(CharSequence constraint) {
		LOG.debug("perform tracks filtering");
		FilterResults results = new FilterResults();
		filterSpecificSearchResults = new HashMap<>();
		int filterCount = getAppliedFiltersCount();
		if (filterCount == 0) {
			results.values = trackItems;
			results.count = trackItems.size();
		} else {
			List<TrackItem> res = new ArrayList<>();
			for (BaseTrackFilter filter : currentFilters) {
				filter.initFilter();
				filterSpecificSearchResults.put(filter.getFilterType(), new ArrayList<>());
			}
			for (TrackItem item : trackItems) {
				ArrayList<BaseTrackFilter> notAcceptedFilters = new ArrayList<>();
				for (BaseTrackFilter filter : currentFilters) {
					if (filter.isEnabled() && !filter.isTrackAccepted(item)) {
						notAcceptedFilters.add(filter);
					}
				}
				for (BaseTrackFilter filter : currentFilters) {
					ArrayList<BaseTrackFilter> tmpNotAcceptedFilters = new ArrayList<>(notAcceptedFilters);
					tmpNotAcceptedFilters.remove(filter);
					if (Algorithms.isEmpty(tmpNotAcceptedFilters)) {
						filterSpecificSearchResults.get(filter.getFilterType()).add(item);
					}
				}
				if (Algorithms.isEmpty(notAcceptedFilters)) {
					res.add(item);
				}
			}
			results.values = res;
			results.count = res.size();
		}
		TrackFolderFilter folderFilter = (TrackFolderFilter) getFilterByType(FilterType.FOLDER);
		if (folderFilter != null) {
			if (Algorithms.isEmpty(filterSpecificSearchResults)) {
				folderFilter.setFullItemsCollection(app.getGpxDbHelper().getTrackFolders());
			} else {
				List<TrackItem> ignoreFoldersItems = filterSpecificSearchResults.get(FilterType.FOLDER);
				folderFilter.updateFullCollection(ignoreFoldersItems);
			}
		}
		LOG.debug("found " + results.count + " tracks");
		return results;
	}

	@Override
	protected void publishResults(CharSequence constraint, FilterResults results) {
		if (callback != null) {
			callback.processResult((List<TrackItem>) results.values);
		}
	}

	public int getAppliedFiltersCount() {
		return getAppliedFilters().size();
	}

	@NonNull
	public List<BaseTrackFilter> getCurrentFilters() {
		return currentFilters;
	}

	@NonNull
	public List<BaseTrackFilter> getAppliedFilters() {
		ArrayList<BaseTrackFilter> appliedFilters = new ArrayList<>();
		for (BaseTrackFilter filter : currentFilters) {
			if (filter.isEnabled()) {
				appliedFilters.add(filter);
			}
		}
		return appliedFilters;
	}


	public TrackNameFilter getNameFilter() {
		return (TrackNameFilter) getFilterByType(FilterType.NAME);
	}

	public void addFiltersChangedListener(FilterChangedListener listener) {
		if (!filterChangedListeners.contains(listener)) {
			filterChangedListeners = CollectionUtils.addToList(filterChangedListeners, listener);
		}
	}

	public void removeFiltersChangedListener(FilterChangedListener listener) {
		if (filterChangedListeners.contains(listener)) {
			filterChangedListeners = CollectionUtils.removeFromList(filterChangedListeners, listener);
		}
	}

	public void resetCurrentFilters() {
		initFilters(app);
		filter("");
	}

	public void filter() {
		TrackNameFilter nameFilter = getNameFilter();
		if (nameFilter != null) {
			filter(nameFilter.getValue());
		}
	}

	@Nullable
	public BaseTrackFilter getFilterByType(FilterType type) {
		for (BaseTrackFilter filter : currentFilters) {
			if (filter.getFilterType() == type) {
				return filter;
			}
		}
		return null;
	}

	void recreateFilters() {
		currentFilters.clear();
		for (FilterType filterType : FilterType.values()) {
			currentFilters.add(TrackFiltersHelper.createFilter(app, filterType, this));
		}
	}


	public void initSelectedFilters(@Nullable List<BaseTrackFilter> selectedFilters) {
		if (selectedFilters != null) {
			initFilters(app);
			for (BaseTrackFilter filter : getCurrentFilters()) {
				for (BaseTrackFilter selectedFilter : selectedFilters) {
					if (filter.getFilterType() == selectedFilter.getFilterType()) {
						filter.initWithValue(selectedFilter);
					}
				}
			}
		}
	}


	@Override
	public void onFilterChanged() {
		for (FilterChangedListener listener : filterChangedListeners) {
			listener.onFilterChanged();
		}

	}

	public void resetFilteredItems() {
		filteredTrackItems = null;
	}

	@Nullable
	public List<TrackItem> getFilteredTrackItems() {
		return filteredTrackItems;
	}

	public void setFilteredTrackItems(List<TrackItem> trackItems) {
		filteredTrackItems = trackItems;
	}

	public void setAllItems(List<TrackItem> trackItems) {
		this.trackItems = trackItems;
	}

	public List<TrackItem> getAllItems() {
		return trackItems;
	}

	public void setCurrentFolder(TrackFolder currentFolder) {
		this.currentFolder = currentFolder;
	}

	public Map<FilterType, List<TrackItem>> getFilterSpecificSearchResults() {
		return new HashMap<>(filterSpecificSearchResults);
	}
}

