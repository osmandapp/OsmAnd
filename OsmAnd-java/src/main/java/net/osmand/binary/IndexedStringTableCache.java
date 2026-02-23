package net.osmand.binary;

import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;
import net.osmand.Collator;

class IndexedStringTableCache {
	static final boolean ENABLED = true;
	private static final Object DISK_CACHE_IO_LOCK = new Object();
	private static final ExecutorService DISK_SAVE_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "IndexedStringTableCache-DiskSave");
		t.setDaemon(true);
		return t;
	});

	private static final int DISK_CACHE_FORMAT_VERSION = 3;
	private static final int DISK_CACHE_MAGIC = 0x49535443;
	private static final String DISK_CACHE_FILE_SUFFIX = ".cache";

	private record DiskMeta(long size, long mtime, HashSet<String> suffixes) {
	}

	record Entry(String[] keys, int[] vals, int[] valStart, int[] valLength,
			TIntObjectHashMap<TIntArrayList> bucket1, TIntObjectHashMap<TIntArrayList> bucket2) {
		long estimateEntryRamBytes() {
			final long objectHeaderMin = 16;
			final long arrayHeaderMin = 16;
			final int refSizeMin = 4;
			final int intSize = 4;

			long min = 0;

			int keysCount = keys() == null ? 0 : keys().length;
			int valsCount = vals() == null ? 0 : vals().length;

			min += objectHeaderMin;
			if (keys() != null) {
				min += arrayHeaderMin + (long) keysCount * refSizeMin;
				for (String s : keys()) {
					if (s == null) {
						continue;
					}
					int len = s.length();
					min += objectHeaderMin;
					min += arrayHeaderMin;
					min += len;
				}
			}

			if (vals() != null) {
				min += arrayHeaderMin + (long) valsCount * intSize;
			}
			if (valStart() != null) {
				min += arrayHeaderMin + (long) valStart().length * intSize;
			}
			if (valLength() != null) {
				min += arrayHeaderMin + (long) valLength().length * intSize;
			}
			min += estimateBucketsRamBytes();

			return min;
		}

		private long estimateBucketsRamBytes() {
			if (bucket1 == null && bucket2 == null) {
				return 0;
			}

			long min = 0;
			min += 16;

			min += IndexedStringTableCache.estimateBucketsRamBytes(bucket1);
			min += IndexedStringTableCache.estimateBucketsRamBytes(bucket2);
			return min;
		}
	}

	private static long estimateBucketsRamBytes(TIntObjectHashMap<TIntArrayList> map) {
		if (map == null || map.isEmpty()) {
			return 0;
		}
		long min = 0;
		min += 16L;
		min += 16L;
		min += (long) map.size() * 4;
		for (TIntArrayList list : map.valueCollection()) {
			if (list == null) {
				continue;
			}
			int sz = list.size();
			min += 16L;
			min += 16L + (long) sz * 4;
		}
		return min;
	}

	long estimateCacheRamBytesByPrefix(String obfPathPrefix) {
		final long mapEntryOverheadMin = 48;
		long min = 0;
		for (Map.Entry<String, Entry> e : cache.entrySet()) {
			String key = e.getKey();
			Entry val = e.getValue();
			if (key == null || val == null) {
				continue;
			}
			if (obfPathPrefix != null && !key.startsWith(obfPathPrefix)) {
				continue;
			}
			min += mapEntryOverheadMin;
			min += val.estimateEntryRamBytes();
		}
		return min;
	}

	static final class Builder {
		private final ArrayList<String> keys = new ArrayList<>();
		private final ArrayList<TIntArrayList> valsPerKey = new ArrayList<>();
		private final TIntObjectHashMap<TIntArrayList> bucket1 = new TIntObjectHashMap<>();
		private final TIntObjectHashMap<TIntArrayList> bucket2 = new TIntObjectHashMap<>();
		private TIntArrayList currentKeyVals;
		private int keyIndex = -1;
		private boolean enabled = ENABLED;

		void onKey(String key) {
			if (!enabled) {
				return;
			}
			keys.add(key);
			currentKeyVals = new TIntArrayList();
			valsPerKey.add(currentKeyVals);
			keyIndex++;

			String foldedKey = foldForBucket(key);
			makeBuckets(bucket1, bucket2, keyIndex, foldedKey);
		}

		void onVal(int val) {
			if (!enabled) {
				return;
			}
			if (currentKeyVals == null) {
				enabled = false;
				return;
			}
			currentKeyVals.add(val);
		}

		boolean isEnabled() {
			return enabled;
		}

		Entry build() {
			if (!enabled || keys.isEmpty()) {
				return null;
			}
			int keysCount = keys.size();
			String[] keysArr = keys.toArray(new String[keysCount]);

			int[] valStart = new int[keysCount];
			int[] valLength = new int[keysCount];
			int totalVals = 0;
			for (int i = 0; i < keysCount; i++) {
				TIntArrayList v = valsPerKey.get(i);
				valStart[i] = totalVals;
				valLength[i] = v.size();
				totalVals += v.size();
			}
			int[] valsArr = new int[totalVals];
			int p = 0;
			for (int i = 0; i < keysCount; i++) {
				TIntArrayList v = valsPerKey.get(i);
				for (int j = 0; j < v.size(); j++) {
					valsArr[p++] = v.get(j);
				}
			}
			assert p == totalVals;
			assert keyIndex + 1 == keysCount;
			return new Entry(keysArr, valsArr, valStart, valLength, bucket1, bucket2);
		}
	}

	private final Map<String, Entry> cache = new ConcurrentHashMap<>();
	private final Map<String, DiskMeta> diskMetaByObfPath = new ConcurrentHashMap<>();
	private final HashSet<String> diskSaveInFlight = new HashSet<>();

	private static String getObfPathKey(File obfFile) {
		return obfFile.getAbsolutePath();
	}

	private static String getInMemoryCacheKey(File obfFile, String suffixId) {
		return getObfPathKey(obfFile) + suffixId;
	}

	private static File getDiskCacheFile(File obfFile) {
		File parent = obfFile.getParentFile();
		if (parent == null) {
			return null;
		}
		return new File(parent, obfFile.getName() + DISK_CACHE_FILE_SUFFIX);
	}

	boolean canUseCache(String prefix, String suffixId) {
		return ENABLED && prefix != null && prefix.isEmpty() && suffixId != null;
	}

	Builder newBuilderIfEnabled(String prefix, String suffixId) {
		return canUseCache(prefix, suffixId) ? new Builder() : null;
	}

	boolean trySearch(File obfFile, String prefix, String suffixId, BinaryMapIndexReader reader, Collator instance,
			List<String> queries, List<TIntArrayList> listOffsets, TIntArrayList matchedCharacters) {
		if (!canUseCache(prefix, suffixId) || obfFile == null) {
			return false;
		}
		String cacheKey = getInMemoryCacheKey(obfFile, suffixId);
		Entry entry = get(cacheKey);
		if (entry == null) {
			load(obfFile);
			entry = get(cacheKey);
		}
		if (entry == null) {
			return false;
		}
		search(entry, reader, instance, queries, listOffsets, matchedCharacters);
		return true;
	}

	void saveIfNeeded(File obfFile, String suffixId, Builder builder, boolean hasSubtables) {
		if (!ENABLED || hasSubtables || obfFile == null || suffixId == null || builder == null || !builder.isEnabled()) {
			return;
		}
		Entry built = builder.build();
		if (built == null) {
			return;
		}
		String cacheKey = getInMemoryCacheKey(obfFile, suffixId);
		put(cacheKey, built);
		scheduleSaveToDiskIfNeeded(obfFile, suffixId);
	}

	private void scheduleSaveToDiskIfNeeded(File obfFile, String suffixId) {
		if (!ENABLED || obfFile == null || suffixId == null) {
			return;
		}
		String obfPathKey = getObfPathKey(obfFile);
		synchronized (DISK_CACHE_IO_LOCK) {
			if (diskSaveInFlight.contains(obfPathKey)) {
				return;
			}
			diskSaveInFlight.add(obfPathKey);
		}

		DISK_SAVE_EXECUTOR.execute(() -> {
			try {
				saveToDiskIfNeeded(obfFile, suffixId);
			} catch (IOException e) {
				// ignore
			} finally {
				synchronized (DISK_CACHE_IO_LOCK) {
					diskSaveInFlight.remove(obfPathKey);
				}
			}
		});
	}

	Entry get(String cacheKey) {
		if (!ENABLED) {
			return null;
		}
		return cache.get(cacheKey);
	}

	void put(String cacheKey, Entry entry) {
		if (!ENABLED) {
			return;
		}
		if (entry == null) {
			return;
		}
		cache.put(cacheKey, entry);
	}

	boolean load(File obfFile) {
		if (!ENABLED) {
			return false;
		}
		if (obfFile == null) {
			return false;
		}
		String obfPathKey = getObfPathKey(obfFile);
		long obfSize = obfFile.length();
		long obfMtime = obfFile.lastModified();

		DiskMeta prevMeta = diskMetaByObfPath.get(obfPathKey);
		if (prevMeta != null && prevMeta.size == obfSize && prevMeta.mtime == obfMtime) {
			return true;
		}

		File sourceFile = getDiskCacheFile(obfFile);
		if (sourceFile == null || !sourceFile.exists() || !sourceFile.isFile()) {
			return false;
		}

		try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(sourceFile)))) {
			int magic = in.readInt();
			if (magic != DISK_CACHE_MAGIC) {
				return false;
			}
			int version = in.readInt();
			if (version != DISK_CACHE_FORMAT_VERSION) {
				return false;
			}
			long sizeInCache = in.readLong();
			long mtimeInCache = in.readLong();
			if (sizeInCache != obfSize || mtimeInCache != obfMtime) {
				return false;
			}
			int entryCount = in.readInt();
			if (entryCount < 0) {
				return false;
			}

			HashSet<String> suffixes = new HashSet<>();
			for (int i = 0; i < entryCount; i++) {
				String suffixId = readString(in);
				Entry entry = readEntry(in);
				if (suffixId == null || entry == null) {
					return false;
				}
				suffixes.add(suffixId);
				cache.put(getInMemoryCacheKey(obfFile, suffixId), entry);
			}

			diskMetaByObfPath.put(obfPathKey, new DiskMeta(obfSize, obfMtime, suffixes));
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	private void saveToDiskIfNeeded(File obfFile, String suffixId) throws IOException {
		if (!ENABLED) {
			return;
		}
		if (obfFile == null || suffixId == null) {
			return;
		}
		synchronized (DISK_CACHE_IO_LOCK) {
			String obfPathKey = getObfPathKey(obfFile);
			long obfSize = obfFile.length();
			long obfMtime = obfFile.lastModified();

			DiskMeta meta = diskMetaByObfPath.get(obfPathKey);
			boolean needsSave = false;
			if (meta == null) {
				boolean loaded = load(obfFile);
				meta = diskMetaByObfPath.get(obfPathKey);
				if (!loaded || meta == null) {
					needsSave = true;
				}
			}
			if (!needsSave) {
				if (meta.size != obfSize || meta.mtime != obfMtime) {
					needsSave = true;
				} else if (!meta.suffixes.contains(suffixId)) {
					needsSave = true;
				}
			}
			if (!needsSave) {
				return;
			}
			saveToDisk(obfFile);
			DiskMeta updated = diskMetaByObfPath.get(obfPathKey);
			if (updated != null) {
				updated.suffixes.add(suffixId);
			}
		}
	}

	private void saveToDisk(File obfFile) throws IOException {
		if (!ENABLED) {
			return;
		}
		if (obfFile == null) {
			return;
		}
		File parent = obfFile.getParentFile();
		if (parent == null) {
			return;
		}
		String obfPathKey = getObfPathKey(obfFile);
		long obfSize = obfFile.length();
		long obfMtime = obfFile.lastModified();

		ArrayList<String> keysToWrite = new ArrayList<>();
		HashSet<String> suffixes = new HashSet<>();
		for (Map.Entry<String, Entry> e : cache.entrySet()) {
			String key = e.getKey();
			if (key == null || !key.startsWith(obfPathKey)) {
				continue;
			}
			if (e.getValue() == null) {
				continue;
			}
			keysToWrite.add(key);
			suffixes.add(key.substring(obfPathKey.length()));
		}
		if (keysToWrite.isEmpty()) {
			return;
		}

		File targetFile = getDiskCacheFile(obfFile);
		if (targetFile == null) {
			return;
		}
		File tmpFile = new File(parent, obfFile.getName() + DISK_CACHE_FILE_SUFFIX + ".tmp");
		try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tmpFile)))) {
			out.writeInt(DISK_CACHE_MAGIC);
			out.writeInt(DISK_CACHE_FORMAT_VERSION);
			out.writeLong(obfSize);
			out.writeLong(obfMtime);
			out.writeInt(keysToWrite.size());
			for (String fullKey : keysToWrite) {
				Entry entry = cache.get(fullKey);
				assert entry != null;
				String suffixId = fullKey.substring(obfPathKey.length());
				writeString(out, suffixId);
				writeEntry(out, entry);
			}
		}
		if (targetFile.exists() && !targetFile.delete()) {
			if (tmpFile.exists()) {
				//noinspection ResultOfMethodCallIgnored
				tmpFile.delete();
			}
			return;
		}
		if (!tmpFile.renameTo(targetFile)) {
			//noinspection ResultOfMethodCallIgnored
			tmpFile.delete();
		}
		diskMetaByObfPath.put(obfPathKey, new DiskMeta(obfSize, obfMtime, suffixes));
	}

	private static Entry readEntry(DataInputStream in) throws IOException {
		int keysCount = in.readInt();
		if (keysCount < 0) {
			return null;
		}
		String[] keys = new String[keysCount];
		for (int i = 0; i < keysCount; i++) {
			String k = readString(in);
			if (k == null) {
				return null;
			}
			keys[i] = k;
		}

		int valsCount = in.readInt();
		if (valsCount < 0) {
			return null;
		}
		int[] vals = new int[valsCount];
		for (int i = 0; i < valsCount; i++) {
			vals[i] = in.readInt();
		}

		int startCount = in.readInt();
		if (startCount != keysCount) {
			return null;
		}
		int[] valStart = new int[startCount];
		for (int i = 0; i < startCount; i++) {
			valStart[i] = in.readInt();
		}

		int lenCount = in.readInt();
		if (lenCount != keysCount) {
			return null;
		}
		int[] valLength = new int[lenCount];
		for (int i = 0; i < lenCount; i++) {
			valLength[i] = in.readInt();
		}

		TIntObjectHashMap<TIntArrayList> bucket1 = new TIntObjectHashMap<>();
		TIntObjectHashMap<TIntArrayList> bucket2 = new TIntObjectHashMap<>();
		for (int i = 0; i < keys.length; i++) {
			String foldedKey = foldForBucket(keys[i]);
			makeBuckets(bucket1, bucket2, i, foldedKey);
		}

		return new Entry(keys, vals, valStart, valLength, bucket1, bucket2);
	}

	private static void makeBuckets(TIntObjectHashMap<TIntArrayList> bucket1, TIntObjectHashMap<TIntArrayList> bucket2,
	                                int i, String foldedKey) {
		int b1 = bucketKey1FromFolded(foldedKey);
		if (b1 != -1) {
			getOrCreateBucket(bucket1, b1).add(i);
		}
		int b2 = bucketKey2FromFolded(foldedKey);
		if (b2 != -1) {
			getOrCreateBucket(bucket2, b2).add(i);
		}
	}

	private static String readString(DataInputStream in) throws IOException {
		int len = in.readInt();
		if (len == -1) {
			return null;
		}
		if (len < 0) {
			return null;
		}
		byte[] bytes = new byte[len];
		in.readFully(bytes);
		return new String(bytes, StandardCharsets.UTF_8);
	}

	private static void writeEntry(DataOutputStream out, Entry entry) throws IOException {
		out.writeInt(entry.keys().length);
		for (String k : entry.keys()) {
			writeString(out, k);
		}

		out.writeInt(entry.vals().length);
		for (int v : entry.vals()) {
			out.writeInt(v);
		}

		out.writeInt(entry.valStart().length);
		for (int v : entry.valStart()) {
			out.writeInt(v);
		}

		out.writeInt(entry.valLength().length);
		for (int v : entry.valLength()) {
			out.writeInt(v);
		}
	}

	private static void writeString(DataOutputStream out, String s) throws IOException {
		if (s == null) {
			out.writeInt(-1);
			return;
		}
		byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
		out.writeInt(bytes.length);
		out.write(bytes);
	}

	void search(Entry entry, BinaryMapIndexReader reader, Collator instance,
			List<String> queries, List<TIntArrayList> listOffsets, TIntArrayList matchedCharacters) {
		if (!ENABLED || entry == null) {
			return;
		}
		boolean[] matched = new boolean[matchedCharacters.size()];
		TIntHashSet candidateIndices = new TIntHashSet();
		boolean useBuckets = true;

		for (String query : queries) {
			if (query == null) {
				continue;
			}
			String foldedQuery = foldForBucket(query);
			int k1 = bucketKey1FromFolded(foldedQuery);
			int k2 = bucketKey2FromFolded(foldedQuery);
			TIntArrayList bucket = null;

			if (k2 != -1) {
				bucket = entry.bucket2().get(k2);
				if (bucket == null || bucket.isEmpty()) {
					bucket = null;
				}
			}
			if (bucket == null && k1 != -1) {
				bucket = entry.bucket1().get(k1);
				if (bucket == null || bucket.isEmpty()) {
					bucket = null;
				}
			}
			if (bucket == null) {
				useBuckets = false;
				break;
			}

			for (int bi = 0; bi < bucket.size(); bi++) {
				candidateIndices.add(bucket.get(bi));
			}
		}

		if (useBuckets) {
			for (int idx : candidateIndices.toArray()) {
				processKeyIndex(entry, reader, instance, queries, listOffsets, matchedCharacters, matched, idx);
			}
			return;
		}

		for (int i = 0; i < entry.keys().length; i++) {
			processKeyIndex(entry, reader, instance, queries, listOffsets, matchedCharacters, matched, i);
		}
	}

	private void processKeyIndex(Entry entry, BinaryMapIndexReader reader, Collator instance,
			List<String> queries, List<TIntArrayList> listOffsets, TIntArrayList matchedCharacters, boolean[] matched,
			int idx) {
		String key = entry.keys()[idx];
		reader.matchIndexByNameKey(instance, queries, listOffsets, matchedCharacters, key, matched);
		int start = entry.valStart()[idx];
		int len = entry.valLength()[idx];
		for (int vi = 0; vi < len; vi++) {
			int val = entry.vals()[start + vi];
			for (int qi = 0; qi < queries.size(); qi++) {
				if (matched[qi]) {
					listOffsets.get(qi).add(val);
				}
			}
		}
	}

	private static String foldForBucket(String value) {
		if (value == null) {
			return "";
		}
		String folded = value.toLowerCase(Locale.getDefault());
		folded = net.osmand.CollatorStringMatcher.alignChars(folded);
		folded = Normalizer.normalize(folded, Normalizer.Form.NFD);
		int length = folded.length();
		if (length == 0) {
			return folded;
		}
		StringBuilder b = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			char c = folded.charAt(i);
			if (Character.getType(c) != Character.NON_SPACING_MARK) {
				b.append(c);
			}
		}
		return b.toString();
	}

	private static int bucketKey1FromFolded(String folded) {
		if (folded == null || folded.isEmpty()) {
			return -1;
		}
		return folded.charAt(0);
	}

	private static int bucketKey2FromFolded(String folded) {
		if (folded == null || folded.length() < 2) {
			return -1;
		}
		return (folded.charAt(0) << 16) | folded.charAt(1);
	}

	private static TIntArrayList getOrCreateBucket(TIntObjectHashMap<TIntArrayList> map, int key) {
		TIntArrayList list = map.get(key);
		if (list == null) {
			list = new TIntArrayList();
			map.put(key, list);
		}
		return list;
	}

	public static void main(String[] args) {
		String mapDir = System.getenv("MAP_DIR");
		if (mapDir == null || mapDir.trim().isEmpty()) {
			System.err.println("MAP_DIR env is required");
			return;
		}
		String filter = System.getenv("MAP_FILTER");
		System.out.println("MAP_DIR=" + mapDir);
		System.out.println("MAP_FILTER=" + (filter == null ? "" : filter));
		System.out.println();

		File root = new File(mapDir);
		if (!root.exists() || !root.isDirectory()) {
			System.err.println("MAP_DIR doesn't exist or not a directory: " + root.getAbsolutePath());
			return;
		}

		ArrayList<File> obfFiles = new ArrayList<>();
		for (File child : Objects.requireNonNull(root.listFiles())) {
			String name = child.getName();
			String nameLc = name.toLowerCase(Locale.ROOT);
			if (!nameLc.endsWith(".obf")) {
				continue;
			}
			if (filter != null && !filter.isEmpty() && !name.startsWith(filter)) {
				continue;
			}
			obfFiles.add(child);
		}

		obfFiles.sort(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
		System.out.println("OBF files: " + obfFiles.size());
		System.out.println();

		IndexedStringTableCache cache = new IndexedStringTableCache();
		int loadedCount = 0;
		for (File obfFile : obfFiles) {
			boolean loaded = cache.load(obfFile);
			if (!loaded) {
				System.out.println(obfFile.getName() + " is skipped.");
				continue;
			}
			loadedCount++;
			String prefix = getObfPathKey(obfFile);
			long bytes = cache.estimateCacheRamBytesByPrefix(prefix);
			System.out.println(obfFile.getName() + ": RAM = " + bytes);
		}

		System.out.println();
		System.out.println("Loaded caches: " + loadedCount + " / " + obfFiles.size());
		System.out.println("Total cache bytes: " + cache.estimateCacheRamBytesByPrefix(null));
	}
}
