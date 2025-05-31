package net.osmand.plus.dialogs;

import android.graphics.PointF;

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

public class SelectLocationController extends BaseDialogController implements IMapLocationListener {

	private static final String PROCESS_ID = "select_location_on_map";

	private ILocationSelectionHandler handler;
	private boolean applyChanges = false;
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
		LatLon latLon = getMapTargetCoordinates(app);
		int format = app.getSettings().COORDINATES_FORMAT.get();
		return OsmAndFormatter.getFormattedCoordinates(latLon.getLatitude(), latLon.getLongitude(), format);
	}

	public void onApplySelection() {
		applyChanges = true;
	}

	public void onResume() {
		updateMapLocationListener(true);
		updateMapPositionShiftedX(landscape);
	}

	public void onPause() {
		updateMapLocationListener(false);
		updateMapPositionShiftedX(false);
	}

	public void onDestroy(@Nullable FragmentActivity activity) {
		if (finishProcessIfNeeded(activity) && applyChanges) {
			applyChanges = false;
			onLocationSelected();
		}
		onScreenClosed();
	}

	private void onLocationSelected() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			handler.onApplySelection(mapActivity);
		}
	}

	private void onScreenClosed() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			handler.onScreenClosed(mapActivity);
		}
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
		return app.getOsmandMap().getMapView().getMapActivity();
	}

	@NonNull
	public static LatLon getMapTargetCoordinates(@NonNull OsmandApplication app) {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		MapRendererView mapRenderer = mapView.getMapRenderer();
		RotatedTileBox tileBox = mapView.getRotatedTileBox();

		int centerX = tileBox.getCenterPixelX();
		int centerY = tileBox.getCenterPixelY();
		return NativeUtilities.getLatLonFromElevatedPixel(mapRenderer, tileBox, centerX, centerY);
	}

	@NonNull
	public static PointF getCenterPixelPoint(@NonNull OsmandApplication app) {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		RotatedTileBox tileBox = mapView.getRotatedTileBox();
		return new PointF(tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
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
