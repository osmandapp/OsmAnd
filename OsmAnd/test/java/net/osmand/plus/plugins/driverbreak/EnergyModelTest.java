package net.osmand.plus.plugins.driverbreak;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for {@link EnergyModel} against hand-calculated values from driver_break_energy.c constants.
 */
@RunWith(AndroidJUnit4.class)
public class EnergyModelTest {

	private static final double TOLERANCE_REL = 0.0001;

	private final EnergyModel model = new EnergyModel();
	private final EnergyParams params = EnergyParams.fromDriverBreakDefaults();

	@Test
	public void flatRoadSegmentCostMatchesHandCalculation() {
		double speedMs = 30.0 / 3.6;
		double fRoll = EnergyModel.CRR * params.getTotalMassKg() * EnergyModel.G_MS2;
		double fAirCoeff = EnergyModel.airCoefficientFromDrag(params.getDragCd(), params.getFrontalAreaM2());
		double ecorr = 0.0001375 * (0.0 - 100.0);
		double fAirAdj = fAirCoeff * (1.0 - ecorr);
		double fAirForce = fAirAdj * speedMs * speedMs;
		double expected = (fRoll + fAirForce) * 1000.0;

		double actual = model.segmentCost(1000.0, 0.0, 0.0, speedMs, params);
		assertRelativeEquals(expected, actual);
	}

	@Test
	public void uphillGradeIncreasesCost() {
		double flat = model.segmentCost(1000.0, 0.0, 0.0, 30.0 / 3.6, params);
		double uphill = model.segmentCost(1000.0, 50.0, 0.0, 30.0 / 3.6, params);
		Assert.assertTrue("Uphill cost must exceed flat road cost", uphill > flat);
	}

	@Test
	public void downhillRecuperationReducesCostVersusPureGravity() {
		double speedMs = 30.0 / 3.6;
		double flat = model.segmentCost(1000.0, 0.0, 0.0, speedMs, params);
		double downhill = model.segmentCost(1000.0, -50.0, 0.0, speedMs, params);
		Assert.assertTrue("Downhill with recuperation must cost less than flat", downhill < flat);
		EnergyParams noRecupParams = new EnergyParams(80.0, 0.30, 2.2, 0.0, 0.0);
		double downhillNoRecup = model.segmentCost(1000.0, -50.0, 0.0, speedMs, noRecupParams);
		Assert.assertTrue("Navit model: recuperation offsets part of gravity assist, raising cost vs no recup",
				downhill > downhillNoRecup);
	}

	@Test
	public void higherElevationReducesDragCost() {
		double speedMs = 30.0 / 3.6;
		double seaLevel = model.segmentCost(1000.0, 0.0, 0.0, speedMs, params);
		double at2000 = model.segmentCost(1000.0, 0.0, 2000.0, speedMs, params);
		Assert.assertTrue(at2000 < seaLevel);
		double expectedSea = expectedFlatCostAtElevation(0.0, speedMs);
		double expected2000 = expectedFlatCostAtElevation(2000.0, speedMs);
		assertRelativeEquals(expected2000 / expectedSea, at2000 / seaLevel);
	}

	@Test
	public void zeroDistanceReturnsZero() {
		Assert.assertEquals(0.0, model.segmentCost(0.0, 0.0, 0.0, 10.0, params), 0.0);
	}

	@Test
	public void zeroSpeedUsesRollingResistanceOnly() {
		double expected = EnergyModel.CRR * params.getTotalMassKg() * EnergyModel.G_MS2 * 500.0;
		Assert.assertEquals(expected, model.segmentCost(500.0, 0.0, 0.0, 0.0, params), expected * TOLERANCE_REL);
	}

	private double expectedFlatCostAtElevation(double elevationM, double speedMs) {
		double fRoll = EnergyModel.rollingForce(params);
		double ecorr = 0.0001375 * (elevationM - 100.0);
		double fAirAdj = EnergyModel.airCoefficientFromDrag(params.getDragCd(), params.getFrontalAreaM2())
				* (1.0 - ecorr);
		return (fRoll + fAirAdj * speedMs * speedMs) * 1000.0;
	}

	private static void assertRelativeEquals(double expected, double actual) {
		double denom = Math.max(Math.abs(expected), 1.0);
		Assert.assertEquals("relative error", 0.0, Math.abs(expected - actual) / denom, TOLERANCE_REL);
	}
}
