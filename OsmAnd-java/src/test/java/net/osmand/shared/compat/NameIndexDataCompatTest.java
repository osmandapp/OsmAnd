package net.osmand.shared.compat;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.WireFormat;

import net.osmand.binary.OsmandOdb.AddressNameIndexDataAtom;
import net.osmand.binary.OsmandOdb.OsmAndAddressNameIndexData.AddressNameIndexData;
import net.osmand.binary.OsmandOdb.OsmAndPoiNameIndex.OsmAndPoiNameIndexData;
import net.osmand.binary.OsmandOdb.OsmAndPoiNameIndexDataAtom;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * The blocks of a name index written by hand, the way the obf writer never writes them: repeated
 * numbers packed, unknown fields of every wire type, a known field with another wire type, and an
 * address atom without its required type. {@link NameIndexCompatTest} reads the blocks of real
 * files, which hold none of these; the parser generated for java reads them all, and so does the
 * copy, field for field, or refuses the block where java does.
 *
 * Not compared: the serialized size. The copy counts the bytes it read the block from, java adds up
 * the fields as it would write them, numbers unpacked, one tag each; the two agree on every block of
 * the files, which java wrote, and on unknown fields, and not on numbers written packed.
 */
public class NameIndexDataCompatTest {

	@BeforeClass
	public static void enabled() {
		SearchCompat.assumeEnabled();
	}

	private interface Writer {
		void write(CodedOutputStream out) throws IOException;
	}

