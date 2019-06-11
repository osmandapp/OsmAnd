package net.osmand.turn_screen_on.helpers;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

public class AndroidUtils {
    public static int dpToPx(Context ctx, float dp) {
        Resources r = ctx.getResources();
        return (int) TypedValue.applyDimension(
                COMPLEX_UNIT_DIP,
                dp,
                r.getDisplayMetrics()
        );
    }
}
