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
        private boolean passThrough;
        int prevMask = 0; // not exactly correct to maintain state here

        SuffixMask(Prefix prefix) {
            this.prefix = prefix;
        }

        void setDictionary(List<String> suffixDictionary) {
            passThrough = suffixDictionary == null;
            if (prefix.key() == null || suffixDictionary == null) {
                return;
            }
            
            if (suffixDictionary.size() == 1 && suffixDictionary.get(0).isEmpty()) {
                passThrough = query != null && CollatorStringMatcher.cmatches(collator, prefix.key(), query, matcherMode);
                return;
            }
            if (masks == null) {
                masks = new TIntArrayList();
            }
            if (query == null) {
                return;
            }
            for (int index = 0; index < suffixDictionary.size(); index++) {
                addSuffix(index, suffixDictionary.get(index));
            }
        }

        boolean shouldPassThrough() {
            return passThrough;
        }
        
        boolean isMatched(int maskIndex, int mask) {
			if (masks == null) {
				return true;
			}
			if (maskIndex == 0) {
				prevMask = 0;
			}
			boolean res = false;
			// use only masks for first and after delimiter
			if (prevMask == 0 && mask % 2 == 0 && masks.contains(mask / 2 - 1)) {
				res = true;
			}
			prevMask = mask;
			return res;
        }

        private void addSuffix(int index, String suffix) {
            if (suffix == null || index < 0) {
                return;
            }
            String fullKey = prefix.key() + suffix;
            if (CollatorStringMatcher.cmatches(collator, fullKey, query, matcherMode)) {
            	masks.add(index);
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

	public boolean matchFullPrefix(String key) {
		return CollatorStringMatcher.cmatches(collator, key, query, matcherMode);
	}
	
}
