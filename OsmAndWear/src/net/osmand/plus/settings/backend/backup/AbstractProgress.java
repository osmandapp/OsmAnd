package net.osmand.plus.settings.backend.backup;

import net.osmand.IProgress;

public abstract class AbstractProgress implements IProgress {
	@Override
	public void startTask(String taskName, int work) {
	}

	@Override
	public void startWork(int work) {
	}

	@Override
	public abstract void progress(int deltaWork);

	@Override
	public void remaining(int remainingWork) {
	}

	@Override
	public void finishTask() {
	}

	@Override
	public boolean isIndeterminate() {
		return false;
	}

	@Override
	public boolean isInterrupted() {
		return false;
	}

	@Override
	public void setGeneralProgress(String genProgress) {
	}
}
