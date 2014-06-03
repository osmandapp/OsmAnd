package net.osmand.plus.sherpafy;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import net.osmand.IProgress;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.ProgressDialogImplementation;
import net.osmand.plus.R;
import net.osmand.plus.activities.DownloadIndexActivity;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Barsik on 30.05.2014.
 */
public class SherpafyStartActivity extends SherlockFragmentActivity {

    private SherpafyCustomization customization;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
		//Initialization
        ((OsmandApplication) getApplication()).applyTheme(this);
        super.onCreate(savedInstanceState);
        getSherlock().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Sherpafy");

        if (!(getMyApplication().getAppCustomization() instanceof SherpafyCustomization)) {
            getMyApplication().setAppCustomization(new SherpafyCustomization());
        }

        customization = (SherpafyCustomization) getMyApplication().getAppCustomization();


		ProgressDialog startProgressDialog = new ProgressDialog(this);
		getMyApplication().checkApplicationIsBeingInitialized(this, startProgressDialog);

		if (startProgressDialog.isShowing())
		{
			startProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialogInterface) {
					onStartDialogClose();
				}
			});
		}
		else {
			onStartDialogClose();
		}
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private OsmandApplication getMyApplication() {
        return (OsmandApplication) getApplication();
    }

	private void updateDefaultView(){

		final Button selectTour = (Button) findViewById(R.id.select_tour);
		selectTour.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//creating alert dialog with multiple tours to select
				AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
				final List<TourInformation> tours = customization.getTourInformations();
				if (customization.getSelectedTour() != null)
				{
					String[] tour_names = new String[tours.size()];
					//creating list of tour names to select
					for (int i =0; i < tours.size();i++){
						tour_names[i] = tours.get(i).getName();
					}
					int cur_tour_ind;
					for (cur_tour_ind =0; cur_tour_ind<tours.size();cur_tour_ind++)
					{
						if (customization.getSelectedTour().equals(tours.get(cur_tour_ind))){break;}
					}

					adb.setSingleChoiceItems(tour_names,cur_tour_ind,new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							selectTour(tours.get(i));
							dialogInterface.dismiss();
						}
					});
				} else{
					String[] tour_names = new String[tours.size()+1];
					tour_names[0] = "None";
					for (int i =1; i < tours.size()+1;i++){
						tour_names[i] = tours.get(i-1).getName();
					}
					adb.setSingleChoiceItems(tour_names,0,new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							if (i==0){return;}
							selectTour(tours.get(i-1));
							dialogInterface.dismiss();
						}
					});
				}

				adb.setTitle("Select tour");
				adb.setNegativeButton("Cancel",null);
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

	private void onStartDialogClose(){
		if (customization.getTourInformations().isEmpty())
		{
			customization.onIndexingFiles( IProgress.EMPTY_PROGRESS, new ConcurrentHashMap<String, String>() );
		}

		Intent start_intent = getIntent();

		if ( start_intent.getStringExtra("settings") != null || customization.getSelectedTour() == null ) {
			super.setContentView(R.layout.sherpafy_start);
			updateDefaultView();
		} else {
			Intent intent = new Intent(getApplicationContext(),TourViewActivity.class);
			startActivity(intent);
			finish();
		}
	}

	private SherpafyStartActivity getActivity() {return this;}

	private void selectTour(TourInformation tour){
		ProgressDialogImplementation dlg = ProgressDialogImplementation.createProgressDialog(getActivity(), "", getString(R.string.indexing_tour, ""),
				ProgressDialog.STYLE_SPINNER);

		new AsyncTask<TourInformation, Void, Void>(){
			private ProgressDialog dlg;

			protected void onPreExecute() {
				dlg = new ProgressDialog(getActivity());
				dlg.setTitle("Selecting tour...");
				dlg.setMessage("Please wait.");
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
