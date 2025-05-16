package net.osmand.plus.search.dialogs;

import static net.osmand.CollatorStringMatcher.StringMatcherMode.CHECK_STARTS_FROM_SPACE;
import static net.osmand.shared.util.KMapUtils.MAX_LATITUDE;
import static net.osmand.shared.util.KMapUtils.MAX_LONGITUDE;
import static net.osmand.shared.util.KMapUtils.MIN_LATITUDE;
import static net.osmand.shared.util.KMapUtils.MIN_LONGITUDE;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.ResultMatcher;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.search.core.SearchPhrase.NameStringMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

class SearchCitiesTask extends AsyncTask<Void, Void, List<Amenity>> {

	private static final int SEARCH_CITY_LIMIT = 500;
	private static final List<String> CITY_SUB_TYPES = Arrays.asList("city", "town", "village");

	private final OsmandApplication app;

	private final LatLon latLon;
	private final String region;
	private final SearchCitiesListener listener;


	SearchCitiesTask(@NonNull OsmandApplication app, @NonNull String region, @NonNull LatLon latLon,
			@Nullable SearchCitiesListener listener) {
		this.app = app;
		this.region = region;
		this.listener = listener;
		this.latLon = latLon;
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.onSearchCitiesStarted();
		}
	}

	@Override
	protected List<Amenity> doInBackground(Void... voids) {
		List<Amenity> results = searchCities();
		sortCities(results);
		return results;
	}

	@NonNull
	private List<Amenity> searchCities() {
		NameStringMatcher matcher = new NameStringMatcher(region, CHECK_STARTS_FROM_SPACE);
		String lang = app.getSettings().MAP_PREFERRED_LOCALE.get();
		boolean transliterate = app.getSettings().MAP_TRANSLITERATE_NAMES.get();
		List<Amenity> amenities = new ArrayList<>();
		double lat = 0;
		double lon = 0;
		if (latLon != null) {
			lat = latLon.getLatitude();
			lon = latLon.getLongitude();
		}
		app.getResourceManager().getAmenitySearcher().searchAmenitiesByName(region, MAX_LATITUDE, MIN_LONGITUDE,
				MIN_LATITUDE, MAX_LONGITUDE, lat, lon, new ResultMatcher<>() {
					int count;

					@Override
					public boolean publish(Amenity amenity) {
						if (count++ > SEARCH_CITY_LIMIT) {
							return false;
						}
						List<String> otherNames = amenity.getOtherNames(true);
						String localeName = amenity.getName(lang, transliterate);
						String subType = amenity.getSubType();
						if (!CITY_SUB_TYPES.contains(subType)
								|| (!matcher.matches(localeName) && !matcher.matches(otherNames))) {
							return false;
						}
						amenities.add(amenity);
						return false;
					}

					@Override
					public boolean isCancelled() {
						return count > SEARCH_CITY_LIMIT || SearchCitiesTask.this.isCancelled();
					}
				});

		return amenities;
	}

	private void sortCities(@NonNull List<Amenity> cities) {
		Collator collator = OsmAndCollator.primaryCollator();
		cities.sort((Comparator<Object>) (obj1, obj2) -> {
			String str1;
			String str2;
			Amenity a = ((Amenity) obj1);
			if ("city".equals(a.getSubType())) {
				str1 = "!" + ((Amenity) obj1).getName();
			} else {
				str1 = ((Amenity) obj1).getName();
			}
			Amenity b = ((Amenity) obj2);
			if ("city".equals(b.getSubType())) {
				str2 = "!" + ((Amenity) obj2).getName();
			} else {
				str2 = ((Amenity) obj2).getName();
			}
			return collator.compare(str1, str2);
		});
	}

	@Override
	protected void onPostExecute(List<Amenity> cities) {
		if (listener != null) {
			listener.onSearchCitiesFinished(cities);

		}
	}

	public interface SearchCitiesListener {

		void onSearchCitiesStarted();

		void onSearchCitiesFinished(@NonNull List<Amenity> cities);
	}
}
