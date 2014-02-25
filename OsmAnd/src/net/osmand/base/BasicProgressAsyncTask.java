package net.osmand.base;

import net.osmand.IProgress;
import net.osmand.OsmAndConstants;
import net.osmand.plus.R;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

public abstract class BasicProgressAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> implements IProgress {
	protected String taskName;
	protected int progress;
	protected int deltaProgress;
	protected int work;
	protected String message = ""; //$NON-NLS-1$
	protected Context ctx;
	protected boolean interrupted = false;
	private Handler uiHandler;

	public BasicProgressAsyncTask(Context ctx) {
		this.ctx = ctx;
		this.work = -1;
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

	protected abstract void updateProgress(boolean updateOnlyProgress);

	@Override
	public void startWork(int work) {
		this.work = work;
		if (this.work == 0) {
			this.work = 1;
		}
		progress = 0;
		deltaProgress = 0;
	}

	@Override
	public void progress(int deltaWork) {
		if (!isIndeterminate()) {
			this.deltaProgress += deltaWork;
			// update only each percent
			if ((deltaProgress > (work / 100)) || ((progress + deltaProgress) >= work)) {
				this.progress += deltaProgress;
				this.deltaProgress = 0;
				updProgress(true);
			}
		}
	}
	
	private void updProgress(final boolean updateOnlyProgress) {
		if(uiHandler != null && (!uiHandler.hasMessages(1) || !updateOnlyProgress)) {
			Message msg = Message.obtain(uiHandler, new Runnable() {
				@Override
				public void run() {
					updateProgress(updateOnlyProgress);
				}
			});
			msg.what = OsmAndConstants.UI_HANDLER_PROGRESS + 2;
			uiHandler.sendMessage(msg);
		}
	}

	

	@Override
	public void remaining(int remainingWork) {
		int newprogress = work - remainingWork;
		progress(newprogress - this.progress);
	}

	@Override
	public void finishTask() {
		work = -1;
		progress = 0;
		if (taskName != null) {
			message = ctx.getResources().getString(R.string.finished_task) + " : " + taskName; //$NON-NLS-1$
			updProgress(false);
		}
	}

	@Override
	public boolean isIndeterminate() {
		return work == -1;
	}

	public int getProgressPercentage() {
		if (work > 0) {
			return (progress * 100) / work;
		}
		return progress;
	}

	public void setInterrupted(boolean interrupted) {
		this.interrupted = interrupted;
	}

	@Override
	public boolean isInterrupted() {
		return interrupted;
	}

}