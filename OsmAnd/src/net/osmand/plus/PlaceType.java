package net.osmand.plus;

public class PlaceType {
    String poiFilterName;
    String poiFilterId;
    String icon;

    public PlaceType(String poiFilterName, String poiFilterId) {
        this.poiFilterName = poiFilterName;
        this.poiFilterId = poiFilterId;
    }

    public String getPoiFilterName() {
        return poiFilterName;
    }

    public String getPoiFilterId() {
        return poiFilterId;
    }
}


