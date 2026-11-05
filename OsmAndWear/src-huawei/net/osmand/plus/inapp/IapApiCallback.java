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

/**
 * Used to callback the result from iap api.
 *
 * @since 2019/12/9
 */
public interface IapApiCallback<T> {

    /**
     * The request is successful.
     * @param result The result of a successful response.
     */
    void onSuccess(T result);

    /**
     * Callback fail.
     * @param e An Exception from IAPSDK.
     */
    void onFail(Exception e);
}
