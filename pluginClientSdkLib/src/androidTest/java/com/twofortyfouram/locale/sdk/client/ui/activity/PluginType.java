package com.twofortyfouram.locale.sdk.client.ui.activity;

import android.support.annotation.NonNull;

import net.jcip.annotations.ThreadSafe;

import static com.twofortyfouram.assertion.Assertions.assertNotEmpty;

/**
 * Enumerates the types of plug-ins for Locale.
 */
@ThreadSafe
public enum PluginType {

    /**
     * A plug-in condition.
     *
     * @see com.twofortyfouram.locale.api.Intent#ACTION_EDIT_CONDITION
     * @see com.twofortyfouram.locale.api.Intent#ACTION_QUERY_CONDITION
     */
    @NonNull
    CONDITION(com.twofortyfouram.locale.api.Intent.ACTION_EDIT_CONDITION,
            com.twofortyfouram.locale.api.Intent.ACTION_QUERY_CONDITION),

    /**
     * A plug-in setting.
     *
     * @see com.twofortyfouram.locale.api.Intent#ACTION_EDIT_SETTING
     * @see com.twofortyfouram.locale.api.Intent#ACTION_FIRE_SETTING
     */
    @NonNull
    SETTING(com.twofortyfouram.locale.api.Intent.ACTION_EDIT_SETTING,
            com.twofortyfouram.locale.api.Intent.ACTION_FIRE_SETTING);

    @NonNull
    private final String mActivityIntentAction;

    @NonNull
    private final String mReceiverIntentAction;

    private PluginType(@NonNull final String activityIntentAction,
                       @NonNull final String receiverIntentAction) {
        assertNotEmpty(activityIntentAction, "activityIntentAction"); //$NON-NLS-1$
        assertNotEmpty(receiverIntentAction, "receiverIntentAction"); //$NON-NLS-1$

        mActivityIntentAction = activityIntentAction;
        mReceiverIntentAction = receiverIntentAction;
    }

    /**
     * @return The Activity Intent action for the plug-in type.
     */
    @NonNull
    public String getActivityIntentAction() {
        return mActivityIntentAction;
    }

    /**
     * @return The BroadcastReceiver Intent action for the plug-in type.
     */
    @NonNull
    public String getReceiverIntentAction() {
        return mReceiverIntentAction;
    }
}
