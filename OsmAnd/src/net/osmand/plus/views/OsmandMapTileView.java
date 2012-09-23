package net.osmand.plus.views;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.osmand.LogUtil;
import net.osmand.OsmAndFormatter;
import net.osmand.access.AccessibleToast;
import net.osmand.access.MapExplorer;
import net.osmand.data.MapTileDownloader.DownloadRequest;
import net.osmand.data.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.map.IMapLocationListener;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.views.MultiTouchSupport.MultiTouchZoomListener;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

public class OsmandMapTileView extends SurfaceView implements IMapDownloaderCallback, Callback {

	protected final static int LOWEST_ZOOM_TO_ROTATE = 10;

	protected final int emptyTileDivisor = 16;
	
	public static final float ZOOM_DELTA = 3;
	public static final float ZOOM_DELTA_1 = 1/3f;
	

	public interface OnTrackBallListener {
		public boolean onTrackBallEvent(MotionEvent e);
	}

	public interface OnLongClickListener {
		public boolean onLongPressEvent(PointF point);
	}

	public interface OnClickListener {
		public boolean onPressEvent(PointF point);
	}

	protected static final Log log = LogUtil.getLog(OsmandMapTileView.class);
	/**MapTree
	 * zoom level - could be float to show zoomed tiles
	 */
	private float zoom = 3;

	private double longitude = 0d;

	private double latitude = 0d;

	private float rotate = 0;
	
	private float rotateSin = 0;
	private float rotateCos = 1;

	private int mapPosition;

	private boolean showMapPosition = true;

	private IMapLocationListener locationListener;

	private OnLongClickListener onLongClickListener;

	private OnClickListener onClickListener;

	private OnTrackBallListener trackBallDelegate;

	private List<OsmandMapLayer> layers = new ArrayList<OsmandMapLayer>();
	
	private BaseMapLayer mainLayer;
	
	private Map<OsmandMapLayer, Float> zOrders = new HashMap<OsmandMapLayer, Float>();

	// UI Part
	// handler to refresh map (in ui thread - ui thread is not necessary, but msg queue is required).
	protected Handler handler = new Handler();

	private AnimateDraggingMapThread animatedDraggingThread;

	private GestureDetector gestureDetector;

	private MultiTouchSupport multiTouchSupport;

	Paint paintGrayFill;
	Paint paintBlackFill;
	Paint paintWhiteFill;
	Paint paintCenter;

	private DisplayMetrics dm;

	private final OsmandApplication application;

	public OsmandMapTileView(Context context, AttributeSet attrs) {
		super(context, attrs);
		application = (OsmandApplication) context.getApplicationContext();
		initView();
		
	}

	public OsmandMapTileView(Context context) {
		super(context);
		application = (OsmandApplication) context.getApplicationContext();
		initView();
	}

	// ///////////////////////////// INITIALIZING UI PART ///////////////////////////////////
	public void initView() {
		paintGrayFill = new Paint();
		paintGrayFill.setColor(Color.GRAY);
		paintGrayFill.setStyle(Style.FILL);
		// when map rotate
		paintGrayFill.setAntiAlias(true);
		
		paintBlackFill= new Paint();
		paintBlackFill.setColor(Color.BLACK);
		paintBlackFill.setStyle(Style.FILL);
		// when map rotate
		paintBlackFill.setAntiAlias(true);

		paintWhiteFill = new Paint();
		paintWhiteFill.setColor(Color.WHITE);
		paintWhiteFill.setStyle(Style.FILL);
		// when map rotate
		paintWhiteFill.setAntiAlias(true);

		paintCenter = new Paint();
		paintCenter.setStyle(Style.STROKE);
		paintCenter.setColor(Color.rgb(60, 60, 60));
		paintCenter.setStrokeWidth(2);
		paintCenter.setAntiAlias(true);

		setClickable(true);
		setLongClickable(true);
		setFocusable(true);

		getHolder().addCallback(this);

		animatedDraggingThread = new AnimateDraggingMapThread(this);
		gestureDetector = new GestureDetector(getContext(), new MapExplorer(this, new MapTileViewOnGestureListener()));
		multiTouchSupport = new MultiTouchSupport(getContext(), new MapTileViewMultiTouchZoomListener());
		gestureDetector.setOnDoubleTapListener(new MapTileViewOnDoubleTapListener());

		WindowManager mgr = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
		dm = new DisplayMetrics();
		mgr.getDefaultDisplay().getMetrics(dm);
		
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
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return application.accessibilityEnabled() ? false : super.onKeyDown(keyCode, event);
	}

