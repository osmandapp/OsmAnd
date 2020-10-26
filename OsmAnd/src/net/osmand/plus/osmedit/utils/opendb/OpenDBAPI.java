package net.osmand.plus.osmedit.utils.opendb;

import android.net.TrafficStats;
import android.os.Build;
import com.google.gson.GsonBuilder;
import net.osmand.PlatformUtil;
import net.osmand.plus.BuildConfig;
import net.osmand.plus.osmedit.utils.IPFSImage;
import net.osmand.plus.osmedit.utils.opendb.ops.OpOperation;
import net.osmand.plus.osmedit.utils.opendb.util.JsonFormatter;
import net.osmand.plus.osmedit.utils.opendb.util.exception.FailedVerificationException;
import org.apache.commons.logging.Log;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyPair;
import java.security.Security;
import java.util.*;

import static net.osmand.plus.osmedit.utils.opendb.SecUtils.*;

public class OpenDBAPI {
	private static final Log log = PlatformUtil.getLog(SecUtils.class);

	private static final int THREAD_ID = 11200;

	public int uploadImage(String[] placeId, String privateKey, String username, String image) throws FailedVerificationException {
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			Security.removeProvider("BC");
			Security.addProvider(new BouncyCastleProvider());
		}
		KeyPair kp = SecUtils.getKeyPair(ALGO_EC, privateKey, null);
		String signed = username + ":opr-web";

		JsonFormatter formatter = new JsonFormatter();
		IPFSImage ipfsImage = new GsonBuilder().create().fromJson(image, IPFSImage.class);
		OpOperation opOperation = new OpOperation();
		opOperation.setType("opr.place");
		List<Object> edits = new ArrayList<>();
		Map<String, Object> edit = new TreeMap<>();
		List<Object> imageResponseList = new ArrayList<>();
		Map<String, Object> imageMap = new TreeMap<>();
		imageMap.put("cid", ipfsImage.cid);
		imageMap.put("hash", ipfsImage.hash);
		imageMap.put("extension", ipfsImage.extension);
		imageMap.put("type", ipfsImage.type);
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
			try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
				wr.write(json.getBytes());
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
