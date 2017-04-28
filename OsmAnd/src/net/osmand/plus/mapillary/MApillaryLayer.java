package net.osmand.plus.mapillary;

import android.graphics.Canvas;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;

public class MapillaryLayer extends MapTileLayer {

	public MapillaryLayer() {
		super(false);
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings drawSettings) {
		super.onPrepareBufferImage(canvas, tileBox, drawSettings);
		// todo add 
	}
}
