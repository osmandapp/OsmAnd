package net.osmand.plus.osmo;

import java.util.List;

public class OsMoService {
	//public static int NUMBER_OF_TRIES_TO_RECONNECT = 20;
	
	private OsMoThread thread;
	private List<OsMoSender> listSenders = new java.util.concurrent.CopyOnWriteArrayList<OsMoSender>();
	private List<OsMoReactor> listReactors = new java.util.concurrent.CopyOnWriteArrayList<OsMoReactor>();
	
	public interface OsMoSender {
		
		public String nextSendCommand();
	}
	
	public interface OsMoReactor {
		
		public boolean acceptCommand(String command);
		
	}
	
	public OsMoService() {
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
		thread = new OsMoThread(listSenders, listReactors);
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
