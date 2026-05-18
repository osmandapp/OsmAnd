package net.osmand.plus.helpers;

import androidx.annotation.NonNull;

import net.osmand.Location;

import java.util.List;

public abstract class LocationCallback {

	public void onLocationResult(@NonNull List<Location> locations) {
	}

	public void onLocationAvailability(boolean locationAvailable) {
	}
}
