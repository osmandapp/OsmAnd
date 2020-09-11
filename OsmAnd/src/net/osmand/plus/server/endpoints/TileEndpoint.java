package net.osmand.plus.server.endpoints;

import android.graphics.*;
import androidx.annotation.GuardedBy;
import fi.iki.elonen.NanoHTTPD;
import net.osmand.PlatformUtil;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.resources.AsyncLoadingThread;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.server.OsmAndHttpServer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.MapUtils;
import org.apache.commons.logging.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

public class TileEndpoint implements OsmAndHttpServer.ApiEndpoint {
	private static final int TIMEOUT_STEP = 150;
	private static final int TIMEOUT = 15000;
	private static final Log LOG = PlatformUtil.getLog(TileEndpoint.class);

	private final RotatedTileBox mapTileBoxCopy;
	private final OsmAndHttpServer server;
	private final MetaTileFileSystemCache cache;
	private int lastRequestedZoom;

	public TileEndpoint(OsmAndHttpServer server) {
		this.server = server;
		this.cache = new MetaTileFileSystemCache(server.getMyApplication());
		this.mapTileBoxCopy = server.getMapActivity().getMapView().getCurrentRotatedTileBox().copy();
		this.cache.clearCache();
	}



	@Override
	public NanoHTTPD.Response process(NanoHTTPD.IHTTPSession session, String url) {
		// https://tile.osmand.net/hd/6/55/25.png
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
			lastRequestedZoom = zoom;
			res = requestMetatile(x, y, zoom);
			if (res == null) {
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
		long tm = System.currentTimeMillis();
		MapActivity mapActivity = server.getMapActivity();
		ResourceManager resourceManager = mapActivity.getMyApplication().getResourceManager();
		if (mapActivity == null) {
			return null;
		}
		MetaTileFileSystemCache.MetaTileCache cacheTile = this.cache.get(zoom, x, y);
		if (cacheTile != null) {
			return cacheTile;
		}
		MetaTileFileSystemCache.MetaTileCache res = cache.createMetaTile(zoom, x, y);
		mapActivity.getMapView().setCurrentViewport(res.bbox);
		int timeout = 0;
		try {
			AsyncLoadingThread athread = resourceManager.getAsyncLoadingThread();
			resourceManager.updateRendererMap(res.bbox, null);
			Thread.sleep(TIMEOUT_STEP); // to do line should be removed in future
			// wait till all resources rendered and loaded
			while (athread.areResourcesLoading() && timeout < TIMEOUT) {
				if(lastRequestedZoom != zoom) {
					return null;
				}
				Thread.sleep(TIMEOUT_STEP);
				timeout += TIMEOUT_STEP;
			}
			mapActivity.getMapView().refreshMap();
			// wait for image to be refreshed
			while(!resourceManager.getRenderingBufferImageThread().getLooper().getQueue().isIdle() &&
					timeout < TIMEOUT) {
				Thread.sleep(TIMEOUT_STEP);
				timeout += TIMEOUT_STEP;
			}
			Bitmap rbmp = mapActivity.getMapView().getBufferBitmap();
			if (timeout >= TIMEOUT) {
				return null;
//				Canvas canvas = new Canvas(tempBmp);
//				Paint paint = new Paint();
//				paint.setColor(Color.RED);
//				paint.setTextSize(12);
//				paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
//				canvas.drawText("TIMEOUT", tempBmp.getWidth() / 2, tempBmp.getHeight() / 2, paint);
				// here we could return stub
			}
			res.bmp = rbmp.copy(rbmp.getConfig(), true);
			this.cache.put(res);
			LOG.debug("Render metatile: " + (System.currentTimeMillis() - tm)/1000.0f);
			return res;
		} catch (InterruptedException e) {
			LOG.error(e);
		}
		return null;
	}
}
