package net.osmand.plus.sherpafy;

import java.io.File;
import java.io.IOException;

import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.api.FileSettingsAPIImpl;
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
	public void customizeMainMenu(Window window) {
		// Update app name
		TextView v =  (TextView) window.findViewById(R.id.AppName);
		v.setText("Sherpafy " + Version.getAppVersion(app));
		
		TextView toursButton = (TextView) window.findViewById(R.id.SettingsButtonText);
		toursButton.setText(R.string.tour);
		// the image could be updated
	}
}
