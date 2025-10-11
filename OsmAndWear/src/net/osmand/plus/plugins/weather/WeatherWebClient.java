package net.osmand.plus.plugins.weather;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.core.jni.IQueryController;
import net.osmand.core.jni.IWebClient.DataRequest;
import net.osmand.core.jni.SWIGTYPE_p_QByteArray;
import net.osmand.core.jni.SwigUtilities;
import net.osmand.core.jni.interface_IWebClient;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.plus.utils.AndroidNetworkUtils.NetworkProgress;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

public class WeatherWebClient extends interface_IWebClient {

	private final Log LOG = PlatformUtil.getLog(WeatherWebClient.class);

	private WeatherWebClientListener downloadStateListener;
	private final AtomicInteger activeRequestsCounter = new AtomicInteger(0);

	public enum DownloadState {
		IDLE,
		STARTED,
		FINISHED
	}

	public interface WeatherWebClientListener {
		void onDownloadStateChanged(@NonNull DownloadState downloadState, int activeRequestsCounter);
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
		return AndroidNetworkUtils.downloadModifiedFile(url, new File(fileName), false, lastTime, new NetworkProgress() {
			@Override
			public boolean isInterrupted() {
				if (queryController != null) {
					return queryController.isAborted();
				}
				return false;
			}

			@Override
			public void startTask(String taskName, int work) {
				super.startTask(taskName, work);
				int requestsCount = activeRequestsCounter.incrementAndGet();
				notifyDownloadStateChanged(DownloadState.STARTED, requestsCount);
			}

			@Override
			public void finishTask() {
				super.finishTask();
				int requestsCount = activeRequestsCounter.decrementAndGet();
				notifyDownloadStateChanged(DownloadState.FINISHED, requestsCount);
			}
		});
	}

	int getActiveRequestsCount() {
		return activeRequestsCounter.get();
	}

	void setDownloadStateListener(@NonNull WeatherWebClientListener listener) {
		downloadStateListener = listener;
	}

	void cleanupResources() {
		downloadStateListener = null;
	}

	private void notifyDownloadStateChanged(@NonNull DownloadState downloadState, int requestsCount) {
		if (downloadStateListener != null) {
			downloadStateListener.onDownloadStateChanged(downloadState, requestsCount);
		}
	}
}
