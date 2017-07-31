package net.osmand.plus.measurementtool;

import android.graphics.Canvas;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;

public class MeasurementToolLayer extends OsmandMapLayer {

	private boolean inMeasurementMode;

	public boolean isInMeasurementMode() {
		return inMeasurementMode;
	}

	public void setInMeasurementMode(boolean inMeasurementMode) {
		this.inMeasurementMode = inMeasurementMode;
	}

	@Override
	public void initLayer(OsmandMapTileView view) {

	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {

	}

	@Override
	public void destroyLayer() {

	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}
}
