package net.osmand.plus.base;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import net.osmand.IProgress;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public abstract class BasicProgressAsyncTask<Tag, Params, Progress, Result> extends AsyncTask<Params, Progress, Result> implements IProgress {

	public static final int UPDATE_TIME_INTERVAL_MS = 500;
	public static final int UPDATE_SIZE_INTERVAL_KB = 300;

	protected String taskName;
	protected String message = ""; //$NON-NLS-1$
	protected OsmandApplication ctx;
	protected boolean interrupted;
	protected Tag tag;
	private Handler uiHandler;
	private ProgressHelper progress;

	public BasicProgressAsyncTask(OsmandApplication app) {
		this.ctx = app;
		progress = new ProgressHelper(() -> updProgress(true));
		progress.setSizeRestriction(UPDATE_SIZE_INTERVAL_KB);
		progress.setTimeRestriction(UPDATE_TIME_INTERVAL_MS);
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
		updProgress(false);
	}

	protected abstract void updateProgress(boolean updateOnlyProgress, Tag tag);

	@Override
	public void startWork(int work) {
		progress.onStartWork(work);
	}

	@Override
	public void progress(int deltaWork) {
		progress.onProgress(deltaWork);
	}
	
	private void updProgress(boolean updateOnlyProgress) {
		if(uiHandler != null && (!uiHandler.hasMessages(1) || !updateOnlyProgress)) {
			Message msg = Message.obtain(uiHandler, () -> updateProgress(updateOnlyProgress, tag));
			msg.what = OsmAndConstants.UI_HANDLER_PROGRESS + 2;
			uiHandler.sendMessage(msg);
		}
	}

	

	@Override
	public void remaining(int remainingWork) {
		int newProgress = progress.getTotalWork() - remainingWork;
		progress(newProgress - progress.getLastKnownProgress());
	}

	@Override
	public void finishTask() {
		progress.onFinishTask();
		if (taskName != null) {
			message = ctx.getResources().getString(R.string.finished_task) + ": " + taskName; //$NON-NLS-1$
			updProgress(false);
		}
	}

	@Override
	public boolean isIndeterminate() {
		return progress.isIndeterminate();
	}

	public float getDownloadProgress() {
		return progress.getDownloadProgress();
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
	public void setGeneralProgress(String genProgress) {}

}