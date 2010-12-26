package net.osmand;

import java.text.Collator;

/**
 * Abstract collator matcher that basically supports subclasses with some collator
 * matching.
 * 
 * @author pavol.zibrita
 */
public abstract class CollatorStringMatcher implements StringMatcher {

	private final Collator collator;

	public CollatorStringMatcher(Collator collator) {
		this.collator = collator;
	}

	public Collator getCollator() {
		return collator;
	}
	
	/**
	 * Check if part contains in base
	 *
	 * @param collator Collator to use
	 * @param part String to search
	 * @param base String where to search
	 * @return true if part is contained in base
	 */
	public static boolean ccontains(Collator collator, String part, String base) {
		int pos = 0;
		if (part.length() > 3) {
			// improve searching by searching first 3 characters
			pos = cindexOf(collator, pos, part.substring(0, 3), base);
			if (pos == -1) {
				return false;
			}
		}
		pos = cindexOf(collator, pos, part, base);
		if (pos == -1) {
			return false;
		}
		return true;
	}

	private static int cindexOf(Collator collator, int start, String part, String base) {
		for (int pos = start; pos <= base.length() - part.length(); pos++) {
			if (collator.equals(base.substring(pos, pos + part.length()), part)) {
				return pos;
			}
		}
		return -1;
	}

	/**
	 * Checks if string starts with another string
	 * 
	 * @param collator
	 * @param searchIn
	 * @param theStart
	 * @return true if searchIn starts with token
	 */
	public static boolean cstartsWith(Collator collator, String searchIn, String theStart) {
		// simulate starts with for collator
		return collator.equals(
				searchIn.substring(0,
						Math.min(searchIn.length(), theStart.length())), theStart);
	}
}
