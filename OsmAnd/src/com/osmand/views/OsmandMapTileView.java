package com.osmand.views;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.SurfaceHolder.Callback;

import com.osmand.DefaultLauncherConstants;
import com.osmand.IMapLocationListener;
import com.osmand.LogUtil;
import com.osmand.MapTileDownloader;
import com.osmand.OsmandSettings;
import com.osmand.MapTileDownloader.DownloadRequest;
import com.osmand.MapTileDownloader.IMapDownloaderCallback;
import com.osmand.map.ITileSource;
import com.osmand.osm.MapUtils;

public class OsmandMapTileView extends SurfaceView implements IMapDownloaderCallback, Callback{

	protected final int emptyTileDivisor = DefaultLauncherConstants.MAP_divNonLoadedImage;
	protected final int maxImgCacheSize = 96;
	protected int drawCoordinatesX = 0;
	protected int drawCoordinatesY = 55;
	
	protected final int timeForDraggingAnimation = 300;
	protected final int minimumDistanceForDraggingAnimation = 40;
	
	
	protected static final Log log = LogUtil.getLog(OsmandMapTileView.class);
	/**
	 * file or directory with tiles
	 */
	private File fileWithTiles;

	/**
	 * zoom level
	 */
	private int zoom = DefaultLauncherConstants.MAP_startMapZoom;
	
	private double longitude = DefaultLauncherConstants.MAP_startMapLongitude;

	private double latitude = DefaultLauncherConstants.MAP_startMapLatitude;
	
	// name of source map 
	private ITileSource map = null;
	
	/**
	 * listeners
	 */
	private List<IMapLocationListener> listeners = new ArrayList<IMapLocationListener>();
	
	private MapTileDownloader downloader = MapTileDownloader.getInstance();
	
	Map<String, Bitmap> cacheOfImages = new HashMap<String, Bitmap>();
	
	private boolean isStartedDragging = false;
	private double startDraggingX = 0d;
	private double startDraggingY = 0d;
	private PointF initStartDragging = null;
	
	Paint paintGrayFill;
	Paint paintWhiteFill;
	Paint paintBlack;
	private AnimatedDragging animatedDraggingThread;

	
	
