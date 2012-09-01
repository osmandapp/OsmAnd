package net.osmand.plus.activities;

import net.osmand.plus.R;
import android.content.Context;

public enum FollowMode {
	/*
	 * FOLLOW_COMPASS("Compass"), FOLLOW_BEARING("Bearing"), FREE_SCROLL("Free Scroll");
	 */

    FOLLOW_COMPASS(R.string.follow_mode_compass),
    FOLLOW_BEARING(R.string.follow_mode_bearing), 
    FREE_SCROLL(R.string.follow_mode_free_scroll);

    private final int key;

    FollowMode(int key) {
        this.key = key;
    }

    public String toHumanString(Context ctx) {
        return ctx.getResources().getString(key);
    }
}