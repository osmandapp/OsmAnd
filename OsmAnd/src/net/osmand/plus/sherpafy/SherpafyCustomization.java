package net.osmand.plus.sherpafy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import net.osmand.IProgress;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.DownloadIndexActivity;
import net.osmand.plus.api.FileSettingsAPIImpl;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.sherpafy.TourInformation.StageInformation;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.TextView;

public class SherpafyCustomization extends OsmAndAppCustomization {
	
	private static final String SELECTED_TOUR = "selected_tour";
	private static final String SELECTED_STAGE = "selected_stage";
	private OsmandSettings originalSettings;
	private CommonPreference<String> selectedTourPref;
	private CommonPreference<String> selectedStagePref;
	private List<TourInformation> tourPresent = new ArrayList<TourInformation>();
	private StageInformation selectedStage = null;
	private TourInformation selectedTour = null;
	private File toursFolder;

	@Override
	public void setup(OsmandApplication app) {
		super.setup(app);
		originalSettings = createSettings(app.getSettings().getSettingsAPI());
		selectedTourPref = originalSettings.registerStringPreference(SELECTED_TOUR, null).makeGlobal();
		toursFolder = new File(originalSettings.getExternalStorageDirectory(), "osmand/tours");
	}


	public boolean checkExceptionsOnStart() {
		return false;
	}

	public boolean showFirstTimeRunAndTips(boolean firstTime, boolean appVersionChanged) {
		return false;
	}

	public boolean checkBasemapDownloadedOnStart() {
		return false;
	}
	
	@Override
	public void customizeMainMenu(Window window, final Activity activity) {
		// Update app name
		TextView v =  (TextView) window.findViewById(R.id.AppName);
		v.setText("Sherpafy");
		
		TextView toursButtonText = (TextView) window.findViewById(R.id.SettingsButtonText);
		toursButtonText.setText(R.string.tour);
		View toursButton = window.findViewById(R.id.SettingsButton);
		toursButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent search = new Intent(activity, getTourSelectionActivity());
				search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				activity.startActivity(search);
			}
		});
		// the image could be also updated
	}
	
	private Class<?> getTourSelectionActivity() {
		return TourCommonActivity.class;
	}
	
	@Override
	public void getDownloadTypes(List<DownloadActivityType> items) {
		super.getDownloadTypes(items);
		items.add(0, TourDownloadType.TOUR);
	}
	
	public void updatedLoadedFiles(java.util.Map<String,String> indexFileNames, java.util.Map<String,String> indexActivatedFileNames) {
//		DownloadIndexActivity.listWithAlternatives(app.getResourceManager().getDateFormat(), 
//				toursFolder, "", indexFileNames);	
	}
	
	public List<String> onIndexingFiles(IProgress progress, Map<String, String> indexFileNames) {
		ArrayList<TourInformation> tourPresent = new ArrayList<TourInformation>();
		List<String> warns = new ArrayList<String>();
		selectedTour = null;
		if(toursFolder.exists()) {
			File[] availableTours = toursFolder.listFiles();
			if(availableTours != null) {
				String selectedName = selectedTourPref.get();
				for(File tr : availableTours) {
					if (tr.isDirectory()) {
						boolean selected = selectedName != null && selectedName.equals(tr.getName());
						String date = app.getResourceManager().getDateFormat()
								.format(new Date(DownloadIndexActivity.findFileInDir(tr).lastModified()));
						indexFileNames.put(tr.getName(), date);
						final TourInformation tourInformation = new TourInformation(tr);
						tourPresent.add(tourInformation);
						if (selected) {
							reloadSelectedTour(progress, tr, tourInformation, warns);
						}
					}
				}
			}
		}
		this.tourPresent = tourPresent;
		return warns;
	}

	public List<TourInformation> getTourInformations() {
		return tourPresent;
	}
	
	public TourInformation getSelectedTour() {
		return selectedTour;
	}

	private void reloadSelectedTour(IProgress progress, File tr, final TourInformation tourInformation, List<String> warns) {
		if(progress != null) {
			progress.startTask(app.getString(R.string.indexing_tour, tr.getName()), -1);
		}
		File settingsFile = new File(tr, "settings.props");
		FileSettingsAPIImpl fapi;
		try {
			fapi = new FileSettingsAPIImpl(app, settingsFile);
			if (!settingsFile.exists()) {
				fapi.saveFile();
			}
			app.getSettings().setSettingsAPI(fapi);
		} catch (IOException e) {
			warns.add(app.getString(R.string.settings_file_create_error));
			app.showToastMessage(R.string.settings_file_create_error);
		}
		selectedStagePref = app.getSettings().registerStringPreference(SELECTED_STAGE, null).makeGlobal();
		try {
			tourInformation.loadFullInformation();
		} catch (Exception e) {
			warns.add("Selected tour : " + e.getMessage());
		}
		selectedTour = tourInformation;
		if(selectedStagePref.get() != null) {
			for(StageInformation s : selectedTour.getStageInformation()) {
				if(s.getName().equals(selectedStagePref.get())) {
					selectedStage = s;
					break;
				}
			}
		}
	}
	
	public StageInformation getSelectedStage() {
		return selectedStage;
	}

	public void selectStage(StageInformation tour, IProgress progress) {
		if(tour == null) {
			selectedStagePref.set(null);
		} else {
			selectedStagePref.set(tour.getName());
		}
	}

	public void selectTour(TourInformation tour, IProgress progress) {
		if(tour == null) {
			selectedTourPref.set(null);
		} else {
			selectedTourPref.set(tour.getName());
		}
		selectedTour = null;
		selectedStage = null;
		app.getResourceManager().reloadIndexes(progress);
	}
}
