package com.osmand.data.index;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;

import com.osmand.LogUtil;

public class DownloaderIndexFromGoogleCode {

	private final static Log log = LogUtil.getLog(DownloaderIndexFromGoogleCode.class);
	/**
	 * @param args
	 * @throws URISyntaxException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws URISyntaxException, IOException {
		Map<String, String> indexFiles = DownloaderIndexFromGoogleCode.getIndexFiles(new String[] { IndexConstants.ADDRESS_INDEX_EXT,
				IndexConstants.POI_INDEX_EXT }, new String[] {
				IndexConstants.ADDRESS_TABLE_VERSION + "", IndexConstants.POI_TABLE_VERSION + "" }); //$NON-NLS-1$//$NON-NLS-2$
		System.out.println(indexFiles);
	}
	
	private static StringBuilder getContent() {
		try {
			URL url = new URL("http://code.google.com/p/osmand/downloads/list"); //$NON-NLS-1$

			BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
			StringBuilder b = new StringBuilder();
			String s = null;
			while ((s = reader.readLine()) != null) {
				b.append(s);
			}
			return b;
		} catch (MalformedURLException e) {
			log.error("Unexpected exception", e); //$NON-NLS-1$
			return null;
		} catch (IOException e) {
			log.error("Input/Output exception", e); //$NON-NLS-1$
			return null;
		}
	}
	

	private static void getIndexFiles(Map<String, String> files , StringBuilder content, String ext, String version){
		int i = 0;
		int prevI = -1;
		String prevFile = null;
		while ((i = content.indexOf(ext, i)) != -1) {
			if(prevI > i){
				files.put(prevFile, null);
				prevI = i;
			}
			int j = i - 1;
			while (content.charAt(j) == '_' || Character.isLetterOrDigit(content.charAt(j)) || content.charAt(j) == '-') {
				j--;
			}
			if(!content.substring(j + 1, i).endsWith("_"+version)){ //$NON-NLS-1$
				i++;
				continue;
			}
			prevFile = content.substring(j + 1, i) + ext;
			String description = null;
			prevI = content.indexOf("{", i); //$NON-NLS-1$
			if(prevI > 0){
				j = content.indexOf("}", prevI); //$NON-NLS-1$
				if(j > 0 && j - prevI < 40){
					description = content.substring(prevI + 1, j);
				}
			}
			if(!files.containsKey(prevFile) || files.get(prevFile) == null){
				files.put(prevFile, description);
			} else {
				prevI = i;
			}
			i++;
		}
	}
	
	public static Map<String, String> getIndexFiles(String[] ext, String[] version){
		StringBuilder content = getContent();
		if(content == null){
			return null;
		}
		Map<String, String> files = new TreeMap<String, String>();
		for(int i=0; i<ext.length; i++){
			getIndexFiles(files, content, ext[i], version[i]);
		}
		return files;
	}
	
	public static Map<String, String> getIndexFiles(String ext, String version){
		StringBuilder content = getContent();
		if(content == null){
			return null;
		}
		Map<String, String> files = new TreeMap<String, String>();
		getIndexFiles(files, content, ext, version);
		return files;
	}
	
	public static URL getInputStreamToLoadIndex(String indexName) throws IOException{
		URL url = new URL("http://osmand.googlecode.com/files/"+indexName); //$NON-NLS-1$
		return url;
	}

}
