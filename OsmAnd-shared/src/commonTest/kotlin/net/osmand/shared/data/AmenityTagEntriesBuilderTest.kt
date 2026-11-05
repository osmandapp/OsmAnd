package net.osmand.shared.data

import net.osmand.shared.data.AmenityTagEntriesBuilder.ValueLabel.NAME
import net.osmand.shared.data.AmenityTagEntriesBuilder.ValueLabel.NAME_OVER_VALUE
import net.osmand.shared.data.AmenityTagEntriesBuilder.ValueLabel.WHOLE
import net.osmand.shared.data.AmenityTagEntriesBuilder.ValueLabelParts
import net.osmand.shared.data.AmenityTagEntriesBuilder.getValueLabelParts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AmenityTagEntriesBuilderTest {

	@Test
	fun splitsNameAndValue() {
		assertEquals(Pair("Smoking", "no"), AmenityTagEntriesBuilder.splitNameValue("Smoking: no"))
		assertEquals(Pair("Payant", "non"), AmenityTagEntriesBuilder.splitNameValue("Payant : non"))
		assertEquals(Pair("吸菸", "允許"), AmenityTagEntriesBuilder.splitNameValue("吸菸: 允許"))
		assertNull(AmenityTagEntriesBuilder.splitNameValue("Cash"))
		assertNull(AmenityTagEntriesBuilder.splitNameValue("1:25 fuel"))
		assertNull(AmenityTagEntriesBuilder.splitNameValue("Yes"))
		assertNull(AmenityTagEntriesBuilder.splitNameValue(""))
		assertNull(AmenityTagEntriesBuilder.splitNameValue(null))
	}

	@Test
	fun showsValuesByLabel() {
		assertEquals(ValueLabelParts("Smoking", "No"), getValueLabelParts("Smoking: no", NAME_OVER_VALUE))
		assertEquals(ValueLabelParts(null, "Kayaks"), getValueLabelParts("Kayaks: yes", NAME))
		assertEquals(ValueLabelParts(null, "Kayaks: limited"), getValueLabelParts("Kayaks: limited", WHOLE))
		assertEquals(ValueLabelParts(null, "Not allowed"), getValueLabelParts("Not allowed", NAME_OVER_VALUE))
		assertEquals(ValueLabelParts(null, "Cash"), getValueLabelParts("Cash", NAME))
	}
}
