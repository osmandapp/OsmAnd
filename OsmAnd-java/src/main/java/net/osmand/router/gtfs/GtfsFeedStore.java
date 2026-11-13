package net.osmand.router.gtfs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The imported GTFS zips and the index built from all of them, kept in one directory:
 * {@code feeds/NAME.zip} and {@code gtfs.index}. The zips are kept so the index can be rebuilt (after a
 * format change, or when a feed is removed) without asking for the files again.
 * <p>
 * Not thread safe: the caller runs one change at a time.
 */
public final class GtfsFeedStore {

	public static final String INDEX_NAME = "gtfs.index";
	private static final String FEEDS_DIR = "feeds";
	private static final String PART_SUFFIX = ".part";
	private static final String REMOVED_SUFFIX = ".removed";
	private static final byte[] ZIP_MAGIC = {'P', 'K', 3, 4};
	/** Slack on top of the zip size, so a copy does not take the last free megabyte. */
	private static final long SPACE_MARGIN = 16L << 20;

	/** An imported zip. */
	public static final class Feed {
		public final String name;
		public final long sizeBytes, modifiedMillis;

		Feed(File f) {
			this.name = f.getName();
			this.sizeBytes = f.length();
			this.modifiedMillis = f.lastModified();
		}
	}

	/** The copy was stopped by the user. */
	public static final class CancelledException extends IOException {
		CancelledException() {
			super("Import cancelled");
		}
	}

	private final File dir;

	public GtfsFeedStore(File dir) {
		this.dir = dir;
	}

	public File getIndexFile() {
		return new File(dir, INDEX_NAME);
	}

	public boolean hasIndex() {
		return getIndexFile().isFile();
	}

	private File feedsDir() {
		return new File(dir, FEEDS_DIR);
	}

	public List<Feed> listFeeds() {
		File[] files = feedsDir().listFiles((d, n) -> n.endsWith(".zip"));
		if (files == null) {
			return Collections.emptyList();
		}
		Arrays.sort(files);
		List<Feed> res = new ArrayList<>();
		for (File f : files) {
			if (f.isFile()) {
				res.add(new Feed(f));
			}
		}
		return res;
	}

	/**
	 * Deletes what an interrupted change leaves behind, for example when the app is killed: half copied zips,
	 * the temporary files of an index build, and a feed that was set aside for removal is put back.
	 * Call it only while no change is running.
	 */
	public void deleteLeftovers() {
		File[] files = feedsDir().listFiles();
		if (files != null) {
			for (File f : files) {
				if (f.getName().endsWith(PART_SUFFIX)) {
					f.delete();
				} else if (f.getName().endsWith(REMOVED_SUFFIX)) {
					String name = f.getName().substring(0, f.getName().length() - REMOVED_SUFFIX.length());
					f.renameTo(new File(feedsDir(), name));
				}
			}
		}
		File[] temps = dir.listFiles((d, n) -> n.startsWith(INDEX_NAME + ".tmp"));
		if (temps != null) {
			for (File f : temps) {
				f.delete();
			}
		}
	}

	/**
	 * A safe file name for a display name given by a document provider: no path parts, only letters, digits,
	 * dot, dash and underscore, and a .zip ending.
	 */
	public static String safeName(String displayName) {
		String n = displayName == null ? "" : displayName.trim();
		int slash = Math.max(n.lastIndexOf('/'), n.lastIndexOf('\\'));
		n = n.substring(slash + 1);
		if (n.toLowerCase(Locale.ROOT).endsWith(".zip")) {
			n = n.substring(0, n.length() - 4);
		}
		n = n.replaceAll("[^A-Za-z0-9._-]", "_").replaceAll("^\\.+", "");
		if (n.length() > 80) {
			n = n.substring(0, 80);
		}
		return (n.isEmpty() ? "feed" : n) + ".zip";
	}

	/**
	 * Copies a feed into the store under a name derived from displayName (a number is added if the name is
	 * taken; importing the same name again does not overwrite). Fails before copying if the volume is too
	 * small for expectedSize (pass -1 when unknown) and after copying if the data is not a zip. The cancel flag
	 * is polled between blocks.
	 */
	public File addFeed(String displayName, InputStream in, long expectedSize, java.util.function.BooleanSupplier cancelled)
			throws IOException {
		File feeds = feedsDir();
		if (!feeds.isDirectory() && !feeds.mkdirs()) {
			throw new IOException("Cannot create " + feeds);
		}
		if (expectedSize > 0) {
			long free = feeds.getUsableSpace();
			if (free < expectedSize + SPACE_MARGIN) {
				throw new IOException("Not enough free space to copy the feed: need " + ((expectedSize + SPACE_MARGIN) >> 20)
						+ " MB, have " + (free >> 20) + " MB");
			}
		}
		File target = uniqueTarget(safeName(displayName));
		File part = new File(feeds, target.getName() + PART_SUFFIX);
		boolean ok = false;
		try (OutputStream out = Files.newOutputStream(part.toPath())) {
			byte[] buf = new byte[1 << 16];
			byte[] head = new byte[4];
			int headLen = 0;
			int n;
			while ((n = in.read(buf)) > 0) {
				if (cancelled != null && cancelled.getAsBoolean()) {
					throw new CancelledException();
				}
				for (int i = 0; i < n && headLen < 4; i++) {
					head[headLen++] = buf[i];
				}
				out.write(buf, 0, n);
			}
			if (headLen < 4 || !Arrays.equals(head, ZIP_MAGIC)) {
				throw new IOException(displayName + ": not a zip file");
			}
			ok = true;
		} finally {
			if (!ok) {
				part.delete();
			}
		}
		Files.move(part.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
		return target;
	}

	private File uniqueTarget(String name) {
		File f = new File(feedsDir(), name);
		String base = name.substring(0, name.length() - 4);
		for (int i = 2; f.exists(); i++) {
			f = new File(feedsDir(), base + "-" + i + ".zip");
		}
		return f;
	}

	/** Deletes one zip by the name listed in {@link #listFeeds()}. Does not touch the index. */
	public boolean removeFeed(String name) {
		for (Feed f : listFeeds()) {
			if (f.name.equals(name)) {
				return new File(feedsDir(), name).delete();
			}
		}
		return false;
	}

	/**
	 * First half of a removal that can be undone: the zip is set aside and no longer listed. Follow with
	 * {@link #finishRemoval} once the index has been rebuilt without it (commit) or the rebuild failed (undo).
	 */
	public boolean stageRemoval(String name) {
		for (Feed f : listFeeds()) {
			if (f.name.equals(name)) {
				return new File(feedsDir(), name).renameTo(new File(feedsDir(), name + REMOVED_SUFFIX));
			}
		}
		return false;
	}

	public void finishRemoval(String name, boolean commit) {
		File aside = new File(feedsDir(), name + REMOVED_SUFFIX);
		if (commit) {
			aside.delete();
		} else {
			aside.renameTo(new File(feedsDir(), name));
		}
	}

	/**
	 * Builds the index from all stored zips and swaps it in atomically; the previous index stays in place if
	 * this fails. With no zips left the index is deleted and null is returned.
	 */
	public GtfsFeedImporter.Report rebuild(GtfsProgressListener progress) throws IOException {
		List<File> zips = new ArrayList<>();
		for (Feed f : listFeeds()) {
			zips.add(new File(feedsDir(), f.name));
		}
		if (zips.isEmpty()) {
			Files.deleteIfExists(getIndexFile().toPath());
			return null;
		}
		return new GtfsFeedImporter(progress).importFeeds(zips, getIndexFile());
	}
}
