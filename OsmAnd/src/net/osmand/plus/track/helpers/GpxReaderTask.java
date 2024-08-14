package net.osmand.plus.track.helpers;

import static net.osmand.gpx.GPXTrackAnalysis.ANALYSIS_VERSION;
import static net.osmand.gpx.GpxParameter.ADDITIONAL_EXAGGERATION;
import static net.osmand.gpx.GpxParameter.DATA_VERSION;
import static net.osmand.gpx.GpxParameter.FILE_CREATION_TIME;
import static net.osmand.gpx.GpxParameter.FILE_LAST_MODIFIED_TIME;
import static net.osmand.gpx.GpxParameter.NEAREST_CITY_NAME;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.data.Amenity;
import net.osmand.data.City;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXTrackAnalysis.TrackPointsAnalyser;
import net.osmand.gpx.GPXUtilities;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.AppInitializeListener;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.srtm.SRTMPlugin;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

class GpxReaderTask extends AsyncTask<Void, GpxDataItem, Void> {

	private static final Log LOG = PlatformUtil.getLog(GpxReaderTask.class);

	private static final int CITY_SEARCH_RADIUS = 50 * 1000;

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
				boolean isApplicationInitializing = app.isApplicationInitializing();
				file = readingItems.poll();
				while (file != null && !isCancelled()) {
					GpxDataItem item = readingItemsMap.remove(file);
					if (GpxDbUtils.isAnalyseNeeded(item) && !isApplicationInitializing) {
						item = updateGpxDataItem(conn, item);
					}
					if (listener != null) {
						listener.onGpxDataItemRead(item);
					}
					if (!isCancelled()) {
						publishProgress(item);
					}
					file = readingItems.poll();
				}
			} catch (Exception e) {
				LOG.error(e.getMessage());
			} finally {
				conn.close();
			}
		} else {
			cancel(false);
		}
		return null;
	}

	@NonNull
	private GpxDataItem updateGpxDataItem(@NonNull SQLiteConnection conn, @Nullable GpxDataItem item) {
		GPXFile gpxFile = GPXUtilities.loadGPXFile(file);
		if (item == null) {
			item = new GpxDataItem(app, file);
			database.insertItem(item, conn);
		}
		if (gpxFile.error == null) {
			TrackPointsAnalyser analyser = PluginsHelper.getTrackPointsAnalyser();
			item.setAnalysis(gpxFile.getAnalysis(file.lastModified(), null, null, analyser));

			long creationTime = item.requireParameter(FILE_CREATION_TIME);
			if (creationTime <= 0) {
				item.setParameter(FILE_CREATION_TIME, GPXUtilities.getCreationTime(gpxFile));
			}
			item.setParameter(FILE_LAST_MODIFIED_TIME, file.lastModified());

			setupNearestCityName(item);
			double additionalExaggeration = item.requireParameter(ADDITIONAL_EXAGGERATION);
			if (additionalExaggeration < SRTMPlugin.MIN_VERTICAL_EXAGGERATION
					|| additionalExaggeration > SRTMPlugin.MAX_VERTICAL_EXAGGERATION) {
				item.setParameter(ADDITIONAL_EXAGGERATION, (double) SRTMPlugin.MIN_VERTICAL_EXAGGERATION);
			}
			item.setParameter(DATA_VERSION, GpxDbUtils.createDataVersion(ANALYSIS_VERSION));

			if (database.getDataItem(file, conn) != null) {
				gpxDbHelper.updateDataItem(item);
			} else {
				database.insertItem(item, conn);
			}
		}
		return item;
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
		GPXTrackAnalysis analysis = item.getAnalysis();
		LatLon latLon = analysis != null ? analysis.getLatLonStart() : null;
		if (latLon == null) {
			item.setParameter(NEAREST_CITY_NAME, "");
		} else {
			searchNearestCity(item, latLon);
		}
	}

	private void searchNearestCity(@NonNull GpxDataItem item, @NonNull LatLon latLon) {
		Map<String, City.CityType> cityTypes = new LinkedHashMap<>();
		QuadRect rect = MapUtils.calculateLatLonBbox(latLon.getLatitude(), latLon.getLongitude(), CITY_SEARCH_RADIUS);
		for (City.CityType t : City.CityType.values()) {
			cityTypes.put(t.name().toLowerCase(Locale.ROOT), t);
		}
		List<Amenity> cities = app.getResourceManager().searchAmenities(new SearchPoiTypeFilter() {
			@Override
			public boolean accept(PoiCategory type, String subcategory) {
				return cityTypes.containsKey(subcategory);
			}

			@Override
			public boolean isEmpty() {
				return false;
			}
		}, rect, false);

		if (!Algorithms.isEmpty(cities)) {
			sortAmenities(cities, cityTypes, latLon);
			Amenity city = cities.get(0);
			item.setParameter(NEAREST_CITY_NAME, city.getName());
			gpxDbHelper.updateDataItem(item);
		} else {
			item.setParameter(NEAREST_CITY_NAME, "");
		}
	}

	private void sortAmenities(@NonNull List<Amenity> amenities, Map<String, City.CityType> cityTypes, @NonNull LatLon latLon) {
		Collections.sort(amenities, (o1, o2) -> {
			double rad1 = 1000, rad2 = 1000;
			if (cityTypes.containsKey(o1.getSubType())) {
				rad1 = cityTypes.get(o1.getSubType()).getRadius();
			}
			if (cityTypes.containsKey(o2.getSubType())) {
				rad2 = cityTypes.get(o2.getSubType()).getRadius();
			}
			double distance1 = MapUtils.getDistance(latLon, o1.getLocation()) / rad1;
			double distance2 = MapUtils.getDistance(latLon, o2.getLocation()) / rad2;
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
