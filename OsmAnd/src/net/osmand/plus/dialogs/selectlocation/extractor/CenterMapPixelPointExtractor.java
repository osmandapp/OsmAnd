package net.osmand.plus.dialogs.selectlocation.extractor;

import android.graphics.PointF;

import androidx.annotation.NonNull;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.views.OsmandMapTileView;

public class CenterMapPixelPointExtractor implements IMapLocationExtractor<PointF> {
	@Override
	public PointF extractLocation(@NonNull OsmandApplication app) {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		RotatedTileBox tileBox = mapView.getRotatedTileBox();
		return new PointF(tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
	}
}
