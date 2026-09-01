package net.osmand.plus.plugins.aprsdriver;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.aprs.AprsPlugin;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class AprsSdrThread extends Thread implements AprsSdrDsp.FrameListener {

	private static final Log LOG = PlatformUtil.getLog(AprsSdrThread.class);
	private static final int QUEUE_CAPACITY = 8;
	private static final int CALIBRATION_SECONDS = 5;

	public static final class IqChunk {
		@Nullable
		public final short[] samples;
		public final int sampleCount;
		public final int rfRate;

		public IqChunk(@Nullable short[] samples, int sampleCount, int rfRate) {
			this.samples = samples;
			this.sampleCount = sampleCount;
			this.rfRate = rfRate;
		}
	}

	private volatile boolean running;
	private volatile boolean replayCompleted;
	private AprsSdrDsp dsp;
	private UsbDeviceConnection connection;
	private UsbEndpoint bulkIn;
	private File iqReplayFile;
	private int rfSampleRate = AprsSdrDsp.DEFAULT_RF_RATE;
	private LinkedBlockingQueue<IqChunk> sampleQueue;
	private AprsSdrDspThread dspThread;

	public AprsSdrThread() {
		super("AprsSdrThread");
		setDaemon(false);
	}

	public void startUsbCapture(@NonNull UsbDeviceConnection connection, @NonNull UsbEndpoint bulkIn) {
		this.connection = connection;
		this.bulkIn = bulkIn;
		this.iqReplayFile = null;
		this.dsp = new AprsSdrDsp(this);
		this.dsp.resetStreamState();
		this.running = true;
		if (!isAlive()) {
			start();
		}
	}

	public void startFileReplay(@NonNull File wavFile, int ifOffsetHz) {
		if (isAlive()) {
			stopCapture();
			try {
				join(5000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		this.iqReplayFile = wavFile;
		this.connection = null;
		this.replayCompleted = false;
		this.dsp = new AprsSdrDsp(this, ifOffsetHz);
		this.dsp.resetForReplay();
		this.running = true;
		start();
	}

	public void stopCapture() {
		running = false;
		if (dspThread != null) {
			dspThread.requestStop();
		}
		interrupt();
	}

	public void awaitFinish(long timeoutMs) throws InterruptedException {
		join(timeoutMs);
	}

	@Override
	public void run() {
		try {
			if (iqReplayFile != null) {
				replayWav(iqReplayFile);
			} else if (connection != null && bulkIn != null) {
				usbLoop();
			}
		} catch (Exception e) {
			LOG.error("AprsSdrThread error: " + e.getMessage());
		} finally {
			stopDspThread();
			if (connection != null) {
				connection.close();
			}
			if (replayCompleted) {
				refreshMapAfterReplay();
			}
		}
	}

	private void stopDspThread() {
		if (sampleQueue != null) {
			sampleQueue.offer(AprsSdrDspThread.POISON);
		}
		if (dspThread != null) {
			try {
				dspThread.join(120_000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			dspThread = null;
		}
		sampleQueue = null;
	}

	private void refreshMapAfterReplay() {
		AprsPlugin plugin = PluginsHelper.getPlugin(AprsPlugin.class);
		if (plugin == null) {
			return;
		}
		OsmandApplication app = plugin.getApplication();
		app.runInUIThread(() -> {
			plugin.updateLayers(app, null);
			int count = plugin.getDataManager().getStations().size();
			LOG.info("IQ replay finished; APRS stations=" + count
					+ " frames=" + (dsp != null ? dsp.getEmittedFrameCount() : 0));
			app.getOsmandMap().getMapView().refreshMap();
		});
	}

	private void usbLoop() {
		byte[] buf = new byte[32768];
		while (running) {
			int n = connection.bulkTransfer(bulkIn, buf, buf.length, 1000);
			if (n > 0) {
				short[] iq = bytesToShorts(buf, n);
				dsp.processInterleavedIq(iq, iq.length, rfSampleRate);
				dsp.extractNewFramesIfReady(false);
			}
		}
		dsp.flush();
	}

	private void replayWav(@NonNull File file) throws Exception {
		long replayStartMs = System.currentTimeMillis();
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			int rfRate = readWavSampleRate(raf);
			raf.seek(44);
			calibrateFromFileStart(raf, rfRate);
			raf.seek(44);

			sampleQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
			dspThread = new AprsSdrDspThread(sampleQueue, dsp);
			dspThread.start();

			byte[] chunk = new byte[8192];
			long replayStartNs = System.nanoTime();
			long iqSamplesDelivered = 0;
			LOG.info("IQ replay pacing enabled at " + rfRate + " Hz (DSP on separate thread)");
			while (running) {
				int n = raf.read(chunk);
				if (n <= 0) {
					replayCompleted = true;
					break;
				}
				short[] iq = bytesToShorts(chunk, n);
				IqChunk iqChunk = new IqChunk(iq, iq.length, rfRate);
				if (!sampleQueue.offer(iqChunk, 5, TimeUnit.SECONDS)) {
					LOG.warn("APRS IQ sample queue full; DSP falling behind real-time pace");
				}
				iqSamplesDelivered += iq.length / 2L;
				long targetElapsedNs = iqSamplesDelivered * 1_000_000_000L / rfRate;
				long actualElapsedNs = System.nanoTime() - replayStartNs;
				long sleepNs = targetElapsedNs - actualElapsedNs;
				if (sleepNs > 1_000_000L) {
					long sleepMs = sleepNs / 1_000_000L;
					int sleepExtraNs = (int) (sleepNs % 1_000_000L);
					Thread.sleep(sleepMs, sleepExtraNs);
				}
			}
		} finally {
			stopDspThread();
		}
		long wallMs = System.currentTimeMillis() - replayStartMs;
		LOG.info("IQ replay wall time " + wallMs + " ms");
	}

	private void calibrateFromFileStart(@NonNull RandomAccessFile raf, int rfRate) throws Exception {
		byte[] cal = new byte[rfRate * CALIBRATION_SECONDS * 4];
		int read = 0;
		while (read < cal.length) {
			int n = raf.read(cal, read, cal.length - read);
			if (n <= 0) {
				break;
			}
			read += n;
		}
		if (read > 0) {
			short[] iq = bytesToShorts(cal, read);
			dsp.calibrateFromInterleavedIq(iq, iq.length, rfRate);
			dsp.resetStreamState();
		}
	}

	private int readWavSampleRate(@NonNull RandomAccessFile raf) throws Exception {
		byte[] header = new byte[44];
		raf.readFully(header);
		if (header[0] != 'R' || header[1] != 'I' || header[2] != 'F' || header[3] != 'F') {
			return AprsSdrDsp.DEFAULT_RF_RATE;
		}
		return ByteBuffer.wrap(header, 24, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
	}

	@NonNull
	private short[] bytesToShorts(@NonNull byte[] data, int len) {
		short[] out = new short[len / 2];
		ByteBuffer bb = ByteBuffer.wrap(data, 0, len).order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < out.length; i++) {
			out[i] = bb.getShort();
		}
		return out;
	}

	@Override
	public void onAx25Frame(@NonNull byte[] frame) {
		AprsPlugin plugin = PluginsHelper.getPlugin(AprsPlugin.class);
		if (plugin == null) {
			return;
		}
		byte[] copy = frame.clone();
		OsmandApplication app = plugin.getApplication();
		app.runInUIThread(() -> {
			if (!plugin.isEnabled()) {
				PluginsHelper.enablePlugin(null, app, plugin, true);
			}
			plugin.ingestAx25Frame(copy);
		});
	}
}
