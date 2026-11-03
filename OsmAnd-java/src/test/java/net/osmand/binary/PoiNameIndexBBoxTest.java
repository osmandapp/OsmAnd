package net.osmand.binary;

import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.Amenity;
import net.osmand.util.MapUtils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Search by name must not lose a POI inside the request bbox only because the top-left corner of
 * its name index data box is outside the bbox. Fixture: resources/test-resources/poi_name_index/README.md.
 */
public class PoiNameIndexBBoxTest {

	private static final File MAP = new File("src/test/resources/poi_name_index/poi_name_index_corner.obf");
	private static final long TARGET_ID = 2002;
	private static final long CONTROL_ID = 2004;

	private BinaryMapIndexReader reader;

	@Before
	public void setUp() throws IOException {
		reader = new BinaryMapIndexReader(new RandomAccessFile(MAP, "r"), MAP);
	}

	@After
	public void tearDown() throws IOException {
		reader.close();
	}

	@Test
	public void smallBBoxFindsPoiOfDataBoxWithCornerOutside() throws IOException {
		Assert.assertEquals(Collections.singletonList(TARGET_ID), searchSmall());
	}

	@Test
	public void disjointBBoxDoesNotFindPoi() throws IOException {
		// This bbox lies in the control POI tile, east of the target data box.
		Assert.assertEquals(Collections.emptyList(), search("Cornerbox", 49.7230, 13.4000, 49.7224, 13.4010));
		Assert.assertEquals(Collections.singletonList(CONTROL_ID), search("Widebox", 49.7230, 13.4000, 49.7224, 13.4010));
	}

	@Test
	public void wideBBoxFindsControlPoi() throws IOException {
		Assert.assertEquals(Collections.singletonList(TARGET_ID), search("Cornerbox", 49.7250, 13.3920, 49.7200, 13.4010));
		Assert.assertEquals(Collections.singletonList(CONTROL_ID), search("Widebox", 49.7250, 13.3920, 49.7200, 13.4010));
	}

	@Test
	public void smallBBoxAfterCacheWarmUp() throws IOException {
		search("Cornerbox", 49.7250, 13.3920, 49.7200, 13.4010);
		Assert.assertEquals(Collections.singletonList(TARGET_ID), searchSmall());
		Assert.assertEquals(Collections.singletonList(TARGET_ID), searchSmall());
	}

	private List<Long> searchSmall() throws IOException {
		return search("Cornerbox", 49.7215672, 13.3968384, 49.7201466, 13.3990356);
	}

	private List<Long> search(String name, double top, double left, double bottom, double right) throws IOException {
		int l = MapUtils.get31TileNumberX(left);
		int r = MapUtils.get31TileNumberX(right);
		int t = MapUtils.get31TileNumberY(top);
		int b = MapUtils.get31TileNumberY(bottom);
		// request point = bbox center, as the map layer does
		SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(l / 2 + r / 2, t / 2 + b / 2, name,
				l, r, t, b, null);
		List<Long> ids = new ArrayList<>();
		for (Amenity a : reader.searchPoiByName(req)) {
			ids.add(a.getId());
		}
		return ids;
	}
}
