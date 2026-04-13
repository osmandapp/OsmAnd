package net.osmand.plus.exploreplaces;

import static net.osmand.data.Amenity.DEFAULT_ELO;
import static net.osmand.data.Amenity.WIKIDATA;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.binary.ObfConstants;
import net.osmand.data.Amenity;
import net.osmand.data.MapObject;
import net.osmand.data.QuadRect;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.edit.Entity.EntityType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.wikipedia.WikipediaPlugin;
import net.osmand.shared.data.KQuadRect;
import net.osmand.shared.exploreplaces.GetExplorePlacesImagesTask;
import net.osmand.shared.exploreplaces.GetExplorePlacesImagesTask.GetImageCardsListener;
import net.osmand.shared.exploreplaces.PlacesDatabaseHelper;
import net.osmand.shared.wiki.WikiCoreHelper.OsmandApiFeatureData;
import net.osmand.shared.wiki.WikiCoreHelper.WikiDataGeometry;
import net.osmand.shared.wiki.WikiCoreHelper.WikiDataProperties;
import net.osmand.shared.wiki.WikiHelper;
import net.osmand.shared.wiki.WikiImage;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;
import net.osmand.util.MapUtils;
import net.osmand.util.TransliterationHelper;

import org.apache.commons.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Extra: display new categories from web

public class ExplorePlacesOnlineProvider implements ExplorePlacesProvider {

	private static final Log LOG = PlatformUtil.getLog(ExplorePlacesOnlineProvider.class);

	public static final int DEFAULT_LIMIT_POINTS = 200;
	private static final int NEARBY_MIN_RADIUS = 50;

	private static final int MAX_TILES_PER_QUAD_RECT = 12;
	private static final int MAX_TILES_PER_CACHE = MAX_TILES_PER_QUAD_RECT * 2;
	private static final double LOAD_ALL_TINY_RECT = 0.5;
	private static final String LANGUAGE_SCOPE_SEPARATOR = "|";

	private final OsmandApplication app;
	private final PlacesDatabaseHelper dbHelper;
	private final Object tilesLock = new Object();
	private final ExecutorService tilePersistenceExecutor = Executors.newSingleThreadExecutor();

	private final Map<TileKey, GetExplorePlacesImagesTask> loadingTasks = new HashMap<>();
	private final Map<TileKey, List<Amenity>> tilesCache = new HashMap<>(); // Memory cache for recent tiles


	private static class TileKey {
		final int zoom;
		final int tileX;
		final int tileY;
		@NonNull
		final String languageScope;

