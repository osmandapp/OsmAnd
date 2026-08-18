package net.osmand.plus.plugins.accessibility;

import net.osmand.plus.R;
import android.content.Context;

public enum AccessibilityMode {

    ON(R.string.shared_string_on),
    OFF(R.string.shared_string_off),
    DEFAULT(R.string.accessibility_default);

    private final int key;

    AccessibilityMode(int key) {
        this.key = key;
    }

    public String toHumanString(Context ctx) {
        return ctx.getString(key);
    }

}
