package net.osmand.plus.osmo;

import org.json.JSONObject;

public interface OsMoReactor {

	public boolean acceptCommand(String command, String id, String data, JSONObject obj, OsMoThread tread);
	
	public String nextSendCommand(OsMoThread tracker);
	
	public void reconnect();
	
}