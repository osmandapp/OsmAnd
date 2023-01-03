package net.osmand.plus.base;

import androidx.annotation.NonNull;

public class ProgressHelper {

	private static final int UPDATE_TIME_INTERVAL_MS = 500;
	private static final int UPDATE_SIZE_INTERVAL_KB = 300;

	private static final int INDETERMINATE = -1;

	private final UpdateProgressListener updateProgressListener;

	private int totalWork = INDETERMINATE;
	private int progress;
	private int deltaProgress;
	private int lastAddedDeltaProgress;
	private int sizeInterval = UPDATE_SIZE_INTERVAL_KB;
	private int timeInterval = UPDATE_TIME_INTERVAL_MS;
	private long lastUpdateTime;
	private boolean notifyOnUpdate;

	public ProgressHelper(@NonNull UpdateProgressListener updateProgressListener) {
		this.updateProgressListener = updateProgressListener;
	}

	public void setMinimumSizeInterval(int proposedValue) {
		if (sizeInterval != INDETERMINATE) {
			setSizeInterval(Math.min(proposedValue, sizeInterval));
		} else {
			setSizeInterval(proposedValue);
		}
	}

	private void setSizeInterval(int minSizeToUpdate) {
		this.sizeInterval = minSizeToUpdate;
	}

	public void setTimeInterval(int timeInterval) {
		this.timeInterval = timeInterval;
	}

	public void onStartWork(int total) {
		this.totalWork = total;
		if (this.totalWork == 0) {
			this.totalWork = 1;
		}
		int onePercent = totalWork / 100;
		setMinimumSizeInterval(onePercent);
		deltaProgress = 0;
		progress = 0;
	}

	public void onProgress(int deltaWork) {
		if (!isIndeterminate()) {
			deltaProgress += deltaWork;
			if (deltaProgress >= sizeInterval || isDownloadComplete()) {
				progress += deltaProgress;
				lastAddedDeltaProgress += deltaProgress;
				deltaProgress = 0;
				notifyOnUpdate = true;
			}
			if (notifyOnUpdate && isTimeToUpdate()) {
				notifyOnUpdate = false;
				lastUpdateTime = System.currentTimeMillis();
				updateProgressListener.onProgressUpdated();
				lastAddedDeltaProgress = 0;
			}
		}
	}

	public boolean isTimeToUpdate() {
		return System.currentTimeMillis() - lastUpdateTime > timeInterval;
	}

	public void onFinishTask() {
		totalWork = INDETERMINATE;
		progress = 0;
	}

	public int getLastAddedDeltaProgress() {
		return lastAddedDeltaProgress;
	}

	public int getLastKnownProgress() {
		return progress;
	}

	public float getLastKnownPercent() {
		return normalizeProgress(totalWork > 0 ? (float) (progress * 100) / totalWork : progress);
	}

	public boolean isIndeterminate() {
		return totalWork == INDETERMINATE;
	}

	public boolean isDownloadComplete() {
		return (progress + deltaProgress) >= totalWork;
	}

	public int getTotalWork() {
		return totalWork;
	}

	public static int normalizeProgressPercent(int progress) {
		return (int) normalizeProgress(progress);
	}

	public static float normalizeProgress(float progress) {
		if (progress < 0) {
			return 0;
		} else if (progress <= 100) {
			return progress;
		} else {
			return 99.9f;
		}
	}

	public interface UpdateProgressListener {
		void onProgressUpdated();
	}
}
