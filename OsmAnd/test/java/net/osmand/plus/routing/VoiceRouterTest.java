package net.osmand.plus.routing;

import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.osmand.plus.routing.VoiceRouter.VoiceMessageListener;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.test.common.AndroidTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class VoiceRouterTest extends AndroidTest {

	@Test
	public void silentRecalculationPlaysNoCommand() {
		VoiceRouter voiceRouter = app.getRoutingHelper().getVoiceRouter();
		OsmandPreference<Boolean> speakRecalculation = settings.SPEAK_ROUTE_RECALCULATION;
		boolean speak = speakRecalculation.get();
		List<List<String>> messages = new ArrayList<>();
		VoiceMessageListener listener = (listCommands, played) -> messages.add(listCommands);
		voiceRouter.addVoiceMessageListener(listener);
		try {
			speakRecalculation.set(false);
			// called from the route calculation thread in the app, not the main one
			voiceRouter.newRouteIsCalculated(false);
			assertTrue("Silent recalculation played " + messages, messages.isEmpty());
		} finally {
			voiceRouter.removeVoiceMessageListener(listener);
			speakRecalculation.set(speak);
		}
	}
}
