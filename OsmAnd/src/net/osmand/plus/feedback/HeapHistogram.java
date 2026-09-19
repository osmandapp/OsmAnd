package net.osmand.plus.feedback;

import androidx.annotation.NonNull;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads an hprof file and writes down how many objects of each class it holds and how many bytes
 * they take. That is the one question a heap size cannot answer: not how large the heap is, but
 * what it is made of.
 * <p>
 * The file is read as a stream and never held in memory: a dump of a process in trouble is
 * hundreds of megabytes, and the whole point of taking it is that memory is already short. Only
 * class names and counters are kept, a few tens of thousands of small entries.
 * <p>
 * Format: the JAVA PROFILE 1.0.3 records, plus the heap dump subrecord tags Android adds
 * (interned string, finalizing, debugger and vm internal roots, and the heap boundary marker).
 */
public class HeapHistogram {

	private static final int TAG_STRING = 0x01;
	private static final int TAG_LOAD_CLASS = 0x02;
	private static final int TAG_HEAP_DUMP = 0x0c;
	private static final int TAG_HEAP_DUMP_SEGMENT = 0x1c;

	private static final int SUB_CLASS_DUMP = 0x20;
	private static final int SUB_INSTANCE_DUMP = 0x21;
	private static final int SUB_OBJECT_ARRAY_DUMP = 0x22;
	private static final int SUB_PRIMITIVE_ARRAY_DUMP = 0x23;
	private static final int SUB_HEAP_DUMP_INFO = 0xfe;
	private static final int SUB_PRIMITIVE_ARRAY_NODATA = 0xc3;

	// an object header costs this much on top of the fields the dump reports
	private static final int OBJECT_OVERHEAD = 8;
	private static final int ARRAY_OVERHEAD = 16;

	private static final String[] PRIMITIVES = {
			"", "", "object", "", "boolean", "char", "float", "double", "byte", "short", "int", "long"
	};
	private static final int[] PRIMITIVE_SIZES = {0, 0, 0, 0, 1, 2, 4, 8, 1, 2, 4, 8};

	// arrays smaller than this are left unattributed: they are many and carry few bytes, and a
	// map over all of them would be a larger allocation than anything this class is measuring
	private static final int BIG_ARRAY_BYTES = 1024;

	private final Map<Long, String> strings = new HashMap<>();
	private final Map<Long, Long> classNames = new HashMap<>();
	private final Map<Long, long[]> byClass = new HashMap<>();
	private final Map<Integer, long[]> byHeap = new HashMap<>();
	private final Map<Long, Long> heapNames = new HashMap<>();
	// array object id -> {bytes, the key naming its kind}
	private final Map<Long, long[]> bigArrays = new HashMap<>();
	private final Map<Long, Long> superClasses = new HashMap<>();
	private final Map<Long, byte[]> declaredFields = new HashMap<>();
	private final Map<Long, int[]> objectFieldOffsets = new HashMap<>();
	// "owner class -> array kind" -> {bytes, count}
	private final Map<String, long[]> owners = new HashMap<>();
	private byte[] instance = new byte[4096];

	private int idSize = 4;
	private int currentHeap;

	@NonNull
	public static String analyze(@NonNull File hprof) throws IOException {
		HeapHistogram histogram = new HeapHistogram();
		try (InputStream in = new BufferedInputStream(new FileInputStream(hprof), 256 * 1024)) {
			histogram.read(new Reader(in), false);
		}
		if (!histogram.bigArrays.isEmpty()) {
			// a second walk finds which object holds each large array: the first cannot, because
			// an array is written to the dump before whatever points at it
			try (InputStream in = new BufferedInputStream(new FileInputStream(hprof), 256 * 1024)) {
				histogram.read(new Reader(in), true);
			}
		}
		return histogram.format();
	}

	private void read(@NonNull Reader in, boolean secondPass) throws IOException {
		// "JAVA PROFILE 1.0.3\0", then the identifier size and a timestamp
		while (in.readByte() != 0) {
			// the version string, its length is not given anywhere
		}
		idSize = in.readInt();
		in.skip(8);
		while (true) {
			int tag;
			try {
				tag = in.readByte() & 0xff;
			} catch (EOFException e) {
				return;
			}
			in.skip(4);
			long length = in.readInt() & 0xffffffffL;
			long end = in.position() + length;
			switch (tag) {
				case TAG_STRING:
					if (!secondPass) {
						long id = in.readId(idSize);
						strings.put(id, in.readString((int) (end - in.position())));
					}
					break;
				case TAG_LOAD_CLASS:
					if (!secondPass) {
						in.skip(4);
						long classId = in.readId(idSize);
						in.skip(4);
						classNames.put(classId, in.readId(idSize));
					}
					break;
				case TAG_HEAP_DUMP:
				case TAG_HEAP_DUMP_SEGMENT:
					readHeapDump(in, end, secondPass);
					break;
				default:
					break;
			}
			if (in.position() < end) {
				in.skip(end - in.position());
			}
		}
	}

