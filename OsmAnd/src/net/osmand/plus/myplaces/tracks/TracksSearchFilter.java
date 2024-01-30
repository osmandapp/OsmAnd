package net.osmand.plus.myplaces.tracks;

import android.widget.Filter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.myplaces.tracks.filters.BaseTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.DateTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.FilterChangedListener;
import net.osmand.plus.myplaces.tracks.filters.ListTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.RangeTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.SingleFieldTrackFilterParams;
import net.osmand.plus.myplaces.tracks.filters.TextTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.TrackFilterType;
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
	private Map<TrackFilterType, List<TrackItem>> filterSpecificSearchResults = new HashMap<>();
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

	@SuppressWarnings("unchecked")
	private void initFilters(@NonNull OsmandApplication app) {
		recreateFilters();

		app.getTaskManager().runInBackground(new OsmAndTaskManager.OsmAndTaskRunnable<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... voids) {
				DateTrackFilter dateFilter = (DateTrackFilter) getFilterByType(TrackFilterType.DATE_CREATION);
				if (dateFilter != null) {
					long minDate = app.getGpxDbHelper().getTracksMinCreateDate();
					long now = (new Date()).getTime();
					dateFilter.setInitialValueFrom(minDate);
					dateFilter.setInitialValueTo(now);
					dateFilter.setValueFrom(minDate);
					dateFilter.setValueTo(now);
				}
				for (TrackFilterType trackFilterType : TrackFilterType.values()) {
					switch (trackFilterType.getFilterType()) {
						case RANGE:
							updateRangeFilterMaxValue(trackFilterType);
							break;
						case SINGLE_FIELD_LIST:
							ListTrackFilter filter = (ListTrackFilter) getFilterByType(trackFilterType);
							if (filter != null) {
								SingleFieldTrackFilterParams filterParams = (SingleFieldTrackFilterParams) trackFilterType.getAdditionalData();
								filter.setFullItemsCollection(app.getGpxDbHelper().getStringIntItemsCollection(
										trackFilterType.getProperty().getColumnName(),
										filterParams.includeEmptyValues(),
										filterParams.sortByName(),
										filterParams.sortDescending()
								));
								if (trackFilterType == TrackFilterType.FOLDER) {
									if (currentFolder != null) {
										filter.setFirstItem(currentFolder.getDirName());
									}
								}
							}
							break;
						default:
							break;
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void unused) {
				onFilterChanged();
			}
		});
	}

	private void updateRangeFilterMaxValue(TrackFilterType trackFilterType) {
		BaseTrackFilter filter = getFilterByType(trackFilterType);
		if (filter instanceof RangeTrackFilter) {
			try {
				String maxValueInDb = app.getGpxDbHelper().getMaxParameterValue(trackFilterType.getProperty());
				if (maxValueInDb != null) {
					((RangeTrackFilter) filter).setMaxValue(maxValueInDb);
				}
			} catch (NumberFormatException error) {
				LOG.error("Can not parse max value for filter " + trackFilterType, error);
			}
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
				filterSpecificSearchResults.put(filter.getTrackFilterType(), new ArrayList<>());
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
						filterSpecificSearchResults.get(filter.getTrackFilterType()).add(item);
					}
				}
				if (Algorithms.isEmpty(notAcceptedFilters)) {
					res.add(item);
				}
			}
			results.values = res;
			results.count = res.size();
		}
		ListTrackFilter folderFilter = (ListTrackFilter) getFilterByType(TrackFilterType.FOLDER);
		if (folderFilter != null) {
			if (Algorithms.isEmpty(filterSpecificSearchResults)) {
				folderFilter.setFullItemsCollection(app.getGpxDbHelper().getStringIntItemsCollection(
						folderFilter.getTrackFilterType().getProperty().getColumnName(),
						folderFilter.getCollectionFilterParams().includeEmptyValues(),
						folderFilter.getCollectionFilterParams().sortByName(),
						folderFilter.getCollectionFilterParams().sortDescending()
				));
			} else {
				List<TrackItem> ignoreFoldersItems = filterSpecificSearchResults.get(TrackFilterType.FOLDER);
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


	public TextTrackFilter getNameFilter() {
		return (TextTrackFilter) getFilterByType(TrackFilterType.NAME);
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
		TextTrackFilter nameFilter = getNameFilter();
		if (nameFilter != null) {
			filter(nameFilter.getValue());
		}
	}

	@Nullable
	public BaseTrackFilter getFilterByType(TrackFilterType type) {
		for (BaseTrackFilter filter : currentFilters) {
			if (filter.getTrackFilterType() == type) {
				return filter;
			}
		}
		return null;
	}

	void recreateFilters() {
		List<BaseTrackFilter> newFiltersFilters = new ArrayList<>();
		for (TrackFilterType trackFilterType : TrackFilterType.values()) {
			newFiltersFilters.add(TrackFiltersHelper.createFilter(app, trackFilterType, this));
		}
		currentFilters = newFiltersFilters;
	}


	public void initSelectedFilters(@Nullable List<BaseTrackFilter> selectedFilters) {
		if (selectedFilters != null) {
			initFilters(app);
			for (BaseTrackFilter filter : getCurrentFilters()) {
				for (BaseTrackFilter selectedFilter : selectedFilters) {
					if (filter.getTrackFilterType() == selectedFilter.getTrackFilterType()) {
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

	public Map<TrackFilterType, List<TrackItem>> getFilterSpecificSearchResults() {
		return new HashMap<>(filterSpecificSearchResults);
	}
}

