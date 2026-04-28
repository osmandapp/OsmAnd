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

	public AndroidApiLocationServiceHelper(@NonNull OsmandApplication app) {
		super(app);
	}

	@Override
	public void requestLocationUpdates(@NonNull LocationCallback locationCallback) {
		this.locationCallback = locationCallback;
		// request location updates
		requestLocationUpdatesImpl();
	}

	protected void requestLocationUpdatesImpl() {
		String provider = LocationManager.GPS_PROVIDER;
		LocationManager locationManager = (LocationManager) app.getSystemService(LOCATION_SERVICE);
		LOG.info("Requesting GPS location updates...");
		try {
			locationManager.requestLocationUpdates(provider, 0, 0, this);
			LOG.info("Successfully registered listener for [" + provider + "]");
		} catch (SecurityException e) {
			LOG.debug(provider + " location service permission not granted", e);
			throw e;
		} catch (IllegalArgumentException e) {
			LOG.debug(provider + " location provider not available", e);
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
					|| provider.equals(LocationManager.PASSIVE_PROVIDER)
					|| provider.equals(LocationManager.FUSED_PROVIDER)) {
				continue;
			}
			try {
				NetworkListener networkListener = new NetworkListener(provider);
				locationManager.requestLocationUpdates(provider, 0, 0, networkListener);
				networkListeners.add(networkListener);
			} catch (SecurityException e) {
				LOG.debug(provider + " location service permission not granted", e);
			} catch (IllegalArgumentException e) {
				LOG.debug(provider + " location provider not available", e);
			}
		}
		for (String provider : locationManager.getAllProviders()) {
			try {
				boolean enabled = locationManager.isProviderEnabled(provider);
				android.location.Location last = locationManager.getLastKnownLocation(provider);
				LOG.warn("providerState [" + provider + "]: enabled=" + enabled + ", lastKnown=" + androidLocToString(last));
			} catch (SecurityException e) {
				LOG.warn("providerState [" + provider + "]: SecurityException", e);
			} catch (Exception e) {
				LOG.warn("providerState [" + provider + "]: error", e);
			}
		}
	}

	private static String androidLocToString(@Nullable android.location.Location location) {
		if (location == null) {
			return "null";
		}
		return "provider=" + location.getProvider()
				+ ", lat=" + String.format(java.util.Locale.US, "%.2f", location.getLatitude())
				+ ", lon=" + String.format(java.util.Locale.US, "%.2f", location.getLongitude())
				+ ", acc=" + (location.hasAccuracy() ? location.getAccuracy() : -1)
				+ ", time=" + location.getTime()
				+ ", elapsedRealtimeNanos=" + location.getElapsedRealtimeNanos();
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
	public net.osmand.Location getFirstTimeRunDefaultLocation(@Nullable LocationCallback locationCallback) {
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
				LOG.debug(provider + " location service permission not granted", e);
			} catch (IllegalArgumentException e) {
				LOG.debug(provider + " location provider not available", e);
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
