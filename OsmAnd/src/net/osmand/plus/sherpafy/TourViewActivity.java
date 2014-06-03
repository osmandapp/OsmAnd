package net.osmand.plus.sherpafy;

import java.util.List;

import net.osmand.IProgress;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.actionbarsherlock.app.SherlockFragmentActivity;

/**
 */
public class TourViewActivity extends SherlockFragmentActivity {

	private SherpafyCustomization customization;
	ImageView img;
	TextView description;
	TextView fullDescription;
	List<TourInformation.StageInformation> stagesInfo;
	TourInformation curTour;
	RadioGroup stages;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.OsmandLightTheme);
        ((OsmandApplication) getApplication()).setLanguage(this);
        super.onCreate(savedInstanceState);
        getSherlock().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setTitle(R.string.sherpafy_app_name);

		ProgressDialog startProgressDialog = new ProgressDialog(this);
		getMyApplication().checkApplicationIsBeingInitialized(this, startProgressDialog);

		setContentView(R.layout.custom_tour_info);

		ToggleButton collapser = (ToggleButton) findViewById(R.id.collapse);
		stages = (RadioGroup) findViewById(R.id.stages);
		stages.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup radioGroup, int i) {
				if (i == 0) {
					customization.selectStage(null, IProgress.EMPTY_PROGRESS);
					fullDescription.setText(curTour.getFulldescription());
					description.setText((curTour.getShortDescription()));
					prepareBitmap();
				} else {
					//-1 because there's one more item Overview, which is not exactly a stage.
					customization.selectStage(stagesInfo.get(i - 1), IProgress.EMPTY_PROGRESS);
					description.setText(stagesInfo.get(i - 1).getDescription());
					fullDescription.setText("");
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

		Button settings = (Button) findViewById(R.id.btn_settings);

		settings.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getApplicationContext(), SherpafyStartActivity.class);
				intent.putExtra("SETTINGS", true);
				startActivity(intent);
			}
		});


	}

	@Override
	protected void onResume() {
		super.onResume();

		customization = (SherpafyCustomization) getMyApplication().getAppCustomization();
		getSupportActionBar().setTitle(customization.getSelectedTour().getName());

		updateTourView();
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

		//in case of reloading view - remove all previous radio buttons
		stages.removeAllViews();

		start_tour.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				MapActivity.launchMapActivityMoveToTop(GetActivity());
			}
		});

		//get count of radio buttons
		final int count = stagesInfo.size() + 1;
		final RadioButton[] rb = new RadioButton[count];

		rb[0] = new RadioButton(this);
		rb[0].setId(0);
		stages.addView(rb[0]);
		rb[0].setText("Overview");
		rb[0].setTextColor(getResources().getColor(R.color.color_black));
		//add radio buttons to view
		for (int i = 1; i < count; i++) {
			rb[i] = new RadioButton(this);
			rb[i].setId(i);
			stages.addView(rb[i]);
			rb[i].setText(stagesInfo.get(i - 1).getName());
			rb[i].setTextColor(getResources().getColor(R.color.color_black));
		}

		TourInformation.StageInformation curStage = customization.getSelectedStage();

		//if there's no current stage - overview should be selected
		if (curStage == null) {
			//DIRTY HACK, I dunno why, but after activity onResume it's not possible to just check item 0
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

	private TourViewActivity GetActivity() {
		return this;
	}
}
