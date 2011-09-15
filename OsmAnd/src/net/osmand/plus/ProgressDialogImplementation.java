package net.osmand.plus;

import net.osmand.IProgress;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Handler;
import android.os.Message;

public class ProgressDialogImplementation implements IProgress {
	
	private String taskName;
	private int progress;
	private int work;
	private String message = ""; //$NON-NLS-1$
	
	private Handler mViewUpdateHandler;
	private Thread run;
	private Context context;
	private ProgressDialog dialog = null;
	private final boolean cancelable;
	

	public ProgressDialogImplementation(Context ctx, ProgressDialog dlg, boolean cancelable){
		this.cancelable = cancelable;
		context = ctx;
		setDialog(dlg);
		
		mViewUpdateHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				if(dialog != null){
					dialog.setMessage(message);
					if (isIndeterminate()) {
						dialog.setIndeterminate(true);
					} else {
						dialog.setIndeterminate(false);
						dialog.setMax(work);
					}
					dialog.show();
				}
			}
		};	
	}
		
	public ProgressDialogImplementation(ProgressDialog dlg, boolean cancelable){
		this(dlg.getContext(), dlg, cancelable);
	}
	
	
	public ProgressDialogImplementation(final ProgressDialog dlg){
		this(dlg, false);
	}
	
	public void setDialog(ProgressDialog dlg){
		if(dlg != null){
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
			this.dialog = dlg;
		}
		
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
		this.progress += deltaWork;
		if (!isIndeterminate() && dialog != null) {
			dialog.setProgress(this.progress);
		}
	}
	
	@Override
	public void remaining(int remainingWork) {
		this.progress = work - remainingWork;
		if (!isIndeterminate() && dialog != null) {
			dialog.setProgress(this.progress);
		}
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
		this.taskName = taskName;
		startWork(work);
		mViewUpdateHandler.sendEmptyMessage(0);
	}

	@Override
	public void finishTask() {
		work = -1;
		progress = 0;
		if (taskName != null) {
			message = context.getResources().getString(R.string.finished_task) +" : "+ taskName; //$NON-NLS-1$
			mViewUpdateHandler.sendEmptyMessage(0);
		}
	}
	@Override
	public boolean isInterrupted() {
		return false;
	}
	
	public ProgressDialog getDialog() {
		return dialog;
	}

	

	@Override
	public void startWork(int work) {
		this.work = work;
		if (this.work == 0) {
			this.work = 1;
		}
		progress = 0;
	}

	@Override
	public void setGeneralProgress(String genProgress) {
		// not implemented yet
		
	}

}
