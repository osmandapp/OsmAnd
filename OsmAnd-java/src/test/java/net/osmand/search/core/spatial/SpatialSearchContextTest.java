package net.osmand.search.core.spatial;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import gnu.trove.map.hash.TLongObjectHashMap;
import net.osmand.binary.BinaryMapAddressReaderAdapter.AddressRegion;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.City;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialSearchGlobalCache;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings;

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
}
