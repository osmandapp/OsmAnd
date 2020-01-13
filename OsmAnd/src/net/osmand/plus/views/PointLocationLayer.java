package net.osmand.plus.views;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.LayerDrawable;
import android.support.v4.content.ContextCompat;

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

import static android.graphics.Paint.ANTI_ALIAS_FLAG;
import static android.graphics.Paint.FILTER_BITMAP_FLAG;

public class PointLocationLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider {
	private static final Log LOG = PlatformUtil.getLog(PointLocationLayer.class);


	protected final static int RADIUS = 7;

	private Paint locationPaint;
	private Paint headingPaint;
	private Paint area;
	private Paint aroundArea;

	private OsmandMapTileView view;

	private ApplicationMode appMode;
	private int color;
	private Bitmap bearingIcon;
	private Bitmap headingIcon;
	private Bitmap locationIconCenter;
	private Bitmap locationIconTop;
	private Bitmap locationIconBottom;
	private OsmAndLocationProvider locationProvider;
	private MapViewTrackingUtilities mapViewTrackingUtilities;
	private boolean nm;
	private boolean locationOutdated;

	public PointLocationLayer(MapViewTrackingUtilities mv) {
		this.mapViewTrackingUtilities = mv;
	}

	private void initUI() {
		locationPaint = new Paint(ANTI_ALIAS_FLAG | FILTER_BITMAP_FLAG);
		headingPaint = new Paint(ANTI_ALIAS_FLAG | FILTER_BITMAP_FLAG);
		area = new Paint();
		area.setColor(view.getResources().getColor(R.color.pos_area));
		aroundArea = new Paint();
		aroundArea.setColor(view.getResources().getColor(R.color.pos_around));
		aroundArea.setStyle(Style.STROKE);
		aroundArea.setStrokeWidth(1);
		aroundArea.setAntiAlias(true);
		locationProvider = view.getApplication().getLocationProvider();
		updateIcons(view.getSettings().getApplicationMode(), false, locationProvider.getLastKnownLocation() == null);
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
		Location lastKnownLocation = locationProvider.getLastStaleKnownLocation();
		updateIcons(view.getSettings().getApplicationMode(), nm,
				view.getApplication().getLocationProvider().getLastKnownLocation() == null);
		if(lastKnownLocation == null || view == null){
			return;
		}
		int locationX;
		int locationY;
		if (mapViewTrackingUtilities.isMapLinkedToLocation()
				&& !MapViewTrackingUtilities.isSmallSpeedForAnimation(lastKnownLocation)
				&& !mapViewTrackingUtilities.isMovingToMyLocation()) {
			locationX = box.getPixXFromLonNoRot(box.getLongitude());
			locationY = box.getPixYFromLatNoRot(box.getLatitude());
		} else {
			locationX = box.getPixXFromLonNoRot(lastKnownLocation.getLongitude());
			locationY = box.getPixYFromLatNoRot(lastKnownLocation.getLatitude());
		}

		final double dist = box.getDistance(0, box.getPixHeight() / 2, box.getPixWidth(), box.getPixHeight() / 2);
		int radius = (int) (((double) box.getPixWidth()) / dist * lastKnownLocation.getAccuracy());

		if (radius > RADIUS * box.getDensity()) {
			int allowedRad = Math.min(box.getPixWidth() / 2, box.getPixHeight() / 2);
			canvas.drawCircle(locationX, locationY, Math.min(radius, allowedRad), area);
			canvas.drawCircle(locationX, locationY, Math.min(radius, allowedRad), aroundArea);
		}
		// draw bearing/direction/location
		if (isLocationVisible(box, lastKnownLocation)) {

			Float heading = locationProvider.getHeading();
			if (!locationOutdated && heading != null && mapViewTrackingUtilities.isShowViewAngle()) {

				canvas.save();
				canvas.rotate(heading - 180, locationX, locationY);
				canvas.drawBitmap(headingIcon, locationX - headingIcon.getWidth() / 2,
						locationY - headingIcon.getHeight() / 2, headingPaint);
				canvas.restore();

			}
			// Issue 5538: Some devices return positives for hasBearing() at rest, hence add 0.0 check:
			boolean isBearing = lastKnownLocation.hasBearing() && (lastKnownLocation.getBearing() != 0.0);
			if (!locationOutdated && isBearing) {
				float bearing = lastKnownLocation.getBearing();
				canvas.rotate(bearing - 90, locationX, locationY);
				canvas.drawBitmap(bearingIcon, locationX - bearingIcon.getWidth() / 2,
						locationY - bearingIcon.getHeight() / 2, locationPaint);
			} else {
				if (locationIconTop != null) {
					canvas.drawBitmap(locationIconTop, locationX - locationIconCenter.getWidth() / 2,
							locationY - locationIconCenter.getHeight() / 2, headingPaint);
				}
				if (locationIconCenter != null) {
					canvas.drawBitmap(locationIconCenter, locationX - locationIconCenter.getWidth() / 2,
							locationY - locationIconCenter.getHeight() / 2, locationPaint);
				}
				if (locationIconBottom != null) {
					canvas.drawBitmap(locationIconBottom, locationX - locationIconCenter.getWidth() / 2,
							locationY - locationIconCenter.getHeight() / 2, headingPaint);
				}
			}

		}
	}

	public boolean isLocationVisible(RotatedTileBox tb, Location l) {
		return l != null && tb.containsLatLon(l.getLatitude(), l.getLongitude());
	}

	@Override
	public void destroyLayer() {
	}

	private void updateIcons(ApplicationMode appMode, boolean nighMode, boolean locationOutdated) {
		if (appMode != this.appMode || this.nm != nighMode || this.locationOutdated != locationOutdated ||
				color != appMode.getIconColorInfo().getColor(nighMode)) {
			this.appMode = appMode;
			this.color = appMode.getIconColorInfo().getColor(nighMode);
			this.nm = nighMode;
			this.locationOutdated = locationOutdated;
			bearingIcon = BitmapFactory.decodeResource(view.getResources(), appMode.getResourceBearingDay());
			headingIcon = BitmapFactory.decodeResource(view.getResources(), appMode.getResourceHeadingDay());
			if (locationOutdated) {
				locationPaint.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(view.getContext(),
						R.color.icon_color_secondary_light), PorterDuff.Mode.SRC_IN));
			} else {
				locationPaint.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(view.getContext(), color),
						PorterDuff.Mode.SRC_IN));
			}
			LayerDrawable layerDrawable = (LayerDrawable) ContextCompat.getDrawable(view.getContext(), appMode.getResourceLocationDay());
			if (layerDrawable != null) {
				locationIconTop = ((BitmapDrawable) layerDrawable.getDrawable(0)).getBitmap();
				locationIconCenter = ((BitmapDrawable) layerDrawable.getDrawable(1)).getBitmap();
				locationIconBottom = ((BitmapDrawable) layerDrawable.getDrawable(2)).getBitmap();
			}
			area.setColor(view.getResources().getColor(!nm ? R.color.pos_area : R.color.pos_area_night));
		}
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o, boolean unknownLocation) {
		if (tileBox.getZoom() >= 3) {
			getMyLocationFromPoint(tileBox, point, o);
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		return getMyLocation();
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

	@Override
	public boolean runExclusiveAction(Object o, boolean unknownLocation) {
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
