package net.osmand.plus.utils;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IProgress;
import net.osmand.StreamWriter;
import net.osmand.plus.utils.AndroidNetworkUtils.NetworkProgress;
import net.osmand.plus.utils.AndroidNetworkUtils.NetworkResult;
import net.osmand.plus.utils.AndroidNetworkUtils.OnFileUploadCallback;

import java.util.Map;

public class UploadFileTask extends AsyncTask<Void, Integer, NetworkResult> {

	private final String url;
	private final StreamWriter streamWriter;
	private final String fileName;
	private final boolean gzip;
	private final Map<String, String> parameters;
	private final Map<String, String> headers;
	private final OnFileUploadCallback callback;

	public UploadFileTask(@NonNull final String url,
	                      @NonNull final StreamWriter streamWriter,
	                      @NonNull final String fileName,
	                      final boolean gzip,
	                      @NonNull final Map<String, String> parameters,
	                      @Nullable final Map<String, String> headers,
	                      @Nullable final OnFileUploadCallback callback) {
		this.url = url;
		this.streamWriter = streamWriter;
		this.fileName = fileName;
		this.gzip = gzip;
		this.parameters = parameters;
		this.headers = headers;
		this.callback = callback;
	}

	@Override
	protected void onPreExecute() {
		if (callback != null) {
			callback.onFileUploadStarted();
		}
	}

	@NonNull
	@Override
	protected NetworkResult doInBackground(Void... v) {
		final int[] progressValue = {0};
		publishProgress(0);
		IProgress progress = null;
		if (callback != null) {
			progress = new NetworkProgress() {
				@Override
				public void progress(int deltaWork) {
					progressValue[0] += deltaWork;
					publishProgress(progressValue[0]);
				}
			};
		}
		return AndroidNetworkUtils.uploadFile(url, streamWriter, fileName, gzip, parameters, headers, progress);
	}

	@Override
	protected void onProgressUpdate(Integer... p) {
		if (callback != null) {
			Integer progress = p[0];
			if (progress >= 0) {
				callback.onFileUploadProgress(progress);
			}
		}
	}

	@Override
	protected void onPostExecute(@NonNull NetworkResult networkResult) {
		if (callback != null) {
			callback.onFileUploadDone(networkResult);
		}
	}
}
