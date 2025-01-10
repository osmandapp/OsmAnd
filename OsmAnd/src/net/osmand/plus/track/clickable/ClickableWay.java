package net.osmand.plus.track.clickable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.SelectedGpxPoint;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.WptPt;

import java.util.Map;

public class ClickableWay {
    private final long osmId;
    private final String name;
    private GpxFile gpxFile = null; // TODO
    private final Map<String, String> tags;
    private final SelectedGpxPoint selectedGpxPoint;

    public ClickableWay(long osmId, @Nullable String name, @NonNull Map<String, String> tags,
                        @NonNull LatLon selectedPointCoordinates) {
        this.osmId = osmId;
        this.name = name;
        this.tags = tags;
        WptPt selectedPoint = new WptPt();
        selectedPoint.setLat(selectedPointCoordinates.getLatitude());
        selectedPoint.setLon(selectedPointCoordinates.getLongitude());
        selectedGpxPoint = new SelectedGpxPoint(null, selectedPoint);
    }

    public SelectedGpxPoint getSelectedGpxPoint() {
        return selectedGpxPoint;
    }

    public String getWayName() {
        return name != null ? name : Long.toString(osmId);
    }

    public String toString() {
        return getWayName();
    }
}
