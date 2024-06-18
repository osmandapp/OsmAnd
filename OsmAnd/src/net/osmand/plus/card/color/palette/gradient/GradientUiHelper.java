package net.osmand.plus.card.color.palette.gradient;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import net.osmand.ColorPalette.ColorValue;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.util.List;

public class GradientUiHelper {

    private final OsmandApplication app;
    private final LayoutInflater themedInflater;
    private final boolean nightMode;

    public GradientUiHelper(@NonNull Context context, boolean nightMode) {
        this.nightMode = nightMode;
        app = (OsmandApplication) context.getApplicationContext();
        themedInflater = UiUtilities.getInflater(context, nightMode);
    }

    public void updateColorItemView(@NonNull View view, @NonNull PaletteColor paletteColor, boolean showOutline) {
        ImageView icon = view.findViewById(R.id.icon);
        AppCompatImageView background = view.findViewById(R.id.background);
        AppCompatImageView outline = view.findViewById(R.id.outline);

        if (paletteColor instanceof PaletteGradientColor) {
            PaletteGradientColor gradientColor = (PaletteGradientColor) paletteColor;
            List<ColorValue> colorsList = gradientColor.getColorPalette().getColors();
            background.setImageDrawable(getGradientDrawable(app, colorsList, GradientDrawable.RECTANGLE));
        }

        if (showOutline) {
            Drawable border = getPaintedIcon(R.drawable.bg_point_square_contour, ColorUtilities.getActiveIconColor(app, nightMode));
            outline.setImageDrawable(border);
            outline.setVisibility(View.VISIBLE);
        } else {
            outline.setVisibility(View.INVISIBLE);
            icon.setImageDrawable(UiUtilities.tintDrawable(
                    icon.getDrawable(), ColorUtilities.getDefaultIconColor(app, nightMode)));
        }
    }

    public static GradientDrawable getGradientDrawable(@NonNull OsmandApplication app, @NonNull List<ColorValue> colorsList, int drawableShape) {
        int[] colors = new int[colorsList.size()];
        for (int i = 0; i < colorsList.size(); i++) {
            ColorValue value = colorsList.get(i);
            colors[i] = Color.argb(value.a, value.r, value.g, value.b);
        }
        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
        gradientDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        gradientDrawable.setShape(drawableShape);
        if (drawableShape == GradientDrawable.RECTANGLE) {
            gradientDrawable.setCornerRadius(AndroidUtils.dpToPx(app, 2));
        }
        return gradientDrawable;
    }

    @NonNull
    public View createRectangleView(@NonNull ViewGroup rootView) {
        return themedInflater.inflate(R.layout.point_editor_button, rootView, false);
    }

    protected Drawable getPaintedIcon(@DrawableRes int id, @ColorInt int color) {
        return app.getUIUtilities().getPaintedIcon(id, color);
    }
}
