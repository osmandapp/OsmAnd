package net.osmand.plus.track.clickable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.SelectedGpxPoint;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.WptPt;

public class ClickableWay {
    private final long osmId;
    private final String name;
    private final GpxFile gpxFile;
    private final SelectedGpxPoint selectedGpxPoint;

    public ClickableWay(@NonNull GpxFile gpxFile, long osmId, @Nullable String name,
                        @NonNull LatLon selectedPointCoordinates) {
        this.gpxFile = gpxFile;
        this.osmId = osmId;
        this.name = name;
        WptPt selectedPoint = new WptPt();
        selectedPoint.setLat(selectedPointCoordinates.getLatitude());
        selectedPoint.setLon(selectedPointCoordinates.getLongitude());
        selectedGpxPoint = new SelectedGpxPoint(null, selectedPoint);
    }

    public SelectedGpxPoint getSelectedGpxPoint() {
        return selectedGpxPoint;
    }

    public String getWayName() {
        return name != null ? (name + "(" + osmId + ")") : Long.toString(osmId); // TODO get back plain name
    }

    public String toString() {
        return getWayName();
    }
}