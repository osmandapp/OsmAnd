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
			prepareImage();
		}
	}
	
	public void setRotate(float rotate) {
		float dif = this.rotate - rotate;
		if (dif > 2 || dif < -2) {
			this.rotate = rotate;
			animatedDraggingThread.stopDragging();
			prepareImage();
		}
	}
	
	public void setRotateWithLocation(float rotate, double latitude, double longitude){
		animatedDraggingThread.stopDragging();
		this.rotate = rotate;
		this.latitude = latitude;
		this.longitude = longitude;
		prepareImage();
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
	
	protected void calculateTileRectangle(RectF pixRect, float cx, float cy, RectF tileRect){
		float x1 = calcDiffTileX(pixRect.left - cx, pixRect.top - cy);
		float x2 = calcDiffTileX(pixRect.left - cx, pixRect.bottom - cy);
		float x3 = calcDiffTileX(pixRect.right - cx, pixRect.top - cy);
		float x4 = calcDiffTileX(pixRect.right - cx, pixRect.bottom - cy);
		float y1 = calcDiffTileY(pixRect.left - cx, pixRect.top - cy);
		float y2 = calcDiffTileY(pixRect.left - cx, pixRect.bottom - cy);
		float y3 = calcDiffTileY(pixRect.right - cx, pixRect.top - cy);
		float y4 = calcDiffTileY(pixRect.right - cx, pixRect.bottom - cy);
		float l = Math.min(Math.min(x1, x2), Math.min(x3, x4)) + getXTile();
		float r = Math.max(Math.max(x1, x2), Math.max(x3, x4)) + getXTile();
		float t = Math.min(Math.min(y1, y2), Math.min(y3, y4)) + getYTile();
		float b = Math.max(Math.max(y1, y2), Math.max(y3, y4)) + getYTile();
		tileRect.set(l, t, r, b);
	}
	
	// used only to save space & reuse 
	protected RectF tilesRect = new RectF();
	protected RectF boundsRect = new RectF();
	
	public void prepareImage() {
		if (OsmandSettings.isUsingInternetToDownloadTiles(getContext())) {
			 MapTileDownloader.getInstance().refuseAllPreviousRequests();
		}
		int tileSize = getTileSize();
		float tileX = getXTile();
		float tileY = getYTile();
		
		SurfaceHolder holder = getHolder();
		synchronized (holder) {
			Canvas canvas = holder.lockCanvas();
			if (canvas != null) {
				ResourceManager mgr = ResourceManager.getResourceManager();
				boolean useInternet = OsmandSettings.isUsingInternetToDownloadTiles(getContext());
				float w = getWidth() / 2;
				float h = getHeight() / 2;
				canvas.rotate(rotate, w , h);
				boundsRect.set(0, 0, getWidth(), getHeight());
				calculateTileRectangle(boundsRect, w, h, tilesRect);
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
		// TODO estimate bounds for rotated map
		if(request == null || rotate != 0){
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
					canvas.rotate(rotate,getWidth()/2, getHeight()/2);
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
	public float calcDiffTileY(float dx, float dy){
		float rad = (float) Math.toRadians(rotate);
		return (-FloatMath.sin(rad) * dx + FloatMath.cos(rad) * dy) / getTileSize();
	}
	
	public float calcDiffTileX(float dx, float dy){
		float rad = (float) Math.toRadians(rotate);
		return (FloatMath.cos(rad) * dx + FloatMath.sin(rad) * dy) / getTileSize();
	}
	
	
	public boolean isPointOnTheMap(double latitude, double longitude){
		int cx = getWidth()/2;
		int cy = getHeight()/2;
		int dx = MapUtils.getPixelShiftX(zoom, longitude, this.longitude, getTileSize());
		int dy = MapUtils.getPixelShiftY(zoom, latitude, this.latitude , getTileSize());
		float rad = (float) Math.toRadians(rotate);
		int newX = (int) (dx * FloatMath.cos(rad) - dy * FloatMath.sin(rad) + cx);
		int newY = (int) (dx * FloatMath.sin(rad) + dy * FloatMath.cos(rad) + cy);
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
