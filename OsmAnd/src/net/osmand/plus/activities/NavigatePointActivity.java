package net.osmand.plus.activities;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.StringTokenizer;

import net.osmand.LogUtil;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.views.OsmandMapTileView;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class NavigatePointActivity extends Activity {
	Dialog dlg;
	MapActivity activity; 
	int currentFormat = Location.FORMAT_DEGREES;
	
	// dialog constructor
	public NavigatePointActivity(MapActivity activity){
		this.activity = activity;
		dlg =  new Dialog(activity);
	}
	// activity constructor
	public NavigatePointActivity() {
	}
	
	public void showDialog(){
		dlg.setContentView(R.layout.navigate_point);
		dlg.setTitle(R.string.map_specify_point);
		LatLon loc = activity.getMapLocation();
		initUI(loc.getLatitude(), loc.getLongitude());
		dlg.show();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// LatLon loc = OsmandSettings.getOsmandSettings(this).getLastKnownMapLocation();
		LatLon loc = MapActivity.getMapLocation();
		setContentView(R.layout.navigate_point);
		setTitle(R.string.map_specify_point);
		initUI(loc.getLatitude(), loc.getLongitude());
		((Button) findViewById(R.id.Cancel)).setText(getString(R.string.navigate_to));
	}
	
	@Override
	public View findViewById(int id) {
		if(dlg != null){
			return dlg.findViewById(id);
		}
		return super.findViewById(id);
	}
	
	public void close(){
		if(dlg != null){
			dlg.dismiss();
		} else {
			MapActivity.launchMapActivityMoveToTop(this);
		}
	}
	
	public void initUI(double latitude, double longitude){
		latitude = MapUtils.checkLatitude(latitude);
		longitude = MapUtils.checkLongitude(longitude);
		((TextView)findViewById(R.id.LatitudeEdit)).setText(convert(latitude, Location.FORMAT_DEGREES));
		((TextView)findViewById(R.id.LongitudeEdit)).setText(convert(longitude, Location.FORMAT_DEGREES));
		((RadioButton)findViewById(R.id.Format1)).setChecked(true);
		((RadioGroup)findViewById(R.id.RadioGroup)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				int newFormat = currentFormat;
				if(checkedId == R.id.Format1){
					newFormat = Location.FORMAT_DEGREES;
				} else if(checkedId == R.id.Format2){
					newFormat = Location.FORMAT_MINUTES;
				} else if(checkedId == R.id.Format3){
					newFormat = Location.FORMAT_SECONDS;
				}
				try { 
					double lat = convert(((TextView) findViewById(R.id.LatitudeEdit)).getText().toString());
					double lon = convert(((TextView) findViewById(R.id.LongitudeEdit)).getText().toString());
					((TextView) findViewById(R.id.ValidateTextView)).setVisibility(View.INVISIBLE);
					((TextView)findViewById(R.id.LatitudeEdit)).setText(convert(lat, newFormat));
					((TextView)findViewById(R.id.LongitudeEdit)).setText(convert(lon, newFormat));
				} catch (RuntimeException e) {
					((TextView) findViewById(R.id.ValidateTextView)).setVisibility(View.VISIBLE);
					((TextView) findViewById(R.id.ValidateTextView)).setText(R.string.invalid_locations);
					Log.w(LogUtil.TAG, "Convertion failed", e); //$NON-NLS-1$
				}
			}
			
		});
		((Button) findViewById(R.id.Cancel)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(dlg == null){
					showOnMap(true);
				} else {
					close();
				}
			}
		});
		((Button) findViewById(R.id.ShowOnMap)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showOnMap(false);
			}
		});
	}
	
	public void showOnMap(boolean navigate){
		try {
			double lat = convert(((TextView) findViewById(R.id.LatitudeEdit)).getText().toString());
			double lon = convert(((TextView) findViewById(R.id.LongitudeEdit)).getText().toString());
			
			if(navigate){
				OsmandSettings.getOsmandSettings(this).setPointToNavigate(lat, lon, MessageFormat.format(getString(R.string.search_history_navigate_to), lat, lon));
			} else {
				// in case when it is dialog
				if(activity != null) {
					OsmandMapTileView v = activity.getMapView();
					v.getAnimatedDraggingThread().startMoving(lat, lon, v.getZoom(), true);
				} else {
					OsmandSettings.getOsmandSettings(this).setMapLocationToShow(lat, lon, MessageFormat.format(getString(R.string.search_history_navigate_to), lat, lon));
				}
			}
			close();
		} catch (RuntimeException e) {
			((TextView) findViewById(R.id.ValidateTextView)).setVisibility(View.VISIBLE);
			((TextView) findViewById(R.id.ValidateTextView)).setText(R.string.invalid_locations);
			Log.w(LogUtil.TAG, "Convertion failed", e); //$NON-NLS-1$
		}
	}
	
	////////////////////////////////////////////////////////////////////////////
	// THIS code is copied from Location.convert() in order to change locale
	// and to fix bug in android implementation : doesn't convert if min = 59.23 or sec = 59.32 or deg=179.3
 	public static final int FORMAT_DEGREES = 0;
	public static final int FORMAT_MINUTES = 1;
	public static final int FORMAT_SECONDS = 2;
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
    	coordinate = coordinate.replace(' ', ':');
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
