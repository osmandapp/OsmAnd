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
    private static final String PUBLIC_KEY = "MIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEAsB+oH8rYQncwpTqGa0kS/5E725HJrq2sW1ThAZtmorYVi52Yt9PmZvNDz7284ol9C2skrKQR34eIer8Tr7Qqq3mlNo+/LVUpq9sa++kB2glaG6jj5NNjM3w4nVYHFIYkd5AQhodJgmqFvnp2s7r7YmyQVXZSehei5bA1G70Bs+El9cSv9shNNGTCaU3ARUu2hy3Ltkc/ov7/ZYYpiwjbyD3cmoMh9jO1++zztXb2phjv1h9zeJOp1i6HsotZll+c9J4jjV3GhrF+ZJm5WrSzGLDLtwSldRpMZFxrSvAJJstjzhDz3LpUM+nPV3HZ5VQ/xosmwWYmiibo89E1gw8p73NTBXHzuQMJcTJ6vTjD8LeMskpXHZUAGhifmFLGN1LbNP9662ulCV12kIbXuzWCwwi/h0DWqmnjKmLvzc88e4BrGrp2zZUnHz7m15voPG+4cQ3z9+cwS4gEI3SUTiFyQGE539SO/11VkkQAJ8P7du1JFNqQw5ZEW3AoE1iUsp5XAgMBAAE=";

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
            signature.update(content.getBytes("UTF-8"));

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