	private void readHeapDump(@NonNull Reader in, long end, boolean secondPass) throws IOException {
		while (in.position() < end) {
			int sub = in.readByte() & 0xff;
			switch (sub) {
				case SUB_INSTANCE_DUMP: {
					in.skip(idSize + 4);
					long cls = in.readId(idSize);
					long size = in.readInt() & 0xffffffffL;
					if (secondPass) {
						attributeArrays(in, cls, size);
					} else {
						count(cls, size + idSize + OBJECT_OVERHEAD);
						in.skip(size);
					}
					break;
				}
				case SUB_OBJECT_ARRAY_DUMP: {
					long arrayId = in.readId(idSize);
					in.skip(4);
					long num = in.readInt() & 0xffffffffL;
					long elementClass = in.readId(idSize);
					long bytes = num * idSize + ARRAY_OVERHEAD;
					if (!secondPass) {
						count(-elementClass, bytes);
						rememberBigArray(arrayId, bytes, -elementClass);
					}
					in.skip(num * idSize);
					break;
				}
				case SUB_PRIMITIVE_ARRAY_DUMP: {
					long arrayId = in.readId(idSize);
					in.skip(4);
					long num = in.readInt() & 0xffffffffL;
					int type = in.readByte() & 0xff;
					int elementSize = type < PRIMITIVE_SIZES.length ? PRIMITIVE_SIZES[type] : 1;
					long bytes = num * elementSize + ARRAY_OVERHEAD;
					if (!secondPass) {
						count(Long.MIN_VALUE + type, bytes);
						rememberBigArray(arrayId, bytes, Long.MIN_VALUE + type);
					}
					in.skip(num * elementSize);
					break;
				}
				case SUB_CLASS_DUMP:
					readClassDump(in, secondPass);
					break;
				case SUB_HEAP_DUMP_INFO: {
					currentHeap = in.readInt();
					heapNames.put((long) currentHeap, in.readId(idSize));
					break;
				}
				case SUB_PRIMITIVE_ARRAY_NODATA:
					in.skip(idSize + 4 + 4 + 1);
					break;
				case 0x01:                                  // root jni global: object + ref
					in.skip(idSize * 2L);
					break;
				case 0x02:                                  // root jni local
				case 0x03:                                  // root java frame
				case 0x08:                                  // root thread object
				case 0x8e:                                  // root jni monitor
					in.skip(idSize + 8L);
					break;
				case 0x04:                                  // root native stack
				case 0x06:                                  // root thread block
					in.skip(idSize + 4L);
					break;
				case 0xff:                                  // root unknown
				case 0x05:                                  // root sticky class
				case 0x07:                                  // root monitor used
				case 0x89:                                  // root interned string
				case 0x8a:                                  // root finalizing
				case 0x8b:                                  // root debugger
				case 0x8c:                                  // root reference cleanup
				case 0x8d:                                  // root vm internal
				case 0x90:                                  // unreachable
					in.skip(idSize);
					break;
				default:
					// an unknown subrecord means the stream is no longer aligned, and every
					// number after it would be invented; stop instead
					throw new IOException("Unknown heap dump subrecord 0x" + Integer.toHexString(sub)
							+ " at " + in.position());
			}
		}
	}

	private void readClassDump(@NonNull Reader in, boolean secondPass) throws IOException {
		long classId = in.readId(idSize);
		in.skip(4);
		long superClass = in.readId(idSize);
		in.skip(idSize * 5L + 4L);
		int constants = in.readShort() & 0xffff;
		for (int i = 0; i < constants; i++) {
			in.skip(2);
			in.skip(valueSize(in.readByte() & 0xff));
		}
		int statics = in.readShort() & 0xffff;
		for (int i = 0; i < statics; i++) {
			in.skip(idSize);
			in.skip(valueSize(in.readByte() & 0xff));
		}
		int fields = in.readShort() & 0xffff;
		if (secondPass) {
			in.skip((idSize + 1L) * fields);
			return;
		}
		byte[] types = new byte[fields];
		for (int i = 0; i < fields; i++) {
			in.skip(idSize);
			types[i] = (byte) in.readByte();
		}
		superClasses.put(classId, superClass);
		declaredFields.put(classId, types);
	}

