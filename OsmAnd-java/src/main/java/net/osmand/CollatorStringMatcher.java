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
		// simple collator contains in any part of the base		
		CHECK_CONTAINS,
		// simple collator equals
		CHECK_EQUALS,
	}

	public CollatorStringMatcher(String part, StringMatcherMode mode) {
		this.collator = OsmAndCollator.primaryCollator();
		part = simplifyStringAndAlignChars(part);
		if (part.length() > 0 && part.charAt(part.length() - 1) == '.') {
			part = part.substring(0, part.length() - 1);
			if (mode == StringMatcherMode.CHECK_EQUALS_FROM_SPACE) {
				mode = StringMatcherMode.CHECK_STARTS_FROM_SPACE;
			} else if (mode == StringMatcherMode.CHECK_EQUALS) {
				mode = StringMatcherMode.CHECK_ONLY_STARTS_WITH;
			}
		}
		this.part = part;
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
		
		if (base.length() <= part.length()) {
			return collator.equals(base, part);
		}
		for (int pos = 0; pos <= base.length() - part.length() + 1; pos++) {
			String temp = base.substring(pos, Math.min(pos + part.length() * 2, base.length()));
			for (int length = temp.length(); length >= 0; length--) {
				String temp2 = temp.substring(0, length);
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
	 * @param fullTextP
	 * @param theStart
	 * @return true if searchIn starts with token
	 */
	public static boolean cstartsWith(Collator collator, String fullTextP, String theStart, 
			boolean checkBeginning, boolean checkSpaces, boolean equals) {
		// FUTURE: This is not effective code, it runs on each comparison
		// It would be more efficient to normalize all strings in file and normalize search string before collator  
		theStart = alignChars(theStart);
		String searchIn = simplifyStringAndAlignChars(fullTextP);
		int searchInLength = searchIn.length();
		int startLength = theStart.length();
		if (startLength == 0) {
			return true;
		}
		// this is not correct without (simplifyStringAndAlignChars) because of Auhofstrasse != Auhofstraße
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
							if(i + startLength == searchInLength || 
									isSpace(searchIn.charAt(i + startLength))) {
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
	
	private static String simplifyStringAndAlignChars(String fullText) {
		fullText = fullText.toLowerCase(Locale.getDefault());
		fullText = alignChars(fullText);
		return fullText;
	}

	private static String alignChars(String fullText) {
		int i;
		while ((i = fullText.indexOf('ß')) != -1) {
			fullText = fullText.substring(0, i) + "ss" + fullText.substring(i+1);
		}
		return fullText;
	}

	private static boolean isSpace(char c){
		return !Character.isLetter(c) && !Character.isDigit(c);
	}
	
}
