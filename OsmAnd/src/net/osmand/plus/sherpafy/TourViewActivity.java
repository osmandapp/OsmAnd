package net.osmand.plus.sherpafy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.osmand.IProgress;
import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.DownloadIndexActivity;
import net.osmand.plus.sherpafy.TourInformation.StageInformation;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;

/**
 */
public class TourViewActivity extends SherlockFragmentActivity {

	private static final int ACTION_GO_TO_MAP = 1;
	private static final int ACTION_TOUR_ID = 3;
	private static final int ACTION_SHARE = 4;
	
	private static final int STATE_LOADING = -1;
	private static final int STATE_TOUR_VIEW = 1;
	private static final int STATE_SELECT_TOUR = 2;
	private static int state = STATE_LOADING;
	
	
	public static final int APP_EXIT_CODE = 4;
	public static final String APP_EXIT_KEY = "APP_EXIT_KEY";

	
	ImageView img;
	TextView description;
	LinearLayout fullDescriptionView;
	RadioGroup stages;
	private ToggleButton collapser;
	private Set<TourInformation> currentTourInformations = new HashSet<TourInformation>();
	
	private SherpafyCustomization customization;
	private Point displaySize;
	private ActionBarDrawerToggle mDrawerToggle;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ArrayAdapter<Object> drawerAdapter;
	private SherpafyToursFragment toursFragment;

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
		drawerAdapter = new ArrayAdapter<Object>(this, android.R.layout.simple_list_item_1){
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				Object it = getItem(position);
				if(convertView == null){
					convertView = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, null);
				}
				if(position == 0) {
					((TextView) convertView).setText(R.string.sherpafy_tours);
				} else {
					((TextView) convertView).setText(it.toString());
				}
				return convertView;
			}
		};
		mDrawerList.setAdapter(drawerAdapter);
		// Set the list's click listener
		mDrawerList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				selectMenu(position, drawerAdapter.getItem(position));
			}
		});

		displaySize = new Point();
		getWindowManager().getDefaultDisplay().getSize(displaySize);
		FragmentManager fragmentManager = getSupportFragmentManager();
		fragmentManager.beginTransaction().replace(R.id.content_frame, new SherpafyLoadingFragment(getMyApplication()))
				.commit();
		
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_navigation_drawer_light,
				R.string.default_buttons_other_actions, R.string.close) {

			@Override
			public void onDrawerOpened(View view) {
				super.onDrawerOpened(view);
				invalidateOptionsMenu();
			}

			@Override
			public void onDrawerClosed(View view) {
				super.onDrawerOpened(view);
				invalidateOptionsMenu();
			}
		};
	}
	
	protected void selectMenu(int position, Object item) {
		FragmentManager fragmentManager = getSupportFragmentManager();
		if (position == 0) {
			if (toursFragment == null) {
				toursFragment = new SherpafyToursFragment(getMyApplication());
			}
			fragmentManager.beginTransaction().replace(R.id.content_frame, toursFragment).commit();
			state = STATE_SELECT_TOUR;
		}
		updateActionBarTitle();
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
	
	
	public void loadingFinished() {
		drawerAdapter.clear();
		drawerAdapter.add(getString(R.string.sherpafy_tours));
		if(customization.getSelectedTour() != null) {
			drawerAdapter.add(customization.getSelectedTour());
			if(customization.getSelectedStage() != null) {
				drawerAdapter.add(customization.getSelectedStage());
			}
		}
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

	private ImageGetter getImageGetter(final View v) {
		return new Html.ImageGetter() {
			@Override
			public Drawable getDrawable(String s) {
				Bitmap file = customization.getSelectedTour().getImageBitmapFromPath(s);
				v.setTag(file);
				Drawable bmp = new BitmapDrawable(getResources(), file);
				// if image is thicker than screen - it may cause some problems, so we need to scale it
				int imagewidth = bmp.getIntrinsicWidth();
				if (displaySize.x - 1 > imagewidth) {
					bmp.setBounds(0, 0, bmp.getIntrinsicWidth(), bmp.getIntrinsicHeight());
				} else {
					double scale = (double) (displaySize.x - 1) / imagewidth;
					bmp.setBounds(0, 0, (int) (scale * bmp.getIntrinsicWidth()),
							(int) (scale * bmp.getIntrinsicHeight()));
				}

				return bmp;
			}

		};
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

		// TourInformation selectedTour = customization.getSelectedTour();
		// if (selectedTour == null || currentTourInformations.contains(selectedTour)) {
		// for (TourInformation i : customization.getTourInformations()) {
		// if (!currentTourInformations.contains(i)) {
		// currentTourInformations.add(i);
		// selectedTour = i;
		// }
		// }
		// if (selectedTour != null) {
		// selectTourAsync(selectedTour);
		// // startTourView();
		// }
		// }
	}

	private void setTourInfoContent() {
		setContentView(R.layout.sherpafy_tour_info);

		collapser = (ToggleButton) findViewById(R.id.collapse);
		stages = (RadioGroup) findViewById(R.id.stages);
		stages.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup radioGroup, int i) {
				if (i == 0) {
					customization.selectStage(null, IProgress.EMPTY_PROGRESS);
				} else {
					final StageInformation st = customization.getSelectedTour().getStageInformation().get(i - 1);
					customization.selectStage(st, IProgress.EMPTY_PROGRESS);
				}
				updateTourContentView();
			}

		});

		collapser.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					stages.setVisibility(View.VISIBLE);
				} else {
					stages.setVisibility(View.GONE);
				}
			}
		});
	}

	private void updateTourContentView() {
		if (customization.getSelectedStage() == null) {
			if (customization.getSelectedTour() != null) {
				TourInformation curTour = customization.getSelectedTour();
				description.setText(Html.fromHtml(curTour.getShortDescription(), getImageGetter(description), null));
				setFullDescriptions(curTour.getFulldescription());
				// ((TextView)findViewById(R.id.tour_name)).setText(getString(R.string.overview));
				setCollapserText(getString(R.string.overview));
				prepareBitmap(curTour.getImageBitmap());
			}
		} else {
			StageInformation st = customization.getSelectedStage();
			description.setText(Html.fromHtml(st.getShortDescription(), getImageGetter(description), null));
			setFullDescriptions(st.getFullDescription());

			// ((TextView)findViewById(R.id.tour_name)).setText(st.getName());
			setCollapserText(st.getName());
			prepareBitmap(st.getImageBitmap());
		}
	}

	private void setFullDescriptions(String fulldescription) {
		List<String> list = new ArrayList<String>();
		if (fulldescription.length() > 0) {
			int i = 0;
			while ((i = fulldescription.indexOf("<img", 1)) != -1) {
				list.add(fulldescription.substring(0, i));
				fulldescription = fulldescription.substring(i);
			}
		}
		list.add(fulldescription);
		fullDescriptionView.removeAllViews();
		for (int i = 0; i < list.size(); i++) {
			final TextView tv = new TextView(this);
			tv.setGravity(Gravity.CENTER_HORIZONTAL);
			tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
			tv.setPadding(0, 3, 0, 3);
			tv.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			addOnClickListener(tv);
			tv.setText(Html.fromHtml(list.get(i), getImageGetter(tv), null));
			fullDescriptionView.addView(tv);
		}

	}

	private void addOnClickListener(final TextView tv) {
		tv.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (v.getTag() instanceof Bitmap) {
					final AccessibleAlertBuilder dlg = new AccessibleAlertBuilder(getActivity());
					dlg.setPositiveButton(R.string.default_buttons_ok, null);
					ScrollView sv = new ScrollView(getActivity());
					ImageView img = new ImageView(getActivity());
					img.setImageBitmap((Bitmap) tv.getTag());
					sv.addView(img);
					dlg.setView(sv);
					dlg.show();
				}
			}
		});
	}

	private void setCollapserText(String t) {
		collapser.setText("  " + t);
		collapser.setTextOff("  " + t);
		collapser.setTextOn("  " + t);
	}


	private void startTourView() {
		if (state != STATE_TOUR_VIEW) {
			setTourInfoContent();
			state = STATE_TOUR_VIEW;
		}
		getSupportActionBar().setTitle(customization.getSelectedTour().getName());
		updateTourView();
		invalidateOptionsMenu();
	}


	@Override
	public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
		if (state == STATE_TOUR_VIEW) {
//			createMenuItem(menu, ACTION_GO_TO_MAP, R.string.start_tour, 0, 0,/* R.drawable.ic_action_marker_light, */
//					MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
//			createMenuItem(menu, ACTION_SETTINGS_ID, R.string.settings, R.drawable.ic_action_settings_light,
//					R.drawable.ic_action_settings_dark, MenuItem.SHOW_AS_ACTION_IF_ROOM
//							| MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		} else if (state == STATE_SELECT_TOUR) {
			if (customization.isTourSelected()) {
				createMenuItem(menu, ACTION_TOUR_ID, R.string.default_buttons_ok, R.drawable.ic_action_ok_light,
						R.drawable.ic_action_ok_dark, MenuItem.SHOW_AS_ACTION_IF_ROOM
								| MenuItem.SHOW_AS_ACTION_WITH_TEXT);
			}
		} 
		return super.onCreateOptionsMenu(menu);
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

	private void updateTourView() {
		TourInformation curTour = customization.getSelectedTour();
		List<StageInformation> stagesInfo = curTour.getStageInformation();

		img = (ImageView) findViewById(R.id.tour_image);
		description = (TextView) findViewById(R.id.tour_description);
		description.setVisibility(View.VISIBLE);
		addOnClickListener(description);
		fullDescriptionView = (LinearLayout) findViewById(R.id.tour_fulldescription);
		fullDescriptionView.setVisibility(View.VISIBLE);

		// in case of reloading view - remove all previous radio buttons
		stages.removeAllViews();

		// get count of radio buttons
		final int count = stagesInfo.size() + 1;
		final RadioButton[] rb = new RadioButton[count];

		rb[0] = new RadioButton(this);
		rb[0].setId(0);
		stages.addView(rb[0]);
		rb[0].setText(getString(R.string.overview));
		// add radio buttons to view
		for (int i = 1; i < count; i++) {
			rb[i] = new RadioButton(this);
			rb[i].setId(i);
			stages.addView(rb[i]);
			rb[i].setText(stagesInfo.get(i - 1).getName());
		}

		StageInformation stage = customization.getSelectedStage();

		int i = 0;
		if (stage != null) {
			for (i = 1; i < count; i++) {
				if (stage == stagesInfo.get(i - 1))
					break;
			}
		}
		if (i != count) {
			stages.check(i);
		} else {
			stages.check(0);
		}
	}

	private void goToMap() {
		if (customization.getSelectedStage() != null) {
			GPXFile gpx = customization.getSelectedStage().getGpx();
			List<SelectedGpxFile> sgpx = getMyApplication().getSelectedGpxHelper().getSelectedGPXFiles();
			if (gpx == null && sgpx.size() > 0) {
				getMyApplication().getSelectedGpxHelper().clearAllGpxFileToShow();
			} else if (sgpx.size() != 1 || sgpx.get(0).getGpxFile() != gpx) {
				getMyApplication().getSelectedGpxHelper().clearAllGpxFileToShow();
				if (gpx != null && gpx.findPointToShow() != null) {
					WptPt p = gpx.findPointToShow();
					getMyApplication().getSettings().setMapLocationToShow(p.lat, p.lon, 16, null);
					getMyApplication().getSelectedGpxHelper().setGpxFileToDisplay(gpx);
				}
			}
		}
		Intent newIntent = new Intent(this, customization.getMapActivity());
		newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		this.startActivityForResult(newIntent, 0);
	}

	private void prepareBitmap(Bitmap imageBitmap) {
		if (imageBitmap != null) {
			img.setImageBitmap(imageBitmap);
			img.setAdjustViewBounds(true);
			img.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			img.setCropToPadding(true);
			img.setVisibility(View.VISIBLE);
		} else {
			img.setVisibility(View.GONE);
		}
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getApplication();
	}

	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
				mDrawerLayout.closeDrawer(mDrawerList);
			} else {
				mDrawerLayout.openDrawer(mDrawerList);
			}
			return true;
		} else if (item.getItemId() == ACTION_GO_TO_MAP) {
			goToMap();
			return true;
		} else if (item.getItemId() == ACTION_TOUR_ID) {
			startTourView();
			return true;
		} else {
			return true;
			// return super.onOptionsItemSelected(item);
		}
	}

	private Activity getActivity() {
		return this;
	}

}
