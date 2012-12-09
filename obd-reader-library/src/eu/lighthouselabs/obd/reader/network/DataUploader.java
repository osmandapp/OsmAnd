package eu.lighthouselabs.obd.reader.network;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class DataUploader {

	public String uploadRecord(String urlStr, Map<String,String> data) throws IOException, URISyntaxException {
		String encData = getEncodedData(data);
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 5000);
        HttpConnectionParams.setSoTimeout(params, 30000);
        HttpClient client = new DefaultHttpClient(params);
        HttpPost request = new HttpPost();
        request.setURI(new URI(urlStr));
        request.setHeader("Content-Type", "application/x-www-form-urlencoded");
        request.setEntity(new StringEntity(encData));
        ResponseHandler<String> resHandle = new BasicResponseHandler();
        String response = client.execute(request,resHandle);
        return response;
	}
	public String getEncodedData(Map<String,String> data) throws UnsupportedEncodingException {
		StringBuffer buff = new StringBuffer();
		Iterator<String> keys = data.keySet().iterator();
		while (keys.hasNext()) {
			String k = keys.next();
			buff.append(URLEncoder.encode(k,"UTF-8"));
			buff.append("=");
			buff.append(URLEncoder.encode(data.get(k),"UTF-8"));
			buff.append("&");
		}
		return buff.toString();
	}
}
