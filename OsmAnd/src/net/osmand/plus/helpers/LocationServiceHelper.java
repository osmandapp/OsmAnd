package net.osmand.plus.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;

import java.util.List;

public abstract class LocationServiceHelper {

	public static abstract class LocationCallback {

		public void onLocationResult(@NonNull List<Location> locations) {
		}

		public void onLocationAvailability(boolean locationAvailable) {
		}
	}

	public abstract void requestLocationUpdates(@NonNull LocationCallback locationCallback);

	public abstract boolean isNetworkLocationUpdatesSupported();

	public abstract void requestNetworkLocationUpdates(@NonNull LocationCallback locationCallback);

	public abstract void removeLocationUpdates();

	public abstract Location getFirstTimeRunDefaultLocation(@Nullable LocationCallback locationCallback);
}
