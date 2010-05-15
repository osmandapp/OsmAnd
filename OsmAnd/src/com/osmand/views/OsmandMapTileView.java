package com.osmand.views;


import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.SurfaceHolder.Callback;

import com.osmand.IMapLocationListener;
import com.osmand.LogUtil;
import com.osmand.OsmandSettings;
import com.osmand.ResourceManager;
import com.osmand.data.preparation.MapTileDownloader;
import com.osmand.data.preparation.MapTileDownloader.DownloadRequest;
import com.osmand.data.preparation.MapTileDownloader.IMapDownloaderCallback;
import com.osmand.map.ITileSource;
import com.osmand.osm.MapUtils;
import com.osmand.views.AnimateDraggingMapThread.AnimateDraggingCallback;

public class OsmandMapTileView extends SurfaceView implements IMapDownloaderCallback, Callback, AnimateDraggingCallback{

	protected final int emptyTileDivisor = 16;
	protected final int timeForDraggingAnimation = 300;
	protected final int minimumDistanceForDraggingAnimation = 40;
	
	
	protected static final Log log = LogUtil.getLog(OsmandMapTileView.class);
	/**
	 * zoom level
	 */
	private int zoom = 3;
	
	private double longitude = 0d;

	private double latitude = 0d;
	
	// name of source map 
	private ITileSource map = null;
	
	private IMapLocationListener locationListener;
	
	private MapTileDownloader downloader = MapTileDownloader.getInstance();
	
	private List<OsmandMapLayer> layers = new ArrayList<OsmandMapLayer>();
	
	// UI Part

	private AnimateDraggingMapThread animatedDraggingThread;
	
	private PointF startDragging = null;
	private PointF autoStartDragging = null;
	private long autoStartDraggingTime = 0;
	
	Paint paintGrayFill;
	Paint paintWhiteFill;
	Paint paintBlack;
	

	
	
	public OsmandMapTileView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}
	
	public OsmandMapTileView(Context context) {
		super(context);
		initView();
	}
	
	/////////////////////////////// INITIALIZING UI PART ///////////////////////////////////
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
		animatedDraggingThread = new AnimateDraggingMapThread();
		animatedDraggingThread.setCallback(this);
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
	
	public void addLayer(OsmandMapLayer layer){
		layers.add(layer);
		layer.initLayer(this);
	}
	
	public void removeLayer(OsmandMapLayer layer){
		layers.remove(layer);
		layer.destroyLayer();
	}
	
	public List<OsmandMapLayer> getLayers() {
		return layers;
	}

	
	/////////////////////////// NON UI PART (could be extracted in common) /////////////////////////////
	public int getTileSize() {
		return map == null ? 256 : map.getTileSize();
	}
	

	public double getXTile(){
		return MapUtils.getTileNumberX(zoom, longitude);
	}
	
	public double getYTile(){
		return MapUtils.getTileNumberY(zoom, latitude);
	}
	
	
	public void setZoom(int zoom){
		if (map == null || (map.getMaximumZoomSupported() >= zoom && map.getMinimumZoomSupported() <= zoom)) {
			animatedDraggingThread.stopDragging();
			this.zoom = zoom;
			prepareImage();
		}
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
	
	public void setLatLon(double latitude, double longitude){
		animatedDraggingThread.stopDragging();
		this.latitude = latitude;
		this.longitude = longitude;
		prepareImage();
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
	
	public void setMapLocationListener(IMapLocationListener l){
		locationListener = l;
	}

	/**
	 * Adds listener to control when map is dragging 
	 */
	public IMapLocationListener setMapLocationListener(){
		return locationListener;
	}
	
	
	//////////////////////////////// DRAWING MAP PART /////////////////////////////////////////////
	
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

	
	private void drawOverMap(Canvas canvas){
		canvas.drawCircle(getWidth() / 2, getHeight() / 2, 3, paintBlack);
		canvas.drawCircle(getWidth() / 2, getHeight() / 2, 6, paintBlack);
		for(OsmandMapLayer layer : layers){
			layer.onDraw(canvas);
		}
	}
	
	public void prepareImage() {
		if (OsmandSettings.isUsingInternetToDownloadTiles(getContext())) {
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
							ResourceManager mgr = ResourceManager.getResourceManager();
							Bitmap bmp = mgr.getTileImageForMapAsync(map, xTileLeft + i, yTileUp + j, zoom, OsmandSettings.isUsingInternetToDownloadTiles(getContext()));
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
	
	
	public void tileDownloaded(DownloadRequest request) {
		if(request == null){
			// we don't know exact images were changed
			prepareImage();
			return;
		}
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
						ResourceManager mgr = ResourceManager.getResourceManager();
						Bitmap bmp = mgr.getTileImageForMapSync(map, request.xTile, request.yTile, zoom, false);
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
	

	
	/////////////////////////////////// DRAGGING PART ///////////////////////////////////////
	
	
	@Override
	public void dragTo(float fromX, float fromY, float toX, float toY){
		float dx = (fromX - toX)/getTileSize(); 
		float dy = (fromY - toY)/getTileSize();
		this.latitude = MapUtils.getLatitudeFromTile(zoom, getYTile() + dy);
		this.longitude = MapUtils.getLongitudeFromTile(zoom, getXTile() + dx);
		prepareImage();
		if(locationListener != null){
			locationListener.locationChanged(latitude, longitude, this);
		}
	}
	
	
	public boolean wasMapDraggingAccelerated(MotionEvent event){
		if(autoStartDragging == null){
			return false;
		}
		if(event.getEventTime() - autoStartDraggingTime < timeForDraggingAnimation){
			float dist = Math.abs(event.getX() - autoStartDragging.x) + Math.abs(event.getY() - autoStartDragging.y);
			if(dist > minimumDistanceForDraggingAnimation){
				return true;
			}
		}
		return false;
	}
	
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		for (int i = layers.size() - 1; i >= 0; i--) {
			if (layers.get(i).onTouchEvent(event)) {
				return true;
			}
		}
		
		if(event.getAction() == MotionEvent.ACTION_DOWN) {
			animatedDraggingThread.stopDragging();
			if(startDragging == null){
				autoStartDragging = new PointF(event.getX(), event.getY());
				autoStartDraggingTime = event.getEventTime();
				startDragging = new PointF(event.getX(), event.getY());
			}
		} else if(event.getAction() == MotionEvent.ACTION_UP) {
			if(startDragging != null){
				dragTo(startDragging.x, startDragging.y, event.getX(), event.getY());
				startDragging = null;
				if(wasMapDraggingAccelerated(event)){
					float timeDist = (int) (event.getEventTime() - autoStartDraggingTime);
					if(timeDist < 20){
						timeDist = 20;
					}
					animatedDraggingThread.startDragging(timeDist, autoStartDragging.x, autoStartDragging.y, 
							event.getX(), event.getY());
				}
				
			}
		} else if(event.getAction() == MotionEvent.ACTION_MOVE) {
			if(startDragging != null){
				dragTo(startDragging.x, startDragging.y, event.getX(), event.getY());
				// save memory do not create new PointF
				startDragging.x = event.getX();
				startDragging.y = event.getY();
				if(event.getEventTime() - autoStartDraggingTime > timeForDraggingAnimation){
					autoStartDraggingTime = event.getEventTime();
					autoStartDragging.x = event.getX();
					autoStartDragging.y = event.getY();
				}
			}
		}
		return super.onTouchEvent(event);
	}
	
	


}
