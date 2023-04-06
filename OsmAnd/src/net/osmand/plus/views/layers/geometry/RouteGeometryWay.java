package net.osmand.plus.views.layers.geometry;

import android.graphics.Paint;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.PointI;
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

import java.util.ArrayList;
import java.util.List;

public class RouteGeometryWay extends
		MultiColoringGeometryWay<RouteGeometryWayContext, MultiColoringGeometryWayDrawer<RouteGeometryWayContext>> {

	private final RoutingHelper helper;
	private RouteCalculationResult route;

	private Integer customDirectionArrowColor;

	//OpenGL
	private boolean drawDirectionArrows = true;
	private VectorLinesCollection actionLinesCollection;

	public RouteGeometryWay(RouteGeometryWayContext context) {
		super(context, new MultiColoringGeometryWayDrawer<>(context));
		this.helper = context.getApp().getRoutingHelper();
	}

	public void setRouteStyleParams(int pathColor,
	                                float pathWidth,
	                                boolean drawDirectionArrows,
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
		if (this.drawDirectionArrows != drawDirectionArrows) {
			resetArrowsProvider();
		}
		updatePaints(pathWidth, routeColoringType);
		getDrawer().setColoringType(routeColoringType);

		this.customColor = pathColor;
		this.customWidth = pathWidth;
		this.drawDirectionArrows = drawDirectionArrows;
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

	public boolean updateRoute(@NonNull RotatedTileBox tb, @NonNull RouteCalculationResult route) {
		if (coloringChanged || tb.getMapDensity() != getMapDensity() || this.route != route) {
			this.route = route;
			coloringChanged = false;
			List<Location> locations = route.getImmutableAllLocations();
			if (coloringType.isGradient()) {
				updateGradientWay(tb, locations);
			} else if (coloringType.isRouteInfoAttribute()) {
				updateSolidMultiColorRoute(tb, locations, route.getOriginalRoute());
			} else {
				updateWay(locations, tb);
			}
			return true;
		}
		return false;
	}

	@NonNull
	@Override
	public GeometryWayStyle<?> getDefaultWayStyle() {
		return coloringType.isGradient()
				? getGradientWayStyle()
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

	@Override
	protected boolean shouldDrawArrows() {
		return drawDirectionArrows;
	}

	public void clearRoute() {
		if (route != null) {
			route = null;
			clearWay();
		}
	}

	public void resetSymbolProviders() {
		super.resetSymbolProviders();
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			if (actionLinesCollection != null) {
				mapRenderer.removeSymbolsProvider(actionLinesCollection);
				actionLinesCollection = null;
			}
		}
	}

	public void resetActionLines() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			if (actionLinesCollection != null) {
				mapRenderer.removeSymbolsProvider(actionLinesCollection);
				actionLinesCollection = null;
			}
		}
	}

	private List<List<Location>> getActionArrows(List<Location> locations) {
		List<List<Location>> ll = new ArrayList<>();
		ll.add(new ArrayList<>());
		int index = 0;
		for (int i = 0; i < locations.size(); i++) {
			Location loc = locations.get(i);
			if (loc != null) {
				ll.get(index).add(loc);
			} else if (i < locations.size() - 1) {
				index++;
				ll.add(new ArrayList<>());
			}
		}
		return ll;
	}

	public void buildActionArrows(List<Location> actionPoints, int customTurnArrowColor) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null) {
			return;
		}
		int baseOrder = this.baseOrder - 1000;
		List<List<Location>> actionArrows = getActionArrows(actionPoints);
		if (!actionArrows.isEmpty()) {
			int lineIdx = 0;
			if (actionLinesCollection == null) {
				actionLinesCollection = new VectorLinesCollection();
			}
			long initialLinesCount = actionLinesCollection.getLines().size();
			for (List<Location> line : actionArrows) {
				QVectorPointI points = new QVectorPointI();
				for (Location point : line) {
					int x = MapUtils.get31TileNumberX(point.getLongitude());
					int y = MapUtils.get31TileNumberY(point.getLatitude());
					points.add(new PointI(x, y));
				}
				if (lineIdx < initialLinesCount) {
					VectorLine vectorLine = actionLinesCollection.getLines().get(lineIdx);
					vectorLine.setPoints(points);
					vectorLine.setIsHidden(false);
					lineIdx++;
				} else {
					VectorLineBuilder vectorLineBuilder = new VectorLineBuilder();
					vectorLineBuilder.setBaseOrder(baseOrder--)
							.setIsHidden(false)
							.setLineId((int) actionLinesCollection.getLines().size())
							.setLineWidth(customWidth)
							.setPoints(points)
							.setEndCapStyle(VectorLine.EndCapStyle.ARROW.ordinal())
							.setFillColor(NativeUtilities.createFColorARGB(customTurnArrowColor));
					vectorLineBuilder.buildAndAddToCollection(actionLinesCollection);
				}
			}
			while (lineIdx < initialLinesCount) {
				actionLinesCollection.getLines().get(lineIdx).setIsHidden(true);
				lineIdx++;
			}
			mapRenderer.addSymbolsProvider(actionLinesCollection);
		}
	}
}