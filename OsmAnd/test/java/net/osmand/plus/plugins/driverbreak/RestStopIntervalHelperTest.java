package net.osmand.plus.plugins.driverbreak;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for {@link RestStopIntervalHelper}.
 */
@RunWith(AndroidJUnit4.class)
public class RestStopIntervalHelperTest {

	@Test
	public void hikingMainStageOnlyPlacesStopsAtMainInterval() {
		List<Double> distances = RestStopIntervalHelper.hikingOrCyclingDistancesM(
				25000.0, 11.295, 2.275, false, 40.0);
		Assert.assertEquals(Arrays.asList(11295.0, 22590.0), distances);
	}

	@Test
	public void hikingWithAlternativeIncludesShorterIntervals() {
		List<Double> distances = RestStopIntervalHelper.hikingOrCyclingDistancesM(
				12000.0, 11.295, 2.275, true, 40.0);
		Assert.assertTrue(distances.contains(2275.0));
		Assert.assertTrue(distances.contains(4550.0));
		Assert.assertTrue(distances.contains(6825.0));
		Assert.assertTrue(distances.contains(9100.0));
		Assert.assertTrue(distances.contains(11295.0));
	}

	@Test
	public void dailyCapRemovesStopsBeyondDaySegment() {
		List<Double> merged = RestStopIntervalHelper.mergeIntervalTargets(50000.0, 11295.0, 2275.0);
		List<Double> capped = RestStopIntervalHelper.applyDailyCap(merged, 40000.0);
		for (double distanceM : capped) {
			double dayStartM = Math.floor(distanceM / 40000.0) * 40000.0;
			Assert.assertTrue(distanceM - dayStartM <= 40000.0 + 0.01);
		}
	}

	@Test
	public void shortRouteHasNoStops() {
		List<Double> distances = RestStopIntervalHelper.hikingOrCyclingDistancesM(
				5000.0, 11.295, 2.275, true, 40.0);
		Assert.assertTrue(distances.isEmpty());
	}

	@Test
	public void disabledAlternativeMatchesMainOnly() {
		List<Double> withAlt = RestStopIntervalHelper.hikingOrCyclingDistancesM(
				25000.0, 11.295, 2.275, true, 40.0);
		List<Double> withoutAlt = RestStopIntervalHelper.hikingOrCyclingDistancesM(
				25000.0, 11.295, 2.275, false, 40.0);
		Assert.assertTrue(withAlt.size() > withoutAlt.size());
		Assert.assertEquals(Collections.singletonList(11295.0), withoutAlt.subList(0, 1));
	}

	@Test
	public void cyclingAlternativePlacesStopsAtAltInterval() {
		List<Double> distances = RestStopIntervalHelper.hikingOrCyclingDistancesM(
				30000.0, 28.24, 5.69, true, 100.0);
		Assert.assertTrue(distances.contains(5690.0));
		Assert.assertTrue(distances.contains(11380.0));
		Assert.assertTrue(distances.contains(17070.0));
	}

	@Test
	public void carBreakEveryFourHoursOfDrivingTime() {
		List<RestStopIntervalHelper.RouteSegmentSample> samples = Collections.singletonList(
				new RestStopIntervalHelper.RouteSegmentSample(500_000, 18_000f));
		List<Double> distances = RestStopIntervalHelper.drivingBreakDistancesFromSamples(
				samples, 4 * 3600.0, 0);
		Assert.assertEquals(1, distances.size());
		Assert.assertEquals(400_000.0, distances.get(0), 100.0);
	}

	@Test
	public void shortDrivingRouteHasNoBreaks() {
		List<RestStopIntervalHelper.RouteSegmentSample> samples = Collections.singletonList(
				new RestStopIntervalHelper.RouteSegmentSample(100_000, 3600f));
		List<Double> distances = RestStopIntervalHelper.drivingBreakDistancesFromSamples(
				samples, 4 * 3600.0, 0);
		Assert.assertTrue(distances.isEmpty());
	}

	@Test
	public void truckBreakUsesSegmentTiming() {
		List<RestStopIntervalHelper.RouteSegmentSample> samples = Arrays.asList(
				new RestStopIntervalHelper.RouteSegmentSample(200_000, 7200f),
				new RestStopIntervalHelper.RouteSegmentSample(200_000, 7200f));
		List<Double> distances = RestStopIntervalHelper.drivingBreakDistancesFromSamples(
				samples, 4 * 3600.0, 0);
		Assert.assertEquals(1, distances.size());
		Assert.assertEquals(400_000.0, distances.get(0), 1.0);
	}
}
