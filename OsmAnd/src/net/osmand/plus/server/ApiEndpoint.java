package net.osmand.plus.server;

import java.util.concurrent.Callable;

import fi.iki.elonen.NanoHTTPD;

public class ApiEndpoint{
	public String uri = "";
	public ApiCall apiCall;

	public NanoHTTPD.Response run(NanoHTTPD.IHTTPSession session){
		return apiCall.call(session);
	}

	public interface ApiCall{
		NanoHTTPD.Response call(NanoHTTPD.IHTTPSession session);
	}
}
