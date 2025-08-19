package net.osmand.plus.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import net.osmand.CallbackWithObject;
import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.StreamWriter;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.OsmAndTaskManager;
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
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
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
import java.util.concurrent.Executor;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class AndroidNetworkUtils {

	public static final int CONNECT_TIMEOUT = 30000;
	public static final int READ_TIMEOUT = CONNECT_TIMEOUT * 2;
	private static final Log LOG = PlatformUtil.getLog(AndroidNetworkUtils.class);

	public static final String CANCELLED_MSG = "cancelled";

	public interface OnRequestResultListener {
		void onResult(@Nullable String result, @Nullable String error, @Nullable Integer resultCode);
	}

	public interface OnFileUploadCallback {
		default void onFileUploadStarted() {
		}

		default void onFileUploadProgress(int percent) {
		}

		default void onFileUploadDone(@NonNull NetworkResult networkResult) {
		}
	}

	public interface OnFilesUploadCallback {
		@Nullable
		Map<String, String> getAdditionalParams(@NonNull File file);

		void onFileUploadProgress(@NonNull File file, int percent);

		void onFileUploadDone(@NonNull File file);

		void onFilesUploadDone(@NonNull Map<File, String> errors);
	}

	public interface OnFilesDownloadCallback {
		@Nullable
		Map<String, String> getAdditionalParams(@NonNull File file);

		void onFileDownloadProgress(@NonNull File file, int percent);

		@WorkerThread
		void onFileDownloadedAsync(@NonNull File file);

		void onFileDownloadDone(@NonNull File file);

		void onFilesDownloadDone(@NonNull Map<File, String> errors);
	}

	public static class RequestResponse {
		private final Request request;
		private final String response;
		private final String error;

		public RequestResponse(@NonNull Request request, @Nullable String response) {
			this.request = request;
			this.response = response;
			this.error = null;
		}

		public RequestResponse(@NonNull Request request, @Nullable String response, @Nullable String error) {
			this.request = request;
			this.response = response;
			this.error = error;
		}

		public Request getRequest() {
			return request;
		}

		public String getResponse() {
			return response;
		}

		public String getError() {
			return error;
		}
	}

	public interface OnSendRequestsListener {
		void onRequestSending(@NonNull Request request);

		void onRequestSent(@NonNull RequestResponse response);

		void onRequestsSent(@NonNull List<RequestResponse> results);
	}

	public static void sendRequestsAsync(@Nullable OsmandApplication ctx,
										 @NonNull List<Request> requests,
										 @Nullable OnSendRequestsListener listener) {
		sendRequestsAsync(ctx, requests, listener, AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public static void sendRequestsAsync(@Nullable OsmandApplication ctx,
										 @NonNull List<Request> requests,
										 @Nullable OnSendRequestsListener listener,
										 Executor executor) {
		OsmAndTaskManager.executeTask(new AsyncTask<Void, Object, List<RequestResponse>>() {

			@Override
			protected List<RequestResponse> doInBackground(Void... params) {
				List<RequestResponse> responses = new ArrayList<>();
				for (Request request : requests) {
					RequestResponse requestResponse;
					try {
						publishProgress(request);
						String[] response = {null, null};
						sendRequest(ctx, request.getUrl(), request.getParameters(),
								request.getUserOperation(), request.isToastAllowed(), request.isPost(), (result, error, resultCode) -> {
									response[0] = result;
									response[1] = error;
								});
						requestResponse = new RequestResponse(request, response[0], response[1]);
					} catch (Exception e) {
						requestResponse = new RequestResponse(request, null, "Unexpected error");
					}
					responses.add(requestResponse);
					publishProgress(requestResponse);
				}
				return responses;
			}

			@Override
			protected void onProgressUpdate(Object... values) {
				if (listener != null) {
					Object obj = values[0];
					if (obj instanceof RequestResponse) {
						listener.onRequestSent((RequestResponse) obj);
					} else if (obj instanceof Request) {
						listener.onRequestSending((Request) obj);
					}
				}
			}

			@Override
			protected void onPostExecute(@NonNull List<RequestResponse> results) {
				if (listener != null) {
					listener.onRequestsSent(results);
				}
			}

		}, executor);
	}

	public static void sendRequestAsync(@Nullable OsmandApplication app,
	                                    @NonNull String url,
	                                    @Nullable Map<String, String> parameters,
	                                    @Nullable String userOperation,
	                                    boolean toastAllowed,
	                                    boolean post,
	                                    @Nullable OnRequestResultListener listener) {
		OsmAndTaskManager.executeTask(new AsyncTask<Void, Void, Void>() {

			private String result;
			private String error;
			private Integer resultCode;

			@Override
			protected Void doInBackground(Void... params) {
				try {
					sendRequest(app, url, parameters, userOperation, toastAllowed, post, (result, error, resultCode) -> {
						this.result = result;
						this.error = error;
						this.resultCode = resultCode;
					});
				} catch (Exception e) {
					// ignore
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void param) {
				if (listener != null) {
					listener.onResult(result, error, resultCode);
				}
			}

		});
	}

	public static void downloadFileAsync(String url,
	                                     File fileToSave,
	                                     CallbackWithObject<String> listener) {
		OsmAndTaskManager.executeTask(new AsyncTask<Void, Void, String>() {

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
		});
	}

	private static void publishFilesDownloadProgress(@NonNull File file, int progress,
													 @Nullable OnFilesDownloadCallback callback) {
		if (callback != null) {
			if (progress >= 0) {
				callback.onFileDownloadProgress(file, progress);
			} else {
				callback.onFileDownloadDone(file);
			}
		}
	}

	public static Map<File, String> downloadFiles(@NonNull String url,
	                                              @NonNull List<File> files,
	                                              @NonNull Map<String, String> parameters,
	                                              @Nullable OnFilesDownloadCallback callback) {
		Map<File, String> errors = new HashMap<>();
		for (File file : files) {
			int[] progressValue = {0};
			publishFilesDownloadProgress(file, 0, callback);
			IProgress progress = null;
			if (callback != null) {
				progress = new NetworkProgress() {
					@Override
					public void progress(int deltaWork) {
						progressValue[0] += deltaWork;
						publishFilesDownloadProgress(file, progressValue[0], callback);
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
			publishFilesDownloadProgress(file, -1, callback);
		}
		if (callback != null) {
			callback.onFilesDownloadDone(errors);
		}
		return errors;
	}

	public static void downloadFilesAsync(@NonNull String url,
	                                      @NonNull List<File> files,
	                                      @NonNull Map<String, String> parameters,
	                                      @Nullable OnFilesDownloadCallback callback) {
		downloadFilesAsync(url, files, parameters, callback, AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public static void downloadFilesAsync(@NonNull String url,
	                                      @NonNull List<File> files,
	                                      @NonNull Map<String, String> parameters,
	                                      @Nullable OnFilesDownloadCallback callback,
	                                      Executor executor) {
		OsmAndTaskManager.executeTask(new AsyncTask<Void, Object, Map<File, String>>() {

			@Override
			@NonNull
			protected Map<File, String> doInBackground(Void... v) {
				Map<File, String> errors = new HashMap<>();
				for (File file : files) {
					int[] progressValue = {0};
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
					publishProgress(file, -1);
				}
				return errors;
			}

			@Override
			protected void onProgressUpdate(Object... objects) {
				if (callback != null) {
					File file = (File) objects[0];
					Integer progress = (Integer) objects[1];
					if (progress >= 0) {
						callback.onFileDownloadProgress(file, progress);
					} else {
						callback.onFileDownloadDone(file);
					}
				}
			}

			@Override
			protected void onPostExecute(@NonNull Map<File, String> errors) {
				if (callback != null) {
					callback.onFilesDownloadDone(errors);
				}
			}

		}, executor);
	}

	public static String sendRequest(@Nullable OsmandApplication ctx, @NonNull Request request) {
		return sendRequest(ctx, request.getUrl(), request.getParameters(),
				request.getUserOperation(), request.isToastAllowed(), request.isPost());
	}

	public static String sendRequest(@Nullable OsmandApplication ctx, @NonNull Request request,
									 @Nullable OnRequestResultListener listener) {
		return sendRequest(ctx, request.getUrl(), request.getParameters(),
				request.getUserOperation(), request.isToastAllowed(), request.isPost(), listener);
	}

	public static String sendRequest(@Nullable OsmandApplication ctx, @NonNull String url,
									 @Nullable Map<String, String> parameters,
									 @Nullable String userOperation, boolean toastAllowed, boolean post) {
		return sendRequest(ctx, url, parameters, userOperation, toastAllowed, post, null);
	}

	public static String sendRequest(@Nullable OsmandApplication app, @NonNull String baseUrl,
	                                 @Nullable Map<String, String> parameters,
	                                 @Nullable String userOperation, boolean toastAllowed,
	                                 boolean post, @Nullable OnRequestResultListener listener) {
		String paramsSeparator = baseUrl.indexOf('?') == -1 ? "?" : "&";
		String contentType = "application/x-www-form-urlencoded;charset=UTF-8";
		String params = getParameters(app, parameters, listener, userOperation, toastAllowed);
		String url = params == null || post ? baseUrl : baseUrl + paramsSeparator + params;
		return sendRequest(app, url, params, userOperation, contentType, toastAllowed, post, listener);
	}

	public static String sendRequest(@Nullable OsmandApplication app, @NonNull String url,
	                                 @Nullable String body, @Nullable String userOperation,
	                                 @Nullable String contentType, boolean toastAllowed,
	                                 boolean post, @Nullable OnRequestResultListener listener) {
		String result = null;
		String error = null;
		Integer resultCode = null;
		HttpURLConnection connection = null;
		try {
			connection = acquireConnection(app, url, body, contentType, post);
			resultCode = connection.getResponseCode();
			if (resultCode != HttpURLConnection.HTTP_OK) {
				if (app != null) {
					error = (!Algorithms.isEmpty(userOperation) ? userOperation + " " : "")
							+ app.getString(R.string.failed_op) + ": " + connection.getResponseMessage();
				} else {
					error = (!Algorithms.isEmpty(userOperation) ? userOperation + " " : "")
							+ "failed: " + connection.getResponseMessage();
				}
				if (toastAllowed && app != null) {
					app.showToastMessage(error);
				}
				InputStream errorStream = connection.getErrorStream();
				if (errorStream != null) {
					error = streamToString(errorStream);
				}
			} else {
				result = streamToString(connection.getInputStream());
			}
		} catch (NullPointerException e) {
			// that's tricky case why NPE is thrown to fix that problem httpClient could be used
			if (app != null) {
				error = app.getString(R.string.auth_failed);
			} else {
				error = "Authorization failed";
			}
			if (toastAllowed && app != null) {
				app.showToastMessage(error);
			}
		} catch (MalformedURLException e) {
			if (app != null) {
				error = MessageFormat.format(app.getString(R.string.shared_string_action_template)
						+ ": " + app.getString(R.string.shared_string_unexpected_error), userOperation);
			} else {
				error = "Action " + userOperation + ": Unexpected error";
			}
			if (toastAllowed && app != null) {
				app.showToastMessage(error);
			}
		} catch (IOException e) {
			error = processIOError(app, userOperation, toastAllowed);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
		if (listener != null) {
			listener.onResult(result, error, resultCode);
		}
		return result;
	}

	@NonNull
	private static HttpURLConnection acquireConnection(@Nullable OsmandApplication app,
	                                                   @NonNull String url, @Nullable String body,
	                                                   @Nullable String contentType, boolean post) throws IOException {
		HttpURLConnection connection = NetworkUtils.getHttpURLConnection(url);
		connection.setRequestProperty("Accept-Charset", "UTF-8");
		connection.setRequestProperty("User-Agent", app != null ? Version.getFullVersion(app) : "OsmAnd");
		connection.setConnectTimeout(CONNECT_TIMEOUT);
		connection.setReadTimeout(READ_TIMEOUT);
		if (body != null && post) {
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setRequestMethod("POST");
			if (!Algorithms.isEmpty(contentType)) {
				connection.setRequestProperty("Content-Type", contentType);
			}
			connection.setRequestProperty("Content-Length", String.valueOf(body.getBytes("UTF-8").length));
			connection.setFixedLengthStreamingMode(body.getBytes("UTF-8").length);

			OutputStream output = new BufferedOutputStream(connection.getOutputStream());
			output.write(body.getBytes("UTF-8"));
			output.flush();
			output.close();
		} else {
			connection.setRequestMethod("GET");
			connection.connect();
		}
		return connection;
	}

	@Nullable
	public static String getParameters(@Nullable OsmandApplication app,
	                                   @Nullable Map<String, String> parameters,
	                                   @Nullable OnRequestResultListener listener,
	                                   @Nullable String userOperation, boolean toastAllowed) {
		try {
			if (!Algorithms.isEmpty(parameters)) {
				StringBuilder builder = new StringBuilder();
				for (Entry<String, String> entry : parameters.entrySet()) {
					if (builder.length() > 0) {
						builder.append("&");
					}
					builder.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), "UTF-8"));
				}
				return builder.toString();
			}
		} catch (UnsupportedEncodingException e) {
			String error = processIOError(app, userOperation, toastAllowed);
			if (listener != null) {
				listener.onResult(null, error, null);
			}
		}
		return null;
	}

	@NonNull
	private static String processIOError(@Nullable OsmandApplication app, @Nullable String userOperation, boolean toastAllowed) {
		String error = null;
		if (app != null) {
			error = MessageFormat.format(app.getString(R.string.shared_string_action_template)
					+ ": " + app.getString(R.string.shared_string_io_error), userOperation);
		} else {
			error = "Action " + userOperation + ": I/O error";
		}
		if (toastAllowed && app != null) {
			app.showToastMessage(error);
		}
		return error;
	}

	public static Bitmap downloadImage(OsmandApplication ctx, String url) {
		Bitmap res = null;
		try {
			URLConnection connection = NetworkUtils.getHttpURLConnection(url);
			connection.setRequestProperty("User-Agent", Version.getFullVersion(ctx));
			connection.setConnectTimeout(CONNECT_TIMEOUT);
			connection.setReadTimeout(READ_TIMEOUT);
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
		HttpURLConnection connection = null;
		try {
			connection = NetworkUtils.getHttpURLConnection(url);
			connection.setConnectTimeout(CONNECT_TIMEOUT);
			connection.setReadTimeout(READ_TIMEOUT);
			if (gzip) {
				connection.setRequestProperty("Accept-Encoding", "deflate, gzip");
			}
			connection.connect();
			if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
				error = connection.getResponseCode() + " " + connection.getResponseMessage();
			} else if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				return streamToString(connection.getErrorStream());
			} else {
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
			}
		} catch (UnknownHostException e) {
			error = e.getMessage();
			LOG.error("UnknownHostException, cannot download file " + url + " " + error);
		} catch (Exception e) {
			error = e.getMessage();
			if (error == null && e instanceof InterruptedIOException) {
				error = CANCELLED_MSG;
			}
			LOG.warn("Cannot download file: " + url, e);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
		if (progress != null) {
			progress.finishTask();
		}
		return error;
	}

	public static long downloadModifiedFile(
			@NonNull String url, @NonNull File fileToSave, boolean gzip, long lastTime, @Nullable IProgress progress) {
		long result = -1;
		HttpURLConnection connection = null;
		try {
			if (progress != null) {
				progress.startTask(url, 0);
			}
			connection = NetworkUtils.getHttpURLConnection(url);
			connection.setConnectTimeout(CONNECT_TIMEOUT);
			connection.setReadTimeout(READ_TIMEOUT);
			if (gzip) {
				connection.setRequestProperty("Accept-Encoding", "deflate, gzip");
			}
			connection.connect();
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				if (progress != null) {
					progress.finishTask();
				}
				return result;
			} else {
				int bytesDivisor = 1024;
				long lastModified = connection.getLastModified();
				if (lastModified > 0 && lastModified <= lastTime) {
					if (progress != null) {
						progress.finishTask();
					}
					return 0;
				}
				if (progress != null) {
					int work = (int) (connection.getContentLengthLong() / bytesDivisor);
					progress.startWork(work);
				}
				InputStream inputStream = gzip
						? new GZIPInputStream(connection.getInputStream())
						: new BufferedInputStream(connection.getInputStream(), 8 * 1024);
				fileToSave.getParentFile().mkdirs();
				OutputStream stream = null;
				try {
					stream = new FileOutputStream(fileToSave);
					Algorithms.streamCopy(inputStream, stream, progress, bytesDivisor);
					stream.flush();
					result = lastModified > 0 ? lastModified : 1;
				} finally {
					Algorithms.closeStream(inputStream);
					Algorithms.closeStream(stream);
				}
			}
		} catch (UnknownHostException e) {
			LOG.error("UnknownHostException, cannot download file " + url + " " + e.getMessage());
		} catch (Exception e) {
			LOG.warn("Cannot download file: " + url, e);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
		if (progress != null) {
			progress.finishTask();
		}
		return result;
	}

	private static String streamToString(InputStream inputStream) throws IOException {
		StringBuilder result = new StringBuilder();
		if (inputStream != null) {
			BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 1024);
			String buffer;
			boolean f = true;
			while ((buffer = in.readLine()) != null) {
				if (!f) {
					result.append("\n");
				} else {
					f = false;
				}
				result.append(buffer);
			}
			try {
				in.close();
				inputStream.close();
			} catch (Exception e) {
				// ignore exception
			}
		}
		return result.toString();
	}

	private static final String BOUNDARY = "CowMooCowMooCowCowCow";

	@NonNull
	public static NetworkResult uploadFile(@NonNull String urlText, @NonNull File file, boolean gzip,
	                                              @NonNull Map<String, String> additionalParams,
	                                              @Nullable Map<String, String> headers,
	                                              @Nullable IProgress progress) throws IOException {
		return uploadFile(urlText, new FileInputStream(file), file.getName(), gzip, additionalParams, headers, progress);
	}

	@NonNull
	public static NetworkResult uploadFile(@NonNull String urlText, @NonNull InputStream inputStream,
	                                              @NonNull String fileName, boolean gzip,
	                                              @NonNull Map<String, String> additionalParams,
	                                              @Nullable Map<String, String> headers,
	                                              @Nullable IProgress progress) {
		BufferedInputStream bis = new BufferedInputStream(inputStream, 20 * 1024);
		StreamWriter streamWriter = (outputStream, pr) -> {
			Algorithms.streamCopy(bis, outputStream, pr, 1024);
			outputStream.flush();
			Algorithms.closeStream(bis);
		};
		return uploadFile(urlText, streamWriter, fileName, gzip, additionalParams, headers, progress);
	}

	@NonNull
	public static NetworkResult uploadFile(@NonNull String urlText, @NonNull StreamWriter streamWriter,
	                                              @NonNull String fileName, boolean gzip,
	                                              @NonNull Map<String, String> additionalParams,
	                                              @Nullable Map<String, String> headers,
	                                              @Nullable IProgress progress) {
		String error = null;
		String response = null;
		try {
			boolean firstPrm = !urlText.contains("?");
			StringBuilder sb = new StringBuilder(urlText);
			for (Entry<String, String> entry : additionalParams.entrySet()) {
				sb.append(firstPrm ? "?" : "&").append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), "UTF-8"));
				firstPrm = false;
			}
			urlText = sb.toString();

			LOG.info("Start uploading file to " + urlText + " " + fileName);
			URL url = new URL(urlText);

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setChunkedStreamingMode(0);
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

			ous.flush();
			if (gzip) {
				GZIPOutputStream gous = new GZIPOutputStream(ous, 1024);
				streamWriter.write(gous, progress);
				gous.flush();
				gous.finish();
			} else {
				streamWriter.write(ous, progress);
			}
			ous.write(("\r\n--" + BOUNDARY + "--\r\n").getBytes());
			ous.flush();
			Algorithms.closeStream(ous);

			int responseCode = conn.getResponseCode();
			String responseMessage = conn.getResponseMessage();

			LOG.info("Finish uploading file " + fileName);
			LOG.info("Response code and message : " + responseCode + " " + responseMessage);

			if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
				error = responseCode + " " + responseMessage;
			} else if (responseCode != HttpURLConnection.HTTP_OK) {
				InputStream errorStream = conn.getErrorStream();
				error = errorStream != null ? streamToString(errorStream) : responseMessage;
			} else {
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
				response = responseBody.toString();
				LOG.info("Response : " + response);
			}
		} catch (IOException e) {
			error = e.getMessage();
			if (error == null && e instanceof InterruptedIOException) {
				error = CANCELLED_MSG;
			}
			LOG.error(error, e);
		}
		return new NetworkResult(response, error);
	}

	public static void uploadFilesAsync(@NonNull String url,
	                                    @NonNull List<File> files,
	                                    boolean gzip,
	                                    @NonNull Map<String, String> parameters,
	                                    @Nullable Map<String, String> headers,
	                                    OnFilesUploadCallback callback) {
		uploadFilesAsync(url, files, gzip, parameters, headers, callback, AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public static void uploadFilesAsync(@NonNull String url,
	                                    @NonNull List<File> files,
	                                    boolean gzip,
	                                    @NonNull Map<String, String> parameters,
	                                    @Nullable Map<String, String> headers,
	                                    OnFilesUploadCallback callback,
	                                    Executor executor) {
		OsmAndTaskManager.executeTask(new AsyncTask<Void, Object, Map<File, String>>() {

			@Override
			@NonNull
			protected Map<File, String> doInBackground(Void... v) {
				Map<File, String> errors = new HashMap<>();
				for (File file : files) {
					int[] progressValue = {0};
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
						NetworkResult result = uploadFile(url, file, gzip, params, headers, progress);
						if (result.getError() != null) {
							errors.put(file, result.getError());
						}
					} catch (Exception e) {
						errors.put(file, e.getMessage());
					}
					publishProgress(file, -1);
				}
				return errors;
			}

			@Override
			protected void onProgressUpdate(Object... objects) {
				if (callback != null) {
					File file = (File) objects[0];
					Integer progress = (Integer) objects[1];
					if (progress >= 0) {
						callback.onFileUploadProgress(file, progress);
					} else {
						callback.onFileUploadDone(file);
					}
				}
			}

			@Override
			protected void onPostExecute(@NonNull Map<File, String> errors) {
				if (callback != null) {
					callback.onFilesUploadDone(errors);
				}
			}

		}, executor);
	}

	public static UploadFileTask uploadFileAsync(@NonNull String url,
	                                             @NonNull File file,
	                                             @NonNull String fileName,
	                                             boolean gzip,
	                                             @NonNull Map<String, String> parameters,
	                                             @Nullable Map<String, String> headers,
	                                             OnFileUploadCallback callback) throws IOException {
		return uploadFileAsync(url, new FileInputStream(file), fileName, gzip, parameters, headers, callback, AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public static UploadFileTask uploadFileAsync(@NonNull String url,
	                                             @NonNull InputStream inputStream,
	                                             @NonNull String fileName,
	                                             boolean gzip,
	                                             @NonNull Map<String, String> parameters,
	                                             @Nullable Map<String, String> headers,
	                                             OnFileUploadCallback callback) {
		return uploadFileAsync(url, inputStream, fileName, gzip, parameters, headers, callback, AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public static UploadFileTask uploadFileAsync(@NonNull String url,
	                                             @NonNull InputStream inputStream,
	                                             @NonNull String fileName,
	                                             boolean gzip,
	                                             @NonNull Map<String, String> parameters,
	                                             @Nullable Map<String, String> headers,
	                                             OnFileUploadCallback callback,
	                                             Executor executor) {

		BufferedInputStream bis = new BufferedInputStream(inputStream, 20 * 1024);
		StreamWriter streamWriter = (outputStream, progress) -> {
			Algorithms.streamCopy(bis, outputStream, progress, 1024);
			outputStream.flush();
			Algorithms.closeStream(bis);
		};
		return uploadFileAsync(url, streamWriter, fileName, gzip, parameters, headers, callback, executor);
	}

	public static UploadFileTask uploadFileAsync(@NonNull String url,
	                                             @NonNull StreamWriter streamWriter,
	                                             @NonNull String fileName,
	                                             boolean gzip,
	                                             @NonNull Map<String, String> parameters,
	                                             @Nullable Map<String, String> headers,
	                                             OnFileUploadCallback callback) {
		return uploadFileAsync(url, streamWriter, fileName, gzip, parameters, headers, callback, AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public static UploadFileTask uploadFileAsync(@NonNull String url,
	                                             @NonNull StreamWriter streamWriter,
	                                             @NonNull String fileName,
	                                             boolean gzip,
	                                             @NonNull Map<String, String> parameters,
	                                             @Nullable Map<String, String> headers,
	                                             OnFileUploadCallback callback,
	                                             Executor executor) {
		UploadFileTask uploadFileTask = new UploadFileTask(url, streamWriter, fileName, gzip, parameters, headers, callback);
		OsmAndTaskManager.executeTask(uploadFileTask, executor);
		return uploadFileTask;
	}

	public static class NetworkResult {

		private final String error;
		private final String response;

		public NetworkResult(@Nullable String response, @Nullable String error) {
			this.error = error;
			this.response = response;
		}

		@Nullable
		public String getError() {
			return error;
		}

		@Nullable
		public String getResponse() {
			return response;
		}
	}

	public static class Request {
		private final String url;
		private final Map<String, String> parameters;
		private final String userOperation;
		private final boolean toastAllowed;
		private final boolean post;

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

	public abstract static class NetworkProgress implements IProgress {
		@Override
		public void startTask(String taskName, int work) {
		}

		@Override
		public void startWork(int work) {
		}

		@Override
		public void progress(int deltaWork) {
		}

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

	@NonNull
	public static String getHttpProtocol() {
		return Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1 ? "http://" : "https://";
	}
}
