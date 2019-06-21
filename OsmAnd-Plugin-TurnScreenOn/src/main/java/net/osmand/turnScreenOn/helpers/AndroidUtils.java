package net.osmand.turnScreenOn.helpers;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
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

    public static Paint createPaintWithGreyScale(){
        Paint pGreyScale = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        pGreyScale.setColorFilter(new ColorMatrixColorFilter(cm));
        return pGreyScale;
    }
}
