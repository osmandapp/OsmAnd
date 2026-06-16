package net.osmand.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.*;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;

import gnu.trove.list.array.TIntArrayList;
import net.osmand.binary.Abbreviations;
import net.osmand.binary.CommonWords;
import net.osmand.search.core.SearchPhrase;

/**
 * Basic algorithms that are used in Search
 */
public class SearchAlgorithms {
    public static final char SUFFIX_DICT_MARKER_RAW_ESCAPE = '\uE000';
    public static final int SUFFIX_DICT_MARKER_BASE = 0xE100;
    public static final int SUFFIX_DICT_MARKER_MAX = 0xF8FF;
    private static final char[] CHARS_TO_NORMALIZE_KEY = {'’', 'ʼ', '(', ')', '´', '`', '′', '‵', 'ʹ'}; // remove () subcities
    private static final char[] CHARS_TO_NORMALIZE_VALUE = {'\'', '\'', ' ', ' ', '\'', '\'', '\'', '\'', '\''};
    private static final char[] APOSTROPHES = {'\'', '’', 'ʼ', '´', '`', '′', '‵', 'ʹ'};
    
    private SearchAlgorithms() {}

	private record CodePointPrefixMatch(int leftOffset, int rightOffset, int commonPrefixCodePointLength) {
	}

    private static CodePointPrefixMatch startWith(String token, String prefix) {
        int leftOffset = 0;
        int rightOffset = 0;
        int commonPrefixCodePointLength = 0;
        while (leftOffset < token.length() && rightOffset < prefix.length()) {
            int leftCodePoint = token.codePointAt(leftOffset);
            int rightCodePoint = prefix.codePointAt(rightOffset);
            if (leftCodePoint != rightCodePoint) {
                break;
            }
            leftOffset += Character.charCount(leftCodePoint);
            rightOffset += Character.charCount(rightCodePoint);
            commonPrefixCodePointLength++;
        }
        return new CodePointPrefixMatch(leftOffset, rightOffset, commonPrefixCodePointLength);
    }

    
    private static List<String> split(String name) {
        int prev = -1;
        Set<String> namesToAdd = new LinkedHashSet<>();

        for (int i = 0; i <= name.length(); ) {
            boolean tokenCharacter = false;
            int currentCodePointCharCount = 1;
            if (i != name.length()) {
                int codePoint = name.codePointAt(i);
                currentCodePointCharCount = Character.charCount(codePoint);
                tokenCharacter = isTokenCharacter(name, i, prev != -1) || codePoint == '\'';
            }
            if (!tokenCharacter) {
                if (prev != -1) {
                    String substr = name.substring(prev, i);
                    namesToAdd.add(substr.toLowerCase());
                    prev = -1;
                }
            } else {
                if (prev == -1) {
                    prev = i;
                }
            }
            i += currentCodePointCharCount;
        }
        return new ArrayList<>(namesToAdd);
    }
    
    
	public static List<String> splitAndNormalizeSearchQuery(String query, List<String> original) {
		String normalizedQuery = canonicalizePunctuation(query);
		List<String> o = SearchPhrase.splitWords(normalizedQuery, new ArrayList<String>(), SearchPhrase.ALLDELIMITERS);
		List<String> queryTokens = new ArrayList<String>();
		for (String token : o) {
			String normalizedToken = normalizeToken(token);
			if (!normalizedToken.isEmpty()) {
				queryTokens.add(normalizedToken);
				original.add(token);
			}
		}
		if (ArabicNormalizer.isSpecialArabic(normalizedQuery)) {
			String arabic = ArabicNormalizer.normalize(normalizedQuery);
			if (arabic != null && !arabic.equals(normalizedQuery)) {
				queryTokens.clear();
				original.clear();
				for (String token : SearchPhrase.splitWords(arabic, new ArrayList<String>(), SearchPhrase.ALLDELIMITERS)) {
					String normalizedToken = normalizeToken(token);
					if (!normalizedToken.isEmpty()) {
						queryTokens.add(normalizedToken);
						original.add(token);
					}
				}
			}
		}
		return queryTokens;
	}
    

    /**
     * Produces unique normalized tokens from the query, plus Arabic-normalized variants when applicable.
     */
    public static List<String> splitAndNormalize(String query) {
        String normalizedQuery = canonicalizePunctuation(query);
        Set<String> queryTokens = new LinkedHashSet<>();
        for (String token : split(normalizedQuery)) {
            String normalizedToken = normalizeToken(token);
            if (!normalizedToken.isEmpty()) {
                queryTokens.add(normalizedToken);
            }
        }
        if (ArabicNormalizer.isSpecialArabic(normalizedQuery)) {
            String arabic = ArabicNormalizer.normalize(normalizedQuery);
            if (arabic != null && !arabic.equals(normalizedQuery)) {
                for (String token : split(arabic)) {
                    String normalizedToken = normalizeToken(token);
                    if (!normalizedToken.isEmpty()) {
                        queryTokens.add(normalizedToken);
                    }
                }
            }
        }
        return new ArrayList<>(queryTokens);
    }
    
