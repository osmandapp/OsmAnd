package net.osmand.plus.views.layers.geometry;

import android.graphics.Canvas;
import android.graphics.Paint;

import net.osmand.Location;
import net.osmand.core.jni.FColorARGB;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QListFColorARGB;
import net.osmand.core.jni.QListVectorLine;
import net.osmand.core.jni.QVectorPointI;
import net.osmand.core.jni.VectorLine;
import net.osmand.core.jni.VectorLineBuilder;
import net.osmand.core.jni.VectorLinesCollection;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.List;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class RouteGeometryWay extends
		MultiColoringGeometryWay<RouteGeometryWayContext, MultiColoringGeometryWayDrawer<RouteGeometryWayContext>> {

	private final RoutingHelper helper;
	private RouteCalculationResult route;

	private Integer customDirectionArrowColor;

	public RouteGeometryWay(RouteGeometryWayContext context) {
		super(context, new MultiColoringGeometryWayDrawer<>(context));
		this.helper = context.getApp().getRoutingHelper();
	}

	public void setRouteStyleParams(int pathColor,
	                                float pathWidth,
	                                @Nullable @ColorInt Integer directionArrowColor,
	                                @NonNull ColoringType routeColoringType,
	                                @Nullable String routeInfoAttribute) {
		this.coloringChanged = this.coloringType != routeColoringType
				|| routeColoringType == ColoringType.ATTRIBUTE
				&& !Algorithms.objectEquals(this.routeInfoAttribute, routeInfoAttribute);

		boolean widthChanged = !Algorithms.objectEquals(customWidth, pathWidth);
		if (widthChanged) {
			updateStylesWidth(pathWidth);
		}
		updatePaints(pathWidth, routeColoringType);
		getDrawer().setColoringType(routeColoringType);

		this.customColor = pathColor;
		this.customWidth = pathWidth;
		this.customDirectionArrowColor = directionArrowColor;
		this.coloringType = routeColoringType;
		this.routeInfoAttribute = routeInfoAttribute;
	}

	@Override
	protected void updatePaints(@Nullable Float width, @NonNull ColoringType routeColoringType) {
		super.updatePaints(width, routeColoringType);
		Paint.Cap cap = routeColoringType.isGradient() || routeColoringType.isRouteInfoAttribute() ?
				Paint.Cap.ROUND : getContext().getAttrs().paint.getStrokeCap();
		getContext().getAttrs().customColorPaint.setStrokeCap(cap);
	}

	public void updateRoute(@NonNull RotatedTileBox tb, @NonNull RouteCalculationResult route) {
		if (coloringChanged || tb.getMapDensity() != getMapDensity() || this.route != route) {
			this.route = route;
			coloringChanged = false;
			List<Location> locations = route.getImmutableAllLocations();

			if (coloringType.isGradient()) {
				updateGradientRoute(tb, locations);
			} else if (coloringType.isRouteInfoAttribute()) {
				updateSolidMultiColorRoute(tb, locations, route.getOriginalRoute());
			} else {
				updateWay(locations, tb);
			}
		}
	}

	@NonNull
	@Override
	public GeometryWayStyle<?> getDefaultWayStyle() {
		return coloringType.isGradient()
				? super.getGradientWayStyle()
				: getArrowWayStyle(customColor);
	}

	@NonNull
	@Override
	public GeometrySolidWayStyle<RouteGeometryWayContext> getSolidWayStyle(int lineColor) {
		return getArrowWayStyle(lineColor);
	}

	@NonNull
	private GeometrySolidWayStyle<RouteGeometryWayContext> getArrowWayStyle(int lineColor) {
		int directionArrowColor = customDirectionArrowColor != null
				? customDirectionArrowColor
				: getContext().getPaintIcon().getColor();
		return new GeometrySolidWayStyle<>(getContext(), lineColor, customWidth, directionArrowColor, true);
	}

	@Override
	protected PathGeometryZoom getGeometryZoom(RotatedTileBox tb) {
		if (coloringType.isGradient()) {
			int zoom = tb.getZoom();
			PathGeometryZoom zm = zooms.get(zoom);
			if (zm == null) {
				zm = new GradientPathGeometryZoom(getLocationProvider(), tb, true);
				zooms.put(zoom, zm);
			}
			return zm;
		}
		return super.getGeometryZoom(tb);
	}

	@Override
	public Location getNextVisiblePoint() {
		return helper.getRoute().getCurrentStraightAnglePoint();
	}

	public void clearRoute() {
		if (route != null) {
			route = null;
			clearWay();
		}
	}

	@Override
	public void drawRouteSegment(RotatedTileBox tb, Canvas canvas, List<Float> tx, List<Float> ty,
								 List<Double> angles, List<Double> distances, double distToFinish,
								 List<GeometryWayStyle<?>> styles) {

		if (openGlRendering) {

			QListVectorLine lines = new QListVectorLine();
			if (collection != null) {
				lines = collection.getLines();
			}

			QVectorPointI points = new QVectorPointI();
			List<Location> ll = helper.getRoute().getRouteLocations();
			for (Location l : ll) {
				int x = MapUtils.get31TileNumberX(l.getLongitude());
				int y = MapUtils.get31TileNumberY(l.getLatitude());
				points.add(new PointI(x, y));
			}
			QListFColorARGB colors = getColorizationMapping();

			if (lines.isEmpty()) {
				int bsOrder = baseOrder;
				if (collection == null) {
					collection = new VectorLinesCollection();
				}

				if (kOutlineColor == null) {
					kOutlineColor = new FColorARGB(150.0f/255.0f, 0.0f, 0.0f, 0.0f);
				}


				int colorizationScheme = getColorizationScheme();

				// Add outline for colorized lines
				if (!colors.isEmpty()) {
					VectorLineBuilder outlineBuilder = new VectorLineBuilder();;
					outlineBuilder.setBaseOrder(bsOrder--)
							.setIsHidden(points.size() < 2)
							.setLineId(kOutlineId)
							.setLineWidth(customWidth + kOutlineWidth)
							.setOutlineWidth(kOutlineWidth)
							.setPoints(points)
							.setFillColor(kOutlineColor)
							.setApproximationEnabled(false);

					outlineBuilder.buildAndAddToCollection(collection);
				}

				VectorLineBuilder vectorLineBuilder = new VectorLineBuilder();
				double width = customWidth;
				int color = customColor;
				vectorLineBuilder
						.setPoints(points)
						.setIsHidden(false)
						.setLineId(1)
						.setLineWidth(width)
						.setFillColor(NativeUtilities.createFColorARGB(color))
						.setBaseOrder(bsOrder--);

				if (!colors.isEmpty()) {
					vectorLineBuilder.setColorizationMapping(colors);
					vectorLineBuilder.setColorizationScheme(colorizationScheme);
				}
				vectorLineBuilder.buildAndAddToCollection(collection);

				openGlView.addSymbolsProvider(collection);
			} else {
				//update route line during navigation
				for (int i = 0; i < lines.size(); i++) {
					VectorLine line = lines.get(i);
					if (line.getPoints().size() != points.size()) {
						line.setPoints(points);
						if (!colors.isEmpty() && line.getOutlineWidth() == 0.0d) {
							line.setColorizationMapping(colors);
						}
					}
				}
			}
		} else {
			super.drawRouteSegment(tb, canvas, tx, ty, angles, distances, distToFinish, styles);
		}
	}

	public QListFColorARGB getColorizationMapping() {
		//OpenGL
		QListFColorARGB colors = new QListFColorARGB();
		if (styleMap != null && styleMap.size() > 0) {
			int lastColor = 0;
			for (int i = 0; i < styleMap.size(); i++) {
				GeometryWayStyle<?> style = styleMap.get(i);
				int color = 0;
				if (style != null) {
					if (style instanceof GeometryGradientWayStyle) {
						color = ((GeometryGradientWayStyle) style).currColor;
						lastColor = ((GeometryGradientWayStyle) style).nextColor;
					} else {
						color = style.getColor() == null ? 0 : style.getColor();
						lastColor = color;
					}
				}
				colors.add(NativeUtilities.createFColorARGB(color));
			}
			colors.add(NativeUtilities.createFColorARGB(lastColor));
		}
		return colors;
	}

	public int getColorizationScheme() {
		//OpenGL
		if (styleMap != null) {
			for (int i = 0; i < styleMap.size(); i++) {
				GeometryWayStyle<?> style = styleMap.get(i);
				if (style != null) {
					return style.getColorizationScheme();
				}
			}
		}
		return GeometryWayStyle.COLORIZATION_NONE;
	}
}