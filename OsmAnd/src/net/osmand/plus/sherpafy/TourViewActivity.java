package net.osmand.plus.sherpafy;

import java.util.WeakHashMap;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.sherpafy.TourInformation.StageInformation;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;

/**
 */
public class TourViewActivity extends SherlockFragmentActivity {

	private static final int STATE_LOADING = -1;
	private static final int STATE_SELECT_TOUR = 1;
	private static final int STATE_TOUR_VIEW = 2;
	private static final int STATE_STAGE_OVERVIEW = 3;
	private static int state = STATE_LOADING;
	
	public static final int APP_EXIT_CODE = 4;
	public static final String APP_EXIT_KEY = "APP_EXIT_KEY";

	
	private SherpafyCustomization customization;
	private Point displaySize;
	private ActionBarDrawerToggle mDrawerToggle;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ArrayAdapter<Object> drawerAdapter;
	private WeakHashMap<Object, Fragment> fragments = new WeakHashMap<Object, Fragment>();
	private static Object selectedItem;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (!(getMyApplication().getAppCustomization() instanceof SherpafyCustomization)) {
			getMyApplication().setAppCustomization(new SherpafyCustomization());
		}
		customization = (SherpafyCustomization) getMyApplication().getAppCustomization();
		setTheme(R.style.OsmandLightTheme);
		((OsmandApplication) getApplication()).setLanguage(this);
		super.onCreate(savedInstanceState);
		if (getIntent() != null) {
			Intent intent = getIntent();
			if (intent.getExtras() != null && intent.getExtras().containsKey(APP_EXIT_KEY)) {
				getMyApplication().closeApplication(this);
				return;
			}
		}
		getSherlock().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setTitle(R.string.sherpafy_app_name);

		setContentView(R.layout.sherpafy_browse_tour);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

		// The drawer title must be set in order to announce state changes when
		// accessibility is turned on. This is typically a simple description,
		// e.g. "Navigation".
		mDrawerLayout.setDrawerTitle(GravityCompat.START, getString(R.string.sherpafy_app_name));