	private static byte[] bytes(Writer writer) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		CodedOutputStream out = CodedOutputStream.newInstance(bos);
		writer.write(out);
		out.flush();
		return bos.toByteArray();
	}

	private static void packed(CodedOutputStream out, int field, int... values) throws IOException {
		byte[] body = bytes(o -> {
			for (int v : values) {
				o.writeRawVarint32(v);
			}
		});
		out.writeTag(field, WireFormat.WIRETYPE_LENGTH_DELIMITED);
		out.writeRawVarint32(body.length);
		out.writeRawBytes(body);
	}

	private static void message(CodedOutputStream out, int field, byte[] body) throws IOException {
		out.writeTag(field, WireFormat.WIRETYPE_LENGTH_DELIMITED);
		out.writeRawVarint32(body.length);
		out.writeRawBytes(body);
	}

	/** Fields the messages do not know, of every wire type, and a known field with another wire type. */
	private static void unknown(CodedOutputStream out, int knownVarintField) throws IOException {
		out.writeUInt32(30, 7);
		out.writeFixed64(31, 0x0102030405060708L);
		out.writeString(32, "unknown");
		out.writeFixed32(33, 0x01020304);
		out.writeString(knownVarintField, "a varint field written as bytes");
	}

	private static byte[] addressAtom(boolean withType) throws IOException {
		return bytes(o -> {
			if (withType) {
				o.writeUInt32(3, 4);
			}
			packed(o, 8, 2, 0, 5, 300, 4);
			o.writeUInt32(8, 6);
			o.writeString(9, " extra");
			packed(o, 10, 1, 0, 2);
			o.writeBytes(12, com.google.protobuf.ByteString.copyFrom(new byte[] {15, 3, 2, 4, 1}));
			o.writeUInt32(16, 12);
			packed(o, 5, -7, 123456, 0);
			o.writeInt32(5, -1);
			packed(o, 6, 99, -99);
			packed(o, 7, (int) 0xfffff00fL, 65537);
			unknown(o, 16);
		});
	}

	private static byte[] addressBlock(boolean withType) throws IOException {
		byte[] atom = addressAtom(true);
		byte[] last = addressAtom(withType);
		return bytes(o -> {
			o.writeUInt32(7, 2);
			o.writeString(2, "strasse");
			o.writeString(2, "tal");
			packed(o, 3, 0, 5, 2);
			o.writeUInt32(3, 9);
			message(o, 4, atom);
			unknown(o, 7);
			message(o, 4, last);
		});
	}

	private static byte[] poiBlock() throws IOException {
		byte[] atom = bytes(o -> {
			o.writeUInt32(2, 15);
			o.writeUInt32(3, 40000);
			o.writeUInt32(4, 30000);
			packed(o, 5, 2, 0, 7);
			o.writeString(7, " extra");
			packed(o, 8, 3);
			packed(o, 9, 1, 2, 3);
			o.writeUInt32(9, 4);
			o.writeBytes(10, com.google.protobuf.ByteString.copyFrom(new byte[] {16, 1, 1, 1, 1}));
			packed(o, 11, 128, 129);
			packed(o, 12, 5, 6);
			o.writeFixed32(14, 0x10203040);
			o.writeFixed32(20, 1);
			o.writeFixed32(21, -2);
			o.writeFixed32(22, 0x7fffffff);
			unknown(o, 2);
		});
		return bytes(o -> {
			packed(o, 1, 4, 5);
			o.writeUInt32(1, 6);
			o.writeString(2, "platz");
			message(o, 3, atom);
			message(o, 3, atom);
			o.writeUInt32(7, 1);
			unknown(o, 7);
		});
	}

	@Test
	public void handMadeAddressBlocksAreReadTheSame() throws IOException {
		byte[] data = addressBlock(true);
		AddressNameIndexData j = AddressNameIndexData.parseFrom(data);
		net.osmand.shared.binary.AddressNameIndexData k = net.osmand.shared.binary.AddressNameIndexData.Companion.parseFrom(data);
		assertEquals("suffixes", j.getSuffixesDictionaryList(), k.getSuffixesDictionaryList());
		assertEquals("common suffixes", j.getSuffixesCommonDictionaryList(), k.getSuffixesCommonDictionaryList());
		assertEquals("hasAtomsLength", j.hasAtomsLength(), k.hasAtomsLength());
		assertEquals("atomsLength", j.getAtomsLength(), k.getAtomsLength());
		assertEquals("atoms", 2, k.getAtomCount());
		assertEquals("atoms", j.getAtomCount(), k.getAtomCount());
		for (int n = 0; n < j.getAtomCount(); n++) {
			AddressNameIndexDataAtom ja = j.getAtom(n);
			net.osmand.shared.binary.AddressNameIndexDataAtom ka = k.getAtom(n);
			assertEquals("hasType", ja.hasType(), ka.hasType());
			assertEquals("type", ja.getType(), ka.getType());
			assertEquals("hasBbox", ja.hasBbox(), ka.hasBbox());
			assertEquals("hasEnclosing", ja.hasEnclosingObjects(), ka.hasEnclosingObjects());
			assertEquals("bits", ja.getSuffixesBitsetIndexList(), ka.getSuffixesBitsetIndexList());
			assertEquals("extra", ja.getExtraSuffixList(), ka.getExtraSuffixList());
			assertEquals("other", ja.getOtherWordsCountList(), ka.getOtherWordsCountList());
			assertArrayEquals("bbox", ja.getBbox().toByteArray(), ka.getBbox());
			assertEquals("enclosing", ja.getEnclosingObjects(), ka.getEnclosingObjects());
			assertEquals("shiftToIndex", ja.getShiftToIndexList(), ka.getShiftToIndexList());
			assertEquals("shiftToCityIndex", ja.getShiftToCityIndexList(), ka.getShiftToCityIndexList());
			assertEquals("xy16", ja.getXy16List(), ka.getXy16List());
		}
		assertTrue("the numbers are there", !k.getAtom(0).getSuffixesBitsetIndexList().isEmpty());
	}

	@Test
	public void anAddressAtomWithoutItsTypeIsRefusedTheSame() throws IOException {
		byte[] data = addressBlock(false);
		boolean javaRefused;
		try {
			AddressNameIndexData.parseFrom(data);
			javaRefused = false;
		} catch (IOException e) {
			javaRefused = true;
		}
		boolean copyRefused;
		try {
			net.osmand.shared.binary.AddressNameIndexData.Companion.parseFrom(data);
			copyRefused = false;
		} catch (IllegalStateException e) {
			copyRefused = true;
		}
		assertTrue("java refuses it", javaRefused);
		assertEquals("refused", javaRefused, copyRefused);
	}

	@Test
	public void handMadePoiBlocksAreReadTheSame() throws IOException {
		byte[] data = poiBlock();
		OsmAndPoiNameIndexData j = OsmAndPoiNameIndexData.parseFrom(data);
		net.osmand.shared.binary.OsmAndPoiNameIndexData k = net.osmand.shared.binary.OsmAndPoiNameIndexData.Companion.parseFrom(data);
		assertEquals("suffixes", j.getSuffixesDictionaryList(), k.getSuffixesDictionaryList());
		assertEquals("common suffixes", j.getSuffixesCommonDictionaryList(), k.getSuffixesCommonDictionaryList());
		assertEquals("hasAtomsLength", j.hasAtomsLength(), k.hasAtomsLength());
		assertEquals("atomsLength", j.getAtomsLength(), k.getAtomsLength());
		assertEquals("atoms", j.getAtomsCount(), k.getAtomsCount());
		for (int n = 0; n < j.getAtomsCount(); n++) {
			OsmAndPoiNameIndexDataAtom ja = j.getAtoms(n);
			net.osmand.shared.binary.OsmAndPoiNameIndexDataAtom ka = k.getAtoms(n);
			assertEquals("hasZoom", ja.hasZoom(), ka.hasZoom());
			assertEquals("zoom", ja.getZoom(), ka.getZoom());
			assertEquals("hasX", ja.hasX(), ka.hasX());
			assertEquals("hasY", ja.hasY(), ka.hasY());
			assertEquals("hasBbox", ja.hasBbox(), ka.hasBbox());
			assertEquals("hasShiftTo", ja.hasShiftTo(), ka.hasShiftTo());
			assertEquals("hasZ8", ja.hasShiftToNextZ8Tile(), ka.hasShiftToNextZ8Tile());
			assertEquals("hasZ10", ja.hasShiftToNextZ10Tile(), ka.hasShiftToNextZ10Tile());
			assertEquals("hasZ12", ja.hasShiftToNextZ12Tile(), ka.hasShiftToNextZ12Tile());
			assertEquals("x", ja.getX(), ka.getX());
			assertEquals("y", ja.getY(), ka.getY());
			assertEquals("bits", ja.getSuffixesBitsetIndexList(), ka.getSuffixesBitsetIndexList());
			assertEquals("extra", ja.getExtraSuffixList(), ka.getExtraSuffixList());
			assertEquals("other", ja.getOtherWordsCountList(), ka.getOtherWordsCountList());
			assertEquals("poiIndInBlock", ja.getPoiIndInBlockList(), ka.getPoiIndInBlockList());
			assertArrayEquals("bbox", ja.getBbox().toByteArray(), ka.getBbox());
			assertEquals("categories", ja.getPoiCategoriesList(), ka.getPoiCategoriesList());
			assertEquals("elo", ja.getEloRatingList(), ka.getEloRatingList());
			assertEquals("shiftTo", ja.getShiftTo(), ka.getShiftTo());
			assertEquals("z8", ja.getShiftToNextZ8Tile(), ka.getShiftToNextZ8Tile());
			assertEquals("z10", ja.getShiftToNextZ10Tile(), ka.getShiftToNextZ10Tile());
			assertEquals("z12", ja.getShiftToNextZ12Tile(), ka.getShiftToNextZ12Tile());
		}
	}
}
