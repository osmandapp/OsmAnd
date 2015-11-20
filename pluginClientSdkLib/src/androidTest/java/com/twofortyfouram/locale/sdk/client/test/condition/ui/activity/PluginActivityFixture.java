package com.twofortyfouram.locale.sdk.client.test.condition.ui.activity;


import android.content.Intent;
import android.support.annotation.NonNull;

import com.twofortyfouram.assertion.Assertions;
import com.twofortyfouram.locale.sdk.client.ui.activity.PluginType;

import net.jcip.annotations.Immutable;

@Immutable
public final class PluginActivityFixture {

    /**
     * @param type Plug-in type.
     * @return The default Intent to start the plug-in Activity. The Intent will
     * contain
     * {@link com.twofortyfouram.locale.api.Intent#EXTRA_STRING_BREADCRUMB}
     * .
     */
    @NonNull
    public static Intent getDefaultStartIntent(@NonNull final PluginType type) {
        Assertions.assertNotNull(type, "type"); //$NON-NLS-1$
        final Intent i = new Intent(type.getActivityIntentAction());

        i.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BREADCRUMB,
                "Edit Situation"); //$NON-NLS-1$

        return i;
    }

    /**
     * Private constructor prevents instantiation.
     *
     * @throws UnsupportedOperationException because this class cannot be
     *                                       instantiated.
     */
    private PluginActivityFixture() {
        throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
    }
}
