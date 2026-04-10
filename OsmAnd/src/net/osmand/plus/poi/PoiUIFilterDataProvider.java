package net.osmand.plus.poi;

import static net.osmand.data.DataSourceType.OFFLINE;
import static net.osmand.data.DataSourceType.ONLINE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.data.Amenity;
import net.osmand.data.DataSourceType;
import net.osmand.data.QuadRect;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.exploreplaces.ExplorePlacesProvider;
import net.osmand.plus.views.layers.POIMapLayer.PoiUIFilterResultMatcher;
import net.osmand.search.AmenitySearcher;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PoiUIFilterDataProvider {

    private static final Log LOG = PlatformUtil.getLog(PoiUIFilterDataProvider.class);

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
            LOG.debug("searchAmenities: Routing to ONLINE wiki search for rect [" + leftLongitude + "," + topLatitude + " to " + rightLongitude + "," + bottomLatitude + "]");
            return searchWikiOnline(lat, lon, topLatitude, bottomLatitude, leftLongitude, rightLongitude,
                    filter.wrapResultMatcher(matcher));
        } else {
            AmenitySearcher amenitySearcher = app.getResourceManager().getAmenitySearcher();
            AmenitySearcher.Settings settings = app.getResourceManager().getDefaultAmenitySearchSettings();
            amenitySearcher.setComparator(comparator, comparatorLimit);
            return amenitySearcher.searchAmenities(filter, filter.additionalFilter, topLatitude, leftLongitude,
                    bottomLatitude, rightLongitude, zoom, true, settings.fileVisibility(),
                    filter.wrapResultMatcher(matcher));
        }
    }

    @NonNull
    private List<Amenity> searchWikiOnline(double lat, double lon, double topLatitude,
                                           double bottomLatitude, double leftLongitude,
                                           double rightLongitude, @Nullable ResultMatcher<Amenity> matcher) {
        QuadRect rect = new QuadRect(leftLongitude, topLatitude, rightLongitude, bottomLatitude);
        LOG.debug("searchWikiOnline: Requesting initial dataCollection from explorePlacesProvider...");
        List<Amenity> data = explorePlacesProvider.getDataCollection(rect, 0);
        LOG.debug("searchWikiOnline: Initial data size returned: " + data.size());
        boolean loading = false;
        boolean cancelled = matcher != null && matcher.isCancelled();
        PoiUIFilterResultMatcher<?> uiFilterResultMatcher = matcher != null ? (PoiUIFilterResultMatcher<?>) matcher : null;
        int waitLoops = 0;
        while (explorePlacesProvider.isLoading() && !cancelled) {
            if (uiFilterResultMatcher != null) {
                uiFilterResultMatcher.defferedResults();
            }
            try {
                Thread.sleep(100);
                waitLoops++;
            } catch (InterruptedException ignore) {
            }
            loading = true;
            cancelled = matcher != null && matcher.isCancelled();
        }
        if (loading) {
            LOG.debug("searchWikiOnline: Waited " + (waitLoops * 100) + "ms for loading. Requesting dataCollection again...");
        }
        if (cancelled) {
            LOG.debug("searchWikiOnline: Task cancelled during wait loop. Returning empty.");
            return new ArrayList<>();
        }
        if (loading) {
            data = explorePlacesProvider.getDataCollection(rect, 0);
            LOG.debug("searchWikiOnline: Post-loading data size returned: " + data.size());
        }
        List<Amenity> result = matcher == null ? data : new ArrayList<>();
        if (matcher != null) {
            int acceptedCount = 0;
            for (Amenity a : data) {
                if (matcher.publish(a)) {
                    result.add(a);
                    acceptedCount++;
                }
            }
            LOG.debug("searchWikiOnline: ResultMatcher applied. Accepted " + acceptedCount + " out of " + data.size() + " items.");
        }
        MapUtils.sortListOfMapObject(result, lat, lon);
        LOG.debug("searchWikiOnline: Final returned list size: " + data.size());
        return data;
    }
}
