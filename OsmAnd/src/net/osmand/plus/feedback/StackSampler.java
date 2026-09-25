package net.osmand.plus.feedback;

import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StackSampler {

	private static final long SAMPLE_INTERVAL = 3 * 1000L;
	private static final int TOP = 10;

	private final Map<String, Integer> counts = new HashMap<>();
	private long lastSampleTime;

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
					synchronized (counts) {
						Integer was = counts.get(key);
						counts.put(key, was == null ? 1 : was + 1);
					}
				}
			}
		} catch (RuntimeException | OutOfMemoryError ignored) {
		}
	}

	@Nullable
	public String drain() {
		synchronized (counts) {
			if (counts.isEmpty()) {
				return null;
			}
			List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
			entries.sort((a, b) -> b.getValue() - a.getValue());
			StringBuilder sb = new StringBuilder(" hot=");
			for (int i = 0; i < entries.size() && i < TOP; i++) {
				sb.append(i > 0 ? "," : "").append(entries.get(i).getKey()).append(':').append(entries.get(i).getValue());
			}
			counts.clear();
			return sb.toString();
		}
	}

	@Nullable
	private String stackKey(StackTraceElement[] stack) {
		StringBuilder sb = new StringBuilder();
		int found = 0;
		for (StackTraceElement frame : stack) {
			String cls = frame.getClassName();
			if (cls.startsWith("net.osmand.")) {
				sb.append(found > 0 ? "<" : "").append(cls.substring(cls.lastIndexOf('.') + 1)).append('.').append(frame.getMethodName());
				if (++found == 2) {
					break;
				}
			}
		}
		return found > 0 ? sb.toString() : null;
	}
}
