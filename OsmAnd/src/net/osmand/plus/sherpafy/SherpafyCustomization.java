package net.osmand.plus.sherpafy;

import java.io.File;
import java.io.IOException;
import java.util.List;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.DownloadIndexActivity;
import net.osmand.plus.activities.MainMenuActivity;
import net.osmand.plus.api.FileSettingsAPIImpl;
import net.osmand.plus.download.DownloadActivityType;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.TextView;

public class SherpafyCustomization extends OsmAndAppCustomization {
	
	private static final String SELECTED_TOUR = "sherpafy_tour";
	private OsmandSettings originalSettings;
	private CommonPreference<String> selectedTourPref;
	private File selectedTourFolder  = null;

	@Override
	public void setup(OsmandApplication app) {
		super.setup(app);
		originalSettings = createSettings(app.getSettings().getSettingsAPI());
		selectedTourPref = originalSettings.registerBooleanPreference(SELECTED_TOUR, null).makeGlobal();
		File toursFolder = new File(originalSettings.getExternalStorageDirectory(), "tours");
		if(selectedTourPref.get() != null) {
			selectedTourFolder = new File(toursFolder, selectedTourPref.get());
			selectedTourFolder.mkdirs();
		}
		
		if(selectedTourFolder != null) {
			File settingsFile = new File(selectedTourFolder, "settings.props");
			FileSettingsAPIImpl fapi;
			try {
				fapi = new FileSettingsAPIImpl(app, settingsFile);
				if (!settingsFile.exists()) {
					fapi.saveFile();
				}
				app.getSettings().setSettingsAPI(fapi);
			} catch (IOException e) {
				app.showToastMessage(R.string.settings_file_create_error);
			}
		}
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
		View toursButton = window.findViewById(R.id.SearchButton);
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
		return MainMenuActivity.class;
	}
	
	@Override
	public void getDownloadTypes(List<DownloadActivityType> items) {
		super.getDownloadTypes(items);
		items.add(0, TourDownloadType.TOUR);
	}
	
	public void updatedLoadedFiles(java.util.Map<String,String> indexFileNames, java.util.Map<String,String> indexActivatedFileNames) {
		DownloadIndexActivity.listWithAlternatives(app.getResourceManager().getDateFormat(), 
				app.getAppPath("tours"), "", indexFileNames);	
	}
}
