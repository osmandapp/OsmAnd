package net.osmand.plus.search;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.google.openlocationcode.OpenLocationCode;
import com.jwetherell.openmap.common.LatLonPoint;
import com.jwetherell.openmap.common.UTMPoint;

import net.osmand.Location;
import net.osmand.LocationConvert;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import static android.text.InputType.TYPE_CLASS_PHONE;
import static android.text.InputType.TYPE_CLASS_TEXT;
import static android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS;
import static android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
import static android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;

public class QuickSearchCoordinatesFragment extends DialogFragment implements OsmAndCompassListener, OsmAndLocationListener {

	public static final String TAG = "QuickSearchCoordinatesFragment";
	private static final String QUICK_SEARCH_COORDS_LAT_KEY = "quick_search_coords_lat_key";
	private static final String QUICK_SEARCH_COORDS_LON_KEY = "quick_search_coords_lon_key";
	private static final String QUICK_SEARCH_COORDS_NORTH_KEY = "quick_search_coords_north_key";
	private static final String QUICK_SEARCH_COORDS_EAST_KEY = "quick_search_coords_east_key";
	private static final String QUICK_SEARCH_COORDS_ZONE_KEY = "quick_search_coords_zone_key";
	private static final String QUICK_SEARCH_COORDS_OLC_KEY = "quick_search_coords_olc_key";
	private static final String QUICK_SEARCH_COORDS_OLC_INFO_KEY = "quick_search_coords_olc_info_key";
	private static final String QUICK_SEARCH_COORDS_FORMAT_KEY = "quick_search_coords_format_key";
	private static final String QUICK_SEARCH_COORDS_USE_MAP_CENTER_KEY = "quick_search_coords_use_map_center_key";

	private static final String QUICK_SEARCH_COORDS_TEXT_KEY = "quick_search_coords_text_key";
	private static final String QUICK_SEARCH_COORDS_LATITUDE_KEY = "quick_search_coords_latitude_key";
	private static final String QUICK_SEARCH_COORDS_LONGITUDE_KEY = "quick_search_coords_longitude_key";

	private View view;
	private View coordsView;
	private View errorView;
	private EditText latEdit;
	private EditText lonEdit;
	private EditText northingEdit;
	private EditText eastingEdit;
	private EditText zoneEdit;
	private EditText olcEdit;
	private TextView olcInfo;
	private EditText formatEdit;
	private int currentFormat = PointDescription.FORMAT_DEGREES;

	private net.osmand.Location myLocation = null;
	private Float heading = null;
	private boolean useMapCenter;
	private boolean paused;
	private int screenOrientation;
	private LatLon currentLatLon;

