package net.osmand.binary;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.Normalizer;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;
import net.osmand.Collator;
import net.osmand.PlatformUtil;
import org.apache.commons.logging.Log;

class IndexedStringTableCache {
	private static final Log LOG = PlatformUtil.getLog(IndexedStringTableCache.class);

	static final boolean ENABLED = readEnabledFromEnv();
	private static final int MAX_KEYS_PER_BUCKET = 300_000;
	private static final int MAX_VALS_PER_BUCKET = 32_000_000;

	private static boolean readEnabledFromEnv() {
		String env = System.getenv("TABLE_CACHE");
		return env == null || env.trim().isEmpty() || switch (env.trim().toLowerCase()) {
			case "0", "false", "no", "off", "disabled" -> false;
			default -> true;
		};
	}

	private static final Object DISK_CACHE_IO_LOCK = new Object();
	private static final Object RAM_BUDGET_LOCK = new Object();
	private static final class RandomAccessFileOutputStream extends java.io.OutputStream {
		private final RandomAccessFile raf;
		private RandomAccessFileOutputStream(RandomAccessFile raf) {
			this.raf = raf;
		}
		@Override
		public void write(int b) throws IOException {
			raf.write(b);
		}
		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			raf.write(b, off, len);
		}
	}
	private static final ExecutorService DISK_SAVE_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "IndexedStringTableCache-DiskSave");
		t.setDaemon(true);
		return t;
	});

	private static final int DISK_CACHE_FORMAT_VERSION = 4;
	private static final int DISK_CACHE_MAGIC = 0x49535443;
	private static final String DISK_CACHE_FILE_SUFFIX = ".cache";
	private static final long RAM_LIMIT_BYTES = 20 * 1024L * 1024L;
	// In-memory LRU key format: obfPath + suffixId + "#b" + 8-hex bucketKey.
	private static final String BUCKET_KEY_MARKER = "#b";
	private static final int STATS_PRINT_EVERY_REQUESTS = 1000;
	private static final int STAT_GET_REQUESTS = 0, STAT_EVICTIONS = 1,
			STAT_SEARCH_CALLS = 2, STAT_SEARCH_SUCCESS = 3, STAT_SEARCH_RAM_SUCCESS = 4;
	private static final int STATS_SIZE = 5;
	private static final AtomicLong[] STATS = new AtomicLong[STATS_SIZE];
	static {
		for (int i = 0; i < STATS_SIZE; i++) {
			STATS[i] = new AtomicLong();
		}
	}

	// Disk cache metadata for a single OBF file (used to validate that *.cache matches the current OBF).
	// suffixes: :poi/:addr sections
	private record DiskMeta(long size, long mtime, HashSet<String> suffixes) {
	}

	// Pointer to a serialized bucket blob inside the *.cache file (random-access seek + read).
	private record DiskBucketRef(long offset, int lengthBytes) {
	}

	// In-memory index for a single OBF file: per suffixId -> bucketKey -> location in *.cache.
	private record DiskIndex(long size, long mtime, HashMap<String, HashMap<Integer, DiskBucketRef>> bucketsBySuffix) {
	}

	// File positions of the (offset,len) fields for one bucket entry in the on-disk index (used for patching/updating).
	private record BucketIndexPos(String suffix, int bucketKey, long offsetPos, long lenPos) {
	}

	record Entry(String[] keys, int[] vals, int[] valStart, int[] valLength,
			TIntObjectHashMap<TIntArrayList> bucket1, TIntObjectHashMap<TIntArrayList> bucket2) {
		long estimateEntryRamBytes() {
			final long objectHeaderMin = 16, arrayHeaderMin = 16;
			final int refSizeMin = 4, intSize = 4;
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
		final long objectHeaderMin = 16L;
		final long arrayHeaderMin = 16L;
		final int refSizeMin = 4;
		final int intSize = 4;
		final int byteSize = 1;
		long min = 0;

		min += objectHeaderMin;
		boolean usedCapacityModel = false;
		try {
			int setCapacity = getArrayLengthByFieldName(map, "_set", int[].class);
			int statesCapacity = getArrayLengthByFieldName(map, "_states", byte[].class);
			int valuesCapacity = getArrayLengthByFieldName(map, "_values", Object[].class);
			if (setCapacity > 0 && statesCapacity > 0 && valuesCapacity > 0) {
				min += arrayHeaderMin + (long) setCapacity * intSize;
				min += arrayHeaderMin + (long) statesCapacity * byteSize;
				min += arrayHeaderMin + (long) valuesCapacity * refSizeMin;
				usedCapacityModel = true;
			}
		} catch (Throwable ignore) {
			// ignore
		}

		if (!usedCapacityModel) {
			min += arrayHeaderMin;
			min += (long) map.size() * intSize;
		}

		for (TIntArrayList list : map.valueCollection()) {
			if (list == null) {
				continue;
			}
			min += objectHeaderMin;
			int dataCapacity = -1;
			try {
				dataCapacity = getArrayLengthByFieldName(list, "_data", int[].class);
			} catch (Throwable ignore) {
				// ignore
			}
			if (dataCapacity > 0) {
				min += arrayHeaderMin + (long) dataCapacity * intSize;
			} else {
				min += arrayHeaderMin + (long) list.size() * intSize;
			}
		}
		return min;
	}

	private static int getArrayLengthByFieldName(Object instance, String fieldName, Class<?> expectedArrayType) {
		if (instance == null || fieldName == null || fieldName.isEmpty() || expectedArrayType == null) {
			return -1;
		}
		Class<?> cls = instance.getClass();
		while (cls != null) {
			try {
				java.lang.reflect.Field f = cls.getDeclaredField(fieldName);
				f.setAccessible(true);
				Object v = f.get(instance);
				if (v == null || v.getClass() != expectedArrayType) {
					return -1;
				}
				return java.lang.reflect.Array.getLength(v);
			} catch (NoSuchFieldException e) {
				cls = cls.getSuperclass();
			} catch (IllegalAccessException e) {
				return -1;
			}
		}
		return -1;
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
	private final Map<String, DiskIndex> diskIndexByObfPath = new ConcurrentHashMap<>();
	private final HashSet<String> diskSaveInFlight = new HashSet<>();
	// Tracks LRU ordering for *loaded* bucket entries only (bounded by RAM_LIMIT_BYTES).
	private final LinkedHashMap<String, Boolean> lruBucketKeys = new LinkedHashMap<>(16, 0.75f, true);
	private long currentRamBytes = 0;
	// Persistence is independent of RAM/LRU: we keep a full snapshot of buckets to be written to disk.
	private final HashMap<String, HashMap<String, HashMap<Integer, Entry>>> pendingPersistByObf = new HashMap<>();

	private static String getObfPathKey(File obfFile) {
		return obfFile.getAbsolutePath();
	}

	private static String getInMemoryBucketCacheKey(File obfFile, String suffixId, int bucketKey) {
		return getObfPathKey(obfFile) + suffixId + BUCKET_KEY_MARKER + String.format(Locale.ROOT, "%08x", bucketKey);
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

	// Fast path: load disk index (meta + per-bucket offsets) and then lazily load only required 2-char buckets.
	boolean trySearch(File obfFile, String prefix, String suffixId, BinaryMapIndexReader reader, Collator instance,
			List<String> queries, List<TIntArrayList> listOffsets, TIntArrayList matchedCharacters) {
		if (!canUseCache(prefix, suffixId) || obfFile == null) {
			return false;
		}
		LinkedHashSet<Integer> requiredBucketsPerKeys = collectRequiredBucketKeys(queries);
		if (requiredBucketsPerKeys.isEmpty()) {
			return false;
		}
		STATS[STAT_SEARCH_CALLS].incrementAndGet();

		boolean hasAllBuckets = true;
		boolean allBucketsServedFromRam = true;
		for (int bucketKey : requiredBucketsPerKeys) {
			String bucketCacheKey = getInMemoryBucketCacheKey(obfFile, suffixId, bucketKey);
			Entry entry = get(bucketCacheKey);
			if (entry == null) {
				allBucketsServedFromRam = false;
				if (!load(obfFile)) {
					hasAllBuckets = false;
					break;
				}
				entry = get(bucketCacheKey);
				if (entry == null) {
					entry = loadBucketFromDisk(obfFile, suffixId, bucketKey);
				}
			}
			if (entry == null) {
				hasAllBuckets = false;
				break;
			}
		}
		if (!hasAllBuckets) {
			return false;
		}

		for (int bucketKey : requiredBucketsPerKeys) {
			String bucketCacheKey = getInMemoryBucketCacheKey(obfFile, suffixId, bucketKey);
			Entry entry = get(bucketCacheKey);
			if (entry != null) {
				search(entry, reader, instance, queries, listOffsets, matchedCharacters);
			}
		}
		STATS[STAT_SEARCH_SUCCESS].incrementAndGet();
		if (allBucketsServedFromRam) {
			STATS[STAT_SEARCH_RAM_SUCCESS].incrementAndGet();
		}
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
		// Lazy RAM: buckets will be loaded on demand from disk index via loadBucketFromDisk() and cached in RAM with LRU.
		addPendingPersistSnapshot(obfFile, suffixId, built);
		scheduleSaveToDiskIfNeeded(obfFile, suffixId);
	}

	// Builds a full (suffixId -> bucketKey -> bucketEntry) snapshot that will be persisted to disk.
	// This is intentionally independent of the RAM-limited LRU (which may evict bucket entries).
	private void addPendingPersistSnapshot(File obfFile, String suffixId, Entry full) {
		if (!ENABLED || obfFile == null || suffixId == null || full == null || full.keys() == null) {
			return;
		}
		String obfPathKey = getObfPathKey(obfFile);
		HashMap<Integer, Entry> buckets = buildAllBuckets(full);
		if (buckets.isEmpty()) {
			return;
		}
		synchronized (DISK_CACHE_IO_LOCK) {
			pendingPersistByObf
					.computeIfAbsent(obfPathKey, k -> new HashMap<>())
					.put(suffixId, buckets);
		}
	}

	// Splits a full Entry into a set of bucket entries (2-char bucketKey -> bucketEntry).
	// A bucket-entry contains only keys/vals for that bucket and does NOT have bucket1/bucket2 maps (they are null).
	private static HashMap<Integer, Entry> buildAllBuckets(Entry full) {
		HashMap<Integer, ArrayList<Integer>> keyIdxByBucket = new HashMap<>();
		for (int i = 0; i < full.keys().length; i++) {
			String key = full.keys()[i];
			int bucketKey = bucketKey2FromString(key);
			if (bucketKey == -1) {
				continue;
			}
			keyIdxByBucket.computeIfAbsent(bucketKey, k -> new ArrayList<>()).add(i);
		}
		HashMap<Integer, Entry> out = new HashMap<>();
		for (Map.Entry<Integer, ArrayList<Integer>> e : keyIdxByBucket.entrySet()) {
			Entry bucketEntry = buildBucketEntry(full, e.getValue());
			if (bucketEntry != null) {
				out.put(e.getKey(), bucketEntry);
			}
		}
		return out;
	}

	// Creates a standalone bucket Entry (subset of keys/vals) for a given set of key indices.
	private static Entry buildBucketEntry(Entry full, ArrayList<Integer> indices) {
		if (full == null || indices == null || indices.isEmpty()) {
			return null;
		}
		String[] keys = new String[indices.size()];
		int[] valStart = new int[indices.size()];
		int[] valLength = new int[indices.size()];
		int totalVals = 0;
		for (int i = 0; i < indices.size(); i++) {
			int fullIdx = indices.get(i);
			keys[i] = full.keys()[fullIdx];
			int len = full.valLength()[fullIdx];
			valStart[i] = totalVals;
			valLength[i] = len;
			totalVals += len;
		}
		int[] vals = new int[totalVals];
		int p = 0;
		for (int fullIdx : indices) {
			int start = full.valStart()[fullIdx];
			int len = full.valLength()[fullIdx];
			for (int j = 0; j < len; j++) {
				vals[p++] = full.vals()[start + j];
			}
		}
		assert p == totalVals;
		return new Entry(keys, vals, valStart, valLength, null, null);
	}

	// RAM budget + global LRU eviction across all loaded buckets.
	// Note: persistence is not affected; disk snapshot is managed via pendingPersistByObf.
	private void putBucketWithBudget(String bucketCacheKey, Entry bucketEntry) {
		if (!ENABLED || bucketCacheKey == null || bucketEntry == null) {
			return;
		}
		final long entryBytes = bucketEntry.estimateEntryRamBytes();
		if (entryBytes > RAM_LIMIT_BYTES) {
			return;
		}
		synchronized (RAM_BUDGET_LOCK) {
			if (currentRamBytes == 0 && !cache.isEmpty()) {
				currentRamBytes = Math.max(0, estimateCacheRamBytesByPrefix(null));
			}
			if (lruBucketKeys.isEmpty() && !cache.isEmpty()) {
				long recalculatedRamBytes = 0;
				for (Map.Entry<String, Entry> e : cache.entrySet()) {
					String key = e.getKey();
					Entry value = e.getValue();
					if (key == null || value == null) {
						continue;
					}
					lruBucketKeys.put(key, Boolean.TRUE);
					recalculatedRamBytes += value.estimateEntryRamBytes();
				}
				currentRamBytes = recalculatedRamBytes;
			}
			while (currentRamBytes + entryBytes > RAM_LIMIT_BYTES) {
				String evictKey = getLruEldestKey();
				if (evictKey == null) {
					break;
				}
				Entry evicted = cache.remove(evictKey);
				lruBucketKeys.remove(evictKey);
				if (evicted != null) {
					currentRamBytes -= evicted.estimateEntryRamBytes();
					if (currentRamBytes < 0) {
						currentRamBytes = 0;
					}
					STATS[STAT_EVICTIONS].incrementAndGet();
				}
			}
			Entry prev = cache.put(bucketCacheKey, bucketEntry);
			if (prev != null) {
				currentRamBytes -= prev.estimateEntryRamBytes();
				if (currentRamBytes < 0) {
					currentRamBytes = 0;
				}
			}
			currentRamBytes += entryBytes;
			lruBucketKeys.put(bucketCacheKey, Boolean.TRUE);
		}
	}

	private String getLruEldestKey() {
		Iterator<String> it = lruBucketKeys.keySet().iterator();
		return it.hasNext() ? it.next() : null;
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
		Entry entry = cache.get(cacheKey);
		long req = STATS[STAT_GET_REQUESTS].incrementAndGet();
		if (req % STATS_PRINT_EVERY_REQUESTS == 0) {
			printStatsIfNeeded(req);
		}
		return entry;
	}

	private static void printStatsIfNeeded(long reqCount) {
		long evictions = STATS[STAT_EVICTIONS].get();
		long tryCalls = STATS[STAT_SEARCH_CALLS].get();
		long tryOk = STATS[STAT_SEARCH_SUCCESS].get();
		long tryRamOk = STATS[STAT_SEARCH_RAM_SUCCESS].get();

		// Global cumulative rates.
		double evictionRate = tryCalls <= 0 ? 0d : (double) evictions / (double) tryCalls;
		double trySuccessRate = tryCalls <= 0 ? 0d : (double) tryOk / (double) tryCalls;
		double ramSuccessRate = tryCalls <= 0 ? 0d : (double) tryRamOk / (double) tryCalls;

		LOG.info("Cache stats: req_count, ram_hit_rate, cache_hit_rate, evict_rate");
		LOG.info(reqCount +
					"," + String.format(Locale.ROOT, "%.2f", ramSuccessRate * 100d) + "%" +
					"," + String.format(Locale.ROOT, "%.2f", trySuccessRate * 100d) + "%" +
					"," + String.format(Locale.ROOT, "%.4f", evictionRate)
		);
	}

	// Reads only disk meta + per-suffix bucket index.
	// Bucket blobs are read on-demand by loadBucketFromDisk() using RandomAccessFile seek(offset).
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
		File sourceFile = getDiskCacheFile(obfFile);

		DiskMeta prevMeta = diskMetaByObfPath.get(obfPathKey);
		DiskIndex prevIndex = diskIndexByObfPath.get(obfPathKey);
		if (prevMeta != null && prevMeta.size == obfSize && prevMeta.mtime == obfMtime && prevIndex != null) {
			if (sourceFile != null && sourceFile.exists() && sourceFile.isFile()) {
			return true;
			}
			diskMetaByObfPath.remove(obfPathKey);
			diskIndexByObfPath.remove(obfPathKey);
			return false;
		}
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
			int suffixCount = in.readInt();
			if (suffixCount < 0) {
				return false;
			}

			HashSet<String> suffixes = new HashSet<>();
			HashMap<String, HashMap<Integer, DiskBucketRef>> bucketsBySuffix = new HashMap<>();
			for (int i = 0; i < suffixCount; i++) {
				String suffixId = readString(in);
				if (suffixId == null) {
					return false;
				}
				suffixes.add(suffixId);
				int bucketCount = in.readInt();
				if (bucketCount < 0) {
					return false;
				}
				HashMap<Integer, DiskBucketRef> refs = new HashMap<>();
				for (int bi = 0; bi < bucketCount; bi++) {
					int bucketKey = in.readInt();
					long offset = in.readLong();
					int lenBytes = in.readInt();
					if (offset < 0 || lenBytes < 0) {
						return false;
					}
					refs.put(bucketKey, new DiskBucketRef(offset, lenBytes));
				}
				bucketsBySuffix.put(suffixId, refs);
			}

			diskMetaByObfPath.put(obfPathKey, new DiskMeta(obfSize, obfMtime, suffixes));
			diskIndexByObfPath.put(obfPathKey, new DiskIndex(obfSize, obfMtime, bucketsBySuffix));
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	// Loads a single bucket blob by (suffixId, bucketKey) using disk index offsets.
	private Entry loadBucketFromDisk(File obfFile, String suffixId, int bucketKey) {
		if (!ENABLED || obfFile == null || suffixId == null) {
			return null;
		}
		String obfPathKey = getObfPathKey(obfFile);
		DiskIndex index = diskIndexByObfPath.get(obfPathKey);
		if (index == null || index.bucketsBySuffix() == null) {
			return null;
		}
		HashMap<Integer, DiskBucketRef> refs = index.bucketsBySuffix().get(suffixId);
		if (refs == null) {
			return null;
		}
		DiskBucketRef ref = refs.get(bucketKey);
		if (ref == null) {
			return null;
		}
		if (ref.lengthBytes() <= 0) {
			return null;
		}
		File sourceFile = getDiskCacheFile(obfFile);
		if (sourceFile == null || !sourceFile.exists() || !sourceFile.isFile()) {
			return null;
		}
		try (RandomAccessFile raf = new RandomAccessFile(sourceFile, "r")) {
			raf.seek(ref.offset());
			try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(raf.getFD()), ref.lengthBytes()))) {
				Entry entry = readEntry(in);
				if (entry == null) {
					return null;
				}
				String bucketCacheKey = getInMemoryBucketCacheKey(obfFile, suffixId, bucketKey);
				putBucketWithBudget(bucketCacheKey, entry);
				return entry;
			}
		} catch (IOException e) {
			return null;
		}
	}

	// Schedules a disk save only when:
	// - cache file doesn't exist yet for this OBF
	// - OBF changed (size/mtime)
	// - a new suffix section needs to be added (e.g. :addr existed, now :poi is built)
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
			File cacheFile = getDiskCacheFile(obfFile);

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
				if (cacheFile == null || !cacheFile.exists() || !cacheFile.isFile()) {
					needsSave = true;
				} else
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

	// Disk format:
	// - header: magic + version + (obfSize, obfMtime)
	// - index: per suffixId a list of (bucketKey -> offset,length)
	// - blobs: serialized bucket Entry per (suffixId,bucketKey)
	// Persistence is full (all buckets), independent of RAM/LRU. When adding a new suffix section,
	// unchanged sections are copied raw-bytes from the existing .cache file using copyBytes(offset, len).
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

		HashMap<String, HashMap<Integer, Entry>> pending;
		synchronized (DISK_CACHE_IO_LOCK) {
			pending = pendingPersistByObf.get(obfPathKey);
		}
		if (pending == null || pending.isEmpty()) {
			return;
		}

		HashMap<String, HashMap<Integer, Entry>> writeBySuffix = new HashMap<>();
		HashSet<String> suffixes = new HashSet<>();
		for (Map.Entry<String, HashMap<Integer, Entry>> e : pending.entrySet()) {
			String suffix = e.getKey();
			HashMap<Integer, Entry> buckets = e.getValue();
			if (suffix == null || buckets == null || buckets.isEmpty()) {
				continue;
			}
			suffixes.add(suffix);
			writeBySuffix.put(suffix, new HashMap<>(buckets));
		}
		if (writeBySuffix.isEmpty()) {
			return;
		}

		DiskIndex existingIndex = diskIndexByObfPath.get(obfPathKey);
		File existingFile = getDiskCacheFile(obfFile);
		if (existingIndex != null && existingIndex.bucketsBySuffix() != null && existingFile != null && existingFile.exists() && existingFile.isFile()) {
			for (Map.Entry<String, HashMap<Integer, DiskBucketRef>> e : existingIndex.bucketsBySuffix().entrySet()) {
				String suffix = e.getKey();
				if (suffix == null) {
					continue;
				}
				if (writeBySuffix.containsKey(suffix)) {
					continue;
				}
				HashMap<Integer, DiskBucketRef> refs = e.getValue();
				if (refs == null || refs.isEmpty()) {
					continue;
				}
				HashMap<Integer, Entry> placeholder = new HashMap<>();
				for (int bk : refs.keySet()) {
					placeholder.put(bk, null);
				}
				suffixes.add(suffix);
				writeBySuffix.put(suffix, placeholder);
			}
		}

		File targetFile = getDiskCacheFile(obfFile);
		if (targetFile == null) {
			return;
		}
		File tmpFile = new File(parent, obfFile.getName() + DISK_CACHE_FILE_SUFFIX + ".tmp");
		DiskIndex updatedIndex;
		try (RandomAccessFile raf = new RandomAccessFile(tmpFile, "rw")) {
			raf.setLength(0);
			try (DataOutputStream out = new DataOutputStream(new RandomAccessFileOutputStream(raf))) {
				out.writeInt(DISK_CACHE_MAGIC);
				out.writeInt(DISK_CACHE_FORMAT_VERSION);
				out.writeLong(obfSize);
				out.writeLong(obfMtime);
				out.writeInt(writeBySuffix.size());

				ArrayList<BucketIndexPos> indexPositions = new ArrayList<>();
				ArrayList<String> suffixOrder = new ArrayList<>(writeBySuffix.keySet());
				suffixOrder.sort(String::compareTo);

				for (String suffix : suffixOrder) {
					HashMap<Integer, Entry> buckets = writeBySuffix.get(suffix);
					if (buckets == null || buckets.isEmpty()) {
						continue;
					}
					writeString(out, suffix);
					ArrayList<Integer> bucketKeys = new ArrayList<>(buckets.keySet());
					bucketKeys.sort(Integer::compareTo);
					out.writeInt(bucketKeys.size());
					for (int bk : bucketKeys) {
						out.writeInt(bk);
						long offsetPos = raf.getFilePointer();
						out.writeLong(0L);
						long lenPos = raf.getFilePointer();
						out.writeInt(0);
						indexPositions.add(new BucketIndexPos(suffix, bk, offsetPos, lenPos));
					}
				}
				HashMap<String, HashMap<Integer, BucketIndexPos>> posMap = new HashMap<>();
				for (BucketIndexPos p : indexPositions) {
					posMap.computeIfAbsent(p.suffix, k -> new HashMap<>()).put(p.bucketKey, p);
				}

				HashMap<String, HashMap<Integer, DiskBucketRef>> bucketsBySuffix = new HashMap<>();
				for (String suffix : suffixOrder) {
					HashMap<Integer, Entry> buckets = writeBySuffix.get(suffix);
					if (buckets == null || buckets.isEmpty()) {
						continue;
					}
					bucketsBySuffix.put(suffix, new HashMap<>());
				}

				try (RandomAccessFile src = existingFile != null && existingFile.exists() ? new RandomAccessFile(existingFile, "r") : null) {
					for (String suffix : suffixOrder) {
						HashMap<Integer, Entry> buckets = writeBySuffix.get(suffix);
						if (buckets == null) {
							continue;
						}
						ArrayList<Integer> bucketKeys = new ArrayList<>(buckets.keySet());
						bucketKeys.sort(Integer::compareTo);
						for (int bk : bucketKeys) {
							Entry entry = buckets.get(bk);
							long entryOffset = raf.getFilePointer();
							if (entry != null) {
								writeEntry(out, entry);
							} else {
								DiskBucketRef ref = null;
								if (existingIndex != null && existingIndex.bucketsBySuffix() != null) {
									HashMap<Integer, DiskBucketRef> refs = existingIndex.bucketsBySuffix().get(suffix);
									ref = refs == null ? null : refs.get(bk);
								}
								if (src != null && ref != null) {
									copyBytes(src, ref.offset(), ref.lengthBytes(), raf);
								}
							}
							long entryEnd = raf.getFilePointer();
							int entryLen = (int) (entryEnd - entryOffset);
							HashMap<Integer, BucketIndexPos> suffixPos = posMap.get(suffix);
							BucketIndexPos pos = suffixPos == null ? null : suffixPos.get(bk);
							if (pos != null) {
								raf.seek(pos.offsetPos);
								raf.writeLong(entryOffset);
								raf.seek(pos.lenPos);
								raf.writeInt(entryLen);
								raf.seek(entryEnd);
							}
							HashMap<Integer, DiskBucketRef> dstRefs = bucketsBySuffix.get(suffix);
							if (dstRefs != null && entryLen > 0) {
								dstRefs.put(bk, new DiskBucketRef(entryOffset, entryLen));
							}
							raf.seek(entryEnd);
						}
					}
				}
				updatedIndex = new DiskIndex(obfSize, obfMtime, bucketsBySuffix);
				out.flush();
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
			return;
		}
		diskMetaByObfPath.put(obfPathKey, new DiskMeta(obfSize, obfMtime, suffixes));
		diskIndexByObfPath.put(obfPathKey, updatedIndex);
		synchronized (DISK_CACHE_IO_LOCK) {
			pendingPersistByObf.remove(obfPathKey);
		}
	}

	// Raw byte copy used for "merge" saves: keep unchanged bucket blobs without re-serializing them.
	private static void copyBytes(RandomAccessFile src, long srcOffset, int length, RandomAccessFile dst) throws IOException {
		if (length <= 0) {
			return;
		}
		final int bufSize = 64 * 1024;
		byte[] buffer = new byte[Math.min(bufSize, length)];
		src.seek(srcOffset);
		int remaining = length;
		while (remaining > 0) {
			int toRead = Math.min(remaining, buffer.length);
			int read = src.read(buffer, 0, toRead);
			if (read <= 0) {
				break;
			}
			dst.write(buffer, 0, read);
			remaining -= read;
		}
	}

	private static Entry readEntry(DataInputStream in) throws IOException {
		int keysCount = in.readInt();
		if (keysCount < 0 || keysCount > MAX_KEYS_PER_BUCKET) {
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
		if (valsCount < 0 || valsCount > MAX_VALS_PER_BUCKET) {
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
		boolean useBuckets = entry.bucket1() != null && entry.bucket2() != null;
		if (!useBuckets) {
			for (int i = 0; i < entry.keys().length; i++) {
				processKeyIndex(entry, reader, instance, queries, listOffsets, matchedCharacters, matched, i);
			}
			return;
		}

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

	// Determines which 2-char buckets are required for the current query set.
	private static LinkedHashSet<Integer> collectRequiredBucketKeys(List<String> queries) {
		LinkedHashSet<Integer> res = new LinkedHashSet<>();
		if (queries == null) {
			return res;
		}
		for (String q : queries) {
			int k = bucketKey2FromString(q);
			if (k != -1) {
				res.add(k);
			}
		}
		return res;
	}

	private static int bucketKey2FromString(String value) {
		String folded = foldForBucket(value);
		return bucketKey2FromFolded(folded);
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

	// Normalizes input for bucket derivation / matching:
	// 1) lower-case
	// 2) alignChars (OsmAnd-specific character alignment)
	// 3) strip diacritics (NFD, remove NON_SPACING_MARK)
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

	// 2-char bucket key used for segmented cache (c0<<16)|c1; if there is no second char, c1=0.
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
