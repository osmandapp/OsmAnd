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

		private final String provider;

		protected NetworkListener(@NonNull String provider) {
			this.provider = provider;
		}

		@Override
		public void onLocationChanged(@NonNull android.location.Location location) {
			LocationCallback locationCallback = LocationServiceHelper.this.networkLocationCallback;
			if (locationCallback != null) {
				net.osmand.Location l = convertLocation(location);
				locationCallback.onLocationResult(l == null ? Collections.emptyList() : Collections.singletonList(l));
			}
		}
	}

	public LocationServiceHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	@Nullable
	protected net.osmand.Location convertLocation(@Nullable android.location.Location location) {
		return location == null ? null : OsmAndLocationProvider.convertLocation(location, app);
	}

	protected void removeNetworkLocationUpdates() {
		LocationManager locationManager = (LocationManager) app.getSystemService(LOCATION_SERVICE);
		while (!networkListeners.isEmpty()) {
			LocationListener listener = networkListeners.poll();
			if (listener != null) {
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
