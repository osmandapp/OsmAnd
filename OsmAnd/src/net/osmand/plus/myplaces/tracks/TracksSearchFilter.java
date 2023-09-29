package net.osmand.plus.myplaces.tracks;

import android.widget.Filter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.myplaces.tracks.filters.AverageAltitudeTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.AverageSpeedTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.BaseTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.CityTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.DateCreationTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.DownhillTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.DurationTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.FilterChangedListener;
import net.osmand.plus.myplaces.tracks.filters.FilterType;
import net.osmand.plus.myplaces.tracks.filters.LengthTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.MaxAltitudeTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.MaxSpeedTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.OtherTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.TimeInMotionTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.TrackFiltersConstants;
import net.osmand.plus.myplaces.tracks.filters.TrackNameFilter;
import net.osmand.plus.myplaces.tracks.filters.UphillTrackFilter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TracksSearchFilter extends Filter implements FilterChangedListener {

	private final List<TrackItem> trackItems;
	private CallbackWithObject<List<TrackItem>> callback;
	private List<BaseTrackFilter> currentFilters = new ArrayList<>();
	private List<FilterChangedListener> filterChangedListeners = new ArrayList<>();

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
			ArrayList<FilterChangedListener> newListeners = new ArrayList<>(filterChangedListeners);
			newListeners.add(listener);
			filterChangedListeners = newListeners;
		}
	}

	public void removeFiltersChangedListener(FilterChangedListener listener) {
		if (filterChangedListeners.contains(listener)) {
			ArrayList<FilterChangedListener> newListeners = new ArrayList(filterChangedListeners);
			newListeners.remove(listener);
			filterChangedListeners = newListeners;
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
			currentFilters.add(createFilter(filterType));
		}

	}

	private BaseTrackFilter createFilter(FilterType filterType) {
		BaseTrackFilter newFilter;

		switch (filterType) {
			case NAME: {
				newFilter = new TrackNameFilter(this);

			}
			break;
			case DURATION: {
				newFilter = new DurationTrackFilter(
						0f,
						TrackFiltersConstants.DEFAULT_MAX_VALUE,
						app, this);
			}
			break;
			case TIME_IN_MOTION: {
				newFilter = new TimeInMotionTrackFilter(
						0f,
						TrackFiltersConstants.DEFAULT_MAX_VALUE,
						app, this);
			}
			break;

			case LENGTH: {
				newFilter = new LengthTrackFilter(
						0f,
						TrackFiltersConstants.LENGTH_MAX_VALUE,
						app, this);
			}
			break;

			case AVERAGE_SPEED: {
				newFilter = new AverageSpeedTrackFilter(
						0f,
						TrackFiltersConstants.DEFAULT_MAX_VALUE,
						app, this);
			}
			break;

			case MAX_SPEED: {
				newFilter = new MaxSpeedTrackFilter(
						0f,
						TrackFiltersConstants.DEFAULT_MAX_VALUE,
						app, this);
			}
			break;

			case AVERAGE_ALTITUDE: {
				newFilter = new AverageAltitudeTrackFilter(
						0f,
						TrackFiltersConstants.DEFAULT_MAX_VALUE,
						app, this);
			}
			break;

			case MAX_ALTITUDE: {
				newFilter = new MaxAltitudeTrackFilter(
						0f,
						TrackFiltersConstants.ALTITUDE_MAX_VALUE,
						app, this);
			}
			break;

			case UPHILL: {
				newFilter = new UphillTrackFilter(
						0f,
						TrackFiltersConstants.DEFAULT_MAX_VALUE,
						app, this);
			}
			break;

			case DOWNHILL: {
				newFilter = new DownhillTrackFilter(
						0f,
						TrackFiltersConstants.DEFAULT_MAX_VALUE,
						app, this);
			}
			break;

			case DATE_CREATION: {
				newFilter = new DateCreationTrackFilter(this);
			}
			break;
			case CITY: {
				newFilter = new CityTrackFilter(this);
			}
			break;
			case OTHER: {
				newFilter = new OtherTrackFilter(app, this);
			}
			break;
			default:
				throw new IllegalArgumentException("Unknown filterType $filterType");
		}


		return newFilter;

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
}

