package net.osmand.plus.views.layers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.SelectedGpxPoint;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.wikivoyage.data.TravelGpx;
import net.osmand.plus.wikivoyage.data.TravelHelper;
import net.osmand.util.Algorithms;

import java.util.List;

public class TravelSelectionLayer extends OsmandMapLayer implements IContextMenuProvider {

	private OsmandApplication app;

	public TravelSelectionLayer(@NonNull Context ctx) {
		super(ctx);
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);
		app = view.getApplication();
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof Pair) {
			Pair<?, ?> pair = (Pair<?, ?>) o;
			if (pair.first instanceof TravelGpx && pair.second instanceof SelectedGpxPoint) {
				WptPt point = ((SelectedGpxPoint) pair.second).getSelectedPoint();
				return new LatLon(point.getLat(), point.getLon());
			}
		}
		return null;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof Pair) {
			Pair<?, ?> pair = (Pair<?, ?>) o;
			if (pair.first instanceof TravelGpx && pair.second instanceof SelectedGpxPoint) {
				TravelGpx travelGpx = (TravelGpx) ((Pair<?, ?>) o).first;
				String name = Algorithms.isEmpty(travelGpx.getDescription()) ? travelGpx.getTitle() : travelGpx.getDescription();
				return new PointDescription(PointDescription.POINT_TYPE_GPX, name);
			}
		}
		return null;
	}

	@Override
	public boolean showMenuAction(@Nullable Object object) {
		MapActivity mapActivity = view.getMapActivity();
		if (mapActivity != null) {
			if (object instanceof Pair) {
				Pair<?, ?> pair = (Pair<?, ?>) object;
				if (pair.first instanceof TravelGpx && pair.second instanceof SelectedGpxPoint) {
					TravelGpx travelGpx = (TravelGpx) pair.first;
					SelectedGpxPoint selectedGpxPoint = (SelectedGpxPoint) pair.second;

					WptPt wptPt = selectedGpxPoint.getSelectedPoint();
					TravelHelper travelHelper = app.getTravelHelper();
					travelHelper.openTrackMenu(travelGpx, mapActivity, travelGpx.getRouteId(), new LatLon(wptPt.getLat(), wptPt.getLon()));
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {

	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o, boolean unknownLocation, boolean excludeUntouchableObjects) {

	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}
}
