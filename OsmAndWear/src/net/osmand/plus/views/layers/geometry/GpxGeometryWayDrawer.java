package net.osmand.plus.views.layers.geometry;

import static net.osmand.shared.routing.Gpx3DWallColorType.NONE;

import android.graphics.Canvas;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.jni.QListFColorARGB;
import net.osmand.core.jni.VectorLinesCollection;
import net.osmand.plus.track.Track3DStyle;
import net.osmand.shared.routing.ColoringType;
import net.osmand.shared.routing.Gpx3DWallColorType;

import java.util.List;

public class GpxGeometryWayDrawer extends MultiColoringGeometryWayDrawer<GpxGeometryWayContext> {

	@Nullable
	protected ColoringType outlineColoringType;

	@Nullable
	private Track3DStyle track3DStyle;

	public GpxGeometryWayDrawer(GpxGeometryWayContext context) {
		super(context);
		outlineColoringType = context.getDefaultColoringType();
	}

	public void setOutlineColoringType(@Nullable ColoringType outlineColoringType) {
		this.outlineColoringType = outlineColoringType;
	}

	public void setTrack3DStyle(@Nullable Track3DStyle track3DStyle) {
		this.track3DStyle = track3DStyle;
	}

	@Override
	public void drawPath(@NonNull VectorLinesCollection collection, int baseOrder, boolean shouldDrawArrows,
	                     @NonNull List<DrawPathData31> pathsData) {
		if (coloringType.isGradient() || track3DStyle != null && track3DStyle.getVisualizationType().is3dType()) {
			drawGradient(collection, baseOrder, shouldDrawArrows, pathsData);
		} else if (coloringType.isDefault() || coloringType.isCustomColor() || coloringType.isTrackSolid() || coloringType.isRouteInfoAttribute()) {
			super.drawPath(collection, baseOrder, shouldDrawArrows, pathsData);
		}
	}

	@Override
	public void drawPath(Canvas canvas, DrawPathData pathData) {
		if (coloringType.isRouteInfoAttribute()) {
			drawCustomSolid(canvas, pathData);
		}
	}

	@NonNull
	@Override
	protected Pair<QListFColorARGB, QListFColorARGB> getColorizationMappings(@NonNull List<DrawPathData31> pathsData) {
		Gpx3DWallColorType wallColorType = track3DStyle != null && track3DStyle.getVisualizationType().is3dType() ? track3DStyle.getWallColorType() : NONE;
		ColoringType outlineColoringType = ColoringType.Companion.valueOf(wallColorType);

		QListFColorARGB mapping = getColorizationMapping(pathsData, coloringType, false);
		QListFColorARGB outlineMapping = outlineColoringType != null ? getColorizationMapping(pathsData, outlineColoringType, true) : null;

		return new Pair<>(mapping, outlineMapping);
	}
}