package net.osmand.plus.server.endpoints;

import android.graphics.*;
import androidx.annotation.GuardedBy;
import fi.iki.elonen.NanoHTTPD;
import net.osmand.PlatformUtil;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.resources.AsyncLoadingThread;
import net.osmand.plus.server.OsmAndHttpServer;
import org.apache.commons.logging.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

public class TileEndpoint implements OsmAndHttpServer.ApiEndpoint {
	private static final int TIMEOUT_STEP = 150;
	private static final int TIMEOUT = 15000;
	private static final Log LOG = PlatformUtil.getLog(TileEndpoint.class);
	@GuardedBy("this")
	//todo cancel on zoom
	private static int lastZoom = -999;
	//TODO restore mapState on Exit
	private final RotatedTileBox mapTileBoxCopy;
	private final MapActivity mapActivity;
	private final MetaTileFileSystemCache cache;

	public TileEndpoint(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		this.cache = new MetaTileFileSystemCache(mapActivity.getMyApplication());
		this.mapTileBoxCopy = mapActivity.getMapView().getCurrentRotatedTileBox().copy();
		//for debug
		this.cache.clearCache();
	}

	@Override
	public NanoHTTPD.Response process(NanoHTTPD.IHTTPSession session, String url) {
		// https://tile.osmand.net/hd/6/55/25.png
		LOG.debug("SERVER: STARTED REQUEST");
		int extInd = url.indexOf('.');
		if (extInd >= 0) {
			url = url.substring(0, extInd);
		}
		if (url.charAt(0) == '/') {
			url = url.substring(1);
		}
		String[] prms = url.split("/");
		if (prms.length < 4) {
			return OsmAndHttpServer.ErrorResponses.response500;
		}
		int zoom = Integer.parseInt(prms[1]);
		int x = Integer.parseInt(prms[2]);
		int y = Integer.parseInt(prms[3]);
		MetaTileFileSystemCache.MetaTileCache res = cache.get(zoom, x, y);
		if (res == null) {
			res = requestMetatile(x, y, zoom);
			if (res == null) {
				LOG.error("SERVER: Cannot request metatile");
				return OsmAndHttpServer.ErrorResponses.response500;
			}
		}

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		Bitmap bmp = res.getSubtile(x, y);
		if (bmp == null) {
			LOG.error("SERVER: Cannot cut bitmap");
			return OsmAndHttpServer.ErrorResponses.response500;
		}
		bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
		byte[] byteArray = stream.toByteArray();
		ByteArrayInputStream str = new ByteArrayInputStream(byteArray);
		return newFixedLengthResponse(
				NanoHTTPD.Response.Status.OK, "image/png",
				str, str.available());
	}

	private synchronized MetaTileFileSystemCache.MetaTileCache requestMetatile(int x, int y, int zoom) {
		long time2 = System.currentTimeMillis();
		MetaTileFileSystemCache.MetaTileCache cacheTile = this.cache.get(zoom, x, y);
		if (cacheTile != null) {
			return cacheTile;
		}
		MetaTileFileSystemCache.MetaTileCache res = cache.createMetaTile(zoom, x, y);
		mapActivity.getMapView().setCurrentViewport(res.bbox);
		int timeout = 0;
		try {
			AsyncLoadingThread athread = mapActivity.getMyApplication().getResourceManager().getAsyncLoadingThread();
			Thread.sleep(TIMEOUT_STEP); // TODO line should be removed in future
			while (athread.areResourcesLoading() && timeout < TIMEOUT) {
				Thread.sleep(TIMEOUT_STEP);
				timeout += TIMEOUT_STEP;
			}
			Bitmap tempBmp = mapActivity.getMapView().getBufferBitmap();
			if (timeout >= TIMEOUT) {
				Canvas canvas = new Canvas(tempBmp);
				Paint paint = new Paint();
				paint.setColor(Color.RED);
				paint.setTextSize(12);
				paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
				canvas.drawText("TIMEOUT", tempBmp.getWidth() / 2, tempBmp.getHeight() / 2, paint);
				// here we could return stub
			}
			res.bmp = tempBmp.copy(tempBmp.getConfig(), true);
			this.cache.put(res);
			LOG.debug("SERVER: TIME TO REQUEST TILE: " + (System.currentTimeMillis() - time2));
			return res;
		} catch (InterruptedException e) {
			LOG.error(e);
		}
		return null;
	}
}
