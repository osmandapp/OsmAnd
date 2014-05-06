package net.osmand.plus.osmo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

import net.osmand.PlatformUtil;

import org.apache.commons.logging.Log;

public class OsMoService {
	private static String TRACKER_SERVER = "srv.osmo.mobi";
	private static int TRACKER_PORT = 4242;
	public static int NUMBER_OF_TRIES_TO_RECONNECT = 20;
	private Log log = PlatformUtil.getLog(OsMoService.class);
	private OutputStreamWriter out;
	private BufferedReader in;
	private Socket socket;
	private Queue<String> buffer = new LinkedList<String>();
	private int numberOfFailures = 0;
	private String loginHash;

	public boolean isActive() {
		return socket != null;
	}

	public String activate(String loginHash) throws IOException {
		this.loginHash = loginHash;
		socket = new Socket(TRACKER_SERVER, TRACKER_PORT);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
		out = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
		
		String t = sendCommand("auth|"+loginHash);
		t += sendCommand("session_open");
		return t;
	}
	
	private String sendCommand(String s) throws IOException {
		s = prepareCommand(s);
		log.info("OSMo command : " + s);
		out.write(s);
		return in.readLine();
	}

	private String prepareCommand(String s) {
		if(s.endsWith("\n")) {
			s += "\n";
		}
		return s;
	}
	
	public String sendCoordinate(double lat, double lon, float hdop, float alt, float speed, float bearing) throws IOException {
		return sendCommandWithBuffer("p|"+lat+":"+lon+":"+hdop+":"+alt+":"+speed+":"+bearing);
	}

	private synchronized String sendCommandWithBuffer(String command) throws IOException {
		buffer.add(command);
		String lastResponse = null;
		while (!buffer.isEmpty()) {
			reconnectIfNeeded();
			try {
				lastResponse = sendCommand(command);
			} catch (IOException e) {
				numberOfFailures++;
				e.printStackTrace();
			}
		}
		return lastResponse;
	}

	private void reconnectIfNeeded() throws IOException {
		if(numberOfFailures >= NUMBER_OF_TRIES_TO_RECONNECT) {
			deactivate();
			activate(this.loginHash);
			numberOfFailures = 0;
		}
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
