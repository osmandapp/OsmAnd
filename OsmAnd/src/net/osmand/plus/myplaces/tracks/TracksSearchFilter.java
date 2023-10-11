package net.osmand.plus.myplaces.tracks;

import android.widget.Filter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.myplaces.tracks.filters.BaseTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.CityTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.DateCreationTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.FilterChangedListener;
import net.osmand.plus.myplaces.tracks.filters.FilterType;
import net.osmand.plus.myplaces.tracks.filters.LengthTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.TrackNameFilter;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TracksSearchFilter extends Filter implements FilterChangedListener {

	private List<TrackItem> trackItems;
	private CallbackWithObject<List<TrackItem>> callback;
	private List<BaseTrackFilter> currentFilters = new ArrayList<>();
	private List<FilterChangedListener> filterChangedListeners = new ArrayList<>();
	private List<TrackItem> filteredTrackItems;

	private OsmandApplication app;

	public TracksSearchFilter(@NonNull OsmandApplication app, @NonNull List<TrackItem> trackItems) {
		this.app = app;
		this.trackItems = trackItems;
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
			cityFilter.setFullCitiesList(app.getGpxDbHelper().getNearestCityList());
		}
	}

	public void setCallback(@Nullable CallbackWithObject<List<TrackItem>> callback) {
		this.callback = callback;
	}

	@Override
	protected FilterResults performFiltering(CharSequence constraint) {
		FilterResults results = new FilterResults();
		int filterCount = getAppliedFiltersCount();
		if (filterCount == 0) {
			results.values = trackItems;
			results.count = trackItems.size();
		} else {
			List<TrackItem> res = new ArrayList<>();
			for (BaseTrackFilter filter : currentFilters) {
				filter.initFilter();
			}
			for (TrackItem item : trackItems) {
				boolean needAddTrack = true;
				for (BaseTrackFilter filter : currentFilters) {
					if (filter.isEnabled() && !filter.isTrackAccepted(item)) {
						needAddTrack = false;
						break;
					}
				}
				if (needAddTrack) {
					res.add(item);
				}
			}
			results.values = res;
			results.count = res.size();
		}
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
			filterChangedListeners = Algorithms.addToList(filterChangedListeners, listener);
		}
	}

	public void removeFiltersChangedListener(FilterChangedListener listener) {
		if (filterChangedListeners.contains(listener)) {
			filterChangedListeners = Algorithms.removeFromList(filterChangedListeners, listener);
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
}

