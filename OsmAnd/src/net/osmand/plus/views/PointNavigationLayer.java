package net.osmand.plus.views;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.PointF;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.ContextMenuLayer.IContextMenuProvider;

import java.util.Iterator;
import java.util.List;

public class PointNavigationLayer extends OsmandMapLayer implements IContextMenuProvider {
	protected final static int DIST_TO_SHOW = 80;

	private Paint point;
	private Paint bitmapPaint;
	
	private OsmandMapTileView view;
	private float[] calculations = new float[2];

	private Bitmap startPoint;
	private Bitmap targetPoint;
	private Bitmap intermediatePoint;
	private Bitmap arrowToDestination;

	private Paint textPaint;

	private final MapActivity map;
	
	public PointNavigationLayer(MapActivity map) {
		this.map = map;
	}
	

	private void initUI() {
		
		point = new Paint();
		point.setColor(view.getResources().getColor(R.color.nav_point));
		point.setAntiAlias(true);
		point.setStyle(Style.FILL);

		bitmapPaint = new Paint();
		bitmapPaint.setDither(true);
		bitmapPaint.setAntiAlias(true);
		bitmapPaint.setFilterBitmap(true);
		textPaint = new Paint();
		float sp = Resources.getSystem().getDisplayMetrics().scaledDensity;
		textPaint.setTextSize(sp * 18);
		textPaint.setTextAlign(Align.CENTER);
		textPaint.setAntiAlias(true);
		startPoint = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_start_point);
		targetPoint = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_target_point);
		intermediatePoint = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_intermediate_point);
		arrowToDestination = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_arrow_to_destination);

		
	}
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		initUI();
	}


	
	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tb, DrawSettings nightMode) {
		if (tb.getZoom() < 3) {
			return;
		}

		TargetPointsHelper targetPoints = map.getMyApplication().getTargetPointsHelper();
		TargetPoint pointToStart = targetPoints.getPointToStart();
		if (pointToStart != null) {
			if (isLocationVisible(tb, pointToStart)) {
				int marginX = startPoint.getWidth() / 6;
				int marginY = startPoint.getHeight();
				int locationX = tb.getPixXFromLonNoRot(pointToStart.getLongitude());
				int locationY = tb.getPixYFromLatNoRot(pointToStart.getLatitude());
				canvas.rotate(-tb.getRotate(), locationX, locationY);
				canvas.drawBitmap(startPoint, locationX - marginX, locationY - marginY, bitmapPaint);
			}
		}

		int index = 0;
		for (TargetPoint ip : targetPoints.getIntermediatePoints()) {
			index ++;
			if (isLocationVisible(tb, ip)) {
				int marginX = intermediatePoint.getWidth() / 6;
				int marginY = intermediatePoint.getHeight();
				int locationX = tb.getPixXFromLonNoRot(ip.getLongitude());
				int locationY = tb.getPixYFromLatNoRot(ip.getLatitude());
				canvas.rotate(-tb.getRotate(), locationX, locationY);
				canvas.drawBitmap(intermediatePoint, locationX - marginX, locationY - marginY, bitmapPaint);
				marginX = intermediatePoint.getWidth() / 3;
				canvas.drawText(index + "", locationX + marginX, locationY - 3 * marginY / 5, textPaint);
				canvas.rotate(tb.getRotate(), locationX, locationY);
			}
		}

		TargetPoint pointToNavigate = targetPoints.getPointToNavigate();
		if (isLocationVisible(tb, pointToNavigate)) {
			int marginX = targetPoint.getWidth() / 6;
			int marginY = targetPoint.getHeight();
			int locationX = tb.getPixXFromLonNoRot(pointToNavigate.getLongitude());
			int locationY = tb.getPixYFromLatNoRot(pointToNavigate.getLatitude());
			canvas.rotate(-tb.getRotate(), locationX, locationY);
			canvas.drawBitmap(targetPoint, locationX - marginX, locationY - marginY, bitmapPaint);
		} 

		Iterator<TargetPoint> it = targetPoints.getIntermediatePoints().iterator();
		if(it.hasNext()) {
			pointToNavigate = it.next();
		}
		if (pointToNavigate != null && !isLocationVisible(tb, pointToNavigate)) {
			boolean show = !view.getApplication().getRoutingHelper().isRouteCalculated();
			if(view.getSettings().SHOW_DESTINATION_ARROW.isSet()) {
				show = view.getSettings().SHOW_DESTINATION_ARROW.get();
			}
			if (show) {
				net.osmand.Location.distanceBetween(view.getLatitude(), view.getLongitude(),
						pointToNavigate.getLatitude(), pointToNavigate.getLongitude(), calculations);
				float bearing = calculations[1] - 90;
				float radiusBearing = DIST_TO_SHOW * tb.getDensity();
				final QuadPoint cp = tb.getCenterPixelPoint();
				canvas.rotate(bearing, cp.x, cp.y);
				canvas.translate(-24 * tb.getDensity() + radiusBearing, -22 * tb.getDensity());
				canvas.drawBitmap(arrowToDestination, cp.x, cp.y, bitmapPaint);
			}
		}
		
	}

	public boolean isLocationVisible(RotatedTileBox tb, TargetPoint p){
		if(p == null || tb == null){
			return false;
		}
		return tb.containsLatLon(p.getLatitude(), p.getLongitude());
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
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o) {
		if (tileBox.getZoom() >= 3) {
			TargetPointsHelper tg = map.getMyApplication().getTargetPointsHelper();
			List<TargetPoint> intermediatePoints = tg.getAllPoints();
			int r = getRadiusPoi(tileBox);
			for (int i = 0; i < intermediatePoints.size(); i++) {
				TargetPoint tp = intermediatePoints.get(i);
				LatLon latLon = tp.point;
				if (latLon != null) {
					int ex = (int) point.x;
					int ey = (int) point.y;
					int x = (int) tileBox.getPixXFromLatLon(latLon.getLatitude(), latLon.getLongitude());
					int y = (int) tileBox.getPixYFromLatLon(latLon.getLatitude(), latLon.getLongitude());
					if (calculateBelongs(ex, ey, x, y, r)) {
						o.add(tp);
					}
				}
			}
		}
	}
	
	private boolean calculateBelongs(int ex, int ey, int objx, int objy, int radius) {
		return Math.abs(objx - ex) <= radius && (ey - objy) <= radius && (objy - ey) <= 2.5 * radius ;
	}
	
	public int getRadiusPoi(RotatedTileBox tb){
		int r = 0;
		final double zoom = tb.getZoom();
		if(zoom <= 15){
			r = 10;
		} else if(zoom <= 16){
			r = 14;
		} else if(zoom <= 17){
			r = 16;
		} else {
			r = 18;
		}
		return (int) (r * tb.getDensity());
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof TargetPoint) {
			return ((TargetPoint) o).point;
		}
		return null;
	}

	@Override
	public String getObjectDescription(Object o) {
		if (o instanceof TargetPoint) {
			return ((TargetPoint) o).getPointDescription(view.getContext()).getFullPlainName(view.getContext());
		}
		return null;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof TargetPoint) {
			return ((TargetPoint) o).getPointDescription(view.getContext());
		}
		return null;
	}
}
