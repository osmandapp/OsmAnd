package net.osmand.router.gtfs.selftest;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.LongSupplier;

import net.osmand.router.gtfs.GtfsFeedImporter;

/**
 * Everything a scenario needs from its host (the Android app or a JVM test): where to work, which feed zips
 * to use, the clock, and optional probes. Scenarios never reach for the platform themselves.
 */
public final class SelfTestEnv {

	/** Log line sink (logcat on Android, stdout on the JVM). */
	public interface Sink {
		void log(String line);
	}

	/** Extra memory figures the platform can give (native heap on Android). Values in bytes, -1 when unknown. */
	public interface MemoryProbe {
		Map<String, Long> snapshot();
	}

	/** How the device itself formats a clock time (the app's own helper on Android). */
	public interface ClockProbe {
		boolean is24Hour();

		String format(long epochSec, ZoneId zone);
	}

	/** Scratch directory; everything below it may be deleted at any time. */
	public final File workDir;
	public final List<File> zips;
	public LongSupplier nowSec = () -> System.currentTimeMillis() / 1000;
	public Sink sink = line -> {
	};
	public MemoryProbe memoryProbe;
	public ClockProbe deviceClock;
	public Locale locale = Locale.getDefault();
	/** An index that exists already and may be used for read-only scenarios (the app's real one), or null. */
	public File preferredIndex;
	/** scenario options, from the broadcast extra (for example constrained=1). */
	public final Map<String, String> options = new LinkedHashMap<>();
	/** Facts about the host for the report header. */
	public final Map<String, Object> info = new LinkedHashMap<>();

	private File sharedIndex;

	public SelfTestEnv(File workDir, List<File> zips) {
		this.workDir = workDir;
		this.zips = zips;
	}

	public void log(String line) {
		sink.log(line);
	}

	public boolean option(String key) {
		String v = options.get(key);
		return v != null && !v.equals("0") && !v.equalsIgnoreCase("false");
	}

	public void setSharedIndex(File f) {
		sharedIndex = f;
	}

	/**
	 * An index for the read-only scenarios: the one import-all built in this run, else the preferred (real)
	 * one, else a fresh one built from the zips into the work directory. Null when there is no feed at all.
	 */
	public synchronized File ensureIndex() throws IOException {
		if (sharedIndex != null && sharedIndex.isFile()) {
			return sharedIndex;
		}
		if (preferredIndex != null && preferredIndex.isFile()) {
			return preferredIndex;
		}
		if (zips.isEmpty()) {
			return null;
		}
		File dir = new File(workDir, "shared-index");
		SelfTestUtil.deleteTree(dir);
		File idx = new File(dir, "gtfs.index");
		log("building a shared index from " + zips.size() + " zips");
		new GtfsFeedImporter(null).importFeeds(new ArrayList<>(zips), idx);
		sharedIndex = idx;
		return idx;
	}

	/** Removes the scratch data of this run. */
	public void cleanup() {
		SelfTestUtil.deleteTree(workDir);
	}
}
