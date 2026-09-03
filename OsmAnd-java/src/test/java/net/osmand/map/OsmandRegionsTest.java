package net.osmand.map;

import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapDataObject;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OsmandRegionsTest {
    OsmandRegions osmandRegions;

    public OsmandRegionsTest() {
        try {
            osmandRegions = PlatformUtil.getOsmandRegions();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testRegionSearchMatching() {
        WorldRegion pennsylvania = osmandRegions.getRegionDataByDownloadName("us_pennsylvania_northamerica");
        Assert.assertFalse(matches("1", pennsylvania)); // 1-char queries matched all regions
        Assert.assertTrue(matches("PA", pennsylvania)); // ref

        WorldRegion meuse = osmandRegions.getRegionDataByDownloadName("france_great-east_meuse_europe");
        Assert.assertFalse(matches("55", meuse)); // ref

        WorldRegion bicol = osmandRegions.getRegionDataByDownloadName("philippines_bicol-region_asia");
        Assert.assertFalse(matches("5", bicol)); // alt_name

        WorldRegion centralVisayas = osmandRegions.getRegionDataByDownloadName("philippines_central-visayas_asia");
        Assert.assertFalse(matches("7", centralVisayas)); // alt_name
    }

    private boolean matches(String query, WorldRegion region) {
        return OsmandRegions.isRegionNameMatched(query, region.getRegionSearchText());
    }

    @Test
    public void testRemoveDuplicates() {
        List<String> cz = List.of(
                "czech-republic_jihovychod_europe",
                "czech-republic_jihozapad_europe",
                "czech-republic_moravskoslezsko_europe",
                "czech-republic_praha_europe",
                "czech-republic_severovychod_europe",
                "czech-republic_severozapad_europe",
                "czech-republic_stredni-cechy_europe",
                "czech-republic_stredni-morava_europe"
        );
        List<WorldRegion> regions = new ArrayList<>();
        for (String downloadName : cz) {
            regions.add(osmandRegions.getRegionDataByDownloadName(downloadName));
        }
        Set<String> deduplicatedDownloadNames = new HashSet<>();
        for (WorldRegion region : WorldRegion.removeDuplicates(regions)) {
            deduplicatedDownloadNames.add(region.getRegionDownloadName());
        }
        Assert.assertEquals(deduplicatedDownloadNames.size(), cz.size());
        Assert.assertTrue(deduplicatedDownloadNames.contains("czech-republic_praha_europe"));
    }

    @Test
    public void testGetCountryRegion() {
        // a country is the region right under a continent
        Assert.assertEquals("europe_germany", countryId("germany_berlin_europe"));
        Assert.assertEquals("europe_germany", countryId("germany_europe"));
        Assert.assertEquals("europe_netherlands", countryId("netherlands_noord-holland_europe"));
        Assert.assertEquals("europe_ukraine", countryId("ukraine_kyiv-city_europe"));
        Assert.assertEquals("northamerica_us", countryId("us_new-hampshire_northamerica"));

        // a country that is placed next to the continents instead of under one
        Assert.assertEquals("russia", countryId("russia_moscow_asia"));
        Assert.assertEquals("russia", countryId("russia_north-caucasus-federal-district_asia"));

        // continents and the world itself are not countries
        Assert.assertNull(osmandRegions.getRegionData("europe").getCountryRegion());
        Assert.assertNull(osmandRegions.getWorldRegion().getCountryRegion());
    }

    private String countryId(String downloadName) {
        WorldRegion region = osmandRegions.getRegionDataByDownloadName(downloadName);
        Assert.assertNotNull("Unknown region " + downloadName, region);
        WorldRegion country = region.getCountryRegion();
        return country != null ? country.getRegionId() : null;
    }

    @Test
    public void testGetRegionsToDownload() {
        // Prague not part of Central Bohemia
        testIncludedExcluded(50.087463, 14.421259,
                "czech-republic_praha_europe", "czech-republic_stredni-cechy_europe");

        // ACT not part of New South Wales
        testIncludedExcluded(-35.308056, 149.124444,
                "australia-oceania_australian-capital-territory_australia-oceania",
                "australia-oceania_new-south-wales_australia-oceania");

        // Additional subregion query must return the main region name (Lienz, Tirol, Austria)
        testIncludedExcluded(46.82987, 12.76812, "austria_tyrol_europe", "");
    }

    private void testIncludedExcluded(double lat, double lon, String included, String excluded) {
        try {
            Set<String> downloadNames = new HashSet<>();
            for (BinaryMapDataObject region : osmandRegions.getRegionsToDownload(lat, lon)) {
                downloadNames.add(osmandRegions.getDownloadName(region));
            }
            Assert.assertTrue(downloadNames.contains(included));
            Assert.assertFalse(downloadNames.contains(excluded));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
