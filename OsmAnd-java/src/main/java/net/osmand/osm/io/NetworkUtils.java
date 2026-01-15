package net.osmand.osm.io;

import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;

import net.osmand.osm.oauth.OsmOAuthAuthorizationClient;
import net.osmand.shared.api.NetworkAPI;
import net.osmand.shared.api.NetworkAPI.NetworkResponse;
import net.osmand.shared.api.NetworkAPIImpl;
import net.osmand.shared.util.PlatformUtil;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
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
	public static String uploadFile(String urlText, File fileToUpload, String userNamePassword,
									OsmOAuthAuthorizationClient client,
									String formName, boolean gzip, Map<String, String> additionalMapData){
		URL url;
		try {
			boolean firstPrm =!urlText.contains("?");
			for (Map.Entry<String, String> entry : additionalMapData.entrySet()) {
				urlText += (firstPrm ? "?" : "&") + entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), "UTF-8");
				firstPrm = false;
			}
			log.info("Start uploading file to " + urlText + " " +fileToUpload.getName());
			url = new URL(urlText);
			HttpURLConnection conn;
			if (client != null && client.isValidToken()) {
				OAuthRequest req = new OAuthRequest(Verb.POST, urlText);
				client.getService().signRequest(client.getAccessToken(), req);
				req.addHeader("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
				try {
					Response r = client.getHttpClient().execute(GPX_UPLOAD_USER_AGENT, req.getHeaders(), req.getVerb(),
							req.getCompleteUrl(), fileToUpload);
					if (r.getCode() != 200) {
						return r.getBody();
					}
					return null;
				} catch (InterruptedException | ExecutionException e) {
					log.error(e);
				}
				return null;
			} else {
				conn = (HttpURLConnection) url.openConnection();
				conn.setDoInput(true);
				conn.setDoOutput(true);
				conn.setRequestMethod("POST");
				if(userNamePassword != null) {
					conn.setRequestProperty("Authorization", "Basic " + Base64.encode(userNamePassword)); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY); //$NON-NLS-1$ //$NON-NLS-2$
			conn.setRequestProperty("User-Agent", "OsmAnd"); //$NON-NLS-1$ //$NON-NLS-2$
			conn.setChunkedStreamingMode(4096);
			OutputStream ous = conn.getOutputStream();
			ous.write(("--" + BOUNDARY + "\r\n").getBytes());
			String filename = fileToUpload.getName();
			if (gzip) {
				filename += ".gz";
			}
			ous.write(("content-disposition: form-data; name=\"" + formName + "\"; filename=\"" + filename + "\"\r\n").getBytes()); //$NON-NLS-1$ //$NON-NLS-2$
			ous.write(("Content-Type: application/octet-stream\r\n\r\n").getBytes()); //$NON-NLS-1$
			InputStream fis = new FileInputStream(fileToUpload);
			BufferedInputStream bis = new BufferedInputStream(fis, 20 * 1024);
			ous.flush();
			if (gzip) {
				GZIPOutputStream gous = new GZIPOutputStream(ous, 1024);
				Algorithms.streamCopy(bis, gous);
				gous.flush();
				gous.finish();
			} else {
				Algorithms.streamCopy(bis, ous);
			}
			ous.write(("\r\n--" + BOUNDARY + "--\r\n").getBytes()); //$NON-NLS-1$ //$NON-NLS-2$
			ous.flush();
			Algorithms.closeStream(bis);
			Algorithms.closeStream(ous);

			log.info("Finish uploading file " + fileToUpload.getName());
			log.info("Response code and message : " + conn.getResponseCode() + " " + conn.getResponseMessage());
			if(conn.getResponseCode() != 200){
				return conn.getResponseMessage();
			}
			InputStream is = conn.getInputStream();
			StringBuilder responseBody = new StringBuilder();
			if (is != null) {
				BufferedReader in = new BufferedReader(new InputStreamReader(is, "UTF-8")); //$NON-NLS-1$
				String s;
				boolean first = true;
				while ((s = in.readLine()) != null) {
					if(first){
						first = false;
					} else {
						responseBody.append("\n"); //$NON-NLS-1$
					}
					responseBody.append(s);
				}
				is.close();
			}
			String response = responseBody.toString();
			log.info("Response : " + response);
			return null;
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			return e.getMessage();
		}
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
		if (networkAPI instanceof NetworkAPIImpl api) {
			return api.getHttpURLConnection(url);
		}
		return (HttpURLConnection) new URL(url).openConnection();
	}
}
