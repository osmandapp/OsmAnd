package net.osmand.plus.helpers;

import android.Manifest;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityActions;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.firstusage.FirstUsageWizardFragment;
import net.osmand.plus.plugins.PluginsHelper;

public class MapPermissionsResultCallback implements OnRequestPermissionsResultCallback {

	private final MapActivity activity;

	public static boolean permissionDone;
	public boolean permissionAsked;
	public boolean permissionGranted;

	public MapPermissionsResultCallback(@NonNull MapActivity activity) {
		this.activity = activity;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
			@NonNull int[] grantResults) {
		if (grantResults.length > 0) {
			OsmandApplication app = activity.getApp();

			PluginsHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
			activity.getMapActions().onRequestPermissionsResult(requestCode, permissions, grantResults);

			OnRequestPermissionsResultCallback aaCallback = app.getCarAppPermissionListener();
			if (aaCallback != null) {
				aaCallback.onRequestPermissionsResult(requestCode, permissions, grantResults);
			}

			if (requestCode == DownloadActivity.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE
					&& permissions.length > 0
					&& Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permissions[0])) {
				permissionAsked = true;
				permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
				if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
					app.showToastMessage(R.string.missing_write_external_storage_permission);
				}
			} else if (requestCode == FirstUsageWizardFragment.FIRST_USAGE_LOCATION_PERMISSION) {
				app.runInUIThread(() -> {
					FirstUsageWizardFragment wizardFragment = activity.getFragmentsHelper().getFirstUsageWizardFragment();
					if (wizardFragment != null) {
						wizardFragment.processLocationPermission(grantResults[0] == PackageManager.PERMISSION_GRANTED);
					}
				}, 1);
			} else if (requestCode == MapActivityActions.REQUEST_LOCATION_FOR_DIRECTIONS_NAVIGATION_PERMISSION
					&& permissions.length > 0
					&& (Manifest.permission.ACCESS_FINE_LOCATION.equals(permissions[0])
					|| Manifest.permission.ACCESS_COARSE_LOCATION.equals(permissions[0]))) {
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					LatLon latLon = activity.getContextMenu().getLatLon();
					if (latLon != null) {
						activity.getMapActions().enterDirectionsFromPoint(latLon.getLatitude(), latLon.getLongitude());
					}
				} else {
					app.showToastMessage(R.string.ask_for_location_permission);
				}
			}
		}
	}
}
