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
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
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

public class OsmandMapTileView extends SurfaceView implements IMapDownloaderCallback, 
			Callback, AnimateDraggingCallback, OnGestureListener, OnDoubleTapListener {

	protected final int emptyTileDivisor = 16;
	protected final int timeForDraggingAnimation = 300;
	protected final int minimumDistanceForDraggingAnimation = 40;
	
	public interface OnTrackBallListener{
		public boolean onTrackBallEvent(MotionEvent e);
		public boolean onTrackBallPressed();
	}
	
	public interface OnLongClickListener {
		public boolean onLongPressEvent(PointF point);
	}
	public interface OnClickListener {
		public boolean onPressEvent(PointF point);
	}
	
	
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
	
	private OnLongClickListener onLongClickListener;
	
	private OnClickListener onClickListener;
	
	private OnTrackBallListener trackBallDelegate;
	
	private List<OsmandMapLayer> layers = new ArrayList<OsmandMapLayer>();
	
	// UI Part
	// handler to refresh map (in ui thread - not necessary in ui thread, but msg queue is desirable). 
	protected Handler handler = new Handler();
	
	private AnimateDraggingMapThread animatedDraggingThread;
	
	private GestureDetector gestureDetector;
	
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
		setLongClickable(true);
		setFocusable(true);
		
		getHolder().addCallback(this);
		
		animatedDraggingThread = new AnimateDraggingMapThread();
		animatedDraggingThread.setCallback(this);
		gestureDetector = new GestureDetector(getContext(), this);
		gestureDetector.setOnDoubleTapListener(this);
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
	
	public void addLayer(OsmandMapLayer layer, OsmandMapLayer afterIt){
		layer.initLayer(this);
		int i = layers.indexOf(afterIt);
		if(i == -1){
			layers.add(layer);
		} else {
			layers.add(i, layer);
		}
	}
	
	public void addLayer(OsmandMapLayer layer){
		layer.initLayer(this);
		layers.add(layer);
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
		int w = getCenterPointX();
		int h = getCenterPointY();
		canvas.drawCircle(w, h, 3, paintBlack);
		canvas.drawCircle(w, h, 6, paintBlack);
		
		for (OsmandMapLayer layer : layers) {
			canvas.restore();
			canvas.save();
			if (!layer.drawInScreenPixels()) {
				canvas.rotate(rotate, w, h);
			}
			layer.onDraw(canvas);
		}
	}
	
	public void calculateTileRectangle(Rect pixRect, float cx, float cy, float ctilex, float ctiley, RectF tileRect){
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
	
	public void calculatePixelRectangle(Rect pixelRect, float cx, float cy, float ctilex, float ctiley, RectF tileRect){
		float x1 = calcDiffPixelX(tileRect.left - ctilex, tileRect.top - ctiley);
		float x2 = calcDiffPixelX(tileRect.left - ctilex, tileRect.bottom - ctiley);
		float x3 = calcDiffPixelX(tileRect.right - ctilex, tileRect.top - ctiley);
		float x4 = calcDiffPixelX(tileRect.right - ctilex, tileRect.bottom - ctiley);
		float y1 = calcDiffPixelY(tileRect.left - ctilex, tileRect.top - ctiley);
		float y2 = calcDiffPixelY(tileRect.left - ctilex, tileRect.bottom - ctiley);
		float y3 = calcDiffPixelY(tileRect.right - ctilex, tileRect.top - ctiley);
		float y4 = calcDiffPixelY(tileRect.right - ctilex, tileRect.bottom - ctiley);
		int l = Math.round(Math.min(Math.min(x1, x2), Math.min(x3, x4)) + cx);
		int r = Math.round(Math.max(Math.max(x1, x2), Math.max(x3, x4)) + cx);
		int t = Math.round(Math.min(Math.min(y1, y2), Math.min(y3, y4)) + cy);
		int b = Math.round(Math.max(Math.max(y1, y2), Math.max(y3, y4)) + cy);
		pixelRect.set(l, t, r, b);
	}
	
	// used only to save space & reuse 
	protected RectF tilesRect = new RectF();
	protected Rect boundsRect = new Rect();
	protected RectF bitmapToDraw = new RectF();
	protected Rect bitmapToZoom = new Rect();
	
	private void refreshMapInternal(){
		if(handler.hasMessages(1)){
			return;
		}
		
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
				int maxLevel = OsmandSettings.getMaximumLevelToDownloadTile(getContext());
				canvas.save();
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
							String ordImgTile = mgr.calculateTileId(map, left + i, top + j, zoom);
							// asking tile image async
							boolean imgExist = mgr.tileExistOnFileSystem(ordImgTile);
							Bitmap bmp = null;
							boolean originalBeLoaded = useInternet && zoom <= maxLevel;
							if (imgExist || originalBeLoaded) {
								bmp = mgr.getTileImageForMapAsync(ordImgTile, map, left + i, top + j, zoom, useInternet);
							}
							if (bmp == null) {
								int div = 2;
								// asking if there is small version of the map (in cache)
								String imgTile2 = mgr.calculateTileId(map, (left + i) / 2, (top + j) / 2, zoom - 1);
								String imgTile4 = mgr.calculateTileId(map, (left + i) / 4, (top + j) / 4, zoom - 2);
								if(originalBeLoaded || imgExist){
									bmp = mgr.getTileImageFromCache(imgTile2);
									div = 2;
									if(bmp == null){
										bmp = mgr.getTileImageFromCache(imgTile4);
										div = 4;
									}
								}
								if(!originalBeLoaded && !imgExist){
									if (mgr.tileExistOnFileSystem(imgTile2) || (useInternet && zoom - 1 <= maxLevel)) {
										bmp = mgr.getTileImageForMapAsync(imgTile2, map, (left + i) / 2, (top + j) / 2, zoom - 1, useInternet);
										div = 2;
									} else if (mgr.tileExistOnFileSystem(imgTile4) || (useInternet && zoom - 2 <= maxLevel)) {
										bmp = mgr.getTileImageForMapAsync(imgTile4, map, (left + i) / 4, (top + j) / 4, zoom - 2, useInternet);
										div = 4;
									}
								}
								
								if(bmp == null){
									drawEmptyTile(canvas, (int) x1, (int) y1);
								} else {
									int xZoom = ((left + i) % div) * tileSize / div;
									int yZoom = ((top + j) % div) * tileSize / div;;
									bitmapToZoom.set(xZoom, yZoom, xZoom + tileSize / div, yZoom + tileSize / div);
									bitmapToDraw.set(x1, y1, x1 + tileSize, y1 + tileSize);
									canvas.drawBitmap(bmp, bitmapToZoom, bitmapToDraw, paintBitmap);
								}
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
	
	public boolean mapIsRefreshing(){
		return handler.hasMessages(1);
	}
	// this method could be called in non UI thread
	public void refreshMap() {
		if(!handler.hasMessages(1)){
			Message msg = Message.obtain(handler, new Runnable(){
				@Override
				public void run() { 
					refreshMapInternal();
				}
			});
			msg.what = 1;
			handler.sendMessageDelayed(msg, 20);
		}
	}
	
	
	public void tileDownloaded(DownloadRequest request) {
		if (request == null || rotate != 0 ) {
			// if image is rotated call refresh the whole canvas
    		// because we can't find dirty rectangular region but all pixels should be drawn
			
			// we don't know exact images were changed
			refreshMap();
			return;
		}
		if(request.error){
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
					Bitmap bmp = mgr.getTileImageForMapSync(null, map, request.xTile, request.yTile, request.zoom, false);
					float x = (request.xTile - tileX) * getTileSize() + w;
					float y = (request.yTile - tileY) * getTileSize() + h;
					if (bmp == null) {
						drawEmptyTile(canvas, x, y);
					} else {
						canvas.drawBitmap(bmp, x, y, paintBitmap);
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
	
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
//		dumpEvent(event);
		/*return */ gestureDetector.onTouchEvent(event);
		return true;
	}
	
	private void dumpEvent(MotionEvent event) {
		String names[] = { "DOWN" , "UP" , "MOVE" , "CANCEL" , "OUTSIDE" ,
			      "POINTER_DOWN" , "POINTER_UP" , "7?" , "8?" , "9?" };
			   StringBuilder sb = new StringBuilder();
			   int action = event.getAction();
			   int actionCode = action & 255;
			   sb.append("event ACTION_" ).append(names[actionCode]);
			   if (actionCode == 5
			         || actionCode == 6) {
			      sb.append("(pid " ).append(action >> 8);
			      sb.append(")" );
			   }
			   sb.append("[" );
//			   for (int i = 0; i < event.getPointerCount(); i++) {
//			      sb.append("#" ).append(i);
//			      sb.append("(pid " ).append(event.getPointerId(i));
//			      sb.append(")=" ).append((int) event.getX(i));
//			      sb.append("," ).append((int) event.getY(i));
//			      if (i + 1 < event.getPointerCount())
//			         sb.append(";" );
//			   }
			   sb.append("]" );
			   android.util.Log.d("com.osmand", sb.toString());
		
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(trackBallDelegate != null && keyCode == KeyEvent.KEYCODE_DPAD_CENTER){
			return trackBallDelegate.onTrackBallPressed();
		}
		return super.onKeyUp(keyCode, event);
	}
	
	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		if(trackBallDelegate != null){
			trackBallDelegate.onTrackBallEvent(event);
		}
		return super.onTrackballEvent(event);
	}
	
	public void setTrackBallDelegate(OnTrackBallListener trackBallDelegate) {
		this.trackBallDelegate = trackBallDelegate;
	}
	
	
	public void setOnLongClickListener(OnLongClickListener l) {
		this.onLongClickListener = l;
	}
	
	public void setOnClickListener(OnClickListener l) {
		this.onClickListener = l;
	}


	@Override
	public boolean onDown(MotionEvent e) {
		animatedDraggingThread.stopDragging();
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		animatedDraggingThread.startDragging(Math.abs(velocityX/1000), Math.abs(velocityY/1000), e1.getX(), e1.getY(), e2.getX(), e2.getY());
		return true;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		if(log.isDebugEnabled()){
			log.debug("On long click event "+  e.getX() + " " + e.getY()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		PointF point = new PointF(e.getX(), e.getY());
		for (int i = layers.size() - 1; i >= 0; i--) {
			if (layers.get(i).onLongPressEvent(point)) {
				return;
			}
		}
		if(onLongClickListener != null && onLongClickListener.onLongPressEvent(point)){
			return;
		}
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		dragTo(e2.getX() + distanceX, e2.getY() + distanceY, e2.getX(), e2.getY());
		return true;
	}

	@Override
	public void onShowPress(MotionEvent e) {
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		PointF point = new PointF(e.getX(), e.getY());
		if(log.isDebugEnabled()){
			log.debug("On click event "+  point.x + " " + point.y); //$NON-NLS-1$ //$NON-NLS-2$
		}
		for (int i = layers.size() - 1; i >= 0; i--) {
			if (layers.get(i).onTouchEvent(point)) {
				return true;
			}
		}
		if(onClickListener != null && onClickListener.onPressEvent(point)){
			return true;
		}
		return false;
	}

	@Override
	public boolean onDoubleTap(MotionEvent e) {
		float dx = e.getX() - getCenterPointX();
		float dy = e.getY() - getCenterPointY();
		float fy = calcDiffTileY(dx, dy);
		float fx = calcDiffTileX(dx, dy);
		double latitude = MapUtils.getLatitudeFromTile(getZoom(), getYTile() + fy);
		double longitude = MapUtils.getLongitudeFromTile(getZoom(), getXTile() + fx);
		setLatLon(latitude, longitude);
		setZoom(zoom + 1);
		return true;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		return false;
	}
	

	


}
