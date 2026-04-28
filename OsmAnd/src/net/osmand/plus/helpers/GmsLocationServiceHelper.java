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

	private static final Log LOG = PlatformUtil.getLog(GmsLocationServiceHelper.class);

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
		networkLocationRequest = new LocationRequestCompat.Builder(5000)
				.setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
				.setMinUpdateIntervalMillis(500)
				.build();
		fusedLocationCallback = new com.google.android.gms.location.LocationCallback() {
			@Override
			public void onLocationResult(@NonNull LocationResult locationResult) {
				Location loc = locationResult.getLastLocation();
				if (loc != null) {
					LOG.info("SUCCESS! Received GMS Fused location: Lat=" +
							String.format(java.util.Locale.US, "%.2f", loc.getLatitude()) + ", Lon=" +
							String.format(java.util.Locale.US, "%.2f", loc.getLongitude()) + ", Acc=" + loc.getAccuracy());
				}
				LocationCallback locationCallback = GmsLocationServiceHelper.this.locationCallback;
				if (locationCallback != null) {
					Location location = locationResult.getLastLocation();
					net.osmand.Location l = convertLocation(location);
					locationCallback.onLocationResult(l == null ? Collections.emptyList() : Collections.singletonList(l));
				}
			}

			@Override
			public void onLocationAvailability(@NonNull LocationAvailability locationAvailability) {
				LOG.info("System fired GMS onLocationAvailability: " + locationAvailability.isLocationAvailable());
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
		LOG.info("Requesting GMS Fused location updates...");
		// request location updates
		try {
			fusedLocationProviderClient.requestLocationUpdates(
					fusedLocationRequest, fusedLocationCallback, Looper.myLooper());
			LOG.info("Successfully registered GMS Fused listener.");
		} catch (SecurityException e) {
			LOG.debug("Location service permission not granted", e);
			throw e;
		} catch (IllegalArgumentException e) {
			LOG.debug("GPS location provider not available", e);
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
		LOG.info("GMS requestNetworkLocationUpdates()");
		LOG.info("GMS allProviders=" + locationManager.getAllProviders());
		LOG.info("GMS enabledProviders=" + locationManager.getProviders(true));
		List<String> providers = locationManager.getProviders(true);
		for (String provider : providers) {
			if (provider == null
					|| provider.equals(LocationManager.GPS_PROVIDER)
					|| provider.equals(LocationManager.FUSED_PROVIDER)) {
				continue;
			}
			LOG.info("GMS requesting network updates from provider=[" + provider + "]");
			try {
				NetworkListener networkListener = new NetworkListener(provider);
				LocationManagerCompat.requestLocationUpdates(locationManager, provider, networkLocationRequest, networkListener, Looper.getMainLooper());
				networkListeners.add(networkListener);
				LOG.info("GMS successfully registered network listener for [" + provider + "]");
			} catch (SecurityException e) {
				LOG.warn("GMS " + provider + " location service permission not granted", e);
			} catch (IllegalArgumentException e) {
				LOG.warn("GMS " + provider + " location provider not available", e);
			} catch (Exception e) {
				LOG.error("GMS unexpected error registering provider=[" + provider + "]", e);
			}
		}
	}

	@Override
	public void removeLocationUpdates() {
		LOG.info("Removing GMS Fused location updates...");
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
		LOG.info("Attempting to get first-time run default location via GMS Fused API...");
		try {
			Task<Location> lastLocation = fusedLocationProviderClient.getLastLocation();
			lastLocation.addOnSuccessListener(loc -> {
				if (loc != null) {
					LOG.info("Success! Found valid cached GMS location.");
				} else {
					LOG.info("GMS getLastLocation returned null.");
				}
				locationCallback.onLocationResult(loc != null ? Collections.singletonList(convertLocation(loc)) : Collections.emptyList());
			});
		} catch (SecurityException e) {
			LOG.debug("Location service permission not granted", e);
		} catch (IllegalArgumentException e) {
			LOG.debug("GPS location provider not available", e);
		}
		return null;
	}
}
