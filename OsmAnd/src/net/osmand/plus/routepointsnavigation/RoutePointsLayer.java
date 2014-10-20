package net.osmand.plus.routepointsnavigation;

import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.os.AsyncTask;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;

import java.util.List;

/**
 * Created by Barsik on 20.06.2014.
 */
public class RoutePointsLayer  extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider {

	private final RoutePointsPlugin plugin;
	private final MapActivity map;

	public RoutePointsLayer(MapActivity map, RoutePointsPlugin plugin){
		this.map = map;
		this.plugin = plugin;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o) {

	}

	@Override
	public LatLon getObjectLocation(Object o) {
		return null;
	}

	@Override
	public String getObjectDescription(Object o) {
		return null;
	}

	@Override
	public String getObjectName(Object o) {
		return null;
	}

	@Override
	public void initLayer(OsmandMapTileView view) {

	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {

	}

	@Override
	public void destroyLayer() {

	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public void populateObjectContextMenu(Object o, ContextMenuAdapter adapter) {
		if (o instanceof GPXUtilities.WptPt && plugin.getCurrentRoute() != null){
			final GPXUtilities.WptPt point = (GPXUtilities.WptPt) o;
			ContextMenuAdapter.OnContextMenuClick listener = new ContextMenuAdapter.OnContextMenuClick() {
				@Override
				public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
					if (itemId == R.string.mark_as_not_visited){
						plugin.getCurrentRoute().markPoint(point,false);
						plugin.saveCurrentRoute();
					} else if (itemId == R.string.mark_as_visited) {
						plugin.getCurrentRoute().markPoint(point, true);
						plugin.saveCurrentRoute();
					} else if (itemId == R.string.mark_as_current){
						plugin.getCurrentRoute().markPoint(point, false);
						plugin.getCurrentRoute().navigateToPoint(point);
						plugin.saveCurrentRoute();
					} else if (itemId == R.string.navigate_to_next){
						plugin.getCurrentRoute().navigateToNextPoint();
						plugin.saveCurrentRoute();
					}
					map.refreshMap();
				}
			};

			if (plugin.getCurrentRoute().getPointStatus(point)){
				adapter.item(R.string.mark_as_not_visited).icons(
						R.drawable.ic_action_gremove_dark, R.drawable.ic_action_gremove_light).listen(listener).reg();
			} else {
				adapter.item(R.string.mark_as_visited).icons(
						R.drawable.ic_action_ok_dark, R.drawable.ic_action_ok_light).listen(listener).reg();
			}

			RoutePointsPlugin.RoutePoint routePoint = plugin.getCurrentRoute().getRoutePointFromWpt(point);
			if (routePoint != null) {
				if (routePoint.isNextNavigate) {
					adapter.item(R.string.navigate_to_next)
							.icons(R.drawable.ic_action_gnext_dark, R.drawable.ic_action_gnext_light).listen(listener)
							.reg();
				} else {
					adapter.item(R.string.mark_as_current)
							.icons(R.drawable.ic_action_signpost_dark, R.drawable.ic_action_signpost_light)
							.listen(listener).reg();
				}
			}
		}
	}


}
