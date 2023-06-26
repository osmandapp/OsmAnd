package net.osmand.plus.helpers;

import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Task;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;

import org.apache.commons.logging.Log;

import java.util.Collections;

public class GmsLocationServiceHelper extends LocationServiceHelper {

	private static final Log LOG = PlatformUtil.getLog(DayNightHelper.class);

	private final OsmandApplication app;

	// FusedLocationProviderClient - Main class for receiving location updates.
	private final FusedLocationProviderClient fusedLocationProviderClient;

	// LocationRequest - Requirements for the location updates, i.e., how often you should receive
	// updates, the priority, etc.
	private final LocationRequest fusedLocationRequest;

	// LocationCallback - Called when FusedLocationProviderClient has a new Location.
	private final com.google.android.gms.location.LocationCallback fusedLocationCallback;

	private LocationCallback locationCallback;

	public GmsLocationServiceHelper(@NonNull OsmandApplication app) {
		this.app = app;

		fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(app);
		fusedLocationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 100).build();
		fusedLocationCallback = new com.google.android.gms.location.LocationCallback() {
			@Override
			public void onLocationResult(@NonNull LocationResult locationResult) {
				LocationCallback locationCallback = GmsLocationServiceHelper.this.locationCallback;
				if (locationCallback != null) {
					Location location = locationResult.getLastLocation();
					net.osmand.Location l = convertLocation(location);
					locationCallback.onLocationResult(l == null ? Collections.emptyList() : Collections.singletonList(l));
				}
			}

			@Override
			public void onLocationAvailability(@NonNull LocationAvailability locationAvailability) {
				LocationCallback locationCallback = GmsLocationServiceHelper.this.locationCallback;
				if (locationCallback != null) {
					locationCallback.onLocationAvailability(locationAvailability.isLocationAvailable());
				}
			}
		};
	}

	@Override
	public void requestLocationUpdates(@NonNull LocationCallback locationCallback) {
		this.locationCallback = locationCallback;
		// request location updates
		try {
			fusedLocationProviderClient.requestLocationUpdates(
					fusedLocationRequest, fusedLocationCallback, Looper.myLooper());
		} catch (SecurityException e) {
			LOG.debug("Location service permission not granted");
			throw e;
		} catch (IllegalArgumentException e) {
			LOG.debug("GPS location provider not available");
			throw e;
		}
	}

	@Override
	public boolean isNetworkLocationUpdatesSupported() {
		return false;
	}

	@Override
	public void requestNetworkLocationUpdates(@NonNull LocationCallback locationCallback) {
	}

	@Override
	public void removeLocationUpdates() {
		// remove location updates
		try {
			fusedLocationProviderClient.removeLocationUpdates(fusedLocationCallback);
		} catch (SecurityException e) {
			LOG.debug("Location service permission not granted", e);
			throw e;
		}
	}

	@Nullable
	public net.osmand.Location getFirstTimeRunDefaultLocation(@Nullable LocationCallback locationCallback) {
		if (locationCallback == null) {
			return null;
		}
		try {
			Task<Location> lastLocation = fusedLocationProviderClient.getLastLocation();
			lastLocation.addOnSuccessListener(loc -> locationCallback.onLocationResult(loc != null
					? Collections.singletonList(convertLocation(loc)) : Collections.emptyList() ));
		} catch (SecurityException e) {
			LOG.debug("Location service permission not granted");
		} catch (IllegalArgumentException e) {
			LOG.debug("GPS location provider not available");
		}
		return null;
	}

	@Nullable
	private net.osmand.Location convertLocation(@Nullable Location location) {
		return location == null ? null : OsmAndLocationProvider.convertLocation(location, app);
	}
}
