package net.osmand.plus.views.layers;

import static net.osmand.plus.settings.backend.OsmAndAppCustomizationFields.ROUTE_INTERMEDIATE_POINT;
import static net.osmand.plus.settings.backend.OsmAndAppCustomizationFields.ROUTE_START_POINT;
import static net.osmand.plus.settings.backend.OsmAndAppCustomizationFields.ROUTE_TARGET_POINT;

import android.content.Context;
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
import net.osmand.plus.base.containers.ShiftedBitmap;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.TargetPoint;
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
			if (movableObject instanceof TargetPoint targetPoint) {
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
			if (nightMode != settings.isNightMode()
					|| shouldUpdateTextSizeForOpenGL() || mapActivityInvalidated) {
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
					drawMarkerOpenGL(markersCollection, mStartPoint, new PointI(x, y), null);
				}
				for (int i = 0; i < intermediatePoints.size(); i++) {
					TargetPoint ip = intermediatePoints.get(i);
					int x = MapUtils.get31TileNumberX(ip.getLongitude());
					int y = MapUtils.get31TileNumberY(ip.getLatitude());
					drawMarkerOpenGL(markersCollection, mIntermediatePoint, new PointI(x, y), String.valueOf(i + 1));
				}
				if (pointToNavigate != null) {
					int x = MapUtils.get31TileNumberX(pointToNavigate.getLongitude());
					int y = MapUtils.get31TileNumberY(pointToNavigate.getLatitude());
					drawMarkerOpenGL(markersCollection, mTargetPoint, new PointI(x, y), null);
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
			updateTextSize();
		}
	}

	private void updateTextSize() {
		float density = view.getDensity();
		float textSize = 18f * textScale * density;
		mTextPaint.setTextSize(textSize);
	}

	private boolean shouldUpdateTextSizeForOpenGL() {
		if (mTextPaint != null && captionStyle != null) {
			return mTextPaint.getTextSize() != captionStyle.getSize();
		}
		return true;
	}

	private void recreateBitmaps() {
		mStartPoint = getScaledBitmap(ROUTE_START_POINT);
		mTargetPoint = getScaledBitmap(ROUTE_TARGET_POINT);
		mIntermediatePoint = getScaledBitmap(ROUTE_INTERMEDIATE_POINT);
		clearMapMarkersCollections();
	}

	@Nullable
	@Override
	protected Bitmap getScaledBitmap(int drawableId) {
		return getScaledBitmap(drawableId, textScale);
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
	public void collectObjectsFromPoint(@NonNull MapSelectionResult result, @NonNull MapSelectionRules rules) {
		PointF point = result.getPoint();
		RotatedTileBox tileBox = result.getTileBox();
		if (tileBox.getZoom() >= 3 && !rules.isOnlyTouchableObjects()) {
			TargetPointsHelper tg = getApplication().getTargetPointsHelper();
			List<TargetPoint> intermediatePoints = tg.getAllPoints();
			int r = tileBox.getDefaultRadiusPoi();
			for (int i = 0; i < intermediatePoints.size(); i++) {
				TargetPoint tp = intermediatePoints.get(i);
				LatLon latLon = tp.getLatLon();
				if (latLon != null) {
					int ex = (int) point.x;
					int ey = (int) point.y;
					PointF pixel = NativeUtilities.getElevatedPixelFromLatLon(getMapRenderer(), tileBox, latLon);
					if (calculateBelongs(ex, ey, (int) pixel.x, (int) pixel.y, r)) {
						result.collect(tp, this);
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
			return ((TargetPoint) o).getLatLon();
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
	public Object getMoveableObjectIcon(@NonNull Object o) {
		if (o instanceof TargetPoint targetPoint) {
			if (Algorithms.objectEquals(targetPoints.getPointToStart(), targetPoint)) {
				return getStartPointIcon();
			} else if (Algorithms.objectEquals(targetPoints.getPointToNavigate(), targetPoint)) {
				return getPointToNavigateIcon();
			} else if (targetPoints.getIntermediatePoints().contains(targetPoint)) {
				return getIntermediatePointIcon();
			}
		}
		return null;
	}

	@Nullable
	@Override
	public String getMoveableObjectLabel(@NonNull Object o) {
		if (o instanceof TargetPoint targetPoint) {
			int index = targetPoints.getIntermediatePoints().indexOf(targetPoint);
			if (index >= 0) {
				return String.valueOf(++index);
			}
		}
		return null;
	}

	@Override
	public void applyNewObjectPosition(@NonNull Object o, @NonNull LatLon position,
	                                   @Nullable ContextMenuLayer.ApplyMovedObjectCallback callback) {
		boolean result = false;
		TargetPoint newTargetPoint = null;
		if (o instanceof TargetPoint oldPoint) {
			TargetPointsHelper targetPointsHelper = getApplication().getTargetPointsHelper();
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

	private void drawMarkerOpenGL(@NonNull MapMarkersCollection markersCollection,
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
			initCaptionStyleOpenGL();
			mapMarkerBuilder
					.setCaptionStyle(captionStyle)
					.setCaptionTopSpace(-mIntermediatePoint.getHeight() * 0.7 - captionStyle.getSize() / 2)
					.setCaption(caption);
		}
		mapMarkerBuilder.buildAndAddToCollection(markersCollection);
	}

	private void initCaptionStyleOpenGL() {
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

	private void drawStartPoint(@NonNull Canvas canvas, @NonNull RotatedTileBox tb,
	                            @NonNull TargetPoint pointToStart) {
		drawPointImpl(canvas, tb, pointToStart, getStartPointIcon(), null);
	}

	private void drawIntermediatePoint(@NonNull Canvas canvas, @NonNull RotatedTileBox tb,
	                                   @NonNull TargetPoint ip, int index) {
		drawPointImpl(canvas, tb, ip, getIntermediatePointIcon(), index + "");
	}

	private void drawPointToNavigate(@NonNull Canvas canvas, @NonNull RotatedTileBox tb,
	                                 @NonNull TargetPoint pointToNavigate) {
		drawPointImpl(canvas, tb, pointToNavigate, getPointToNavigateIcon(), null);
	}

	@NonNull
	public ShiftedBitmap getStartPointIcon() {
		return getShiftedBitmap(mStartPoint);
	}

	@NonNull
	public ShiftedBitmap getIntermediatePointIcon() {
		return getShiftedBitmap(mIntermediatePoint);
	}

	@NonNull
	public ShiftedBitmap getPointToNavigateIcon() {
		return getShiftedBitmap(mTargetPoint);
	}

	@NonNull
	private ShiftedBitmap getShiftedBitmap(@NonNull Bitmap bitmap) {
		return new ShiftedBitmap(bitmap, bitmap.getWidth() / 6f, bitmap.getHeight());
	}

	private void drawPointImpl(@NonNull Canvas canvas, @NonNull RotatedTileBox tileBox,
	                           @NonNull TargetPoint point, @NonNull ShiftedBitmap shiftedBitmap,
	                           @Nullable String label) {
		float x, y, rotationX, rotationY;
		
		if (contextMenuLayer.getMoveableObject() != null && point == contextMenuLayer.getMoveableObject()) {
			PointF centerPoint = contextMenuLayer.getMovableCenterPoint(tileBox);
			x = centerPoint.x;
			y = centerPoint.y;
			rotationX = tileBox.getCenterPixelX();
			rotationY = tileBox.getCenterPixelY();
		} else {
			rotationX = x = tileBox.getPixXFromLonNoRot(point.getLongitude());
			rotationY = y = tileBox.getPixYFromLatNoRot(point.getLatitude());
		}
		Bitmap bitmap = shiftedBitmap.getBitmap();
		float marginX = shiftedBitmap.getMarginX();
		float marginY = shiftedBitmap.getMarginY();
		
		canvas.save();
		canvas.rotate(-tileBox.getRotate(), rotationX, rotationY);
		canvas.drawBitmap(bitmap, x - marginX, y - marginY, mBitmapPaint);
		if (label != null) {
			marginX = bitmap.getWidth() / 3f;
			canvas.drawText(label, x + marginX, y - 3 * marginY / 5f, mTextPaint);
		}
		canvas.restore();
	}
}
