package net.osmand.plus.views;

import net.osmand.osm.LatLon;
import net.osmand.plus.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.location.Location;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class PointNavigationLayer extends OsmandMapLayer {
	protected final static int DIST_TO_SHOW = 80;

	private Paint point;
	private Paint bitmapPaint;
	
	protected LatLon pointToNavigate = null;
	private OsmandMapTileView view;
	private float[] calculations = new float[2];
	
	private DisplayMetrics dm;
	private Bitmap targetPoint;
	private Bitmap arrowToDestination;
	
	

	private void initUI() {
		
		point = new Paint();
		point.setColor(view.getResources().getColor(R.color.nav_point));
		point.setAntiAlias(true);
		point.setStyle(Style.FILL);

		bitmapPaint = new Paint();
		bitmapPaint.setDither(true);
		bitmapPaint.setAntiAlias(true);
		bitmapPaint.setFilterBitmap(true);
		targetPoint = BitmapFactory.decodeResource(view.getResources(), R.drawable.target_point);
		arrowToDestination = BitmapFactory.decodeResource(view.getResources(), R.drawable.arrow_to_destination);

		
	}
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);
		initUI();
	}


	
	@Override
	public void onDraw(Canvas canvas, RectF latLonBounds, RectF tilesRect, DrawSettings nightMode) {
		if(pointToNavigate == null){
			return;
		}
		if (isLocationVisible()) {
			int marginX = targetPoint.getWidth() / 3;
			int marginY = 2 * targetPoint.getHeight() / 3;
			int locationX = view.getMapXForPoint(pointToNavigate.getLongitude());
			int locationY = view.getMapYForPoint(pointToNavigate.getLatitude());
			canvas.rotate(-view.getRotate(), locationX, locationY);
			canvas.drawBitmap(targetPoint, locationX - marginX, locationY - marginY, bitmapPaint);
		} else {
			Location.distanceBetween(view.getLatitude(), view.getLongitude(), pointToNavigate.getLatitude(),
					pointToNavigate.getLongitude(), calculations);
			float bearing = calculations[1] - 90;
			float radiusBearing = DIST_TO_SHOW * dm.density;
			canvas.rotate(bearing, view.getCenterPointX(), view.getCenterPointY());
			canvas.translate(-24 * dm.density + radiusBearing, -22 * dm.density);
			canvas.drawBitmap(arrowToDestination, view.getCenterPointX(), view.getCenterPointY(), bitmapPaint);
			
		}
	}

	public boolean isLocationVisible(){
		if(pointToNavigate == null || view == null){
			return false;
		}
		return view.isPointOnTheRotatedMap(pointToNavigate.getLatitude(), pointToNavigate.getLongitude());
	}
	
	
	public LatLon getPointToNavigate() {
		return pointToNavigate;
	}
	
	public void setPointToNavigate(LatLon pointToNavigate) {
		this.pointToNavigate = pointToNavigate;
		view.refreshMap();
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
