package net.osmand.plus.measurementtool;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.view.MotionEvent;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.MapUtils;

import java.util.LinkedList;
import java.util.List;

import gnu.trove.list.array.TIntArrayList;

public class MeasurementToolLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider {

	private OsmandMapTileView view;
	private boolean inMeasurementMode;
	private final LinkedList<WptPt> measurementPoints = new LinkedList<>();
	private Bitmap centerIconDay;
	private Bitmap centerIconNight;
	private Bitmap pointIcon;
	private Paint bitmapPaint;
	private final RenderingLineAttributes lineAttrs = new RenderingLineAttributes("measureDistanceLine");
	private final Path path = new Path();
	private int marginX;
	private int marginY;
	private final TIntArrayList tx = new TIntArrayList();
	private final TIntArrayList ty = new TIntArrayList();
	private OnSingleTapListener singleTapListener;

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;

		centerIconDay = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_ruler_center_day);
		centerIconNight = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_ruler_center_night);
		pointIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_pedestrian_location);

		bitmapPaint = new Paint();
		bitmapPaint.setAntiAlias(true);
		bitmapPaint.setDither(true);
		bitmapPaint.setFilterBitmap(true);

		marginY = pointIcon.getHeight() / 2;
		marginX = pointIcon.getWidth() / 2;
	}

	void setOnSingleTapListener(OnSingleTapListener listener) {
		this.singleTapListener = listener;
	}

	public boolean isInMeasurementMode() {
		return inMeasurementMode;
	}

	void setInMeasurementMode(boolean inMeasurementMode) {
		this.inMeasurementMode = inMeasurementMode;
	}

	public int getPointsCount() {
		return measurementPoints.size();
	}

	LinkedList<WptPt> getMeasurementPoints() {
		return measurementPoints;
	}

	String getDistanceSt() {
		float dist = 0;
		if (measurementPoints.size() > 0) {
			for (int i = 1; i < measurementPoints.size(); i++) {
				dist += MapUtils.getDistance(measurementPoints.get(i - 1).lat, measurementPoints.get(i - 1).lon,
						measurementPoints.get(i).lat, measurementPoints.get(i).lon);
			}
		}
		return OsmAndFormatter.getFormattedDistance(dist, view.getApplication());
	}

	void clearPoints() {
		measurementPoints.clear();
		view.refreshMap();
	}

	@Override
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		if (inMeasurementMode && singleTapListener != null) {
			singleTapListener.onSingleTap();
		}
		return false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event, RotatedTileBox tileBox) {
		return super.onTouchEvent(event, tileBox);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tb, DrawSettings settings) {
		if (inMeasurementMode) {
			lineAttrs.updatePaints(view, settings, tb);
			drawCenterIcon(canvas, tb, tb.getCenterPixelPoint(), settings.isNightMode());

			if (measurementPoints.size() > 0) {
				path.reset();
				tx.reset();
				ty.reset();
				for (int i = 0; i < measurementPoints.size(); i++) {
					WptPt pt = measurementPoints.get(i);
					int locX = tb.getPixXFromLonNoRot(pt.lon);
					int locY = tb.getPixYFromLatNoRot(pt.lat);
					if (i == 0) {
						path.moveTo(locX, locY);
					} else {
						path.lineTo(locX, locY);
					}
					tx.add(locX);
					ty.add(locY);
				}
				path.lineTo(tb.getCenterPixelX(), tb.getCenterPixelY());
				tx.add(tb.getCenterPixelX());
				ty.add(tb.getCenterPixelY());
				calculatePath(tb, tx, ty, path);
				canvas.drawPath(path, lineAttrs.paint);
				for (WptPt pt : measurementPoints) {
					if (tb.containsLatLon(pt.lat, pt.lon)) {
						int locX = tb.getPixXFromLonNoRot(pt.lon);
						int locY = tb.getPixYFromLatNoRot(pt.lat);
						canvas.drawBitmap(pointIcon, locX - marginX, locY - marginY, bitmapPaint);
					}
				}
			}
		}
	}

	private void drawCenterIcon(Canvas canvas, RotatedTileBox tb, QuadPoint center, boolean nightMode) {
		canvas.rotate(-tb.getRotate(), center.x, center.y);
		if (nightMode) {
			canvas.drawBitmap(centerIconNight, center.x - centerIconNight.getWidth() / 2,
					center.y - centerIconNight.getHeight() / 2, bitmapPaint);
		} else {
			canvas.drawBitmap(centerIconDay, center.x - centerIconDay.getWidth() / 2,
					center.y - centerIconDay.getHeight() / 2, bitmapPaint);
		}
		canvas.rotate(tb.getRotate(), center.x, center.y);
	}

	public void addPoint(int position, WptPt point) {
		measurementPoints.add(position, point);
		view.refreshMap();
	}

	public WptPt addPoint(int position) {
		RotatedTileBox tb = view.getCurrentRotatedTileBox();
		LatLon l = tb.getLatLonFromPixel(tb.getCenterPixelX(), tb.getCenterPixelY());
		WptPt pt = new WptPt();
		pt.lat = l.getLatitude();
		pt.lon = l.getLongitude();
		if (measurementPoints.size() > 0) {
			if (!measurementPoints.get(measurementPoints.size() - 1).equals(pt)) {
				measurementPoints.add(position, pt);
				view.refreshMap();
				return pt;
			}
		} else {
			measurementPoints.add(position, pt);
			view.refreshMap();
			return pt;
		}
		return null;
	}

	public WptPt removePoint(int position) {
		WptPt res = measurementPoints.remove(position);
		view.refreshMap();
		return res;
	}

	public void moveMapToPoint(int pos) {
		if (measurementPoints.size() > 0) {
			if (pos >= measurementPoints.size()) {
				pos = measurementPoints.size() - 1;
			} else if (pos < 0) {
				pos = 0;
			}
			WptPt pt = measurementPoints.get(pos);
			view.getAnimatedDraggingThread().startMoving(pt.getLatitude(), pt.getLongitude(), view.getZoom(), true);
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
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o) {

	}

	@Override
	public LatLon getObjectLocation(Object o) {
		return null;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		return null;
	}

	@Override
	public boolean disableSingleTap() {
		return isInMeasurementMode();
	}

	@Override
	public boolean disableLongPressOnMap() {
		return isInMeasurementMode();
	}

	@Override
	public boolean isObjectClickable(Object o) {
		return !isInMeasurementMode();
	}

	interface OnSingleTapListener {
		void onSingleTap();
	}
}
