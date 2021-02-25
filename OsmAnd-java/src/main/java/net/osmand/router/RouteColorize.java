package net.osmand.router;

import net.osmand.GPXUtilities;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.OsmMapUtils;
import net.osmand.util.MapUtils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class RouteColorize {

    public int zoom;
    public double[] latitudes;
    public double[] longitudes;
    public double[] values;
    public double minValue;
    public double maxValue;
    public double[][] palette;

    private List<Data> dataList;
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";

    public enum ValueType {
        ELEVATION,
        SPEED,
        SLOPE,
        NONE
    }

    private ValueType valueType;

    public static int SLOPE_RANGE = 150;


    /**
     * @param minValue can be NaN
     * @param maxValue can be NaN
     * @param palette  array {{[color][value]},...}, color in sRGB format
     */
    public RouteColorize(int zoom, double[] latitudes, double[] longitudes, double[] values, double minValue, double maxValue, double[][] palette) {
        this.zoom = zoom;
        this.latitudes = latitudes;
        this.longitudes = longitudes;
        this.values = values;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.palette = palette;

        if (Double.isNaN(minValue) || Double.isNaN(maxValue)) {
            calculateMinMaxValue();
        }
        checkPalette();
        sortPalette();
    }

    /**
     * @param palette array {{[color][value]},...}, color in sRGB format
     * @param type    ELEVATION, SPEED, SLOPE
     */
    public RouteColorize(int zoom, double[][] palette, List<GPXUtilities.WptPt> wptPtList, ValueType type) {
        this.zoom = zoom;
        this.palette = palette;

        latitudes = new double[wptPtList.size()];
        longitudes = new double[wptPtList.size()];
        values = new double[wptPtList.size()];
        double[] elevations = new double[wptPtList.size()];
        for (int i = 0; i < wptPtList.size(); i++) {
            latitudes[i] = wptPtList.get(i).lat;
            longitudes[i] = wptPtList.get(i).lon;
            if (type == ValueType.ELEVATION) {
                values[i] = wptPtList.get(i).ele;
            } else if (type == ValueType.SPEED) {
                values[i] = wptPtList.get(i).speed;
            } else if (type == ValueType.SLOPE) {
                elevations[i] = wptPtList.get(i).ele;
            }
        }

        if (type == ValueType.SLOPE) {
            values = calculateSlopesByElevations(latitudes, longitudes, elevations, SLOPE_RANGE);
        }

        calculateMinMaxValue();
        valueType = type;
        checkPalette();
        sortPalette();
    }

    /**
     * Calculate slopes from elevations needs for right colorizing
     *
     * @param slopeRange - in what range calculate the derivative, usually we used 150 meters
     * @return slopes array, in the begin and the end present NaN values!
     */
    public static double[] calculateSlopesByElevations(double[] latitudes, double[] longitudes, double[] elevations, double slopeRange) {

        double[] newElevations = elevations;
        for (int i = 2; i < elevations.length - 2; i++) {
            newElevations[i] = elevations[i - 2]
                    + elevations[i - 1]
                    + elevations[i]
                    + elevations[i + 1]
                    + elevations[i + 2];
            newElevations[i] /= 5;
        }
        elevations = newElevations;

        double[] slopes = new double[elevations.length];
        if (latitudes.length != longitudes.length || latitudes.length != elevations.length) {
            System.out.println(ANSI_RED + "Sizes of arrays latitudes, longitudes and values  are not match" + ANSI_RESET);
            return slopes;
        }

        double[] distances = new double[elevations.length];
        double totalDistance = 0.0d;
        distances[0] = totalDistance;
        for (int i = 0; i < elevations.length - 1; i++) {
            totalDistance += MapUtils.getDistance(latitudes[i], longitudes[i], latitudes[i + 1], longitudes[i + 1]);
            distances[i + 1] = totalDistance;
        }

        for (int i = 0; i < elevations.length; i++) {
            if (distances[i] < slopeRange / 2 || distances[i] > totalDistance - slopeRange / 2) {
                slopes[i] = Double.NaN;
            } else {
                double[] arg = findDerivativeArguments(distances, elevations, i, slopeRange);
                slopes[i] = (arg[1] - arg[0]) / (arg[3] - arg[2]);
            }
        }
        return slopes;
    }

    public List<Data> getResult(boolean simplify) {
        List<Data> result = new ArrayList<>();
        if (simplify) {
            result = simplify();
        } else {
            for (int i = 0; i < latitudes.length; i++) {
                result.add(new Data(i, latitudes[i], longitudes[i], values[i]));
            }
        }
        for (Data data : result) {
            data.color = getColorByValue(data.val);
        }
        return result;
    }

    public Color getColorByValue(double value) {
        if (Double.isNaN(value)) {
            return getDefaultColor();
        }
        for (int i = 0; i < palette.length - 1; i++) {
            if (value == palette[i][1])
                return new Color((int) palette[i][0]);
            if (value > palette[i][1] && value < palette[i + 1][1]) {
                Color minPaletteColor = new Color((int) palette[i][0]);
                Color maxPaletteColor = new Color((int) palette[i + 1][0]);
                double minPaletteValue = palette[i][1];
                double maxPaletteValue = palette[i + 1][1];
                double percent = (value - minPaletteValue) / (maxPaletteValue - minPaletteValue);
                double resultRed = minPaletteColor.getRed() + percent * (maxPaletteColor.getRed() - minPaletteColor.getRed());
                double resultGreen = minPaletteColor.getGreen() + percent * (maxPaletteColor.getGreen() - minPaletteColor.getGreen());
                double resultBlue = minPaletteColor.getBlue() + percent * (maxPaletteColor.getBlue() - minPaletteColor.getBlue());
                double resultAlpha = minPaletteColor.getAlpha() + percent * (maxPaletteColor.getAlpha() - minPaletteColor.getAlpha());
                return new Color((int) resultRed, (int) resultGreen, (int) resultBlue, (int) resultAlpha);
            }
        }
        return getDefaultColor();
    }

    private Color getDefaultColor() {
        if (valueType != null && valueType == ValueType.SLOPE) {
            return new Color(255, 222, 2, 227);
        }
        return new Color(0, 0, 0, 0);
    }

    private List<Data> simplify() {
        if (dataList == null) {
            dataList = new ArrayList<>();
            for (int i = 0; i < latitudes.length; i++) {
                dataList.add(new Data(i, latitudes[i], longitudes[i], values[i]));
            }
        }
        List<Node> nodes = new ArrayList<>();
        List<Node> result = new ArrayList<>();
        for (Data data : dataList) {
            nodes.add(new net.osmand.osm.edit.Node(data.lat, data.lon, data.id));
        }
        OsmMapUtils.simplifyDouglasPeucker(nodes, zoom + 5, 1, result, true);

        List<Data> simplified = new ArrayList<>();

        for (int i = 1; i < result.size() - 1; i++) {
            int prevId = (int) result.get(i - 1).getId();
            int currentId = (int) result.get(i).getId();
            List<Data> sublist = dataList.subList(prevId, currentId);
            simplified.addAll(getExtremums(sublist));
        }
        return simplified;
    }

    private List<Data> getExtremums(List<Data> subDataList) {
        if (subDataList.size() <= 2)
            return subDataList;

        List<Data> result = new ArrayList<>();
        double min;
        double max;
        min = max = subDataList.get(0).val;
        for (Data pt : subDataList) {
            if (min > pt.val)
                min = pt.val;
            if (max < pt.val)
                max = pt.val;
        }

        double diff = max - min;

        result.add(subDataList.get(0));
        for (int i = 1; i < subDataList.size() - 1; i++) {
            double prev = subDataList.get(i - 1).val;
            double current = subDataList.get(i).val;
            double next = subDataList.get(i + 1).val;
            Data currentData = subDataList.get(i);

            if ((current > prev && current > next) || (current < prev && current < next)
                    || (current < prev && current == next) || (current == prev && current < next)
                    || (current > prev && current == next) || (current == prev && current > next)) {
                Data prevInResult;
                if (result.size() > 0) {
                    prevInResult = result.get(0);
                    if (prevInResult.val / diff > 0.05d) {// check differences in 5%
                        result.add(currentData);
                    }
                } else
                    result.add(currentData);
            }
        }
        result.add(subDataList.get(subDataList.size() - 1));
        return result;
    }

    private void checkPalette() {
        if (palette.length < 2 || palette[0].length < 2 || palette[1].length < 2) {
            System.out.println(ANSI_YELLOW + "Fill palette in {{[color][value]},...} format. Will use default palette" + ANSI_RESET);
            palette = new double[3][2];
            Color red = new Color(255, 1, 1, 255);
            Color yellow = new Color(255, 222, 2, 227);
            Color green = new Color(46, 185, 0, 191);

            double[][] defaultPalette = {
                    { green.getRGB(), minValue},
                    { yellow.getRGB(), valueType == ValueType.SLOPE ? 0 : (minValue + maxValue) / 2},
                    { red.getRGB(), maxValue}
            };
            palette = defaultPalette;
        }
        double min;
        double max = min = palette[0][1];
        int minIndex = 0;
        int maxIndex = 0;
        for (int i = 0; i < palette.length; i++) {
            double[] p = palette[i];
            if (p[1] > max) {
                max = p[1];
                maxIndex = i;
            }
            if (p[1] < min) {
                min = p[1];
                minIndex = i;
            }
        }
        if (minValue < min) {
            palette[minIndex][1] = minValue;
        }
        if (maxValue > max) {
            palette[maxIndex][1] = maxValue;
        }
    }

    private void sortPalette() {
        java.util.Arrays.sort(palette, new java.util.Comparator<double[]>() {
            public int compare(double[] a, double[] b) {
                return Double.compare(a[1], b[1]);
            }
        });
    }

    /**
     * @return double[minElevation, maxElevation, minDist, maxDist]
     */
    private static double[] findDerivativeArguments(double[] distances, double[] elevations, int index, double slopeRange) {
        double[] result = new double[4];
        double minDist = distances[index] - slopeRange / 2;
        double maxDist = distances[index] + slopeRange / 2;
        result[0] = Double.NaN;
        result[1] = Double.NaN;
        result[2] = minDist;
        result[3] = maxDist;
        int closestMaxIndex = -1;
        int closestMinIndex = -1;
        for (int i = index; i < distances.length; i++) {
            if (distances[i] == maxDist) {
                result[1] = elevations[i];
                break;
            }
            if (distances[i] > maxDist) {
                closestMaxIndex = i;
                break;
            }
        }
        for (int i = index; i >= 0; i--) {
            if (distances[i] == minDist) {
                result[0] = elevations[i];
                break;
            }
            if (distances[i] < minDist) {
                closestMinIndex = i;
                break;
            }
        }
        if (closestMaxIndex > 0) {
            double diff = distances[closestMaxIndex] - distances[closestMaxIndex - 1];
            double coef = (maxDist - distances[closestMaxIndex - 1]) / diff;
            if (coef > 1 || coef < 0) {
                System.out.println(ANSI_RED + "Coefficient fo max must be 0..1 , coef=" + coef + ANSI_RESET);
            }
            result[1] = (1 - coef) * elevations[closestMaxIndex - 1] + coef * elevations[closestMaxIndex];
        }
        if (closestMinIndex >= 0) {
            double diff = distances[closestMinIndex + 1] - distances[closestMinIndex];
            double coef = (minDist - distances[closestMinIndex]) / diff;
            if (coef > 1 || coef < 0) {
                System.out.println(ANSI_RED + "Coefficient for min must be 0..1 , coef=" + coef + ANSI_RESET);
            }
            result[0] = (1 - coef) * elevations[closestMinIndex] + coef * elevations[closestMinIndex + 1];
        }
        if (Double.isNaN(result[0]) || Double.isNaN(result[1])) {
            System.out.println(ANSI_RED + "Elevations wasn't calculated" + ANSI_RESET);
        }
        return result;
    }

    private void calculateMinMaxValue() {
        if (values.length == 0)
            return;
        minValue = maxValue = Double.NaN;
        for (double value : values) {
            if ((Double.isNaN(maxValue) || Double.isNaN(minValue)) && !Double.isNaN(value))
                maxValue = minValue = value;
            if (minValue > value)
                minValue = value;
            if (maxValue < value)
                maxValue = value;
        }
    }

    public class Data {
        int id;
        public double lat;
        public double lon;
        public double val;
        public Color color;

        Data(int id, double lat, double lon, double val) {
            this.id = id;
            this.lat = lat;
            this.lon = lon;
            this.val = val;
        }
    }

}
