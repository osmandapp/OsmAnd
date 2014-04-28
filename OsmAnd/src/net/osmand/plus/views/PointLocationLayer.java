package net.osmand.plus.views;


import net.osmand.Location;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.R;
import net.osmand.plus.base.MapViewTrackingUtilities;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;

public class PointLocationLayer extends OsmandMapLayer {
	protected final static int RADIUS = 7;
	protected final static float HEADING_ANGLE = 60;
	
	private Paint locationPaint;
	private Paint area;
	private Paint aroundArea;
	private Paint headingPaint;
	
	private OsmandMapTileView view;
	
	private ApplicationMode appMode;
	private Bitmap bearingIcon;
	private Bitmap locationIcon;
	private OsmAndLocationProvider locationProvider;
	private MapViewTrackingUtilities mapViewTrackingUtilities;
	private boolean nm;
	
	public PointLocationLayer(MapViewTrackingUtilities mv) {
		this.mapViewTrackingUtilities = mv;
	}

	private void initUI() {
		locationPaint = new Paint();
		locationPaint.setAntiAlias(true);
		locationPaint.setFilterBitmap(true);
		locationPaint.setDither(true);
		
		
		area = new Paint();
		area.setColor(view.getResources().getColor(R.color.pos_area));
		
		aroundArea = new Paint();
		aroundArea.setColor(view.getResources().getColor(R.color.pos_around));
		aroundArea.setStyle(Style.STROKE);
		aroundArea.setStrokeWidth(1);
		aroundArea.setAntiAlias(true);
		
		headingPaint = new Paint();
		headingPaint.setColor(view.getResources().getColor(R.color.pos_heading));
		headingPaint.setAntiAlias(true);
		headingPaint.setStyle(Style.FILL);
		
		checkAppMode(view.getSettings().getApplicationMode());
		locationProvider = view.getApplication().getLocationProvider();
	}
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		initUI();
	}


	
	private RectF getHeadingRect(int locationX, int locationY){
		int rad = (int) (view.getDensity() * 35);
		return new RectF(locationX - rad, locationY - rad, locationX + rad, locationY + rad);
	}
	
	@Override
	public void onDraw(Canvas canvas, RotatedTileBox box, DrawSettings nightMode) {
		// draw
		boolean nm = nightMode != null && nightMode.isNightMode();
		if(nm != this.nm) {
			this.nm = nm;
			area.setColor(view.getResources().getColor(!nm?R.color.pos_area : R.color.pos_area_night));
			headingPaint.setColor(view.getResources().getColor(!nm?R.color.pos_heading :R.color.pos_heading_night));
		}
		Location lastKnownLocation = locationProvider.getLastKnownLocation();
		if(lastKnownLocation == null || view == null){
			return;
		}
		int locationX = box.getPixXFromLonNoRot(lastKnownLocation.getLongitude());
		int locationY = box.getPixYFromLatNoRot(lastKnownLocation.getLatitude());

		final double dist = box.getDistance(0, box.getPixHeight() / 2, box.getPixWidth(), box.getPixHeight() / 2);
		int radius = (int) (((double) box.getPixWidth()) / dist * lastKnownLocation.getAccuracy());
		
		if (radius > RADIUS * box.getDensity()) {
			int allowedRad = Math.min(box.getPixWidth() / 2, box.getPixHeight() / 2);
			canvas.drawCircle(locationX, locationY, Math.min(radius, allowedRad), area);
			canvas.drawCircle(locationX, locationY, Math.min(radius, allowedRad), aroundArea);
		}
		// draw bearing/direction/location
		if (isLocationVisible(box, lastKnownLocation)) {
			checkAppMode(view.getSettings().getApplicationMode());
			boolean isBearing = lastKnownLocation.hasBearing();
			if (!isBearing) {
				canvas.drawBitmap(locationIcon, locationX - locationIcon.getWidth() / 2, locationY - locationIcon.getHeight() / 2,
						locationPaint);
			}
			Float heading = locationProvider.getHeading();
			if (heading != null && mapViewTrackingUtilities.isShowViewAngle()) {
				canvas.drawArc(getHeadingRect(locationX, locationY), heading - HEADING_ANGLE / 2 - 90, HEADING_ANGLE, true, headingPaint);
			}
			if (isBearing) {
				float bearing = lastKnownLocation.getBearing();
				canvas.rotate(bearing - 90, locationX, locationY);
				canvas.drawBitmap(bearingIcon, locationX - bearingIcon.getWidth() / 2, locationY - bearingIcon.getHeight() / 2,
						locationPaint);
			}

		}
	}

	public boolean isLocationVisible(RotatedTileBox tb, Location l){
		if(l == null ){
			return false;
		}
		return tb.containsLatLon(l.getLatitude(), l.getLongitude());
	}
	

	@Override
	public void destroyLayer() {
		
	}
	public void checkAppMode(ApplicationMode appMode) {
		if (appMode != this.appMode) {
			this.appMode = appMode;
			bearingIcon = BitmapFactory.decodeResource(view.getResources(), appMode.getResourceBearing());
			locationIcon = BitmapFactory.decodeResource(view.getResources(), appMode.getResourceLocation());
		}
		
	}
	@Override
	public boolean drawInScreenPixels() {
		return false;
	}


}
