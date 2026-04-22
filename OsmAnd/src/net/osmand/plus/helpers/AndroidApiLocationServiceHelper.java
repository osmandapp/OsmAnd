package net.osmand.plus.helpers;

import static android.content.Context.LOCATION_SERVICE;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.location.LocationListenerCompat;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AndroidApiLocationServiceHelper extends LocationServiceHelper implements LocationListenerCompat {

	private static final Log LOG = PlatformUtil.getLog(AndroidApiLocationServiceHelper.class);

	private LocationCallback locationCallback;

	public AndroidApiLocationServiceHelper(OsmandApplication app) {
		super(app);
	}

	@Override
	public void requestLocationUpdates(@NonNull LocationCallback locationCallback) {
		this.locationCallback = locationCallback;
		// request location updates
		LocationManager locationManager = (LocationManager) app.getSystemService(LOCATION_SERVICE);
		LOG.info("Requesting GPS location updates...");
		try {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
			LOG.info("Successfully registered listener for [gps]");
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
		boolean isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
				locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		LOG.info("Global location enabled state: " + isLocationEnabled);
		LOG.info("All installed providers: " + locationManager.getAllProviders());
		List<String> enabledProviders = locationManager.getProviders(true);
		LOG.info("Currently ENABLED providers: " + enabledProviders);

		if (!enabledProviders.contains(LocationManager.NETWORK_PROVIDER)) {
			LOG.warn("NETWORK_PROVIDER is NOT in the enabled list!");
		}
		try {
			if (locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)) {
				LOG.info("Attempting to explicitly request updates from provider: [" + LocationManager.NETWORK_PROVIDER + "]");
				NetworkListener networkListener = new NetworkListener();
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, networkListener);
				networkListeners.add(networkListener);
				LOG.info("Successfully registered explicit listener for [" + LocationManager.NETWORK_PROVIDER + "]");
			}
		} catch (SecurityException e) {
			LOG.debug("network location service permission not granted", e);
		} catch (IllegalArgumentException e) {
			LOG.debug("network location provider not available", e);
		} catch (Exception e) {
			LOG.error("Unexpected error explicitly registering network provider", e);
		}
		List<String> providers = locationManager.getProviders(true);
		for (String provider : providers) {
			if (provider == null
					|| provider.equals(LocationManager.GPS_PROVIDER)
					|| provider.equals(LocationManager.PASSIVE_PROVIDER)
					|| provider.equals(LocationManager.FUSED_PROVIDER)
					|| provider.equals(LocationManager.NETWORK_PROVIDER)) {
				continue;
			}
			LOG.info("Attempting to request updates from dynamic provider: [" + provider + "]");
			try {
				NetworkListener networkListener = new NetworkListener();
				locationManager.requestLocationUpdates(provider, 0, 0, networkListener);
				networkListeners.add(networkListener);
				LOG.info("Successfully registered listener for [" + provider + "]");
			} catch (SecurityException e) {
				LOG.debug(provider + " location service permission not granted", e);
			} catch (IllegalArgumentException e) {
				LOG.debug(provider + " location provider not available", e);
			}
		}
	}

	@Override
	public void removeLocationUpdates() {
		LOG.info("Removing all location updates...");
		// remove location updates
		LocationManager locationManager = (LocationManager) app.getSystemService(LOCATION_SERVICE);
		try {
			locationManager.removeUpdates(this);
		} catch (SecurityException e) {
			LOG.debug("Location service permission not granted", e);
			throw e;
		} finally {
			removeNetworkLocationUpdates();
		}
	}

	@Nullable
	public net.osmand.Location getFirstTimeRunDefaultLocation(
			@Nullable LocationCallback locationCallback) {
		LOG.info("Attempting to get first-time run default location (Last Known Location)...");
		LocationManager locationManager = (LocationManager) app.getSystemService(Context.LOCATION_SERVICE);
		List<String> providers = new ArrayList<>(locationManager.getProviders(true));
		// note, passive provider is from API_LEVEL 8 but it is a constant, we can check for it.
		// constant should not be changed in future
		int passiveFirst = providers.indexOf(LocationManager.PASSIVE_PROVIDER);
		// put passive provider to first place
		if (passiveFirst > -1) {
			providers.add(0, providers.remove(passiveFirst));
		}
		// find location
		for (String provider : providers) {
			try {
				LOG.info("Checking last known location from provider: [" + provider + "]");
				net.osmand.Location location = convertLocation(locationManager.getLastKnownLocation(provider));
				if (location != null) {
					LOG.info("Success! Found valid cached location from [" + provider + "]");
					return location;
				}
			} catch (SecurityException e) {
				// location service permission not granted
				LOG.warn("SecurityException checking last known location for [" + provider + "]", e);
			} catch (IllegalArgumentException e) {
				// location provider not available
				LOG.warn("IllegalArgumentException checking last known location for [" + provider + "]", e);
			}
		}
		LOG.info("Failed to find any cached last known location.");
		return null;
	}

	@Override
	public void onLocationChanged(@NonNull Location location) {
		LOG.info("SUCCESS! Received GPS location: Lat=" +
				String.format(java.util.Locale.US, "%.2f", location.getLatitude()) + ", Lon=" +
				String.format(java.util.Locale.US, "%.2f", location.getLongitude()) + ", Acc=" + location.getAccuracy());
		LocationCallback locationCallback = this.locationCallback;
		if (locationCallback != null) {
			net.osmand.Location l = convertLocation(location);
			locationCallback.onLocationResult(l == null ? Collections.emptyList() : Collections.singletonList(l));
		}
	}

	@Override
	public void onProviderEnabled(@NonNull String provider) {
		LOG.info("System fired onProviderEnabled for GPS [" + provider + "]");
		LocationCallback locationCallback = this.locationCallback;
		if (locationCallback != null) {
			locationCallback.onLocationAvailability(true);
		}
	}

	@Override
	public void onProviderDisabled(@NonNull String provider) {
		LOG.warn("System fired onProviderDisabled for GPS [" + provider + "]. Hardware disabled by user or OS.");
		LocationCallback locationCallback = this.locationCallback;
		if (locationCallback != null) {
			locationCallback.onLocationAvailability(false);
		}
	}
}
