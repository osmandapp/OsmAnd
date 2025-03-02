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
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.OsmandMap;
import net.osmand.plus.views.OsmandMapTileView;

public class SelectLocationController extends BaseDialogController implements IMapLocationListener {

	private static final String PROCESS_ID = "select_location_on_map";

	private LocationSelectionHandler handler;

	public SelectLocationController(@NonNull OsmandApplication app,
	                                @NonNull LocationSelectionHandler handler) {
		super(app);
		setLocationSelectionHandler(handler);
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	public void setLocationSelectionHandler(@NonNull LocationSelectionHandler handler) {
		this.handler = handler;
	}

	@NonNull
	public String getDialogTitle() {
		return handler.getDialogTitle();
	}

	@NonNull
	public String getFormattedCoordinates() {
		LatLon latLon = getMapCenterCoordinates(app);
		int format = app.getSettings().COORDINATES_FORMAT.get();
		return OsmAndFormatter.getFormattedCoordinates(latLon.getLatitude(), latLon.getLongitude(), format);
	}

	public void onApplySelection() {
		handler.onLocationSelected(getMapCenterCoordinates(app));
	}

	public void onResume() {
		updateMapLocationListener(true);
	}

	public void onPause() {
		updateMapLocationListener(false);
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

	@Override
	public void locationChanged(double newLatitude, double newLongitude, Object source) {
		dialogManager.askRefreshDialogCompletely(getProcessId());
	}

	@Nullable
	public Object getCenterPointIcon() {
		return handler != null ? handler.getCenterPointIcon() : null;
	}

	@NonNull
	public static LatLon getMapCenterCoordinates(@NonNull OsmandApplication app) {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		MapRendererView mapRenderer = mapView.getMapRenderer();
		RotatedTileBox tb = mapView.getCurrentRotatedTileBox().copy();
		int centerPixX = tb.getCenterPixelX();
		int centerPixY = tb.getCenterPixelY();
		return NativeUtilities.getLatLonFromElevatedPixel(mapRenderer, tb, centerPixX, centerPixY);
	}

	@Nullable
	public static SelectLocationController getExistedInstance(@NonNull OsmandApplication app) {
		DialogManager dialogManager = app.getDialogManager();
		return (SelectLocationController) dialogManager.findController(PROCESS_ID);
	}

	public static void showDialog(@NonNull FragmentActivity activity,
								  @NonNull LocationSelectionHandler handler) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		SelectLocationController controller = new SelectLocationController(app, handler);

		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, controller);

		FragmentManager manager = activity.getSupportFragmentManager();
		SelectLocationFragment.showInstance(manager);
	}
}
