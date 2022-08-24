package net.osmand.plus.base;

import androidx.annotation.NonNull;

public class ProgressHelper {

	private final static int INDETERMINATE = -1;
	private final OnUpdateProgress progressUiAdapter;

	private int totalWork = INDETERMINATE;
	private int progress;
	private int deltaProgress;
	private int sizeInterval = INDETERMINATE;
	private int timeInterval = INDETERMINATE;
	private long lastUpdateTime = INDETERMINATE;

	public ProgressHelper(@NonNull OnUpdateProgress progressUiAdapter) {
		this.progressUiAdapter = progressUiAdapter;
	}

	public void setMinimumSizeInterval(int proposedValue) {
		if (sizeInterval != INDETERMINATE) {
			setSizeInterval(Math.min(proposedValue, sizeInterval));
		} else {
			setSizeInterval(proposedValue);
		}
	}

	public void setSizeInterval(int minSizeToUpdate) {
		this.sizeInterval = minSizeToUpdate;
	}

	public void setTimeInterval(int minTimeToUpdate) {
		this.timeInterval = minTimeToUpdate;
	}

	public void onStartWork(int total) {
		this.totalWork = total;
		if (this.totalWork == 0) {
			this.totalWork = 1;
		}
		// one percent of total work size
		setMinimumSizeInterval(totalWork / 100);
		deltaProgress = 0;
		progress = 0;
	}

	public void onProgress(int deltaWork) {
		if (!isIndeterminate()) {
			deltaProgress += deltaWork;
			if (deltaProgress >= sizeInterval || isDownloadComplete()) {
				progress += deltaProgress;
				if (shouldUpdateUI()) {
					lastUpdateTime = System.currentTimeMillis();
					progressUiAdapter.updateProgress();
				}
				deltaProgress = 0;
			}
		}
	}

	public boolean shouldUpdateUI() {
		if (timeInterval != INDETERMINATE) {
			long now = System.currentTimeMillis();
			return (now - lastUpdateTime) > timeInterval;
		}
		return true;
	}

	public void onFinishTask() {
		totalWork = INDETERMINATE;
		progress = 0;
	}

	public int getLastKnownDeltaProgress() {
		return deltaProgress;
	}

	public int getLastKnownProgress() {
		return progress;
	}

	public float getDownloadProgress() {
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
		return (int) Math.floor(normalizeProgress(progress));
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

	public interface OnUpdateProgress {
		void updateProgress();
	}

}
