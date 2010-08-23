package net.osmand.views;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.osmand.Algoritms;
import net.osmand.LogUtil;
import net.osmand.R;
import net.osmand.osm.MapUtils;

import org.apache.commons.logging.Log;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.BitmapFactory.Options;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.FloatMath;
import android.widget.Toast;

public class YandexTrafficLayer implements OsmandMapLayer {

	private static final long DELTA = 60000;
	private OsmandMapTileView view;
	private long lastTimestampUpdated;
	private String mTimestamp = null;
	private Rect pixRect;
	private RectF tileRect;
	private boolean visible = false;
	private Handler handler = null;
	private static final Log log = LogUtil.getLog(YandexTrafficLayer.class);
	
	private Map<String, Bitmap> tiles = new LinkedHashMap<String, Bitmap>();
	
	private Rect srcImageRect = new Rect(0, 0, 256, 256);
	private RectF dstImageRect = new RectF();
	 
	protected int cMinX;
	protected int cMaxX;
	protected int cMinY;
	protected int cMaxY;
	protected int cZoom;
	private Paint paint;

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		pixRect = new Rect();
		tileRect = new RectF();
		if(isVisible()){
			startThread();
		}
		paint = new Paint();
		paint.setFilterBitmap(true);
	}
	
	public boolean isVisible() {
		return visible;
	}
		
	public void setVisible(boolean visible) {
		if(this.visible != visible){
			if(visible){
				Toast.makeText(view.getContext(), R.string.thanks_yandex_traffic, Toast.LENGTH_LONG).show();
				startThread();
			} else {
				stopThread();
			}
			this.visible = visible;
			
		}
	}
	
	private synchronized void startThread(){
		if (handler == null) {
			new Thread("Yandex traffic") { //$NON-NLS-1$
				@Override
				public void run() {
					Looper.prepare();
					handler = new Handler();
					Looper.loop();
				}
			}.start();
		}
		
	}
	
	private synchronized void stopThread(){
		if (handler != null) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					Looper.myLooper().quit();
				}
			});
			handler = null;
		}
		
	}
	
	protected void checkedCachedImages(int zoom){
		boolean inside = cMinX <= tileRect.left && tileRect.right <= cMaxX && cMinY <= tileRect.top && tileRect.bottom <= cMaxY ;
		if (!inside || (cZoom != zoom)) {
			cMinX = ((int) tileRect.left);
			cMaxX = ((int) tileRect.right ) + 1;
			cMinY = ((int) tileRect.top);
			cMaxY = ((int) tileRect.bottom ) + 1;
			if (cZoom != zoom) {
				cZoom = zoom;
				clearCache();
			}
			if (handler != null) {
				if (!handler.hasMessages(1)) {
					Message msg = Message.obtain(handler, new Runnable() {
						@Override
						public void run() {
							updateCachedImages(cMinX, cMaxX, cMinY, cMaxY, cZoom);
						}
					});
					msg.what = 1;
					handler.sendMessage(msg);
				}
			}
		}
	}
	
	protected void updateCachedImages(int tMinX, int tMaxX, int tMinY, int tMaxY, int tZoom){
		try {
			updateTimeStamp();
			if (mTimestamp != null) {
				// clear before to save memory
				Set<String> unusedTiles = new HashSet<String>(tiles.keySet());
				for (int i = cMinX; i <= cMaxX; i++) {
					for (int j = cMinY; j <= cMaxY; j++) {
						String tileId = calculateTileId(i, j, cZoom);
						unusedTiles.remove(tileId);
					}
				}
				for(String s : unusedTiles){
					Bitmap bmp = tiles.remove(s);
					if(bmp != null){
						bmp.recycle();
					}
				}
				for (int i = tMinX; i <= tMaxX; i++) {
					for (int j = tMinY; j <= tMaxY; j++) {
						String tileId = calculateTileId(i, j, tZoom);
						if(tiles.get(tileId) == null){
							downloadTile(tileId, i, j, tZoom, mTimestamp);
							if(tMaxX != cMaxX || tMinX!= cMinX || tMaxY != cMaxY || tMinY!= cMinY || tZoom !=cZoom){
								return;
							}
							view.refreshMap();
						}
					}
				}
			}
			
		} catch (IOException e) {
			log.error("IOException", e); //$NON-NLS-1$
		}
	}
	
	private StringBuilder builder = new StringBuilder(50);
	protected synchronized String calculateTileId(int tileX, int tileY, int zoom){
		builder.setLength(0);
		builder.append(zoom).append('/').append(tileX).append('/').append(tileY);
		return builder.toString();
	}
	
	protected void downloadTile(String tileId, int tileX, int tileY, int zoom, String timeStamp) throws IOException{
		if(zoom > 17){
			return;
		}
		String u = "http://jgo.maps.yandex.net/tiles?l=trf&x="+tileX +"&y="+tileY+"&z="+zoom+"&tm="+timeStamp;  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$//$NON-NLS-4$
		long time = System.currentTimeMillis();
		try {
			if (log.isDebugEnabled()) {
				log.debug("Start loading traffic : " + u); //$NON-NLS-1$
			}
			InputStream is = new URL(u).openStream();
			Options opt = new BitmapFactory.Options();
			Bitmap bmp = BitmapFactory.decodeStream(is,null, opt);
			is.close();
			tiles.put(tileId, bmp);
			if (log.isDebugEnabled()) {
				log.debug("Loaded traffic : " + tileId+ " " + (System.currentTimeMillis() - time) +"ms " + tiles.size()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		} catch(IOException e){
			// File not found very often exception
			log.error("Traffic loading failed " + e.getMessage()); //$NON-NLS-1$
		}
	}

	@Override
	public void onDraw(Canvas canvas) {
		if (visible) {
			pixRect.set(0, 0, view.getWidth(), view.getHeight());
			double lat = MapUtils.getLatitudeFromTile(view.getFloatZoom(), view.getYTile());
			float tileY = (float) MapUtils.getTileEllipsoidNumberY(view.getFloatZoom(), lat);
			view.calculateTileRectangle(pixRect, view.getCenterPointX(), view.getCenterPointY(), view.getXTile(), tileY, tileRect);
			double topLat = MapUtils.getLatitudeFromEllipsoidTileY(view.getFloatZoom(), (int) tileRect.top);
			double leftLon = MapUtils.getLongitudeFromTile(view.getFloatZoom(), (int) tileRect.left);
			int x = view.getRotatedMapXForPoint(topLat, leftLon);
			int y = view.getRotatedMapYForPoint(topLat, leftLon);
			checkedCachedImages(view.getZoom());
			float right = FloatMath.ceil(tileRect.right);
			float bottom = FloatMath.ceil(tileRect.bottom);
			for (int i = (int) tileRect.left; i <= right; i++) {
				for (int j = (int) tileRect.top; j <= bottom; j++) {
					String tId = calculateTileId(i, j, view.getZoom());
					if (tiles.get(tId) != null) {
						dstImageRect.top = y + (j - (int) tileRect.top) * view.getTileSize();
						dstImageRect.left = x + (i - (int) tileRect.left) * view.getTileSize();
						dstImageRect.bottom = dstImageRect.top + view.getTileSize();
						dstImageRect.right = dstImageRect.left + view.getTileSize();
						canvas.drawBitmap(tiles.get(tId), srcImageRect, dstImageRect, paint);
					}
				}
			}
		}
	}
	
	protected void updateTimeStamp() throws IOException {
		if (mTimestamp == null || (System.currentTimeMillis() - lastTimestampUpdated) > DELTA) {
			log.info("Updating timestamp"); //$NON-NLS-1$
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
			mTimestamp = str.substring(start, end);
			lastTimestampUpdated = System.currentTimeMillis();
			Algoritms.closeStream(in);
			Algoritms.closeStream(out);
			log.info("Timestamp updated"); //$NON-NLS-1$
			clearCache();
		}
	}

	private void clearCache() {
		ArrayList<String> l = new ArrayList<String>(tiles.keySet());
		for(String k : l){
			Bitmap b = tiles.remove(k);
			if(b != null){
				b.recycle();
			}
		}
	}
	

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public boolean onLongPressEvent(PointF point) {
		return false;
	}

	@Override
	public boolean onTouchEvent(PointF point) {
		return false;
	}
	
	@Override
	public void destroyLayer() {
		if(isVisible()){
			stopThread();
		}
	}

}
