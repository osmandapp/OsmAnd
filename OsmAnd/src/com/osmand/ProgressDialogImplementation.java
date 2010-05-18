package com.osmand;

import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Message;

public class ProgressDialogImplementation implements IProgress {
	
	private static final float deltaToChange = 0.04f;
	private String taskName;
	private int progress;
	private int work;
	private int deltaWork;
	private String message = "";
	
	private Handler mViewUpdateHandler;

	public ProgressDialogImplementation(final ProgressDialog dlg){
		mViewUpdateHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				dlg.setMessage(message);
			}
		};
	}

	@Override
	public void progress(int deltaWork) {
		this.deltaWork += deltaWork;
		if(change(progress + deltaWork)){
			this.progress += deltaWork;
			updateMessage();
		}
	}
	
	private void updateMessage() {
		message = taskName + String.format(" %.1f %%", this.progress * 100f / ((float) this.work));
		mViewUpdateHandler.sendEmptyMessage(0);
	}

	public boolean change(int newProgress) {
		if (newProgress < progress) {
			return false;
		}
		if ((newProgress - progress) / ((float) work) < deltaToChange) {
			return false;
		}
		return true;
	}
	@Override
	public void remaining(int remainingWork) {
		if(change(work - remainingWork)){
			this.progress = work - remainingWork;
			updateMessage();
		}
		deltaWork = work - remainingWork - this.progress;
	}
	
	public boolean isIndeterminate(){
		return work == -1;
	}

	@Override
	public void startTask(String taskName, int work) {
		if(taskName == null){
			taskName = "";
		}
		message = taskName;
		mViewUpdateHandler.sendEmptyMessage(0);
		this.taskName = taskName;
		startWork(work);
	}

	@Override
	public void finishTask() {
		if (taskName != null) {
			message = "Finished : " + taskName;
			mViewUpdateHandler.sendEmptyMessage(0);
		}
		work = -1;
		progress = 0;
	}
	@Override
	public boolean isInterrupted() {
		return false;
	}

	

	@Override
	public void startWork(int work) {
		this.work = work;
		if(this.work == 0){
			this.work = 1;
		}
		progress = 0;
		deltaWork = 0;
	}

}
