package com.osmand.views;


import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Paint.Style;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.osmand.DefaultLauncherConstants;
import com.osmand.IMapLocationListener;
import com.osmand.MapTileDownloader;
import com.osmand.OsmandSettings;
import com.osmand.MapTileDownloader.DownloadRequest;
import com.osmand.MapTileDownloader.IMapDownloaderCallback;
import com.osmand.map.ITileSource;
import com.osmand.osm.MapUtils;

public class OsmandMapTileView extends View implements IMapDownloaderCallback {

	protected final int emptyTileDivisor = DefaultLauncherConstants.MAP_divNonLoadedImage;
	protected final int maxImgCacheSize = 512;
	
	protected int drawCoordinatesX = 0;
	protected int drawCoordinatesY = 55;
	
	protected static final Log log = LogFactory.getLog(OsmandMapTileView.class);
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
	
	
	// cached data to draw images
	private Bitmap[][] images;
	// this value is always <= 0
	private int xStartingImage = 0;
	// this value is always <= 0
	private int yStartingImage = 0;
	
	Map<String, Bitmap> cacheOfImages = new WeakHashMap<String, Bitmap>();
	private PointF startDragging = null;
	
	Paint paintGrayFill;
	Paint paintWhiteFill;
	Paint paintBlack;
	final Handler mHandler = new Handler();

    // Create runnable for posting
    final Runnable invalidateView = new Runnable() {
        public void run() {
            invalidate();
        }
    };
	
	
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
		
		prepareImage();
		setClickable(true);
		downloader.setDownloaderCallback(this);
	}
	

	public int getTileSize() {
		return map == null ? 256 : map.getTileSize();
	}
	

	public void dragTo(PointF p){
		double dx = (startDragging.x - (double)p.x)/getTileSize(); 
		double dy = (startDragging.y - (double)p.y)/getTileSize();
		this.latitude = MapUtils.getLatitudeFromTile(zoom, getYTile() + dy);
		this.longitude = MapUtils.getLongitudeFromTile(zoom, getXTile() + dx);
		prepareImage();
		fireMapLocationListeners(this);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(event.getAction() == MotionEvent.ACTION_DOWN) {
			if(startDragging == null){
				startDragging  = new PointF(event.getX(), event.getY());
			}
		} else if(event.getAction() == MotionEvent.ACTION_UP) {
			if(startDragging != null){
				dragTo(new PointF(event.getX(), event.getY()));
				startDragging = null;
			}
		} else if(event.getAction() == MotionEvent.ACTION_MOVE) {
			if(startDragging != null){
				PointF p = new PointF(event.getX(), event.getY());
				dragTo(p);
				startDragging = p;
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
	protected void onDraw(Canvas canvas) {
		canvas.drawRect(0,0, getWidth(), getHeight(), paintWhiteFill);
		if (images != null) {
			for (int i = 0; i < images.length; i++) {
				for (int j = 0; j < images[i].length; j++) {
					if (images[i][j] == null) {
						drawEmptyTile(canvas, i*getTileSize()+xStartingImage, j * getTileSize() + yStartingImage);
					} else {
						canvas.drawBitmap(images[i][j], i * getTileSize() + xStartingImage, j * getTileSize() + yStartingImage, null);
					}
				}
			}
		}
		canvas.drawCircle(getWidth()/2, getHeight()/2, 3, paintBlack);
		canvas.drawCircle(getWidth()/2, getHeight()/2, 6, paintBlack);
		if (OsmandSettings.showGPSLocationOnMap) {
			canvas.drawText(MessageFormat.format("Lat : {0}, lon : {1}, zoom : {2}", latitude, longitude, zoom), 
					drawCoordinatesX, drawCoordinatesY, paintBlack);
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
	
	public Bitmap getImageFor(int x, int y, int zoom, boolean loadIfNeeded) {
		if (map == null || fileWithTiles == null || !fileWithTiles.canRead()) {
			return null;
		}

		String file = getFileForImage(x, y, zoom, map.getTileFormat());
		if (cacheOfImages.get(file) == null) {
			File en = new File(fileWithTiles, file);
			if (cacheOfImages.size() > maxImgCacheSize) {
				ArrayList<String> list = new ArrayList<String>(cacheOfImages.keySet());
				for (int i = 0; i < list.size(); i += 2) {
					Bitmap bmp = cacheOfImages.remove(list.get(i));
					bmp.recycle();
				}
				System.gc();
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
					String urlToLoad = map.getUrlToLoad(x, y, zoom);
					if (urlToLoad != null) {
						downloader.requestToDownload(urlToLoad, new DownloadRequest(en, x, y, zoom));
					}
				}
			}
		}

		return cacheOfImages.get(file);
	}
	
	@Override
	public void tileDownloaded(String dowloadedUrl, DownloadRequest request) {
		int tileSize = getTileSize();
		double xTileLeft = getXTile() - getWidth() / (2d * tileSize);
		double yTileUp = getYTile() - getHeight() / (2d * tileSize);
		int i = request.xTile - (int)xTileLeft;
		int j = request.yTile - (int)yTileUp;
		if(request.zoom == this.zoom && 
				(i >= 0 && i < images.length) && (j >= 0 && j < images[i].length)) {
			images[i][j] = getImageFor(request.xTile, request.yTile, zoom, false);
			mHandler.post(invalidateView);
		}
	}
	
	public void prepareImage(){
		prepareImage(OsmandSettings.useInternetToDownloadTiles);
	}
	
	public void prepareImage(boolean loadNecessaryImages) {
		if (loadNecessaryImages) {
			downloader.refuseAllPreviousRequests();
		}
		double xTileLeft = getXTile() - getWidth() / (2d * getTileSize());
		double xTileRight = getXTile() + getWidth() / (2d * getTileSize());
		double yTileUp = getYTile() - getHeight() / (2d * getTileSize());
		double yTileDown = getYTile() + getHeight() / (2d * getTileSize());

		xStartingImage = -(int) ((xTileLeft - Math.floor(xTileLeft)) * getTileSize());
		yStartingImage = -(int) ((yTileUp - Math.floor(yTileUp)) * getTileSize());

		int tileXCount = ((int) xTileRight - (int) xTileLeft + 1);
		int tileYCount = ((int) yTileDown - (int) yTileUp + 1);
		images = new Bitmap[tileXCount][tileYCount];
		for (int i = 0; i < images.length; i++) {
			for (int j = 0; j < images[i].length; j++) {
				images[i][j] = getImageFor((int) xTileLeft + i, (int) yTileUp + j, zoom, loadNecessaryImages);
			}
		}
		invalidate();
	}
	
	
	
	public void setZoom(int zoom){
		if (map == null || (map.getMaximumZoomSupported() >= zoom && map.getMinimumZoomSupported() <= zoom)) {
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




}
