package net.osmand.util;

import java.text.Normalizer;
import java.util.*;
import java.util.function.Function;

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

    private static String substringByCodePoints(String value, int codePointCount) {
        if (codePointCount <= 0 || value.isEmpty()) {
            return "";
        }
        int availableCodePointCount = value.codePointCount(0, value.length());
        if (codePointCount >= availableCodePointCount) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, codePointCount));
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

    public static List<String> splitAndNormalize(String query) {
        String normalizedQuery = Algorithms.normalizeSearchText(query);
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
    
    private static String normalizeToken(String token) {
        if (token == null) {
            return "";
        }
        return Normalizer.normalize(token, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
    }

    public static String buildIndexedPrefix(String token, int maxPrefixLength) {
        String normalizedToken = normalizeToken(token);
	    if (maxPrefixLength <= 0) {
		    return "";
	    }
        if (normalizedToken.codePointCount(0, normalizedToken.length()) > maxPrefixLength) {
	        return substringByCodePoints(normalizedToken, maxPrefixLength);
        }
        return normalizedToken;
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

    private static final int MARKER_LCP_LENGTH = SUFFIX_DICT_MARKER_MAX - SUFFIX_DICT_MARKER_BASE;
    public record SuffixEntry(String resolvedSuffix, String encodedSuffix) {}
    public static final String EMPTY_POI_SUFFIX_DICTIONARY_SENTINEL = "";
    
    public static class SuffixDictionaryData<T> {
        public final List<SuffixEntry> dictionaryEntries = new ArrayList<>();
        public final Map<String, Integer> resolvedSuffixToIndex = new HashMap<>();
        public final Map<T, int[]> bitsets = new LinkedHashMap<>();
    }

    private static boolean startsWithSuffixMarker(String value) {
        if (value.isEmpty()) {
            return false;
        }
        int markerCodePoint = value.codePointAt(0);
        return markerCodePoint == SUFFIX_DICT_MARKER_RAW_ESCAPE
                || (markerCodePoint >= SUFFIX_DICT_MARKER_BASE && markerCodePoint <= SUFFIX_DICT_MARKER_MAX);
    }
    
    private static String encodeSuffix(String suffix) {
        return startsWithSuffixMarker(suffix) ? SUFFIX_DICT_MARKER_RAW_ESCAPE + suffix : suffix;
    }

    private static int countCodePoints(String value) {
        return value.codePointCount(0, value.length());
    }
    
    public static String encodeSuffix(String suffix, String previousSuffix) {
        String encodedRawSuffix = encodeSuffix(suffix);
        if (previousSuffix == null) {
            return encodedRawSuffix;
        }
        int commonPrefixCodePointLength = commonPrefixLength(previousSuffix, suffix);
        if (commonPrefixCodePointLength > MARKER_LCP_LENGTH) {
            return encodedRawSuffix;
        }
        int offset = suffix.offsetByCodePoints(0, commonPrefixCodePointLength);
        String suffixRemainder = suffix.substring(offset);
        String deltaEncodedSuffix = new String(Character.toChars(SUFFIX_DICT_MARKER_BASE + commonPrefixCodePointLength))
                + suffixRemainder;
        return countCodePoints(deltaEncodedSuffix) < countCodePoints(encodedRawSuffix) ? deltaEncodedSuffix : encodedRawSuffix;
    }

    public static <T> SuffixDictionaryData<T> buildSuffixDictionary(String prefix, List<T> objects,
                                                                    Function<T, Collection<String>> tokenSupplier) {
        SuffixDictionaryData<T> data = new SuffixDictionaryData<>();
        TreeSet<String> sortedSuffixes = new TreeSet<>();
        Map<T, Set<String>> suffixesByObject = new LinkedHashMap<>();
        for (T object : objects) {
            Set<String> objectSuffixes = new LinkedHashSet<>();
            suffixesByObject.put(object, objectSuffixes);
            for (String token : tokenSupplier.apply(object)) {
                int suffixOffset = suffixOffsetAfterPrefix(token, prefix);
                String suffix;
                if (suffixOffset < 0) {
                    if (!Objects.equals(token, prefix)) {
                        continue;
                    }
                    suffix = "";
                } else {
                    suffix = Normalizer.normalize(token.substring(suffixOffset), Normalizer.Form.NFC);
                }
                if (suffix == null) {
                    continue;
                }
                objectSuffixes.add(suffix);
                sortedSuffixes.add(suffix);
            }
        }
        String previousSuffix = null;
        for (String suffix : sortedSuffixes) {
            String encodedSuffix = encodeSuffix(suffix, previousSuffix);
            SuffixEntry entry = new SuffixEntry(suffix, encodedSuffix);
            data.resolvedSuffixToIndex.put(entry.resolvedSuffix(), data.dictionaryEntries.size());
            data.dictionaryEntries.add(entry);
            previousSuffix = suffix;
        }
        int dictionaryWordCount = (data.dictionaryEntries.size() + Integer.SIZE - 1) / Integer.SIZE;
        if (dictionaryWordCount == 0) {
            return data;
        }
        for (T object : objects) {
            int[] bitsetWords = new int[dictionaryWordCount];
            Set<String> objectSuffixes = suffixesByObject.get(object);
            if (objectSuffixes != null) {
                for (String suffix : objectSuffixes) {
                    Integer suffixIndex = data.resolvedSuffixToIndex.get(suffix);
                    if (suffixIndex == null) {
                        continue;
                    }
                    bitsetWords[suffixIndex >> 5] |= 1 << (suffixIndex & 31);
                }
            }
            data.bitsets.put(object, bitsetWords);
        }
        return data;
    }
}
