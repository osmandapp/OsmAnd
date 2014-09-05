package net.osmand.plus.views;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.view.*;
import net.osmand.PlatformUtil;
import net.osmand.access.AccessibilityActionsProvider;
import net.osmand.access.AccessibleToast;
import net.osmand.access.MapExplorer;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.data.QuadPointDouble;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.IMapLocationListener;
import net.osmand.map.MapTileDownloader.DownloadRequest;
import net.osmand.map.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.helpers.SimpleTwoFingerTapDetector;
import net.osmand.plus.views.MultiTouchSupport.MultiTouchZoomListener;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.SurfaceHolder.Callback;
import android.widget.Toast;

public class OsmandMapTileView extends SurfaceView implements IMapDownloaderCallback, Callback {

	private static final int MAP_REFRESH_MESSAGE = OsmAndConstants.UI_HANDLER_MAP_VIEW + 4;
	private static final int MAP_FORCE_REFRESH_MESSAGE = OsmAndConstants.UI_HANDLER_MAP_VIEW + 5;
	private static final int BASE_REFRESH_MESSAGE = OsmAndConstants.UI_HANDLER_MAP_VIEW + 3;
	protected final static int LOWEST_ZOOM_TO_ROTATE = 9;
	private boolean MEASURE_FPS = false;
	private FPSMeasurement main = new FPSMeasurement();
	private FPSMeasurement additional = new FPSMeasurement();

	private class FPSMeasurement {
		int fpsMeasureCount = 0;
		int fpsMeasureMs = 0;
		long fpsFirstMeasurement = 0;
		float fps;

		void calculateFPS(long start, long end) {
			fpsMeasureMs += end - start;
			fpsMeasureCount++;
			if (fpsMeasureCount > 10 || (start - fpsFirstMeasurement) > 400) {
				fpsFirstMeasurement = start;
				fps = (1000f * fpsMeasureCount / fpsMeasureMs);
				fpsMeasureCount = 0;
				fpsMeasureMs = 0;
			}
		}
	}


	protected static final int emptyTileDivisor = 16;


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
	protected Handler handler;
	private Handler baseHandler;

	private AnimateDraggingMapThread animatedDraggingThread;

	private GestureDetector gestureDetector;

	private MultiTouchSupport multiTouchSupport;

	Paint paintGrayFill;
	Paint paintBlackFill;
	Paint paintWhiteFill;
	Paint paintCenter;

	private DisplayMetrics dm;

	private final OsmandApplication application;

	protected OsmandSettings settings = null;

	private Bitmap bufferBitmap;
	private RotatedTileBox bufferImgLoc;
	private Bitmap bufferBitmapTmp;

	private Paint paintImg;

	private boolean afterTwoFingerTap = false;
	SimpleTwoFingerTapDetector twoFingerTapDetector = new SimpleTwoFingerTapDetector() {
		@Override
		public void onTwoFingerTap() {
			afterTwoFingerTap = true;
			getAnimatedDraggingThread().startZooming(getZoom()-1,true);
		}
	};


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

		paintBlackFill = new Paint();
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

		paintImg = new Paint();
		paintImg.setFilterBitmap(true);
//		paintImg.setDither(true);

		setClickable(true);
		setLongClickable(true);
		setFocusable(true);


		handler = new Handler();

