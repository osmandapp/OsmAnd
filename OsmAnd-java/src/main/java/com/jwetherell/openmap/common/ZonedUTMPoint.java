// **********************************************************************
//
// <copyright>
//
//  BBN Technologies
//  10 Moulton Street
//  Cambridge, MA 02138
//  (617) 873-8000
//
//  Copyright (C) BBNT Solutions LLC. All rights reserved.
//
// </copyright>
// **********************************************************************

package com.jwetherell.openmap.common;

public class ZonedUTMPoint extends UTMPoint {

    public ZonedUTMPoint() {
        super();
    }

    public ZonedUTMPoint(LatLonPoint llpoint) {
        super(llpoint);
        zone_letter = getLetterDesignator(llpoint.getLatitude());
    }

    public ZonedUTMPoint(LatLonPoint llpoint, Ellipsoid ellip) {
        super(llpoint, ellip);
        zone_letter = getLetterDesignator(llpoint.getLatitude());
    }

    public ZonedUTMPoint(double northing, double easting, int zone_number, char zone_letter) {
        super(northing, easting, zone_number, MGRSPoint.MGRSZoneToUTMZone(zone_letter));
        this.zone_letter = zone_letter;
    }

    /**
     * Converts UTM coords to lat/long given an ellipsoid.
     * <p>
     * Equations from USGS Bulletin 1532 <br>
     * East Longitudes are positive, West longitudes are negative. <br>
     * North latitudes are positive, South latitudes are negative. <br>
     * 
     * @param ellip
     *            an ellipsoid definition.
     * @param UTMNorthing
     *            A float value for the northing to be converted.
     * @param UTMEasting
     *            A float value for the easting to be converted.
     * @param ZoneNumber
     *            An int value specifiying the UTM zone number.
     * @param ZoneLetter
     *            A char value specifying the ZoneLetter within the ZoneNumber,
     *            letter being MGRS zone.
     * @param llpoint
     *            a LatLonPoint, if you want it to be filled in with the
     *            results. If null, a new LatLonPoint will be allocated.
     * @return A LatLonPoint class instance containing the lat/long value, or
     *         <code>null</code> if conversion failed. If you pass in a
     *         LatLonPoint, it will be returned as well, if successful.
     */
    public static LatLonPoint ZonedUTMtoLL(Ellipsoid ellip, double UTMNorthing, double UTMEasting, int ZoneNumber, char ZoneLetter, LatLonPoint llpoint) {
        return UTMPoint.UTMtoLL(ellip, UTMNorthing, UTMEasting, ZoneNumber, MGRSPoint.MGRSZoneToUTMZone(ZoneLetter), llpoint);
    }

    /**
     * Determines the correct MGRS letter designator for the given latitude
     * returns 'Z' if latitude is outside the MGRS limits of 84N to 80S.
     * 
     * @param lat
     *            The float value of the latitude.
     * 
     * @return A char value which is the MGRS zone letter.
     */
    protected char getLetterDesignator(double lat) {
        // This is here as an error flag to show that the Latitude is
        // outside MGRS limits
        char LetterDesignator = 'Z';

        if ((84 >= lat) && (lat >= 72)) LetterDesignator = 'X';
        else if ((72 > lat) && (lat >= 64)) LetterDesignator = 'W';
        else if ((64 > lat) && (lat >= 56)) LetterDesignator = 'V';
        else if ((56 > lat) && (lat >= 48)) LetterDesignator = 'U';
        else if ((48 > lat) && (lat >= 40)) LetterDesignator = 'T';
        else if ((40 > lat) && (lat >= 32)) LetterDesignator = 'S';
        else if ((32 > lat) && (lat >= 24)) LetterDesignator = 'R';
        else if ((24 > lat) && (lat >= 16)) LetterDesignator = 'Q';
        else if ((16 > lat) && (lat >= 8)) LetterDesignator = 'P';
        else if ((8 > lat) && (lat >= 0)) LetterDesignator = 'N';
        else if ((0 > lat) && (lat >= -8)) LetterDesignator = 'M';
        else if ((-8 > lat) && (lat >= -16)) LetterDesignator = 'L';
        else if ((-16 > lat) && (lat >= -24)) LetterDesignator = 'K';
        else if ((-24 > lat) && (lat >= -32)) LetterDesignator = 'J';
        else if ((-32 > lat) && (lat >= -40)) LetterDesignator = 'H';
        else if ((-40 > lat) && (lat >= -48)) LetterDesignator = 'G';
        else if ((-48 > lat) && (lat >= -56)) LetterDesignator = 'F';
        else if ((-56 > lat) && (lat >= -64)) LetterDesignator = 'E';
        else if ((-64 > lat) && (lat >= -72)) LetterDesignator = 'D';
        else if ((-72 > lat) && (lat >= -80)) LetterDesignator = 'C';
        return LetterDesignator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Zone_number=" + zone_number + ", Hemisphere=" + zone_letter + ", Northing=" + northing + ", Easting=" + easting;
    }
}