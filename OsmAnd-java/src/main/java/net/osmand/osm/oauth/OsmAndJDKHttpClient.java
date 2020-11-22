package net.osmand.osm.oauth;

import com.github.scribejava.core.exceptions.OAuthException;
import com.github.scribejava.core.httpclient.HttpClient;
import com.github.scribejava.core.httpclient.jdk.JDKHttpClientConfig;
import com.github.scribejava.core.httpclient.jdk.JDKHttpFuture;
import com.github.scribejava.core.httpclient.multipart.MultipartPayload;
import com.github.scribejava.core.httpclient.multipart.MultipartUtils;
import com.github.scribejava.core.model.*;
import net.osmand.util.Algorithms;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class OsmAndJDKHttpClient implements HttpClient {
	private static final String BOUNDARY = "CowMooCowMooCowCowCow";
	private final JDKHttpClientConfig config;

	public OsmAndJDKHttpClient() {
		this(JDKHttpClientConfig.defaultConfig());
	}

	public OsmAndJDKHttpClient(JDKHttpClientConfig clientConfig) {
		config = clientConfig;
	}

	@Override
	public void close() {
	}

	@Override
	public <T> Future<T> executeAsync(String userAgent, Map<String, String> headers, Verb httpVerb, String completeUrl,
	                                  byte[] bodyContents, OAuthAsyncRequestCallback<T> callback, OAuthRequest.ResponseConverter<T> converter) {

		return doExecuteAsync(userAgent, headers, httpVerb, completeUrl, BodyType.BYTE_ARRAY, bodyContents, callback,
				converter);
	}

	@Override
	public <T> Future<T> executeAsync(String userAgent, Map<String, String> headers, Verb httpVerb, String completeUrl,
	                                  MultipartPayload bodyContents, OAuthAsyncRequestCallback<T> callback,
	                                  OAuthRequest.ResponseConverter<T> converter) {

		return doExecuteAsync(userAgent, headers, httpVerb, completeUrl, BodyType.MULTIPART, bodyContents, callback,
				converter);
	}

	@Override
	public <T> Future<T> executeAsync(String userAgent, Map<String, String> headers, Verb httpVerb, String completeUrl,
	                                  String bodyContents, OAuthAsyncRequestCallback<T> callback, OAuthRequest.ResponseConverter<T> converter) {

		return doExecuteAsync(userAgent, headers, httpVerb, completeUrl, BodyType.STRING, bodyContents, callback,
				converter);
	}

	@Override
	public <T> Future<T> executeAsync(String userAgent, Map<String, String> headers, Verb httpVerb, String completeUrl,
	                                  File bodyContents, OAuthAsyncRequestCallback<T> callback, OAuthRequest.ResponseConverter<T> converter) {
		return doExecuteAsync(userAgent, headers, httpVerb, completeUrl, BodyType.STREAM, bodyContents, callback,
				converter);
	}

	private <T> Future<T> doExecuteAsync(String userAgent, Map<String, String> headers, Verb httpVerb,
	                                     String completeUrl, BodyType bodyType, Object bodyContents, OAuthAsyncRequestCallback<T> callback,
	                                     OAuthRequest.ResponseConverter<T> converter) {
		try {
			final Response response = doExecute(userAgent, headers, httpVerb, completeUrl, bodyType, bodyContents);
			@SuppressWarnings("unchecked") final T t = converter == null ? (T) response : converter.convert(response);
			if (callback != null) {
				callback.onCompleted(t);
			}
			return new JDKHttpFuture<>(t);
		} catch (IOException | RuntimeException e) {
			if (callback != null) {
				callback.onThrowable(e);
			}
			return new JDKHttpFuture<>(e);
		}
	}

	@Override
	public Response execute(String userAgent, Map<String, String> headers, Verb httpVerb, String completeUrl,
	                        byte[] bodyContents) throws InterruptedException, ExecutionException, IOException {
		return doExecute(userAgent, headers, httpVerb, completeUrl, BodyType.BYTE_ARRAY, bodyContents);
	}

	@Override
	public Response execute(String userAgent, Map<String, String> headers, Verb httpVerb, String completeUrl,
	                        MultipartPayload multipartPayloads) throws InterruptedException, ExecutionException, IOException {
		return doExecute(userAgent, headers, httpVerb, completeUrl, BodyType.MULTIPART, multipartPayloads);
	}

	@Override
	public Response execute(String userAgent, Map<String, String> headers, Verb httpVerb, String completeUrl,
	                        String bodyContents) throws InterruptedException, ExecutionException, IOException {
		return doExecute(userAgent, headers, httpVerb, completeUrl, BodyType.STRING, bodyContents);
	}

	@Override
	public Response execute(String userAgent, Map<String, String> headers, Verb httpVerb, String completeUrl,
	                        File bodyContents) throws InterruptedException, ExecutionException, IOException {
		return doExecute(userAgent, headers, httpVerb, completeUrl, BodyType.STREAM, bodyContents);
	}

	private Response doExecute(String userAgent, Map<String, String> headers, Verb httpVerb, String completeUrl,
	                           BodyType bodyType, Object bodyContents) throws IOException {
		final URL url = new URL(completeUrl);
		final HttpURLConnection connection;
		if (config.getProxy() == null) {
			connection = (HttpURLConnection) url.openConnection();
		} else {
			connection = (HttpURLConnection) url.openConnection(config.getProxy());
		}
		connection.setInstanceFollowRedirects(config.isFollowRedirects());
		connection.setRequestMethod(httpVerb.name());
		if (config.getConnectTimeout() != null) {
			connection.setConnectTimeout(config.getConnectTimeout());
		}
		if (config.getReadTimeout() != null) {
			connection.setReadTimeout(config.getReadTimeout());
		}
		addHeaders(connection, headers, userAgent);
		if (httpVerb.isPermitBody()) {
			bodyType.setBody(connection, bodyContents, httpVerb.isRequiresBody());
		}

		try {
			connection.connect();
			final int responseCode = connection.getResponseCode();
			return new Response(responseCode, connection.getResponseMessage(), parseHeaders(connection),
					responseCode >= 200 && responseCode < 400 ? connection.getInputStream()
							: connection.getErrorStream());
		} catch (UnknownHostException e) {
			throw new OAuthException("The IP address of a host could not be determined.", e);
		}
	}

	private enum BodyType {
		BYTE_ARRAY {
			@Override
			void setBody(HttpURLConnection connection, Object bodyContents, boolean requiresBody) throws IOException {
				addBody(connection, (byte[]) bodyContents, requiresBody);
			}
		},
		STREAM {
			@Override
			void setBody(HttpURLConnection connection, Object bodyContents, boolean requiresBody) throws IOException {
				addBody(connection, (File) bodyContents, requiresBody);
			}
		},
		MULTIPART {
			@Override
			void setBody(HttpURLConnection connection, Object bodyContents, boolean requiresBody) throws IOException {
				addBody(connection, (MultipartPayload) bodyContents, requiresBody);
			}
		},
		STRING {
			@Override
			void setBody(HttpURLConnection connection, Object bodyContents, boolean requiresBody) throws IOException {
				addBody(connection, ((String) bodyContents).getBytes(), requiresBody);
			}
		};

		abstract void setBody(HttpURLConnection connection, Object bodyContents, boolean requiresBody)
				throws IOException;
	}

	private static Map<String, String> parseHeaders(HttpURLConnection conn) {
		final Map<String, String> headers = new HashMap<>();

		for (Map.Entry<String, List<String>> headerField : conn.getHeaderFields().entrySet()) {
			final String key = headerField.getKey();
			final String value = headerField.getValue().get(0);
			if ("Content-Encoding".equalsIgnoreCase(key)) {
				headers.put("Content-Encoding", value);
			} else {
				headers.put(key, value);
			}
		}
		return headers;
	}

	private static void addHeaders(HttpURLConnection connection, Map<String, String> headers, String userAgent) {
		for (Map.Entry<String, String> header : headers.entrySet()) {
			connection.setRequestProperty(header.getKey(), header.getValue());
		}

		if (userAgent != null) {
			connection.setRequestProperty(OAuthConstants.USER_AGENT_HEADER_NAME, userAgent);
		}
	}

	private static void addBody(HttpURLConnection connection, File file, boolean requiresBody) throws IOException {
		if (requiresBody) {
			String filename = file.getName();
			String formName = "file";
			InputStream stream = new FileInputStream(file);
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY); //$NON-NLS-1$ //$NON-NLS-2$
			connection.setRequestProperty("User-Agent", "OsmAnd"); //$NON-NLS-1$ //$NON-NLS-2$
			final OutputStream ous = connection.getOutputStream();
			ous.write(("--" + BOUNDARY + "\r\n").getBytes());
			ous.write(("content-disposition: form-data; name=\"" + formName + "\"; filename=\"" + filename + "\"\r\n").getBytes()); //$NON-NLS-1$ //$NON-NLS-2$
			ous.write(("Content-Type: application/octet-stream\r\n\r\n").getBytes()); //$NON-NLS-1$
			BufferedInputStream bis = new BufferedInputStream(stream, 20 * 1024);
			ous.flush();
			Algorithms.streamCopy(bis, ous);
			ous.write(("\r\n--" + BOUNDARY + "--\r\n").getBytes()); //$NON-NLS-1$ //$NON-NLS-2$
			ous.flush();
			Algorithms.closeStream(bis);
		}
	}

	private static void addBody(HttpURLConnection connection, byte[] content, boolean requiresBody) throws IOException {
		final int contentLength = content.length;
		if (requiresBody || contentLength > 0) {
			connection.setDoOutput(true);
			final OutputStream outputStream = prepareConnectionForBodyAndGetOutputStream(connection, contentLength);
			if (contentLength > 0) {
				outputStream.write(content);
			}
		}
	}

	private static void addBody(HttpURLConnection connection, MultipartPayload multipartPayload, boolean requiresBody)
			throws IOException {

		for (Map.Entry<String, String> header : multipartPayload.getHeaders().entrySet()) {
			connection.setRequestProperty(header.getKey(), header.getValue());
		}

		if (requiresBody) {
			final ByteArrayOutputStream os = MultipartUtils.getPayload(multipartPayload);
			final int contentLength = os.size();
			connection.setDoOutput(true);
			final OutputStream outputStream = prepareConnectionForBodyAndGetOutputStream(connection, contentLength);
			if (contentLength > 0) {
				os.writeTo(outputStream);
			}
		}
	}

	private static OutputStream prepareConnectionForBodyAndGetOutputStream(HttpURLConnection connection,
	                                                                       int contentLength) throws IOException {
		connection.setRequestProperty(CONTENT_LENGTH, String.valueOf(contentLength));
		if (connection.getRequestProperty(CONTENT_TYPE) == null) {
			connection.setRequestProperty(CONTENT_TYPE, DEFAULT_CONTENT_TYPE);
		}
		return connection.getOutputStream();
	}
}