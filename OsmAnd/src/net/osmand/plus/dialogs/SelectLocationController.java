package net.osmand.plus.dialogs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.core.android.MapRendererView;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.IMapLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.interfaces.dialog.IDialog;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.MapDisplayPositionManager;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.OsmandMap;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.SelectLocationLayer;

public class SelectLocationController extends BaseDialogController implements IMapLocationListener {

	private static final String PROCESS_ID = "select_location_on_map";

	private ILocationSelectionHandler handler;
	private boolean landscape;

	public SelectLocationController(@NonNull OsmandApplication app,
	                                @NonNull ILocationSelectionHandler handler) {
		super(app);
		setLocationSelectionHandler(handler);
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	public void bindDialog(@NonNull MapActivity mapActivity, @NonNull IDialog dialog) {
		registerDialog(dialog);
		landscape = !AndroidUiHelper.isOrientationPortrait(mapActivity);
	}

	public void setLocationSelectionHandler(@NonNull ILocationSelectionHandler handler) {
		this.handler = handler;
	}

	@NonNull
	public String getDialogTitle() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return handler.getDialogTitle(mapActivity);
		}
		return "";
	}

	@NonNull
	public String getFormattedCoordinates() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			LatLon latLon = getMapTargetCoordinates(mapActivity);
			int format = app.getSettings().COORDINATES_FORMAT.get();
			return OsmAndFormatter.getFormattedCoordinates(latLon.getLatitude(), latLon.getLongitude(), format);
		}
		return "";
	}

	public void onApplySelection() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			LatLon targetLatLon = getMapTargetCoordinates(mapActivity);
			handler.onLocationSelected(mapActivity, targetLatLon);
		}
	}

	public void onResume() {
		updateMapLocationListener(true);
		updateMapPositionShiftedX(landscape);
	}

	public void onPause() {
		updateMapLocationListener(false);
		updateMapPositionShiftedX(false);
	}

	private void updateMapLocationListener(boolean register) {
		OsmandMap osmandMap = app.getOsmandMap();
		OsmandMapTileView mapView = osmandMap.getMapView();
		if (register) {
			mapView.addMapLocationListener(this);
		} else {
			mapView.removeMapLocationListener(this);
		}
	}

	private void updateMapPositionShiftedX(boolean shifted) {
		MapViewTrackingUtilities mapViewTrackingUtilities = app.getMapViewTrackingUtilities();
		MapDisplayPositionManager positionManager = mapViewTrackingUtilities.getMapDisplayPositionManager();
		positionManager.setMapPositionShiftedX(shifted);
	}

	@Override
	public void locationChanged(double newLatitude, double newLongitude, Object source) {
		dialogManager.askRefreshDialogCompletely(getProcessId());
	}

	@Nullable
	public Object getCenterPointIcon() {
		MapActivity mapActivity = getMapActivity();
		if (handler != null && mapActivity != null) {
			return handler.getCenterPointIcon(mapActivity);
		}
		return null;
	}

	@Nullable
	private MapActivity getMapActivity() {
		SelectLocationLayer layer = app.getOsmandMap().getMapLayers().getSelectLocationLayer();
		return layer != null ? layer.getMapActivity() : null;
	}

	@NonNull
	public static LatLon getMapTargetCoordinates(@NonNull FragmentActivity activity) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		MapRendererView mapRenderer = mapView.getMapRenderer();

		RotatedTileBox tileBox = mapView.getRotatedTileBox();
		int centerX = tileBox.getCenterPixelX();
		int centerY = tileBox.getCenterPixelY();
		return NativeUtilities.getLatLonFromElevatedPixel(mapRenderer, tileBox, centerX, centerY);
	}

	@Nullable
	public static SelectLocationController getExistedInstance(@NonNull OsmandApplication app) {
		DialogManager dialogManager = app.getDialogManager();
		return (SelectLocationController) dialogManager.findController(PROCESS_ID);
	}

	public static void showDialog(@NonNull FragmentActivity activity,
								  @NonNull ILocationSelectionHandler handler) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		SelectLocationController controller = new SelectLocationController(app, handler);

		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, controller);

		FragmentManager manager = activity.getSupportFragmentManager();
		SelectLocationFragment.showInstance(manager);
	}
}
