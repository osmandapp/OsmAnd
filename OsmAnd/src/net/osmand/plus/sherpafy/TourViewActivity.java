package net.osmand.plus.sherpafy;

import java.util.List;

import net.osmand.IProgress;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.DownloadIndexActivity;
import net.osmand.plus.activities.MapActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
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
	private static final int STATE_TOUR_VIEW = 1; 
	private static final int STATE_LOADING = 0;
	private static final int STATE_SELECT_TOUR = -1;
	int state = 0;
	private SherpafyCustomization customization;
	ImageView img;
	TextView description;
	TextView fullDescription;
	List<TourInformation.StageInformation> stagesInfo;
	TourInformation curTour;
	RadioGroup stages;
	

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
		state = STATE_LOADING;
		getMyApplication().checkApplicationIsBeingInitialized(this, (TextView) findViewById(R.id.ProgressMessage),
				(ProgressBar) findViewById(R.id.ProgressBar), new Runnable() {
					@Override
					public void run() {
						setMainContent();
					}
				});
		
	}
	
	
	private void setTourInfoContent() {
		setContentView(R.layout.sherpafy_tour_info);

		ToggleButton collapser = (ToggleButton) findViewById(R.id.collapse);
		stages = (RadioGroup) findViewById(R.id.stages);
		stages.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup radioGroup, int i) {
				if (i == 0) {
					customization.selectStage(null, IProgress.EMPTY_PROGRESS);
					fullDescription.setText(curTour.getFulldescription());
					description.setText((curTour.getShortDescription()));
					((TextView)findViewById(R.id.tour_name)).setText(getString(R.string.overview));
					prepareBitmap();
				} else {
					customization.selectStage(stagesInfo.get(i - 1), IProgress.EMPTY_PROGRESS);
					description.setText(stagesInfo.get(i - 1).getDescription());
					fullDescription.setText(stagesInfo.get(i - 1).getFullDescription());
					((TextView)findViewById(R.id.tour_name)).setText(stagesInfo.get(i - 1).getName());
				}
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

	private void startSettings() {
		if(state != STATE_SELECT_TOUR) {
			setTourSelectionContentView();
			state = STATE_SELECT_TOUR;
		}
	}
	
	private void setMainContent() {
		if(customization.getSelectedTour() != null) {
			if(state != STATE_TOUR_VIEW) {
				setTourInfoContent();
				state = STATE_TOUR_VIEW;
			}
			getSupportActionBar().setTitle(customization.getSelectedTour().getName());
			updateTourView();
		} else {
			startSettings();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
		createMenuItem(menu, GO_TO_MAP, R.string.start_tour, 
				0, 0,/*R.drawable.ic_action_marker_light,*/
				MenuItem.SHOW_AS_ACTION_ALWAYS| MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		createMenuItem(menu, SETTINGS_ID, R.string.osmo_share_session, 
				R.drawable.ic_action_settings_light, R.drawable.ic_action_settings_dark,
				MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
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
		curTour = customization.getSelectedTour();
		stagesInfo = curTour.getStageInformation();

		img = (ImageView) findViewById(R.id.tour_image);
		description = (TextView) findViewById(R.id.tour_description);
		description.setVisibility(View.VISIBLE);
		fullDescription = (TextView) findViewById(R.id.tour_fulldescription);
		fullDescription.setVisibility(View.VISIBLE);
		Button start_tour = (Button) findViewById(R.id.start_tour);

		// in case of reloading view - remove all previous radio buttons
		stages.removeAllViews();

		start_tour.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				goToMap();
			}
		});
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

		TourInformation.StageInformation curStage = customization.getSelectedStage();
		if (curStage == null) {
			stages.check(0);
			description.setText(curTour.getShortDescription());
			fullDescription.setText(curTour.getFulldescription());
			prepareBitmap();
		} else {
			int i;
			for (i = 1; i < count; i++) {
				if (curStage.equals(stagesInfo.get(i - 1)))
					break;
			}
			if (i != count) {
				stages.check(i);
			} else {
				stages.check(0);
			}
		}
	}
	
	private void goToMap() {
		MapActivity.launchMapActivityMoveToTop(getActivity());
		// TODO select GPX
	}

	private void prepareBitmap() {
		final Bitmap imageBitmap = curTour.getImageBitmap();
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
		} else {
			return super.onOptionsItemSelected(item);
		}
	}


	private void setTourSelectionContentView() {
		setContentView(R.layout.sherpafy_start);
		final Button selectTour = (Button) findViewById(R.id.select_tour);
		final Button downloadTour = (Button) findViewById(R.id.download_tour);
		int width = Math.max(downloadTour.getWidth(), selectTour.getWidth());
		downloadTour.setWidth(width);
		selectTour.setWidth(width);
		selectTour.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// creating alert dialog with multiple tours to select
				AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
				final List<TourInformation> tours = customization.getTourInformations();
				if (customization.getSelectedTour() != null) {
					String[] tourNames = new String[tours.size()];
					// creating list of tour names to select
					for (int i = 0; i < tours.size(); i++) {
						tourNames[i] = tours.get(i).getName();
					}
					int i;
					for (i = 0; i < tours.size(); i++) {
						if (customization.getSelectedTour().equals(tours.get(i))) {
							break;
						}
					}

					adb.setSingleChoiceItems(tourNames, i, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							selectTour(tours.get(i));
							dialogInterface.dismiss();
						}
					});
				} else {
					String[] tourNames = new String[tours.size() + 1];
					tourNames[0] = getString(R.string.none);
					for (int i = 1; i < tours.size() + 1; i++) {
						tourNames[i] = tours.get(i - 1).getName();
					}
					adb.setSingleChoiceItems(tourNames, 0, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							if (i == 0) {
								return;
							}
							selectTour(tours.get(i - 1));
							dialogInterface.dismiss();
						}
					});
				}

				adb.setTitle(R.string.select_tour);
				adb.setNegativeButton(R.string.default_buttons_cancel, null);
				adb.show();
			}
		});

		downloadTour.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent download = new Intent(v.getContext(), DownloadIndexActivity.class);
				download.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				v.getContext().startActivity(download);
			}
		});
	}


	private Activity getActivity() {
		return this;
	}

	private void selectTour(final TourInformation tour){
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
				customization.selectTour(params[0], IProgress.EMPTY_PROGRESS);
				return null;
			}

			protected void onPostExecute(Void result) {
				dlg.dismiss();
				setTourInfoContent();
			};
		}.execute(tour);
	}

}
