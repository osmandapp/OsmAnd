package net.osmand.plus.track.clickable;

import net.osmand.data.LatLon;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.SelectedGpxPoint;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.WptPt;

import java.util.Map;

public class ClickableWay {
    private final long osmId;
    private String name = "test_name"; // TODO
    private GpxFile gpxFile = null; // TODO
    private final Map<String, String> tags;
    private final SelectedGpxPoint selectedGpxPoint;

    public ClickableWay(long osmId, Map<String, String> tags, LatLon coordinates) {
        this.osmId = osmId;
        this.tags = tags;
        WptPt selectedPoint = new WptPt();
        selectedPoint.setLat(coordinates.getLatitude());
        selectedPoint.setLon(coordinates.getLongitude());
        selectedGpxPoint = new SelectedGpxPoint(null, selectedPoint);
    }

    public SelectedGpxPoint getSelectedGpxPoint() {
        return selectedGpxPoint;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return getName();
    }
}
