package net.osmand.plus.views;

import java.util.List;

import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.ContextMenuLayer.IContextMenuProvider;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.PointF;

public class PointNavigationLayer extends OsmandMapLayer implements IContextMenuProvider {
	protected final static int DIST_TO_SHOW = 80;

	private Paint point;
	private Paint bitmapPaint;
	
	private OsmandMapTileView view;
	private float[] calculations = new float[2];
	
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
		targetPoint = BitmapFactory.decodeResource(view.getResources(), R.drawable.target_point);
		intermediatePoint = BitmapFactory.decodeResource(view.getResources(), R.drawable.intermediate_point);
		arrowToDestination = BitmapFactory.decodeResource(view.getResources(), R.drawable.arrow_to_destination);

		
	}
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		initUI();
	}


	
	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tb, DrawSettings nightMode) {
		int index = 0;
		
		TargetPointsHelper targetPoints = map.getMyApplication().getTargetPointsHelper();
		for (TargetPoint ip : targetPoints.getIntermediatePoints()) {
			index ++;
			if (isLocationVisible(tb, ip)) {
				int marginX = intermediatePoint.getWidth() / 3;
				int marginY = intermediatePoint.getHeight();
				int locationX = tb.getPixXFromLonNoRot(ip.getLongitude());
				int locationY = tb.getPixYFromLatNoRot(ip.getLatitude());
				canvas.rotate(-tb.getRotate(), locationX, locationY);
				canvas.drawBitmap(intermediatePoint, locationX - marginX, locationY - marginY, bitmapPaint);
				canvas.drawText(index + "", locationX + marginX, locationY - 2 * marginY / 3, textPaint);
				canvas.rotate(tb.getRotate(), locationX, locationY);
			}
		}
		TargetPoint pointToNavigate = targetPoints.getPointToNavigate();
		if (isLocationVisible(tb, pointToNavigate)) {
			int marginX = targetPoint.getWidth() / 3;
			int marginY = targetPoint.getHeight();
			int locationX = tb.getPixXFromLonNoRot(pointToNavigate.getLongitude());
			int locationY = tb.getPixYFromLatNoRot(pointToNavigate.getLatitude());
			canvas.rotate(-tb.getRotate(), locationX, locationY);
			canvas.drawBitmap(targetPoint, locationX - marginX, locationY - marginY, bitmapPaint);
		} else if (pointToNavigate != null) {
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
	public boolean onLongPressEvent(PointF point, RotatedTileBox tileBox) {
		return false;
	}

	@Override
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		return false;
	}
	

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o) {
		TargetPointsHelper tg = map.getMyApplication().getTargetPointsHelper();
		List<TargetPoint> intermediatePoints = tg.getIntermediatePointsWithTarget();
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
	
	private boolean calculateBelongs(int ex, int ey, int objx, int objy, int radius) {
		return Math.abs(objx - ex) <= radius && (ey - objy) <= radius && (objy - ey) <= 2.5 * radius ;
	}
	
	public int getRadiusPoi(RotatedTileBox tb){
		int r = 0;
		final float zoom = tb.getZoom() + tb.getZoomScale();
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
			return ((TargetPoint) o).getVisibleName(view.getContext());
		}
		return null;
	}

	@Override
	public String getObjectName(Object o) {
		if (o instanceof TargetPoint) {
			return ((TargetPoint) o).getVisibleName(view.getContext());
		}
		return null;
	}
	
	@Override
	public void populateObjectContextMenu(Object o, ContextMenuAdapter adapter) {
		if(o instanceof TargetPoint) {
			final TargetPoint a = (TargetPoint) o;
			OnContextMenuClick listener = new ContextMenuAdapter.OnContextMenuClick() {
				@Override
				public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
					if (itemId == R.string.delete_target_point) {
						TargetPointsHelper targetPointsHelper = map.getMyApplication().getTargetPointsHelper();
						if(a.intermediate) {
							targetPointsHelper.removeWayPoint(true, a.index);
						} else {
							targetPointsHelper.removeWayPoint(true, -1);
						}
					}
					map.getMapLayers().getContextMenuLayer().setLocation(null, "");
				}
			};
			
			
			adapter.item(R.string.delete_target_point)
			.icons( R.drawable.ic_action_remove_dark, R.drawable.ic_action_remove_light).listen(listener).reg();
			
		}
	}

}
