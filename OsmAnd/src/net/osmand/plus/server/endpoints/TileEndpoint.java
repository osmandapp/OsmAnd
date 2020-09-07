package net.osmand.plus.server.endpoints;

import android.graphics.Bitmap;
import fi.iki.elonen.NanoHTTPD;
import net.osmand.PlatformUtil;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.server.OsmAndHttpServer;
import net.osmand.plus.views.OsmandMapTileView;
import org.apache.commons.logging.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Scanner;

import static fi.iki.elonen.NanoHTTPD.SOCKET_READ_TIMEOUT;
import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

public class TileEndpoint implements OsmAndHttpServer.ApiEndpoint, OsmandMapTileView.IMapOnImageDrawn {
	private static final int TILE_SIZE_PX = 512;
	private static final int TIMEOUT_STEP = 500;
	private static final Log LOG = PlatformUtil.getLog(TileEndpoint.class);
	private RotatedTileBox resultBmpViewport;
	private Bitmap resultBitmap;
	private MapActivity mapActivity;

	public TileEndpoint(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
	}

	@Override
	public void onDraw(RotatedTileBox viewport, Bitmap bmp) {
		this.resultBmpViewport = viewport;
		this.resultBitmap = bmp;
	}

	@Override
	public NanoHTTPD.Response process(NanoHTTPD.IHTTPSession session) {
		this.mapActivity.getMapView().setOnImageDrawnListener(this);
		RotatedTileBox tileBoxCopy = mapActivity.getMapView().getCurrentRotatedTileBox().copy();
		Scanner s = new Scanner(session.getUri()).useDelimiter("/");
		//reading path
		s.next();
		int zoom = s.nextInt();
		double lat = s.nextDouble();
		double lon = s.nextDouble();
		Bitmap bitmap = requestTile(lat, lon, zoom);
		if (bitmap == null) {
			return OsmAndHttpServer.ErrorResponses.response500;
		}
		//stream also needs to be synchronized
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
		byte[] byteArray = stream.toByteArray();
		ByteArrayInputStream str = new ByteArrayInputStream(byteArray);
		mapActivity.getMapView().setCurrentViewport(tileBoxCopy);
		this.mapActivity.getMapView().setOnImageDrawnListener(null);
		return newFixedLengthResponse(
				NanoHTTPD.Response.Status.OK,
				"image/png",
				str,
				str.available());
	}

	private synchronized Bitmap requestTile(double lat, double lon, int zoom) {
		final RotatedTileBox rotatedTileBox = new RotatedTileBox.RotatedTileBoxBuilder()
				.setLocation(lat, lon)
				.setZoom(zoom)
				.setPixelDimensions(TILE_SIZE_PX, TILE_SIZE_PX, 0.5f, 0.5f).build();
		mapActivity.getMapView().setCurrentViewport(rotatedTileBox);
		Bitmap bmp = null;
		int timeout = 0;
		try {
			while (!rotatedTileBox.equals(resultBmpViewport) && timeout < SOCKET_READ_TIMEOUT) {
				Thread.sleep(TIMEOUT_STEP);
				timeout += TIMEOUT_STEP;
			}
			resultBmpViewport = null;
			return resultBitmap;
		} catch (InterruptedException e) {
			LOG.error(e);
		}
		return bmp;
	}
}
