package net.osmand.plus.settings.fragments.search;

import android.util.Pair;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class LexicographicalListComparator<T> implements Comparator<List<T>> {

	private final Comparator<T> elementComparator;

	public LexicographicalListComparator(final Comparator<T> elementComparator) {
		this.elementComparator = elementComparator;
	}

	@Override
	public int compare(final List<T> list1, final List<T> list2) {
		if (list1.size() < list2.size()) {
			return -1;
		} else if (list1.size() > list2.size()) {
			return +1;
		} else {
			return LexicographicalListComparator
					.zip(list1, list2)
					.stream()
					.map(elementPair -> elementComparator.compare(elementPair.first, elementPair.second))
					.filter(compareResult -> compareResult != 0)
					.findFirst()
					.orElse(0);
		}
	}

	// adapted from https://stackoverflow.com/questions/31963297/how-to-zip-two-java-lists
	private static <A, B> List<Pair<A, B>> zip(final List<A> as, final List<B> bs) {
		if (as.size() != bs.size()) {
			throw new IllegalArgumentException();
		}
		return IntStream.range(0, as.size())
				.mapToObj(i -> Pair.create(as.get(i), bs.get(i)))
				.collect(Collectors.toList());
	}
}
