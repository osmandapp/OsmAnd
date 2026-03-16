package net.osmand.binary;

import net.osmand.CollatorStringMatcher;
import net.osmand.PlatformUtil;
import net.osmand.util.Algorithms;
import net.sf.junidecode.Junidecode;

import java.util.*;
import java.util.concurrent.atomic.LongAdder;

import org.apache.commons.logging.Log;

public final class BloomFilter {
	
	public static final int VERSION = 0; // with 2026-04-01 version will be 1
	
	private static final int BOX_BITS = 512;
	private static final int DEFAULT_HASHES = 5;
	private static final int BOX_SIZE = BOX_BITS / Byte.SIZE;
	
	private static final Log log = PlatformUtil.getLog(BloomFilter.class);

	private final LongAdder writeBoxBitAcc = new LongAdder();
	private final LongAdder writeBoxAcc = new LongAdder(), writeBoxCount = new LongAdder();
	private final LongAdder skipBoxAcc = new LongAdder(), readBoxCount = new LongAdder();
	
	private static BloomFilter INSTANCE = new BloomFilter();

	private BloomFilter() {
	}

	public static BloomFilter getInstance() {
		return INSTANCE;
	}
	
	
	public void logInfo() {
		log.info("Avg box's tokens: " + writeBoxAcc.sum() + "/" + writeBoxCount.sum());
		log.info("Avg bloom bits: " + writeBoxBitAcc.sum() + "/" + writeBoxCount.sum());
	}

	private Set<String> extendTokens(Collection<String> tokens) {
		Set<String> extendedTokens = new TreeSet<>();
		for (String token : tokens) {
			if (!Algorithms.isEmpty(token)) {
				extendedTokens.add(token);
				for (int endIndex = 1; endIndex <= token.length(); endIndex++) {
					extendedTokens.add(token.substring(0, endIndex));
				}
				String transliteratedToken = Junidecode.unidecode(token);
				if (!Algorithms.isEmpty(transliteratedToken) && !token.equals(transliteratedToken)) {
					extendedTokens.add(transliteratedToken);
					for (int endIndex = 1; endIndex <= transliteratedToken.length(); endIndex++) {
						extendedTokens.add(transliteratedToken.substring(0, endIndex));
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

		byte[] bloom = new byte[BOX_SIZE];
		Collection<String> extTokens = extendTokens(tokens);
		for (String token : extTokens) {
			addToken(bloom, token);
		}
		writeBoxAcc.add(extTokens.size());
		writeBoxBitAcc.add(bitCount(bloom));
		writeBoxCount.increment();

		return bloom;
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
		skipBoxAcc.increment();
		return false;
	}

	public static void resetStats() {
		BloomFilter instance = getInstance();
		instance.readBoxCount.reset();
		instance.skipBoxAcc.reset();
		instance.writeBoxAcc.reset();
		instance.writeBoxCount.reset();
		instance.writeBoxBitAcc.reset();
	}
}
