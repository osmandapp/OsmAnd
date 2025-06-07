package net.osmand.plus.dialogs.selectlocation.extractor;

import androidx.annotation.NonNull;

import net.osmand.core.android.MapRendererView;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;

public class CenterMapLatLonExtractor implements IMapLocationExtractor<LatLon> {
	@Override
	public LatLon extractLocation(@NonNull OsmandApplication app) {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		MapRendererView mapRenderer = mapView.getMapRenderer();
		RotatedTileBox tileBox = mapView.getRotatedTileBox();

		int centerX = tileBox.getCenterPixelX();
		int centerY = tileBox.getCenterPixelY();
		return NativeUtilities.getLatLonFromElevatedPixel(mapRenderer, tileBox, centerX, centerY);
	}
}
