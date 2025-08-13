package net.osmand.plus.views.layers;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.SelectedGpxPoint;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.wikivoyage.data.TravelGpx;
import net.osmand.plus.wikivoyage.data.TravelHelper;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;

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
				if (!Algorithms.isEmpty(travelGpx.getTitle())) {
					return new PointDescription(PointDescription.POINT_TYPE_GPX, travelGpx.getTitle());
				} else if (!Algorithms.isEmpty(travelGpx.getDescription())) {
					return new PointDescription(PointDescription.POINT_TYPE_GPX, travelGpx.getDescription());
				} else {
					return new PointDescription(PointDescription.POINT_TYPE_GPX, travelGpx.getRouteId()); // nullable
				}
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
					travelHelper.openTrackMenu(travelGpx, mapActivity, travelGpx.getGpxFileName(),
							new LatLon(wptPt.getLat(), wptPt.getLon()), false);
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
	public void collectObjectsFromPoint(@NonNull MapSelectionResult result,
			boolean unknownLocation, boolean excludeUntouchableObjects) {

	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}
}
