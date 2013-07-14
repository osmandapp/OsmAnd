package net.osmand.plus.activities.search;

import java.text.MessageFormat;

import net.osmand.access.AccessibleToast;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityActions;
import net.osmand.plus.resources.RegionAddressRepository;
import net.osmand.util.Algorithms;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;

public class SearchAddressFragment extends SherlockFragment {

	public static final String SELECT_ADDRESS_POINT_INTENT_KEY = "SELECT_ADDRESS_POINT_INTENT_KEY";
	public static final int SELECT_ADDRESS_POINT_RESULT_OK = 1;	
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
		osmandSettings = ((OsmandApplication) getApplication()).getSettings();
		attachListeners();
		setHasOptionsMenu(true);
		return view;
	}
	
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		boolean light = ((OsmandApplication) getApplication()).getSettings().isLightActionBar();
		if(getActivity() instanceof SearchAddressActivity) {
			com.actionbarsherlock.view.MenuItem menuItem = menu.add(0, SELECT_POINT, 0, "").setShowAsActionFlags(
					MenuItem.SHOW_AS_ACTION_ALWAYS );
			menuItem = menuItem.setIcon(light ? R.drawable.ic_action_ok_light : R.drawable.ic_action_ok_dark);
			menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
					select(SELECT_POINT);
					return true;
				}
			});
		} else {
			com.actionbarsherlock.view.MenuItem menuItem = menu.add(0, NAVIGATE_TO, 0, R.string.get_directions).setShowAsActionFlags(
					MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
			menuItem = menuItem.setIcon(light ? R.drawable.ic_action_gdirections_light : R.drawable.ic_action_gdirections_dark);
			menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
					select(NAVIGATE_TO);
					return true;
				}
			});
			TargetPointsHelper targets = ((OsmandApplication) getApplication()).getTargetPointsHelper();
			if (targets.getPointToNavigate() != null) {
				menuItem = menu.add(0, ADD_WAYPOINT, 0, R.string.context_menu_item_intermediate_point).setShowAsActionFlags(
						MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
				menuItem = menuItem.setIcon(light ? R.drawable.ic_action_flage_light : R.drawable.ic_action_flage_dark);
			} else {
				menuItem = menu.add(0, ADD_WAYPOINT, 0, R.string.context_menu_item_destination_point).setShowAsActionFlags(
						MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
				menuItem = menuItem.setIcon(light ? R.drawable.ic_action_flag_light : R.drawable.ic_action_flag_dark);
			}
			menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
					select(ADD_WAYPOINT);
					return true;
				}
			});
			menuItem = menu.add(0, SHOW_ON_MAP, 0, R.string.search_shown_on_map).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
			menuItem = menuItem.setIcon(light ? R.drawable.ic_action_marker_light : R.drawable.ic_action_marker_dark);

			menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
					select(SHOW_ON_MAP);
					return true;
				}
			});
			
			menuItem = menu.add(0, ADD_TO_FAVORITE, 0, R.string.add_to_favourite).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
			menuItem = menuItem.setIcon(light ? R.drawable.ic_action_fav_light : R.drawable.ic_action_fav_dark);

			menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
					select(ADD_TO_FAVORITE);
					return true;
				}
			});
			menuItem = menu.add(0, ONLINE_SEARCH, 0, R.string.search_online_address).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
			menuItem = menuItem.setIcon(light ? R.drawable.ic_action_gnext_light : R.drawable.ic_action_gnext_dark);
			menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
					((SearchActivity) getActivity()).startSearchAddressOnline();
					return true;
				}
			});
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
		findViewById(R.id.ResetBuilding).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				building = null;
				searchPoint = null;
				updateUI();
			}
		 });
		 findViewById(R.id.ResetStreet).setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v) {
					street = null;
					building = null;
					street2 = null;
					searchPoint = null;
					updateUI();
				}
		 });
		 findViewById(R.id.ResetCity).setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v) {
					postcode = null;
					city = null;
					street = null;
					street2 = null;
					building = null;
					searchPoint = null;
					updateUI();
				}
		 });
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
	
	public void select(int mode) {
		if (searchPoint == null) {
			AccessibleToast.makeText(getActivity(), R.string.please_select_address, Toast.LENGTH_SHORT).show();
			return;
		}
		String historyName = null;
		String objectName = "";
		int zoom = 14;
		if (!Algorithms.isEmpty(street2) && !Algorithms.isEmpty(street)) {
			String cityName = !Algorithms.isEmpty(postcode) ? postcode : city;
			objectName = street;
			historyName = MessageFormat.format(getString(R.string.search_history_int_streets), street, street2,
					cityName);
			zoom = 17;
		} else if (!Algorithms.isEmpty(building)) {
			String cityName = !Algorithms.isEmpty(postcode) ? postcode : city;
			objectName = street + " " + building;
			historyName = MessageFormat.format(getString(R.string.search_history_building), building, street,
					cityName);
			zoom = 17;
		} else if (!Algorithms.isEmpty(street)) {
			String cityName = postcode != null ? postcode : city;
			objectName = street;
			historyName = MessageFormat.format(getString(R.string.search_history_street), street, cityName);
			zoom = 16;
		} else if (!Algorithms.isEmpty(city)) {
			historyName = MessageFormat.format(getString(R.string.search_history_city), city);
			objectName = city;
			zoom = 14;
		}
		if(mode == ADD_TO_FAVORITE) {
			Bundle b = new Bundle();
			Dialog dlg = MapActivityActions.createAddFavouriteDialog(getActivity(), b);
			dlg.show();
			MapActivityActions.prepareAddFavouriteDialog(getActivity(), dlg, b, searchPoint.getLatitude(), searchPoint.getLongitude(), objectName);
		} else if(mode == SELECT_POINT ){
			Intent intent = getActivity().getIntent();
			intent.putExtra(SELECT_ADDRESS_POINT_INTENT_KEY, objectName);
			intent.putExtra(SELECT_ADDRESS_POINT_LAT, searchPoint.getLatitude());
			intent.putExtra(SELECT_ADDRESS_POINT_LON, searchPoint.getLongitude());
			getActivity().setResult(SELECT_ADDRESS_POINT_RESULT_OK, intent);
			getActivity().finish();
		} else {
			OsmandApplication ctx = (OsmandApplication) getActivity().getApplication();
			if (mode == NAVIGATE_TO) {
				MapActivityActions.directionsToDialogAndLaunchMap(getActivity(), searchPoint.getLatitude(), searchPoint.getLongitude(),  historyName);
			} else if (mode == ADD_WAYPOINT) {
				MapActivityActions.addWaypointDialogAndLaunchMap(getActivity(), searchPoint.getLatitude(), searchPoint.getLongitude(), historyName);
			} else if (mode == SHOW_ON_MAP) {
				osmandSettings.setMapLocationToShow(searchPoint.getLatitude(), searchPoint.getLongitude(), zoom, historyName);
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
			countryButton.setText(region.replace('_', ' '));
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
		((RadioGroup)findViewById(R.id.RadioGroup)).setVisibility(Algorithms.isEmpty(street) ? View.GONE : View.VISIBLE);
		
		if(radioBuilding){
			((RadioButton)findViewById(R.id.RadioBuilding)).setChecked(true);
		} else {
			((RadioButton)findViewById(R.id.RadioIntersStreet)).setChecked(true);
		}
		updateBuildingSection();
		
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
		RegionAddressRepository reg = ((OsmandApplication)getApplication()).getResourceManager().getRegionRepository(region);
		if(reg != null && reg.useEnglishNames() != osmandSettings.USE_ENGLISH_NAMES.get()){
			reg.setUseEnglishNames(osmandSettings.USE_ENGLISH_NAMES.get());
		}
		loadData();
		updateUI();
		
	}

	
	
}
