package net.osmand.plus.sherpafy;

import java.io.File;
import java.util.List;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import net.osmand.IProgress;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.DownloadIndexActivity;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.sherpafy.TourInformation.StageInformation;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;

/**
 */
public class TourViewActivity extends SherlockFragmentActivity {

	private static final int GO_TO_MAP = 1;
	private static final int SETTINGS_ID = 2;
	private static final int TOUR_ID = 3;
	private static final int STATE_TOUR_VIEW = 1; 
	private static final int STATE_LOADING = 0;
	private static final int STATE_SELECT_TOUR = -1;
	private static int state = STATE_LOADING;
	private SherpafyCustomization customization;
	ImageView img;
	TextView description;
	TextView fullDescription;
	RadioGroup stages;
	private ToggleButton collapser;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
    	if (!(getMyApplication().getAppCustomization() instanceof SherpafyCustomization)) {
            getMyApplication().setAppCustomization(new SherpafyCustomization());
        }
        customization = (SherpafyCustomization) getMyApplication().getAppCustomization();
    	setTheme(R.style.OsmandLightTheme);
        ((OsmandApplication) getApplication()).setLanguage(this);
        super.onCreate(savedInstanceState);
        if(getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT || 
        		getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
        	getSherlock().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setTitle(R.string.sherpafy_app_name);

		setContentView(R.layout.sherpafy_loading);
		if (state == STATE_LOADING) {
			getMyApplication().checkApplicationIsBeingInitialized(this, (TextView) findViewById(R.id.ProgressMessage),
					(ProgressBar) findViewById(R.id.ProgressBar), new Runnable() {
						@Override
						public void run() {
							if(customization.getSelectedTour() != null) {
								startTourView();
							} else {
								startSettings();
							}
						}
					});
		} else if(state == STATE_SELECT_TOUR ){
			state = STATE_LOADING;
			startSettings();
		} else if(state == STATE_TOUR_VIEW){
			state = STATE_LOADING;
			startTourView();
		}
		
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
				fullDescription.setText(Html.fromHtml(curTour.getFulldescription()));
				description.setText(Html.fromHtml(curTour.getShortDescription()));
				// ((TextView)findViewById(R.id.tour_name)).setText(getString(R.string.overview));
				setCollapserText(getString(R.string.overview));
				prepareBitmap(curTour.getImageBitmap());
			}
		} else {
			StageInformation st = customization.getSelectedStage();
			description.setText(Html.fromHtml(st.getDescription(), new Html.ImageGetter() {
				@Override
				public Drawable getDrawable(String s) {

					Bitmap file = customization.getSelectedTour().getImageBitmapFromPath(s);
					Drawable bmp = new BitmapDrawable(getResources(),file);
					bmp.setBounds(0,0, bmp.getIntrinsicHeight(), bmp.getIntrinsicHeight());
					return bmp;
				}

			}, null));

			fullDescription.setText(Html.fromHtml(st.getFullDescription()));
			// ((TextView)findViewById(R.id.tour_name)).setText(st.getName());
			setCollapserText(st.getName());
			prepareBitmap(st.getImageBitmap());
		}
	}


	private void setCollapserText(String t) {
		collapser.setText("  " + t);
		collapser.setTextOff("  " + t);
		collapser.setTextOn("  " + t);
	}

	private void startSettings() {
		if(state != STATE_SELECT_TOUR) {
			setTourSelectionContentView();
			state = STATE_SELECT_TOUR;
		}
		invalidateOptionsMenu();
	}
	
	private void startTourView() {
		if(state != STATE_TOUR_VIEW) {
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
			createMenuItem(menu, GO_TO_MAP, R.string.start_tour, 0, 0,/* R.drawable.ic_action_marker_light, */
					MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
			createMenuItem(menu, SETTINGS_ID, R.string.settings, R.drawable.ic_action_settings_light,
					R.drawable.ic_action_settings_dark, MenuItem.SHOW_AS_ACTION_IF_ROOM
							| MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		} else if(state == STATE_SELECT_TOUR) {
			if (customization.isTourSelected()) {
				createMenuItem(menu, TOUR_ID, R.string.default_buttons_ok, R.drawable.ic_action_ok_light,
						R.drawable.ic_action_ok_dark, MenuItem.SHOW_AS_ACTION_IF_ROOM
								| MenuItem.SHOW_AS_ACTION_WITH_TEXT);
			}
		}
		return super.onCreateOptionsMenu(menu);
	}
	
	public MenuItem createMenuItem(Menu m, int id, int titleRes, int iconLight, int iconDark, int menuItemType) {
//		int r = getMyApplication().getSettings().isLightActionBar() ? iconLight : iconDark;
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
		fullDescription = (TextView) findViewById(R.id.tour_fulldescription);
		fullDescription.setVisibility(View.VISIBLE);

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
			if (gpx != null && gpx.findPointToShow() != null) {
				WptPt p = gpx.findPointToShow();
				getMyApplication().getSettings().setMapLocationToShow(p.lat, p.lon,
						getMyApplication().getSettings().getLastKnownMapZoom(), null);
				getMyApplication().getSelectedGpxHelper().setGpxFileToDisplay(gpx);
			}
		}
		MapActivity.launchMapActivityMoveToTop(getActivity());
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

	
	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		if (item.getItemId() == GO_TO_MAP) {
			goToMap();
			return true;
		} else if (item.getItemId() == SETTINGS_ID) {
			startSettings();
			return true;
		} else if (item.getItemId() == TOUR_ID) {
			startTourView();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}


	private void setTourSelectionContentView() {
		setContentView(R.layout.sherpafy_start);
		final Button selectTour = (Button) findViewById(R.id.select_tour);
		final View accessCode = (View) findViewById(R.id.access_code);
		accessCode.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openAccessCode(false);
			}
		});
		
		if (!customization.getTourInformations().isEmpty()) {
			selectTour.setText(R.string.select_tour);
			selectTour.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					selectTourDialog();
				}
			});
		} else {
			selectTour.setText(R.string.download_tour);
			selectTour.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if(customization.getAccessCode().length() == 0) {
						openAccessCode(true);
					} else {
						startDownloadActivity();
					}
				}

			});
		}
	}
	
	protected void openAccessCode(final boolean startDownload) {
		Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.enter_access_code);
		final EditText editText = new EditText(this);
		editText.setInputType(EditorInfo.TYPE_TEXT_FLAG_CAP_CHARACTERS);
		LinearLayout ll = new LinearLayout(this);
		ll.setPadding(5, 3, 5, 0);
		ll.addView(editText, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		builder.setView(ll);
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String acCode = editText.getText().toString();
				if(!customization.setAccessCode(acCode)) {
					Toast.makeText(getActivity(), R.string.access_code_is_not_valid, Toast.LENGTH_LONG).show();
					return;
				}
				if(startDownload) {
					startDownloadActivity();
				}
			}
		});
		builder.create().show();
	}


	private void startDownloadActivity() {
		final Intent download = new Intent(this, DownloadIndexActivity.class);
		download.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(download);
	}
	
	private void selectTourDialog() {
		// creating alert dialog with multiple tours to select
		AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
		final List<TourInformation> tours = customization.getTourInformations();

		final String[] tourNames = new String[tours.size() + 1];
		// creating list of tour names to select
		for (int i = 0; i < tourNames.length - 1; i++) {
			tourNames[i] = tours.get(i).getName();
		}
		tourNames[tourNames.length - 1] = getString(R.string.download_more);
		int ch = -1;
		if (customization.getSelectedTour() != null) {
			for (int i = 0; i < tourNames.length - 1; i++) {
				if (customization.getSelectedTour().equals(tours.get(i))) {
					ch = i;
					break;
				}
			}
		}

		adb.setSingleChoiceItems(tourNames, ch, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				if (i == tourNames.length - 1) {
					startDownloadActivity();
				} else {
					selectTourAsync(tours.get(i));
					dialogInterface.dismiss();
				}
			}
		});

		adb.setTitle(R.string.select_tour);
		adb.setNegativeButton(R.string.default_buttons_cancel, null);
		adb.show();
	}


	private Activity getActivity() {
		return this;
	}

	private void selectTourAsync(final TourInformation tour){
		new AsyncTask<TourInformation, Void, Void>(){
			private ProgressDialog dlg;

			protected void onPreExecute() {
				dlg = new ProgressDialog(getActivity());
				dlg.setTitle(R.string.selecting_tour_progress);
				dlg.setMessage(getString(R.string.indexing_tour, tour == null ? "" : tour.getName()));
				dlg.show();
			};

			@Override
			protected Void doInBackground(TourInformation... params) {
				//if tour is already selected - do nothing
				if (customization.getSelectedTour()!= null){
					if (customization.getSelectedTour().equals(params[0])){
						return null;
					}
				}
				customization.selectTour(params[0], IProgress.EMPTY_PROGRESS);
				return null;
			}

			protected void onPostExecute(Void result) {
				//to avoid illegal argument exception when rotating phone during loading
				try {
					dlg.dismiss();
				} catch (Exception ex){
					ex.printStackTrace();
				}
				startTourView();
			};
		}.execute(tour);
	}

}
