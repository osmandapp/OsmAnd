package net.osmand.plus.liveupdates;

import android.os.AsyncTask;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import net.osmand.PlatformUtil;
import net.osmand.osm.io.NetworkUtils;

import org.apache.commons.logging.Log;

public class GetJsonAsyncTask<P> extends AsyncTask<String, Void, P> {
	private static final Log LOG = PlatformUtil.getLog(GetJsonAsyncTask.class);
	private final Class<P> protocolClass;
	private final Gson gson = new Gson();
	private OnResponseListener<P> onResponseListener;
	private OnErrorListener onErrorListener;
	private volatile String error;

	public GetJsonAsyncTask(Class<P> protocolClass) {
		this.protocolClass = protocolClass;
	}

	@Override
	protected P doInBackground(String... params) {
		StringBuilder response = new StringBuilder();
		error = NetworkUtils.sendGetRequest(params[0], null, response);
		if (error == null) {
			try {
				return gson.fromJson(response.toString(), protocolClass);
			} catch (JsonSyntaxException e) {
				error = e.getLocalizedMessage();
			}
		}
		LOG.error(error);
		return null;
	}

	@Override
	protected void onPostExecute(P protocol) {
		if (protocol != null) {
			if (onResponseListener != null) {
				onResponseListener.onResponse(protocol);
			}
		} else if (onErrorListener != null) {
			onErrorListener.onError(error);
		}
	}

	public void setOnResponseListener(OnResponseListener<P> onResponseListener) {
		this.onResponseListener = onResponseListener;
	}

	public void setOnErrorListener(OnErrorListener onErrorListener) {
		this.onErrorListener = onErrorListener;
	}

	public interface OnResponseListener<Protocol> {
		void onResponse(Protocol response);
	}

	public interface OnErrorListener {
		void onError(String error);
	}
}
