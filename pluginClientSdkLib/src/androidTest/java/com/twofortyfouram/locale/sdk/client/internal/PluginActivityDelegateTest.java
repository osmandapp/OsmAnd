package com.twofortyfouram.locale.sdk.client.internal;


import android.content.Intent;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.twofortyfouram.locale.sdk.client.test.condition.ui.activity.PluginActivityFixture;
import com.twofortyfouram.locale.sdk.client.ui.activity.PluginType;

public final class PluginActivityDelegateTest extends AndroidTestCase {


    @SmallTest
    public static void testIsLocaleIntent_condition() {
        assertTrue(PluginActivityDelegate
                .isLocalePluginIntent(PluginActivityFixture.getDefaultStartIntent(PluginType.CONDITION)));
    }

    @SmallTest
    public static void testIsLocaleIntent_setting() {
        assertTrue(PluginActivityDelegate
                .isLocalePluginIntent(PluginActivityFixture.getDefaultStartIntent(PluginType.SETTING)));
    }

    @SmallTest
    public static void testIsLocaleIntent_neither() {
        assertFalse(PluginActivityDelegate.isLocalePluginIntent(new Intent()));
    }
}
