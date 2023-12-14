package net.osmand.plus.myplaces.tracks;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.myplaces.tracks.filters.BaseTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.DateTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.FilterChangedListener;
import net.osmand.plus.myplaces.tracks.filters.FilterType;
import net.osmand.plus.myplaces.tracks.filters.ListTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.OtherTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.RangeTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.TextTrackFilter;

import java.util.Date;

public class TrackFiltersHelper {

	public static BaseTrackFilter createNameFilter(FilterType filterType, FilterChangedListener listener) {
		return new TextTrackFilter(filterType, listener);
	}

	public static BaseTrackFilter createDateFilter(FilterType filterType, Long minDate, FilterChangedListener listener) {
		return new DateTrackFilter(filterType, minDate, listener);
	}

	public static BaseTrackFilter createOtherFilter(OsmandApplication app, FilterType filterType, FilterChangedListener listener) {
		return new OtherTrackFilter(app, filterType, listener);
	}

	public static BaseTrackFilter createSingleListFilter(OsmandApplication app, FilterType filterType, FilterChangedListener listener) {
		return new ListTrackFilter(app, filterType, listener);
	}

	public static BaseTrackFilter createRangeFilter(OsmandApplication app, FilterType filterType, FilterChangedListener listener) {
		if (filterType.getDefaultParams().size() < 2) {
			throw new IllegalArgumentException("RangeTrackFilter needs 2 default params: minValue, maxValue");
		}
		Object minValue = filterType.getDefaultParams().get(0);
		Object maxValue = filterType.getDefaultParams().get(1);
		if (minValue.getClass() != maxValue.getClass()) {
			throw new IllegalArgumentException("RangeTrackFilter's 2 default params (minValue, maxValue) must be the same type");
		}
		Class<?> parameterClass = filterType.getPropertyList().get(0).getTypeClass();
		if (parameterClass == Double.class) {
			return new RangeTrackFilter<>((Double) minValue, (Double) maxValue, app, filterType, listener);
		} else if (parameterClass == Float.class) {
			return new RangeTrackFilter<>((Float) minValue, (Float) maxValue, app, filterType, listener);
		} else if (parameterClass == Integer.class) {
			return new RangeTrackFilter<>((Integer) minValue, (Integer) maxValue, app, filterType, listener);
		} else if (parameterClass == Long.class) {
			return new RangeTrackFilter<>((Long) minValue, (Long) maxValue, app, filterType, listener);
		}
		throw new IllegalArgumentException("Unsupported gpxParameter type class " + parameterClass);
	}

	public static BaseTrackFilter createFilter(OsmandApplication app, FilterType filterType, FilterChangedListener filterChangedListener) {
		BaseTrackFilter newFilter;

		switch (filterType.getFilterDisplayType()) {
			case TEXT: {
				newFilter = createNameFilter(filterType, filterChangedListener);
				break;
			}
			case RANGE: {
				newFilter = createRangeFilter(app, filterType, filterChangedListener);
				break;
			}
			case DATE_RANGE: {
				newFilter = createDateFilter(filterType, (new Date()).getTime(), filterChangedListener);
				break;
			}

			case OTHER: {
				newFilter = createOtherFilter(app, filterType, filterChangedListener);
				break;
			}

			case SINGLE_FIELD_LIST: {
				newFilter = createSingleListFilter(app, filterType, filterChangedListener);
				break;
			}

			default:
				throw new IllegalArgumentException("Unknown filterType " + filterType);
		}
		return newFilter;
	}

	@NonNull
	public static Class<? extends BaseTrackFilter> getFilterClass(FilterType filterType) {
		Class<? extends BaseTrackFilter> filterClass = null;
		switch (filterType.getFilterDisplayType()) {
			case TEXT: {
				filterClass = TextTrackFilter.class;
				break;
			}
			case RANGE: {
				filterClass = RangeTrackFilter.class;
				break;
			}
			case DATE_RANGE: {
				filterClass = DateTrackFilter.class;
				break;
			}

			case OTHER: {
				filterClass = OtherTrackFilter.class;
				break;
			}

			case SINGLE_FIELD_LIST: {
				filterClass = ListTrackFilter.class;
				break;
			}

			default:
				throw new IllegalArgumentException("Unknown filterType " + filterType);
		}
		return filterClass;
	}
}