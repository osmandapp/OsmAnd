package net.osmand.plus.exploreplaces;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.plus.search.listitems.QuickSearchWikiItem;
import net.osmand.search.SearchUICore;
import net.osmand.search.core.SearchCoreFactory;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchResult;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class ConvertAmenitiesTask extends AsyncTask<Void, Void, List<QuickSearchListItem>> {

	private static final Log log = PlatformUtil.getLog(ConvertAmenitiesTask.class.getName());

	private final OsmandApplication app;
	private final List<Amenity> amenities;
	private final boolean topImagesFilter;
	private final CallbackWithObject<List<QuickSearchListItem>> callback;
	private OsmAndLocationProvider locationProvider;

	public ConvertAmenitiesTask(@NonNull OsmandApplication app, @NonNull List<Amenity> amenities,
	                            boolean topImagesFilter,
	                            @NonNull OsmAndLocationProvider locationProvider,
	                            @Nullable CallbackWithObject<List<QuickSearchListItem>> callback) {
		this.app = app;
		this.amenities = new ArrayList<>(amenities);
		this.topImagesFilter = topImagesFilter;
		this.callback = callback;
		this.locationProvider = locationProvider;
	}

	@Override
	protected List<QuickSearchListItem> doInBackground(Void... params) {
		SearchUICore core = app.getSearchUICore().getCore();
		SearchPhrase phrase = SearchPhrase.emptyPhrase(core.getSearchSettings());

		if (topImagesFilter) {
			amenities.sort((o1, o2) ->
					Integer.compare(o2.getTravelEloNumber(), o1.getTravelEloNumber()));
		} else {
			Location location = locationProvider.getLastStaleKnownLocation();
			if (location != null) {
				MapUtils.sortListOfMapObject(amenities, location.getLatitude(), location.getLongitude());
			}
		}
		List<QuickSearchListItem> items = new ArrayList<>();
		for (Amenity amenity : amenities) {
			if(isCancelled()){
				break;
			}
			SearchResult result = SearchCoreFactory.createSearchResult(amenity, phrase, core.getPoiTypes());
			if (topImagesFilter) {
				items.add(new QuickSearchWikiItem(app, result));
			} else {
				items.add(new QuickSearchListItem(app, result));
			}
		}
		return items;
	}

	@Override
	protected void onPostExecute(List<QuickSearchListItem> items) {
		if (callback != null) {
			callback.processResult(items);
		}
	}
}