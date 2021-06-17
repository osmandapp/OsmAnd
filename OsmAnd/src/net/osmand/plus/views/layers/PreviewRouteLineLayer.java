package net.osmand.plus.views.layers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.render.OsmandRenderer;
import net.osmand.plus.routing.PreviewRouteLineInfo;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.CommonPreference;
import net.osmand.plus.track.GradientScaleType;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.geometry.GeometryWayStyle;
import net.osmand.plus.views.layers.geometry.RouteGeometryWay;
import net.osmand.plus.views.layers.geometry.RouteGeometryWay.GeometryGradientWayStyle;
import net.osmand.plus.views.layers.geometry.RouteGeometryWayContext;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.router.RouteColorize;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

import static net.osmand.plus.dialogs.ConfigureMapMenu.CURRENT_TRACK_WIDTH_ATTR;

public class PreviewRouteLineLayer extends OsmandMapLayer {

	private static final Log log = PlatformUtil.getLog(PreviewRouteLineLayer.class);

	private static final int DEFAULT_WIDTH_MULTIPLIER = 7;

	protected OsmandMapTileView view;
	protected boolean nightMode;

	private LayerDrawable previewIcon;
	private Bitmap actionArrow;
	protected Paint paintIconAction;

	private RouteGeometryWayContext previewWayContext;
	private RouteGeometryWay previewLineGeometry;
	private final Map<String, Float> cachedRouteLineWidth = new HashMap<>();

	protected PreviewRouteLineInfo previewRouteLineInfo;
	protected GradientScaleType gradientScaleType = null;

