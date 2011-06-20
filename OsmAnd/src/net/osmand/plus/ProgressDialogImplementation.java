package net.osmand.plus;

import net.osmand.IProgress;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Handler;
import android.os.Message;

public class ProgressDialogImplementation implements IProgress {
	
	private static final float deltaToChange = 0.023f;
	private String taskName;
	private int progress;
	private int work;
	private int deltaWork;
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
		this.deltaWork += deltaWork;
		if(change(progress + this.deltaWork)){
			this.progress += this.deltaWork;
			this.deltaWork = 0;
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
			message = context.getResources().getString(R.string.finished_task) +" : "+ taskName; //$NON-NLS-1$
			mViewUpdateHandler.sendEmptyMessage(0);
		}
		work = -1;
		progress = 0;
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
