package net.osmand.plus.views;

import java.util.ArrayList;
import java.util.List;

import net.osmand.Location;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
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

public class RouteLayer extends OsmandMapLayer {
	
	private OsmandMapTileView view;
	
	private final RoutingHelper helper;
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
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		path.reset();
		if (helper.getFinalLocation() != null && helper.getRoute().isCalculated()) {
			paint.setColor(getColor(settings));
			int w = tileBox.getPixWidth();
			int h = tileBox.getPixHeight();
			Location lastProjection = helper.getLastProjection();
			final RotatedTileBox cp ;
			if(lastProjection != null &&
					tileBox.containsLatLon(lastProjection.getLatitude(), lastProjection.getLongitude())){
				cp = tileBox.copy();
				cp.increasePixelDimensions(w /2, h);
			} else {
				cp = tileBox;
			}

			final QuadRect latlonRect = cp.getLatLonBounds();
			double topLatitude = latlonRect.top;
			double leftLongitude = latlonRect.left;
			double bottomLatitude = latlonRect.bottom;
			double rightLongitude = latlonRect.right;
			double lat = topLatitude - bottomLatitude + 0.1;
			double lon = rightLongitude - leftLongitude + 0.1;
			drawLocations(tileBox, canvas, topLatitude + lat, leftLongitude - lon, bottomLatitude - lat, rightLongitude + lon);
		}
	
	}
	
	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {}


	private void drawSegment(RotatedTileBox tb, Canvas canvas) {
		if (points.size() > 0) {
			int px = tb.getPixXFromLonNoRot(points.get(0).getLongitude());
			int py = tb.getPixYFromLatNoRot(points.get(0).getLatitude());
			path.moveTo(px, py);
			for (int i = 1; i < points.size(); i++) {
				Location o = points.get(i);
				int x = tb.getPixXFromLonNoRot(o.getLongitude());
				int y = tb.getPixYFromLatNoRot(o.getLatitude());
				path.lineTo(x, y);
			}
			canvas.drawPath(path, paint);
		}
	}
	
	public void drawLocations(RotatedTileBox tb, Canvas canvas, double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude) {
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
				
				drawSegment(tb, canvas);
				previousVisible = false;
				points.clear();
			}
		}
		drawSegment(tb, canvas);
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
	public boolean onLongPressEvent(PointF point, RotatedTileBox tileBox) {
		return false;
	}

	@Override
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		return false;
	}




}
