package net.osmand.plus.base;

import androidx.annotation.NonNull;

public class ProgressHelper {

	private final static int INDETERMINATE = -1;
	private final OnUpdateProgress progressUiAdapter;

	private int totalWork = INDETERMINATE;
	private int progress;
	private int deltaProgress;
	private int sizeRestriction = INDETERMINATE;
	private int timeRestriction = INDETERMINATE;
	private long lastUpdateTime = INDETERMINATE;

	public ProgressHelper(@NonNull OnUpdateProgress progressUiAdapter) {
		this.progressUiAdapter = progressUiAdapter;
	}

	public void setMinimumSizeRestriction(int proposedValue) {
		if (sizeRestriction != INDETERMINATE) {
			setSizeRestriction(Math.min(proposedValue, sizeRestriction));
		} else {
			setSizeRestriction(proposedValue);
		}
	}

	public void setSizeRestriction(int minSizeToUpdate) {
		this.sizeRestriction = minSizeToUpdate;
	}

	public void setTimeRestriction(int minTimeToUpdate) {
		this.timeRestriction = minTimeToUpdate;
	}

	public void onStartWork(int total) {
		this.totalWork = total;
		if (this.totalWork == 0) {
			this.totalWork = 1;
		}
		// one percent of total work size
		setMinimumSizeRestriction(totalWork / 100);
		deltaProgress = 0;
		progress = 0;
	}

	public void onProgress(int deltaWork) {
		if (!isIndeterminate()) {
			deltaProgress += deltaWork;
			if (deltaProgress >= sizeRestriction || isDownloadComplete()) {
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
		if (timeRestriction != INDETERMINATE) {
			long now = System.currentTimeMillis();
			return (now - lastUpdateTime) > timeRestriction;
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
