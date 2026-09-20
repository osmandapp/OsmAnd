package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.ObfConstants;
import net.osmand.data.Amenity;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * {@link net.osmand.shared.binary.ObfConstants} is a copy of the device half of
 * {@link ObfConstants}: how an osm id is packed into an obf id and back.
 *
 * The arithmetic is bit shifts and masks over values that reach far up a long, and a copy that got
 * one shift wrong would give plausible looking ids that point at the wrong osm objects. So this
 * runs both over ids built to sit on each of the bits that mean something - relation, split,
 * propagated node - and then over random ids.
 */
public class ObfConstantsCompatTest {

	@Test
	public void constantsAreTheSame() {
		assertEquals("SHIFT_ID", ObfConstants.SHIFT_ID,
				net.osmand.shared.binary.ObfConstants.SHIFT_ID);
		assertEquals("SHIFT_MULTIPOLYGON_IDS", ObfConstants.SHIFT_MULTIPOLYGON_IDS,
				net.osmand.shared.binary.ObfConstants.SHIFT_MULTIPOLYGON_IDS);
		assertEquals("SHIFT_NON_SPLIT_EXISTING_IDS", ObfConstants.SHIFT_NON_SPLIT_EXISTING_IDS,
				net.osmand.shared.binary.ObfConstants.SHIFT_NON_SPLIT_EXISTING_IDS);
		assertEquals("SHIFT_PROPAGATED_NODE_IDS", ObfConstants.SHIFT_PROPAGATED_NODE_IDS,
				net.osmand.shared.binary.ObfConstants.SHIFT_PROPAGATED_NODE_IDS);
		assertEquals("SHIFT_PROPAGATED_NODES_BITS", ObfConstants.SHIFT_PROPAGATED_NODES_BITS,
				net.osmand.shared.binary.ObfConstants.SHIFT_PROPAGATED_NODES_BITS);
		assertEquals("MAX_ID_PROPAGATED_NODES", ObfConstants.MAX_ID_PROPAGATED_NODES,
				net.osmand.shared.binary.ObfConstants.MAX_ID_PROPAGATED_NODES);
		assertEquals("RELATION_BIT", ObfConstants.RELATION_BIT,
				net.osmand.shared.binary.ObfConstants.RELATION_BIT);
		assertEquals("PROPAGATE_NODE_BIT", ObfConstants.PROPAGATE_NODE_BIT,
				net.osmand.shared.binary.ObfConstants.PROPAGATE_NODE_BIT);
		assertEquals("SPLIT_BIT", ObfConstants.SPLIT_BIT,
				net.osmand.shared.binary.ObfConstants.SPLIT_BIT);
		assertEquals("DUPLICATE_SPLIT", ObfConstants.DUPLICATE_SPLIT,
				net.osmand.shared.binary.ObfConstants.DUPLICATE_SPLIT);
	}

	@Test
	public void unpackingIdsIsTheSame() {
		for (long id : ids()) {
			String m = "id " + id;
			assertEquals(m + " from map object id", ObfConstants.getOsmIdFromMapObjectId(id),
					net.osmand.shared.binary.ObfConstants.INSTANCE.getOsmIdFromMapObjectId(id));
			assertEquals(m + " from binary map object id", ObfConstants.getOsmIdFromBinaryMapObjectId(id),
					net.osmand.shared.binary.ObfConstants.INSTANCE.getOsmIdFromBinaryMapObjectId(id));

			Amenity java = new Amenity();
			java.setId(id);
			net.osmand.shared.data.Amenity copy = new net.osmand.shared.data.Amenity();
			copy.setId(id);
			assertEquals(m + " of map object", ObfConstants.getOsmObjectId(java),
					net.osmand.shared.binary.ObfConstants.INSTANCE.getOsmObjectId(copy));
			assertEquals(m + " url available", ObfConstants.isOsmUrlAvailable(java),
					net.osmand.shared.binary.ObfConstants.INSTANCE.isOsmUrlAvailable(copy));

			BinaryMapDataObject javaObject = new BinaryMapDataObject(
					id, new int[0], null, 1, false, new int[0], null, 0, 0);
			net.osmand.shared.binary.BinaryMapDataObject copyObject =
					new net.osmand.shared.binary.BinaryMapDataObject(
							id, new int[0], null, 1, false, new int[0], null, 0, 0);
			assertEquals(m + " of binary map object", ObfConstants.getOsmObjectId(javaObject),
					net.osmand.shared.binary.ObfConstants.INSTANCE.getOsmObjectId(copyObject));
			assertEquals(m + " binary toString", javaObject.toString(), copyObject.toString());
		}
		assertEquals("null map object", ObfConstants.getOsmObjectId((BinaryMapDataObject) null),
				net.osmand.shared.binary.ObfConstants.INSTANCE
						.getOsmObjectId((net.osmand.shared.binary.BinaryMapDataObject) null));
	}

