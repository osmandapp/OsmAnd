package net.osmand.plus.sherpafy;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import net.osmand.IProgress;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

import java.util.List;

/**
 * Created by Barsik on 02.06.2014.
 */
public class TourViewActivity extends SherlockFragmentActivity {

	private SherpafyCustomization customization;
	ImageView img;
	TextView description;
	TextView fullDescription;
	TextView name;
	List<TourInformation.StageInformation> stages_info;
	TourInformation cur_tour;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		((OsmandApplication) getApplication()).applyTheme(this);
		getSupportActionBar().setDisplayHomeAsUpEnabled(false);

		//		if (customization.getTourInformations().isEmpty())
//		{
//			customization.onIndexingFiles( IProgress.EMPTY_PROGRESS, new ConcurrentHashMap<String, String>() );
//		}

		ProgressDialog startProgressDialog = new ProgressDialog(this);
		getMyApplication().checkApplicationIsBeingInitialized(this, startProgressDialog);

		setContentView(R.layout.custom_tour_info);
	}

	@Override
	protected void onResume() {
		super.onResume();

		customization = (SherpafyCustomization) getMyApplication().getAppCustomization();
		getSupportActionBar().setTitle(customization.getSelectedTour().getName());

		updateTourView();
	}


	private void updateTourView() {
		img = (ImageView) findViewById(R.id.tour_image);
		description = (TextView) findViewById(R.id.tour_description);
		description.setVisibility(View.VISIBLE);
		fullDescription = (TextView) findViewById(R.id.tour_fulldescription);
		fullDescription.setVisibility(View.VISIBLE);
		name = (TextView) findViewById(R.id.tour_name);
		Button start_tour = (Button) findViewById(R.id.start_tour);
		Button itenerary = (Button) findViewById(R.id.itenerary);
		Button settings = (Button) findViewById(R.id.btn_settings);
		RadioGroup stages = (RadioGroup) findViewById(R.id.stages);

		stages.removeAllViews();

		start_tour.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				MapActivity.launchMapActivityMoveToTop(GetActivity());
			}
		});

		cur_tour = customization.getSelectedTour();
		stages_info = cur_tour.getStageInformation();

		stages.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup radioGroup, int i) {
				customization.selectStage(stages_info.get(i), IProgress.EMPTY_PROGRESS);
				if (i == 0) {
					fullDescription.setText(cur_tour.getFulldescription());
					description.setText((cur_tour.getShortDescription()));
					prepareBitmap();
				} else {
					description.setText(stages_info.get(i).getDescription());
					fullDescription.setText("");

				}
			}
		});

		settings.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getApplicationContext(), SherpafyStartActivity.class);
				intent.putExtra("settings", "settings");
				startActivity(intent);
			}
		});


		//get count of radio buttons
		final int count = stages_info.size();
		final RadioButton[] rb = new RadioButton[count];

		//add radio buttons to view
		for (int i = 0; i < count; i++) {
			rb[i] = new RadioButton(this);
			rb[i].setId(i);
			stages.addView(rb[i]);
			rb[i].setText(stages_info.get(i).getName());
			rb[i].setTextColor(getResources().getColor(R.color.color_black));
		}

		TourInformation.StageInformation cur_stage = customization.getSelectedStage();

		name.setText(cur_tour.getName());

		//if there's no current stage - overview should be selected
		if (cur_stage == null) {
			stages.check(0);
			description.setText(cur_tour.getShortDescription());
			fullDescription.setText(cur_tour.getFulldescription());
			prepareBitmap();
		} else {
			int i = 0;
			for (i = 0; i < count; i++) {
				if (cur_stage.equals(stages_info.get(i)))
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
		final Bitmap imageBitmap = cur_tour.getImageBitmap();
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
