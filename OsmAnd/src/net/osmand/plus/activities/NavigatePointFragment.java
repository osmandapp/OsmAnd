package net.osmand.plus.activities;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.StringTokenizer;

import android.content.pm.ActivityInfo;
import android.support.v4.view.MenuItemCompat;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.activities.search.SearchActivity;
import net.osmand.plus.activities.search.SearchActivity.SearchActivityChild;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.dialogs.FavoriteDialogs;
import net.osmand.plus.helpers.ScreenOrientationHelper;
import net.osmand.util.MapUtils;
import android.app.Dialog;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.jwetherell.openmap.common.LatLonPoint;
import com.jwetherell.openmap.common.UTMPoint;

public class NavigatePointFragment extends Fragment implements SearchActivityChild {
	int currentFormat = Location.FORMAT_DEGREES;
	
	public static final String SEARCH_LAT = SearchActivity.SEARCH_LAT;
	public static final String SEARCH_LON = SearchActivity.SEARCH_LON;
	public static final String SEARCH_NORTHING = "NORTHING";
	public static final String SEARCH_EASTING = "EASTING";
	public static final String SEARCH_ZONE = "ZONE";
	private static final int UTM_FORMAT = 3;
	private static final String SELECTION = "SELECTION";
	

	private static final int NAVIGATE_TO = 1;
	private static final int ADD_WAYPOINT = 2;
	private static final int SHOW_ON_MAP = 3;
	private static final int ADD_TO_FAVORITE = 4;

	private View view;
	private LatLon location;

