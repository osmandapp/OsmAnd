package net.osmand.binary;

import net.osmand.util.Algorithms;

import java.util.Locale;
import java.util.Set;

public final class BloomFilter {
	private static final int DEFAULT_BITS = 512;
	private static final int DEFAULT_HASHES = 5;
	private static final int DEFAULT_SIZE = DEFAULT_BITS / Byte.SIZE;

	private BloomFilter() {
	}

	public static byte[] build(Set<String> tokens) {
		if (tokens == null || tokens.isEmpty()) {
			return null;
		}
		byte[] bloom = new byte[DEFAULT_SIZE];
		for (String token : tokens) {
			addToken(bloom, token);
		}
		return bloom;
	}

	private static void addToken(byte[] bloom, String token) {
		if (bloom == null || bloom.length == 0 || Algorithms.isEmpty(token)) {
			return;
		}
		String normalizedToken = token.toLowerCase(Locale.ROOT);
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
		String normalizedToken = token.toLowerCase(Locale.ROOT);
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
}
