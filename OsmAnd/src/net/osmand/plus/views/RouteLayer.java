package net.osmand.plus.views;

import java.util.ArrayList;
import java.util.List;

import net.osmand.plus.R;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
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

	// cache
	private RenderingRulesStorage cachedRrs;
	private boolean cachedNightMode;
	private int cachedColor;

	public RouteLayer(RoutingHelper helper){
		this.helper = helper;
	}
	

	private void initUI() {
		boundsRect = new Rect(0, 0, view.getWidth(), view.getHeight());
		latlonRect = new RectF();
		paint = new Paint();
		
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

	
	private int getColor(DrawSettings nightMode){
		RenderingRulesStorage rrs = view.getApplication().getRendererRegistry().getCurrentSelectedRenderer();
		boolean n = nightMode != null && nightMode.isNightMode();
		if (rrs != cachedRrs || cachedNightMode != n) {
			cachedRrs = rrs;
			cachedNightMode = n;
			cachedColor = view.getResources().getColor(cachedNightMode?R.color.nav_track_fluorescent :  R.color.nav_track);
			if (cachedRrs != null) {
				RenderingRuleSearchRequest req = new RenderingRuleSearchRequest(rrs);
				req.setBooleanFilter(rrs.PROPS.R_NIGHT_MODE, cachedNightMode);
				if (req.searchRenderingAttribute("routeColor")) {
					cachedColor = req.getIntPropertyValue(rrs.PROPS.R_ATTR_COLOR_VALUE);
				}
			}
		}
		return cachedColor;
	}
	
	@Override
	public void onDraw(Canvas canvas, RectF latLonBounds, RectF tilesRect, DrawSettings nightMode) {
		path.reset();
		if (helper.getFinalLocation() != null && helper.getRoute().isCalculated()) {
			paint.setColor(getColor(nightMode));
			int w = view.getWidth();
			int h = view.getHeight();
			Location lastProjection = helper.getLastProjection();
			if(lastProjection != null &&
					view.isPointOnTheRotatedMap(lastProjection.getLatitude(), lastProjection.getLongitude())){
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
		Location lastProjection = helper.getLastProjection();
		if (lastProjection != null) {
			if (leftLongitude <= lastProjection.getLongitude() && lastProjection.getLongitude() <= rightLongitude
					&& bottomLatitude <= lastProjection.getLatitude() && lastProjection.getLatitude() <= topLatitude) {
				points.add(lastProjection);
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
					} else if (lastProjection != null) {
						points.add(0, lastProjection);
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
