package net.osmand.plus.inapp;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.huawei.hms.iap.entity.InAppPurchaseData;
import com.huawei.hms.iap.entity.OwnedPurchasesResult;

import org.json.JSONException;

public class InAppUtils {
	private static final String TAG = "InAppUtils";

	@Nullable
	public static InAppPurchaseData getPurchaseData(OwnedPurchasesResult result, String productId) {
		if (result == null || result.getInAppPurchaseDataList() == null) {
			Log.i(TAG, "result is null");
			return null;
		}
		int index = result.getItemList().indexOf(productId);
		if (index != -1) {
			String data = result.getInAppPurchaseDataList().get(index);
			String signature = result.getInAppSignature().get(index);
			return getInAppPurchaseData(productId, data, signature);
		}
		return null;
	}

	@Nullable
	public static InAppPurchaseData getInAppPurchaseData(@Nullable String productId, @NonNull String data, @NonNull String signature) {
		if (CipherUtil.doCheck(data, signature, CipherUtil.getPublicKey())) {
			try {
				InAppPurchaseData purchaseData = new InAppPurchaseData(data);
				if (purchaseData.getPurchaseState() == InAppPurchaseData.PurchaseState.PURCHASED) {
					if (productId == null || productId.equals(purchaseData.getProductId())) {
						return purchaseData;
					}
				}
			} catch (JSONException e) {
				Log.e(TAG, "delivery: " + e.getMessage());
			}
		} else {
			Log.e(TAG, "delivery: verify signature error");
		}
		return null;
	}
}