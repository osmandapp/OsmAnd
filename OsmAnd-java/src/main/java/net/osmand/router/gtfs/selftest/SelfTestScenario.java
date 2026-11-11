package net.osmand.router.gtfs.selftest;

/** One named check of the GTFS code. Scenarios must not touch any UI. */
public interface SelfTestScenario {

	/** Runs the scenario; failed expectations go into the result, unexpected throwables are caught by the runner. */
	void run(SelfTestEnv env, ScenarioResult result) throws Exception;
}
