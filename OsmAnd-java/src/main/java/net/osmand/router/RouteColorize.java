package net.osmand.router;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.GPXUtilities.Track;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.PlatformUtil;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.OsmMapUtils;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

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

    private List<RouteColorizationPoint> dataList;

    public static final int DARK_GREY = rgbaToDecimal(92, 92, 92, 255);
    public static final int LIGHT_GREY = rgbaToDecimal(200, 200, 200, 255);
    public static final int GREEN = rgbaToDecimal(90, 220, 95, 255);
    public static final int YELLOW = rgbaToDecimal(212, 239, 50, 255);
    public static final int RED = rgbaToDecimal(243, 55, 77, 255);
    public static final int[] colors = new int[] {GREEN, YELLOW, RED};

    private static final int MAX_SLOPE_VALUE = 25;

    public enum ColorizationType {
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

    private ColorizationType colorizationType;

    public static int SLOPE_RANGE = 150;//150 meters
    private static final double MIN_DIFFERENCE_SLOPE = 0.05d;//5%

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
    public RouteColorize(int zoom, GPXFile gpxFile, ColorizationType type) {
        this(zoom, gpxFile, null, type, 0);
    }

    public RouteColorize(int zoom, GPXFile gpxFile, GPXTrackAnalysis analysis, ColorizationType type, float maxProfileSpeed) {

        if (!gpxFile.hasTrkPt()) {
            LOG.warn("GPX file is not consist of track points");
            return;
        }

        List<Double> latList = new ArrayList<>();
        List<Double> lonList = new ArrayList<>();
        List<Double> valList = new ArrayList<>();
        int wptIdx = 0;

        if (analysis == null) {
            analysis = Algorithms.isEmpty(gpxFile.path)
                    ? gpxFile.getAnalysis(System.currentTimeMillis())
                    : gpxFile.getAnalysis(gpxFile.modifiedTime);
        }
        for (Track t : gpxFile.tracks) {
            for (TrkSegment ts : t.segments) {
                for (WptPt p : ts.points) {
                    latList.add(p.lat);
                    lonList.add(p.lon);
                    if (type == ColorizationType.SPEED) {
                        valList.add((double) analysis.speedData.get(wptIdx).speed);
                    } else {
                        valList.add((double) analysis.elevationData.get(wptIdx).elevation);
                    }
                    wptIdx++;
                }
            }
        }

        this.zoom = zoom;
        colorizationType = type;
        latitudes = listToArray(latList);
        longitudes = listToArray(lonList);

        if (type == ColorizationType.SLOPE) {
            values = calculateSlopesByElevations(latitudes, longitudes, listToArray(valList), SLOPE_RANGE);
        } else {
            values = listToArray(valList);
        }
        calculateMinMaxValue();
        maxValue = getMaxValue(colorizationType, analysis, minValue, maxProfileSpeed);
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

    public List<RouteColorizationPoint> getResult(boolean simplify) {
        List<RouteColorizationPoint> result = new ArrayList<>();
        if (simplify) {
            result = simplify();
        } else {
            for (int i = 0; i < latitudes.length; i++) {
                result.add(new RouteColorizationPoint(i, latitudes[i], longitudes[i], values[i]));
            }
        }
        for (RouteColorizationPoint data : result) {
            data.color = getColorByValue(data.val);
        }
        return result;
    }

    public int getColorByValue(double value) {
        if (Double.isNaN(value)) {
            value = (minValue + maxValue) / 2;
        }
        for (int i = 0; i < palette.length - 1; i++) {
            if (value == palette[i][VALUE_INDEX])
                return (int) palette[i][DECIMAL_COLOR_INDEX];
            if (value >= palette[i][VALUE_INDEX] && value <= palette[i + 1][VALUE_INDEX]) {
                int minPaletteColor = (int) palette[i][DECIMAL_COLOR_INDEX];
                int maxPaletteColor = (int) palette[i + 1][DECIMAL_COLOR_INDEX];
                double minPaletteValue = palette[i][VALUE_INDEX];
                double maxPaletteValue = palette[i + 1][VALUE_INDEX];
                double percent = (value - minPaletteValue) / (maxPaletteValue - minPaletteValue);
                double resultRed = getRed(minPaletteColor) + percent * (getRed(maxPaletteColor) - getRed(minPaletteColor));
                double resultGreen = getGreen(minPaletteColor) + percent * (getGreen(maxPaletteColor) - getGreen(minPaletteColor));
                double resultBlue = getBlue(minPaletteColor) + percent * (getBlue(maxPaletteColor) - getBlue(minPaletteColor));
                double resultAlpha = getAlpha(minPaletteColor) + percent * (getAlpha(maxPaletteColor) - getAlpha(minPaletteColor));
                return rgbaToDecimal((int) resultRed, (int) resultGreen, (int) resultBlue, (int) resultAlpha);
            }
        }
        return getTransparentColor();
    }

    public void setPalette(double[][] palette) {
        this.palette = palette;
        checkPalette();
        sortPalette();
    }

    public void setPalette(int[] gradientPalette) {
        if (gradientPalette == null || gradientPalette.length != 3) {
            return;
        }
        setPalette(new double[][] {
                {minValue, gradientPalette[0]},
                {(minValue + maxValue) / 2, gradientPalette[1]},
                {maxValue, gradientPalette[2]}
        });
    }

    private int getTransparentColor() {
        return rgbaToDecimal(0, 0, 0, 0);
    }

    private List<RouteColorizationPoint> simplify() {
        if (dataList == null) {
            dataList = new ArrayList<>();
            for (int i = 0; i < latitudes.length; i++) {
                //System.out.println(latitudes[i] + " " + longitudes[i] + " " + values[i]);
                dataList.add(new RouteColorizationPoint(i, latitudes[i], longitudes[i], values[i]));
            }
        }
        List<Node> nodes = new ArrayList<>();
        List<Node> result = new ArrayList<>();
        for (RouteColorizationPoint data : dataList) {
            nodes.add(new net.osmand.osm.edit.Node(data.lat, data.lon, data.id));
        }
        OsmMapUtils.simplifyDouglasPeucker(nodes, zoom + 5, 1, result, true);

        List<RouteColorizationPoint> simplified = new ArrayList<>();

        for (int i = 1; i < result.size() - 1; i++) {
            int prevId = (int) result.get(i - 1).getId();
            int currentId = (int) result.get(i).getId();
            List<RouteColorizationPoint> sublist = dataList.subList(prevId, currentId);
            simplified.addAll(getExtremums(sublist));
        }
        return simplified;
    }

    private List<RouteColorizationPoint> getExtremums(List<RouteColorizationPoint> subDataList) {
        if (subDataList.size() <= 2) {
            return subDataList;
        }

        List<RouteColorizationPoint> result = new ArrayList<>();
        double min;
        double max;
        min = max = subDataList.get(0).val;
        for (RouteColorizationPoint pt : subDataList) {
            if (min > pt.val) {
                min = pt.val;
            }
            if (max < pt.val) {
                max = pt.val;
            }
        }

        double diff = max - min;

        result.add(subDataList.get(0));
        for (int i = 1; i < subDataList.size() - 1; i++) {
            double prev = subDataList.get(i - 1).val;
            double current = subDataList.get(i).val;
            double next = subDataList.get(i + 1).val;
            RouteColorizationPoint currentData = subDataList.get(i);

            if ((current > prev && current > next) || (current < prev && current < next)
                    || (current < prev && current == next) || (current == prev && current < next)
                    || (current > prev && current == next) || (current == prev && current > next)) {
                RouteColorizationPoint prevInResult;
                if (result.size() > 0) {
                    prevInResult = result.get(0);
                    if (prevInResult.val / diff > MIN_DIFFERENCE_SLOPE) {
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
                    {(minValue + maxValue) / 2, YELLOW},
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
                int color = rgbaToDecimal((int) p[RED_COLOR_INDEX], (int) p[GREEN_COLOR_INDEX], (int) p[BLUE_COLOR_INDEX], 255);
                sRGBPalette[i] = new double[]{p[VALUE_INDEX], color};
            } else if (p.length >= 5) {
                int color = rgbaToDecimal((int) p[RED_COLOR_INDEX], (int) p[GREEN_COLOR_INDEX], (int) p[BLUE_COLOR_INDEX], (int) p[ALPHA_COLOR_INDEX]);
                sRGBPalette[i] = new double[]{p[VALUE_INDEX], color};
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

    public static double getMinValue(ColorizationType type, GPXTrackAnalysis analysis) {
        return type == ColorizationType.ELEVATION ? analysis.minElevation : 0.0;
    }

    public static double getMaxValue(ColorizationType type, GPXTrackAnalysis analysis, double minValue, double maxProfileSpeed) {
        if (type == ColorizationType.SPEED) {
            return Math.max(analysis.maxSpeed, maxProfileSpeed);
        } else if (type == ColorizationType.ELEVATION) {
            return Math.max(analysis.maxElevation, minValue + 50);
        } else {
            return MAX_SLOPE_VALUE;
        }
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

    private int getRed(int value) {
        return (value >> 16) & 0xFF;
    }

    private int getGreen(int value) {
        return (value >> 8) & 0xFF;
    }

    private int getBlue(int value) {
        return (value >> 0) & 0xFF;
    }

    private int getAlpha(int value) {
        return (value >> 24) & 0xff;
    }

    public static class RouteColorizationPoint {
        int id;
        public double lat;
        public double lon;
        public double val;
        public int color;

        RouteColorizationPoint(int id, double lat, double lon, double val) {
            this.id = id;
            this.lat = lat;
            this.lon = lon;
            this.val = val;
        }
    }
}