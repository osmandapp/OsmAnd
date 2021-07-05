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
import java.util.concurrent.Executor;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class AndroidNetworkUtils {

	private static final int CONNECTION_TIMEOUT = 15000;
	private static final Log LOG = PlatformUtil.getLog(AndroidNetworkUtils.class);

	public interface OnRequestResultListener {
		void onResult(@Nullable String result, @Nullable String error);
	}

	public interface OnFileUploadCallback {
		void onFileUploadStarted();
		void onFileUploadProgress(int percent);
		void onFileUploadDone(@Nullable String error);
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

	public static void sendRequestsAsync(@Nullable final OsmandApplication ctx,
										 @NonNull final List<Request> requests,
										 @Nullable final OnSendRequestsListener listener) {
		sendRequestsAsync(ctx, requests, listener, AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public static void sendRequestsAsync(@Nullable final OsmandApplication ctx,
										 @NonNull final List<Request> requests,
										 @Nullable final OnSendRequestsListener listener,
										 final Executor executor) {

		new AsyncTask<Void, Object, List<RequestResponse>>() {

			@Override
			protected List<RequestResponse> doInBackground(Void... params) {
				List<RequestResponse> responses = new ArrayList<>();
				for (Request request : requests) {
					RequestResponse requestResponse;
					try {
						publishProgress(request);
						final String[] response = {null, null};
						sendRequest(ctx, request.getUrl(), request.getParameters(),
								request.getUserOperation(), request.isToastAllowed(), request.isPost(), new OnRequestResultListener() {
									@Override
									public void onResult(@Nullable String result, @Nullable String error) {
										response[0] = result;
										response[1] = error;
									}
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

		}.executeOnExecutor(executor, (Void) null);
	}

	public static void sendRequestAsync(final OsmandApplication ctx,
										final String url,
										final Map<String, String> parameters,
										final String userOperation,
										final boolean toastAllowed,
										final boolean post,
										final OnRequestResultListener listener) {
		sendRequestAsync(ctx, url, parameters, userOperation, toastAllowed, post, listener,
				AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public static void sendRequestAsync(final OsmandApplication ctx,
										final String url,
										final Map<String, String> parameters,
										final String userOperation,
										final boolean toastAllowed,
										final boolean post,
										final OnRequestResultListener listener,
										final Executor executor) {
		new AsyncTask<Void, Void, String[]>() {

			@Override
			protected String[] doInBackground(Void... params) {
				final String[] res = {null, null};
				try {
					sendRequest(ctx, url, parameters, userOperation, toastAllowed, post, new OnRequestResultListener() {
						@Override
						public void onResult(@Nullable String result, @Nullable String error) {
							res[0] = result;
							res[1] = error;
						}
					});
				} catch (Exception e) {
					// ignore
				}
				return res;
			}

			@Override
			protected void onPostExecute(String[] response) {
				if (listener != null) {
					listener.onResult(response[0], response[1]);
				}
			}

		}.executeOnExecutor(executor, (Void) null);
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

	public static Map<File, String> downloadFiles(final @NonNull String url,
												  final @NonNull List<File> files,
												  final @NonNull Map<String, String> parameters,
												  final @Nullable OnFilesDownloadCallback callback) {
		Map<File, String> errors = new HashMap<>();
		for (final File file : files) {
			final int[] progressValue = {0};
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

	public static void downloadFilesAsync(final @NonNull String url,
										  final @NonNull List<File> files,
										  final @NonNull Map<String, String> parameters,
										  final @Nullable OnFilesDownloadCallback callback) {
		downloadFilesAsync(url, files, parameters, callback, AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public static void downloadFilesAsync(final @NonNull String url,
										  final @NonNull List<File> files,
										  final @NonNull Map<String, String> parameters,
										  final @Nullable OnFilesDownloadCallback callback,
										  final Executor executor) {

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

		}.executeOnExecutor(executor, (Void) null);
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

	public static String sendRequest(@Nullable OsmandApplication ctx, @NonNull String url,
									 @Nullable Map<String, String> parameters,
									 @Nullable String userOperation, boolean toastAllowed, boolean post,
									 @Nullable OnRequestResultListener listener) {
		String result = null;
		String error = null;
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
				if (ctx != null) {
					error = (!Algorithms.isEmpty(userOperation) ? userOperation + " " : "")
							+ ctx.getString(R.string.failed_op) + ": " + connection.getResponseMessage();
				} else {
					error = (!Algorithms.isEmpty(userOperation) ? userOperation + " " : "")
							+ "failed: " + connection.getResponseMessage();
				}
				if (toastAllowed && ctx != null) {
					showToast(ctx, error);
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
			if (ctx != null) {
				error = ctx.getString(R.string.auth_failed);
			} else {
				error = "Authorization failed";
			}
			if (toastAllowed && ctx != null) {
				showToast(ctx, error);
			}
		} catch (MalformedURLException e) {
			if (ctx != null) {
				error = MessageFormat.format(ctx.getResources().getString(R.string.shared_string_action_template)
						+ ": " + ctx.getResources().getString(R.string.shared_string_unexpected_error), userOperation);
			} else {
				error = "Action " + userOperation + ": Unexpected error";
			}
			if (toastAllowed && ctx != null) {
				showToast(ctx, error);
			}
		} catch (IOException e) {
			if (ctx != null) {
				error = MessageFormat.format(ctx.getResources().getString(R.string.shared_string_action_template)
						+ ": " + ctx.getResources().getString(R.string.shared_string_io_error), userOperation);
			} else {
				error = "Action " + userOperation + ": I/O error";
			}
			if (toastAllowed && ctx != null) {
				showToast(ctx, error);
			}
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
		if (listener != null) {
			listener.onResult(result, error);
		}
		return result;
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
			HttpURLConnection connection = NetworkUtils.getHttpURLConnection(url);
			connection.setConnectTimeout(CONNECTION_TIMEOUT);
			connection.setReadTimeout(CONNECTION_TIMEOUT);
			if (gzip) {
				connection.setRequestProperty("Accept-Encoding", "deflate, gzip");
			}
			connection.connect();
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
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
			LOG.warn("Cannot download file: " + url, e);
		}
		return error;
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
		BufferedInputStream bis = new BufferedInputStream(inputStream, 20 * 1024);
		StreamWriter streamWriter = new StreamWriter() {
			@Override
			public void write(OutputStream outputStream, IProgress progress) throws IOException {
				Algorithms.streamCopy(bis, outputStream, progress, 1024);
				outputStream.flush();
				Algorithms.closeStream(bis);
			}
		};
		return uploadFile(urlText, streamWriter, fileName, gzip, additionalParams, headers, progress);
	}

	public static String uploadFile(@NonNull String urlText, @NonNull StreamWriter streamWriter,
									@NonNull String fileName, boolean gzip,
									@NonNull Map<String, String> additionalParams,
									@Nullable Map<String, String> headers,
									@Nullable IProgress progress) {
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

			LOG.info("Finish uploading file " + fileName);
			LOG.info("Response code and message : " + conn.getResponseCode() + " " + conn.getResponseMessage());
			if (conn.getResponseCode() != 200) {
				InputStream errorStream = conn.getErrorStream();
				if (errorStream != null) {
					return streamToString(errorStream);
				}
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
		uploadFilesAsync(url, files, gzip, parameters, headers, callback, AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public static void uploadFilesAsync(final @NonNull String url,
										final @NonNull List<File> files,
										final boolean gzip,
										final @NonNull Map<String, String> parameters,
										final @Nullable Map<String, String> headers,
										final OnFilesUploadCallback callback,
										final Executor executor) {

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

		}.executeOnExecutor(executor, (Void) null);
	}

	public static void uploadFileAsync(final @NonNull String url,
										final @NonNull InputStream inputStream,
										final @NonNull String fileName,
										final boolean gzip,
										final @NonNull Map<String, String> parameters,
										final @Nullable Map<String, String> headers,
										final OnFileUploadCallback callback) {
		uploadFileAsync(url, inputStream, fileName, gzip, parameters, headers, callback, AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public static void uploadFileAsync(final @NonNull String url,
										final @NonNull InputStream inputStream,
										final @NonNull String fileName,
										final boolean gzip,
										final @NonNull Map<String, String> parameters,
										final @Nullable Map<String, String> headers,
										final OnFileUploadCallback callback,
										final Executor executor) {

		BufferedInputStream bis = new BufferedInputStream(inputStream, 20 * 1024);
		StreamWriter streamWriter = new StreamWriter() {
			@Override
			public void write(OutputStream outputStream, IProgress progress) throws IOException {
				Algorithms.streamCopy(bis, outputStream, progress, 1024);
				outputStream.flush();
				Algorithms.closeStream(bis);
			}
		};
		uploadFileAsync(url, streamWriter, fileName, gzip, parameters, headers, callback, executor);
	}

	public static void uploadFileAsync(final @NonNull String url,
										final @NonNull StreamWriter streamWriter,
										final @NonNull String fileName,
										final boolean gzip,
										final @NonNull Map<String, String> parameters,
										final @Nullable Map<String, String> headers,
										final OnFileUploadCallback callback) {
		uploadFileAsync(url, streamWriter, fileName, gzip, parameters, headers, callback, AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public static void uploadFileAsync(final @NonNull String url,
										final @NonNull StreamWriter streamWriter,
										final @NonNull String fileName,
										final boolean gzip,
										final @NonNull Map<String, String> parameters,
										final @Nullable Map<String, String> headers,
										final OnFileUploadCallback callback,
										final Executor executor) {

		new AsyncTask<Void, Integer, String>() {

			@Override
			protected void onPreExecute() {
				if (callback != null) {
					callback.onFileUploadStarted();
				}
			}

			@Override
			@Nullable
			protected String doInBackground(Void... v) {
				String error;
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
				try {
					error = uploadFile(url, streamWriter, fileName, gzip, parameters, headers, progress);
				} catch (Exception e) {
					error = e.getMessage();
				}
				return error;
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
			protected void onPostExecute(@Nullable String error) {
				if (callback != null) {
					callback.onFileUploadDone(error);
				}
			}

		}.executeOnExecutor(executor, (Void) null);
	}

	private static void showToast(OsmandApplication ctx, String message) {
		ctx.showToastMessage(message);
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
