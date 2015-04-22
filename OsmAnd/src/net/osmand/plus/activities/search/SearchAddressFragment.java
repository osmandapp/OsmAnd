package net.osmand.plus.activities.search;

import net.osmand.access.AccessibleToast;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.dialogs.FavoriteDialogs;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.resources.RegionAddressRepository;
import net.osmand.util.Algorithms;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class SearchAddressFragment extends Fragment {

	public static final String SELECT_ADDRESS_POINT_INTENT_KEY = "SELECT_ADDRESS_POINT_INTENT_KEY";
	public static final int SELECT_ADDRESS_POINT_RESULT_OK = 1;	
	private static final boolean ENABLE_ONLINE_ADDRESS = false; // disabled moved to poi search
	public static final String SELECT_ADDRESS_POINT_LAT = "SELECT_ADDRESS_POINT_LAT";
	public static final String SELECT_ADDRESS_POINT_LON = "SELECT_ADDRESS_POINT_LON";
	private static final int NAVIGATE_TO = 0;
	private static final int ADD_WAYPOINT = 1;
	private static final int SHOW_ON_MAP = 2;
	private static final int ONLINE_SEARCH = 3;
	private static final int SELECT_POINT = 4;
	private static final int ADD_TO_FAVORITE = 5;
	
	private Button streetButton;
	private Button cityButton;
	private Button countryButton;
	private Button buildingButton;
	
	private String region = null;
	private String city = null;
	private String postcode = null;
	private String street = null;
	private String building = null;
	private String street2 = null;
	private boolean radioBuilding = true;
	
	private OsmandSettings osmandSettings;
	private LatLon searchPoint = null;

	private View view;
	

	public View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.search_address, container, false);
		
		streetButton = (Button) findViewById(R.id.StreetButton);
		cityButton = (Button) findViewById(R.id.CityButton);
		countryButton = (Button) findViewById(R.id.CountryButton);
		buildingButton = (Button) findViewById(R.id.BuildingButton);
		osmandSettings = getApplication().getSettings();
		attachListeners();
		setHasOptionsMenu(true);
		return view;
	}



	@Override
	public void onCreateOptionsMenu(Menu onCreate, MenuInflater inflater) {
		Menu menu = onCreate;
		boolean portrait = AndroidUiHelper.isOrientationPortrait(getActivity());
		if(getActivity() instanceof SearchActivity) {
			if (portrait) {
				menu = ((SearchActivity) getActivity()).getClearToolbar(true).getMenu();
			} else {
				((SearchActivity) getActivity()).getClearToolbar(false);
			}
		}
		if(getActivity() instanceof SearchAddressActivity) {
			MenuItem menuItem = menu.add(0, SELECT_POINT, 0, "");
			MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
			menuItem = menuItem.setIcon(R.drawable.ic_action_done);
			menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					select(SELECT_POINT);
					return true;
				}
			});
		} else {
			MenuItem menuItem = menu.add(0, NAVIGATE_TO, 0, R.string.context_menu_item_directions_to);
			MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
			menuItem = menuItem.setIcon(R.drawable.ic_action_gdirections_dark);
			menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					select(NAVIGATE_TO);
					return true;
				}
			});
			TargetPointsHelper targets = getApplication().getTargetPointsHelper();
			if (targets.getPointToNavigate() != null) {
				menuItem = menu.add(0, ADD_WAYPOINT, 0, R.string.context_menu_item_intermediate_point);
				MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
				menuItem = menuItem.setIcon(R.drawable.ic_action_flage_dark);
			} else {
				menuItem = menu.add(0, ADD_WAYPOINT, 0, R.string.context_menu_item_destination_point);
				MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
				menuItem = menuItem.setIcon(R.drawable.ic_action_flag_dark);
			}
			menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					select(ADD_WAYPOINT);
					return true;
				}
			});
			menuItem = menu.add(0, SHOW_ON_MAP, 0, R.string.shared_string_show_on_map);
			MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
			menuItem = menuItem.setIcon(R.drawable.ic_action_marker_dark);

			menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					select(SHOW_ON_MAP);
					return true;
				}
			});
			
			menuItem = menu.add(0, ADD_TO_FAVORITE, 0, R.string.shared_string_add_to_favorites);
			MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
			menuItem = menuItem.setIcon(R.drawable.ic_action_fav_dark);

			menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					select(ADD_TO_FAVORITE);
					return true;
				}
			});
			if (ENABLE_ONLINE_ADDRESS) {
				menuItem = menu.add(0, ONLINE_SEARCH, 0, R.string.search_online_address);
				MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
				menuItem = menuItem.setIcon(R.drawable.ic_world_globe_dark);
				menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						((SearchActivity) getActivity()).startSearchAddressOnline();
						return true;
					}
				});
			}
		}

	}
	
	private OsmandApplication getApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	private Intent createIntent(Class<?> cl){
		LatLon location = null;
		Intent intent = getActivity().getIntent();
		if(intent != null){
			double lat = intent.getDoubleExtra(SearchActivity.SEARCH_LAT, 0);
			double lon = intent.getDoubleExtra(SearchActivity.SEARCH_LON, 0);
			if(lat != 0 || lon != 0){
				location = new LatLon(lat, lon);
			}
		}
		if (location == null && getActivity() instanceof SearchActivity) {
			location = ((SearchActivity) getActivity()).getSearchPoint();
		}
		Intent newIntent = new Intent(getActivity(), cl);
		if (location != null) {
			newIntent.putExtra(SearchActivity.SEARCH_LAT, location.getLatitude());
			newIntent.putExtra(SearchActivity.SEARCH_LON, location.getLongitude());
		}
		return newIntent;
	}
	
	private void attachListeners() {
		countryButton.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				startActivity(createIntent(SearchRegionByNameActivity.class));
			}
		});
		cityButton.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				startActivity(createIntent(SearchCityByNameActivity.class));
			}
		});
		streetButton.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				startActivity(createIntent(SearchStreetByNameActivity.class));
			}
		});
		buildingButton.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				if(radioBuilding){
					startActivity(createIntent(SearchBuildingByNameActivity.class));
				} else {
					startActivity(createIntent(SearchStreet2ByNameActivity.class));
				}
			}
		});
		OsmandApplication app = getApplication();
		Drawable icon = getApplication().getIconsCache().getContentIcon(R.drawable.ic_action_remove_dark);
		((ImageView)findViewById(R.id.ResetBuilding)).setBackgroundDrawable(icon);
		findViewById(R.id.ResetBuilding).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				building = null;
				searchPoint = null;
				osmandSettings.setLastSearchedBuilding("", null);
				//also empties Point, REMOVES intersecting street
				updateUI();
			}
		 });
		((ImageView)findViewById(R.id.ResetStreet)).setBackgroundDrawable(icon);
		 findViewById(R.id.ResetStreet).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				street = null;
				street2 = null;
				building = null;
				searchPoint = null;
				osmandSettings.setLastSearchedStreet("", null);
				//also empties Building, (Intersecting Street), Point
				updateUI();
			}
		 });
		 ((ImageView)findViewById(R.id.ResetCity)).setBackgroundDrawable(icon);
		 findViewById(R.id.ResetCity).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				postcode = null;
				city = null;
				street = null;
				street2 = null;
				building = null;
				searchPoint = null;
				osmandSettings.setLastSearchedCity(-1L, "", null);
				//also empties Street, (Intersecting Street), Building, Point, REMOVES Postcode
				updateUI();
			}
		 });
		 ((ImageView)findViewById(R.id.ResetCountry)).setBackgroundDrawable(icon);
		 findViewById(R.id.ResetCountry).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				region = null;
				postcode = null;
				city = null;
				street = null;
				street2 = null;
				building = null;
				searchPoint = null;
				osmandSettings.setLastSearchedRegion("", null);
				// also empties City, Postcode, Street, (Interseting street), Building, Point
				updateUI();
			}
		});
		((RadioGroup)findViewById(R.id.RadioGroup)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				SearchAddressFragment.this.radioBuilding = checkedId == R.id.RadioBuilding;
				if(radioBuilding){
					SearchAddressFragment.this.street2 = null;
				} else {
					SearchAddressFragment.this.building = null;
				}
				updateBuildingSection();
			}
		});
	}
	
	public static class AddressInformation {
		String objectType = "";
		int zoom = 14;
		public String objectName ="";
		
		public PointDescription getHistoryName() {
			return new PointDescription(PointDescription.POINT_TYPE_ADDRESS, objectType, objectName);
		}
		
		public static AddressInformation build2StreetIntersection(Context ctx, OsmandSettings settings){
			AddressInformation ai = new AddressInformation();
			String cityName = getCityName(settings);
			ai.objectName = settings.getLastSearchedStreet() +" x " +
 					settings.getLastSearchedIntersectedStreet() + " " + cityName;
			ai.objectType = cityName;
			ai.zoom = 17;
			return ai;
		}
		
		public static AddressInformation buildStreet(Context ctx, OsmandSettings settings){
			AddressInformation ai = new AddressInformation();
			String cityName = getCityName(settings);
			String street = settings.getLastSearchedStreet();
			ai.objectName = street;
			ai.objectType = cityName;
			ai.zoom = 16;
			return ai;
		}
		
		
		public static AddressInformation buildBuilding(Context ctx, OsmandSettings settings){
			AddressInformation ai = new AddressInformation();
			String cityName = getCityName(settings);
			String street = settings.getLastSearchedStreet();
			String building = settings.getLastSearchedBuilding();
			ai.objectName = street + " " + building;
			ai.objectType = cityName;
			ai.zoom = 17;
			return ai;
		}

		private static String getCityName(OsmandSettings settings) {
			String postcode = settings.getLastSearchedPostcode();
			String city = settings.getLastSearchedCityName();
			String cityName = !Algorithms.isEmpty(postcode) ? postcode : city;
			return cityName;
		}

		private static String getRegionName(Context ctx, OsmandSettings settings) {
			return FileNameTranslationHelper.getFileName(ctx, ((OsmandApplication) ctx.getApplicationContext())
					.getResourceManager().getOsmandRegions(), settings.getLastSearchedRegion());
		}
		
		public static AddressInformation buildCity(Context ctx, OsmandSettings settings){
			AddressInformation ai = new AddressInformation();
			String city = settings.getLastSearchedCityName();
			ai.objectName = city;
			ai.objectType = getRegionName(ctx, settings);
			ai.zoom = 14;
			return ai;
		}
	}
	
	public void select(int mode) {
		if (searchPoint == null) {
			AccessibleToast.makeText(getActivity(), R.string.please_select_address, Toast.LENGTH_SHORT).show();
			return;
		}
		AddressInformation ai = new AddressInformation();
		PointDescription pointDescription = ai.getHistoryName();
		if (!Algorithms.isEmpty(street2) && !Algorithms.isEmpty(street)) {
			ai = AddressInformation.build2StreetIntersection(getActivity(), osmandSettings);
			pointDescription.setName(street2);
			pointDescription.setTypeName(getRegionName() + ", " + city);
		} else if (!Algorithms.isEmpty(building)) {
			ai = AddressInformation.buildBuilding(getActivity(), osmandSettings);
			pointDescription.setName(street + ", " + building);
			pointDescription.setTypeName(getRegionName() + ", " + city);
		} else if (!Algorithms.isEmpty(street)) {
			ai = AddressInformation.buildStreet(getActivity(), osmandSettings);
			pointDescription.setName(street);
			pointDescription.setTypeName(getRegionName() + ", " + city);
		} else if(!Algorithms.isEmpty(city)) {
			ai = AddressInformation.buildCity(getActivity(), osmandSettings);
			pointDescription.setName(city);
			pointDescription.setTypeName(getRegionName());
		}

		if(mode == ADD_TO_FAVORITE) {
			Bundle b = new Bundle();
			Dialog dlg = FavoriteDialogs.createAddFavouriteDialog(getActivity(), b);
			dlg.show();
			FavoriteDialogs.prepareAddFavouriteDialog(getActivity(), dlg, b, searchPoint.getLatitude(), searchPoint.getLongitude(),
					pointDescription);
		} else if(mode == SELECT_POINT ){
			Intent intent = getActivity().getIntent();
			intent.putExtra(SELECT_ADDRESS_POINT_INTENT_KEY, ai.objectName);
			intent.putExtra(SELECT_ADDRESS_POINT_LAT, searchPoint.getLatitude());
			intent.putExtra(SELECT_ADDRESS_POINT_LON, searchPoint.getLongitude());
			getActivity().setResult(SELECT_ADDRESS_POINT_RESULT_OK, intent);
			getActivity().finish();
		} else {
			if (mode == NAVIGATE_TO) {
				DirectionsDialogs.directionsToDialogAndLaunchMap(getActivity(), searchPoint.getLatitude(), searchPoint.getLongitude(),  pointDescription);
			} else if (mode == ADD_WAYPOINT) {
				DirectionsDialogs.addWaypointDialogAndLaunchMap(getActivity(), searchPoint.getLatitude(), searchPoint.getLongitude(), pointDescription);
			} else if (mode == SHOW_ON_MAP) {
				osmandSettings.setMapLocationToShow(searchPoint.getLatitude(), searchPoint.getLongitude(), ai.zoom, pointDescription);
				MapActivity.launchMapActivityMoveToTop(getActivity());
			}
		}
	}
	
	
	protected void updateBuildingSection(){
		if(radioBuilding){
			((TextView)findViewById(R.id.BuildingText)).setText(R.string.search_address_building);
			if(Algorithms.isEmpty(building)){
				((TextView)findViewById(R.id.BuildingButton)).setText(R.string.choose_building);
			} else {
				((TextView)findViewById(R.id.BuildingButton)).setText(building);
			}
		} else {
			((TextView)findViewById(R.id.BuildingText)).setText(R.string.search_address_street);
			if(Algorithms.isEmpty(street2)){
				((TextView)findViewById(R.id.BuildingButton)).setText(R.string.choose_intersected_street);
			} else {
				((TextView)findViewById(R.id.BuildingButton)).setText(street2);
			}
		}
		findViewById(R.id.ResetBuilding).setEnabled(!Algorithms.isEmpty(street2) || !Algorithms.isEmpty(building));
	}

	private View findViewById(int resId) {
		return view.findViewById(resId);
	}

	protected void updateUI(){
		findViewById(R.id.ResetCountry).setEnabled(!Algorithms.isEmpty(region));
		if(Algorithms.isEmpty(region)){
			countryButton.setText(R.string.ChooseCountry);
		} else {
			String rnname = getRegionName();
			countryButton.setText(rnname);
		}

		findViewById(R.id.ResetCity).setEnabled(!Algorithms.isEmpty(city) || !Algorithms.isEmpty(postcode));
		if(Algorithms.isEmpty(city) && Algorithms.isEmpty(postcode)){
			cityButton.setText(R.string.choose_city);
		} else {
			if(!Algorithms.isEmpty(postcode)){
				cityButton.setText(postcode);
			} else {
				cityButton.setText(city.replace('_', ' '));
			}
		}
		cityButton.setEnabled(!Algorithms.isEmpty(region));

		findViewById(R.id.ResetStreet).setEnabled(!Algorithms.isEmpty(street));
		if(Algorithms.isEmpty(street)){
			streetButton.setText(R.string.choose_street);
		} else {
			streetButton.setText(street);
		}
		streetButton.setEnabled(!Algorithms.isEmpty(city) || !Algorithms.isEmpty(postcode));
		
		buildingButton.setEnabled(!Algorithms.isEmpty(street));
		findViewById(R.id.RadioGroup).setVisibility(Algorithms.isEmpty(street) ? View.GONE : View.VISIBLE);
		
		if(radioBuilding){
			((RadioButton)findViewById(R.id.RadioBuilding)).setChecked(true);
		} else {
			((RadioButton)findViewById(R.id.RadioIntersStreet)).setChecked(true);
		}
		updateBuildingSection();
	}



	private String getRegionName() {
		return FileNameTranslationHelper.getFileName(getActivity(),
				((OsmandApplication) getActivity().getApplication()).getResourceManager().getOsmandRegions(), region);
	}
	
	public void loadData() {
		if (!Algorithms.isEmpty(region)) {
			String postcodeStr = osmandSettings.getLastSearchedPostcode();
			if (!Algorithms.isEmpty(postcodeStr)) {
				postcode = postcodeStr;
			} else {
				city = osmandSettings.getLastSearchedCityName();
			}

			if (!Algorithms.isEmpty(postcode) || !Algorithms.isEmpty(city)) {
				street = osmandSettings.getLastSearchedStreet();
				if (!Algorithms.isEmpty(street)) {
					String str = osmandSettings.getLastSearchedIntersectedStreet();
					radioBuilding = Algorithms.isEmpty(str);
					if (!radioBuilding) {
						street2 = str;
					} else {
						building = osmandSettings.getLastSearchedBuilding();
					}
				}
			}
		}
	}
	
	
	@Override
	public void onResume() {
		super.onResume();
		searchPoint = osmandSettings.getLastSearchedPoint();
		region = null;
		postcode = null;
		city = null;
		street = null;
		building = null;
		region = osmandSettings.getLastSearchedRegion();
		RegionAddressRepository reg = getApplication().getResourceManager().getRegionRepository(region);
		if(reg != null && reg.useEnglishNames() != osmandSettings.usingEnglishNames()){
			reg.setUseEnglishNames(osmandSettings.usingEnglishNames());
		}
		loadData();
		updateUI();
		
	}

	
	
}
