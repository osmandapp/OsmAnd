package net.osmand.plus.card.color.palette.gradient;

import static android.graphics.drawable.GradientDrawable.LINEAR_GRADIENT;
import static android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM;
import static android.graphics.drawable.GradientDrawable.RECTANGLE;

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
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import net.osmand.ColorPalette.ColorValue;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.text.DecimalFormat;
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
			List<ColorValue> colors = gradientColor.getColorPalette().getColors();
			background.setImageDrawable(getGradientDrawable(app, colors, RECTANGLE));
		}

		if (showOutline) {
			Drawable border = getPaintedIcon(R.drawable.bg_point_square_contour, ColorUtilities.getActiveIconColor(app, nightMode));
			outline.setImageDrawable(border);
			outline.setVisibility(View.VISIBLE);
		} else {
			outline.setVisibility(View.INVISIBLE);
			icon.setImageDrawable(UiUtilities.tintDrawable(icon.getDrawable(), ColorUtilities.getDefaultIconColor(app, nightMode)));
		}
	}

	@NonNull
	public static GradientDrawable getGradientDrawable(@NonNull OsmandApplication app, @NonNull List<ColorValue> values, int shape) {
		int[] colors = new int[values.size()];
		for (int i = 0; i < values.size(); i++) {
			ColorValue value = values.get(i);
			colors[i] = Color.argb(value.a, value.r, value.g, value.b);
		}
		GradientDrawable drawable = new GradientDrawable(TOP_BOTTOM, colors);
		drawable.setGradientType(LINEAR_GRADIENT);
		drawable.setShape(shape);
		if (shape == RECTANGLE) {
			drawable.setCornerRadius(AndroidUtils.dpToPx(app, 2));
		}
		return drawable;
	}

	@NonNull
	public View createRectangleView(@NonNull ViewGroup view) {
		return themedInflater.inflate(R.layout.point_editor_button, view, false);
	}

	@Nullable
	protected Drawable getPaintedIcon(@DrawableRes int id, @ColorInt int color) {
		return app.getUIUtilities().getPaintedIcon(id, color);
	}

	@NonNull
	public static String formatTerrainTypeValues(float value) {
		DecimalFormat format = new DecimalFormat(value >= 10 ? "#" : "#.#");

		String formattedValue = format.format(value);
		if (formattedValue.endsWith(".0")) {
			formattedValue = formattedValue.substring(0, formattedValue.length() - 2);
		}
		return formattedValue;
	}
}
