package net.osmand.plus.views.layers;

import static net.osmand.data.PointDescription.POINT_TYPE_BLOCKED_ROAD;

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
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.avoidroads.AvoidRoadInfo;
import net.osmand.plus.avoidroads.AvoidRoadsHelper;
import net.osmand.plus.avoidroads.AvoidRoadsCallback;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer.ApplyMovedObjectCallback;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.List;

public class ImpassableRoadsLayer extends OsmandMapLayer implements
		ContextMenuLayer.IContextMenuProvider, ContextMenuLayer.IMoveObjectProvider {

	private static final int START_ZOOM = 10;

	private AvoidRoadsHelper avoidRoadsHelper;
	private ContextMenuLayer contextMenuLayer;

	private Bitmap roadWorkIcon;
	private Paint activePaint;
	private Paint paint;

	//OpenGL
	private int impassibleRoadsCount;

	public ImpassableRoadsLayer(@NonNull Context ctx) {
		super(ctx);
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);

		avoidRoadsHelper = getApplication().getAvoidSpecificRoads();
		contextMenuLayer = view.getLayerByClass(ContextMenuLayer.class);
		createResources();
		activePaint = new Paint();
		ColorMatrix matrix = new ColorMatrix();
		matrix.setSaturation(0);
		paint = new Paint();
		paint.setColorFilter(new ColorMatrixColorFilter(matrix));
	}

	@Override
	protected void updateResources() {
		super.updateResources();
		createResources();
	}

	private void createResources(){
		roadWorkIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.ic_pin_avoid_road);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (contextMenuLayer.getMoveableObject() instanceof AvoidRoadInfo) {
			PointF pf = contextMenuLayer.getMovableCenterPoint(tileBox);
			drawPoint(canvas, pf.x, pf.y, true);
			AvoidRoadInfo movableRoad = (AvoidRoadInfo) contextMenuLayer.getMoveableObject();
			setMovableObject(movableRoad.getLatitude(), movableRoad.getLongitude());
		}
		if (movableObject != null && !contextMenuLayer.isInChangeMarkerPositionMode()) {
			cancelMovableObject();
		}
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		super.onPrepareBufferImage(canvas, tileBox, settings);
		if (tileBox.getZoom() >= START_ZOOM) {
			MapRendererView mapRenderer = getMapRenderer();
			if (mapRenderer != null) {
				if (impassibleRoadsCount != avoidRoadsHelper.getImpassableRoads().size() || mapActivityInvalidated) {
					clearMapMarkersCollections();
				}
				initMarkersCollection();
				impassibleRoadsCount = avoidRoadsHelper.getImpassableRoads().size();
				mapActivityInvalidated = false;
				return;
			}
			for (AvoidRoadInfo road : avoidRoadsHelper.getImpassableRoads()) {
				if (contextMenuLayer.getMoveableObject() instanceof AvoidRoadInfo) {
					AvoidRoadInfo object = (AvoidRoadInfo) contextMenuLayer.getMoveableObject();
					if (object.getId() == road.getId()) {
						continue;
					}
				}
				if (tileBox.containsLatLon(road.getLatLon())) {
					drawPoint(canvas, tileBox, road.getLatitude(), road.getLongitude(), true);
				}
			}
		} else {
			clearMapMarkersCollections();
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

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o,
	                                    boolean unknownLocation, boolean excludeUntouchableObjects) {
		List<AvoidRoadInfo> impassableRoads = avoidRoadsHelper.getImpassableRoads();
		if (tileBox.getZoom() >= START_ZOOM && !excludeUntouchableObjects && !Algorithms.isEmpty(impassableRoads)) {
			MapRendererView mapRenderer = getMapRenderer();
			float radius = getScaledTouchRadius(getApplication(), getRadiusPoi(tileBox)) * TOUCH_RADIUS_MULTIPLIER;
			QuadRect screenArea = new QuadRect(
					point.x - radius,
					point.y - radius / 2f,
					point.x + radius,
					point.y + radius * 3f
			);
			List<PointI> touchPolygon31 = null;
			if (mapRenderer != null) {
				touchPolygon31 = NativeUtilities.getPolygon31FromScreenArea(mapRenderer, screenArea);
				if (touchPolygon31 == null) {
					return;
				}
			}

			for (AvoidRoadInfo road : impassableRoads) {
				LatLon latLon = road.getLatLon();
				boolean add = mapRenderer != null
						? NativeUtilities.isPointInsidePolygon(latLon, touchPolygon31)
						: tileBox.isLatLonInsidePixelArea(latLon, screenArea);
				if (add) {
					o.add(road);
				}
			}
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof AvoidRoadInfo) {
			return ((AvoidRoadInfo) o).getLatLon();
		}
		return null;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof AvoidRoadInfo) {
			AvoidRoadInfo route = (AvoidRoadInfo) o;
			return new PointDescription(POINT_TYPE_BLOCKED_ROAD, route.getName(getContext()));
		}
		return null;
	}

	@Override
	public boolean isObjectMovable(Object o) {
		return o instanceof AvoidRoadInfo;
	}

	@Override
	public void applyNewObjectPosition(@NonNull Object o, @NonNull LatLon latLon,
	                                   @Nullable ApplyMovedObjectCallback callback) {
		MapActivity mapActivity = getMapActivity();
		if (o instanceof AvoidRoadInfo && mapActivity != null) {
			AvoidRoadInfo object = (AvoidRoadInfo) o;
			OsmandApplication application = getApplication();
			application.getAvoidSpecificRoads().replaceImpassableRoad(mapActivity, object, latLon, false, new AvoidRoadsCallback() {
				@Override
				public void onAddImpassableRoad(boolean success, @Nullable AvoidRoadInfo roadInfo) {
					if (callback != null) {
						callback.onApplyMovedObject(success, roadInfo);
					}
				}

				@Override
				public boolean isCancelled() {
					return callback != null && callback.isCancelled();
				}
			});
			applyMovableObject(latLon);
		}
	}

	/**OpenGL*/
	private void initMarkersCollection() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null || mapMarkersCollection != null) {
			return;
		}
		mapMarkersCollection = new MapMarkersCollection();
		for (AvoidRoadInfo road : avoidRoadsHelper.getImpassableRoads()) {
			boolean isMoveable = false;
			if (contextMenuLayer.getMoveableObject() instanceof AvoidRoadInfo) {
				AvoidRoadInfo object = (AvoidRoadInfo) contextMenuLayer.getMoveableObject();
				if (object.getId() == road.getId()) {
					isMoveable = true;
				}
			}
			Bitmap bitmap = getMergedBitmap(true);
			LatLon latLon = road.getLatLon();
			int x = MapUtils.get31TileNumberX(latLon.getLongitude());
			int y = MapUtils.get31TileNumberY(latLon.getLatitude());

			MapMarkerBuilder mapMarkerBuilder = new MapMarkerBuilder();
			mapMarkerBuilder
					.setPosition(new PointI(x, y))
					.setIsHidden(isMoveable)
					.setBaseOrder(getPointsOrder())
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
}
