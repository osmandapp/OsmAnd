package net.osmand.plus.charts;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.mikephil.charting.data.Entry;

import net.osmand.plus.OsmandApplication;
import net.osmand.shared.ColorPalette;
import net.osmand.shared.ColorPalette.ColorValue;
import net.osmand.shared.gpx.GradientScaleType;
import net.osmand.shared.palette.domain.PaletteItem;
import net.osmand.shared.palette.domain.category.GradientPaletteCategory;

import java.util.Arrays;
import java.util.List;

/**
 * Maps a position on the chart's X axis to a colour, using the values of another data set.
 * <p>
 * This is what lets one data set draw the line while a second one drives its colours: the line
 * geometry comes from the first data set, the colours from this source. It is deliberately
 * type-agnostic — it only knows (x, value) pairs plus a palette — so any pair of data sets can be
 * combined, not just altitude and slope.
 * <p>
 * Values are kept in the palette's own domain, not in the chart's display units. The chart shows
 * slope in percent while the slope palette is defined in fractions, and altitude may be shown in
 * feet while the altitude palette is in meters, so {@link #createValueConverter} undoes the display
 * scaling that the data set builders applied.
 */
public class ChartColorSource {

	private final float[] xs;
	private final float[] values;
	private final ColorPalette palette;
	private final OrderedLineDataSet dataSet;

	private ChartColorSource(@NonNull float[] xs, @NonNull float[] values,
	                         @NonNull ColorPalette palette, @NonNull OrderedLineDataSet dataSet) {
		this.xs = xs;
		this.values = values;
		this.palette = palette;
		this.dataSet = dataSet;
	}

	@Nullable
	public static ChartColorSource create(@NonNull OsmandApplication app,
	                                      @NonNull OrderedLineDataSet dataSet) {
		List<Entry> entries = dataSet.getEntries();
		if (entries == null || entries.size() < 2) {
			return null;
		}
		GPXDataSetType type = dataSet.getDataSetType();
		ValueConverter converter = createValueConverter(app, type);

		int size = entries.size();
		float[] xs = new float[size];
		float[] values = new float[size];
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		for (int i = 0; i < size; i++) {
			Entry entry = entries.get(i);
			float value = converter.toPaletteValue(entry.getY());
			xs[i] = entry.getX();
			values[i] = value;
			if (!Float.isNaN(value)) {
				min = Math.min(min, value);
				max = Math.max(max, value);
			}
		}
		if (min > max) {
			return null;
		}
		ColorPalette palette = resolvePalette(app, type, min, max);
		return palette != null ? new ChartColorSource(xs, values, palette, dataSet) : null;
	}

	public int getColorAt(float x) {
		return palette.getColorByValue(getValueAt(x));
	}

	public float getValueAt(float x) {
		if (x <= xs[0]) {
			return values[0];
		}
		int last = xs.length - 1;
		if (x >= xs[last]) {
			return values[last];
		}
		int index = Arrays.binarySearch(xs, x);
		if (index >= 0) {
			return values[index];
		}
		int insertion = -index - 1;
		int lo = insertion - 1;
		int hi = insertion;
		float range = xs[hi] - xs[lo];
		if (range <= 0) {
			return values[lo];
		}
		float ratio = (x - xs[lo]) / range;
		return values[lo] + ratio * (values[hi] - values[lo]);
	}

	@NonNull
	public ColorPalette getPalette() {
		return palette;
	}

	@NonNull
	public GPXDataSetType getDataSetType() {
		return dataSet.getDataSetType();
	}

	@NonNull
	public OrderedLineDataSet getDataSet() {
		return dataSet;
	}

	public int getBandColorAt(float x) {
		float value = getValueAt(x);
		List<ColorValue> breakpoints = palette.getColors();
		if (breakpoints.isEmpty()) {
			return palette.getColorByValue(value);
		}
		ColorValue band = breakpoints.get(0);
		for (ColorValue breakpoint : breakpoints) {
			if (value < breakpoint.getValue()) {
				break;
			}
			band = breakpoint;
		}
		return band.getClr();
	}

	public float getMinX() {
		return xs[0];
	}

	public float getMaxX() {
		return xs[xs.length - 1];
	}

	@Nullable
	private static ColorPalette resolvePalette(@NonNull OsmandApplication app,
	                                           @NonNull GPXDataSetType type,
	                                           double min, double max) {
		GradientScaleType scaleType = toGradientScaleType(type);
		if (scaleType != null) {
			GradientPaletteCategory category = scaleType.toPaletteCategory();
			for (PaletteItem item : app.getPaletteRepository().getPaletteItems(category.getId())) {
				if (item instanceof PaletteItem.Gradient gradient) {
					ColorPalette palette = gradient.getColorPalette();
					if (!palette.isValid()) {
						continue;
					}
					return gradient.isFixed()
							? palette
							: new ColorPalette(palette, min, max, isBipolar(type));
				}
			}
		}
		ColorPalette relative = isBipolar(type)
				? ColorPalette.Companion.getBIPOLAR_MIN_MAX_PALETTE()
				: ColorPalette.Companion.getMIN_MAX_PALETTE();
		return new ColorPalette(relative, min, max, isBipolar(type));
	}

	@Nullable
	private static GradientScaleType toGradientScaleType(@NonNull GPXDataSetType type) {
		return switch (type) {
			case ALTITUDE, ALTITUDE_EXTRM -> GradientScaleType.ALTITUDE;
			case SPEED -> GradientScaleType.SPEED;
			case SLOPE -> GradientScaleType.SLOPE;
			default -> null;
		};
	}

	private static boolean isBipolar(@NonNull GPXDataSetType type) {
		return type == GPXDataSetType.SLOPE;
	}

	@NonNull
	private static ValueConverter createValueConverter(@NonNull OsmandApplication app,
	                                                   @NonNull GPXDataSetType type) {
		switch (type) {
			case ALTITUDE:
			case ALTITUDE_EXTRM: {
				// createGPXElevationDataSet() multiplies meters by this to get feet
				boolean useFeet = app.getSettings().METRIC_SYSTEM.get().shouldUseFeet();
				float convEle = useFeet ? 3.28084f : 1.0f;
				return value -> value / convEle;
			}
			case SLOPE:
				// the chart shows slope in percent, the slope palette is defined in fractions
				return value -> value / 100f;
			default: {
				Pair<Float, Float> scaling = ChartUtils.getScalingY(app, type);
				if (scaling == null) {
					return value -> value;
				}
				float mul = scaling.first;
				float div = scaling.second;
				if (!Float.isNaN(div)) {
					// pace-like values are inverted, not scaled
					return value -> value != 0 ? div / value : Float.NaN;
				}
				if (!Float.isNaN(mul) && mul != 0) {
					return value -> value / mul;
				}
				return value -> value;
			}
		}
	}

	private interface ValueConverter {
		float toPaletteValue(float chartValue);
	}
}
