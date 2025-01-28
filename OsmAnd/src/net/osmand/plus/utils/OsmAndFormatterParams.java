package net.osmand.plus.utils;

public class OsmAndFormatterParams {

	public static final boolean DEFAULT_FORCE_TRAILING = true;
	public static final int DEFAULT_EXTRA_DECIMAL_PRECISION = 1;
	boolean forceTrailingZerosInDecimalMainUnit = DEFAULT_FORCE_TRAILING;
	boolean forcePreciseValue = false;
	int extraDecimalPrecision = DEFAULT_EXTRA_DECIMAL_PRECISION;

	public static final OsmAndFormatterParams USE_LOWER_BOUNDS = useLowerBoundParam();
	public static final OsmAndFormatterParams NO_TRAILING_ZEROS = new OsmAndFormatterParams().setTrailingZerosForMainUnit(false);
	public static final OsmAndFormatterParams DEFAULT = new OsmAndFormatterParams();

	boolean useLowerBound = false;

	public boolean isUseLowerBound() {
		return useLowerBound;
	}

	private static OsmAndFormatterParams useLowerBoundParam() {
		OsmAndFormatterParams p = new OsmAndFormatterParams();
		p.useLowerBound = true;
		p.extraDecimalPrecision = 0;
		p.forceTrailingZerosInDecimalMainUnit = true;
		return p;
	}

	private OsmAndFormatterParams setTrailingZerosForMainUnit(boolean forceTrailingZeros) {
		this.forceTrailingZerosInDecimalMainUnit = forceTrailingZeros;
		return this;
	}

	public void setForcePreciseValue(boolean forceDecimalPlaces) {
		this.forcePreciseValue = forceDecimalPlaces;
	}

	public void setExtraDecimalPrecision(int extraDecimalPrecision) {
		this.extraDecimalPrecision = extraDecimalPrecision;
	}

}
