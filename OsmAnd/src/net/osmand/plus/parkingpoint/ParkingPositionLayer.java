package net.osmand.plus.parkingpoint;

import java.util.List;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.DisplayMetrics;
import android.view.WindowManager;

/**
 * Class represents a layer which depicts the position of the parked car
 * @author Alena Fedasenka
 * @see ParkingPositionPlugin
 *
 */
public class ParkingPositionLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider {
	/**
	 * magic number so far
	 */
	private static final int radius = 18;

	private DisplayMetrics dm;
	
	private final MapActivity map;
	private OsmandMapTileView view;
	
	private Paint bitmapPaint;

	private Bitmap parkingNoLimitIcon;
	private Bitmap parkingLimitIcon;
	
	private boolean timeLimit;

	private ParkingPositionPlugin plugin;

	public ParkingPositionLayer(MapActivity map, ParkingPositionPlugin plugin) {
		this.map = map;
		this.plugin = plugin;
	}
	
	public LatLon getParkingPoint() {
		return plugin.getParkingPosition();
	}
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);

		bitmapPaint = new Paint();
		bitmapPaint.setDither(true);
		bitmapPaint.setAntiAlias(true);
		bitmapPaint.setFilterBitmap(true);
		parkingNoLimitIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_poi_parking_pos_no_limit);
		parkingLimitIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_poi_parking_pos_limit);
		timeLimit = plugin.getParkingType();
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tb, DrawSettings nightMode) {
        LatLon parkingPoint = getParkingPoint();
        if (parkingPoint == null)
			return;
		
		Bitmap parkingIcon;
		if (!timeLimit) {
			parkingIcon = parkingNoLimitIcon;
		} else {
			parkingIcon = parkingLimitIcon;
		}
        double latitude = parkingPoint.getLatitude();
		double longitude = parkingPoint.getLongitude();
		if (isLocationVisible(tb, latitude, longitude)) {
			int marginX = parkingNoLimitIcon.getWidth() / 2;
			int marginY = parkingNoLimitIcon.getHeight() / 2;
			int locationX = tb.getPixXFromLonNoRot(longitude);
			int locationY = tb.getPixYFromLatNoRot(latitude);
			canvas.rotate(-view.getRotate(), locationX, locationY);
			canvas.drawBitmap(parkingIcon, locationX - marginX, locationY - marginY, bitmapPaint);
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
	public boolean disableSingleTap() {
		return false;
	}

	@Override
	public boolean disableLongPressOnMap() {
		return false;
	}

	@Override
	public boolean isObjectClickable(Object o) {
		return o == getParkingPoint();
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o) {
		getParkingFromPoint(tileBox, point, o);
	}

	@Override
	public LatLon getObjectLocation(Object o) {
        if(o == getParkingPoint()) {
            return getParkingPoint();
        }
		return null;
	}
	
	@Override
	public String getObjectDescription(Object o) {
		return plugin.getParkingDescription(map);

	}

	public String getFormattedTime(long time){
		return plugin.getFormattedTime(time, map);
	}
	
	@Override
	public PointDescription getObjectName(Object o) {
		return new PointDescription(PointDescription.POINT_TYPE_PARKING_MARKER,
				view.getContext().getString(R.string.osmand_parking_position_name));
	}
	
	public void refresh() {
		if (view != null) {
			view.refreshMap();
		}
	}
	
	/**
	 * @param latitude
	 * @param longitude
	 * @return true if the parking point is located on a visible part of map
	 */
	private boolean isLocationVisible(RotatedTileBox tb, double latitude, double longitude){
		if(getParkingPoint() == null || view == null){
			return false;
		}
		return tb.containsLatLon(latitude, longitude);
	}
	
	/**
	 * @param point
	 * @param parkingPosition
	 *            is in this case not necessarily has to be a list, but it's also used in method
	 *            <link>collectObjectsFromPoint(PointF point, List<Object> o)</link>
	 */
	private void getParkingFromPoint(RotatedTileBox tb, PointF point, List<? super LatLon> parkingPosition) {
		LatLon parkingPoint = getParkingPoint();
		if (parkingPoint != null && view != null) {
			int ex = (int) point.x;
			int ey = (int) point.y;
			LatLon position = plugin.getParkingPosition();
			int x = (int) tb.getPixXFromLatLon(position.getLatitude(), position.getLongitude());
			int y = (int) tb.getPixYFromLatLon(position.getLatitude(), position.getLongitude());
			// the width of an image is 40 px, the height is 60 px -> radius = 20,
			// the position of a parking point relatively to the icon is at the center of the bottom line of the image
			int rad = (int) (radius * tb.getDensity());
			if (Math.abs(x - ex) <= rad && (ey - y) <= rad && (y - ey) <= 2.5 * rad) {
				parkingPosition.add(parkingPoint);
			}
		}
	}
	
}
