package net.osmand.plus.resources;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.data.LatLon;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuController;

import java.lang.ref.WeakReference;

public class SearchOsmandRegionTask extends AsyncTask<Void, Void, BinaryMapDataObject> {

	private final WeakReference<MenuController> controllerRef;
	private final LatLon latLon;
	private final int zoom;
	private OsmandRegionSearcher regionSearcher;

	public SearchOsmandRegionTask(@NonNull MenuController controller, @NonNull LatLon latLon, int zoom) {
		this.controllerRef = new WeakReference<>(controller);
		this.latLon = latLon;
		this.zoom = zoom;
	}

	@Nullable
	private MenuController getController() {
		return controllerRef.get();
	}

	@Nullable
	private MapActivity getMapActivity() {
		MenuController controller = getController();
		return controller != null ? controller.getMapActivity() : null;
	}

	@Override
	protected void onPreExecute() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			regionSearcher = new OsmandRegionSearcher(mapActivity.getMyApplication(), latLon, zoom);
		}
	}

	@Override
	protected BinaryMapDataObject doInBackground(Void... voids) {
		if (regionSearcher != null) {
			regionSearcher.search();
			return regionSearcher.getBinaryMapDataObject();
		}
		return null;
	}

	@Override
	protected void onPostExecute(BinaryMapDataObject binaryMapDataObject) {
		MenuController controller = getController();
		if (controller != null && regionSearcher != null) {
			controller.createMapDownloadControls(binaryMapDataObject, regionSearcher.getRegionFullName());
		}
	}
}
