package net.osmand.plus.plugins.aprs;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class AprsSyntheticGroundTruthTest {

	@Test
	public void parseAllSyntheticStations() throws Exception {
		InputStream in = getClass().getResourceAsStream("/synthetic_gjoevik_5stations_120s.json");
		Assert.assertNotNull("Copy iq-file/synthetic_gjoevik_5stations_120s.json to test/resources", in);
		String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		JSONArray arr = new JSONArray(json);
		AprsPacketParser parser = new AprsPacketParser();
		Set<String> callsigns = new HashSet<>();
		boolean qrvFound = false;
		boolean weatherFound = false;
		for (int i = 0; i < arr.length(); i++) {
			JSONObject o = arr.getJSONObject(i);
			String call = o.getString("callsign");
			String payload = o.getString("payload");
			AprsStation s = parser.parseAprsPayload(call, payload);
			Assert.assertNotNull("Failed for " + call + " payload " + payload, s);
			Assert.assertEquals(call, s.getCallsign());
			Assert.assertTrue(s.hasPosition());
			callsigns.add(call);
			if (o.optBoolean("qrv")) {
				Assert.assertTrue(s.getQrvFrequencyMhz() > 0);
				qrvFound = true;
			}
			if (o.optBoolean("weather")) {
				Assert.assertTrue(s.isWeatherStation());
				weatherFound = true;
			}
		}
		Assert.assertEquals(5, callsigns.size());
		Assert.assertTrue(qrvFound);
		Assert.assertTrue(weatherFound);
	}
}
