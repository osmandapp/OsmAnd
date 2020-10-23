package net.osmand.plus.osmedit.utils;


import android.net.TrafficStats;
import android.os.Build;
import android.util.Base64;
import com.google.gson.GsonBuilder;
import net.osmand.PlatformUtil;
import net.osmand.plus.osmedit.utils.ops.OpOperation;
import net.osmand.plus.osmedit.utils.util.JsonFormatter;
import net.osmand.plus.osmedit.utils.util.exception.FailedVerificationException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class SecUtils {
	private static final String SIG_ALGO_SHA1_EC = "SHA1withECDSA";
	private static final String SIG_ALGO_NONE_EC = "NonewithECDSA";

	public static final String SIG_ALGO_ECDSA = "ECDSA";
	public static final String ALGO_EC = "EC";

	public static final String DECODE_BASE64 = "base64";
	public static final String HASH_SHA256 = "sha256";
	public static final String HASH_SHA1 = "sha1";

	public static final String JSON_MSG_TYPE = "json";
	public static final String KEY_BASE64 = DECODE_BASE64;

	private static final Log log = PlatformUtil.getLog(SecUtils.class);

	private static final int THREAD_ID = 11200;


	public static int uploadImage(String[] placeId, String privateKey, String username, String image) throws FailedVerificationException {
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
		String signature = signMessageWithKeyBase64(kp, hashBytes, SIG_ALGO_SHA1_EC, null);
		opOperation.addOrSetStringValue("hash", hash);
		opOperation.addOrSetStringValue("signature", signature);
		TrafficStats.setThreadStatsTag(THREAD_ID);
		String url = "http://test.openplacereviews.org/api/auth/process-operation?addToQueue=true&dontSignByServer=false";
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

	public static EncodedKeySpec decodeKey(String key) {
		if (key.startsWith(KEY_BASE64 + ":")) {
			key = key.substring(KEY_BASE64.length() + 1);
			int s = key.indexOf(':');
			if (s == -1) {
				throw new IllegalArgumentException(String.format("Key doesn't contain algorithm of hashing to verify"));
			}
			return getKeySpecByFormat(key.substring(0, s),
					android.util.Base64.decode(key.substring(s + 1), Base64.DEFAULT));
		}
		throw new IllegalArgumentException(String.format("Key doesn't contain algorithm of hashing to verify"));
	}

	public static String encodeKey(String algo, PublicKey pk) {
		if (algo.equals(KEY_BASE64)) {
			return SecUtils.KEY_BASE64 + ":" + pk.getFormat() + ":" + encodeBase64(pk.getEncoded());
		}
		throw new UnsupportedOperationException("Algorithm is not supported: " + algo);
	}

	public static String encodeKey(String algo, PrivateKey pk) {
		if (algo.equals(KEY_BASE64)) {
			return SecUtils.KEY_BASE64 + ":" + pk.getFormat() + ":" + encodeBase64(pk.getEncoded());
		}
		throw new UnsupportedOperationException("Algorithm is not supported: " + algo);
	}

	public static EncodedKeySpec getKeySpecByFormat(String format, byte[] data) {
		switch (format) {
			case "PKCS#8":
				return new PKCS8EncodedKeySpec(data);
			case "X.509":
				return new X509EncodedKeySpec(data);
		}
		throw new IllegalArgumentException(format);
	}

	public static String encodeBase64(byte[] data) {
		return new String(android.util.Base64.decode(data, android.util.Base64.DEFAULT));
	}

	public static boolean validateKeyPair(String algo, PrivateKey privateKey, PublicKey publicKey)
			throws FailedVerificationException {
		if (!algo.equals(ALGO_EC)) {
			throw new FailedVerificationException("Algorithm is not supported: " + algo);
		}
		// create a challenge
		byte[] challenge = new byte[512];
		ThreadLocalRandom.current().nextBytes(challenge);

		try {
			// sign using the private key
			Signature sig = Signature.getInstance(SIG_ALGO_SHA1_EC);
			sig.initSign(privateKey);
			sig.update(challenge);
			byte[] signature = sig.sign();

			// verify signature using the public key
			sig.initVerify(publicKey);
			sig.update(challenge);

			boolean keyPairMatches = sig.verify(signature);
			return keyPairMatches;
		} catch (InvalidKeyException e) {
			throw new FailedVerificationException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new FailedVerificationException(e);
		} catch (SignatureException e) {
			throw new FailedVerificationException(e);
		}
	}

	public static KeyPair getKeyPair(String algo, String prKey, String pbKey) throws FailedVerificationException {
		try {
			KeyFactory keyFactory = KeyFactory.getInstance(algo);
			PublicKey pb = null;
			PrivateKey pr = null;
			if (pbKey != null) {
				pb = keyFactory.generatePublic(decodeKey(pbKey));
			}
			if (prKey != null) {
				pr = keyFactory.generatePrivate(decodeKey(prKey));
			}
			return new KeyPair(pb, pr);
		} catch (NoSuchAlgorithmException e) {
			throw new FailedVerificationException(e);
		} catch (InvalidKeySpecException e) {
			throw new FailedVerificationException(e);
		}
	}

	public static String signMessageWithKeyBase64(KeyPair keyPair, byte[] msg, String signAlgo, ByteArrayOutputStream out) {
		byte[] sigBytes;
		try {
			sigBytes = signMessageWithKey(keyPair, msg, signAlgo);
		} catch (FailedVerificationException e) {
			throw new IllegalStateException("Cannot get bytes");
		}
		if (out != null) {
			try {
				out.write(sigBytes);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
		String signature = Base64.encodeToString(sigBytes, Base64.DEFAULT).replace("\n", "");
		return signAlgo + ":" + DECODE_BASE64 + ":" + signature;
	}

	public static byte[] signMessageWithKey(KeyPair keyPair, byte[] msg, String signAlgo) throws FailedVerificationException {
		try {
			Signature sig = Signature.getInstance(getInternalSigAlgo(signAlgo), "BC");
			sig.initSign(keyPair.getPrivate());
			sig.update(msg);
			byte[] signatureBytes = sig.sign();
			return signatureBytes;
		} catch (NoSuchAlgorithmException e) {
			throw new FailedVerificationException(e);
		} catch (InvalidKeyException e) {
			throw new FailedVerificationException(e);
		} catch (SignatureException e) {
			throw new FailedVerificationException(e);
		} catch (NoSuchProviderException e) {
			throw new RuntimeException(e);
		}
	}

	private static String getInternalSigAlgo(String sigAlgo) {
		return sigAlgo.equals(SIG_ALGO_ECDSA) ? SIG_ALGO_NONE_EC : sigAlgo;
	}

	public static byte[] calculateHash(String algo, byte[] b1, byte[] b2) {
		byte[] m = mergeTwoArrays(b1, b2);
		if (algo.equals(HASH_SHA256)) {
			return DigestUtils.sha256(m);
		} else if (algo.equals(HASH_SHA1)) {
			return DigestUtils.sha1(m);
		}
		throw new UnsupportedOperationException();
	}

	public static byte[] mergeTwoArrays(byte[] b1, byte[] b2) {
		byte[] m = b1 == null ? b2 : b1;
		if (b2 != null && b1 != null) {
			m = new byte[b1.length + b2.length];
			System.arraycopy(b1, 0, m, 0, b1.length);
			System.arraycopy(b2, 0, m, b1.length, b2.length);
		}
		return m;
	}

	public static String calculateHashWithAlgo(String algo, String salt, String msg) {
		try {
			char[] hex = Hex.encodeHex(calculateHash(algo, salt == null ? null : salt.getBytes("UTF-8"),
					msg == null ? null : msg.getBytes("UTF-8")));

			return algo + ":" + new String(hex);
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	public static byte[] getHashBytes(String msg) {
		if (msg == null || msg.length() == 0) {
			// special case for empty hash
			return new byte[0];
		}
		int i = msg.lastIndexOf(':');
		String s = i >= 0 ? msg.substring(i + 1) : msg;
		try {
			return Hex.decodeHex(s.toCharArray());
		} catch (DecoderException e) {
			throw new IllegalArgumentException(e);
		}
	}

}
