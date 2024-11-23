package net.osmand.plus.views.layers;

import static net.osmand.plus.settings.backend.OsmandSettings.RENDERER_PREFERENCE_PREFIX;
import static net.osmand.plus.views.layers.geometry.RouteGeometryWay.MIN_COLOR_SQUARE_DISTANCE;
import static net.osmand.render.RenderingRuleStorageProperties.ADDITIONAL;
import static net.osmand.render.RenderingRuleStorageProperties.TAG;
import static net.osmand.render.RenderingRuleStorageProperties.VALUE;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.drawable.LayerDrawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

import net.osmand.PlatformUtil;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.routing.PreviewRouteLineInfo;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.layers.base.BaseRouteLayer;
import net.osmand.plus.views.layers.geometry.GeometryGradientWayStyle;
import net.osmand.plus.views.layers.geometry.GeometryWayPoint;
import net.osmand.plus.views.layers.geometry.GeometryWayStyle;
import net.osmand.plus.views.layers.geometry.RouteGeometryWay;
import net.osmand.plus.views.layers.geometry.RouteGeometryWayContext;
import net.osmand.render.RenderingRule;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.router.RouteStatisticsHelper;
import net.osmand.shared.ColorPalette;
import net.osmand.shared.gpx.GradientScaleType;
import net.osmand.shared.routing.ColoringType;
import net.osmand.shared.routing.RouteColorize;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PreviewRouteLineLayer extends BaseRouteLayer {

	private static final Log log = PlatformUtil.getLog(PreviewRouteLineLayer.class);

	private LayerDrawable previewIcon;

	private RouteGeometryWayContext previewWayContext;
	private RouteGeometryWay previewLineGeometry;

	public PreviewRouteLineLayer(@NonNull Context ctx) {
		super(ctx);
	}

	@Override
	protected void initGeometries(float density) {
		previewWayContext = new RouteGeometryWayContext(getContext(), density);
		previewWayContext.disableMapRenderer();
		previewWayContext.updatePaints(nightMode, attrs);
		previewLineGeometry = new RouteGeometryWay(previewWayContext);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (previewRouteLineInfo != null) {

			updateRouteColoringType();
			updateAttrs(settings, tileBox);
			updateRouteColors(nightMode);

			float angle = tileBox.getRotate();
			QuadPoint c = tileBox.getCenterPixelPoint();

			canvas.rotate(-angle, c.x, c.y);
			drawRouteLinePreview(canvas, tileBox, previewRouteLineInfo);
			canvas.rotate(angle, c.x, c.y);
		}
	}

	@Override
	protected void updateAttrs(DrawSettings settings, RotatedTileBox tileBox) {
		boolean updatePaints = attrs.updatePaints(view.getApplication(), settings, tileBox);
		attrs.isPaint2 = false;
		attrs.isPaint3 = false;

		nightMode = settings != null && settings.isNightMode();

		if (updatePaints) {
			previewWayContext.updatePaints(nightMode, attrs);
		}
	}

	@Override
	protected void updateTurnArrowColor() {
		customTurnArrowColor = routeColoringType.isGradient() ? Color.WHITE : attrs.paint3.getColor();
		paintIconAction.setColorFilter(new PorterDuffColorFilter(customTurnArrowColor, PorterDuff.Mode.MULTIPLY));
	}

	@Override
	public void setPreviewRouteLineInfo(PreviewRouteLineInfo previewRouteLineInfo) {
		super.setPreviewRouteLineInfo(previewRouteLineInfo);
		if (previewRouteLineInfo == null) {
			previewIcon = null;
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


		List<GeometryWayPoint> points = new ArrayList<>();
		points.add(new GeometryWayPoint(points.size(), startX, startY));
		points.add(new GeometryWayPoint(points.size(), centerX, startY));
		points.add(new GeometryWayPoint(points.size(), centerX, endY));
		points.add(new GeometryWayPoint(points.size(), endX, endY));

		previewLineGeometry.setRouteStyleParams(getRouteLineColor(), getRouteLineWidth(tileBox),
				shouldShowDirectionArrows(), getDirectionArrowsColor(), routeColoringType, routeInfoAttribute, routeGradientPalette);
		fillPreviewLineArrays(points);
		canvas.rotate(+tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
		previewLineGeometry.drawRouteSegment(tileBox, canvas, points, 0);
		canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());

		if (previewRouteLineInfo.shouldShowTurnArrows()) {
			int lineLength = AndroidUtils.dpToPx(getContext(), 24);
			int offset = AndroidUtils.isLayoutRtl(getContext()) ? lineLength : -lineLength;

			List<List<PointF>> arrows = new ArrayList<>();
			arrows.add(Arrays.asList(
					new PointF(centerX + offset, startY),
					new PointF(centerX, startY),
					new PointF(centerX, startY - lineLength)
			));
			arrows.add(Arrays.asList(
					new PointF(centerX, endY + lineLength),
					new PointF(centerX, endY),
					new PointF(centerX - offset, endY)
			));

			drawTurnArrows(canvas, arrows, points);
		}

		if (previewIcon == null) {
			previewIcon = (LayerDrawable) AppCompatResources.getDrawable(getContext(), previewInfo.getIconId());
			if (previewIcon != null) {
				DrawableCompat.setTint(previewIcon.getDrawable(1), previewInfo.getIconColor());
			}
		}
		canvas.rotate(-90, centerX, centerY);
		drawIcon(canvas, previewIcon, (int) centerX, (int) centerY);
		canvas.rotate(90, centerX, centerY);
	}

	@Override
	protected void updateResources() {
		super.updateResources();
		previewIcon = null;
	}

	private void fillPreviewLineArrays(List<GeometryWayPoint> points) {
		fillDistancesAngles(points);
		if (routeColoringType.isSolidSingleColor()) {
			for (int i = 0; i < points.size(); i++) {
				points.get(i).style = previewLineGeometry.getDefaultWayStyle();
			}
		} else if (routeColoringType == ColoringType.ALTITUDE) {
			fillAltitudeGradientArrays(points);
		} else if (routeColoringType == ColoringType.SLOPE) {
			fillSlopeGradientArrays(points);
		} else if (routeColoringType.isRouteInfoAttribute()) {
			boolean success = fillRouteInfoAttributeArrays(points);
			if (!success) {
				for (int i = 0; i < points.size(); i++) {
					points.get(i).style = previewLineGeometry.getDefaultWayStyle();
				}
			}
		}

	}


	private void fillAltitudeGradientArrays(List<GeometryWayPoint> points) {
		int[] colors = ColorPalette.Companion.getCOLORS();
		GeometryGradientWayStyle<?> style = null;
		for (int i = 1; i < points.size(); i++) {
			style = previewLineGeometry.getGradientWayStyle();
			points.get(i - 1).style = style;
			double prevDist = points.get(i - 1).distance;
			double currDist = points.get(i).distance;
			double nextDist = i + 1 == points.size() ? 0 : points.get(i + 1).distance;
			style.currColor = getPreviewColor(colors, i - 1, (prevDist + currDist / 2) / (prevDist + currDist));
			style.nextColor = getPreviewColor(colors, i, (currDist + nextDist / 2) / (currDist + nextDist));
		}
		points.get(points.size() - 1).style = style;
	}

	private void fillSlopeGradientArrays(List<GeometryWayPoint> points) {
		ColorPalette previewPalette = ColorPalette.Companion.getMIN_MAX_PALETTE();
		GradientScaleType gradientScaleType = routeColoringType.toGradientScaleType();
		if (gradientScaleType != null) {
			RouteColorize.ColorizationType colorizationType = gradientScaleType.toColorizationType();
			previewPalette = getApplication().getColorPaletteHelper().requireGradientColorPaletteSync(colorizationType, routeGradientPalette);
		}
		List<Integer> palette = new ArrayList<>();
		for (ColorPalette.ColorValue colorValue : previewPalette.getColors()) {
			palette.add(colorValue.getClr());
		}
		int ratiosAmount = palette.size() - 1;
		double lengthRatio = 1d / palette.size();
		List<Double> gradientLengthsRatio = new ArrayList<>(Collections.nCopies(ratiosAmount, lengthRatio));
		List<Integer> colors = new ArrayList<>();

		fillMultiColorLineArrays(palette, gradientLengthsRatio, points, colors);

		GeometryGradientWayStyle<?> style = null;
		for (int i = 1; i < points.size(); i++) {
			style = previewLineGeometry.getGradientWayStyle();
			points.get(i - 1).style = style;
			double currDist = points.get(i).distance;
			double nextDist = i + 1 == points.size() ? 0 : points.get(i + 1).distance;
			style.currColor = i == 1 ? colors.get(0) : ((GeometryGradientWayStyle<?>) points.get(i - 2).style).nextColor;
			if (colors.get(i) != 0) {
				style.nextColor = colors.get(i);
			} else {
				double coeff = currDist / (currDist + nextDist);
				style.nextColor = ColorPalette.Companion.getIntermediateColor(colors.get(i - 1), colors.get(i + 1), coeff);
			}
		}
		points.get(points.size() - 1).style = points.get(points.size() - 2).style;
	}

	private boolean fillRouteInfoAttributeArrays(List<GeometryWayPoint> points) {
		List<Integer> palette = fetchColorsOfRouteInfoAttribute();
		if (Algorithms.isEmpty(palette)) {
			return false;
		}
		int ratiosAmount = palette.size() - 1;
		double lengthRatio = 1d / palette.size();
		List<Double> attributesLengthsRatio = new ArrayList<>(Collections.nCopies(ratiosAmount, lengthRatio));
		List<Integer> colors = new ArrayList<>();

		fillMultiColorLineArrays(palette, attributesLengthsRatio, points, colors);
		for (int i = 0; i < points.size() - 1; i++) {
			points.get(i).style = previewLineGeometry.getSolidWayStyle(colors.get(i));
		}
		points.get(points.size() - 1).style = points.get(points.size() - 2).style;
		return true;
	}

	private void fillMultiColorLineArrays(List<Integer> palette, List<Double> lengthRatios,
	                                      List<GeometryWayPoint> points, List<Integer> colors) {
		double totalDist = 0;
		for(GeometryWayPoint p : points) {
			totalDist += p.distance;
		}

		boolean rtl = AndroidUtils.isLayoutRtl(getContext());

		List<GeometryWayPoint> src = new ArrayList<>(points);
		int[] colorsArray = new int[points.size() + lengthRatios.size()];
		colorsArray[0] = palette.get(0);
		colorsArray[colorsArray.length - 1] = palette.get(palette.size() - 1);
		double passedDist = 0;

		for (int i = 0; i < lengthRatios.size(); i++) {
			double ratio = lengthRatios.get(i);
			double length = passedDist + totalDist * ratio;
			passedDist += totalDist * ratio;
			int insertIdx;
			for (insertIdx = 1; insertIdx < src.size() && length - src.get(insertIdx).distance > 0; insertIdx++) {
				length -= src.get(insertIdx).distance;
			}

			float px = src.get(insertIdx - 1).tx;
			float py = src.get(insertIdx - 1).ty;
			float nx = src.get(insertIdx).tx;
			float ny = src.get(insertIdx).ty;
			float r = (float) (length / src.get(insertIdx).distance);
			float x = (float) Math.ceil(rtl ? px - (px - nx) * r : px + (nx - px) * r);
			float y = (float) Math.ceil(py + (ny - py) * r);
			int idx = findNextPrevPointIdx(x, y, points, !rtl);
			points.add(idx, new GeometryWayPoint(idx, x, y));
			int color = palette.get(i + 1);
			colorsArray[idx] = color;
		}

		for (int colorIdx = 1; colorIdx < colorsArray.length; colorIdx++) {
			if (colorsArray[colorIdx] == 0) {
				colorsArray[colorIdx] = colorsArray[colorIdx - 1];
			}
		}

		fillDistancesAngles(points);

		for (int color : colorsArray) {
			colors.add(color);
		}
	}

	@NonNull
	private List<Integer> fetchColorsOfRouteInfoAttribute() {
		RenderingRulesStorage renderer = getValidRenderer();
		RenderingRuleSearchRequest request = new RenderingRuleSearchRequest(renderer);

		applyProfileFiltersToRequest(request, renderer);

		if (request.searchRenderingAttribute(routeInfoAttribute)) {
			return fetchColorsInternal(renderer, request);
		}
		return Collections.emptyList();
	}

	private RenderingRulesStorage getValidRenderer() {
		OsmandApplication app = view.getApplication();
		RenderingRulesStorage currentRenderer = app.getRendererRegistry().getCurrentSelectedRenderer();
		RenderingRulesStorage defaultRenderer = app.getRendererRegistry().defaultRender();
		if (currentRenderer == null) {
			return defaultRenderer;
		}
		for (String attributeName : currentRenderer.getRenderingAttributeNames()) {
			if (attributeName.startsWith(RouteStatisticsHelper.ROUTE_INFO_PREFIX)) {
				return currentRenderer;
			}
		}
		return defaultRenderer;
	}

	private void applyProfileFiltersToRequest(RenderingRuleSearchRequest request, RenderingRulesStorage renderer) {
		request.setBooleanFilter(renderer.PROPS.R_NIGHT_MODE, nightMode);

		SharedPreferences preferences = (SharedPreferences) view.getSettings().getProfilePreferences(getAppMode());
		for (RenderingRuleProperty property : renderer.PROPS.getCustomRules()) {
			String preferenceKey = RENDERER_PREFERENCE_PREFIX + property.getAttrName();
			if (property.isString()) {
				request.setStringFilter(property, preferences.getString(preferenceKey, null));
			} else if (property.isBoolean()) {
				request.setBooleanFilter(property, preferences.getBoolean(preferenceKey, false));
			} else if (property.isInt() || property.isColor()) {
				request.setIntFilter(property, preferences.getInt(preferenceKey, 0));
			} else if (property.isFloat()) {
				request.setFloatFilter(property, preferences.getFloat(preferenceKey, 0));
			}
		}
	}

	private List<Integer> fetchColorsInternal(RenderingRulesStorage renderer, RenderingRuleSearchRequest request) {
		List<RenderingRule> renderingRules = renderer.getRenderingAttributeRule(routeInfoAttribute)
				.getIfElseChildren();
		if (Algorithms.isEmpty(renderingRules)) {
			return Collections.emptyList();
		}

		List<Integer> colors = new ArrayList<>();
		for (RenderingRule rule : renderingRules) {
			setTagValueAdditional(renderer, request, rule);
			if (request.searchRenderingAttribute(routeInfoAttribute)) {
				String stringColor = request.getColorStringPropertyValue(renderer.PROPS.R_ATTR_COLOR_VALUE);
				int color = 0;
				try {
					color = Algorithms.parseColor(stringColor);
				} catch (IllegalArgumentException e) {
					log.error(e);
				}
				if (color != 0 && !colors.contains(color)) {
					colors.add(color);
				}
			}
		}

		return colors;
	}

	private void setTagValueAdditional(RenderingRulesStorage renderer, RenderingRuleSearchRequest request,
	                                   RenderingRule rule) {
		RenderingRuleProperty[] properties = rule.getProperties();
		if (properties == null) {
			return;
		}

		for (RenderingRuleProperty property : properties) {
			String attribute = property.getAttrName();
			if (TAG.equals(attribute)) {
				request.setStringFilter(renderer.PROPS.R_TAG, rule.getStringPropertyValue(property.getAttrName()));
			} else if (VALUE.equals(attribute)) {
				request.setStringFilter(renderer.PROPS.R_VALUE, rule.getStringPropertyValue(property.getAttrName()));
			} else if (ADDITIONAL.equals(attribute)) {
				request.setStringFilter(renderer.PROPS.R_ADDITIONAL, rule.getStringPropertyValue(property.getAttrName()));
			}
		}
	}

	private void fillDistancesAngles(List<GeometryWayPoint> points) {
		points.get(0).angle = 0;
		points.get(0).distance = 0;
		points.get(0).index = 0;
		for (int i = 1; i < points.size(); i++) {
			float x = points.get(i).tx;
			float y = points.get(i).ty;
			points.get(i).index = i;
			float px = points.get(i - 1).tx;
			float py = points.get(i - 1).ty;
			double angleRad = Math.atan2(y - py, x - px);
			points.get(i).angle = (angleRad * 180 / Math.PI) + 90f;
			points.get(i).distance = Math.sqrt((y - py) * (y - py) + (x - px) * (x - px));
		}
	}

	private int findNextPrevPointIdx(float x, float y, List<GeometryWayPoint> points, boolean next) {
		for (int i = 0; i < points.size(); i++) {
			if (next && points.get(i).tx >= x || !next && points.get(i).tx <= x) {
				if (points.get(i).ty == y) {
					return i;
				} else if (points.get(i).ty <= y) {
					return i;
				}
			}
		}
		return points.size() - 1;
	}

	private int getPreviewColor(int[] colors, int index, double coeff) {
		if (index == 0) {
			return colors[0];
		} else if (index > 0 && index < colors.length) {
			return ColorPalette.Companion.getIntermediateColor(colors[index - 1], colors[index], coeff);
		} else if (index == colors.length) {
			return colors[index - 1];
		}
		return 0;
	}

	private void drawTurnArrows(@NonNull Canvas canvas,
	                            @NonNull List<List<PointF>> arrows,
	                            @NonNull List<GeometryWayPoint> points) {
		Path path = new Path();
		Matrix matrix = new Matrix();
		int styleTurnArrowColor = attrs.paint3.getColor();
		float routeWidth = previewLineGeometry.getDefaultWayStyle().getWidth(0);
		if (routeWidth != 0) {
			attrs.paint3.setStrokeWidth(routeWidth / 2);
			//attrs.paint3.setStrokeWidth(Math.min(previewLineGeometry.getContext().getAttrs().defaultWidth3, routeWidth / 2));
		}

		for (List<PointF> arrow : arrows) {
			int arrowColor = getTurnArrowColor(arrow, points);
			setTurnArrowPaintsColor(arrowColor);

			path.reset();
			for (PointF point : arrow) {
				if (path.isEmpty()) {
					path.moveTo(point.x, point.y);
				} else {
					path.lineTo(point.x, point.y);
				}
			}
			canvas.drawPath(path, attrs.paint3);

			PointF penultimatePoint = arrow.get(arrow.size() - 2);
			PointF lastPoint = arrow.get(arrow.size() - 1);
			drawTurnArrow(canvas, matrix, lastPoint.x, lastPoint.y, penultimatePoint.x, penultimatePoint.y);
		}

		setTurnArrowPaintsColor(styleTurnArrowColor);
	}

	@ColorInt
	private int getTurnArrowColor(@NonNull List<PointF> arrowPointsX,
	                              @NonNull List<GeometryWayPoint> points) {
		boolean rtl = AndroidUtils.isLayoutRtl(getContext());

		int lightColor = ColorUtilities.getSecondaryIconColor(getContext(), false);
		int darkColor = ColorUtilities.getSecondaryIconColor(getContext(), true);

		int originalLowDistanceCount = 0;
		int lightLowDistanceCount = 0;
		int darkLowDistanceCount = 0;

		int arrowPointIndex = 0;
		for (int i = 0; i < points.size() - 1 && arrowPointIndex < arrowPointsX.size(); i++) {
			float startX = points.get(i).tx;
			float startY = points.get(i).ty;
			float endX = points.get(i + 1).tx;
			float endY = points.get(i + 1).ty;

			PointF arrowPoint = arrowPointsX.get(arrowPointIndex);

			boolean horizontalSegment = !rtl
					&& startY == arrowPoint.y
					&& endY == arrowPoint.y
					&& startX <= arrowPoint.x
					&& arrowPoint.x < endX;
			boolean horizontalSegmentRtl = rtl
					&& startY == arrowPoint.y
					&& endY == arrowPoint.y
					&& startX >= arrowPoint.x
					&& arrowPoint.x > endX;
			boolean verticalSegment = startX == arrowPoint.x
					&& endX == arrowPoint.x
					&& startY >= arrowPoint.y
					&& arrowPoint.y > endY;
			float offset;
			if (horizontalSegment) {
				offset = (arrowPoint.x - startX) / (endX - startX);
			} else if (horizontalSegmentRtl) {
				offset = (startX - arrowPoint.x) / (startX - endX);
			} else if (verticalSegment) {
				offset = (arrowPoint.y - startY) / (endY - startY);
			} else {
				continue;
			}

			int lineColor;
			GeometryWayStyle<?> style = points.get(i).style;
			if (style instanceof GeometryGradientWayStyle<?>) {
				GeometryGradientWayStyle<?> gradientStyle = (GeometryGradientWayStyle<?>) (style);
				int startColor = gradientStyle.currColor;
				int endColor = gradientStyle.nextColor;
				lineColor = ColorPalette.Companion.getIntermediateColor(startColor, endColor, offset);
			} else {
				 lineColor = style.getColor(getRouteLineColor());
			}

			if (ColorUtilities.getColorsSquareDistance(customTurnArrowColor, lineColor) < MIN_COLOR_SQUARE_DISTANCE) {
				originalLowDistanceCount++;
			}
			if (ColorUtilities.getColorsSquareDistance(lightColor, lineColor) < MIN_COLOR_SQUARE_DISTANCE) {
				lightLowDistanceCount++;
			}
			if (ColorUtilities.getColorsSquareDistance(darkColor, lineColor) < MIN_COLOR_SQUARE_DISTANCE) {
				darkLowDistanceCount++;
			}

			arrowPointIndex++;
		}

		if (originalLowDistanceCount < points.size() / 2f
				|| originalLowDistanceCount < Math.min(lightLowDistanceCount, darkLowDistanceCount)) {
			return customTurnArrowColor;
		}

		return lightLowDistanceCount <= darkLowDistanceCount ? lightColor : darkColor;
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}
}