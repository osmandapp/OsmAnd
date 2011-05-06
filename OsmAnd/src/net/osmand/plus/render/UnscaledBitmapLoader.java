package net.osmand.plus.render;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Build;
import android.util.DisplayMetrics;

public abstract class UnscaledBitmapLoader {

    public static final UnscaledBitmapLoader instance;

    static {
        instance = Integer.parseInt(Build.VERSION.SDK) < 4 ? new Old() : new New();
    }

    public static Bitmap loadFromResource(Resources resources, int resId, BitmapFactory.Options options, DisplayMetrics densityDpi) {
        return instance.load(resources, resId, options, densityDpi);
    }

    private static class Old extends UnscaledBitmapLoader {

        @Override
        Bitmap load(Resources resources, int resId, Options options, DisplayMetrics densityDpi) {
            return BitmapFactory.decodeResource(resources, resId, options);
        }

    }

    private static class New extends UnscaledBitmapLoader {

        @Override
        Bitmap load(Resources resources, int resId, Options options, DisplayMetrics dm) {
            options = new BitmapFactory.Options();
            options.inScaled = false;
            options.inTargetDensity = dm.densityDpi;
			options.inDensity = dm.densityDpi;
            return BitmapFactory.decodeResource(resources, resId, options);
        }

    }

    abstract Bitmap load(Resources resources, int resId, BitmapFactory.Options options, DisplayMetrics densityDpi);

}