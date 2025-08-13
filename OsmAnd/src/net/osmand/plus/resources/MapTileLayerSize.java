package net.osmand.plus.resources;

import androidx.annotation.NonNull;

import net.osmand.plus.views.layers.MapTileLayer;

public class MapTileLayerSize {

	final MapTileLayer layer;
	Long markToGCTimestamp;
	long activeTimestamp;
	int tiles;

	public MapTileLayerSize(@NonNull MapTileLayer layer, int tiles, long activeTimestamp) {
		this.layer = layer;
		this.tiles = tiles;
		this.activeTimestamp = activeTimestamp;
	}
}
