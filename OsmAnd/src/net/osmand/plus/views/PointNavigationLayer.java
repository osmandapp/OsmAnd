package net.osmand.plus.views;

import net.osmand.osm.LatLon;
import net.osmand.plus.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class PointNavigationLayer implements OsmandMapLayer {
	protected final static int RADIUS = 10;
	protected final static int DIST_TO_SHOW = 120;

	private Paint point;
	
	protected LatLon pointToNavigate = null;
	private OsmandMapTileView view;
	private Path pathForDirection;
	private float[] calculations = new float[2];
	private DisplayMetrics dm;
	private Bitmap targetPoint;
	

	private void initUI() {
		point = new Paint();
		point.setAntiAlias(true);
		targetPoint = BitmapFactory.decodeResource(view.getResources(), R.drawable.target_point);

		pathForDirection = new Path();
		
	}
	
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);
		initUI();
	}


	
	@Override
	public void onDraw(Canvas canvas, RectF latLonBounds, RectF tilesRect, boolean nightMode) {
		if(pointToNavigate == null){
			return;
		}
		if (isLocationVisible()) {
			int marginX = targetPoint.getWidth() / 3;
			int marginY = 2 * targetPoint.getHeight() / 3;
			int locationX = view.getMapXForPoint(pointToNavigate.getLongitude());
			int locationY = view.getMapYForPoint(pointToNavigate.getLatitude());
			canvas.rotate(-view.getRotate(), locationX, locationY);
			Drawable drawable = view.getResources().getDrawable(R.drawable.target_point);
			drawable.draw(canvas);
			canvas.drawBitmap(targetPoint, locationX - marginX, locationY - marginY, point);
		} else {
			Location.distanceBetween(view.getLatitude(), view.getLongitude(), pointToNavigate.getLatitude(),
					pointToNavigate.getLongitude(), calculations);
			float bearing = calculations[1];
			pathForDirection.reset();
			pathForDirection.moveTo(0, 0);
			pathForDirection.lineTo(0.5f, 1.5f);
			pathForDirection.lineTo(-0.5f, 1.5f);
			pathForDirection.lineTo(0, 0);
			float radiusBearing = DIST_TO_SHOW;
			Matrix m = new Matrix();
			m.reset();
			m.postScale(RADIUS * dm.density * 2, RADIUS * 2 * dm.density);
			m.postTranslate(0, - radiusBearing * dm.density );
			m.postTranslate(view.getCenterPointX(), view.getCenterPointY());
			m.postRotate(bearing, view.getCenterPointX(), view.getCenterPointY());
			pathForDirection.transform(m);
			canvas.drawPath(pathForDirection, point);
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
	public boolean onTouchEvent(PointF point) {
		return false;
	}

}
