package net.osmand.util;

import net.osmand.binary.RouteDataObject;
import org.junit.Assert;
import org.junit.Test;

public class ParseLengthTest {
	@Test
	public void testParseLength() {
		assertTrueLength("10 km", 10000);
		assertTrueLength("0.01 km", 10);
		assertTrueLength("0.01 km 10 m", 20);
		assertTrueLength("10 m", 10);
		assertTrueLength("10m", 10);
		assertTrueLength("3.4 m", 3.4f);
		assertTrueLength("3.40 m", 3.4f);
		assertTrueLength("10 m 10m", 20);
		assertTrueLength("14'10\"", 4.5212f);
		assertTrueLength("14.5'", 4.4196f);
		assertTrueLength("14.5 ft", 4.4196f);
		assertTrueLength("14'0\"", 4.2672f);
		assertTrueLength("15ft", 4.572f);
		assertTrueLength("15 ft 1 in", 4.5974f);
		assertTrueLength("4.1 metres", 4.1f);
		assertTrueLength("14'0''", 4.2672f);
		assertTrueLength("14 feet", 4.2672f);
		assertTrueLength("14 mile", 22530.76f);
		assertTrueLength("14 cm", 0.14f);

// 		float badValue = -1;
// 		assertTrueLength("none", badValue);
// 		assertTrueLength("m 4.1", badValue);
// 		assertTrueLength("1F4 m", badValue);
	}

	private static void assertTrueLength(String vl, float exp) {
		float dest = RouteDataObject.parseLength(vl, 0);
		Assert.assertEquals(exp, dest, 0.00001);
	}
}
