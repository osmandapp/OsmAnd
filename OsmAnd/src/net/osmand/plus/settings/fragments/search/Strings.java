package net.osmand.plus.settings.fragments.search;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

class Strings {

	public static String joinNonNullElements(final String delimiter, final List<? extends CharSequence> elements) {
		return String.join(delimiter, filterNonNull(elements));
	}

	private static <T> List<T> filterNonNull(final List<T> ts) {
		return ts
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}
}
