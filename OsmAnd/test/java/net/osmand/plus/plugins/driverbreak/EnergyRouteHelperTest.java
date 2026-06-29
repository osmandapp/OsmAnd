package net.osmand.plus.plugins.driverbreak;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class EnergyRouteHelperTest {

	@Test
	public void isCheaperAlternativeAcceptsFivePercentSavingWithinTwentyPercentDistance() {
		Assert.assertTrue(EnergyRouteHelper.isCheaperAlternative(1000.0, 940.0, 10000.0, 11500.0));
	}

	@Test
	public void isCheaperAlternativeRejectsInsufficientEnergySaving() {
		Assert.assertFalse(EnergyRouteHelper.isCheaperAlternative(1000.0, 960.0, 10000.0, 11000.0));
	}

	@Test
	public void isCheaperAlternativeRejectsExcessiveDistanceIncrease() {
		Assert.assertFalse(EnergyRouteHelper.isCheaperAlternative(1000.0, 900.0, 10000.0, 12500.0));
	}

	@Test
	public void samplingTriggeredWhenSegmentCountExceedsLimit() {
		Assert.assertTrue(EnergyRouteHelper.usesSegmentSampling(500));
		Assert.assertFalse(EnergyRouteHelper.usesSegmentSampling(EnergyRouteHelper.maxEnergySampleSegments()));
	}

	@Test
	public void maxEnergySampleSegmentsIs256() {
		Assert.assertEquals(256, EnergyRouteHelper.maxEnergySampleSegments());
	}
}
