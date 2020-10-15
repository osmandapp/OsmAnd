package net.osmand.plus.osmedit.utils.ops;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PerformanceMetrics {
	private static final PerformanceMetrics inst = new PerformanceMetrics();
	public static final int METRICS_COUNT = 2;
	public static PerformanceMetrics i() {
		return inst;
	}
	private final Map<String, PerformanceMetric> metrics = new ConcurrentHashMap<String, PerformanceMetric>();
	private final AtomicInteger ids = new AtomicInteger();
	private final PerformanceMetric DISABLED = new PerformanceMetric(-1, "<disabled>");
	private boolean enabled = true;
	private PerformanceMetric overhead;
	
	private PerformanceMetrics() {
		overhead = getByKey("_overhead");
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	public Map<String, PerformanceMetric> getMetrics() {
		return metrics;
	}

	public PerformanceMetric getMetric(String prefix, String key) {
		if(!enabled) {
			return DISABLED;
		}
		return getMetric(prefix + "." + key);
	}
	
	public void reset(int c) {
		for(PerformanceMetric p : metrics.values()) {
			p.reset(c);
		}
	}
	
	
	public PerformanceMetric getMetric(String key) {
		if(!enabled) {
			return DISABLED;
		}
		long s = System.nanoTime();
		PerformanceMetric pm = getByKey(key);
		overhead.capture(System.nanoTime() - s);
		return pm;
	}

	private PerformanceMetric getByKey(String key) {
		PerformanceMetric pm = metrics.get(key);
		if(pm == null) {
			pm = new PerformanceMetric(ids.incrementAndGet(), key);
			metrics.put(key, pm);
		}
		return pm;
	}
	
	
	public final class PerformanceMetric {
		final String name;
		final int id;
		String description;
		AtomicInteger invocations = new AtomicInteger();
		AtomicLong totalDuration = new AtomicLong();
		AtomicInteger invocationsA = new AtomicInteger();
		AtomicLong totalDurationA = new AtomicLong();
		AtomicInteger invocationsB = new AtomicInteger();
		AtomicLong totalDurationB = new AtomicLong();

		
		
		private PerformanceMetric(int id, String name) {
			this.id = id;
			this.name = name;
		}
		
		public Metric start() {
			if(id == -1) {
				return Metric.EMPTY;
			}
			return new Metric(this);
		}
		
		public String getName() {
			return name;
		}
		
		public void setDescription(String description) {
			this.description = description;
		}
		
		public String getDescription() {
			return description;
		}
		
		public void reset(int c) {
			if(c == 1) {
				invocationsA.set(0);
				totalDurationA.set(0);
			} else if(c == 2) {
				totalDurationB.set(0);
				invocationsB.set(0);
			}
		}
		
		public int getInvocations(int c) {
			if(c == 1) {
				return invocationsA.get();
			} else if(c == 2) {
				return invocationsB.get();
			}
			return invocations.get();
		}
		
		public long getDuration(int c) {
			if(c == 1) {
				return totalDurationA.get();
			} else if(c == 2) {
				return totalDurationB.get();
			}
			return totalDuration.get();
		}
		
		public int getId() {
			return id;
		}
		
		long capture(long d) {
			invocations.incrementAndGet();
			totalDuration.addAndGet(d);
			invocationsA.incrementAndGet();
			totalDurationA.addAndGet(d);
			invocationsB.incrementAndGet();
			totalDurationB.addAndGet(d);
			return d;
		}
	}
	
	public static final class Metric {
		long start;
		PerformanceMetric m;
		boolean e;
		public static final Metric EMPTY = new Metric(true);
		private Metric(PerformanceMetric m) {
			start = System.nanoTime();
			this.m = m;
		}
		
		private Metric(boolean empty) {
			e = empty;
		}
		
		public long capture() {
			if(e) {
				return 0;
			}
			return m.capture(System.nanoTime() - start);
		}
	}
}
