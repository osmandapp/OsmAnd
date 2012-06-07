package net.osmand.plus.parkingpoint;

import java.util.List;

import net.osmand.OsmAndFormatter;
import net.osmand.access.AccessibleToast;
import net.osmand.data.TransportStop;
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
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

public class ParkingPositionLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider{

	private Paint bitmapPaint;

	protected LatLon parkingPoint = null;

	private DisplayMetrics dm;
	private Bitmap parkingPosition;

	
	private final MapActivity map;
	private OsmandMapTileView view;
	
	private Paint paintText;
	private Paint paintSubText;

	public ParkingPositionLayer(MapActivity map){
		this.map = map;
	}
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
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
		parkingPosition = BitmapFactory.decodeResource(view.getResources(), R.drawable.poi_parking_pos_no_limit);
//		TODO		
		MapInfoLayer mapInfoLayer = map.getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null)
			mapInfoLayer.addRightStack(createParkingPlaceInfoControl());
	}

	@Override
	public void onDraw(Canvas canvas, RectF latLonBounds, RectF tilesRect,
			DrawSettings nightMode) {
		if (parkingPoint == null) {
			return;
		}
		double latitude = parkingPoint.getLatitude();
		double longitude = parkingPoint.getLongitude();
		if (view.isPointOnTheRotatedMap(latitude, longitude)) {
			int marginX = parkingPosition.getWidth() / 3;
			int marginY = 2 * parkingPosition.getHeight() / 3;
			int locationX = view.getMapXForPoint(longitude);
			int locationY = view.getMapYForPoint(latitude);
			canvas.rotate(-view.getRotate(), locationX, locationY);
			canvas.drawBitmap(parkingPosition, locationX - marginX, locationY - marginY, bitmapPaint);
		}
	}

	public LatLon getParkingPoint() {
		return parkingPoint;
	}

	public void setParkingPoint(LatLon parkingPosition) {
		this.parkingPoint = parkingPosition;
		view.refreshMap();
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
		return true;
	}

	@Override
	public boolean onSingleTap(PointF point) {
		if(isParkingPointVisible()){
			StringBuilder res = new StringBuilder();
			res.append("The location of your parked car"); //TODO externalize!
			AccessibleToast.makeText(view.getContext(), res.toString(), Toast.LENGTH_LONG).show();
			return true;
		}
		return false;
	}

	private boolean isParkingPointVisible() {
		float[] calculations = new float[1];
		if (parkingPoint != null) {
			OsmandSettings settings = ((OsmandApplication) map.getApplication()).getSettings();
			Location.distanceBetween(view.getLatitude(), view.getLongitude(),settings.getParkingPosition().getLatitude(),settings.getParkingPosition().getLongitude() , calculations);		
			//		TODO tune the magic number 5
			if (calculations[0] < 5)
				return true;
		}
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, List<Object> o) {
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		LatLon parkingPos = ((OsmandApplication)map.getApplication()).getSettings().getParkingPosition();
		if(parkingPos != null){
			return parkingPos;
		}
		return null;
	}

	@Override
	public String getObjectDescription(Object o) {
		return map.getString(R.string.osmand_parking_position_description);
	}

	@Override
	public String getObjectName(Object o) {
		return map.getString(R.string.osmand_parking_position_name);
	}
	
	
//	TODO	
	private TextInfoControl createParkingPlaceInfoControl() {
		TextInfoControl parkingPlaceControl = new TextInfoControl(map, 0, paintText, paintSubText) {
			private float[] calculations = new float[1];
			private int cachedMeters = 0;
			
			
			@Override
			public boolean updateInfo() {
				LatLon parkingPoint = map.getMapLayers().getParkingPositionLayer().getParkingPoint();
				if (parkingPoint != null) {
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
		};
		parkingPlaceControl.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AnimateDraggingMapThread thread = view.getAnimatedDraggingThread();
				LatLon parkingPoint = view.getSettings().getParkingPosition();
				if (parkingPoint != null) {
					int fZoom = view.getZoom() < 15 ? 15 : view.getZoom();
					thread.startMoving(parkingPoint.getLatitude(), parkingPoint.getLongitude(), fZoom, true);
				}
			}
		});
		parkingPlaceControl.setText(null, null);
		parkingPlaceControl.setImageDrawable(view.getResources().getDrawable(R.drawable.poi_parking_pos_info));
		return parkingPlaceControl;
	}

	public boolean distChanged(int oldDist, int dist){
		if(oldDist != 0 && oldDist - dist < 100 && Math.abs(((float) dist - oldDist)/oldDist) < 0.01){
			return false;
		}
		return true;
	}
	
}
