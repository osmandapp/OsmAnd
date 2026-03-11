package net.osmand.binary;

import net.osmand.CollatorStringMatcher;
import net.osmand.util.Algorithms;
import net.sf.junidecode.Junidecode;

import java.util.*;
import java.util.concurrent.atomic.LongAdder;

public final class BloomFilter {
	private static final int INDEX_BITS = 512, BOX_BITS = 256;
	private static final int DEFAULT_HASHES = 5;
	private static final int INDEX_SIZE = INDEX_BITS / Byte.SIZE, BOX_SIZE = BOX_BITS / Byte.SIZE;

	public static final LongAdder writeIndexBitAcc = new LongAdder(), writeBoxBitAcc = new LongAdder();
	public static final LongAdder writeBoxAcc = new LongAdder(), writeBoxCount = new LongAdder();
	public static final LongAdder writeIndexAcc = new LongAdder(), writeIndexCount = new LongAdder();

	public static final LongAdder skipIndexAcc = new LongAdder(), readIndexCount = new LongAdder();
	public static final LongAdder skipBoxAcc = new LongAdder(), readBoxCount = new LongAdder();

	private BloomFilter() {
	}

	private static Set<String> extendTokens(Collection<String> tokens) {
		Set<String> extendedTokens = new TreeSet<>();
		for (String token : tokens) {
			if (!Algorithms.isEmpty(token)) {
				extendedTokens.add(token);
				for (int endIndex = 1; endIndex <= token.length(); endIndex++) {
					extendedTokens.add(token.substring(0, endIndex));
				}
				/*
				String transliteratedToken = Junidecode.unidecode(token);
				if (!Algorithms.isEmpty(transliteratedToken) && !token.equals(transliteratedToken)) {
					extendedTokens.add(transliteratedToken);
					for (int endIndex = 1; endIndex <= transliteratedToken.length(); endIndex++) {
						extendedTokens.add(transliteratedToken.substring(0, endIndex));
					}
				}*/
			}
		}
		return extendedTokens;
	}

	public static byte[] build(Collection<String> tokens, boolean isIndex) {
		if (tokens == null || tokens.isEmpty()) {
			return null;
		}

		byte[] bloom = new byte[isIndex ? INDEX_SIZE : BOX_SIZE];
		Collection<String> extTokens = extendTokens(tokens);
		for (String token : extTokens) {
			addToken(bloom, token);
		}
		if (isIndex) {
			writeIndexAcc.add(extTokens.size());
			writeIndexBitAcc.add(bitCount(bloom));
			writeIndexCount.increment();
		} else {
			writeBoxAcc.add(extTokens.size());
			writeBoxBitAcc.add(bitCount(bloom));
			writeBoxCount.increment();
		}
		return bloom;
	}

	private static long bitCount(byte[] bloom) {
		if (bloom == null) {
			return 0;
		}
		long sum = 0;
		for (byte b : bloom) {
			sum += Integer.bitCount(b & 0xFF);
		}
		return sum;
	}

	private static void addToken(byte[] bloom, String token) {
		if (bloom == null || bloom.length == 0 || Algorithms.isEmpty(token)) {
			return;
		}
		String normalizedToken = normalize(token);
		if (Algorithms.isEmpty(normalizedToken)) {
			return;
		}
		addNormToken(bloom, normalizedToken);
	}

	private static void addNormToken(byte[] bloom, String normalizedToken) {
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

	public static boolean matches(byte[] bloom, String token) {
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

	private static String normalize(String token) {
		if (Algorithms.isEmpty(token)) {
			return "";
		}
		String normalizedToken = CollatorStringMatcher.alignChars(token);
		return normalizedToken.toLowerCase(Locale.ROOT);
	}

	public static boolean matches(byte[] bloomBytes, Collection<String> queryTokens) {
		if (queryTokens == null || queryTokens.isEmpty()) {
			return true;
		}

		boolean isIndex = bloomBytes.length == INDEX_SIZE;
		(isIndex ? readIndexCount : readBoxCount).increment();
		for (String queryToken : queryTokens) {
			if (BloomFilter.matches(bloomBytes, queryToken)) {
				return true;
			}
		}
		(isIndex ? skipIndexAcc: skipBoxAcc).increment();
		return false;
	}

	public static void reset() {
		readBoxCount.reset();
		readIndexCount.reset();
		skipBoxAcc.reset();
		skipIndexAcc.reset();

		writeIndexAcc.reset();
		writeIndexCount.reset();
		writeBoxAcc.reset();
		writeBoxCount.reset();
		writeBoxBitAcc.reset();
		writeIndexBitAcc.reset();
	}
}
