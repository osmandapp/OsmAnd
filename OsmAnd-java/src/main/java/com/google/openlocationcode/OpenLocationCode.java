package com.google.openlocationcode;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Representation of open location code. https://github.com/google/open-location-code The
 * OpenLocationCode class is a wrapper around String value {@code code}, which guarantees that the
 * value is a valid Open Location Code.
 *
 * @author Jiri Semecky
 */
public final class OpenLocationCode {

  private static final BigDecimal BD_0 = new BigDecimal(0);
  private static final BigDecimal BD_5 = new BigDecimal(5);
  private static final BigDecimal BD_4 = new BigDecimal(4);
  private static final BigDecimal BD_20 = new BigDecimal(20);
  private static final BigDecimal BD_90 = new BigDecimal(90);
  private static final BigDecimal BD_180 = new BigDecimal(180);
  private static final double LATITUDE_PRECISION_8_DIGITS = computeLatitudePrecision(8) / 4;
  private static final double LATITUDE_PRECISION_6_DIGITS = computeLatitudePrecision(6) / 4;
  private static final double LATITUDE_PRECISION_4_DIGITS = computeLatitudePrecision(4) / 4;

  private static final char[] ALPHABET = "23456789CFGHJMPQRVWX".toCharArray();
  private static final Map<Character, Integer> CHARACTER_TO_INDEX = new HashMap<>();

  static {
    int index = 0;
    for (char character : ALPHABET) {
      char lowerCaseCharacter = Character.toLowerCase(character);
      CHARACTER_TO_INDEX.put(character, index);
      CHARACTER_TO_INDEX.put(lowerCaseCharacter, index);
      index++;
    }
  }

  private static final char SEPARATOR = '+';
  private static final char SEPARATOR_POSITION = 8;
  private static final char SUFFIX_PADDING = '0';

  /** Class providing information about area covered by Open Location Code. */
  public class CodeArea {

    private final BigDecimal southLatitude;
    private final BigDecimal westLongitude;
    private final BigDecimal northLatitude;
    private final BigDecimal eastLongitude;

    public CodeArea(
        BigDecimal southLatitude,
        BigDecimal westLongitude,
        BigDecimal northLatitude,
        BigDecimal eastLongitude) {
      this.southLatitude = southLatitude;
      this.westLongitude = westLongitude;
      this.northLatitude = northLatitude;
      this.eastLongitude = eastLongitude;
    }

    public double getSouthLatitude() {
      return southLatitude.doubleValue();
    }

    public double getWestLongitude() {
      return westLongitude.doubleValue();
    }

    public double getLatitudeHeight() {
      return northLatitude.subtract(southLatitude).doubleValue();
    }

    public double getLongitudeWidth() {
      return eastLongitude.subtract(westLongitude).doubleValue();
    }

    public double getCenterLatitude() {
      return southLatitude.add(northLatitude).doubleValue() / 2;
    }

    public double getCenterLongitude() {
      return westLongitude.add(eastLongitude).doubleValue() / 2;
    }

    public double getNorthLatitude() {
      return northLatitude.doubleValue();
    }

    public double getEastLongitude() {
      return eastLongitude.doubleValue();
    }
  }


  /** The state of the OpenLocationCode. */
  private final String code;

  /** Creates Open Location Code for the provided code. */
  public OpenLocationCode(String code) {
    if (!isValidCode(code)) {
      throw new IllegalArgumentException(
          "The provided code '" + code + "' is not a valid Open Location Code.");
    }
    this.code = code.toUpperCase();
  }

