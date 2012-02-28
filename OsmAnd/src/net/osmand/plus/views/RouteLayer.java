package net.osmand.plus.views;

import java.util.ArrayList;
import java.util.List;

import net.osmand.LogUtil;
import net.osmand.osm.MapUtils;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.R;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.location.Location;
import android.util.Log;

public class RouteLayer extends OsmandMapLayer {
	
	private OsmandMapTileView view;
	
	private final RoutingHelper helper;
	private Rect boundsRect;
	private RectF tileRect;
	private List<Location> points = new ArrayList<Location>();
	private Paint paint;

	private Path path;
	
	public RouteLayer(RoutingHelper helper){
		this.helper = helper;
	}
	

	private void initUI() {
		boundsRect = new Rect(0, 0, view.getWidth(), view.getHeight());
		tileRect = new RectF();
		paint = new Paint();
		if (view.getSettings().FLUORESCENT_OVERLAYS.get()) {
			paint.setColor(view.getResources().getColor(R.color.nav_track_fluorescent));
		} else {
			paint.setColor(view.getResources().getColor(R.color.nav_track));
		}
		paint.setStyle(Style.STROKE);
		paint.setStrokeWidth(14);
		//paint.setAlpha(150);
		paint.setAntiAlias(true);
		paint.setStrokeCap(Cap.ROUND);
		paint.setStrokeJoin(Join.ROUND);
		path = new Path();
	}
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		initUI();
	}

	
	
	@Override
	public void onDraw(Canvas canvas, RectF latLonBounds, RectF tilesRect, DrawSettings nightMode) {
		initUI(); //to change color immediately when needed
		path.reset();
		if (helper.hasPointsToShow()) {
			long time = System.currentTimeMillis();
			int w = view.getWidth();
			int h = view.getHeight();
			if(helper.getCurrentLocation() != null &&
					view.isPointOnTheRotatedMap(helper.getCurrentLocation().getLatitude(), helper.getCurrentLocation().getLongitude())){
				boundsRect = new Rect(-w / 2, -h, 3 * w / 2, h);
			} else {
				boundsRect = new Rect(0, 0, w, h);
			}
			view.calculateTileRectangle(boundsRect, view.getCenterPointX(), view.getCenterPointY(), view.getXTile(), view.getYTile(),
					tileRect);
			double topLatitude = MapUtils.getLatitudeFromTile(view.getZoom(), tileRect.top);
			double leftLongitude = MapUtils.getLongitudeFromTile(view.getZoom(), tileRect.left);
			double bottomLatitude = MapUtils.getLatitudeFromTile(view.getZoom(), tileRect.bottom);
			double rightLongitude = MapUtils.getLongitudeFromTile(view.getZoom(), tileRect.right);
			double lat = topLatitude - bottomLatitude + 0.1;
			double lon = rightLongitude - leftLongitude + 0.1;
			helper.fillLocationsToShow(topLatitude + lat, leftLongitude - lon, bottomLatitude - lat, rightLongitude + lon, points);
			if((System.currentTimeMillis() - time) > 80){
				Log.e(LogUtil.TAG, "Calculate route layer " + (System.currentTimeMillis() - time)); //$NON-NLS-1$
			}
			
			if (points.size() > 0) {
				int px = view.getMapXForPoint(points.get(0).getLongitude());
				int py = view.getMapYForPoint(points.get(0).getLatitude());
				path.moveTo(px, py);
				for (int i = 1; i < points.size(); i++) {
					Location o = points.get(i);
					int x = view.getMapXForPoint(o.getLongitude());
					int y = view.getMapYForPoint(o.getLatitude());
//					if (i == 1) {
//						pathBearing = (float) (Math.atan2(y - py, x - px) / Math.PI * 180);
//					}
					path.lineTo(x, y);
				}
				canvas.drawPath(path, paint);
			}
		}
	}
	
	public RoutingHelper getHelper() {
		return helper;
	}

	
	// to show further direction
	public Path getPath() {
		return path;
	}

	
	@Override
	public void destroyLayer() {
		
	}
	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public boolean onLongPressEvent(PointF point) {
		return false;
	}

	@Override
	public boolean onSingleTap(PointF point) {
		return false;
	}




}
