package net.osmand.plus.configmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.OnResultCallback;
import net.osmand.core.android.MapRendererView;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.IMapLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.OsmandMap;
import net.osmand.plus.views.OsmandMapTileView;

//TODO: improve packaging and move this class to more relevant package
public class SelectLocationController extends BaseDialogController implements IMapLocationListener {

	private static final String PROCESS_ID = "select_location_on_map";

	private final OsmandSettings settings;
	private OnResultCallback<LatLon> onResultCallback;

	public SelectLocationController(@NonNull OsmandApplication app) {
		super(app);
		settings = app.getSettings();
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	public void setOnResultCallback(@NonNull OnResultCallback<LatLon> onResultCallback) {
		this.onResultCallback = onResultCallback;
	}

	@NonNull
	public String getDialogTitle() {
		//TODO: return suitable title dependent on purpose for which dialog is displayed
		return "Choose a location";
	}

	@NonNull
	public String getFormattedCoordinates() {
		LatLon latLon = getMapCenterCoordinates(app);
		int format = settings.COORDINATES_FORMAT.get();
		return OsmAndFormatter.getFormattedCoordinates(latLon.getLatitude(), latLon.getLongitude(), format);
	}

	public void onApplySelection() {
		onResultCallback.onResult(getMapCenterCoordinates(app));
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

	@NonNull
	private static LatLon getMapCenterCoordinates(@NonNull OsmandApplication app) {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		MapRendererView mapRenderer = mapView.getMapRenderer();
		RotatedTileBox tb = mapView.getCurrentRotatedTileBox().copy();
		int centerPixX = tb.getCenterPixelX();
		int centerPixY = tb.getCenterPixelY();
		return NativeUtilities.getLatLonFromElevatedPixel(mapRenderer, tb, centerPixX, centerPixY);
	}

	@Nullable
	public static SelectLocationController getExistedInstance(OsmandApplication app) {
		DialogManager dialogManager = app.getDialogManager();
		return (SelectLocationController) dialogManager.findController(PROCESS_ID);
	}

	public static void showDialog(@NonNull FragmentActivity activity,
	                              @NonNull OnResultCallback<LatLon> onResultCallback) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		SelectLocationController controller = new SelectLocationController(app);
		controller.setOnResultCallback(onResultCallback);

		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, controller);

		FragmentManager manager = activity.getSupportFragmentManager();
		SelectLocationFragment.showInstance(manager);
	}
}
