package net.osmand.plus;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.osmand.IProgress;
import net.osmand.plus.base.ProgressHelper;

public class ProgressImplementation implements IProgress {

	private static final int HANDLER_START_TASK = OsmAndConstants.UI_HANDLER_PROGRESS + 1;
	private static final int HADLER_UPDATE_PROGRESS = OsmAndConstants.UI_HANDLER_PROGRESS + 2;
	private String taskName;
	private ProgressHelper progressHelper;
	private String message = "";

	private final Handler mViewUpdateHandler;
	private Thread run;
	private final Context context;
	private ProgressDialog dialog;
	private ProgressBar progressBar;
	private Runnable finishRunnable;
	private final boolean cancelable;
	private TextView tv;


	public ProgressImplementation(Context ctx, ProgressDialog dlg, boolean cancelable) {
		this.cancelable = cancelable;
		context = ctx;
		setDialog(dlg);

		progressHelper = new ProgressHelper(() -> updateProgressMessage(progressHelper.getLastKnownProgress()));

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
							dialog.setMax(progressHelper.getTotalWork());
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
							progressBar.setMax(progressHelper.getTotalWork());
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

	public static ProgressImplementation createProgressDialog(Context ctx, String title, String message, int style, DialogInterface.OnCancelListener listener) {
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
		dlg.setProgressNumberFormat(null);
		dlg.setProgressStyle(style);
		return new ProgressImplementation(dlg, true);
	}

	public void setProgressBar(TextView tv, ProgressBar progressBar, Runnable finish) {
		this.tv = tv;
		this.progressBar = progressBar;
		this.finishRunnable = finish;
	}

	public void setDialog(ProgressDialog dlg) {
		if (dlg != null) {
			if (cancelable) {
				dlg.setOnCancelListener(dialog -> {
					if (run != null) {
						run.stop();
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
		if (dialog != null) {
			progressHelper.onProgress(deltaWork);
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
		int newProgress = progressHelper.getTotalWork() - remainingWork;
		progress(newProgress - progressHelper.getLastKnownProgress());
	}

	@Override
	public boolean isIndeterminate() {
		return progressHelper.isIndeterminate();
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
		progressHelper.onFinishTask();
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
		progressHelper.onStartWork(work);
	}

}
