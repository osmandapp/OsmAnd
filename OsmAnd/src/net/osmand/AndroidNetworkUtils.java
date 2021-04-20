package net.osmand;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class AndroidNetworkUtils {

	private static final int CONNECTION_TIMEOUT = 15000;
	private static final Log LOG = PlatformUtil.getLog(AndroidNetworkUtils.class);

	public interface OnRequestResultListener {
		void onResult(String result);
	}

	public interface OnFilesUploadCallback {
		@Nullable
		Map<String, String> getAdditionalParams(@NonNull File file);
		void onFileUploadProgress(@NonNull File file, int percent);
		void onFilesUploadDone(@NonNull Map<File, String> errors);
	}

	public interface OnFilesDownloadCallback {
		@Nullable
		Map<String, String> getAdditionalParams(@NonNull File file);
		void onFileDownloadProgress(@NonNull File file, int percent);
		@WorkerThread
		void onFileDownloadedAsync(@NonNull File file);
		void onFilesDownloadDone(@NonNull Map<File, String> errors);
	}

	public static class RequestResponse {
		private Request request;
		private String response;

		RequestResponse(@NonNull Request request, @Nullable String response) {
			this.request = request;
			this.response = response;
		}

		public Request getRequest() {
			return request;
		}

		public String getResponse() {
			return response;
		}
	}

	public interface OnSendRequestsListener {
		void onRequestSent(@NonNull RequestResponse response);
		void onRequestsSent(@NonNull List<RequestResponse> results);
	}

	public static void sendRequestsAsync(@Nullable final OsmandApplication ctx,
										 @NonNull final List<Request> requests,
										 @Nullable final OnSendRequestsListener listener) {

		new AsyncTask<Void, RequestResponse, List<RequestResponse>>() {

			@Override
			protected List<RequestResponse> doInBackground(Void... params) {
				List<RequestResponse> responses = new ArrayList<>();
				for (Request request : requests) {
					RequestResponse requestResponse;
					try {
						String response = sendRequest(ctx, request.getUrl(), request.getParameters(),
								request.getUserOperation(), request.isToastAllowed(), request.isPost());
						requestResponse = new RequestResponse(request, response);
					} catch (Exception e) {
						requestResponse = new RequestResponse(request, null);
					}
					responses.add(requestResponse);
					publishProgress(requestResponse);
				}
				return responses;
			}

			@Override
			protected void onProgressUpdate(RequestResponse... values) {
				if (listener != null) {
					listener.onRequestSent(values[0]);
				}
			}

			@Override
			protected void onPostExecute(@NonNull List<RequestResponse> results) {
				if (listener != null) {
					listener.onRequestsSent(results);
				}
			}

		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
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

		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
	}

	public static void downloadFileAsync(final String url,
	                                     final File fileToSave,
	                                     final CallbackWithObject<String> listener) {

		new AsyncTask<Void, Void, String>() {

			@Override
			protected String doInBackground(Void... params) {
				return downloadFile(url, fileToSave, false, null);
			}

			@Override
			protected void onPostExecute(String error) {
				if (listener != null) {
					listener.processResult(error);
				}
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
	}

	public static void downloadFilesAsync(final @NonNull String url,
										  final @NonNull List<File> files,
										  final @NonNull Map<String, String> parameters,
										  final @Nullable OnFilesDownloadCallback callback) {

		new AsyncTask<Void, Object, Map<File, String>>() {

			@Override
			@NonNull
			protected Map<File, String> doInBackground(Void... v) {
				Map<File, String> errors = new HashMap<>();
				for (final File file : files) {
					final int[] progressValue = {0};
					publishProgress(file, 0);
					IProgress progress = null;
					if (callback != null) {
						progress = new NetworkProgress() {
							@Override
							public void progress(int deltaWork) {
								progressValue[0] += deltaWork;
								publishProgress(file, progressValue[0]);
							}
						};
					}
					try {
						Map<String, String> params = new HashMap<>(parameters);
						if (callback != null) {
							Map<String, String> additionalParams = callback.getAdditionalParams(file);
							if (additionalParams != null) {
								params.putAll(additionalParams);
							}
						}
						boolean firstPrm = !url.contains("?");
						StringBuilder sb = new StringBuilder(url);
						for (Entry<String, String> entry : params.entrySet()) {
							sb.append(firstPrm ? "?" : "&").append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), "UTF-8"));
							firstPrm = false;
						}
						String res = downloadFile(sb.toString(), file, true, progress);
						if (res != null) {
							errors.put(file, res);
						} else {
							if (callback != null) {
								callback.onFileDownloadedAsync(file);
							}
						}
					} catch (Exception e) {
						errors.put(file, e.getMessage());
					}
					publishProgress(file, Integer.MAX_VALUE);
				}
				return errors;
			}

			@Override
			protected void onProgressUpdate(Object... objects) {
				if (callback != null) {
					callback.onFileDownloadProgress((File) objects[0], (Integer) objects[1]);
				}
			}

			@Override
			protected void onPostExecute(@NonNull Map<File, String> errors) {
				if (callback != null) {
					callback.onFilesDownloadDone(errors);
				}
			}

		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
	}

	public static String sendRequest(@Nullable OsmandApplication ctx, @NonNull String url,
									 @Nullable Map<String, String> parameters,
									 @Nullable String userOperation, boolean toastAllowed, boolean post) {
		HttpURLConnection connection = null;
		try {
			
			String params = null;
			if (parameters != null && parameters.size() > 0) {
				StringBuilder sb = new StringBuilder();
				for (Entry<String, String> entry : parameters.entrySet()) {
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
			connection.setRequestProperty("User-Agent", ctx != null ? Version.getFullVersion(ctx) : "OsmAnd");
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
				if (toastAllowed && ctx != null) {
					String msg = (!Algorithms.isEmpty(userOperation) ? userOperation + " " : "")
							+ ctx.getString(R.string.failed_op) + ": "
							+ connection.getResponseMessage();
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
			if (toastAllowed && ctx != null) {
				String msg = ctx.getString(R.string.auth_failed);
				showToast(ctx, msg);
			}
		} catch (MalformedURLException e) {
			if (toastAllowed && ctx != null) {
				showToast(ctx, MessageFormat.format(ctx.getResources().getString(R.string.shared_string_action_template)
						+ ": " + ctx.getResources().getString(R.string.shared_string_unexpected_error), userOperation));
			}
		} catch (IOException e) {
			if (toastAllowed && ctx != null) {
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

	public static String downloadFile(@NonNull String url, @NonNull File fileToSave, boolean gzip, @Nullable IProgress progress) {
		String error = null;
		try {
			URLConnection connection = NetworkUtils.getHttpURLConnection(url);
			connection.setConnectTimeout(CONNECTION_TIMEOUT);
			connection.setReadTimeout(CONNECTION_TIMEOUT);
			if (gzip) {
				connection.setRequestProperty("Accept-Encoding", "deflate, gzip");
			}
			InputStream inputStream = gzip
					? new GZIPInputStream(connection.getInputStream())
					: new BufferedInputStream(connection.getInputStream(), 8 * 1024);
			fileToSave.getParentFile().mkdirs();
			OutputStream stream = null;
			try {
				stream = new FileOutputStream(fileToSave);
				Algorithms.streamCopy(inputStream, stream, progress, 1024);
				stream.flush();
			} finally {
				Algorithms.closeStream(inputStream);
				Algorithms.closeStream(stream);
			}
		} catch (UnknownHostException e) {
			error = e.getMessage();
			LOG.error("UnknownHostException, cannot download file " + url + " " + error);
		} catch (Exception e) {
			error = e.getMessage();
			LOG.warn("Cannot download file : " + url, e);
		}
		return error;
	}

	private static final String BOUNDARY = "CowMooCowMooCowCowCow";

	public static String uploadFile(@NonNull String urlText, @NonNull File file, boolean gzip,
									@NonNull Map<String, String> additionalParams,
									@Nullable Map<String, String> headers,
									@Nullable IProgress progress) throws IOException {
		return uploadFile(urlText, new FileInputStream(file), file.getName(), gzip, additionalParams, headers, progress);
	}

	public static String uploadFile(@NonNull String urlText, @NonNull InputStream inputStream,
									@NonNull String fileName, boolean gzip,
									@NonNull Map<String, String> additionalParams,
									@Nullable Map<String, String> headers,
									@Nullable IProgress progress) {
		URL url;
		try {
			boolean firstPrm = !urlText.contains("?");
			StringBuilder sb = new StringBuilder(urlText);
			for (Entry<String, String> entry : additionalParams.entrySet()) {
				sb.append(firstPrm ? "?" : "&").append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), "UTF-8"));
				firstPrm = false;
			}
			urlText = sb.toString();

			LOG.info("Start uploading file to " + urlText + " " + fileName);
			url = new URL(urlText);

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
			conn.setRequestProperty("User-Agent", "OsmAnd");
			if (headers != null) {
				for (Entry<String, String> header : headers.entrySet()) {
					conn.setRequestProperty(header.getKey(), header.getValue());
				}
			}

			OutputStream ous = conn.getOutputStream();
			ous.write(("--" + BOUNDARY + "\r\n").getBytes());
			if (gzip) {
				fileName += ".gz";
			}
			ous.write(("content-disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n").getBytes());
			ous.write(("Content-Type: application/octet-stream\r\n\r\n").getBytes());

			BufferedInputStream bis = new BufferedInputStream(inputStream, 20 * 1024);
			ous.flush();
			if (gzip) {
				GZIPOutputStream gous = new GZIPOutputStream(ous, 1024);
				Algorithms.streamCopy(bis, gous, progress, 1024);
				gous.flush();
				gous.finish();
			} else {
				Algorithms.streamCopy(bis, ous, progress, 1024);
			}

			ous.write(("\r\n--" + BOUNDARY + "--\r\n").getBytes());
			ous.flush();
			Algorithms.closeStream(bis);
			Algorithms.closeStream(ous);

			LOG.info("Finish uploading file " + fileName);
			LOG.info("Response code and message : " + conn.getResponseCode() + " " + conn.getResponseMessage());
			if (conn.getResponseCode() != 200) {
				return conn.getResponseMessage();
			}
			InputStream is = conn.getInputStream();
			StringBuilder responseBody = new StringBuilder();
			if (is != null) {
				BufferedReader in = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				String s;
				boolean first = true;
				while ((s = in.readLine()) != null) {
					if (first) {
						first = false;
					} else {
						responseBody.append("\n");
					}
					responseBody.append(s);

				}
				is.close();
			}
			String response = responseBody.toString();
			LOG.info("Response : " + response);
			return null;
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
			return e.getMessage();
		}
	}

	public static void uploadFilesAsync(final @NonNull String url,
										final @NonNull List<File> files,
										final boolean gzip,
										final @NonNull Map<String, String> parameters,
										final @Nullable Map<String, String> headers,
										final OnFilesUploadCallback callback) {

		new AsyncTask<Void, Object, Map<File, String>>() {

			@Override
			@NonNull
			protected Map<File, String> doInBackground(Void... v) {
				Map<File, String> errors = new HashMap<>();
				for (final File file : files) {
					final int[] progressValue = {0};
					publishProgress(file, 0);
					IProgress progress = null;
					if (callback != null) {
						progress = new NetworkProgress() {
							@Override
							public void progress(int deltaWork) {
								progressValue[0] += deltaWork;
								publishProgress(file, progressValue[0]);
							}
						};
					}
					try {
						Map<String, String> params = new HashMap<>(parameters);
						if (callback != null) {
							Map<String, String> additionalParams = callback.getAdditionalParams(file);
							if (additionalParams != null) {
								params.putAll(additionalParams);
							}
						}
						String res = uploadFile(url, file, gzip, params, headers, progress);
						if (res != null) {
							errors.put(file, res);
						}
					} catch (Exception e) {
						errors.put(file, e.getMessage());
					}
					publishProgress(file, Integer.MAX_VALUE);
				}
				return errors;
			}

			@Override
			protected void onProgressUpdate(Object... objects) {
				if (callback != null) {
					callback.onFileUploadProgress((File) objects[0], (Integer) objects[1]);
				}
			}

			@Override
			protected void onPostExecute(@NonNull Map<File, String> errors) {
				if (callback != null) {
					callback.onFilesUploadDone(errors);
				}
			}

		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
	}

	private static void showToast(OsmandApplication ctx, String message) {
		ctx.showToastMessage(message);
	}

	public static class Request {
		private String url;
		private Map<String, String> parameters;
		private String userOperation;
		private boolean toastAllowed;
		private boolean post;

		public Request(String url, Map<String, String> parameters, String userOperation, boolean toastAllowed, boolean post) {
			this.url = url;
			this.parameters = parameters;
			this.userOperation = userOperation;
			this.toastAllowed = toastAllowed;
			this.post = post;
		}

		public String getUrl() {
			return url;
		}

		public Map<String, String> getParameters() {
			return parameters;
		}

		public String getUserOperation() {
			return userOperation;
		}

		public boolean isToastAllowed() {
			return toastAllowed;
		}

		public boolean isPost() {
			return post;
		}
	}

	private abstract static class NetworkProgress implements IProgress {
		@Override
		public void startTask(String taskName, int work) {
		}

		@Override
		public void startWork(int work) {
		}

		@Override
		public abstract void progress(int deltaWork);

		@Override
		public void remaining(int remainingWork) {
		}

		@Override
		public void finishTask() {
		}

		@Override
		public boolean isIndeterminate() {
			return false;
		}

		@Override
		public boolean isInterrupted() {
			return false;
		}

		@Override
		public void setGeneralProgress(String genProgress) {
		}
	}
}
