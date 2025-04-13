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
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.OsmandMap;
import net.osmand.plus.views.OsmandMapTileView;

public class SelectLocationController extends BaseDialogController implements IMapLocationListener {

	private static final String PROCESS_ID = "select_location_on_map";

	private ILocationSelectionHandler handler;

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

	public void setLocationSelectionHandler(@NonNull ILocationSelectionHandler handler) {
		this.handler = handler;
	}

	@NonNull
	public String getDialogTitle() {
		return handler.getDialogTitle();
	}

	@NonNull
	public String getFormattedCoordinates(@NonNull FragmentActivity activity) {
		LatLon latLon = getMapTargetCoordinates(activity);
		int format = app.getSettings().COORDINATES_FORMAT.get();
		return OsmAndFormatter.getFormattedCoordinates(latLon.getLatitude(), latLon.getLongitude(), format);
	}

	public void onApplySelection(@NonNull FragmentActivity activity) {
		boolean landscape = !AndroidUiHelper.isOrientationPortrait(activity);
		boolean rtl = AndroidUtils.isLayoutRtl(activity);
		app.runInUIThread(() -> notifyLocationSelected(landscape, rtl), 100);
	}

	private void notifyLocationSelected(boolean landscape, boolean rtl) {
		LatLon targetLatLon = getMapTargetCoordinates(app, landscape, rtl);
		handler.onLocationSelected(targetLatLon);
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
	public static LatLon getMapTargetCoordinates(@NonNull FragmentActivity activity) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		boolean landscape = !AndroidUiHelper.isOrientationPortrait(activity);
		boolean rtl = AndroidUtils.isLayoutRtl(activity);
		return getMapTargetCoordinates(app, landscape, rtl);
	}

	public static LatLon getMapTargetCoordinates(@NonNull OsmandApplication app, boolean landscape, boolean rtl) {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		MapRendererView mapRenderer = mapView.getMapRenderer();
		RotatedTileBox tileBox = mapView.getRotatedTileBox();

		int centerX = getTargetPixelX(tileBox, landscape, rtl);
		int centerY = tileBox.getCenterPixelY();
		return NativeUtilities.getLatLonFromElevatedPixel(mapRenderer, tileBox, centerX, centerY);
	}

	public static int getTargetPixelX(@NonNull MapActivity activity) {
		RotatedTileBox tileBox = activity.getMapView().getRotatedTileBox();
		boolean landscape = !AndroidUiHelper.isOrientationPortrait(activity);
		boolean rtl = AndroidUtils.isLayoutRtl(activity);
		return getTargetPixelX(tileBox, landscape, rtl);
	}

	public static int getTargetPixelX(@NonNull RotatedTileBox tb, boolean landscape, boolean rtl) {
		if (landscape) {
			return Math.round((rtl ? 0.3f : 0.7f) * tb.getPixWidth());
		}
		return tb.getCenterPixelX();
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
