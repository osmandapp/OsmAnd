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
