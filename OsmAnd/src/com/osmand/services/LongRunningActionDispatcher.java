package com.osmand.services;

import java.util.concurrent.Callable;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;

public class LongRunningActionDispatcher {

	private Context context;

	private LongRunningActionCallback callback;

	private ProgressDialog progressDialog;

	private Handler finishedHandler = new Handler();

	public LongRunningActionDispatcher(Context context,

	LongRunningActionCallback callback) {
		this.context = context;
		this.callback = callback;
	}

	@SuppressWarnings("unchecked")
	public void startLongRunningAction(final Callable callable,

	String progressDialogTitle, String progressDialogMessage) {

		progressDialog = ProgressDialog.show(context, progressDialogTitle,
		progressDialogMessage, true, false);

		new Thread(new Runnable() {

			public void run() {
				Exception error = null;
				try {
					callable.call();
				} catch (Exception e) {
					error = e;
				}

				final Exception finalError = error;
				finishedHandler.post(new Runnable() {
					public void run() {
						onLongRunningActionFinished(finalError);
					}
				});
			}

		}).start();

	}

	private void onLongRunningActionFinished(Exception error) {
		progressDialog.dismiss();
		callback.onLongRunningActionFinished(error);
	}

}
