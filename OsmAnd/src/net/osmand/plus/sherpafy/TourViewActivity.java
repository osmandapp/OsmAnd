package net.osmand.plus.sherpafy;

import java.util.List;
import java.util.WeakHashMap;

import android.view.KeyEvent;
import android.widget.*;
import net.osmand.IProgress;
import net.osmand.data.LatLon;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.activities.DownloadIndexActivity;
import net.osmand.plus.sherpafy.TourInformation.StageFavorite;
import net.osmand.plus.sherpafy.TourInformation.StageInformation;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
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
import android.widget.AdapterView.OnItemClickListener;

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
	private static final int STATE_DETAILED_OVERVIEW = 4;
	private static final int STATE_DETAILED_INSTRUCTIONS = 5;
	private static final int STAGE_GALLERY = 6;
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

		displaySize = new Point(getWindowManager().getDefaultDisplay().getWidth(), getWindowManager().getDefaultDisplay().getHeight());
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_navigation_drawer_light,
				R.string.default_buttons_other_actions, R.string.close);
		if (getMyApplication().isApplicationInitializing()) {
			FragmentManager fragmentManager = getSupportFragmentManager();
			fragmentManager.beginTransaction().replace(R.id.content_frame, new SherpafyLoadingFragment()).commit();
		} else if (state == STATE_DETAILED_INSTRUCTIONS) {
			TourInformation tour = (TourInformation) selectedItem;
			showDetailedInstructions(getString(R.string.sherpafy_instructions), tour.getInstructions());
		} else if (state == STATE_DETAILED_OVERVIEW) {
			TourInformation tour = (TourInformation) selectedItem;
			showDetailedInstructions(getString(R.string.sherpafy_overview), tour.getFulldescription());
		} else {
			showSelectedItem();
		}
	}

	@Override
	public void onBackPressed() {
		if (state == STATE_SELECT_TOUR) {
			super.onBackPressed();
		} else if (state == STATE_TOUR_VIEW) {
			selectMenu(R.string.sherpafy_tours);
		} else if (state == STATE_STAGE_OVERVIEW) {
			SherpafyStageFragment fragment = (SherpafyStageFragment) getSupportFragmentManager().findFragmentByTag(String.valueOf(state));
			if (fragment != null) {
				fragment.onBackPressed();
			}
		} else if (state == STATE_DETAILED_OVERVIEW || state == STATE_DETAILED_INSTRUCTIONS) {
			showSelectedItem();
		}
	}

	private ArrayAdapter<Object> setupAdapter() {
		return new ArrayAdapter<Object>(this, R.layout.sherpafy_drawer_list_item) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				Object it = getItem(position);
				if (convertView == null) {
					convertView = getLayoutInflater().inflate(R.layout.sherpafy_drawer_list_item, null);
				}
				final ImageView imView = (ImageView) convertView.findViewById(R.id.Icon);
				TextView tv = (TextView) convertView.findViewById(R.id.Text);
				if (it.equals(R.string.sherpafy_tours)) {
					imView.
							setImageResource(R.drawable.icon_sherpafy);
					tv.setText(getString(R.string.sherpafy_tours));
				} else if (it instanceof TourInformation) {
					if (selectedItem == it) {
						imView.setImageResource(R.drawable.ic_action_ok_light);
					} else {
						imView.setImageResource(R.drawable.ic_action_globus_light);
					}
					tv.setText(((TourInformation) it).getName());
				} else if (it instanceof StageInformation) {
					if (customization.getSelectedStage() == it) {
						imView.setImageResource(R.drawable.ic_action_gplay_over_light);
					} else if (selectedItem == it) {
						imView.setImageResource(R.drawable.ic_action_ok_light);
					} else {
						imView.setImageDrawable(
								new StageImageDrawable(TourViewActivity.this, StageImageDrawable.MENU_COLOR,
										(((StageInformation) it).getOrder() + 1) + "", 0));
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
		if (state == STATE_LOADING) {
			getSupportActionBar().setTitle(R.string.app_name);
		} else if (state == STATE_SELECT_TOUR) {
			getSupportActionBar().setTitle(R.string.sherpafy_tours);
		} else if (state == STATE_TOUR_VIEW) {
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

	public MenuItem createMenuItem(Menu m, int id, int titleRes, int iconLight, int iconDark, int menuItemType,
								   final OnMenuItemClickListener listener) {
		// int r = getMyApplication().getSettings().isLightActionBar() ? iconLight : iconDark;
		int r = iconLight;
		MenuItem menuItem = m.add(0, id, 0, titleRes);
		if (r != 0) {
			menuItem.setIcon(r);
		}
		menuItem.setShowAsActionFlags(menuItemType).setOnMenuItemClickListener(listener);
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
		} else if (item instanceof TourInformation) {
			state = STATE_TOUR_VIEW;
			if (fragment == null) {
				fragment = new SherpafyTourFragment();
				Bundle bl = new Bundle();
				bl.putString("TOUR", ((TourInformation) item).getId());
				fragment.setArguments(bl);
				fragments.put(item, fragment);
			}
			setDrawerIndicatorVisible(true);
		} else if (item instanceof StageInformation) {
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
		if (fragment != null) {
			fragmentManager.beginTransaction().replace(R.id.content_frame, fragment, String.valueOf(state)).commit();
		}
		selectedItem = item;
		if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
			mDrawerLayout.closeDrawer(mDrawerList);
		}
		drawerAdapter.clear();
		drawerAdapter.add(R.string.sherpafy_tours);
		TourInformation selectedTour = customization.getSelectedTour();
		for (TourInformation it : customization.getTourInformations()) {
			int insert = drawerAdapter.getCount();
			if (it == selectedTour) {
				insert = 1;
			}
			drawerAdapter.insert(it, insert++);
			if (it == selectedItem || (selectedItem instanceof StageInformation &&
					((StageInformation) selectedItem).getTour() == it)) {
				for (StageInformation st : it.getStageInformation()) {
					drawerAdapter.insert(st, insert++);
				}
			} else if (it == selectedTour) {
				StageInformation st = customization.getSelectedStage();
				if (st != null) {
					drawerAdapter.insert(st, insert++);
				}
			}
		}
		updateActionBarTitle();
	}

	private void setDrawerIndicatorVisible(boolean b) {
		if (mDrawerToggle.isDrawerIndicatorEnabled() != b) {
			mDrawerToggle.setDrawerIndicatorEnabled(b);
		}
	}


	public void showSelectedItem() {
		if (selectedItem != null) {
			selectMenu(selectedItem);
		} else {
			if (customization.getSelectedStage() != null) {
				selectMenu(customization.getSelectedStage());
			} else if (customization.getSelectedTour() != null) {
				selectMenu(customization.getSelectedTour());
			} else {
				selectMenu(R.string.sherpafy_tours);
			}
		}
	}


	public void showFavoriteFragment(StageInformation stage, StageFavorite sf) {
		FragmentManager fragmentManager = getSupportFragmentManager();
		setDrawerIndicatorVisible(false);
		SherpafyFavoriteFragment fragment = new SherpafyFavoriteFragment();
		Bundle bl = new Bundle();
		bl.putInt(SherpafyFavoriteFragment.STAGE_PARAM, stage.getOrder());
		bl.putString(SherpafyFavoriteFragment.TOUR_PARAM, stage.getTour().getId());
		bl.putInt(SherpafyFavoriteFragment.FAV_PARAM, sf.getOrder());
		fragment.setArguments(bl);
		fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
	}

	public void showDetailedInstructions(String title, String cont) {
		state = STATE_DETAILED_INSTRUCTIONS;
		showHtmlFragment(title, cont);
	}

	public void showDetailedOverview(String title, String cont) {
		state = STATE_DETAILED_OVERVIEW;
		showHtmlFragment(title, cont);
	}

	private void showHtmlFragment(String title, String cont) {
		FragmentManager fragmentManager = getSupportFragmentManager();
		setDrawerIndicatorVisible(false);
		SherpafyHtmlFragment fragment = new SherpafyHtmlFragment();
		Bundle bl = new Bundle();
		bl.putString(SherpafyHtmlFragment.HTML, cont);
		bl.putString(SherpafyHtmlFragment.TITLE, title);
		fragment.setArguments(bl);
		fragmentManager.beginTransaction().replace(R.id.content_frame, fragment, String.valueOf(STATE_DETAILED_OVERVIEW)).commit();
	}


	public void startDownloadActivity() {
		final Intent download = new Intent(this, DownloadIndexActivity.class);
		download.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(download);
	}

	public void goToMap(LatLon location) {
		if (location != null) {
			getMyApplication().getSettings().setMapLocationToShow(location.getLatitude(), location.getLongitude(), 16, null);
		}
		Intent newIntent = new Intent(this, customization.getMapActivity());
		newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		this.startActivityForResult(newIntent, 0);
	}

	public void startStage(final StageInformation stage) {
		if (stage != customization.getSelectedStage() && customization.getSelectedStage() != null) {
			Builder bld = new AlertDialog.Builder(this);
			bld.setMessage(R.string.start_new_stage);
			bld.setPositiveButton(R.string.default_buttons_yes, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					runStage(stage.getTour(), stage, customization.getSelectedStage() != stage);
				}
			});
			bld.setNegativeButton(R.string.default_buttons_no, null);
			bld.show();
		} else {
			runStage(stage.getTour(), stage, customization.getSelectedStage() != stage);
		}
	}


	public void startTour(final TourInformation tour) {
		if (tour != customization.getSelectedTour() && customization.getSelectedTour() != null) {
			Builder bld = new AlertDialog.Builder(this);
			bld.setMessage(R.string.start_new_stage);
			bld.setPositiveButton(R.string.default_buttons_yes, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					startTourImpl(tour);
				}
			});
			bld.setNegativeButton(R.string.default_buttons_no, null);
			bld.show();
		} else {
			startTourImpl(tour);
		}
	}

	private void startTourImpl(TourInformation tour) {
		StageInformation stage;
		if (!tour.getStageInformation().isEmpty()) {
			if (tour != customization.getSelectedTour() || customization.getSelectedStage() == null) {
				stage = tour.getStageInformation().get(0);
			} else {
				stage = customization.getSelectedStage();
			}
			runStage(tour, stage, customization.getSelectedTour() != tour);
		}
	}


	private void runStage(TourInformation tour, StageInformation stage, boolean startOver) {
		WptPt point = null;
		GPXFile gpx = null;
		customization.selectTour(tour, IProgress.EMPTY_PROGRESS);
		customization.selectStage(stage, IProgress.EMPTY_PROGRESS);
		if (customization.getSelectedStage() != null) {
			gpx = customization.getSelectedStage().getGpx();
			List<SelectedGpxFile> sgpx = getMyApplication().getSelectedGpxHelper().getSelectedGPXFiles();
			if (gpx == null && sgpx.size() > 0) {
				getMyApplication().getSelectedGpxHelper().clearAllGpxFileToShow();
			} else if (sgpx.size() != 1 || sgpx.get(0).getGpxFile() != gpx) {
				getMyApplication().getSelectedGpxHelper().clearAllGpxFileToShow();
				if (gpx != null && gpx.findPointToShow() != null) {
					point = gpx.findPointToShow();
					getMyApplication().getSelectedGpxHelper().setGpxFileToDisplay(gpx);
				}
			}
		}
		WptPt lp = gpx.getLastPoint();
		if (lp != null) {
			TargetPointsHelper targetPointsHelper = getMyApplication().getTargetPointsHelper();
			targetPointsHelper.navigateToPoint(new LatLon(lp.lat, lp.lon), true, -1, lp.name);
			getMyApplication().getSettings().navigateDialog();
		}
		if (startOver && point != null) {
			goToMap(new LatLon(point.lat, point.lon));
		} else {
			goToMap(null);
		}
	}

}
