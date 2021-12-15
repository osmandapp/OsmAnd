package net.osmand.plus.plugins.openplacereviews;

import static org.openplacereviews.opendb.SecUtils.ALGO_EC;
import static org.openplacereviews.opendb.SecUtils.JSON_MSG_TYPE;
import static org.openplacereviews.opendb.SecUtils.signMessageWithKeyBase64;

import android.net.TrafficStats;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

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


public class OpenDBAPI {
	public static final String PURPOSE = "osmand-android";
	private static final Log log = PlatformUtil.getLog(SecUtils.class);
	private static final String checkLoginEndpoint = "api/auth/user-check-loginkey?";
	private static final String LOGIN_SUCCESS_MESSAGE = "\"result\":\"OK\"";
	private static final int THREAD_ID = 11200;

	public static class UploadImageResult {
		public int responseCode = -1;
		public String error;
	}

	private static class OPRImage {
		String type;
		String hash;
		String cid;
		String extension;
	}

	/**
	 * Method for check if user is logged in into blockchain
	 *
	 * @param app        app context
	 * @param baseUrl    base URL
	 * @param username   blockchain username in OPR format
	 * @param privateKey "base64:PKCS#8:actualKey"
	 * @return false if not logged in or check failed
	 */
	@WorkerThread
	public boolean checkPrivateKeyValid(@NonNull OsmandApplication app, @NonNull String baseUrl,
	                                    @NonNull String username, @NonNull String privateKey) {
		String url;
		try {
			url = baseUrl + checkLoginEndpoint + "purpose=" + PURPOSE + "&name=" + username +
					"&privateKey=" + URLEncoder.encode(privateKey, "UTF-8");
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

	public UploadImageResult uploadImage(@NonNull String[] placeId, @NonNull String baseUrl, @NonNull String privateKey,
	                                     @NonNull String username, @NonNull String image) throws FailedVerificationException {
		UploadImageResult res = new UploadImageResult();
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

		Map<String, Object> imageMap = new TreeMap<>();
		imageMap.put("cid", oprImage.cid);
		imageMap.put("hash", oprImage.hash);
		imageMap.put("extension", oprImage.extension);
		imageMap.put("type", oprImage.type);

		List<String> ids = new ArrayList<>(Arrays.asList(placeId));
		Map<String, Object> change = new TreeMap<>();
		Map<String, Object> images = new TreeMap<>();
		images.put("append", imageMap);
		change.put("version", "increment");
		change.put("images.review", images);

		Map<String, Object> edit = new TreeMap<>();
		edit.put("id", ids);
		edit.put("change", change);
		edit.put("current", new Object());

		List<Object> edits = new ArrayList<>();
		edits.add(edit);
		opOperation.putObjectValue(OpOperation.F_EDIT, edits);
		opOperation.setSignedBy(signed);
		String hash = JSON_MSG_TYPE + ":"
				+ SecUtils.calculateHashWithAlgo(SecUtils.HASH_SHA256, null, formatter.opToJsonNoHash(opOperation));
		byte[] hashBytes = SecUtils.getHashBytes(hash);
		String signature = signMessageWithKeyBase64(kp, hashBytes, SecUtils.SIG_ALGO_SHA1_EC, null);
		opOperation.addOrSetStringValue("hash", hash);
		opOperation.addOrSetStringValue("signature", signature);
		TrafficStats.setThreadStatsTag(THREAD_ID);
		String url = baseUrl + "api/auth/process-operation?addToQueue=true&dontSignByServer=false";
		String json = formatter.opToJson(opOperation);
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
				log.error(e.getMessage(), e);
			}
			int rc = connection.getResponseCode();
			res.responseCode = rc;
			if (rc != 200) {
				BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
				StringBuilder sb = new StringBuilder();
				String strCurrentLine;
				while ((strCurrentLine = br.readLine()) != null) {
					log.error(strCurrentLine);
					sb.append(strCurrentLine);
				}
				res.error = sb.toString();
			}
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
		if (!Algorithms.isEmpty(res.error)) {
			log.debug("OpenDBAPI uploadImage error: " + res.responseCode + " = " + res.error);
		}
		return res;
	}
}
