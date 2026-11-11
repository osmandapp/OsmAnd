package net.osmand.router.gtfs.guidance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class EnglishGuidanceFormatterTest {

	private final EnglishGuidanceFormatter f = new EnglishGuidanceFormatter(ZoneId.of("America/New_York"));

	private static GuidanceEvent ev(GuidanceEventType type, Object... args) {
		return new GuidanceEvent(1, type, 0, GuidanceState.WAIT_FOR_VEHICLE, null, 0, -1, -1, -1, -1, null, null,
				Arrays.asList(args));
	}

	@Test
	public void silentEventsHaveNoText() {
		assertNull(f.format(ev(GuidanceEventType.STATE_CHANGED)));
		assertNull(f.format(ev(GuidanceEventType.RIDE_ENDED)));
	}

	@Test
	public void boardWithAndWithoutHeadsign() {
		long dep = java.time.ZonedDateTime.of(2026, 11, 17, 8, 14, 0, 0, ZoneId.of("America/New_York")).toEpochSecond();
		assertEquals("Board the Q toward Coney Island at Times Sq, departs 08:14",
				f.format(ev(GuidanceEventType.BOARD_PROMPT, "Q", "Coney Island", "Times Sq", dep)));
		assertEquals("Board the Q at Times Sq, departs 08:14",
				f.format(ev(GuidanceEventType.BOARD_PROMPT, "Q", null, "Times Sq", dep)));
		assertEquals("Board the Q at Times Sq, departs 08:14",
				f.format(ev(GuidanceEventType.BOARD_PROMPT, "Q", "", "Times Sq", dep)));
	}

	@Test
	public void singularStop() {
		assertEquals("Next stop: A. 1 stop remaining", f.format(ev(GuidanceEventType.NEXT_STOP, "A", 1)));
		assertEquals("Get off at Z, 1 stop", f.format(ev(GuidanceEventType.RIDE_STARTED, "Z", 1, 0L)));
		assertEquals("Get off at Z, 7 stops", f.format(ev(GuidanceEventType.RIDE_STARTED, "Z", 7, 0L)));
	}

	@Test
	public void timesUseZoneAndWrapPastMidnight() {
		long late = java.time.ZonedDateTime.of(2026, 11, 17, 23, 58, 0, 0, ZoneId.of("America/New_York")).toEpochSecond();
		assertEquals("Transfer to the F at Jay St, departs 00:03",
				f.format(ev(GuidanceEventType.TRANSFER_INSTRUCTION, "F", null, "Jay St", late + 300)));
		assertEquals("Transfer to the F at Jay St, departs 23:58",
				f.format(ev(GuidanceEventType.TRANSFER_INSTRUCTION, "F", null, "Jay St", late)));
	}

	@Test
	public void otherSentences() {
		assertEquals("Replan from now?", f.format(ev(GuidanceEventType.REPLAN_REQUESTED)));
		assertEquals("Guidance cancelled", f.format(ev(GuidanceEventType.CANCELLED, "cancel")));
		assertEquals("Planning a new trip", f.format(ev(GuidanceEventType.CANCELLED, "replan")));
		assertEquals("Walk to your destination",
				f.format(ev(GuidanceEventType.WALK_LEG_STARTED, new Object[] { null })));
		assertEquals(Collections.emptyList(), ev(GuidanceEventType.ARRIVED).getArgs());
	}
}
