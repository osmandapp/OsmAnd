package net.osmand.plus;

import net.osmand.IProgress;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ProgressImplementation implements IProgress {
	
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
	private ProgressBar progressBar = null;
	private Runnable finishRunnable = null;
	private final boolean cancelable;
	private TextView tv;
	

	public ProgressImplementation(Context ctx, ProgressDialog dlg, boolean cancelable) {
		this.cancelable = cancelable;
		context = ctx;
		setDialog(dlg);

		mViewUpdateHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);

				switch (msg.what) {
				case HANDLER_START_TASK:
					if (dialog != null) {
						dialog.setMessage(message);
						if (isIndeterminate()) {
							dialog.setMax(1);
							dialog.setIndeterminate(true);
						} else {
							dialog.setIndeterminate(false);
							dialog.setMax(work);
						}
						dialog.show();
					}
					if (tv != null) {
						tv.setText(message);
					}
					if (progressBar != null) {
						if (isIndeterminate()) {
							progressBar.setMax(1);
							progressBar.setIndeterminate(true);
						} else {
							progressBar.setIndeterminate(false);
							progressBar.setMax(work);
						}
					}
					break;
				case HADLER_UPDATE_PROGRESS:
					if (dialog != null) {
						dialog.setProgress(msg.arg1);
					} else if (progressBar != null) {
						progressBar.setProgress(msg.arg1);
					}
					break;
				}
			}
		};
	}
		
	public ProgressImplementation(ProgressDialog dlg, boolean cancelable){
		this(dlg.getContext(), dlg, cancelable);
	}


	public static ProgressImplementation createProgressDialog(Context ctx, String title, String message, int style) {
		return createProgressDialog(ctx, title, message, style, null);
	}
	
	public static ProgressImplementation createProgressDialog(Context ctx, String title, String message, int style, final DialogInterface.OnCancelListener listener) {
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
		return new ProgressImplementation(dlg, true);
	}
	
	public void setProgressBar(TextView tv, ProgressBar progressBar, Runnable finish) {
		this.tv = tv;
		this.progressBar = progressBar;
		this.finishRunnable = finish;
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
	public void setGeneralProgress(String genProgress) {
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
			Resources resources = context.getResources();
			message = resources.getString(R.string.ltr_or_rtl_combine_via_colon, resources.getString(R.string.finished_task), taskName);
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
	
	public Runnable getFinishRunnable() {
		return finishRunnable;
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
