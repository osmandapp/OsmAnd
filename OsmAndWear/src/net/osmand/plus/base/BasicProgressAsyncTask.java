package net.osmand.plus.base;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import net.osmand.IProgress;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public abstract class BasicProgressAsyncTask<Tag, Params, Progress, Result> extends AsyncTask<Params, Progress, Result> implements IProgress {

	protected String taskName;
	protected String message = ""; //$NON-NLS-1$
	protected OsmandApplication ctx;
	protected boolean interrupted;
	protected Tag tag;
	private Handler uiHandler;
	private final ProgressHelper progressHelper;

	public BasicProgressAsyncTask(OsmandApplication app) {
		this.ctx = app;
		progressHelper = new ProgressHelper(() -> updateProgress(true));
	}

	public String getDescription() {
		return message;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		uiHandler = new Handler();
		this.interrupted = false;
	}

	@Override
	public void startTask(String taskName, int work) {
		if (taskName == null) {
			taskName = ""; //$NON-NLS-1$
		}
		message = taskName;
		this.taskName = taskName;
		startWork(work);
		updateProgress(false);
	}

	protected abstract void updateProgress(boolean updateOnlyProgress, Tag tag);

	@Override
	public void startWork(int work) {
		progressHelper.onStartWork(work);
	}

	@Override
	public void progress(int deltaWork) {
		progressHelper.onProgress(deltaWork);
	}

	private void updateProgress(boolean updateOnlyProgress) {
		if (uiHandler != null && (!uiHandler.hasMessages(1) || !updateOnlyProgress)) {
			Message msg = Message.obtain(uiHandler, () -> updateProgress(updateOnlyProgress, tag));
			msg.what = OsmAndConstants.UI_HANDLER_PROGRESS + 2;
			uiHandler.sendMessage(msg);
		}
	}

	@Override
	public void remaining(int remainingWork) {
		int newProgress = progressHelper.getTotalWork() - remainingWork;
		progress(newProgress - progressHelper.getLastKnownProgress());
	}

	@Override
	public void finishTask() {
		progressHelper.onFinishTask();
		if (taskName != null) {
			message = ctx.getResources().getString(R.string.finished_task) + ": " + taskName; //$NON-NLS-1$
			updateProgress(false);
		}
	}

	@Override
	public boolean isIndeterminate() {
		return progressHelper.isIndeterminate();
	}

	public float getDownloadProgress() {
		return progressHelper.getLastKnownPercent();
	}

	public void setInterrupted(boolean interrupted) {
		this.interrupted = interrupted;
	}

	@Override
	public boolean isInterrupted() {
		return interrupted;
	}

	protected void setTag(Tag tag) {
		this.tag = tag;
	}

	public Tag getTag() {
		return tag;
	}

	@Override
	public void setGeneralProgress(String genProgress) {
	}
}