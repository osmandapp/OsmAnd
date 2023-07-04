package net.osmand.plus.plugins.weather;

import net.osmand.core.jni.SWIGTYPE_p_QByteArray;
import net.osmand.core.jni.SWIGTYPE_p_std__functionT_void_funsigned_long_long_const_unsigned_long_long_constF_t;
import net.osmand.core.jni.SWIGTYPE_p_std__shared_ptrT_IQueryController_const_t;
import net.osmand.core.jni.SWIGTYPE_p_std__shared_ptrT_OsmAnd__IWebClient__IRequestResult_const_t;
import net.osmand.core.jni.SwigUtilities;
import net.osmand.core.jni.interface_IWebClient;
import net.osmand.plus.utils.AndroidNetworkUtils;

import java.io.File;

public class WeatherWebClient extends interface_IWebClient {
	public WeatherWebClient() {
	}

	@Override
	public SWIGTYPE_p_QByteArray downloadData(String url, SWIGTYPE_p_std__shared_ptrT_OsmAnd__IWebClient__IRequestResult_const_t requestResult, SWIGTYPE_p_std__functionT_void_funsigned_long_long_const_unsigned_long_long_constF_t progressCallback, SWIGTYPE_p_std__shared_ptrT_IQueryController_const_t queryController, String userAgent) {
		return SwigUtilities.emptyQByteArray();
	}

	@Override
	public String downloadString(String url, SWIGTYPE_p_std__shared_ptrT_OsmAnd__IWebClient__IRequestResult_const_t requestResult, SWIGTYPE_p_std__functionT_void_funsigned_long_long_const_unsigned_long_long_constF_t progressCallback, SWIGTYPE_p_std__shared_ptrT_IQueryController_const_t queryController) {
		return "";
	}

	@Override
	public long downloadFile(String url, String fileName, long lastTime, SWIGTYPE_p_std__shared_ptrT_OsmAnd__IWebClient__IRequestResult_const_t requestResult, SWIGTYPE_p_std__functionT_void_funsigned_long_long_const_unsigned_long_long_constF_t progressCallback, SWIGTYPE_p_std__shared_ptrT_IQueryController_const_t queryController) {
		return AndroidNetworkUtils.downloadModifiedFile(url, new File(fileName), false, lastTime, new AndroidNetworkUtils.NetworkProgress() {
			@Override
			public boolean isInterrupted() {
				return SwigUtilities.isAborted(queryController);
			}
		});
	}
}
