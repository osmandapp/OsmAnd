package net.osmand.plus.exploreplaces;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.ObfConstants;
import net.osmand.data.Amenity;
import net.osmand.data.ExploreTopPlacePoint;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.search.GetNearbyPlacesImagesTask;
import net.osmand.search.core.SearchCoreFactory;
import net.osmand.shared.data.KQuadRect;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;
import net.osmand.util.MapUtils;
import net.osmand.wiki.WikiCoreHelper.OsmandApiFeatureData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

// TODO use gzip in loading
// TODO errors shouldn'go with empty response "" into cache!
// TODO remove checks poi type subtype null
// TODO display all data downloaded even if maps are not loaded
// TODO: why recreate provider when new points are loaded? that causes blinking
// TODO: scheduleImageRefreshes in layer is incorrect it starts downloading all images and stops interacting
// TODO images shouldn't be queried if they are not visible in all lists! size doesn't matter !
// TODO show on map close button is not visible
// TODO layer sometimes becomes non-interactive - MAP FPS drops
// TODO Context menu doesn't work correctly and duplicates actual POI
// TODO compass is not rotating
// Extra: display new categories from web
public class ExplorePlacesProviderJava implements ExplorePlacesProvider {

	public static final int DEFAULT_LIMIT_POINTS = 200;
	private static final int NEARBY_MIN_RADIUS = 50;

	private static final int MAX_TILES_PER_QUAD_RECT = 12;
	private static final int MAX_TILES_PER_CACHE = MAX_TILES_PER_QUAD_RECT * 2;
	private static final double LOAD_ALL_TINY_RECT = 0.5;

	private final OsmandApplication app;
	private volatile int startedTasks = 0;
	private volatile int finishedTasks = 0;

	private final Map<TileKey, QuadRect> loadingTiles = new HashMap<>(); // Track tiles being loaded
	private final Map<TileKey, List<ExploreTopPlacePoint>> tilesCache = new HashMap<>(); // Memory cache for recent tiles

	private static class TileKey {
		int zoom;
		int tileX;
		int tileY;

