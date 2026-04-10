package net.osmand.plus.search.listitems;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.helpers.LocaleHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.search.SearchUICore;
import net.osmand.search.SearchUICore.SearchResultCollection;
import net.osmand.search.core.SearchCoreFactory;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchSettings;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public class SearchPopularPlacesTask extends AsyncTask<Void, Void, SearchResultCollection> {

	private static final int SEARCH_POI_RADIUS = 15000;

	private final OsmandApplication app;
	private final LatLon latLon;
	private final PoiUIFilter filter;
	private final CallbackWithObject<SearchResultCollection> callback;

	public SearchPopularPlacesTask(@NonNull OsmandApplication app, @NonNull PoiUIFilter filter,
			@NonNull LatLon latLon, @Nullable CallbackWithObject<SearchResultCollection> callback) {
		this.app = app;
		this.filter = filter;
		this.latLon = latLon;
		this.callback = callback;
	}

	@Override
	protected SearchResultCollection doInBackground(Void... params) {
		QuadRect rect = MapUtils.calculateLatLonBbox(latLon.getLatitude(), latLon.getLongitude(), SEARCH_POI_RADIUS);
		List<Amenity> amenities = getAmenities(rect);
		return createSearchResultCollection(amenities);
	}

	@NonNull
	private List<Amenity> getAmenities(@NonNull QuadRect rect) {
		return filter.searchAmenities(rect.top, rect.left, rect.bottom, rect.right, -1, null, true);
	}

	@NonNull
	public SearchResultCollection createSearchResultCollection(@NonNull List<Amenity> amenities) {
		SearchUICore core = app.getSearchUICore().getCore();
		String locale = LocaleHelper.getPreferredPlacesLanguage(app);
		boolean transliterate = app.getSettings().MAP_TRANSLITERATE_NAMES.get();
		SearchSettings settings = core.getSearchSettings().setLang(locale, transliterate);

		SearchPhrase phrase = SearchPhrase.emptyPhrase(settings);
		SearchResultCollection collection = new SearchResultCollection(phrase);

		List<SearchResult> results = new ArrayList<>();
		for (Amenity amenity : amenities) {
			SearchResult result = SearchCoreFactory.createSearchResult(amenity, phrase, core.getPoiTypes());
			results.add(result);
		}
		collection.addSearchResults(results, false, false);
		return collection;
	}

	@Override
	protected void onPostExecute(SearchResultCollection collection) {
		if (callback != null) {
			callback.processResult(collection);
		}
	}
}