    public static String normalizeToken(String token) {
        if (token == null) {
            return "";
        }
        return Normalizer.normalize(token, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
    }

    /**
    * Canonicalizes punctuation variants so equivalent search text is tokenized the same way.
    */
    public static String canonicalizePunctuation(String s) {
        boolean norm = Algorithms.containsChar(s, CHARS_TO_NORMALIZE_KEY);
        if (!norm) {
            return s;
        }
        for (int k = 0; k < CHARS_TO_NORMALIZE_KEY.length; k++) {
            s = s.replace(CHARS_TO_NORMALIZE_KEY[k], CHARS_TO_NORMALIZE_VALUE[k]);
        }
        return s;
    }

    /**
     * Split string by words and convert to lowercase, use as delimiter all chars except letters and digits
     * @param str input string
     * @return result words list
     */
    public static List<String> splitByWordsLowercase(String str) {
        List<String> splitStr = new ArrayList<>();
        int prev = -1;
        for (int i = 0; i <= str.length(); i++) {
            if (i == str.length() ||
                    (!Character.isLetter(str.charAt(i)) && !Character.isDigit(str.charAt(i)))) {
                if (prev != -1) {
                    String subStr = str.substring(prev, i);
                    splitStr.add(subStr.toLowerCase());
                    prev = -1;
                }
            } else {
                if (prev == -1) {
                    prev = i;
                }
            }
        }
        return splitStr;
    }

    public static String removeQuotes(String s) {
        if (!s.contains("«") && !s.contains("»")) {
            return s;
        }
        return s.replace("«", "").replace("»", "");
    }
    
    public static String removeApostrophes(String s) {
        if (!Algorithms.containsChar(s, APOSTROPHES)) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean apostroph = false;
            for (char d : APOSTROPHES) {
                if (d == c) {
                    apostroph = true;
                    break;
                }
            }
            if (!apostroph) {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    
    private static boolean isTokenCharacter(String value, int index, boolean tokenAlreadyStarted) {
        int character = value.codePointAt(index);
        if (Character.isLetter(character) || Character.isDigit(character)) {
            return true;
        }
        int nextIndex = index + Character.charCount(character);
        int previousIndex = index > 0 ? value.offsetByCodePoints(index, -1) : -1;
        boolean isHyphenNearNumber = character == '-'
                && ((nextIndex < value.length() && Character.isDigit(value.codePointAt(nextIndex)))
                || (previousIndex >= 0 && Character.isDigit(value.codePointAt(previousIndex))));
        if (isHyphenNearNumber) {
            return true;
        }
        int characterType = Character.getType(character);
        return tokenAlreadyStarted && (characterType == Character.NON_SPACING_MARK
                || characterType == Character.COMBINING_SPACING_MARK
                || characterType == Character.ENCLOSING_MARK);
    }

    /**
     * Decodes either a raw suffix entry or a delta entry that reuses a prefix from the previous suffix.
     */
    public static String nameIndexDecodeDictionarySuffix(String previousSuffix, String encodedSuffix) {
        if (encodedSuffix.isEmpty()) {
            return "";
        }
        int markerCodePoint = encodedSuffix.codePointAt(0);
        if (markerCodePoint >= SUFFIX_DICT_MARKER_BASE && markerCodePoint <= SUFFIX_DICT_MARKER_MAX) {
            if (previousSuffix == null) {
                throw new IllegalStateException("Delta-encoded suffix dictionary entry requires previous suffix");
            }
            int commonPrefixCodePointLength = markerCodePoint - SUFFIX_DICT_MARKER_BASE;
            int prefixEndOffset = previousSuffix.offsetByCodePoints(0,
                    Math.min(commonPrefixCodePointLength, previousSuffix.codePointCount(0, previousSuffix.length())));
            String suffixRemainder = encodedSuffix.substring(Character.charCount(markerCodePoint));
            return Normalizer.normalize(previousSuffix.substring(0, prefixEndOffset) + suffixRemainder, Normalizer.Form.NFC);
        }
        return Normalizer.normalize(decodeRawSuffix(encodedSuffix), Normalizer.Form.NFC);
    }
    
    private static String decodeRawSuffix(String encodedSuffix) {
        if (encodedSuffix.isEmpty()) {
            return "";
        }
        int markerCodePoint = encodedSuffix.codePointAt(0);
        if (markerCodePoint == SUFFIX_DICT_MARKER_RAW_ESCAPE) {
            return encodedSuffix.substring(Character.charCount(markerCodePoint));
        }
        return encodedSuffix;
    }

    private static final int MARKER_LCP_LENGTH = SUFFIX_DICT_MARKER_MAX - SUFFIX_DICT_MARKER_BASE;
    public static final String EMPTY_SUFFIX_DICTIONARY_SENTINEL = "\uE100";
    

    private static boolean startsWithSuffixMarker(String value) {
        if (value.isEmpty()) {
            return false;
        }
        int markerCodePoint = value.codePointAt(0);
        return markerCodePoint == SUFFIX_DICT_MARKER_RAW_ESCAPE
                || (markerCodePoint >= SUFFIX_DICT_MARKER_BASE && markerCodePoint <= SUFFIX_DICT_MARKER_MAX);
    }
    
    
    private static int countCodePoints(String value) {
        return value.codePointCount(0, value.length());
    }
    
	private static String nameIndexEncodeSuffix(String suffix) {
        return startsWithSuffixMarker(suffix) ? SUFFIX_DICT_MARKER_RAW_ESCAPE + suffix : suffix;
    }
    
    public static String nameIndexEncodeSuffix(String suffix, String previousSuffix) {
        String encodedRawSuffix = nameIndexEncodeSuffix(suffix);
        if (previousSuffix == null) {
            return encodedRawSuffix;
        }
        int commonPrefixCodePointLength = startWith(previousSuffix, suffix).commonPrefixCodePointLength;
        if (commonPrefixCodePointLength > MARKER_LCP_LENGTH) {
            return encodedRawSuffix;
        }
        int offset = suffix.offsetByCodePoints(0, commonPrefixCodePointLength);
        String suffixRemainder = suffix.substring(offset);
        String deltaEncodedSuffix = new String(Character.toChars(SUFFIX_DICT_MARKER_BASE + commonPrefixCodePointLength))
                + suffixRemainder;
        return countCodePoints(deltaEncodedSuffix) < countCodePoints(encodedRawSuffix) ? deltaEncodedSuffix : encodedRawSuffix;
    }

 
    public static String replaceGermanSS(String fullText) {
        int i;
        while ((i = fullText.indexOf('ß')) != -1) {
            fullText = fullText.substring(0, i) + "ss" + fullText.substring(i + 1);
        }
        return fullText;
    }

	public static void removeCommonWords(List<String> names) {
		// remove all common words (most common delete first) but leave at least 1
		int pos = 0;
		while (names.size() > 1 && pos != -1) {
			int prioP = Integer.MAX_VALUE;
			pos = -1;
			for (int k = 0; k < names.size(); k++) {
				String word = names.get(k);
				int prio = CommonWords.getCommon(word);
				if (Abbreviations.isConjunction(word)) {
					prio = 0;
				}
				if (prio != -1 && prio < prioP) {
					pos = k;
					prioP = prio;
				}
			}
			if (pos != -1) {
				names.remove(pos);
			}
		}
	}
	
	
	// [zoom - default = 15 - 1km],[xzoom-left],[xzoom-right-delta],[y-top],[y-bottom-delta],...
	// input is boundary encoded - 4 first uints is bbox -  of x31-left, y31-top, x31-right, y31-bottom
	public static int[] encodeBboxForNameAtoms(int zoom, int[] bbox31) {
		int[] res = new int[bbox31.length + 1];
		res[0] = zoom;
		int dz = 31 - zoom;
		// support for array of bboxes could be added later 
		// without it some width could be negative -180 meridian
		res[1] = bbox31[0] >> dz;
		res[2] = Math.max(1, (bbox31[2] >> dz) - res[1]);
		res[3] = bbox31[1] >> dz;
		res[4] = Math.max(1, (bbox31[3] >> dz) - res[3]);
		return res;
	}
	
	// return array of x31-left, y31-top, x31-right, y31-bottom
	public static int[] decodeBboxForNameAtoms(int[] vls, int x16, int y16) {
		if (vls.length < 5) {
			return null;
		}
		int zoom = vls[0];
		int[] res = new int[((vls.length - 1) / 4) * 4];
		for(int ind = 0; ind < res.length; ind+=4) {
			res[ind] = ((x16 >> (16 - zoom)) - vls[ind + 1]) << (31 - zoom);
			res[ind + 1] = ((y16 >> (16 - zoom)) - vls[ind + 3]) << (31 - zoom);
			res[ind + 2] = (vls[ind + 2] << (31 - zoom)) + res[ind];
			res[ind + 3] = (vls[ind + 4] << (31 - zoom)) + res[ind + 1];
		}
		return res;
	}
	
	public static int[] decodeBboxForNameAtomsBytes(ByteString bbox, int x16, int y16) {
		int[] dBbox = null;
		if (bbox != null) {
			ByteArrayInputStream bis = new ByteArrayInputStream(bbox.toByteArray());
			TIntArrayList lst = new TIntArrayList();
			while (bis.available() > 0) {
				try {
					int n = CodedInputStream.readRawVarint32(bis);
					lst.add(n);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			dBbox = SearchAlgorithms.decodeBboxForNameAtoms(lst.toArray(), x16, y16);
		}
		return dBbox;
	}
	
	
}

