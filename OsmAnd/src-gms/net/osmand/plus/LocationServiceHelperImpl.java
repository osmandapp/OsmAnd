package net.osmand.plus;

import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import net.osmand.PlatformUtil;
import net.osmand.plus.helpers.DayNightHelper;
import net.osmand.plus.helpers.LocationServiceHelper;

import org.apache.commons.logging.Log;

import java.util.Collections;

public class LocationServiceHelperImpl extends LocationServiceHelper {

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

	public LocationServiceHelperImpl(@NonNull OsmandApplication app) {
		this.app = app;

		fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(app);

		fusedLocationRequest = new LocationRequest()
				// Sets the desired interval for active location updates. This interval is inexact. You
				// may not receive updates at all if no location sources are available, or you may
				// receive them less frequently than requested. You may also receive updates more
				// frequently than requested if other applications are requesting location at a more
				// frequent interval.
				//
				// IMPORTANT NOTE: Apps running on Android 8.0 and higher devices (regardless of
				// targetSdkVersion) may receive updates less frequently than this interval when the app
				// is no longer in the foreground.
				.setInterval(100)

				// Sets the fastest rate for active location updates. This interval is exact, and your
				// application will never receive updates more frequently than this value.
				//.setFastestInterval(50)

				// Sets the maximum time when batched location updates are delivered. Updates may be
				// delivered sooner than this interval.
				.setMaxWaitTime(0)

				.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

		fusedLocationCallback = new com.google.android.gms.location.LocationCallback() {
			@Override
			public void onLocationResult(LocationResult locationResult) {
				LocationCallback locationCallback = LocationServiceHelperImpl.this.locationCallback;
				if (locationCallback != null) {
					Location location = locationResult != null ? locationResult.getLastLocation() : null;
					net.osmand.Location l = convertLocation(location);
					locationCallback.onLocationResult(l == null
							? Collections.<net.osmand.Location>emptyList() : Collections.singletonList(l));
				}

			}

			@Override
			public void onLocationAvailability(LocationAvailability locationAvailability) {
				LocationCallback locationCallback = LocationServiceHelperImpl.this.locationCallback;
				if (locationAvailability != null &&  locationCallback != null) {
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
	public net.osmand.Location getFirstTimeRunDefaultLocation(@Nullable final LocationCallback locationCallback) {
		if (locationCallback == null) {
			return null;
		}
		try {
			Task<Location> lastLocation = fusedLocationProviderClient.getLastLocation();
			lastLocation.addOnSuccessListener(new OnSuccessListener<Location>() {
				@Override
				public void onSuccess(Location loc) {
					locationCallback.onLocationResult(loc != null
							? Collections.singletonList(convertLocation(loc)) : Collections.<net.osmand.Location>emptyList() );
				}
			});
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
