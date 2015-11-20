/*
 * android-spackle-lib https://github.com/twofortyfouram/android-spackle
 * Copyright 2014 two forty four a.m. LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twofortyfouram.spackle;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.twofortyfouram.spackle.test.R;
import com.twofortyfouram.test.assertion.MoarAsserts;

/**
 * Tests {@link ResourceUtil}.
 */
public final class ResourceUtilTest extends AndroidTestCase {

    @SmallTest
    public static void testNonInstantiable() {
        MoarAsserts.assertNoninstantiable(ResourceUtil.class);
    }

    @SmallTest
    public void testGetPositionForIdInArray() {
        assertEquals(0, ResourceUtil.getPositionForIdInArray(getContext(), R.array.com_twofortyfouram_spackle_test_array,
                R.string.com_twofortyfouram_spackle_test_string_key_1));

        assertEquals(1, ResourceUtil.getPositionForIdInArray(getContext(), R.array.com_twofortyfouram_spackle_test_array,
                R.string.com_twofortyfouram_spackle_test_string_key_2));
        assertEquals(2, ResourceUtil.getPositionForIdInArray(getContext(), R.array.com_twofortyfouram_spackle_test_array,
                R.string.com_twofortyfouram_spackle_test_string_key_3));
    }

    @SmallTest
    public void testResourceIdForPositionInArray() {
        assertEquals(R.string.com_twofortyfouram_spackle_test_string_key_1,
                ResourceUtil.getResourceIdForPositionInArray(getContext(), R.array.com_twofortyfouram_spackle_test_array, 0));

        assertEquals(R.string.com_twofortyfouram_spackle_test_string_key_2,
                ResourceUtil.getResourceIdForPositionInArray(getContext(), R.array.com_twofortyfouram_spackle_test_array, 1));

        assertEquals(R.string.com_twofortyfouram_spackle_test_string_key_3,
                ResourceUtil.getResourceIdForPositionInArray(getContext(), R.array.com_twofortyfouram_spackle_test_array, 2));
    }
}
