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

    private record CodePointPrefixMatch(int leftOffset, int rightOffset, int commonPrefixCodePointLength) {}

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

    public static int commonPrefixLength(String left, String right) {
        return startWith(left, right).commonPrefixCodePointLength;
    }

    public static int suffixOffsetAfterPrefix(String token, String prefix) {
        CodePointPrefixMatch prefixMatch = startWith(token, prefix);
        if (prefixMatch.rightOffset != prefix.length()) {
            return -1;
        }
        return prefixMatch.leftOffset < token.length() ? prefixMatch.leftOffset : -1;
    }

    public static Set<String> splitSearchNames(String name) {
        int prev = -1;
        Set<String> namesToAdd = new HashSet<>();

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
        return namesToAdd;
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
