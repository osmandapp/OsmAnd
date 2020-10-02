package net.osmand.osm.io;

import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import net.osmand.PlatformUtil;
import net.osmand.osm.oauth.OsmOAuthAuthorizationClient;
import net.osmand.util.Algorithms;
import org.apache.commons.logging.Log;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.zip.GZIPOutputStream;

public class NetworkUtils {
	private static final Log log = PlatformUtil.getLog(NetworkUtils.class);

	private static Proxy proxy = null;

	public static String sendGetRequest(String urlText, String userNamePassword, StringBuilder responseBody){
		try {
			log.info("GET : " + urlText);
			HttpURLConnection conn = getHttpURLConnection(urlText);
			conn.setDoInput(true);
			conn.setDoOutput(false);
			conn.setRequestMethod("GET");
			if(userNamePassword != null) {
				conn.setRequestProperty("Authorization", "Basic " + Base64.encode(userNamePassword)); //$NON-NLS-1$ //$NON-NLS-2$
			}
	        conn.setRequestProperty("User-Agent", "OsmAnd"); //$NON-NLS-1$ //$NON-NLS-2$
			log.info("Response code and message : " + conn.getResponseCode() + " " + conn.getResponseMessage());
			if(conn.getResponseCode() != 200){
				return conn.getResponseMessage();
			}
			InputStream is = conn.getInputStream();
			responseBody.setLength(0);
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
			return null;
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
			for (String key : additionalMapData.keySet()) {
				urlText += (firstPrm ? "?" : "&") + key + "=" + URLEncoder.encode(additionalMapData.get(key), "UTF-8");
				firstPrm = false;
			}
			log.info("Start uploading file to " + urlText + " " +fileToUpload.getName());
			url = new URL(urlText);
			HttpURLConnection conn;
			if (client != null && client.isValidToken()){
				OAuthRequest req = new OAuthRequest(Verb.POST, urlText);
				req.setPayload(prepareStream(formName,fileToUpload,gzip));
				client.getService().signRequest(client.getAccessToken(), req);
				req.addHeader("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
				try {
					Response r = client.getService().execute(req);
					if(r.getCode() != 200){
						return r.getBody();
					}
					return null;
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
				return null;
			}
			else {
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
	        OutputStream ous = conn.getOutputStream();
			ous.write(prepareStream(formName,fileToUpload,gzip));
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

    private static byte[] prepareStream(String formName, File fileToUpload, boolean gzip) {
        try {
            ByteArrayOutputStream ous = new ByteArrayOutputStream();
//			for (String key : additionalMapData.keySet()) {
//				ous.write(("--" + BOUNDARY + "\r\n").getBytes());
//				ous.write(("content-disposition: form-data; name=\"" + key + "\"\r\n").getBytes()); //$NON-NLS-1$ //$NON-NLS-2$
//				ous.write((additionalMapData.get(key) + "\r\n").getBytes());
//			}
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
				gous.close();
			} else {
                Algorithms.streamCopy(bis, ous);
            }

            ous.write(("\r\n--" + BOUNDARY + "--\r\n").getBytes()); //$NON-NLS-1$ //$NON-NLS-2$
            ous.flush();
            Algorithms.closeStream(bis);
            Algorithms.closeStream(ous);
            return ous.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

	public static void setProxy(String host, int port) {
		if(host != null && port > 0) {
			InetSocketAddress isa = new InetSocketAddress(host, port);
			proxy = new Proxy(Proxy.Type.HTTP, isa);
		} else {
			proxy = null;
		}
	}

	public static Proxy getProxy() {
		return proxy;
	}

	public static HttpURLConnection getHttpURLConnection(String urlString) throws MalformedURLException, IOException {
		return getHttpURLConnection(new URL(urlString));
	}

	public static HttpURLConnection getHttpURLConnection(URL url) throws IOException {
		if (proxy != null) {
			return (HttpURLConnection) url.openConnection(proxy);
		} else {
			return (HttpURLConnection) url.openConnection();
		}
	}
}
