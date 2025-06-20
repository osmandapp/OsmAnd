package net.osmand.plus.exploreplaces;

import static net.osmand.data.Amenity.DEFAULT_ELO;
import static net.osmand.data.Amenity.WIKIDATA;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import static net.osmand.binary.ObfConstants.createMapObjectIdFromOsmId;
import net.osmand.data.Amenity;
import net.osmand.data.QuadRect;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.edit.Entity;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.search.GetExplorePlacesImagesTask;
import net.osmand.plus.search.GetExplorePlacesImagesTask.GetImageCardsListener;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.wikipedia.WikipediaPlugin;
import net.osmand.shared.KAsyncTask;
import net.osmand.shared.data.KQuadRect;
import net.osmand.shared.wiki.WikiHelper;
import net.osmand.shared.wiki.WikiImage;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;
import net.osmand.util.MapUtils;
import net.osmand.util.TransliterationHelper;
import net.osmand.wiki.WikiCoreHelper.OsmandApiFeatureData;
import net.osmand.wiki.WikiCoreHelper.WikiDataProperties;

import org.apache.commons.logging.Log;

import java.util.*;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

// Extra: display new categories from web

public class ExplorePlacesOnlineProvider implements ExplorePlacesProvider {

	private static final Log LOG = PlatformUtil.getLog(ExplorePlacesOnlineProvider.class);

	public static final int DEFAULT_LIMIT_POINTS = 200;
	private static final int NEARBY_MIN_RADIUS = 50;

	private static final int MAX_TILES_PER_QUAD_RECT = 12;
	private static final int MAX_TILES_PER_CACHE = MAX_TILES_PER_QUAD_RECT * 2;
	private static final double LOAD_ALL_TINY_RECT = 0.5;

	private final OsmandApplication app;
	private final PlacesDatabaseHelper dbHelper;

	private final Map<TileKey, GetExplorePlacesImagesTask> loadingTasks = new HashMap<>();
	private final Map<TileKey, List<Amenity>> tilesCache = new HashMap<>(); // Memory cache for recent tiles


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

	public ExplorePlacesOnlineProvider(OsmandApplication app) {
		this.app = app;
		this.dbHelper = new PlacesDatabaseHelper(app);
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
		app.runInUIThread(() -> {
			for (ExplorePlacesListener listener : listeners) {
				if (isPartial) {
					listener.onPartialExplorePlacesDownloaded(); // Notify for partial updates
				} else {
					listener.onNewExplorePlacesDownloaded(); // Notify for full updates
				}
			}
		});
	}

	@NonNull
	private List<String> getPreferredLangs() {
		String preferredLang = app.getSettings().MAP_PREFERRED_LOCALE.get();
		Set<String> languages = new LinkedHashSet<>();
		WikipediaPlugin plugin = PluginsHelper.requirePlugin(WikipediaPlugin.class);
		if (plugin.hasCustomSettings()) {
			List<String> langs = plugin.getLanguagesToShow();
			if (langs.contains(preferredLang)) {
				languages.add(preferredLang);
			}
			languages.addAll(langs);
		}
		return new ArrayList<>(languages);
	}

	@NonNull
	public List<Amenity> getDataCollection(QuadRect rect) {
		return getDataCollection(rect, DEFAULT_LIMIT_POINTS);
	}