	private void rememberBigArray(long arrayId, long bytes, long kind) {
		if (bytes >= BIG_ARRAY_BYTES) {
			bigArrays.put(arrayId, new long[] {bytes, kind});
		}
	}

	/** Reads one instance and credits any large array it points at to this instance's class. */
	private void attributeArrays(@NonNull Reader in, long classId, long size) throws IOException {
		int[] offsets = offsetsOf(classId);
		if (offsets.length == 0 || size > Integer.MAX_VALUE) {
			in.skip(size);
			return;
		}
		int length = (int) size;
		if (instance.length < length) {
			instance = new byte[length];
		}
		in.readFully(instance, length);
		for (int offset : offsets) {
			if (offset + idSize > length) {
				break;
			}
			long target = readId(instance, offset);
			if (target == 0) {
				continue;
			}
			long[] array = bigArrays.remove(target);
			if (array != null) {
				String key = className(classId) + " -> " + nameOf(array[1]);
				long[] owner = owners.get(key);
				if (owner == null) {
					owner = new long[2];
					owners.put(key, owner);
				}
				owner[0] += array[0];
				owner[1]++;
			}
		}
	}

	private long readId(@NonNull byte[] bytes, int offset) {
		long value = 0;
		for (int i = 0; i < idSize; i++) {
			value = (value << 8) | (bytes[offset + i] & 0xff);
		}
		return value;
	}

	/** Offsets of the object fields of a class, its own first and then its superclasses'. */
	@NonNull
	private int[] offsetsOf(long classId) {
		int[] cached = objectFieldOffsets.get(classId);
		if (cached != null) {
			return cached;
		}
		int[] offsets = new int[16];
		int found = 0;
		int offset = 0;
		long current = classId;
		while (current != 0) {
			byte[] types = declaredFields.get(current);
			if (types == null) {
				break;
			}
			for (byte type : types) {
				if (type == 2) {
					if (found == offsets.length) {
						int[] bigger = new int[found * 2];
						System.arraycopy(offsets, 0, bigger, 0, found);
						offsets = bigger;
					}
					offsets[found++] = offset;
				}
				offset += valueSize(type);
			}
			Long superClass = superClasses.get(current);
			current = superClass != null ? superClass : 0;
		}
		int[] result = new int[found];
		System.arraycopy(offsets, 0, result, 0, found);
		objectFieldOffsets.put(classId, result);
		return result;
	}

	private int valueSize(int type) {
		if (type == 2) {
			return idSize;
		}
		return type < PRIMITIVE_SIZES.length && PRIMITIVE_SIZES[type] > 0 ? PRIMITIVE_SIZES[type] : 1;
	}

	private void count(long key, long bytes) {
		long[] entry = byClass.get(key);
		if (entry == null) {
			entry = new long[2];
			byClass.put(key, entry);
		}
		entry[0]++;
		entry[1] += bytes;
		long[] heap = byHeap.get(currentHeap);
		if (heap == null) {
			heap = new long[2];
			byHeap.put(currentHeap, heap);
		}
		heap[0]++;
		heap[1] += bytes;
	}

	@NonNull
	private String nameOf(long key) {
		if (key == Long.MIN_VALUE) {
			return "unknown[]";
		}
		if (key < Long.MIN_VALUE + PRIMITIVES.length && key > Long.MIN_VALUE) {
			return PRIMITIVES[(int) (key - Long.MIN_VALUE)] + "[]";
		}
		if (key < 0) {
			return className(-key);
		}
		return className(key);
	}

	@NonNull
	private String className(long classId) {
		Long nameId = classNames.get(classId);
		String name = nameId != null ? strings.get(nameId) : null;
		return name != null ? name : "unknown";
	}

