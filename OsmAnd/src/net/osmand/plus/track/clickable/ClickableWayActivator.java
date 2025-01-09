package net.osmand.plus.track.clickable;

import android.graphics.PointF;

import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer;
import net.osmand.shared.gpx.primitives.WptPt;

import java.util.List;

public class ClickableWayActivator implements ContextMenuLayer.IContextMenuProvider {
    private OsmandApplication app;
    private OsmandMapTileView view;

    public ClickableWayActivator(OsmandApplication app, OsmandMapTileView view) {
        this.app = app;
        this.view = view;
        // could be derived from OsmandMapLayer(ctx) in case of necessity...
    }

    @Override
    public boolean showMenuAction(@Nullable Object object) {
        if (object != null && object instanceof ClickableWay) {
            System.err.printf("XXX ClickableWayActivator(%s)\n", object);
            // TODO GpxUiHelper.saveAndOpenGpx(...)
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
        String name = ((ClickableWay) o).getName();
        return new PointDescription(PointDescription.POINT_TYPE_GPX, name);
    }

    @Override
    public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o, boolean unknownLocation, boolean excludeUntouchableObjects) {
    }
}