	@NonNull
	public List<Amenity> getDataCollection(QuadRect rect, int limit) {
		synchronized (loadingTasks) {
			if (rect == null) {
				loadingTasks.values().removeIf(KAsyncTask::cancel);
				return Collections.emptyList();
			}
			KQuadRect kRect = SharedUtil.kQuadRect(rect);
			loadingTasks.values().removeIf(task -> (!task.isRunning()
					|| !kRect.contains(task.getMapRect()) && !KQuadRect.Companion.intersects(kRect, task.getMapRect())) && task.cancel());
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
		List<Amenity> filteredAmenities = new ArrayList<>();
		Set<Long> uniqueIds = new HashSet<>(); // Use a Set to track unique IDs
		List<String> languages = getPreferredLangs();

		// Iterate over the tiles and load data
		for (int tileX = (int) minTileX; tileX <= (int) maxTileX; tileX++) {
			for (int tileY = (int) minTileY; tileY <= (int) maxTileY; tileY++) {
				if (!dbHelper.isDataExpired(zoom, tileX, tileY, languages)) {
					TileKey tileKey = new TileKey(zoom, tileX, tileY);
					List<Amenity> cachedPlaces = tilesCache.get(tileKey);
					if (cachedPlaces != null) {
						for (Amenity amenity : cachedPlaces) {
							filterAmenity(amenity, filteredAmenities, rect, uniqueIds, loadAll);
						}
					} else {
						List<OsmandApiFeatureData> places = dbHelper.getPlaces(zoom, tileX, tileY, languages);
						cachedPlaces = new ArrayList<>();
						for (OsmandApiFeatureData item : places) {
							Amenity amenity = createAmenity(item);
							if (amenity != null) {
								filterAmenity(amenity, filteredAmenities, rect, uniqueIds, loadAll);
								cachedPlaces.add(amenity);
							}
						}
						tilesCache.put(tileKey, cachedPlaces);
					}
				} else {
					loadTile(zoom, tileX, tileY, languages);
				}
			}
		}
		clearCache(zoom, (int) minTileX, (int) maxTileX, (int) minTileY, (int) maxTileY);

		// Sort the points by Elo in descending order
		filteredAmenities.sort((a1, a2) -> Integer.compare(a2.getTravelEloNumber(), a1.getTravelEloNumber()));

		// Limit the number of points
		if (limit > 0 && filteredAmenities.size() > limit) {
			filteredAmenities = filteredAmenities.subList(0, limit);
		}

		return filteredAmenities;
	}

	private void filterAmenity(@NonNull Amenity amenity, @NonNull List<Amenity> filteredAmenities,
			@NonNull QuadRect rect, @NonNull Set<Long> uniqueIds, boolean loadAll) {
		double lat = amenity.getLocation().getLatitude();
		double lon = amenity.getLocation().getLongitude();
		if ((rect.contains(lon, lat, lon, lat) || loadAll) && uniqueIds.add(amenity.getId())) {
			filteredAmenities.add(amenity);
		}
	}

	private void clearCache(int zoom, int minTileX, int maxTileX, int minTileY, int maxTileY) {
		Iterator<Entry<TileKey, List<Amenity>>> iterator = tilesCache.entrySet().iterator();
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

	@Nullable
	private Amenity createAmenity(@NonNull OsmandApiFeatureData featureData) {
		Amenity amenity = new Amenity();
		WikiDataProperties properties = featureData.properties;

		amenity.setAdditionalInfo(WIKIDATA, app.getString(R.string.wikidata_id_pattern, properties.id));
		amenity.setName(properties.wikiTitle);
		amenity.setEnName(TransliterationHelper.transliterate(amenity.getName()));
		amenity.setDescription(properties.wikiDesc);

		if (!Algorithms.isEmpty(properties.wikiLangs)) {
			amenity.updateContentLocales(Set.of(properties.wikiLangs.split(",")));
		}

		if (!Algorithms.isEmpty(properties.photoTitle)) {
			WikiImage imageData = WikiHelper.INSTANCE.getImageData(properties.photoTitle);
			amenity.setWikiPhoto(imageData.getImageHiResUrl());
			amenity.setWikiIconUrl(imageData.getImageIconUrl());
			amenity.setWikiImageStubUrl(imageData.getImageStubUrl());
		}
		amenity.setLocation(featureData.geometry.coordinates[1], featureData.geometry.coordinates[0]);

		String poitype = properties.poitype;
		String subtype = properties.poisubtype;
		PoiCategory wikiCategory = app.getPoiTypes().getPoiCategoryByName("osmwiki");
		PoiCategory category = Algorithms.isEmpty(poitype) ? null : app.getPoiTypes().getPoiCategoryByName(poitype);
		if (Algorithms.isEmpty(subtype) || category == null) {
			category = wikiCategory;
			subtype = "wikiplace";
		}
		if (category == null) {
			return null;
		}
		amenity.setType(category);
		amenity.setSubType(subtype);
		if (properties.osmid > 0) {
			amenity.setId(createMapObjectIdFromOsmId(properties.osmid, Entity.EntityType.valueOf(properties.osmtype)));
		} else {
			amenity.setId(-Long.parseLong(properties.id));
		}
		//amenity.setTravelTopic(properties.wikiTitle);
		//amenity.setWikiCategory(properties.wikiDesc);
		amenity.setTravelEloNumber(properties.elo != null ? properties.elo.intValue() : DEFAULT_ELO);
		return amenity;
	}

	@SuppressLint("DefaultLocale")
	private void loadTile(int zoom, int tileX, int tileY, @NonNull List<String> languages) {
		double left;
		double right;
		double top;
		double bottom;

		TileKey tileKey = new TileKey(zoom, tileX, tileY);
		synchronized (loadingTasks) {
			if (loadingTasks.containsKey(tileKey)) {
				return;
			}
			left = MapUtils.getLongitudeFromTile(zoom, tileX);
			right = MapUtils.getLongitudeFromTile(zoom, tileX + 1);
			top = MapUtils.getLatitudeFromTile(zoom, tileY);
			bottom = MapUtils.getLatitudeFromTile(zoom, tileY + 1);
		}

		KQuadRect tileRect = new KQuadRect(left, top, right, bottom);
		synchronized (loadingTasks) {
			GetExplorePlacesImagesTask task = new GetExplorePlacesImagesTask(app, tileRect, zoom,
					languages, new GetImageCardsListener() {
				@Override
				public void onTaskStarted() {
				}

				@Override
				public void onFinish(@Nullable List<? extends OsmandApiFeatureData> result) {
					synchronized (ExplorePlacesOnlineProvider.this) {
						notifyListeners(isLoading());
					}
					if (result != null) {
						Map<String, List<OsmandApiFeatureData>> map = new HashMap<>();
						for (OsmandApiFeatureData data : result) {
							List<OsmandApiFeatureData> list = map.computeIfAbsent(
									data.properties.wikiLang, k -> new ArrayList<>());
							list.add(data);
						}
						dbHelper.insertPlaces(zoom, tileX, tileY, map);
					}
					synchronized (loadingTasks) {
						loadingTasks.remove(tileKey);
					}
				}
			});
			loadingTasks.put(tileKey, task);
			task.execute();
		}
	}

	@Override
	public boolean isLoading() {
		return !loadingTasks.isEmpty();
	}

	public boolean isLoadingRect(@NonNull QuadRect rect) {
		KQuadRect kRect = SharedUtil.kQuadRect(rect);
		synchronized (loadingTasks) {
			for (GetExplorePlacesImagesTask task : loadingTasks.values()) {
				if (task.getMapRect().contains(kRect)) {
					return true;
				}
			}
			return false;
		}
	}
}
