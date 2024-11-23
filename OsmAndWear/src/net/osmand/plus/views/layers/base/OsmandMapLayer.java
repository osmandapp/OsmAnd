package net.osmand.plus.views.layers.base;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.android.MapRendererView.MapRendererViewListener;
import net.osmand.core.jni.MapMarkersCollection;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QListMapMarker;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.render.OsmandRenderer;
import net.osmand.plus.render.OsmandRenderer.RenderingContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.lang.ref.WeakReference;
import java.util.*;

public abstract class OsmandMapLayer implements MapRendererViewListener {
	private static final Log LOG = PlatformUtil.getLog(OsmandMapLayer.class);

	public static final float ICON_VISIBLE_PART_RATIO = 0.45f;
	public static final float TOUCH_RADIUS_MULTIPLIER = 1.5f;

	@NonNull
	private final Context ctx;
	@Nullable
	private MapActivity mapActivity;
	protected OsmandMapTileView view;
	protected boolean mapActivityInvalidated;
	protected boolean mapRendererChanged;
	protected boolean invalidated;

	protected List<LatLon> fullObjectsLatLon;
	protected List<LatLon> smallObjectsLatLon;

	//OpenGL
	protected MapMarkersCollection mapMarkersCollection;
	protected PointI movableObject;
	protected int pointsOrder = 0;
	protected float density = 0f;

	public static class CustomMapObjects<T> {
		protected List<T> customMapObjects;
		private boolean isChanged;

		@NonNull
		public List<T> getMapObjects() {
			if (customMapObjects == null) {
				return Collections.emptyList();
			} else {
				return customMapObjects;
			}
		}

		public void setCustomMapObjects(List<T> customMapObjects) {
			if (this.customMapObjects != customMapObjects) {
				isChanged = true;
			}
			this.customMapObjects = customMapObjects;
		}

		public void acceptChanges() {
			isChanged = false;
		}

		public boolean isChanged() {
			return isChanged;
		}
	}

	public enum MapGestureType {
		DOUBLE_TAP_ZOOM_IN,
		DOUBLE_TAP_ZOOM_CHANGE,
		TWO_POINTERS_ZOOM_OUT,
		TWO_POINTERS_ROTATION,
		TWO_POINTERS_TILT
	}

	protected OsmandMapLayer(@NonNull Context ctx) {
		this.ctx = ctx;
	}

	public int getBaseOrder() {
		return (int) ((view != null ? view.getZorder(this) : 10f) * -100000f);
	}

	public int getPointsOrder() {
		return pointsOrder != 0 ? pointsOrder : getBaseOrder();
	}

	public void setPointsOrder(float pointsZorder) {
		this.pointsOrder = (int) (pointsZorder * -100000f);
	}

	@NonNull
	public Context getContext() {
		return ctx;
	}

	@Nullable
	public OsmandMapTileView getTileView() {
		return view;
	}

	public boolean hasMapRenderer() {
		return getMapRenderer() != null;
	}

	@Nullable
	public MapRendererView getMapRenderer() {
		return view != null ? view.getMapRenderer() : null;
	}

	@Nullable
	public MapActivity getMapActivity() {
		return mapActivity;
	}

	public void setInvalidated(boolean invalidated) {
		this.invalidated = invalidated;
	}

	public void setMapActivity(@Nullable MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		MapRendererView mapRenderer = getMapRenderer();
		if (mapActivity != null) {
			if (mapRenderer != null) {
				mapRenderer.addListener(this);
			}
			mapActivityInvalidated = true;
		} else {
			if (mapRenderer != null) {
				mapRenderer.removeListener(this);
			}
		}
	}

	public void onMapRendererChange(@Nullable MapRendererView currentMapRenderer,
	                                @Nullable MapRendererView newMapRenderer) {
		if (newMapRenderer == null) {
			cleanupResources();
		} else {
			mapRendererChanged = true;
		}
	}

