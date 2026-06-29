package net.osmand.plus.plugins.driverbreak;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for {@link RouteValidator} static classification helpers.
 */
@RunWith(AndroidJUnit4.class)
public class RouteValidatorTest {

	@Test
	public void forbiddenHighwayDetection() {
		Assert.assertTrue(RouteValidator.isForbiddenHighway("motorway"));
		Assert.assertTrue(RouteValidator.isForbiddenHighway("primary_link"));
		Assert.assertFalse(RouteValidator.isForbiddenHighway("cycleway"));
	}

	@Test
	public void mtbScaleParsing() {
		Assert.assertEquals(4, RouteValidator.parseMtbScaleDigit("4"));
		Assert.assertEquals(3, RouteValidator.parseMtbScaleDigit("3+"));
		Assert.assertEquals(-1, RouteValidator.parseMtbScaleDigit(null));
	}
}
