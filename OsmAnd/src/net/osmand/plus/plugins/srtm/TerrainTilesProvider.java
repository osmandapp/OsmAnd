package net.osmand.plus.plugins.srtm;

import net.osmand.core.android.TileSourceProxyProvider;
import net.osmand.core.jni.ZoomLevel;
import net.osmand.map.ITileSource;
import net.osmand.plus.OsmandApplication;

import androidx.annotation.NonNull;

public class TerrainTilesProvider extends TileSourceProxyProvider {

	private final SRTMPlugin srtmPlugin;

	public TerrainTilesProvider(@NonNull OsmandApplication app,
	                            @NonNull ITileSource tileSource,
	                            @NonNull SRTMPlugin srtmPlugin) {
		super(app, tileSource);
		this.srtmPlugin = srtmPlugin;
	}

	@Override
	public ZoomLevel getMinVisibleZoom() {
		return ZoomLevel.swigToEnum(srtmPlugin.getTerrainMinZoom());
	}

	@Override
	public ZoomLevel getMaxVisibleZoom() {
		return ZoomLevel.swigToEnum(srtmPlugin.getTerrainMaxZoom());
	}
}