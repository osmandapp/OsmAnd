package net.osmand.plus.track.clickable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.SelectedGpxPoint;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;

public class ClickableWay {
    private final long osmId;
    private final String name;
    private final QuadRect bbox;
    private final GpxFile gpxFile;
    private final SelectedGpxPoint selectedGpxPoint;

    public ClickableWay(@NonNull GpxFile gpxFile, long osmId, @Nullable String name,
                        @NonNull LatLon selectedLatLon, @NonNull QuadRect bbox) {
        this.gpxFile = gpxFile;
        this.osmId = osmId;
        this.name = name;
        this.bbox = bbox;

        WptPt wpt = new WptPt();
        wpt.setLat(selectedLatLon.getLatitude());
        wpt.setLon(selectedLatLon.getLongitude());
        this.selectedGpxPoint = new SelectedGpxPoint(null, wpt);
    }

    public long getOsmId() {
        return osmId;
    }

    @NonNull
    public QuadRect getBbox() {
        return bbox;
    }

    @NonNull
    public GpxFile getGpxFile() {
        return gpxFile;
    }

    @NonNull
    public SelectedGpxPoint getSelectedGpxPoint() {
        return selectedGpxPoint;
    }

    @NonNull
    public String getGpxFileName() {
        return Algorithms.sanitizeFileName(getWayName());
    }

    @NonNull
    public String getWayName() {
        if (!Algorithms.isEmpty(name)) {
            return name;
        } else {
            String altName = gpxFile.getExtensionsToRead().get("ref");
            return altName != null ? altName : Long.toString(osmId);
        }
    }

    @Override
    public String toString() {
        return getWayName();
    }
}
