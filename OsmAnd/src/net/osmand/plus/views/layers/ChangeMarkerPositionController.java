package net.osmand.plus.views.layers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dialogs.selectlocation.ILocationSelectionHandler;
import net.osmand.plus.dialogs.selectlocation.SelectLocationController;
import net.osmand.plus.dialogs.selectlocation.extractor.CenterMapLatLonExtractor;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.ContextMenuLayer.IMoveObjectProvider;

public class ChangeMarkerPositionController implements ILocationSelectionHandler<LatLon> {

	private final OsmandApplication app;
	private final ChangeMarkerPositionHandler handler;

	public ChangeMarkerPositionController(@NonNull OsmandApplication app,
	                                      @NonNull ChangeMarkerPositionHandler handler) {
		this.app = app;
		this.handler = handler;
	}

	@Nullable
	@Override
	public Object getCenterPointIcon(@NonNull MapActivity mapActivity) {
		Object o = handler.getChangeMarkerPositionObject();
		IContextMenuProvider provider = handler.getSelectedObjectContextMenuProvider();
		if (o != null && provider instanceof IMoveObjectProvider l) {
			return l.getMoveableObjectIcon(o);
		}
		return null;
	}

	@Nullable
	@Override
	public String getCenterPointLabel(@NonNull MapActivity mapActivity) {
		Object o = handler.getChangeMarkerPositionObject();
		IContextMenuProvider provider = handler.getSelectedObjectContextMenuProvider();
		if (o != null && provider instanceof IMoveObjectProvider l) {
			return l.getMoveableObjectLabel(o);
		}
		return null;
	}

	@Override
	public void onLocationSelected(@NonNull MapActivity mapActivity, @NonNull LatLon location) {
		handler.applyNewMarkerPosition(location);
	}

	@Override
	public void onScreenClosed(@NonNull MapActivity mapActivity, boolean selected) {
		if (!selected) {
			handler.cancelMovingMarker();
		}
	}

	@NonNull
	@Override
	public String getDialogTitle(@NonNull MapActivity mapActivity) {
		return app.getString(R.string.change_markers_position);
	}

	public static void showDialog(@NonNull MapActivity mapActivity,
	                              @NonNull ChangeMarkerPositionHandler handler) {
		OsmandApplication app = mapActivity.getApp();
		ChangeMarkerPositionController controller = new ChangeMarkerPositionController(app, handler);
		SelectLocationController.showDialog(mapActivity, new CenterMapLatLonExtractor(), controller);
	}
}
