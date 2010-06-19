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
	
	public RouteLayer(RoutingHelper helper){
		this.helper = helper;
	}
	

	private void initUI() {
		boundsRect = new Rect(0, 0, view.getWidth(), view.getHeight());
		tileRect = new RectF();
		paint = new Paint();
		paint.setColor(Color.GRAY);
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
		if (helper.hasPointsToShow()) {
			long time = System.currentTimeMillis();
			boundsRect = new Rect(0, 0, view.getWidth(), view.getHeight());
			view.calculateTileRectangle(boundsRect, view.getCenterPointX(), view.getCenterPointY(), view.getXTile(), view.getYTile(),
					tileRect);
			double topLatitude = MapUtils.getLatitudeFromTile(view.getZoom(), tileRect.top);
			double leftLongitude = MapUtils.getLongitudeFromTile(view.getZoom(), tileRect.left);
			double bottomLatitude = MapUtils.getLatitudeFromTile(view.getZoom(), tileRect.bottom);
			double rightLongitude = MapUtils.getLongitudeFromTile(view.getZoom(), tileRect.right);
			helper.fillLocationsToShow(topLatitude, leftLongitude, bottomLatitude, rightLongitude, points);
			if((System.currentTimeMillis() - time) > 40){
				Log.e(LogUtil.TAG, "Calculate route layer " + (System.currentTimeMillis() - time));
			}
			if (points.size() > 0) {
				int px = view.getMapXForPoint(points.get(0).getLongitude());
				int py = view.getMapYForPoint(points.get(0).getLatitude());
				path.reset();
				path.moveTo(px, py);
				for (Location o : points) {
					int x = view.getMapXForPoint(o.getLongitude());
					int y = view.getMapYForPoint(o.getLatitude());
					path.lineTo(x, y);
				}
				canvas.drawPath(path, paint);
			}
		}
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
