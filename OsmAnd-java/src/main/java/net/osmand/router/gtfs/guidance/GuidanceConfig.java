package net.osmand.router.gtfs.guidance;

/** Thresholds of the guidance engine. Immutable; {@link #defaults()} matches design.md section 9. */
public final class GuidanceConfig {

	/** Within this distance of a walk's end a walk is over (default 50 m). */
	public final double arriveRadiusM;
	/** Within this distance of a stop on a ride the vehicle is taken to be there (default 75 m). */
	public final double stopRadiusM;
	/** Farther than this from the path, with a good fix, is off route (default 300 m). */
	public final double offRouteM;
	/** Off route ends when back within this distance (default 250 m; the gap is the hysteresis). */
	public final double offRouteResumeM;
	/** Fixes less accurate than this are treated as no fix (default 100 m). */
	public final double maxAccuracyM;
	/** Seconds after the departure before an unboarded vehicle counts as missed (default 120). */
	public final int graceSec;
	/** A fix older than this does not count as present (default 60 s). */
	public final int fixFreshSec;
	/** Without a fix, a walk is taken as done this long after its scheduled end (default 60 s). */
	public final int walkSlackSec;
	/** Without a fix, the ride is taken as over this long after the alight arrival (default 60 s). */
	public final int alightSlackSec;
	/** Consecutive off-path fixes needed to declare OFF_ROUTE (default 2). */
	public final int offRouteFixes;

	public GuidanceConfig(double arriveRadiusM, double stopRadiusM, double offRouteM, double offRouteResumeM,
	                      double maxAccuracyM, int graceSec, int fixFreshSec, int walkSlackSec, int alightSlackSec,
	                      int offRouteFixes) {
		this.arriveRadiusM = arriveRadiusM;
		this.stopRadiusM = stopRadiusM;
		this.offRouteM = offRouteM;
		this.offRouteResumeM = offRouteResumeM;
		this.maxAccuracyM = maxAccuracyM;
		this.graceSec = graceSec;
		this.fixFreshSec = fixFreshSec;
		this.walkSlackSec = walkSlackSec;
		this.alightSlackSec = alightSlackSec;
		this.offRouteFixes = offRouteFixes;
	}

	public static GuidanceConfig defaults() {
		return new GuidanceConfig(50, 75, 300, 250, 100, 120, 60, 60, 60, 2);
	}

	/** Copy with another grace period. */
	public GuidanceConfig withGraceSec(int grace) {
		return new GuidanceConfig(arriveRadiusM, stopRadiusM, offRouteM, offRouteResumeM, maxAccuracyM, grace,
				fixFreshSec, walkSlackSec, alightSlackSec, offRouteFixes);
	}
}
