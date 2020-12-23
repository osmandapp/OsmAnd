package net.osmand.plus.routing;

public interface RouteCalculationProgressCallback {

	void start();

	void updateProgress(int progress);

	void requestPrivateAccessRouting();

	void finish();
}
