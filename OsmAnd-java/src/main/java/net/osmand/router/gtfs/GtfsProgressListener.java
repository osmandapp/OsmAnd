package net.osmand.router.gtfs;

/** Progress and cancellation callbacks for the importer and the planner. All methods may be called from the working thread. */
public interface GtfsProgressListener {

	/** Stage name for display and percentage of the whole job (0..100). */
	void onProgress(String stage, int percent);

	boolean isCancelled();
}
