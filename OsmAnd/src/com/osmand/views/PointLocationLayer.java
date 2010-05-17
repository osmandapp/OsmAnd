package com.osmand.views;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.view.MotionEvent;

import com.osmand.osm.MapUtils;

public class PointLocationLayer implements OsmandMapLayer {
	private Paint location;
	private Paint area;
	
	protected Location lastKnownLocation = null;
	protected final static int RADIUS = 7;
	private OsmandMapTileView view;

	private void initUI() {
		location = new Paint();
		location.setColor(Color.BLUE);
		location.setAlpha(150);
		location.setAntiAlias(true);
		
		area = new Paint();
		area.setColor(Color.BLUE);
		area.setAlpha(40);		
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
		if (isLocationVisible(lastKnownLocation)) {
			int locationX = MapUtils.getPixelShiftX(view.getZoom(), lastKnownLocation.getLongitude(), view.getLongitude(), view
					.getTileSize())
					+ view.getWidth() / 2;
			int locationY = MapUtils
					.getPixelShiftY(view.getZoom(), lastKnownLocation.getLatitude(), view.getLatitude(), view.getTileSize())
					+ view.getHeight() / 2;
			// TODO specify bearing!
			int radius = MapUtils.getLengthXFromMeters(view.getZoom(), view.getLatitude(), view.getLongitude(), lastKnownLocation
					.getAccuracy(), view.getTileSize(), view.getWidth());

			if (locationX >= 0 && locationY >= 0) {
				canvas.drawCircle(locationX, locationY, RADIUS, location);
			}
			if (radius > RADIUS) {
				canvas.drawCircle(locationX, locationY, radius, area);
			}
		}
	}

	public boolean isLocationVisible(Location l){
		if(l == null || view == null){
			return false;
		}
		int newX = MapUtils.getPixelShiftX(view.getZoom(), 
				l.getLongitude(), view.getLongitude(), view.getTileSize()) + 
				view.getWidth()/2;
		int newY = MapUtils.getPixelShiftY(view.getZoom(), 
				l.getLatitude(), view.getLatitude() , view.getTileSize()) + 
				view.getHeight()/2;
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
	
	public void setLastKnownLocation(Location lastKnownLocation) {
		boolean redraw = isLocationVisible(this.lastKnownLocation) || isLocationVisible(lastKnownLocation);
		this.lastKnownLocation = lastKnownLocation;
		if(redraw){
			view.prepareImage();
		}
	}

	@Override
	public void destroyLayer() {
		
	}




}
