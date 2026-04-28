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
	public void requestNetworkLocationUpdates(@NonNull LocationCallback locationCallback) {
		LocationManager locationManager = (LocationManager) app.getSystemService(LOCATION_SERVICE);
		LOG.info("Requesting (Fused/GPS) location updates...");
		List<String> enabledProviders = null;
		try {
			enabledProviders = locationManager.getProviders(true);
			LOG.info("Diagnostic - All providers: " + locationManager.getAllProviders());
			LOG.info("Diagnostic - Enabled providers: " + enabledProviders);
		} catch (Exception e) {
			LOG.debug("Failed to log providers", e);
		}
		super.requestNetworkLocationUpdates(locationCallback);
		try {
			if (enabledProviders != null && enabledProviders.contains("fused")) {
				LOG.info("Requesting FUSED for fast indoor fallback...");
				NetworkListener networkListener = new NetworkListener("fused");
				locationManager.requestLocationUpdates("fused", 0, 0, networkListener);
				networkListeners.add(networkListener);
				LOG.info("Successfully registered listener for fused provider");
			} else {
				LOG.warn("device does not have 'fused' enabled or available.");
			}
		} catch (SecurityException e) {
			LOG.debug("Fused location service permission not granted", e);
		} catch (IllegalArgumentException e) {
			LOG.debug("Fused location provider not available", e);
		} catch (Exception e) {
			LOG.error("Unexpected error registering fused fallback", e);
		}
	}
}