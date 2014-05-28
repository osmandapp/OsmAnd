package net.osmand.plus.osmo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.osmand.Location;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.osmo.OsMoGroupsStorage.OsMoDevice;

import org.json.JSONException;
import org.json.JSONObject;

public class OsMoTracker implements OsMoSender, OsMoReactor {
	private ConcurrentLinkedQueue<Location> bufferOfLocations = new ConcurrentLinkedQueue<Location>();
	private boolean startSendingLocations;
	private OsMoService service;
	private int locationsSent = 0;
	private OsmoTrackerListener uiTrackerListener = null;
	private OsmoTrackerListener trackerListener = null;
	private Location lastSendLocation;
	private Location lastBufferLocation;
	private CommonPreference<Integer> pref;
	private String sessionURL;
	private Map<String, OsMoDevice> trackingDevices = new java.util.concurrent.ConcurrentHashMap<String, OsMoGroupsStorage.OsMoDevice>();
	
	public interface OsmoTrackerListener {
		
		public void locationChange(String trackerId, Location location);
	}

	public OsMoTracker(OsMoService service, CommonPreference<Integer> pref) {
		this.service = service;
		this.pref = pref;
		service.registerSender(this);
		service.registerReactor(this);
	}
	
	public String getSessionURL() {
		if (!isEnabledTracker()) {
			return null;
		}
		return "http://test1342.osmo.mobi/u/" + sessionURL;
	}
	
	public boolean isEnabledTracker() {
		return startSendingLocations;
	}
	
	public void enableTracker() {
		if(!startSendingLocations) {
			startSendingLocations = true;
			service.pushCommand("TRACKER_SESSION_OPEN");
		}
	}
	
	public void disableTracker() {
		if(startSendingLocations) {
			startSendingLocations = false;
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
			StringBuilder cmd = new StringBuilder("T|");
			cmd.append("L").append((float)loc.getLatitude()).append(":").append((float)loc.getLongitude());
			if(loc.hasAccuracy()) {
				cmd.append("H").append((float)loc.getAccuracy());
			}
			if(loc.hasAltitude()) {
				cmd.append("A").append((float)loc.getAltitude());
			}
			if(loc.hasSpeed()) {
				cmd.append("S").append((float)loc.getSpeed());
			}
			if(loc.hasBearing()) {
				cmd.append("C").append((float)loc.getBearing());
			}
			if((System.currentTimeMillis() - loc.getTime()) > 30000 && loc.getTime() != 0) {
				cmd.append("T").append(loc.getTime());
			}
			locationsSent ++;
			return cmd.toString(); 
		}
		return null;
	}
	
	public Location getLastSendLocation() {
		return lastSendLocation;
	}

	public void sendCoordinate(Location location) {
		if(startSendingLocations && location != null) {
			long ltime = lastBufferLocation == null ? 0 : lastBufferLocation.getTime();
			
			if (location.getTime() - ltime  > pref.get()) {
				if(lastBufferLocation != null && (!lastBufferLocation.hasSpeed() || lastBufferLocation.getSpeed() < 1) &&
						lastBufferLocation.distanceTo(location) < 20){
					// ignores
					return;
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
			float lat = 0;
			float lon = 0;
			float speed = 0;
			int k = 0;
			for (int i = 1; i <= data.length(); i++) {
				boolean separator = i == data.length() || 
					!(Character.isDigit(data.charAt(i)) ||
							data.charAt(i) == ':' || data.charAt(i) == '.');
				if (separator) {
					char ch = data.charAt(k);
					String vl = data.substring(k + 1, i);
					if (ch == 'L') {
						int l = vl.indexOf(":");
						lat = Float.parseFloat(vl.substring(0, l));
						lon = Float.parseFloat(vl.substring(l + 1));
					} else if (ch == 'S') {
						speed = Float.parseFloat(vl);
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
					loc.setSpeed(speed);
				}
				if(trackerListener != null) {
					trackerListener.locationChange(tid, loc);
				}
				if(uiTrackerListener != null){
					uiTrackerListener.locationChange(tid, loc);
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
	
	public OsmoTrackerListener getUITrackerListener() {
		return uiTrackerListener;
	}
	
	public void setUITrackerListener(OsmoTrackerListener trackerListener) {
		this.uiTrackerListener = trackerListener;
	}

	public Collection<OsMoDevice> getTrackingDevices() {
		return trackingDevices.values();
	}

	

}