	@Test
	public void routeIdPrefixesAreTheSame() {
		for (String routeId : new String[] {"O7700604", "OSM7700604", "O0", "OSM0", "O", "OSM",
				"Onot a number", "7700604", "", "OO123", "O-5", "O99999999999999999999"}) {
			assertEquals("route id " + routeId, ObfConstants.getOsmIdFromPrefixedRouteId(routeId),
					net.osmand.shared.binary.ObfConstants.INSTANCE.getOsmIdFromPrefixedRouteId(routeId));
		}
	}

	@Test
	public void indexedTagsAreTheSame() {
		for (String tag : new String[] {"name", "name:en", "name:de", "ref", "brand", "brand:en",
				"route_name", "route_name:de", "shield_stub_name", "tiger:name", "noname",
				"name:etymology", "name:etymology:wikidata", "artist_name", "addr:street",
				"addr:housenumber", "wikidata", "route_id", "route_members_ids", "opening_hours",
				"description", null}) {
			String m = "tag " + tag;
			assertEquals(m + " as name", ObfConstants.isTagIndexedForSearchAsName(tag),
					net.osmand.shared.binary.ObfConstants.INSTANCE.isTagIndexedForSearchAsName(tag));
			assertEquals(m + " as id", ObfConstants.isTagIndexedForSearchAsId(tag),
					net.osmand.shared.binary.ObfConstants.INSTANCE.isTagIndexedForSearchAsId(tag));
			assertEquals(m + " as related", ObfConstants.isTagIndexedAsSearchRelated(tag),
					net.osmand.shared.binary.ObfConstants.INSTANCE.isTagIndexedAsSearchRelated(tag));
			if (tag != null) {
				assertEquals(m + " non indexed as name",
						ObfConstants.isTagNonIndexedForSearchAsName(tag),
						net.osmand.shared.binary.ObfConstants.INSTANCE.isTagNonIndexedForSearchAsName(tag));
			}
		}
	}

	/** Ids sitting on each meaningful bit, the plain node and way shapes, and random ones. */
	private static List<Long> ids() {
		List<Long> ids = new ArrayList<>();
		ids.add(0L);
		ids.add(-1L);
		ids.add(1L);
		ids.add(Long.MAX_VALUE);
		for (long osm : new long[] {1, 42, 7700604, 988560310, 12345678901L}) {
			ids.add(osm << 1); // node
			ids.add((osm << 1) + 1); // way
			ids.add(ObfConstants.RELATION_BIT + ((osm << ObfConstants.SHIFT_ID) << ObfConstants.DUPLICATE_SPLIT));
			ids.add(ObfConstants.SPLIT_BIT + ((osm << ObfConstants.SHIFT_ID) << ObfConstants.DUPLICATE_SPLIT));
			ids.add(ObfConstants.PROPAGATE_NODE_BIT + (osm << ObfConstants.SHIFT_PROPAGATED_NODES_BITS));
		}
		Random random = new Random(11);
		for (int i = 0; i < 20000; i++) {
			ids.add(random.nextLong() >>> random.nextInt(16));
		}
		return ids;
	}
}
