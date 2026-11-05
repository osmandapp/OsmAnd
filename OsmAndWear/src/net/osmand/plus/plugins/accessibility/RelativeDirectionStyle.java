package net.osmand.plus.plugins.accessibility;

import net.osmand.plus.R;
import android.content.Context;

public enum RelativeDirectionStyle {

    SIDEWISE(R.string.direction_style_sidewise),
    CLOCKWISE(R.string.direction_style_clockwise);

    private final int key;

    RelativeDirectionStyle(int key) {
        this.key = key;
    }

    public String toHumanString(Context ctx) {
        return ctx.getString(key);
    }

}
