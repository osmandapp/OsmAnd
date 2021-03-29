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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MGRSPoint extends ZonedUTMPoint {

    /**
     * UTM zones are grouped, and assigned to one of a group of 6 sets.
     */
    private final static int NUM_100K_SETS = 6;

    /**
     * The column letters (for easting) of the lower left value, per set.
     */
    private final static int[] SET_ORIGIN_COLUMN_LETTERS = { 'A', 'J', 'S', 'A', 'J', 'S' };

    /**
     * The row letters (for northing) of the lower left value, per set.
     */
    private final static int[] SET_ORIGIN_ROW_LETTERS = { 'A', 'F', 'A', 'F', 'A', 'F' };

    public final static int ACCURACY_1_METER = 5;
    public final static int ACCURACY_10_METER = 4;
    public final static int ACCURACY_100_METER = 3;
    public final static int ACCURACY_1000_METER = 2;
    public final static int ACCURACY_10000_METER = 1;

    /** The set origin column letters to use. */
    private int[] originColumnLetters = SET_ORIGIN_COLUMN_LETTERS;
    /** The set origin row letters to use. */
    private int[] originRowLetters = SET_ORIGIN_ROW_LETTERS;

    private final static int A = 'A';
    private final static int I = 'I';
    private final static int O = 'O';
    private final static int V = 'V';
    private final static int Z = 'Z';

    /** The String holding the MGRS coordinate value. */
    private String mgrs = null;

    /**
     * Controls the number of digits that the MGRS coordinate will have, which
     * directly affects the accuracy of the coordinate. Default is
     * ACCURACY_1_METER, which indicates that MGRS coordinates will have 10
     * digits (5 easting, 5 northing) after the 100k two letter code, indicating
     * 1 meter resolution.
     */
    private int accuracy = ACCURACY_1_METER;

    public MGRSPoint() {
    }

    public MGRSPoint(LatLonPoint llpoint) {
        this(llpoint, Ellipsoid.WGS_84);
    }

    public MGRSPoint(LatLonPoint llpoint, Ellipsoid ellip) {
        this();
        LLtoMGRS(llpoint, ellip, this);
    }

    public MGRSPoint(String mgrsString) throws NumberFormatException {
        this();
        setMGRS(mgrsString);
    }

    public MGRSPoint(double northing, double easting, int zoneNumber, char zoneLetter) {
        super(northing, easting, zoneNumber, zoneLetter);
    }

    /**
     * Set the MGRS value for this Point. Will be decoded, and the UTM values
     * figured out. You can call toLatLonPoint() to translate it to lat/lon
     * decimal degrees.
     */
    public void setMGRS(String mgrsString) throws NumberFormatException {
        try {
            mgrs = mgrsString.toUpperCase(); // Just to make sure.
            decode(mgrs);
        } catch (StringIndexOutOfBoundsException sioobe) {
            throw new NumberFormatException("MGRSPoint has bad string: " + mgrsString);
        } catch (NullPointerException npe) {
            // Blow off
        }
    }

    /**
     * Set the UTM parameters from a MGRS string.
     * 
     * @param mgrsString
     *            an UPPERCASE coordinate string is expected.
     */
    protected void decode(String mgrsString) throws NumberFormatException {
        if (mgrsString.contains(" ")) {
            String[] parts = mgrsString.split(" ");
            StringBuilder s = new StringBuilder();
            for (String i : parts) {
                s.append(i);
            }
            mgrsString = s.toString();
        }

        if (mgrsString == null || mgrsString.length() == 0) {
            throw new NumberFormatException("MGRSPoint coverting from nothing");
        }

        // Ensure an upper-case string
        mgrsString = mgrsString.toUpperCase();

        int length = mgrsString.length();

        String hunK = null;
        StringBuffer sb = new StringBuffer();
        char testChar;
        int i = 0;

        // get Zone number
        while (!Character.isLetter(testChar = mgrsString.charAt(i))) {
            if (i >= 2) {
                throw new NumberFormatException("MGRSPoint bad conversion from: " + mgrsString + ", first two characters need to be a number between 1-60.");
            }
            sb.append(testChar);
            i++;
        }

        zone_number = Integer.parseInt(sb.toString());

        if (zone_number < 1 || zone_number > 60) {
            throw new NumberFormatException("MGRSPoint bad conversion from: " + mgrsString + ", first two characters need to be a number between 1-60.");
        }

        if (i == 0 || i + 3 > length) {
            // A good MGRS string has to be 4-5 digits long,
            // ##AAA/#AAA at least.
            throw new NumberFormatException("MGRSPoint bad conversion from: " + mgrsString + ", MGRS string must be at least 4-5 digits long");
        }

        zone_letter = mgrsString.charAt(i++);

        // Should we check the zone letter here? Why not.
        if (zone_letter <= 'A' || zone_letter == 'B' || zone_letter == 'Y' || zone_letter >= 'Z' || zone_letter == 'I' || zone_letter == 'O') {
            throw new NumberFormatException("MGRSPoint zone letter " + zone_letter + " not handled: " + mgrsString);
        }

        hunK = mgrsString.substring(i, i += 2);

        // Validate, check the zone, make sure each letter is between A-Z, not I
        // or O
        char char1 = hunK.charAt(0);
        char char2 = hunK.charAt(1);
        if (char1 < 'A' || char2 < 'A' || char1 > 'Z' || char2 > 'Z' || char1 == 'I' || char2 == 'I' || char1 == 'O' || char2 == 'O') {
            throw new NumberFormatException("MGRSPoint bad conversion from " + mgrsString + ", invalid 100k designator");
        }

        int set = get100kSetForZone(zone_number);

        float east100k = getEastingFromChar(char1, set);
        float north100k = getNorthingFromChar(char2, set);

        // We have a bug where the northing may be 2000000 too low.
        // How do we know when to roll over?

        while (north100k < getMinNorthing(zone_letter)) {
            north100k += 2000000;
        }

        // calculate the char index for easting/northing separator
        int remainder = length - i;

        if (remainder % 2 != 0) {
            throw new NumberFormatException(
                    "MGRSPoint has to have an even number \nof digits after the zone letter and two 100km letters - front \nhalf for easting meters, second half for \nnorthing meters"
                            + mgrsString);
        }

        int sep = remainder / 2;

        float sepEasting = 0f;
        float sepNorthing = 0f;

        if (sep > 0) {
            float accuracyBonus = 100000f / (float) Math.pow(10, sep);
            String sepEastingString = mgrsString.substring(i, i + sep);
            sepEasting = Float.parseFloat(sepEastingString) * accuracyBonus;
            String sepNorthingString = mgrsString.substring(i + sep);
            sepNorthing = Float.parseFloat(sepNorthingString) * accuracyBonus;
        }

        easting = sepEasting + east100k;
        northing = sepNorthing + north100k;
    }

    /**
     * Given the first letter from a two-letter MGRS 100k zone, and given the
     * MGRS table set for the zone number, figure out the easting value that
     * should be added to the other, secondary easting value.
     */
    protected float getEastingFromChar(char e, int set) {
        int baseCol[] = getOriginColumnLetters();
        // colOrigin is the letter at the origin of the set for the
        // column
        int curCol = baseCol[set - 1];
        float eastingValue = 100000f;
        boolean rewindMarker = false;

        while (curCol != e) {
            curCol++;
            if (curCol == I) curCol++;
            if (curCol == O) curCol++;
            if (curCol > Z) {
                if (rewindMarker) {
                    throw new NumberFormatException("Bad character: " + e);
                }
                curCol = A;
                rewindMarker = true;
            }
            eastingValue += 100000f;
        }

        return eastingValue;
    }

    /**
     * Given the second letter from a two-letter MGRS 100k zone, and given the
     * MGRS table set for the zone number, figure out the northing value that
     * should be added to the other, secondary northing value. You have to
     * remember that Northings are determined from the equator, and the vertical
     * cycle of letters mean a 2000000 additional northing meters. This happens
     * approx. every 18 degrees of latitude. This method does *NOT* count any
     * additional northings. You have to figure out how many 2000000 meters need
     * to be added for the zone letter of the MGRS coordinate.
     * 
     * @param n
     *            second letter of the MGRS 100k zone
     * @param set
     *            the MGRS table set number, which is dependent on the UTM zone
     *            number.
     */
    protected float getNorthingFromChar(char n, int set) {

        if (n > 'V') {
            throw new NumberFormatException("MGRSPoint given invalid Northing " + n);
        }

        int baseRow[] = getOriginRowLetters();
        // rowOrigin is the letter at the origin of the set for the
        // column
        int curRow = baseRow[set - 1];
        float northingValue = 0f;
        boolean rewindMarker = false;

        while (curRow != n) {
            curRow++;
            if (curRow == I) curRow++;
            if (curRow == O) curRow++;
            // fixing a bug making whole application hang in this loop
            // when 'n' is a wrong character
            if (curRow > V) {
                if (rewindMarker) { // making sure that this loop ends
                    throw new NumberFormatException("Bad character: " + n);
                }
                curRow = A;
                rewindMarker = true;
            }
            northingValue += 100000f;
        }

        return northingValue;
    }

    /**
     * The function getMinNorthing returns the minimum northing value of a MGRS
     * zone.
     * 
     * portted from Geotrans' c Latitude_Band_Value structure table. zoneLetter
     * : MGRS zone (input)
     */
    protected float getMinNorthing(char zoneLetter) throws NumberFormatException {
        float northing;
        switch (zoneLetter) {
            case 'C':
                northing = 1100000.0f;
                break;
            case 'D':
                northing = 2000000.0f;
                break;
            case 'E':
                northing = 2800000.0f;
                break;
            case 'F':
                northing = 3700000.0f;
                break;
            case 'G':
                northing = 4600000.0f;
                break;
            case 'H':
                northing = 5500000.0f;
                break;
            case 'J':
                northing = 6400000.0f;
                break;
            case 'K':
                northing = 7300000.0f;
                break;
            case 'L':
                northing = 8200000.0f;
                break;
            case 'M':
                northing = 9100000.0f;
                break;
            case 'N':
                northing = 0.0f;
                break;
            case 'P':
                northing = 800000.0f;
                break;
            case 'Q':
                northing = 1700000.0f;
                break;
            case 'R':
                northing = 2600000.0f;
                break;
            case 'S':
                northing = 3500000.0f;
                break;
            case 'T':
                northing = 4400000.0f;
                break;
            case 'U':
                northing = 5300000.0f;
                break;
            case 'V':
                northing = 6200000.0f;
                break;
            case 'W':
                northing = 7000000.0f;
                break;
            case 'X':
                northing = 7900000.0f;
                break;
            default:
                northing = -1.0f;
        }
        if (northing >= 0.0)
            return northing;
        // else
        throw new NumberFormatException("Invalid zone letter: " + zone_letter);
    }

    /**
     * Convert this MGRSPoint to a LatLonPoint, and assume a WGS_84 ellipsoid.
     */
    public LatLonPoint toLatLonPoint() {
        return toLatLonPoint(Ellipsoid.WGS_84, new LatLonPoint());
    }

    /**
     * Convert this MGRSPoint to a LatLonPoint, and use the given ellipsoid.
     */
    public LatLonPoint toLatLonPoint(Ellipsoid ellip) {
        return toLatLonPoint(ellip, new LatLonPoint());
    }

    /**
     * Fill in the given LatLonPoint with the converted values of this
     * MGRSPoint, and use the given ellipsoid.
     */
    public LatLonPoint toLatLonPoint(Ellipsoid ellip, LatLonPoint llpoint) {
        return MGRStoLL(this, ellip, llpoint);
    }

    /**
     * Create a LatLonPoint from a MGRSPoint.
     * 
     * @param mgrsp
     *            to convert.
     * @param ellip
     *            Ellipsoid for earth model.
     * @param llp
     *            a LatLonPoint to fill in values for. If null, a new
     *            LatLonPoint will be returned. If not null, the new values will
     *            be set in this object, and it will be returned.
     * @return LatLonPoint with values converted from MGRS coordinate.
     */
    public static LatLonPoint MGRStoLL(MGRSPoint mgrsp, Ellipsoid ellip, LatLonPoint llp) {
        return UTMtoLL(ellip, mgrsp.northing, mgrsp.easting, mgrsp.zone_number, MGRSPoint.MGRSZoneToUTMZone(mgrsp.zone_letter), llp);
    }

    /**
     * Converts a LatLonPoint to a MGRS Point, assuming the WGS_84 ellipsoid.
     * 
     * @return MGRSPoint, or null if something bad happened.
     */
    public static MGRSPoint LLtoMGRS(LatLonPoint llpoint) {
        return LLtoMGRS(llpoint, Ellipsoid.WGS_84, new MGRSPoint());
    }

    /**
     * Create a MGRSPoint from a LatLonPoint.
     * 
     * @param llp
     *            LatLonPoint to convert.
     * @param ellip
     *            Ellipsoid for earth model.
     * @param mgrsp
     *            a MGRSPoint to fill in values for. If null, a new MGRSPoint
     *            will be returned. If not null, the new values will be set in
     *            this object, and it will be returned.
     * @return MGRSPoint with values converted from lat/lon.
     */
    public static MGRSPoint LLtoMGRS(LatLonPoint llp, Ellipsoid ellip, MGRSPoint mgrsp) {
        if (mgrsp == null) {
            mgrsp = new MGRSPoint();
        }

        // Calling LLtoUTM here results in N/S zone letters! wrong!
        mgrsp = (MGRSPoint) LLtoUTM(llp, ellip, mgrsp);
        // Need to add this to set the right letter for the latitude.
        mgrsp.zone_letter = mgrsp.getLetterDesignator(llp.getLatitude());
        mgrsp.resolve();
        return mgrsp;
    }

    /**
     * Convert MGRS zone letter to UTM zone letter, N or S.
     * 
     * @param mgrsZone
     * @return N of given zone is equal or larger than N, S otherwise.
     */
    public static char MGRSZoneToUTMZone(char mgrsZone) {
        if (Character.toUpperCase(mgrsZone) >= 'N')
            return 'N';
        // else
        return 'S';
    }

    /**
     * Method that provides a check for MGRS zone letters. Returns an uppercase
     * version of any valid letter passed in.
     */
    public char checkZone(char zone) {
        zone = Character.toUpperCase(zone);

        if (zone <= 'A' || zone == 'B' || zone == 'Y' || zone >= 'Z' || zone == 'I' || zone == 'O') {
            throw new NumberFormatException("Invalid MGRSPoint zone letter: " + zone);
        }

        return zone;
    }

    /**
     * Set the number of digits to use for easting and northing numbers in the
     * mgrs string, which reflects the accuracy of the coordinate. From 5 (1
     * meter) to 1 (10,000 meter).
     */
    public void setAccuracy(int value) {
        accuracy = value;
        mgrs = null;
    }

    public int getAccuracy() {
        return accuracy;
    }

    /**
     * Create the mgrs string based on the internal UTM settings, should be
     * called if the accuracy changes.
     * 
     * @param digitAccuracy
     *            The number of digits to use for the northing and easting
     *            numbers. 5 digits reflect a 1 meter accuracy, 4 - 10 meter, 3
     *            - 100 meter, 2 - 1000 meter, 1 - 10,000 meter.
     */
    public void resolve(int digitAccuracy) {
        setAccuracy(digitAccuracy);
        resolve();
    }

    /**
     * Create the mgrs string based on the internal UTM settings, using the
     * accuracy set in the MGRSPoint.
     */
    public void resolve() {
        if (zone_letter == 'Z') {
            mgrs = "Latitude limit exceeded";
        } else {
            StringBuffer sb = new StringBuffer(Integer.toString(zone_number)).append(zone_letter).append(get100kID(easting, northing, zone_number));
            StringBuffer seasting = new StringBuffer(Integer.toString((int) easting));
            StringBuffer snorthing = new StringBuffer(Integer.toString((int) northing));

            while (accuracy + 1 > seasting.length()) {
                seasting.insert(0, '0');
            }

            // We have to be careful here, the 100k values shouldn't
            // be
            // used for calculating stuff here.

            while (accuracy + 1 > snorthing.length()) {
                snorthing.insert(0, '0');
            }

            while (snorthing.length() > 6) {
                snorthing.deleteCharAt(0);
            }

            try {
                sb.append(seasting.substring(1, accuracy + 1)).append(snorthing.substring(1, accuracy + 1));

                mgrs = sb.toString();
            } catch (IndexOutOfBoundsException ioobe) {
                mgrs = null;
            }
        }
    }

    /**
     * Given a UTM zone number, figure out the MGRS 100K set it is in.
     */
    private int get100kSetForZone(int i) {
        int set = i % NUM_100K_SETS;
        if (set == 0) set = NUM_100K_SETS;
        return set;
    }

    /**
     * Provided so that extensions to this class can provide different origin
     * letters, in case of different ellipsoids. The int[] represents all of the
     * first letters in the bottom left corner of each set box, as shown in an
     * MGRS 100K box layout.
     */
    private int[] getOriginColumnLetters() {
        return originColumnLetters;
    }

    /**
     * Provided so that extensions to this class can provide different origin
     * letters, in case of different ellipsoids. The int[] represents all of the
     * second letters in the bottom left corner of each set box, as shown in an
     * MGRS 100K box layout.
     */
    private int[] getOriginRowLetters() {
        return originRowLetters;
    }

    /**
     * Get the two letter 100k designator for a given UTM easting, northing and
     * zone number value.
     */
    private String get100kID(double easting, double northing, int zone_number) {
        int set = get100kSetForZone(zone_number);
        int setColumn = ((int) easting / 100000);
        int setRow = ((int) northing / 100000) % 20;
        return get100kID(setColumn, setRow, set);
    }

    /**
     * Get the two-letter MGRS 100k designator given information translated from
     * the UTM northing, easting and zone number.
     * 
     * @param setColumn
     *            the column index as it relates to the MGRS 100k set
     *            spreadsheet, created from the UTM easting. Values are 1-8.
     * @param setRow
     *            the row index as it relates to the MGRS 100k set spreadsheet,
     *            created from the UTM northing value. Values are from 0-19.
     * @param set
     *            the set block, as it relates to the MGRS 100k set spreadsheet,
     *            created from the UTM zone. Values are from 1-60.
     * @return two letter MGRS 100k code.
     */
    private String get100kID(int setColumn, int setRow, int set) {
        int baseCol[] = getOriginColumnLetters();
        int baseRow[] = getOriginRowLetters();

        // colOrigin and rowOrigin are the letters at the origin of
        // the set
        int colOrigin = baseCol[set - 1];
        int rowOrigin = baseRow[set - 1];

        // colInt and rowInt are the letters to build to return
        int colInt = colOrigin + setColumn - 1;
        int rowInt = rowOrigin + setRow;
        boolean rollover = false;

        if (colInt > Z) {
            colInt = colInt - Z + A - 1;
            rollover = true;
        }

        if (colInt == I || (colOrigin < I && colInt > I) || ((colInt > I || colOrigin < I) && rollover)) {
            colInt++;
        }
        if (colInt == O || (colOrigin < O && colInt > O) || ((colInt > O || colOrigin < O) && rollover)) {
            colInt++;
            if (colInt == I) {
                colInt++;
            }
        }

        if (colInt > Z) {
            colInt = colInt - Z + A - 1;
        }

        if (rowInt > V) {
            rowInt = rowInt - V + A - 1;
            rollover = true;
        } else {
            rollover = false;
        }

        if (rowInt == I || (rowOrigin < I && rowInt > I) || ((rowInt > I || rowOrigin < I) && rollover)) {
            rowInt++;
        }

        if (rowInt == O || (rowOrigin < O && rowInt > O) || ((rowInt > O || rowOrigin < O) && rollover)) {
            rowInt++;
            if (rowInt == I) {
                rowInt++;
            }
        }

        if (rowInt > V) {
            rowInt = rowInt - V + A - 1;
        }

        String twoLetter = (char) colInt + "" + (char) rowInt;

        return twoLetter;
    }

    public String toFlavoredString() {
        try {
            List<String> all = new ArrayList<>();
            for (int i = 0; i <= mgrs.length(); i++) {
                if (Character.isAlphabetic(mgrs.charAt(i))){
                    all.add(mgrs.substring(0,i+1));
                    all.add(mgrs.substring(i+1,i+3));
                    String remains = mgrs.substring(i+3);
                    all.add(remains.substring(0,remains.length()/2));
                    all.add(remains.substring(remains.length()/2));
                    break;
                }
            }
            StringBuilder os = new StringBuilder();
            for(String part: all){
                if (os.length() > 0) os.append(" ");
                os.append(part);
            }
            return os.toString();
        }catch (Exception e){
            return mgrs;
        }
    }

    public String toFlavoredString(int accuracy) {
        try {
            List<String> all = new ArrayList<>();
            for (int i = 0; i <= mgrs.length(); i++) {
                if (Character.isAlphabetic(mgrs.charAt(i))){
                    all.add(mgrs.substring(0,i+1));
                    all.add(mgrs.substring(i+1,i+3));
                    String remains = mgrs.substring(i+3);
                    int easting = Integer.parseInt(remains.substring(0,remains.length()/2));
                    int northing = Integer.parseInt(remains.substring(remains.length()/2));
                    double resolution = Math.pow(10, getAccuracy() - accuracy);
                    long roundedEasting = Math.round(easting/resolution);
                    long roundedNorthing = Math.round(northing/resolution);
                    int eastShift = 0;
                    int northShift = 0;
                    if (roundedEasting == resolution*10){
                        roundedEasting = 0L;
                        eastShift = 1;
                    }
                    if (roundedNorthing == resolution*10){
                        roundedNorthing = 0L;
                        northShift = 1;
                    }
                    if (eastShift != 0 || northShift != 0){
                        all.set(1, shiftChar(all.get(1), eastShift, northShift));
                        String zero = "";
                    }
                    
                    all.add(String.format(Locale.US,"%0" + accuracy + "d", roundedEasting));
                    all.add(String.format(Locale.US,"%0" + accuracy + "d", roundedNorthing));
                    break;
                }
            }
            StringBuilder os = new StringBuilder();
            for(String part: all){
                if (os.length() > 0) os.append(" ");
                os.append(part);
            }
            return os.toString();
        }catch (Exception e){
            return toFlavoredString();
        }
    }

    private static String shiftChar(String chars, int east, int north){
        ArrayList<Character> keys = new ArrayList<Character>(
                Arrays.asList('A','B','C','D','E','F','G','H','J','K','L','M','N','P','Q','R','S','T','U','V','W','X','Y','Z'));
        StringBuilder s = new StringBuilder();
        if (east != 0){
            int idx = keys.indexOf(chars.charAt(0));
            idx += east;
            if (idx >= keys.size()) idx -= keys.size();
            if (idx < 0) idx += keys.size();
            s.append(keys.get(idx));
        }else s.append(chars.charAt(0));
        if (north != 0){
            int idx = keys.indexOf(chars.charAt(1));
            idx += north;
            if (idx >= keys.size()) idx -= keys.size();
            if (idx < 0) idx += keys.size();
            s.append(keys.get(idx));
        }else s.append(chars.charAt(1));
        return s.toString();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return mgrs;
    }
}
