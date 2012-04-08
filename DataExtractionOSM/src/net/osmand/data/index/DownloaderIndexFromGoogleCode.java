package net.osmand.data.index;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.osmand.LogUtil;
import net.osmand.data.index.ExtractGooglecodeAuthorization.GooglecodeUploadTokens;

import org.apache.commons.logging.Log;


public class DownloaderIndexFromGoogleCode {

	private final static Log log = LogUtil.getLog(DownloaderIndexFromGoogleCode.class);

	public static void main(String[] args) {
		System.out.println(DownloaderIndexFromGoogleCode.getIndexFiles(new LinkedHashMap<String, String>()));
	}
	
	private static Map<String, String> getContent(Map<String, String> files,
			String... ext) {
		BufferedReader reader = null;
		int num = 400;
		int start = 0;
		boolean downloadNext = true;
		while (downloadNext) {
			downloadNext = false;
			try {
				URL url = new URL(
						"http://code.google.com/p/osmand/downloads/list?num=" + num + "&start=" + start); //$NON-NLS-1$ //$NON-NLS-2$
				reader = new BufferedReader(new InputStreamReader(url
						.openStream()));
				String s = null;
				String prevFile = null;
				while ((s = reader.readLine()) != null) {
					boolean hrefDownload = s.indexOf("files") != -1; //$NON-NLS-1$
					if (hrefDownload || s.indexOf("{") != -1) { //$NON-NLS-1$
						downloadNext |= hrefDownload;
						for (String extension : ext) {
							prevFile = getIndexFiles(files, s, prevFile, extension);
						}
					}
				}
				start += num + 1;
			} catch (MalformedURLException e) {
				log.error("Unexpected exception", e); //$NON-NLS-1$
			} catch (IOException e) {
				log.error("Input/Output exception", e); //$NON-NLS-1$
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						log.error("Error closing stream to url.", e); //$NON-NLS-1$
					}
				}
			}
		}
		log.info("Loaded indexes:" + files.size()); //$NON-NLS-1$
		return files;
	}
	

	private static String getIndexFiles(Map<String, String> files, String content, String prevFile, String ext){
		int i = content.indexOf(ext, 0);
		if (i != -1) {
			int j = i - 1;
			while (content.charAt(j) == '_' || Character.isLetterOrDigit(content.charAt(j)) || content.charAt(j) == '-' || content.charAt(j) == '.') {
				j--;
			}
			prevFile = content.substring(j + 1, i) + ext;

		}
		if (prevFile != null && ((i = content.indexOf('{')) != -1)) {
			int j = content.indexOf('}');
			if (j != -1 && j - i < 40) {
				// String description = content.substring(i, j + 1);
				files.put(prevFile, content);
				prevFile = null;
			}
		}
		return prevFile;
	}
	
	public static Map<String, String> getIndexFiles(Map<String, String> files){
		return getContent(files, ".obf", ".zip", ".zip-1", ".zip-2", ".zip-3", ".zip-4");
	}
	
	public static URL getInputStreamToLoadIndex(String indexName) throws IOException{
		URL url = new URL("http://osmand.googlecode.com/files/"+indexName); //$NON-NLS-1$
		return url;
	}
	
	
	public static String deleteFileFromGoogleDownloads(String fileName, GooglecodeUploadTokens ggtokens) throws IOException {
		// prepare data
		Map<String, String> cookies = new HashMap<String, String>();
		cookies.put("HSID", ggtokens.getHsid());  //$NON-NLS-1$
		cookies.put("SID", ggtokens.getSid()); //$NON-NLS-1$
		StringBuilder cookieString = new StringBuilder();
		int size = cookies.size();
		for (String c : cookies.keySet()) {
			size--;
			cookieString.append(c).append("=").append(cookies.get(c)); //$NON-NLS-1$
			if (size > 0) {
				cookieString.append("; "); //$NON-NLS-1$
			}
		}
		
		String urlText = "http://code.google.com/p/osmand/downloads/delete.do?name="+fileName; //$NON-NLS-1$
		log.info("Url to delete :" + urlText);
		StringBuilder requestBody = new StringBuilder();
		requestBody.
				append("token=").append(ggtokens.getToken()). //$NON-NLS-1$
				append("&pagegen=").append(ggtokens.getPagegen()). //$NON-NLS-1$
				append("&filename=").append(fileName). //$NON-NLS-1$
				append("&delete=Delete+download"); //$NON-NLS-1$
		log.info("Request body : " + requestBody);
		
		// getting url
		URL url = new URL(urlText);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		
		connection.setRequestProperty("Cookie", cookieString.toString()); //$NON-NLS-1$
		connection.setConnectTimeout(15000);
		connection.setRequestMethod("POST"); //$NON-NLS-1$
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");  //$NON-NLS-1$//$NON-NLS-2$
		connection.setRequestProperty("Content-Length", requestBody.length()+""); //$NON-NLS-1$ //$NON-NLS-2$
		
		connection.setDoInput(true);
		connection.setDoOutput(true);
		
		connection.connect();
		
		
		OutputStream out = connection.getOutputStream();
		if (requestBody != null) {
			BufferedWriter bwr = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"), 1024); //$NON-NLS-1$
			bwr.write(requestBody.toString());
			bwr.flush();
			bwr.close();
		}
		
		log.info("Response : " + connection.getResponseMessage()); //$NON-NLS-1$
		// populate return fields.
		StringBuilder responseBody = new StringBuilder();
		InputStream i = connection.getInputStream();
		if (i != null) {
			BufferedReader in = new BufferedReader(new InputStreamReader(i, "UTF-8"), 256); //$NON-NLS-1$
			String s;
			boolean f = true;
			while ((s = in.readLine()) != null) {
				if(!f){
					responseBody.append("\n"); //$NON-NLS-1$
				} else {
					f = false;
				}
				responseBody.append(s);
			}
		}
		return responseBody.toString();
		
		
	}

}
