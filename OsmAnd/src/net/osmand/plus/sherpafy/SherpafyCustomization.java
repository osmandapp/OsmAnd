package net.osmand.plus.sherpafy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import net.osmand.IProgress;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.DownloadIndexActivity;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.api.FileSettingsAPIImpl;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.sherpafy.TourInformation.StageInformation;
import android.app.Activity;
import android.view.Window;
import android.widget.TextView;

public class SherpafyCustomization extends OsmAndAppCustomization {
	
	private static final String SELECTED_TOUR = "selected_tour";
	private static final String ACCESS_CODE = "access_code";
	private static final String SELECTED_STAGE = "selected_stage";
	private OsmandSettings originalSettings;
	private CommonPreference<String> selectedTourPref;
	private CommonPreference<String> selectedStagePref;
	private List<TourInformation> tourPresent = new ArrayList<TourInformation>();
	private StageInformation selectedStage = null;
	private TourInformation selectedTour = null;
	private File toursFolder;
	private CommonPreference<String> accessCodePref;

	@Override
	public void setup(OsmandApplication app) {
		super.setup(app);
		originalSettings = createSettings(app.getSettings().getSettingsAPI());
		selectedTourPref = originalSettings.registerStringPreference(SELECTED_TOUR, null).makeGlobal();
		accessCodePref = originalSettings.registerStringPreference(ACCESS_CODE, "").makeGlobal();
		toursFolder = new File(originalSettings.getExternalStorageDirectory(), "osmand/tours");
	}
	
	public boolean setAccessCode(String acCode) {
		acCode = acCode.toUpperCase();
		if(validate(acCode)) {
			accessCodePref.set(acCode);
			return true;
		}
		return false;
	}
	
	private boolean validate(String acCode) {
		if (acCode.length() < 3) {
			return false;
		}
		int k = 0;
		for (int i = 0; i < acCode.length() - 1; i++) {
			k += (acCode.charAt(i) - 'A');
		}
		return (k % 10) == (acCode.charAt(acCode.length() - 1) - '0');
	}

	public boolean isSettingsAvailable(){
		return accessCodePref.get().startsWith("BAB2");
	}
	
	public String getAccessCode() {
		return accessCodePref.get();
	}

	public boolean isTourSelected() {
		return selectedTourPref.get() != null;
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
		
//		TextView toursButtonText = (TextView) window.findViewById(R.id.SettingsButtonText);
//		toursButtonText.setText(R.string.tour);
//		View toursButton = window.findViewById(R.id.SettingsButton);
//		toursButton.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				final Intent search = new Intent(activity, getTourSelectionActivity());
//				search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//				activity.startActivity(search);
//			}
//		});


		// the image could be also updated
	}
	
	
	@Override
	public void getDownloadTypes(List<DownloadActivityType> items) {
		super.getDownloadTypes(items);
		items.add(0, TourDownloadType.TOUR);
	}
	
	public void updatedLoadedFiles(java.util.Map<String,String> indexFileNames, java.util.Map<String,String> indexActivatedFileNames) {
		DownloadIndexActivity.listWithAlternatives(app.getResourceManager().getDateFormat(),
				toursFolder, "", indexFileNames);
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
						try {
							tourInformation.loadFullInformation();
						} catch (Exception e) {
							e.printStackTrace();
						}
						if (selected) {
							reloadSelectedTour(progress, tr, tourInformation, warns);
						}
					}
				}
				if (selectedName == null) {
					app.getSettings().setSettingsAPI(originalSettings.getSettingsAPI());
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
					loadSelectedStage();
					break;
				}
			}
		}
	}
	
	public StageInformation getSelectedStage() {
		return selectedStage;
	}

	public void selectStage(StageInformation stage, IProgress progress) {
		if(stage == null) {
			selectedStagePref.set(null);
			selectedStage = null;
		} else {
			selectedStagePref.set(stage.getName());
			selectedStage = stage;
		}
		loadSelectedStage();
	}

	private void loadSelectedStage() {
		final StageInformation st = selectedStage;
		if(st != null && st.gpxFile != null) {
			if(st.gpx == null) {
				st.gpx = GPXUtilities.loadGPXFile(app, st.gpxFile);
			}
		}
	}


	public void selectTour(TourInformation tour, IProgress progress) {
		if (tour == null) {
			selectedTourPref.set(null);
		} else {
			selectedTourPref.set(tour.getName());
		}
		selectedTour = null;
		selectedStage = null;
		// to avoid null reference ecxeption if there's no selected tour yet.
		if (selectedStagePref != null) {
			selectedStagePref.set(null);
		}
		app.getResourceManager().reloadIndexes(progress);
	}

	@Override
	public void prepareLayerContextMenu(MapActivity activity, ContextMenuAdapter adapter) {
		filter(adapter, R.string.layer_poi, R.string.layer_amenity_label, R.string.layer_favorites);
	}
	
	@Override
	public void prepareLocationMenu(MapActivity mapActivity, ContextMenuAdapter adapter) {
		filter(adapter, R.string.context_menu_item_directions_to,
				R.string.context_menu_item_destination_point, R.string.context_menu_item_search,
				R.string.context_menu_item_share_location, R.string.context_menu_item_add_favorite);
	}
	
	@Override
	public void prepareOptionsMenu(MapActivity mapActivity, ContextMenuAdapter adapter) {
		
		filter(adapter, R.string.exit_Button, R.string.favorites_Button, R.string.menu_layers,
				R.string.cancel_navigation, R.string.cancel_route, R.string.clear_destination,
				R.string.get_directions, 
				R.string.menu_mute_on, R.string.menu_mute_off,
				R.string.where_am_i);
	}
	
	public void filter(ContextMenuAdapter a, Integer... ids) {
		if(isSettingsAvailable()) {
			return;
		}
		TreeSet<Integer> set = new TreeSet<Integer>(Arrays.asList(ids));
		for(int i =0; i < a.length();) {
			int itemId = a.getItemId(i);
			if(set.contains(itemId)) {
				i++;
			} else {
				a.removeItem(i);
			}
		}
	}
}
