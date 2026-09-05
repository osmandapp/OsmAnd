package net.osmand.plus.views;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.views.layers.MapTileLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer;

import android.graphics.Canvas;

/**
 * This class is designed to represent adapter for specific map sources
 * that requires additional computation or updates
 */
public abstract class MapTileAdapter {
	
	protected MapTileLayer layer;
	protected OsmandMapTileView view;

	public void initLayerAdapter(MapTileLayer layer, OsmandMapTileView view){
		this.layer = layer;
		this.view = view;
	}
	
	public abstract void onDraw(Canvas canvas, RotatedTileBox tileBox, OsmandMapLayer.DrawSettings drawSettings);
	
	public abstract void onClear(); 

	public abstract void onInit();
}
