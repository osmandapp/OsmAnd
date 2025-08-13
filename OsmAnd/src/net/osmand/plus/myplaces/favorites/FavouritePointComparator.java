package net.osmand.plus.myplaces.favorites;

import androidx.annotation.NonNull;

import net.osmand.data.FavouritePoint;
import net.osmand.util.Algorithms;

import java.text.Collator;
import java.util.Comparator;

public class FavouritePointComparator implements Comparator<FavouritePoint> {

	private final Collator collator;

	public FavouritePointComparator(@NonNull Collator collator) {
		this.collator = collator;
	}

	@Override
	public int compare(FavouritePoint o1, FavouritePoint o2) {
		String s1 = o1.getName();
		String s2 = o2.getName();
		int i1 = Algorithms.extractIntegerNumber(s1);
		int i2 = Algorithms.extractIntegerNumber(s2);
		String ot1 = Algorithms.extractIntegerPrefix(s1);
		String ot2 = Algorithms.extractIntegerPrefix(s2);
		// Next 6 lines needed for correct comparison of names with and without digits
		if (ot1.isEmpty()) {
			ot1 = s1;
		}
		if (ot2.isEmpty()) {
			ot2 = s2;
		}
		int res = collator.compare(ot1, ot2);
		if (res == 0) {
			res = Integer.compare(i1, i2);
		}
		if (res == 0) {
			res = collator.compare(s1, s2);
		}
		return res;
	}
}
