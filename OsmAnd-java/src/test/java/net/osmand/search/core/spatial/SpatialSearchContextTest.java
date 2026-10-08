package net.osmand.search.core.spatial;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import gnu.trove.map.hash.TLongObjectHashMap;
import net.osmand.binary.BinaryMapAddressReaderAdapter.AddressRegion;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.NameIndexReader;
import net.osmand.binary.OsmandOdb.CommonIndexedStats;
import net.osmand.data.City;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialSearchGlobalCache;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings;
import net.osmand.util.SearchAlgorithms;

public class SpatialSearchContextTest {

	@Test
	public void testStreetParentCityReferenceFileWithoutCachedCity() throws IOException {
		City city = new City(City.CityType.CITY);
		city.setName("Kyiv");
		city.setLocation(50.4501, 30.5234);
		Street street = new Street(city);
		street.setName("Khreshchatyk");
		AddressRegion addressRegion = new AddressRegion();
		long cityOffset = 100L;
		long streetOffset = 200L;

		// Skip OBF initialization and replace only the object reads.
		BinaryMapIndexReader reader = new BinaryMapIndexReader(null, new File("spatial-search-test.obf"), false) {
			@Override
			public City readCityObject(AddressRegion region, long offset) {
				Assert.assertSame(addressRegion, region);
				Assert.assertTrue(offset == cityOffset);
				return city;
			}

			@Override
			public Street readStreetObject(AddressRegion region, City parentCity, long offset) {
				Assert.assertSame(addressRegion, region);
				Assert.assertSame(city, parentCity);
				Assert.assertTrue(offset == streetOffset);
				return street;
			}
		};
		try {
			reader.getAddressIndexes().add(addressRegion);
			SpatialSearchContext context = new SpatialSearchContext(SpatialTextSearchSettings.defaultSettings(),
					Collections.singletonList(reader), null, city.getLocation());
			context.initFiles(new SpatialSearchGlobalCache());
			TLongObjectHashMap<MapObject> cache = new TLongObjectHashMap<>();
			Assert.assertNull(city.getReferenceFile());

			// Address IDs reserve the lower 14 bits for the index number (zero here).
			long cityId = cityOffset << 14;
			long streetId = streetOffset << 14;
			Assert.assertSame(street, context.readAddrObject(streetId, cityId, cache));
			Assert.assertSame("A parent city read with its street must retain the source reader",
					reader, street.getCity().getReferenceFile());
		} finally {
			reader.close();
		}
	}

	/**
	 * Common words of the address index as the 2026-09 maps store them: word, frequency, non-indexed.
	 * A kind word ranks a house whose street the query never named last, so a street name taken for
	 * one sends "707 John Street Elmira" below "601 Johnson Street" and "9 Lange Straße Stuttgart"
	 * below "9 Stuttgarter Straße".
	 */
	@Test
	public void testStreetNameWordsAreNotKindWords() {
		SpatialSearchContext context = new SpatialSearchContext(SpatialTextSearchSettings.defaultSettings(),
				Collections.emptyList(), null, new LatLon(0, 0));
		NameIndexReader pennsylvania = addressIndex(
				"street", 75284, 72691,
				"avenue", 34308, 33753,
				"john", 432, 157,
				"main", 2113, 122);
		NameIndexReader stuttgart = addressIndex(
				"strasse", 15140, 15126,
				"weg", 5465, 5458,
				"lange", 377, 125,
				"hohe", 327, 86);

		Assert.assertTrue("street", context.isKindWord(pennsylvania, "street"));
		Assert.assertTrue("avenue", context.isKindWord(pennsylvania, "avenue"));
		Assert.assertTrue("straße", context.isKindWord(stuttgart, "straße"));
		Assert.assertTrue("weg", context.isKindWord(stuttgart, "weg"));

		Assert.assertFalse("john", context.isKindWord(pennsylvania, "john"));
		Assert.assertFalse("main", context.isKindWord(pennsylvania, "main"));
		Assert.assertFalse("lange", context.isKindWord(stuttgart, "lange"));
		Assert.assertFalse("hohe", context.isKindWord(stuttgart, "hohe"));
	}

