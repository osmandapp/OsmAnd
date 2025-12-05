package net.osmand.data;

import com.vividsolutions.jts.util.Assert;
import junit.framework.TestCase;

public class TransportRouteTest extends TestCase {

	public void testParseIntervalTagToSeconds() {
		// https://wiki.openstreetmap.org/wiki/Key:interval
		Assert.equals(5 * 60, TransportRoute.parseIntervalTagToSeconds("5"));
		Assert.equals(42 * 60, TransportRoute.parseIntervalTagToSeconds("42"));
		Assert.equals(15 * 60, TransportRoute.parseIntervalTagToSeconds("00:15"));
		Assert.equals(48 * 3600, TransportRoute.parseIntervalTagToSeconds("48:00"));
		Assert.equals(5 * 60 + 30, TransportRoute.parseIntervalTagToSeconds("00:05:30"));
		Assert.equals(10 * 3600 + 5 * 60 + 30, TransportRoute.parseIntervalTagToSeconds("10:05:30"));
		Assert.equals(0, TransportRoute.parseIntervalTagToSeconds("one:two:three"));
		Assert.equals(0, TransportRoute.parseIntervalTagToSeconds("-1:-2:-3"));
		Assert.equals(0, TransportRoute.parseIntervalTagToSeconds("1:2:3:4"));
		Assert.equals(0, TransportRoute.parseIntervalTagToSeconds(null));
		Assert.equals(0, TransportRoute.parseIntervalTagToSeconds("?"));
		Assert.equals(0, TransportRoute.parseIntervalTagToSeconds(""));
	}

}
