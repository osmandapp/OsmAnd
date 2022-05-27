package net.osmand.plus.views.layers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.MapMarker;
import net.osmand.core.jni.MapMarkerBuilder;
import net.osmand.core.jni.MapMarkersCollection;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QListMapMarker;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AvoidSpecificRoads;
import net.osmand.plus.helpers.AvoidSpecificRoads.AvoidRoadInfo;
import net.osmand.plus.helpers.AvoidSpecificRoads.AvoidSpecificRoadsCallback;
import net.osmand.plus.plugins.osmedit.data.OsmPoint;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer.ApplyMovedObjectCallback;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.util.MapUtils;

import java.util.List;
import java.util.Map;

public class ImpassableRoadsLayer extends OsmandMapLayer implements
		ContextMenuLayer.IContextMenuProvider, ContextMenuLayer.IMoveObjectProvider {

	private static final int START_ZOOM = 10;

	private AvoidSpecificRoads avoidSpecificRoads;
	private ContextMenuLayer contextMenuLayer;

	private Bitmap roadWorkIcon;
	private Paint activePaint;
	private Paint paint;

	//OpenGL
	private MapMarkersCollection mapMarkersCollection;
	private int impassibleRoadsCount = 0;
	private PointI movableObject;

	public ImpassableRoadsLayer(@NonNull Context ctx) {
		super(ctx);
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);

		avoidSpecificRoads = getApplication().getAvoidSpecificRoads();
		contextMenuLayer = view.getLayerByClass(ContextMenuLayer.class);
		roadWorkIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.ic_pin_avoid_road);
		activePaint = new Paint();
		ColorMatrix matrix = new ColorMatrix();
		matrix.setSaturation(0);
		paint = new Paint();
		paint.setColorFilter(new ColorMatrixColorFilter(matrix));
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (contextMenuLayer.getMoveableObject() instanceof AvoidRoadInfo) {
			PointF pf = contextMenuLayer.getMovableCenterPoint(tileBox);
			drawPoint(canvas, pf.x, pf.y, true);
			AvoidRoadInfo objectInMotion = (AvoidRoadInfo) contextMenuLayer.getMoveableObject();
			setMovableObject(objectInMotion);
		}
		if (movableObject != null && !contextMenuLayer.isInChangeMarkerPositionMode()) {
			cancelMovableObject();
		}
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (tileBox.getZoom() >= START_ZOOM) {
			MapRendererView mapRenderer = getMapRenderer();
			if (mapRenderer != null) {
				if (impassibleRoadsCount != avoidSpecificRoads.getImpassableRoads().size()) {
					clearMarkersCollections();
				}
				initMarkersCollection();
				impassibleRoadsCount = avoidSpecificRoads.getImpassableRoads().size();
				return;
			}
			for (Map.Entry<LatLon, AvoidRoadInfo> entry : avoidSpecificRoads.getImpassableRoads().entrySet()) {
				LatLon location = entry.getKey();
				AvoidRoadInfo road = entry.getValue();
				if (road != null && contextMenuLayer.getMoveableObject() instanceof AvoidRoadInfo) {
					AvoidRoadInfo object = (AvoidRoadInfo) contextMenuLayer.getMoveableObject();
					if (object.id == road.id) {
						continue;
					}
				}
				final double latitude = location.getLatitude();
				final double longitude = location.getLongitude();
				if (tileBox.containsLatLon(latitude, longitude)) {
					drawPoint(canvas, tileBox, latitude, longitude, road != null);
				}
			}
		} else {
			clearMarkersCollections();
		}
	}

	private void drawPoint(Canvas canvas, RotatedTileBox tileBox, double latitude, double longitude, boolean active) {
		float x = tileBox.getPixXFromLatLon(latitude, longitude);
		float y = tileBox.getPixYFromLatLon(latitude, longitude);
		drawPoint(canvas, x, y, active);
	}

	private void drawPoint(Canvas canvas, float x, float y, boolean active) {
		float textScale = getTextScale();
		y -= roadWorkIcon.getHeight() / 2f * textScale;
		Rect destRect = getIconDestinationRect(x, y, roadWorkIcon.getWidth(), roadWorkIcon.getHeight(), textScale);
		canvas.drawBitmap(roadWorkIcon, null, destRect, active ? activePaint : paint);
	}

	@Override
	public void destroyLayer() {
		clearMarkersCollections();
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	private int getRadiusPoi(RotatedTileBox tb) {
		int r;
		if (tb.getZoom() < START_ZOOM) {
			r = 0;
		} else {
			r = 15;
		}
		return (int) (r * tb.getDensity());
	}

	private boolean calculateBelongs(int ex, int ey, int objx, int objy, int radius) {
		return Math.abs(objx - ex) <= radius && (ey - objy) <= radius / 2 && (objy - ey) <= 3 * radius;
	}

	@Override
	public boolean disableSingleTap() {
		return false;
	}

	@Override
	public boolean disableLongPressOnMap(PointF point, RotatedTileBox tileBox) {
		return false;
	}

	@Override
	public boolean isObjectClickable(Object o) {
		return false;
	}

	@Override
	public boolean runExclusiveAction(Object o, boolean unknownLocation) {
		return false;
	}

	@Override
	public boolean showMenuAction(@Nullable Object o) {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o, boolean unknownLocation) {
		if (tileBox.getZoom() >= START_ZOOM) {
			int ex = (int) point.x;
			int ey = (int) point.y;
			int compare = getScaledTouchRadius(getApplication(), getRadiusPoi(tileBox));
			int radius = compare * 3 / 2;

			for (Map.Entry<LatLon, AvoidRoadInfo> entry : avoidSpecificRoads.getImpassableRoads().entrySet()) {
				LatLon location = entry.getKey();
				AvoidRoadInfo road = entry.getValue();
				if (location != null && road != null) {
					int x = (int) tileBox.getPixXFromLatLon(location.getLatitude(), location.getLongitude());
					int y = (int) tileBox.getPixYFromLatLon(location.getLatitude(), location.getLongitude());
					if (calculateBelongs(ex, ey, x, y, compare)) {
						compare = radius;
						o.add(road);
					}
				}
			}
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof AvoidRoadInfo) {
			AvoidRoadInfo avoidRoadInfo = (AvoidRoadInfo) o;
			return new LatLon(avoidRoadInfo.latitude, avoidRoadInfo.longitude);
		}
		return null;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof AvoidRoadInfo) {
			AvoidRoadInfo route = (AvoidRoadInfo) o;
			return new PointDescription(PointDescription.POINT_TYPE_BLOCKED_ROAD, route.name);
		}
		return null;
	}

	@Override
	public boolean isObjectMovable(Object o) {
		return o instanceof AvoidRoadInfo;
	}

	@Override
	public void applyNewObjectPosition(@NonNull Object o,
									   @NonNull LatLon position,
									   @Nullable final ApplyMovedObjectCallback callback) {
		MapActivity mapActivity = getMapActivity();
		if (o instanceof AvoidRoadInfo && mapActivity != null) {
			final AvoidRoadInfo object = (AvoidRoadInfo) o;
			final OsmandApplication application = getApplication();
			application.getAvoidSpecificRoads().replaceImpassableRoad(mapActivity, object, position, false, new AvoidSpecificRoadsCallback() {
				@Override
				public void onAddImpassableRoad(boolean success, AvoidRoadInfo newObject) {
					if (callback != null) {
						callback.onApplyMovedObject(success, newObject);
					}
				}

				@Override
				public boolean isCancelled() {
					return callback != null && callback.isCancelled();
				}
			});
			applyMovableObject(position);
		}
	}

	/**OpenGL*/
	private void initMarkersCollection() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null || mapMarkersCollection != null) {
			return;
		}
		mapMarkersCollection = new MapMarkersCollection();
		for (Map.Entry<LatLon, AvoidRoadInfo> entry : avoidSpecificRoads.getImpassableRoads().entrySet()) {
			LatLon location = entry.getKey();
			AvoidRoadInfo road = entry.getValue();
			if (road != null && contextMenuLayer.getMoveableObject() instanceof AvoidRoadInfo) {
				AvoidRoadInfo object = (AvoidRoadInfo) contextMenuLayer.getMoveableObject();
				if (object.id == road.id) {
					continue;
				}
			}
			Bitmap bitmap = getMergedBitmap(road != null);
			MapMarkerBuilder mapMarkerBuilder = new MapMarkerBuilder();
			int x = MapUtils.get31TileNumberX(location.getLongitude());
			int y = MapUtils.get31TileNumberY(location.getLatitude());
			PointI pointI = new PointI(x, y);
			mapMarkerBuilder
					.setPosition(pointI)
					.setIsHidden(false)
					.setBaseOrder(baseOrder)
					.setIsAccuracyCircleSupported(false)
					.setPinIcon(NativeUtilities.createSkImageFromBitmap(bitmap))
					.setPinIconHorisontalAlignment(MapMarker.PinIconHorisontalAlignment.CenterHorizontal)
					.setPinIconVerticalAlignment(MapMarker.PinIconVerticalAlignment.Top)
					.buildAndAddToCollection(mapMarkersCollection);
		}
		mapRenderer.addSymbolsProvider(mapMarkersCollection);
	}

	/**OpenGL*/
	private Bitmap getMergedBitmap(boolean active) {
		float textScale = getTextScale();
		Bitmap bitmapResult = Bitmap.createBitmap(roadWorkIcon.getWidth(), roadWorkIcon.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmapResult);
		int cx = roadWorkIcon.getWidth() / 2;
		int cy = roadWorkIcon.getHeight() / 2;
		Rect destRect = getIconDestinationRect(cx, cy, roadWorkIcon.getWidth(), roadWorkIcon.getHeight(), textScale);
		canvas.drawBitmap(roadWorkIcon, null, destRect, active ? activePaint : paint);
		return bitmapResult;
	}

	/**OpenGL*/
	private void clearMarkersCollections() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null) {
			return;
		}
		if (mapMarkersCollection != null) {
			mapRenderer.removeSymbolsProvider(mapMarkersCollection);
			mapMarkersCollection = null;
		}
	}

	/** OpenGL */
	private void setMovableObject(@NonNull AvoidRoadInfo objectInMotion) {
		MapRendererView mapRenderer = getMapView().getMapRenderer();
		if (mapRenderer == null || mapMarkersCollection == null || movableObject != null) {
			return;
		}
		int x = MapUtils.get31TileNumberX(objectInMotion.longitude);
		int y = MapUtils.get31TileNumberY(objectInMotion.latitude);
		QListMapMarker markers = mapMarkersCollection.getMarkers();
		for (int i = 0; i < markers.size(); i++) {
			MapMarker m = markers.get(i);
			if (m.getPosition().getX() == x && m.getPosition().getY() == y) {
				m.setIsHidden(true);
				movableObject = m.getPosition();
				break;
			}
		}
	}

	/** OpenGL */
	private void applyMovableObject(@NonNull LatLon newPosition) {
		MapRendererView mapRenderer = getMapView().getMapRenderer();
		if (mapRenderer == null || movableObject == null || mapMarkersCollection == null) {
			return;
		}
		int x = MapUtils.get31TileNumberX(newPosition.getLongitude());
		int y = MapUtils.get31TileNumberY(newPosition.getLatitude());
		QListMapMarker markers = mapMarkersCollection.getMarkers();
		for (int i = 0; i < markers.size(); i++) {
			MapMarker m = markers.get(i);
			if (m.getPosition().getX() == movableObject.getX() && m.getPosition().getY() == movableObject.getY()) {
				m.setPosition(new PointI(x, y));
				m.setIsHidden(false);
				movableObject = null;
				break;
			}
		}
	}

	/** OpenGL */
	private void cancelMovableObject() {
		MapRendererView mapRenderer = getMapView().getMapRenderer();
		if (mapRenderer == null || movableObject == null || mapMarkersCollection == null) {
			return;
		}
		QListMapMarker markers = mapMarkersCollection.getMarkers();
		for (int i = 0; i < markers.size(); i++) {
			MapMarker m = markers.get(i);
			if (m.getPosition().getX() == movableObject.getX() && m.getPosition().getY() == movableObject.getY()) {
				m.setIsHidden(false);
				movableObject = null;
				break;
			}
		}
	}
}
