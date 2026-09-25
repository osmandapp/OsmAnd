package net.osmand.plus.helpers;

import static android.content.Context.LOCATION_SERVICE;

import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.location.LocationManagerCompat;
import androidx.core.location.LocationRequestCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Task;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;

import org.apache.commons.logging.Log;

import java.util.Collections;
import java.util.List;

public class GmsLocationServiceHelper extends LocationServiceHelper {

	private static final Log LOG = PlatformUtil.getLog(DayNightHelper.class);

	// FusedLocationProviderClient - Main class for receiving location updates.
	private final FusedLocationProviderClient fusedLocationProviderClient;

	// LocationRequest - Requirements for the location updates, i.e., how often you should receive
	// updates, the priority, etc.
	private final LocationRequest fusedLocationRequest;
	private final LocationRequestCompat networkLocationRequest;

	// LocationCallback - Called when FusedLocationProviderClient has a new Location.
	private final com.google.android.gms.location.LocationCallback fusedLocationCallback;

	private LocationCallback locationCallback;

	public GmsLocationServiceHelper(@NonNull OsmandApplication app) {
		super(app);

		fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(app);
		fusedLocationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 100).build();
		networkLocationRequest = new LocationRequestCompat.Builder(100).setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY).build();
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
		return true;
	}

	@Override
	public void requestNetworkLocationUpdates(@NonNull LocationCallback locationCallback) {
		this.networkLocationCallback = locationCallback;
		// request location updates
		LocationManager locationManager = (LocationManager) app.getSystemService(LOCATION_SERVICE);
		List<String> providers = locationManager.getProviders(true);
		for (String provider : providers) {
			if (provider == null
					|| provider.equals(LocationManager.GPS_PROVIDER)
					|| provider.equals(LocationManager.FUSED_PROVIDER)) {
				continue;
			}
			try {
				NetworkListener networkListener = new NetworkListener();
				LocationManagerCompat.requestLocationUpdates(locationManager, provider, networkLocationRequest,
						networkListener, Looper.myLooper());
				networkListeners.add(networkListener);
			} catch (SecurityException e) {
				LOG.debug(provider + " location service permission not granted");
			} catch (IllegalArgumentException e) {
				LOG.debug(provider + " location provider not available");
			}
		}
	}

	@Override
	public void removeLocationUpdates() {
		// remove location updates
		try {
			fusedLocationProviderClient.removeLocationUpdates(fusedLocationCallback);
		} catch (SecurityException e) {
			LOG.debug("Location service permission not granted", e);
			throw e;
		} finally {
			removeNetworkLocationUpdates();
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
}