	public OsmandMapTileView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}
	
	public OsmandMapTileView(Context context) {
		super(context);
		initView();
	}
	
	public void initView(){
		paintGrayFill = new Paint();
		paintGrayFill.setColor(Color.GRAY);
		paintGrayFill.setStyle(Style.FILL);
		
		paintWhiteFill = new Paint();
		paintWhiteFill.setColor(Color.WHITE);
		paintWhiteFill.setStyle(Style.FILL);
		
		paintBlack = new Paint();
		paintBlack.setStyle(Style.STROKE);
		paintBlack.setColor(Color.BLACK);

		setClickable(true);
		getHolder().addCallback(this);
		downloader.setDownloaderCallback(this);
		asyncLoadingTiles.start();
	}
	

	public int getTileSize() {
		return map == null ? 256 : map.getTileSize();
	}
	

	public void dragTo(double fromX, double fromY, double toX, double toY){
		double dx = (fromX - toX)/getTileSize(); 
		double dy = (fromY - toY)/getTileSize();
		this.latitude = MapUtils.getLatitudeFromTile(zoom, getYTile() + dy);
		this.longitude = MapUtils.getLongitudeFromTile(zoom, getXTile() + dx);
		prepareImage();
		// TODO
//		fireMapLocationListeners(this);
	}
	
	public class AnimatedDragging extends Thread {
		private float curX;
		private float  curY;
		private float  vx;
		private float  vy;
		private float ax;
		private float ay;
		private byte dirX;
		private byte dirY;
		private long time = System.currentTimeMillis();
		private boolean stopped;
		private final float  a = 0.0005f; 

		public AnimatedDragging(float  dTime, float  startX, float  startY, float  endX, float  endY) {
			vx = Math.abs((endX - startX)/dTime);
			vy = Math.abs((endY - startY)/dTime);
			dirX = (byte) (endX > startX ? 1 : -1);
			dirY = (byte) (endY > startY ? 1 : -1);
			ax = vx * a;
			ay = vy * a;
		}

		@Override
		public void run() {
			try {
				while ((vx > 0 || vy > 0) && !isStartedDragging && !stopped) {
					sleep((long) (40d/(Math.max(vx, vy)+0.45)));
					long curT = System.currentTimeMillis();
					int dt = (int) (curT - time);
					float  newX = vx > 0 ? curX + dirX * vx * dt : curX;
					float  newY = vy > 0 ? curY + dirY * vy * dt : curY;
					if(!isStartedDragging){
						dragTo(curX, curY, newX, newY);
					}
					vx -= ax * dt;
					vy -= ay * dt;
					time = curT;
					curX = newX;
					curY = newY;
				}
			} catch (InterruptedException e) {
			}
			animatedDraggingThread = null;
		}
		
		public void stopEvaluation(){
			stopped = true;
		}
		
	}
	
	
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(event.getAction() == MotionEvent.ACTION_DOWN) {
			if(animatedDraggingThread != null){
				animatedDraggingThread.stopEvaluation();
			}
			if(!isStartedDragging){
				startDraggingX = event.getX();
				startDraggingY = event.getY();
				isStartedDragging = true;
				initStartDragging = new PointF(event.getX(), event.getY());
			}
		} else if(event.getAction() == MotionEvent.ACTION_UP) {
			if(isStartedDragging){
				dragTo(startDraggingX, startDraggingY, event.getX(), event.getY());
				if(event.getEventTime() - event.getDownTime() < timeForDraggingAnimation && 
						Math.abs(event.getX() - initStartDragging.x) + Math.abs(event.getY() - initStartDragging.y) > minimumDistanceForDraggingAnimation){
					float timeDist = (int) (event.getEventTime() - event.getDownTime());
					if(timeDist < 20){
						timeDist = 20;
					}
					animatedDraggingThread = new AnimatedDragging(timeDist, initStartDragging.x, initStartDragging.y, 
							event.getX(), event.getY());
					isStartedDragging = false;
					animatedDraggingThread.start();
				}
				isStartedDragging = false;
				
			}
		} else if(event.getAction() == MotionEvent.ACTION_MOVE) {
			if(isStartedDragging){
				dragTo(startDraggingX, startDraggingY, event.getX(), event.getY());
				startDraggingX = event.getX();
				startDraggingY = event.getY();
			}
		}
		return super.onTouchEvent(event);
	}
	
	public double getXTile(){
		return MapUtils.getTileNumberX(zoom, longitude);
	}
	
	public double getYTile(){
		return MapUtils.getTileNumberY(zoom, latitude);
	}
	
	protected void drawEmptyTile(Canvas cvs, int x, int y){
		int tileDiv = getTileSize() / emptyTileDivisor;
		for (int k1 = 0; k1 < emptyTileDivisor; k1++) {
			
			for (int k2 = 0; k2 < emptyTileDivisor; k2++) {
				int xk = x + tileDiv* k1;
				int yk = y + tileDiv* k2;
				if ((k1 + k2) % 2 == 0) {
					cvs.drawRect(xk, yk, xk + tileDiv, yk + tileDiv, paintGrayFill);
				} else {
					cvs.drawRect(xk, yk, xk + tileDiv, yk + tileDiv, paintWhiteFill);
				}
			}
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		prepareImage();
		super.onSizeChanged(w, h, oldw, oldh);
	}
	

	public String getFileForImage (int x, int y, int zoom, String ext){
		return map.getName() +"/"+zoom+"/"+(x) +"/"+y+ext+".tile";
	}
	
	public AsyncLoadingThread asyncLoadingTiles = new AsyncLoadingThread();
		
	public class AsyncLoadingThread extends Thread {
		Map<String, DownloadRequest> requests = Collections.synchronizedMap(new LinkedHashMap<String, DownloadRequest>());
		
		public AsyncLoadingThread(){
			super("Async loading tiles");
		}
		
		@Override
		public void run() {
			while(true){
				try {
					boolean update = false;
					while(!requests.isEmpty()){
						String f = requests.keySet().iterator().next();
						DownloadRequest r = requests.remove(f);
						// TODO last param
						getImageForTile(r.xTile, r.yTile, r.zoom, OsmandSettings.useInternetToDownloadTiles);
						update = true;
					}
					if(update){
						prepareImage();
					}
					sleep(350);
				} catch (InterruptedException e) {
					log.error(e);
				} catch (RuntimeException e){
					log.error(e);
				}
			}
		}
		
		public void requestToLoadImage(String s, DownloadRequest req){
			requests.put(s, req);
			
		}
	};
	
	private Bitmap getImageForTile(int x, int y, int zoom, boolean loadIfNeeded){
		String file = getFileForImage(x, y, zoom, map.getTileFormat());
		if(fileWithTiles == null || !fileWithTiles.canRead()){
			return null;
		}
		File en = new File(fileWithTiles, file);
		if (cacheOfImages.size() > maxImgCacheSize) {
			onLowMemory();
		}
		
		if (!downloader.isFileCurrentlyDownloaded(en)) {
			if (en.exists()) {
				long time = System.currentTimeMillis();
				cacheOfImages.put(file, BitmapFactory.decodeFile(en.getAbsolutePath()));
				if (log.isDebugEnabled()) {
					log.debug("Loaded file : " + file + " " + -(time - System.currentTimeMillis()) + " ms");
				}
			} 
			
			if(loadIfNeeded && cacheOfImages.get(file) == null){
				ConnectivityManager mgr = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo info = mgr.getActiveNetworkInfo();
				if (info != null && info.isConnected()) {
					String urlToLoad = map.getUrlToLoad(x, y, zoom);
					if (urlToLoad != null) {
						downloader.requestToDownload(urlToLoad, new DownloadRequest(en, x, y, zoom));
					}
				}
			}
		}
		return cacheOfImages.get(file);
	}
	
	public Bitmap getImageFor(int x, int y, int zoom, boolean loadIfNeeded) {
		if (map == null) {
			return null;
		}
		String file = getFileForImage(x, y, zoom, map.getTileFormat());
		if (cacheOfImages.get(file) == null && loadIfNeeded) {
			// TODO use loadIfNeeded
			asyncLoadingTiles.requestToLoadImage(file, new DownloadRequest(null, x, y, zoom));
		}
		return cacheOfImages.get(file);
	}
	
	
	public void tileDownloaded(String dowloadedUrl, DownloadRequest request) {
		int xTileLeft = (int) Math.floor(getXTile() - getWidth() / (2d * getTileSize()));
		int yTileUp = (int) Math.floor(getYTile() - getHeight() / (2d * getTileSize()));
		int startingX = (int) ((xTileLeft - getXTile()) * getTileSize() + getWidth() / 2);
		int startingY = (int) ((yTileUp - getYTile()) * getTileSize() + getHeight() / 2);
		int i = (request.xTile - xTileLeft) * getTileSize() + startingX;
		int j = (request.yTile - yTileUp) * getTileSize() + startingY;
		if (request.zoom == this.zoom && 
				(i + getTileSize() >= 0 && i < getWidth()) && (j + getTileSize() >= 0 && j < getHeight())) {
			SurfaceHolder holder = getHolder();
			synchronized (holder) {
				Canvas canvas = holder.lockCanvas(new Rect(i, j, getTileSize() + i, getTileSize() + j));
				if (canvas != null) {
					try {
						Bitmap bmp = getImageFor(request.xTile, request.yTile, zoom, true);
						if (bmp == null) {
							drawEmptyTile(canvas, i, j);
						} else {
							canvas.drawBitmap(bmp, i, j, null);
						}
						drawOverMap(canvas);
					} finally {
						holder.unlockCanvasAndPost(canvas);
					}
				}
			}
		}
	}
	private MessageFormat formatOverMap = new MessageFormat("Lat : {0}, lon : {1}, zoom : {2}, mem : {3}");
	java.util.Formatter formatterOMap = new java.util.Formatter();
	private ByteArrayOutputStream stream = new ByteArrayOutputStream();
	
	private void drawOverMap(Canvas canvas){
		canvas.drawCircle(getWidth() / 2, getHeight() / 2, 3, paintBlack);
		canvas.drawCircle(getWidth() / 2, getHeight() / 2, 6, paintBlack);
		if (OsmandSettings.showGPSLocationOnMap) {
			float f=  (Runtime.getRuntime().totalMemory())/ 1e6f;
			formatterOMap = new Formatter();
			canvas.drawText(formatterOMap.format("Lat : %.3f, lon : %.3f, zoom : %d, mem : %.3f",  latitude, longitude, zoom, f).toString(), 
					drawCoordinatesX, drawCoordinatesY, paintBlack);
//			canvas.drawText(formatOverMap.format(new Object[]{latitude, longitude, zoom, f}), drawCoordinatesX,
//					drawCoordinatesY, paintBlack);
		}
	}
	
	public void prepareImage() {
		if (OsmandSettings.useInternetToDownloadTiles) {
			downloader.refuseAllPreviousRequests();
		}
		int width = getWidth();
		int height = getHeight();
		int tileSize = getTileSize();
		
		int xTileLeft = (int) Math.floor(getXTile() - width / (2d * getTileSize()));
		int yTileUp = (int) Math.floor(getYTile() - height / (2d * getTileSize()));
		int startingX = (int) ((xTileLeft - getXTile()) * getTileSize() + getWidth() / 2);
		int startingY = (int) ((yTileUp - getYTile()) * getTileSize() + getHeight() / 2);
		
		SurfaceHolder holder = getHolder();
		synchronized (holder) {
			Canvas canvas = holder.lockCanvas();
			if (canvas != null) {
				try {
					for (int i = 0; i * tileSize + startingX < width; i++) {
						for (int j = 0; j * tileSize + startingY < height; j++) {
							Bitmap bmp = getImageFor(xTileLeft + i, yTileUp + j, zoom, true);
							if (bmp == null) {
								drawEmptyTile(canvas, i * tileSize + startingX, j * tileSize + startingY);
							} else {
								canvas.drawBitmap(bmp, i * tileSize + startingX, j * tileSize + startingY, null);
							}
						}
					}
					drawOverMap(canvas);
				} finally {
					holder.unlockCanvasAndPost(canvas);
				}
			}
		}
	}
	
	
	
	public void setZoom(int zoom){
		if (map == null || (map.getMaximumZoomSupported() >= zoom && map.getMinimumZoomSupported() <= zoom)) {
			if(animatedDraggingThread != null){
				animatedDraggingThread.stopEvaluation();
			}
			this.zoom = zoom;
			prepareImage();
		}
	}
	
	public File getFileWithTiles() {
		return fileWithTiles;
	}
	
	public ITileSource getMap() {
		return map;
	}
	
	public void setMap(ITileSource map) {
		this.map = map;
		if(map.getMaximumZoomSupported() < this.zoom){
			zoom = map.getMaximumZoomSupported();
		}
		if(map.getMinimumZoomSupported() > this.zoom){
			zoom = map.getMinimumZoomSupported();
		}
		prepareImage();
	}
	
	public void setFileWithTiles(File fileWithTiles) {
		this.fileWithTiles = fileWithTiles;
		prepareImage();
	}

	public void setLatLon(double latitude, double longitude){
		this.latitude = latitude;
		this.longitude = longitude;
		prepareImage();
		fireMapLocationListeners(null);
	}
	
	public double getLatitude() {
		return latitude;
	}
	
	public double getLongitude() {
		return longitude;
	}
	
	public int getZoom() {
		return zoom;
	}
	
	
	public void onLowMemory(){
		log.info("On low memory : cleaning tiles - size = " + cacheOfImages.size());
		ArrayList<String> list = new ArrayList<String>(cacheOfImages.keySet());
		// remove first images (as we think they are older)
		for (int i = 0; i < list.size()/2; i ++) {
			Bitmap bmp = cacheOfImages.remove(list.get(i));
			if(bmp != null){
				bmp.recycle();
			}
		}
		System.gc();
	}
	

	public void addMapLocationListener(IMapLocationListener l){
		listeners.add(l);
	}
	
	public void removeMapLocationListener(IMapLocationListener l){
		listeners.remove(l);
	}
	
	protected void fireMapLocationListeners(Object source){
		for(IMapLocationListener l : listeners){
			l.locationChanged(latitude, longitude, source);
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		prepareImage();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		prepareImage();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO clear cache ?
		
	}


}
