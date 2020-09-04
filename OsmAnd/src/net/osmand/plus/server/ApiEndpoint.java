package net.osmand.plus.server;

import fi.iki.elonen.NanoHTTPD;
import net.osmand.plus.OsmandApplication;

public interface ApiEndpoint {
	NanoHTTPD.Response process(NanoHTTPD.IHTTPSession session);

	void setApplication(OsmandApplication application);
}
