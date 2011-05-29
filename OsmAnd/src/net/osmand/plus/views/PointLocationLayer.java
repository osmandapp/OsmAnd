package net.osmand.plus.views;

import net.osmand.osm.MapUtils;
import net.osmand.plus.activities.ApplicationMode;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.location.Location;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class PointLocationLayer implements OsmandMapLayer {
	protected final static int RADIUS = 7;
	protected final static int HEADING_RADIUS = 60;
	protected final static float HEADING_ANGLE = 60;
	
	private Paint location;
	private Paint bearing;
	private Paint bearingOver;
	private Paint area;
	private Paint headingPaint;
	private Path pathForDirection;
	
	protected Location lastKnownLocation = null;
	private DisplayMetrics dm;
	private OsmandMapTileView view;
	
	private Float heading = null;
	
	private ApplicationMode appMode;
	
	

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
		
		bearingOver = new Paint();
		bearingOver.setColor(Color.BLACK);
		bearingOver.setAntiAlias(true);
		bearingOver.setStyle(Style.STROKE);
		
		pathForDirection = new Path();
	}
	
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		initUI();
		dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);
	}


	
	private RectF getHeadingRect(int locationX, int locationY){
		int rad = Math.min(3*view.getWidth()/8, 3*view.getHeight()/8);
		return new RectF(locationX - rad, locationY - rad, locationX + rad, locationY + rad);
	}
	
	@Override
	public void onDraw(Canvas canvas, RectF latLonBounds, RectF tilesRect, boolean nightMode) {
		if (isLocationVisible(lastKnownLocation)) {
			checkAppMode(view.getSettings().getApplicationMode());
			
			int locationX = view.getMapXForPoint(lastKnownLocation.getLongitude());
			int locationY = view.getMapYForPoint(lastKnownLocation.getLatitude());
			
			int radius = MapUtils.getLengthXFromMeters(view.getZoom(), view.getLatitude(), view.getLongitude(), 
					lastKnownLocation.getAccuracy(), view.getTileSize(), view.getWidth());

			if(appMode == ApplicationMode.CAR){
				if(!lastKnownLocation.hasBearing()){
					canvas.drawCircle(locationX, locationY, RADIUS * 2.5f * dm.density, location);
					canvas.drawCircle(locationX, locationY, RADIUS * 2.5f * dm.density, bearingOver);
				}
			} else {
				canvas.drawCircle(locationX, locationY, RADIUS * dm.density, location);
				canvas.drawCircle(locationX, locationY, RADIUS * dm.density, bearingOver);
			}
			if (radius > RADIUS) {
				canvas.drawCircle(locationX, locationY, radius, area);
			}
			if(heading != null){
				canvas.drawArc(getHeadingRect(locationX, locationY), 
						heading - HEADING_ANGLE/ 2 - 90, HEADING_ANGLE, true, headingPaint);
			}
			
			if(lastKnownLocation.hasBearing()){
				float bearing = lastKnownLocation.getBearing();
				int radiusBearing = (int) (30 * dm.density);
				if(lastKnownLocation.hasSpeed() && appMode != ApplicationMode.CAR){
					radiusBearing = 
						Math.max(MapUtils.getLengthXFromMeters(view.getZoom(), view.getLatitude(), view.getLongitude(), 
							lastKnownLocation.getSpeed(), view.getTileSize(), view.getWidth()) * 2, radiusBearing);
					radiusBearing = Math.min(radiusBearing, view.getHeight() / 4);
				}
				radiusBearing += RADIUS * dm.density /2;
				
				pathForDirection.reset();
				pathForDirection.moveTo(0, 0);
				pathForDirection.lineTo((float) RADIUS, 1f);
				pathForDirection.lineTo((float) -RADIUS, 1f);
				pathForDirection.lineTo(0, 0);
				Matrix m = new Matrix();
				m.reset();
				if(appMode == ApplicationMode.CAR){
					m.postScale(2.5f * dm.density, radiusBearing * 1.5f);
					m.postTranslate(0, -radiusBearing/2);
				} else if(appMode == ApplicationMode.BICYCLE){
					m.postScale(2 * dm.density, radiusBearing);
					m.postTranslate(0, -radiusBearing/2);
				} else {
					m.postScale(dm.density, radiusBearing * 0.5f);
					m.postTranslate(0, -radiusBearing);
				}
				m.postTranslate(locationX, locationY);
				m.postRotate(bearing, locationX, locationY);
				
				pathForDirection.transform(m);
				canvas.drawPath(pathForDirection, this.bearing);
				canvas.drawPath(pathForDirection, this.bearingOver);
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
	
	public void setHeading(Float heading){
		this.heading = heading;
		if(!view.mapIsRefreshing() && isLocationVisible(this.lastKnownLocation)){
			view.refreshMap();
		}
	}
	
	public Float getHeading() {
		return heading;
	}
	
	public void setLastKnownLocation(Location lastKnownLocation) {
		boolean redraw = isLocationVisible(this.lastKnownLocation) || isLocationVisible(lastKnownLocation);
		this.lastKnownLocation = lastKnownLocation;
		if (redraw) {
			view.refreshMap();
		}
	}

	@Override
	public void destroyLayer() {
		
	}
	public void checkAppMode(ApplicationMode appMode) {
		if (appMode != this.appMode) {
			this.appMode = appMode;
			if (this.appMode == ApplicationMode.CAR || this.appMode == ApplicationMode.BICYCLE) {
				this.bearing.setAlpha(180);
			} else {
				this.bearing.setAlpha(150);
			}
		}
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
