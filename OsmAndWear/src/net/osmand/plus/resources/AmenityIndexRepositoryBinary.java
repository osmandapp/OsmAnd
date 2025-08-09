package net.osmand.plus.resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapPoiReaderAdapter;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiSubType;
import net.osmand.data.Amenity;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.resources.ResourceManager.BinaryMapReaderResource;
import net.osmand.plus.resources.ResourceManager.BinaryMapReaderResourceType;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AmenityIndexRepositoryBinary implements AmenityIndexRepository {

	private static final Log log = PlatformUtil.getLog(AmenityIndexRepositoryBinary.class);
	private final BinaryMapReaderResource resource;
	private final MapPoiTypes poiTypes;
	private Map<String, List<String>> deltaPoiCategories = new HashMap<>();

	public AmenityIndexRepositoryBinary(BinaryMapReaderResource resource, OsmandApplication app) {
		this.resource = resource;
		poiTypes = app.getPoiTypes();
		checkCachedCategories(app.getPoiFilters());
	}

	public Map<String, List<String>> getDeltaPoiCategories() {
		return deltaPoiCategories;
	}

	private void checkCachedCategories(PoiFiltersHelper poiFiltersHelper) {
		String fileName = resource.getFileName();
		long lastModified = resource.getFileLastModified();
		Pair<Long, Map<String, List<String>>> cache = poiFiltersHelper.getCacheByResourceName(fileName);
		if (cache == null || cache.first != null && cache.first != lastModified) {
			deltaPoiCategories = new HashMap<>();
			try {
				BinaryMapIndexReader reader = getOpenFile();
				if (reader != null) {
					reader.initCategories();
					List<BinaryMapPoiReaderAdapter.PoiRegion> regions = reader.getPoiIndexes();
					for (BinaryMapPoiReaderAdapter.PoiRegion region : regions) {
						calculateDeltaSubcategories(region);
					}
					if (cache == null) {
						poiFiltersHelper.insertCacheForResource(fileName, lastModified, deltaPoiCategories);
					} else {
						poiFiltersHelper.updateCacheForResource(fileName, lastModified, deltaPoiCategories);
					}
				}
			} catch (IOException e) {
				log.error("Error initializing categories ", e);
			}
		} else if (cache.second != null) {
			deltaPoiCategories = cache.second;
		}
	}

	private void calculateDeltaSubcategories(BinaryMapPoiReaderAdapter.PoiRegion region) {
		List<String> categories = region.getCategories();
		List<List<String>> subCategories = region.getSubcategories();
		for (int i = 0; i < categories.size(); i++) {
			String categoryName = categories.get(i);

			PoiCategory poiCategory = poiTypes.getPoiCategoryByName(categoryName);
			List<String> deltaSubCategories = null;
			for (String subCategory : subCategories.get(i)) {
				if (poiCategory.getPoiTypeByKeyName(subCategory) == null) {
					if (deltaSubCategories == null) {
						deltaSubCategories = new ArrayList<>();
					}
					deltaSubCategories.add(subCategory);
				}
			}
			if (deltaSubCategories != null) {
				if(deltaPoiCategories.containsKey(categoryName)) {
					deltaPoiCategories.get(categoryName).addAll(deltaSubCategories);
				} else {
					deltaPoiCategories.put(categoryName, deltaSubCategories);
				}
			}
		}
	}

	@Nullable
	private BinaryMapIndexReader getOpenFile() {
		return resource.getReader(BinaryMapReaderResourceType.POI);
	}

	@Override
	public void close() {
	}

	@Override
	public boolean checkContains(double latitude, double longitude) {
		int x31 = MapUtils.get31TileNumberX(longitude);
		int y31 = MapUtils.get31TileNumberY(latitude);
		BinaryMapIndexReader reader = getOpenFile();
		return reader != null && reader.containsPoiData(x31, y31, x31, y31);
	}

	@Override
	public boolean checkContainsInt(int top31, int left31, int bottom31, int right31) {
		BinaryMapIndexReader reader = getOpenFile();
		return reader != null && reader.containsPoiData(left31, top31, right31, bottom31);
	}


	public synchronized Map<PoiCategory, List<String>> searchAmenityCategoriesByName(String query, Map<PoiCategory, List<String>> map) {
		try {
			BinaryMapIndexReader reader = getOpenFile();
			return reader != null ? reader.searchPoiCategoriesByName(query, map) : new HashMap<PoiCategory, List<String>>();
		} catch (IOException e) {
			log.error("Error searching amenities", e); //$NON-NLS-1$
		}
		return map;
	}

	@NonNull
	public synchronized List<PoiSubType> searchPoiSubTypesByPrefix(@NonNull String query) {
		List<PoiSubType> poiSubTypes = new ArrayList<>();
		try {
			BinaryMapIndexReader reader = getOpenFile();
			if (reader != null) {
				poiSubTypes.addAll(reader.searchPoiSubTypesByPrefix(query));
			}
		} catch (IOException e) {
			log.error("Error searching poiSubTypes", e);
		}
		return poiSubTypes;
	}

	public synchronized List<Amenity> searchAmenitiesByName(int x, int y, int l, int t, int r, int b, String query, ResultMatcher<Amenity> resulMatcher) {
		long now = System.currentTimeMillis();
		List<Amenity> amenities = Collections.emptyList();
		SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(x, y, query, l, r, t, b, resulMatcher);
		try {
			BinaryMapIndexReader index = getOpenFile();
			if (index != null) {
				amenities = index.searchPoiByName(req);
				if (log.isDebugEnabled()) {
					String nm = "";
					List<MapIndex> mi = index.getMapIndexes();
					if (mi.size() > 0) {
						nm = mi.get(0).getName();
					}
					log.debug(String.format("Search for %s done in %s ms found %s (%s) %s.",  //$NON-NLS-1$
							query, System.currentTimeMillis() - now, amenities.size(), nm, index.getFile().getName())); //$NON-NLS-1$
				}
			}
		} catch (IOException e) {
			log.error("Error searching amenities", e); //$NON-NLS-1$
		}
		return amenities;
	}

	@Override
	public synchronized List<Amenity> searchAmenities(int stop, int sleft, int sbottom, int sright, int zoom,
													  SearchPoiTypeFilter filter, ResultMatcher<Amenity> matcher) {
		long now = System.currentTimeMillis();
		SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(sleft, sright, stop, sbottom, zoom,
				filter, matcher);
		List<Amenity> result = null;
		try {
			BinaryMapIndexReader reader = getOpenFile();
			if (reader != null) {
				result = reader.searchPoi(req);
			}
		} catch (IOException e) {
			log.error("Error searching amenities", e); //$NON-NLS-1$
		}
		if (log.isDebugEnabled() && result != null) {
			log.debug(String.format("Search for %s done in %s ms found %s.",  //$NON-NLS-1$
					MapUtils.get31LatitudeY(stop) + " " + MapUtils.get31LongitudeX(sleft), System.currentTimeMillis() - now, result.size())); //$NON-NLS-1$
		}
		return result;
	}

	@Override
	public synchronized List<Amenity> searchAmenitiesOnThePath(List<Location> locations, double radius, SearchPoiTypeFilter filter, ResultMatcher<Amenity> matcher) {
		long now = System.currentTimeMillis();
		List<Amenity> result = null;
		SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(locations, radius,
				filter, matcher);
		try {
			BinaryMapIndexReader reader = getOpenFile();
			if (reader != null) {
				result = reader.searchPoi(req);
			}
		} catch (IOException e) {
			log.error("Error searching amenities", e); //$NON-NLS-1$
			return result;
		}
		if (log.isDebugEnabled() && result != null) {
			log.debug(String.format("Search done in %s ms found %s.", (System.currentTimeMillis() - now), result.size())); //$NON-NLS-1$
		}
		return result;

	}

}
