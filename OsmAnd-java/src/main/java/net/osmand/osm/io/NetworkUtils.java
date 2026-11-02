package net.osmand.osm.io;

import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;

import net.osmand.osm.oauth.OsmOAuthAuthorizationClient;
import net.osmand.shared.api.NetworkAPI;
import net.osmand.shared.api.NetworkAPI.NetworkResponse;
import net.osmand.shared.util.PlatformUtil;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class NetworkUtils {

	private static final Log log = net.osmand.PlatformUtil.getLog(NetworkUtils.class);
	private static final String GPX_UPLOAD_USER_AGENT = "OsmGPXUploadAgent";

	public static NetworkResponse sendGetRequest(String url, String auth) {
		return sendGetRequest(url, auth, false);
	}

	public static NetworkResponse sendGetRequest(String url, String auth, boolean useGzip) {
		return net.osmand.shared.util.PlatformUtil.INSTANCE.getNetworkAPI().sendGetRequest(url, auth, useGzip, "OsmAnd");
	}

	public static String sendPostDataRequest(String urlText, String formName, String fileName, InputStream data) {
		try {
			log.info("POST : " + urlText);
			HttpURLConnection conn = getHttpURLConnection(urlText);
			conn.setDoInput(true);
			conn.setDoOutput(false);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Accept", "*/*");
			conn.setRequestProperty("User-Agent", "OsmAnd"); //$NON-NLS-1$ //$NON-NLS-2$
			conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
			OutputStream ous = conn.getOutputStream();
			ous.write(("--" + BOUNDARY + "\r\n").getBytes());
			ous.write(("Content-Disposition: form-data; name=\"" + formName + "\"; filename=\"" + fileName + "\"\r\n").getBytes()); //$NON-NLS-1$ //$NON-NLS-2$
			ous.write(("Content-Type: application/octet-stream\r\n\r\n").getBytes()); //$NON-NLS-1$
			Algorithms.streamCopy(data, ous);
			ous.write(("\r\n--" + BOUNDARY + "--\r\n").getBytes()); //$NON-NLS-1$ //$NON-NLS-2$
			ous.flush();
			log.info("Response code and message : " + conn.getResponseCode() + " " + conn.getResponseMessage());
			if (conn.getResponseCode() != 200) {
				return null;
			}
			StringBuilder responseBody = new StringBuilder();
			InputStream is = conn.getInputStream();
			responseBody.setLength(0);
			if (is != null) {
				BufferedReader in = new BufferedReader(new InputStreamReader(is, "UTF-8")); //$NON-NLS-1$
				String s;
				boolean first = true;
				while ((s = in.readLine()) != null) {
					if (first) {
						first = false;
					} else {
						responseBody.append("\n"); //$NON-NLS-1$
					}
					responseBody.append(s);
				}
				is.close();
			}
			Algorithms.closeStream(is);
			Algorithms.closeStream(data);
			Algorithms.closeStream(ous);
			return responseBody.toString();
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			return e.getMessage();
		}
	}

	private static final String BOUNDARY = "CowMooCowMooCowCowCow"; //$NON-NLS-1$
	private static final int MAX_ERROR_MESSAGE_LENGTH = 500;
	public static String uploadFile(String urlText, File fileToUpload, String userNamePassword,
									OsmOAuthAuthorizationClient client,
									String formName, boolean gzip, Map<String, String> additionalMapData){
		HttpURLConnection conn = null;
		try {
			log.info("Start uploading file to " + urlText + " " +fileToUpload.getName());
			conn = getHttpURLConnection(urlText);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");

			if (client != null && client.isValidToken()) {
				OAuthRequest req = new OAuthRequest(Verb.POST, urlText);
				client.getService().signRequest(client.getAccessToken(), req);
				for (Map.Entry<String, String> header : req.getHeaders().entrySet()) {
					conn.setRequestProperty(header.getKey(), header.getValue());
				}
				conn.setRequestProperty("User-Agent", GPX_UPLOAD_USER_AGENT);
			} else {
				if(userNamePassword != null) {
					conn.setRequestProperty("Authorization", "Basic " + Base64.encode(userNamePassword)); //$NON-NLS-1$ //$NON-NLS-2$
				}
				conn.setRequestProperty("User-Agent", "OsmAnd"); //$NON-NLS-1$ //$NON-NLS-2$
			}

			conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY); //$NON-NLS-1$ //$NON-NLS-2$
			conn.setChunkedStreamingMode(4096);
			OutputStream ous = conn.getOutputStream();
			try {
				writeMultipartBody(ous, fileToUpload, formName, gzip, additionalMapData);
			} finally {
				Algorithms.closeStream(ous);
			}

			log.info("Finish uploading file " + fileToUpload.getName());
			log.info("Response code and message : " + conn.getResponseCode() + " " + conn.getResponseMessage());
			int responseCode = conn.getResponseCode();
			if (responseCode != 200) {
				String err = readPlainTextError(conn);
				if (err == null) {
					err = conn.getResponseMessage();
				}
				// null result means success, so never return an empty error for a failed upload
				return Algorithms.isEmpty(err) ? "HTTP " + responseCode : err;
			}
			InputStream is = conn.getInputStream();
			StringBuilder responseBody = new StringBuilder();
			if (is != null) {
				try (BufferedReader in = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) { //$NON-NLS-1$
					String s;
					boolean first = true;
					while ((s = in.readLine()) != null) {
						if (first) {
							first = false;
						} else {
							responseBody.append("\n"); //$NON-NLS-1$
						}
						responseBody.append(s);
					}
				}
			}
			String response = responseBody.toString();
			log.info("Response : " + response);
			return null;
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			return e.getMessage() != null ? e.getMessage() : e.toString();
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	private static String readPlainTextError(HttpURLConnection conn) {
		// OSM API reports errors as text/plain; anything else (e.g. proxy HTML pages) is not shown to the user
		String contentType = conn.getContentType();
		if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("text/plain")) {
			return null;
		}
		InputStream errorStream = conn.getErrorStream();
		if (errorStream == null) {
			return null;
		}
		try (Reader reader = new InputStreamReader(errorStream, StandardCharsets.UTF_8)) {
			// read at most one char over the limit to detect truncation without buffering the whole body
			char[] buf = new char[MAX_ERROR_MESSAGE_LENGTH + 1];
			int len = 0;
			int n;
			while (len < buf.length && (n = reader.read(buf, len, buf.length - len)) != -1) {
				len += n;
			}
			boolean truncated = len > MAX_ERROR_MESSAGE_LENGTH;
			String err = new String(buf, 0, Math.min(len, MAX_ERROR_MESSAGE_LENGTH)).trim();
			if (err.isEmpty()) {
				return null;
			}
			return truncated ? err + "..." : err;
		} catch (IOException e) {
			return null;
		}
	}

	// same escaping as browsers apply to multipart/form-data names and filenames (HTML spec)
	static String escapeQuotedValue(String value) {
		return value.replace("\"", "%22").replace("\r", "%0D").replace("\n", "%0A");
	}

	private static class NonClosingOutputStream extends FilterOutputStream {

		NonClosingOutputStream(OutputStream out) {
			super(out);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			out.write(b, off, len);
		}

		@Override
		public void close() throws IOException {
			flush();
		}
	}

	private static void writeMultipartBody(OutputStream ous, File fileToUpload, String formName, boolean gzip,
										   Map<String, String> additionalMapData) throws IOException {
		if (additionalMapData != null) {
			for (Map.Entry<String, String> entry : additionalMapData.entrySet()) {
				if (entry.getValue() == null) {
					// omit field so server applies its default instead of receiving an empty string
					continue;
				}
				ous.write(("--" + BOUNDARY + "\r\n").getBytes(StandardCharsets.UTF_8));
				ous.write(("Content-Disposition: form-data; name=\"" + escapeQuotedValue(entry.getKey()) + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
				ous.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
				ous.write("\r\n".getBytes(StandardCharsets.UTF_8));
			}
		}
		String filename = fileToUpload.getName();
		if (gzip) {
			filename += ".gz";
		}
		ous.write(("--" + BOUNDARY + "\r\n").getBytes(StandardCharsets.UTF_8));
		ous.write(("Content-Disposition: form-data; name=\"" + escapeQuotedValue(formName) + "\"; filename=\"" + escapeQuotedValue(filename) + "\"\r\n").getBytes(StandardCharsets.UTF_8));
		ous.write(("Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
		InputStream fis = new FileInputStream(fileToUpload);
		BufferedInputStream bis = new BufferedInputStream(fis, 20 * 1024);
		try {
			if (gzip) {
				// closing gzip stream releases native Deflater memory but must keep connection stream open
				try (GZIPOutputStream gous = new GZIPOutputStream(new NonClosingOutputStream(ous), 1024)) {
					Algorithms.streamCopy(bis, gous);
				}
			} else {
				Algorithms.streamCopy(bis, ous);
			}
		} finally {
			Algorithms.closeStream(bis);
		}
		ous.write(("\r\n--" + BOUNDARY + "--\r\n").getBytes(StandardCharsets.UTF_8));
		ous.flush();
	}

	public static void main(String[] args) {
		File myFile = new File("/Users/macmini/Downloads/great-britain-latest.osm.pbf");
		String type = "pbf-big";
		String serverUrl = "http://localhost:8080/userdata/upload-file?name="+myFile.getName()+"&type="+type+"&deviceid=2&accessToken=dd8d3693-4812-440a-8042-b0d848310e23";
		Map<String, String> additionalMapData = new HashMap<>();
		uploadFile(serverUrl, myFile, null, null, "file", true, additionalMapData);
	}

	public static void setProxy(String host, int port) {
		PlatformUtil.INSTANCE.getNetworkAPI().setProxy(host, port);
	}

	public static boolean hasProxy() {
		return PlatformUtil.INSTANCE.getNetworkAPI().hasProxy();
	}

	public static HttpURLConnection getHttpURLConnection(URL url) throws IOException {
		return getHttpURLConnection(url.toString());
	}

	public static HttpURLConnection getHttpURLConnection(String url) throws MalformedURLException, IOException {
		NetworkAPI networkAPI = PlatformUtil.INSTANCE.getNetworkAPI();
		String proxyHost = networkAPI.getProxyHost();
		int proxyPort = networkAPI.getProxyPort();
		if (!Algorithms.isEmpty(proxyHost) && proxyPort > 0) {
			Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
			return (HttpURLConnection) new URL(url).openConnection(proxy);
		}
		return (HttpURLConnection) new URL(url).openConnection();
	}
}
