package net.osmand.plus.server.endpoints;

import android.graphics.Bitmap;
import fi.iki.elonen.NanoHTTPD;
import net.osmand.PlatformUtil;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.resources.AsyncLoadingThread;
import net.osmand.plus.server.ApiEndpoint;
import net.osmand.plus.server.OsmAndHttpServer;
import org.apache.commons.logging.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

public class TileEndpoint implements ApiEndpoint {
	private static final int RENDER_WAIT_THRESHOLD = 5000;
	private static final Object lock = new Object();
	Map<RotatedTileBox, Bitmap> hashMap = new HashMap<>();
	Map<RotatedTileBox, Bitmap> map = Collections.synchronizedMap(hashMap);
	OsmandApplication application;

	public TileEndpoint(OsmandApplication application) {
		this.application = application;
	}

	@Override
	public NanoHTTPD.Response process(NanoHTTPD.IHTTPSession session) {
		synchronized (lock) {
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
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
			byte[] byteArray = stream.toByteArray();
			ByteArrayInputStream str = new ByteArrayInputStream(byteArray);
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

	private Bitmap requestTile(double lat, double lon, int zoom) {
		final RotatedTileBox rotatedTileBox = new RotatedTileBox.RotatedTileBoxBuilder()
				.setLocation(lat, lon)
				.setZoom(zoom)
				.setPixelDimensions(512, 512, 0.5f, 0.5f).build();
		final MapRenderRepositories renderer = application.getResourceManager().getRenderer();
		application.getResourceManager().updateRendererMap(rotatedTileBox, new AsyncLoadingThread.OnMapLoadedListener() {
			@Override
			public void onMapLoaded(boolean interrupted) {
				map.put(rotatedTileBox, renderer.getBitmap());
			}
		});
		Bitmap bmp;
		int sleepTime = 500;
		while ((bmp = map.get(rotatedTileBox)) == null) {
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			sleepTime += 500;
			if (sleepTime > RENDER_WAIT_THRESHOLD) {
				break;
			}
		}
		return bmp;
	}
}
