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
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.SurfaceHolder.Callback;

import com.osmand.LogUtil;
import com.osmand.OsmandSettings;
import com.osmand.ResourceManager;
import com.osmand.data.preparation.MapTileDownloader;
import com.osmand.data.preparation.MapTileDownloader.DownloadRequest;
import com.osmand.data.preparation.MapTileDownloader.IMapDownloaderCallback;
import com.osmand.map.IMapLocationListener;
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
	
	private float rotate = 0;
	
	private int mapPosition;
	
	// name of source map 
	private ITileSource map = null;
	
	private IMapLocationListener locationListener;
	
	private List<OsmandMapLayer> layers = new ArrayList<OsmandMapLayer>();
	
	// UI Part
	private AnimateDraggingMapThread animatedDraggingThread;
	
	private PointF startDragging = null;
	private PointF autoStartDragging = null;
	private long autoStartDraggingTime = 0;
	
	Paint paintGrayFill;
	Paint paintWhiteFill;
	Paint paintBlack;
	Paint paintBitmap;
	
	

	
	
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
		// when map rotate
		paintGrayFill.setAntiAlias(true);

		paintWhiteFill = new Paint();
		paintWhiteFill.setColor(Color.WHITE);
		paintWhiteFill.setStyle(Style.FILL);
		// when map rotate
		paintWhiteFill.setAntiAlias(true);
		
		paintBlack = new Paint();
		paintBlack.setStyle(Style.STROKE);
		paintBlack.setColor(Color.BLACK);
		
		paintBitmap = new Paint();
		paintBitmap.setFilterBitmap(true);

		setClickable(true);
		getHolder().addCallback(this);
		
		animatedDraggingThread = new AnimateDraggingMapThread();
		animatedDraggingThread.setCallback(this);
	}
	
	
	
	
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		refreshMap();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		refreshMap();
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
	

	public float getXTile(){
		return (float) MapUtils.getTileNumberX(zoom, longitude);
	}
	
	public float getYTile(){
		return (float) MapUtils.getTileNumberY(zoom, latitude);
	}
	
	
	public void setZoom(int zoom){
		if (map == null || (map.getMaximumZoomSupported() >= zoom && map.getMinimumZoomSupported() <= zoom)) {
			animatedDraggingThread.stopDragging();
			this.zoom = zoom;
			refreshMap();
		}
	}
	
	public void setRotate(float rotate) {
		float dif = this.rotate - rotate;
		if (dif > 2 || dif < -2) {
			this.rotate = rotate;
			animatedDraggingThread.stopDragging();
			refreshMap();
		}
	}
	
	public void setRotateWithLocation(float rotate, double latitude, double longitude){
		animatedDraggingThread.stopDragging();
		this.rotate = rotate;
		this.latitude = latitude;
		this.longitude = longitude;
		refreshMap();
	}
	
	public float getRotate() {
		return rotate;
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
		refreshMap();
	}
	
	public void setLatLon(double latitude, double longitude){
		animatedDraggingThread.stopDragging();
		this.latitude = latitude;
		this.longitude = longitude;
		refreshMap();
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
	
	protected void drawEmptyTile(Canvas cvs, float x, float y){
		int tileDiv = getTileSize() / emptyTileDivisor;
		for (int k1 = 0; k1 < emptyTileDivisor; k1++) {
			for (int k2 = 0; k2 < emptyTileDivisor; k2++) {
				float xk = x + tileDiv* k1;
				float yk = y + tileDiv* k2;
				if ((k1 + k2) % 2 == 0) {
					cvs.drawRect(xk, yk, xk + tileDiv, yk + tileDiv, paintGrayFill);
				} else {
					cvs.drawRect(xk, yk, xk + tileDiv, yk + tileDiv, paintWhiteFill);
				}
			}
		}
	}

	public int getCenterPointX(){
		return getWidth() / 2;
	}
	
	public int getCenterPointY(){
		if(mapPosition == OsmandSettings.BOTTOM_CONSTANT){
			return 3 * getHeight() / 4;
		}
		return getHeight() / 2;
	}
	
	public void setMapPosition(int type){
		this.mapPosition = type;
	}
	
	
	private void drawOverMap(Canvas canvas){
		canvas.drawCircle(getCenterPointX(), getCenterPointY(), 3, paintBlack);
		canvas.drawCircle(getCenterPointX(), getCenterPointY(), 6, paintBlack);
		for(OsmandMapLayer layer : layers){
			layer.onDraw(canvas);
		}
	}
	
	protected void calculateTileRectangle(Rect pixRect, float cx, float cy, float ctilex, float ctiley, RectF tileRect){
		float x1 = calcDiffTileX(pixRect.left - cx, pixRect.top - cy);
		float x2 = calcDiffTileX(pixRect.left - cx, pixRect.bottom - cy);
		float x3 = calcDiffTileX(pixRect.right - cx, pixRect.top - cy);
		float x4 = calcDiffTileX(pixRect.right - cx, pixRect.bottom - cy);
		float y1 = calcDiffTileY(pixRect.left - cx, pixRect.top - cy);
		float y2 = calcDiffTileY(pixRect.left - cx, pixRect.bottom - cy);
		float y3 = calcDiffTileY(pixRect.right - cx, pixRect.top - cy);
		float y4 = calcDiffTileY(pixRect.right - cx, pixRect.bottom - cy);
		float l = Math.min(Math.min(x1, x2), Math.min(x3, x4)) + ctilex;
		float r = Math.max(Math.max(x1, x2), Math.max(x3, x4)) + ctilex;
		float t = Math.min(Math.min(y1, y2), Math.min(y3, y4)) + ctiley;
		float b = Math.max(Math.max(y1, y2), Math.max(y3, y4)) + ctiley;
		tileRect.set(l, t, r, b);
	}
	
	protected void calculatePixelRectangle(Rect pixelRect, float cx, float cy, float ctilex, float ctiley, RectF tileRect){
		float x1 = calcDiffPixelX(tileRect.left - ctilex, tileRect.top - ctiley);
		float x2 = calcDiffPixelX(tileRect.left - ctilex, tileRect.bottom - ctiley);
		float x3 = calcDiffPixelX(tileRect.right - ctilex, tileRect.top - ctiley);
		float x4 = calcDiffPixelX(tileRect.right - ctilex, tileRect.bottom - ctiley);
		float y1 = calcDiffPixelY(tileRect.left - ctilex, tileRect.top - ctiley);
		float y2 = calcDiffPixelY(tileRect.left - ctilex, tileRect.bottom - ctiley);
		float y3 = calcDiffPixelY(tileRect.right - ctilex, tileRect.top - ctiley);
		float y4 = calcDiffPixelY(tileRect.right - ctilex, tileRect.bottom - ctiley);
		int l = (int) (Math.min(Math.min(x1, x2), Math.min(x3, x4)) + cx);
		int r = (int) (Math.max(Math.max(x1, x2), Math.max(x3, x4)) + cx);
		int t = (int) (Math.min(Math.min(y1, y2), Math.min(y3, y4)) + cy);
		int b = (int) (Math.max(Math.max(y1, y2), Math.max(y3, y4)) + cy);
		pixelRect.set(l, t, r, b);
	}
	
	// used only to save space & reuse 
	protected RectF tilesRect = new RectF();
	protected Rect boundsRect = new Rect();
	
	public void refreshMap() {
		if (OsmandSettings.isUsingInternetToDownloadTiles(getContext())) {
			 MapTileDownloader.getInstance().refuseAllPreviousRequests();
		}
		int tileSize = getTileSize();
		float tileX = getXTile();
		float tileY = getYTile();
		float w = getCenterPointX();
		float h = getCenterPointY();
		
		SurfaceHolder holder = getHolder();
		synchronized (holder) {
			Canvas canvas = holder.lockCanvas();
			if (canvas != null) {
				ResourceManager mgr = ResourceManager.getResourceManager();
				boolean useInternet = OsmandSettings.isUsingInternetToDownloadTiles(getContext());
				
				canvas.rotate(rotate, w , h);
				boundsRect.set(0, 0, getWidth(), getHeight());
				calculateTileRectangle(boundsRect, w, h, tileX, tileY, tilesRect);
				try {
					int left = (int) FloatMath.floor(tilesRect.left);
					int top = (int) FloatMath.floor(tilesRect.top);
					int width = (int) (FloatMath.ceil(tilesRect.right) - left);
					int height = (int) (FloatMath.ceil(tilesRect.bottom) - top);
					for (int i = 0; i <width; i++) {
						for (int j = 0; j< height; j++) {
							float x1 = (i + left - tileX) * tileSize + w;
							float y1 = (j + top - tileY) * tileSize + h;
							Bitmap bmp = mgr.getTileImageForMapAsync(map, left + i, top + j, zoom, useInternet);
							if (bmp == null) {
								drawEmptyTile(canvas, (int) x1, (int) y1);
							} else {
								canvas.drawBitmap(bmp, x1, y1, paintBitmap);
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
		if (request == null) {
			// we don't know exact images were changed
			refreshMap();
			return;
		}
		if (request.zoom != this.zoom) {
			return;
		}
		float w = getCenterPointX();
		float h = getCenterPointY();
		float tileX = getXTile();
		float tileY = getYTile();

		SurfaceHolder holder = getHolder();
		synchronized (holder) {
			tilesRect.set(request.xTile, request.yTile, request.xTile + 1, request.yTile + 1);
			calculatePixelRectangle(boundsRect, w, h, tileX, tileY, tilesRect);
			if(boundsRect.left > getWidth() || boundsRect.right < 0 || boundsRect.bottom < 0 || boundsRect.top > getHeight()){
				return;
			}
			Canvas canvas = holder.lockCanvas(boundsRect);
			if (canvas != null) {
				canvas.rotate(rotate, w , h);
				try {
					ResourceManager mgr = ResourceManager.getResourceManager();
					Bitmap bmp = mgr.getTileImageForMapSync(map, request.xTile, request.yTile, zoom, false);
					float x = (request.xTile - getXTile()) * getTileSize() + w;
					float y = (request.yTile - getYTile()) * getTileSize() + h;
					if (bmp == null) {
						drawEmptyTile(canvas, x, y);
					} else {
						canvas.drawBitmap(bmp, x, y, null);
					}
					drawOverMap(canvas);
				} finally {
					holder.unlockCanvasAndPost(canvas);
				}
			}
		}
	}
	

	
	/////////////////////////////////// DRAGGING PART ///////////////////////////////////////
	public float calcDiffTileY(float dx, float dy){
		float rad = (float) Math.toRadians(rotate);
		return (-FloatMath.sin(rad) * dx + FloatMath.cos(rad) * dy) / getTileSize();
	}
	
	public float calcDiffTileX(float dx, float dy){
		float rad = (float) Math.toRadians(rotate);
		return (FloatMath.cos(rad) * dx + FloatMath.sin(rad) * dy) / getTileSize();
	}
	
	public float calcDiffPixelY(float dTileX, float dTileY){
		float rad = (float) Math.toRadians(rotate);
		return (FloatMath.sin(rad) * dTileX + FloatMath.cos(rad) * dTileY) * getTileSize();
	}
	
	public float calcDiffPixelX(float dTileX, float dTileY){
		float rad = (float) Math.toRadians(rotate);
		return (FloatMath.cos(rad) * dTileX - FloatMath.sin(rad) * dTileY) * getTileSize();
	}
	
	/**
	 * These methods do not consider rotating
	 */
	public int getMapXForPoint(double longitude){
		double tileX = MapUtils.getTileNumberX(zoom, longitude);
		return (int) ((tileX - getXTile()) * getTileSize() + getCenterPointX());
	}
	public int getMapYForPoint(double latitude){
		double tileY = MapUtils.getTileNumberY(zoom, latitude);
		return (int) ((tileY - getYTile()) * getTileSize() + getCenterPointY());
	}
	
	public int getRotatedMapXForPoint(double latitude, double longitude){
		int cx = getCenterPointX();
		double xTile = MapUtils.getTileNumberX(zoom, longitude);
		double yTile = MapUtils.getTileNumberY(zoom, latitude);
		return (int) (calcDiffPixelX((float) (xTile - getXTile()), (float) (yTile - getYTile())) + cx);
	}
	public int getRotatedMapYForPoint(double latitude, double longitude){
		int cy = getCenterPointY();
		double xTile = MapUtils.getTileNumberX(zoom, longitude);
		double yTile = MapUtils.getTileNumberY(zoom, latitude);
		return (int) (calcDiffPixelY((float) (xTile - getXTile()), (float) (yTile - getYTile())) + cy);
	}
	
	
	public boolean isPointOnTheRotatedMap(double latitude, double longitude){
		int cx = getCenterPointX();
		int cy = getCenterPointY();
		double xTile = MapUtils.getTileNumberX(zoom, longitude);
		double yTile = MapUtils.getTileNumberY(zoom, latitude);
		int newX = (int) (calcDiffPixelX((float) (xTile - getXTile()), (float) (yTile - getYTile())) + cx);
		int newY = (int) (calcDiffPixelY((float) (xTile - getXTile()), (float) (yTile - getYTile())) + cy);
		if(newX >= 0 && newX <= getWidth() && newY >=0 && newY <= getHeight()){
			return true;
		} 
		return false;
	}
	
	@Override
	public void dragTo(float fromX, float fromY, float toX, float toY){
		float dx = (fromX - toX) ; 
		float dy = (fromY - toY);
		float fy = calcDiffTileY(dx, dy);
		float fx = calcDiffTileX(dx, dy);
		
		this.latitude = MapUtils.getLatitudeFromTile(zoom, getYTile() + fy);
		this.longitude = MapUtils.getLongitudeFromTile(zoom, getXTile() + fx);
		refreshMap();
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
