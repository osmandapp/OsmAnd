package net.osmand.plus.liveupdates.network;

import android.os.AsyncTask;

import com.google.gson.Gson;

import net.osmand.PlatformUtil;
import net.osmand.osm.io.NetworkUtils;

import org.apache.commons.logging.Log;

/**
 * Created by GaidamakUA on 1/12/16.
 */
public class GetJsonAsyncTask<Protocol> extends AsyncTask<String, Void, Protocol> {
	private static final Log LOG = PlatformUtil.getLog(GetJsonAsyncTask.class);
	private final Class<Protocol> protocolClass;
	private final Gson gson = new Gson();
	private OnResponseListener<Protocol> onResponseListener;

	public GetJsonAsyncTask(Class<Protocol> protocolClass) {
		this.protocolClass = protocolClass;
	}

	@Override
	protected Protocol doInBackground(String... params) {
		StringBuilder response = new StringBuilder();
		String error = NetworkUtils.sendGetRequest(params[0], null, response);
		if (error == null) {
			return gson.fromJson(response.toString(), protocolClass);
		}
		LOG.error(error);
		return null;
	}

	@Override
	protected void onPostExecute(Protocol protocol) {
		if (onResponseListener != null) {
			onResponseListener.onResponse(protocol);
		}
	}

	public void setOnResponseListener(OnResponseListener<Protocol> onResponseListener) {
		this.onResponseListener = onResponseListener;
	}

	public interface OnResponseListener<Protocol> {
		void onResponse(Protocol response);
	}
}
