package net.osmand.binary;

import net.osmand.NativeLibrary;

import org.junit.Test;
import org.junit.Assert;

public class ObfConstantsTest {

	@Test
	public void getOsmObjectIdShiftRenderedObjectIdTest() {
		NativeLibrary.RenderedObject ro = new NativeLibrary.RenderedObject();

		ro.setId(995209258204L); // https://www.openstreetmap.org/node/7775072329
		Assert.assertEquals(7775072329L, ObfConstants.getOsmObjectId(ro)); // Node (canoe)
		ro.setId(1174315991128L); // https://www.openstreetmap.org/node/9174343680
		Assert.assertEquals(9174343680L, ObfConstants.getOsmObjectId(ro)); // Node (barrier)
		ro.setId(1643724341992L); // https://www.openstreetmap.org/node/12841596421
		Assert.assertEquals(12841596421L, ObfConstants.getOsmObjectId(ro)); // Node (traffic_calming)

		ro.setId(117182426983L); // https://www.openstreetmap.org/way/915487710
		Assert.assertEquals(915487710L, ObfConstants.getOsmObjectId(ro)); // Way (amenity)
		ro.setId(164762636675L); // https://www.openstreetmap.org/way/1287208099
		Assert.assertEquals(1287208099L, ObfConstants.getOsmObjectId(ro)); // Way (amenity)
		ro.setId(24823121815L); // https://www.openstreetmap.org/way/193930639
		Assert.assertEquals(193930639L, ObfConstants.getOsmObjectId(ro)); // Way (building)

		ro.setId(8804205012992L); // https://www.openstreetmap.org/relation/1980466
		Assert.assertEquals(1980466L, ObfConstants.getOsmObjectId(ro)); // Relation (natural=water) UI=ok
		ro.setId(8817178845508L); // https://www.openstreetmap.org/relation/5147906
		Assert.assertEquals(5147906L, ObfConstants.getOsmObjectId(ro)); // Relation (amenity=university) UI=ok
		ro.setId(8873046276608L); // https://www.openstreetmap.org/relation/18787415
		Assert.assertEquals(18787417L, ObfConstants.getOsmObjectId(ro)); // Relation (type=multipolygon) UI=fail

		ro.setId(1131128778166272L); // https://www.openstreetmap.org/way/1276579913
		Assert.assertEquals(1276579913L, ObfConstants.getOsmObjectId(ro)); // Propagated Node (osmand_amenity)
		ro.setId(1130851123449856L); // https://www.openstreetmap.org/way/1208793117
		Assert.assertEquals(1208793117L, ObfConstants.getOsmObjectId(ro)); // Propagated Node (osmand_access)
		ro.setId(1126863823527946L); // https://www.openstreetmap.org/way/235331222
		Assert.assertEquals(235331222L, ObfConstants.getOsmObjectId(ro)); // Propagated Node (osmand_access)
	}
}