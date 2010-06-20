package com.osmand.views;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.location.Location;
import android.util.Log;

import com.osmand.LogUtil;
import com.osmand.activities.RoutingHelper;
import com.osmand.osm.MapUtils;

public class RouteLayer implements OsmandMapLayer {
	
	private OsmandMapTileView view;
	
	private final RoutingHelper helper;
	private Rect boundsRect;
	private RectF tileRect;
	private List<Location> points = new ArrayList<Location>();
	private Paint paint;

	private Path path;
	private float pathBearing;
	
	public RouteLayer(RoutingHelper helper){
		this.helper = helper;
	}
	

	private void initUI() {
		boundsRect = new Rect(0, 0, view.getWidth(), view.getHeight());
		tileRect = new RectF();
		paint = new Paint();
		paint.setColor(Color.BLUE);
		paint.setStyle(Style.STROKE);
		paint.setStrokeWidth(14);
		paint.setAlpha(150);
		paint.setAntiAlias(true);
		path = new Path();
	}
	
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		initUI();
	}

	
	
	@Override
	public void onDraw(Canvas canvas) {
		path.reset();
		if (helper.hasPointsToShow()) {
			long time = System.currentTimeMillis();
			int w = view.getWidth();
			int h = view.getHeight();
			boundsRect = new Rect(-w / 2, -h, 3 * w / 2, h);
//			boundsRect = new Rect(0, 0, w, h);
			view.calculateTileRectangle(boundsRect, view.getCenterPointX(), view.getCenterPointY(), view.getXTile(), view.getYTile(),
					tileRect);
			double topLatitude = MapUtils.getLatitudeFromTile(view.getZoom(), tileRect.top);
			double leftLongitude = MapUtils.getLongitudeFromTile(view.getZoom(), tileRect.left);
			double bottomLatitude = MapUtils.getLatitudeFromTile(view.getZoom(), tileRect.bottom);
			double rightLongitude = MapUtils.getLongitudeFromTile(view.getZoom(), tileRect.right);
			helper.fillLocationsToShow(topLatitude, leftLongitude, bottomLatitude, rightLongitude, points);
			if((System.currentTimeMillis() - time) > 40){
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
					if (i == 1) {
						pathBearing = (float) (Math.atan2(y - py, x - px) / Math.PI * 180);
					}
					path.lineTo(x, y);
				}
				canvas.drawPath(path, paint);
			}
		}
	}

	
	// to show further direction
	public Path getPath() {
		return path;
	}
	
	public float getPathBearing(){
		return pathBearing;
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
	public boolean onTouchEvent(PointF point) {
		return false;
	}




}