	public void addLayer(OsmandMapLayer layer, float zOrder) {
		int i = 0;
		for (i = 0; i < layers.size(); i++) {
			if (zOrders.get(layers.get(i)) > zOrder) {
				break;
			}
		}
		layer.initLayer(this);
		layers.add(i, layer);
		zOrders.put(layer, zOrder);
	}

	public void removeLayer(OsmandMapLayer layer) {
		layers.remove(layer);
		zOrders.remove(layer);
		layer.destroyLayer();
	}

	public List<OsmandMapLayer> getLayers() {
		return layers;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends OsmandMapLayer> T getLayerByClass(Class<T> cl) {
		for(OsmandMapLayer lr : layers) {
			if(cl.isInstance(lr)){
				return (T) lr;
			}
		}
		return null;
	}

	public OsmandApplication getApplication() {
		return application;
	}

	// ///////////////////////// NON UI PART (could be extracted in common) /////////////////////////////
	/**
	 * Returns real tile size in pixels for float zoom .  
	 */
	public float getTileSize() {
		float res = getSourceTileSize();
		if (zoom != (int) zoom) {
			res *= (float) Math.pow(2, zoom - (int) zoom);
		}

		
		return res;
	}

	public int getSourceTileSize() {
		int r = 256;
		if (mainLayer instanceof MapTileLayer) {
			r = ((MapTileLayer) mainLayer).getSourceTileSize();
		}
		// that trigger allows to scale tiles for certain devices
		// for example for device with density > 1 draw tiles the same size as with density = 1
		// It makes text bigger but blurry, the settings could be introduced for that
		if (dm != null && dm.density > 1f && !getSettings().USE_HIGH_RES_MAPS.get()) {
			return (int) (r * dm.density);
		}
		return r;
	}

	/**
	 * @return x tile based on (int) zoom
	 */
	public float getXTile() {
		return (float) MapUtils.getTileNumberX(getZoom(), longitude);
	}

	/**
	 * @return y tile based on (int) zoom
	 */
	public float getYTile() {
		return (float) MapUtils.getTileNumberY(getZoom(), latitude);
	}
	
	/**
	 * @return y tile based on (int) zoom
	 */
	public float getEllipticYTile() {
		return (float) MapUtils.getTileEllipsoidNumberY(getZoom(), latitude);
	}
	
	public void setZoom(float zoom) {
		if (mainLayer != null && zoom <= mainLayer.getMaximumShownMapZoom() && zoom >= mainLayer.getMinimumShownMapZoom()) {
			animatedDraggingThread.stopAnimating();
			this.zoom = zoom;
			refreshMap();
		}
	}

	
	public boolean isMapRotateEnabled(){
		return zoom > LOWEST_ZOOM_TO_ROTATE;
	}

	public void setRotate(float rotate) {
		if (isMapRotateEnabled()) {
			float diff = MapUtils.unifyRotationDiff(rotate, getRotate());
			if (Math.abs(diff) > 5) { // check smallest rotation
				animatedDraggingThread.startRotate(rotate);
			}
		}
	}

	public boolean isShowMapPosition() {
		return showMapPosition;
	}

	public void setShowMapPosition(boolean showMapPosition) {
		this.showMapPosition = showMapPosition;
	}

	public float getRotate() {
		return isMapRotateEnabled() ?  rotate : 0;
	}


	public void setLatLon(double latitude, double longitude) {
		animatedDraggingThread.stopAnimating();
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
		return (int) zoom;
	}
	
	public float getFloatZoom() {
		return zoom;
	}

	public boolean isZooming(){
		// zooming scale
		float diff = (zoom - getZoom()) * ZOOM_DELTA;
		if(Math.abs(diff - Math.round(diff)) < 0.0001) {
			return false;
		}
		return true;
		// return zoom != getZoom();
	}
	
	public void setMapLocationListener(IMapLocationListener l) {
		locationListener = l;
	}

	/**
	 * Adds listener to control when map is dragging
	 */
	public IMapLocationListener setMapLocationListener() {
		return locationListener;
	}

	// ////////////////////////////// DRAWING MAP PART /////////////////////////////////////////////

	protected void drawEmptyTile(Canvas cvs, float x, float y, float ftileSize, boolean nightMode) {
		float tileDiv = (ftileSize / emptyTileDivisor);
		for (int k1 = 0; k1 < emptyTileDivisor; k1++) {
			for (int k2 = 0; k2 < emptyTileDivisor; k2++) {
				float xk = x + tileDiv * k1;
				float yk = y + tileDiv * k2;
				if ((k1 + k2) % 2 == 0) {
					cvs.drawRect(xk, yk, xk + tileDiv, yk + tileDiv, paintGrayFill);
				} else {
					cvs.drawRect(xk, yk, xk + tileDiv, yk + tileDiv, nightMode ? paintBlackFill : paintWhiteFill);
				}
			}
		}
	}
	
	public BaseMapLayer getMainLayer() {
		return mainLayer;
	}
	
	public void setMainLayer(BaseMapLayer mainLayer) {
		this.mainLayer = mainLayer;
		if (mainLayer.getMaximumShownMapZoom() < this.zoom) {
			zoom = mainLayer.getMaximumShownMapZoom();
		}
		if (mainLayer.getMinimumShownMapZoom() > this.zoom) {
			zoom = mainLayer.getMinimumShownMapZoom();
		}
		refreshMap();
	}

	public int getCenterPointX() {
		return getWidth() / 2;
	}

	public int getCenterPointY() {
		if (mapPosition == OsmandSettings.BOTTOM_CONSTANT) {
			return 3 * getHeight() / 4;
		}
		return getHeight() / 2;
	}

	public void setMapPosition(int type) {
		this.mapPosition = type;
	}
	
	public double calcLongitude(int pixelFromCenter) {
		return MapUtils.getLongitudeFromTile(getZoom(), getXTile() + pixelFromCenter /  getTileSize());
	}
	
	public double calcLatitude(int pixelFromCenter) {
		return MapUtils.getLatitudeFromTile(getZoom(), getXTile() + pixelFromCenter /  getTileSize());
	}
	
	public void calculateLatLonRectangle(Rect pixRect, RectF latLonRect) {
		int z = (int) zoom;
		float tileX = (float) MapUtils.getTileNumberX(z, getLongitude());
		float tileY = (float) MapUtils.getTileNumberY(z, getLatitude());
		float w = getCenterPointX();
		float h = getCenterPointY();
		RectF tilesRect = new RectF();
		calculateTileRectangle(pixRect, w, h, tileX, tileY, tilesRect);
		
		latLonRect.top = (float) MapUtils.getLatitudeFromTile(z, tilesRect.top);
		latLonRect.left = (float) MapUtils.getLongitudeFromTile(z, tilesRect.left);
		latLonRect.bottom = (float) MapUtils.getLatitudeFromTile(z, tilesRect.bottom);
		latLonRect.right = (float) MapUtils.getLongitudeFromTile(z, tilesRect.right);
	}
	

	public void calculateTileRectangle(Rect pixRect, float cx, float cy, float ctilex, float ctiley, RectF tileRect) {
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

	public void calculatePixelRectangle(Rect pixelRect, float cx, float cy, float ctilex, float ctiley, RectF tileRect) {
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
	protected RectF latlonRect = new RectF();
	protected Rect boundsRect = new Rect();
	protected RectF bitmapToDraw = new RectF();
	protected Rect bitmapToZoom = new Rect();
	protected OsmandSettings settings = null;
	
	public OsmandSettings getSettings(){
		if(settings == null){
			settings = getApplication().getSettings();
		}
		return settings;
	}

	private void refreshMapInternal(boolean force) {
		handler.removeMessages(1);
		
		// long time = System.currentTimeMillis();

		boolean useInternet = getSettings().USE_INTERNET_TO_DOWNLOAD_TILES.get();
		if (useInternet) {
			if(application != null) {
				application.getResourceManager().getMapTileDownloader().refuseAllPreviousRequests();
			}
		}
		

		SurfaceHolder holder = getHolder();
		synchronized (holder) {
			int nzoom = getZoom();
			float tileX = (float) MapUtils.getTileNumberX(nzoom, longitude);
			float tileY = (float) MapUtils.getTileNumberY(nzoom, latitude);
			float w = getCenterPointX();
			float h = getCenterPointY();
			Canvas canvas = holder.lockCanvas();
			if (canvas != null) {
				boolean nightMode = false;
				if (application != null) {
					Boolean dayNightRenderer = application.getDaynightHelper().getDayNightRenderer();
					if (dayNightRenderer != null) {
						nightMode = !dayNightRenderer.booleanValue();
					}
				}
				try {
					boundsRect.set(0, 0, getWidth(), getHeight());
					calculateTileRectangle(boundsRect, w, h, tileX, tileY, tilesRect);
					latlonRect.top = (float) MapUtils.getLatitudeFromTile(nzoom, tilesRect.top);
					latlonRect.left = (float) MapUtils.getLongitudeFromTile(nzoom, tilesRect.left);
					latlonRect.bottom = (float) MapUtils.getLatitudeFromTile(nzoom, tilesRect.bottom);
					latlonRect.right = (float) MapUtils.getLongitudeFromTile(nzoom, tilesRect.right);
					if(nightMode){
						canvas.drawARGB(255, 100, 100, 100);
					} else {
						canvas.drawARGB(255, 225, 225, 225);
					}
					// TODO map
//					float ftileSize = getTileSize();
//					int left = (int) FloatMath.floor(tilesRect.left);
//					int top = (int) FloatMath.floor(tilesRect.top);
//					int width = (int) FloatMath.ceil(tilesRect.right - left);
//					int height = (int) FloatMath.ceil(tilesRect.bottom - top);
//					for (int i = 0; i < width; i++) {
//						for (int j = 0; j < height; j++) {
//							float x1 = (i + left - tileX) * ftileSize + w;
//							float y1 = (j + top - tileY) * ftileSize + h;
//							drawEmptyTile(canvas, x1, y1, ftileSize, nightMode);
//						}
//					}
					drawOverMap(canvas, latlonRect, tilesRect, new DrawSettings(nightMode,force));
					
//					log.info("Draw with layers " + (System.currentTimeMillis() - time));
				} finally {
					holder.unlockCanvasAndPost(canvas);
				}
			}
		}
	}
	
	private void drawOverMap(Canvas canvas, RectF latlonRect, RectF tilesRect, DrawSettings drawSettings) {
		int w = getCenterPointX();
		int h = getCenterPointY();

		// long prev = System.currentTimeMillis();

		for (int i = 0; i < layers.size(); i++) {
			try {
				
				OsmandMapLayer layer = layers.get(i);
				canvas.save();
				// rotate if needed
				if (!layer.drawInScreenPixels()) {
					canvas.rotate(getRotate(), w, h);
				}
				layer.onDraw(canvas, latlonRect, tilesRect, drawSettings);
				canvas.restore();
			} catch (IndexOutOfBoundsException e) {
				// skip it
			}
			// long time = System.currentTimeMillis();
			// log.debug("Layer time " + (time - prev) + " " + zOrders.get(layers.get(i)));
			// prev = time;
		}
		canvas.restore();
		if (showMapPosition) {
			canvas.drawCircle(w, h, 3 * dm.density, paintCenter);
			canvas.drawCircle(w, h, 7 * dm.density, paintCenter);
		}
	}

	public boolean mapIsRefreshing() {
		return handler.hasMessages(1);
	}

	public boolean mapIsAnimating() {
		return animatedDraggingThread != null && animatedDraggingThread.isAnimating();
	}

	// this method could be called in non UI thread
	public void refreshMap() {
		refreshMap(false);
	}
	
	// this method could be called in non UI thread
	public void refreshMap(final boolean force) {
		if (!handler.hasMessages(1) || force) {
			handler.removeMessages(1);
			Message msg = Message.obtain(handler, new Runnable() {
				@Override
				public void run() {
					refreshMapInternal(force);
				}
			});
			msg.what = 1;
			handler.sendMessageDelayed(msg, 20);
		}
	}

	@Override
	public void tileDownloaded(DownloadRequest request) {
		// force to refresh map because image can be loaded from different threads
		// and threads can block each other especially for sqlite images when they
		// are inserting into db they block main thread
		refreshMap();
	}

	// ///////////////////////////////// DRAGGING PART ///////////////////////////////////////
	public float calcDiffTileY(float dx, float dy) {
		if(isMapRotateEnabled()){
			return (-rotateSin * dx + rotateCos * dy) / getTileSize();
		} else {
			return dy / getTileSize();
		}
		
	}

	public float calcDiffTileX(float dx, float dy) {
		if(isMapRotateEnabled()){
			return (rotateCos * dx + rotateSin * dy) / getTileSize();
		} else {
			return dx / getTileSize();
		}
	}

	public float calcDiffPixelY(float dTileX, float dTileY) {
		if(isMapRotateEnabled()){
			return (rotateSin * dTileX + rotateCos * dTileY) * getTileSize();
		} else {
			return dTileY * getTileSize();
		}
		
	}

	public float calcDiffPixelX(float dTileX, float dTileY) {
		if(isMapRotateEnabled()){
			return (rotateCos * dTileX - rotateSin * dTileY) * getTileSize();
		} else {
			return dTileX * getTileSize();
		}
	}

	/**
	 * These methods do not consider rotating
	 */
	public int getMapXForPoint(double longitude) {
		double tileX = MapUtils.getTileNumberX(getZoom(), longitude);
		return (int) ((tileX - getXTile()) * getTileSize() + getCenterPointX());
	}

	public int getMapYForPoint(double latitude) {
		double tileY = MapUtils.getTileNumberY(getZoom(), latitude);
		return (int) ((tileY - getYTile()) * getTileSize() + getCenterPointY());
	}

	public int getRotatedMapXForPoint(double latitude, double longitude) {
		int cx = getCenterPointX();
		double xTile = MapUtils.getTileNumberX(getZoom(), longitude);
		double yTile = MapUtils.getTileNumberY(getZoom(), latitude);
		return (int) (calcDiffPixelX((float) (xTile - getXTile()), (float) (yTile - getYTile())) + cx);
	}

	public int getRotatedMapYForPoint(double latitude, double longitude) {
		int cy = getCenterPointY();
		double xTile = MapUtils.getTileNumberX(getZoom(), longitude);
		double yTile = MapUtils.getTileNumberY(getZoom(), latitude);
		return (int) (calcDiffPixelY((float) (xTile - getXTile()), (float) (yTile - getYTile())) + cy);
	}

	public boolean isPointOnTheRotatedMap(double latitude, double longitude) {
		int cx = getCenterPointX();
		int cy = getCenterPointY();
		double xTile = MapUtils.getTileNumberX(getZoom(), longitude);
		double yTile = MapUtils.getTileNumberY(getZoom(), latitude);
		int newX = (int) (calcDiffPixelX((float) (xTile - getXTile()), (float) (yTile - getYTile())) + cx);
		int newY = (int) (calcDiffPixelY((float) (xTile - getXTile()), (float) (yTile - getYTile())) + cy);
		if (newX >= 0 && newX <= getWidth() && newY >= 0 && newY <= getHeight()) {
			return true;
		}
		return false;
	}

	protected void dragToAnimate(float fromX, float fromY, float toX, float toY, boolean notify) {
		float dx = (fromX - toX);
		float dy = (fromY - toY);
		moveTo(dx, dy);
		if (locationListener != null && notify) {
			locationListener.locationChanged(latitude, longitude, this);
		}
	}

	protected void rotateToAnimate(float rotate) {
		if (isMapRotateEnabled()) {
			this.rotate = MapUtils.unifyRotationTo360(rotate);
			float rotateRad = (float) Math.toRadians(rotate);
			this.rotateCos = FloatMath.cos(rotateRad);
			this.rotateSin = FloatMath.sin(rotateRad);
			refreshMap();
		}
	}
	
	protected void setLatLonAnimate(double latitude, double longitude, boolean notify) {
		this.latitude = latitude;
		this.longitude = longitude;
		refreshMap();
		if (locationListener != null && notify) {
			locationListener.locationChanged(latitude, longitude, this);
		}
	}
	
	// for internal usage
	protected void zoomToAnimate(float zoom, boolean notify) {
		if (mainLayer != null && mainLayer.getMaximumShownMapZoom() >= zoom && mainLayer.getMinimumShownMapZoom() <= zoom) {
			this.zoom = zoom;
			refreshMap();
			if (notify && locationListener != null) {
				locationListener.locationChanged(latitude, longitude, this);
			}
		}
	}

	public void moveTo(float dx, float dy) {
		float fy = calcDiffTileY(dx, dy);
		float fx = calcDiffTileX(dx, dy);

		this.latitude = MapUtils.getLatitudeFromTile(getZoom(), getYTile() + fy);
		this.longitude = MapUtils.getLongitudeFromTile(getZoom(), getXTile() + fx);
		refreshMap();
		// do not notify here listener

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			animatedDraggingThread.stopAnimating();
		}
		for(int i=layers.size() - 1; i >= 0; i--) {
			if(layers.get(i).onTouchEvent(event)) {
				return true;
			}
		}
		if (!multiTouchSupport.onTouchEvent(event)) {
			/* return */gestureDetector.onTouchEvent(event);
		}
		return true;
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		if (trackBallDelegate != null) {
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


	public LatLon getLatLonFromScreenPoint(float x, float y) {
		float dx = x - getCenterPointX();
		float dy = y - getCenterPointY();
		float fy = calcDiffTileY(dx, dy);
		float fx = calcDiffTileX(dx, dy);
		double latitude = MapUtils.getLatitudeFromTile(getZoom(), getYTile() + fy);
		double longitude = MapUtils.getLongitudeFromTile(getZoom(), getXTile() + fx);
		return new LatLon(latitude, longitude);
	}

	

	
	public AnimateDraggingMapThread getAnimatedDraggingThread() {
		return animatedDraggingThread;
	}

	public void showMessage(final String msg) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				AccessibleToast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show(); //$NON-NLS-1$
			}
		});
	}

	
	private class MapTileViewMultiTouchZoomListener implements MultiTouchZoomListener {
		private float initialMultiTouchZoom;
		private PointF initialMultiTouchCenterPoint;
		private LatLon initialMultiTouchLocation;
		private float x1;
		private float y1;
		private float x2;
		private float y2;
		
