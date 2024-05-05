package net.osmand.plus.myplaces.tracks;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.myplaces.tracks.filters.BaseTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.DateTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.FilterChangedListener;
import net.osmand.plus.myplaces.tracks.filters.ListTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.OtherTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.RangeTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.TextTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.TrackFilterType;

import java.util.Date;

public class TrackFiltersHelper {

	public static BaseTrackFilter createTextFilter(TrackFilterType filterType, FilterChangedListener listener) {
		return new TextTrackFilter(filterType, listener);
	}

	public static BaseTrackFilter createDateFilter(TrackFilterType trackFilterType, Long minDate, FilterChangedListener listener) {
		return new DateTrackFilter(trackFilterType, minDate, listener);
	}

	public static BaseTrackFilter createOtherFilter(OsmandApplication app, TrackFilterType trackFilterType, FilterChangedListener listener) {
		return new OtherTrackFilter(app, trackFilterType, listener);
	}

	public static BaseTrackFilter createSingleListFilter(OsmandApplication app, TrackFilterType trackFilterType, FilterChangedListener listener) {
		return new ListTrackFilter(app, trackFilterType, listener);
	}

	public static BaseTrackFilter createRangeFilter(OsmandApplication app, TrackFilterType trackFilterType, FilterChangedListener listener) {
		if (trackFilterType.getDefaultParams().size() < 2) {
			throw new IllegalArgumentException("RangeTrackFilter needs 2 default params: minValue, maxValue");
		}
		Object minValue = trackFilterType.getDefaultParams().get(0);
		Object maxValue = trackFilterType.getDefaultParams().get(1);
		if (minValue.getClass() != maxValue.getClass()) {
			throw new IllegalArgumentException("RangeTrackFilter's 2 default params (minValue, maxValue) must be the same type");
		}
		Class<?> parameterClass = trackFilterType.getProperty().getTypeClass();
		if (parameterClass == Double.class) {
			return new RangeTrackFilter<>((Double) minValue, (Double) maxValue, app, trackFilterType, listener);
		} else if (parameterClass == Float.class) {
			return new RangeTrackFilter<>((Float) minValue, (Float) maxValue, app, trackFilterType, listener);
		} else if (parameterClass == Integer.class) {
			return new RangeTrackFilter<>((Integer) minValue, (Integer) maxValue, app, trackFilterType, listener);
		} else if (parameterClass == Long.class) {
			return new RangeTrackFilter<>((Long) minValue, (Long) maxValue, app, trackFilterType, listener);
		}
		throw new IllegalArgumentException("Unsupported gpxParameter type class " + parameterClass);
	}

	public static BaseTrackFilter createFilter(OsmandApplication app, TrackFilterType trackFilterType, FilterChangedListener filterChangedListener) {
		BaseTrackFilter newFilter;

		switch (trackFilterType.getFilterType()) {
			case TEXT: {
				newFilter = createTextFilter(trackFilterType, filterChangedListener);
				break;
			}
			case RANGE: {
				newFilter = createRangeFilter(app, trackFilterType, filterChangedListener);
				break;
			}
			case DATE_RANGE: {
				newFilter = createDateFilter(trackFilterType, (new Date()).getTime(), filterChangedListener);
				break;
			}

			case OTHER: {
				newFilter = createOtherFilter(app, trackFilterType, filterChangedListener);
				break;
			}

			case SINGLE_FIELD_LIST: {
				newFilter = createSingleListFilter(app, trackFilterType, filterChangedListener);
				break;
			}

			default:
				throw new IllegalArgumentException("Unknown filterType " + trackFilterType);
		}
		return newFilter;
	}

	@NonNull
	public static Class<? extends BaseTrackFilter> getFilterClass(TrackFilterType trackFilterType) {
		Class<? extends BaseTrackFilter> filterClass = null;
		switch (trackFilterType.getFilterType()) {
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
				throw new IllegalArgumentException("Unknown filterType " + trackFilterType);
		}
		return filterClass;
	}
}