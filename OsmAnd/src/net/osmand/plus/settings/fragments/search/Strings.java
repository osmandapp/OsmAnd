package net.osmand.plus.settings.fragments.search;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

class Strings {

	public static List<CharSequence> concat(final Optional<CharSequence[]> entries,
											final Optional<String> description) {
		final Builder<CharSequence> builder = ImmutableList.builder();
		entries.map(Arrays::asList).ifPresent(builder::addAll);
		description.ifPresent(builder::add);
		return builder.build();
	}

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
