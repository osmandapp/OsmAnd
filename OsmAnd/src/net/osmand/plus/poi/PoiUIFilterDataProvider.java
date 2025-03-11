package net.osmand.plus.poi;

import androidx.annotation.NonNull;

import net.osmand.ResultMatcher;
import net.osmand.data.Amenity;
import net.osmand.data.DataSourceType;
import net.osmand.data.QuadRect;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.exploreplaces.ExplorePlacesProvider;
import net.osmand.util.MapUtils;

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
                                  ResultMatcher<Amenity> matcher) {
        if (filter.isTopWikiFilter() && getDataSourceType() == DataSourceType.ONLINE) {
            return searchWikiOnline(lat, lon, topLatitude, bottomLatitude, leftLongitude, rightLongitude);
        } else {
            return app.getResourceManager().searchAmenities(filter, filter.additionalFilter, topLatitude, leftLongitude,
                    bottomLatitude, rightLongitude, zoom, true, filter.wrapResultMatcher(matcher));
        }
    }

    @NonNull
    private List<Amenity> searchWikiOnline(double lat, double lon, double topLatitude,
                                           double bottomLatitude, double leftLongitude,
                                           double rightLongitude) {
        QuadRect rect = new QuadRect(leftLongitude, topLatitude, rightLongitude, bottomLatitude);
        List<Amenity> res = explorePlacesProvider.getDataCollection(rect, 0);
        boolean loaded = false;
        while (explorePlacesProvider.isLoading()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignore) {
            }
            loaded = true;
        }
        if (loaded) {
            res = explorePlacesProvider.getDataCollection(rect);
        }

        MapUtils.sortListOfMapObject(res, lat, lon);
        return res;
    }
}
