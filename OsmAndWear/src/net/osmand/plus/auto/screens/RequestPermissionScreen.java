package net.osmand.plus.auto.screens;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.OnClickListener;
import androidx.car.app.model.ParkedOnlyOnClickListener;
import androidx.car.app.model.Template;

import net.osmand.plus.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen for asking the user to grant location permission.
 */
public class RequestPermissionScreen extends BaseAndroidAutoScreen {

	/**
	 * Callback called when the location permission is granted.
	 */
	public interface LocationPermissionCheckCallback {
		/**
		 * Callback called when the location permission is granted.
		 */
		void onPermissionGranted();
	}

	LocationPermissionCheckCallback mLocationPermissionCheckCallback;

	public RequestPermissionScreen(@NonNull CarContext carContext,
	                               @Nullable LocationPermissionCheckCallback callback) {
		super(carContext);
		mLocationPermissionCheckCallback = callback;
	}

	@NonNull
	@Override
	public Template onGetTemplate() {
		List<String> permissions = new ArrayList<>();
		permissions.add(ACCESS_FINE_LOCATION);
		permissions.add(ACCESS_COARSE_LOCATION);

		String message = getCarContext().getString(R.string.location_access_request_title);

		OnClickListener listener = ParkedOnlyOnClickListener.create(() ->
				getCarContext().requestPermissions(
						permissions,
						(approved, rejected) -> {
							if (!approved.isEmpty()) {
								LocationPermissionCheckCallback locationPermissionCheckCallback = mLocationPermissionCheckCallback;
								if (locationPermissionCheckCallback != null) {
									locationPermissionCheckCallback.onPermissionGranted();
								}
							}
							finish();
						}));

		Action action = new Action.Builder()
				.setTitle(getCarContext().getString(R.string.location_access_request_action))
				.setBackgroundColor(CarColor.GREEN)
				.setOnClickListener(listener)
				.build();

		return new MessageTemplate.Builder(message).addAction(action).setHeaderAction(
				Action.APP_ICON).build();
	}
}
