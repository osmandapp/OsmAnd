package net.osmand.plus.charts;

import static net.osmand.shared.gpx.GpxParameter.COLOR_PALETTE;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.mikephil.charting.data.Entry;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.track.helpers.GpxAppearanceHelper;
import net.osmand.shared.ColorPalette;
import net.osmand.shared.ColorPalette.ColorValue;
import net.osmand.shared.gpx.GradientScaleType;
import net.osmand.shared.palette.domain.PaletteItem;
import net.osmand.shared.palette.domain.category.GradientPaletteCategory;

import java.io.File;
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
 * Values are kept in the palette's own domain, not in the chart's display units: the chart shows
 * slope in percent while the palette is defined in fractions, and altitude may be shown in feet.
 * {@link #createValueConverter} undoes the display scaling that the data set builders applied.
 */
public class ChartColorSource {

	private static final ColorPalette BIPOLAR_PALETTE = buildPalette(new double[] {
			-0.15, -0.08, -0.03, 0.0, 0.03, 0.08, 0.15
	}, new int[] {
			0xFF2E7FD4, 0xFF3FA9E8, 0xFF5FC8C0, 0xFF8FCB55, 0xFFF7B342, 0xFFFA7A3C, 0xFFC62828
	});

	private static final ColorPalette LINEAR_PALETTE = buildPalette(new double[] {
			0.0, 0.167, 0.333, 0.5, 0.667, 0.833, 1.0
	}, new int[] {
			0xFF6FCF5B, 0xFFA6D847, 0xFFDCDD3E, 0xFFF5C63C, 0xFFF79B3E, 0xFFF26F49, 0xFFE8474F
	});

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
	                                      @NonNull OrderedLineDataSet dataSet,
	                                      @Nullable String trackPath) {
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
		return new ChartColorSource(xs, values, resolvePalette(app, type, min, max, trackPath), dataSet);
	}

	public int getColorAt(float x) {
		return getColor(getValueAt(x));
	}

	public int getColor(float value) {
		return palette.getColorByValue(value);
	}

	public float getAverageValueAt(float xFrom, float xTo) {
		if (xTo <= xFrom) {
			return getValueAt(xFrom);
		}
		int from = insertionIndex(xFrom);
		int to = insertionIndex(xTo);
		double sum = 0;
		int count = 0;
		for (int i = from; i < to; i++) {
			if (!Float.isNaN(values[i])) {
				sum += values[i];
				count++;
			}
		}
		return count > 0 ? (float) (sum / count) : getValueAt((xFrom + xTo) / 2f);
	}

	private int insertionIndex(float x) {
		int index = Arrays.binarySearch(xs, x);
		return index >= 0 ? index : -index - 1;
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
		return getBandColor(getValueAt(x));
	}

	public int getBandColor(float value) {
		List<ColorValue> breakpoints = palette.getColors();
		if (breakpoints.isEmpty()) {
			return palette.getColorByValue(value);
		}
		ColorValue nearest = breakpoints.get(0);
		double smallestDistance = Math.abs(value - nearest.getValue());
		for (ColorValue breakpoint : breakpoints) {
			double distance = Math.abs(value - breakpoint.getValue());
			if (distance < smallestDistance) {
				smallestDistance = distance;
				nearest = breakpoint;
			}
		}
		return nearest.getClr();
	}

	public float getMinX() {
		return xs[0];
	}

	public float getMaxX() {
		return xs[xs.length - 1];
	}

	@NonNull
	private static ColorPalette resolvePalette(@NonNull OsmandApplication app,
	                                           @NonNull GPXDataSetType type,
	                                           double min, double max,
	                                           @Nullable String trackPath) {
		if (OsmandDevelopmentPlugin.CHART_TRACK_PALETTES) {
			ColorPalette trackPalette = findTrackPalette(app, type, min, max, trackPath);
			if (trackPalette != null) {
				return trackPalette;
			}
		}
		return isBipolar(type)
				? BIPOLAR_PALETTE
				: new ColorPalette(LINEAR_PALETTE, min, max, false);
	}

	/**
	 * The gradient the map colours this attribute by, so a chart can match the track next to it.
	 * <p>
	 * Only altitude, speed and slope have such a palette at all; everything else keeps the built-in
	 * one. Returns null whenever the chosen palette is missing or unusable, so the caller falls back
	 * rather than drawing nothing.
	 */
	@Nullable
	private static ColorPalette findTrackPalette(@NonNull OsmandApplication app,
	                                             @NonNull GPXDataSetType type,
	                                             double min, double max,
	                                             @Nullable String trackPath) {
		GradientScaleType scaleType = toGradientScaleType(type);
		if (scaleType == null) {
			return null;
		}
		GradientPaletteCategory category = scaleType.toPaletteCategory();
		String name = findPaletteName(app, trackPath);
		PaletteItem item = app.getPaletteRepository().findPaletteItem(category.getId(), name);
		if (!(item instanceof PaletteItem.Gradient gradient)) {
			return null;
		}
		ColorPalette palette = gradient.getColorPalette();
		if (!palette.isValid()) {
			return null;
		}
		return gradient.isFixed() ? palette : new ColorPalette(palette, min, max, isBipolar(type));
	}

	/**
	 * A saved track keeps its palette in its own COLOR_PALETTE parameter; only the track being
	 * recorded right now uses the global setting. Reading only the global one meant that choosing a
	 * palette for a saved track had no effect here at all.
	 */
	@NonNull
	private static String findPaletteName(@NonNull OsmandApplication app, @Nullable String trackPath) {
		if (trackPath != null) {
			GpxAppearanceHelper helper = new GpxAppearanceHelper(app);
			String name = helper.getAppearanceParameter(new File(trackPath), COLOR_PALETTE);
			if (name != null) {
				return name;
			}
		}
		return app.getSettings().CURRENT_GRADIENT_PALETTE.get();
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

	@NonNull
	private static ColorPalette buildPalette(@NonNull double[] values, @NonNull int[] colors) {
		ColorPalette palette = new ColorPalette();
		for (int i = 0; i < values.length; i++) {
			palette.addPoint(values[i], colors[i]);
		}
		return palette;
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