	protected RenderingLineAttributes attrs;
	protected boolean useAttrRouteColor = true;
	protected int routeLineColor;
	protected Integer directionArrowsColor;
	protected int customTurnArrowColor = 0;
	protected Boolean attrsIsPaint_1 = null;

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		float density = view.getDensity();
		initAttrs(density);
		initGeometries(density);
		initPaints();
		initIcons();
	}

	protected void initAttrs(float density) {
		attrs = new RenderingLineAttributes("route");
		attrs.defaultWidth = (int) (12 * density);
		attrs.defaultWidth3 = (int) (7 * density);
		attrs.defaultColor = view.getResources().getColor(R.color.nav_track);
		attrs.shadowPaint.setColor(0x80000000);
		attrs.shadowPaint.setStrokeCap(Paint.Cap.ROUND);
		attrs.paint3.setStrokeCap(Paint.Cap.BUTT);
		attrs.paint3.setColor(Color.WHITE);
		attrs.paint2.setStrokeCap(Paint.Cap.BUTT);
		attrs.paint2.setColor(Color.BLACK);
	}

	protected void initGeometries(float density) {
		previewWayContext = new RouteGeometryWayContext(view.getContext(), density);
		previewWayContext.updatePaints(nightMode, attrs);
		previewLineGeometry = new RouteGeometryWay(previewWayContext);
	}

	protected void initPaints() {
		paintIconAction = new Paint();
		paintIconAction.setFilterBitmap(true);
		paintIconAction.setAntiAlias(true);
	}

	protected void initIcons() {
		actionArrow = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_action_arrow, null);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (previewRouteLineInfo != null) {

			updateRouteGradient();
			updateAttrs(settings, tileBox);
			updateRouteColors(nightMode);

			float angle = tileBox.getRotate();
			QuadPoint c = tileBox.getCenterPixelPoint();

			canvas.rotate(-angle, c.x, c.y);
			drawRouteLinePreview(canvas, tileBox, previewRouteLineInfo);
			canvas.rotate(angle, c.x, c.y);
		}
	}

	private void drawRouteLinePreview(Canvas canvas,
									  RotatedTileBox tileBox,
									  PreviewRouteLineInfo previewInfo) {
		Rect previewBounds = previewInfo.getLineBounds();
		if (previewBounds == null) {
			return;
		}
		float startX = previewBounds.left;
		float startY = previewBounds.bottom;
		float endX = previewBounds.right;
		float endY = previewBounds.top;
		float centerX = previewInfo.getCenterX();
		float centerY = previewInfo.getCenterY();

		List<Float> tx = new ArrayList<>();
		List<Float> ty = new ArrayList<>();
		tx.add(startX);
		tx.add(centerX);
		tx.add(centerX);
		tx.add(endX);
		ty.add(startY);
		ty.add(startY);
		ty.add(endY);
		ty.add(endY);

		List<Double> angles = new ArrayList<>();
		List<Double> distances = new ArrayList<>();
		List<GeometryWayStyle<?>> styles = new ArrayList<>();
		previewLineGeometry.setRouteStyleParams(getRouteLineColor(), getRouteLineWidth(tileBox), directionArrowsColor, gradientScaleType);
		fillPreviewLineArrays(tx, ty, angles, distances, styles);
		canvas.rotate(+tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
		previewLineGeometry.drawRouteSegment(tileBox, canvas, tx, ty, angles, distances, 0, styles);
		canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());

		Matrix matrix = new Matrix();
		Path path = new Path();
		int lineLength = AndroidUtils.dpToPx(view.getContext(), 24);
		int offset = AndroidUtils.isLayoutRtl(view.getContext()) ? lineLength : -lineLength;
		int attrsTurnArrowColor = attrs.paint3.getColor();
		if (customTurnArrowColor != 0) {
			attrs.paint3.setColor(customTurnArrowColor);
		}
		path.moveTo(centerX + offset, startY);
		path.lineTo(centerX, startY);
		path.lineTo(centerX, startY - lineLength);
		canvas.drawPath(path, attrs.paint3);
		drawDirectionArrow(canvas, attrs.paint3, matrix, centerX, startY - lineLength, centerX, startY);
		path.reset();
		path.moveTo(centerX, endY + lineLength);
		path.lineTo(centerX, endY);
		path.lineTo(centerX - offset, endY);
		canvas.drawPath(path, attrs.paint3);
		drawDirectionArrow(canvas, attrs.paint3, matrix, centerX - offset, endY, centerX, endY);
		attrs.paint3.setColor(attrsTurnArrowColor);

		if (previewIcon == null) {
			previewIcon = (LayerDrawable) AppCompatResources.getDrawable(view.getContext(), previewInfo.getIconId());
			DrawableCompat.setTint(previewIcon.getDrawable(1), previewInfo.getIconColor());
		}
		canvas.rotate(-90, centerX, centerY);
		drawIcon(canvas, previewIcon, (int) centerX, (int) centerY);
		canvas.rotate(90, centerX, centerY);
	}

	private void fillPreviewLineArrays(List<Float> tx, List<Float> ty, List<Double> angles,
									   List<Double> distances, List<GeometryWayStyle<?>> styles) {
		fillDistancesAngles(tx, ty, angles, distances);

		if (gradientScaleType == null) {
			for (int i = 0; i < tx.size(); i++) {
				styles.add(previewLineGeometry.getDefaultWayStyle());
			}
		} else if (gradientScaleType == GradientScaleType.ALTITUDE) {
			fillAltitudeGradientArrays(distances, styles);
		} else if (gradientScaleType == GradientScaleType.SLOPE) {
			fillSlopeGradientArrays(tx, ty, angles, distances, styles);
		}
	}

	private void fillAltitudeGradientArrays(List<Double> distances, List<GeometryWayStyle<?>> styles) {
		int[] colors = RouteColorize.COLORS;
		for (int i = 1; i < distances.size(); i++) {
			RouteGeometryWay.GeometryGradientWayStyle style = previewLineGeometry.getGradientWayStyle();
			styles.add(style);
			double prevDist = distances.get(i - 1);
			double currDist = distances.get(i);
			double nextDist = i + 1 == distances.size() ? 0 : distances.get(i + 1);
			style.currColor = getPreviewColor(colors, i - 1, (prevDist + currDist / 2) / (prevDist + currDist));
			style.nextColor = getPreviewColor(colors, i, (currDist + nextDist / 2) / (currDist + nextDist));
		}
		styles.add(styles.get(styles.size() - 1));
	}

	private void fillSlopeGradientArrays(List<Float> tx, List<Float> ty, List<Double> angles,
										 List<Double> distances, List<GeometryWayStyle<?>> styles) {
		double totalDist = 0;
		for (Double d : distances) {
			totalDist += d;
		}

		boolean rtl = AndroidUtils.isLayoutRtl(view.getContext());
		int[] palette = RouteColorize.SLOPE_COLORS;
		List<Double> gradientLengthsRatio = Arrays.asList(0.145833, 0.130209, 0.291031);
		List<Float> srcTx = new ArrayList<>(tx);
		List<Float> srcTy = new ArrayList<>(ty);
		int[] colors = new int[srcTx.size() + gradientLengthsRatio.size()];
		colors[0] = palette[0];
		colors[colors.length - 1] = palette[palette.length - 1];
		double passedDist = 0;

		for (int i = 0; i < gradientLengthsRatio.size(); i++) {
			double ratio = gradientLengthsRatio.get(i);
			double length = passedDist + totalDist * ratio;
			passedDist += totalDist * ratio;
			int insertIdx;
			for (insertIdx = 1; insertIdx < distances.size() && length - distances.get(insertIdx) > 0; insertIdx++) {
				length -= distances.get(insertIdx);
			}

			float px = srcTx.get(insertIdx - 1);
			float py = srcTy.get(insertIdx - 1);
			float nx = srcTx.get(insertIdx);
			float ny = srcTy.get(insertIdx);
			float r = (float) (length / distances.get(insertIdx));
			float x = (float) Math.ceil(rtl ? px - (px - nx) * r : px + (nx - px) * r);
			float y = (float) Math.ceil(py + (ny - py) * r);
			int idx = findNextPrevPointIdx(x, y, tx, ty, !rtl);
			tx.add(idx, x);
			ty.add(idx, y);
			colors[idx] = palette[i + 1];
		}

		distances.clear();
		angles.clear();
		fillDistancesAngles(tx, ty, angles, distances);

		for (int i = 1; i < tx.size(); i++) {
			GeometryGradientWayStyle style = previewLineGeometry.getGradientWayStyle();
			styles.add(style);
			double currDist = distances.get(i);
			double nextDist = i + 1 == distances.size() ? 0 : distances.get(i + 1);
			style.currColor = i == 1 ? colors[0] : ((GeometryGradientWayStyle) styles.get(i - 2)).nextColor;
			if (colors[i] != 0) {
				style.nextColor = colors[i];
			} else {
				double coeff = currDist / (currDist + nextDist);
				style.nextColor = RouteColorize.getIntermediateColor(colors[i - 1], colors[i + 1], coeff);
			}
		}
		styles.add(styles.get(styles.size() - 1));
	}

	public boolean isPreviewRouteLineVisible() {
		return previewRouteLineInfo != null;
	}

	public void setPreviewRouteLineInfo(PreviewRouteLineInfo previewInfo) {
		this.previewRouteLineInfo = previewInfo;
		if (previewInfo == null) {
			previewIcon = null;
		}
	}

	protected void updateAttrs(DrawSettings settings, RotatedTileBox tileBox) {
		boolean updatePaints = attrs.updatePaints(view.getApplication(), settings, tileBox);
		attrs.isPaint2 = false;
		attrs.isPaint3 = false;

		nightMode = settings != null && settings.isNightMode();

		updateIsPaint_1(updatePaints);

		if (updatePaints) {
			previewWayContext.updatePaints(nightMode, attrs);
		}
	}

	protected void updateRouteColors(boolean night) {
		Integer color;
		if (previewRouteLineInfo != null) {
			color = previewRouteLineInfo.getColor(night);
		} else {
			CommonPreference<Integer> colorPreference = night ?
					view.getSettings().ROUTE_LINE_COLOR_NIGHT :
					view.getSettings().ROUTE_LINE_COLOR_DAY;
			int storedValue = colorPreference.getModeValue(getAppMode());
			color = storedValue != 0 ? storedValue : null;
		}
		if (color == null) {
			useAttrRouteColor = gradientScaleType == null;
			directionArrowsColor = null;
			updateAttrs(new DrawSettings(night), view.getCurrentRotatedTileBox());
			color = attrs.paint.getColor();
		} else if (routeLineColor != color) {
			useAttrRouteColor = false;
			directionArrowsColor = UiUtilities.getContrastColor(view.getContext(), color, false);
			updateIsPaint_1(false);
		}
		routeLineColor = color;
		updateTurnArrowColor();
	}

	protected boolean updateRouteGradient() {
		GradientScaleType prev = gradientScaleType;
		if (previewRouteLineInfo != null) {
			gradientScaleType = previewRouteLineInfo.getGradientScaleType();
		} else {
			ApplicationMode mode = view.getApplication().getRoutingHelper().getAppMode();
			gradientScaleType = view.getSettings().ROUTE_LINE_GRADIENT.getModeValue(mode);
		}
		return prev != gradientScaleType;
	}

	protected void updateIsPaint_1(boolean updatePaints) {
		if (updatePaints) {
			attrsIsPaint_1 = attrs.isPaint_1;
		}
		if (attrsIsPaint_1 != null) {
			attrs.isPaint_1 = attrsIsPaint_1 && useAttrRouteColor;
		}
	}

	@ColorInt
	public int getRouteLineColor(boolean night) {
		updateRouteColors(night);
		return routeLineColor;
	}

	@ColorInt
	public int getRouteLineColor() {
		return routeLineColor;
	}

	protected float getRouteLineWidth(@NonNull RotatedTileBox tileBox) {
		String widthKey;
		if (previewRouteLineInfo != null) {
			widthKey = previewRouteLineInfo.getWidth();
		} else {
			widthKey = view.getSettings().ROUTE_LINE_WIDTH.getModeValue(getAppMode());
		}
		return widthKey != null ? getWidthByKey(tileBox, widthKey) : attrs.paint.getStrokeWidth();
	}

	@Nullable
	protected Float getWidthByKey(RotatedTileBox tileBox, String widthKey) {
		Float resultValue = cachedRouteLineWidth.get(widthKey);
		if (resultValue != null) {
			return resultValue;
		}
		if (!Algorithms.isEmpty(widthKey) && Algorithms.isInt(widthKey)) {
			try {
				int widthDp = Integer.parseInt(widthKey);
				resultValue = (float) AndroidUtils.dpToPx(view.getApplication(), widthDp);
			} catch (NumberFormatException e) {
				log.error(e.getMessage(), e);
				resultValue = DEFAULT_WIDTH_MULTIPLIER * view.getDensity();
			}
		} else {
			RenderingRulesStorage rrs = view.getApplication().getRendererRegistry().getCurrentSelectedRenderer();
			RenderingRuleSearchRequest req = new RenderingRuleSearchRequest(rrs);
			req.setBooleanFilter(rrs.PROPS.R_NIGHT_MODE, nightMode);
			req.setIntFilter(rrs.PROPS.R_MINZOOM, tileBox.getZoom());
			req.setIntFilter(rrs.PROPS.R_MAXZOOM, tileBox.getZoom());
			RenderingRuleProperty ctWidth = rrs.PROPS.get(CURRENT_TRACK_WIDTH_ATTR);
			if (ctWidth != null) {
				req.setStringFilter(ctWidth, widthKey);
			}
			if (req.searchRenderingAttribute("gpx")) {
				OsmandRenderer.RenderingContext rc = new OsmandRenderer.RenderingContext(view.getContext());
				rc.setDensityValue((float) tileBox.getMapDensity());
				resultValue = rc.getComplexValue(req, req.ALL.R_STROKE_WIDTH);
			}
		}
		cachedRouteLineWidth.put(widthKey, resultValue);
		return resultValue;
	}

	protected void updateTurnArrowColor() {
		customTurnArrowColor = gradientScaleType == null ? attrs.paint3.getColor() : Color.WHITE;
		paintIconAction.setColorFilter(new PorterDuffColorFilter(customTurnArrowColor, PorterDuff.Mode.MULTIPLY));
	}

	protected void drawDirectionArrow(Canvas canvas, Paint turnArrowsPaint, Matrix matrix,
	                                  float x, float y, float px, float py) {
		double angleRad = Math.atan2(y - py, x - px);
		double angle = (angleRad * 180 / Math.PI) + 90f;
		double distSegment = Math.sqrt((y - py) * (y - py) + (x - px) * (x - px));
		if (distSegment == 0) {
			return;
		}
		float pdx = x - px;
		float pdy = y - py;
		float scale = turnArrowsPaint.getStrokeWidth() / (actionArrow.getWidth() / 2.25f);
		float scaledWidth = actionArrow.getWidth();
		matrix.reset();
		matrix.postTranslate(0, -actionArrow.getHeight() / 2f);
		matrix.postRotate((float) angle, actionArrow.getWidth() / 2f, 0);
		if (scale > 1.0f) {
			matrix.postScale(scale, scale);
			scaledWidth *= scale;
		}
		matrix.postTranslate(px + pdx - scaledWidth / 2f, py + pdy);
		canvas.drawBitmap(actionArrow, matrix, paintIconAction);
	}

	protected void drawIcon(Canvas canvas, Drawable drawable, int locationX, int locationY) {
		drawable.setBounds(locationX - drawable.getIntrinsicWidth() / 2,
				locationY - drawable.getIntrinsicHeight() / 2,
				locationX + drawable.getIntrinsicWidth() / 2,
				locationY + drawable.getIntrinsicHeight() / 2);
		drawable.draw(canvas);
	}

	private void fillDistancesAngles(List<Float> tx, List<Float> ty, List<Double> angles,
									 List<Double> distances) {
		angles.add(0d);
		distances.add(0d);
		for (int i = 1; i < tx.size(); i++) {
			float x = tx.get(i);
			float y = ty.get(i);
			float px = tx.get(i - 1);
			float py = ty.get(i - 1);
			double angleRad = Math.atan2(y - py, x - px);
			Double angle = (angleRad * 180 / Math.PI) + 90f;
			angles.add(angle);
			double dist = Math.sqrt((y - py) * (y - py) + (x - px) * (x - px));
			distances.add(dist);
		}
	}

	private int findNextPrevPointIdx(float x, float y, List<Float> tx, List<Float> ty, boolean next) {
		for (int i = 0; i < tx.size(); i++) {
			if (next && tx.get(i) >= x || !next && tx.get(i) <= x) {
				if (ty.get(i) == y) {
					return i;
				} else if (ty.get(i) <= y) {
					return i;
				}
			}
		}
		return tx.size() - 1;
	}

	private int getPreviewColor(int[] colors, int index, double coeff) {
		if (index == 0) {
			return colors[0];
		} else if (index > 0 && index < colors.length) {
			return RouteColorize.getIntermediateColor(colors[index - 1], colors[index], coeff);
		} else if (index == colors.length) {
			return colors[index - 1];
		}
		return 0;
	}

	private ApplicationMode getAppMode() {
		return view.getApplication().getRoutingHelper().getAppMode();
	}

	@Override
	public void destroyLayer() {
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}
}