package net.osmand.plus.osmo;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.osmo.OsMoGroupsStorage.OsMoDevice;

import org.apache.commons.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class OsMoTracker implements OsMoReactor {
	// 130.5143667 Maximum is 8 digits and minimum 1
	private static final DecimalFormat floatingPointFormat = new DecimalFormat("0.0#######");
	private static final DecimalFormat twoDigitsFormat = new DecimalFormat("0.0#");
	private static final DecimalFormat integerFormat = new DecimalFormat("0");
	static {
		DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ENGLISH);
		floatingPointFormat.setDecimalFormatSymbols(symbols);
		twoDigitsFormat.setDecimalFormatSymbols(symbols);
		integerFormat.setDecimalFormatSymbols(symbols);
	}

	private ConcurrentLinkedQueue<Location> bufferOfLocations = new ConcurrentLinkedQueue<>();
	private OsMoService service;
	private int locationsSent = 0;
	private OsmoTrackerListener trackerListener = null;
	private Location lastSendLocation;
	private Location lastBufferLocation;
	private OsmandPreference<Integer> pref;
	private String sessionURL;
	private Map<String, OsMoDevice> trackingDevices = new java.util.concurrent.ConcurrentHashMap<>();
	private OsmandPreference<Boolean> stateSendLocation;
	protected static final Log LOG = PlatformUtil.getLog(OsMoTracker.class);

	public interface OsmoTrackerListener {
		void locationChange(String trackerId, Location location);
	}

	public OsMoTracker(OsMoService service, OsmandPreference<Integer> interval,
					   OsmandPreference<Boolean> stateSendLocation) {
		this.service = service;
		this.pref = interval;
		this.stateSendLocation = stateSendLocation;
		service.registerReactor(this);
	}

	public String getSessionURL() {
		if (!isEnabledTracker() || sessionURL == null) {
			return null;
		}
		return OsMoService.TRACK_URL + sessionURL;
	}

	public boolean isEnabledTracker() {
		return stateSendLocation.get();
	}

	public void enableTracker() {
		if (!isEnabledTracker()) {
			enableTrackerCmd();
		}
	}

	public void enableTrackerCmd() {
		stateSendLocation.set(true);
		service.pushCommand("TRACKER_SESSION_OPEN");
	}

	public void disableTracker() {
		if (isEnabledTracker()) {
			stateSendLocation.set(false);
			service.pushCommand("TRACKER_SESSION_CLOSE");
		}
	}

	public void startTrackingId(OsMoDevice d) {
		service.pushCommand("LISTEN:" + d.getTrackerId());
		trackingDevices.put(d.getTrackerId(), d);
	}

	public void stopTrackingId(OsMoDevice d) {
		service.pushCommand("UNLISTEN:" + d.getTrackerId());
		trackingDevices.remove(d.getTrackerId());
	}

	@Override
	public String nextSendCommand(OsMoThread thread) {
		if (!bufferOfLocations.isEmpty()) {
			Location loc = bufferOfLocations.poll();
			lastSendLocation = loc;
			locationsSent++;
			if ((System.currentTimeMillis() - loc.getTime()) > 2 * 60000 && loc.getTime() != 0) {
				return "B|" + formatLocation(loc);
			} else {
				return "T|" + formatLocation(loc);
			}
		}
		return null;
	}

	public static String formatLocation(Location loc) {
		StringBuffer cmd = new StringBuffer();
		cmd.append("L");
		floatingPointFormat.format(loc.getLatitude(), cmd, new FieldPosition(cmd.length()));
		cmd.append(":");
		floatingPointFormat.format(loc.getLongitude(), cmd, new FieldPosition(cmd.length()));
		if (loc.hasAccuracy()) {
			cmd.append("H");
			integerFormat.format(loc.getAccuracy(), cmd, new FieldPosition(cmd.length()));
		}
		if (loc.hasAltitude()) {
			cmd.append("A");
			integerFormat.format(loc.getAltitude(), cmd, new FieldPosition(cmd.length()));
		}
		if (loc.hasSpeed() && (int) (loc.getSpeed() * 100) != 0) {
			cmd.append("S");
			twoDigitsFormat.format(loc.getSpeed(), cmd, new FieldPosition(cmd.length()));
		}
		if (loc.hasBearing()) {
			cmd.append("C");
			integerFormat.format(loc.getBearing(), cmd, new FieldPosition(cmd.length()));
		}
		if (loc.getTime() != 0) {
			cmd.append("T");
			integerFormat.format(loc.getTime(), cmd, new FieldPosition(cmd.length()));
		}
		LOG.debug("formatLocation cmd=" + cmd);
		return cmd.toString();
	}

	public Location getLastSendLocation() {
		return lastSendLocation;
	}

	public void sendCoordinate(Location location) {
		if (stateSendLocation.get() && location != null) {
			long ltime = lastBufferLocation == null ? 0 : lastBufferLocation.getTime();

			if (location.getTime() - ltime > pref.get()) {
				if (lastBufferLocation != null && (!lastBufferLocation.hasSpeed() || lastBufferLocation.getSpeed() < 1) &&
						lastBufferLocation.distanceTo(location) < 20) {
					if (lastBufferLocation != null && location.getTime() - ltime < 60000) {
						// ignores
						return;
					}
				}
				lastBufferLocation = location;
				bufferOfLocations.add(location);
			}
		}
	}

	public int getLocationsSent() {
		return locationsSent;
	}

	public int getBufferLocationsSize() {
		return bufferOfLocations.size();
	}

	public void sendCoordinate(double lat, double lon) {
		Location l = new Location("test");
		l.setTime(System.currentTimeMillis());
		l.setLatitude(lat);
		l.setLongitude(lon);
		sendCoordinate(l);
	}


	@Override
	public boolean acceptCommand(String command, String tid, String data, JSONObject obj, OsMoThread thread) {
		switch (command) {
			case "LISTEN":
				return true;
			case "UNLISTEN":
				return true;
			case "TRACKER_SESSION_CLOSE":
				return true;
			case "TRACKER_SESSION_OPEN":
				try {
					sessionURL = obj.getString("url");
				} catch (JSONException e) {
					service.showErrorMessage(e.getMessage());
					e.printStackTrace();
				}
				return true;
			case "LT":
				double lat = 0;
				double lon = 0;
				double speed = 0;
				int k = 0;
				for (int i = 1; i <= data.length(); i++) {
					boolean separator = i == data.length() ||
							!(Character.isDigit(data.charAt(i)) ||
									data.charAt(i) == ':' || data.charAt(i) == '.' || data.charAt(i) == '-');
					if (separator) {
						char ch = data.charAt(k);
						String vl = data.substring(k + 1, i);
						if (ch == 'L') {
							int l = vl.indexOf(":");
							lat = Double.parseDouble(vl.substring(0, l));
							lon = Double.parseDouble(vl.substring(l + 1));
						} else if (ch == 'S') {
							speed = Double.parseDouble(vl);
						}
						k = i;
					}
				}
				if (lat != 0 || lon != 0) {
					Location loc = new Location("osmo");
					loc.setTime(System.currentTimeMillis());
					loc.setLatitude(lat);
					loc.setLongitude(lon);
					if (speed > 0) {
						loc.setSpeed((float) speed);
					}
					if (trackerListener != null) {
						trackerListener.locationChange(tid, loc);
					}
				}
				return true;
		}
		return false;
	}

	public void setTrackerListener(OsmoTrackerListener trackerListener) {
		this.trackerListener = trackerListener;
	}

	public OsmoTrackerListener getTrackerListener() {
		return trackerListener;
	}


	public Collection<OsMoDevice> getTrackingDevices() {
		return trackingDevices.values();
	}

	@Override
	public void onConnected() {
		if (stateSendLocation.get()) {
			enableTrackerCmd();
		}
	}

	@Override
	public void onDisconnected(String msg) {
	}


}
