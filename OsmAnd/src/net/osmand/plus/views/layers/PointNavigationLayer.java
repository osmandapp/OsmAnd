package net.osmand.plus.views.layers;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.PointF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.MapMarker;
import net.osmand.core.jni.MapMarkerBuilder;
import net.osmand.core.jni.MapMarkersCollection;
import net.osmand.core.jni.PointI;
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
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public class PointNavigationLayer extends OsmandMapLayer implements
		IContextMenuProvider, IMoveObjectProvider {

	private final TargetPointsHelper targetPoints;

	private boolean carView;
	private float textScale = 1f;
	private double pointSizePx;

	private Bitmap mStartPoint;
	private Bitmap mTargetPoint;
	private Bitmap mIntermediatePoint;

	private Paint mBitmapPaint;
	private Paint mTextPaint;

	private ContextMenuLayer contextMenuLayer;

	//OpenGL
	private TextRasterizer.Style captionStyle;
	private List<TargetPoint> renderedPoints;
	private boolean nightMode;

	public PointNavigationLayer(@NonNull Context context) {
		super(context);
		targetPoints = getApplication().getTargetPointsHelper();
	}

	private void initUI() {
		mBitmapPaint = new Paint();
		mBitmapPaint.setDither(true);
		mBitmapPaint.setAntiAlias(true);
		mBitmapPaint.setFilterBitmap(true);

		mTextPaint = new Paint();
		mTextPaint.setTextAlign(Align.CENTER);
		mTextPaint.setAntiAlias(true);

		updateTextSize();
		updateBitmaps(true);
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);

		initUI();
		contextMenuLayer = view.getLayerByClass(ContextMenuLayer.class);
	}

	@Override
	protected void updateResources() {
		super.updateResources();
		updateBitmaps(true);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tb, DrawSettings nightMode) {
		if (tb.getZoom() < 3) {
			clearMapMarkersCollections();
			return;
		}
		updateBitmaps(false);
		if (getMapView().hasMapRenderer()) {
			Object movableObject = contextMenuLayer.getMoveableObject();
			if (movableObject instanceof TargetPoint) {
				//draw movable object on canvas
				TargetPoint targetPoint = (TargetPoint) movableObject;
				if (targetPoints.getPointToStart() == targetPoint) {
					drawStartPoint(canvas, tb, targetPoint);
				} else if (targetPoints.getPointToNavigate() == targetPoint) {
					drawPointToNavigate(canvas, tb, targetPoint);
				} else if (targetPoints.getIntermediatePoints().contains(targetPoint)) {
					drawIntermediatePoint(canvas, tb, targetPoint, targetPoints.getIntermediatePoints().indexOf(targetPoint) + 1);
				}
				setMovableObject(targetPoint.getLatitude(), targetPoint.getLongitude());
			}
			if (this.movableObject != null && !contextMenuLayer.isInChangeMarkerPositionMode()) {
				cancelMovableObject();
			}
			return;
		}
		TargetPoint pointToStart = targetPoints.getPointToStart();
		if (pointToStart != null) {
			if (isLocationVisible(tb, pointToStart)) {
				drawStartPoint(canvas, tb, pointToStart);
			}
		}
		int index = 0;
		for (TargetPoint ip : targetPoints.getIntermediatePoints()) {
			index++;
			if (isLocationVisible(tb, ip)) {
				drawIntermediatePoint(canvas, tb, ip, index);
			}
		}
		TargetPoint pointToNavigate = targetPoints.getPointToNavigate();
		if (isLocationVisible(tb, pointToNavigate)) {
			drawPointToNavigate(canvas, tb, pointToNavigate);
		}
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		super.onPrepareBufferImage(canvas, tileBox, settings);
		MapRendererView mapRenderer = getMapView().getMapRenderer();
		if (mapRenderer != null) {
			//OpenGL
			if (nightMode != settings.isNightMode() || mapActivityInvalidated) {
				//switch to day/night mode
				captionStyle = null;
				clearMapMarkersCollections();
				nightMode = settings.isNightMode();
			}
			TargetPoint pointToStart = targetPoints.getPointToStart();
			TargetPoint pointToNavigate = targetPoints.getPointToNavigate();
			List<TargetPoint> intermediatePoints = targetPoints.getIntermediatePoints();
			List<TargetPoint> allPoints = new ArrayList<>();
			if (pointToStart != null) {
				allPoints.add(pointToStart);
			}
			if (!Algorithms.isEmpty(intermediatePoints)) {
				allPoints.addAll(intermediatePoints);
			}
			if (pointToNavigate != null) {
				allPoints.add(pointToNavigate);
			}
			List<TargetPoint> renderedPoints = this.renderedPoints;
			if (renderedPoints != null) {
				if (allPoints.isEmpty() || renderedPoints.size() != allPoints.size()) {
					clearMapMarkersCollections();
				} else {
					for (int i = 0; i < allPoints.size(); i++) {
						TargetPoint r = renderedPoints.get(i);
						TargetPoint a = allPoints.get(i);
						if (!a.equals(r)) {
							clearMapMarkersCollections();
							break;
						}
					}
				}
			}
			MapMarkersCollection markersCollection = this.mapMarkersCollection;
			if (markersCollection == null && !allPoints.isEmpty()) {
				markersCollection = new MapMarkersCollection();
				if (pointToStart != null) {
					int x = MapUtils.get31TileNumberX(pointToStart.getLongitude());
					int y = MapUtils.get31TileNumberY(pointToStart.getLatitude());
					drawMarker(markersCollection, mStartPoint, new PointI(x, y), null);
				}
				for (int i = 0; i < intermediatePoints.size(); i++) {
					TargetPoint ip = intermediatePoints.get(i);
					int x = MapUtils.get31TileNumberX(ip.getLongitude());
					int y = MapUtils.get31TileNumberY(ip.getLatitude());
					drawMarker(markersCollection, mIntermediatePoint, new PointI(x, y), String.valueOf(i + 1));
				}
				if (pointToNavigate != null) {
					int x = MapUtils.get31TileNumberX(pointToNavigate.getLongitude());
					int y = MapUtils.get31TileNumberY(pointToNavigate.getLatitude());
					drawMarker(markersCollection, mTargetPoint, new PointI(x, y), null);
				}
				mapRenderer.addSymbolsProvider(markersCollection);
				this.mapMarkersCollection = markersCollection;
			}
			this.renderedPoints = allPoints;
		}
		mapActivityInvalidated = false;
	}

	private void updateBitmaps(boolean forceUpdate) {
		OsmandApplication app = getApplication();
		float textScale = getTextScale();
		boolean carView = app.getOsmandMap().getMapView().isCarView();
		boolean carViewChanged = this.carView != carView;
		if (this.textScale != textScale || carViewChanged || forceUpdate) {
			this.textScale = textScale;
			this.carView = carView;
			recreateBitmaps();
			pointSizePx = Math.sqrt(mTargetPoint.getWidth() * mTargetPoint.getWidth()
					+ mTargetPoint.getHeight() * mTargetPoint.getHeight());
			if (carViewChanged) {
				updateTextSize();
			}
		}
	}

	private void updateTextSize() {
		mTextPaint.setTextSize(18f * Resources.getSystem().getDisplayMetrics().scaledDensity
				* getApplication().getOsmandMap().getCarDensityScaleCoef());
	}

	private void recreateBitmaps() {
		mStartPoint = getScaledBitmap(R.drawable.map_start_point);
		mTargetPoint = getScaledBitmap(R.drawable.map_target_point);
		mIntermediatePoint = getScaledBitmap(R.drawable.map_intermediate_point);
		clearMapMarkersCollections();
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
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o,
	                                    boolean unknownLocation, boolean excludeUntouchableObjects) {
		if (tileBox.getZoom() >= 3 && !excludeUntouchableObjects) {
			TargetPointsHelper tg = getApplication().getTargetPointsHelper();
			List<TargetPoint> intermediatePoints = tg.getAllPoints();
			int r = tileBox.getDefaultRadiusPoi();
			for (int i = 0; i < intermediatePoints.size(); i++) {
				TargetPoint tp = intermediatePoints.get(i);
				LatLon latLon = tp.point;
				if (latLon != null) {
					int ex = (int) point.x;
					int ey = (int) point.y;
					PointF pixel = NativeUtilities.getElevatedPixelFromLatLon(getMapRenderer(), tileBox, latLon);
					if (calculateBelongs(ex, ey, (int) pixel.x, (int) pixel.y, r)) {
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
			return ((TargetPoint) o).getPointDescription(getContext());
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
		applyMovableObject(position);
	}

	/** OpenGL */
	private void drawMarker(@NonNull MapMarkersCollection markersCollection,
	                        @NonNull Bitmap bitmap, @NonNull PointI position, @Nullable String caption) {
		if (!getMapView().hasMapRenderer()) {
			return;
		}

		MapMarkerBuilder mapMarkerBuilder = new MapMarkerBuilder();
		mapMarkerBuilder
				.setPosition(position)
				.setIsHidden(false)
				.setBaseOrder(getPointsOrder())
				.setPinIcon(NativeUtilities.createSkImageFromBitmap(bitmap))
				.setPinIconVerticalAlignment(MapMarker.PinIconVerticalAlignment.Top)
				.setPinIconHorisontalAlignment(MapMarker.PinIconHorisontalAlignment.Right);

		if (caption != null) {
			initCaptionStyle();
			mapMarkerBuilder
					.setCaptionStyle(captionStyle)
					.setCaptionTopSpace(-mIntermediatePoint.getHeight() * 0.7 - captionStyle.getSize() / 2)
					.setCaption(caption);
		}
		mapMarkerBuilder.buildAndAddToCollection(markersCollection);
	}

	/** OpenGL */
	private void initCaptionStyle() {
		if (!getMapView().hasMapRenderer() || captionStyle != null) {
			return;
		}
		int captionColor = getContext().getResources().getColor(
				nightMode ? R.color.widgettext_night : R.color.widgettext_day, null);
		captionStyle = new TextRasterizer.Style();
		captionStyle.setSize(mTextPaint.getTextSize());
		captionStyle.setWrapWidth(20);
		captionStyle.setMaxLines(3);
		captionStyle.setBold(false);
		captionStyle.setItalic(false);
		captionStyle.setColor(NativeUtilities.createColorARGB(captionColor));
	}

	private void drawStartPoint(Canvas canvas, RotatedTileBox tb, TargetPoint pointToStart) {
		int marginX = mStartPoint.getWidth() / 6;
		int marginY = mStartPoint.getHeight();
		float locationX = getPointX(tb, pointToStart);
		float locationY = getPointY(tb, pointToStart);
		canvas.rotate(-tb.getRotate(), locationX, locationY);
		canvas.drawBitmap(mStartPoint, locationX - marginX, locationY - marginY, mBitmapPaint);
		canvas.rotate(tb.getRotate(), locationX, locationY);
	}

	private void drawIntermediatePoint(Canvas canvas, RotatedTileBox tb, TargetPoint ip, int index) {
		float marginX = mIntermediatePoint.getWidth() / 6f;
		float marginY = mIntermediatePoint.getHeight();
		float locationX = getPointX(tb, ip);
		float locationY = getPointY(tb, ip);
		canvas.rotate(-tb.getRotate(), locationX, locationY);
		canvas.drawBitmap(mIntermediatePoint, locationX - marginX, locationY - marginY, mBitmapPaint);
		marginX = mIntermediatePoint.getWidth() / 3f;
		canvas.drawText(index + "", locationX + marginX, locationY - 3 * marginY / 5f, mTextPaint);
		canvas.rotate(tb.getRotate(), locationX, locationY);
	}

	private void drawPointToNavigate(Canvas canvas, RotatedTileBox tb, TargetPoint pointToNavigate) {
		int marginX = mTargetPoint.getWidth() / 6;
		int marginY = mTargetPoint.getHeight();
		float locationX = getPointX(tb, pointToNavigate);
		float locationY = getPointY(tb, pointToNavigate);
		canvas.rotate(-tb.getRotate(), locationX, locationY);
		canvas.drawBitmap(mTargetPoint, locationX - marginX, locationY - marginY, mBitmapPaint);
		canvas.rotate(tb.getRotate(), locationX, locationY);
	}
}
