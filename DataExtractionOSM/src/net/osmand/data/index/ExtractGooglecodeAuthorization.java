package net.osmand.data.index;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtractGooglecodeAuthorization {

	public static class GooglecodeUploadTokens {
		private final String sid, hsid, token, pagegen;

		public GooglecodeUploadTokens(String sid, String hsid, String token,
				String pagegen) {
			this.sid = sid;
			this.hsid = hsid;
			this.token = token;
			this.pagegen = pagegen;
		}

		public String getHsid() {
			return hsid;
		}

		public String getSid() {
			return sid;
		}

		public String getToken() {
			return token;
		}

		public String getPagegen() {
			return pagegen;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Token:").append(token)//
					.append("\nPagegen:").append(pagegen)//
					.append("\nSID:").append(sid)//
					.append("\nHSID:").append(hsid);
			return builder.toString();
		}
	}

	boolean debug = false;
	
	public ExtractGooglecodeAuthorization()	{
		this(false);
	}

	public ExtractGooglecodeAuthorization(boolean debug)	{
		this.debug = debug;
	}

	public GooglecodeUploadTokens getGooglecodeTokensForUpload(String user, String password) throws IOException {
		final MyCookieStore cookies = new MyCookieStore(
				new java.net.CookieManager().getCookieStore());
		CookieHandler.setDefault(new java.net.CookieManager(cookies, null));
		//first try to delete file
		HttpURLConnection conn = connectTo("http://code.google.com/p/osmand/downloads/delete?name=en-tts_0.voice.zip");
		StringBuilder responseBody = readAnswer(conn.getInputStream());
		
		//then we are redirect to login page
		conn = connectTo("https://www.google.com/accounts/ServiceLogin?service=code&ltmpl=phosting&continue=http%3A%2F%2Fcode.google.com%2Fp%2Fosmand%2Fdownloads%2Fdelete%3Fname%3Den-tts_0.voice.zip");
		responseBody = readAnswer(conn.getInputStream());

		String dsh = matchResponseBody(".*id=\"dsh\"[^\"]*value=\"([^\"]*)\".*", responseBody);
		String galx = matchResponseBody(".*name=\"GALX\"[^\"]*value=\"([^\"]*)\".*", responseBody);
		
		if (dsh == null || galx == null) {
			throw new IOException("Failed to retrieve dsh or galx from login page!");
		}

		StringBuilder data = buildPostData(user, password, dsh, galx);

		// try authentification Request: https://www.google.com/accounts/ServiceLoginAuth");
		URL url = new URL("https://www.google.com/accounts/ServiceLoginAuth");
		conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		conn.setRequestProperty("Accept-Charset", "ISO-8859-2,utf-8;q=0.7,*;q=0.7");
		conn.setRequestProperty("Accept-Language", "sk,cs;q=0.8,en-us;q=0.5,en;q=0.3");
		conn.setRequestProperty("Connection", "keep-alive");
		conn.setRequestProperty(
				"Referer",
				"https://www.google.com/accounts/ServiceLogin?service=code&ltmpl=phosting&continue=http%3A%2F%2Fcode.google.com%2Fp%2Fosmand%2Fdownloads%2Fdelete%3Fname%3Den-tts_0.voice.zip");
		conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:5.0) Gecko/20100101 Firefox/5.0");
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); //$NON-NLS-1$ //$NON-NLS-
		conn.setRequestProperty("Content-Length", data.length() + "");
		OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());

		// write parameters
		writer.write(data.toString());
		writer.flush();
		conn.connect();
		if (debug) {
			System.out.println("Connected to:" + url.toString());
			System.out.println("-- Data sent:\n" + data.toString());
			System.our.ptintln("--");
		}

		// Get the response
		responseBody = readAnswer(conn.getInputStream());
		writer.close();
		
		//try again the delete page, we should be now connected
		conn = connectTo("http://code.google.com/p/osmand/downloads/delete?name=en-tts_0.voice.zip");
		responseBody = readAnswer(conn.getInputStream());

		String token = matchResponseBody(".*name=\"token\"[^\"]*value=\"([^\"]*)\".*", responseBody);
		String pagegen = matchResponseBody(".*name=\"pagegen\"[^\"]*value=\"([^\"]*)\".*", responseBody);

		if (token == null || pagegen == null) {
			throw new IOException("Faild to retrieve token or pagegen from delete page!");
		}
		return new GooglecodeUploadTokens(cookies.getCookie("SID"), cookies.getCookie("HSID"), token, pagegen);
	}

	private String matchResponseBody(String matchString, StringBuilder responseBody) {
		String dsh = null;
		Matcher matcher = Pattern.compile(matchString, Pattern.DOTALL | Pattern.MULTILINE).matcher(responseBody.toString());
		if (matcher.matches()) {
			dsh = matcher.group(1);
		}
		return dsh;
	}

	private StringBuilder buildPostData(String user, String password,
			String dsh, String galx) throws UnsupportedEncodingException {
		// Build parameter string
		Map<String, String> values = new LinkedHashMap<String, String>();
		values.put("asts", null);
		values.put("continue","https://code.google.com/p/osmand/downloads/delete?name=en-tts_0.voice.zip");
		// values.put("dnConn","https://accounts.youtube.com");
		values.put("dsh", dsh);
		values.put("Email", user);
		values.put("GALX", galx);
		values.put("ltmpl", "phosting");
		values.put("Passwd", password);
		values.put("pstMsg", "1");
		values.put("rmShow", "1");
		values.put("secTok", null);
		values.put("service", "code");
		values.put("signIn", "Sign in");
		values.put("timeStmp", null);

		StringBuilder data = new StringBuilder();
		for (String key : values.keySet()) {
			data.append(URLEncoder.encode(key, "UTF-8")).append("=");
			if (values.get(key) != null) {
				data.append(URLEncoder.encode(values.get(key), "UTF-8"));
			}
			data.append("&");
		}
		data.deleteCharAt(data.length() - 1);
		return data;
	}

	private HttpURLConnection connectTo(String urlString) throws MalformedURLException,
			IOException {
		URL url = new URL(urlString);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		conn.setRequestProperty("Accept-Charset", "ISO-8859-2,utf-8;q=0.7,*;q=0.7");
		conn.setRequestProperty("Accept-Language", "sk,cs;q=0.8,en-us;q=0.5,en;q=0.3");
		conn.setRequestProperty("Connection", "keep-alive");
		conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:5.0) Gecko/20100101 Firefox/5.0");
		conn.connect();
		if (debug) {
			System.out.println("Connecting to:" + urlString);
		}
		return conn;
	}

	private static StringBuilder readAnswer(InputStream input)
			throws IOException, UnsupportedEncodingException {
		StringBuilder responseBody = new StringBuilder();
		InputStream i = input;
		if (i != null) {
			BufferedReader in = new BufferedReader(new InputStreamReader(i,
					"UTF-8"), 256); //$NON-NLS-1$
			String s;
			boolean f = true;
			while ((s = in.readLine()) != null) {
				if (!f) {
					responseBody.append("\n"); //$NON-NLS-1$
				} else {
					f = false;
				}
				responseBody.append(s);
			}
			i.close();
		}
		if (debug) {
			System.out.println("Aswer from server:\n" + responseBody.toString());
		}
		return responseBody;
	}
	
	public static void main(String[] args) throws IOException
	{
		if (args.length < 2) {
			System.out.println("Use: ExtractGooglecodeAuthorization gmailname gmailpassword");
			return;
		} else {
			System.out.println(new ExtractGooglecodeAuthorization(true).getGooglecodeTokensForUpload(args[0], args[1]).toString());
		}
	}


}
