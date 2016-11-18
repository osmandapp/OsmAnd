package net.osmand.plus.views;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ArrayAdapter;

import net.osmand.Location;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AvoidSpecificRoads.AvoidSpecificRoadsCallback;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.ContextMenuLayer.ApplyMovedObjectCallback;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ImpassableRoadsLayer extends OsmandMapLayer implements
		ContextMenuLayer.IContextMenuProvider, ContextMenuLayer.IMoveObjectProvider {

	private static final int startZoom = 10;
	private final MapActivity activity;
	private Bitmap roadWorkIcon;
	private OsmandMapTileView view;
	private Paint paint;
	private Map<Long, Location> missingRoadLocations;
	private List<RouteDataObject> missingRoads;
	private RoutingHelper routingHelper;

	private ContextMenuLayer contextMenuLayer;

	private Set<PreservedRoadDataObject> mPreservedRoadDataObjects;

	public ImpassableRoadsLayer(MapActivity activity) {
		this.activity = activity;
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		roadWorkIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_pin_avoid_road);
		paint = new Paint();
		routingHelper = activity.getRoutingHelper();

		contextMenuLayer = view.getLayerByClass(ContextMenuLayer.class);

		List<LatLon> impassibleRoads = activity.getMyApplication().getSettings().getImpassableRoadPoints();
		mPreservedRoadDataObjects = new HashSet<>(impassibleRoads.size());
		for (LatLon impassibleRoad : impassibleRoads) {
			mPreservedRoadDataObjects.add(new PreservedRoadDataObject(impassibleRoad));
		}
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (contextMenuLayer.getMoveableObject() instanceof RouteDataObject) {
			PointF pf = contextMenuLayer.getMovableCenterPoint(tileBox);
			drawPoint(canvas, pf.x, pf.y);
		}
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (tileBox.getZoom() >= startZoom) {
			for (long id : getMissingRoadLocations().keySet()) {
				if(contextMenuLayer.getMoveableObject() instanceof RouteDataObject) {
					RouteDataObject object = (RouteDataObject) contextMenuLayer.getMoveableObject();
					if (object.id == id) {
						continue;
					}
				}
				Location location = getMissingRoadLocations().get(id);
				final double latitude = location.getLatitude();
				final double longitude = location.getLongitude();
				drawPoint(canvas, tileBox, latitude, longitude);
			}
			for (PreservedRoadDataObject preservedRoadDataObject : mPreservedRoadDataObjects) {
				final LatLon latLon = preservedRoadDataObject.getLatLon();
				drawPoint(canvas, tileBox, latLon.getLatitude(), latLon.getLongitude());
			}
		}
	}

	private void drawPoint(Canvas canvas, RotatedTileBox tileBox, double latitude, double longitude) {
		float x = tileBox.getPixXFromLatLon(latitude, longitude);
		float y = tileBox.getPixYFromLatLon(latitude, longitude);
		drawPoint(canvas, x, y);
	}

	private void drawPoint(Canvas canvas, float x, float y) {
		float left = x - roadWorkIcon.getWidth() / 2;
		float top = y - roadWorkIcon.getHeight();
		canvas.drawBitmap(roadWorkIcon, left, top, paint);
	}

	public Map<Long, Location> getMissingRoadLocations() {
		if (missingRoadLocations == null) {
			missingRoadLocations = activity.getMyApplication().getDefaultRoutingConfig().getImpassableRoadLocations();
		}
		return missingRoadLocations;
	}

	public List<RouteDataObject> getMissingRoads() {
		if (missingRoads == null) {
			missingRoads = activity.getMyApplication().getDefaultRoutingConfig().getImpassableRoads();
		}
		return missingRoads;
	}

	@Override
	public void destroyLayer() {

	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	public int getRadiusPoi(RotatedTileBox tb) {
		int r;
		if (tb.getZoom() < startZoom) {
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
	public boolean disableLongPressOnMap() {
		return false;
	}

	@Override
	public boolean isObjectClickable(Object o) {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o) {
		if (tileBox.getZoom() >= startZoom) {
			int ex = (int) point.x;
			int ey = (int) point.y;
			int compare = getRadiusPoi(tileBox);
			int radius = compare * 3 / 2;

			for (RouteDataObject road : getMissingRoads()) {
				Location location = getMissingRoadLocations().get(road.getId());
				if (location != null) {
					int x = (int) tileBox.getPixXFromLatLon(location.getLatitude(), location.getLongitude());
					int y = (int) tileBox.getPixYFromLatLon(location.getLatitude(), location.getLongitude());
					if (calculateBelongs(ex, ey, x, y, compare)) {
						compare = radius;
						o.add(road);
					}
				}
			}
		}
		if (!mPreservedRoadDataObjects.isEmpty()) {
			activity.getMyApplication().getAvoidSpecificRoads().initPreservedData();
			mPreservedRoadDataObjects.clear();
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof RouteDataObject) {
			RouteDataObject route = (RouteDataObject) o;
			Location location = missingRoadLocations.get(route.getId());
			return new LatLon(location.getLatitude(), location.getLongitude());
		}
		return null;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof RouteDataObject) {
			RouteDataObject route = (RouteDataObject) o;
			return new PointDescription(PointDescription.POINT_TYPE_BLOCKED_ROAD, route.getName());
		}
		return null;
	}

	@Override
	public void populateObjectContextMenu(final LatLon latLon, final Object o, ContextMenuAdapter adapter, MapActivity mapActivity) {
		if (latLon != null && o == null
				&& (routingHelper.isRoutePlanningMode() || routingHelper.isFollowingMode())) {

			ContextMenuAdapter.ItemClickListener listener = new ContextMenuAdapter.ItemClickListener() {
				@Override
				public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked) {
					if (itemId == R.string.avoid_road) {
						activity.getMyApplication().getAvoidSpecificRoads().addImpassableRoad(
								activity, latLon, false, null, false);
					}
					return true;
				}
			};

			adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.avoid_road, activity)
					.setIcon(R.drawable.ic_action_road_works_dark)
					.setListener(listener)
					.createItem());
		}
	}

	@Override
	public boolean isObjectMovable(Object o) {
		return o instanceof RouteDataObject;
	}

	@Override
	public void applyNewObjectPosition(@NonNull Object o,
									   @NonNull LatLon position,
									   @Nullable final ApplyMovedObjectCallback callback) {
		if (o instanceof RouteDataObject) {
			final RouteDataObject object = (RouteDataObject) o;
			final OsmandApplication application = activity.getMyApplication();
			application.getAvoidSpecificRoads().replaceImpassableRoad(activity, object, position, false, new AvoidSpecificRoadsCallback() {
				@Override
				public void onAddImpassableRoad(boolean success, RouteDataObject newObject) {
					if (callback != null) {
						callback.onApplyMovedObject(success, newObject);
					}
				}

				@Override
				public boolean isCancelled() {
					return callback != null && callback.isCancelled();
				}
			});
		}
	}

	private static class PreservedRoadDataObject {
		private final LatLon mLatLon;

		private PreservedRoadDataObject(LatLon latLon) {
			this.mLatLon = latLon;
		}

		public LatLon getLatLon() {
			return mLatLon;
		}
	}
}
