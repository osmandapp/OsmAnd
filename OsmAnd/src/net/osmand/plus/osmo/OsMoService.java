package net.osmand.plus.osmo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

public class OsMoService {
	private static String TRACKER_URL = "ws://srv.osmo.mobi:4242";
	private URLConnection conn;
	private OutputStreamWriter out;
	private BufferedReader in;

	public boolean isActive() {
		return conn != null;
	}

	public String activate(String hash) throws IOException {
		URL tu = new URL(TRACKER_URL);
		conn = tu.openConnection();
		conn.connect();
		in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
		out = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
		
		String t = sendCommand("auth|"+hash);
		t += sendCommand("session_open");
		return t;
	}
	
	private String sendCommand(String s) throws IOException {
		if(s.endsWith("\n")) {
			s += "\n";
		}
		out.write(s);
		return in.readLine();
	}
	
	public String sendCoordinate(double lat, double lon, float hdop, float alt, float speed, float bearing) throws IOException {
		return sendCommand("p|"+lat+":"+lon+":"+hdop+":"+alt+":"+speed+":"+bearing);
	}

	public String deactivate() throws IOException {
		String t = sendCommand("session_close");
		in.close();
		out.close();
		conn = null;
		return t;
	}

}
