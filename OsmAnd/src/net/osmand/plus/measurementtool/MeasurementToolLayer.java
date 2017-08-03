package net.osmand.plus.measurementtool;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.MapUtils;

import java.util.LinkedList;

import gnu.trove.list.array.TIntArrayList;

public class MeasurementToolLayer extends OsmandMapLayer {

	private OsmandMapTileView view;
	private boolean inMeasurementMode;
	private LinkedList<WptPt> measurementPoints = new LinkedList<>();
	private LinkedList<WptPt> cacheMeasurementPoints;
	private Bitmap centerIconDay;
	private Bitmap centerIconNight;
	private Bitmap pointIcon;
	private Paint bitmapPaint;
	private RenderingLineAttributes lineAttrs = new RenderingLineAttributes("rulerLine");
	private Path path = new Path();
	private int marginX;
	private int marginY;
	private TIntArrayList tx = new TIntArrayList();
	private TIntArrayList ty = new TIntArrayList();

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

	public boolean isInMeasurementMode() {
		return inMeasurementMode;
	}

	void setInMeasurementMode(boolean inMeasurementMode) {
		this.inMeasurementMode = inMeasurementMode;
	}

	int getPointsCount() {
		return measurementPoints.size();
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
		cacheMeasurementPoints.clear();
		view.refreshMap();
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

					if (tb.containsLatLon(pt.lat, pt.lon)) {
						canvas.drawBitmap(pointIcon, locX - marginX, locY - marginY, bitmapPaint);
					}
				}
				path.lineTo(tb.getCenterPixelX(), tb.getCenterPixelY());
				tx.add(tb.getCenterPixelX());
				ty.add(tb.getCenterPixelY());
				calculatePath(tb, tx, ty, path);
				canvas.drawPath(path, lineAttrs.paint);
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

	void addPointOnClick() {
		RotatedTileBox tb = view.getCurrentRotatedTileBox();
		LatLon l = tb.getLatLonFromPixel(tb.getCenterPixelX(), tb.getCenterPixelY());
		WptPt pt = new WptPt();
		pt.lat = l.getLatitude();
		pt.lon = l.getLongitude();
		if (measurementPoints.size() > 0) {
			if (!measurementPoints.get(measurementPoints.size() - 1).equals(pt)) {
				measurementPoints.add(pt);
			}
		} else {
			measurementPoints.add(pt);
		}
		cacheMeasurementPoints = new LinkedList<>(measurementPoints);
		view.refreshMap();
	}

	boolean undoPointOnClick() {
		measurementPoints.remove(measurementPoints.size() - 1);
		WptPt pt = measurementPoints.get(measurementPoints.size() - 1);
		view.getAnimatedDraggingThread().startMoving(pt.getLatitude(), pt.getLongitude(), view.getZoom(), true);
		return measurementPoints.size() > 0;
	}

	boolean redoPointOnClick() {
		WptPt pt = cacheMeasurementPoints.get(measurementPoints.size());
		measurementPoints.add(pt);
		view.getAnimatedDraggingThread().startMoving(pt.getLatitude(), pt.getLongitude(), view.getZoom(), true);
		return cacheMeasurementPoints.size() > measurementPoints.size();
	}

	@Override
	public void destroyLayer() {

	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}
}
