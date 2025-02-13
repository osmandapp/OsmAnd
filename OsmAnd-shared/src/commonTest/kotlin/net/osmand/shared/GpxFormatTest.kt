package net.osmand.shared

import net.osmand.shared.gpx.GpxFormatter
import net.osmand.shared.gpx.GpxUtilities
import kotlin.test.Test
import kotlin.test.assertEquals

class GpxFormatTest {

    @Test
    fun testGpxLatLonFormat() {
        assertEquals("0.00", GpxFormatter.formatLatLon(0.0000000))
        assertEquals("0.0000001", GpxFormatter.formatLatLon(0.0000001))
        assertEquals("12.00", GpxFormatter.formatLatLon(12.0))
        assertEquals("12.30", GpxFormatter.formatLatLon(12.3))
        assertEquals("12.34567", GpxFormatter.formatLatLon(12.34567))
        assertEquals("0.1234568", GpxFormatter.formatLatLon(0.123456789))
        assertEquals("0.0001834", GpxFormatter.formatLatLon(0.0001834))
        assertEquals("-0.0001834", GpxFormatter.formatLatLon(-0.0001834))
        assertEquals("0.00001", GpxFormatter.formatLatLon(0.0000100))
        assertEquals("0.0001", GpxFormatter.formatLatLon(0.0001000))
        assertEquals("-0.0001", GpxFormatter.formatLatLon(-0.0001000))
    }

    @Test
    fun testGpxDecimalFormat() {
        assertEquals("1.2", GpxFormatter.formatDecimal(1.21))
        assertEquals("1.3", GpxFormatter.formatDecimal(1.26))
        assertEquals("2", GpxFormatter.formatDecimal(2.0))
        assertEquals("-3.7", GpxFormatter.formatDecimal(-3.74))
        assertEquals("0", GpxFormatter.formatDecimal(0.0))
    }
}