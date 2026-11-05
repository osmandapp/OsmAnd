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

import android.app.Activity;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.huawei.hms.iap.IapApiException;
import com.huawei.hms.iap.entity.OrderStatusCode;

import net.osmand.PlatformUtil;
import net.osmand.plus.utils.AndroidUtils;

/**
 *  Handles the exception returned from the iap api.
 *
 * @since 2019/12/9
 */
public class ExceptionHandle {

    protected static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(ExceptionHandle.class);

    /**
     * The exception is solved.
     */
    public static final int SOLVED = 0;

    /**
     * Handles the exception returned from the iap api.
     * @param activity The Activity to call the iap api.
     * @param e The exception returned from the iap api.
     * @return int
     */
    public static int handle(@Nullable Activity activity, Exception e) {

        if (e instanceof IapApiException) {
            IapApiException iapApiException = (IapApiException) e;
            LOG.info("returnCode: " + iapApiException.getStatusCode());
            switch (iapApiException.getStatusCode()) {
                case OrderStatusCode.ORDER_STATE_CANCEL:
                    showToast(activity, "Order has been canceled!");
                    return SOLVED;
                case OrderStatusCode.ORDER_STATE_PARAM_ERROR:
                    showToast(activity, "Order state param error!");
                    return SOLVED;
                case OrderStatusCode.ORDER_STATE_NET_ERROR:
                    showToast(activity, "Order state net error!");
                    return SOLVED;
                case OrderStatusCode.ORDER_VR_UNINSTALL_ERROR:
                    showToast(activity, "Order vr uninstall error!");
                    return SOLVED;
                case OrderStatusCode.ORDER_HWID_NOT_LOGIN:
                    IapRequestHelper.startResolutionForResult(activity, iapApiException.getStatus(), Constants.REQ_CODE_LOGIN);
                    return SOLVED;
                case OrderStatusCode.ORDER_PRODUCT_OWNED:
                    showToast(activity, "Product already owned error!");
                    return OrderStatusCode.ORDER_PRODUCT_OWNED;
                case OrderStatusCode.ORDER_PRODUCT_NOT_OWNED:
                    showToast(activity, "Product not owned error!");
                    return SOLVED;
                case OrderStatusCode.ORDER_PRODUCT_CONSUMED:
                    showToast(activity, "Product consumed error!");
                    return SOLVED;
                case OrderStatusCode.ORDER_ACCOUNT_AREA_NOT_SUPPORTED:
                    showToast(activity, "Order account area not supported error!");
                    return SOLVED;
                case OrderStatusCode.ORDER_NOT_ACCEPT_AGREEMENT:
                    showToast(activity, "User does not agree the agreement");
                    return SOLVED;
                default:
                    // handle other error scenarios
                    showToast(activity, "Order unknown error (" + iapApiException.getStatusCode() + ")");
                    return SOLVED;
            }
        } else {
            showToast(activity, "External error");
            LOG.error(e.getMessage(), e);
            return SOLVED;
        }
    }

    private static void showToast(@Nullable Activity activity, String s) {
        if (AndroidUtils.isActivityNotDestroyed(activity)) {
            Toast.makeText(activity, s, Toast.LENGTH_SHORT).show();
        }
    }
}