package net.osmand.plus.plugins.weather;

import net.osmand.core.jni.IQueryController;
import net.osmand.core.jni.SWIGTYPE_p_QByteArray;
import net.osmand.core.jni.SwigUtilities;
import net.osmand.core.jni.interface_IWebClient;
import net.osmand.core.jni.IWebClient.DataRequest;
import net.osmand.plus.utils.AndroidNetworkUtils;

import java.io.File;

public class WeatherWebClient extends interface_IWebClient {
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
		return AndroidNetworkUtils.downloadModifiedFile(url, new File(fileName), false, lastTime, new AndroidNetworkUtils.NetworkProgress() {
			@Override
			public boolean isInterrupted() {
				if (queryController != null) {
					return queryController.isAborted();
				}
				return false;
			}
		});
	}
}