		@Override
		public void onZoomEnded(float distance, float relativeToStart) {
			float dz = (float) (Math.log(relativeToStart) / Math.log(2) * 1.5);
			float calcZoom = initialMultiTouchZoom + dz;
			setZoom(Math.round(calcZoom));
			final int newZoom = getZoom();
			zoomPositionChanged(newZoom);
			if (application.accessibilityEnabled()) {
				if (newZoom != initialMultiTouchZoom) {
					showMessage(getContext().getString(R.string.zoomIs) + " " + String.valueOf(newZoom)); //$NON-NLS-1$
				} else {
					final LatLon p1 = getLatLonFromScreenPoint(x1, y1);
					final LatLon p2 = getLatLonFromScreenPoint(x2, y2);
					showMessage(OsmAndFormatter.getFormattedDistance((float)MapUtils.getDistance(p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude()), getContext()));
				}
			}
		}
		
		@Override
		public void onGestureInit(float x1, float y1, float x2, float y2) {
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
		}

		@Override
		public void onZoomStarted(float distance, PointF centerPoint) {
			initialMultiTouchCenterPoint = centerPoint;
			initialMultiTouchLocation = getLatLonFromScreenPoint(centerPoint.x, centerPoint.y);
			initialMultiTouchZoom = zoom;
		}

