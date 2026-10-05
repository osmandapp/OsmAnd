package net.osmand.shared.compat;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.osmand.binary.RouteDataObject;
import net.osmand.shared.util.KArabicNormalizer;
import net.osmand.shared.util.KSearchAlgorithms;
import net.osmand.util.ArabicNormalizer;
import net.osmand.util.SearchAlgorithms;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * {@link KSearchAlgorithms} is a copy of the part of {@link SearchAlgorithms} that the obf reader
 * needs. Every function it copied is asked the same thing here as the java one, over the road names
 * of the test maps and over names carrying what these functions are there for: apostrophes, quotes,
 * the german sharp s, arabic diacritics and digits, and the delta encoded suffixes of the poi name
 * index.
 */
public class SearchAlgorithmsCompatTest {

	private static final int NAMES = 400;

	private static final String[] TRICKY_NAMES = {
			"Straße des 17. Juni", "Auhofstraße", "Rue de l'Église", "OʼConnell Street",
			"«Кафе» на углу", "Улица «Мира»", "L'Aquila", "dr.luth", "no.", "st.", ".",
			"Ærøskøbing", "Þingvellir", "Œuvre", "Йошкар-Ола", "Ёлкино", "Ελληνικά",
			"شارع الملك فهد", "طريق ٢٥", "شَارِع", "مُحَمَّد", "ﺍﻟﻜﻮﻳﺖ",
			"İstanbul", "Kraków", "Nová Ves", "Đường Nguyễn Huệ", "東京駅", "서울역",
			"East 42-nd Street", "A-1", "B 2", "  double  space  ", "-", "--", "",
			"Route 66 / Шоссе", "Ул. 8 Марта", "3½ Street",
	};

	private static List<String> names;

	@BeforeClass
	public static void corpus() throws IOException {
		Set<String> collected = new LinkedHashSet<>();
		for (String name : TRICKY_NAMES) {
			collected.add(name);
		}
		for (RouteDataObject road : TestObf.roads(2000)) {
			if (road == null || road.names == null) {
				continue;
			}
			for (String name : road.names.valueCollection()) {
				if (name != null && !name.isEmpty()) {
					collected.add(name);
				}
			}
			if (collected.size() >= NAMES) {
				break;
			}
		}
		names = new ArrayList<>(collected);
		assertTrue("names: " + names.size(), names.size() >= NAMES);
	}

	@Test
	public void foldsNamesTheSameWay() {
		for (String name : names) {
			assertEquals("alignChars " + name,
					SearchAlgorithms.alignChars(name), KSearchAlgorithms.INSTANCE.alignChars(name));
			assertEquals("removeApostrophes " + name,
					SearchAlgorithms.removeApostrophes(name), KSearchAlgorithms.INSTANCE.removeApostrophes(name));
			assertEquals("removeQuotes " + name,
					SearchAlgorithms.removeQuotes(name), KSearchAlgorithms.INSTANCE.removeQuotes(name));
			assertEquals("replaceGermanSS " + name,
					SearchAlgorithms.replaceGermanSS(name), KSearchAlgorithms.INSTANCE.replaceGermanSS(name));
			assertEquals("canonicalizePunctuation " + name,
					SearchAlgorithms.canonicalizePunctuation(name), KSearchAlgorithms.INSTANCE.canonicalizePunctuation(name));
			assertEquals("normalizeToken " + name,
					SearchAlgorithms.normalizeToken(name), KSearchAlgorithms.INSTANCE.normalizeToken(name));
		}
	}

	@Test
	public void splitsNamesTheSameWay() {
		for (String name : names) {
			assertEquals("split " + name,
					SearchAlgorithms.split(name), KSearchAlgorithms.INSTANCE.split(name));
			assertEquals("splitByWordsLowercase " + name,
					SearchAlgorithms.splitByWordsLowercase(name), KSearchAlgorithms.INSTANCE.splitByWordsLowercase(name));
			for (boolean unique : new boolean[] {false, true}) {
				assertEquals("splitAndNormalize " + unique + " " + name,
						SearchAlgorithms.splitAndNormalize(name, unique),
						KSearchAlgorithms.INSTANCE.splitAndNormalize(name, unique));
				List<String> javaOriginal = new ArrayList<>();
				List<String> copyOriginal = new ArrayList<>();
				assertEquals("splitAndNormalize with original " + unique + " " + name,
						SearchAlgorithms.splitAndNormalize(name, javaOriginal, unique),
						KSearchAlgorithms.INSTANCE.splitAndNormalize(name, copyOriginal, unique));
				assertEquals("original tokens " + unique + " " + name, javaOriginal, copyOriginal);
			}
		}
	}

	@Test
	public void normalizesArabicTheSameWay() {
		for (String name : names) {
			assertEquals("isSpecialArabic " + name,
					ArabicNormalizer.isSpecialArabic(name), KArabicNormalizer.INSTANCE.isSpecialArabic(name));
			assertEquals("normalize " + name,
					ArabicNormalizer.normalize(name), KArabicNormalizer.INSTANCE.normalize(name));
		}
	}

	/**
	 * Suffix dictionary entries as the writer produces them: each one encoded against the previous,
	 * so that the delta entries, the ones that reuse a prefix, are exercised and not only the raw
	 * ones.
	 */
	@Test
	public void decodesSuffixDictionaryEntriesTheSameWay() {
		int deltaEntries = 0;
		String previous = null;
		for (String name : names) {
			String suffix = SearchAlgorithms.normalizeToken(name);
			String encoded = SearchAlgorithms.nameIndexEncodeSuffix(suffix, previous);
			if (!encoded.equals(suffix)) {
				deltaEntries++;
			}
			assertEquals("decode '" + suffix + "' after '" + previous + "'",
					SearchAlgorithms.nameIndexDecodeDictionarySuffix(previous, encoded),
					KSearchAlgorithms.INSTANCE.nameIndexDecodeDictionarySuffix(previous, encoded));
			previous = suffix;
		}
		assertEquals("", SearchAlgorithms.nameIndexDecodeDictionarySuffix(previous, ""));
		assertEquals("", KSearchAlgorithms.INSTANCE.nameIndexDecodeDictionarySuffix(previous, ""));
		assertTrue("delta encoded entries: " + deltaEntries, deltaEntries > 0);
	}

	@Test
	public void encodesAndDecodesNameAtomBoxesTheSameWay() {
		Random random = new Random(11);
		for (int i = 0; i < 2000; i++) {
			int zoom = 8 + random.nextInt(9);
			int left = random.nextInt(Integer.MAX_VALUE);
			int top = random.nextInt(Integer.MAX_VALUE);
			int[] bbox31 = {
					left, top,
					left + random.nextInt(1 << 20), top + random.nextInt(1 << 20)
			};
			int[] javaEncoded = SearchAlgorithms.encodeBboxForNameAtoms(zoom, bbox31);
			int[] copyEncoded = KSearchAlgorithms.INSTANCE.encodeBboxForNameAtoms(zoom, bbox31);
			assertArrayEquals("encode at zoom " + zoom, javaEncoded, copyEncoded);

			int x16 = bbox31[0] >> 15;
			int y16 = bbox31[1] >> 15;
			assertArrayEquals("decode at zoom " + zoom,
					SearchAlgorithms.decodeBboxForNameAtoms(javaEncoded, x16, y16, 31),
					KSearchAlgorithms.INSTANCE.decodeBboxForNameAtoms(copyEncoded, x16, y16, 31));
		}
		assertEquals(null, KSearchAlgorithms.INSTANCE.decodeBboxForNameAtoms(new int[] {15, 1, 1, 1}, 0, 0, 31));
	}
}
