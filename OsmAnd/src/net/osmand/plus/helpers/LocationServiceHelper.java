package net.osmand.plus.helpers;

import static android.content.Context.LOCATION_SERVICE;

import android.location.LocationListener;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.location.LocationListenerCompat;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;

import org.apache.commons.logging.Log;

import java.util.Collections;
import java.util.LinkedList;

public abstract class LocationServiceHelper {

	protected static final Log LOG = PlatformUtil.getLog(LocationServiceHelper.class);

	protected final OsmandApplication app;

	protected LocationCallback networkLocationCallback;
	protected final LinkedList<LocationListener> networkListeners = new LinkedList<>();

	// Working with location checkListeners
	protected class NetworkListener implements LocationListenerCompat {

		private final String requestedProvider;

		protected NetworkListener(@NonNull String requestedProvider) {
			this.requestedProvider = requestedProvider;
		}

		@Override
		public void onLocationChanged(@NonNull android.location.Location location) {
			LOG.info("network callback: requestedProvider=[" + requestedProvider
					+ "], actualProvider=[" + location.getProvider()
					+ "], lat=" + String.format(java.util.Locale.US, "%.3f", location.getLatitude())
					+ ", lon=" + String.format(java.util.Locale.US, "%.3f", location.getLongitude())
					+ ", acc=" + (location.hasAccuracy() ? location.getAccuracy() : -1)
					+ ", time=" + location.getTime());

			LocationCallback locationCallback = LocationServiceHelper.this.networkLocationCallback;
			if (locationCallback != null) {
				net.osmand.Location l = convertLocation(location);
				locationCallback.onLocationResult(l == null ? Collections.emptyList() : Collections.singletonList(l));
			} else {
				LOG.warn("network callback dropped: networkLocationCallback is null");
			}
		}

		@Override
		public void onProviderEnabled(@NonNull String provider) {
			LOG.info("provider enabled: requestedProvider=[" + requestedProvider + "], provider=[" + provider + "]");
		}

		@Override
		public void onProviderDisabled(@NonNull String provider) {
			LOG.warn("provider disabled: requestedProvider=[" + requestedProvider + "], provider=[" + provider + "]");
		}
	}

	public LocationServiceHelper(OsmandApplication app) {
		this.app = app;
	}

	@Nullable
	protected net.osmand.Location convertLocation(@Nullable android.location.Location location) {
		return location == null ? null : OsmAndLocationProvider.convertLocation(location, app);
	}

	protected void removeNetworkLocationUpdates() {
		LOG.info("Removing network location updates. Active listeners count: " + networkListeners.size());
		LocationManager locationManager = (LocationManager) app.getSystemService(LOCATION_SERVICE);
		while (!networkListeners.isEmpty()) {
			LocationListener listener = networkListeners.poll();
			if (listener != null) {
				LOG.info("Removing a network listener instance");
				locationManager.removeUpdates(listener);
			}
		}
	}

	public abstract void requestLocationUpdates(@NonNull LocationCallback locationCallback);

	public abstract boolean isNetworkLocationUpdatesSupported();

	public abstract void requestNetworkLocationUpdates(@NonNull LocationCallback locationCallback);

	public abstract void removeLocationUpdates();

	public abstract Location getFirstTimeRunDefaultLocation(@Nullable LocationCallback locationCallback);
}
