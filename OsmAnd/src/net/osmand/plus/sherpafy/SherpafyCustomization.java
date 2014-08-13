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
import net.osmand.IndexConstants;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.DownloadIndexActivity;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityLayers;
import net.osmand.plus.api.FileSettingsAPIImpl;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.sherpafy.TourInformation.StageFavorite;
import net.osmand.plus.sherpafy.TourInformation.StageInformation;
import net.osmand.util.Algorithms;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

public class SherpafyCustomization extends OsmAndAppCustomization {
	
	private static final String SELECTED_TOUR = "selected_tour";
	private static final String ACCESS_CODE = "access_code";
	private static final String SELECTED_STAGE = "selected_stage";
	private static final String VISITED_STAGES = "visited_STAGES";
	private OsmandSettings originalSettings;
	private CommonPreference<String> selectedTourPref;
	private CommonPreference<String> selectedStagePref;
	private CommonPreference<String> visitedStagesPref;
	private List<TourInformation> tourPresent = new ArrayList<TourInformation>();
	private StageInformation selectedStage = null;
	private TourInformation selectedTour = null;
	private File toursFolder;
	private CommonPreference<String> accessCodePref;
	private List<FavouritePoint> cachedFavorites = new ArrayList<FavouritePoint>();

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
		if(validate(acCode) || Algorithms.isEmpty(acCode)) {
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
							reloadSelectedTour(progress, tourInformation);
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

	private void reloadSelectedTour(IProgress progress, final TourInformation tourInformation) {
		if(progress != null) {
			progress.startTask(app.getString(R.string.indexing_tour, tourInformation.getName()), -1);
		}
		File settingsFile = new File(tourInformation.getFolder(), "settings.props");
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
		selectedStagePref = app.getSettings().registerStringPreference(SELECTED_STAGE, null).makeGlobal();
		visitedStagesPref = app.getSettings().registerStringPreference(VISITED_STAGES, null).makeGlobal();
		selectedTour = tourInformation;
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
		cachedFavorites = new ArrayList<FavouritePoint>();
		for(Object o : st.favorites ) {
			if(o instanceof StageFavorite) {
				StageFavorite sf = (StageFavorite) o;
				FavouritePoint fp = new FavouritePoint(sf.getLatLon().getLatitude(), sf.getLatLon().getLongitude(), 
						sf.getName(), sf.getGroup() == null ? "" : sf.getGroup().name);
				if(sf.getGroup() != null && sf.getGroup().getColor() != 0 ){
					fp.setColor(sf.getGroup().getColor());
				}
				fp.setRemoveable(false);
				fp.setExtraParam(sf.getOrder());
				cachedFavorites.add(fp);
			}
		}
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
		selectedTour = tour;
		reloadSelectedTour(progress, tour);
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
		MapActivityLayers layers = mapActivity.getMapLayers();
		if(layers.getContextMenuLayer().getFirstSelectedObject() instanceof FavouritePoint) {
			FavouritePoint fp = ((FavouritePoint)layers.getContextMenuLayer().getFirstSelectedObject());
			if(fp.getExtraParam() >= 0 && selectedStage != null) {
				StageFavorite sf = (StageFavorite) selectedStage.getFavorites().get(fp.getExtraParam());
				showFavoriteDialog(mapActivity, selectedStage, sf);
			}
		}
	}
	
	private void showFavoriteDialog(MapActivity mapActivity, StageInformation stage, StageFavorite sf) {
		SherpafyFavoriteFragment fragment = new SherpafyFavoriteFragment();
		Bundle bl = new Bundle();
		bl.putInt(SherpafyFavoriteFragment.STAGE_PARAM, stage.getOrder());
		bl.putString(SherpafyFavoriteFragment.TOUR_PARAM, stage.getTour().getId());
		bl.putInt(SherpafyFavoriteFragment.FAV_PARAM, sf.getOrder());
		fragment.setArguments(bl);
		Builder bld = new AlertDialog.Builder(mapActivity);
		bld.setTitle(sf.getName() + " TODO ");
		bld.setPositiveButton(R.string.default_buttons_ok, null);
		// TODO
//		Builder bld = new AlertDialog.Builder(mapActivity);
//		FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
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
	
	@Override
	public List<FavouritePoint> getFavorites() {
		return cachedFavorites;
	}
	
	@Override
	public String getIndexesUrl() {
		String s = "http://"+IndexConstants.INDEX_DOWNLOAD_DOMAIN+"/tours.php?gzip&" + Version.getVersionAsURLParam(app);
		if(!Algorithms.isEmpty(accessCodePref.get())) {
			s += "&code="+accessCodePref.get();
		}
		return s;
	}
}
