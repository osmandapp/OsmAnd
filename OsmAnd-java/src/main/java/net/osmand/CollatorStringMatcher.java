package net.osmand;

import java.util.Locale;



/**
 * Abstract collator matcher that basically supports subclasses with some collator
 * matching.
 * 
 * @author pavol.zibrita
 */
public class CollatorStringMatcher implements StringMatcher {

	private final Collator collator;
	private final StringMatcherMode mode;
	private final String part;
	
	public static enum StringMatcherMode {
		// tests only first word as base starts with part
		CHECK_ONLY_STARTS_WITH,
		// tests all words (split by space) and one of word should start with a given part
		CHECK_STARTS_FROM_SPACE,
		// tests all words except first (split by space) and one of word should start with a given part
		CHECK_STARTS_FROM_SPACE_NOT_BEGINNING,
		// tests all words (split by space) and one of word should be equal to part
		CHECK_EQUALS_FROM_SPACE,
		// TO DO: make a separate method
		// trims part to make shorter then full text and tests only first word as base starts with part
		TRIM_AND_CHECK_ONLY_STARTS_WITH,
		// simple collator contains in any part of the base		
		CHECK_CONTAINS,
		// simple collator equals
		CHECK_EQUALS,
	}

	public CollatorStringMatcher(String part, StringMatcherMode mode) {
		this.collator = OsmAndCollator.primaryCollator();
		this.part = part.toLowerCase(Locale.getDefault());
		this.mode = mode;
	}

	public Collator getCollator() {
		return collator;
	}
	
	@Override
	public boolean matches(String name) {
		return cmatches(collator, name, part, mode);
	}
	
	
	public static boolean cmatches(Collator collator, String fullName, String part, StringMatcherMode mode){
		switch (mode) {
		case CHECK_CONTAINS:
			return ccontains(collator, fullName, part); 
		case CHECK_EQUALS_FROM_SPACE:
			return cstartsWith(collator, fullName, part, true, true, true);
		case CHECK_STARTS_FROM_SPACE:
			return cstartsWith(collator, fullName, part, true, true, false);
		case CHECK_STARTS_FROM_SPACE_NOT_BEGINNING:
			return cstartsWith(collator, fullName, part, false, true, false);
		case CHECK_ONLY_STARTS_WITH:
			return cstartsWith(collator, fullName, part, true, false, false);
		case TRIM_AND_CHECK_ONLY_STARTS_WITH:
			if (part.length() > fullName.length()) {
				part = part.substring(0, fullName.length());
			}
			return cstartsWith(collator, fullName, part, true, false, false);
		case CHECK_EQUALS:
			return cstartsWith(collator, fullName, part, false, false, true);
		}
		return false;
	}
	
	
	/**
	 * Check if part contains in base
	 *
	 * @param collator Collator to use
	 * @param part String to search
	 * @param base String where to search
	 * @return true if part is contained in base
	 */
	public static boolean ccontains(Collator collator, String base, String part) {
//		int pos = 0;
//		if (part.length() > 3) {
//			// improve searching by searching first 3 characters
//			pos = cindexOf(collator, pos, part.substring(0, 3), base);
//			if (pos == -1) {
//				return false;
//			}
//		}
//		pos = cindexOf(collator, pos, part, base);
//		if (pos == -1) {
//			return false;
//		}
//		return true;
		
		if (base.length() <= part.length())
			return collator.equals(base, part);
		
		for (int pos = 0; pos <= base.length() - part.length() + 1; pos++) {
			String temp = base.substring(pos, base.length());
			
			for (int length = temp.length(); length >= 0; length--) {
				String temp2 = temp.substring(0,  length);
				if (collator.equals(temp2, part)) 
					return true;
			}
		}
		
		return false;
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
	 * Checks if string starts with another string.
	 * Special check try to find as well in the middle of name
	 * 
	 * @param collator
	 * @param fullText
	 * @param theStart
	 * @return true if searchIn starts with token
	 */
	public static boolean cstartsWith(Collator collator, String fullText, String theStart, 
			boolean checkBeginning, boolean checkSpaces, boolean equals) {
		String searchIn = fullText.toLowerCase(Locale.getDefault());
		int searchInLength = searchIn.length();
		int startLength = theStart.length();
		if (startLength == 0) {
			return true;
		}
		if (startLength > searchInLength) {
			return false;
		}
		// simulate starts with for collator
		if (checkBeginning) {
			boolean starts = collator.equals(searchIn.substring(0, startLength), theStart);
			if (starts) {
				if (equals) {
					if (startLength == searchInLength || isSpace(searchIn.charAt(startLength))) {
						return true;
					}
				} else {
					return true;
				}
			}
		}
		if (checkSpaces) {
			for (int i = 1; i <= searchInLength - startLength; i++) {
				if (isSpace(searchIn.charAt(i - 1)) && !isSpace(searchIn.charAt(i))) {
					if (collator.equals(searchIn.substring(i, i + startLength), theStart)) {
						if(equals) {
							if(i + startLength == searchInLength || isSpace(searchIn.charAt(i + startLength))) {
								return true;
							}
						} else {
							return true;
						}
					}
				}
			}
		}
		if (!checkBeginning && !checkSpaces && equals) {
			return collator.equals(searchIn, theStart);
		}
		return false;
	}
	
	private static boolean isSpace(char c){
		return !Character.isLetter(c) && !Character.isDigit(c);
	}
}
