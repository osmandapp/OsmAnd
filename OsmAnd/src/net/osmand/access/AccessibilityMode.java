package net.osmand.access;

import net.osmand.plus.R;
import android.content.Context;

public enum AccessibilityMode {

    ON(R.string.accessibility_on),
    OFF(R.string.accessibility_off),
    DEFAULT(R.string.accessibility_default);

    private final int key;

    AccessibilityMode(int key) {
        this.key = key;
    }

    public String toHumanString(Context ctx) {
        return ctx.getResources().getString(key);
    }

}