		// Set the adapter for the list view
		drawerAdapter = setupAdapter();
		mDrawerList.setAdapter(drawerAdapter);
		// Set the list's click listener
		mDrawerList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				selectMenu(drawerAdapter.getItem(position));
			}
		});

		displaySize = new Point();
		getWindowManager().getDefaultDisplay().getSize(displaySize);
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_navigation_drawer_light,
				R.string.default_buttons_other_actions, R.string.close);
		if (getMyApplication().isApplicationInitializing()) {
			FragmentManager fragmentManager = getSupportFragmentManager();
			fragmentManager.beginTransaction().replace(R.id.content_frame, new SherpafyLoadingFragment()).commit();
		} else {
			showSelectedItem();
		}
	}
	

	private ArrayAdapter<Object> setupAdapter() {
		return new ArrayAdapter<Object>(this, R.layout.sherpafy_drawer_list_item){
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				Object it = getItem(position);
				if(convertView == null){
					convertView = getLayoutInflater().inflate(R.layout.sherpafy_drawer_list_item, null);
				}
				final ImageView imView = (ImageView) convertView.findViewById(R.id.Icon);
				TextView tv = (TextView) convertView.findViewById(R.id.Text);
				if(it.equals(R.string.sherpafy_tours)) {
					imView.
						setImageResource(R.drawable.icon_sherpafy);
					tv.setText(getString(R.string.sherpafy_tours));
				} else if(it instanceof TourInformation){
					if(selectedItem == it) {
						imView.setImageResource(R.drawable.ic_action_ok_light);
					} else {
						imView.setImageResource(R.drawable.ic_action_globus_light);
					}
					tv.setText(((TourInformation) it).getName());
				} else if(it instanceof StageInformation){
					if(customization.getSelectedStage() == it) {
						imView.setImageResource(R.drawable.ic_action_gplay_over_light);
					} else if(selectedItem == it) {
						imView.setImageResource(R.drawable.ic_action_ok_light);
					} else {
						imView.setImageDrawable(
								new StageImageDrawable(TourViewActivity.this, StageImageDrawable.MENU_COLOR, 
								(((StageInformation) it).getOrder() + 1)+"", 0));
					}
					tv.setText(((StageInformation) it).getName());
				} else {
					imView.setImageDrawable(null);
					tv.setText(it.toString());
				}
				return convertView;
			}
		};
	}
	

	public void updateActionBarTitle() {
		if(state == STATE_LOADING) {
			getSupportActionBar().setTitle(R.string.app_name);
		} else if(state == STATE_SELECT_TOUR) {
			getSupportActionBar().setTitle(R.string.sherpafy_tours);
		} else if(state == STATE_TOUR_VIEW) {
		}
		invalidateOptionsMenu();
	}
	
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == APP_EXIT_CODE) {
			getMyApplication().closeApplication(this);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	public MenuItem createMenuItem(Menu m, int id, int titleRes, int iconLight, int iconDark, int menuItemType) {
		// int r = getMyApplication().getSettings().isLightActionBar() ? iconLight : iconDark;
		int r = iconLight;
		MenuItem menuItem = m.add(0, id, 0, titleRes);
		if (r != 0) {
			menuItem.setIcon(r);
		}
		menuItem.setShowAsActionFlags(menuItemType).setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
				return onOptionsItemSelected(item);
			}
		});
		return menuItem;
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getApplication();
	}

	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		if (item.getItemId() == android.R.id.home && mDrawerToggle.isDrawerIndicatorEnabled()) {
			if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
				mDrawerLayout.closeDrawer(mDrawerList);
			} else {
				mDrawerLayout.openDrawer(mDrawerList);
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}


	public void selectMenu(Object item) {
		FragmentManager fragmentManager = getSupportFragmentManager();
		Fragment fragment = fragments.get(item);
		if (new Integer(R.string.sherpafy_tours).equals(item)) {
			if (fragment == null) {
				fragment = new SherpafySelectToursFragment();
				fragments.put(item, fragment);
			}
			state = STATE_SELECT_TOUR;
			setDrawerIndicatorVisible(true);
		} else if(item instanceof TourInformation) {
			state = STATE_TOUR_VIEW;
			if (fragment == null) {
				fragment = new SherpafyTourFragment();
				Bundle bl = new Bundle();
				bl.putString("TOUR", ((TourInformation) item).getId());
				fragment.setArguments(bl);
				fragments.put(item, fragment);
			}
			setDrawerIndicatorVisible(true);
		} else if(item instanceof StageInformation) {
			state = STATE_STAGE_OVERVIEW;
			if (fragment == null) {
				fragment = new SherpafyStageFragment();
				Bundle bl = new Bundle();
				bl.putString(SherpafyStageFragment.TOUR_PARAM, ((StageInformation) item).getTour().getId());
				bl.putInt(SherpafyStageFragment.STAGE_PARAM, ((StageInformation) item).getOrder());
				fragment.setArguments(bl);
				fragments.put(item, fragment);
			}
			setDrawerIndicatorVisible(false);
		}
		if(fragment != null) {
			fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
		}
		selectedItem = item;
		if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
			mDrawerLayout.closeDrawer(mDrawerList);
		}
		drawerAdapter.clear();
		drawerAdapter.add(R.string.sherpafy_tours);
		TourInformation selectedTour = customization.getSelectedTour();
		for(TourInformation it :  customization.getTourInformations()) {
			int insert = drawerAdapter.getCount();
			if(it == selectedTour) {
				insert = 1;
			}
			drawerAdapter.insert(it, insert++);
			if(it == selectedItem || (selectedItem instanceof StageInformation &&
					((StageInformation) selectedItem).getTour() == it)) {
				for(StageInformation st : it.getStageInformation()) {
					drawerAdapter.insert(st, insert++);
				}
			} else if(it == selectedTour) {
				StageInformation st = customization.getSelectedStage();
				if(st != null) {
					drawerAdapter.insert(st, insert++);
				}
			}
		}
		updateActionBarTitle();
	}
	
	private void setDrawerIndicatorVisible(boolean b) {
		if(mDrawerToggle.isDrawerIndicatorEnabled() != b) {
			mDrawerToggle.setDrawerIndicatorEnabled(b);
		}
	}


	public void showSelectedItem() {
		if(selectedItem != null) {
			selectMenu(selectedItem);
		} else {
			if(customization.getSelectedStage() != null) {
				selectMenu(customization.getSelectedStage());
			} else if(customization.getSelectedTour() != null) {
				selectMenu(customization.getSelectedTour());
			} else {
				selectMenu(R.string.sherpafy_tours);
			}
		}
	}

	public void showHtmlFragment(String title, String cont) {
		FragmentManager fragmentManager = getSupportFragmentManager();
		setDrawerIndicatorVisible(false);
		SherpafyHtmlFragment fragment = new SherpafyHtmlFragment();
		Bundle bl = new Bundle();
		bl.putString(SherpafyHtmlFragment.HTML, cont);
		bl.putString(SherpafyHtmlFragment.TITLE, title);
		fragment.setArguments(bl);
		fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
	}


}
