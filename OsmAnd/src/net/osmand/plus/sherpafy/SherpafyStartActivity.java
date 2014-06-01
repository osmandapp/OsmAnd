package net.osmand.plus.sherpafy;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
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

    private ProgressDialog startProgressDialog;
    private SherpafyCustomization customization;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((OsmandApplication) getApplication()).applyTheme(this);
        super.onCreate(savedInstanceState);
        getSherlock().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Sherpafy");

        if (!(getMyApplication().getAppCustomization() instanceof SherpafyCustomization)) {
            getMyApplication().setAppCustomization(new SherpafyCustomization());
        }

        customization = (SherpafyCustomization) getMyApplication().getAppCustomization();

		if (customization.getTourInformations().isEmpty())
		{
			customization.onIndexingFiles(new IProgress() {
				@Override
				public void startTask(String taskName, int work) {

				}

				@Override
				public void startWork(int work) {

				}

				@Override
				public void progress(int deltaWork) {

				}

				@Override
				public void remaining(int remainingWork) {

				}

				@Override
				public void finishTask() {

				}

				@Override
				public boolean isIndeterminate() {
					return false;
				}

				@Override
				public boolean isInterrupted() {
					return false;
				}
			}, new ConcurrentHashMap<String, String>() );
		}

        if (customization.getSelectedTour() == null) {

            startProgressDialog = new ProgressDialog(this);
            getMyApplication().checkApplicationIsBeingInitialized(this, startProgressDialog);

			updateDefaultView();
        } else {
            super.setContentView(R.layout.custom_tour_info);
            updateTourView();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
		if (customization.getSelectedTour() != null)
		{
			super.setContentView(R.layout.custom_tour_info);
			updateTourView();
		}
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private OsmandApplication getMyApplication() {
        return (OsmandApplication) getApplication();
    }

	private void updateDefaultView(){
		super.setContentView(R.layout.sherpafy_start);

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

    private void updateTourView() {
        ImageView img = (ImageView) findViewById(R.id.tour_image);
        TextView description = (TextView) findViewById(R.id.tour_description);
        TextView fullDescription = (TextView) findViewById(R.id.tour_fulldescription);
        TextView name = (TextView) findViewById(R.id.tour_name);
		Button start_tour = (Button) findViewById(R.id.start_tour);
		Button itenerary = (Button) findViewById(R.id.itenerary);
		Button settings = (Button) findViewById(R.id.settings);

		settings.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				updateDefaultView();
			}
		});

		TourInformation tour = customization.getSelectedTour();
		List<TourInformation.StageInformation> stages = tour.getStageInformation();

        name.setText(tour.getName());
        description.setText(tour.getShortDescription());
        description.setVisibility(View.VISIBLE);
        fullDescription.setText(tour.getFulldescription());
        fullDescription.setVisibility(View.VISIBLE);
        final Bitmap imageBitmap = tour.getImageBitmap();
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


}
