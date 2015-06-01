package net.osmand.plus.osmo;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.osmand.Location;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.osmo.OsMoGroupsStorage.OsMoDevice;

import org.json.JSONException;
import org.json.JSONObject;

public class OsMoTracker implements OsMoReactor {
	private ConcurrentLinkedQueue<Location> bufferOfLocations = new ConcurrentLinkedQueue<Location>();
	private OsMoService service;
	private int locationsSent = 0;
	private OsmoTrackerListener trackerListener = null;
	private Location lastSendLocation;
	private Location lastBufferLocation;
	private OsmandPreference<Integer> pref;
	private String sessionURL;
	private Map<String, OsMoDevice> trackingDevices = new java.util.concurrent.ConcurrentHashMap<String, OsMoGroupsStorage.OsMoDevice>();
	private OsmandPreference<Boolean> stateSendLocation;
	
	public interface OsmoTrackerListener {
		
		public void locationChange(String trackerId, Location location);
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
		if(!isEnabledTracker()) {
			enableTrackerCmd();
		}
	}
	
	public void enableTrackerCmd() {
		stateSendLocation.set(true);
		service.pushCommand("TRACKER_SESSION_OPEN");
	}
	
	public void disableTracker() {
		if(isEnabledTracker()) {
			stateSendLocation.set(false);
			service.pushCommand("TRACKER_SESSION_CLOSE");
		}
	}
	
	public void startTrackingId(OsMoDevice d) {
		service.pushCommand("LISTEN:"+d.getTrackerId());
		trackingDevices.put(d.getTrackerId(), d);
	}
	
	public void stopTrackingId(OsMoDevice d) {
		service.pushCommand("UNLISTEN:"+d.getTrackerId());
		trackingDevices.remove(d.getTrackerId());
	}

	@Override
	public String nextSendCommand(OsMoThread thread) {
		if(!bufferOfLocations.isEmpty()){
			Location loc = bufferOfLocations.poll();
			lastSendLocation = loc;
			locationsSent ++;
			if((System.currentTimeMillis() - loc.getTime()) > 2 * 60000 && loc.getTime() != 0) {
				return "B|"+formatLocation(loc); 
			} else {
				return "T|"+formatLocation(loc); 
			}
		}
		return null;
	}

	public static String formatLocation(Location loc) {
		StringBuilder cmd = new StringBuilder();
		cmd.append("L").append((float)loc.getLatitude()).append(":").append((float)loc.getLongitude());
		if(loc.hasAccuracy()) {
			cmd.append("H").append((int)loc.getAccuracy());
		}
		if(loc.hasAltitude()) {
			cmd.append("A").append((int)loc.getAltitude());
		}
		if(loc.hasSpeed()) {
			cmd.append("S").append((float)((int)(loc.getSpeed()*100))/100f);
		}
		if(loc.hasBearing()) {
			cmd.append("C").append((int)loc.getBearing());
		}
		if((System.currentTimeMillis() - loc.getTime()) > 30000 && loc.getTime() != 0) {
			cmd.append("T").append(loc.getTime());
		}
		return cmd.toString();
	}
	
	public Location getLastSendLocation() {
		return lastSendLocation;
	}

	public void sendCoordinate(Location location) {
		if(stateSendLocation.get() && location != null) {
			long ltime = lastBufferLocation == null ? 0 : lastBufferLocation.getTime();
			
			if (location.getTime() - ltime  > pref.get()) {
				if(lastBufferLocation != null && (!lastBufferLocation.hasSpeed() || lastBufferLocation.getSpeed() < 1) &&
						lastBufferLocation.distanceTo(location) < 20){
					if(lastBufferLocation != null && location.getTime() - ltime < 60000) {
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
		if(command.equals("LISTEN")) {
			return true;
		} else if(command.equals("UNLISTEN")) {
			return true;
		} else if(command.equals("TRACKER_SESSION_CLOSE")) {
			return true;
		} else if(command.equals("TRACKER_SESSION_OPEN")) {
			try {
				sessionURL = obj.getString("url");
			} catch (JSONException e) {
				service.showErrorMessage(e.getMessage());
				e.printStackTrace();
			}
			return true;
		} else if(command.equals("LT")) {
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
			if(lat != 0 || lon != 0) {
				Location loc = new Location("osmo");
				loc.setTime(System.currentTimeMillis());
				loc.setLatitude(lat);
				loc.setLongitude(lon);
				if(speed > 0) {
					loc.setSpeed((float) speed);
				}
				if(trackerListener != null) {
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
		if(stateSendLocation.get()) {
			enableTrackerCmd();
		}
	}

	@Override
	public void onDisconnected(String msg) {
	}


}
