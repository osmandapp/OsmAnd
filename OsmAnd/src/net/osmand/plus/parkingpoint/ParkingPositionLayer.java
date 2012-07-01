package net.osmand.plus.parkingpoint;

import java.util.ArrayList;
import java.util.List;

import net.osmand.OsmAndFormatter;
import net.osmand.access.AccessibleToast;
import net.osmand.osm.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.TextInfoControl;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.RectF;
import android.location.Location;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

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
	private static final int radius = 20;

	private LatLon parkingPoint = null;

	private DisplayMetrics dm;
	
	private final MapActivity map;
	private OsmandMapTileView view;
	private OsmandSettings settings;
	
	private Paint bitmapPaint;
	private Paint paintText;
	private Paint paintSubText;

	private Bitmap parkingNoLimitIcon;
	private Bitmap parkingLimitIcon;
	
	private TextInfoControl parkingPlaceControl;

	private boolean timeLimit;

	public ParkingPositionLayer(MapActivity map) {
		this.map = map;
	}
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		this.settings = ((OsmandApplication) map.getApplication()).getSettings();
		parkingPoint = settings.getParkingPosition();
		dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);

		paintText = new Paint();
		paintText.setStyle(Style.FILL_AND_STROKE);
		paintText.setColor(Color.BLACK);
		paintText.setTextSize(23 * MapInfoLayer.scaleCoefficient);
		paintText.setAntiAlias(true);
		paintText.setStrokeWidth(4);

		paintSubText = new Paint();
		paintSubText.setStyle(Style.FILL_AND_STROKE);
		paintSubText.setColor(Color.BLACK);
		paintSubText.setTextSize(15 * MapInfoLayer.scaleCoefficient);
		paintSubText.setAntiAlias(true);
		
		bitmapPaint = new Paint();
		bitmapPaint.setDither(true);
		bitmapPaint.setAntiAlias(true);
		bitmapPaint.setFilterBitmap(true);
		parkingNoLimitIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.poi_parking_pos_no_limit);
		parkingLimitIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.poi_parking_pos_limit);
		
		
		MapInfoLayer mapInfoLayer = map.getMapLayers().getMapInfoLayer();
		if ((mapInfoLayer != null) && (parkingPlaceControl == null))
			mapInfoLayer.addRightStack(createParkingPlaceInfoControl());
	}

	@Override
	public void onDraw(Canvas canvas, RectF latLonBounds, RectF tilesRect, DrawSettings nightMode) {
//		settings.clearParkingPosition();
		parkingPoint = settings.getParkingPosition();
		if (parkingPoint == null)
			return;
		timeLimit = settings.getParkingType();
		Bitmap parkingIcon;
		if (!timeLimit) {
			parkingIcon = parkingNoLimitIcon;
		} else {
			parkingIcon = parkingLimitIcon;
		}
		double latitude = parkingPoint.getLatitude();
		double longitude = parkingPoint.getLongitude();
		if (isLocationVisible(latitude, longitude)) {
			int marginX = parkingNoLimitIcon.getWidth() / 2;
			int marginY = 72;//magic number!
			int locationX = view.getMapXForPoint(longitude);
			int locationY = view.getMapYForPoint(latitude);
			canvas.rotate(-view.getRotate(), locationX, locationY);
			canvas.drawBitmap(parkingIcon, locationX - marginX, locationY - marginY, bitmapPaint);
		}
	}

	@Override
	public boolean onSingleTap(PointF point) {
		List <LatLon> parkPos = new ArrayList<LatLon>();
		getParkingFromPoint(point, parkPos);
		if(!parkPos.isEmpty()){
			StringBuilder res = new StringBuilder();
			res.append(view.getContext().getString(R.string.osmand_parking_position_description));
			AccessibleToast.makeText(view.getContext(), getObjectDescription(null), Toast.LENGTH_LONG).show();
			return true;
		}
		return false;
	}

	@Override
	public void destroyLayer() {
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, List<Object> o) {
		getParkingFromPoint(point, o);
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		return parkingPoint;
	}
	
	@Override
	public String getObjectDescription(Object o) {	
		StringBuilder timeLimitDesc = new StringBuilder(); 
		if (settings.getParkingType()) {
			long parkingTime = settings.getParkingTime();
			Time time = new Time();
			time.set(parkingTime);
			timeLimitDesc.append(map.getString(R.string.osmand_parking_position_description_add) + " ");
			timeLimitDesc.append(time.hour);
			timeLimitDesc.append(":");
			int minute = time.minute;
			timeLimitDesc.append(minute<10 ? "0" + minute : minute);
			if (!DateFormat.is24HourFormat(map.getApplicationContext())) {
				timeLimitDesc.append(time.hour >= 12 ? map.getString(R.string.osmand_parking_pm) : map.getString(R.string.osmand_parking_am));
		    }
		}
		return map.getString(R.string.osmand_parking_position_description, timeLimitDesc.toString());
	}

	@Override
	public String getObjectName(Object o) {
		return view.getContext().getString(R.string.osmand_parking_position_name);
	}
	
	public void setParkingPointOnLayer(LatLon point) {
		this.parkingPoint = point;
		if (view != null && view.getLayers().contains(ParkingPositionLayer.this)) {
			view.refreshMap();
		}
	}

	/**
	 * @param latitude
	 * @param longitude
	 * @return true if the parking point is located on a visible part of map
	 */
	private boolean isLocationVisible(double latitude, double longitude){
		if(parkingPoint == null || view == null){
			return false;
		}
		return view.isPointOnTheRotatedMap(latitude, longitude);
	}
	
	/**
	 * @param point
	 * @param parkingPosition is in this case not necessarily has to be a list, 
	 * but it's also used in method <link>collectObjectsFromPoint(PointF point, List<Object> o)</link>
	 */
	private void getParkingFromPoint(PointF point, List<? super LatLon> parkingPosition) {
		if (parkingPoint != null && view != null) {
			int ex = (int) point.x;
			int ey = (int) point.y;
			LatLon position = settings.getParkingPosition();
			int x = view.getRotatedMapXForPoint(position.getLatitude(), position.getLongitude());
			int y = view.getRotatedMapYForPoint(position.getLatitude(), position.getLongitude());
			//the width of an image is 40 px, the height is 60 px -> radius = 20, 
			//the position of a parking point relatively to the icon is at the center of the bottom line of the image 
			if (Math.abs(x - ex) <= radius && ((y - ey) <= radius*3) && ((y - ey) >= 0)) {
				parkingPosition.add(parkingPoint);
			}
		}
	}
	
	/**
	 * @return the control to be added on a MapInfoLayer 
	 * that shows a distance between 
	 * the current position on the map 
	 * and the location of the parked car
	 */
	private TextInfoControl createParkingPlaceInfoControl() {
		parkingPlaceControl = new TextInfoControl(map, 0, paintText, paintSubText) {
			private float[] calculations = new float[1];
			private int cachedMeters = 0;			
			
			@Override
			public boolean updateInfo() {
				if( parkingPoint != null) {
					int d = 0;
					if (map.getRoutingHelper().isRouterEnabled()) {
						d = map.getRoutingHelper().getLeftDistance();
					}
					if (d == 0) {
						Location.distanceBetween(view.getLatitude(), view.getLongitude(), parkingPoint.getLatitude(), parkingPoint.getLongitude(), calculations);
						d = (int) calculations[0];
					}
					if (distChanged(cachedMeters, d)) {
						cachedMeters = d;
						if (cachedMeters <= 20) {
							cachedMeters = 0;
							setText(null, null);
						} else {
							String ds = OsmAndFormatter.getFormattedDistance(cachedMeters, map);
							int ls = ds.lastIndexOf(' ');
							if (ls == -1) {
								setText(ds, null);
							} else {
								setText(ds.substring(0, ls), ds.substring(ls + 1));
							}
						}
						return true;
					}
				} else if (cachedMeters != 0) {
					cachedMeters = 0;
					setText(null, null);
					return true;
				}
				return false;
			}		
			
			/**
			 * Utility method.
			 * @param oldDist
			 * @param dist
			 * @return
			 */
			private boolean distChanged(int oldDist, int dist){
				if(oldDist != 0 && oldDist - dist < 100 && Math.abs(((float) dist - oldDist)/oldDist) < 0.01){
					return false;
				}
				return true;
			}
		};
		parkingPlaceControl.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AnimateDraggingMapThread thread = view.getAnimatedDraggingThread();
				LatLon parkingPoint = view.getSettings().getParkingPosition();
				if (parkingPoint != null) {
					float fZoom = view.getFloatZoom() < 15 ? 15 : view.getFloatZoom();
					thread.startMoving(parkingPoint.getLatitude(), parkingPoint.getLongitude(), fZoom, true);
				}
			}
		});
		parkingPlaceControl.setText(null, null);
		parkingPlaceControl.setImageDrawable(view.getResources().getDrawable(R.drawable.poi_parking_pos_info));
		return parkingPlaceControl;
	}
	
}
