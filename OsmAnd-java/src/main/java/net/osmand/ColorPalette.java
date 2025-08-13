package net.osmand;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;

public class ColorPalette {

	private static final Log LOG = PlatformUtil.getLog(ColorPalette.class);

	public static final int DARK_GREY = rgbaToDecimal(92, 92, 92, 255);
	public static final int LIGHT_GREY = rgbaToDecimal(200, 200, 200, 255);
	public static final int GREEN = rgbaToDecimal(90, 220, 95, 255);
	public static final int YELLOW = rgbaToDecimal(212, 239, 50, 255);
	public static final int RED = rgbaToDecimal(243, 55, 77, 255);
	public static final int BLUE_SLOPE = rgbaToDecimal(0, 0, 255, 255);
	public static final int CYAN_SLOPE = rgbaToDecimal(0, 255, 255, 255);
	public static final int GREEN_SLOPE = rgbaToDecimal(46, 185, 0, 255);
	public static final int WHITE = rgbaToDecimal(255, 255, 255, 255);
	public static final int YELLOW_SLOPE = rgbaToDecimal(255, 222, 2, 255);
	public static final int RED_SLOPE = rgbaToDecimal(255, 1, 1, 255);
	public static final int PURPLE_SLOPE = rgbaToDecimal(130, 1, 255, 255);

	public static final int[] COLORS = new int[] { GREEN, YELLOW, RED };
	public static final int[] SLOPE_COLORS = new int[] { CYAN_SLOPE, GREEN_SLOPE, LIGHT_GREY, YELLOW_SLOPE, RED_SLOPE };

	public static final double SLOPE_MIN_VALUE = -1.00;// -100%
	public static final double SLOPE_MAX_VALUE = 1.0;// 100%

	public static final ColorPalette SLOPE_PALETTE = parsePalette(new double[][] { { SLOPE_MIN_VALUE, BLUE_SLOPE },
			{ -0.15, CYAN_SLOPE }, { -0.05, GREEN_SLOPE }, { 0.0, LIGHT_GREY }, { 0.05, YELLOW_SLOPE },
			{ 0.15, RED_SLOPE }, { SLOPE_MAX_VALUE, PURPLE_SLOPE } });
	public static final ColorPalette MIN_MAX_PALETTE = parsePalette(
			new double[][] { { 0, GREEN }, { 0.5, YELLOW }, { 1, RED } });

	List<ColorValue> colors = new ArrayList<>();

	public ColorPalette() {
	}

	// Scale palette to min / max value from (0, 1)
	public ColorPalette(ColorPalette c, double minVal, double maxVal) {
		for (ColorValue cv : c.colors) {
			double val = cv.val * (maxVal - minVal) + minVal;
			colors.add(new ColorValue(val, cv.clr));
		}
	}

	public List<ColorValue> getColors() {
		return colors;
	}

	public static int rgbaToDecimal(int r, int g, int b, int a) {
		int value = ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | ((b & 0xFF) << 0);
		return value;
	}

	public int getColorByValue(double value) {
		if (Double.isNaN(value)) {
			return ColorPalette.LIGHT_GREY;
		}
		for (int i = 0; i < colors.size() - 1; i++) {
			ColorValue min = colors.get(i);
			ColorValue max = colors.get(i + 1);
			if (value == min.val)
				return min.clr;
			if (value >= min.val && value <= max.val) {
				double percent = (value - min.val) / (max.val - min.val);
				return getIntermediateColor(min, max, percent);
			}
		}
		if (value <= colors.get(0).val) {
			return colors.get(0).clr;
		} else if (value >= colors.get(colors.size() - 1).val) {
			return colors.get(colors.size() - 1).clr;
		}
		return ColorPalette.getTransparentColor();
	}

	private int getIntermediateColor(ColorValue min, ColorValue max, double percent) {
		double r = min.r + percent * (max.r - min.r);
		double g = min.g + percent * (max.g - min.g);
		double b = min.b + percent * (max.b - min.b);
		double a = min.a + percent * (max.a - min.a);
		return rgbaToDecimal((int) r, (int) g, (int) b, (int) a);
	}
	
	public static int getIntermediateColor(int min, int max, double percent) {
		double r = red(min) + percent * (red(max) - red(min));
		double g = green(min) + percent * (green(max) - green(min));
		double b = blue(min) + percent * (blue(max) - blue(min));
		double a = alpha(min) + percent * (alpha(max) - alpha(min));
		return rgbaToDecimal((int) r, (int) g, (int) b, (int) a);
	}
	
