package net.osmand.shared.compat;

import org.junit.Assume;

/**
 * The compat tests of the copy of the search run only when {@code OSMAND_SEARCH_COMPAT} is set: every
 * build of the app runs the tests of OsmAnd-java, and these take minutes and more heap than it gives.
 * Run them with {@code OSMAND_TEST_HEAP=1g} as well. Without the variable a {@code --tests} filter
 * that names only them finds no tests, and Gradle fails.
 */
final class SearchCompat {

	private SearchCompat() {
	}

	static boolean enabled() {
		String value = System.getenv("OSMAND_SEARCH_COMPAT");
		return value != null && !value.isEmpty();
	}

	/** Skips the test class unless {@code OSMAND_SEARCH_COMPAT} is set. */
	static void assumeEnabled() {
		Assume.assumeTrue("set OSMAND_SEARCH_COMPAT to run", enabled());
	}
}
