package net.osmand.plus.sherpafy;

import java.util.List;

import net.osmand.IProgress;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.DownloadIndexActivity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.actionbarsherlock.app.SherlockFragmentActivity;

/**
 */
public class SherpafyStartActivity extends SherlockFragmentActivity {

    private SherpafyCustomization customization;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
		//Initialization
    	Intent intent = getIntent();
    	if(intent != null && !intent.hasExtra("SETTINGS") && customization.getSelectedTour() != null) {
    		super.onCreate(savedInstanceState);
    		Intent nintent = new Intent(getApplicationContext(), TourViewActivity.class);
			startActivity(nintent);
			finish();
    		return;
    	}
    	setTheme(R.style.OsmandLightTheme);
        ((OsmandApplication) getApplication()).setLanguage(this);
        super.onCreate(savedInstanceState);
        getSherlock().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setTitle(R.string.sherpafy_app_name);
        
        
        
        super.setContentView(R.layout.sherpafy_start);
        if (!(getMyApplication().getAppCustomization() instanceof SherpafyCustomization)) {
            getMyApplication().setAppCustomization(new SherpafyCustomization());
        }
        customization = (SherpafyCustomization) getMyApplication().getAppCustomization();

		ProgressDialog startProgressDialog = new ProgressDialog(this);
		getMyApplication().checkApplicationIsBeingInitialized(this, startProgressDialog);
        setContentView();
        
        // FIXME is this needed?
//        if (customization.getTourInformations().isEmpty()) {
//			customization.onIndexingFiles(IProgress.EMPTY_PROGRESS, new ConcurrentHashMap<String, String>());
//		}
	}

    private OsmandApplication getMyApplication() {
        return (OsmandApplication) getApplication();
    }

	private void setContentView() {
		final Button selectTour = (Button) findViewById(R.id.select_tour);
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

		Button downloadTour = (Button) findViewById(R.id.download_tour);
		downloadTour.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent download = new Intent(v.getContext(), DownloadIndexActivity.class);
				download.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				v.getContext().startActivity(download);
			}
		});
	}


	private SherpafyStartActivity getActivity() {
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

				Intent intent = new Intent(getApplicationContext(), TourViewActivity.class);
				startActivity(intent);
				finish();
			};
		}.execute(tour);
	}
}
