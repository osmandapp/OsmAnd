package net.osmand.router.gtfs;

import static org.junit.Assert.assertTrue;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

/**
 * JDK 9 gave the java.nio buffers covariant overrides ({@code ByteBuffer.position(int)} returning ByteBuffer,
 * {@code MappedByteBuffer.duplicate()} and so on). Code compiled against a newer JDK records those signatures, and
 * older Android runtimes do not have them: the app dies with NoSuchMethodError on the first call. Calling through
 * {@code java.nio.Buffer} (or {@code ByteBuffer.duplicate()}) records signatures that exist everywhere. This reads the
 * constant pool of every compiled class of the package and fails on a reference to one of the covariant overrides.
 */
public class GtfsAndroidNioCompatTest {

	private static final Set<String> TYPED_BUFFERS = new HashSet<>(Arrays.asList("java/nio/ByteBuffer",
			"java/nio/CharBuffer", "java/nio/ShortBuffer", "java/nio/IntBuffer", "java/nio/LongBuffer",
			"java/nio/FloatBuffer", "java/nio/DoubleBuffer"));
	private static final Set<String> BUFFER_METHODS = new HashSet<>(Arrays.asList("position", "limit", "flip", "clear",
			"rewind", "mark", "reset"));

	@Test
	public void noCompiledClassCallsAnNioMethodThatOlderAndroidLacks() throws Exception {
		File dir = new File(GtfsIndex.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		File pkg = new File(dir, "net/osmand/router/gtfs");
		List<String> bad = new ArrayList<>();
		int scanned = scan(pkg, bad);
		assertTrue("no classes scanned in " + pkg, scanned > 10);
		assertTrue("references to JDK 9+ only NIO signatures (go through java.nio.Buffer): " + bad, bad.isEmpty());
	}

	private static int scan(File dir, List<String> bad) throws IOException, URISyntaxException {
		int n = 0;
		File[] files = dir.listFiles();
		if (files == null) {
			return 0;
		}
		for (File f : files) {
			if (f.isDirectory()) {
				n += scan(f, bad);
			} else if (f.getName().endsWith(".class")) {
				n++;
				check(f, bad);
			}
		}
		return n;
	}

	private static void check(File f, List<String> bad) throws IOException {
		try (DataInputStream in = new DataInputStream(new FileInputStream(f))) {
			in.readInt();
			in.readUnsignedShort();
			in.readUnsignedShort();
			int count = in.readUnsignedShort();
			String[] utf = new String[count];
			int[] classNameIdx = new int[count];
			int[] natName = new int[count], natDesc = new int[count];
			int[] refClass = new int[count], refNat = new int[count];
			boolean[] isMethodRef = new boolean[count];
			for (int i = 1; i < count; i++) {
				int tag = in.readUnsignedByte();
				switch (tag) {
					case 1:
						utf[i] = in.readUTF();
						break;
					case 3:
					case 4:
						in.readInt();
						break;
					case 5:
					case 6:
						in.readLong();
						i++;
						break;
					case 7:
						classNameIdx[i] = in.readUnsignedShort();
						break;
					case 8:
					case 16:
					case 19:
					case 20:
						in.readUnsignedShort();
						break;
					case 9:
					case 11:
					case 10:
						refClass[i] = in.readUnsignedShort();
						refNat[i] = in.readUnsignedShort();
						isMethodRef[i] = tag != 9;
						break;
					case 12:
						natName[i] = in.readUnsignedShort();
						natDesc[i] = in.readUnsignedShort();
						break;
					case 15:
						in.readUnsignedByte();
						in.readUnsignedShort();
						break;
					case 17:
					case 18:
						in.readInt();
						break;
					default:
						throw new IOException("unknown constant pool tag " + tag + " in " + f);
				}
			}
			for (int i = 1; i < count; i++) {
				if (!isMethodRef[i]) {
					continue;
				}
				String owner = utf[classNameIdx[refClass[i]]];
				String name = utf[natName[refNat[i]]];
				String desc = utf[natDesc[refNat[i]]];
				boolean mapped = "java/nio/MappedByteBuffer".equals(owner)
						&& !"force".equals(name) && !"load".equals(name) && !"isLoaded".equals(name)
						&& !"<init>".equals(name);
				boolean typed = TYPED_BUFFERS.contains(owner) && BUFFER_METHODS.contains(name)
						&& !desc.endsWith(")Ljava/nio/Buffer;");
				if (mapped || typed) {
					bad.add(f.getName() + ": " + owner + "." + name + desc);
				}
			}
		}
	}
}
