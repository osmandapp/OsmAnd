package net.osmand.plus.osmedit.opr;

import android.net.TrafficStats;
import android.os.Build;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import net.osmand.PlatformUtil;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.openplacereviews.opendb.SecUtils;
import org.openplacereviews.opendb.ops.OpOperation;
import org.openplacereviews.opendb.util.JsonFormatter;
import org.openplacereviews.opendb.util.exception.FailedVerificationException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyPair;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.openplacereviews.opendb.SecUtils.ALGO_EC;
import static org.openplacereviews.opendb.SecUtils.JSON_MSG_TYPE;
import static org.openplacereviews.opendb.SecUtils.signMessageWithKeyBase64;


public class OpenDBAPI {
	public static final String PURPOSE = "osmand-android";
	private static final Log log = PlatformUtil.getLog(SecUtils.class);
	private static final String checkLoginEndpoint = "api/auth/user-check-loginkey?";
	private static final String LOGIN_SUCCESS_MESSAGE = "\"result\":\"OK\"";
	private static final int THREAD_ID = 11200;

	/*
	 * method for check if user is loggined in blockchain
	 * params
	 *  - username: blockchain username in format "openplacereviews"
	 *  - privatekey: "base64:PKCS#8:actualKey"
	 * Need to encode key
	 * Do not call on mainThread
	 */
	public boolean checkPrivateKeyValid(OsmandApplication app, String baseUrl, String username, String privateKey) {
		String url = null;
		try {
			String purposeParam = "purpose=" + PURPOSE;
			url = baseUrl + checkLoginEndpoint + purposeParam + "&" +
					"name=" +
					username +
					"&" +
					"privateKey=" +
					//need to encode the key
					URLEncoder.encode(privateKey, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return false;
		}

		StringBuilder response = new StringBuilder();
		String error = NetworkUtils.sendGetRequest(url, null, response);
		if (error == null) {
			String responseStr = response.toString();
			try {
				Map<String, String> map = new Gson().fromJson(
						responseStr, new TypeToken<HashMap<String, String>>() {
						}.getType()
				);
				if (!Algorithms.isEmpty(map) && map.containsKey("blockchain-name")) {
					String blockchainName = map.get("blockchain-name");
					app.getSettings().OPR_BLOCKCHAIN_NAME.set(blockchainName);
				} else {
					return false;
				}
			} catch (JsonSyntaxException e) {
				return false;
			}
			return responseStr.contains(LOGIN_SUCCESS_MESSAGE);
		} else {
			return false;
		}
	}

	public int uploadImage(String[] placeId, String baseUrl, String privateKey, String username, String image, StringBuilder sb) throws FailedVerificationException {
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			Security.removeProvider("BC");
			Security.addProvider(new BouncyCastleProvider());
		}
		KeyPair kp = SecUtils.getKeyPair(ALGO_EC, privateKey, null);
		String signed = username + ":" + PURPOSE;

		JsonFormatter formatter = new JsonFormatter();
		OPRImage oprImage = new GsonBuilder().create().fromJson(image, OPRImage.class);
		OpOperation opOperation = new OpOperation();
		opOperation.setType("opr.place");
		List<Object> edits = new ArrayList<>();
		Map<String, Object> edit = new TreeMap<>();
		List<Object> imageResponseList = new ArrayList<>();
		Map<String, Object> imageMap = new TreeMap<>();
		imageMap.put("cid", oprImage.cid);
		imageMap.put("hash", oprImage.hash);
		imageMap.put("extension", oprImage.extension);
		imageMap.put("type", oprImage.type);
		imageResponseList.add(imageMap);
		List<String> ids = new ArrayList<>(Arrays.asList(placeId));
		Map<String, Object> change = new TreeMap<>();
		Map<String, Object> images = new TreeMap<>();
		images.put("append", imageMap);
		change.put("version", "increment");
		change.put("images.review", images);
		edit.put("id", ids);
		edit.put("change", change);
		edit.put("current", new Object());
		edits.add(edit);
		opOperation.putObjectValue(OpOperation.F_EDIT, edits);
		opOperation.setSignedBy(signed);
		String hash = JSON_MSG_TYPE + ":"
				+ SecUtils.calculateHashWithAlgo(SecUtils.HASH_SHA256, null,
				formatter.opToJsonNoHash(opOperation));
		byte[] hashBytes = SecUtils.getHashBytes(hash);
		String signature = signMessageWithKeyBase64(kp, hashBytes, SecUtils.SIG_ALGO_SHA1_EC, null);
		opOperation.addOrSetStringValue("hash", hash);
		opOperation.addOrSetStringValue("signature", signature);
		TrafficStats.setThreadStatsTag(THREAD_ID);
		String url = baseUrl + "api/auth/process-operation?addToQueue=true&dontSignByServer=false";
		String json = formatter.opToJson(opOperation);
		System.out.println("JSON: " + json);
		HttpURLConnection connection;
		try {
			connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setConnectTimeout(10000);
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			try {
				DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
				wr.write(json.getBytes());
			} catch (Exception e) {
				e.printStackTrace();
			}
			int rc = connection.getResponseCode();
			if (rc != 200) {
				log.error("ERROR HAPPENED");
				BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
				String strCurrentLine;
				while ((strCurrentLine = br.readLine()) != null) {
					log.error(strCurrentLine);
					sb.append(strCurrentLine);
				}
			}
			return rc;
		} catch (IOException e) {
			log.error(e);
		}
		return -1;
	}

	public class OPRImage {
		public String type;
		public String hash;
		public String cid;
		public String extension;
	}
}
