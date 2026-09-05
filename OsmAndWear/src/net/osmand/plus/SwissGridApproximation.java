package net.osmand.plus;

import net.osmand.data.LatLon;

/**
 * Approximate transformations between Swiss Grid LV03 / LV95 and WGS84 according to
 * <a href="SwissTopo">https://www.swisstopo.admin.ch/content/swisstopo-internet/de/online/calculation-services/_jcr_content/contentPar/tabs/items/dokumente_und_publik/tabPar/downloadlist/downloadItems/8_1467103085694.download/refsys_d.pdf</a>
 */
public class SwissGridApproximation {

    private SwissGridApproximation() {
    }

    public static LatLon convertLV95ToWGS84(double easting, double northing) {
        double y_prime = (easting - 2600000) / 1000000;
        double x_prime = (northing - 1200000) / 1000000;

        return new LatLon(latitudeToWGS84(x_prime, y_prime), longitudeToWGS84(x_prime, y_prime));
    }

    public static LatLon convertLV03ToWGS84(double easting, double northing) {
        double y_prime = (easting - 600000) / 1000000;
        double x_prime = (northing - 200000) / 1000000;

        return new LatLon(latitudeToWGS84(x_prime, y_prime), longitudeToWGS84(x_prime, y_prime));
    }

    private static double latitudeToWGS84(double x, double y) {
        double latitude_sec = 16.9023892
                + (3.238272 * x)
                - (0.270978 * Math.pow(y, 2))
                - (0.002528 * Math.pow(x, 2))
                - (0.0447 * Math.pow(y, 2) * x)
                - (0.0140 * Math.pow(x, 3));
        return latitude_sec * 100 / 36;
    }

    private static double longitudeToWGS84(double x, double y) {
        double longitude_sec = 2.6779094
                + (4.728982 * y)
                + (0.791484 * y * x)
                + (0.1306 * y * Math.pow(x, 2))
                - (0.0436 * Math.pow(y, 3));
        return longitude_sec * 100 / 36;
    }

    public static double[] convertWGS84ToLV03(LatLon loc) {
        double[] lv95 = convertWGS84ToLV95(loc);

        return new double[]{lv95[0] - 2000000, lv95[1] - 1000000};
    }

    public static double[] convertWGS84ToLV95(LatLon loc) {
        double phi_prime = (decimalToSexagesimalSeconds(loc.getLatitude()) - 169028.66) / 10000;
        double lambda_prime = (decimalToSexagesimalSeconds(loc.getLongitude()) - 26782.5) / 10000;

        double easting = 2600072.37
                + (211455.93 * lambda_prime)
                - (10938.51 * lambda_prime * phi_prime)
                - (0.36 * lambda_prime * Math.pow(phi_prime, 2))
                - (44.54 * Math.pow(lambda_prime, 3));
        double northing = 1200147.07
                + (308807.95 * phi_prime)
                + (3745.25 * Math.pow(lambda_prime, 2))
                + (76.63 * Math.pow(phi_prime, 2))
                - (194.56 * Math.pow(lambda_prime, 2) * phi_prime)
                + (119.79 * Math.pow(phi_prime, 3));

        return new double[]{easting, northing};
    }

    private static double decimalToSexagesimalSeconds(double decimal) {
        int degree = (int) Math.floor(decimal);
        int minutes = (int) Math.floor((decimal - degree) * 60);
        double seconds = (((decimal - degree) * 60) - minutes) * 60;

        return seconds + minutes * 60 + degree * 3600;
    }

}
