package net.osmand.plus.plugins.driverbreak;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class EnergyRouteAlternativeExtractorTest {

	@Test
	public void collectRouteVariantsReturnsEmptyForMissingRoute() {
		Assert.assertTrue(EnergyRouteAlternativeExtractor.collectRouteVariants(
				new net.osmand.plus.routing.RouteCalculationResult("error")).isEmpty());
	}
}
