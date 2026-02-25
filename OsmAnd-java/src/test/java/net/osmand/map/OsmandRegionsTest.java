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
