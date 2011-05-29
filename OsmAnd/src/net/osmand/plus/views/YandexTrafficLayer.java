package net.osmand.plus.views;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import net.osmand.Algoritms;
import net.osmand.LogUtil;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;

import org.apache.commons.logging.Log;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.widget.Toast;

public class YandexTrafficLayer extends MapTileLayer {

	private final static Log log = LogUtil.getLog(MapTileLayer.class);
	private final static String YANDEX_PREFFIX = ".YandexTraffic_";
	private static final long DELTA = 10 * 60 * 1000;
	
	private long lastTimestampUpdated;
	private String mTimestamp = null;
	private boolean updateThreadRan = false;
		
	public void setVisible(boolean visible) {
		if(isVisible() != visible){
			if(visible){
				Toast.makeText(view.getContext(), R.string.thanks_yandex_traffic, Toast.LENGTH_LONG).show();
			} 
			super.setVisible(visible);
			
		}
	}
	
	@Override
	public void onDraw(Canvas canvas, RectF latlonRect, RectF tilesRect, boolean nightMode) {
		updateTimeStamp();
		super.onDraw(canvas, latlonRect, tilesRect, nightMode);
	}
	
	protected void updateTimeStamp() {
		if ((mTimestamp == null || (System.currentTimeMillis() - lastTimestampUpdated) > DELTA) && !updateThreadRan) {
			updateThreadRan = true;
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						updateTimeStampImpl();
					} finally {

					}

				}
			}, "UpdateYandexTraffic").start();
		}
	}

	protected void updateTimeStampImpl() {
		if (mTimestamp == null || (System.currentTimeMillis() - lastTimestampUpdated) > DELTA) {
			log.info("Updating timestamp"); //$NON-NLS-1$
			try {
				BufferedInputStream in = new BufferedInputStream(new URL("http://jgo.maps.yandex.net/trf/stat.js").openStream(), 1024); //$NON-NLS-1$
				ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
				BufferedOutputStream out = new BufferedOutputStream(dataStream, 1024);
				Algoritms.streamCopy(in, out);
				out.flush();
				String str = dataStream.toString();
				// JSONObject json = new JSONObject(str.replace("YMaps.TrafficLoader.onLoad(\"stat\",", "").replace("});", "}"));
				int start = str.indexOf("timestamp:"); //$NON-NLS-1$
				start = str.indexOf("\"", start) + 1; //$NON-NLS-1$
				int end = str.indexOf("\"", start); //$NON-NLS-1$
				// exception case
				if (start < 0 || end < 0) {
					return;
				}
				String newTimestamp = str.substring(start, end);
				lastTimestampUpdated = System.currentTimeMillis();
				Algoritms.closeStream(in);
				Algoritms.closeStream(out);
				log.info("Timestamp updated"); //$NON-NLS-1$
				if (!newTimestamp.equals(mTimestamp)) {
					mTimestamp = newTimestamp;
					TileSourceTemplate template = new TileSourceTemplate(YANDEX_PREFFIX + mTimestamp,
							"http://jgo.maps.yandex.net/tiles?l=trf&x={1}&y={2}&z={0}&tm=" + mTimestamp, ".png", 17, 7, 256, 8, 18000);
					template.setEllipticYTile(true);
					clearCache();
					this.map = template;
				}
			} catch (IOException e) {
				log.info("Exception while updating yandex traffic template", e);
			}
		}
	}

	@Override
	public void destroyLayer() {
		super.destroyLayer();
		clearCache();
	}

	private void clearCache() {
		File dir = view.getSettings().extendOsmandPath(ResourceManager.TILES_PATH);
		for (File ds : dir.listFiles()) {
			if (ds.isDirectory() && ds.getName().startsWith(YANDEX_PREFFIX)) {
				Algoritms.removeAllFiles(ds);
			}
		}

	}
}