	public View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.navigate_point, container, false);
		setHasOptionsMenu(true);

		location = null;
		OsmandApplication app = (OsmandApplication) getActivity().getApplication();
		Intent intent = getActivity().getIntent();
		if(intent != null){
			double lat = intent.getDoubleExtra(SEARCH_LAT, 0);
			double lon = intent.getDoubleExtra(SEARCH_LON, 0);
			if(lat != 0 || lon != 0){
				location = new LatLon(lat, lon);
			}
		}
		if (location == null && getActivity() instanceof SearchActivity) {
			location = ((SearchActivity) getActivity()).getSearchPoint();
		}
		if (location == null) {
			location = app.getSettings().getLastKnownMapLocation();
		}
		initUI(location.getLatitude(), location.getLongitude());
		if(savedInstanceState != null && savedInstanceState.containsKey(SEARCH_LAT) && savedInstanceState.containsKey(SEARCH_LON) && 
				currentFormat != UTM_FORMAT) {
			String lat = savedInstanceState.getString(SEARCH_LAT);
			String lon = savedInstanceState.getString(SEARCH_LON);
			if(lat != null && lon != null && lat.length() > 0 && lon.length() > 0) {
				((Spinner)view.findViewById(R.id.Format)).setSelection(savedInstanceState.getInt(SELECTION, 0));
				currentFormat = savedInstanceState.getInt(SELECTION, 0);
				((TextView)view.findViewById(R.id.LatitudeEdit)).setText(lat);
				((TextView)view.findViewById(R.id.LongitudeEdit)).setText(lon);
			}
		}
		return view;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if(view != null) {
			final TextView latEdit = ((TextView)view.findViewById(R.id.LatitudeEdit));
			final TextView lonEdit = ((TextView)view.findViewById(R.id.LongitudeEdit));
			outState.putString(SEARCH_LAT, latEdit.getText().toString());
			outState.putString(SEARCH_LON, lonEdit.getText().toString());
			outState.putInt(SELECTION, ((Spinner)view.findViewById(R.id.Format)).getSelectedItemPosition());
		}
	}
	
	
	@Override
	public void onCreateOptionsMenu(Menu onCreate, MenuInflater inflater) {
		OsmandApplication app = (OsmandApplication) getActivity().getApplication();
		int orientation = ScreenOrientationHelper.getScreenOrientation(getActivity());
		boolean portrait = orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ||
				orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
		boolean light = app.getSettings().isLightActionBar();
		Menu menu = onCreate;
		if(getActivity() instanceof SearchActivity) {
			if (portrait) {
				menu = ((SearchActivity) getActivity()).getClearToolbar(true).getMenu();
			} else {
				((SearchActivity) getActivity()).getClearToolbar(false);
			}
			light = false;
		}
		MenuItem menuItem = menu.add(0, NAVIGATE_TO, 0, R.string.context_menu_item_directions_to);
		MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		menuItem = menuItem.setIcon(light ? R.drawable.ic_action_gdirections_light : R.drawable.ic_action_gdirections_dark);
		menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				select(NAVIGATE_TO);
				return true;
			}
		});
		TargetPointsHelper targets = app.getTargetPointsHelper();
		if (targets.getPointToNavigate() != null) {
			menuItem = menu.add(0, ADD_WAYPOINT, 0, R.string.context_menu_item_intermediate_point);
			MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
			menuItem = menuItem.setIcon(light ? R.drawable.ic_action_flage_light
					: R.drawable.ic_action_flage_dark);
		} else {
			menuItem = menu.add(0, ADD_WAYPOINT, 0, R.string.context_menu_item_destination_point);
			MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
			menuItem = menuItem.setIcon(light ? R.drawable.ic_action_flag_light
					: R.drawable.ic_action_flag_dark);
		}
			menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					select(ADD_WAYPOINT);
					return true;
				}
			});
		//}
		menuItem = menu.add(0, SHOW_ON_MAP, 0, R.string.search_shown_on_map);
		MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		menuItem = menuItem.setIcon(light ? R.drawable.ic_action_marker_light : R.drawable.ic_action_marker_dark);

		menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				select(SHOW_ON_MAP);
				return true;
			}
		});
		
		menuItem = menu.add(0, ADD_TO_FAVORITE, 0, R.string.add_to_favourite);
		MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		menuItem = menuItem.setIcon(light ? R.drawable.ic_action_fav_light : R.drawable.ic_action_fav_dark);

		menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				select(ADD_TO_FAVORITE);
				return true;
			}
		});
	}
	
	@Override
	public void onResume() {
		super.onResume();

		//Hardy: onResume() code is needed so that search origin is properly reflected in tab contents when origin has been changed on one tab, then tab is changed to another one.
		location = null;
		OsmandApplication app = (OsmandApplication) getActivity().getApplication();
		//Intent intent = getSherlockActivity().getIntent();
		//if (intent != null) {
		//	if (intent.hasExtra(SearchActivity.SEARCH_LAT) && intent.hasExtra(SearchActivity.SEARCH_LON)) {
		//		double lat = intent.getDoubleExtra(SearchActivity.SEARCH_LAT, 0);
		//		double lon = intent.getDoubleExtra(SearchActivity.SEARCH_LON, 0);
		//		if (lat != 0 || lon != 0) {
		//			location = new LatLon(lat, lon);
		//		}
		//	}
		//}
		if (location == null && getActivity() instanceof SearchActivity) {
			location = ((SearchActivity) getActivity()).getSearchPoint();
		}
		if (location == null) {
			location = app.getSettings().getLastKnownMapLocation();
		}
		locationUpdate(location);
	}
	
	@Override
	public void locationUpdate(LatLon l) {
		//location = l;
		if (view != null) {
			if (l != null) {
				showCurrentFormat(l);
			} else {
				showCurrentFormat(new LatLon(0, 0));
			}
		}
	}
	
	protected void showCurrentFormat(LatLon l) {
		final EditText latEdit = ((EditText)view.findViewById(R.id.LatitudeEdit));
		final EditText lonEdit = ((EditText)view.findViewById(R.id.LongitudeEdit));
		boolean utm = currentFormat == UTM_FORMAT;
		view.findViewById(R.id.easting_row).setVisibility(utm ? View.VISIBLE : View.GONE);
		view.findViewById(R.id.northing_row).setVisibility(utm ? View.VISIBLE : View.GONE);
		view.findViewById(R.id.zone_row).setVisibility(utm ? View.VISIBLE : View.GONE);
		view.findViewById(R.id.lat_row).setVisibility(!utm ? View.VISIBLE : View.GONE);
		view.findViewById(R.id.lon_row).setVisibility(!utm ? View.VISIBLE : View.GONE);
		if(currentFormat == UTM_FORMAT) {
			final EditText northingEdit = ((EditText)view.findViewById(R.id.NorthingEdit));
			final EditText eastingEdit = ((EditText)view.findViewById(R.id.EastingEdit));
			final EditText zoneEdit = ((EditText)view.findViewById(R.id.ZoneEdit));
			UTMPoint pnt = new UTMPoint(new LatLonPoint(l.getLatitude(), l.getLongitude()));
			zoneEdit.setText(pnt.zone_number + ""+pnt.zone_letter);
			northingEdit.setText(((long)pnt.northing)+"");
			eastingEdit.setText(((long)pnt.easting)+"");
		} else {
			latEdit.setText(convert(MapUtils.checkLatitude(l.getLatitude()), currentFormat));
			lonEdit.setText(convert(MapUtils.checkLongitude(l.getLongitude()), currentFormat));
		}
	}

	protected LatLon parseLocation() {
		LatLon loc ;
		if(currentFormat == UTM_FORMAT) { 
			double northing = Double.parseDouble(((EditText)view.findViewById(R.id.NorthingEdit)).getText().toString());
			double easting = Double.parseDouble(((EditText)view.findViewById(R.id.EastingEdit)).getText().toString());
			String zone = ((EditText)view.findViewById(R.id.ZoneEdit)).getText().toString();
			char c = zone.charAt(zone.length() -1);
			int z = Integer.parseInt(zone.substring(0, zone.length() - 1));
			UTMPoint upoint = new UTMPoint(northing, easting, z, c);
			LatLonPoint ll = upoint.toLatLonPoint();
			loc = new LatLon(ll.getLatitude(), ll.getLongitude());
		} else {
			double lat = convert(((EditText) view.findViewById(R.id.LatitudeEdit)).getText().toString());
			double lon = convert(((EditText) view.findViewById(R.id.LongitudeEdit)).getText().toString());
			loc = new LatLon(lat, lon);	
		}
		return loc;
	}
	
	public void initUI(double latitude, double longitude){
		currentFormat = Location.FORMAT_DEGREES;
		showCurrentFormat(new LatLon(latitude, longitude));
		final Spinner format = ((Spinner)view.findViewById(R.id.Format));
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, new String[] {
				getString(R.string.navigate_point_format_D),
				getString(R.string.navigate_point_format_DM),
				getString(R.string.navigate_point_format_DMS),
				"UTM"
		});
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		format.setAdapter(adapter);
		format.setSelection(0);
		format.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
				int newFormat = currentFormat;
				String itm = (String) format.getItemAtPosition(position);
				if(getString(R.string.navigate_point_format_D).equals(itm)){
					newFormat = Location.FORMAT_DEGREES;
				} else if(getString(R.string.navigate_point_format_DM).equals(itm)){
					newFormat = Location.FORMAT_MINUTES;
				} else if(getString(R.string.navigate_point_format_DMS).equals(itm)){
					newFormat = Location.FORMAT_SECONDS;
				} else if (position == UTM_FORMAT) {
					newFormat = UTM_FORMAT;
				}
				try { 
					LatLon loc = parseLocation();
					currentFormat = newFormat;
					view.findViewById(R.id.ValidateTextView).setVisibility(View.INVISIBLE);
					showCurrentFormat(loc);
				} catch (RuntimeException e) {
					view.findViewById(R.id.ValidateTextView).setVisibility(View.VISIBLE);
					((TextView) view.findViewById(R.id.ValidateTextView)).setText(R.string.invalid_locations);
					Log.w(PlatformUtil.TAG, "Convertion failed", e); //$NON-NLS-1$
				}
				
			}

		

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		addPasteListeners();
	}

	protected void addPasteListeners() {
		final EditText latEdit = ((EditText)view.findViewById(R.id.LatitudeEdit));
		final EditText lonEdit = ((EditText)view.findViewById(R.id.LongitudeEdit));
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


	public void select(int mode){
		try {
			LatLon loc = parseLocation();
			double lat = loc.getLatitude();
			double lon = loc.getLongitude();
			if(mode == ADD_TO_FAVORITE) {
				Bundle b = new Bundle();
				Dialog dlg = FavoriteDialogs.createAddFavouriteDialog(getActivity(), b);
				dlg.show();
				FavoriteDialogs.prepareAddFavouriteDialog(getActivity(), dlg, b, lat, lon, getString(R.string.point_on_map, lat, lon));
			} else if (mode == NAVIGATE_TO) {
				DirectionsDialogs.directionsToDialogAndLaunchMap(getActivity(), lat, lon, getString(R.string.point_on_map, lat, lon));
			} else if (mode == ADD_WAYPOINT) {
				DirectionsDialogs.addWaypointDialogAndLaunchMap(getActivity(), lat, lon, getString(R.string.point_on_map, lat, lon));
			} else if (mode == SHOW_ON_MAP){
				OsmandApplication app = (OsmandApplication) getActivity().getApplication();
				app.getSettings().setMapLocationToShow(lat, lon, Math.max(12, app.getSettings().getLastKnownMapZoom()),
						getString(R.string.point_on_map, lat, lon));
				MapActivity.launchMapActivityMoveToTop(getActivity());
			}
			
		} catch (RuntimeException e) {
			view.findViewById(R.id.ValidateTextView).setVisibility(View.VISIBLE);
			((TextView) view.findViewById(R.id.ValidateTextView)).setText(R.string.invalid_locations);
			Log.w(PlatformUtil.TAG, "Convertion failed", e); //$NON-NLS-1$
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
