package net.osmand.plus.server.endpoints;

import android.graphics.Bitmap;
import androidx.fragment.app.FragmentActivity;
import fi.iki.elonen.NanoHTTPD;
import net.osmand.PlatformUtil;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.resources.AsyncLoadingThread;
import net.osmand.plus.server.ApiEndpoint;
import net.osmand.plus.server.IMapOnImageDrawn;
import net.osmand.plus.server.OsmAndHttpServer;
import net.osmand.plus.server.ServerFragment;
import org.apache.commons.logging.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

public class TileEndpoint implements ApiEndpoint, IMapOnImageDrawn {
	private static final int RENDER_WAIT_THRESHOLD = 5000;
	private static final Object lock = new Object();
	private static final int TILE_SIZE_PX = 512;
	private static final Log LOG = PlatformUtil.getLog(TileEndpoint.class);
	Map<RotatedTileBox, Bitmap> hashMap = new HashMap<>();
	Map<RotatedTileBox, Bitmap> map = Collections.synchronizedMap(hashMap);
	OsmandApplication application;
	private RotatedTileBox viewPort;
	private Bitmap resultBitmap;
	private MapActivity mapActivity;

	@Override
	public void onDraw(RotatedTileBox viewport, Bitmap bmp) {
		map.put(viewport, bmp);
	}

	public TileEndpoint(OsmandApplication application, MapActivity mapActivity) {
		this.application = application;
		this.mapActivity = mapActivity;
	}

	@Override
	public NanoHTTPD.Response process(NanoHTTPD.IHTTPSession session) {
		synchronized (lock) {
			this.mapActivity.getMapView().setOnImageDrawnListener(this);
			RotatedTileBox tileBoxCopy = mapActivity.getMapView().getCurrentRotatedTileBox().copy();
			int zoom;
			double lat;
			double lon;
			String fullUri = session.getUri().replace("/tile/", "");
			Scanner s = new Scanner(fullUri).useDelimiter("/");
			zoom = s.nextInt();
			lat = s.nextDouble();
			lon = s.nextDouble();
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
			return newFixedLengthResponse(
					NanoHTTPD.Response.Status.OK,
					"image/png",
					str,
					str.available());
		}
	}

	@Override
	public void setApplication(OsmandApplication application) {
		this.application = application;
	}

	public void setMapActivity(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
	}

	private Bitmap requestTile(double lat, double lon, int zoom) {
		final RotatedTileBox rotatedTileBox = new RotatedTileBox.RotatedTileBoxBuilder()
				.setLocation(lat, lon)
				.setZoom(zoom)
				.setPixelDimensions(TILE_SIZE_PX, TILE_SIZE_PX, 0.5f, 0.5f).build();
		final MapRenderRepositories renderer = application.getResourceManager().getRenderer();
		mapActivity.getMapView().setCurrentViewport(rotatedTileBox);
//		application.getResourceManager().updateRendererMap(rotatedTileBox, new AsyncLoadingThread.OnMapLoadedListener() {
//			@Override
//			public void onMapLoaded(boolean interrupted) {
//				map.put(rotatedTileBox, renderer.getBitmap());
//			}
//		});
		Bitmap bmp = null;
		int sleepTime = 500;
		int timeout = 0;


//		try {
//			while ((viewPort == null && !rotatedTileBox.equals(viewPort)) || timeout < 5000) {
//				Thread.sleep(100);
//				timeout += 100;
//			}
//			return resultBitmap;
//		} catch (InterruptedException e) {
//			LOG.error(e);
//		}
		while ((bmp = map.get(rotatedTileBox)) == null) {
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				LOG.error(e);
			}
			sleepTime += 500;
			if (sleepTime > RENDER_WAIT_THRESHOLD) {
				break;
			}
		}
		return bmp;
	}
}
