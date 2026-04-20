package net.osmand.binary;

import gnu.trove.list.array.TIntArrayList;
import net.osmand.Collator;
import net.osmand.CollatorStringMatcher;
import net.osmand.util.Algorithms;

import java.util.*;

public class QueryToken {
    final String query;
    final List<Prefix> prefixes;
    final Collator collator;
    final CollatorStringMatcher.StringMatcherMode matcherMode;
    final Map<String, Set<String>> suffixes = new HashMap<>();
    
    record Prefix(String key, int offset) {}

    class SuffixMask {
        TIntArrayList masks;
        final Prefix prefix;
        final boolean enabled;

        SuffixMask(Prefix prefix) {
            this.prefix = prefix;
            enabled = query != null && prefix.key() != null && !CollatorStringMatcher.cmatches(collator, prefix.key(), query, matcherMode);
        }

        void setDictionary(List<String> suffixDictionary) {
            if (!enabled || suffixDictionary == null) {
                return;
            }
            if (masks == null) {
                masks = new TIntArrayList();
            }
            
            int index = suffixDictionary.size() - 1;
            if (index < 0) {
                return;
            }
            String entry = suffixDictionary.get(index);
            if (prefix.key() != null) {
                suffixes.computeIfAbsent(prefix.key(), key -> new LinkedHashSet<>()).add(entry);
            }
            String fullKey = prefix.key() + entry;
            if (CollatorStringMatcher.cmatches(collator, fullKey, query, matcherMode)) {
                int wordIndex = index >> 5;
                while (masks.size() <= wordIndex) {
                    masks.add(0);
                }
                masks.set(wordIndex, masks.get(wordIndex) | (1 << (index & 31)));
            }
        }
    }

    QueryToken(String query, Collator collator, CollatorStringMatcher.StringMatcherMode matcherMode, List<Prefix> prefixes) {
        this.query = query;
        this.collator = collator;
        this.matcherMode = matcherMode;
        this.prefixes = filter(prefixes);
    }

    private List<Prefix> filter(List<Prefix> prefixes) {
        if (prefixes == null || prefixes.isEmpty()) {
            return Collections.emptyList();
        }
        List<Prefix> sortedPrefixes = new ArrayList<>(prefixes);
        sortedPrefixes.sort((left, right) -> {
            int lengthCompare = Integer.compare(right.key.length(), left.key().length());
            if (lengthCompare != 0) {
                return lengthCompare;
            }
            return left.key().compareTo(right.key());
        });

        List<Prefix> strongestPrefixes = new ArrayList<>();
        for (Prefix candidate : sortedPrefixes) {
            boolean matchesQuery = !Algorithms.isEmpty(query) && candidate.key() != null
                    && CollatorStringMatcher.cmatches(collator, candidate.key(), query, CollatorStringMatcher.StringMatcherMode.CHECK_ONLY_STARTS_WITH);
            Prefix candidatePrefix = new Prefix(candidate.key(), candidate.offset());
            if (matchesQuery) {
                strongestPrefixes.add(candidatePrefix);
                continue;
            }
            boolean dominated = false;
            for (Prefix strongestPrefix : strongestPrefixes) {
                String strongestKey = strongestPrefix.key();
                if (strongestKey == null) {
                    continue;
                }
                boolean strongestMatchesQuery = !Algorithms.isEmpty(query)
                        && CollatorStringMatcher.cmatches(collator, query, strongestKey, CollatorStringMatcher.StringMatcherMode.CHECK_ONLY_STARTS_WITH);
                if (strongestMatchesQuery && candidate.key() != null && strongestKey.length() > candidate.key().length()
                        && strongestKey.startsWith(candidate.key())) {
                    dominated = true;
                    break;
                }
            }
            if (!dominated) {
                strongestPrefixes.add(candidatePrefix);
            }
        }
        return strongestPrefixes;
    }
}
