package net.osmand.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

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

    private static int suffixOffsetAfterPrefix(String token, String prefix) {
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
    
    private static String normalizeToken(String token) {
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
    
    public static String nameIndexPreparePrefix(String token, int maxPrefixLength) {
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
    public record SuffixEntry(String resolvedSuffix, String encodedSuffix) {}
    public static final String EMPTY_SUFFIX_DICTIONARY_SENTINEL = "\uE100";
    
    public static class SuffixDictionary<T> {
        public final List<SuffixEntry> dictionaryEntries = new ArrayList<>();
        public final Map<String, Integer> resolvedSuffixToIndex = new HashMap<>();
        public final Map<T, int[]> bitsets = new LinkedHashMap<>();
    }

    public static class CompactSuffixes {
        public final List<Integer> suffixesBitsetIndex = new ArrayList<>();
        public String extraSuffix;
        public int nonCommonWords;
    }

    public static class CommonIndexedTokens {
        public final List<String> values = new ArrayList<>();
        public final List<Integer> matched = new ArrayList<>();
        public final List<Integer> nonindexed = new ArrayList<>();
        public final Map<String, Integer> tokenToIndex = new HashMap<>();
    }

    public static class CompactSuffixDictionary<T> {
        public static final int MAX_DICTIONARY_SIZE = 128;
        public final List<SuffixEntry> dictionaryEntries = new ArrayList<>();
        public final Map<String, Integer> resolvedSuffixToIndex = new HashMap<>();
        public final Map<T, CompactSuffixes> suffixes = new LinkedHashMap<>();
    }

    private static boolean startsWithSuffixMarker(String value) {
        if (value.isEmpty()) {
            return false;
        }
        int markerCodePoint = value.codePointAt(0);
        return markerCodePoint == SUFFIX_DICT_MARKER_RAW_ESCAPE
                || (markerCodePoint >= SUFFIX_DICT_MARKER_BASE && markerCodePoint <= SUFFIX_DICT_MARKER_MAX);
    }
    
    private static String nameIndexEncodeSuffix(String suffix) {
        return startsWithSuffixMarker(suffix) ? SUFFIX_DICT_MARKER_RAW_ESCAPE + suffix : suffix;
    }

    private static int countCodePoints(String value) {
        return value.codePointCount(0, value.length());
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

    /**
     * Collects unique suffixes for the prefix, stores them once in sorted encoded form, and builds per-object bitsets.
     */
    public static <T> SuffixDictionary<T> nameIndexBuildSuffixDictionary(String prefix, List<T> objects,
                                                                         Function<T, Collection<String>> tokenSupplier) {
        SuffixDictionary<T> data = new SuffixDictionary<>();
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
            String encodedSuffix = nameIndexEncodeSuffix(suffix, previousSuffix);
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

    public static class CombinedSuffixDictionary<T> {
        public final List<SuffixEntry> dictionaryEntries = new ArrayList<>();
        public final List<Integer> commonDictionaryEntries = new ArrayList<>();
        public final Map<String, Integer> resolvedSuffixToIndex = new HashMap<>();
        public final Map<T, int[]> bitsets = new LinkedHashMap<>();
        public final Map<T, CompactSuffixes> compactSuffixes = new LinkedHashMap<>();
    }

    private record CommonSuffixCandidate(String fullToken) {}

    public static <T> CommonIndexedTokens nameIndexBuildCommonIndexedTokens(Map<String, ? extends Collection<T>> objectsByPrefix,
            Function<T, Collection<String>> partialTokenSupplier, Function<T, Collection<String>> separatedTokenSupplier) {
        return nameIndexBuildCommonIndexedTokens(objectsByPrefix,
                (prefix, object) -> partialTokenSupplier.apply(object),
                (prefix, object) -> separatedTokenSupplier.apply(object));
    }

    public static <T> CommonIndexedTokens nameIndexBuildCommonIndexedTokens(Map<String, ? extends Collection<T>> objectsByPrefix,
            BiFunction<String, T, Collection<String>> partialTokenSupplier,
            BiFunction<String, T, Collection<String>> separatedTokenSupplier) {
        Map<String, Integer> matched = new HashMap<>();
        for (Map.Entry<String, ? extends Collection<T>> entry : objectsByPrefix.entrySet()) {
            String prefix = entry.getKey();
            for (T object : entry.getValue()) {
                Set<String> objectCommonTokens = new LinkedHashSet<>();
                for (CommonSuffixCandidate candidate : collectCommonSuffixCandidates(prefix, object,
                        partialTokenSupplier, separatedTokenSupplier)) {
                    objectCommonTokens.add(candidate.fullToken());
                }
                for (String token : objectCommonTokens) {
                    matched.merge(token, 1, Integer::sum);
                }
            }
        }
        List<String> values = new ArrayList<>(matched.keySet());
        values.sort(SearchAlgorithms::compareCommonIndexedTokens);
        CommonIndexedTokens data = new CommonIndexedTokens();
        for (String value : values) {
            data.tokenToIndex.put(value, data.values.size());
            data.values.add(value);
            data.matched.add(matched.get(value));
            data.nonindexed.add(0);
        }
        return data;
    }

    private static int compareCommonIndexedTokens(String left, String right) {
        int leftCommon = CommonWords.getCommon(left);
        int rightCommon = CommonWords.getCommon(right);
        boolean leftIsCommon = leftCommon != -1;
        boolean rightIsCommon = rightCommon != -1;
        if (leftIsCommon != rightIsCommon) {
            return leftIsCommon ? -1 : 1;
        }
        if (leftIsCommon && leftCommon != rightCommon) {
            return Integer.compare(leftCommon, rightCommon);
        }
        int leftFrequent = CommonWords.getFrequentlyUsed(left);
        int rightFrequent = CommonWords.getFrequentlyUsed(right);
        if (leftFrequent != rightFrequent) {
            return Integer.compare(leftFrequent, rightFrequent);
        }
        return left.compareTo(right);
    }

    private static boolean isCommonIndexedToken(String token) {
        return token != null && !isPureDecimalInteger(token)
                && (CommonWords.getCommon(token) != -1 || CommonWords.getFrequentlyUsed(token) != -1);
    }

    private static <T> List<CommonSuffixCandidate> collectCommonSuffixCandidates(String prefix, T object,
            BiFunction<String, T, Collection<String>> partialTokenSupplier,
            BiFunction<String, T, Collection<String>> separatedTokenSupplier) {
        List<CommonSuffixCandidate> candidates = new ArrayList<>();
        for (String token : partialTokenSupplier.apply(prefix, object)) {
            int suffixOffset = suffixOffsetAfterPrefix(token, prefix);
            String suffix = null;
            String fullToken = null;
            if (suffixOffset < 0) {
                if (Objects.equals(token, prefix)) {
                    suffix = "";
                    fullToken = prefix;
                }
            } else {
                suffix = Normalizer.normalize(token.substring(suffixOffset), Normalizer.Form.NFC);
                fullToken = prefix + suffix;
            }
            if (isCommonIndexedToken(fullToken)) {
                candidates.add(new CommonSuffixCandidate(fullToken));
            }
        }
        for (String token : separatedTokenSupplier.apply(prefix, object)) {
            if (Objects.equals(token, prefix) || Algorithms.isEmpty(token) || encodePureDecimalSuffix(token) != null) {
                continue;
            }
            if (isCommonIndexedToken(token)) {
                candidates.add(new CommonSuffixCandidate(token));
            }
        }
        return candidates;
    }

    /**
     * Builds one compact suffix dictionary for the new name-index structure.
     * Partial token remainders are stored as-is; separated word-boundary suffixes are stored with a single leading
     * space marker. The dictionary is capped at {@link CompactSuffixDictionary#MAX_DICTIONARY_SIZE}; over-cap
     * suffixes spill into {@code extraSuffix}. Pure decimal separated suffixes use odd inline values and do not
     * consume dictionary slots.
     */
    public static <T> CombinedSuffixDictionary<T> nameIndexBuildCombinedSuffixDictionary(String prefix, List<T> objects,
            Function<T, Collection<String>> partialTokenSupplier, Function<T, Collection<String>> separatedTokenSupplier) {
        return nameIndexBuildCombinedSuffixDictionary(prefix, objects, partialTokenSupplier, separatedTokenSupplier, null);
    }

    public static <T> CombinedSuffixDictionary<T> nameIndexBuildCombinedSuffixDictionary(String prefix, List<T> objects,
            Function<T, Collection<String>> partialTokenSupplier, Function<T, Collection<String>> separatedTokenSupplier,
            CommonIndexedTokens commonTokens) {
        CombinedSuffixDictionary<T> data = new CombinedSuffixDictionary<>();
        Map<T, Set<String>> suffixesByObject = new LinkedHashMap<>();
        Map<T, Set<Integer>> commonRefsByObject = new LinkedHashMap<>();
        Map<String, Integer> suffixFrequency = new HashMap<>();
        Map<Integer, Integer> commonRefToIndex = new LinkedHashMap<>();

        for (T object : objects) {
            Set<String> objectSuffixes = new LinkedHashSet<>();
            Set<Integer> objectCommonRefs = new LinkedHashSet<>();
            Set<Integer> objectNonindexedCommonRefs = new LinkedHashSet<>();
            suffixesByObject.put(object, objectSuffixes);
            commonRefsByObject.put(object, objectCommonRefs);
            for (String token : partialTokenSupplier.apply(object)) {
                int suffixOffset = suffixOffsetAfterPrefix(token, prefix);
                String suffix = null;
                String fullToken = null;
                if (suffixOffset < 0) {
                    if (Objects.equals(token, prefix)) {
                        suffix = "";
                        fullToken = prefix;
                    }
                } else {
                    suffix = Normalizer.normalize(token.substring(suffixOffset), Normalizer.Form.NFC);
                    fullToken = prefix + suffix;
                }
                if (suffix != null) {
                    // suffixesCommonDictionary stores separated suffix-token refs only; partial/full-token
                    // suffixes stay in the local dictionary or extraSuffix even when their full token is common.
                    objectSuffixes.add(suffix);
                    Integer commonIndex = commonTokens == null ? null : commonTokens.tokenToIndex.get(fullToken);
                    if (commonIndex != null) {
                        objectNonindexedCommonRefs.add(commonIndex);
                    }
                }
            }
            for (String token : separatedTokenSupplier.apply(object)) {
                if (Objects.equals(token, prefix) || Algorithms.isEmpty(token) || encodePureDecimalSuffix(token) != null) {
                    continue;
                }
                Integer commonIndex = commonTokens == null ? null : commonTokens.tokenToIndex.get(token);
                if (commonIndex == null) {
                    objectSuffixes.add(" " + token);
                } else {
                    objectCommonRefs.add(commonIndex);
                    objectNonindexedCommonRefs.remove(commonIndex);
                }
            }
            if (commonTokens != null) {
                for (int commonIndex : objectNonindexedCommonRefs) {
                    commonTokens.nonindexed.set(commonIndex, commonTokens.nonindexed.get(commonIndex) + 1);
                }
            }
            for (String suffix : objectSuffixes) {
                suffixFrequency.merge(suffix, 1, Integer::sum);
            }
            for (int commonRef : objectCommonRefs) {
                commonRefToIndex.computeIfAbsent(commonRef, ignored -> commonRefToIndex.size());
            }
        }
        List<String> rankedSuffixes = new ArrayList<>(suffixFrequency.keySet());
        rankedSuffixes.sort(Comparator
                .comparingInt((String suffix) -> suffixFrequency.get(suffix)).reversed()
                .thenComparing(Comparator.naturalOrder()));
        if (rankedSuffixes.size() > CompactSuffixDictionary.MAX_DICTIONARY_SIZE) {
            rankedSuffixes = rankedSuffixes.subList(0, CompactSuffixDictionary.MAX_DICTIONARY_SIZE);
        }
        Set<String> dictionarySuffixSet = new HashSet<>(rankedSuffixes);

        String previousSuffix = null;
        for (String suffix : rankedSuffixes) {
            String encodedSuffix = nameIndexEncodeSuffix(suffix, previousSuffix);
            SuffixEntry entry = new SuffixEntry(suffix, encodedSuffix);
            data.resolvedSuffixToIndex.put(entry.resolvedSuffix(), data.dictionaryEntries.size());
            data.dictionaryEntries.add(entry);
            previousSuffix = suffix;
        }
        data.commonDictionaryEntries.addAll(commonRefToIndex.keySet());

        for (T object : objects) {
            CompactSuffixes objectSuffixes = new CompactSuffixes();
            List<String> extraSuffixes = new ArrayList<>();
            for (String suffix : suffixesByObject.getOrDefault(object, Collections.emptySet())) {
                Integer suffixIndex = dictionarySuffixSet.contains(suffix) ? data.resolvedSuffixToIndex.get(suffix) : null;
                if (suffixIndex != null) {
                    objectSuffixes.suffixesBitsetIndex.add(suffixIndex << 1);
                    // Empty suffix only confirms that prefix itself is a complete token.
                    if (!suffix.isEmpty()) {
                        objectSuffixes.nonCommonWords++;
                    }
                } else if (!suffix.isEmpty()) {
                    // Empty suffix cannot be represented in space-delimited extraSuffix.
                    extraSuffixes.add(suffix);
                    objectSuffixes.nonCommonWords++;
                }
            }
            for (int commonRef : commonRefsByObject.getOrDefault(object, Collections.emptySet())) {
                Integer commonDictionaryIndex = commonRefToIndex.get(commonRef);
                if (commonDictionaryIndex != null) {
                    objectSuffixes.suffixesBitsetIndex.add((data.dictionaryEntries.size() + commonDictionaryIndex) << 1);
                }
            }
            for (String token : new LinkedHashSet<>(separatedTokenSupplier.apply(object))) {
                if (!Objects.equals(token, prefix)) {
                    Integer encodedNumber = encodePureDecimalSuffix(token);
                    if (encodedNumber != null) {
                        objectSuffixes.suffixesBitsetIndex.add(encodedNumber);
                        objectSuffixes.nonCommonWords++;
                    }
                }
            }
            Collections.sort(objectSuffixes.suffixesBitsetIndex);
            Collections.sort(extraSuffixes);
            if (!extraSuffixes.isEmpty()) {
                objectSuffixes.extraSuffix = String.join(" ", extraSuffixes);
            }
            data.compactSuffixes.put(object, objectSuffixes);
        }
        return data;
    }

    public static boolean isPureDecimalInteger(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        for (int i = 0; i < token.length(); i++) {
            if (!Character.isDigit(token.charAt(i))) {
                return false;
            }
        }
        return token.length() == 1 || token.charAt(0) != '0';
    }

    private static Integer encodePureDecimalSuffix(String token) {
        if (!isPureDecimalInteger(token)) {
            return null;
        }
        try {
            long value = Long.parseLong(token);
            if (value > 0x7fffffffL) {
                return null;
            }
            return (int) ((value << 1) | 1);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Cross-category suffix policy for compact complex tokens.
     * <p>
     * Current production policy is intentionally strict:
     * - USUAL prefixes include NUMBER tokens only.
     * - FREQUENT prefixes include other FREQUENT tokens, COMMON tokens, and NUMBER tokens.
     * <p>
     * After index-size/search-quality testing, cross-category suffixes can be enabled here without changing
     * writer call sites:
     * - Set INCLUDE_USUAL_SUFFIXES_FOR_USUAL_PREFIX to true to let USUAL prefixes also match other USUAL tokens.
     * - Set INCLUDE_FREQUENT_SUFFIXES_FOR_USUAL_PREFIX to true to let USUAL prefixes also match FREQUENT tokens.
     * - Set INCLUDE_COMMON_SUFFIXES_FOR_USUAL_PREFIX to true to let USUAL prefixes also match COMMON tokens.
     * - Set INCLUDE_USUAL_SUFFIXES_FOR_FREQUENT_PREFIX to true to let FREQUENT prefixes also match USUAL tokens.
     * - Set INCLUDE_FREQUENT_SUFFIXES_FOR_FREQUENT_PREFIX to false to test FREQUENT prefixes without other FREQUENT tokens.
     * <p>
     * Keep these switches false by default until OBF size, dictionary hit rate, extraSuffix growth, and false-positive
     * search behavior are measured on representative maps. NUMBER suffixes are not controlled by these switches:
     * they remain included for both USUAL and FREQUENT prefixes.
     */
    private static final boolean INCLUDE_USUAL_SUFFIXES_FOR_USUAL_PREFIX = false;
    private static final boolean INCLUDE_FREQUENT_SUFFIXES_FOR_USUAL_PREFIX = true;
    private static final boolean INCLUDE_COMMON_SUFFIXES_FOR_USUAL_PREFIX = true;
    private static final boolean INCLUDE_USUAL_SUFFIXES_FOR_FREQUENT_PREFIX = false;
    private static final boolean INCLUDE_FREQUENT_SUFFIXES_FOR_FREQUENT_PREFIX = true;

    public static Set<String> nameIndexPrepareComplexPrefixes(List<String> tokens, boolean allowNumberPrefixes) {
        List<String> uniqueTokens = new ArrayList<>(new LinkedHashSet<>(tokens));
        List<String> usual = new ArrayList<>();
        List<String> frequent = new ArrayList<>();
        List<String> common = new ArrayList<>();
        List<String> numbers = new ArrayList<>();
        for (String token : uniqueTokens) {
            if (CommonWords.isNumber2Letters(token)) {
                numbers.add(token);
            } else if (CommonWords.getCommon(token) != -1) {
                common.add(token);
            } else if (CommonWords.getFrequentlyUsed(token) != -1) {
                frequent.add(token);
            } else {
                usual.add(token);
            }
        }
        LinkedHashSet<String> prefixes = new LinkedHashSet<>();
        if (!usual.isEmpty()) {
            prefixes.addAll(usual);
            prefixes.addAll(frequent);
        } else if (!frequent.isEmpty()) {
            prefixes.addAll(frequent);
        } else if (!common.isEmpty()) {
            prefixes.addAll(common);
        } else if (allowNumberPrefixes) {
            prefixes.addAll(numbers);
        }
        return prefixes;
    }

    public static List<String> nameIndexPrepareComplexSuffixes(List<String> tokens, String prefix) {
        List<String> uniqueTokens = new ArrayList<>(new LinkedHashSet<>(tokens));
        List<String> usual = new ArrayList<>();
        List<String> frequent = new ArrayList<>();
        List<String> common = new ArrayList<>();
        List<String> numbers = new ArrayList<>();
        for (String token : uniqueTokens) {
            if (CommonWords.isNumber2Letters(token)) {
                numbers.add(token);
            } else if (CommonWords.getCommon(token) != -1) {
                common.add(token);
            } else if (CommonWords.getFrequentlyUsed(token) != -1) {
                frequent.add(token);
            } else {
                usual.add(token);
            }
        }
        List<String> suffixes = new ArrayList<>();
        if (usual.contains(prefix)) {
            if (INCLUDE_USUAL_SUFFIXES_FOR_USUAL_PREFIX) {
                suffixes.addAll(usual);
            }
            if (INCLUDE_FREQUENT_SUFFIXES_FOR_USUAL_PREFIX) {
                suffixes.addAll(frequent);
            }
            if (INCLUDE_COMMON_SUFFIXES_FOR_USUAL_PREFIX) {
                suffixes.addAll(common);
            }
            suffixes.addAll(numbers);
        } else if (frequent.contains(prefix)) {
            if (INCLUDE_USUAL_SUFFIXES_FOR_FREQUENT_PREFIX) {
                suffixes.addAll(usual);
            }
            if (INCLUDE_FREQUENT_SUFFIXES_FOR_FREQUENT_PREFIX) {
                suffixes.addAll(frequent);
            }
            suffixes.addAll(common);
            suffixes.addAll(numbers);
        } else {
            suffixes.addAll(uniqueTokens);
        }
        suffixes.removeIf(token -> Objects.equals(token, prefix));
        return suffixes;
    }

    public static boolean nameIndexIsSingleRawNumberValue(String rawText, List<String> normalizedTokens) {
        if (Algorithms.isEmpty(rawText) || normalizedTokens == null || normalizedTokens.isEmpty()) {
            return false;
        }
        for (int i = 0; i < rawText.length(); i++) {
            if (Character.isWhitespace(rawText.charAt(i))) {
                return false;
            }
        }
        for (String token : normalizedTokens) {
            if (!CommonWords.isNumber2Letters(token)) {
                return false;
            }
        }
        return true;
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
			res[ind + 1] = ((y16 >> (16 - zoom))) - (vls[ind + 3]) << (31 - zoom);
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
