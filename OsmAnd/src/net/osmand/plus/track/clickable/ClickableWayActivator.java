package net.osmand.plus.track.clickable;

import static net.osmand.IndexConstants.GPX_FILE_EXT;

import android.graphics.PointF;

import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.shared.gpx.primitives.WptPt;

import java.io.File;
import java.util.List;

public class ClickableWayActivator implements ContextMenuLayer.IContextMenuProvider {
    private OsmandApplication app;
    private OsmandMapTileView view;

    public ClickableWayActivator(OsmandApplication app, OsmandMapTileView view) {
        this.app = app;
        this.view = view;
        // could be derived from OsmandMapLayer(ctx) in case of necessity
    }

    @Override
    public boolean showMenuAction(@Nullable Object object) {
        if (object instanceof ClickableWay that) {
            MapActivity mapActivity = view.getMapActivity();
            GpxFile gpxFile = that.getGpxFile();
            GpxTrackAnalysis analysis = gpxFile.getAnalysis(0);
            WptPt selectedPoint = that.getSelectedGpxPoint().getSelectedPoint();
            String safeFileName = that.getGpxFileName() + GPX_FILE_EXT;
            File file = new File(FileUtils.getTempDir(app), safeFileName);
            GpxUiHelper.saveAndOpenGpx(mapActivity, file, gpxFile, selectedPoint, analysis, null, true);
            return true;
        }
        return false;
    }

    @Override
    public LatLon getObjectLocation(Object o) {
        WptPt wpt = ((ClickableWay) o).getSelectedGpxPoint().getSelectedPoint();
        return new LatLon(wpt.getLatitude(), wpt.getLongitude());
    }

    @Override
    public PointDescription getObjectName(Object o) {
        String name = ((ClickableWay) o).getWayName();
        return new PointDescription(PointDescription.POINT_TYPE_GPX, name);
    }

    @Override
    public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o, boolean unknownLocation, boolean excludeUntouchableObjects) {
    }
}
