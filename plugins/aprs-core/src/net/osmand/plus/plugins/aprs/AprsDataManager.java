package net.osmand.plus.plugins.aprs;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class AprsDataManager implements AprsMessageListener {

	private static final Log LOG = PlatformUtil.getLog(AprsDataManager.class);

	public interface StationListener {
		void onStationReceived(@NonNull AprsStation station);
		void onStationRemoved(@NonNull AprsStation station);
	}

	private final AprsPacketParser parser = new AprsPacketParser();
	private final Map<String, AprsStation> stations = new ConcurrentHashMap<>();
	private final OsmandApplication app;
	private StationListener listener;
	private Timer cleanupTimer;
	private int expiryMinutes = 30;
	private float radiusKm = 300f;

	public AprsDataManager(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public void setListener(@Nullable StationListener listener) {
		this.listener = listener;
	}

	public void setExpiryMinutes(int expiryMinutes) {
		this.expiryMinutes = expiryMinutes;
	}

	public void setRadiusKm(float radiusKm) {
		this.radiusKm = radiusKm;
	}

	public void startUpdates() {
		stopUpdates();
		cleanupTimer = new Timer();
		cleanupTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				removeExpiredStations();
			}
		}, 20000, 30000);
	}

	public void stopUpdates() {
		if (cleanupTimer != null) {
			cleanupTimer.cancel();
			cleanupTimer = null;
		}
	}

	public synchronized void cleanupResources() {
		stopUpdates();
		stations.clear();
	}

	@Override
	public void onAx25Frame(@NonNull byte[] frame) {
		AprsStation parsed = parser.parseAx25Frame(frame);
		if (parsed == null) {
			return;
		}
		ingestStation(parsed);
	}

	public void ingestStation(@NonNull AprsStation parsed) {
		if (parsed.hasPosition() && !withinRadius(parsed)) {
			return;
		}
		stations.merge(parsed.getCallsign(), parsed, (existing, update) -> {
			existing.updateFrom(update);
			return existing;
		});
		AprsStation station = stations.get(parsed.getCallsign());
		if (station != null) {
			if (station.hasPosition()) {
				LOG.info("APRS station store update callsign=" + station.getCallsign()
						+ " lat=" + station.getLatitude()
						+ " lon=" + station.getLongitude()
						+ " course=" + station.getCourse()
						+ " lastHeardMs=" + station.getLastHeardMs());
			}
			if (listener != null) {
				listener.onStationReceived(station);
			}
		}
	}

	@Override
	public void stopListener() {
		stopUpdates();
	}

	public boolean withinRadius(@NonNull AprsStation station) {
		if (!station.hasPosition()) {
			return true;
		}
		net.osmand.Location loc = app.getLocationProvider().getLastKnownLocation();
		if (loc == null) {
			return true;
		}
		float[] results = new float[1];
		Location.distanceBetween(loc.getLatitude(), loc.getLongitude(),
				station.getLatitude(), station.getLongitude(), results);
		return results[0] / 1000f <= radiusKm;
	}

	private synchronized void removeExpiredStations() {
		long cutoff = System.currentTimeMillis() - expiryMinutes * 60_000L;
		for (Iterator<Map.Entry<String, AprsStation>> it = stations.entrySet().iterator(); it.hasNext(); ) {
			AprsStation s = it.next().getValue();
			if (s.getLastHeardMs() < cutoff) {
				it.remove();
				if (listener != null) {
					listener.onStationRemoved(s);
				}
				LOG.debug("Removed expired APRS station " + s.getCallsign());
			}
		}
	}

	@NonNull
	public synchronized List<AprsStation> getStations() {
		return new ArrayList<>(stations.values());
	}

	@Nullable
	public synchronized AprsStation getStation(@NonNull String callsign) {
		return stations.get(callsign);
	}
}
