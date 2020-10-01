/**
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package net.osmand.plus.inapp;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

/**
 * Signature related tools.
 *
 * @since 2019/12/9
 */
public class CipherUtil {
    private static final String TAG = "CipherUtil";
    private static final String SIGN_ALGORITHMS = "SHA256WithRSA";
    private static final String PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAooen3X9jSWarxugznzzMSvp4zir1Pg6uPOm7fqlLOL0Ix52e5FpeotMx871pQ9hrCkiyFg2e6UxD8IXXjvK6QJQbjNJ2jIfKkCusm90yloSEfvyLeiq5y7zg4+DoPglHi8RxZ9y308YIqnRDoslfGm5DnWa8RKUvFRVRiu1p3FN4SYIa/FWLtS5yygemtqMJi8I14V7xqQ5wExCGeSA6j1/AAWXEwZncJwKn0BTXQSvwVBPBRM5ksgt4q+Sc484ZIbntATyxsUipnEBFxq1OXn5Zw5/vVxUC8RSyDMQ/kC2RaEcFtA1tlIIjIdurbpNg3tyViPfQUQndvOs4nDrFzwIDAQAB";

    /**
     * the method to check the signature for the data returned from the interface
     * @param content Unsigned data
     * @param sign the signature for content
     * @param publicKey the public of the application
     * @return boolean
     */
    public static boolean doCheck(String content, String sign, String publicKey) {
        if (TextUtils.isEmpty(publicKey)) {
            Log.e(TAG, "publicKey is null");
            return false;
        }

        if (TextUtils.isEmpty(content) || TextUtils.isEmpty(sign)) {
            Log.e(TAG, "data is error");
            return false;
        }

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] encodedKey = Base64.decode(publicKey, Base64.DEFAULT);
            PublicKey pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));

            java.security.Signature signature = java.security.Signature.getInstance(SIGN_ALGORITHMS);

            signature.initVerify(pubKey);
            signature.update(content.getBytes("utf-8"));

            boolean bverify = signature.verify(Base64.decode(sign, Base64.DEFAULT));
            return bverify;

        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "doCheck NoSuchAlgorithmException" + e);
        } catch (InvalidKeySpecException e) {
            Log.e(TAG, "doCheck InvalidKeySpecException" + e);
        } catch (InvalidKeyException e) {
            Log.e(TAG, "doCheck InvalidKeyException" + e);
        } catch (SignatureException e) {
            Log.e(TAG, "doCheck SignatureException" + e);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "doCheck UnsupportedEncodingException" + e);
        }
        return false;
    }

    /**
     * get the publicKey of the application
     * During the encoding process, avoid storing the public key in clear text.
     * @return publickey
     */
    public static String getPublicKey(){
        return PUBLIC_KEY;
    }

}
