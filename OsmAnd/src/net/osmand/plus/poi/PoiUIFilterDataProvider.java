package net.osmand.plus.poi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.ResultMatcher;
import net.osmand.data.Amenity;
import net.osmand.data.DataSourceType;
import net.osmand.data.QuadRect;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.exploreplaces.ExplorePlacesProvider;
import net.osmand.plus.views.layers.POIMapLayer.PoiUIFilterResultMatcher;
import net.osmand.search.AmenitySearcher;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public class PoiUIFilterDataProvider {

    private final OsmandApplication app;
    private final PoiUIFilter filter;

    private final ExplorePlacesProvider explorePlacesProvider;

    public PoiUIFilterDataProvider(@NonNull OsmandApplication app, @NonNull PoiUIFilter filter) {
        this.app = app;
        this.filter = filter;
        this.explorePlacesProvider = app.getExplorePlacesProvider();
    }

    public DataSourceType getDataSourceType() {
        if (filter.isTopWikiFilter()) {
            return app.getSettings().WIKI_DATA_SOURCE_TYPE.get() == DataSourceType.ONLINE
                    ? DataSourceType.ONLINE : DataSourceType.OFFLINE;
        } else {
            return DataSourceType.OFFLINE;
        }
    }

    List<Amenity> searchAmenities(double lat, double lon, double topLatitude,
                                  double bottomLatitude, double leftLongitude,
                                  double rightLongitude, int zoom,
                                  @Nullable ResultMatcher<Amenity> matcher) {
        if (filter.isTopWikiFilter() && getDataSourceType() == DataSourceType.ONLINE) {
            return searchWikiOnline(lat, lon, topLatitude, bottomLatitude, leftLongitude, rightLongitude,
                    filter.wrapResultMatcher(matcher));
        } else {
            AmenitySearcher amenitySearcher = app.getResourceManager().getAmenitySearcher();
            AmenitySearcher.Settings settings = app.getResourceManager().getDefaultAmenitySearchSettings();
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
        return data;
    }
}
