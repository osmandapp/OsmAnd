package net.osmand.plus.osmo;

import java.util.LinkedList;

import net.osmand.Location;
import net.osmand.plus.osmo.OsMoService.OsMoSender;

public class OsMoTracker implements OsMoSender {
	private LinkedList<Location> bufferOfLocations = new LinkedList<Location>(); 

	public OsMoTracker(OsMoService service) {
		service.registerSender(this);
	}

	@Override
	public String nextSendCommand() {
		if(!bufferOfLocations.isEmpty()){
			Location loc = bufferOfLocations.poll();
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
			return cmd.toString(); 
		}
		return null;
	}

	public void sendCoordinate(Location location) {
		bufferOfLocations.add(location);
	}

	public void sendCoordinate(double lat, double lon) {
		Location l = new Location("test");
		l.setLatitude(lat);
		l.setLongitude(lon);
		bufferOfLocations.add(l);
	}
	

}
