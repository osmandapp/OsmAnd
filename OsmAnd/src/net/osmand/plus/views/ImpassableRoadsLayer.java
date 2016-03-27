package net.osmand.plus.views;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.widget.ArrayAdapter;

import net.osmand.Location;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RoutingHelper;

import java.util.List;
import java.util.Map;

public class ImpassableRoadsLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider {

	private static final int startZoom = 10;
	private final MapActivity activity;
	private Bitmap roadWorkIcon;
	private OsmandMapTileView view;
	private Paint paint;
	private Map<Long, Location> missingRoadLocations;
	private List<RouteDataObject> missingRoads;
	private RoutingHelper routingHelper;

	public ImpassableRoadsLayer(MapActivity activity) {
		this.activity = activity;
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		roadWorkIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_pin_avoid_road);
		paint = new Paint();
		routingHelper = activity.getRoutingHelper();
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {

	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (tileBox.getZoom() >= startZoom) {
			for (long id : getMissingRoadLocations().keySet()) {
				Location location = getMissingRoadLocations().get(id);
				float x = tileBox.getPixXFromLatLon(location.getLatitude(), location.getLongitude());
				float y = tileBox.getPixYFromLatLon(location.getLatitude(), location.getLongitude());
				float left = x - roadWorkIcon.getWidth() / 2;
				float top = y - roadWorkIcon.getHeight();
				canvas.drawBitmap(roadWorkIcon, left, top, paint);
			}
		}
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
		int r = 0;
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
				int x = (int) tileBox.getPixXFromLatLon(location.getLatitude(), location.getLongitude());
				int y = (int) tileBox.getPixYFromLatLon(location.getLatitude(), location.getLongitude());
				if (calculateBelongs(ex, ey, x, y, compare)) {
					compare = radius;
					o.add(road);
				}
			}
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
	public String getObjectDescription(Object o) {
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

			ContextMenuAdapter.OnContextMenuClick listener = new ContextMenuAdapter.OnContextMenuClick() {
				@Override
				public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
					if (itemId == R.string.avoid_road) {
						activity.getMyApplication().getAvoidSpecificRoads().addImpassableRoad(
								activity, latLon, false);
					}
					activity.refreshMap();
					return true;
				}
			};

			adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.avoid_road, activity)
					.setColorIcon(R.drawable.ic_action_road_works_dark)
					.setListener(listener)
					.createItem());
		}
	}
}
