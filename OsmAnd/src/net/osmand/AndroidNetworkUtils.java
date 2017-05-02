package net.osmand;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.plus.Version;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.Map;

public class AndroidNetworkUtils {

	private static final int CONNECTION_TIMEOUT = 15000;
	private static final Log LOG = PlatformUtil.getLog(AndroidNetworkUtils.class);

	public interface OnRequestResultListener {
		void onResult(String result);
	}

	public static void sendRequestAsync(final OsmandApplication ctx,
										final String url,
										final Map<String, String> parameters,
										final String userOperation,
										final boolean toastAllowed,
										final boolean post,
										final OnRequestResultListener listener) {

		new AsyncTask<Void, Void, String>() {

			@Override
			protected String doInBackground(Void... params) {
				try {
					return sendRequest(ctx, url, parameters, userOperation, toastAllowed, post);
				} catch (Exception e) {
					return null;
				}
			}

			@Override
			protected void onPostExecute(String response) {
				if (listener != null) {
					listener.onResult(response);
				}
			}

		}.execute((Void) null);
	}


	public static String sendRequest(OsmandApplication ctx, String url, Map<String, String> parameters,
									 String userOperation, boolean toastAllowed, boolean post) {
		HttpURLConnection connection = null;
		try {
			
			String params = null;
			if (parameters != null && parameters.size() > 0) {
				StringBuilder sb = new StringBuilder();
				for (Map.Entry<String, String> entry : parameters.entrySet()) {
					if (sb.length() > 0) {
						sb.append("&");
					}
					sb.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), "UTF-8"));
				}
				params = sb.toString();
			}
			String paramsSeparator = url.indexOf('?') == -1 ? "?" : "&";
			connection = NetworkUtils.getHttpURLConnection(params == null || post ? url : url + paramsSeparator + params);
			connection.setRequestProperty("Accept-Charset", "UTF-8");
			connection.setRequestProperty("User-Agent", Version.getFullVersion(ctx));
			connection.setConnectTimeout(15000);
			if (params != null && post) {
				connection.setDoInput(true);
				connection.setDoOutput(true);
				connection.setUseCaches(false);
				connection.setRequestMethod("POST");
				connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
				connection.setRequestProperty("Content-Length", String.valueOf(params.getBytes("UTF-8").length));
				connection.setFixedLengthStreamingMode(params.getBytes("UTF-8").length);

				OutputStream output = new BufferedOutputStream(connection.getOutputStream());
				output.write(params.getBytes("UTF-8"));
				output.flush();
				output.close();

			} else {
				
				connection.setRequestMethod("GET");
				connection.connect();
			}

			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				if (toastAllowed) {
					String msg = userOperation
							+ " " + ctx.getString(R.string.failed_op) + ": " + connection.getResponseMessage();
					showToast(ctx, msg);
				}
			} else {
				StringBuilder responseBody = new StringBuilder();
				responseBody.setLength(0);
				InputStream i = connection.getInputStream();
				if (i != null) {
					BufferedReader in = new BufferedReader(new InputStreamReader(i, "UTF-8"), 256);
					String s;
					boolean f = true;
					while ((s = in.readLine()) != null) {
						if (!f) {
							responseBody.append("\n");
						} else {
							f = false;
						}
						responseBody.append(s);
					}
					try {
						in.close();
						i.close();
					} catch (Exception e) {
						// ignore exception
					}
				}
				return responseBody.toString();
			}

		} catch (NullPointerException e) {
			// that's tricky case why NPE is thrown to fix that problem httpClient could be used
			if (toastAllowed) {
				String msg = ctx.getString(R.string.auth_failed);
				showToast(ctx, msg);
			}
		} catch (MalformedURLException e) {
			if (toastAllowed) {
				showToast(ctx, MessageFormat.format(ctx.getResources().getString(R.string.shared_string_action_template)
						+ ": " + ctx.getResources().getString(R.string.shared_string_unexpected_error), userOperation));
			}
		} catch (IOException e) {
			if (toastAllowed) {
				showToast(ctx, MessageFormat.format(ctx.getResources().getString(R.string.shared_string_action_template)
						+ ": " + ctx.getResources().getString(R.string.shared_string_io_error), userOperation));
			}
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}

		return null;
	}

	public static Bitmap downloadImage(OsmandApplication ctx, String url) {
		Bitmap res = null;
		try {
			URLConnection connection = NetworkUtils.getHttpURLConnection(url);
			connection.setRequestProperty("User-Agent", Version.getFullVersion(ctx));
			connection.setConnectTimeout(CONNECTION_TIMEOUT);
			connection.setReadTimeout(CONNECTION_TIMEOUT);
			BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream(), 8 * 1024);
			try {
				res = BitmapFactory.decodeStream(inputStream);
			} finally {
				Algorithms.closeStream(inputStream);
			}
		} catch (UnknownHostException e) {
			LOG.error("UnknownHostException, cannot download image " + url + " " + e.getMessage());
		} catch (Exception e) {
			LOG.error("Cannot download image : " + url, e);
		}
		return res;
	}

	private static void showToast(OsmandApplication ctx, String message) {
		ctx.showToastMessage(message);
	}

}
