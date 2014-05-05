package net.osmand.plus.osmo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class OsMoService {
	private static String TRACKER_SERVER = "srv.osmo.mobi";
	private static int TRACKER_PORT = 4242;
	private OutputStreamWriter out;
	private BufferedReader in;
	private Socket socket;

	public boolean isActive() {
		return socket != null;
	}

	public String activate(String hash) throws IOException {
		socket = new Socket(TRACKER_SERVER, TRACKER_PORT);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
		out = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
		
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
		socket.close();
		socket = null;
		return t;
	}

}
