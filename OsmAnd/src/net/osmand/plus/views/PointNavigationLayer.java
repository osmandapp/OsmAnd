package net.osmand.plus.views;

import java.util.List;

import net.osmand.data.LatLon;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.ContextMenuLayer.IContextMenuProvider;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class PointNavigationLayer extends OsmandMapLayer implements IContextMenuProvider {
	protected final static int DIST_TO_SHOW = 80;

	private Paint point;
	private Paint bitmapPaint;
	
	private OsmandMapTileView view;
	private float[] calculations = new float[2];
	
	private DisplayMetrics dm;
	private Bitmap targetPoint;
	private Bitmap intermediatePoint;
	private Bitmap arrowToDestination;

	private Paint textPaint;

	private final MapActivity map;
	
	public PointNavigationLayer(MapActivity map) {
		this.map = map;
	}
	
	public static class TargetPoint {
		public LatLon location;
		public String name;
		public boolean intermediate;
		public int index;
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
		dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);
		initUI();
	}


	
	@Override
	public void onDraw(Canvas canvas, RectF latLonBounds, RectF tilesRect, DrawSettings nightMode) {
		int index = 0;
		
		TargetPointsHelper targetPoints = map.getMyApplication().getTargetPointsHelper();
		for (LatLon ip : targetPoints.getIntermediatePoints()) {
			index ++;
			if (isLocationVisible(ip)) {
				int marginX = intermediatePoint.getWidth() / 3;
				int marginY = intermediatePoint.getHeight();
				int locationX = view.getMapXForPoint(ip.getLongitude());
				int locationY = view.getMapYForPoint(ip.getLatitude());
				canvas.rotate(-view.getRotate(), locationX, locationY);
				canvas.drawBitmap(intermediatePoint, locationX - marginX, locationY - marginY, bitmapPaint);
				canvas.drawText(index + "", locationX + marginX, locationY - 2 * marginY / 3, textPaint);
				canvas.rotate(view.getRotate(), locationX, locationY);
			}
		}
		LatLon pointToNavigate = targetPoints.getPointToNavigate();
		if (isLocationVisible(pointToNavigate)) {
			int marginX = targetPoint.getWidth() / 3;
			int marginY = targetPoint.getHeight();
			int locationX = view.getMapXForPoint(pointToNavigate.getLongitude());
			int locationY = view.getMapYForPoint(pointToNavigate.getLatitude());
			canvas.rotate(-view.getRotate(), locationX, locationY);
			canvas.drawBitmap(targetPoint, locationX - marginX, locationY - marginY, bitmapPaint);
		} else if (pointToNavigate != null && view.getSettings().SHOW_DESTINATION_ARROW.get()) {
			net.osmand.Location.distanceBetween(view.getLatitude(), view.getLongitude(), pointToNavigate.getLatitude(),
					pointToNavigate.getLongitude(), calculations);
			float bearing = calculations[1] - 90;
			float radiusBearing = DIST_TO_SHOW * dm.density;
			canvas.rotate(bearing, view.getCenterPointX(), view.getCenterPointY());
			canvas.translate(-24 * dm.density + radiusBearing, -22 * dm.density);
			canvas.drawBitmap(arrowToDestination, view.getCenterPointX(), view.getCenterPointY(), bitmapPaint);
		}
		
	}

	public boolean isLocationVisible(LatLon p){
		if(p == null || view == null){
			return false;
		}
		return view.isPointOnTheRotatedMap(p.getLatitude(), p.getLongitude());
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
		return false;
	}

	@Override
	public boolean onSingleTap(PointF point) {
		return false;
	}
	

	@Override
	public void collectObjectsFromPoint(PointF point, List<Object> o) {
		TargetPointsHelper tg = map.getMyApplication().getTargetPointsHelper();
		List<LatLon> intermediatePoints = tg.getIntermediatePointsWithTarget();
		List<String> names = tg.getIntermediatePointNamesWithTarget();
		int r = getRadiusPoi(view.getZoom());
		for (int i = 0; i < intermediatePoints.size(); i++) {
			LatLon latLon = intermediatePoints.get(i);
			boolean target = i == intermediatePoints.size() - 1;
			if (latLon != null) {
				int ex = (int) point.x;
				int ey = (int) point.y;
				int x = view.getRotatedMapXForPoint(latLon.getLatitude(), latLon.getLongitude());
				int y = view.getRotatedMapYForPoint(latLon.getLatitude(), latLon.getLongitude());
				if (calculateBelongs(ex, ey, x, y, r)) {
					TargetPoint tp = new TargetPoint();
					tp.location = latLon;
					tp.intermediate = !target;
					if (target) {
						tp.name = view.getContext().getString(R.string.destination_point, "")  + " : " + names.get(i);
					} else {
						tp.name = (i + 1) + ". " + view.getContext().getString(R.string.intermediate_point, "")  + " : " + names.get(i);
					}
					tp.index = i;
					o.add(tp);
				}
			}
		}
		
		
	}
	
	private boolean calculateBelongs(int ex, int ey, int objx, int objy, int radius) {
		return Math.abs(objx - ex) <= radius && (ey - objy) <= radius && (objy - ey) <= 2.5 * radius ;
	}
	
	public int getRadiusPoi(int zoom){
		int r = 0;
		if(zoom <= 15){
			r = 10;
		} else if(zoom == 16){
			r = 14;
		} else if(zoom == 17){
			r = 16;
		} else {
			r = 18;
		}
		return (int) (r * dm.density);
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof TargetPoint) {
			return ((TargetPoint) o).location;
		}
		return null;
	}

	@Override
	public String getObjectDescription(Object o) {
		if (o instanceof TargetPoint) {
			return ((TargetPoint) o).name;
		}
		return null;
	}

	@Override
	public String getObjectName(Object o) {
		if (o instanceof TargetPoint) {
			return ((TargetPoint) o).name;
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
