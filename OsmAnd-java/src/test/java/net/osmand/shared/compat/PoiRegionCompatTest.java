package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;

import gnu.trove.set.hash.TLongHashSet;

import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiRegion;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiSubType;
import net.osmand.osm.MapPoiTypes;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * The bookkeeping inside {@link net.osmand.shared.binary.PoiRegion} against java's: which tag
 * groups a section has already read, and how a subtype id is split.
 *
 * This is here because none of the obf files the tests ship with carries tag groups or top index
 * attributes, so {@link PoiSearchCompatTest} never reaches the code that handles them. What can be
 * checked without such a file is checked here; reading them out of one waits for a fixture that
 * has them.
 */
public class PoiRegionCompatTest {

	private static final String POI_TYPES = "src/test/resources/poi_types.xml";

	@BeforeClass
	public static void readPoiTypes() {
		MapPoiTypes.setDefault(new MapPoiTypes(POI_TYPES));
		net.osmand.shared.osm.MapPoiTypes.setDefault(
				new net.osmand.shared.osm.MapPoiTypes(POI_TYPES));
	}

	/** Tag groups are stored by id into a list that grows with holes in it. */
	@Test
	public void tagGroupsAreStoredTheSame() {
		PoiRegion java = new PoiRegion();
		net.osmand.shared.binary.PoiRegion copy = new net.osmand.shared.binary.PoiRegion();
		for (int id : new int[] {3, 1, 7, 3, 0}) {
			java.setTagGroups(id, pairs("place", "city", "name", "Praha " + id));
			copy.setTagGroups(id, copyPairs("place", "city", "name", "Praha " + id));
		}
		for (int id = 0; id < 12; id++) {
			assertEquals("tag group " + id, describe(java.getTagValues(id)),
					copyDescribe(copy.getTagValues(id)));
		}
	}

	/** Which tiles a section still has to read tag groups for. */
	@Test
	public void missingTagGroupsAreTheSame() {
		PoiRegion java = new PoiRegion();
		net.osmand.shared.binary.PoiRegion copy = new net.osmand.shared.binary.PoiRegion();
		Random random = new Random(7);
		for (int round = 0; round < 200; round++) {
			long[] asked = new long[1 + random.nextInt(8)];
			for (int i = 0; i < asked.length; i++) {
				asked[i] = random.nextInt(40);
			}
			TLongHashSet javaAsked = new TLongHashSet(asked);
			net.osmand.shared.util.collections.KTLongHashSet copyAsked =
					new net.osmand.shared.util.collections.KTLongHashSet();
			copyAsked.addAll(asked);

			TLongHashSet javaMissing = java.checkMissingTagGroups(javaAsked);
			net.osmand.shared.util.collections.KTLongHashSet copyMissing =
					copy.checkMissingTagGroups(copyAsked);
			assertEquals("round " + round + " missing", sorted(javaMissing.toArray()),
					sorted(copyMissing.toArray()));
			// the same set the caller passed in, modified in place, on both sides
			assertEquals("round " + round + " asked in place", sorted(javaAsked.toArray()),
					sorted(copyAsked.toArray()));

			java.updReadTagGroups(javaMissing);
			copy.updReadTagGroups(copyMissing);
		}
		// everything asked for has been read by now
		TLongHashSet javaAll = new TLongHashSet();
		net.osmand.shared.util.collections.KTLongHashSet copyAll =
				new net.osmand.shared.util.collections.KTLongHashSet();
		for (long i = 0; i < 40; i++) {
			javaAll.add(i);
			copyAll.add(i);
		}
		assertEquals("nothing left", sorted(java.checkMissingTagGroups(javaAll).toArray()),
				sorted(copy.checkMissingTagGroups(copyAll).toArray()));
	}

	/** A subtype id packs the attribute and the value differently depending on its low bit. */
	@Test
	public void subtypeIdsAreSplitTheSame() {
		PoiRegion java = new PoiRegion();
		net.osmand.shared.binary.PoiRegion copy = new net.osmand.shared.binary.PoiRegion();
		for (int i = 0; i < 40; i++) {
			PoiSubType js = new PoiSubType();
			net.osmand.shared.binary.PoiSubType ks = new net.osmand.shared.binary.PoiSubType();
			js.name = "subtype_" + i;
			ks.name = "subtype_" + i;
			js.text = i % 5 == 0;
			ks.text = i % 5 == 0;
			if (!js.text) {
				js.possibleValues = new ArrayList<>();
				ks.possibleValues = new ArrayList<>();
				for (int v = 0; v < i; v++) {
					js.possibleValues.add("value_" + v);
					ks.possibleValues.add("value_" + v);
				}
			}
			java.getSubTypes().add(js);
			copy.getSubTypes().add(ks);
		}
		Random random = new Random(13);
		for (int round = 0; round < 20000; round++) {
			int id = round < 2000 ? round : random.nextInt(1 << 22);
			StringBuilder jv = new StringBuilder();
			StringBuilder kv = new StringBuilder();
			PoiSubType jst = java.getSubtypeFromId(id, jv);
			net.osmand.shared.binary.PoiSubType kst = copy.getSubtypeFromId(id, kv);
			assertEquals("id " + id + " present", jst == null, kst == null);
			if (jst != null) {
				assertEquals("id " + id + " name", jst.name, kst.name);
				assertEquals("id " + id + " text", jst.text, kst.text);
			}
			assertEquals("id " + id + " value", jv.toString(), kv.toString());
		}
	}

	private static List<TagValuePair> pairs(String... tagValues) {
		List<TagValuePair> list = new ArrayList<>();
		for (int i = 0; i < tagValues.length; i += 2) {
			list.add(new TagValuePair(tagValues[i], tagValues[i + 1], -1));
		}
		return list;
	}

	private static List<net.osmand.shared.binary.TagValuePair> copyPairs(String... tagValues) {
		List<net.osmand.shared.binary.TagValuePair> list = new ArrayList<>();
		for (int i = 0; i < tagValues.length; i += 2) {
			list.add(new net.osmand.shared.binary.TagValuePair(tagValues[i], tagValues[i + 1], -1));
		}
		return list;
	}

	private static String describe(List<TagValuePair> pairs) {
		if (pairs == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		for (TagValuePair p : pairs) {
			sb.append(p.tag).append('=').append(p.value).append(';');
		}
		return sb.toString();
	}

	private static String copyDescribe(List<net.osmand.shared.binary.TagValuePair> pairs) {
		if (pairs == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		for (net.osmand.shared.binary.TagValuePair p : pairs) {
			sb.append(p.tag).append('=').append(p.value).append(';');
		}
		return sb.toString();
	}

	private static String sorted(long[] values) {
		long[] copy = values.clone();
		Arrays.sort(copy);
		return Arrays.toString(copy);
	}
}
