package net.osmand.binary;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Random;

import org.junit.Test;

/**
 * Route gpx files are written through the shared {@link net.osmand.shared.util.StringBundle} now,
 * and the files already in the wild were written through {@link StringBundle}. This runs the same
 * values through both and asserts they produce the same text and read it back the same way.
 *
 * These live here because this is the only module where both implementations are on the classpath.
 */
public class StringBundleCompatTest {

	private static final String KEY = "k";

	private String javaText(java.util.function.Consumer<StringBundle> write) {
		StringBundle bundle = new StringBundle();
		write.accept(bundle);
		return bundle.getString(KEY, null);
	}

	private String sharedText(java.util.function.Consumer<net.osmand.shared.util.StringBundle> write) {
		net.osmand.shared.util.StringBundle bundle = new net.osmand.shared.util.StringBundle();
		write.accept(bundle);
		return bundle.getString(KEY, null);
	}

	@Test
	public void testScalarsProduceTheSameText() {
		assertEquals(javaText(b -> b.putInt(KEY, -42)), sharedText(b -> b.putInt(KEY, -42)));
		assertEquals(javaText(b -> b.putLong(KEY, 1234567890123L)), sharedText(b -> b.putLong(KEY, 1234567890123L)));
		assertEquals(javaText(b -> b.putBoolean(KEY, true)), sharedText(b -> b.putBoolean(KEY, true)));
		assertEquals(javaText(b -> b.putString(KEY, "text")), sharedText(b -> b.putString(KEY, "text")));
		assertEquals(javaText(b -> b.putFloat(KEY, 12.5f)), sharedText(b -> b.putFloat(KEY, 12.5f)));
		for (int digits = 2; digits <= 6; digits++) {
			int d = digits;
			assertEquals("digits " + d,
					javaText(b -> b.putFloat(KEY, 63.456f, d)), sharedText(b -> b.putFloat(KEY, 63.456f, d)));
			assertEquals("digits " + d,
					javaText(b -> b.putFloat(KEY, 3f, d)), sharedText(b -> b.putFloat(KEY, 3f, d)));
		}
	}

	@Test
	public void testIntArrayRoundTrip() {
		int[][] arrays = {{}, {1}, {1, 2, 3}, {-5, 0, 7}};
		for (int[] array : arrays) {
			String java = javaText(b -> b.putArray(KEY, array));
			String shared = sharedText(b -> b.putArray(KEY, array));
			assertEquals(java, shared);

			StringBundle javaBundle = new StringBundle();
			javaBundle.putArray(KEY, array);
			net.osmand.shared.util.StringBundle sharedBundle = new net.osmand.shared.util.StringBundle();
			sharedBundle.putArray(KEY, array);
			assertArrayEquals(javaBundle.getIntArray(KEY, null), sharedBundle.getIntArray(KEY, null));
		}
	}

	@Test
	public void testIntIntArrayRoundTripWithEmptyRows() {
		// null rows are what point types look like for a point that carries none
		int[][] array = {{1, 2}, null, {3}, {}, {4, 5, 6}};

		StringBundle javaBundle = new StringBundle();
		javaBundle.putArray(KEY, array);
		net.osmand.shared.util.StringBundle sharedBundle = new net.osmand.shared.util.StringBundle();
		sharedBundle.putArray(KEY, array);
		assertEquals(javaBundle.getString(KEY, null), sharedBundle.getString(KEY, null));

		int[][] fromJava = javaBundle.getIntIntArray(KEY, null);
		int[][] fromShared = sharedBundle.getIntIntArray(KEY, null);
		assertEquals(fromJava.length, fromShared.length);
		for (int i = 0; i < fromJava.length; i++) {
			assertArrayEquals("row " + i, fromJava[i], fromShared[i]);
		}
	}

	@Test
	public void testTrailingEmptyRowsAreDroppedByBoth() {
		// java's String.split drops the empty parts at the end, so the last rows do not come back
		int[][] array = {{1}, null, null};
		StringBundle javaBundle = new StringBundle();
		javaBundle.putArray(KEY, array);
		net.osmand.shared.util.StringBundle sharedBundle = new net.osmand.shared.util.StringBundle();
		sharedBundle.putArray(KEY, array);
		assertEquals(1, javaBundle.getIntIntArray(KEY, null).length);
		assertEquals(1, sharedBundle.getIntIntArray(KEY, null).length);
	}

	@Test
	public void testUnparseableValuesFallBackTheSameWay() {
		StringBundle javaBundle = new StringBundle();
		javaBundle.putString(KEY, "not a number");
		net.osmand.shared.util.StringBundle sharedBundle = new net.osmand.shared.util.StringBundle();
		sharedBundle.putString(KEY, "not a number");

		assertEquals(javaBundle.getInt(KEY, 7), (int) sharedBundle.getInt(KEY, 7));
		assertEquals(javaBundle.getLong(KEY, 7L), (long) sharedBundle.getLong(KEY, 7L));
		assertEquals(javaBundle.getFloat(KEY, 7f), sharedBundle.getFloat(KEY, 7f), 1e-6f);
		assertEquals(javaBundle.getBoolean(KEY, true), sharedBundle.getBoolean(KEY, true));
		assertNull(javaBundle.getIntArray(KEY, null));
		assertNull(sharedBundle.getIntArray(KEY, null));
	}

	@Test
	public void testRandomFloatsProduceTheSameText() {
		Random random = new Random(20260908L);
		for (int digits = 2; digits <= 6; digits++) {
			int d = digits;
			for (int i = 0; i < 20_000; i++) {
				float value = (random.nextFloat() - 0.3f) * (float) Math.pow(10, random.nextInt(6));
				assertEquals(value + " at " + d,
						javaText(b -> b.putFloat(KEY, value, d)), sharedText(b -> b.putFloat(KEY, value, d)));
			}
		}
	}
}