	@NonNull
	public MapActivity requireMapActivity() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			throw new IllegalStateException("Layer " + this + " not attached to MapActivity.");
		}
		return mapActivity;
	}

	@NonNull
	public OsmandApplication getApplication() {
		return (OsmandApplication) ctx.getApplicationContext();
	}

	public String getString(@StringRes int resId) {
		return ctx.getString(resId);
	}

	public String getString(@StringRes int resId, Object... formatArgs) {
		return ctx.getString(resId, formatArgs);
	}

	@ColorInt
	protected int getColor(@ColorRes int resId) {
		return ColorUtilities.getColor(getContext(), resId);
	}

	public OsmandMapTileView getMapView() {
		return getApplication().getOsmandMap().getMapView();
	}

	public boolean isMapGestureAllowed(MapGestureType type) {
		return true;
	}

	public boolean areMapRendererViewEventsAllowed() {
		return false;
	}

	public void initLayer(@NonNull OsmandMapTileView view) {
		this.view = view;
	}

	public abstract void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings);

	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null && areMapRendererViewEventsAllowed()) {
			mapRenderer.addListener(this);
		}
		float density = getContext().getResources().getDisplayMetrics().density;
		if (this.density != density) {
			this.density = density;
			updateResources();
		}
	}

	protected void updateResources() {

	}

	@Override
	public void onUpdateFrame(MapRendererView mapRenderer) {
	}

	@Override
	public void onFrameReady(MapRendererView mapRenderer) {
	}

	public void destroyLayer() {
		cleanupResources();
	}

	protected void cleanupResources() {
		clearMapMarkersCollections();
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null && areMapRendererViewEventsAllowed()) {
			mapRenderer.removeListener(this);
		}
	}

	public void populateObjectContextMenu(@NonNull LatLon latLon, @Nullable Object o, @NonNull ContextMenuAdapter adapter) {
	}

	public boolean onSingleTap(@NonNull PointF point, @NonNull RotatedTileBox tileBox) {
		return false;
	}

	public boolean onLongPressEvent(@NonNull PointF point, @NonNull RotatedTileBox tileBox) {
		return false;
	}

	public boolean onTouchEvent(@NonNull MotionEvent event, @NonNull RotatedTileBox tileBox) {
		return false;
	}

	@SafeVarargs
	public final <Params> void executeTaskInBackground(AsyncTask<Params, ?, ?> task, Params... params) {
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
	}

	public boolean isPresentInFullObjects(LatLon latLon) {
		if (fullObjectsLatLon == null) {
			return true;
		} else if (latLon != null) {
			return fullObjectsLatLon.contains(latLon);
		}
		return false;
	}

	public boolean isPresentInSmallObjects(LatLon latLon) {
		if (smallObjectsLatLon != null && latLon != null) {
			return smallObjectsLatLon.contains(latLon);
		}
		return false;
	}

	/**
	 * This method returns whether canvas should be rotated as
	 * map rotated before {@link #onDraw(Canvas, RotatedTileBox, DrawSettings)} and
	 * {@link #onPrepareBufferImage(Canvas, RotatedTileBox, DrawSettings)}.
	 * If the layer draws simply layer over screen (not over map)
	 * it should return true.
	 */
	public abstract boolean drawInScreenPixels();

	public static class DrawSettings {

		public long mapRefreshTimestamp;

		private final boolean nightMode;
		private final boolean updateVectorRendering;
		private final float density;

		public DrawSettings(boolean nightMode) {
			this(nightMode, false, 0);
		}

		public DrawSettings(boolean nightMode, boolean updateVectorRendering) {
			this(nightMode, updateVectorRendering, 0);
		}

		public DrawSettings(boolean nightMode, boolean updateVectorRendering, float density) {
			this.nightMode = nightMode;
			this.updateVectorRendering = updateVectorRendering;
			this.density = density;
		}

		public boolean isUpdateVectorRendering() {
			return updateVectorRendering;
		}

		public boolean isNightMode() {
			return nightMode;
		}

		public float getDensity() {
			return density;
		}
	}

	@NonNull
	public static QuadTree<QuadRect> initBoundIntersections(@NonNull RotatedTileBox tileBox) {
		return initBoundIntersections(tileBox.getPixWidth(), tileBox.getPixHeight());
	}

	@NonNull
	public static QuadTree<QuadRect> initBoundIntersections(float width, float height) {
		QuadRect bounds = new QuadRect(0, 0, width, height);
		bounds.inset(-bounds.width() / 4, -bounds.height() / 4);
		return new QuadTree<>(bounds, 4, 0.6f);
	}

	public static boolean intersects(@NonNull QuadTree<QuadRect> boundIntersections, float x, float y, float width, float height) {
		QuadRect visibleRect = calculateRect(x, y, width, height);
		return intersects(boundIntersections, visibleRect, true);
	}

	public static boolean intersects(@NonNull QuadTree<QuadRect> boundIntersections, @NonNull QuadRect visibleRect, boolean insert) {
		List<QuadRect> result = new ArrayList<>();
		boundIntersections.queryInBox(new QuadRect(visibleRect), result);
		for (QuadRect rect : result) {
			if (QuadRect.intersects(rect, visibleRect)) {
				return true;
			}
		}
		if (insert) {
			boundIntersections.insert(visibleRect, new QuadRect(visibleRect));
		}
		return false;
	}

	public QuadRect getCorrectedQuadRect(QuadRect latlonRect) {
		double topLatitude = latlonRect.top;
		double leftLongitude = latlonRect.left;
		double bottomLatitude = latlonRect.bottom;
		double rightLongitude = latlonRect.right;
		// double lat = 0;
		// double lon = 0;
		// this is buggy lat/lon should be 0 but in that case
		// it needs to be fixed in case there is no route points in the view bbox
		double lat = topLatitude - bottomLatitude + 0.1;
		double lon = rightLongitude - leftLongitude + 0.1;
		return new QuadRect(leftLongitude - lon, topLatitude + lat, rightLongitude + lon, bottomLatitude - lat);
	}

	public static QuadRect calculateRect(float x, float y, float width, float height) {
		QuadRect rf;
		double left = x - width / 2.0d;
		double top = y - height / 2.0d;
		double right = left + width;
		double bottom = top + height;
		rf = new QuadRect(left, top, right, bottom);
		return rf;
	}

	protected float getIconSize(OsmandApplication app) {
		return app.getResources().getDimensionPixelSize(R.dimen.favorites_icon_outline_size) * ICON_VISIBLE_PART_RATIO * getTextScale();
	}

	public Rect getIconDestinationRect(float x, float y, int width, int height, float scale) {
		int scaledWidth = width;
		int scaledHeight = height;
		if (scale != 1.0f) {
			scaledWidth = (int) (width * scale);
			scaledHeight = (int) (height * scale);
		}
		Rect rect = new Rect(0, 0, scaledWidth, scaledHeight);
		rect.offset((int) x - scaledWidth / 2, (int) y - scaledHeight / 2);
		return rect;
	}

	public static int getScaledTouchRadius(@NonNull OsmandApplication app, int radiusPoi) {
		float textScale = getTextScale(app);
		if (textScale < 1.0f) {
			textScale = 1.0f;
		}
		return (int) textScale * radiusPoi;
	}

	public static void setMapButtonIcon(@NonNull ImageView imageView, @Nullable Drawable icon, @NonNull ScaleType scaleType) {
		int btnSizePx = imageView.getLayoutParams().height;
		int iconSizePx = imageView.getContext().getResources().getDimensionPixelSize(R.dimen.map_widget_icon);
		int iconPadding = (btnSizePx - iconSizePx) / 2;
		imageView.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
		imageView.setScaleType(scaleType);
		imageView.setImageDrawable(icon);
	}

	@Nullable
	protected Bitmap getScaledBitmap(@DrawableRes int drawableId) {
		return getScaledBitmap(drawableId, getTextScale());
	}

	protected Bitmap getScaledBitmap(@DrawableRes int drawableId, float scale) {
		OsmandApplication app = getApplication();
		MapActivity activity = getMapActivity();
		Bitmap bitmap = BitmapFactory.decodeResource(activity == null ? app.getResources() : activity.getResources(), drawableId);
		if (bitmap != null && scale != 1f && scale > 0) {
			bitmap = AndroidUtils.scaleBitmap(bitmap,
					(int) (bitmap.getWidth() * scale), (int) (bitmap.getHeight() * scale), false);
		}
		return bitmap;
	}

	public float getTextScale() {
		return getTextScale(getApplication());
	}

	public float getOriginalTextScale() {
		return getOriginalTextScale(getApplication());
	}

	public static float getTextScale(@NonNull OsmandApplication app) {
		return app.getOsmandMap().getTextScale();
	}

	public static float getOriginalTextScale(@NonNull OsmandApplication app) {
		return app.getOsmandMap().getOriginalTextScale();
	}

	public float getMapDensity() {
		return getApplication().getOsmandMap().getMapDensity();
	}

	/**OpenGL*/
	protected void clearMapMarkersCollections() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null && mapMarkersCollection != null) {
			mapRenderer.removeSymbolsProvider(mapMarkersCollection);
			mapMarkersCollection = null;
		}
	}

	/** OpenGL */
	protected void setMovableObject(double lat, double lon) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null || mapMarkersCollection == null || movableObject != null) {
			return;
		}
		int x = MapUtils.get31TileNumberX(lon);
		int y = MapUtils.get31TileNumberY(lat);
		QListMapMarker markers = mapMarkersCollection.getMarkers();
		for (int i = 0; i < markers.size(); i++) {
			net.osmand.core.jni.MapMarker m = markers.get(i);
			if (m.getPosition().getX() == x && m.getPosition().getY() == y) {
				m.setIsHidden(true);
				movableObject = m.getPosition();
				break;
			}
		}
	}

	/** OpenGL */
	protected void applyMovableObject(@NonNull LatLon position) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null || movableObject == null || mapMarkersCollection == null) {
			return;
		}
		int x = MapUtils.get31TileNumberX(position.getLongitude());
		int y = MapUtils.get31TileNumberY(position.getLatitude());
		QListMapMarker markers = mapMarkersCollection.getMarkers();
		for (int i = 0; i < markers.size(); i++) {
			net.osmand.core.jni.MapMarker m = markers.get(i);
			if (m.getPosition().getX() == movableObject.getX() && m.getPosition().getY() == movableObject.getY()) {
				m.setPosition(new PointI(x, y));
				m.setIsHidden(false);
				movableObject = null;
				break;
			}
		}
	}

	/** OpenGL */
	protected void cancelMovableObject() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null || movableObject == null || mapMarkersCollection == null) {
			return;
		}
		QListMapMarker markers = mapMarkersCollection.getMarkers();
		for (int i = 0; i < markers.size(); i++) {
			net.osmand.core.jni.MapMarker m = markers.get(i);
			if (m.getPosition().getX() == movableObject.getX() && m.getPosition().getY() == movableObject.getY()) {
				m.setIsHidden(false);
				movableObject = null;
				break;
			}
		}
	}

	public static class TileBoxRequest {
		private final int left;
		private final int top;
		private final int right;
		private final int bottom;
		private final int width;
		private final int height;
		private final int zoom;
		private final QuadRect latLonBounds;

		public TileBoxRequest(@NonNull RotatedTileBox tileBox) {
			zoom = tileBox.getZoom();

			QuadRect tilesRect = tileBox.getTileBounds();
			left = (int) Math.floor(tilesRect.left);
			top = (int) Math.floor(tilesRect.top);
			right = (int) Math.ceil(tilesRect.right);
			bottom = (int) Math.ceil(tilesRect.bottom);
			width = (int) Math.ceil(tilesRect.right - left);
			height = (int) Math.ceil(tilesRect.bottom - top);

			latLonBounds = new QuadRect(
					MapUtils.getLongitudeFromTile(zoom, alignTile(left)),
					MapUtils.getLatitudeFromTile(zoom, alignTile(top)),
					MapUtils.getLongitudeFromTile(zoom, alignTile(right)),
					MapUtils.getLatitudeFromTile(zoom, alignTile(bottom)));
		}

		private TileBoxRequest(@NonNull TileBoxRequest request, int tileExtentX, int tileExtentY) {
			zoom = request.zoom;

			left = alignTile(request.left - tileExtentX);
			top = alignTile(request.top - tileExtentY);
			right = alignTile(request.right + tileExtentX);
			bottom = alignTile(request.bottom + tileExtentY);
			width = right - left;
			height = bottom - top;

			latLonBounds = new QuadRect(
					MapUtils.getLongitudeFromTile(zoom, alignTile((double) left)),
					MapUtils.getLatitudeFromTile(zoom, alignTile((double) top)),
					MapUtils.getLongitudeFromTile(zoom, alignTile((double) right)),
					MapUtils.getLatitudeFromTile(zoom, alignTile((double) bottom)));
		}

		private double alignTile(double tile) {
			if (tile < 0) {
				return 0;
			}
			if (tile >= MapUtils.getPowZoom(zoom)) {
				return MapUtils.getPowZoom(zoom) - .000001;
			}
			return tile;
		}

		private int alignTile(int tile) {
			return tile < 0 ? 0 : Math.min(tile, (int) MapUtils.getPowZoom(zoom));
		}

		public TileBoxRequest extend(int tileExtentX, int tileExtentY) {
			return new TileBoxRequest(this, tileExtentX, tileExtentY);
		}

		public int getLeft() {
			return left;
		}

		public int getTop() {
			return top;
		}

		public int getRight() {
			return right;
		}

		public int getBottom() {
			return bottom;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		public int getZoom() {
			return zoom;
		}

		public QuadRect getLatLonBounds() {
			return latLonBounds;
		}

		public boolean contains(@Nullable TileBoxRequest request) {
			return request != null && latLonBounds.contains(request.latLonBounds);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			TileBoxRequest that = (TileBoxRequest) o;
			return left == that.left && top == that.top && width == that.width
					&& height == that.height && zoom == that.zoom;
		}

		@Override
		public int hashCode() {
			return Objects.hash(left, top, width, height, zoom);
		}

		@NonNull
		@Override
		public String toString() {
			return "" + latLonBounds;
		}
	}

	public abstract class MapLayerData<T> {
		public final long DATA_REQUEST_TIMEOUT = 10 * 1000;
		public int ZOOM_THRESHOLD = 1;
		public RotatedTileBox queriedBox;
		public TileBoxRequest queriedRequest;
		protected T results;
		protected Task currentTask;
		protected Task pendingTask;
		private List<WeakReference<DataReadyCallback>> callbacks = new LinkedList<>();

		public class DataReadyCallback {
			private final TileBoxRequest request;
			private T results;
			private boolean ready;
			private final Object sync = new Object();

			public DataReadyCallback(@NonNull TileBoxRequest request) {
				this.request = request;
			}

			public TileBoxRequest getRequest() {
				return request;
			}

			@Nullable
			public T getResults() {
				return results;
			}

			public boolean isReady() {
				return ready;
			}

			public Object getSync() {
				return sync;
			}

			private void onDataReady(@Nullable T results) {
				this.results = results;
				synchronized (sync) {
					ready = true;
					sync.notifyAll();
				}
			}
		}

		public RotatedTileBox getQueriedBox() {
			return queriedBox;
		}

		public TileBoxRequest getQueriedRequest() {
			return queriedRequest;
		}

		public T getResults() {
			return results;
		}

		public DataReadyCallback getDataReadyCallback(@NonNull TileBoxRequest request) {
			return new DataReadyCallback(request);
		}

		public synchronized void addDataReadyCallback(@NonNull DataReadyCallback callback) {
			LinkedList<WeakReference<DataReadyCallback>> ncall = new LinkedList<>(callbacks);
			ncall.add(new WeakReference<>(callback));
			callbacks = ncall;
		}

		public synchronized void removeDataReadyCallback(@NonNull DataReadyCallback callback) {
			LinkedList<WeakReference<DataReadyCallback>> ncall = new LinkedList<>(callbacks);
			Iterator<WeakReference<DataReadyCallback>> it = ncall.iterator();
			while (it.hasNext()) {
				DataReadyCallback c = it.next().get();
				if (c == callback) {
					it.remove();
				}
			}
			callbacks = ncall;
		}

		public synchronized void fireDataReadyCallback(@Nullable T results) {
			for (WeakReference<DataReadyCallback> callback : callbacks) {
				DataReadyCallback c = callback.get();
				if (c != null) {
					c.onDataReady(results);
				}
			}
		}

		public boolean queriedBoxContains(RotatedTileBox queriedData, RotatedTileBox newBox) {
			return queriedData != null && queriedData.containsTileBox(newBox) && Math.abs(queriedData.getZoom() - newBox.getZoom()) <= ZOOM_THRESHOLD;
		}

		public boolean queriedRequestContains(TileBoxRequest queriedRequest, TileBoxRequest newRequest) {
			return queriedRequest != null && queriedRequest.contains(newRequest) && Math.abs(queriedRequest.getZoom() - newRequest.getZoom()) <= ZOOM_THRESHOLD;
		}

		public void queryNewData(@NonNull RotatedTileBox tileBox) {
			if (!queriedBoxContains(queriedBox, tileBox)) {
				Task ct = currentTask;
				if (ct == null || !queriedBoxContains(ct.getExtendedBox(), tileBox)) {
					RotatedTileBox original = tileBox.copy();
					RotatedTileBox extended = original.copy();
					extended.increasePixelDimensions(tileBox.getPixWidth() / 2, tileBox.getPixHeight() / 2);
					Task task = new Task(original, extended);
					if (currentTask == null) {
						executeTaskInBackground(task);
					} else {
						pendingTask = task;
					}
				}
			}
		}

		public void queryNewData(@NonNull TileBoxRequest request) {
			if (!queriedRequestContains(queriedRequest, request)) {
				Task ct = currentTask;
				if (ct == null || !queriedRequestContains(ct.getExtendedBoxRequest(), request)) {
					TileBoxRequest extendedRequest = request.extend(request.width / 2, request.height / 2);
					Task task = new Task(request, extendedRequest);
					if (currentTask == null) {
						executeTaskInBackground(task);
					} else {
						pendingTask = task;
					}
				}
			} else {
				fireDataReadyCallback(results);
			}
		}

		public void layerOnPreExecute() {
		}

		public void layerOnPostExecute() {
		}

		public boolean isInterrupted() {
			return pendingTask != null;
		}

		protected abstract T calculateResult(@NonNull QuadRect latLonBounds, int zoom);

		@SuppressLint("StaticFieldLeak")
		public class Task extends AsyncTask<Object, Object, T> {
			private final RotatedTileBox originalBox;
			private final RotatedTileBox extendedBox;
			private final TileBoxRequest originalBoxRequest;
			private final TileBoxRequest extendedBoxRequest;

			public Task(@NonNull RotatedTileBox originalBox, @NonNull RotatedTileBox extendedBox) {
				this.originalBox = originalBox;
				this.extendedBox = extendedBox;
				this.originalBoxRequest = null;
				this.extendedBoxRequest = null;
			}

			public Task(@NonNull TileBoxRequest originalBoxRequest, @NonNull TileBoxRequest extendedBoxRequest) {
				this.originalBoxRequest = originalBoxRequest;
				this.extendedBoxRequest = extendedBoxRequest;
				this.originalBox = null;
				this.extendedBox = null;
			}

			public RotatedTileBox getOriginalBox() {
				return originalBox;
			}

			public RotatedTileBox getExtendedBox() {
				return extendedBox;
			}

			public TileBoxRequest getOriginalBoxRequest() {
				return originalBoxRequest;
			}

			public TileBoxRequest getExtendedBoxRequest() {
				return extendedBoxRequest;
			}

			@Override
			protected T doInBackground(Object... params) {
				if (extendedBoxRequest != null) {
					if (queriedRequestContains(queriedRequest, extendedBoxRequest)) {
						return null;
					}
					return calculateResult(extendedBoxRequest.getLatLonBounds(), extendedBoxRequest.getZoom());
				} else {
					if (queriedBoxContains(queriedBox, extendedBox)) {
						return null;
					}
					QuadRect bounds = extendedBox.getLatLonBounds();
					return bounds != null ? calculateResult(bounds, extendedBox.getZoom()) : null;
				}
			}

			@Override
			protected void onPreExecute() {
				currentTask = this;
				layerOnPreExecute();
			}

			@Override
			protected void onPostExecute(T result) {
				if (result != null) {
					queriedBox = extendedBox;
					queriedRequest = extendedBoxRequest;
					results = result;
				}
				currentTask = null;
				if (pendingTask != null) {
					executeTaskInBackground(pendingTask);
					pendingTask = null;
				} else {
					fireDataReadyCallback(results);
					layerOnPostExecute();
				}
			}
		}

		public void clearCache() {
			results = null;
			queriedBox = null;
			queriedRequest = null;
		}
	}

	public static class RenderingLineAttributes {
		protected int cachedHash;
		public Paint paint;
		public Paint customColorPaint;
		public int customColor;
		public float customWidth;
		public int defaultWidth;
		public int defaultColor;
		public boolean isPaint2;
		public Paint paint2;
		public int defaultWidth2;
		public boolean isPaint3;
		public Paint paint3;
		public int defaultWidth3;
		public Paint shadowPaint;
		public boolean isShadowPaint;
		public int defaultShadowWidthExtent = 2;
		public Paint paint_1;
		public boolean isPaint_1;
		public int defaultWidth_1;
		private final String renderingAttribute;

		public RenderingLineAttributes(String renderingAttribute) {
			this.renderingAttribute = renderingAttribute;
			paint = initPaint();
			customColorPaint = new Paint(paint);
			paint2 = initPaint();
			paint3 = initPaint();
			paint_1 = initPaint();
			shadowPaint = initPaint();
		}

		private Paint initPaint() {
			Paint paint = new Paint();
			paint.setStyle(Style.STROKE);
			paint.setAntiAlias(true);
			paint.setStrokeCap(Cap.ROUND);
			paint.setStrokeJoin(Join.ROUND);
			return paint;
		}

		public boolean updatePaints(OsmandApplication app, DrawSettings settings, RotatedTileBox tileBox) {
			OsmandRenderer renderer = app.getResourceManager().getRenderer().getRenderer();
			RenderingRulesStorage rrs = app.getRendererRegistry().getCurrentSelectedRenderer();
			boolean isNight = settings != null && settings.isNightMode();
			float density;
			OsmandMapTileView mapView = app.getOsmandMap().getMapView();
			density = mapView.isCarView() ? mapView.getCarViewDensity() : tileBox.getDensity();
			int hsh = calculateHash(rrs, isNight, density);
			if (hsh != cachedHash) {
				cachedHash = hsh;
				if (rrs != null) {
					RenderingRuleSearchRequest req = new RenderingRuleSearchRequest(rrs);
					req.setBooleanFilter(rrs.PROPS.R_NIGHT_MODE, isNight);
					if (req.searchRenderingAttribute(renderingAttribute)) {
						RenderingContext rc = new OsmandRenderer.RenderingContext(app);
						rc.setDensityValue(density);
						// cachedColor = req.getIntPropertyValue(rrs.PROPS.R_COLOR);
						renderer.updatePaint(req, paint, 0, false, rc);
						isPaint2 = renderer.updatePaint(req, paint2, 1, false, rc);
						if (paint2.getStrokeWidth() == 0 && defaultWidth2 != 0) {
							paint2.setStrokeWidth(defaultWidth2);
						}
						isPaint3 = renderer.updatePaint(req, paint3, 2, false, rc);
						if (paint3.getStrokeWidth() == 0 && defaultWidth3 != 0) {
							paint3.setStrokeWidth(defaultWidth3);
						}
						isPaint_1 = renderer.updatePaint(req, paint_1, -1, false, rc);
						if (paint_1.getStrokeWidth() == 0 && defaultWidth_1 != 0) {
							paint_1.setStrokeWidth(defaultWidth_1);
						}
						isShadowPaint = req.isSpecified(rrs.PROPS.R_SHADOW_RADIUS);
						if (isShadowPaint) {
							ColorFilter cf = new PorterDuffColorFilter(
									req.getIntPropertyValue(rrs.PROPS.R_SHADOW_COLOR), Mode.SRC_IN);
							shadowPaint.setColorFilter(cf);
							shadowPaint.setStrokeWidth(paint.getStrokeWidth() + defaultShadowWidthExtent
									* rc.getComplexValue(req, rrs.PROPS.R_SHADOW_RADIUS));
						}
					} else {
						System.err.println("Rendering attribute route is not found !");
					}
					updateDefaultColor(paint, defaultColor);
					if (paint.getStrokeWidth() == 0 && defaultWidth != 0) {
						paint.setStrokeWidth(defaultWidth);
					}
					customColorPaint = new Paint(paint);
				}
				return true;
			}
			return false;
		}

		private void updateDefaultColor(Paint paint, int defaultColor) {
			if ((paint.getColor() == 0 || paint.getColor() == Color.BLACK) && defaultColor != 0) {
				paint.setColor(defaultColor);
			}
		}

		private int calculateHash(Object... o) {
			return Arrays.hashCode(o);
		}

		public void drawPath(Canvas canvas, Path path) {
			if (isPaint_1) {
				canvas.drawPath(path, paint_1);
			}
			if (isShadowPaint) {
				canvas.drawPath(path, shadowPaint);
			}
			if (customColor != 0 || customWidth != 0) {
				if (customColor != 0) {
					customColorPaint.setColor(customColor);
				}
				if (customWidth != 0) {
					customColorPaint.setStrokeWidth(customWidth);
				}
				canvas.drawPath(path, customColorPaint);
			} else {
				canvas.drawPath(path, paint);
			}
			if (isPaint2) {
				canvas.drawPath(path, paint2);
			}
			if (isPaint3) {
				canvas.drawPath(path, paint3);
			}
		}
	}

}
