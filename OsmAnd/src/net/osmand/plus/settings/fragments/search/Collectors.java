package net.osmand.plus.settings.fragments.search;

import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.stream.Collector;

public class Collectors {

	public static <T, K, U>
	Collector<T, ?, LinkedHashMap<K, U>> toOrderedMap(Function<? super T, ? extends K> keyMapper,
													  Function<? super T, ? extends U> valueMapper) {

		return java.util.stream.Collectors.toMap(
				keyMapper,
				valueMapper,
				(u1, u2) -> {
					throw new IllegalStateException();
				},
				LinkedHashMap::new);
	}
}