  /** Creates Open Location Code from the provided latitude, longitude and desired code length. */
  public OpenLocationCode(double latitude, double longitude, int codeLength)
      throws IllegalArgumentException {
    if (codeLength < 4 || (codeLength < 10 & codeLength % 2 == 1)) {
      throw new IllegalArgumentException("Illegal code length " + codeLength);
    }

    latitude = clipLatitude(latitude);
    longitude = normalizeLongitude(longitude);

    // Latitude 90 needs to be adjusted to be just less, so the returned code can also be decoded.
    if (latitude == 90) {
      latitude = latitude - 0.9 * computeLatitudePrecision(codeLength);
    }

    StringBuilder codeBuilder = new StringBuilder();

    // Ensure the latitude and longitude are within [0, 180] and [0, 360) respectively.
    /* Note: double type can't be used because of the rounding arithmetic due to floating point
     * implementation. Eg. "8.95 - 8" can give result 0.9499999999999 instead of 0.95 which
     * incorrectly classify the points on the border of a cell.
     */
    BigDecimal remainingLongitude = new BigDecimal(longitude + 180);
    BigDecimal remainingLatitude = new BigDecimal(latitude + 90);

    // Create up to 10 significant digits from pairs alternating latitude and longitude.
    int generatedDigits = 0;

    while (generatedDigits < codeLength) {
      // Always the integer part of the remaining latitude/longitude will be used for the following
      // digit.
      if (generatedDigits == 0) {
        // First step World division: Map <0..400) to <0..20) for both latitude and longitude.
        remainingLatitude = remainingLatitude.divide(BD_20);
        remainingLongitude = remainingLongitude.divide(BD_20);
      } else if (generatedDigits < 10) {
        remainingLatitude = remainingLatitude.multiply(BD_20);
        remainingLongitude = remainingLongitude.multiply(BD_20);
      } else {
        remainingLatitude = remainingLatitude.multiply(BD_5);
        remainingLongitude = remainingLongitude.multiply(BD_4);
      }
      int latitudeDigit = remainingLatitude.intValue();
      int longitudeDigit = remainingLongitude.intValue();
      if (generatedDigits < 10) {
        codeBuilder.append(ALPHABET[latitudeDigit]);
        codeBuilder.append(ALPHABET[longitudeDigit]);
        generatedDigits += 2;
      } else {
        codeBuilder.append(ALPHABET[4 * latitudeDigit + longitudeDigit]);
        generatedDigits += 1;
      }
      remainingLatitude = remainingLatitude.subtract(new BigDecimal(latitudeDigit));
      remainingLongitude = remainingLongitude.subtract(new BigDecimal(longitudeDigit));
      if (generatedDigits == SEPARATOR_POSITION) {
        codeBuilder.append(SEPARATOR);
      }
    }
    if (generatedDigits < SEPARATOR_POSITION) {
      for (; generatedDigits < SEPARATOR_POSITION; generatedDigits++) {
        codeBuilder.append(SUFFIX_PADDING);
      }
      codeBuilder.append(SEPARATOR);
    }
    this.code = codeBuilder.toString();
  }

  /** Creates Open Location Code with code length 10 from the provided latitude, longitude. */
  public OpenLocationCode(double latitude, double longitude) {
    this(latitude, longitude, 10);
  }

  public String getCode() {
    return code;
  }

  /**
   * Encodes latitude/longitude into 10 digit Open Location Code. This method is equivalent to
   * creating the OpenLocationCode object and getting the code from it.
   */
  public static String encode(double latitude, double longitude) {
    return new OpenLocationCode(latitude, longitude).getCode();
  }

  /**
   * Encodes latitude/longitude into Open Location Code of the provided length. This method is
   * equivalent to creating the OpenLocationCode object and getting the code from it.
   */
  public static String encode(double latitude, double longitude, int codeLength) {
    return new OpenLocationCode(latitude, longitude, codeLength).getCode();
  }

  /**
   * Decodes {@link OpenLocationCode} object into {@link CodeArea} object encapsulating
   * latitude/longitude bounding box.
   */
  public CodeArea decode() {
    if (!isFullCode(code)) {
      throw new IllegalStateException(
          "Method decode() could only be called on valid full codes, code was " + code + ".");
    }
    String decoded = code.replaceAll("[0+]", "");
    // Decode the lat/lng pair component.
    BigDecimal southLatitude = BD_0;
    BigDecimal westLongitude = BD_0;

    int digit = 0;
    double latitudeResolution = 400, longitudeResolution = 400;

    // Decode pair.
    while (digit < decoded.length()) {
      if (digit < 10) {
        latitudeResolution /= 20;
        longitudeResolution /= 20;
        southLatitude =
            southLatitude.add(
                new BigDecimal(latitudeResolution * CHARACTER_TO_INDEX.get(decoded.charAt(digit))));
        westLongitude =
            westLongitude.add(
                new BigDecimal(
                    longitudeResolution * CHARACTER_TO_INDEX.get(decoded.charAt(digit + 1))));
        digit += 2;
      } else {
        latitudeResolution /= 5;
        longitudeResolution /= 4;
        southLatitude =
            southLatitude.add(
                new BigDecimal(
                    latitudeResolution * (CHARACTER_TO_INDEX.get(decoded.charAt(digit)) / 4)));
        westLongitude =
            westLongitude.add(
                new BigDecimal(
                    longitudeResolution * (CHARACTER_TO_INDEX.get(decoded.charAt(digit)) % 4)));
        digit += 1;
      }
    }
    return new CodeArea(
        southLatitude.subtract(BD_90),
        westLongitude.subtract(BD_180),
        southLatitude.subtract(BD_90).add(new BigDecimal(latitudeResolution)),
        westLongitude.subtract(BD_180).add(new BigDecimal(longitudeResolution)));
  }

