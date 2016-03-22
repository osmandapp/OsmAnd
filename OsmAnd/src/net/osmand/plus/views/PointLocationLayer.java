package net.osmand.plus.views;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.RectF;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.R;
import net.osmand.plus.base.MapViewTrackingUtilities;

import org.apache.commons.logging.Log;

import java.util.List;

public class PointLocationLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider {
	private static final Log LOG = PlatformUtil.getLog(PointLocationLayer.class);

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
		
		updateIcons(view.getSettings().getApplicationMode(), false);
		locationProvider = view.getApplication().getLocationProvider();
	}
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		initUI();
	}


	
	private RectF getHeadingRect(int locationX, int locationY){
		int rad = (int) (view.getDensity() * 60);
		return new RectF(locationX - rad, locationY - rad, locationX + rad, locationY + rad);
	}
	
	@Override
	public void onDraw(Canvas canvas, RotatedTileBox box, DrawSettings nightMode) {
		if(box.getZoom() < 3) {
			return;
		}
		// draw
		boolean nm = nightMode != null && nightMode.isNightMode();
		updateIcons(view.getSettings().getApplicationMode(), nm);
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
			boolean isBearing = lastKnownLocation.hasBearing();

			Float heading = locationProvider.getHeading();
			if (heading != null && mapViewTrackingUtilities.isShowViewAngle()) {
				canvas.drawArc(getHeadingRect(locationX, locationY), heading - HEADING_ANGLE / 2 - 90, HEADING_ANGLE,
						true, headingPaint);
			}
			if (isBearing) {
				float bearing = lastKnownLocation.getBearing();
				canvas.rotate(bearing - 90, locationX, locationY);
				canvas.drawBitmap(bearingIcon, locationX - bearingIcon.getWidth() / 2,
						locationY - bearingIcon.getHeight() / 2, locationPaint);
			} else {
				canvas.drawBitmap(locationIcon, locationX - locationIcon.getWidth() / 2,
						locationY - locationIcon.getHeight() / 2, locationPaint);
			}

		}
	}

	public boolean isLocationVisible(RotatedTileBox tb, Location l) {
		return l != null && tb.containsLatLon(l.getLatitude(), l.getLongitude());
	}
	

	@Override
	public void destroyLayer() {
		
	}
	public void updateIcons(ApplicationMode appMode, boolean nighMode) {
		if (appMode != this.appMode || this.nm != nighMode) {
			this.appMode = appMode;
			this.nm = nighMode;
			final int resourceBearingDay = appMode.getResourceBearingDay();
			final int resourceBearingNight = appMode.getResourceBearingNight();
			final int resourceBearing = nighMode ? resourceBearingNight : resourceBearingDay;
			bearingIcon = BitmapFactory.decodeResource(view.getResources(), resourceBearing);

			final int resourceLocationDay = appMode.getResourceLocationDay();
			final int resourceLocationNight = appMode.getResourceLocationNight();
			final int resourceLocation = nighMode ? resourceLocationNight : resourceLocationDay;
			locationIcon = BitmapFactory.decodeResource(view.getResources(), resourceLocation);
			area.setColor(view.getResources().getColor(!nm ? R.color.pos_area : R.color.pos_area_night));
			headingPaint.setColor(view.getResources().getColor(!nm ? R.color.pos_heading : R.color.pos_heading_night));
		}
		
	}
	@Override
	public boolean drawInScreenPixels() {
		return false;
	}


	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o) {
		if (tileBox.getZoom() >= 3) {
			getMyLocationFromPoint(tileBox, point, o);
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		return getMyLocation();
	}

	@Override
	public String getObjectDescription(Object o) {
		return view.getResources().getString(R.string.shared_string_my_location);
	}

	@Override
	public PointDescription getObjectName(Object o) {
		return new PointDescription(PointDescription.POINT_TYPE_MY_LOCATION,
				view.getContext().getString(R.string.shared_string_my_location), "");
	}

	@Override
	public boolean disableSingleTap() {
		return false;
	}

	@Override
	public boolean disableLongPressOnMap() {
		return false;
	}

	@Override
	public boolean isObjectClickable(Object o) {
		return false;
	}

	private LatLon getMyLocation() {
		Location location = locationProvider.getLastKnownLocation();
		if (location != null) {
			return new LatLon(location.getLatitude(), location.getLongitude());
		} else {
			return null;
		}
	}

	private void getMyLocationFromPoint(RotatedTileBox tb, PointF point, List<? super LatLon> myLocation) {
		LatLon location = getMyLocation();
		if (location != null && view != null) {
			int ex = (int) point.x;
			int ey = (int) point.y;
			int x = (int) tb.getPixXFromLatLon(location.getLatitude(), location.getLongitude());
			int y = (int) tb.getPixYFromLatLon(location.getLatitude(), location.getLongitude());
			int rad = (int) (18 * tb.getDensity());
			if (Math.abs(x - ex) <= rad && (ey - y) <= rad && (y - ey) <= 2.5 * rad) {
				myLocation.add(location);
			}
		}
	}
}
