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
            if (suffixDictionary.size() == 1 && suffixDictionary.get(0).isEmpty()) {
                masks.add(1);
                return;
            }
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
                int intWordIndex = index >> 5; // word selection where index >> 5 == index / 32
                while (masks.size() <= intWordIndex) { // each int word in masks list holds 32 suffix flags
                    masks.add(0);
                }
                int bitOffset = index & 31; // selection of bit inside the word where index & 31 == index % 32 and stays in 0..31
                int wordMask = 1 << bitOffset; // building a one-bit mask
                int prev = masks.get(intWordIndex);
                masks.set(intWordIndex, prev | wordMask);
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