  /**
   * Decodes code representing Open Location Code into {@link CodeArea} object encapsulating
   * latitude/longitude bounding box.
   *
   * @param code Open Location Code to be decoded.
   * @throws IllegalArgumentException if the provided code is not a valid Open Location Code.
   */
  public static CodeArea decode(String code) throws IllegalArgumentException {
    return new OpenLocationCode(code).decode();
  }

  /** Returns whether this {@link OpenLocationCode} is a full Open Location Code. */
  public boolean isFull() {
    return code.indexOf(SEPARATOR) == SEPARATOR_POSITION;
  }

  /** Returns whether the provided Open Location Code is a full Open Location Code. */
  public static boolean isFull(String code) throws IllegalArgumentException {
    return new OpenLocationCode(code).isFull();
  }

  /** Returns whether this {@link OpenLocationCode} is a short Open Location Code. */
  public boolean isShort() {
    return code.indexOf(SEPARATOR) >= 0 && code.indexOf(SEPARATOR) < SEPARATOR_POSITION;
  }

  /** Returns whether the provided Open Location Code is a short Open Location Code. */
  public static boolean isShort(String code) throws IllegalArgumentException {
    return new OpenLocationCode(code).isShort();
  }

  /**
   * Returns whether this {@link OpenLocationCode} is a padded Open Location Code, meaning that it
   * contains less than 8 valid digits.
   */
  private boolean isPadded() {
    return code.indexOf(SUFFIX_PADDING) >= 0;
  }

  /**
   * Returns whether the provided Open Location Code is a padded Open Location Code, meaning that it
   * contains less than 8 valid digits.
   */
  public static boolean isPadded(String code) throws IllegalArgumentException {
    return new OpenLocationCode(code).isPadded();
  }

  /**
   * Returns short {@link OpenLocationCode} from the full Open Location Code created by removing
   * four or six digits, depending on the provided reference point.  It removes as many digits as
   * possible.
   */
  public OpenLocationCode shorten(double referenceLatitude, double referenceLongitude) {
    if (!isFull()) {
      throw new IllegalStateException("shorten() method could only be called on a full code.");
    }
    if (isPadded()) {
      throw new IllegalStateException("shorten() method can not be called on a padded code.");
    }

    CodeArea codeArea = decode();
    double latitudeDiff = Math.abs(referenceLatitude - codeArea.getCenterLatitude());
    double longitudeDiff = Math.abs(referenceLongitude - codeArea.getCenterLongitude());

    if (latitudeDiff < LATITUDE_PRECISION_8_DIGITS && longitudeDiff < LATITUDE_PRECISION_8_DIGITS) {
      return new OpenLocationCode(code.substring(8));
    }
    if (latitudeDiff < LATITUDE_PRECISION_6_DIGITS && longitudeDiff < LATITUDE_PRECISION_6_DIGITS) {
      return new OpenLocationCode(code.substring(6));
    }
    if (latitudeDiff < LATITUDE_PRECISION_4_DIGITS && longitudeDiff < LATITUDE_PRECISION_4_DIGITS) {
      return new OpenLocationCode(code.substring(4));
    }
    throw new IllegalArgumentException(
        "Reference location is too far from the Open Location Code center.");
  }

  /**
   * Returns an {@link OpenLocationCode} object representing a full Open Location Code from this
   * (short) Open Location Code, given the reference location.
   */
  public OpenLocationCode recover(double referenceLatitude, double referenceLongitude) {
    if (isFull()) {
      // Note: each code is either full xor short, no other option.
      return this;
    }
    referenceLatitude = clipLatitude(referenceLatitude);
    referenceLongitude = normalizeLongitude(referenceLongitude);

    int digitsToRecover = 8 - code.indexOf(SEPARATOR);
    // The resolution (height and width) of the padded area in degrees.
    double paddedAreaSize = Math.pow(20, 2 - (digitsToRecover / 2));

    // Use the reference location to pad the supplied short code and decode it.
    String recoveredPrefix =
        new OpenLocationCode(referenceLatitude, referenceLongitude)
            .getCode()
            .substring(0, digitsToRecover);
    OpenLocationCode recovered = new OpenLocationCode(recoveredPrefix + code);
    CodeArea recoveredCodeArea = recovered.decode();
    double recoveredLatitude = recoveredCodeArea.getCenterLatitude();
    double recoveredLongitude = recoveredCodeArea.getCenterLongitude();

    // Move the recovered latitude by one resolution up or down if it is too far from the reference.
    double latitudeDiff = recoveredLatitude - referenceLatitude;
    if (latitudeDiff > paddedAreaSize / 2) {
      recoveredLatitude -= paddedAreaSize;
    } else if (latitudeDiff < -paddedAreaSize / 2) {
      recoveredLatitude += paddedAreaSize;
    }

    // Move the recovered longitude by one resolution up or down if it is too far from the
    // reference.
    double longitudeDiff = recoveredCodeArea.getCenterLongitude() - referenceLongitude;
    if (longitudeDiff > paddedAreaSize / 2) {
      recoveredLongitude -= paddedAreaSize;
    } else if (longitudeDiff < -paddedAreaSize / 2) {
      recoveredLongitude += paddedAreaSize;
    }

    return new OpenLocationCode(
        recoveredLatitude, recoveredLongitude, recovered.getCode().length() - 1);
  }

