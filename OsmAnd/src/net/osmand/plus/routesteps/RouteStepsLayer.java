package net.osmand.plus.routesteps;

import android.graphics.Canvas;
import android.graphics.PointF;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;

import java.util.List;

/**
 * Created by Barsik on 10.06.2014.
 */
public class RouteStepsLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider {

	private final MapActivity map;
	private RouteStepsPlugin plugin;

	public RouteStepsLayer(MapActivity map, RouteStepsPlugin plugin){
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
}
