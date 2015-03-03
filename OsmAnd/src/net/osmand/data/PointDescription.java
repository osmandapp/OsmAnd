package net.osmand.data;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.StringTokenizer;

import com.jwetherell.openmap.common.LatLonPoint;
import com.jwetherell.openmap.common.UTMPoint;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;
import android.content.Context;
import android.support.annotation.NonNull;

public class PointDescription {
	private String type = "";
	private String name = "";
	private String typeName;

	public static final String POINT_TYPE_FAVORITE = "favorite";
	public static final String POINT_TYPE_WPT = "wpt";
	public static final String POINT_TYPE_POI = "poi";
	public static final String POINT_TYPE_ADDRESS = "address";
	public static final String POINT_TYPE_OSM_NOTE= "osm_note";
	public static final String POINT_TYPE_MARKER = "marker";
	public static final String POINT_TYPE_NOTE = "avnote";
	public static final String POINT_TYPE_LOCATION = "location";
	public static final String POINT_TYPE_ALARM = "alarm";
	public static final String POINT_TYPE_TARGET = "destination";
	

	public static final PointDescription LOCATION_POINT = new PointDescription(POINT_TYPE_LOCATION, "");

	public PointDescription(String type, String name) {
		this.type = type;
		this.name = name;
		if (this.name == null) {
			this.name = "";
		}
	}
	
	public PointDescription(String type, String typeName, String name) {
		this.type = type;
		this.name = name;
		this.typeName = typeName;
		if (this.name == null) {
			this.name = "";
		}
	}

	public String getTypeName() {
		return typeName;
	}

	@NonNull
	public String getName() {
		return name;
	}

	public String getFullPlainName(Context ctx, double lat, double lon) {
		if (isLocation()) {
			OsmandSettings st = ((OsmandApplication) ctx.getApplicationContext()).getSettings();
			int f = st.COORDINATES_FORMAT.get();
			if (f == PointDescription.UTM_FORMAT) {
				UTMPoint pnt = new UTMPoint(new LatLonPoint(lat, lon));
				return pnt.zone_number + "" + pnt.zone_letter + " " + ((long) pnt.northing) + " "
						+ ((long) pnt.easting);
			} else {
				return ctx.getString(R.string.location_on_map, convert(lat, f), convert(lon, f));
			}
		} else {
			String typeName = this.typeName;
			if (isFavorite()) {
				typeName = ctx.getString(R.string.favorite);
			} else if (isPoi()) {
				typeName = ctx.getString(R.string.poi);
			} else if (isWpt()) {
				return ctx.getString(R.string.gpx_wpt);
			}
			if (!Algorithms.isEmpty(typeName)) {
				if (Algorithms.isEmpty(name)) {
					return typeName;
				} else {
					return typeName + ": " + name;
				}
			}
			return name;
		}
	}

	public boolean isLocation() {
		return POINT_TYPE_LOCATION.equals(type);
	}
	
	public boolean isAddress() {
		return POINT_TYPE_ADDRESS.equals(type);
	}
	
	public boolean isWpt() {
		return POINT_TYPE_WPT.equals(type);
	}
	
	public boolean isPoi() {
		return POINT_TYPE_POI.equals(type);
	}

	public boolean isFavorite() {
		return POINT_TYPE_FAVORITE.equals(type);
	}

	public boolean isAvNote() {
		return POINT_TYPE_NOTE.equals(type);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((typeName == null) ? 0 : typeName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		PointDescription other = (PointDescription) obj;
		if (Algorithms.objectEquals(other.name, name) 
				&& Algorithms.objectEquals(other.type, type)
				&& Algorithms.objectEquals(other.typeName, typeName)) {
			return true;
		}
		return false;
	}
	
	
	public static String getSimpleName(LocationPoint o, Context ctx) {
		return o.getPointDescription(ctx).getFullPlainName(ctx, o.getLatitude(), o.getLongitude());
	}

	public static String serializeToString(PointDescription p) {
		if (p == null) {
			return "";
		}
		String tp = p.type ;
		if(!Algorithms.isEmpty(p.typeName)) {
			tp = tp +'.' + p.typeName;
		}
		return tp + "#" + p.name;
	}

	public static PointDescription deserializeFromString(String s) {
		if (s != null && s.length() > 0) {
			int in = s.indexOf('#');
			if (in >= 0) {
				String nm = s.substring(in + 1).trim();
				String tp = s.substring(0, in);
				if(tp.contains(".")) {
					return new PointDescription(tp.substring(0, tp.indexOf('.')), tp.substring(tp.indexOf('.') + 1), nm);
				} else {
					return new PointDescription(tp, nm);
				}
			}
		}
		return null;
	}
	

	////////////////////////////////////////////////////////////////////////////
	// THIS code is copied from Location.convert() in order to change locale
	// and to fix bug in android implementation : doesn't convert if min = 59.23 or sec = 59.32 or deg=179.3
 	public static final int FORMAT_DEGREES = 0;
	public static final int FORMAT_MINUTES = 1;
	public static final int FORMAT_SECONDS = 2;
	public static final int UTM_FORMAT = 3;
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
    public static double convert(String coordinate) {
    	coordinate = coordinate.replace(' ', ':').replace('#', ':').replace(',', '.');
        if (coordinate == null) {
            throw new NullPointerException("coordinate");
        }

        boolean negative = false;
        if (coordinate.charAt(0) == '-') {
            coordinate = coordinate.substring(1);
            negative = true;
        }

        StringTokenizer st = new StringTokenizer(coordinate, ":");
        int tokens = st.countTokens();
        if (tokens < 1) {
            throw new IllegalArgumentException("coordinate=" + coordinate);
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
                throw new IllegalArgumentException("coordinate=" + coordinate);
            }
            if (min < 0 || min > 60d) {
                throw new IllegalArgumentException("coordinate=" +
                        coordinate);
            }
            if (sec < 0 || sec > 60d) {
                throw new IllegalArgumentException("coordinate=" +
                        coordinate);
            }

            val = deg*3600.0 + min*60.0 + sec;
            val /= 3600.0;
            return negative ? -val : val;
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("coordinate=" + coordinate);
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

		DecimalFormat df = new DecimalFormat("###.#####", new DecimalFormatSymbols(Locale.US)); //$NON-NLS-1$
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