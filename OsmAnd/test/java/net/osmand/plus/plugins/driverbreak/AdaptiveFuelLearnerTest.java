package net.osmand.plus.plugins.driverbreak;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for EMA fuel learning formula in {@link AdaptiveFuelLearner}.
 */
@RunWith(AndroidJUnit4.class)
public class AdaptiveFuelLearnerTest {

	@Test
	public void emaFirstSampleEqualsSample() {
		double rate = applyEma(0.0, 10.0);
		Assert.assertEquals(10.0, rate, 1e-6);
	}

	@Test
	public void emaConvergesToSampleValue() {
		double rate = 0.0;
		for (int i = 0; i < 20; i++) {
			rate = applyEma(rate, 8.0);
		}
		Assert.assertEquals(8.0, rate, 8.0 * 0.01);
	}

	@Test
	public void emaThreeStepManualCalculation() {
		double alpha = AdaptiveFuelLearner.EMA_ALPHA;
		double r1 = applyEma(0.0, 10.0);
		double r2 = applyEma(r1, 8.0);
		double r3 = applyEma(r2, 12.0);
		Assert.assertEquals(10.0, r1, 1e-6);
		Assert.assertEquals(alpha * 8.0 + (1.0 - alpha) * 10.0, r2, 1e-6);
		Assert.assertEquals(alpha * 12.0 + (1.0 - alpha) * r2, r3, 1e-6);
	}

	private static double applyEma(double current, double sample) {
		if (current <= 0.0) {
			return sample;
		}
		return AdaptiveFuelLearner.EMA_ALPHA * sample
				+ (1.0 - AdaptiveFuelLearner.EMA_ALPHA) * current;
	}
}
