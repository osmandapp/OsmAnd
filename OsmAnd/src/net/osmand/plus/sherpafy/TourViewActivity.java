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
	List<TourInformation.StageInformation> stages_info;
	TourInformation cur_tour;
	RadioGroup stages;
	private boolean hack = false;


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

		Button collapser = (Button) findViewById(R.id.collapse);
		stages = (RadioGroup) findViewById(R.id.stages);
		stages.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup radioGroup, int i) {
				if (hack) {return;}
				if (i == 0) {
					customization.selectStage(null, IProgress.EMPTY_PROGRESS);
					fullDescription.setText(cur_tour.getFulldescription());
					description.setText((cur_tour.getShortDescription()));
					prepareBitmap();
				} else {
					//-1 because there's one more item Overview, which is not exactly a stage.
					customization.selectStage(stages_info.get(i - 1), IProgress.EMPTY_PROGRESS);
					description.setText(stages_info.get(i - 1).getDescription());
					fullDescription.setText("");
				}
			}
		});

		collapser.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Button btn = (Button) view;
				if (btn.getText().equals("+")) {
					btn.setText("-");
					stages.setVisibility(View.VISIBLE);
				} else {
					btn.setText("+");
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
		cur_tour = customization.getSelectedTour();
		stages_info = cur_tour.getStageInformation();

		img = (ImageView) findViewById(R.id.tour_image);
		description = (TextView) findViewById(R.id.tour_description);
		description.setVisibility(View.VISIBLE);
		fullDescription = (TextView) findViewById(R.id.tour_fulldescription);
		fullDescription.setVisibility(View.VISIBLE);
		Button start_tour = (Button) findViewById(R.id.start_tour);
		Button itenerary = (Button) findViewById(R.id.itenerary);

		//in case of reloading view - remove all previous radio buttons
		stages.removeAllViews();

		start_tour.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				MapActivity.launchMapActivityMoveToTop(GetActivity());
			}
		});

		//get count of radio buttons
		final int count = stages_info.size() + 1;
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
			rb[i].setText(stages_info.get(i - 1).getName());
			rb[i].setTextColor(getResources().getColor(R.color.color_black));
		}

		TourInformation.StageInformation cur_stage = customization.getSelectedStage();

		//if there's no current stage - overview should be selected
		if (cur_stage == null) {
			//DIRTY HACK, I dunno why, but after activity onResume it's not possible to just check item 0
			hack = true;
			stages.check(1);
			stages.check(0);
			hack = false;
			description.setText(cur_tour.getShortDescription());
			fullDescription.setText(cur_tour.getFulldescription());
			prepareBitmap();
		} else {
			int i;
			for (i = 1; i < count; i++) {
				if (cur_stage.equals(stages_info.get(i - 1)))
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
