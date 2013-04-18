package net.osmand.plus.access;

import net.osmand.plus.ClientContext;
import net.osmand.plus.R;

public enum RelativeDirectionStyle {

    SIDEWISE(R.string.direction_style_sidewise),
    CLOCKWISE(R.string.direction_style_clockwise);

    private final int key;

    RelativeDirectionStyle(int key) {
        this.key = key;
    }

    public String toHumanString(ClientContext ctx) {
        return ctx.getString(key);
    }

}
