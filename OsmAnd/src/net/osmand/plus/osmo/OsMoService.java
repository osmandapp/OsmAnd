package net.osmand.plus.osmo;

import java.util.List;

import org.json.JSONObject;

import net.osmand.plus.OsmandApplication;

public class OsMoService {
	private OsMoThread thread;
	private List<OsMoSender> listSenders = new java.util.concurrent.CopyOnWriteArrayList<OsMoSender>();
	private List<OsMoReactor> listReactors = new java.util.concurrent.CopyOnWriteArrayList<OsMoReactor>();
	private OsmandApplication app;
	
	public interface OsMoSender {
		
		public String nextSendCommand(OsMoThread tracker);
	}
	
	public interface OsMoReactor {
		
		public boolean acceptCommand(String command, String data, JSONObject obj, OsMoThread tracker);
		
	}
	
	public OsMoService(OsmandApplication app) {
		this.app = app;
	}
	
	public boolean isConnected() {
		return thread != null && thread.isConnected();
	}
	
	public boolean connect(boolean forceReconnect) {
		if(thread != null) {
			if(!forceReconnect ) {
				return isConnected();
			}
			thread.stopConnection();
		}
		thread = new OsMoThread(app, listSenders, listReactors);
		return true;
	}
	
	public void disconnect() {
		if(thread != null) {
			thread.stopConnection();
		}
	}
	
	public void registerSender(OsMoSender sender) {
		if(!listSenders.contains(sender)) {
			listSenders.add(sender);
		}
	}
	
	public void registerReactor(OsMoReactor reactor) {
		if(!listReactors.contains(reactor)) {
			listReactors.add(reactor);
		}
	}

	public void removeSender(OsMoSender s) {
		listSenders.remove(s);
	}
	
	public void removeReactor(OsMoReactor s) {
		listReactors.remove(s);
	}
	

}
