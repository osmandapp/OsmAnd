package net.osmand.plus.search.dialogs;

import static android.text.InputType.TYPE_CLASS_PHONE;
import static android.text.InputType.TYPE_CLASS_TEXT;
import static android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS;
import static android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
import static android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
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
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.google.openlocationcode.OpenLocationCode;
import com.google.openlocationcode.OpenLocationCode.CodeArea;
import com.jwetherell.openmap.common.LatLonPoint;
import com.jwetherell.openmap.common.MGRSPoint;
import com.jwetherell.openmap.common.UTMPoint;
import com.jwetherell.openmap.common.ZonedUTMPoint;

import net.osmand.LocationConvert;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.R;
import net.osmand.plus.SwissGridApproximation;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.search.dialogs.SearchCitiesTask.SearchCitiesListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UpdateLocationUtils;
import net.osmand.plus.utils.UpdateLocationUtils.UpdateLocationViewCache;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class QuickSearchCoordinatesFragment extends BaseFullScreenDialogFragment implements OsmAndCompassListener, OsmAndLocationListener {

	public static final String TAG = QuickSearchCoordinatesFragment.class.getSimpleName();
	private static final String QUICK_SEARCH_COORDS_LAT_KEY = "quick_search_coords_lat_key";
	private static final String QUICK_SEARCH_COORDS_LON_KEY = "quick_search_coords_lon_key";
	private static final String QUICK_SEARCH_COORDS_NORTH_KEY = "quick_search_coords_north_key";
	private static final String QUICK_SEARCH_COORDS_EAST_KEY = "quick_search_coords_east_key";
	private static final String QUICK_SEARCH_COORDS_ZONE_KEY = "quick_search_coords_zone_key";
	private static final String QUICK_SEARCH_COORDS_MGRS_KEY = "quick_search_coords_mgrs_key";
	private static final String QUICK_SEARCH_COORDS_OLC_KEY = "quick_search_coords_olc_key";
	private static final String QUICK_SEARCH_COORDS_OLC_INFO_KEY = "quick_search_coords_olc_info_key";
	private static final String QUICK_SEARCH_COORDS_SWISS_GRID_EAST_KEY = "quick_search_coords_swiss_grid_east_key";
	private static final String QUICK_SEARCH_COORDS_SWISS_GRID_NORTH_KEY = "quick_search_coords_swiss_grid_north_key";
	private static final String QUICK_SEARCH_COORDS_FORMAT_KEY = "quick_search_coords_format_key";

	private static final String QUICK_SEARCH_COORDS_TEXT_KEY = "quick_search_coords_text_key";
	private static final String QUICK_SEARCH_COORDS_LATITUDE_KEY = "quick_search_coords_latitude_key";
	private static final String QUICK_SEARCH_COORDS_LONGITUDE_KEY = "quick_search_coords_longitude_key";

	public static int CURRENT_FORMAT = -1;

	private View view;
	private View coordsView;
	private View additionalCoordsView;
	private View errorView;
	private EditText latEdit;
	private EditText lonEdit;
	private EditText northingEdit;
	private EditText eastingEdit;
	private EditText zoneEdit;
	private EditText mgrsEdit;
	private EditText olcEdit;
	private TextView olcInfo;
	private EditText swissGridEastEdit;
	private EditText swissGridNorthEdit;
	private EditText formatEdit;
	private ProgressBar searchProgressBar;

	private net.osmand.Location myLocation;
	private float heading;
	private boolean paused;
	private LatLon currentLatLon;
	private LatLon additionalUtmLatLon;
	private UpdateLocationViewCache updateLocationViewCache;

	private SearchCitiesTask parseOlcCodeTask;

	public QuickSearchCoordinatesFragment() {
	}

	@Override
	@SuppressLint("PrivateResource")
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		view = inflate(R.layout.search_advanced_coords, container, false);

		Toolbar toolbar = view.findViewById(R.id.toolbar);
		int backIconColorId = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);
		Drawable icBack = getIcon(AndroidUtils.getNavigationIconResId(app), backIconColorId);
		toolbar.setNavigationIcon(icBack);
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(v -> dismiss());
		toolbar.setBackgroundColor(ColorUtilities.getAppBarColor(app, nightMode));
		toolbar.setTitleTextColor(getColor(!nightMode ? R.color.card_and_list_background_light : R.color.text_color_primary_dark));

		updateLocationViewCache = UpdateLocationUtils.getUpdateLocationViewCache(view.getContext());
		myLocation = app.getLocationProvider().getLastKnownLocation();

		if (CURRENT_FORMAT == -1) CURRENT_FORMAT = settings.COORDINATES_FORMAT.get();

		latEdit = view.findViewById(R.id.latitudeEditText);
		lonEdit = view.findViewById(R.id.longitudeEditText);
		northingEdit = view.findViewById(R.id.northingEditText);
		eastingEdit = view.findViewById(R.id.eastingEditText);
		zoneEdit = view.findViewById(R.id.zoneEditText);
		mgrsEdit = view.findViewById(R.id.mgrsEditText);
		olcEdit = view.findViewById(R.id.olcEditText);
		olcInfo = view.findViewById(R.id.olcInfoTextView);
		swissGridEastEdit = view.findViewById(R.id.swissGridEastEditText);
		swissGridNorthEdit = view.findViewById(R.id.swissGridNorthEditText);
		formatEdit = view.findViewById(R.id.formatEditText);
		searchProgressBar = view.findViewById(R.id.searchProgressBar);

		String defaultLat = "";
		String defaultZone = "";
		String defaultMgrs = "";
		String defaultOlc = "";
		String defaultSwissGridEast = "";
		String defaultSwissGridNorth = "";
		boolean coordinatesApplied = false;
		if (getArguments() != null) {
			String text = getArguments().getString(QUICK_SEARCH_COORDS_TEXT_KEY);
			if (!Algorithms.isEmpty(text)) {
				if (CURRENT_FORMAT == PointDescription.UTM_FORMAT) {
					defaultZone = text.trim();
				} else if (CURRENT_FORMAT == PointDescription.MGRS_FORMAT) {
					defaultMgrs = text.trim();
				} else if (CURRENT_FORMAT == PointDescription.OLC_FORMAT) {
					defaultOlc = text.trim();
				} else if (CURRENT_FORMAT == PointDescription.SWISS_GRID_FORMAT || CURRENT_FORMAT == PointDescription.SWISS_GRID_PLUS_FORMAT) {
					defaultSwissGridEast = text.trim();
				} else {
					defaultLat = text.trim();
				}
			} else {
				double latitude = getArguments().getDouble(QUICK_SEARCH_COORDS_LATITUDE_KEY, Double.NaN);
				double longitude = getArguments().getDouble(QUICK_SEARCH_COORDS_LONGITUDE_KEY, Double.NaN);
				if (!Double.isNaN(latitude) && !Double.isNaN(longitude)) {
					currentLatLon = new LatLon(latitude, longitude);
					applyFormat(CURRENT_FORMAT, true);
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
		String swissGridEastStr = getStringValue(savedInstanceState, QUICK_SEARCH_COORDS_SWISS_GRID_EAST_KEY, defaultSwissGridEast);
		String swissGridNorthStr = getStringValue(savedInstanceState, QUICK_SEARCH_COORDS_SWISS_GRID_NORTH_KEY, defaultSwissGridNorth);

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
			swissGridEastEdit.setText(swissGridEastStr);
			swissGridEastEdit.setSelection(swissGridEastStr.length());
			swissGridNorthEdit.setText(swissGridNorthStr);
			swissGridNorthEdit.setSelection(swissGridNorthStr.length());
		}

		formatEdit.setText(PointDescription.formatToHumanString(app, CURRENT_FORMAT));
		formatEdit.setOnClickListener(v ->
				ChooseCoordsFormatDialogFragment.showInstance(getChildFragmentManager()));

		TextWatcher textWatcher = new SimpleTextWatcher() {
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
		swissGridEastEdit.addTextChangedListener(textWatcher);
		swissGridNorthEdit.addTextChangedListener(textWatcher);


		OnEditorActionListener doneListener = (v, actionId, event) -> {
			if (actionId == EditorInfo.IME_ACTION_DONE) {
				if (currentLatLon != null && additionalUtmLatLon != null) {
					Activity activity = getActivity();
					if (activity != null) {
						AndroidUtils.hideSoftKeyboard(activity, eastingEdit);
					}
				} else {
					showOnMap(currentLatLon != null ? currentLatLon : additionalUtmLatLon);
				}
				return true;
			} else {
				return false;
			}
		};

		lonEdit.setOnEditorActionListener(doneListener);
		eastingEdit.setOnEditorActionListener(doneListener);
		mgrsEdit.setOnEditorActionListener(doneListener);
		olcEdit.setOnEditorActionListener(doneListener);
		swissGridEastEdit.setOnEditorActionListener(doneListener);
		swissGridNorthEdit.setOnEditorActionListener(doneListener);

		UiUtilities ic = app.getUIUtilities();
		((ImageView) view.findViewById(R.id.latitudeImage))
				.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_coordinates_latitude));
		((ImageView) view.findViewById(R.id.longitudeImage))
				.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_coordinates_longitude));
		((ImageView) view.findViewById(R.id.northingImage))
				.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_coordinates_latitude));
		((ImageView) view.findViewById(R.id.eastingImage))
				.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_coordinates_longitude));

		ImageButton latitudeClearButton = view.findViewById(R.id.latitudeClearButton);
		latitudeClearButton.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_remove_dark));
		latitudeClearButton.setOnClickListener(v -> latEdit.setText(""));
		ImageButton longitudeClearButton = view.findViewById(R.id.longitudeClearButton);
		longitudeClearButton.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_remove_dark));
		longitudeClearButton.setOnClickListener(v -> lonEdit.setText(""));
		ImageButton northingClearButton = view.findViewById(R.id.northingClearButton);
		northingClearButton.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_remove_dark));
		northingClearButton.setOnClickListener(v -> northingEdit.setText(""));
		ImageButton eastingClearButton = view.findViewById(R.id.eastingClearButton);
		eastingClearButton.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_remove_dark));
		eastingClearButton.setOnClickListener(v -> eastingEdit.setText(""));
		ImageButton zoneClearButton = view.findViewById(R.id.zoneClearButton);
		zoneClearButton.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_remove_dark));
		zoneClearButton.setOnClickListener(v -> zoneEdit.setText(""));
		ImageButton olcClearButton = view.findViewById(R.id.olcClearButton);
		olcClearButton.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_remove_dark));
		olcClearButton.setOnClickListener(v -> olcEdit.setText(""));
		ImageButton mgrsClearButton = view.findViewById(R.id.mgrsClearButton);
		mgrsClearButton.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_remove_dark));
		mgrsClearButton.setOnClickListener(v -> mgrsEdit.setText(""));
		ImageButton swissGridEastClearButton = view.findViewById(R.id.swissGridEastClearButton);
		swissGridEastClearButton.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_remove_dark));
		swissGridEastClearButton.setOnClickListener(v -> swissGridEastEdit.setText(""));
		ImageButton swissGridNorthClearButton = view.findViewById(R.id.swissGridNorthClearButton);
		swissGridNorthClearButton.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_remove_dark));
		swissGridNorthClearButton.setOnClickListener(v -> swissGridNorthEdit.setText(""));

		ImageButton formatSelectButton = view.findViewById(R.id.formatSelectButton);
		formatSelectButton.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_arrow_drop_down));
		formatSelectButton.setOnClickListener(v ->
				ChooseCoordsFormatDialogFragment.showInstance(getChildFragmentManager()));

		View coordinatesViewContainer = view.findViewById(R.id.found_location);
		coordsView = setupCoordinatesView(coordinatesViewContainer);
		coordsView.setOnClickListener(v -> showOnMap(currentLatLon));

		View additionalCoordinatesViewContainer = view.findViewById(R.id.additional_found_location);
		additionalCoordsView = setupCoordinatesView(additionalCoordinatesViewContainer);
		additionalCoordsView.setOnClickListener(v -> showOnMap(additionalUtmLatLon));

		errorView = view.findViewById(R.id.error_item);

		parseLocation();
		updateControlsVisibility();

		return view;
	}

	private View setupCoordinatesView(View view) {
		View coordinatesView = view.findViewById(R.id.searchListItemLayout);
		coordinatesView.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
		coordinatesView.setClickable(true);

		int iconColorRes = !nightMode ? R.color.osmand_orange : R.color.osmand_orange_dark;
		Drawable icon = getIcon(R.drawable.ic_action_world_globe, iconColorRes);
		((ImageView) coordinatesView.findViewById(R.id.imageView)).setImageDrawable(icon);

		AndroidUiHelper.updateVisibility(view.findViewById(R.id.divider), false);
		AndroidUiHelper.updateVisibility(coordinatesView.findViewById(R.id.time), false);
		AndroidUiHelper.updateVisibility(coordinatesView.findViewById(R.id.time_icon), false);
		AndroidUiHelper.updateVisibility(coordinatesView.findViewById(R.id.toggle_item), false);
		AndroidUiHelper.updateVisibility(coordinatesView.findViewById(R.id.type_name_icon), false);

		return coordinatesView;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		if (view != null) {
			TextView latEdit = view.findViewById(R.id.latitudeEditText);
			TextView lonEdit = view.findViewById(R.id.longitudeEditText);
			TextView northEdit = view.findViewById(R.id.northingEditText);
			TextView eastEdit = view.findViewById(R.id.eastingEditText);
			TextView zoneEdit = view.findViewById(R.id.zoneEditText);
			TextView mgrsEdit = view.findViewById(R.id.mgrsEditText);
			TextView olcEdit = view.findViewById(R.id.olcEditText);
			TextView olcInfo = view.findViewById(R.id.olcInfoTextView);
			TextView swissGridEastEdit = view.findViewById(R.id.swissGridEastEditText);
			TextView swissGridNorthEdit = view.findViewById(R.id.swissGridNorthEditText);
			outState.putString(QUICK_SEARCH_COORDS_LAT_KEY, latEdit.getText().toString());
			outState.putString(QUICK_SEARCH_COORDS_LON_KEY, lonEdit.getText().toString());
			outState.putString(QUICK_SEARCH_COORDS_NORTH_KEY, northEdit.getText().toString());
			outState.putString(QUICK_SEARCH_COORDS_EAST_KEY, eastEdit.getText().toString());
			outState.putString(QUICK_SEARCH_COORDS_ZONE_KEY, zoneEdit.getText().toString());
			outState.putString(QUICK_SEARCH_COORDS_MGRS_KEY, mgrsEdit.getText().toString());
			outState.putString(QUICK_SEARCH_COORDS_OLC_KEY, olcEdit.getText().toString());
			outState.putString(QUICK_SEARCH_COORDS_OLC_INFO_KEY, olcInfo.getText().toString());
			outState.putString(QUICK_SEARCH_COORDS_SWISS_GRID_EAST_KEY, swissGridEastEdit.getText().toString());
			outState.putString(QUICK_SEARCH_COORDS_SWISS_GRID_NORTH_KEY, swissGridNorthEdit.getText().toString());
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
		OsmAndLocationProvider locationProvider = app.getLocationProvider();
		locationProvider.removeCompassListener(locationProvider.getNavigationInfo());
		locationProvider.addCompassListener(this);
		locationProvider.addLocationListener(this);
		myLocation = locationProvider.getLastKnownLocation();
		updateLocation(myLocation);
	}

	private void stopLocationUpdate() {
		OsmAndLocationProvider locationProvider = app.getLocationProvider();
		locationProvider.removeLocationListener(this);
		locationProvider.removeCompassListener(this);
		locationProvider.addCompassListener(locationProvider.getNavigationInfo());
	}

	private void showOnMap(@Nullable LatLon latLon) {
		if (latLon != null) {
			QuickSearchDialogFragment dialogFragment = (QuickSearchDialogFragment) getParentFragment();
			dialogFragment.hideToolbar();
			dialogFragment.hide();

			double lat = latLon.getLatitude();
			double lon = latLon.getLongitude();
			PointDescription pointDescription = new PointDescription(lat, lon);
			QuickSearchListFragment.showOnMap(getMapActivity(), dialogFragment, lat, lon, 15,
					pointDescription, latLon);

			dismiss();
		}
	}

	@Override
	public void updateCompassValue(float value) {
		// 99 in next line used to one-time initialize arrows (with reference vs. fixed-north direction)
		// on non-compass devices
		if (Math.abs(MapUtils.degreesDiff(value, heading)) > 5) {
			heading = value;
			app.runInUIThread(() -> {
				updateLocationUI(coordsView, currentLatLon);
				updateLocationUI(additionalCoordsView, additionalUtmLatLon);
			});
		}
	}

	@Override
	public void updateLocation(net.osmand.Location location) {
		this.myLocation = location;
		app.runInUIThread(() -> {
			updateLocationUI(coordsView, currentLatLon);
			updateLocationUI(additionalCoordsView, additionalUtmLatLon);
		});
	}

	private void updateLocationUI(@NonNull View coordinatesView, @Nullable LatLon location) {
		if (!paused) {
			QuickSearchDialogFragment dialogFragment = (QuickSearchDialogFragment) getParentFragment();
			dialogFragment.getAccessibilityAssistant().lockEvents();
			updateCompassVisibility(coordinatesView, location);
			dialogFragment.getAccessibilityAssistant().unlockEvents();
		}
	}

	private void updateControlsVisibility() {
		switch (CURRENT_FORMAT) {

			case PointDescription.OLC_FORMAT: {
				view.findViewById(R.id.eastingLayout).setVisibility(View.GONE);
				view.findViewById(R.id.northingLayout).setVisibility(View.GONE);
				view.findViewById(R.id.zoneLayout).setVisibility(View.GONE);
				view.findViewById(R.id.olcLayout).setVisibility(View.VISIBLE);
				view.findViewById(R.id.olcInfoLayout).setVisibility(View.VISIBLE);
				view.findViewById(R.id.latitudeLayout).setVisibility(View.GONE);
				view.findViewById(R.id.longitudeLayout).setVisibility(View.GONE);
				view.findViewById(R.id.mgrsLayout).setVisibility(View.GONE);
				view.findViewById(R.id.swissGridEastLayout).setVisibility(View.GONE);
				view.findViewById(R.id.swissGridNorthLayout).setVisibility(View.GONE);
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
				view.findViewById(R.id.swissGridEastLayout).setVisibility(View.GONE);
				view.findViewById(R.id.swissGridNorthLayout).setVisibility(View.GONE);
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
				view.findViewById(R.id.swissGridEastLayout).setVisibility(View.GONE);
				view.findViewById(R.id.swissGridNorthLayout).setVisibility(View.GONE);
				break;
			}
			case PointDescription.SWISS_GRID_FORMAT:
			case PointDescription.SWISS_GRID_PLUS_FORMAT: {
				view.findViewById(R.id.eastingLayout).setVisibility(View.GONE);
				view.findViewById(R.id.northingLayout).setVisibility(View.GONE);
				view.findViewById(R.id.zoneLayout).setVisibility(View.GONE);
				view.findViewById(R.id.olcLayout).setVisibility(View.GONE);
				view.findViewById(R.id.olcInfoLayout).setVisibility(View.GONE);
				view.findViewById(R.id.latitudeLayout).setVisibility(View.GONE);
				view.findViewById(R.id.longitudeLayout).setVisibility(View.GONE);
				view.findViewById(R.id.mgrsLayout).setVisibility(View.GONE);
				view.findViewById(R.id.swissGridEastLayout).setVisibility(View.VISIBLE);
				view.findViewById(R.id.swissGridNorthLayout).setVisibility(View.VISIBLE);
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
				view.findViewById(R.id.swissGridEastLayout).setVisibility(View.GONE);
				view.findViewById(R.id.swissGridNorthLayout).setVisibility(View.GONE);
				break;
			}
		}
	}

	private String provideOlcInfo(String olcString) {
		try {
			if (!OpenLocationCode.isValidCode(olcString))
				return getString(R.string.navigate_point_olc_info_invalid);
			OpenLocationCode olc = new OpenLocationCode(olcString);
			if (olc.isShort())
				return getString(R.string.navigate_point_olc_info_short);
			OpenLocationCode.CodeArea area = olc.decode();
			int areaWidth = (int) Math.ceil(MapUtils.getDistance(area.getNorthLatitude(), area.getWestLongitude(),
					area.getNorthLatitude(), area.getEastLongitude()));
			int areaHeight = (int) Math.ceil(MapUtils.getDistance(area.getNorthLatitude(), area.getWestLongitude(),
					area.getSouthLatitude(), area.getWestLongitude()));
			return getString(R.string.navigate_point_olc_info_area,
					OsmAndFormatter.getFormattedDistance(areaWidth, app),
					OsmAndFormatter.getFormattedDistance(areaHeight, app));
		} catch (IllegalArgumentException iae) {
			return getString(R.string.navigate_point_olc_info_invalid);
		}
	}

	private void setInputTypeDependingOnFormat(EditText[] editTexts) {
		for (EditText et : editTexts) {
			if (CURRENT_FORMAT == PointDescription.FORMAT_DEGREES) {
				et.setInputType(TYPE_CLASS_PHONE);
			} else {
				et.setInputType(TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_VISIBLE_PASSWORD |
						TYPE_TEXT_FLAG_CAP_CHARACTERS | TYPE_TEXT_FLAG_NO_SUGGESTIONS);
			}
		}
	}

	protected boolean applyFormat(int format, boolean forceApply) {
		if (CURRENT_FORMAT != format || forceApply) {
			int prevFormat = CURRENT_FORMAT;
			CURRENT_FORMAT = format;
			formatEdit.setText(PointDescription.formatToHumanString(app, CURRENT_FORMAT));
			EditText latEdit = view.findViewById(R.id.latitudeEditText);
			EditText lonEdit = view.findViewById(R.id.longitudeEditText);
			updateControlsVisibility();
			LatLon latLon = currentLatLon;
			if (CURRENT_FORMAT == PointDescription.UTM_FORMAT) {
				EditText northingEdit = view.findViewById(R.id.northingEditText);
				EditText eastingEdit = view.findViewById(R.id.eastingEditText);
				EditText zoneEdit = view.findViewById(R.id.zoneEditText);
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
				} else if (prevFormat == PointDescription.SWISS_GRID_FORMAT || prevFormat == PointDescription.SWISS_GRID_PLUS_FORMAT) {
					zoneEdit.setText("");
					northingEdit.setText(swissGridNorthEdit.getText());
					eastingEdit.setText(swissGridEastEdit.getText());
				} else {
					zoneEdit.setText(latEdit.getText());
					northingEdit.setText("");
					eastingEdit.setText("");
				}
			} else if (CURRENT_FORMAT == PointDescription.MGRS_FORMAT) {
				EditText mgrsEdit = view.findViewById(R.id.mgrsEditText);
				if (latLon != null) {
					MGRSPoint pnt = new MGRSPoint(new LatLonPoint(latLon.getLatitude(), latLon.getLongitude()));
					mgrsEdit.setText(pnt.toFlavoredString(5));
				} else if (prevFormat == PointDescription.UTM_FORMAT) {
					mgrsEdit.setText(zoneEdit.getText());
				} else if (prevFormat == PointDescription.OLC_FORMAT) {
					mgrsEdit.setText(olcEdit.getText());
				} else if (prevFormat == PointDescription.SWISS_GRID_FORMAT || prevFormat == PointDescription.SWISS_GRID_PLUS_FORMAT) {
					mgrsEdit.setText(swissGridEastEdit.getText());
				} else {
					mgrsEdit.setText(latEdit.getText());
				}
			} else if (CURRENT_FORMAT == PointDescription.OLC_FORMAT) {
				if (latLon != null) {
					String code = OsmAndFormatter.getOpenLocationCode(latLon.getLatitude(), latLon.getLongitude());
					olcEdit.setText(code);
				} else if (prevFormat == PointDescription.UTM_FORMAT) {
					olcEdit.setText(zoneEdit.getText());
				} else if (prevFormat == PointDescription.MGRS_FORMAT) {
					olcEdit.setText(mgrsEdit.getText());
				} else if (prevFormat == PointDescription.SWISS_GRID_FORMAT || prevFormat == PointDescription.SWISS_GRID_PLUS_FORMAT) {
					olcEdit.setText(swissGridEastEdit.getText());
				} else {
					olcEdit.setText(latEdit.getText());
				}
				olcInfo.setText(provideOlcInfo(olcEdit.getText().toString()));
			} else if (CURRENT_FORMAT == PointDescription.SWISS_GRID_FORMAT || CURRENT_FORMAT == PointDescription.SWISS_GRID_PLUS_FORMAT) {
				if (latLon != null) {
					double[] swissGrid;
					if (CURRENT_FORMAT == PointDescription.SWISS_GRID_FORMAT) {
						swissGrid = SwissGridApproximation.convertWGS84ToLV03(latLon);
					} else {
						swissGrid = SwissGridApproximation.convertWGS84ToLV95(latLon);
					}
					DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(Locale.US);
					formatSymbols.setDecimalSeparator('.');
					formatSymbols.setGroupingSeparator(' ');
					DecimalFormat swissGridFormat = new DecimalFormat("###,###.##", formatSymbols);
					swissGridEastEdit.setText(swissGridFormat.format(swissGrid[0]));
					swissGridNorthEdit.setText(swissGridFormat.format(swissGrid[1]));
				} else if (prevFormat == PointDescription.UTM_FORMAT) {
					swissGridEastEdit.setText(zoneEdit.getText());
					swissGridNorthEdit.setText("");
				} else if (prevFormat == PointDescription.MGRS_FORMAT) {
					swissGridEastEdit.setText(mgrsEdit.getText());
					swissGridNorthEdit.setText("");
				} else if (prevFormat == PointDescription.OLC_FORMAT) {
					swissGridEastEdit.setText(olcEdit.getText());
					swissGridNorthEdit.setText("");
				} else if (prevFormat != PointDescription.SWISS_GRID_PLUS_FORMAT) {
					swissGridEastEdit.setText(latEdit.getText());
					swissGridNorthEdit.setText("");
				}
			} else {
				setInputTypeDependingOnFormat(new EditText[] {latEdit, lonEdit});
				if (latLon != null) {
					latEdit.setText(LocationConvert.convert(MapUtils.checkLatitude(latLon.getLatitude()), CURRENT_FORMAT));
					lonEdit.setText(LocationConvert.convert(MapUtils.checkLongitude(latLon.getLongitude()), CURRENT_FORMAT));
				} else if (prevFormat == PointDescription.UTM_FORMAT) {
					latEdit.setText(zoneEdit.getText());
					lonEdit.setText("");
				} else if (prevFormat == PointDescription.MGRS_FORMAT) {
					latEdit.setText(mgrsEdit.getText());
					lonEdit.setText("");
				} else if (prevFormat == PointDescription.OLC_FORMAT) {
					latEdit.setText(olcEdit.getText());
					lonEdit.setText("");
				} else if (prevFormat == PointDescription.SWISS_GRID_FORMAT || prevFormat == PointDescription.SWISS_GRID_PLUS_FORMAT) {
					latEdit.setText(swissGridEastEdit.getText());
					lonEdit.setText(swissGridNorthEdit.getText());
				}
			}
			return latLon != null;
		} else {
			return false;
		}
	}

	private void parseLocation() {
		LatLon loc;
		LatLon additionalLoc = null;
		try {
			if (CURRENT_FORMAT == LocationConvert.UTM_FORMAT) {
				double northing = Double.parseDouble(northingEdit.getText().toString());
				double easting = Double.parseDouble(eastingEdit.getText().toString());
				String zone = zoneEdit.getText().toString();
				int zoneNumber = Integer.parseInt(zone.substring(0, zone.length() - 1));
				char zoneLetter = zone.charAt(zone.length() - 1);

				Pair<LatLon, LatLon> locations = parseUtmLocations(northing, easting, zoneNumber, zoneLetter);
				loc = locations.first;
				if (loc == null || !loc.equals(locations.second)) {
					additionalLoc = locations.second;
				}
			} else if (CURRENT_FORMAT == LocationConvert.MGRS_FORMAT) {
				String mgrs = (mgrsEdit.getText().toString());
				MGRSPoint upoint = new MGRSPoint(mgrs);
				LatLonPoint ll = upoint.toLatLonPoint();
				loc = new LatLon(ll.getLatitude(), ll.getLongitude());
			} else if (CURRENT_FORMAT == LocationConvert.OLC_FORMAT) {
				String olcText = olcEdit.getText().toString();
				olcInfo.setText(provideOlcInfo(olcText));
				loc = parseOlcCode(olcText);
			} else if (CURRENT_FORMAT == LocationConvert.SWISS_GRID_FORMAT) {
				double eastCoordinate = Double.parseDouble(swissGridEastEdit.getText().toString().replaceAll("\\s+", ""));
				double northCoordinate = Double.parseDouble(swissGridNorthEdit.getText().toString().replaceAll("\\s+", ""));
				loc = SwissGridApproximation.convertLV03ToWGS84(eastCoordinate, northCoordinate);
			} else if (CURRENT_FORMAT == LocationConvert.SWISS_GRID_PLUS_FORMAT) {
				double eastCoordinate = Double.parseDouble(swissGridEastEdit.getText().toString().replaceAll("\\s+", ""));
				double northCoordinate = Double.parseDouble(swissGridNorthEdit.getText().toString().replaceAll("\\s+", ""));
				loc = SwissGridApproximation.convertLV95ToWGS84(eastCoordinate, northCoordinate);
			} else {
				double lat = LocationConvert.convert(latEdit.getText().toString(), true);
				double lon = LocationConvert.convert(lonEdit.getText().toString(), true);
				loc = new LatLon(lat, lon);
			}
			currentLatLon = loc;
			additionalUtmLatLon = additionalLoc;
		} catch (Exception e) {
			currentLatLon = null;
			additionalUtmLatLon = null;
		}
		updateLocationCell(coordsView, currentLatLon, additionalUtmLatLon != null);
		updateLocationCell(additionalCoordsView, additionalUtmLatLon, false);
		updateErrorVisibility();
	}

	private Pair<LatLon, LatLon> parseUtmLocations(double northing, double easting, int zoneNumber,
			char zoneLetter) {
		LatLon first = parseZonedUtmPoint(northing, easting, zoneNumber, zoneLetter);
		LatLon second = parseUtmPoint(northing, easting, zoneNumber, zoneLetter);
		return Pair.create(first, second);
	}

	private LatLon parseZonedUtmPoint(double northing, double easting, int zoneNumber,
			char zoneLetter) {
		try {
			ZonedUTMPoint point = new ZonedUTMPoint(northing, easting, zoneNumber, zoneLetter);
			LatLonPoint latLonPoint = point.ZonedUTMtoLL();
			return new LatLon(latLonPoint.getLatitude(), latLonPoint.getLongitude());
		} catch (NumberFormatException e) {
		}
		return null;
	}

	private LatLon parseUtmPoint(double northing, double easting, int zoneNumber, char zoneLetter) {
		try {
			UTMPoint point = new UTMPoint(northing, easting, zoneNumber, zoneLetter);
			LatLonPoint latLonPoint = point.toLatLonPoint();
			return new LatLon(latLonPoint.getLatitude(), latLonPoint.getLongitude());
		} catch (NumberFormatException e) {
		}
		return null;
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
		CodeArea codeArea = null;
		if (OpenLocationCode.isFullCode(olcTextCode)) {
			codeArea = OpenLocationCode.decode(olcTextCode);
		} else if (OpenLocationCode.isShortCode(olcTextCode)) {
			OpenLocationCode code = new OpenLocationCode(olcTextCode);
			LatLon mapLocation = requireMapActivity().getMapLocation();
			if (cityName.isEmpty()) {
				if (mapLocation != null) {
					OpenLocationCode newCode = code.recover(mapLocation.getLatitude(), mapLocation.getLongitude());
					codeArea = code.recover(mapLocation.getLatitude(), mapLocation.getLongitude()).decode();
					olcInfo.setText(provideOlcInfo(newCode.getCode()));
				}
			} else {
				parseOlcCodeTask = new SearchCitiesTask(app, cityName, mapLocation, getSearchCitiesListener(olcTextCode));
				OsmAndTaskManager.executeTask(parseOlcCodeTask);
			}
		}
		if (codeArea != null) {
			loc = new LatLon(codeArea.getCenterLatitude(), codeArea.getCenterLongitude());
		}

		return loc;
	}

	@NonNull
	private SearchCitiesListener getSearchCitiesListener(@NonNull String olcTextCode) {
		return new SearchCitiesListener() {
			@Override
			public void onSearchCitiesStarted() {
				if (isResumed()) {
					updateProgressBar(true);
				}
			}

			@Override
			public void onSearchCitiesFinished(@NonNull List<Amenity> cities) {
				if (isResumed()) {
					updateProgressBar(false);

					if (!cities.isEmpty() && OpenLocationCode.isValidCode(olcTextCode)) {
						LatLon latLon = cities.get(0).getLocation();
						OpenLocationCode code = new OpenLocationCode(olcTextCode);
						OpenLocationCode newCode = code.recover(latLon.getLatitude(), latLon.getLongitude());
						CodeArea codeArea = newCode.decode();
						updateCurrentLocation(new LatLon(codeArea.getCenterLatitude(), codeArea.getCenterLongitude()), newCode.getCode());
					}
				}
			}
		};
	}

	public void stopSearchAsyncTask() {
		if (parseOlcCodeTask != null && parseOlcCodeTask.getStatus() == AsyncTask.Status.RUNNING) {
			parseOlcCodeTask.cancel(true);
		}
	}

	private void updateProgressBar(boolean visible) {
		searchProgressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
	}

	private void updateCurrentLocation(LatLon latLon, String olcText) {
		currentLatLon = latLon;
		updateLocationCell(coordsView, currentLatLon, false);
		olcInfo.setText(provideOlcInfo(olcText));
		updateErrorVisibility();
	}

	private void updateLocationCell(View coordinatesView, LatLon latLon, boolean showDivider) {
		if (latLon == null) {
			AndroidUiHelper.updateVisibility(coordinatesView, false);
		} else {
			TextView titleView = coordinatesView.findViewById(R.id.title);
			TextView subtitleView = coordinatesView.findViewById(R.id.subtitle);
			titleView.setText(PointDescription.getLocationNamePlain(app, latLon.getLatitude(), latLon.getLongitude()));
			OsmAndTaskManager.executeTask(new AsyncTask<LatLon, Void, String>() {
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
			}, latLon);
			updateLocationUI(coordinatesView, latLon);
			AndroidUiHelper.updateVisibility(coordinatesView, true);
			AndroidUiHelper.updateVisibility(((View) coordinatesView.getParent()).findViewById(R.id.divider), showDivider);
		}
	}

	private void updateCompassVisibility(@NonNull View view, @Nullable LatLon latLon) {
		boolean showCompass = latLon != null;
		if (showCompass) {
			updateDistanceDirection(view, latLon);
		}
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.compass_layout), showCompass);
	}

	private void updateErrorVisibility() {
		AndroidUiHelper.updateVisibility(errorView, currentLatLon == null);
	}

	private void updateDistanceDirection(View view, LatLon latLon) {
		TextView distanceText = view.findViewById(R.id.distance);
		ImageView direction = view.findViewById(R.id.direction);
		UpdateLocationUtils.updateLocationView(app, updateLocationViewCache, direction, distanceText, latLon);
	}

	public static void showInstance(@NonNull DialogFragment parentFragment, String text) {
		FragmentManager manager = parentFragment.getChildFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle args = new Bundle();
			args.putString(QUICK_SEARCH_COORDS_TEXT_KEY, text);
			QuickSearchCoordinatesFragment fragment = new QuickSearchCoordinatesFragment();
			fragment.setArguments(args);
			fragment.show(manager, TAG);
		}
	}

	public static void showInstance(@NonNull DialogFragment parentFragment, double lat, double lon) {
		FragmentManager manager = parentFragment.getChildFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle bundle = new Bundle();
			bundle.putDouble(QUICK_SEARCH_COORDS_LATITUDE_KEY, lat);
			bundle.putDouble(QUICK_SEARCH_COORDS_LONGITUDE_KEY, lon);
			QuickSearchCoordinatesFragment fragment = new QuickSearchCoordinatesFragment();
			fragment.setArguments(bundle);
			fragment.show(manager, TAG);
		}
	}
}