		@Override
		public void onZooming(float distance, float relativeToStart) {
			float dz = (float) (Math.log(relativeToStart) / Math.log(2) * 1.5);
			float calcZoom = initialMultiTouchZoom + dz;
			if (Math.abs(calcZoom - zoom) > 0.05) {
				setZoom(calcZoom);
				zoomPositionChanged(calcZoom);
			}
		}
		
		private void zoomPositionChanged(float calcZoom) {
			float dx = initialMultiTouchCenterPoint.x - getCenterPointX();
			float dy = initialMultiTouchCenterPoint.y - getCenterPointY();
			float ex = calcDiffTileX(dx, dy);
			float ey = calcDiffTileY(dx, dy);
			int z = (int)calcZoom;
			double tx = MapUtils.getTileNumberX(z, initialMultiTouchLocation.getLongitude());
			double ty = MapUtils.getTileNumberY(z, initialMultiTouchLocation.getLatitude());
			double lat = MapUtils.getLatitudeFromTile(z, ty - ey);
			double lon = MapUtils.getLongitudeFromTile(z, tx - ex);
			setLatLon(lat, lon);
		}

	}

	private class MapTileViewOnGestureListener implements OnGestureListener {
		@Override
		public boolean onDown(MotionEvent e) {
			return false;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			animatedDraggingThread.startDragging(velocityX, velocityY, 
						e1.getX(), e1.getY(), e2.getX(), e2.getY(), true);
			return true;
		}

