package net.osmand.plus.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;

public abstract class LocationServiceHelper {

	public abstract void requestLocationUpdates(@NonNull LocationCallback locationCallback);

	public abstract boolean isNetworkLocationUpdatesSupported();

	public abstract void requestNetworkLocationUpdates(@NonNull LocationCallback locationCallback);

	public abstract void removeLocationUpdates();

	public abstract Location getFirstTimeRunDefaultLocation(@Nullable LocationCallback locationCallback);
}
