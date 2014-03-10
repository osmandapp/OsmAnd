package net.osmand.plus.dialogs;

import net.osmand.IProgress;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.R;
import net.osmand.plus.R.string;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Handler;
import android.os.Message;

public class ProgressDialogImplementation implements IProgress {
	
	private static final int HANDLER_START_TASK = OsmAndConstants.UI_HANDLER_PROGRESS + 1;
	private static final int HADLER_UPDATE_PROGRESS = OsmAndConstants.UI_HANDLER_PROGRESS + 2;
	private String taskName;
	private int progress;
	private int deltaProgress;
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
					switch (msg.what) {
						case HANDLER_START_TASK:
							dialog.setMessage(message);
							if (isIndeterminate()) {
								dialog.setMax(1);
								dialog.setIndeterminate(true);
							} else {
								dialog.setIndeterminate(false);
								dialog.setMax(work);
							}
							dialog.show();
							break;
						case HADLER_UPDATE_PROGRESS:
							dialog.setProgress(msg.arg1);
							break;
					}
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
	

	public static ProgressDialogImplementation createProgressDialog(Context ctx, String title, String message, int style) {
		return createProgressDialog(ctx, title, message, style, null);
	}
	
	public static ProgressDialogImplementation createProgressDialog(Context ctx, String title, String message, int style, final DialogInterface.OnCancelListener listener) {
		ProgressDialog dlg = new ProgressDialog(ctx) {
			@Override
			public void cancel() {
				if(listener != null) {
					listener.onCancel(this);
				}  else {
					super.cancel();
				}
			}
		};
		dlg.setTitle(title);
		dlg.setMessage(message);
		dlg.setIndeterminate(style == ProgressDialog.STYLE_HORIZONTAL); // re-set in mViewUpdateHandler.handleMessage above
		dlg.setCancelable(true);
		// we'd prefer a plain progress bar without numbers,
		// but that is only available starting from API level 11
		try {
			ProgressDialog.class
				.getMethod("setProgressNumberFormat", new Class[] { String.class })
				.invoke(dlg, (String)null);
		} catch (NoSuchMethodException nsme) {
			// failure, must be older device
		} catch (IllegalAccessException nsme) {
			// failure, must be older device
		} catch (java.lang.reflect.InvocationTargetException nsme) {
			// failure, must be older device
		}
		dlg.setProgressStyle(style);
		return new ProgressDialogImplementation(dlg, true);
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
		if (!isIndeterminate() && dialog != null) {
			this.deltaProgress += deltaWork;
			//update only each percent
			if ((deltaProgress > (work / 100)) || ((progress + deltaProgress) >= work)) {
				this.progress += deltaProgress;
				this.deltaProgress = 0;
				updateProgressMessage(this.progress);
			}
		}
	}

	private void updateProgressMessage(int aProgress) {
		Message msg = mViewUpdateHandler.obtainMessage();
		msg.arg1 = aProgress;
		msg.what = HADLER_UPDATE_PROGRESS;
		mViewUpdateHandler.sendMessage(msg);
	}
	
	@Override
	public void remaining(int remainingWork) {
		int newprogress = work - remainingWork;
		progress(newprogress - this.progress);
	}
	
	@Override
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
		mViewUpdateHandler.sendEmptyMessage(HANDLER_START_TASK);
	}

	@Override
	public void finishTask() {
		work = -1;
		progress = 0;
		if (taskName != null) {
			message = context.getResources().getString(R.string.finished_task) +" : "+ taskName; //$NON-NLS-1$
			mViewUpdateHandler.sendEmptyMessage(HANDLER_START_TASK);
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
		deltaProgress = 0;
	}

}