	/**
	 * A word of an alternative name ("av" of "North Av") is in the table but never a non-indexed word: it is a kind word
	 * when the one word it stands for is, so "4 avenue 8" ranks a house on North Avenue as low as one on 4th Avenue.
	 */
	@Test
	public void testAbbreviationOfAKindWordIsAKindWord() {
		SpatialSearchContext context = new SpatialSearchContext(SpatialTextSearchSettings.defaultSettings(),
				Collections.emptyList(), null, new LatLon(0, 0));
		NameIndexReader newYork = addressIndex(
				"av", 120, 0,
				"avenue", 34308, 33753,
				"st", 900, 0,
				"street", 75284, 72691);

		Assert.assertFalse("av alone", context.isKindWord(newYork, "av"));
		Assert.assertTrue("av", context.isKindWord(newYork, "av", "en_US", "street"));
		// two meanings (Street, Saint): says nothing about the kind
		Assert.assertFalse("st", context.isKindWord(newYork, "st", "en_US", "street"));
	}

	/**
	 * Names of one atom that an index rule spells the other way are one name: the variant with more query words wins.
	 * Names of other languages ("пошта" of name:uk, "почта" of name:ru) are not.
	 */
	@Test
	public void testNamesSpelledByAnIndexRule() {
		Assert.assertTrue(SpatialSearchToken.spelledByRule("college avenue east", "college ave east", "en"));
		Assert.assertTrue(SpatialSearchToken.spelledByRule("college ave east", "college avenue east", "en"));
		Assert.assertFalse(SpatialSearchToken.spelledByRule("college avenue east", "college road east", "en"));
		Assert.assertFalse(SpatialSearchToken.spelledByRule("нова 5 пошта", "нова 5 почта", "uk_UA"));
		// two alternative names of one name by two rules (Parkway -> pkwy, South -> s): every differing word is paired
		Assert.assertTrue(SpatialSearchToken.spelledByRule("eastern pkwy south sidewalk", "eastern parkway s sidewalk",
				"en_US"));
		Assert.assertFalse(SpatialSearchToken.spelledByRule("eastern pkwy south sidewalk", "eastern parkway s mall",
				"en_US"));
	}

	/**
	 * A word that is no number and a word of a name of the object ("о." of "о. Пасхи") stays that word: a POI ref
	 * guessed from another name of the object ("Пасхи" unglued from "о. Пасхи", no other words) does not replace it.
	 */
	@Test
	public void testNameWordIsNoRefOfAnotherName() {
		SpatialSearchToken.NameIndexAtomXY world = new SpatialSearchToken.NameIndexAtomXY(null, null, null);
		SpatialSearchToken.NameIndexAtom name = new SpatialSearchToken.NameIndexAtom("о.", SpatialSearchToken.POI_TYPE,
				7, 0, null, false, 1, 0, world, 0, -1);
		SpatialSearchToken.NameIndexAtom ref = new SpatialSearchToken.NameIndexAtom("пасхи",
				SpatialSearchToken.POI_REF_TYPE, 7, 0, null, false, 0, 0, world, 0, -1);
		SpatialSearchToken nameFirst = new SpatialSearchToken(2, "о.", "о.", 0);
		nameFirst.addAtom(name);
		nameFirst.addAtom(ref);
		Assert.assertSame(name, nameFirst.getAtomToken(name));
		SpatialSearchToken refFirst = new SpatialSearchToken(2, "о.", "о.", 0);
		refFirst.addAtom(new SpatialSearchToken.NameIndexAtom(ref));
		refFirst.addAtom(name);
		Assert.assertSame(name, refFirst.getAtomToken(name));
	}

	private static NameIndexReader addressIndex(Object... wordFreqNonindexed) {
		CommonIndexedStats.Builder stats = CommonIndexedStats.newBuilder();
		String previous = null;
		for (int i = 0; i < wordFreqNonindexed.length; i += 3) {
			String word = SearchAlgorithms.alignChars((String) wordFreqNonindexed[i]);
			stats.addValue(SearchAlgorithms.nameIndexEncodeSuffix(word, previous));
			stats.addMatched((Integer) wordFreqNonindexed[i + 1]);
			stats.addNonindexed((Integer) wordFreqNonindexed[i + 2]);
			previous = word;
		}
		NameIndexReader indx = new NameIndexReader(new AddressRegion());
		indx.setCommonIndexed(stats.build());
		return indx;
	}
}
