package net.osmand;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.StringTokenizer;

public class LocationConvert {

	////////////////////////////////////////////////////////////////////////////
	// THIS code is copied from Location.convert() in order to change locale
	// and to fix bug in android implementation : doesn't convert if min = 59.23 or sec = 59.32 or deg=179.3
 	public static final int FORMAT_DEGREES = 0;
	public static final int FORMAT_MINUTES = 1;
	public static final int FORMAT_SECONDS = 2;
	public static final int UTM_FORMAT = 3;
	public static final int OLC_FORMAT = 4;
	private static final char DELIM = ':';

	

	/**
     * Converts a String in one of the formats described by
     * FORMAT_DEGREES, FORMAT_MINUTES, or FORMAT_SECONDS into a
     * double.
     *
     * @throws NullPointerException if coordinate is null
     * @throws IllegalArgumentException if the coordinate is not
     * in one of the valid formats.
     */
    public static double convert(String coordinate, boolean throwException) {
    	coordinate = coordinate.replace(' ', ':').replace('#', ':').replace(',', '.')
    			.replace('\'', ':').replace('\"', ':');
        if (coordinate == null) {
        	if(!throwException) {
        		return Double.NaN;
        	} else {
        		throw new NullPointerException("coordinate");
        	}
        }

        boolean negative = false;
        if (coordinate.charAt(0) == '-') {
            coordinate = coordinate.substring(1);
            negative = true;
        }

        StringTokenizer st = new StringTokenizer(coordinate, ":");
        int tokens = st.countTokens();
        if (tokens < 1) {
        	if(!throwException) {
        		return Double.NaN;
        	} else {
        		throw new IllegalArgumentException("coordinate=" + coordinate);
        	}
        }
        try {
            String degrees = st.nextToken();
            double val;
            if (tokens == 1) {
                val = Double.parseDouble(degrees);
                return negative ? -val : val;
            }

            String minutes = st.nextToken();
            int deg = Integer.parseInt(degrees);
            double min;
            double sec = 0.0;

            if (st.hasMoreTokens()) {
                min = Integer.parseInt(minutes);
                String seconds = st.nextToken();
                sec = Double.parseDouble(seconds);
            } else {
                min = Double.parseDouble(minutes);
            }

            boolean isNegative180 = negative && (deg == 180) &&
                (min == 0) && (sec == 0);

            // deg must be in [0, 179] except for the case of -180 degrees
            if ((deg < 0.0) || (deg > 180 && !isNegative180)) {
            	if(!throwException) {
            		return Double.NaN;
            	} else {
            		throw new IllegalArgumentException("coordinate=" + coordinate);
            	}
            }
            if (min < 0 || min > 60d) {
            	if(!throwException) {
            		return Double.NaN;
            	} else {
            		throw new IllegalArgumentException("coordinate=" +
                        coordinate);
            	}
            }
            if (sec < 0 || sec > 60d) {
            	if(!throwException) {
            		return Double.NaN;
            	} else {
            		throw new IllegalArgumentException("coordinate=" +
                        coordinate);
            	}
            }

            val = deg*3600.0 + min*60.0 + sec;
            val /= 3600.0;
            return negative ? -val : val;
        } catch (NumberFormatException nfe) {
        	if(!throwException) {
        		return Double.NaN;
        	} else {
        		throw new IllegalArgumentException("coordinate=" + coordinate);
        	}
        }
    }


	public static String convert(double coordinate, int outputType) {
		if (coordinate < -180.0 || coordinate > 180.0 || Double.isNaN(coordinate)) {
			throw new IllegalArgumentException("coordinate=" + coordinate); //$NON-NLS-1$
		}
		if ((outputType != FORMAT_DEGREES) && (outputType != FORMAT_MINUTES) && (outputType != FORMAT_SECONDS)) {
			throw new IllegalArgumentException("outputType=" + outputType); //$NON-NLS-1$
		}

		StringBuilder sb = new StringBuilder();

		// Handle negative values
		if (coordinate < 0) {
			sb.append('-');
			coordinate = -coordinate;
		}

		DecimalFormat df = new DecimalFormat("##0.00000", new DecimalFormatSymbols(Locale.US)); //$NON-NLS-1$
		if (outputType == FORMAT_MINUTES || outputType == FORMAT_SECONDS) {
			int degrees = (int) Math.floor(coordinate);
			sb.append(degrees);
			sb.append(DELIM);
			coordinate -= degrees;
			coordinate *= 60.0;
			if (outputType == FORMAT_SECONDS) {
				int minutes = (int) Math.floor(coordinate);
				sb.append(minutes);
				sb.append(DELIM);
				coordinate -= minutes;
				coordinate *= 60.0;
			}
		}
		sb.append(df.format(coordinate));
		return sb.toString();
	}
}
