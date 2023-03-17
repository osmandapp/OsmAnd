package net.osmand.plus.views;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLConnection;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.data.RotatedTileBox;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.views.layers.MapTileLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import android.graphics.Canvas;

public class YandexTrafficAdapter  extends MapTileAdapter {

	private static final Log log = PlatformUtil.getLog(MapTileLayer.class);
	private static final String YANDEX_PREFFIX = ".YandexTraffic_";
	private static final String YANDEX_BASE_URL = "https://core-jams-rdr.maps.yandex.net";
	private static final long DELTA = 10 * 60 * 1000;
	
	private long lastTimestampUpdated;
	private String mTimestamp;
	private boolean updateThreadRan;
	
	
	@Override
	public void onInit() {
	}
	
	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, OsmandMapLayer.DrawSettings drawSettings) {
		updateTimeStamp();
	}
	
	protected void updateTimeStamp() {
		if ((mTimestamp == null || (System.currentTimeMillis() - lastTimestampUpdated) > DELTA) && !updateThreadRan) {
			updateThreadRan = true;
			new Thread(this::updateTimeStampImpl, "UpdateYandexTraffic").start();
		}
	}

	protected void updateTimeStampImpl() {
		if (mTimestamp == null || (System.currentTimeMillis() - lastTimestampUpdated) > DELTA) {
			log.info("Updating timestamp"); //$NON-NLS-1$
			try {
				URLConnection connection = NetworkUtils.getHttpURLConnection(YANDEX_BASE_URL + "/trf/stat.js");
				BufferedInputStream in = new BufferedInputStream(connection.getInputStream(), 1024); //$NON-NLS-1$
				ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
				BufferedOutputStream out = new BufferedOutputStream(dataStream, 1024);
				Algorithms.streamCopy(in, out);
				out.flush();
				String str = dataStream.toString();
				// JSONObject json = new JSONObject(str.replace("YMaps.TrafficLoader.onLoad(\"stat\",", "").replace("});", "}"));
				int start = str.indexOf("timestamp:"); //$NON-NLS-1$
				start = str.indexOf('\"', start) + 1; //$NON-NLS-1$
				int end = str.indexOf('\"', start); //$NON-NLS-1$
				// exception case
				if (start < 0 || end < 0) {
					log.info("Timestamp wasn't updated " + str); //$NON-NLS-1$
					return;
				}
				String newTimestamp = str.substring(start, end);
				lastTimestampUpdated = System.currentTimeMillis();
				Algorithms.closeStream(in);
				Algorithms.closeStream(out);
				log.info("Timestamp updated to " + newTimestamp); //$NON-NLS-1$
				if (!newTimestamp.equals(mTimestamp)) {
					mTimestamp = newTimestamp;
					TileSourceTemplate template = new TileSourceTemplate(YANDEX_PREFFIX + mTimestamp,
							YANDEX_BASE_URL + "/1.1/tiles?l=trf,trfe&x={1}&y={2}&z={0}&tm=" + mTimestamp, ".png", 17, 7, 256, 8, 18000);
					template.setEllipticYTile(true);
					template.setExpirationTimeMinutes(20);
					clearCache();
					this.layer.setMapForMapTileAdapter(template, this);
				}
			} catch (IOException e) {
				log.info("Exception while updating yandex traffic template", e);
			}
		}
	}

	@Override
	public void onClear() {
		clearCache();
	}

	private void clearCache() {
		File dir = view.getApplication().getAppPath(IndexConstants.TILES_INDEX_DIR);
		for (File ds : dir.listFiles()) {
			if (ds.isDirectory() && ds.getName().startsWith(YANDEX_PREFFIX)) {
				Algorithms.removeAllFiles(ds);
			}
		}

	}
}
