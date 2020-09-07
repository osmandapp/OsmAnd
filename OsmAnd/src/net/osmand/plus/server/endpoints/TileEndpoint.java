package net.osmand.plus.server.endpoints;

import android.graphics.Bitmap;

import fi.iki.elonen.NanoHTTPD;

import net.osmand.PlatformUtil;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.server.OsmAndHttpServer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Scanner;

import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

public class TileEndpoint implements OsmAndHttpServer.ApiEndpoint, OsmandMapTileView.IMapOnImageDrawn {
	private static final int TILE_SIZE_PX = 512;
	private static final int TIMEOUT_STEP = 500;
	private static final int TIMEOUT = 5000;

	private static final Log LOG = PlatformUtil.getLog(TileEndpoint.class);
	private final MapActivity mapActivity;
	private RotatedTileBox resultBmpViewport;
	private Bitmap resultBitmap;

	public TileEndpoint(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
	}

	@Override
	public void onDraw(RotatedTileBox viewport, Bitmap bmp) {
		this.resultBmpViewport = viewport;
		this.resultBitmap = bmp;
	}

	@Override
	public NanoHTTPD.Response process(NanoHTTPD.IHTTPSession session, String url) {
		this.mapActivity.getMapView().setOnImageDrawnListener(this);
		// https://tile.osmand.net/hd/6/55/25.png
		int extInd = url.indexOf('.');
		if(extInd >= 0) {
			url = url.substring(0, extInd);
		}
		String[] prms = url.split("/");
		if(prms.length < 4) {
			return OsmAndHttpServer.ErrorResponses.response500;
		}
		int zoom = Integer.parseInt(prms[1]);
		int x = Integer.parseInt(prms[2]);
		int y = Integer.parseInt(prms[3]);
		Bitmap bitmap = requestTile(x, y, zoom);
		if (bitmap == null) {
			return OsmAndHttpServer.ErrorResponses.response500;
		}
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
		byte[] byteArray = stream.toByteArray();
		ByteArrayInputStream str = new ByteArrayInputStream(byteArray);
		this.mapActivity.getMapView().setOnImageDrawnListener(null);
		return newFixedLengthResponse(
				NanoHTTPD.Response.Status.OK, "image/png",
				str, str.available());
	}

	private synchronized Bitmap requestTile(int x, int y, int zoom) {
		this.resultBitmap = null;
		double lat = MapUtils.getLatitudeFromTile(zoom, y);
		double lon = MapUtils.getLongitudeFromTile(zoom, x);
		final RotatedTileBox rotatedTileBox = new RotatedTileBox.RotatedTileBoxBuilder()
				.setLocation(lat, lon)
				.setZoom(zoom)
				.setPixelDimensions(TILE_SIZE_PX, TILE_SIZE_PX, 0.5f, 0.5f).build();
		mapActivity.getMapView().setCurrentViewport(rotatedTileBox);
		int timeout = 0;
		try {
			while (this.resultBitmap != null && timeout < TIMEOUT) {
				Thread.sleep(TIMEOUT_STEP);
				timeout += TIMEOUT_STEP;
			}
			Bitmap res = resultBitmap;
			this.resultBitmap = null;
			return res;
		} catch (InterruptedException e) {
			LOG.error(e);
		}
		return null;
	}
}