		public TileKey(int zoom, int tileX, int tileY, @NonNull String languageScope) {
			this.zoom = zoom;
			this.tileX = tileX;
			this.tileY = tileY;
			this.languageScope = languageScope;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof TileKey tileKey)) {
				return false;
			}
			return zoom == tileKey.zoom && tileX == tileKey.tileX && tileY == tileKey.tileY
					&& Objects.equals(languageScope, tileKey.languageScope);
		}

		@Override
		public int hashCode() {
			return Objects.hash(zoom, tileX, tileY, languageScope);
		}
	}

	public ExplorePlacesOnlineProvider(OsmandApplication app) {
		this.app = app;
		this.dbHelper = new PlacesDatabaseHelper();
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
		synchronized (tilesLock) {
			if (rect == null) {
				Iterator<Entry<TileKey, GetExplorePlacesImagesTask>> iterator = loadingTasks.entrySet().iterator();
				while (iterator.hasNext()) {
					iterator.next().getValue().cancel();
					iterator.remove();
				}
				return Collections.emptyList();
			}
			KQuadRect kRect = SharedUtil.kQuadRect(rect);
			Iterator<Entry<TileKey, GetExplorePlacesImagesTask>> iterator = loadingTasks.entrySet().iterator();
			while (iterator.hasNext()) {
				GetExplorePlacesImagesTask task = iterator.next().getValue();
				if (!kRect.contains(task.getMapRect()) && !KQuadRect.Companion.intersects(kRect, task.getMapRect())) {
					task.cancel();
					iterator.remove();
				}
			}
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
		boolean loadAll = zoom == MAX_LEVEL_ZOOM_CACHE
				&& (Math.abs(maxTileX - minTileX) <= LOAD_ALL_TINY_RECT || Math.abs(maxTileY - minTileY) <= LOAD_ALL_TINY_RECT);

		// Fetch data for all tiles within the bounds
		List<Amenity> filteredAmenities = new ArrayList<>();
		Set<Long> uniqueIds = new HashSet<>(); // Use a Set to track unique IDs
		List<String> languages = getPreferredLangs();

		// Iterate over the tiles and load data
		for (int tileX = (int) minTileX; tileX <= (int) maxTileX; tileX++) {
			for (int tileY = (int) minTileY; tileY <= (int) maxTileY; tileY++) {
				TileKey tileKey = createTileKey(zoom, tileX, tileY, languages);
				List<Amenity> cachedPlaces;
				synchronized (tilesLock) {
					cachedPlaces = tilesCache.get(tileKey);
				}
				if (cachedPlaces != null) {
					filterAmenities(cachedPlaces, filteredAmenities, rect, uniqueIds, loadAll);
				} else if (!dbHelper.isDataExpired(zoom, tileX, tileY, languages)) {
					List<OsmandApiFeatureData> places = dbHelper.getPlaces(zoom, tileX, tileY, languages);
					cachedPlaces = new ArrayList<>();
					for (OsmandApiFeatureData item : places) {
						Amenity amenity = createAmenity(item);
						if (amenity != null) {
							filterAmenity(amenity, filteredAmenities, rect, uniqueIds, loadAll);
							cachedPlaces.add(amenity);
						}
					}
					synchronized (tilesLock) {
						tilesCache.put(tileKey, cachedPlaces);
					}
				} else {
					loadTile(tileKey, zoom, tileX, tileY, languages);
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

	@NonNull
	private TileKey createTileKey(int zoom, int tileX, int tileY, @NonNull List<String> languages) {
		return new TileKey(zoom, tileX, tileY, getLanguageScope(languages));
	}

	@NonNull
	private String getLanguageScope(@NonNull List<String> languages) {
		return String.join(LANGUAGE_SCOPE_SEPARATOR, languages);
	}

	private void filterAmenity(@NonNull Amenity amenity, @NonNull List<Amenity> filteredAmenities,
			@NonNull QuadRect rect, @NonNull Set<Long> uniqueIds, boolean loadAll) {
		double lat = amenity.getLocation().getLatitude();
		double lon = amenity.getLocation().getLongitude();
		if ((rect.contains(lon, lat, lon, lat) || loadAll) && uniqueIds.add(amenity.getId())) {
			filteredAmenities.add(amenity);
		}
	}

	private void filterAmenities(@NonNull List<Amenity> amenities, @NonNull List<Amenity> filteredAmenities,
			@NonNull QuadRect rect, @NonNull Set<Long> uniqueIds, boolean loadAll) {
		for (Amenity amenity : amenities) {
			filterAmenity(amenity, filteredAmenities, rect, uniqueIds, loadAll);
		}
	}

	private void clearCache(int zoom, int minTileX, int maxTileX, int minTileY, int maxTileY) {
		synchronized (tilesLock) {
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
	}

	@Nullable
	private Amenity createAmenity(@NonNull OsmandApiFeatureData featureData) {
		Amenity amenity = new Amenity();
		WikiDataProperties properties = featureData.getProperties();

		String id = properties.getId();
		amenity.setAdditionalInfo(WIKIDATA, app.getString(R.string.wikidata_id_pattern, id));
		String lang = properties.getLang();
		if (Algorithms.isEmpty(lang)) {
			lang = properties.getWikiLang();
		}
		if (lang != null && !lang.isEmpty()) {
			amenity.setAdditionalInfo("wiki_lang:" + lang, "yes");
		}
		amenity.setName(properties.getWikiTitle());
		amenity.setEnName(TransliterationHelper.transliterate(amenity.getName()));
		amenity.setDescription(properties.getWikiDesc());

		String labelsJson = properties.getLabelsJson();
		if (!Algorithms.isEmpty(labelsJson) && labelsJson.length() > 2) {
			try {
				MapObject.parseNamesJSON(new JSONObject(labelsJson), amenity);
			} catch (JSONException e) {
				LOG.error(e);
			}
		}
		String wikiLangs = properties.getWikiLangs();
		if (!Algorithms.isEmpty(wikiLangs)) {
			amenity.updateContentLocales(Set.of(wikiLangs.split(",")));
		}

		String photoTitle = properties.getPhotoTitle();
		if (!Algorithms.isEmpty(photoTitle)) {
			WikiImage imageData = WikiHelper.INSTANCE.getImageData(photoTitle);
			amenity.setWikiPhoto(imageData.getImageHiResUrl());
			amenity.setWikiIconUrl(imageData.getImageIconUrl());
			amenity.setWikiImageStubUrl(imageData.getImageStubUrl());
		}
		WikiDataGeometry geometry = featureData.getGeometry();
		if (geometry != null) {
			amenity.setLocation(geometry.getCoordinates()[1], geometry.getCoordinates()[0]);
		}

		String poitype = properties.getPoitype();
		String subtype = properties.getPoisubtype();
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

		Long osmId = properties.getOsmid();
		if (osmId != null && osmId > 0) {
			amenity.setId(ObfConstants.createMapObjectIdFromCleanOsmId(osmId, EntityType.valueOf(properties.getOsmtype())));
		} else if (id != null) {
			amenity.setId(-Long.parseLong(id));
		}
		//amenity.setTravelTopic(properties.wikiTitle);
		//amenity.setWikiCategory(properties.wikiDesc);
		Double elo = properties.getElo();
		amenity.setTravelEloNumber(elo != null ? elo.intValue() : DEFAULT_ELO);

		return amenity;
	}

	@SuppressLint("DefaultLocale")
	private void loadTile(@NonNull TileKey tileKey, int zoom, int tileX, int tileY, @NonNull List<String> languages) {
		double left = MapUtils.getLongitudeFromTile(zoom, tileX);
		double right = MapUtils.getLongitudeFromTile(zoom, tileX + 1);
		double top = MapUtils.getLatitudeFromTile(zoom, tileY);
		double bottom = MapUtils.getLatitudeFromTile(zoom, tileY + 1);

		KQuadRect tileRect = new KQuadRect(left, top, right, bottom);
		List<String> requestLanguages = new ArrayList<>(languages);
		synchronized (tilesLock) {
			if (loadingTasks.containsKey(tileKey)) {
				return;
			}
			GetExplorePlacesImagesTask task = new GetExplorePlacesImagesTask(tileRect, zoom, requestLanguages, new GetImageCardsListener() {

				@Override
				public void onTaskStarted() {
				}

				@Override
				public void onFinish(@NonNull List<OsmandApiFeatureData> result) {
					schedulePersistLoadedTile(tileKey, zoom, tileX, tileY, requestLanguages, result);
				}
			});
			loadingTasks.put(tileKey, task);
			task.execute();
		}
	}

	private void schedulePersistLoadedTile(@NonNull TileKey tileKey, int zoom, int tileX, int tileY,
			@NonNull List<String> languages, @NonNull List<OsmandApiFeatureData> result) {
		tilePersistenceExecutor.execute(() -> persistLoadedTile(tileKey, zoom, tileX, tileY, languages, result));
	}

	private void persistLoadedTile(@NonNull TileKey tileKey, int zoom, int tileX, int tileY,
			@NonNull List<String> languages, @NonNull List<OsmandApiFeatureData> result) {
		boolean isPartial;
		List<Amenity> cachedPlaces;
		try {
			cachedPlaces = createCachedPlaces(result);
		} catch (Exception e) {
			LOG.error("Failed to create cached explore places tile", e);
			cachedPlaces = Collections.emptyList();
		}
		try {
			Map<String, List<OsmandApiFeatureData>> placesByLang = groupPlacesByLanguage(result, languages);
			Boolean allLanguagesEmptyResult = Algorithms.isEmpty(languages)
					? Algorithms.isEmpty(result)
					: null;
			boolean persisted = dbHelper.insertPlaces(
					zoom, tileX, tileY, placesByLang, allLanguagesEmptyResult);
			if (!persisted) {
				LOG.error("Failed to persist explore places tile");
			}
		} catch (Exception e) {
			LOG.error("Failed to persist loaded explore places tile", e);
		}
		synchronized (tilesLock) {
			tilesCache.put(tileKey, cachedPlaces);
			loadingTasks.remove(tileKey);
			isPartial = !loadingTasks.isEmpty();
		}
		notifyListeners(isPartial);
	}

	@NonNull
	private Map<String, List<OsmandApiFeatureData>> groupPlacesByLanguage(@NonNull List<OsmandApiFeatureData> result,
			@NonNull List<String> languages) {
		Map<String, List<OsmandApiFeatureData>> map = new HashMap<>();
		if (!Algorithms.isEmpty(result)) {
			for (OsmandApiFeatureData data : result) {
				WikiDataProperties properties = data.getProperties();
				String lang = properties.getLang();
				if (Algorithms.isEmpty(lang)) {
					lang = properties.getWikiLang();
				}
				List<OsmandApiFeatureData> list = map.computeIfAbsent(lang, k -> new ArrayList<>());
				list.add(data);
			}
		}
		for (String lang : languages) {
			map.putIfAbsent(lang, Collections.emptyList());
		}
		return map;
	}

	@NonNull
	private List<Amenity> createCachedPlaces(@NonNull List<OsmandApiFeatureData> places) {
		List<Amenity> amenities = new ArrayList<>();
		Set<Long> uniqueIds = new HashSet<>();
		for (OsmandApiFeatureData item : places) {
			Amenity amenity = createAmenity(item);
			if (amenity == null) {
				continue;
			}
			Long id = amenity.getId();
			if (id == null || uniqueIds.add(id)) {
				amenities.add(amenity);
			}
		}
		return amenities;
	}

	@Override
	public boolean isLoading() {
		synchronized (tilesLock) {
			return !loadingTasks.isEmpty();
		}
	}

	public boolean isLoadingRect(@NonNull QuadRect rect) {
		KQuadRect kRect = SharedUtil.kQuadRect(rect);
		synchronized (tilesLock) {
			for (GetExplorePlacesImagesTask task : loadingTasks.values()) {
				if (KQuadRect.Companion.intersects(kRect, task.getMapRect())) {
					return true;
				}
			}
			return false;
		}
	}
}
