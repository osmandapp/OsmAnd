package net.osmand;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;

import net.osmand.util.Algorithms;


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

    public static final int[] COLORS = new int[] {GREEN, YELLOW, RED};
    public static final int[] SLOPE_COLORS = new int[] {CYAN_SLOPE, GREEN_SLOPE, LIGHT_GREY, YELLOW_SLOPE, RED_SLOPE};

    public static final double SLOPE_MIN_VALUE = -1.00;//-100%
    public static final double SLOPE_MAX_VALUE = 1.0;//100%
    //public static final double[][] SLOPE_PALETTE = {{SLOPE_MIN_VALUE, GREEN_SLOPE}, {0.0, WHITE}, {0.125, YELLOW_SLOPE}, {0.25, RED_SLOPE}, {SLOPE_MAX_VALUE, PURPLE_SLOPE}};
    public static final double[][] SLOPE_PALETTE = {{SLOPE_MIN_VALUE, BLUE_SLOPE}, {-0.15, CYAN_SLOPE}, {-0.05, GREEN_SLOPE}, {0.0, LIGHT_GREY}, {0.05, YELLOW_SLOPE}, {0.15, RED_SLOPE}, {SLOPE_MAX_VALUE, PURPLE_SLOPE}};

    public static int rgbaToDecimal(int r, int g, int b, int a) {
        int value = ((a & 0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8)  |
                ((b & 0xFF) << 0);
        return value;
    }

    private static int getRed(int value) {
        return (value >> 16) & 0xFF;
    }

    private static int getGreen(int value) {
        return (value >> 8) & 0xFF;
    }

    private static int getBlue(int value) {
        return (value >> 0) & 0xFF;
    }

    private static int getAlpha(int value) {
        return (value >> 24) & 0xff;
    }
    
    public static int getTransparentColor() {
		return rgbaToDecimal(0, 0, 0, 0);
	}
    
	public static int getIntermediateColor(int minPaletteColor, int maxPaletteColor, double percent) {
		double resultRed = getRed(minPaletteColor) + percent * (getRed(maxPaletteColor) - getRed(minPaletteColor));
		double resultGreen = getGreen(minPaletteColor)
				+ percent * (getGreen(maxPaletteColor) - getGreen(minPaletteColor));
		double resultBlue = getBlue(minPaletteColor) + percent * (getBlue(maxPaletteColor) - getBlue(minPaletteColor));
		double resultAlpha = getAlpha(minPaletteColor)
				+ percent * (getAlpha(maxPaletteColor) - getAlpha(minPaletteColor));
		return rgbaToDecimal((int) resultRed, (int) resultGreen, (int) resultBlue, (int) resultAlpha);
	}

	public static double[][] getPaletteScale(double minValue, double maxValue) {
		return new double[][] { { minValue, GREEN }, { (minValue + maxValue) / 2, YELLOW }, { maxValue, RED } };
	}
	
	public static int[] stringToGradientPalette(String str, String gradientScaleType) {
		boolean isSlope = "gradient_slope_color".equals(gradientScaleType);
		if (Algorithms.isBlank(str)) {
			return isSlope ? SLOPE_COLORS : COLORS;
		}
		String[] arr = str.split(" ");
		if (arr.length < 2) {
			return isSlope ? SLOPE_COLORS : COLORS;
		}
		int[] colors = new int[arr.length];
		try {
			for (int i = 0; i < arr.length; i++) {
				colors[i] = Algorithms.parseColor(arr[i]);
			}
		} catch (IllegalArgumentException e) {
			return isSlope ? SLOPE_COLORS :COLORS;
		}
		return colors;
	}

	public static String gradientPaletteToString(int[] palette, String gradientScaleType) {
		boolean isSlope = "gradient_slope_color".equals(gradientScaleType);
		int[] src;
		if (palette != null && palette.length >= 2) {
			src = palette;
		} else {
			src = isSlope ? SLOPE_COLORS : COLORS;
		}
		StringBuilder stringPalette = new StringBuilder();
		for (int i = 0; i < src.length; i++) {
			stringPalette.append(Algorithms.colorToString(src[i]));
			if (i + 1 != src.length) {
				stringPalette.append(" ");
			}
		}
		return stringPalette.toString();
	}


	public static class ColorValue {
		int r;
		int g;
		int b;
		int a;
		double val;
		
		public static ColorValue rgba(double val, int r, int g, int b, int a) {
			ColorValue clr = new ColorValue();
			clr.r = r;
			clr.g = g;
			clr.b = b;
			clr.a = a;
			clr.val = val;
			return clr;
		}

		@Override
		public String toString() {
			return "ColorValue [r=" + r + ", g=" + g + ", b=" + b + ", a=" + a + ", val=" + val + "]";
		}
		
	}
	
	public static String writeColorPalette(List<ColorValue> palette) throws IOException {
		StringBuilder bld = new StringBuilder();
		for (ColorValue v : palette) {
			bld.append(v.val).append(",").append(v.r).append(",").append(v.g).append(",").append(v.b).append(",")
					.append(v.a).append("\n");
		}
		return bld.toString().trim();
	}
	
	public static List<ColorValue> parseColorPalette(Reader reader) throws IOException {
		List<ColorValue> palette = new ArrayList<>();
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
							Integer.parseInt(values[2]), Integer.parseInt(values[3]), values.length >= 4 ? Integer.parseInt(values[4]) : 255);
					palette.add(rgba);
				} catch (NumberFormatException e) {
					LOG.error(e.getMessage(), e);
				}
			}
		}
		return palette;
	}
	
	
	
	
	public static void main(String[] args) throws IOException {
		List<ColorValue> pls = ColorPalette.parseColorPalette(new FileReader("/Users/victorshcherb/osmand/repos/resources/color-palette/route_speed_default.txt"));
		System.out.println(pls);
		System.out.println(ColorPalette.writeColorPalette(pls));
	}
	
	
}
