package net.osmand.plus.views;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.osmand.PlatformUtil;
import net.osmand.access.AccessibilityActionsProvider;
import net.osmand.access.AccessibleToast;
import net.osmand.access.MapExplorer;
import net.osmand.data.*;
import net.osmand.map.IMapLocationListener;
import net.osmand.map.MapTileDownloader.DownloadRequest;
import net.osmand.map.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.plus.*;
import net.osmand.plus.views.MultiTouchSupport.MultiTouchZoomListener;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
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
	private boolean MEASURE_FPS = false;
	private int fpsMeasureCount = 0;
	private int fpsMeasureMs = 0;
	private long fpsFirstMeasurement = 0;
	private float fps;

	protected static final int emptyTileDivisor = 16;
	
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

	protected static final Log log = PlatformUtil.getLog(OsmandMapTileView.class);

	private RotatedTileBox currentViewport;
	private float rotate; // accumulate

	private int mapPosition;

	private boolean showMapPosition = true;

	private IMapLocationListener locationListener;

	private OnLongClickListener onLongClickListener;

	private OnClickListener onClickListener;

	private OnTrackBallListener trackBallDelegate;

	private AccessibilityActionsProvider accessibilityActions;

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
		currentViewport = new RotatedTileBox.RotatedTileBoxBuilder().
				setLocation(0, 0).setZoomAndScale(3, 0).setPixelDimensions(getWidth(), getHeight()).build();
		currentViewport.setDensity(dm.density);
		
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
		return application.getInternalAPI().accessibilityEnabled() ? false : super.onKeyDown(keyCode, event);
	}

	public synchronized void addLayer(OsmandMapLayer layer, float zOrder) {
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

	public synchronized void removeLayer(OsmandMapLayer layer) {
		while(layers.remove(layer));
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
	public void setIntZoom(int zoom) {
		if (mainLayer != null && zoom <= mainLayer.getMaximumShownMapZoom() && zoom >= mainLayer.getMinimumShownMapZoom()) {
			animatedDraggingThread.stopAnimating();
			currentViewport.setZoomAndAnimation(zoom, 0);
			currentViewport.setRotate(zoom > LOWEST_ZOOM_TO_ROTATE ? rotate : 0 );
			refreshMap();
		}
	}

	public void setComplexZoom(int zoom, float scale) {
		if (mainLayer != null && zoom <= mainLayer.getMaximumShownMapZoom() && zoom >= mainLayer.getMinimumShownMapZoom()) {
			animatedDraggingThread.stopAnimating();
			currentViewport.setZoom(zoom, scale, 0);
			currentViewport.setRotate(zoom > LOWEST_ZOOM_TO_ROTATE ? rotate : 0 );
			refreshMap();
		}
	}

	
	public boolean isMapRotateEnabled(){
		return getZoom() > LOWEST_ZOOM_TO_ROTATE;
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
		return currentViewport.getRotate();
	}


	public void setLatLon(double latitude, double longitude) {
		animatedDraggingThread.stopAnimating();
		currentViewport.setLatLonCenter(latitude, longitude);
		refreshMap();
	}

	public double getLatitude() {
		return currentViewport.getLatitude();
	}

	public double getLongitude() {
		return currentViewport.getLongitude();
	}

	public int getZoom() {
		return currentViewport.getZoom();
	}

	public float getSettingsZoomScale(){
		return settings.MAP_ZOOM_SCALE_BY_DENSITY.get() + (float)Math.sqrt(Math.max(0, getDensity() - 1));
	}
	
	public float getZoomScale() {
		return currentViewport.getZoomScale();
	}

	public boolean isZooming(){
		return currentViewport.isZoomAnimated();
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
	public BaseMapLayer getMainLayer() {
		return mainLayer;
	}
	
	public void setMainLayer(BaseMapLayer mainLayer) {
		this.mainLayer = mainLayer;
		int zoom = currentViewport.getZoom();
		if (mainLayer.getMaximumShownMapZoom() < zoom) {
			zoom = mainLayer.getMaximumShownMapZoom();
		}
		if (mainLayer.getMinimumShownMapZoom() > zoom) {
			zoom = mainLayer.getMinimumShownMapZoom();
		}
		currentViewport.setZoomAndAnimation(zoom, 0);
		refreshMap();
	}

	public void setMapPosition(int type) {
		this.mapPosition = type;
	}
	
	protected OsmandSettings settings = null;
	public OsmandSettings getSettings(){
		if(settings == null){
			settings = getApplication().getSettings();
		}
		return settings;
	}

	private void refreshMapNonLightweightMap() {

	}

	private void refreshMapInternal(boolean updateVectorRendering) {
		handler.removeMessages(1);
		long ms = SystemClock.elapsedRealtime();
		boolean useInternet = getSettings().USE_INTERNET_TO_DOWNLOAD_TILES.get();
		if (useInternet) {
			if(application != null) {
				application.getResourceManager().getMapTileDownloader().refuseAllPreviousRequests();
			}
		}
		

		SurfaceHolder holder = getHolder();
		synchronized (holder) {
			Canvas canvas = holder.lockCanvas();
			if (canvas != null) {
				try {
					final float ratioy = mapPosition == OsmandSettings.BOTTOM_CONSTANT ? 0.8f : 0.5f;
					final int cy = (int) (ratioy * getHeight());
					if(currentViewport.getPixWidth() != getWidth() || currentViewport.getPixHeight() != getHeight() ||
							currentViewport.getCenterPixelY() != cy) {
						currentViewport.setPixelDimensions(getWidth(), getHeight(), 0.5f, ratioy);
					}
					// TODO high res
					// (getSettings().USE_HIGH_RES_MAPS.get() ? 0 : 0.5f)
					boolean nightMode = application.getDaynightHelper().isNightMode();
					if (nightMode) {
						canvas.drawARGB(255, 100, 100, 100);
					} else {
						canvas.drawARGB(255, 225, 225, 225);
					}
					drawOverMap(canvas, currentViewport, new DrawSettings(nightMode, updateVectorRendering), false);
				} finally {
					holder.unlockCanvasAndPost(canvas);
				}
			}
			if (MEASURE_FPS) {
				fpsMeasureMs += SystemClock.elapsedRealtime() - ms;
				fpsMeasureCount++;
				if (fpsMeasureCount > 10 || (ms - fpsFirstMeasurement) > 400) {
					fpsFirstMeasurement = ms;
					fps = (1000f * fpsMeasureCount / fpsMeasureMs);
					fpsMeasureCount = 0;
					fpsMeasureMs = 0;
				}
			}
		}
	}

	public boolean isMeasureFPS() {
		return MEASURE_FPS;
	}
	
	public void setMeasureFPS(boolean measureFPS) {
		MEASURE_FPS = measureFPS;
	}
	
	public float getFPS(){
		return fps;
	}
	
	private void drawOverMap(Canvas canvas, RotatedTileBox tileBox, DrawSettings drawSettings, boolean
	                         onPrepareImage) {
		final QuadPoint c = tileBox.getCenterPixelPoint();

		// long prev = System.currentTimeMillis();

		for (int i = 0; i < layers.size(); i++) {
			try {
				
				OsmandMapLayer layer = layers.get(i);
				canvas.save();
				// rotate if needed
				if (!layer.drawInScreenPixels()) {
					canvas.rotate(tileBox.getRotate(), c.x, c.y);
				}
				if(onPrepareImage) {
					layer.onPrepareBufferImage(canvas, tileBox, drawSettings);
				} else {
					layer.onDraw(canvas, tileBox, drawSettings);
				}
				canvas.restore();
			} catch (IndexOutOfBoundsException e) {
				// skip it
			}
		}
		if (showMapPosition && !onPrepareImage) {
			canvas.drawCircle(c.x, c.y, 3 * dm.density, paintCenter);
			canvas.drawCircle(c.x, c.y, 7 * dm.density, paintCenter);
		}
	}

	public boolean mapIsRefreshing() {
		return handler.hasMessages(1);
	}

	// this method could be called in non UI thread
	public void refreshMap() {
		refreshMap(false);
	}
	
	// this method could be called in non UI thread
	public void refreshMap(final boolean updateVectorRendering) {
		if (!handler.hasMessages(1) || updateVectorRendering) {
			handler.removeMessages(1);
			Message msg = Message.obtain(handler, new Runnable() {
				@Override
				public void run() {
					refreshMapInternal(updateVectorRendering);
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

	public net.osmand.data.RotatedTileBox getCurrentRotatedTileBox() {
		return currentViewport;
	}

	public float getDensity() {
		return currentViewport.getDensity();
	}

	public float getScaleCoefficient() {
		float scaleCoefficient = getDensity();
		if (Math.min(dm.widthPixels / (dm.density * 160), dm.heightPixels / (dm.density * 160)) > 2.5f) {
			// large screen
			scaleCoefficient *= 1.5f;
		}
		return scaleCoefficient;
	}
	/**
	 * These methods do not consider rotating
	 */
	protected void dragToAnimate(float fromX, float fromY, float toX, float toY, boolean notify) {
		float dx = (fromX - toX);
		float dy = (fromY - toY);
		moveTo(dx, dy);
		if (locationListener != null && notify) {
			locationListener.locationChanged(getLatitude(), getLongitude(), this);
		}
	}

	protected void rotateToAnimate(float rotate) {
		if (isMapRotateEnabled()) {
			this.rotate = MapUtils.unifyRotationTo360(rotate);
			currentViewport.setRotate(this.rotate);
			refreshMap();
		}
	}
	
	protected void setLatLonAnimate(double latitude, double longitude, boolean notify) {
		currentViewport.setLatLonCenter(latitude, longitude);
		refreshMap();
		if (locationListener != null && notify) {
			locationListener.locationChanged(latitude, longitude, this);
		}
	}

	protected void setZoomAnimate(int zoom, float zoomScale, boolean notify) {
		currentViewport.setZoom(zoom, zoomScale, 0);
		refreshMap();
		if (locationListener != null && notify) {
			locationListener.locationChanged(getLatitude(), getLongitude(), this);
		}
	}
	
	// for internal usage
	protected void zoomToAnimate(float tzoom, boolean notify) {
		int zoom = getZoom();
		float zoomToAnimate = tzoom - zoom - getZoomScale();
		if(zoomToAnimate >= 1) {
			zoom += (int) zoomToAnimate;
			zoomToAnimate -= (int) zoomToAnimate;
		}
		while (zoomToAnimate < 0) {
			zoom--;
			zoomToAnimate += 1;
		}
		if (mainLayer != null && mainLayer.getMaximumShownMapZoom() >= zoom && mainLayer.getMinimumShownMapZoom() <= zoom) {
			currentViewport.setZoomAndAnimation(zoom,  zoomToAnimate);
			currentViewport.setRotate(zoom > LOWEST_ZOOM_TO_ROTATE ? rotate : 0 );
			refreshMap();
			if (notify && locationListener != null) {
				locationListener.locationChanged(getLatitude(), getLongitude(), this);
			}
		}
	}

	public void moveTo(float dx, float dy) {
		final RotatedTileBox tb = currentViewport;
		final QuadPoint cp = currentViewport.getCenterPixelPoint();
		final LatLon latlon = currentViewport.getLatLonFromPixel(cp.x + dx, cp.y + dy);
		currentViewport.setLatLonCenter(latlon.getLatitude(), latlon.getLongitude());
		refreshMap();
		// do not notify here listener

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			animatedDraggingThread.stopAnimating();
		}
		for(int i=layers.size() - 1; i >= 0; i--) {
			if(layers.get(i).onTouchEvent(event, getCurrentRotatedTileBox())) {
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

	public void setAccessibilityActions(AccessibilityActionsProvider actions) {
		accessibilityActions = actions;
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
		private PointF initialMultiTouchCenterPoint;
		private RotatedTileBox initialViewport;
		private float x1;
		private float y1;
		private float x2;
		private float y2;
		private LatLon initialCenterLatLon;
		private boolean startRotating = false;

		@Override
		public void onZoomEnded(double relativeToStart, float angleRelative) {
			// 1.5 works better even on dm.density=1 devices
			float dz = (float) (Math.log(relativeToStart) / Math.log(2)) * 1.5f;
			setIntZoom(Math.round(dz) + initialViewport.getZoom());
			if(Math.abs(angleRelative) < 17){
				angleRelative = 0;
			}
			rotateToAnimate(initialViewport.getRotate() + angleRelative);
			final int newZoom = getZoom();
			if (application.getInternalAPI().accessibilityEnabled()) {
				if (newZoom != initialViewport.getZoom()) {
					showMessage(getContext().getString(R.string.zoomIs) + " " + newZoom); //$NON-NLS-1$
				} else {
					final LatLon p1 = initialViewport.getLatLonFromPixel(x1, y1);
					final LatLon p2 = initialViewport.getLatLonFromPixel(x2, y2);
					showMessage(OsmAndFormatter.getFormattedDistance((float)MapUtils.getDistance(p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude()), application));
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
		public void onZoomStarted(PointF centerPoint) {
			initialMultiTouchCenterPoint = centerPoint;
			initialViewport = getCurrentRotatedTileBox().copy();
			initialCenterLatLon = initialViewport.getLatLonFromPixel(initialMultiTouchCenterPoint.x,
					initialMultiTouchCenterPoint.y);
			startRotating = false;
		}

		@Override
		public void onZoomingOrRotating(double relativeToStart, float relAngle) {
			double dz = (Math.log(relativeToStart) / Math.log(2)) * 1.5;
			if (Math.abs(dz) <= (startRotating ? 0.05 : 0.05)) {
				// keep only rotating
				dz = 0;
			}
			if(Math.abs(relAngle) < 17 && !startRotating) {
				relAngle = 0;
			} else {
				startRotating = true;
			}
			if(dz != 0 || relAngle != 0) {
				changeZoomPosition((float) dz, relAngle);
			}

		}
		
		private void changeZoomPosition(float dz, float angle) {
			final QuadPoint cp = initialViewport.getCenterPixelPoint();
			float dx = cp.x - initialMultiTouchCenterPoint.x ;
			float dy = cp.y - initialMultiTouchCenterPoint.y ;
			final RotatedTileBox calc = initialViewport.copy();
			calc.setLatLonCenter(initialCenterLatLon.getLatitude(), initialCenterLatLon.getLongitude());

			float calcZoom = initialViewport.getZoom() + dz + initialViewport.getZoomScale();
			float calcRotate = calc.getRotate() + angle;
			calc.setRotate(angle);
			calc.setZoomAnimation(dz);
			final LatLon r = calc.getLatLonFromPixel(cp.x + dx, cp.y + dy);
			setLatLon(r.getLatitude(), r.getLongitude());
			if (Math.abs(currentViewport.getZoomAnimation() + currentViewport.getZoom()  + currentViewport.getZoomScale() -
				calcZoom) > 0.1) {
				zoomToAnimate(calcZoom, true);
			}
			if(currentViewport.getRotate() != calcRotate){
				rotateToAnimate(calcRotate);
			}
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
			if ((accessibilityActions != null) && accessibilityActions.onLongClick(point, getCurrentRotatedTileBox() )) {
				return;
			}
			for (int i = layers.size() - 1; i >= 0; i--) {
				if (layers.get(i).onLongPressEvent(point, getCurrentRotatedTileBox())) {
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
			if ((accessibilityActions != null) && accessibilityActions.onClick(point, getCurrentRotatedTileBox())) {
				return true;
			}
			for (int i = layers.size() - 1; i >= 0; i--) {
				if (layers.get(i).onSingleTap(point, getCurrentRotatedTileBox())) {
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
			final RotatedTileBox tb = getCurrentRotatedTileBox();
			final double lat = tb.getLatFromPixel(e.getX(), e.getY());
			final double lon = tb.getLonFromPixel(e.getX(), e.getY());
			getAnimatedDraggingThread().startMoving(lat, lon, getZoom() + 1, true);
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
