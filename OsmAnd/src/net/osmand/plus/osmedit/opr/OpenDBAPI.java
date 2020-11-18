package net.osmand.plus.osmedit.opr;

import android.net.TrafficStats;
import android.os.Build;
import com.google.gson.GsonBuilder;
import net.osmand.AndroidNetworkUtils;
import net.osmand.PlatformUtil;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.BuildConfig;
import org.apache.commons.logging.Log;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.openplacereviews.opendb.SecUtils;
import org.openplacereviews.opendb.ops.OpOperation;
import org.openplacereviews.opendb.util.JsonFormatter;
import org.openplacereviews.opendb.util.exception.FailedVerificationException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyPair;
import java.security.Security;
import java.util.*;

import static org.openplacereviews.opendb.SecUtils.*;


public class OpenDBAPI {
	private static final Log log = PlatformUtil.getLog(SecUtils.class);
	private static final String checkLoginEndpoint = "api/auth/user-check-loginkey?";
	private static final String LOGIN_SUCCESS_MESSAGE = "success";
	private static final int THREAD_ID = 11200;

	/*
	 * method for check if user is loggined in blockchain
	 * params
	 *  - username: blockchain username in format "openplacereviews:test_1"
	 *  - privatekey: "base64:PKCS#8:actualKey"
	 * Need to encode key
	 * Do not call on mainThread
	 */
	public boolean checkPrivateKeyValid(String username, String privateKey) throws UnsupportedEncodingException {
		String url = null;
		try {
			url = BuildConfig.OPR_BASE_URL + checkLoginEndpoint +
					"name=" +
					username +
					"&" +
					"privateKey=" +
					//need to encode the key
					URLEncoder.encode(privateKey, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw e;
		}
		StringBuilder response = new StringBuilder();
		return (NetworkUtils.sendGetRequest(url,null,response) == null) &&
				response.toString().contains(LOGIN_SUCCESS_MESSAGE);
	}

	public int uploadImage(String[] placeId, String privateKey, String username, String image) throws FailedVerificationException {
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			Security.removeProvider("BC");
			Security.addProvider(new BouncyCastleProvider());
		}
		KeyPair kp = SecUtils.getKeyPair(ALGO_EC, privateKey, null);
		String signed = username;// + ":opr-web";

		JsonFormatter formatter = new JsonFormatter();
		OPRImage OPRImage = new GsonBuilder().create().fromJson(image, OPRImage.class);
		OpOperation opOperation = new OpOperation();
		opOperation.setType("opr.place");
		List<Object> edits = new ArrayList<>();
		Map<String, Object> edit = new TreeMap<>();
		List<Object> imageResponseList = new ArrayList<>();
		Map<String, Object> imageMap = new TreeMap<>();
		imageMap.put("cid", OPRImage.cid);
		imageMap.put("hash", OPRImage.hash);
		imageMap.put("extension", OPRImage.extension);
		imageMap.put("type", OPRImage.type);
		imageResponseList.add(imageMap);
		List<String> ids = new ArrayList<>(Arrays.asList(placeId));
		Map<String, Object> change = new TreeMap<>();
		Map<String, Object> images = new TreeMap<>();
		Map<String, Object> outdoor = new TreeMap<>();
		outdoor.put("outdoor", imageResponseList);
		images.put("append", outdoor);
		change.put("version", "increment");
		change.put("images", images);
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
		String url = BuildConfig.OPR_BASE_URL + "api/auth/process-operation?addToQueue=true&dontSignByServer=false";
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
				}
			}
			return rc;
		} catch (IOException e) {
			log.error(e);
		}
		return -1;
	}
}