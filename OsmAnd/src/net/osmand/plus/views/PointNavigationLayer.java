package net.osmand.plus.views;

import java.util.ArrayList;
import java.util.List;

import net.osmand.osm.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.routing.RoutingHelper;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.RectF;
import android.location.Location;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class PointNavigationLayer extends OsmandMapLayer {
	protected final static int DIST_TO_SHOW = 80;

	private Paint point;
	private Paint bitmapPaint;
	
	protected LatLon pointToNavigate = null;
	protected List<LatLon> intermediatePoints = new ArrayList<LatLon>();
	private OsmandMapTileView view;
	private float[] calculations = new float[2];
	
	private DisplayMetrics dm;
	private Bitmap targetPoint;
	private Bitmap intermediatePoint;
	private Bitmap arrowToDestination;

	private Paint textPaint;

	private RoutingHelper routingHelper;
	
	

	private void initUI() {
		
		point = new Paint();
		point.setColor(view.getResources().getColor(R.color.nav_point));
		point.setAntiAlias(true);
		point.setStyle(Style.FILL);

		bitmapPaint = new Paint();
		bitmapPaint.setDither(true);
		bitmapPaint.setAntiAlias(true);
		bitmapPaint.setFilterBitmap(true);
		textPaint = new Paint();
		float sp = Resources.getSystem().getDisplayMetrics().scaledDensity;
		textPaint.setTextSize(sp * 18);
		textPaint.setTextAlign(Align.CENTER);
		textPaint.setAntiAlias(true);
		targetPoint = BitmapFactory.decodeResource(view.getResources(), R.drawable.target_point);
		intermediatePoint = BitmapFactory.decodeResource(view.getResources(), R.drawable.intermediate_point);
		arrowToDestination = BitmapFactory.decodeResource(view.getResources(), R.drawable.arrow_to_destination);

		
	}
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);
		routingHelper = view.getApplication().getRoutingHelper();
		initUI();
	}


	
	@Override
	public void onDraw(Canvas canvas, RectF latLonBounds, RectF tilesRect, DrawSettings nightMode) {
		int index = 0;
		
		if(routingHelper != null && routingHelper.isFollowingMode() && routingHelper.isRouteCalculated()) {
			List<LatLon> ip = routingHelper.getIntermediatePoints();
			int sz = ip == null ? 0 : ip.size();
			while(sz > intermediatePoints.size()) {
				intermediatePoints.remove(0);
			}
		}
		for (LatLon ip : intermediatePoints) {
			index ++;
			if (isLocationVisible(ip)) {
				int marginX = intermediatePoint.getWidth() / 3;
				int marginY = 2 * intermediatePoint.getHeight() / 3;
				int locationX = view.getMapXForPoint(ip.getLongitude());
				int locationY = view.getMapYForPoint(ip.getLatitude());
				canvas.rotate(-view.getRotate(), locationX, locationY);
				canvas.drawBitmap(intermediatePoint, locationX - marginX, locationY - marginY, bitmapPaint);
				canvas.drawText(index + "", locationX + marginX, locationY - marginY / 2, textPaint);
				canvas.rotate(view.getRotate(), locationX, locationY);
			}
		}
		if (isLocationVisible(pointToNavigate)) {
			int marginX = targetPoint.getWidth() / 3;
			int marginY = 2 * targetPoint.getHeight() / 3;
			int locationX = view.getMapXForPoint(pointToNavigate.getLongitude());
			int locationY = view.getMapYForPoint(pointToNavigate.getLatitude());
			canvas.rotate(-view.getRotate(), locationX, locationY);
			canvas.drawBitmap(targetPoint, locationX - marginX, locationY - marginY, bitmapPaint);
		} else if (pointToNavigate != null) {
			Location.distanceBetween(view.getLatitude(), view.getLongitude(), pointToNavigate.getLatitude(),
					pointToNavigate.getLongitude(), calculations);
			float bearing = calculations[1] - 90;
			float radiusBearing = DIST_TO_SHOW * dm.density;
			canvas.rotate(bearing, view.getCenterPointX(), view.getCenterPointY());
			canvas.translate(-24 * dm.density + radiusBearing, -22 * dm.density);
			canvas.drawBitmap(arrowToDestination, view.getCenterPointX(), view.getCenterPointY(), bitmapPaint);
		}
		
	}

	public boolean isLocationVisible(LatLon p){
		if(p == null || view == null){
			return false;
		}
		return view.isPointOnTheRotatedMap(p.getLatitude(), p.getLongitude());
	}
	
	
	public LatLon getPointToNavigate() {
		return pointToNavigate;
	}
	
	public void setPointToNavigate(LatLon pointToNavigate, List<LatLon> intermediatePoints) {
		this.pointToNavigate = pointToNavigate;
		this.intermediatePoints.clear();
		this.intermediatePoints.addAll(intermediatePoints);
		view.refreshMap();
	}
	
	public List<LatLon> getIntermediatePoints() {
		return intermediatePoints;
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
