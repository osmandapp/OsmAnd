package net.osmand.plus.feedback;

import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StackSampler {

	private static final long SAMPLE_INTERVAL = 2 * 1000L;
	private static final int TOP = 10;

	private final Map<String, Integer> counts = new HashMap<>();
	private long lastSampleTime;
	private int samples;
	private long sampleMs;
	private long maxSampleMs;

	public void sample() {
		long time = SystemClock.elapsedRealtime();
		if (time - lastSampleTime < SAMPLE_INTERVAL) {
			return;
		}
		lastSampleTime = time;
		try {
			Thread main = Looper.getMainLooper().getThread();
			Thread[] threads = new Thread[Thread.activeCount() + 16];
			int count = Thread.enumerate(threads);
			for (int i = 0; i < count; i++) {
				Thread thread = threads[i];
				String key = thread.getState() == Thread.State.RUNNABLE && thread != Thread.currentThread()
						? stackKey(thread.getStackTrace()) : null;
				if (key != null) {
					key = thread == main ? "main/" + key : key;
					Integer was = counts.get(key);
					counts.put(key, was == null ? 1 : was + 1);
				}
			}
		} catch (RuntimeException | OutOfMemoryError ignored) {
		}
		long ms = SystemClock.elapsedRealtime() - time;
		samples++;
		sampleMs += ms;
		maxSampleMs = Math.max(maxSampleMs, ms);
	}

	@Nullable
	public String drain() {
		if (samples == 0) {
			return null;
		}
		StringBuilder sb = new StringBuilder(" hotn=").append(samples)
				.append(" hotms=").append(sampleMs).append('/').append(maxSampleMs);
		List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
		entries.sort((a, b) -> b.getValue() - a.getValue());
		for (int i = 0; i < entries.size() && i < TOP; i++) {
			sb.append(i > 0 ? "," : " hot=").append(entries.get(i).getKey()).append(':').append(entries.get(i).getValue());
		}
		counts.clear();
		samples = 0;
		sampleMs = 0;
		maxSampleMs = 0;
		return sb.toString();
	}

	@Nullable
	private String stackKey(StackTraceElement[] stack) {
		if (stack.length > 0 && stack[stack.length - 1].getClassName().endsWith("$EGLThread")) {
			return "render";
		}
		StringBuilder sb = new StringBuilder();
		int found = 0;
		StackTraceElement entry = null;
		for (StackTraceElement frame : stack) {
			if (frame.getClassName().startsWith("net.osmand.")) {
				if (found < 2) {
					sb.append(found > 0 ? "<" : "").append(frameName(frame));
				} else {
					entry = frame;
				}
				found++;
			}
		}
		if (entry != null) {
			sb.append(found > 3 ? "<..<" : "<").append(frameName(entry));
		}
		return found > 0 ? sb.toString() : null;
	}

	private String frameName(StackTraceElement frame) {
		String cls = frame.getClassName();
		return cls.substring(cls.lastIndexOf('.') + 1) + '.' + frame.getMethodName();
	}
}
