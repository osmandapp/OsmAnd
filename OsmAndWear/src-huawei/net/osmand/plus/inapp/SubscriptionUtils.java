/**
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.osmand.plus.inapp;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.huawei.hms.iap.Iap;
import com.huawei.hms.iap.entity.InAppPurchaseData;
import com.huawei.hms.iap.entity.OrderStatusCode;
import com.huawei.hms.iap.entity.OwnedPurchasesResult;
import com.huawei.hms.iap.entity.PurchaseResultInfo;

import org.json.JSONException;

import java.util.List;

/**
 * Util for Subscription function.
 *
 * @since 2019/12/9
 */
public class SubscriptionUtils {
	private static final String TAG = "SubscriptionUtils";

	/**
	 * Decide whether to offer subscription service
	 *
	 * @param result    the OwnedPurchasesResult from IapClient.obtainOwnedPurchases
	 * @param productId subscription product id
	 * @return decision result
	 */
	@Nullable
	public static InAppPurchaseData getPurchaseData(OwnedPurchasesResult result, String productId) {
		if (null == result) {
			Log.e(TAG, "OwnedPurchasesResult is null");
			return null;
		}
		List<String> dataList = result.getInAppPurchaseDataList();
        List<String> signatureList = result.getInAppSignature();
        for (int i = 0; i < dataList.size(); i++) {
            String data = dataList.get(i);
            String signature = signatureList.get(i);
            InAppPurchaseData purchaseData = getInAppPurchaseData(productId, data, signature);
            if (purchaseData != null) {
                return purchaseData;
            }
        }
		return null;
	}

	@Nullable
	public static InAppPurchaseData getInAppPurchaseData(@Nullable String productId, @NonNull String data, @NonNull String signature) {
		try {
			InAppPurchaseData purchaseData = new InAppPurchaseData(data);
			if (productId == null || productId.equals(purchaseData.getProductId())) {
				boolean credible = CipherUtil.doCheck(data, signature, CipherUtil.getPublicKey());
				if (credible) {
					return purchaseData.isSubValid() ? purchaseData : null;
				} else {
					Log.e(TAG, "check the data signature fail");
					return null;
				}
			}
		} catch (JSONException e) {
			Log.e(TAG, "parse InAppPurchaseData JSONException", e);
			return null;
		}
		return null;
	}

	/**
	 * Parse PurchaseResult data from intent
	 *
	 * @param activity Activity
	 * @param data     the intent from onActivityResult
	 * @return PurchaseResultInfo
	 */
	public static PurchaseResultInfo getPurchaseResult(Activity activity, Intent data) {
		PurchaseResultInfo purchaseResultInfo = Iap.getIapClient(activity).parsePurchaseResultInfoFromIntent(data);
		if (null == purchaseResultInfo) {
			Log.e(TAG, "PurchaseResultInfo is null");
		} else {
			int returnCode = purchaseResultInfo.getReturnCode();
			String errMsg = purchaseResultInfo.getErrMsg();
			switch (returnCode) {
				case OrderStatusCode.ORDER_PRODUCT_OWNED:
					Log.w(TAG, "you have owned this product");
					break;
				case OrderStatusCode.ORDER_STATE_SUCCESS:
					boolean credible = CipherUtil.doCheck(purchaseResultInfo.getInAppPurchaseData(), purchaseResultInfo.getInAppDataSignature(), CipherUtil
							.getPublicKey());
					if (credible) {
						try {
							InAppPurchaseData inAppPurchaseData = new InAppPurchaseData(purchaseResultInfo.getInAppPurchaseData());
							if (!inAppPurchaseData.isSubValid()) {
								return getFailedPurchaseResultInfo();
							}
						} catch (JSONException e) {
							Log.e(TAG, "parse InAppPurchaseData JSONException", e);
							return getFailedPurchaseResultInfo();
						}
					} else {
						Log.e(TAG, "check the data signature fail");
						return getFailedPurchaseResultInfo();
					}
				default:
					Log.e(TAG, "returnCode: " + returnCode + " , errMsg: " + errMsg);
					break;
			}
		}
		return purchaseResultInfo;
	}

	private static PurchaseResultInfo getFailedPurchaseResultInfo() {
		PurchaseResultInfo info = new PurchaseResultInfo();
		info.setReturnCode(OrderStatusCode.ORDER_STATE_FAILED);
		return info;
	}
}
