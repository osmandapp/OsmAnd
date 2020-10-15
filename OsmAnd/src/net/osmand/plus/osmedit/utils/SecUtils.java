package net.osmand.plus.osmedit.utils;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
//import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

import net.osmand.plus.osmedit.utils.ops.OpOperation;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
//import org.bouncycastle.crypto.generators.SCrypt;
//import org.bouncycastle.crypto.prng.FixedSecureRandom;
public class SecUtils {
	private static final String SIG_ALGO_SHA1_EC = "SHA1withECDSA";
	private static final String SIG_ALGO_NONE_EC = "NonewithECDSA";
	
	public static final String SIG_ALGO_ECDSA = "ECDSA";
	public static final String ALGO_EC = "EC";
	public static final String EC_256SPEC_K1 = "secp256k1";

	public static final String KEYGEN_PWD_METHOD_1 = "EC256K1_S17R8";
	public static final String DECODE_BASE64 = "base64";
	public static final String HASH_SHA256 = "sha256";
	public static final String HASH_SHA1 = "sha1";

	public static final String JSON_MSG_TYPE = "json";
	public static final String KEY_BASE64 = DECODE_BASE64;

	public static void main(String[] args) {
		//1) create op, 2) sign op 3) send to server process op
		//
		KeyPairGenerator keyGen = null ;
		SecureRandom random = null;
		try {
			keyGen = KeyPairGenerator.getInstance(ALGO_EC);
			random = SecureRandom.getInstance("SHA1PRNG");
			keyGen.initialize(1024, random);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		KeyPair kp = null;
		try {
			kp = SecUtils.getKeyPair(ALGO_EC,
					"base64:PKCS#8:MD4CAQAwEAYHKoZIzj0CAQYFK4EEAAoEJzAlAgEBBCDR+/ByIjTHZgfdnMfP9Ab5s14mMzFX+8DYqUiGmf/3rw=="
					, "base64:X.509:MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEOMUiRZwU7wW8L3A1qaJPwhAZy250VaSxJmKCiWdn9EMeubXQgWNT8XUWLV5Nvg7O3sD+1AAQLG5kHY8nOc/AyA==");
		} catch (FailedVerificationException e) {
			e.printStackTrace();
		}
//		KeyPair kp = generateECKeyPairFromPassword(KEYGEN_PWD_METHOD_1, "openplacereviews", "");
//		KeyPair kp = generateRandomEC256K1KeyPair();
		System.out.println(kp.getPrivate().getFormat());
		System.out.println(kp.getPrivate().getAlgorithm());
		try {
			System.out.println(SecUtils.validateKeyPair(ALGO_EC, kp.getPrivate(), kp.getPublic()));
		} catch (FailedVerificationException e) {
			e.printStackTrace();
		}
		String pr = encodeKey(KEY_BASE64, kp.getPrivate());
		String pk = encodeKey(KEY_BASE64, kp.getPublic());
		String algo = kp.getPrivate().getAlgorithm();
		System.out.println(String.format("Private key: %s %s\nPublic key: %s %s", kp.getPrivate().getFormat(), pr, kp
				.getPublic().getFormat(), pk));
		String signMessageTest = "Hello this is a registration message test";
		byte[] signature = signMessageWithKey(kp, signMessageTest.getBytes(), SIG_ALGO_SHA1_EC);
		System.out.println(String.format("Signed message: %s %s", android.util.Base64.decode(signature, android.util.Base64.DEFAULT),
				signMessageTest));

		KeyPair nk = null;
		try {
			nk = getKeyPair(algo, pr, pk);
		} catch (FailedVerificationException e) {
			e.printStackTrace();
		}
		// validate
		pr = new String(android.util.Base64.decode(nk.getPrivate().getEncoded(), android.util.Base64.DEFAULT));
		pk = new String(android.util.Base64.decode(nk.getPublic().getEncoded(), android.util.Base64.DEFAULT));

		System.out.println(String.format("Private key: %s %s\nPublic key: %s %s", nk.getPrivate().getFormat(), pr, nk
				.getPublic().getFormat(), pk));
		System.out.println(validateSignature(nk, signMessageTest.getBytes(), SIG_ALGO_SHA1_EC, signature));

		JsonFormatter formatter = new JsonFormatter();
		String msg = "{\n" +
				"		\"type\" : \"sys.signup\",\n" +
				"		\"signed_by\": \"openplacereviews\",\n" +
				"		\"create\": [{\n" +
				"			\"id\": [\"openplacereviews\"],\n" +
				"			\"name\" : \"openplacereviews\",\n" +
				"			\"algo\": \"EC\",\n" +
				"			\"auth_method\": \"provided\",\n" +
				"			\"pubkey\": \"base64:X.509:MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEn6GkOTN3SYc+OyCYCpqPzKPALvUgfUVNDJ+6eyBlCHI1/gKcVqzHLwaO90ksb29RYBiF4fW/PqHcECNzwJB+QA==\"\n" +
				"		}]\n" +
				"	}";

		OpOperation opOperation = formatter.parseOperation(msg);
		String hash = JSON_MSG_TYPE + ":"
				+ SecUtils.calculateHashWithAlgo(SecUtils.HASH_SHA256, null,
				formatter.opToJsonNoHash(opOperation));

		byte[] hashBytes = SecUtils.getHashBytes(hash);
		String signatureTxt = SecUtils.signMessageWithKeyBase64(kp, hashBytes, SecUtils.SIG_ALGO_ECDSA, null);
		System.out.println(formatter.opToJsonNoHash(opOperation));
		System.out.println(hash);
		System.out.println(signatureTxt);
	}

	public static EncodedKeySpec decodeKey(String key) {
		if (key.startsWith(KEY_BASE64 + ":")) {
			key = key.substring(KEY_BASE64.length() + 1);
			int s = key.indexOf(':');
			if (s == -1) {
				throw new IllegalArgumentException(String.format("Key doesn't contain algorithm of hashing to verify"));
			}
			return getKeySpecByFormat(key.substring(0, s),
					android.util.Base64.decode(key.substring(s + 1), android.util.Base64.DEFAULT));
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

	public static KeyPair generateKeyPairFromPassword(String algo, String keygenMethod, String salt, String pwd)
			throws FailedVerificationException {
		if (algo.equals(ALGO_EC)) {
			return generateECKeyPairFromPassword(keygenMethod, salt, pwd);
		}
		throw new UnsupportedOperationException("Unsupported algo keygen method: " + algo);
	}

	public static KeyPair generateECKeyPairFromPassword(String keygenMethod, String salt, String pwd)
			throws FailedVerificationException {
		if (keygenMethod.equals(KEYGEN_PWD_METHOD_1)) {
			return generateEC256K1KeyPairFromPassword(salt, pwd);
		}
		throw new UnsupportedOperationException("Unsupported keygen method: " + keygenMethod);
	}

	// "EC:secp256k1:scrypt(salt,N:17,r:8,p:1,len:256)" algorithm - EC256K1_S17R8
	public static KeyPair generateEC256K1KeyPairFromPassword(String salt, String pwd)
			throws FailedVerificationException {
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALGO_EC);
			ECGenParameterSpec ecSpec = new ECGenParameterSpec(EC_256SPEC_K1);
			if (pwd.length() < 10) {
				throw new IllegalArgumentException("Less than 10 characters produces only 50 bit entropy");
			}
			byte[] bytes = pwd.getBytes("UTF-8");
			//byte[] scrypt = SCrypt.generate(bytes, salt.getBytes("UTF-8"), 1 << 17, 8, 1, 256);
			//kpg.initialize(ecSpec, new FixedSecureRandom(scrypt));
			return kpg.genKeyPair();
		} catch (NoSuchAlgorithmException e) {
			throw new FailedVerificationException(e);
		} catch (UnsupportedEncodingException e) {
			throw new FailedVerificationException(e);
		} /* catch (InvalidAlgorithmParameterException e) {
			throw new FailedVerificationException(e);
		}*/
	}

	public static KeyPair generateRandomEC256K1KeyPair() throws FailedVerificationException {
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALGO_EC);
			ECGenParameterSpec ecSpec = new ECGenParameterSpec(EC_256SPEC_K1);
			kpg.initialize(ecSpec);
			return kpg.genKeyPair();
		} catch (NoSuchAlgorithmException e) {
			throw new FailedVerificationException(e);
		} catch (InvalidAlgorithmParameterException e) {
			throw new FailedVerificationException(e);
		}
	}

	public static String signMessageWithKeyBase64(KeyPair keyPair, byte[] msg, String signAlgo, ByteArrayOutputStream out) {
		byte[] sigBytes = signMessageWithKey(keyPair, msg, signAlgo);
		if(out != null) {
			try {
				out.write(sigBytes);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
		String signature = new String(android.util.Base64.decode(sigBytes, android.util.Base64.DEFAULT));
		return signAlgo + ":" + DECODE_BASE64 + ":" + signature;
	}

	public static byte[] signMessageWithKey(KeyPair keyPair, byte[] msg, String signAlgo) {
		try {
			Signature sig = Signature.getInstance(getInternalSigAlgo(signAlgo));
			sig.initSign(keyPair.getPrivate());
			sig.update(msg);
			byte[] signatureBytes = sig.sign();
			return signatureBytes;
		} catch (NoSuchAlgorithmException e) {
			//throw new FailedVerificationException(e);
		} catch (InvalidKeyException e) {
			//throw new FailedVerificationException(e);
		} catch (SignatureException e) {
			//throw new FailedVerificationException(e);
		}
		return new byte[0];
	}
	
	public static boolean validateSignature(KeyPair keyPair, byte[] msg, String sig) {
		if(sig == null || keyPair == null) {
			 return false;
		}
		int ind = sig.indexOf(':');
		String sigAlgo = sig.substring(0, ind);
		return validateSignature(keyPair, msg, sigAlgo, decodeSignature(sig.substring(ind + 1)));
	}

	public static boolean validateSignature(KeyPair keyPair, byte[] msg, String sigAlgo, byte[] signature) {
		if (keyPair == null) {
			return false;
		}
		try {
			Signature sig = Signature.getInstance(getInternalSigAlgo(sigAlgo));
			sig.initVerify(keyPair.getPublic());
			sig.update(msg);
			return sig.verify(signature);
		} catch (NoSuchAlgorithmException e) {
			//throw new FailedVerificationException(e);
		} catch (InvalidKeyException e) {
			//throw new FailedVerificationException(e);
		} catch (SignatureException e) {
			//throw new FailedVerificationException(e);
		}
		return false;
	}

	private static String getInternalSigAlgo(String sigAlgo) {
		return sigAlgo.equals(SIG_ALGO_ECDSA)? SIG_ALGO_NONE_EC : sigAlgo;
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
		if(b2 != null && b1 != null) {
			m = new byte[b1.length + b2.length];
			System.arraycopy(b1, 0, m, 0, b1.length);
			System.arraycopy(b2, 0, m, b1.length, b2.length);
		}
		return m;
	}
	
	public static String calculateHashWithAlgo(String algo, String salt, String msg) {
		try {
			String hex = Hex.encodeHexString(calculateHash(algo, salt == null ? null : salt.getBytes("UTF-8"),
					msg == null ? null : msg.getBytes("UTF-8")));
			return algo + ":" + hex;
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		} 
	}
	
	public static String calculateHashWithAlgo(String algo, byte[] bts) {
		byte[] hash = calculateHash(algo, bts, null);
		return formatHashWithAlgo(algo, hash);
	}

	public static String formatHashWithAlgo(String algo, byte[] hash) {
		String hex = Hex.encodeHexString(hash);
		return algo + ":" + hex;
	}

	public static byte[] getHashBytes(String msg) {
		if(msg == null || msg.length() == 0) {
			// special case for empty hash
			return new byte[0];
		}
		int i = msg.lastIndexOf(':');
		String s = i >= 0 ? msg.substring(i + 1) : msg;
		try {
			return Hex.decodeHex(s);
		} catch (DecoderException e) {
			throw new IllegalArgumentException(e);
		}
	}
	

	public static boolean validateHash(String hash, String salt, String msg) {
		int s = hash.indexOf(":");
		if (s == -1) {
			throw new IllegalArgumentException(String.format("Hash %s doesn't contain algorithm of hashing to verify",
					s));
		}
		String v = calculateHashWithAlgo(hash.substring(0, s), salt, msg);
		return hash.equals(v);
	}

	public static byte[] decodeSignature(String digest) {
		try {
			int indexOf = digest.indexOf(DECODE_BASE64 + ":");
			if (indexOf != -1) {
//				return Base64.getDecoder().decode(digest.substring(indexOf + DECODE_BASE64.length() + 1).
//						getBytes("UTF-8"));
				return android.util.Base64.decode(digest.substring(indexOf + DECODE_BASE64.length() + 1)
						.getBytes("UTF-8"), android.util.Base64.DEFAULT);
			}
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
		throw new IllegalArgumentException("Unknown format for signature " + digest);
	}

	public static String hexify(byte[] bytes) {
		if(bytes == null || bytes.length == 0) {
			return "";
		}
		return Hex.encodeHexString(bytes);
		
	}

	

}
