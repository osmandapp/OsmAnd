package net.osmand.plus.search;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.google.openlocationcode.OpenLocationCode;
import com.jwetherell.openmap.common.LatLonPoint;
import com.jwetherell.openmap.common.MGRSPoint;
import com.jwetherell.openmap.common.UTMPoint;

import net.osmand.AndroidUtils;
import net.osmand.Collator;
import net.osmand.CollatorStringMatcher;
import net.osmand.LocationConvert;
import net.osmand.OsmAndCollator;
import net.osmand.ResultMatcher;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.UpdateLocationViewCache;
import net.osmand.plus.activities.MapActivity;
import net.osmand.search.core.SearchPhrase;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
	private static final String QUICK_SEARCH_COORDS_MGRS_KEY = "quick_search_coords_mgrs_key";
	private static final String QUICK_SEARCH_COORDS_OLC_KEY = "quick_search_coords_olc_key";
	private static final String QUICK_SEARCH_COORDS_OLC_INFO_KEY = "quick_search_coords_olc_info_key";
	private static final String QUICK_SEARCH_COORDS_FORMAT_KEY = "quick_search_coords_format_key";

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
	private EditText mgrsEdit;
	private EditText olcEdit;
	private TextView olcInfo;
	private EditText formatEdit;
	private ProgressBar searchProgressBar;
	private int currentFormat = PointDescription.FORMAT_DEGREES;

	private net.osmand.Location myLocation = null;
	private float heading ;
	private boolean paused;
	private LatLon currentLatLon;
	private UpdateLocationViewCache updateLocationViewCache;
	private boolean isLightTheme;

	private ProcessIndexItemsTask parseOlcCodeTask = null;

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
		isLightTheme = getMyApplication().getSettings().isLightContent();
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
		Drawable icBack = app.getUIUtilities().getIcon(AndroidUtils.getNavigationIconResId(app),
				isLightTheme ? R.color.active_buttons_and_links_text_light : R.color.active_buttons_and_links_text_dark);
		toolbar.setNavigationIcon(icBack);
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		toolbar.setBackgroundColor(ContextCompat.getColor(app, isLightTheme ? R.color.app_bar_color_light : R.color.app_bar_color_dark));
		toolbar.setTitleTextColor(ContextCompat.getColor(app, isLightTheme ? R.color.color_white : R.color.text_color_primary_dark));
		
		updateLocationViewCache = app.getUIUtilities().getUpdateLocationViewCache();
		myLocation = app.getLocationProvider().getLastKnownLocation();
		currentFormat = app.getSettings().COORDINATES_FORMAT.get();

		latEdit = ((EditText) view.findViewById(R.id.latitudeEditText));
		lonEdit = ((EditText) view.findViewById(R.id.longitudeEditText));
		northingEdit = ((EditText) view.findViewById(R.id.northingEditText));
		eastingEdit = ((EditText) view.findViewById(R.id.eastingEditText));
		zoneEdit = ((EditText) view.findViewById(R.id.zoneEditText));
		mgrsEdit = ((EditText) view.findViewById(R.id.mgrsEditText));
		olcEdit = ((EditText) view.findViewById(R.id.olcEditText));
		olcInfo = ((TextView) view.findViewById(R.id.olcInfoTextView));
		formatEdit = ((EditText) view.findViewById(R.id.formatEditText));
		searchProgressBar = ((ProgressBar) view.findViewById(R.id.searchProgressBar));

		String defaultLat = "";
		String defaultZone = "";
		String defaultMgrs = "";
		String defaultOlc = "";
		boolean coordinatesApplied = false;
		if (getArguments() != null) {
			String text = getArguments().getString(QUICK_SEARCH_COORDS_TEXT_KEY);
			if (!Algorithms.isEmpty(text)) {
				if (currentFormat == PointDescription.UTM_FORMAT) {
					defaultZone = text.trim();
				} else if (currentFormat == PointDescription.MGRS_FORMAT) {
					defaultMgrs = text.trim();
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
		String mgrsStr = getStringValue(savedInstanceState, QUICK_SEARCH_COORDS_MGRS_KEY, defaultMgrs);
		String olcStr = getStringValue(savedInstanceState, QUICK_SEARCH_COORDS_OLC_KEY, defaultOlc);
		String olcInfoStr = getStringValue(savedInstanceState, QUICK_SEARCH_COORDS_OLC_INFO_KEY, defaultOlc);

		if (savedInstanceState != null)
			currentFormat = savedInstanceState.getInt(QUICK_SEARCH_COORDS_FORMAT_KEY, -1);
		if (currentFormat == -1)
			currentFormat = getArguments().getInt(QUICK_SEARCH_COORDS_FORMAT_KEY, -1);
		if (currentFormat == -1)
			currentFormat = app.getSettings().COORDINATES_FORMAT.get();

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
			mgrsEdit.setText(mgrsStr);
			mgrsEdit.setSelection(mgrsStr.length());
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
		mgrsEdit.addTextChangedListener(textWatcher);
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
		mgrsEdit.setOnEditorActionListener(doneListener);
		olcEdit.setOnEditorActionListener(doneListener);

		UiUtilities ic = app.getUIUtilities();
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
		ImageButton mgrsClearButton = (ImageButton) view.findViewById(R.id.mgrsClearButton);
		mgrsClearButton.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_remove_dark));
		mgrsClearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mgrsEdit.setText("");
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
			final TextView mgrsEdit = ((TextView) view.findViewById(R.id.mgrsEditText));
			final TextView olcEdit = ((TextView) view.findViewById(R.id.olcEditText));
			final TextView olcInfo = ((TextView) view.findViewById(R.id.olcInfoTextView));
			outState.putString(QUICK_SEARCH_COORDS_LAT_KEY, latEdit.getText().toString());
			outState.putString(QUICK_SEARCH_COORDS_LON_KEY, lonEdit.getText().toString());
			outState.putString(QUICK_SEARCH_COORDS_NORTH_KEY, northEdit.getText().toString());
			outState.putString(QUICK_SEARCH_COORDS_EAST_KEY, eastEdit.getText().toString());
			outState.putString(QUICK_SEARCH_COORDS_ZONE_KEY, zoneEdit.getText().toString());
			outState.putString(QUICK_SEARCH_COORDS_MGRS_KEY, mgrsEdit.getText().toString());
			outState.putString(QUICK_SEARCH_COORDS_OLC_KEY, olcEdit.getText().toString());
			outState.putString(QUICK_SEARCH_COORDS_OLC_INFO_KEY, olcInfo.getText().toString());
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		startLocationUpdate();
		paused = false;
	}

	@Override
	public void onPause() {
		super.onPause();
		paused = true;
		stopSearchAsyncTask();
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
		if (Math.abs(MapUtils.degreesDiff(value, heading)) > 5) {
			heading = value; 
			getMyApplication().runInUIThread(new Runnable() {
				@Override
				public void run() {
					updateLocationUI();
				}
			});
		}
	}

	@Override
	public void updateLocation(final net.osmand.Location location) {
		this.myLocation = location;
		getMyApplication().runInUIThread(new Runnable() {
			@Override
			public void run() {
				updateLocationUI();
			}
		});
	}


	private void updateLocationUI() {
		if (!paused) {
			QuickSearchDialogFragment dialogFragment = (QuickSearchDialogFragment) getParentFragment();
			dialogFragment.getAccessibilityAssistant().lockEvents();
			updateCompassVisibility(coordsView);
			dialogFragment.getAccessibilityAssistant().unlockEvents();
		}
	}

	private void updateControlsVisibility() {
		switch (currentFormat){

			case PointDescription.OLC_FORMAT: {
				view.findViewById(R.id.eastingLayout).setVisibility(View.GONE);
				view.findViewById(R.id.northingLayout).setVisibility(View.GONE);
				view.findViewById(R.id.zoneLayout).setVisibility(View.GONE);
				view.findViewById(R.id.olcLayout).setVisibility(View.VISIBLE);
				view.findViewById(R.id.olcInfoLayout).setVisibility(View.VISIBLE);
				view.findViewById(R.id.latitudeLayout).setVisibility(View.GONE);
				view.findViewById(R.id.longitudeLayout).setVisibility(View.GONE);
				view.findViewById(R.id.mgrsLayout).setVisibility(View.GONE);
				break;
			}
			case PointDescription.UTM_FORMAT: {
				view.findViewById(R.id.eastingLayout).setVisibility(View.VISIBLE);
				view.findViewById(R.id.northingLayout).setVisibility(View.VISIBLE);
				view.findViewById(R.id.zoneLayout).setVisibility(View.VISIBLE);
				view.findViewById(R.id.olcLayout).setVisibility(View.GONE);
				view.findViewById(R.id.olcInfoLayout).setVisibility(View.GONE);
				view.findViewById(R.id.latitudeLayout).setVisibility(View.GONE);
				view.findViewById(R.id.longitudeLayout).setVisibility(View.GONE);
				view.findViewById(R.id.mgrsLayout).setVisibility(View.GONE);
				break;
			}
			case PointDescription.MGRS_FORMAT: {
				view.findViewById(R.id.eastingLayout).setVisibility(View.GONE);
				view.findViewById(R.id.northingLayout).setVisibility(View.GONE);
				view.findViewById(R.id.zoneLayout).setVisibility(View.GONE);
				view.findViewById(R.id.olcLayout).setVisibility(View.GONE);
				view.findViewById(R.id.olcInfoLayout).setVisibility(View.GONE);
				view.findViewById(R.id.latitudeLayout).setVisibility(View.GONE);
				view.findViewById(R.id.longitudeLayout).setVisibility(View.GONE);
				view.findViewById(R.id.mgrsLayout).setVisibility(View.VISIBLE);
				break;
			}
			default: {
				view.findViewById(R.id.eastingLayout).setVisibility(View.GONE);
				view.findViewById(R.id.northingLayout).setVisibility(View.GONE);
				view.findViewById(R.id.zoneLayout).setVisibility(View.GONE);
				view.findViewById(R.id.olcLayout).setVisibility(View.GONE);
				view.findViewById(R.id.olcInfoLayout).setVisibility(View.GONE);
				view.findViewById(R.id.latitudeLayout).setVisibility(View.VISIBLE);
				view.findViewById(R.id.longitudeLayout).setVisibility(View.VISIBLE);
				view.findViewById(R.id.mgrsLayout).setVisibility(View.GONE);
				break;
			}
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
            final LatLon latLon = currentLatLon;
			if (currentFormat == PointDescription.UTM_FORMAT) {
				final EditText northingEdit = ((EditText) view.findViewById(R.id.northingEditText));
				final EditText eastingEdit = ((EditText) view.findViewById(R.id.eastingEditText));
				final EditText zoneEdit = ((EditText) view.findViewById(R.id.zoneEditText));
				if (latLon != null) {
					UTMPoint pnt = new UTMPoint(new LatLonPoint(latLon.getLatitude(), latLon.getLongitude()));
					zoneEdit.setText(pnt.zone_number + "" + pnt.zone_letter);
					northingEdit.setText(((long) pnt.northing) + "");
					eastingEdit.setText(((long) pnt.easting) + "");
				} else if (prevFormat == PointDescription.OLC_FORMAT) {
					zoneEdit.setText(olcEdit.getText());
					northingEdit.setText("");
					eastingEdit.setText("");
				} else if (prevFormat == PointDescription.MGRS_FORMAT) {
					zoneEdit.setText(mgrsEdit.getText());
					northingEdit.setText("");
					eastingEdit.setText("");
				} else {
					zoneEdit.setText(latEdit.getText());
					northingEdit.setText("");
					eastingEdit.setText("");
				}
			} else if (currentFormat == PointDescription.MGRS_FORMAT) {
				final EditText mgrsEdit = ((EditText) view.findViewById(R.id.mgrsEditText));
				if (latLon != null) {
					MGRSPoint pnt = new MGRSPoint(new LatLonPoint(latLon.getLatitude(), latLon.getLongitude()));
					mgrsEdit.setText(pnt.toFlavoredString(5));
				} else if (prevFormat == PointDescription.UTM_FORMAT) {
					mgrsEdit.setText(zoneEdit.getText());
				} else if (prevFormat == PointDescription.OLC_FORMAT) {
					mgrsEdit.setText(olcEdit.getText());
				} else {
					mgrsEdit.setText(latEdit.getText());

				}
			} else if (currentFormat == PointDescription.OLC_FORMAT) {
				if (latLon != null) {
					String olc = OpenLocationCode.encode(latLon.getLatitude(), latLon.getLongitude());
					olcEdit.setText(olc);
					olcInfo.setText(provideOlcInfo(olc));
				} else if (prevFormat == PointDescription.UTM_FORMAT) {
					olcEdit.setText(zoneEdit.getText());
					olcInfo.setText(provideOlcInfo(olcEdit.getText().toString()));
				} else if (prevFormat == PointDescription.MGRS_FORMAT) {
					olcEdit.setText(mgrsEdit.getText());
					olcInfo.setText(provideOlcInfo(olcEdit.getText().toString()));
				} else {
					olcEdit.setText(latEdit.getText());
					olcInfo.setText(provideOlcInfo(olcEdit.getText().toString()));
				}
			} else {
				if (latLon != null) {
					latEdit.setText(LocationConvert.convert(MapUtils.checkLatitude(latLon.getLatitude()), currentFormat));
					lonEdit.setText(LocationConvert.convert(MapUtils.checkLongitude(latLon.getLongitude()), currentFormat));
				} else if (prevFormat == PointDescription.UTM_FORMAT) {
					latEdit.setText(zoneEdit.getText());
					lonEdit.setText("");
				} else if (prevFormat == PointDescription.MGRS_FORMAT) {
					latEdit.setText(mgrsEdit.getText());
					lonEdit.setText("");
				} else if (prevFormat == PointDescription.OLC_FORMAT) {
					latEdit.setText(olcEdit.getText());
					lonEdit.setText("");
				}
			}
			return latLon != null;
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
			} else if (currentFormat == LocationConvert.MGRS_FORMAT) {
				String mgrs = (mgrsEdit.getText().toString());
				MGRSPoint upoint = new MGRSPoint(mgrs);
				LatLonPoint ll = upoint.toLatLonPoint();
				loc = new LatLon(ll.getLatitude(), ll.getLongitude());
			} else if (currentFormat == LocationConvert.OLC_FORMAT) {
				String olcText = olcEdit.getText().toString();
				olcInfo.setText(provideOlcInfo(olcText));
				loc = parseOlcCode(olcText);
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

	private LatLon parseOlcCode(String olcText) {
		LatLon loc = null;
		stopSearchAsyncTask();
		updateProgressBar(false);
		String olcTextCode;
		String cityName = "";
		String[] olcTextParts = olcText.split(" ");
		if (olcTextParts.length > 1) {
			olcTextCode = olcTextParts[0];
			cityName = olcTextParts[1];
		} else {
			olcTextCode = olcText;
		}
		OpenLocationCode.CodeArea codeArea = null;
		if (OpenLocationCode.isFullCode(olcTextCode)) {
			codeArea = OpenLocationCode.decode(olcTextCode);
		} else if (OpenLocationCode.isShortCode(olcTextCode)) {
			OpenLocationCode code = new OpenLocationCode(olcTextCode);
			LatLon mapLocation = getMapActivity().getMapLocation();
			if (cityName.isEmpty()) {
				if (mapLocation != null) {
					OpenLocationCode newCode = code.recover(mapLocation.getLatitude(), mapLocation.getLongitude());
					codeArea = code.recover(mapLocation.getLatitude(), mapLocation.getLongitude()).decode();
					olcInfo.setText(provideOlcInfo(newCode.getCode()));
				}
			} else {
				parseOlcCodeTask = new ProcessIndexItemsTask(this, cityName, olcTextCode, mapLocation);
				parseOlcCodeTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		}
		if (codeArea != null) {
			loc = new LatLon(codeArea.getCenterLatitude(), codeArea.getCenterLongitude());
		}

		return loc;
	}

	public void stopSearchAsyncTask() {
		if (parseOlcCodeTask != null && parseOlcCodeTask.getStatus() == AsyncTask.Status.RUNNING) {
			parseOlcCodeTask.cancel(true);
		}
	}

	private static class ProcessIndexItemsTask extends AsyncTask<Void, Void, List<Amenity>> {

		private OsmandApplication app;
		private WeakReference<QuickSearchCoordinatesFragment> weakFragment;
		private final List<String> citySubTypes = Arrays.asList("city", "town", "village");

		private final LatLon searchLocation;
		private final String region;
		private final String olcText;
		private final int searchCityLimit = 100;


		ProcessIndexItemsTask(QuickSearchCoordinatesFragment fragment, String region, String olcText, LatLon searchLocation) {
			app = fragment.getMyApplication();
			weakFragment = new WeakReference<>(fragment);
			this.region = region;
			this.olcText = olcText;
			this.searchLocation = searchLocation;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			QuickSearchCoordinatesFragment fragment = weakFragment.get();
			if (fragment != null && fragment.isResumed()) {
				fragment.updateProgressBar(true);
			}
		}

		@Override
		protected List<Amenity> doInBackground(Void... voids) {
			List<Amenity> results = new ArrayList<>(searchCities(app, region));
			sortCities(results);
			return results;
		}

		@Override
		protected void onPostExecute(List<Amenity> regions) {
			if (isCancelled()) {
				return;
			}
			QuickSearchCoordinatesFragment fragment = weakFragment.get();
			if (fragment != null && fragment.isResumed()) {
				fragment.updateProgressBar(false);
				if (!regions.isEmpty() && OpenLocationCode.isValidCode(olcText)) {
					LatLon latLon = regions.get(0).getLocation();
					OpenLocationCode code = new OpenLocationCode(olcText);
					OpenLocationCode newCode = code.recover(latLon.getLatitude(), latLon.getLongitude());
					OpenLocationCode.CodeArea codeArea = newCode.decode();
					fragment.updateCurrentLocation(new LatLon(codeArea.getCenterLatitude(), codeArea.getCenterLongitude()), newCode.getCode());
				}
			}
		}

		private List<Amenity> searchCities(final OsmandApplication app, final String text) {
			final SearchPhrase.NameStringMatcher nm = new SearchPhrase.NameStringMatcher(
					text, CollatorStringMatcher.StringMatcherMode.CHECK_STARTS_FROM_SPACE);
			final String lang = app.getSettings().MAP_PREFERRED_LOCALE.get();
			final boolean transliterate = app.getSettings().MAP_TRANSLITERATE_NAMES.get();
			final List<Amenity> amenities = new ArrayList<>();
			double lat = 0;
			double lon = 0;
			if (searchLocation != null) {
				lat = searchLocation.getLatitude();
				lon = searchLocation.getLongitude();
			}
			app.getResourceManager().searchAmenitiesByName(region, MapUtils.MAX_LATITUDE,
					MapUtils.MIN_LONGITUDE, MapUtils.MIN_LATITUDE, MapUtils.MAX_LONGITUDE, lat, lon, new ResultMatcher<Amenity>() {
						int count = 0;

						@Override
						public boolean publish(Amenity amenity) {
							if (count++ > searchCityLimit) {
								return false;
							}
							List<String> otherNames = amenity.getOtherNames(true);
							String localeName = amenity.getName(lang, transliterate);
							String subType = amenity.getSubType();
							if (!citySubTypes.contains(subType)
									|| (!nm.matches(localeName) && !nm.matches(otherNames))) {
								return false;
							}
							amenities.add(amenity);
							return false;
						}

						@Override
						public boolean isCancelled() {
							return count > searchCityLimit || ProcessIndexItemsTask.this.isCancelled();
						}
					});

			return amenities;
		}

		private void sortCities(List<Amenity> cities) {
			final Collator collator = OsmAndCollator.primaryCollator();
			Collections.sort(cities, new Comparator<Object>() {
				@Override
				public int compare(Object obj1, Object obj2) {
					String str1;
					String str2;
					Amenity a = ((Amenity) obj1);
					if ("city".equals(a.getSubType())) {
						str1 = "!" + ((Amenity) obj1).getName();
					} else {
						str1 = ((Amenity) obj1).getName();
					}
					Amenity b = ((Amenity) obj2);
					if ("city".equals(b.getSubType())) {
						str2 = "!" + ((Amenity) obj2).getName();
					} else {
						str2 = ((Amenity) obj2).getName();
					}
					return collator.compare(str1, str2);
				}
			});
		}
	}

	private void updateProgressBar(boolean visible) {
		searchProgressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
	}

	private void updateCurrentLocation(final LatLon latLon, String olcText) {
		currentLatLon = latLon;
		updateLocationCell(currentLatLon);
		olcInfo.setText(provideOlcInfo(olcText));
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
			}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, latLon);
			updateLocationUI();
			errorView.setVisibility(View.GONE);
			coordsView.setVisibility(View.VISIBLE);
		}
	}

	private void updateCompassVisibility(View view) {
		View compassView = view.findViewById(R.id.compass_layout);
		boolean showCompass = currentLatLon != null;
		if (showCompass) {
			updateDistanceDirection(view, currentLatLon);
			compassView.setVisibility(View.VISIBLE);
		} else {
			if (!showCompass) {
				compassView.setVisibility(View.GONE);
			} else {
				compassView.setVisibility(View.INVISIBLE);
			}
		}
	}

	private void updateDistanceDirection(View view, LatLon location) {
		TextView distanceText = (TextView) view.findViewById(R.id.distance);
		ImageView direction = (ImageView) view.findViewById(R.id.direction);
		getMyApplication().getUIUtilities().updateLocationView(updateLocationViewCache, direction, distanceText, currentLatLon);
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
			String[] entries = new String[6];
			entries[0] = PointDescription.formatToHumanString(getContext(), PointDescription.FORMAT_DEGREES);
			entries[1] = PointDescription.formatToHumanString(getContext(), PointDescription.FORMAT_MINUTES);
			entries[2] = PointDescription.formatToHumanString(getContext(), PointDescription.FORMAT_SECONDS);
			entries[3] = PointDescription.formatToHumanString(getContext(), PointDescription.UTM_FORMAT);
			entries[4] = PointDescription.formatToHumanString(getContext(), PointDescription.OLC_FORMAT);
			entries[5] = PointDescription.formatToHumanString(getContext(), PointDescription.MGRS_FORMAT);

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
