package net.osmand.plus.quickaction.actions;

import net.osmand.plus.settings.backend.OsmandSettings;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DisplayPositionActionTest {

	@Test
	public void testGetNextPlacement_cyclesThroughDisplayPositionStates() {
		assertEquals(
				OsmandSettings.POSITION_PLACEMENT_CENTER,
				DisplayPositionAction.getNextPlacement(OsmandSettings.POSITION_PLACEMENT_AUTOMATIC));
		assertEquals(
				OsmandSettings.POSITION_PLACEMENT_BOTTOM,
				DisplayPositionAction.getNextPlacement(OsmandSettings.POSITION_PLACEMENT_CENTER));
		assertEquals(
				OsmandSettings.POSITION_PLACEMENT_AUTOMATIC,
				DisplayPositionAction.getNextPlacement(OsmandSettings.POSITION_PLACEMENT_BOTTOM));
	}
}
