package net.osmand.plus.helpers;

import android.location.LocationManager;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class HMDLocationServiceHelper extends AndroidApiLocationServiceHelper {

	private static final Log LOG = PlatformUtil.getLog(HMDLocationServiceHelper.class);

	public HMDLocationServiceHelper(@NonNull OsmandApplication app) {
		super(app);
	}

	@NonNull
	@Override
	protected List<String> getIgnoredNetworkProviders() {
		List<String> list = new ArrayList<>();
		list.add(LocationManager.GPS_PROVIDER);
		list.add(LocationManager.PASSIVE_PROVIDER);

		return list;
	}
}