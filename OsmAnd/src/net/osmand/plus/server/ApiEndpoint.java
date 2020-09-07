package net.osmand.plus.server;

import fi.iki.elonen.NanoHTTPD;
import net.osmand.plus.activities.MapActivity;

public interface ApiEndpoint {
	NanoHTTPD.Response process(NanoHTTPD.IHTTPSession session);

	void setMapActivity(MapActivity mapActivity);
}
