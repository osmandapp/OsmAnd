package net.osmand.plus.measurementtool;

interface SnapToRoadProgressListener {

	void showProgressBar();

	void updateProgress(int progress);

	void hideProgressBar();

	void refresh();
}
