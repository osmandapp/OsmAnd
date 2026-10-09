//Revision d1a1f6e81d0716a47cbddf5754ee77fa5fc6d1d8
package org.openplacereviews.opendb;


import android.util.Base64;
import org.openplacereviews.opendb.util.exception.FailedVerificationException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.concurrent.ThreadLocalRandom;

//This class is a copy of SecUtils class from OpenDB project with changes for android platform
public class SecUtils {
	public static final String SIG_ALGO_SHA1_EC = "SHA1withECDSA";
	public static final String SIG_ALGO_NONE_EC = "NonewithECDSA";

	public static final String SIG_ALGO_ECDSA = "ECDSA";
	public static final String ALGO_EC = "EC";

	public static final String DECODE_BASE64 = "base64";
	public static final String HASH_SHA256 = "sha256";
	public static final String HASH_SHA1 = "sha1";

	public static final String JSON_MSG_TYPE = "json";
	public static final String KEY_BASE64 = DECODE_BASE64;


	public static EncodedKeySpec decodeKey(String key) {
		if (key.startsWith(KEY_BASE64 + ":")) {
			key = key.substring(KEY_BASE64.length() + 1);
			int s = key.indexOf(':');
			if (s == -1) {
				throw new IllegalArgumentException(String.format("Key doesn't contain algorithm of hashing to verify"));
			}
			//should use android.util.Base64 for android platform instead of Base64.getDecoder()
			return getKeySpecByFormat(key.substring(0, s),
					android.util.Base64.decode(key.substring(s + 1), Base64.DEFAULT));
		}
		throw new IllegalArgumentException(String.format("Key doesn't contain algorithm of hashing to verify"));
	}

	public static String encodeKey(String algo, PublicKey pk) {
		if (algo.equals(KEY_BASE64)) {
			return KEY_BASE64 + ":" + pk.getFormat() + ":" + encodeBase64(pk.getEncoded());
		}
		throw new UnsupportedOperationException("Algorithm is not supported: " + algo);
	}

	public static String encodeKey(String algo, PrivateKey pk) {
		if (algo.equals(KEY_BASE64)) {
			return KEY_BASE64 + ":" + pk.getFormat() + ":" + encodeBase64(pk.getEncoded());
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
		//should use android.util.Base64 for android platform instead of Base64.getDecoder()
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
			//use BouncyCastle on android platform in order to achieve consistency between platforms
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
			//use Hex.encodeHex for android platform instead of Hex.encodeHexString
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
