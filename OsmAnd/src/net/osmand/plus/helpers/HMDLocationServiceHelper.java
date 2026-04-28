package net.osmand.plus.helpers;

import static android.content.Context.LOCATION_SERVICE;

import android.location.LocationManager;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;

import org.apache.commons.logging.Log;

import java.util.List;

public class HMDLocationServiceHelper extends AndroidApiLocationServiceHelper {

	private static final Log LOG = PlatformUtil.getLog(HMDLocationServiceHelper.class);

	public HMDLocationServiceHelper(@NonNull OsmandApplication app) {
		super(app);
	}

	@Override
	protected void requestLocationUpdatesImpl() {
		String provider = LocationManager.GPS_PROVIDER;
		LocationManager locationManager = (LocationManager) app.getSystemService(LOCATION_SERVICE);
		LOG.info("Requesting HMD (Fused/GPS) location updates...");
		try {
			List<String> providers = locationManager.getProviders(true);
			if (providers.contains("fused")) {
				provider = "fused";
			}
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
}