package net.osmand.plus.views.layers;

import android.content.Context;
import android.graphics.Canvas;

import androidx.annotation.NonNull;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.helpers.CoordinatesGridHelper;
import net.osmand.plus.views.layers.base.OsmandMapLayer;

public class CoordinatesGridLayer extends OsmandMapLayer {

	private final CoordinatesGridHelper gridHelper;
	private Boolean cachedNightMode;

	public CoordinatesGridLayer(@NonNull Context ctx) {
		super(ctx);
		OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
		gridHelper = app.getOsmandMap().getMapView().getGridHelper();
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		// do nothing
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (cachedNightMode == null || cachedNightMode != settings.isNightMode()) {
			cachedNightMode = settings.isNightMode();
			gridHelper.updateGridSettings();
		}
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}
}