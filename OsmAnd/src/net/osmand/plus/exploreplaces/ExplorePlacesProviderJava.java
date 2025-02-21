package net.osmand.plus.exploreplaces;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.ObfConstants;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.ExploreTopPlacePoint;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// TODO use gzip in loading
// TODO errors shouldn'go with empty response "" into cache!
// TODO remove checks poi type subtype null
// TODO display all data downloaded even if maps are not loaded
// TODO: why recreate provider when new points are loaded? that causes blinking
// Extra: display new categories from web
public class ExplorePlacesProviderJava implements ExplorePlacesProvider {

	private static final int DEFAULT_LIMIT_POINTS = 200;
	private static final int NEARBY_MIN_RADIUS = 50;


	private static final int MAX_TILES_PER_QUAD_RECT = 12;
	private static final double LOAD_ALL_TINY_RECT = 0.5;

	private OsmandApplication app;
	private volatile int startedTasks = 0;
	private volatile int finishedTasks = 0;

	private final Set<String> loadingTiles = new HashSet<>(); // Track tiles being loaded

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

	public List<ExploreTopPlacePoint> getDataCollection(QuadRect rect) {
		return getDataCollection(rect, DEFAULT_LIMIT_POINTS);
	}

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
					List<OsmandApiFeatureData> places = dbHelper.getPlaces(zoom, tileX, tileY, queryLang);
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
					}
				} else {
					loadTile(zoom, tileX, tileY, queryLang, dbHelper);
				}
			}
		}

		// Sort the points by Elo in descending order
		filteredPoints.sort((p1, p2) -> Double.compare(p2.getElo(), p1.getElo()));

		// Limit the number of points
		if (filteredPoints.size() > limit) {
			filteredPoints = filteredPoints.subList(0, limit);
		}

		return filteredPoints;
	}

	@SuppressLint("DefaultLocale")
	private void loadTile(int zoom, int tileX, int tileY, String queryLang, PlacesDatabaseHelper dbHelper) {
		synchronized (loadingTiles) {
			String tileKey = zoom + "_" + tileX + "_" + tileY;
			if (loadingTiles.contains(tileKey)) {
				return;
			}
			loadingTiles.add(tileKey);
		}
		double left = MapUtils.getLongitudeFromTile(zoom, tileX);
		double right = MapUtils.getLongitudeFromTile(zoom, tileX + 1);
		double top = MapUtils.getLatitudeFromTile(zoom, tileY);
		double bottom = MapUtils.getLatitudeFromTile(zoom, tileY + 1);

		KQuadRect tileRect = new KQuadRect(left, top, right, bottom);
		// Increment the task counter
		synchronized (this) {
			startedTasks++;
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
					finishedTasks++; // Increment the finished task counter
					notifyListeners(startedTasks != finishedTasks);
				}
				if (result != null) {
					// Store the data in the database for the current tile
					dbHelper.insertPlaces(zoom, ftileX, ftileY, queryLang, result);
				}
				// Remove the tile from the loading set
				String tileKey = zoom + "_" + ftileX + "_" + ftileY;
				synchronized (loadingTiles) {
					loadingTiles.remove(tileKey);
				}
			}
		}).execute();
	}

	public void showPointInContextMenu(MapActivity mapActivity, ExploreTopPlacePoint point) {
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
}