	public QuickSearchCoordinatesFragment() {
	}

	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		boolean isLightTheme = getMyApplication().getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;
		int themeId = isLightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);
	}

	@Override
	@SuppressLint("PrivateResource")
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		final OsmandApplication app = getMyApplication();
		view = inflater.inflate(R.layout.search_advanced_coords, container, false);

		Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(app.getIconsCache().getIcon(R.drawable.ic_arrow_back));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		screenOrientation = DashLocationFragment.getScreenOrientation(getActivity());
		myLocation = app.getLocationProvider().getLastKnownLocation();
		currentFormat = app.getSettings().COORDINATES_FORMAT.get();

		latEdit = ((EditText) view.findViewById(R.id.latitudeEditText));
		lonEdit = ((EditText) view.findViewById(R.id.longitudeEditText));
		northingEdit = ((EditText) view.findViewById(R.id.northingEditText));
		eastingEdit = ((EditText) view.findViewById(R.id.eastingEditText));
		zoneEdit = ((EditText) view.findViewById(R.id.zoneEditText));
		olcEdit = ((EditText) view.findViewById(R.id.olcEditText));
		olcInfo = ((TextView) view.findViewById(R.id.olcInfoTextView));
		formatEdit = ((EditText) view.findViewById(R.id.formatEditText));

		String defaultLat = "";
		String defaultZone = "";
		String defaultOlc = "";
		boolean coordinatesApplied = false;
		if (getArguments() != null) {
			String text = getArguments().getString(QUICK_SEARCH_COORDS_TEXT_KEY);
			if (!Algorithms.isEmpty(text)) {
				if (currentFormat == PointDescription.UTM_FORMAT) {
					defaultZone = text.trim();
				} else if (currentFormat == PointDescription.OLC_FORMAT) {
					defaultOlc = text.trim();
				} else {
					defaultLat = text.trim();
				}
			} else {
				double latitude = getArguments().getDouble(QUICK_SEARCH_COORDS_LATITUDE_KEY, Double.NaN);
				double longitude = getArguments().getDouble(QUICK_SEARCH_COORDS_LONGITUDE_KEY, Double.NaN);
				if (!Double.isNaN(latitude) && !Double.isNaN(longitude)) {
					currentLatLon = new LatLon(latitude, longitude);
					applyFormat(currentFormat, true);
					coordinatesApplied = true;
				}
			}
		}

		String latStr = getStringValue(savedInstanceState, QUICK_SEARCH_COORDS_LAT_KEY, defaultLat);
		String lonStr = getStringValue(savedInstanceState, QUICK_SEARCH_COORDS_LON_KEY, "");
		String northingStr = getStringValue(savedInstanceState, QUICK_SEARCH_COORDS_NORTH_KEY, "");
		String eastingStr = getStringValue(savedInstanceState, QUICK_SEARCH_COORDS_EAST_KEY, "");
		String zoneStr = getStringValue(savedInstanceState, QUICK_SEARCH_COORDS_ZONE_KEY, defaultZone);
		String olcStr = getStringValue(savedInstanceState, QUICK_SEARCH_COORDS_OLC_KEY, defaultOlc);
		String olcInfoStr = getStringValue(savedInstanceState, QUICK_SEARCH_COORDS_OLC_INFO_KEY, defaultOlc);

		if (savedInstanceState != null)
			currentFormat = savedInstanceState.getInt(QUICK_SEARCH_COORDS_FORMAT_KEY, -1);
		if (currentFormat == -1)
			currentFormat = getArguments().getInt(QUICK_SEARCH_COORDS_FORMAT_KEY, -1);
		if (currentFormat == -1)
			currentFormat = app.getSettings().COORDINATES_FORMAT.get();

		if (savedInstanceState != null && savedInstanceState.containsKey(QUICK_SEARCH_COORDS_USE_MAP_CENTER_KEY))
			useMapCenter = savedInstanceState.getBoolean(QUICK_SEARCH_COORDS_USE_MAP_CENTER_KEY);
		else if (getArguments().containsKey(QUICK_SEARCH_COORDS_USE_MAP_CENTER_KEY))
			useMapCenter = getArguments().getBoolean(QUICK_SEARCH_COORDS_USE_MAP_CENTER_KEY);

		if (!coordinatesApplied) {
			latEdit.setText(latStr);
			latEdit.setSelection(latStr.length());
			lonEdit.setText(lonStr);
			lonEdit.setSelection(lonStr.length());
			northingEdit.setText(northingStr);
			northingEdit.setSelection(northingStr.length());
			eastingEdit.setText(eastingStr);
			eastingEdit.setSelection(eastingStr.length());
			zoneEdit.setText(zoneStr);
			zoneEdit.setSelection(zoneStr.length());
			olcEdit.setText(olcStr);
			olcEdit.setSelection(olcStr.length());
			olcInfo.setText(olcInfoStr);
		}

		formatEdit.setText(PointDescription.formatToHumanString(app, currentFormat));
		formatEdit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new ChooseCoordsFormatDialogFragment().show(getChildFragmentManager(), "ChooseCoordinatesFormatFragment");
			}
		});

		TextWatcher textWatcher = new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				parseLocation();
			}
		};
		latEdit.addTextChangedListener(textWatcher);
		lonEdit.addTextChangedListener(textWatcher);
		northingEdit.addTextChangedListener(textWatcher);
		eastingEdit.addTextChangedListener(textWatcher);
		zoneEdit.addTextChangedListener(textWatcher);
		olcEdit.addTextChangedListener(textWatcher);

		OnEditorActionListener doneListener = new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					showOnMap();
					return true;
				} else {
					return false;
				}
			}
		};

		lonEdit.setOnEditorActionListener(doneListener);
		zoneEdit.setOnEditorActionListener(doneListener);
		olcEdit.setOnEditorActionListener(doneListener);

		IconsCache ic = app.getIconsCache();
		((ImageView) view.findViewById(R.id.latitudeImage))
				.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_coordinates_latitude));
		((ImageView) view.findViewById(R.id.longitudeImage))
				.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_coordinates_longitude));
		((ImageView) view.findViewById(R.id.northingImage))
				.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_coordinates_latitude));
		((ImageView) view.findViewById(R.id.eastingImage))
				.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_coordinates_longitude));

		ImageButton latitudeClearButton = (ImageButton) view.findViewById(R.id.latitudeClearButton);
		latitudeClearButton.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_remove_dark));
		latitudeClearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				latEdit.setText("");
			}
		});
		ImageButton longitudeClearButton = (ImageButton) view.findViewById(R.id.longitudeClearButton);
		longitudeClearButton.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_remove_dark));
		longitudeClearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				lonEdit.setText("");
			}
		});
		ImageButton northingClearButton = (ImageButton) view.findViewById(R.id.northingClearButton);
		northingClearButton.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_remove_dark));
		northingClearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				northingEdit.setText("");
			}
		});
		ImageButton eastingClearButton = (ImageButton) view.findViewById(R.id.eastingClearButton);
		eastingClearButton.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_remove_dark));
		eastingClearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				eastingEdit.setText("");
			}
		});
		ImageButton zoneClearButton = (ImageButton) view.findViewById(R.id.zoneClearButton);
		zoneClearButton.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_remove_dark));
		zoneClearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				olcEdit.setText("");
			}
		});
		ImageButton olcClearButton = (ImageButton) view.findViewById(R.id.olcClearButton);
		olcClearButton.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_remove_dark));
		olcClearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				olcEdit.setText("");
			}
		});
		ImageButton formatSelectButton = (ImageButton) view.findViewById(R.id.formatSelectButton);
		formatSelectButton.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_arrow_drop_down));
		formatSelectButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new ChooseCoordsFormatDialogFragment().show(getChildFragmentManager(), "ChooseCoordinatesFormatFragment");
			}
		});

		coordsView = view.findViewById(R.id.searchListItemLayout);
		view.findViewById(R.id.divider).setVisibility(View.GONE);
		TypedValue outValue = new TypedValue();
		app.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
		coordsView.setBackgroundResource(outValue.resourceId);
		coordsView.setClickable(true);
		coordsView.findViewById(R.id.toggle_item).setVisibility(View.GONE);
		coordsView.findViewById(R.id.time_icon).setVisibility(View.GONE);
		coordsView.findViewById(R.id.time).setVisibility(View.GONE);
		coordsView.findViewById(R.id.type_name_icon).setVisibility(View.GONE);
		((ImageView) coordsView.findViewById(R.id.imageView)).setImageDrawable(
				ic.getIcon(R.drawable.ic_action_world_globe, app.getSettings().isLightContent()
						? R.color.osmand_orange : R.color.osmand_orange_dark));
		coordsView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showOnMap();
			}
		});

		errorView = view.findViewById(R.id.error_item);

		parseLocation();
		updateControlsVisibility();

		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (view != null) {
			final TextView latEdit = ((TextView) view.findViewById(R.id.latitudeEditText));
			final TextView lonEdit = ((TextView) view.findViewById(R.id.longitudeEditText));
			final TextView northEdit = ((TextView) view.findViewById(R.id.northingEditText));
			final TextView eastEdit = ((TextView) view.findViewById(R.id.eastingEditText));
			final TextView zoneEdit = ((TextView) view.findViewById(R.id.zoneEditText));
			final TextView olcEdit = ((TextView) view.findViewById(R.id.olcEditText));
			final TextView olcInfo = ((TextView) view.findViewById(R.id.olcInfoTextView));
			outState.putString(QUICK_SEARCH_COORDS_LAT_KEY, latEdit.getText().toString());
			outState.putString(QUICK_SEARCH_COORDS_LON_KEY, lonEdit.getText().toString());
			outState.putString(QUICK_SEARCH_COORDS_NORTH_KEY, northEdit.getText().toString());
			outState.putString(QUICK_SEARCH_COORDS_EAST_KEY, eastEdit.getText().toString());
			outState.putString(QUICK_SEARCH_COORDS_ZONE_KEY, zoneEdit.getText().toString());
			outState.putString(QUICK_SEARCH_COORDS_OLC_KEY, olcEdit.getText().toString());
			outState.putString(QUICK_SEARCH_COORDS_OLC_INFO_KEY, olcInfo.getText().toString());
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!useMapCenter) {
			startLocationUpdate();
		}
		paused = false;
	}

	@Override
	public void onPause() {
		super.onPause();
		paused = true;
		stopLocationUpdate();
	}

	private String getStringValue(Bundle savedInstanceState, String key, String defaultValue) {
		String res = null;
		if (savedInstanceState != null)
			res = savedInstanceState.getString(key);
		if (res == null)
			res = defaultValue;
		return res;
	}

	private void startLocationUpdate() {
		OsmandApplication app = getMyApplication();
		app.getLocationProvider().removeCompassListener(app.getLocationProvider().getNavigationInfo());
		app.getLocationProvider().addCompassListener(this);
		app.getLocationProvider().addLocationListener(this);
		myLocation = app.getLocationProvider().getLastKnownLocation();
		updateLocation(myLocation);
	}

	private void stopLocationUpdate() {
		OsmandApplication app = getMyApplication();
		app.getLocationProvider().removeLocationListener(this);
		app.getLocationProvider().removeCompassListener(this);
		app.getLocationProvider().addCompassListener(app.getLocationProvider().getNavigationInfo());
	}

	private void showOnMap() {
		if (currentLatLon != null) {
			QuickSearchDialogFragment dialogFragment = (QuickSearchDialogFragment) getParentFragment();
			dialogFragment.hideToolbar();
			dialogFragment.hide();

			PointDescription pointDescription =
					new PointDescription(currentLatLon.getLatitude(), currentLatLon.getLongitude());

			QuickSearchListFragment.showOnMap(getMapActivity(), dialogFragment,
					currentLatLon.getLatitude(), currentLatLon.getLongitude(),
					15, pointDescription, currentLatLon);

			dismiss();
		}
	}

	@Override
	public void updateCompassValue(final float value) {
		// 99 in next line used to one-time initialize arrows (with reference vs. fixed-north direction)
		// on non-compass devices
		float lastHeading = heading != null ? heading : 99;
		heading = value;
		if (Math.abs(MapUtils.degreesDiff(lastHeading, heading)) > 5) {
			final net.osmand.Location location = this.myLocation;
			getMyApplication().runInUIThread(new Runnable() {
				@Override
				public void run() {
					updateLocationUI(location, value);
				}
			});
		} else {
			heading = lastHeading;
		}
	}

	@Override
	public void updateLocation(final net.osmand.Location location) {
		this.myLocation = location;
		final Float heading = this.heading;
		getMyApplication().runInUIThread(new Runnable() {
			@Override
			public void run() {
				updateLocationUI(location, heading);
			}
		});
	}

	private void updateLocationUI(net.osmand.Location location, Float heading) {
		LatLon latLon = null;
		if (location != null) {
			latLon = new LatLon(location.getLatitude(), location.getLongitude());
			((QuickSearchDialogFragment)getParentFragment()).getNavigationInfo().updateLocation(location);
		}
		updateLocationUI(latLon, heading);
	}

	private void updateLocationUI(LatLon latLon, Float heading) {
		if (latLon != null && !paused) {
			QuickSearchDialogFragment dialogFragment = (QuickSearchDialogFragment) getParentFragment();
			dialogFragment.getAccessibilityAssistant().lockEvents();
			updateCompassVisibility(coordsView, latLon, heading);
			dialogFragment.getAccessibilityAssistant().unlockEvents();
			if(heading != null) {
				dialogFragment.getNavigationInfo().updateTargetDirection(currentLatLon, heading.floatValue());
			}
		}
	}

	private void updateControlsVisibility() {
		if (currentFormat == PointDescription.OLC_FORMAT) {
			view.findViewById(R.id.eastingLayout).setVisibility(View.GONE);
			view.findViewById(R.id.northingLayout).setVisibility(View.GONE);
			view.findViewById(R.id.zoneLayout).setVisibility(View.GONE);
			view.findViewById(R.id.olcLayout).setVisibility(View.VISIBLE);
			view.findViewById(R.id.olcInfoLayout).setVisibility(View.VISIBLE);
			view.findViewById(R.id.latitudeLayout).setVisibility(View.GONE);
			view.findViewById(R.id.longitudeLayout).setVisibility(View.GONE);
		} else {
			boolean utm = currentFormat == PointDescription.UTM_FORMAT;
			view.findViewById(R.id.eastingLayout).setVisibility(utm ? View.VISIBLE : View.GONE);
			view.findViewById(R.id.northingLayout).setVisibility(utm ? View.VISIBLE : View.GONE);
			view.findViewById(R.id.zoneLayout).setVisibility(utm ? View.VISIBLE : View.GONE);
			view.findViewById(R.id.olcLayout).setVisibility(View.GONE);
			view.findViewById(R.id.olcInfoLayout).setVisibility(View.GONE);
			view.findViewById(R.id.latitudeLayout).setVisibility(!utm ? View.VISIBLE : View.GONE);
			view.findViewById(R.id.longitudeLayout).setVisibility(!utm ? View.VISIBLE : View.GONE);
		}
	}

	private String provideOlcInfo(String olcString) {
		try {
			if (!OpenLocationCode.isValidCode(olcString))
				return getContext().getString(R.string.navigate_point_olc_info_invalid);
			OpenLocationCode olc = new OpenLocationCode(olcString);
			if (olc.isShort())
				return getContext().getString(R.string.navigate_point_olc_info_short);
			OpenLocationCode.CodeArea area = olc.decode();
			int areaWidth = (int)Math.ceil(MapUtils.getDistance(area.getNorthLatitude(), area.getWestLongitude(),
					area.getNorthLatitude(), area.getEastLongitude()));
			int areaHeight = (int)Math.ceil(MapUtils.getDistance(area.getNorthLatitude(), area.getWestLongitude(),
					area.getSouthLatitude(), area.getWestLongitude()));
			return getContext().getString(R.string.navigate_point_olc_info_area,
					OsmAndFormatter.getFormattedDistance(areaWidth, getMyApplication()),
					OsmAndFormatter.getFormattedDistance(areaHeight, getMyApplication()));
		} catch (IllegalArgumentException iae) {
			return getContext().getString(R.string.navigate_point_olc_info_invalid);
		}
	}

	private void setInputTypeDependingOnFormat(EditText[] editTexts) {
		for (EditText et : editTexts) {
			if (currentFormat == PointDescription.FORMAT_DEGREES) {
				et.setInputType(TYPE_CLASS_PHONE);
			} else {
				et.setInputType(TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_VISIBLE_PASSWORD |
						TYPE_TEXT_FLAG_CAP_CHARACTERS | TYPE_TEXT_FLAG_NO_SUGGESTIONS);
			}
		}
	}

	private boolean applyFormat(int format, boolean forceApply) {
		if (currentFormat != format || forceApply) {
			int prevFormat = currentFormat;
			currentFormat = format;
			formatEdit.setText(PointDescription.formatToHumanString(getMyApplication(), currentFormat));
			final EditText latEdit = ((EditText) view.findViewById(R.id.latitudeEditText));
			final EditText lonEdit = ((EditText) view.findViewById(R.id.longitudeEditText));
            setInputTypeDependingOnFormat(new EditText[]{latEdit, lonEdit});
            updateControlsVisibility();
			if (currentFormat == PointDescription.UTM_FORMAT) {
				final EditText northingEdit = ((EditText) view.findViewById(R.id.northingEditText));
				final EditText eastingEdit = ((EditText) view.findViewById(R.id.eastingEditText));
				final EditText zoneEdit = ((EditText) view.findViewById(R.id.zoneEditText));
				if (currentLatLon != null) {
					UTMPoint pnt = new UTMPoint(new LatLonPoint(currentLatLon.getLatitude(), currentLatLon.getLongitude()));
					zoneEdit.setText(pnt.zone_number + "" + pnt.zone_letter);
					northingEdit.setText(((long) pnt.northing) + "");
					eastingEdit.setText(((long) pnt.easting) + "");
				} else if (prevFormat == PointDescription.OLC_FORMAT) {
					zoneEdit.setText(olcEdit.getText());
					northingEdit.setText("");
					eastingEdit.setText("");
				} else {
					zoneEdit.setText(latEdit.getText());
					northingEdit.setText("");
					eastingEdit.setText("");
				}
			} else if (currentFormat == PointDescription.OLC_FORMAT) {
				if (currentLatLon != null) {
					String olc = OpenLocationCode.encode(currentLatLon.getLatitude(), currentLatLon.getLongitude());
					olcEdit.setText(olc);
					olcInfo.setText(provideOlcInfo(olc));
				} else if (prevFormat == PointDescription.UTM_FORMAT) {
					olcEdit.setText(zoneEdit.getText());
					olcInfo.setText(provideOlcInfo(olcEdit.getText().toString()));
				} else {
					olcEdit.setText(latEdit.getText());
					olcInfo.setText(provideOlcInfo(olcEdit.getText().toString()));
				}
			} else {
				if (currentLatLon != null) {
					latEdit.setText(LocationConvert.convert(MapUtils.checkLatitude(currentLatLon.getLatitude()), currentFormat));
					lonEdit.setText(LocationConvert.convert(MapUtils.checkLongitude(currentLatLon.getLongitude()), currentFormat));
				} else if (prevFormat == PointDescription.UTM_FORMAT) {
					latEdit.setText(zoneEdit.getText());
					lonEdit.setText("");
				} else if (prevFormat == PointDescription.OLC_FORMAT) {
					latEdit.setText(olcEdit.getText());
					lonEdit.setText("");
				}
			}
			return currentLatLon != null;
		} else {
			return false;
		}
	}

	private void parseLocation() {
		LatLon loc;
		try {
			if (currentFormat == LocationConvert.UTM_FORMAT) {
				double northing = Double.parseDouble(northingEdit.getText().toString());
				double easting = Double.parseDouble(eastingEdit.getText().toString());
				String zone = zoneEdit.getText().toString();
				char c = zone.charAt(zone.length() - 1);
				int z = Integer.parseInt(zone.substring(0, zone.length() - 1));
				UTMPoint upoint = new UTMPoint(northing, easting, z, c);
				LatLonPoint ll = upoint.toLatLonPoint();
				loc = new LatLon(ll.getLatitude(), ll.getLongitude());
			} else if (currentFormat == LocationConvert.OLC_FORMAT) {
				String olcText = olcEdit.getText().toString();
				olcInfo.setText(provideOlcInfo(olcText));
				// can throw exception for invalid OLC string
				OpenLocationCode.CodeArea codeArea = OpenLocationCode.decode(olcText);
				loc = new LatLon(codeArea.getCenterLatitude(), codeArea.getCenterLongitude());
			} else {
				double lat = LocationConvert.convert(latEdit.getText().toString(), true);
				double lon = LocationConvert.convert(lonEdit.getText().toString(), true);
				loc = new LatLon(lat, lon);
			}
			currentLatLon = loc;
		} catch (Exception e) {
			currentLatLon = null;
		}
		updateLocationCell(currentLatLon);
	}

	private void updateLocationCell(final LatLon latLon) {
		final OsmandApplication app = getMyApplication();
		if (latLon == null) {
			coordsView.setVisibility(View.GONE);
			errorView.setVisibility(View.VISIBLE);
		} else {
			final TextView titleView = (TextView) coordsView.findViewById(R.id.title);
			final TextView subtitleView = (TextView) coordsView.findViewById(R.id.subtitle);
			titleView.setText(PointDescription.getLocationNamePlain(app, latLon.getLatitude(), latLon.getLongitude()));
			new AsyncTask<LatLon, Void, String>() {
				@Override
				protected String doInBackground(LatLon... params) {
					return app.getRegions().getCountryName(latLon);
				}

				@Override
				protected void onPostExecute(String country) {
					if (!paused) {
						subtitleView.setText(country == null ? "" : country);
					}
				}
			}.execute(latLon);
			updateLocationUI(latLon, heading);
			errorView.setVisibility(View.GONE);
			coordsView.setVisibility(View.VISIBLE);
		}
	}

	private void updateCompassVisibility(View view, LatLon location, Float heading) {
		View compassView = view.findViewById(R.id.compass_layout);
		Location ll = getMyApplication().getLocationProvider().getLastKnownLocation();
		boolean showCompass = currentLatLon != null && location != null;
		if (ll != null && showCompass) {
			updateDistanceDirection(view, location, currentLatLon, heading);
			compassView.setVisibility(View.VISIBLE);
		} else {
			if (!showCompass) {
				compassView.setVisibility(View.GONE);
			} else {
				compassView.setVisibility(View.INVISIBLE);
			}
		}
	}

	private void updateDistanceDirection(View view, LatLon myLatLon, LatLon location, Float heading) {
		TextView distanceText = (TextView) view.findViewById(R.id.distance);
		ImageView direction = (ImageView) view.findViewById(R.id.direction);

		DashLocationFragment.updateLocationView(useMapCenter, myLatLon,
				heading, direction, distanceText,
				location.getLatitude(),
				location.getLongitude(),
				screenOrientation, getMyApplication(), getMapActivity());
	}

	public static void showDialog(DialogFragment parentFragment, String text) {
		Bundle bundle = new Bundle();
		bundle.putString(QUICK_SEARCH_COORDS_TEXT_KEY, text);
		QuickSearchCoordinatesFragment fragment = new QuickSearchCoordinatesFragment();
		fragment.setArguments(bundle);
		fragment.show(parentFragment.getChildFragmentManager(), TAG);
	}

	public static void showDialog(DialogFragment parentFragment, double latitude, double longitude) {
		Bundle bundle = new Bundle();
		bundle.putDouble(QUICK_SEARCH_COORDS_LATITUDE_KEY, latitude);
		bundle.putDouble(QUICK_SEARCH_COORDS_LONGITUDE_KEY, longitude);
		QuickSearchCoordinatesFragment fragment = new QuickSearchCoordinatesFragment();
		fragment.setArguments(bundle);
		fragment.show(parentFragment.getChildFragmentManager(), TAG);
	}

	public static class ChooseCoordsFormatDialogFragment extends DialogFragment {
		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			final QuickSearchCoordinatesFragment parent = (QuickSearchCoordinatesFragment) getParentFragment();
			String[] entries = new String[5];
			entries[0] = PointDescription.formatToHumanString(getContext(), PointDescription.FORMAT_DEGREES);
			entries[1] = PointDescription.formatToHumanString(getContext(), PointDescription.FORMAT_MINUTES);
			entries[2] = PointDescription.formatToHumanString(getContext(), PointDescription.FORMAT_SECONDS);
			entries[3] = PointDescription.formatToHumanString(getContext(), PointDescription.UTM_FORMAT);
			entries[4] = PointDescription.formatToHumanString(getContext(), PointDescription.OLC_FORMAT);

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.coords_format)
					.setSingleChoiceItems(entries, parent.currentFormat, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							parent.applyFormat(which, false);
							dialog.dismiss();
						}
					});
			return builder.create();
		}
	}
}
