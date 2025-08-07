package net.osmand.plus.card.color.palette.gradient;

import static android.graphics.drawable.GradientDrawable.LINEAR_GRADIENT;
import static android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT;
import static android.graphics.drawable.GradientDrawable.RECTANGLE;
import static net.osmand.gpx.GpxParameter.MAX_ELEVATION;
import static net.osmand.gpx.GpxParameter.MIN_ELEVATION;
import static net.osmand.plus.card.color.palette.gradient.GradientColorsPaletteCard.MAX_ALTITUDE_ADDITION;

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

import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.plugins.srtm.TerrainMode.TerrainType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.FormattedValue;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.shared.ColorPalette.ColorValue;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.shared.routing.RouteColorize.ColorizationType;

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
			colors[i] = Color.argb(value.getA(), value.getR(), value.getG(), value.getB());
		}
		GradientDrawable drawable = new GradientDrawable(LEFT_RIGHT, colors);
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

	@NonNull
	public static IAxisValueFormatter getGradientTypeFormatter(@NonNull OsmandApplication app, @NonNull Object gradientType, @Nullable GpxTrackAnalysis analysis) {
		if (gradientType instanceof TerrainType) {
			return getTerrainTypeFormatter(app, (TerrainType) gradientType);
		}
		return getColorizationTypeFormatter(app, (ColorizationType) gradientType, analysis);
	}

	@NonNull
	private static IAxisValueFormatter getTerrainTypeFormatter(@NonNull OsmandApplication app, @NonNull TerrainType terrainType) {
		return (value, axis) -> {
			boolean shouldShowUnit = axis.mEntries.length >= 1 && axis.mEntries[0] == value;
			String stringValue = GradientUiHelper.formatTerrainTypeValues(value);
			String typeValue = "";
			switch (terrainType) {
				case SLOPE:
					typeValue = "°";
					break;
				case HEIGHT:
					FormattedValue formattedValue = OsmAndFormatter.getFormattedAltitudeValue(value, app, app.getSettings().ALTITUDE_METRIC.get());
					stringValue = formattedValue.value;
					typeValue = formattedValue.unit;
					break;
			}
			return shouldShowUnit ? app.getString(R.string.ltr_or_rtl_combine_via_space, stringValue, typeValue) : stringValue;
		};
	}

	private static IAxisValueFormatter getColorizationTypeFormatter(@NonNull OsmandApplication app, @NonNull ColorizationType colorizationType, @Nullable GpxTrackAnalysis analysis) {
		return (value, axis) -> {
			boolean shouldShowUnit = axis.mEntries.length >= 1 && axis.mEntries[0] == value;
			String stringValue = formatValue(value, 100);
			String type = "%";
			FormattedValue formattedValue;
			switch (colorizationType) {
				case SPEED:
					if (analysis != null && analysis.getMaxSpeed() != 0) {
						type = app.getSettings().SPEED_SYSTEM.getModeValue(app.getSettings().getApplicationMode()).toShortString();
						stringValue = formatValue(value, analysis.getMaxSpeed());
					}
					break;
				case ELEVATION:
					if (analysis != null) {
						float calculatedValue;
						float minElevation = (float) analysis.getMinElevation();
						float maxElevation = (float) analysis.getMaxElevation() + MAX_ALTITUDE_ADDITION;
						if (minElevation != (double) MIN_ELEVATION.getDefaultValue() && maxElevation != (double) MAX_ELEVATION.getDefaultValue()) {
							if (value == 0) {
								calculatedValue = minElevation;
							} else {
								calculatedValue = minElevation + (value * ((maxElevation - minElevation)));
							}
						} else {
							break;
						}
						formattedValue = OsmAndFormatter.getFormattedAltitudeValue(calculatedValue, app, app.getSettings().ALTITUDE_METRIC.get());
						stringValue = formattedValue.value;
						type = formattedValue.unit;
					}
					break;
			}
			return shouldShowUnit ? app.getString(R.string.ltr_or_rtl_combine_via_space, stringValue, type) : stringValue;
		};
	}

	@NonNull
	private static String formatValue(float value, float multiplier) {
		DecimalFormat decimalFormat = new DecimalFormat("#");
		return decimalFormat.format(value * multiplier);
	}
}
