package com.osmand.views;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Paint.Style;
import android.location.Location;
import android.util.FloatMath;
import android.view.MotionEvent;

import com.osmand.osm.MapUtils;

public class PointLocationLayer implements OsmandMapLayer {
	private Paint location;
	private Paint bearing;
	private Paint area;
	
	protected Location lastKnownLocation = null;
	protected final static int RADIUS = 7;
	private OsmandMapTileView view;
	private Path pathForDirection;

	private void initUI() {
		location = new Paint();
		location.setColor(Color.BLUE);
		location.setAlpha(150);
		location.setAntiAlias(true);
		
		area = new Paint();
		area.setColor(Color.BLUE);
		area.setAlpha(40);		
		
		bearing = new Paint();
		bearing.setColor(Color.BLUE);
		bearing.setAlpha(150);
		bearing.setAntiAlias(true);
		bearing.setStyle(Style.FILL);
		
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
	
	
	// TODO simplify calculation if possible
	@Override
	public void onDraw(Canvas canvas) {
		if (isLocationVisible(lastKnownLocation)) {
			int locationX = MapUtils.getPixelShiftX(view.getZoom(), lastKnownLocation.getLongitude(), view.getLongitude(), 
					view.getTileSize()) + view.getWidth() / 2;
			int locationY = MapUtils.getPixelShiftY(view.getZoom(), 
					lastKnownLocation.getLatitude(), view.getLatitude(), view.getTileSize()) + view.getHeight() / 2;
			int radius = MapUtils.getLengthXFromMeters(view.getZoom(), view.getLatitude(), view.getLongitude(), lastKnownLocation
					.getAccuracy(), view.getTileSize(), view.getWidth());

			canvas.drawCircle(locationX, locationY, RADIUS, location);
			if (radius > RADIUS) {
				canvas.drawCircle(locationX, locationY, radius, area);
			}
			if(lastKnownLocation.hasBearing()){
				float bearing = lastKnownLocation.getBearing();
				int radiusBearing = 30;
				if(lastKnownLocation.hasSpeed()){
					radiusBearing = 
						Math.max(MapUtils.getLengthXFromMeters(view.getZoom(), view.getLatitude(), view.getLongitude(), 
							lastKnownLocation.getSpeed(), view.getTileSize(), view.getWidth()) * 2, radiusBearing);
				}
				radiusBearing += RADIUS /2;
				
				pathForDirection.reset();
				pathForDirection.moveTo(0, 0);
				pathForDirection.lineTo((float) RADIUS, 1f);
				pathForDirection.lineTo((float) -RADIUS, 1f);
				pathForDirection.lineTo(0, 0);
				Matrix m = new Matrix();
				m.reset();
				m.postScale(1, radiusBearing*0.5f);
				m.postTranslate(0, -radiusBearing);
				m.postTranslate(locationX, locationY);
				m.postRotate(bearing, locationX, locationY);
				
				pathForDirection.transform(m);
				canvas.drawPath(pathForDirection, this.bearing);
			}
			
			

		}
	}

	public boolean isLocationVisible(Location l){
		if(l == null || view == null){
			return false;
		}
		int cx = view.getWidth()/2;
		int cy = view.getHeight()/2;
		int dx = MapUtils.getPixelShiftX(view.getZoom(), 
				l.getLongitude(), view.getLongitude(), view.getTileSize());
		int dy = MapUtils.getPixelShiftY(view.getZoom(), 
				l.getLatitude(), view.getLatitude() , view.getTileSize());
		float rad = (float) Math.toRadians(view.getRotate());
		int newX = (int) (dx * FloatMath.cos(rad) - dy * FloatMath.sin(rad) + cx);
		int newY = (int) (dx * FloatMath.sin(rad) + dy * FloatMath.cos(rad) + cy);
		int radius = MapUtils.getLengthXFromMeters(view.getZoom(), view.getLatitude(), view.getLongitude(), 
				l.getAccuracy(), view.getTileSize(), view.getWidth());
		if(newX >= 0 && newX <= view.getWidth() && newY >=0 && newY <= view.getHeight()){
			return true;
		} else {
			// check radius (simplified version
			if (newX + radius >= 0 && newX - radius <= view.getWidth() && newY + radius >= 0 && newY - radius <= view.getHeight()) {
				return true;
			}
		}
		return false;
	}
	
	
	public Location getLastKnownLocation() {
		return lastKnownLocation;
	}
	
	public void setLastKnownLocation(Location lastKnownLocation, boolean doNotRedraw) {
		this.lastKnownLocation = lastKnownLocation;
		if (!doNotRedraw) {
			boolean redraw = isLocationVisible(this.lastKnownLocation) || isLocationVisible(lastKnownLocation);
			if (redraw) {
				view.prepareImage();
			}
		}
	}

	@Override
	public void destroyLayer() {
		
	}




}
