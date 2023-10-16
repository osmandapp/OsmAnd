package net.osmand.plus.myplaces.tracks;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
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

public class TrackFiltersHelper {

	public static BaseTrackFilter createFilter(OsmandApplication app, FilterType filterType, FilterChangedListener filterChangedListener) {
		BaseTrackFilter newFilter;

		switch (filterType) {
			case NAME: {
				newFilter = new TrackNameFilter(filterChangedListener);

			}
			break;
			case DURATION: {
				newFilter = new DurationTrackFilter(
						0f,
						TrackFiltersConstants.DEFAULT_MAX_VALUE,
						app, filterChangedListener);
			}
			break;
			case TIME_IN_MOTION: {
				newFilter = new TimeInMotionTrackFilter(
						0f,
						TrackFiltersConstants.DEFAULT_MAX_VALUE,
						app, filterChangedListener);
			}
			break;

			case LENGTH: {
				newFilter = new LengthTrackFilter(
						0f,
						TrackFiltersConstants.LENGTH_MAX_VALUE,
						app, filterChangedListener);
			}
			break;

			case AVERAGE_SPEED: {
				newFilter = new AverageSpeedTrackFilter(
						0f,
						TrackFiltersConstants.DEFAULT_MAX_VALUE,
						app, filterChangedListener);
			}
			break;

			case MAX_SPEED: {
				newFilter = new MaxSpeedTrackFilter(
						0f,
						TrackFiltersConstants.DEFAULT_MAX_VALUE,
						app, filterChangedListener);
			}
			break;

			case AVERAGE_ALTITUDE: {
				newFilter = new AverageAltitudeTrackFilter(
						0f,
						TrackFiltersConstants.DEFAULT_MAX_VALUE,
						app, filterChangedListener);
			}
			break;

			case MAX_ALTITUDE: {
				newFilter = new MaxAltitudeTrackFilter(
						0f,
						TrackFiltersConstants.ALTITUDE_MAX_VALUE,
						app, filterChangedListener);
			}
			break;

			case UPHILL: {
				newFilter = new UphillTrackFilter(
						0f,
						TrackFiltersConstants.DEFAULT_MAX_VALUE,
						app, filterChangedListener);
			}
			break;

			case DOWNHILL: {
				newFilter = new DownhillTrackFilter(
						0f,
						TrackFiltersConstants.DEFAULT_MAX_VALUE,
						app, filterChangedListener);
			}
			break;

			case DATE_CREATION: {
				newFilter = new DateCreationTrackFilter(filterChangedListener);
			}
			break;
			case CITY: {
				newFilter = new CityTrackFilter(filterChangedListener);
			}
			break;
			case OTHER: {
				newFilter = new OtherTrackFilter(app, filterChangedListener);
			}
			break;
			default:
				throw new IllegalArgumentException("Unknown filterType " + filterType);
		}
		return newFilter;
	}

	@NonNull
	public static Class<? extends BaseTrackFilter> getFilterClass(FilterType filterType) {
		Class<? extends BaseTrackFilter> filterClass = null;
		switch (filterType) {
			case NAME: {
				filterClass = TrackNameFilter.class;
				break;
			}
			case DURATION: {
				filterClass = DurationTrackFilter.class;
				break;
			}
			case TIME_IN_MOTION: {
				filterClass = TimeInMotionTrackFilter.class;
				break;
			}
			case LENGTH: {
				filterClass = LengthTrackFilter.class;
				break;
			}
			case AVERAGE_SPEED: {
				filterClass = AverageSpeedTrackFilter.class;
				break;
			}
			case MAX_SPEED: {
				filterClass = MaxSpeedTrackFilter.class;
				break;
			}
			case AVERAGE_ALTITUDE: {
				filterClass = AverageAltitudeTrackFilter.class;
				break;
			}
			case MAX_ALTITUDE: {
				filterClass = MaxAltitudeTrackFilter.class;
				break;
			}
			case UPHILL: {
				filterClass = UphillTrackFilter.class;
				break;
			}
			case DOWNHILL: {
				filterClass = DownhillTrackFilter.class;
				break;
			}
			case DATE_CREATION: {
				filterClass = DateCreationTrackFilter.class;
				break;
			}
			case CITY: {
				filterClass = CityTrackFilter.class;
				break;
			}
			case OTHER: {
				filterClass = OtherTrackFilter.class;
				break;
			}
			default:
				throw new IllegalArgumentException("Unknown filterType " + filterType);
		}
		return filterClass;
	}
}