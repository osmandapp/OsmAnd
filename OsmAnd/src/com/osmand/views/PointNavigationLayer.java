package com.osmand.views;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Paint.Style;
import android.location.Location;
import android.view.MotionEvent;

import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;

public class PointNavigationLayer implements OsmandMapLayer {
	protected final static int RADIUS = 10;
	protected final static int DIST_TO_SHOW = 120;

	private Paint point;
	
	protected LatLon pointToNavigate = null;
	private OsmandMapTileView view;
	private Path pathForDirection;
	private float[] calculations = new float[2];
	

	private void initUI() {
		point = new Paint();
		point.setColor(Color.rgb(250, 80, 80));
		point.setAlpha(230);
		point.setAntiAlias(true);
		point.setStyle(Style.FILL);

		pathForDirection = new Path();
	}
	
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		initUI();
	}


	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return false;
	}
	
	
	@Override
	public void onDraw(Canvas canvas) {
		if(pointToNavigate == null){
			return;
		}
		if (isLocationVisible()) {
			int locationX = MapUtils.getPixelShiftX(view.getZoom(), pointToNavigate.getLongitude(), view.getLongitude(), 
					view.getTileSize()) + view.getWidth() / 2;
			int locationY = MapUtils.getPixelShiftY(view.getZoom(), 
					pointToNavigate.getLatitude(), view.getLatitude(), view.getTileSize()) + view.getHeight() / 2;

			canvas.drawCircle(locationX, locationY, RADIUS, point);
		} else {
			Location.distanceBetween(view.getLatitude(), view.getLongitude(), pointToNavigate.getLatitude(),
					pointToNavigate.getLongitude(), calculations);
			float bearing = calculations[1];
			pathForDirection.reset();
			pathForDirection.moveTo(0, 0);
			pathForDirection.lineTo(0.5f, 1f);
			pathForDirection.lineTo(-0.5f, 1f);
			pathForDirection.lineTo(0, 0);
			float radiusBearing = DIST_TO_SHOW ;
			Matrix m = new Matrix();
			m.reset();
			m.postScale(RADIUS * 2, RADIUS * 2);
			m.postTranslate(0, -radiusBearing);
			m.postTranslate(view.getWidth() / 2, view.getHeight() / 2);
			m.postRotate(bearing, view.getWidth() / 2, view.getHeight() / 2);
			pathForDirection.transform(m);
			canvas.drawPath(pathForDirection, point);
		}
	}

	public boolean isLocationVisible(){
		if(pointToNavigate == null || view == null){
			return false;
		}
		return view.isPointOnTheMap(pointToNavigate.getLatitude(), pointToNavigate.getLongitude());
	}
	
	
	public LatLon getPointToNavigate() {
		return pointToNavigate;
	}
	
	public void setPointToNavigate(LatLon pointToNavigate) {
		this.pointToNavigate = pointToNavigate;
		view.prepareImage();
	}

	@Override
	public void destroyLayer() {
		
	}




}
