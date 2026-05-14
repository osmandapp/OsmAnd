package net.osmand.plus.helpers;

import static android.location.LocationManager.GPS_PROVIDER;
import static android.location.LocationManager.PASSIVE_PROVIDER;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;

import org.apache.commons.logging.Log;

import java.util.Arrays;
import java.util.List;

public class HMDLocationServiceHelper extends AndroidApiLocationServiceHelper {

	private static final Log LOG = PlatformUtil.getLog(HMDLocationServiceHelper.class);

	@SuppressLint("InlinedApi")
	private static final List<String> IGNORED_NETWORK_PROVIDERS = Arrays.asList(GPS_PROVIDER, PASSIVE_PROVIDER);

	public HMDLocationServiceHelper(@NonNull OsmandApplication app) {
		super(app);
	}

	@NonNull
	@Override
	protected List<String> getIgnoredNetworkProviders() {
		return IGNORED_NETWORK_PROVIDERS;
	}
}