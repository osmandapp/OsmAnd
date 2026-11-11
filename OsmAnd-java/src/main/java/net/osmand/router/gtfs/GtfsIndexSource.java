package net.osmand.router.gtfs;

import java.io.File;
import java.io.IOException;

import net.osmand.PlatformUtil;

import org.apache.commons.logging.Log;

/**
 * Opens the index lazily on first use and keeps it. The file is checked on every {@link #get()} (size and
 * modification time), so an atomic swap by the importer, or a delete, is noticed without a notification.
 * A file that cannot be opened is not retried until it changes.
 */
public final class GtfsIndexSource {

	private static final Log LOG = PlatformUtil.getLog(GtfsIndexSource.class);

	/** An open index with what is derived from it once. */
	public static final class Loaded {
		public final GtfsIndex index;
		public final GtfsRoutePlanner planner;
		public final GtfsCoverage coverage;

		Loaded(GtfsIndex index) {
			this.index = index;
			this.planner = new GtfsRoutePlanner(index);
			this.coverage = GtfsCoverage.of(index);
		}
	}

	private final File file;
	private Loaded loaded;
	private long stampLength = -1, stampModified = -1;
	private IOException lastError;

	public GtfsIndexSource(File file) {
		this.file = file;
	}

	/** The open index, or null when there is none or it is unusable (see {@link #getLastError()}). */
	public synchronized Loaded get() {
		long len = file.isFile() ? file.length() : -1;
		long mod = len >= 0 ? file.lastModified() : -1;
		if (len != stampLength || mod != stampModified) {
			stampLength = len;
			stampModified = mod;
			loaded = null;
			lastError = null;
			if (len >= 0) {
				try {
					loaded = new Loaded(GtfsIndex.open(file));
					if (loaded.coverage == null) {
						loaded = null;
					}
				} catch (IOException | RuntimeException e) {
					LOG.error("Cannot open GTFS index " + file, e);
					lastError = e instanceof IOException ? (IOException) e : new IOException(e);
				}
			}
		}
		return loaded;
	}

	/** Why the last attempt to open the index failed, or null. */
	public synchronized IOException getLastError() {
		return lastError;
	}

	/** Forgets the open index; the next {@link #get()} opens the file again. */
	public synchronized void invalidate() {
		stampLength = -1;
		stampModified = -1;
		loaded = null;
		lastError = null;
	}
}
