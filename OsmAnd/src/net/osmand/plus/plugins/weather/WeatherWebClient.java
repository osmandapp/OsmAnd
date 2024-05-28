package net.osmand.plus.plugins.weather;

import net.osmand.core.jni.IQueryController;
import net.osmand.core.jni.IWebClient.DataRequest;
import net.osmand.core.jni.SWIGTYPE_p_QByteArray;
import net.osmand.core.jni.SwigUtilities;
import net.osmand.core.jni.interface_IWebClient;
import net.osmand.plus.utils.AndroidNetworkUtils;

import java.io.File;

public class WeatherWebClient extends interface_IWebClient {

	private WeatherWebClientListener downloadStateListener;

	public interface WeatherWebClientListener {
		void onDownloadStateChanged(boolean downloadState);
	}

	public WeatherWebClient() {
	}

	@Override
	public SWIGTYPE_p_QByteArray downloadData(String url, DataRequest dataRequest, String userAgent) {
		return SwigUtilities.emptyQByteArray();
	}

	@Override
	public String downloadString(String url, DataRequest dataRequest) {
		return "";
	}

	@Override
	public long downloadFile(String url, String fileName, long lastTime, DataRequest dataRequest) {
		IQueryController queryController = dataRequest.getQueryController();
		long downloadResult = AndroidNetworkUtils.downloadModifiedFile(url, new File(fileName), false, lastTime, new AndroidNetworkUtils.NetworkProgress() {
			@Override
			public boolean isInterrupted() {
				if (queryController != null) {
					return queryController.isAborted();
				}
				return false;
			}

			@Override
			public void startWork(int work) {
				super.startWork(work);
				notifyDownloadStateChange(true);
			}

			@Override
			public void finishTask() {
				super.finishTask();
			}
		});
		notifyDownloadStateChange(false);
		return downloadResult;
	}

	private void notifyDownloadStateChange(boolean downloadState) {
		if(downloadStateListener != null) {
			downloadStateListener.onDownloadStateChanged(downloadState);
		}
	}

	public void setDownloadStateListener(WeatherWebClientListener listener) {
		downloadStateListener = listener;
	}
}
