package net.osmand.plus.settings.fragments.search;

import androidx.annotation.Nullable;

import com.google.common.collect.ImmutableList.Builder;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

class SearchableInfoProviderHelper {

	public static CharSequence[] nullToEmpty(final CharSequence[] charSequences) {
		return charSequences != null ? charSequences : new CharSequence[0];
	}

	public static <T> List<T> nullToEmpty(final List<T> ts) {
		return ts != null ? ts : Collections.emptyList();
	}

	public static String join(final String delimiter, final @Nullable List<? extends CharSequence> elements) {
		return String.join(delimiter, nullToEmpty(elements));
	}

	public static void addIfNonNull(final Builder<CharSequence> builder, final @Nullable String element) {
		if (element != null) {
			builder.add(element);
		}
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
