package net.osmand.plus.exploreplaces;

import androidx.annotation.NonNull;

import com.squareup.picasso.Picasso;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.ObfConstants;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.NearbyPlacePoint;
import net.osmand.data.QuadRect;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.search.GetNearbyPlacesImagesTask;
import net.osmand.search.core.SearchCoreFactory;
import net.osmand.shared.data.KLatLon;
import net.osmand.shared.data.KQuadRect;
import net.osmand.shared.util.KMapUtils;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;
import net.osmand.util.MapUtils;
import net.osmand.wiki.WikiCoreHelper.OsmandApiFeatureData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ExplorePlacesProviderJava implements ExplorePlacesProvider {
	private OsmandApplication app;
	private long lastModifiedTime = 0;
	private static final int NEARBY_MIN_RADIUS = 50;

	private final int LEVEL_ZOOM_CACHE = 12; // Constant zoom level
	private final int DEFAULT_QUERY_RADIUS = 30000;
	private volatile int runningTasks = 0;

//	private final int DEFAULT_QUERY_RADIUS = 0;

	public ExplorePlacesProviderJava(OsmandApplication app) {
		this.app = app;
	}

	private List<ExplorePlacesListener> listeners = Collections.emptyList();

	public void addListener(ExplorePlacesListener listener) {
		if (!listeners.contains(listener)) {
			listeners = CollectionUtils.addToList(listeners, listener);
		}
	}

	public void removeListener(ExplorePlacesListener listener) {
		listeners = CollectionUtils.removeFromList(listeners, listener);
	}

	public void notifyListeners() {
		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
				for (ExplorePlacesListener listener: listeners) {
					listener.onNewExplorePlacesDownloaded();
				}
			}
		});
	}
	private String getLang() {
		String preferredLang = app.getSettings().MAP_PREFERRED_LOCALE.get();
		if (Algorithms.isEmpty(preferredLang)) {
			preferredLang = app.getLanguage();
		}
		return preferredLang;
	}

	public List<NearbyPlacePoint> getDataCollection(QuadRect rect) {
		return getDataCollection(rect, 1000);
	}
	public List<NearbyPlacePoint> getDataCollection(QuadRect rect, int limit) {
		if (rect == null) {
			return Collections.emptyList();
		}
		// Calculate tile bounds for the QuadRect and cast to int
		int zoom = LEVEL_ZOOM_CACHE;
		int minTileX = (int) MapUtils.getTileNumberX(zoom, rect.left);
		int maxTileX = (int) MapUtils.getTileNumberX(zoom, rect.right);
		int minTileY = (int) MapUtils.getTileNumberY(zoom, rect.top);
		int maxTileY = (int) MapUtils.getTileNumberY(zoom, rect.bottom);

		// Fetch data for all tiles within the bounds
		PlacesDatabaseHelper dbHelper = new PlacesDatabaseHelper(app);
		List<NearbyPlacePoint> filteredPoints = new ArrayList<>();
		Set<Long> uniqueIds = new HashSet<>(); // Use a Set to track unique IDs
		final String queryLang = getLang();
		for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
			for (int tileY = minTileY; tileY <= maxTileY; tileY++) {
				// Check if data is already present and not expired
				if (!dbHelper.isDataExpired(zoom, tileX, tileY, queryLang)) {
					List<OsmandApiFeatureData> places = dbHelper.getPlaces(LEVEL_ZOOM_CACHE, tileX, tileY, getLang());
					for (OsmandApiFeatureData item : places) {
						if (!Algorithms.isEmpty(item.properties.photoTitle)) {
							NearbyPlacePoint point = new NearbyPlacePoint(item);
							double lat = point.getLatitude();
							double lon = point.getLongitude();

							// Filter by QuadRect and check for duplicates using the ID
							if (rect.contains(lon, lat, lon, lat) && uniqueIds.add(point.getId())) {
								filteredPoints.add(point);
							}
						}
					}
					continue; // Skip downloading if data is present and not expired
				} else {
					loadTile(zoom, tileX, tileY, queryLang, dbHelper);
				}

			}
		}
		filteredPoints.sort((p1, p2) -> {
			return Double.compare(p2.getElo(), p1.getElo());  // Sort in descending order (highest elo first)
		});
		if (filteredPoints.size() > limit) {
			filteredPoints = filteredPoints.subList(0, limit);
		}
		return filteredPoints;
	}

	private void loadTile(int zoom, int tileX, int tileY, String queryLang, PlacesDatabaseHelper dbHelper) {
		// Calculate the bounding box for the current tile
		double left = MapUtils.getLongitudeFromTile(zoom, tileX);
		double right = MapUtils.getLongitudeFromTile(zoom, tileX + 1);
		double top = MapUtils.getLatitudeFromTile(zoom, tileY);
		double bottom = MapUtils.getLatitudeFromTile(zoom, tileY + 1);

		KQuadRect tileRect = new KQuadRect(left, top, right, bottom);
		// Increment the task counter
		synchronized (this) {
			runningTasks++;
		}
		// Create and execute a task for the current tile
		int ftileX = tileX;
		int ftileY = tileY;
		new GetNearbyPlacesImagesTask(
				app,
				tileRect, zoom,
				queryLang, new GetNearbyPlacesImagesTask.GetImageCardsListener() {
			@Override
			public void onTaskStarted() {
			}

			@Override
			public void onFinish(@NonNull List<? extends OsmandApiFeatureData> result) {
				synchronized (ExplorePlacesProviderJava.this) {
					runningTasks--; // Decrement the task counter
				}
				if (result != null) {
					// Store the data in the database for the current tile
					dbHelper.insertPlaces(zoom, ftileX, ftileY, queryLang, result);
				}
				notifyListeners();
			}
		}).execute();
	}

	public void showPointInContextMenu(MapActivity mapActivity, NearbyPlacePoint point) {
		double latitude = point.getLatitude();
		double longitude = point.getLongitude();
		app.getSettings().setMapLocationToShow(
				latitude,
				longitude,
				SearchCoreFactory.PREFERRED_NEARBY_POINT_ZOOM,
				point.getPointDescription(app),
				true,
				point);
		MapActivity.launchMapActivityMoveToTop(mapActivity);
	}

	public Amenity getAmenity(LatLon latLon, long osmId) {
		final Amenity[] foundAmenity = new Amenity[]{null};
		int radius = NEARBY_MIN_RADIUS;
		QuadRect rect = MapUtils.calculateLatLonBbox(latLon.getLatitude(), latLon.getLongitude(), radius);
		app.getResourceManager().searchAmenities(
				BinaryMapIndexReader.ACCEPT_ALL_POI_TYPE_FILTER,
				rect.top, rect.left, rect.bottom, rect.right,
				-1, true,
				new ResultMatcher<Amenity>() {
					@Override
					public boolean publish(Amenity amenity) {
						long id = ObfConstants.getOsmObjectId(amenity);
						if (osmId == id) {
							foundAmenity[0] = amenity;
							return true;
						}
						return false;
					}

					@Override
					public boolean isCancelled() {
						return foundAmenity[0] != null;
					}
				});
		return foundAmenity[0];
	}

	@Override
	public boolean isLoading() {
		return runningTasks > 0; // Return true if any task is running
	}

}