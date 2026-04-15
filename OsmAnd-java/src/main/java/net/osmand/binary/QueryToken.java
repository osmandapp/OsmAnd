package net.osmand.binary;

import gnu.trove.list.array.TIntArrayList;
import net.osmand.Collator;
import net.osmand.CollatorStringMatcher;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QueryToken {
    public record TokenPrefix(String key, TIntArrayList offsets) {}

    final String query;
    final List<TokenPrefix> prefixes;
    final Collator collator;
    final CollatorStringMatcher.StringMatcherMode matcherMode;

    QueryToken(String query, Collator collator, CollatorStringMatcher.StringMatcherMode matcherMode, List<TokenPrefix> prefixes) {
        this.query = query;
        this.collator = collator;
        this.matcherMode = matcherMode;
        this.prefixes = filter(prefixes);
    }

    private List<TokenPrefix> filter(List<TokenPrefix> prefixes) {
        if (prefixes == null || prefixes.isEmpty()) {
            return Collections.emptyList();
        }
        List<QueryToken.TokenPrefix> sortedPrefixes = new ArrayList<>(prefixes);
        sortedPrefixes.sort((left, right) -> {
            int lengthCompare = Integer.compare(right.key.length(), left.key().length());
            if (lengthCompare != 0) {
                return lengthCompare;
            }
            return left.key().compareTo(right.key());
        });

        List<TokenPrefix> strongestPrefixes = new ArrayList<>();
        for (TokenPrefix candidate : sortedPrefixes) {
            boolean matchesQuery = !Algorithms.isEmpty(query) && candidate.key() != null
                    && CollatorStringMatcher.cmatches(collator, candidate.key(), query, CollatorStringMatcher.StringMatcherMode.CHECK_STARTS_FROM_SPACE);
            TokenPrefix candidatePrefix = new TokenPrefix(candidate.key(), candidate.offsets());
            if (matchesQuery) {
                strongestPrefixes.add(candidatePrefix);
                continue;
            }
            boolean dominated = false;
            for (TokenPrefix strongestPrefix : strongestPrefixes) {
                String strongestKey = strongestPrefix.key();
                if (strongestKey == null) {
                    continue;
                }
                boolean strongestMatchesQuery = !Algorithms.isEmpty(query)
                        && CollatorStringMatcher.cmatches(collator, query, strongestKey, CollatorStringMatcher.StringMatcherMode.CHECK_STARTS_FROM_SPACE);
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
