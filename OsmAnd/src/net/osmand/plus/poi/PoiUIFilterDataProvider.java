package net.osmand.plus.poi;

import static net.osmand.data.DataSourceType.OFFLINE;
import static net.osmand.data.DataSourceType.ONLINE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.DataSourceType;
import net.osmand.data.QuadRect;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.exploreplaces.ExplorePlacesProvider;
import net.osmand.plus.resources.BinaryMapReaderResource;
import net.osmand.plus.resources.ResourceManager.BinaryMapReaderResourceType;
import net.osmand.plus.views.layers.POIMapLayer.PoiUIFilterResultMatcher;
import net.osmand.search.AmenitySearcher;
import net.osmand.search.core.spatial.SpatialTextSearchAPI;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PoiUIFilterDataProvider {

    private static final Log LOG = PlatformUtil.getLog(PoiUIFilterDataProvider.class);

    // one for the app: the poi type index and the name-index cache are big
    private static SpatialTextSearchAPI nameIndexSearch;

    private final OsmandApplication app;
    private final PoiUIFilter filter;

    private final ExplorePlacesProvider explorePlacesProvider;

    public PoiUIFilterDataProvider(@NonNull OsmandApplication app, @NonNull PoiUIFilter filter) {
        this.app = app;
        this.filter = filter;
        this.explorePlacesProvider = app.getExplorePlacesProvider();
    }

	@NonNull
	public DataSourceType getDataSourceType() {
		return filter.isTopWikiFilter() ? app.getSettings().WIKI_DATA_SOURCE_TYPE.get() : OFFLINE;
	}

    List<Amenity> searchAmenities(double lat, double lon, double topLatitude,
                                  double bottomLatitude, double leftLongitude,
                                  double rightLongitude, int zoom,
                                  @Nullable ResultMatcher<Amenity> matcher,
                                  @Nullable Comparator<Amenity> comparator,
                                  int comparatorLimit) {
        if (filter.isTopWikiFilter() && getDataSourceType() == ONLINE) {
            return searchWikiOnline(lat, lon, topLatitude, bottomLatitude, leftLongitude, rightLongitude,
                    filter.wrapResultMatcher(matcher));
        } else {
            AmenitySearcher amenitySearcher = app.getResourceManager().getAmenitySearcher();
            AmenitySearcher.Settings settings = app.getResourceManager().getDefaultAmenitySearchSettings();
            ResultMatcher<Amenity> resultMatcher = filter.wrapResultMatcher(matcher);
            List<Amenity> indexed = new ArrayList<>();
            Set<String> indexedFiles = searchNameIndex(topLatitude, leftLongitude, bottomLatitude, rightLongitude, zoom,
                    resultMatcher, indexed);
            List<Amenity> res = amenitySearcher.searchAmenities(filter, filter.additionalFilter, topLatitude,
                    leftLongitude, bottomLatitude, rightLongitude, zoom, true, settings.fileVisibility(),
                    resultMatcher, repo -> !indexedFiles.contains(repo.getFile().getName()), comparator, comparatorLimit);
            if (!indexed.isEmpty()) {
                res = new ArrayList<>(res);
                res.addAll(indexed);
            }
            return res;
        }
    }

    // spatial search: the maps with poi types in the name index are read from it, the others with the type filter
    @NonNull
    private Set<String> searchNameIndex(double top, double left, double bottom, double right, int zoom,
                                        @NonNull ResultMatcher<Amenity> matcher, @NonNull List<Amenity> result) {
        List<String> keys = app.getSettings().USE_SPATIAL_TEXT_SEARCH.get() && filter.additionalFilter == null
                && !filter.isTopWikiFilter()
                ? getNameIndexSearch().getNameIndexKeys(filter) : null;
        if (keys == null) {
            return Collections.emptySet();
        }
        int left31 = MapUtils.get31TileNumberX(left);
        int top31 = MapUtils.get31TileNumberY(top);
        int right31 = MapUtils.get31TileNumberX(right);
        int bottom31 = MapUtils.get31TileNumberY(bottom);
        Set<String> files = new HashSet<>();
        List<BinaryMapIndexReader> readers = new ArrayList<>();
        for (BinaryMapReaderResource resource : app.getResourceManager().getFileReaders()) {
            BinaryMapIndexReader shallow = resource.getShallowReader();
            if (shallow != null && SpatialTextSearchAPI.hasPoiTypesInNameIndex(shallow)
                    && shallow.containsPoiData(left31, top31, right31, bottom31)) {
                BinaryMapIndexReader reader = resource.getReader(BinaryMapReaderResourceType.POI_NAME_INDEX);
                if (reader != null) {
                    readers.add(reader);
                    files.add(resource.getFileName());
                }
            }
        }
        if (readers.isEmpty()) {
            return files;
        }
        try {
            QuadRect bbox = new QuadRect(left, top, right, bottom);
            for (Amenity a : getNameIndexSearch().searchPoiByCategory(readers, keys, bbox, zoom)) {
                if (matcher.isCancelled()) {
                    break;
                }
                if (!a.isClosed() && matcher.publish(a)) {
                    result.add(a);
                }
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return Collections.emptySet();
        }
        return files;
    }

    @NonNull
    private SpatialTextSearchAPI getNameIndexSearch() {
        synchronized (PoiUIFilterDataProvider.class) {
            if (nameIndexSearch == null) {
                nameIndexSearch = new SpatialTextSearchAPI(app.getPoiTypes());
            }
            return nameIndexSearch;
        }
    }

    @NonNull
    private List<Amenity> searchWikiOnline(double lat, double lon, double topLatitude,
                                           double bottomLatitude, double leftLongitude,
                                           double rightLongitude, @Nullable ResultMatcher<Amenity> matcher) {
        QuadRect rect = new QuadRect(leftLongitude, topLatitude, rightLongitude, bottomLatitude);
        List<Amenity> data = explorePlacesProvider.getDataCollection(rect, 0);
        boolean loading = false;
        boolean cancelled = matcher != null && matcher.isCancelled();
        PoiUIFilterResultMatcher<?> uiFilterResultMatcher = matcher != null ? (PoiUIFilterResultMatcher<?>) matcher : null;
        while (explorePlacesProvider.isLoading() && !cancelled) {
            if (uiFilterResultMatcher != null) {
                uiFilterResultMatcher.defferedResults();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignore) {
            }
            loading = true;
            cancelled = matcher != null && matcher.isCancelled();
        }
        if (cancelled) {
            return new ArrayList<>();
        }
        if (loading) {
            data = explorePlacesProvider.getDataCollection(rect, 0);
        }
        List<Amenity> result = matcher == null ? data : new ArrayList<>();
        if (matcher != null) {
            for (Amenity a : data) {
                if (matcher.publish(a)) {
                    result.add(a);
                }
            }
        }
        MapUtils.sortListOfMapObject(result, lat, lon);
        return result;
    }
}
