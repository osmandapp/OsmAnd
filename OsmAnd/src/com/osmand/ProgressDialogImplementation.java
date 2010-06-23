package com.osmand;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Handler;
import android.os.Message;

public class ProgressDialogImplementation implements IProgress {
	
	private static final float deltaToChange = 0.04f;
	private String taskName;
	private int progress;
	private int work;
	private int deltaWork;
	private String message = ""; //$NON-NLS-1$
	
	private Handler mViewUpdateHandler;
	private Thread run;
	private Context context;

	public ProgressDialogImplementation(final ProgressDialog dlg, boolean cancelable){
		context = dlg.getContext();
		if(cancelable){
			dlg.setOnCancelListener(new OnCancelListener(){
				@Override
				public void onCancel(DialogInterface dialog) {
					if(run != null){
						run.stop();
					}
					
				}
			});
		}
		mViewUpdateHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				dlg.setMessage(message);
			}
		};	
	}
	
	public ProgressDialogImplementation(final ProgressDialog dlg){
		this(dlg, false);
	}
	
	public void setRunnable(String threadName, Runnable run){
		this.run = new Thread(run, threadName);
	}
	
	public void run(){
		if(run == null){
			throw new IllegalStateException();
		}
		run.start();
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
		message = taskName + String.format(" %.1f %%", this.progress * 100f / ((float) this.work)); //$NON-NLS-1$
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
			taskName = ""; //$NON-NLS-1$
		}
		message = taskName;
		mViewUpdateHandler.sendEmptyMessage(0);
		this.taskName = taskName;
		startWork(work);
	}

	@Override
	public void finishTask() {
		if (taskName != null) {
			message = context.getResources().getString(R.string.finished_task) + taskName;
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

	@Override
	public void setGeneralProgress(String genProgress) {
		// not implemented yet
		
	}

}
