package net.osmand.plus.plugins.aprsdriver;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;

import org.apache.commons.logging.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Consumes paced IQ chunks from a queue and runs streaming {@link AprsSdrDsp} demod.
 */
public class AprsSdrDspThread extends Thread {

	private static final Log LOG = PlatformUtil.getLog(AprsSdrDspThread.class);

	public static final AprsSdrThread.IqChunk POISON = new AprsSdrThread.IqChunk(null, 0, 0);

	private final BlockingQueue<AprsSdrThread.IqChunk> queue;
	private final AprsSdrDsp dsp;
	private final AtomicBoolean running = new AtomicBoolean(true);

	public AprsSdrDspThread(@NonNull BlockingQueue<AprsSdrThread.IqChunk> queue,
	                        @NonNull AprsSdrDsp dsp) {
		super("AprsSdrDspThread");
		setDaemon(true);
		this.queue = queue;
		this.dsp = dsp;
	}

	@Override
	public void run() {
		try {
			while (running.get()) {
				AprsSdrThread.IqChunk chunk = queue.take();
				if (chunk.samples == null) {
					break;
				}
				dsp.processInterleavedIq(chunk.samples, chunk.sampleCount, chunk.rfRate);
				dsp.extractNewFramesIfReady(false);
			}
			dsp.flush();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			LOG.error("AprsSdrDspThread error: " + e.getMessage());
		}
	}

	public void requestStop() {
		running.set(false);
		interrupt();
	}
}