		baseHandler = new Handler(application.getResourceManager().getRenderingBufferImageThread().getLooper());
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
		return application.accessibilityEnabled() ? false : super.onKeyDown(keyCode, event);
	}

	public boolean isLayerVisible(OsmandMapLayer layer) {
		return layers.contains(layer);
	}

	public float getZorder(OsmandMapLayer layer) {
		Float z = zOrders.get(layer);
		if (z == null) {
			return 10;
		}
		return z;
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
		while (layers.remove(layer)) ;
		zOrders.remove(layer);
		layer.destroyLayer();
	}
	
	public synchronized void removeAllLayers() {
		while(layers.size() > 0) {
			removeLayer(layers.get(0));
		}
	}

	public List<OsmandMapLayer> getLayers() {
		return layers;
	}

	@SuppressWarnings("unchecked")
	public <T extends OsmandMapLayer> T getLayerByClass(Class<T> cl) {
		for (OsmandMapLayer lr : layers) {
			if (cl.isInstance(lr)) {
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
			currentViewport.setRotate(zoom > LOWEST_ZOOM_TO_ROTATE ? rotate : 0);
			refreshMap();
		}
	}

	public void setComplexZoom(int zoom, float scale) {
		if (mainLayer != null && zoom <= mainLayer.getMaximumShownMapZoom() && zoom >= mainLayer.getMinimumShownMapZoom()) {
			animatedDraggingThread.stopAnimating();
			currentViewport.setZoom(zoom, scale, 0);
			currentViewport.setRotate(zoom > LOWEST_ZOOM_TO_ROTATE ? rotate : 0);
			refreshMap();
		}
	}


	public boolean isMapRotateEnabled() {
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

	public float getSettingsZoomScale() {
		return getSettings().getSettingsZoomScale(getDensity());
	}

	public float getZoomScale() {
		return currentViewport.getZoomScale();
	}

	public boolean isZooming() {
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

	public OsmandSettings getSettings() {
		if (settings == null) {
			settings = getApplication().getSettings();
		}
		return settings;
	}

	private void drawBasemap(Canvas canvas) {
		if (bufferImgLoc != null) {
			float rot = -bufferImgLoc.getRotate();
			canvas.rotate(rot, currentViewport.getCenterPixelX(), currentViewport.getCenterPixelY());
			final RotatedTileBox calc = currentViewport.copy();
			calc.setRotate(bufferImgLoc.getRotate());

			int cz = getZoom();
			QuadPointDouble lt = bufferImgLoc.getLeftTopTile(cz);
			QuadPointDouble rb = bufferImgLoc.getRightBottomTile(cz);
			final float x1 = calc.getPixXFromTile(lt.x, lt.y, cz);
			final float x2 = calc.getPixXFromTile(rb.x, rb.y, cz);
			final float y1 = calc.getPixYFromTile(lt.x, lt.y, cz);
			final float y2 = calc.getPixYFromTile(rb.x, rb.y, cz);
			if (!bufferBitmap.isRecycled()) {
				RectF rct = new RectF(x1, y1, x2, y2);
				canvas.drawBitmap(bufferBitmap, null, rct, paintImg);
			}
			canvas.rotate(-rot, currentViewport.getCenterPixelX(), currentViewport.getCenterPixelY());
		}
	}

	private void refreshBaseMapInternal(RotatedTileBox tileBox, DrawSettings drawSettings) {
		if (tileBox.getPixHeight() == 0 || tileBox.getPixWidth() == 0) {
			return;
		}
		if (bufferBitmapTmp == null || tileBox.getPixHeight() != bufferBitmapTmp.getHeight()
				|| tileBox.getPixWidth() != bufferBitmapTmp.getWidth()) {
			bufferBitmapTmp = Bitmap.createBitmap(tileBox.getPixWidth(), tileBox.getPixHeight(), Config.RGB_565);
		}
		long start = SystemClock.elapsedRealtime();
		final QuadPoint c = tileBox.getCenterPixelPoint();
		Canvas canvas = new Canvas(bufferBitmapTmp);
		fillCanvas(canvas, drawSettings);
		for (int i = 0; i < layers.size(); i++) {
			try {
				OsmandMapLayer layer = layers.get(i);
				canvas.save();
				// rotate if needed
				if (!layer.drawInScreenPixels()) {
					canvas.rotate(tileBox.getRotate(), c.x, c.y);
				}
				layer.onPrepareBufferImage(canvas, tileBox, drawSettings);
				canvas.restore();
			} catch (IndexOutOfBoundsException e) {
				// skip it
			}
		}
		Bitmap t = bufferBitmap;
		synchronized (this) {
			bufferImgLoc = tileBox;
			bufferBitmap = bufferBitmapTmp;
			bufferBitmapTmp = t;
		}
		long end = SystemClock.elapsedRealtime();
		additional.calculateFPS(start, end);
	}

	private void refreshMapInternal(DrawSettings drawSettings) {
		SurfaceHolder holder = getHolder();
		long ms = SystemClock.elapsedRealtime();
		synchronized (holder) {
			Canvas canvas = holder.lockCanvas();
			if (canvas != null) {
				try {
					final float ratioy = mapPosition == OsmandSettings.BOTTOM_CONSTANT ? 0.85f : 0.5f;
					final int cy = (int) (ratioy * getHeight());
					if (currentViewport.getPixWidth() != getWidth() || currentViewport.getPixHeight() != getHeight() ||
							currentViewport.getCenterPixelY() != cy) {
						currentViewport.setPixelDimensions(getWidth(), getHeight(), 0.5f, ratioy);
						refreshBufferImage(drawSettings);
					}
					// make copy to avoid concurrency 
					RotatedTileBox viewportToDraw = currentViewport.copy();
					fillCanvas(canvas, drawSettings);
					drawOverMap(canvas, viewportToDraw, drawSettings);
				} finally {
					holder.unlockCanvasAndPost(canvas);
				}
			}
			if (MEASURE_FPS) {
				main.calculateFPS(ms, SystemClock.elapsedRealtime());
			}
		}
	}

	private void fillCanvas(Canvas canvas, DrawSettings drawSettings) {
		if (drawSettings.isNightMode()) {
			canvas.drawARGB(255, 100, 100, 100);
		} else {
			canvas.drawARGB(255, 225, 225, 225);
		}
	}


	public boolean isMeasureFPS() {
		return MEASURE_FPS;
	}

	public void setMeasureFPS(boolean measureFPS) {
		MEASURE_FPS = measureFPS;
	}

	public float getFPS() {
		return main.fps;
	}

	public float getSecondaryFPS() {
		return additional.fps;
	}

	private void drawOverMap(Canvas canvas, RotatedTileBox tileBox, DrawSettings drawSettings) {
		final QuadPoint c = tileBox.getCenterPixelPoint();
		synchronized (this) {
			if (bufferBitmap != null && !bufferBitmap.isRecycled()) {
				canvas.save();
				canvas.rotate(tileBox.getRotate(), c.x, c.y);
				drawBasemap(canvas);
				canvas.restore();
			}
		}

		for (int i = 0; i < layers.size(); i++) {
			try {
				OsmandMapLayer layer = layers.get(i);
				canvas.save();
				// rotate if needed
				if (!layer.drawInScreenPixels()) {
					canvas.rotate(tileBox.getRotate(), c.x, c.y);
				}
				layer.onDraw(canvas, tileBox, drawSettings);
				canvas.restore();
			} catch (IndexOutOfBoundsException e) {
				// skip it
			}
		}
		if (showMapPosition) {
			canvas.drawCircle(c.x, c.y, 3 * dm.density, paintCenter);
			canvas.drawCircle(c.x, c.y, 7 * dm.density, paintCenter);
		}
	}

	private void refreshBufferImage(final DrawSettings drawSettings) {
		if (!baseHandler.hasMessages(BASE_REFRESH_MESSAGE) || drawSettings.isUpdateVectorRendering()) {
			Message msg = Message.obtain(baseHandler, new Runnable() {
				@Override
				public void run() {
					baseHandler.removeMessages(BASE_REFRESH_MESSAGE);
					try {
						DrawSettings param = drawSettings;
						if (handler.hasMessages(MAP_FORCE_REFRESH_MESSAGE)) {
							if (!param.isUpdateVectorRendering()) {
								param = new DrawSettings(drawSettings.isNightMode(), true);
							}
							handler.removeMessages(MAP_FORCE_REFRESH_MESSAGE);
						}
						refreshBaseMapInternal(currentViewport.copy(), param);
						sendRefreshMapMsg(param, 0);
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				}
			});
			msg.what = drawSettings.isUpdateVectorRendering() ? MAP_FORCE_REFRESH_MESSAGE : BASE_REFRESH_MESSAGE;
			// baseHandler.sendMessageDelayed(msg, 0);
			baseHandler.sendMessage(msg);
		}
	}

	// this method could be called in non UI thread
	public void refreshMap() {
		refreshMap(false);
	}

	// this method could be called in non UI thread
	public void refreshMap(final boolean updateVectorRendering) {
		if (isShown()) {
			boolean nightMode = application.getDaynightHelper().isNightMode();
			DrawSettings drawSettings = new DrawSettings(nightMode, updateVectorRendering);
			sendRefreshMapMsg(drawSettings, 20);
			refreshBufferImage(drawSettings);
		}
	}

	private void sendRefreshMapMsg(final DrawSettings drawSettings, int delay) {
		if (!handler.hasMessages(MAP_REFRESH_MESSAGE) || drawSettings.isUpdateVectorRendering()) {
			Message msg = Message.obtain(handler, new Runnable() {
				@Override
				public void run() {
					DrawSettings param = drawSettings;
					handler.removeMessages(MAP_REFRESH_MESSAGE);

					refreshMapInternal(param);
				}
			});
			msg.what = MAP_REFRESH_MESSAGE;
			if (delay > 0) {
				handler.sendMessageDelayed(msg, delay);
			} else {
				handler.sendMessage(msg);
			}
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
		if (zoomToAnimate >= 1) {
			zoom += (int) zoomToAnimate;
			zoomToAnimate -= (int) zoomToAnimate;
		}
		while (zoomToAnimate < 0) {
			zoom--;
			zoomToAnimate += 1;
		}
		if (mainLayer != null && mainLayer.getMaximumShownMapZoom() >= zoom && mainLayer.getMinimumShownMapZoom() <= zoom) {
			currentViewport.setZoomAndAnimation(zoom, zoomToAnimate);
			currentViewport.setRotate(zoom > LOWEST_ZOOM_TO_ROTATE ? rotate : 0);
			refreshMap();
			if (notify && locationListener != null) {
				locationListener.locationChanged(getLatitude(), getLongitude(), this);
			}
		}
	}

	public void moveTo(float dx, float dy) {
		final QuadPoint cp = currentViewport.getCenterPixelPoint();
		final LatLon latlon = currentViewport.getLatLonFromPixel(cp.x + dx, cp.y + dy);
		currentViewport.setLatLonCenter(latlon.getLatitude(), latlon.getLongitude());
		refreshMap();
		// do not notify here listener

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (twoFingerTapDetector.onTouchEvent(event)) {
			return true;
		}

		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			animatedDraggingThread.stopAnimating();
		}
		for (int i = layers.size() - 1; i >= 0; i--) {
			if (layers.get(i).onTouchEvent(event, getCurrentRotatedTileBox())) {
				return true;
			}
		}
		if (!multiTouchSupport.onTouchEvent(event)) {
			/* return */
			gestureDetector.onTouchEvent(event);
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
		private static final float ANGLE_THRESHOLD = 15;

		@Override
		public void onZoomEnded(double relativeToStart, float angleRelative) {
			// 1.5 works better even on dm.density=1 devices
			float dz = (float) (Math.log(relativeToStart) / Math.log(2)) * 1.5f;
			setIntZoom(Math.round(dz) + initialViewport.getZoom());
			if (Math.abs(angleRelative) < ANGLE_THRESHOLD * relativeToStart ||
					Math.abs(angleRelative) < ANGLE_THRESHOLD / relativeToStart) {
				angleRelative = 0;
			}
			rotateToAnimate(initialViewport.getRotate() + angleRelative);
			final int newZoom = getZoom();
			if (application.accessibilityEnabled()) {
				if (newZoom != initialViewport.getZoom()) {
					showMessage(getContext().getString(R.string.zoomIs) + " " + newZoom); //$NON-NLS-1$
				} else {
					final LatLon p1 = initialViewport.getLatLonFromPixel(x1, y1);
					final LatLon p2 = initialViewport.getLatLonFromPixel(x2, y2);
					showMessage(OsmAndFormatter.getFormattedDistance((float) MapUtils.getDistance(p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude()), application));
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
			if (Math.abs(dz) <= 0.1) {
				// keep only rotating
				dz = 0;
			}
			if (Math.abs(relAngle) < ANGLE_THRESHOLD && !startRotating) {
				relAngle = 0;
			} else {
				startRotating = true;
			}
			if (dz != 0 || relAngle != 0) {
				changeZoomPosition((float) dz, relAngle);
			}

		}

		private void changeZoomPosition(float dz, float angle) {
			final QuadPoint cp = initialViewport.getCenterPixelPoint();
			float dx = cp.x - initialMultiTouchCenterPoint.x;
			float dy = cp.y - initialMultiTouchCenterPoint.y;
			final RotatedTileBox calc = initialViewport.copy();
			calc.setLatLonCenter(initialCenterLatLon.getLatitude(), initialCenterLatLon.getLongitude());

			float calcZoom = initialViewport.getZoom() + dz + initialViewport.getZoomScale();
			float calcRotate = calc.getRotate() + angle;
			calc.setRotate(calcRotate);
			calc.setZoomAnimation(dz);
			final LatLon r = calc.getLatLonFromPixel(cp.x + dx, cp.y + dy);
			setLatLon(r.getLatitude(), r.getLongitude());
			zoomToAnimate(calcZoom, true);
			rotateToAnimate(calcRotate);
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
			if (multiTouchSupport.isInZoomMode() || afterTwoFingerTap) {
				afterTwoFingerTap = false;
				return;
			}
			if (log.isDebugEnabled()) {
				log.debug("On long click event " + e.getX() + " " + e.getY()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			PointF point = new PointF(e.getX(), e.getY());
			if ((accessibilityActions != null) && accessibilityActions.onLongClick(point, getCurrentRotatedTileBox())) {
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