	@Override
	public String toString() {
		return writeColorPalette();
	}

	public String writeColorPalette() {
		return writeColorPalette(colors);
	}

	public static String writeColorPalette(List<ColorValue> colors) {
		StringBuilder bld = new StringBuilder();
		for (ColorValue v : colors) {
			bld.append(v.val).append(",");
			bld.append(v.r).append(",").append(v.g).append(",").append(v.b).append(",").append(v.a).append("\n");
		}
		return bld.toString().trim();
	}


	private void sortPalette() {
		Collections.sort(colors, new java.util.Comparator<ColorValue>() {
			public int compare(ColorValue a, ColorValue b) {
				return Double.compare(a.val, b.val);
			}
		});
	}

	public static class ColorValue {
		public final int r;
		public final int g;
		public final int b;
		public final int a;
		public final int clr;
		public double val;

		public ColorValue(double val, int r, int g, int b, int a) {
			this.r = r;
			this.g = g;
			this.b = b;
			this.a = a;
			this.clr = rgbaToDecimal(r, g, b, a);
			this.val = val;
		}

		public ColorValue(int clr) {
			this(0, clr);
		}

		public ColorValue(double val, int clr) {
			this.r = red(clr);
			this.g = green(clr);
			this.b = blue(clr);
			this.a = alpha(clr);
			this.clr = clr;
			this.val = val;
		}

		public void setValue(double val) {
			this.val = val;
		}

		public static ColorValue rgba(double val, int r, int g, int b, int a) {
			ColorValue clr = new ColorValue(val, r, g, b, a);
			return clr;
		}

		@Override
		public String toString() {
			return "ColorValue [r=" + r + ", g=" + g + ", b=" + b + ", a=" + a + ", val=" + val + "]";
		}

	}

	public static ColorPalette parsePalette(double[]... vl) {
		ColorPalette palette = new ColorPalette();
		for (double[] v : vl) {
			ColorValue c = null;
			if (v.length == 2) {
				c = new ColorValue(v[0], (int) v[1]);
			} else if (v.length == 4) {
				c = new ColorValue(v[0], (int) v[1], (int) v[2], (int) v[3], 255);
			} else if (v.length >= 5) {
				c = new ColorValue(v[0], (int) v[1], (int) v[2], (int) v[3], (int) v[4]);
			}
			if (c != null) {
				palette.colors.add(c);
			}
		}
		palette.sortPalette();
		return palette;
	}

	public static ColorPalette parseColorPalette(Reader reader) throws IOException {
		return parseColorPalette(reader, true);
	}

	public static ColorPalette parseColorPalette(Reader reader, boolean shouldSort) throws IOException {
		ColorPalette palette = new ColorPalette();
		BufferedReader r = new BufferedReader(reader);
		String line;
		while ((line = r.readLine()) != null) {
			String t = line.trim();
			if (t.startsWith("#")) {
				continue;
			}
			String[] values = t.split(",");
			if (values.length >= 4) {
				try {
					ColorValue rgba = ColorValue.rgba(Double.parseDouble(values[0]), Integer.parseInt(values[1]),
							Integer.parseInt(values[2]), Integer.parseInt(values[3]),
							values.length >= 5 ? Integer.parseInt(values[4]) : 255);
					palette.colors.add(rgba);
				} catch (NumberFormatException e) {
					LOG.error(e.getMessage(), e);
				}
			}
		}
		if (shouldSort) {
			palette.sortPalette();
		}
		return palette;
	}

	private static int red(int value) {
		return (value >> 16) & 0xFF;
	}

	private static int green(int value) {
		return (value >> 8) & 0xFF;
	}

	private static int blue(int value) {
		return (value >> 0) & 0xFF;
	}

	private static int alpha(int value) {
		return (value >> 24) & 0xff;
	}

	public static int getTransparentColor() {
		return rgbaToDecimal(0, 0, 0, 0);
	}

	public static void main(String[] args) throws IOException {
		ColorPalette pls = ColorPalette
				.parseColorPalette(new FileReader("../../resources/color-palette/route_speed_default.txt"));
		System.out.println(pls.colors);
		System.out.println(pls.writeColorPalette());
	}

}
