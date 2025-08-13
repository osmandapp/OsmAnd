package net.osmand.plus.track.clickable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.binary.HeightDataLoader.CancellableCallback;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer;
import net.osmand.plus.views.layers.MapSelectionResult;
import net.osmand.shared.gpx.primitives.WptPt;

public class ClickableWayMenuProvider implements ContextMenuLayer.IContextMenuProvider {
    private final OsmandMapTileView view;
    private final CancellableCallback<ClickableWay> readHeightData;
    private final CallbackWithObject<ClickableWay> openAsGpxFile;

    public ClickableWayMenuProvider(@NonNull OsmandMapTileView view,
                                    @NonNull CancellableCallback<ClickableWay> readHeightData,
                                    @NonNull CallbackWithObject<ClickableWay> openAsGpxFile) {
        this.view = view;
        this.readHeightData = readHeightData;
        this.openAsGpxFile = openAsGpxFile;
        // could be derived from OsmandMapLayer(ctx) in case of necessity
    }

    @Override
    public boolean showMenuAction(@Nullable Object object) {
        if (object instanceof ClickableWay that) {
            MapActivity mapActivity = view.getMapActivity();
            if (mapActivity != null) {
                OsmAndTaskManager.executeTask(new ClickableWayAsyncTask(mapActivity, that, readHeightData, openAsGpxFile));
                return true;
            }
        }
        return false;
    }

    @Override
    public LatLon getObjectLocation(@NonNull Object o) {
        WptPt wpt = ((ClickableWay) o).getSelectedGpxPoint().getSelectedPoint();
        return new LatLon(wpt.getLatitude(), wpt.getLongitude());
    }

    @NonNull
    @Override
    public PointDescription getObjectName(@NonNull Object o) {
        String name = ((ClickableWay) o).getWayName();
        return new PointDescription(PointDescription.POINT_TYPE_GPX, name);
    }

    @Override
    public void collectObjectsFromPoint(@NonNull MapSelectionResult result, boolean unknownLocation, boolean excludeUntouchableObjects) {
    }
}
