package net.osmand.binary;

import net.osmand.CollatorStringMatcher;
import net.osmand.PlatformUtil;
import net.osmand.util.Algorithms;
import net.sf.junidecode.Junidecode;

import java.util.*;
import java.util.concurrent.atomic.LongAdder;

import org.apache.commons.logging.Log;

public final class BloomFilter {
	
	
	public static final int VERSION = 1; // with 2026-04-01 version will be 1
	public static final boolean PUBLISH = VERSION > 0;
	
	public static final int BLOOM_BITS = 512;
	private static final int DEFAULT_HASHES = 5;
	private static final int BLOOM_SIZE = BLOOM_BITS / Byte.SIZE;
	public static final int MAX_SATURATION_BITS = 384;
	public static final int MIN_BLOOM_CONTINUATION_PREFIX_LENGTH = 1; // Min suffix length to be included in bloomIndex.

	private static final LongAdder writeTokensCount = new LongAdder(), writeBoxCount = new LongAdder();
	private static final LongAdder skipCount = new LongAdder(), readBoxCount = new LongAdder(), falsePositive = new LongAdder();
	
	private static final BloomFilter INSTANCE = new BloomFilter();

	private BloomFilter() {
	}

	public static BloomFilter getInstance() {
		return INSTANCE;
	}
	
	public String logSkipRatio() {
		return String.format("True ratio: %d / %d = %.2f, False ratio: %d / %d = %.2f", skipCount.sum(), readBoxCount.sum(),
				100 * skipCount.sum() / (double) readBoxCount.sum(),
				falsePositive.sum(), readBoxCount.sum(),
				100 * falsePositive.sum() / (double) readBoxCount.sum());
	}

	public static void incFalsePositive() {
		falsePositive.increment();
	}

	private Set<String> extendTokens(Collection<String> tokens, boolean transliterate) {
		Set<String> extendedTokens = new TreeSet<>(tokens);
		for (String token : tokens) {
			if (Algorithms.isEmpty(token))
				continue;
			for (int i = MIN_BLOOM_CONTINUATION_PREFIX_LENGTH; i < token.length(); i++) {
				extendedTokens.add(token.substring(0, i));
			}
			if (transliterate) {
				String transliteratedToken = Junidecode.unidecode(token);
				if (!Algorithms.isEmpty(transliteratedToken) && !token.equals(transliteratedToken)) {
					extendedTokens.add(transliteratedToken);
					for (int i = MIN_BLOOM_CONTINUATION_PREFIX_LENGTH; i <= transliteratedToken.length(); i++) {
						extendedTokens.add(transliteratedToken.substring(0, i));
					}
				}
			}
		}
		return extendedTokens;
	}

	public byte[] build(Collection<String> tokens) {
		if (tokens == null || tokens.isEmpty()) {
			return null;
		}

		byte[] bloom = new byte[BLOOM_SIZE];
		Collection<String> bloomTokens = extendTokens(tokens, true);
		for (String token : bloomTokens) {
			addToken(bloom, token);
		}
		writeTokensCount.add(bloomTokens.size());
		writeBoxCount.increment();

		return bloom;
	}

	public int countExactBits(Collection<String> tokens) {
		if (tokens == null || tokens.isEmpty()) {
			return 0;
		}
		byte[] bloom = new byte[BLOOM_SIZE];
		for (String token : extendTokens(tokens, true)) {
			addToken(bloom, token);
		}
		return (int) bitCount(bloom);
	}

	private long bitCount(byte[] bloom) {
		if (bloom == null) {
			return 0;
		}
		long sum = 0;
		for (byte b : bloom) {
			sum += Integer.bitCount(b & 0xFF);
		}
		return sum;
	}

	private void addToken(byte[] bloom, String token) {
		if (bloom == null || bloom.length == 0 || Algorithms.isEmpty(token)) {
			return;
		}
		String normalizedToken = normalize(token);
		if (Algorithms.isEmpty(normalizedToken)) {
			return;
		}
		addNormToken(bloom, normalizedToken);
	}

	private void addNormToken(byte[] bloom, String normalizedToken) {
		int h1 = normalizedToken.hashCode();
		int h2 = Integer.rotateLeft(h1, 16) ^ 0x9E3779B9;
		if (h2 == 0) {
			h2 = 0x85ebca6b;
		}
		int byteCount = bloom.length;
		int totalBits = byteCount << 3;
		if (totalBits == 0) {
			return;
		}
		for (int i = 0; i < DEFAULT_HASHES; i++) {
			int bitIndex = Math.floorMod(h1 + (i * h2), totalBits);
			int byteIndex = bitIndex >>> 3;
			if (byteIndex >= byteCount) {
				continue;
			}
			int bitMask = 1 << (bitIndex & 7);
			bloom[byteIndex] = (byte) (bloom[byteIndex] | bitMask);
		}
	}

	private boolean matches(byte[] bloom, String token) {
		if (bloom == null || bloom.length == 0 || Algorithms.isEmpty(token)) {
			return true;
		}
		String normalizedToken = normalize(token);
		if (Algorithms.isEmpty(normalizedToken)) {
			return true;
		}
		int h1 = normalizedToken.hashCode();
		int h2 = Integer.rotateLeft(h1, 16) ^ 0x9E3779B9;
		if (h2 == 0) {
			h2 = 0x85ebca6b;
		}
		int byteCount = bloom.length;
		int totalBits = byteCount << 3;
		if (totalBits == 0) {
			return true;
		}
		for (int i = 0; i < DEFAULT_HASHES; i++) {
			int bitIndex = Math.floorMod(h1 + (i * h2), totalBits);
			int byteIndex = bitIndex >>> 3;
			if (byteIndex >= byteCount) {
				return true;
			}
			int bitMask = 1 << (bitIndex & 7);
			if ((bloom[byteIndex] & bitMask) == 0) {
				return false;
			}
		}
		return true;
	}

	private String normalize(String token) {
		if (Algorithms.isEmpty(token)) {
			return "";
		}
		String normalizedToken = CollatorStringMatcher.alignChars(token);
		return normalizedToken.toLowerCase(Locale.ROOT);
	}

	public boolean matches(byte[] bloomBytes, Collection<String> queryTokens) {
		if (queryTokens == null || queryTokens.isEmpty()) {
			return true;
		}

		readBoxCount.increment();
		for (String queryToken : queryTokens) {
			if (matches(bloomBytes, queryToken)) {
				return true;
			}
		}
		skipCount.increment();
		return false;
	}

	public static void resetStats() {
		readBoxCount.reset();
		falsePositive.reset();
		skipCount.reset();

		writeTokensCount.reset();
		writeBoxCount.reset();
	}

}
