package net.osmand.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Basic algorithms that are used in Search
 */
public class SearchAlgorithms {
    public static final char SUFFIX_DICT_MARKER_RAW_ESCAPE = '\uE000';
    public static final int SUFFIX_DICT_MARKER_BASE = 0xE100;
    public static final int SUFFIX_DICT_MARKER_MAX = 0xF8FF;
    
    private SearchAlgorithms() {}

    public static Set<String> splitSearchNames(String name) {
        int prev = -1;
        Set<String> namesToAdd = new HashSet<>();

        for (int i = 0; i <= name.length(); i++) {
            boolean tokenCharacter = i != name.length()
                    && (isTokenCharacter(name, i, prev != -1) || name.charAt(i) == '\'');
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
        }
        return namesToAdd;
    }

    private static boolean isTokenCharacter(String value, int index, boolean tokenAlreadyStarted) {
        char character = value.charAt(index);
        if (Character.isLetter(character) || Character.isDigit(character)) {
            return true;
        }
        boolean isHyphenNearNumber = character == '-'
                && ((index + 1 < value.length() && Character.isDigit(value.charAt(index + 1)))
                || (index - 1 >= 0 && Character.isDigit(value.charAt(index - 1))));
        if (isHyphenNearNumber) {
            return true;
        }
        int characterType = Character.getType(character);
        return tokenAlreadyStarted && (characterType == Character.NON_SPACING_MARK
                || characterType == Character.COMBINING_SPACING_MARK
                || characterType == Character.ENCLOSING_MARK);
    }

    public static List<String> splitAndNormalize(String query) {
        String normalizedQuery = Algorithms.normalizeSearchText(query);
        Set<String> queryTokens = splitSearchNames(normalizedQuery);
        if (ArabicNormalizer.isSpecialArabic(normalizedQuery)) {
            String arabic = ArabicNormalizer.normalize(normalizedQuery);
            if (arabic != null && !arabic.equals(normalizedQuery)) {
                queryTokens.addAll(splitSearchNames(arabic));
            }
        }
        return new ArrayList<>(queryTokens);
    }

    public static String decodeSuffixDictionaryEntry(String previousSuffix, String encodedSuffix) {
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
}
