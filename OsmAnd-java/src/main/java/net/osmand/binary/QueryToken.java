package net.osmand.binary;

import gnu.trove.list.array.TIntArrayList;
import net.osmand.Collator;
import net.osmand.CollatorStringMatcher;

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

        SuffixMask(Prefix prefix) {
            this.prefix = prefix;
        }

        void setDictionary(List<String> suffixDictionary) {
            if (prefix.key() == null || suffixDictionary == null) {
                return;
            }
            
            if (masks == null) {
                masks = new TIntArrayList();
            }
            suffixes.put(prefix.key(), new LinkedHashSet<>(suffixDictionary));
            if (query == null) {
                return;
            }
            for (int index = 0; index < suffixDictionary.size(); index++) {
                addSuffix(index, suffixDictionary.get(index));
            }
        }

        private void addSuffix(int index, String suffix) {
            if (suffix == null || index < 0) {
                return;
            }
            String fullKey = prefix.key() + suffix;
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

        if (prefixes == null || prefixes.isEmpty()) {
            this.prefixes = Collections.emptyList();
        } else {
            this.prefixes = new ArrayList<>(prefixes);
            this.prefixes.sort((left, right) -> {
                int lengthCompare = Integer.compare(right.key.length(), left.key().length());
                if (lengthCompare != 0) {
                    return lengthCompare;
                }
                return left.key().compareTo(right.key());
            });
        }
    }
}
