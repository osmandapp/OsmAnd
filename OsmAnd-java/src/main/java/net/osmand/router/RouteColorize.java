package net.osmand.router;

import net.osmand.GPXUtilities;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapAddressReaderAdapter;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.OsmMapUtils;
import net.osmand.util.MapUtils;
import org.apache.commons.logging.Log;

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



    public static final int DARK_GREY = rgbaToDecimal(92, 92, 92, 255);
    public static final int LIGHT_GREY = rgbaToDecimal(200, 200, 200, 255);
    public static final int RED = rgbaToDecimal(255,1,1,255);
    public static final int GREEN = rgbaToDecimal(46,185,0,191);
    public static final int YELLOW = rgbaToDecimal(255,222,2,227);

    public enum ValueType {
        ELEVATION,
        SPEED,
        SLOPE,
        NONE
    }

    private final int VALUE_INDEX = 0;
    private final int DECIMAL_COLOR_INDEX = 1;//sRGB decimal format
    private final int RED_COLOR_INDEX = 1;//RGB
    private final int GREEN_COLOR_INDEX = 2;//RGB
    private final int BLUE_COLOR_INDEX = 3;//RGB
    private final int ALPHA_COLOR_INDEX = 4;//RGBA

    private ValueType valueType;

    public static int SLOPE_RANGE = 150;

    private static final Log LOG = PlatformUtil.getLog(RouteColorize.class);

    /**
     * @param minValue can be NaN
     * @param maxValue can be NaN
     * @param palette  array {{value,color},...} - color in sRGB (decimal) format OR {{value,RED,GREEN,BLUE,ALPHA},...} - color in RGBA format
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
     * @param type ELEVATION, SPEED, SLOPE
     */
    public RouteColorize(int zoom, GPXUtilities.GPXFile gpxFile, ValueType type) {

        if (!gpxFile.hasTrkPt()) {
            LOG.warn("GPX file is not consist of track points");
            return;
        }

        List<Double> latList = new ArrayList<>();
        List<Double> lonList = new ArrayList<>();
        List<Double> valList = new ArrayList<>();
        for (GPXUtilities.Track t : gpxFile.tracks) {
            for (GPXUtilities.TrkSegment ts : t.segments) {
                for (GPXUtilities.WptPt p : ts.points) {
                    latList.add(p.lat);
                    lonList.add(p.lon);
                    if (type == ValueType.SPEED) {
                        valList.add(p.speed);
                    } else {
                        valList.add(p.ele);
                    }
                }
            }
        }

        this.zoom = zoom;
        latitudes = listToArray(latList);
        longitudes = listToArray(lonList);

        if (type == ValueType.SLOPE) {
            values = calculateSlopesByElevations(latitudes, longitudes, listToArray(valList), SLOPE_RANGE);
        } else {
            values = listToArray(valList);
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
    public double[] calculateSlopesByElevations(double[] latitudes, double[] longitudes, double[] elevations, double slopeRange) {

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
            LOG.warn("Sizes of arrays latitudes, longitudes and values are not match");
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
            value = (minValue + maxValue) / 2;
        }
        for (int i = 0; i < palette.length - 1; i++) {
            if (value == palette[i][VALUE_INDEX])
                return new Color((int) palette[i][DECIMAL_COLOR_INDEX]);
            if (value >= palette[i][VALUE_INDEX] && value <= palette[i + 1][VALUE_INDEX]) {
                Color minPaletteColor = new Color((int) palette[i][DECIMAL_COLOR_INDEX]);
                Color maxPaletteColor = new Color((int) palette[i + 1][DECIMAL_COLOR_INDEX]);
                double minPaletteValue = palette[i][VALUE_INDEX];
                double maxPaletteValue = palette[i + 1][VALUE_INDEX];
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

    public void setPalette(double[][] palette) {
        this.palette = palette;
        checkPalette();
        sortPalette();
    }

    private Color getDefaultColor() {
        return new Color(0, 0, 0, 0);
    }

    private List<Data> simplify() {
        if (dataList == null) {
            dataList = new ArrayList<>();
            for (int i = 0; i < latitudes.length; i++) {
                //System.out.println(latitudes[i] + " " + longitudes[i] + " " + values[i]);
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
        if (palette == null || palette.length < 2 || palette[0].length < 2 || palette[1].length < 2) {
            LOG.info("Will use default palette");
            palette = new double[3][2];

            double[][] defaultPalette = {
                    {minValue, GREEN},
                    {valueType == ValueType.SLOPE ? 0 : (minValue + maxValue) / 2, YELLOW},
                    {maxValue, RED}
            };
            palette = defaultPalette;
        }
        double min;
        double max = min = palette[0][VALUE_INDEX];
        int minIndex = 0;
        int maxIndex = 0;
        double[][] sRGBPalette = new double[palette.length][2];
        for (int i = 0; i < palette.length; i++) {
            double[] p = palette[i];
            if (p.length == 2) {
                sRGBPalette[i] = p;
            } else if (p.length == 4) {
                Color color = new Color((int) p[RED_COLOR_INDEX], (int) p[GREEN_COLOR_INDEX], (int) p[BLUE_COLOR_INDEX]);
                sRGBPalette[i] = new double[]{p[VALUE_INDEX], color.getRGB()};
            } else if (p.length >= 5) {
                Color color = new Color((int) p[RED_COLOR_INDEX], (int) p[GREEN_COLOR_INDEX], (int) p[BLUE_COLOR_INDEX], (int) p[ALPHA_COLOR_INDEX]);
                sRGBPalette[i] = new double[]{p[VALUE_INDEX], color.getRGB()};
            }
            if (p[VALUE_INDEX] > max) {
                max = p[VALUE_INDEX];
                maxIndex = i;
            }
            if (p[VALUE_INDEX] < min) {
                min = p[VALUE_INDEX];
                minIndex = i;
            }
        }
        palette = sRGBPalette;
        if (minValue < min) {
            palette[minIndex][VALUE_INDEX] = minValue;
        }
        if (maxValue > max) {
            palette[maxIndex][VALUE_INDEX] = maxValue;
        }
    }

    private void sortPalette() {
        java.util.Arrays.sort(palette, new java.util.Comparator<double[]>() {
            public int compare(double[] a, double[] b) {
                return Double.compare(a[VALUE_INDEX], b[VALUE_INDEX]);
            }
        });
    }

    /**
     * @return double[minElevation, maxElevation, minDist, maxDist]
     */
    private double[] findDerivativeArguments(double[] distances, double[] elevations, int index, double slopeRange) {
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
                LOG.warn("Coefficient fo max must be 0..1 , coef=" + coef);
            }
            result[1] = (1 - coef) * elevations[closestMaxIndex - 1] + coef * elevations[closestMaxIndex];
        }
        if (closestMinIndex >= 0) {
            double diff = distances[closestMinIndex + 1] - distances[closestMinIndex];
            double coef = (minDist - distances[closestMinIndex]) / diff;
            if (coef > 1 || coef < 0) {
                LOG.warn("Coefficient for min must be 0..1 , coef=" + coef);
            }
            result[0] = (1 - coef) * elevations[closestMinIndex] + coef * elevations[closestMinIndex + 1];
        }
        if (Double.isNaN(result[0]) || Double.isNaN(result[1])) {
            LOG.warn("Elevations wasn't calculated");
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

    private double[] listToArray(List<Double> doubleList) {
        double[] result = new double[doubleList.size()];
        for (int i = 0; i < doubleList.size(); i++) {
            result[i] = doubleList.get(i);
        }
        return result;
    }

    private static int rgbaToDecimal(int r, int g, int b, int a) {
        int value = ((a & 0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8)  |
                ((b & 0xFF) << 0);
        return value;
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