		public TileKey(int zoom, int tileX, int tileY) {
			this.zoom = zoom;
			this.tileX = tileX;
			this.tileY = tileY;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof TileKey tileKey)) {
				return false;
			}
            return zoom == tileKey.zoom && tileX == tileKey.tileX && tileY == tileKey.tileY;
		}

		@Override
		public int hashCode() {
			return Objects.hash(zoom, tileX, tileY);
		}
	}

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

	/**
	 * Notify listeners about new data being downloaded.
	 *
	 * @param isPartial Whether the notification is for a partial update or a full update.
	 */
	public void notifyListeners(boolean isPartial) {
		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
				for (ExplorePlacesListener listener : listeners) {
					if (isPartial) {
						listener.onPartialExplorePlacesDownloaded(); // Notify for partial updates
					} else {
						listener.onNewExplorePlacesDownloaded(); // Notify for full updates
					}
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

	@NonNull
	public List<ExploreTopPlacePoint> getDataCollection(QuadRect rect) {
		return getDataCollection(rect, DEFAULT_LIMIT_POINTS);
	}

	@NonNull
    public List<ExploreTopPlacePoint> getDataCollection(QuadRect rect, int limit) {
		if (rect == null) {
			return Collections.emptyList();
		}
		// Calculate the initial zoom level
		int zoom = MAX_LEVEL_ZOOM_CACHE;
		while (zoom >= 0) {
			int tileWidth = (int) (MapUtils.getTileNumberX(zoom, rect.right)) -
					((int) MapUtils.getTileNumberX(zoom, rect.left)) + 1;
			int tileHeight = (int) (MapUtils.getTileNumberY(zoom, rect.bottom)) -
					((int) MapUtils.getTileNumberY(zoom, rect.top)) + 1;
			if (tileWidth * tileHeight <= MAX_TILES_PER_QUAD_RECT) {
				break;
			}
			zoom -= 3;
		}
		zoom = Math.max(zoom, 1);
		// Calculate tile bounds for the QuadRect as float values
		float minTileX = (float) MapUtils.getTileNumberX(zoom, rect.left);
		float maxTileX = (float) MapUtils.getTileNumberX(zoom, rect.right);
		float minTileY = (float) MapUtils.getTileNumberY(zoom, rect.top);
		float maxTileY = (float) MapUtils.getTileNumberY(zoom, rect.bottom);
		boolean loadAll = zoom == MAX_LEVEL_ZOOM_CACHE &&
				Math.abs(maxTileX - minTileX) <= LOAD_ALL_TINY_RECT || Math.abs(maxTileY - minTileY) <= LOAD_ALL_TINY_RECT;

		// Fetch data for all tiles within the bounds
		PlacesDatabaseHelper dbHelper = new PlacesDatabaseHelper(app);
		List<ExploreTopPlacePoint> filteredPoints = new ArrayList<>();
		Set<Long> uniqueIds = new HashSet<>(); // Use a Set to track unique IDs
		final String queryLang = getLang();

		// Iterate over the tiles and load data
		for (int tileX = (int) minTileX; tileX <= (int) maxTileX; tileX++) {
			for (int tileY = (int) minTileY; tileY <= (int) maxTileY; tileY++) {
				if (!dbHelper.isDataExpired(zoom, tileX, tileY, queryLang)) {
					TileKey tileKey = new TileKey(zoom, tileX, tileY);
					List<ExploreTopPlacePoint> cachedPlaces = tilesCache.get(tileKey);
					if (cachedPlaces != null) {
						for (ExploreTopPlacePoint place : cachedPlaces) {
							double lat = place.getLatitude();
							double lon = place.getLongitude();
							if ((rect.contains(lon, lat, lon, lat) || loadAll) && uniqueIds.add(place.getId())) {
								filteredPoints.add(place);
							}
						}
					} else {
						List<OsmandApiFeatureData> places = dbHelper.getPlaces(zoom, tileX, tileY, queryLang);
						cachedPlaces = new ArrayList<>();
						for (OsmandApiFeatureData item : places) {
							// TODO remove checks poi type subtype null
							if (Algorithms.isEmpty(item.properties.photoTitle)
									|| item.properties.poitype == null || item.properties.poisubtype == null) {
								continue;
							}
							ExploreTopPlacePoint point = new ExploreTopPlacePoint(item);
							double lat = point.getLatitude();
							double lon = point.getLongitude();
							if ((rect.contains(lon, lat, lon, lat) || loadAll) && uniqueIds.add(point.getId())) {
								filteredPoints.add(point);
							}
							cachedPlaces.add(point);
						}
						tilesCache.put(tileKey, cachedPlaces);
					}
				} else {
					loadTile(zoom, tileX, tileY, queryLang, dbHelper);
				}
			}
		}
		clearCache(zoom, (int) minTileX, (int) maxTileX, (int) minTileY, (int) maxTileY);

		// Sort the points by Elo in descending order
		filteredPoints.sort((p1, p2) -> Double.compare(p2.getElo(), p1.getElo()));

		// Limit the number of points
		if (filteredPoints.size() > limit) {
			filteredPoints = filteredPoints.subList(0, limit);
		}

		return filteredPoints;
	}

	private void clearCache(int zoom, int minTileX, int maxTileX, int minTileY, int maxTileY) {
		Iterator<Entry<TileKey, List<ExploreTopPlacePoint>>> iterator = tilesCache.entrySet().iterator();
		while (iterator.hasNext()) {
			TileKey key = iterator.next().getKey();
			if (key.zoom != zoom) {
				iterator.remove();
			}
		}
		int numTiles = tilesCache.size();
		if (numTiles > MAX_TILES_PER_CACHE) {
			List<TileKey> currentZoomKeys = new ArrayList<>(tilesCache.keySet());
			currentZoomKeys.sort(Comparator.comparingInt(key -> {
				int dx = Math.max(0, Math.max(minTileX - key.tileX, key.tileX - maxTileX));
				int dy = Math.max(0, Math.max(minTileY - key.tileY, key.tileY - maxTileY));
				return dx + dy;
			}));
			for (int i = MAX_TILES_PER_CACHE; i < numTiles; i++) {
				TileKey keyToRemove = currentZoomKeys.get(i);
				tilesCache.remove(keyToRemove);
			}
		}
	}

	@SuppressLint("DefaultLocale")
	private void loadTile(int zoom, int tileX, int tileY, String queryLang, PlacesDatabaseHelper dbHelper) {
		double left;
		double right;
		double top;
		double bottom;

		TileKey tileKey = new TileKey(zoom, tileX, tileY);
		synchronized (loadingTiles) {
			if (loadingTiles.containsKey(tileKey)) {
				return;
			}
			left = MapUtils.getLongitudeFromTile(zoom, tileX);
			right = MapUtils.getLongitudeFromTile(zoom, tileX + 1);
			top = MapUtils.getLatitudeFromTile(zoom, tileY);
			bottom = MapUtils.getLatitudeFromTile(zoom, tileY + 1);
			loadingTiles.put(tileKey, new QuadRect(left, top, right, bottom));
		}

		KQuadRect tileRect = new KQuadRect(left, top, right, bottom);
		// Increment the task counter
		synchronized (this) {
			startedTasks++;
		}
		// Create and execute a task for the current tile
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
					finishedTasks++; // Increment the finished task counter
					notifyListeners(startedTasks != finishedTasks);
				}
                // Store the data in the database for the current tile
                dbHelper.insertPlaces(zoom, tileX, tileY, queryLang, result);
                // Remove the tile from the loading set
				synchronized (loadingTiles) {
					loadingTiles.remove(tileKey);
				}
			}
		}).execute();
	}

	public void showPointInContextMenu(@NonNull MapActivity mapActivity, @NonNull ExploreTopPlacePoint point) {
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
		return startedTasks > finishedTasks; // Return true if any task is running
	}

	@Override
	public int getDataVersion() {
		// data version is increased once new data is downloaded
		return finishedTasks;
	}

	public boolean isLoadingRect(@NonNull QuadRect rect) {
		synchronized (loadingTiles) {
			for (QuadRect loadingRect : loadingTiles.values()) {
				if (loadingRect.contains(rect)) {
					return true;
				}
			}
			return false;
		}
	}
}