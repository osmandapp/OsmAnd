package net.osmand.search.core;

import net.osmand.CallbackWithObject;
import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.search.FullAmenitySearch;
import net.osmand.util.MapUtils;

import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SearchAmenitiesAsync {

    private ThreadPoolExecutor singleThreadedExecutor;
    private LinkedBlockingQueue<Runnable> taskQueue;

    private FullAmenitySearch fullAmenitySearch;
    private Executor mainThreadExecutor;

    public SearchAmenitiesAsync(FullAmenitySearch fullAmenitySearch, Executor mainThreadExecutor) {
        taskQueue = new LinkedBlockingQueue<Runnable>();
        singleThreadedExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, taskQueue);
        this.fullAmenitySearch = fullAmenitySearch;
        this.mainThreadExecutor = mainThreadExecutor;
    }

    public void searchAmenity(LatLon latLon, long id, Collection<String> names, String wikidata, CallbackWithObject<Amenity> callbackWithAmenity) {
        singleThreadedExecutor.submit(() -> {
            Amenity amenity = fullAmenitySearch.findAmenity(latLon, id, names, wikidata);
            mainThreadExecutor.execute(() -> callbackWithAmenity.processResult(amenity));
        });
    }

    public void searchAmenity(RenderedObject renderedObject, CallbackWithObject<Amenity> callbackWithAmenity) {
        LatLon latLon = renderedObject.getLabelLatLon();
        if (latLon == null && renderedObject.getLabelX() != 0) {
            latLon = new LatLon(MapUtils.get31LatitudeY(renderedObject.getLabelY()), MapUtils.get31LongitudeX(renderedObject.getLabelX()));
        } else if (!renderedObject.getX().isEmpty()) {
            latLon = new LatLon(MapUtils.get31LatitudeY(renderedObject.getY().get(0)), MapUtils.get31LongitudeX(renderedObject.getX().get(0)));
        } else {
            callbackWithAmenity.processResult(null);
            return;
        }
        final LatLon finalLatLon = latLon;
        singleThreadedExecutor.submit(() -> {
            String wikidata = renderedObject.getTagValue(Amenity.WIKIDATA);
            Amenity amenity = fullAmenitySearch.findAmenity(finalLatLon, renderedObject.getId(), null, wikidata);
            mainThreadExecutor.execute(() -> callbackWithAmenity.processResult(amenity));
        });
    }

}
