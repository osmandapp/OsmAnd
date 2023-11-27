package net.osmand.plus.track.helpers;

import static net.osmand.data.City.CityType.CITY;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXUtilities;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.AppInitializer.AppInitializeListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

class GpxReaderTask extends AsyncTask<Void, GpxDataItem, Void> {

	private final OsmandApplication app;
	private final GPXDatabase database;
	private final GpxDbHelper gpxDbHelper;

	private final ConcurrentLinkedQueue<File> readingItems;
	private final Map<File, GpxDataItem> readingItemsMap;
	private final GpxDbReaderCallback listener;

	private File file;


	public GpxReaderTask(@NonNull OsmandApplication app, @NonNull ConcurrentLinkedQueue<File> readingItems,
	                     @NonNull Map<File, GpxDataItem> readingItemsMap, @Nullable GpxDbReaderCallback listener) {
		this.app = app;
		this.gpxDbHelper = app.getGpxDbHelper();
		this.database = gpxDbHelper.getGPXDatabase();
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
					if (GpxDbHelper.isAnalyseNeeded(file, item)) {
						GPXFile gpxFile = GPXUtilities.loadGPXFile(file);
						GPXTrackAnalysis analysis = gpxFile.getAnalysis(file.lastModified());
						if (item == null) {
							item = new GpxDataItem(file);
							item.getGpxData().setAnalysis(analysis);
							database.insert(item, conn);
						} else {
							database.updateAnalysis(conn, item, analysis);
						}
						if (item.getGpxData().getFileCreationTime() <= 0) {
							database.updateCreateTime(item, GPXUtilities.getCreationTime(gpxFile));
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
					checkAndSearchNearestCity(item);
				}
			});
		} else {
			checkAndSearchNearestCity(item);
		}
	}

	private void checkAndSearchNearestCity(@NonNull GpxDataItem item) {
		GpxData data = item.getGpxData();
		GPXTrackAnalysis analysis = data.getAnalysis();
		LatLon latLon = analysis != null ? analysis.latLonStart : null;
		if (latLon == null) {
			data.setNearestCityName("");
		} else {
			searchNearestCity(item, latLon);
		}
	}

	private void searchNearestCity(@NonNull GpxDataItem item, @NonNull LatLon latLon) {
		QuadRect rect = MapUtils.calculateLatLonBbox(latLon.getLatitude(), latLon.getLongitude(), (int) CITY.getRadius());
		List<Amenity> cities = app.getResourceManager().searchAmenities(new SearchPoiTypeFilter() {
			@Override
			public boolean accept(PoiCategory type, String subcategory) {
				return Algorithms.equalsToAny(subcategory, "city", "town");
			}

			@Override
			public boolean isEmpty() {
				return false;
			}
		}, rect);

		if (!Algorithms.isEmpty(cities)) {
			sortAmenities(cities, latLon);
			Amenity city = cities.get(0);
			gpxDbHelper.updateNearestCityName(item, city.getName());
		} else {
			item.getGpxData().setNearestCityName("");
		}
	}

	private void sortAmenities(@NonNull List<Amenity> amenities, @NonNull LatLon latLon) {
		Collections.sort(amenities, (o1, o2) -> {
			double distance1 = MapUtils.getDistance(latLon, o1.getLocation());
			double distance2 = MapUtils.getDistance(latLon, o2.getLocation());
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

		void onGpxDataItemRead(@NonNull GpxDataItem item);

		void onProgressUpdate(@NonNull GpxDataItem... dataItems);

		void onReadingCancelled();

		void onReadingFinished(boolean cancelled);
	}
}