		@Override
		public void onLongPress(MotionEvent e) {
			if (multiTouchSupport.isInZoomMode()) {
				return;
			}
			if (log.isDebugEnabled()) {
				log.debug("On long click event " + e.getX() + " " + e.getY()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			PointF point = new PointF(e.getX(), e.getY());
			for (int i = layers.size() - 1; i >= 0; i--) {
				if (layers.get(i).onLongPressEvent(point)) {
					return;
				}
			}
			if (onLongClickListener != null && onLongClickListener.onLongPressEvent(point)) {
				return;
			}
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			dragToAnimate(e2.getX() + distanceX, e2.getY() + distanceY, e2.getX(), e2.getY(), true);
			return true;
		}

		@Override
		public void onShowPress(MotionEvent e) {
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			PointF point = new PointF(e.getX(), e.getY());
			if (log.isDebugEnabled()) {
				log.debug("On click event " + point.x + " " + point.y); //$NON-NLS-1$ //$NON-NLS-2$
			}
			for (int i = layers.size() - 1; i >= 0; i--) {
				if (layers.get(i).onSingleTap(point)) {
					return true;
				}
			}
			if (onClickListener != null && onClickListener.onPressEvent(point)) {
				return true;
			}
			return false;
		}
	}


	private class MapTileViewOnDoubleTapListener implements OnDoubleTapListener {
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			LatLon l = getLatLonFromScreenPoint(e.getX(), e.getY());
			getAnimatedDraggingThread().startMoving(l.getLatitude(), l.getLongitude(), getZoom() + 1, true);
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

}
