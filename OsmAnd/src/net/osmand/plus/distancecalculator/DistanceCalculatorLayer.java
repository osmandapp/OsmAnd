package net.osmand.plus.distancecalculator;

import net.osmand.osm.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

/**
 * @author Maciej Papiez
 *
 */
public class DistanceCalculatorLayer extends OsmandMapLayer {

	private final MapActivity map;
	private OsmandMapTileView view;
	private OsmandSettings settings;

	private LatLon originPoint = null;
	private LatLon destinationPoint = null;
	private Bitmap originIcon;
	private Bitmap destinationIcon;
	private Paint bitmapPaint;


	public DistanceCalculatorLayer(MapActivity map) {
		this.map = map;
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		this.settings = ((OsmandApplication) map.getApplication()).getSettings();
	
		originIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_pin_origin);
		destinationIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_pin_destination);
		
		bitmapPaint = new Paint();
		bitmapPaint.setDither(true);
		bitmapPaint.setAntiAlias(true);
		bitmapPaint.setFilterBitmap(true);
	}

	@Override
	public void onDraw(Canvas canvas, RectF latlonRect, RectF tilesRect,
			DrawSettings settings) {
		drawIcon(canvas, originPoint, originIcon);
		drawIcon(canvas, destinationPoint, destinationIcon);
	}

	/**
	 * Draw a given map pin icon (source or destination point)
	 * @param canvas to draw on
	 * @param point the icon should be drawn at
	 * @param icon to be drawn
	 */
	public void drawIcon(Canvas canvas, LatLon point, Bitmap icon) {
		if (point == null)
			return;

		double latitude = point.getLatitude();
		double longitude = point.getLongitude();
		if (isLocationVisible(latitude, longitude)) {
			int marginY = icon.getHeight();
			int locationX = view.getMapXForPoint(longitude);
			int locationY = view.getMapYForPoint(latitude);
			canvas.rotate(-view.getRotate(), locationX, locationY);
			canvas.drawBitmap(icon, locationX, locationY - marginY, bitmapPaint);
		}
	}

	@Override
	public void destroyLayer() {}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public boolean onLongPressEvent(PointF point) {
		if(originPoint == null) {
			originPoint = view.getLatLonFromScreenPoint(point.x, point.y);
		} else if(destinationPoint == null) {
			destinationPoint = view.getLatLonFromScreenPoint(point.x, point.y);
			
			double startLatitude = originPoint.getLatitude();
			double startLongitude = originPoint.getLongitude();
			double endLatitude = destinationPoint.getLatitude();
			double endLongitude = destinationPoint.getLongitude();
			float[] results = new float[3];
			
			Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, results);
			
			(Toast.makeText(view.getContext(), "The distance = " + formatDistance(results[0]), Toast.LENGTH_SHORT)).show();
			
			Log.i("SOMETAG", "Distance calculated: " + results[0]);
		} else {
			originPoint = null;
			destinationPoint = null;
		}
		
		view.refreshMap();

		return true;
	}
	
	/**
	 * Format a distance into String representation with correct unit
	 * @param distance to be formatted (in meters)
	 * @return the formatted String with the distance and unit (m or km)
	 */
	private String formatDistance(float distance) {
		if(distance < 5000)
			return String.format("%.2f m", distance);
		else
			return String.format("%.2f km", distance / 1000);
	}

	/**
	 * @param latitude
	 * @param longitude
	 * @return true if the distance measurement point is located on a visible part of map
	 */
	private boolean isLocationVisible(double latitude, double longitude) {
		if(originPoint == null || view == null){
			return false;
		}
		return view.isPointOnTheRotatedMap(latitude, longitude);
	}
}

