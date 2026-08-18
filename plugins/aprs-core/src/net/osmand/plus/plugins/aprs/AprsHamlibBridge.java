package net.osmand.plus.plugins.aprs;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sends rigctld set-frequency command when user taps a QRV station.
 */
public class AprsHamlibBridge {

	private static final org.apache.commons.logging.Log LOG =
			net.osmand.PlatformUtil.getLog(AprsHamlibBridge.class);

	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private String host = "localhost";
	private int port = 4532;
	private boolean enabled = false;

	public void setHost(@NonNull String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void tuneToFrequencyMhz(double frequencyMhz) {
		if (!enabled || frequencyMhz <= 0) {
			return;
		}
		long hz = Math.round(frequencyMhz * 1_000_000);
		executor.execute(() -> sendFrequency(hz));
	}

	private void sendFrequency(long hz) {
		String cmd = String.format(Locale.US, "F %d\n", hz);
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(host, port), 3000);
			socket.setSoTimeout(3000);
			OutputStream out = socket.getOutputStream();
			out.write(cmd.getBytes());
			out.flush();
			LOG.info("Hamlib rigctld: sent " + cmd.trim());
		} catch (IOException e) {
			LOG.warn("Hamlib rigctld unavailable: " + e.getMessage());
		}
	}

	public void shutdown() {
		executor.shutdownNow();
	}
}
