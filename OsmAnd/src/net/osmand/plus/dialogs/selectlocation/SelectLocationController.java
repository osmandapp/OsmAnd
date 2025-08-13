package net.osmand.plus.dialogs.selectlocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.LatLon;
import net.osmand.map.IMapLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.interfaces.dialog.IDialog;
import net.osmand.plus.dialogs.selectlocation.extractor.CenterMapLatLonExtractor;
import net.osmand.plus.dialogs.selectlocation.extractor.IMapLocationExtractor;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.MapDisplayPositionManager;
import net.osmand.plus.helpers.MapDisplayPositionManager.IMapDisplayPositionProvider;
import net.osmand.plus.settings.enums.MapPosition;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.OsmandMap;
import net.osmand.plus.views.OsmandMapTileView;

public class SelectLocationController<ResultType> extends BaseDialogController
		implements IMapLocationListener, IMapDisplayPositionProvider {

	private static final String PROCESS_ID = "select_location_on_map";

	private ILocationSelectionHandler<ResultType> handler;
	private final IMapLocationExtractor<ResultType> preferredExtractor;
	private final CenterMapLatLonExtractor latLonExtractor;
	private boolean applyChanges = false;
	private boolean landscape;

	public SelectLocationController(@NonNull OsmandApplication app,
	                                @NonNull IMapLocationExtractor<ResultType> extractor,
	                                @NonNull ILocationSelectionHandler<ResultType> handler) {
		super(app);
		setLocationSelectionHandler(handler);
		this.preferredExtractor = extractor;
		this.latLonExtractor = new CenterMapLatLonExtractor();
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

	public void setLocationSelectionHandler(@NonNull ILocationSelectionHandler<ResultType> handler) {
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
		LatLon latLon = latLonExtractor.extractLocation(app);
		int format = app.getSettings().COORDINATES_FORMAT.get();
		return OsmAndFormatter.getFormattedCoordinates(latLon.getLatitude(), latLon.getLongitude(), format);
	}

	public void onConfirmSelection() {
		applyChanges = true;
	}

	public void onResume() {
		updateMapLocationListener(true);
		updateDisplayPositionListener(true);
	}

	public void onPause() {
		updateMapLocationListener(false);
		updateDisplayPositionListener(false);
	}

	public void onDestroy(@Nullable FragmentActivity activity) {
		if (finishProcessIfNeeded(activity)) {
			if (applyChanges) {
				onLocationSelected();
			}
			onScreenClosed(applyChanges);
			applyChanges = false;
		}
	}

	private void onLocationSelected() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			handler.onLocationSelected(mapActivity, preferredExtractor.extractLocation(app));
		}
	}

	private void onScreenClosed(boolean locationSelected) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			handler.onScreenClosed(mapActivity, locationSelected);
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

	@Override
	public void locationChanged(double newLatitude, double newLongitude, Object source) {
		app.runInUIThread(() -> dialogManager.askRefreshDialogCompletely(getProcessId()));
	}

	private void updateDisplayPositionListener(boolean register) {
		MapViewTrackingUtilities utilities = app.getMapViewTrackingUtilities();
		MapDisplayPositionManager manager = utilities.getMapDisplayPositionManager();
		manager.updateMapPositionProviders(this, register);
		manager.updateMapDisplayPosition(true);
	}

	@Nullable
	@Override
	public MapPosition getMapDisplayPosition() {
		return landscape ? MapPosition.LANDSCAPE_MIDDLE_END : MapPosition.CENTER;
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
	public String getCenterPointLabel() {
		MapActivity mapActivity = getMapActivity();
		if (handler != null && mapActivity != null) {
			return handler.getCenterPointLabel(mapActivity);
		}
		return null;
	}

	@Nullable
	private MapActivity getMapActivity() {
		return app.getOsmandMap().getMapView().getMapActivity();
	}

	@Nullable
	public static <ResultType> SelectLocationController<ResultType> getExistedInstance(@NonNull OsmandApplication app) {
		DialogManager dialogManager = app.getDialogManager();
		return (SelectLocationController<ResultType>) dialogManager.findController(PROCESS_ID);
	}

	public static <ResultType> void showDialog(@NonNull FragmentActivity activity,
	                                           @NonNull IMapLocationExtractor<ResultType> extractor,
	                                           @NonNull ILocationSelectionHandler<ResultType> handler) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		SelectLocationController<ResultType> controller = new SelectLocationController<>(app, extractor, handler);

		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, controller);

		FragmentManager manager = activity.getSupportFragmentManager();
		SelectLocationFragment.showInstance(manager);
	}
}
