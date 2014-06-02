package net.osmand.plus.sherpafy;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import net.osmand.IProgress;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.DownloadIndexActivity;

import java.util.List;
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

		Button selectTour = (Button) findViewById(R.id.select_tour);
		selectTour.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent select = new Intent(v.getContext(), TourCommonActivity.class);
				select.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				v.getContext().startActivity(select);
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

}
