package net.osmand.plus.views.layers;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.PointF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.jni.MapMarker;
import net.osmand.core.jni.MapMarkerBuilder;
import net.osmand.core.jni.MapMarkersCollection;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QListMapMarker;
import net.osmand.core.jni.TextRasterizer;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.ContextMenuLayer.IMoveObjectProvider;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.util.MapUtils;

import java.util.List;

public class PointNavigationLayer extends OsmandMapLayer implements
		IContextMenuProvider, IMoveObjectProvider {

	private Paint mBitmapPaint;

	private OsmandMapTileView mView;
	private boolean carView;
	private float textScale = 1f;
	private double pointSizePx;

	private Bitmap mStartPoint;
	private Bitmap mTargetPoint;
	private Bitmap mIntermediatePoint;

	private Paint mTextPaint;

	private ContextMenuLayer contextMenuLayer;

	//OpenGL
	private MapMarkersCollection collection;
	private TextRasterizer.Style captionStyle;
	private List<TargetPoint> renderedPoints;
	private boolean nightMode = false;
	private enum MarkerType {
		START,
		INTERMEDIATE,
		DESTINATION
	};

	public PointNavigationLayer(@NonNull Context context, int order) {
		super(context);
		baseOrder = order;
	}

	private void initUI() {
		mBitmapPaint = new Paint();
		mBitmapPaint.setDither(true);
		mBitmapPaint.setAntiAlias(true);
		mBitmapPaint.setFilterBitmap(true);

		float sp = Resources.getSystem().getDisplayMetrics().scaledDensity;
		mTextPaint = new Paint();
		mTextPaint.setTextSize(sp * 18);
		mTextPaint.setTextAlign(Align.CENTER);
		mTextPaint.setAntiAlias(true);

		updateBitmaps(true);
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		this.mView = view;
		initUI();

		contextMenuLayer = view.getLayerByClass(ContextMenuLayer.class);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tb, DrawSettings nightMode) {
		if (tb.getZoom() < 3) {
			return;
		}
		updateBitmaps(false);

		if (getMapView().hasMapRenderer()) {
			//OpenGL draw in onPrepareBufferImage
			return;
		}

		OsmandApplication app = getApplication();
		TargetPointsHelper targetPoints = app.getTargetPointsHelper();
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

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (getMapView().hasMapRenderer()) {
			//OpenGL
			if (nightMode != settings.isNightMode()) {
				//switch to day/night mode
				captionStyle = null;
				removeMarkers();
				nightMode = settings.isNightMode();
			}

			OsmandApplication app = getApplication();
			TargetPointsHelper targetPoints = app.getTargetPointsHelper();
			List<TargetPoint> allPoints = targetPoints.getAllPoints();


			if (renderedPoints != null) {
				if (renderedPoints.size() != allPoints.size()) {
					removeMarkers();
				}
				if (allPoints.size() > 0 && allPoints.size() == renderedPoints.size()) {
					for (int i = 0; i < allPoints.size(); i++) {
						TargetPoint r = renderedPoints.get(i);
						TargetPoint a = renderedPoints.get(i);
						if (!a.equals(r)) {
							removeMarkers();
							break;
						}
					}
				}
			}

			if (collection == null) {
				collection = new MapMarkersCollection();
				TargetPoint pointToStart = targetPoints.getPointToStart();
				if (pointToStart != null) {
					int x = MapUtils.get31TileNumberX(pointToStart.getLongitude());
					int y = MapUtils.get31TileNumberY(pointToStart.getLatitude());
					drawMarker(mStartPoint, new PointI(x, y), null);
				}

				List<TargetPoint> intermediatePoints = targetPoints.getIntermediatePoints();
				if (intermediatePoints.size() > 0) {
					int index = 0;
					for (TargetPoint ip : intermediatePoints) {
						index++;
						int x = MapUtils.get31TileNumberX(ip.getLongitude());
						int y = MapUtils.get31TileNumberY(ip.getLatitude());
						drawMarker(mIntermediatePoint, new PointI(x, y), String.valueOf(index));
					}
				}

				TargetPoint pointToNavigate = targetPoints.getPointToNavigate();
				if (pointToNavigate != null) {
					int x = MapUtils.get31TileNumberX(pointToNavigate.getLongitude());
					int y = MapUtils.get31TileNumberY(pointToNavigate.getLatitude());
					drawMarker(mTargetPoint, new PointI(x, y), null);
				}

				renderedPoints = allPoints;
				if (getMapView().getMapRenderer() != null) {
					getMapView().getMapRenderer().addSymbolsProvider(collection);
				}
			}
		}
	}

	private void updateBitmaps(boolean forceUpdate) {
		OsmandApplication app = getApplication();
		float textScale = getTextScale();
		boolean carView = app.getOsmandMap().getMapView().isCarView();
		if (this.textScale != textScale || this.carView != carView || forceUpdate) {
			this.textScale = textScale;
			this.carView = carView;
			recreateBitmaps();
			pointSizePx = Math.sqrt(mTargetPoint.getWidth() * mTargetPoint.getWidth()
					+ mTargetPoint.getHeight() * mTargetPoint.getHeight());
		}
	}

	private void recreateBitmaps() {
		mStartPoint = getScaledBitmap(R.drawable.map_start_point);
		mTargetPoint = getScaledBitmap(R.drawable.map_target_point);
		mIntermediatePoint = getScaledBitmap(R.drawable.map_intermediate_point);
		removeMarkers();
	}

	@Nullable
	@Override
	protected Bitmap getScaledBitmap(int drawableId) {
		return getScaledBitmap(drawableId, textScale);
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
		double tx = tb.getPixXFromLatLon(p.getLatitude(), p.getLongitude());
		double ty = tb.getPixYFromLatLon(p.getLatitude(), p.getLongitude());
		return tx >= -pointSizePx && tx <= tb.getPixWidth() + pointSizePx && ty >= -pointSizePx && ty <= tb.getPixHeight() + pointSizePx;
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
	public boolean showMenuAction(@Nullable Object o) {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o, boolean unknownLocation) {
		if (tileBox.getZoom() >= 3) {
			TargetPointsHelper tg = getApplication().getTargetPointsHelper();
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
		if (o instanceof TargetPoint) {
			TargetPointsHelper targetPointsHelper = getApplication().getTargetPointsHelper();
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
			TargetPointsHelper targetPointsHelper = getApplication().getTargetPointsHelper();
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

	/** OpenGL */
	private void drawMarker(Bitmap bitmap, PointI position, @Nullable String caption) {
		if (getMapView().getMapRenderer() == null) {
			return;
		}

		MapMarkerBuilder mapMarkerBuilder = new MapMarkerBuilder();
		mapMarkerBuilder
				.setPosition(position)
				.setPinIconVerticalAlignment(MapMarker.PinIconVerticalAlignment.Top)
				.setPinIconHorisontalAlignment(MapMarker.PinIconHorisontalAlignment.Right)
				.setIsHidden(false)
				.setBaseOrder(baseOrder)
				.setPinIcon(NativeUtilities.createSkImageFromBitmap(bitmap))
				.setPinIconVerticalAlignment(MapMarker.PinIconVerticalAlignment.Top)
				.setPinIconHorisontalAlignment(MapMarker.PinIconHorisontalAlignment.Right);

		if (caption != null) {
			initCaptionStyle();
			mapMarkerBuilder
					.setCaptionStyle(captionStyle)
					.setCaptionTopSpace(- mIntermediatePoint.getHeight() * 0.7 - captionStyle.getSize() / 2)
					.setCaption(caption);
		}
		mapMarkerBuilder.buildAndAddToCollection(collection);
	}

	/** OpenGL */
	private void removeMarkers() {
		if (getMapView().getMapRenderer() != null && collection != null) {
			getMapView().getMapRenderer().removeSymbolsProvider(collection);
			collection = null;
		}
	}

	/** OpenGL */
	private void initCaptionStyle() {
		if (captionStyle != null || !getMapView().hasMapRenderer()) {
			return;
		}
		int nightColor = getContext().getResources().getColor(R.color.widgettext_night, null);
		int dayColor = getContext().getResources().getColor(R.color.widgettext_day, null);
		captionStyle = new TextRasterizer.Style();
		captionStyle.setSize(mTextPaint.getTextSize());
		captionStyle.setWrapWidth(20);
		captionStyle.setMaxLines(3);
		captionStyle.setBold(false);
		captionStyle.setItalic(false);
		captionStyle.setColor(NativeUtilities.createColorARGB(nightMode ? nightColor : dayColor));
	}
}
