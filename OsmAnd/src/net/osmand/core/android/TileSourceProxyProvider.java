package net.osmand.core.android;

import java.io.IOException;

import net.osmand.IndexConstants;
import net.osmand.core.jni.AlphaChannelPresence;
import net.osmand.core.jni.MapStubStyle;
import net.osmand.core.jni.SWIGTYPE_p_QByteArray;
import net.osmand.core.jni.SwigUtilities;
import net.osmand.core.jni.TileId;
import net.osmand.core.jni.ZoomLevel;
import net.osmand.core.jni.interface_ImageMapLayerProvider;
import net.osmand.map.ITileSource;
import net.osmand.plus.OsmandApplication;

public class TileSourceProxyProvider extends interface_ImageMapLayerProvider {

	private final OsmandApplication app;
	private final ITileSource tileSource;
	
	public TileSourceProxyProvider(OsmandApplication app, ITileSource tileSource) {
		this.app = app;
		this.tileSource = tileSource;
	}

	@Override
	public MapStubStyle getDesiredStubsStyle() {
		return MapStubStyle.Unspecified;
	}
	
	@Override
	public ZoomLevel getMinZoom() {
		return ZoomLevel.swigToEnum(tileSource.getMinimumZoomSupported());
	}
	
	@Override
	public ZoomLevel getMaxZoom() {
		return ZoomLevel.swigToEnum(tileSource.getMaximumZoomSupported());
	}

	@Override
	public SWIGTYPE_p_QByteArray obtainImage(TileId tileId, ZoomLevel zoom) {
		byte[] image;
		try {
			image = tileSource.getBytes(tileId.getX(), tileId.getY(), zoom.swigValue(),
					app.getAppPath(IndexConstants.TILES_INDEX_DIR).getAbsolutePath());
		} catch(IOException e) {
			return SwigUtilities.emptyQByteArray();
		}
		if (image == null)
			return SwigUtilities.emptyQByteArray();

		return SwigUtilities.createQByteArrayAsCopyOf(image);
	}
	
	@Override
	public long getTileSize() {
		return tileSource.getTileSize();
	}
	
	@Override
	public float getTileDensityFactor() {
		return 1.0f;
	}
	
	@Override
	public AlphaChannelPresence getAlphaChannelPresence() {
		return AlphaChannelPresence.Unknown;
	}
}
