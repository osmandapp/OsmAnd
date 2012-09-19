package net.osmand.plus.activities;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.StringTokenizer;

import net.osmand.LogUtil;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.search.SearchActivity;
import net.osmand.plus.activities.search.SearchActivity.SearchActivityChild;
import net.osmand.plus.views.OsmandMapTileView;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

public class NavigatePointActivity extends Activity implements SearchActivityChild {
	Dialog dlg;
	MapActivity activity; 
	int currentFormat = Location.FORMAT_DEGREES;
	
	public static final String SEARCH_LAT = SearchActivity.SEARCH_LAT;
	public static final String SEARCH_LON = SearchActivity.SEARCH_LON;
	private OsmandSettings settings;
	
	// dialog constructor
	public NavigatePointActivity(MapActivity activity){
		this.activity = activity;
		dlg =  new Dialog(activity);
		settings = ((OsmandApplication) activity.getApplication()).getSettings();
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
		
		setContentView(R.layout.navigate_point);
		setTitle(R.string.map_specify_point);
		((Button) findViewById(R.id.Cancel)).setText(getString(R.string.navigate_to));
		settings = ((OsmandApplication) getApplication()).getSettings();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		LatLon loc = null;
		Intent intent = getIntent();
		if(intent != null){
			double lat = intent.getDoubleExtra(SEARCH_LAT, 0);
			double lon = intent.getDoubleExtra(SEARCH_LON, 0);
			if(lat != 0 || lon != 0){
				loc = new LatLon(lat, lon);
			}
		}
		if (loc == null && getParent() instanceof SearchActivity) {
			loc = ((SearchActivity) getParent()).getSearchPoint();
		}
		if (loc == null) {
			loc = settings.getLastKnownMapLocation();
		}
		initUI(loc.getLatitude(), loc.getLongitude());
	}
	
	@Override
	public void locationUpdate(LatLon loc) {
		if(loc != null){
			initUI(loc.getLatitude(), loc.getLongitude());
		} else {
			initUI(0, 0);
		}
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
		final TextView latEdit = ((TextView)findViewById(R.id.LatitudeEdit));
		final TextView lonEdit = ((TextView)findViewById(R.id.LongitudeEdit));
		currentFormat = Location.FORMAT_DEGREES;
		latEdit.setText(convert(latitude, Location.FORMAT_DEGREES));
		lonEdit.setText(convert(longitude, Location.FORMAT_DEGREES));
		final Spinner format = ((Spinner)findViewById(R.id.Format));
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.my_spinner_text, new String[] {
				getString(R.string.navigate_point_format_D),
				getString(R.string.navigate_point_format_DM),
				getString(R.string.navigate_point_format_DMS)
		});
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		format.setAdapter(adapter);
		format.setSelection(0);
		format.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				int newFormat = currentFormat;
				String itm = (String) format.getItemAtPosition(position);
				if(getString(R.string.navigate_point_format_D).equals(itm)){
					newFormat = Location.FORMAT_DEGREES;
				} else if(getString(R.string.navigate_point_format_DM).equals(itm)){
					newFormat = Location.FORMAT_MINUTES;
				} else if(getString(R.string.navigate_point_format_DMS).equals(itm)){
					newFormat = Location.FORMAT_SECONDS;
				}
				currentFormat = newFormat;
				try { 
					double lat = convert(((TextView) findViewById(R.id.LatitudeEdit)).getText().toString());
					double lon = convert(((TextView) findViewById(R.id.LongitudeEdit)).getText().toString());
					((TextView) findViewById(R.id.ValidateTextView)).setVisibility(View.INVISIBLE);
					latEdit.setText(convert(lat, newFormat));
					lonEdit.setText(convert(lon, newFormat));
				} catch (RuntimeException e) {
					((TextView) findViewById(R.id.ValidateTextView)).setVisibility(View.VISIBLE);
					((TextView) findViewById(R.id.ValidateTextView)).setText(R.string.invalid_locations);
					Log.w(LogUtil.TAG, "Convertion failed", e); //$NON-NLS-1$
				}
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
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
		TextWatcher textWatcher = new TextWatcher() {
			String pasteString = null;
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				pasteString = null; 
				if(count > 3) {
					pasteString = s.subSequence(start, start + count).toString();
				}
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				
				if(pasteString != null){
					int latSt = -1;
					int latEnd = -1;
					int lonSt = -1;
					int lonEnd = -1;
					int step = 0; // 0 - init, 1- lat, 2-between, 3-lon
					for (int i = 0; i < pasteString.length(); i++) {
						char ch = pasteString.charAt(i);
						if (Character.isDigit(ch)) {
							if (step == 0 || step == 2){
								int t = i;
								if (i > 0 && pasteString.charAt(i - 1) == '-') {
									t--;
								}
								if (step == 0) {
									latSt = t;
								} else {
									lonSt = t;
								}
								step++;
							}
						} else if (ch == '.' || ch == ':' ) {
							// do nothing here
						} else {
							if (step == 1) {
								latEnd = i;
								step++;
							} else if (step == 3) {
								lonEnd = i;
								step++;
								break;
							}
						}
					}
					
					if(lonSt != -1){
						if(lonEnd == -1){
							lonEnd = pasteString.length();
						}
						try {
							String latString = pasteString.substring(latSt, latEnd);
							String lonString = pasteString.substring(lonSt, lonEnd);
							Double.parseDouble(latString);
							Double.parseDouble(lonString);
							latEdit.setText(latString);
							lonEdit.setText(lonString);
						} catch (NumberFormatException e) {
						}
					}
				}
			}
		};
		latEdit.addTextChangedListener(textWatcher);
		lonEdit.addTextChangedListener(textWatcher);
	}
	
	public void showOnMap(boolean navigate){
		try {
			double lat = convert(((TextView) findViewById(R.id.LatitudeEdit)).getText().toString());
			double lon = convert(((TextView) findViewById(R.id.LongitudeEdit)).getText().toString());
			
			if(navigate){
				if(activity != null) {
					MapActivityActions.navigateToPoint(activity, lat, lon, getString(R.string.point_on_map, lat, lon));
				} else {
					MapActivityActions.navigateToPoint(this, lat, lon, getString(R.string.point_on_map, lat, lon));
				}
				if(dlg != null){
					dlg.dismiss();
				}
			} else {
				// in case when it is dialog
				if(activity != null) {
					OsmandMapTileView v = activity.getMapView();
					v.getAnimatedDraggingThread().startMoving(lat, lon, v.getFloatZoom(), true);
				} else {
					settings.setMapLocationToShow(lat, lon, Math.max(12, settings.getLastKnownMapZoom()), 
							getString(R.string.point_on_map, lat, lon));
				}
				close();
			}
			
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
