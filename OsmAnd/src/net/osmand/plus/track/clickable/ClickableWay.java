package net.osmand.plus.track.clickable;

import static net.osmand.shared.gpx.GpxUtilities.ACTIVITY_TYPE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.SelectedGpxPoint;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;

import java.util.Map;

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

    @Nullable
    public String getActivityType() {
        return gpxFile.getMetadata().getExtensionsToRead().get(ACTIVITY_TYPE);
    }

    @NonNull
    public Map<String, String> getGpxTags() {
        return gpxFile.getExtensionsToRead();
    }

    @Override
    public String toString() {
        return getWayName();
    }
}