  /**
   * Returns whether the bounding box specified by the Open Location Code contains provided point.
   */
  public boolean contains(double latitude, double longitude) {
    CodeArea codeArea = decode();
    return codeArea.getSouthLatitude() <= latitude
        && latitude < codeArea.getNorthLatitude()
        && codeArea.getWestLongitude() <= longitude
        && longitude < codeArea.getEastLongitude();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OpenLocationCode that = (OpenLocationCode) o;
    return hashCode() == that.hashCode();
  }

  @Override
  public int hashCode() {
    return code != null ? code.hashCode() : 0;
  }

  @Override
  public String toString() {
    return getCode();
  }

  // Exposed static helper methods.

  /** Returns whether the provided string is a valid Open Location code. */
  public static boolean isValidCode(String code) {
    if (code == null || code.length() < 2) {
      return false;
    }

    // There must be exactly one separator.
    int separatorPosition = code.indexOf(SEPARATOR);
    if (separatorPosition == -1) {
      return false;
    }
    if (separatorPosition != code.lastIndexOf(SEPARATOR)) {
      return false;
    }

    if (separatorPosition % 2 != 0) {
      return false;
    }

    // Check first two characters: only some values from the alphabet are permitted.
    if (separatorPosition == 8) {
      // First latitude character can only have first 9 values.
      Integer index0 = CHARACTER_TO_INDEX.get(code.charAt(0));
      if (index0 == null || index0 > 8) {
        return false;
      }

      // First longitude character can only have first 18 values.
      Integer index1 = CHARACTER_TO_INDEX.get(code.charAt(1));
      if (index1 == null || index1 > 17) {
        return false;
      }
    }

    // Check the characters before the separator.
    boolean paddingStarted = false;
    for (int i = 0; i < separatorPosition; i++) {
      if (paddingStarted) {
        // Once padding starts, there must not be anything but padding.
        if (code.charAt(i) != SUFFIX_PADDING) {
          return false;
        }
        continue;
      }
      if (CHARACTER_TO_INDEX.keySet().contains(code.charAt(i))) {
        continue;
      }
      if (SUFFIX_PADDING == code.charAt(i)) {
        paddingStarted = true;
        // Padding can start on even character: 2, 4 or 6.
        if (i != 2 && i != 4 && i != 6) {
          return false;
        }
        continue;
      }
      return false;  // Illegal character.
    }

    // Check the characters after the separator.
    if (code.length() > separatorPosition + 1) {
      if (paddingStarted) {
        return false;
      }
      // Only one character after separator is forbidden.
      if (code.length() == separatorPosition + 2) {
        return false;
      }
      for (int i = separatorPosition + 1; i < code.length(); i++) {
        if (!CHARACTER_TO_INDEX.keySet().contains(code.charAt(i))) {
          return false;
        }
      }
    }

    return true;
  }

  /** Returns if the code is a valid full Open Location Code. */
  public static boolean isFullCode(String code) {
    try {
      return new OpenLocationCode(code).isFull();
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /** Returns if the code is a valid short Open Location Code. */
  public static boolean isShortCode(String code) {
    try {
      return new OpenLocationCode(code).isShort();
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  // Private static methods.

  private static double clipLatitude(double latitude) {
    return Math.min(Math.max(latitude, -90), 90);
  }

  private static double normalizeLongitude(double longitude) {
    if (longitude < -180) {
      longitude = (longitude % 360) + 360;
    }
    if (longitude >= 180) {
      longitude = (longitude % 360) - 360;
    }
    return longitude;
  }

  /**
   * Compute the latitude precision value for a given code length. Lengths <= 10 have the same
   * precision for latitude and longitude, but lengths > 10 have different precisions due to the
   * grid method having fewer columns than rows. Copied from the JS implementation.
   */
  private static double computeLatitudePrecision(int codeLength) {
    if (codeLength <= 10) {
      return Math.pow(20, Math.floor(codeLength / -2 + 2));
    }
    return Math.pow(20, -3) / Math.pow(5, codeLength - 10);
  }
}
