package net.osmand.plus.osmedit;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Denis on
 * 20.03.2015.
 */
public class OsmEditsLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider {

	private static final int startZoom = 10;
	private final OsmEditingPlugin plugin;
	private final MapActivity activity;
	private Bitmap poi;
	private Bitmap bug;
	private OsmandMapTileView view;
	private Paint paintIcon;



	public OsmEditsLayer(MapActivity activity, OsmEditingPlugin plugin) {
		this.activity = activity;
		this.plugin = plugin;
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;

		poi = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_pin_poi);
		bug = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_pin_poi);
		paintIcon = new Paint();
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {

	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (tileBox.getZoom() >= startZoom) {
			List<LatLon> fullObjectsLatLon = new ArrayList<>();
			drawPoints(canvas, tileBox, plugin.getDBBug().getOsmbugsPoints(), fullObjectsLatLon);
			drawPoints(canvas, tileBox, plugin.getDBPOI().getOpenstreetmapPoints(), fullObjectsLatLon);
			this.fullObjectsLatLon = fullObjectsLatLon;
		}
	}

	private void drawPoints(Canvas canvas, RotatedTileBox tileBox, List<? extends OsmPoint> objects,
							List<LatLon> fullObjectsLatLon) {
		for (OsmPoint o : objects) {
			float x = tileBox.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
			float y = tileBox.getPixYFromLatLon(o.getLatitude(), o.getLongitude());
			Bitmap b;
			if (o.getGroup() == OsmPoint.Group.POI) {
				b = poi;
			} else if (o.getGroup() == OsmPoint.Group.BUG) {
				b = bug;
			} else {
				b = poi;
			}
			canvas.drawBitmap(b, x - b.getWidth() / 2, y - b.getHeight() / 2, paintIcon);
			fullObjectsLatLon.add(new LatLon(o.getLatitude(), o.getLongitude()));
		}
	}

	@Override
	public void destroyLayer() {

	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}


	public void getOsmEditsFromPoint(PointF point, RotatedTileBox tileBox, List<? super OsmPoint> am) {
		int ex = (int) point.x;
		int ey = (int) point.y;
		int compare = getRadiusPoi(tileBox);
		int radius = compare * 3 / 2;
		compare = getFromPoint(tileBox, am, ex, ey, compare, radius, plugin.getDBBug().getOsmbugsPoints());
		compare = getFromPoint(tileBox, am, ex, ey, compare, radius, plugin.getDBPOI().getOpenstreetmapPoints());
	}

	private int getFromPoint(RotatedTileBox tileBox, List<? super OsmPoint> am, int ex, int ey, int compare,
			int radius, List<? extends OsmPoint> pnts) {
		for (OsmPoint n : pnts) {
			int x = (int) tileBox.getPixXFromLatLon(n.getLatitude(), n.getLongitude());
			int y = (int) tileBox.getPixYFromLatLon(n.getLatitude(), n.getLongitude());
			if (calculateBelongs(ex, ey, x, y, compare)) {
				compare = radius;
				am.add(n);
			}
		}
		return compare;
	}

	private boolean calculateBelongs(int ex, int ey, int objx, int objy, int radius) {
		return Math.abs(objx - ex) <= radius && (ey - objy) <= radius / 2 && (objy - ey) <= 3 * radius ;
	}

	public int getRadiusPoi(RotatedTileBox tb){
		int r = 0;
		if(tb.getZoom()  < startZoom){
			r = 0;
		} else {
			r = 15;
		}
		return (int) (r * tb.getDensity());
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
		return o instanceof OsmPoint;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o) {
		if (tileBox.getZoom() >= startZoom) {
			getOsmEditsFromPoint(point, tileBox, o);
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof OsmPoint) {
			return new LatLon(((OsmPoint)o).getLatitude(),((OsmPoint)o).getLongitude());
		}
		return null;
	}

	@Override
	public String getObjectDescription(Object o) {
		if(o instanceof OsmPoint) {
			OsmPoint point =  (OsmPoint) o;
			return OsmEditingPlugin.getEditName(point);
		}
		return null;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if(o instanceof OsmPoint) {
			OsmPoint point =  (OsmPoint) o;
			String name = "";
			String type = "";
			if (point.getGroup() == OsmPoint.Group.POI){
				name = ((OpenstreetmapPoint) point).getName();
				type = PointDescription.POINT_TYPE_OSM_NOTE;
			} else if (point.getGroup() == OsmPoint.Group.BUG) {
				name = ((OsmNotesPoint) point).getText();
				type = PointDescription.POINT_TYPE_OSM_BUG;
			}
			return new PointDescription(type, name);
		}
		return null;
	}
}
