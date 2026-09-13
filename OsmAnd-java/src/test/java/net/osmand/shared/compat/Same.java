package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

/**
 * Compares what two calls do, not only what they return: a value, or the exception they throw.
 * Java's routing code throws on some inputs (a point without types, an attribute that is not a
 * number, an estimate between two points off the route), and the copy has to do the same there.
 */
final class Same {

	interface Call {
		Object get() throws Exception;
	}

	private Same() {
	}

	/** Doubles that come out of the same formula in a different association order: equal to a few ulps. */
	static void close(String m, double java, double copy) {
		double tolerance = 8 * Math.ulp(Math.max(Math.abs(java), Math.abs(copy)));
		assertEquals(m, java, copy, tolerance);
	}

	static void close(String m, float java, float copy) {
		float tolerance = 8 * Math.ulp(Math.max(Math.abs(java), Math.abs(copy)));
		assertEquals(m, java, copy, tolerance);
	}

	static void outcome(String m, Call java, Call copy) {
		assertEquals(m, describe(java), describe(copy));
	}

	private static String describe(Call call) {
		try {
			Object value = call.get();
			if (value instanceof int[]) {
				return Arrays.toString((int[]) value);
			}
			if (value instanceof float[]) {
				return Arrays.toString((float[]) value);
			}
			if (value instanceof Object[]) {
				return Arrays.deepToString((Object[]) value);
			}
			return String.valueOf(value);
		} catch (Throwable t) {
			return "threw " + t.getClass().getName();
		}
	}
}
