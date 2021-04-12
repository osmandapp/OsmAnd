package net.osmand.plus.views.layers;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.PointF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;

import java.util.List;

public class PointNavigationLayer extends OsmandMapLayer implements
		IContextMenuProvider, ContextMenuLayer.IMoveObjectProvider {
	protected final static int DIST_TO_SHOW = 80;

	private Paint mPoint;
	private Paint mBitmapPaint;

	private OsmandMapTileView mView;
	private float[] mCalculations = new float[2];

	private Bitmap mStartPoint;
	private Bitmap mTargetPoint;
	private Bitmap mIntermediatePoint;

	private Paint mTextPaint;

	private final MapActivity map;

	private ContextMenuLayer contextMenuLayer;

	public PointNavigationLayer(MapActivity map) {
		this.map = map;
	}

	private void initUI() {
		mPoint = new Paint();
		mPoint.setColor(ContextCompat.getColor(map, R.color.nav_point));
		mPoint.setAntiAlias(true);
		mPoint.setStyle(Style.FILL);

		mBitmapPaint = new Paint();
		mBitmapPaint.setDither(true);
		mBitmapPaint.setAntiAlias(true);
		mBitmapPaint.setFilterBitmap(true);
		mTextPaint = new Paint();
		float sp = Resources.getSystem().getDisplayMetrics().scaledDensity;
		mTextPaint.setTextSize(sp * 18);
		mTextPaint.setTextAlign(Align.CENTER);
		mTextPaint.setAntiAlias(true);
		mStartPoint = BitmapFactory.decodeResource(mView.getResources(), R.drawable.map_start_point);
		mTargetPoint = BitmapFactory.decodeResource(mView.getResources(), R.drawable.map_target_point);
		mIntermediatePoint = BitmapFactory.decodeResource(mView.getResources(), R.drawable.map_intermediate_point);
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.mView = view;
		initUI();

		contextMenuLayer = view.getLayerByClass(ContextMenuLayer.class);
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
				int marginX = mStartPoint.getWidth() / 6;
				int marginY = mStartPoint.getHeight();
				float locationX = getPointX(tb, pointToStart);
				float locationY = getPointY(tb, pointToStart);
				canvas.rotate(-tb.getRotate(), locationX, locationY);
				canvas.drawBitmap(mStartPoint, locationX - marginX, locationY - marginY, mBitmapPaint);
				canvas.rotate(tb.getRotate(), locationX, locationY);
			}
		}

		int index = 0;
		for (TargetPoint ip : targetPoints.getIntermediatePoints()) {
			index++;
			if (isLocationVisible(tb, ip)) {
				int marginX = mIntermediatePoint.getWidth() / 6;
				int marginY = mIntermediatePoint.getHeight();
				float locationX = getPointX(tb, ip);
				float locationY = getPointY(tb, ip);
				canvas.rotate(-tb.getRotate(), locationX, locationY);
				canvas.drawBitmap(mIntermediatePoint, locationX - marginX, locationY - marginY, mBitmapPaint);
				marginX = mIntermediatePoint.getWidth() / 3;
				canvas.drawText(index + "", locationX + marginX, locationY - 3 * marginY / 5, mTextPaint);
				canvas.rotate(tb.getRotate(), locationX, locationY);
			}
		}

		TargetPoint pointToNavigate = targetPoints.getPointToNavigate();
		if (isLocationVisible(tb, pointToNavigate)) {
			int marginX = mTargetPoint.getWidth() / 6;
			int marginY = mTargetPoint.getHeight();
			float locationX = getPointX(tb, pointToNavigate);
			float locationY = getPointY(tb, pointToNavigate);
			canvas.rotate(-tb.getRotate(), locationX, locationY);
			canvas.drawBitmap(mTargetPoint, locationX - marginX, locationY - marginY, mBitmapPaint);
			canvas.rotate(tb.getRotate(), locationX, locationY);
		}

	}

	private float getPointX(RotatedTileBox tileBox, TargetPoint point) {
		if (contextMenuLayer.getMoveableObject() != null
				&& point == contextMenuLayer.getMoveableObject()) {
			return contextMenuLayer.getMovableCenterPoint(tileBox).x;
		} else {
			return tileBox.getPixXFromLonNoRot(point.getLongitude());
		}
	}

	private float getPointY(RotatedTileBox tileBox, TargetPoint point) {
		if (contextMenuLayer.getMoveableObject() != null
				&& point == contextMenuLayer.getMoveableObject()) {
			return contextMenuLayer.getMovableCenterPoint(tileBox).y;
		} else {
			return tileBox.getPixYFromLatNoRot(point.getLatitude());
		}
	}

	public boolean isLocationVisible(RotatedTileBox tb, TargetPoint p) {
		if (contextMenuLayer.getMoveableObject() != null
				&& p == contextMenuLayer.getMoveableObject()) {
			return true;
		} else if (p == null || tb == null) {
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
	public boolean disableLongPressOnMap(PointF point, RotatedTileBox tileBox) {
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

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o, boolean unknownLocation) {
		if (tileBox.getZoom() >= 3) {
			TargetPointsHelper tg = map.getMyApplication().getTargetPointsHelper();
			List<TargetPoint> intermediatePoints = tg.getAllPoints();
			int r = getDefaultRadiusPoi(tileBox);
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
		return Math.abs(objx - ex) <= radius && (ey - objy) <= radius && (objy - ey) <= 2.5 * radius;
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof TargetPoint) {
			return ((TargetPoint) o).point;
		}
		return null;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof TargetPoint) {
			return ((TargetPoint) o).getPointDescription(mView.getContext());
		}
		return null;
	}

	@Override
	public boolean isObjectMovable(Object o) {
		if (o != null && o instanceof TargetPoint) {
			TargetPointsHelper targetPointsHelper = map.getMyApplication().getTargetPointsHelper();
			return targetPointsHelper.getAllPoints().contains(o);
		}
		return false;
	}

	@Override
	public void applyNewObjectPosition(@NonNull Object o, @NonNull LatLon position,
									   @Nullable ContextMenuLayer.ApplyMovedObjectCallback callback) {
		boolean result = false;
		TargetPoint newTargetPoint = null;
		if (o instanceof TargetPoint) {
			TargetPointsHelper targetPointsHelper = map.getMyApplication().getTargetPointsHelper();
			TargetPoint oldPoint = (TargetPoint) o;
			if (oldPoint.start) {
				targetPointsHelper.setStartPoint(position, true, null);
				newTargetPoint = targetPointsHelper.getPointToStart();
			} else if (oldPoint == targetPointsHelper.getPointToNavigate()) {
				targetPointsHelper.navigateToPoint(position, true, -1, null);
				newTargetPoint = targetPointsHelper.getPointToNavigate();
			} else if (oldPoint.intermediate) {
				List<TargetPoint> points = targetPointsHelper.getIntermediatePointsWithTarget();
				int i = points.indexOf(oldPoint);
				if (i != -1) {
					newTargetPoint = new TargetPoint(position,
							new PointDescription(PointDescription.POINT_TYPE_LOCATION, ""));
					points.set(i, newTargetPoint);
					targetPointsHelper.reorderAllTargetPoints(points, true);
				}

			}
			result = true;
		}
		if (callback != null) {
			callback.onApplyMovedObject(result, newTargetPoint == null ? o : newTargetPoint);
		}
	}
}
