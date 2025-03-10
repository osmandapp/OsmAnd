package net.osmand.plus.exploreplaces;

import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.data.Amenity;
import net.osmand.data.QuadRect;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiType;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.exploreplaces.ExplorePlacesProvider.ExplorePlacesListener;
import net.osmand.plus.poi.PoiFilterUtils;
import net.osmand.plus.poi.PoiFilterUtils.AmenityNameFilter;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import net.osmand.util.TransliterationHelper;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExplorePlacesOnlineFilter extends PoiUIFilter implements ExplorePlacesListener {

    private static final Log LOG = PlatformUtil.getLog(ExplorePlacesOnlineFilter.class);

    private final ExplorePlacesProvider explorePlacesProvider;

    public ExplorePlacesOnlineFilter(OsmandApplication app) {
        super(app);
        this.filterId = TOP_WIKI_FILTER_ID;
        this.name = app.getString(R.string.popular_places_nearby);
        this.explorePlacesProvider = app.getExplorePlacesProvider();
    }

    @Override
    public boolean isAutomaticallyIncreaseSearch() {
        return false;
    }

    @Override
    public AmenityNameFilter getNameFilter() {
        return a -> true;
    }

    @Override
    protected List<Amenity> searchAmenitiesInternal(double lat, double lon, double topLatitude,
                                                    double bottomLatitude, double leftLongitude,
                                                    double rightLongitude, int zoom,
                                                    ResultMatcher<Amenity> matcher) {
        QuadRect rect = new QuadRect(leftLongitude, topLatitude, rightLongitude, bottomLatitude);
        currentSearchResult = explorePlacesProvider.getDataCollection(rect, 200, zoom);
        boolean loaded = false;
        while (explorePlacesProvider.isLoading()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignore) {
            }
            loaded = true;
        }
        if (loaded) {
            currentSearchResult = explorePlacesProvider.getDataCollection(rect);
        }

        MapUtils.sortListOfMapObject(currentSearchResult, lat, lon);
        return currentSearchResult;
    }

    @Override
    public void onNewExplorePlacesDownloaded() {

    }
}
