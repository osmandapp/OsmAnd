package net.osmand.plus.track.helpers;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.data.City.CityType;
import net.osmand.data.LatLon;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXUtilities;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.AppInitializer.AppInitializeListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.search.SearchUICore;
import net.osmand.search.SearchUICore.SearchResultCollection;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchCoreFactory.SearchAddressByNameAPI;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchSettings;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

class GpxReaderTask extends AsyncTask<Void, GpxDataItem, Void> {

	private final OsmandApplication app;
	private final GPXDatabase database;
	private final GpxDbHelper gpxDbHelper;
	private final SearchUICore searchUICore;

	private final ConcurrentLinkedQueue<File> readingItems;
	private final Map<File, GpxDataItem> readingItemsMap;
	private final GpxDbReaderCallback listener;

	private File file;
	private SearchSettings searchSettings;


	public GpxReaderTask(@NonNull OsmandApplication app, @NonNull ConcurrentLinkedQueue<File> readingItems,
	                     @NonNull Map<File, GpxDataItem> readingItemsMap, @Nullable GpxDbReaderCallback listener) {
		this.app = app;
		this.gpxDbHelper = app.getGpxDbHelper();
		this.database = gpxDbHelper.getGPXDatabase();
		this.searchUICore = app.getSearchUICore().getCore();
		this.readingItems = readingItems;
		this.readingItemsMap = readingItemsMap;
		this.listener = listener;
	}

	@Nullable
	public File getFile() {
		return file;
	}

	public boolean isReading() {
		return !Algorithms.isEmpty(readingItems) || file != null;
	}

	@Override
	protected Void doInBackground(Void... voids) {
		SQLiteConnection conn = database.openConnection(false);
		if (conn != null) {
			try {
				file = readingItems.poll();
				while (file != null && !isCancelled()) {
					GpxDataItem item = readingItemsMap.remove(file);
					if (item != null && item.getFile() == null) {
						item = database.getItem(file, conn);
					}
					if (GpxDbHelper.isAnalyseNeeded(file, item)) {
						GPXFile gpxFile = GPXUtilities.loadGPXFile(file);
						GPXTrackAnalysis analysis = gpxFile.getAnalysis(file.lastModified());
						if (item == null || item.getFile() == null) {
							item = new GpxDataItem(file, analysis);
							database.insert(item, conn);
						} else {
							database.updateAnalysis(item, analysis, conn);
						}
					}
					if (GpxDbHelper.isCitySearchNeeded(item)) {
						setupNearestCityName(item);
					}
					if (listener != null) {
						listener.onGpxDataItemRead(item);
					}
					if (!isCancelled()) {
						publishProgress(item);
					}
					file = readingItems.poll();
				}
			} finally {
				conn.close();
			}
		} else {
			cancel(false);
		}
		return null;
	}

	private void setupNearestCityName(@NonNull GpxDataItem item) {
		if (app.isApplicationInitializing()) {
			app.getAppInitializer().addListener(new AppInitializeListener() {
				@Override
				public void onFinish(@NonNull AppInitializer init) {
					searchNearestCity(item);
				}
			});
		} else {
			searchNearestCity(item);
		}
	}

	private void searchNearestCity(@NonNull GpxDataItem item) {
		GPXTrackAnalysis analysis = item.getAnalysis();
		LatLon latLon = analysis != null ? analysis.latLonStart : null;
		if (latLon == null) {
			item.setNearestCityName("");
			return;
		}
		SearchSettings settings = getSearchSettings(latLon);

		CallbackWithObject<SearchResultCollection> callback = resultCollection -> {
			if (resultCollection != null && resultCollection.hasSearchResults()) {
				List<SearchResult> searchResults = new ArrayList<>(resultCollection.getCurrentSearchResults());
				sortSearchResults(searchResults, latLon);

				SearchResult searchResult = searchResults.get(0);
				double distance = MapUtils.getDistance(latLon, searchResult.location);
				if (distance <= CityType.CITY.getRadius()) {
					gpxDbHelper.updateNearestCityName(item, searchResult.localeName);
					return true;
				}
			}
			item.setNearestCityName("");
			return false;
		};
		searchUICore.shallowSearchAsync(SearchAddressByNameAPI.class, "", null, false, false, settings, callback);
	}

	@NonNull
	private SearchSettings getSearchSettings(@NonNull LatLon latLon) {
		if (searchSettings == null) {
			searchSettings = searchUICore.getSearchSettings()
					.setEmptyQueryAllowed(true)
					.setSortByName(false)
					.setSearchTypes(ObjectType.CITY)
					.setRadiusLevel(1);
		}
		return searchSettings.setOriginalLocation(latLon);
	}

	private void sortSearchResults(@NonNull List<SearchResult> results, @NonNull LatLon latLon) {
		Collections.sort(results, (o1, o2) -> {
			double distance1 = MapUtils.getDistance(latLon, o1.location);
			double distance2 = MapUtils.getDistance(latLon, o2.location);
			return Double.compare(distance1, distance2);
		});
	}

	@Override
	protected void onProgressUpdate(GpxDataItem... dataItems) {
		if (listener != null) {
			listener.onProgressUpdate(dataItems);
		}
	}

	@Override
	protected void onCancelled(Void result) {
		if (listener != null) {
			listener.onReadingCancelled();
		}
	}

	@Override
	protected void onPostExecute(Void result) {
		if (listener != null) {
			listener.onReadingFinished(isCancelled());
		}
	}

	interface GpxDbReaderCallback {

		@NonNull
		GPXDatabase getGPXDatabase();

		void onGpxDataItemRead(@NonNull GpxDataItem item);

		void onProgressUpdate(@NonNull GpxDataItem... dataItems);

		void onReadingCancelled();

		void onReadingFinished(boolean cancelled);
	}
}
