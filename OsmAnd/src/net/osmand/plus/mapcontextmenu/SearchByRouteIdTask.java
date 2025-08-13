package net.osmand.plus.mapcontextmenu;

import android.os.AsyncTask;

import androidx.annotation.Nullable;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.search.AmenitySearcher;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class SearchByRouteIdTask extends AsyncTask<Void, Void, List<Amenity>> {

    private final String routeId;
    private final String routeMembersIds;
    private final SearchByRouteIdListener listener;

    private final SearchType searchType;
    private final OsmandApplication app;

    private final Amenity amenity;

    public enum SearchType {
        RELATED,
        PART_OF,
        MEMBERS
    }

    public interface SearchByRouteIdListener {
        void onFinish(List<Amenity> amenities);
    }

    protected SearchByRouteIdTask(@Nullable Amenity amenity, SearchType type, OsmandApplication app, SearchByRouteIdListener listener) {
        this.listener = listener;
        if (amenity != null) {
            routeId = amenity.getAdditionalInfo(Amenity.ROUTE_ID);
            routeMembersIds = amenity.getAdditionalInfo(Amenity.ROUTE_MEMBERS_IDS);
        } else {
            routeId = null;
            routeMembersIds = null;
        }
        searchType = type;
        this.app = app;
        this.amenity = amenity;
    }

    public SearchByRouteIdTask(String routeId, String routeMembersIds, SearchType type, OsmandApplication app, SearchByRouteIdListener listener) {
        this.routeId = routeId;
        this.routeMembersIds = routeMembersIds;
        this.listener = listener;
        this.searchType = type;
        this.app = app;
        this.amenity = null;
    }

    @Override
    protected List<Amenity> doInBackground(Void... params) {
        List<Amenity> amenities = new ArrayList<>();
        AmenitySearcher amenitySearcher = app.getResourceManager().getAmenitySearcher();
        switch (searchType) {
            case MEMBERS:
                if (!Algorithms.isEmpty(routeMembersIds)) {
                    Map<String, List<Amenity>> members = amenitySearcher.searchRouteMembers(routeMembersIds);
                    for (Map.Entry<String, List<Amenity>> entry : members.entrySet()) {
                        List<Amenity> val = entry.getValue();
                        if (!Algorithms.isEmpty(val)) {
                            amenities.add(val.get(0));
                        }
                    }
                }
                break;
            case RELATED:
                if (!Algorithms.isEmpty(routeId)) {
                    Map<String, List<Amenity>> related = amenitySearcher.searchRouteMembers(routeId);
                    List<Amenity> list = new ArrayList<>();
                    for (Map.Entry<String, List<Amenity>> entry : related.entrySet()) {
                        List<Amenity> val = entry.getValue();
                        if (!Algorithms.isEmpty(val)) {
                            list.addAll(val);
                        }
                    }
                    HashSet<LatLon> latLonHashSet = new HashSet<>();
                    for (Amenity am : list) {
                        LatLon l = am.getLocation();
                        if (!latLonHashSet.contains(l)) {
                            if (amenity == null || !Algorithms.objectEquals(am.getId(), amenity.getId())) {
                                amenities.add(am);
                            }
                        }
                        latLonHashSet.add(l);
                    }
                }
                break;
            case PART_OF:
                if (!Algorithms.isEmpty(routeId)) {
                    List<Amenity> list =  amenitySearcher.searchRoutePartOf(routeId);
                    HashSet<String> routeIdHash = new HashSet<>();
                    for (Amenity am : list) {
                        String routeId = am.getAdditionalInfo(Amenity.ROUTE_ID);
                        if (!routeIdHash.contains(routeId)) {
                            amenities.add(am);
                        }
                        routeIdHash.add(routeId);
                    }
                }
                break;
        }
        return amenities;
    }


    @Override
    protected void onPostExecute(List<Amenity> amenities) {
        if (listener != null) {
            listener.onFinish(amenities);
        }
    }
}
