package net.osmand.plus.views;

import net.osmand.osm.MapUtils;
import net.osmand.plus.R;
import net.osmand.plus.activities.ApplicationMode;
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

public class PointLocationLayer implements OsmandMapLayer {
	protected final static int RADIUS = 7;
	protected final static float HEADING_ANGLE = 60;
	
	private Paint locationPaint;
	private Paint area;
	private Paint headingPaint;
	
	protected Location lastKnownLocation = null;
	private DisplayMetrics dm;
	private OsmandMapTileView view;
	
	private Float heading = null;
	
	private ApplicationMode appMode;
	private Bitmap bearingIcon;
	private Bitmap locationIcon;

	private void initUI() {
		locationPaint = new Paint();
		locationPaint.setAntiAlias(true);
		locationPaint.setFilterBitmap(true);
		locationPaint.setDither(true);
		
		area = new Paint();
		area.setColor(Color.BLUE);
		area.setAlpha(40);		
		
		headingPaint = new Paint();
		headingPaint.setColor(Color.BLUE);
		headingPaint.setAlpha(50);
		headingPaint.setAntiAlias(true);
		headingPaint.setStyle(Style.FILL);
		
		checkAppMode(view.getSettings().getApplicationMode());
		
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
			boolean isBearing = lastKnownLocation.hasBearing();
			if(!isBearing){
				canvas.drawBitmap(locationIcon, locationX - locationIcon.getWidth() / 2, 
						locationY - locationIcon.getHeight() / 2, locationPaint);
			}
			if (radius > RADIUS * dm.density) {
				canvas.drawCircle(locationX, locationY, radius, area);
			}
				
			if(heading != null){
				canvas.drawArc(getHeadingRect(locationX, locationY), 
						heading - HEADING_ANGLE/ 2 - 90, HEADING_ANGLE, true, headingPaint);
			}
			
			if(isBearing){
				float bearing = lastKnownLocation.getBearing();
				canvas.rotate(bearing - 90, locationX, locationY);
				canvas.drawBitmap(bearingIcon, locationX - bearingIcon.getWidth() / 2, 
						locationY - bearingIcon.getHeight() / 2, locationPaint);
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
			if (appMode == ApplicationMode.CAR) {
				bearingIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.car_bearing);
				locationIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.car_location);
			} else if (appMode == ApplicationMode.BICYCLE) {
				bearingIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.bicycle_bearing);
				locationIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.bicycle_location);
			} else {
				bearingIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.pedestrian_bearing);
				locationIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.pedestrian_location);
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
