package net.osmand.plus.helpers;

import static android.content.Context.LOCATION_SERVICE;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class AndroidApiLocationServiceHelper extends LocationServiceHelper implements LocationListener {

	private static final Log LOG = PlatformUtil.getLog(DayNightHelper.class);

	private final OsmandApplication app;

	private LocationCallback locationCallback;
	private LocationCallback networkLocationCallback;
	private final LinkedList<LocationListener> networkListeners = new LinkedList<>();

	// Working with location checkListeners
	private class NetworkListener implements LocationListener {

		@Override
		public void onLocationChanged(@NonNull Location location) {
			LocationCallback locationCallback = AndroidApiLocationServiceHelper.this.networkLocationCallback;
			if (locationCallback != null) {
				net.osmand.Location l = convertLocation(location);
				locationCallback.onLocationResult(l == null ? Collections.emptyList() : Collections.singletonList(l));
			}
		}
	}

	public AndroidApiLocationServiceHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	@Override
	public void requestLocationUpdates(@NonNull LocationCallback locationCallback) {
		this.locationCallback = locationCallback;
		// request location updates
		LocationManager locationManager = (LocationManager) app.getSystemService(LOCATION_SERVICE);
		try {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
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
			if (provider == null || provider.equals(LocationManager.GPS_PROVIDER)) {
				continue;
			}
			try {
				NetworkListener networkListener = new NetworkListener();
				locationManager.requestLocationUpdates(provider, 0, 0, networkListener);
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
		LocationManager locationManager = (LocationManager) app.getSystemService(LOCATION_SERVICE);
		try {
			locationManager.removeUpdates(this);
		} catch (SecurityException e) {
			LOG.debug("Location service permission not granted", e);
			throw e;
		} finally {
			while (!networkListeners.isEmpty()) {
				LocationListener listener = networkListeners.poll();
				if (listener != null) {
					locationManager.removeUpdates(listener);
				}
			}
		}
	}

	@Nullable
	public net.osmand.Location getFirstTimeRunDefaultLocation(@Nullable LocationCallback locationCallback) {
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
				net.osmand.Location location = convertLocation(locationManager.getLastKnownLocation(provider));
				if (location != null) {
					return location;
				}
			} catch (SecurityException e) {
				// location service permission not granted
			} catch (IllegalArgumentException e) {
				// location provider not available
			}
		}
		return null;
	}

	@Nullable
	private net.osmand.Location convertLocation(@Nullable Location location) {
		return location == null ? null : OsmAndLocationProvider.convertLocation(location, app);
	}

	@Override
	public void onLocationChanged(@NonNull Location location) {
		LocationCallback locationCallback = this.locationCallback;
		if (locationCallback != null) {
			net.osmand.Location l = convertLocation(location);
			locationCallback.onLocationResult(l == null ? Collections.emptyList() : Collections.singletonList(l));
		}
	}

	@Override
	public void onProviderEnabled(@NonNull String provider) {
		LocationCallback locationCallback = this.locationCallback;
		if (locationCallback != null) {
			locationCallback.onLocationAvailability(true);
		}
	}

	@Override
	public void onProviderDisabled(@NonNull String provider) {
		LocationCallback locationCallback = this.locationCallback;
		if (locationCallback != null) {
			locationCallback.onLocationAvailability(false);
		}
	}
}
