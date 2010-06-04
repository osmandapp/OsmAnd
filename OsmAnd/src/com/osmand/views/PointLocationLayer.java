package com.osmand.views;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.location.Location;

import com.osmand.osm.MapUtils;

public class PointLocationLayer implements OsmandMapLayer {
	protected final static int RADIUS = 7;
	protected final static int HEADING_RADIUS = 60;
	protected final static float HEADING_ANGLE = 60;
	
	private Paint location;
	private Paint bearing;
	private Paint area;
	private Paint headingPaint;
	private Path pathForDirection;
	
	protected Location lastKnownLocation = null;
	
	private OsmandMapTileView view;
	
	private Float heading = null;
	

	private void initUI() {
		location = new Paint();
		location.setColor(Color.BLUE);
		location.setAlpha(150);
		location.setAntiAlias(true);
		
		area = new Paint();
		area.setColor(Color.BLUE);
		area.setAlpha(40);		
		
		headingPaint = new Paint();
		headingPaint.setColor(Color.BLUE);
		headingPaint.setAlpha(50);
		headingPaint.setAntiAlias(true);
		headingPaint.setStyle(Style.FILL);
		
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


	
	private RectF getHeadingRect(int locationX, int locationY){
		int rad = Math.min(3*view.getWidth()/8, 3*view.getHeight()/8);
		return new RectF(locationX - rad, locationY - rad, locationX + rad, locationY + rad);
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		if (isLocationVisible(lastKnownLocation)) {
			int locationX = view.getMapXForPoint(lastKnownLocation.getLongitude());
			int locationY = view.getMapYForPoint(lastKnownLocation.getLatitude());
			int radius = MapUtils.getLengthXFromMeters(view.getZoom(), view.getLatitude(), view.getLongitude(), lastKnownLocation
					.getAccuracy(), view.getTileSize(), view.getWidth());

			canvas.drawCircle(locationX, locationY, RADIUS, location);
			if (radius > RADIUS) {
				canvas.drawCircle(locationX, locationY, radius, area);
			}
			if(heading != null){
				canvas.drawArc(getHeadingRect(locationX, locationY), 
						heading - HEADING_ANGLE/ 2 - 90, HEADING_ANGLE, true, headingPaint);
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
				m.postScale(1, radiusBearing * 0.5f);
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
		return view.isPointOnTheRotatedMap(l.getLatitude(), l.getLongitude());
	}
	
	
	public Location getLastKnownLocation() {
		return lastKnownLocation;
	}
	
	public void setHeading(Float heading, boolean doNotRedraw){
		this.heading = heading;
		if(!doNotRedraw && isLocationVisible(this.lastKnownLocation)){
			view.refreshMap();
		}
	}
	
	public Float getHeading() {
		return heading;
	}
	
	public void setLastKnownLocation(Location lastKnownLocation, boolean doNotRedraw) {
		this.lastKnownLocation = lastKnownLocation;
		if (!doNotRedraw) {
			boolean redraw = isLocationVisible(this.lastKnownLocation) || isLocationVisible(lastKnownLocation);
			if (redraw) {
				view.refreshMap();
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
