package net.osmand.plus.views;

import java.util.ArrayList;
import java.util.List;

import net.osmand.osm.MapUtils;
import net.osmand.plus.R;
import net.osmand.plus.routing.RoutingHelper;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.location.Location;

public class RouteLayer extends OsmandMapLayer {
	
	private OsmandMapTileView view;
	
	private final RoutingHelper helper;
	private Rect boundsRect;
	private RectF latlonRect;
	private List<Location> points = new ArrayList<Location>();
	private Paint paint;

	private Path path;

	private Boolean fluorescent;
	
	public RouteLayer(RoutingHelper helper){
		this.helper = helper;
	}
	

	private void initUI() {
		boundsRect = new Rect(0, 0, view.getWidth(), view.getHeight());
		latlonRect = new RectF();
		paint = new Paint();
		fluorescent = view.getSettings().FLUORESCENT_OVERLAYS.get();
		if (view.getSettings().FLUORESCENT_OVERLAYS.get()) {
			paint.setColor(view.getResources().getColor(R.color.nav_track_fluorescent));
		} else {
			paint.setColor(view.getResources().getColor(R.color.nav_track));
		}
		paint.setStyle(Style.STROKE);
		paint.setStrokeWidth(14);
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
		path.reset();
		if (helper.getFinalLocation() != null && helper.getRoute().isCalculated()) {
			if(view.getSettings().FLUORESCENT_OVERLAYS.get() != fluorescent) {
				initUI(); //to change color immediately when needed
			}
			int w = view.getWidth();
			int h = view.getHeight();
			if(helper.getCurrentLocation() != null &&
					view.isPointOnTheRotatedMap(helper.getCurrentLocation().getLatitude(), helper.getCurrentLocation().getLongitude())){
				boundsRect = new Rect(-w / 2, -h, 3 * w / 2, h);
			} else {
				boundsRect = new Rect(0, 0, w, h);
			}
			view.calculateLatLonRectangle(boundsRect, latlonRect);
			double topLatitude = latlonRect.top;
			double leftLongitude = latlonRect.left;
			double bottomLatitude = latlonRect.bottom;
			double rightLongitude = latlonRect.right;
			double lat = topLatitude - bottomLatitude + 0.1;
			double lon = rightLongitude - leftLongitude + 0.1;
			fillLocationsToShow(topLatitude + lat, leftLongitude - lon, bottomLatitude - lat, rightLongitude + lon);
			
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
	
	public synchronized void fillLocationsToShow(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude) {
		points.clear();
		boolean previousVisible = false;
		Location lastFixedLocation = helper.getLastFixedLocation();
		if (lastFixedLocation != null) {
			if (leftLongitude <= lastFixedLocation.getLongitude() && lastFixedLocation.getLongitude() <= rightLongitude
					&& bottomLatitude <= lastFixedLocation.getLatitude() && lastFixedLocation.getLatitude() <= topLatitude) {
				points.add(lastFixedLocation);
				previousVisible = true;
			}
		}
		List<Location> routeNodes = helper.getRoute().getNextLocations();
		for (int i = 0; i < routeNodes.size(); i++) {
			Location ls = routeNodes.get(i);
			if (leftLongitude <= ls.getLongitude() && ls.getLongitude() <= rightLongitude && bottomLatitude <= ls.getLatitude()
					&& ls.getLatitude() <= topLatitude) {
				points.add(ls);
				if (!previousVisible) {
					if (i > 0) {
						points.add(0, routeNodes.get(i - 1));
					} else if (lastFixedLocation != null) {
						points.add(0, lastFixedLocation);
					}
				}
				previousVisible = true;
			} else if (previousVisible) {
				points.add(ls);
				previousVisible = false;
				// do not continue make method more efficient (because it calls in UI thread)
				// this break also has logical sense !
				break;
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
