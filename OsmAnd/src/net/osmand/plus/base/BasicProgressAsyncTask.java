package net.osmand.plus.base;

import net.osmand.IProgress;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

public abstract class BasicProgressAsyncTask<Tag, Params, Progress, Result> extends AsyncTask<Params, Progress, Result> implements IProgress {
	protected String taskName;
	protected int progress;
	protected int deltaProgress;
	protected int work;
	protected String message = ""; //$NON-NLS-1$
	protected OsmandApplication ctx;
	protected boolean interrupted = false;
	protected Tag tag;
	private Handler uiHandler;

	public BasicProgressAsyncTask(OsmandApplication app) {
		this.ctx = app;
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

	protected abstract void updateProgress(boolean updateOnlyProgress, 
			Tag tag);

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
					updateProgress(updateOnlyProgress, tag);
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
			message = ctx.getResources().getString(R.string.finished_task) + ": " + taskName; //$NON-NLS-1$
			updProgress(false);
		}
	}

	@Override
	public boolean isIndeterminate() {
		return work == -1;
	}

	public int getProgressPercentage() {
		if (work > 0) {
			return normalizeProgress((progress * 100) / work);
		}
		return normalizeProgress(progress);
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

	private int normalizeProgress(int progress) {
		if (progress < 0) {
			return 0;
		} else if (progress <= 100) {
			return progress;
		} else {
			return 99;
		}
	}
}