package net.osmand.plus.views.layers.geometry;

import android.graphics.Canvas;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.jni.QListFColorARGB;
import net.osmand.plus.routing.ColoringType;

import java.util.List;

public class GpxGeometryWayDrawer extends MultiColoringGeometryWayDrawer<GpxGeometryWayContext> {

	@Nullable
	protected ColoringType outlineColoringType;

	public GpxGeometryWayDrawer(GpxGeometryWayContext context) {
		super(context);
		outlineColoringType = context.getDefaultColoringType();
	}

	public void setOutlineColoringType(@Nullable ColoringType outlineColoringType) {
		this.outlineColoringType = outlineColoringType;
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
		QListFColorARGB mapping = getColorizationMapping(pathsData, coloringType, false);
		QListFColorARGB outlineMapping = outlineColoringType != null ? getColorizationMapping(pathsData, outlineColoringType, true) : null;

		return new Pair<>(mapping, outlineMapping);
	}
}