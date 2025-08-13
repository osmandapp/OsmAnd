package net.osmand.shared

import net.osmand.shared.gpx.GpxUtilities
import kotlin.test.Test
import kotlin.test.assertEquals

class ParseTimeTest {
    @Test
    fun testParseTime() {
        // GPX_TIME_PATTERN_TZ GPX_TIME_PATTERN_TZ_NO_SEPARATOR
        assertEquals(0, GpxUtilities.parseTime("1970-01-01T00:00:00.000Z"));
        assertEquals(1323785716000, GpxUtilities.parseTime("2011-12-13T14:15:16Z"));
        assertEquals(1323785716000, GpxUtilities.parseTime("2011-12-13T14:15:16+0000"));
        assertEquals(1323778516000, GpxUtilities.parseTime("2011-12-13T14:15:16+0200"));
        assertEquals(1323792916000, GpxUtilities.parseTime("2011-12-13T14:15:16-0200"));
        assertEquals(1323785716000, GpxUtilities.parseTime("2011-12-13T14:15:16+00:00"));
        assertEquals(1323778516000, GpxUtilities.parseTime("2011-12-13T14:15:16+02:00"));
        assertEquals(1323792916000, GpxUtilities.parseTime("2011-12-13T14:15:16-02:00"));

        // GPX_TIME_PATTERN_TZ with fractional seconds (flexibleMillisecondsTimeParser)
        assertEquals(1323785716100, GpxUtilities.parseTime("2011-12-13T14:15:16.1Z"));
        assertEquals(1323785716220, GpxUtilities.parseTime("2011-12-13T14:15:16.22Z"));
        assertEquals(1323785716333, GpxUtilities.parseTime("2011-12-13T14:15:16.333Z"));
        assertEquals(1323785716444, GpxUtilities.parseTime("2011-12-13T14:15:16.4444Z"));
        assertEquals(1323785716555, GpxUtilities.parseTime("2011-12-13T14:15:16.55555Z"));
        assertEquals(1323785716666, GpxUtilities.parseTime("2011-12-13T14:15:16.666666Z"));
        assertEquals(1323785716777, GpxUtilities.parseTime("2011-12-13T14:15:16.7777777Z"));
        assertEquals(1323785716000, GpxUtilities.parseTime("2011-12-13T14:15:16.0000001Z"));
        assertEquals(1323785716011, GpxUtilities.parseTime("2011-12-13T14:15:16.0111111Z"));
        assertEquals(1323785716333, GpxUtilities.parseTime("2011-12-13T14:15:16.333+0000"));
        assertEquals(1323785716333, GpxUtilities.parseTime("2011-12-13T14:15:16.333+00:00"));
        assertEquals(1323785716000, GpxUtilities.parseTime("2011-12-13T14:15:16.00000Z"));
        assertEquals(1323785716000, GpxUtilities.parseTime("2011-12-13T14:15:16.000Z"));
        assertEquals(1323785716000, GpxUtilities.parseTime("2011-12-13T14:15:16.0Z"));
        assertEquals(1323785716000, GpxUtilities.parseTime("2011-12-13T14:15:16.Z"));

        // GPX_TIME_PATTERN_NO_TZ
        assertEquals(1323785716000, GpxUtilities.parseTime("2011-12-13T14:15:16"));

        // GPX_TIME_PATTERN_NO_SECONDS GPX_TIME_PATTERN_NO_SECONDS_NO_TZ
        assertEquals(1323785700000, GpxUtilities.parseTime("2011-12-13T14:15"));
        assertEquals(1323785700000, GpxUtilities.parseTime("2011-12-13T14:15Z"));
        assertEquals(1323785700000, GpxUtilities.parseTime("2011-12-13T14:15+00:00"));
        assertEquals(1323785700000, GpxUtilities.parseTime("2011-12-13T14:15-00:00"));

        // GPX_TIME_PATTERN_TZ_EXTRA_Z 2005-05-07T05:45:04+04:00Z
        assertEquals(1323778516000, GpxUtilities.parseTime("2011-12-13T14:15:16+02:00Z"));

        // Test: " " -> "T" (Spaces are not allowed in ISO 8601, but allowed in its profile RFC 3339)
        assertEquals(1323785716000, GpxUtilities.parseTime("2011-12-13 14:15:16Z"));
        assertEquals(1323785716777, GpxUtilities.parseTime("2011-12-13 14:15:16.7777777Z"));

        // Test: trim, [\r\n]
        assertEquals(1323785716000, GpxUtilities.parseTime("2011-12-13 14:15:16Z "));
        assertEquals(1323785716000, GpxUtilities.parseTime("2011-12-13 14:15:16Z\t"));
        assertEquals(1323785716000, GpxUtilities.parseTime(" 2011-12-13 14:15:16Z\n"));
        assertEquals(1323785716000, GpxUtilities.parseTime("2011-12-13 14:15:16Z  \t\t"));
        assertEquals(1323785716000, GpxUtilities.parseTime("\r\n2011-12-13 14:15:16Z\r\n"));
        assertEquals(1323785716000, GpxUtilities.parseTime("\t\t  2011-12-13 14:15:16Z  \t\t"));
    }
}