	@NonNull
	private String format() {
		List<Map.Entry<Long, long[]>> entries = new ArrayList<>(byClass.entrySet());
		Collections.sort(entries, (a, b) -> Long.compare(b.getValue()[1], a.getValue()[1]));
		long totalBytes = 0;
		long totalCount = 0;
		for (Map.Entry<Long, long[]> entry : entries) {
			totalCount += entry.getValue()[0];
			totalBytes += entry.getValue()[1];
		}
		StringBuilder sb = new StringBuilder();
		sb.append("objects=").append(totalCount).append(" bytes=").append(totalBytes / 1024).append("K\n");
		for (Map.Entry<Integer, long[]> heap : byHeap.entrySet()) {
			Long nameId = heapNames.get((long) (int) heap.getKey());
			String name = nameId != null ? strings.get(nameId) : null;
			sb.append("heap ").append(name != null ? name : String.valueOf(heap.getKey()));
			sb.append(' ').append(heap.getValue()[1] / 1024).append("K ");
			sb.append(heap.getValue()[0]).append(" objects\n");
		}
		if (!owners.isEmpty()) {
			List<Map.Entry<String, long[]>> held = new ArrayList<>(owners.entrySet());
			Collections.sort(held, (a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]));
			sb.append("--- bytesK count holder of arrays >= ").append(BIG_ARRAY_BYTES / 1024).append("K\n");
			int written = 0;
			for (Map.Entry<String, long[]> entry : held) {
				if (entry.getValue()[0] < 64 * 1024 || written++ >= 40) {
					break;
				}
				sb.append(entry.getValue()[0] / 1024).append('\t');
				sb.append(entry.getValue()[1]).append('\t');
				sb.append(entry.getKey()).append('\n');
			}
			long unattributed = 0;
			for (long[] array : bigArrays.values()) {
				unattributed += array[0];
			}
			sb.append("unattributed\t").append(bigArrays.size()).append('\t');
			sb.append(unattributed / 1024).append("K still held by arrays or roots\n");
		}
		sb.append("--- bytesK count class\n");
		for (Map.Entry<Long, long[]> entry : entries) {
			if (entry.getValue()[1] < 64 * 1024) {
				break;
			}
			sb.append(entry.getValue()[1] / 1024).append('\t');
			sb.append(entry.getValue()[0]).append('\t');
			sb.append(nameOf(entry.getKey())).append('\n');
		}
		return sb.toString();
	}

	/** Sequential reader that knows how far into the file it is, without seeking. */
	private static class Reader {

		private final InputStream in;
		private final byte[] buffer = new byte[8];
		private long position;

		Reader(@NonNull InputStream in) {
			this.in = in;
		}

		long position() {
			return position;
		}

		int readByte() throws IOException {
			int value = in.read();
			if (value < 0) {
				throw new EOFException();
			}
			position++;
			return value;
		}

		private void readFully(int count) throws IOException {
			int read = 0;
			while (read < count) {
				int step = in.read(buffer, read, count - read);
				if (step < 0) {
					throw new EOFException();
				}
				read += step;
			}
			position += count;
		}

		int readShort() throws IOException {
			readFully(2);
			return ((buffer[0] & 0xff) << 8) | (buffer[1] & 0xff);
		}

		int readInt() throws IOException {
			readFully(4);
			return ((buffer[0] & 0xff) << 24) | ((buffer[1] & 0xff) << 16)
					| ((buffer[2] & 0xff) << 8) | (buffer[3] & 0xff);
		}

		long readId(int idSize) throws IOException {
			if (idSize == 8) {
				readFully(8);
				long value = 0;
				for (int i = 0; i < 8; i++) {
					value = (value << 8) | (buffer[i] & 0xff);
				}
				return value;
			}
			return readInt() & 0xffffffffL;
		}

		void readFully(@NonNull byte[] into, int length) throws IOException {
			int read = 0;
			while (read < length) {
				int step = in.read(into, read, length - read);
				if (step < 0) {
					throw new EOFException();
				}
				read += step;
			}
			position += length;
		}

		@NonNull
		String readString(int length) throws IOException {
			if (length <= 0) {
				return "";
			}
			byte[] bytes = new byte[length];
			int read = 0;
			while (read < length) {
				int step = in.read(bytes, read, length - read);
				if (step < 0) {
					throw new EOFException();
				}
				read += step;
			}
			position += length;
			return new String(bytes, StandardCharsets.UTF_8);
		}

		void skip(long count) throws IOException {
			long left = count;
			while (left > 0) {
				long step = in.skip(left);
				if (step <= 0) {
					if (in.read() < 0) {
						throw new EOFException();
					}
					step = 1;
				}
				left -= step;
			}
			position += count;
		}
	}
}
