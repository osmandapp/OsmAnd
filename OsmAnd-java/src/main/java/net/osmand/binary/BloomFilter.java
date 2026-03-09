package net.osmand.binary;

import net.osmand.CollatorStringMatcher;
import net.osmand.util.Algorithms;
import net.sf.junidecode.Junidecode;

import java.util.*;

public final class BloomFilter {
	private static final int DEFAULT_BITS = 512;
	private static final int DEFAULT_HASHES = 5;
	private static final int DEFAULT_SIZE = DEFAULT_BITS / Byte.SIZE;
	private static final int INT32_BITS = Integer.SIZE;

	private BloomFilter() {
	}

	private static Set<String> extendTokens(Collection<String> tokens) {
		Set<String> extendedTokens = new TreeSet<>();
		for (String token : tokens) {
			if (Algorithms.isEmpty(token)) {
				continue;
			}
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
		return extendedTokens;
	}

	public static int buildInt32(Collection<String> tokens, boolean startsFrom) {
		if (tokens == null || tokens.isEmpty()) {
			return 0;
		}
		int bloom = 0;
		if (startsFrom) {
			for (String token : extendTokens(tokens)) {
				bloom = addToken(bloom, token);
			}
		} else {
			for (String token : tokens) {
				bloom = addToken(bloom, token);
			}
		}
		return bloom;
	}

	public static byte[] build(Collection<String> tokens, boolean startsFrom) {
		if (tokens == null || tokens.isEmpty()) {
			return null;
		}

		byte[] bloom = new byte[DEFAULT_SIZE];
		for (String token : (startsFrom ? extendTokens(tokens) : tokens)) {
			addToken(bloom, token);
		}
		return bloom;
	}

	private static int addToken(int bloom, String token) {
		if (Algorithms.isEmpty(token)) {
			return bloom;
		}
		String normalizedToken = normalize(token);
		if (Algorithms.isEmpty(normalizedToken)) {
			return bloom;
		}
		return addNormToken(bloom, normalizedToken);
	}

	private static int addNormToken(int bloom, String normalizedToken) {
		int h1 = normalizedToken.hashCode();
		int h2 = Integer.rotateLeft(h1, 16) ^ 0x9E3779B9;
		if (h2 == 0) {
			h2 = 0x85ebca6b;
		}
		for (int i = 0; i < DEFAULT_HASHES; i++) {
			int bitIndex = Math.floorMod(h1 + (i * h2), INT32_BITS);
			bloom |= (1 << bitIndex);
		}
		return bloom;
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
		for (int i = 0; i < DEFAULT_HASHES; i++) {
			int bitIndex = Math.floorMod(h1 + (i * h2), DEFAULT_BITS);
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
		for (int i = 0; i < DEFAULT_HASHES; i++) {
			int bitIndex = Math.floorMod(h1 + (i * h2), DEFAULT_BITS);
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

	public static boolean matches(int bloom, String token) {
		if (bloom == 0 || Algorithms.isEmpty(token)) {
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
		for (int i = 0; i < DEFAULT_HASHES; i++) {
			int bitIndex = Math.floorMod(h1 + (i * h2), INT32_BITS);
			int bitMask = 1 << bitIndex;
			if ((bloom & bitMask) == 0) {
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
		for (String queryToken : queryTokens) {
			if (BloomFilter.matches(bloomBytes, queryToken)) {
				return true;
			}
		}
		return false;
	}

	public static boolean matches(int atomBloom, Collection<String> queryTokens) {
		if (queryTokens == null || queryTokens.isEmpty()) {
			return true;
		}
		for (String queryToken : queryTokens) {
			if (BloomFilter.matches(atomBloom, queryToken)) {
				return true;
			}
		}
		return false;
	}
}
