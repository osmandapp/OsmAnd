package net.osmand.plus.sherpafy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.LocationPoint;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.Version;
import net.osmand.plus.download.DownloadIndexFragment;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityLayers;
import net.osmand.plus.activities.SelectedGPXFragment;
import net.osmand.plus.activities.actions.ShareLocation;
import net.osmand.plus.api.FileSettingsAPIImpl;
import net.osmand.plus.api.SettingsAPI;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.sherpafy.TourInformation.StageFavorite;
import net.osmand.plus.sherpafy.TourInformation.StageInformation;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.Algorithms;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class SherpafyCustomization extends OsmAndAppCustomization {
	
	private static final String SELECTED_TOUR = "selected_tour";
	private static final String ACCESS_CODE = "access_code";
	private static final String SELECTED_STAGE = "selected_stage_int";
	private static final String VISITED_STAGES = "visited_stages_int";
	private CommonPreference<Integer> selectedStagePref;
	private CommonPreference<Integer> visitedStagesPref;
	private boolean toursIndexed;
	private List<TourInformation> tourPresent = new ArrayList<TourInformation>();
	private StageInformation selectedStage = null;
	private TourInformation selectedTour = null;
	private File toursFolder;
	private CommonPreference<String> accessCodePref;
	private List<StageFavorite> cachedFavorites = new ArrayList<StageFavorite>();
	private SettingsAPI originalApi;
	private CommonPreference<String> saveGPXFolder;
	public static final String TOUR_SERVER = "download.osmand.net";
	private static final String SAVE_GPX_FOLDER = "save_gpx_folder";
	private Object originalGlobal;

	@Override
	public void setup(OsmandApplication app) {
		super.setup(app);
		originalApi = osmandSettings.getSettingsAPI();
		originalGlobal = osmandSettings.getGlobalPreferences();
		saveGPXFolder = osmandSettings.registerStringPreference(SAVE_GPX_FOLDER, null).makeGlobal();
		if(osmandSettings.OSMAND_THEME.get() != OsmandSettings.OSMAND_LIGHT_THEME) {
			osmandSettings.OSMAND_THEME.set(OsmandSettings.OSMAND_LIGHT_THEME);
		}
		accessCodePref = osmandSettings.registerStringPreference(ACCESS_CODE, "").makeGlobal();
		toursFolder = new File(osmandSettings.getExternalStorageDirectory(), "osmand/tours");

	}

	@Override
	public File getExternalStorageDir() {
		final String defaultLocation = Environment.getExternalStorageDirectory().getAbsolutePath();
		return new File(originalApi.getString(originalGlobal, OsmandSettings.EXTERNAL_STORAGE_DIR,
				defaultLocation));
	}

	public boolean setAccessCode(String acCode) {
		acCode = acCode.toUpperCase();
		if(DownloadActivity.downloadListIndexThread != null) {
			DownloadActivity.downloadListIndexThread.clear();
		}
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
		return originalApi.getString(originalGlobal, SELECTED_TOUR, null)!= null;
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
		DownloadIndexFragment.listWithAlternatives(app.getResourceManager().getDateFormat(),
				toursFolder, "", indexFileNames);
	}
	
	public List<String> onIndexingFiles(IProgress progress, Map<String, String> indexFileNames) {
		ArrayList<TourInformation> tourPresent = new ArrayList<TourInformation>();
		List<String> warns = new ArrayList<String>();
		selectedTour = null;
		final HashSet<String> suggestToDownloadMap = new HashSet<String>();
		if(toursFolder.exists()) {
			File[] availableTours = toursFolder.listFiles();
			if(availableTours != null) {
				String selectedName = originalApi.getString(originalGlobal, SELECTED_TOUR, null);
				for(File tr : availableTours) {
					if (tr.isDirectory()) {
						String date = app.getResourceManager().getDateFormat()
								.format(new Date(DownloadIndexFragment.findFileInDir(tr).lastModified()));
						indexFileNames.put(tr.getName(), date);
						final TourInformation tourInformation = new TourInformation(tr);
						tourPresent.add(tourInformation);
						try {
							tourInformation.loadFullInformation();
						} catch (Exception e) {
							e.printStackTrace();
						}
						// check that tour was downloaded
						if(toursIndexed) {
							for (String map : tourInformation.getMaps()) {
								if (!new File(toursFolder.getParentFile(), map + ".obf").exists()) {
									suggestToDownloadMap.add(map);
								}
							}
						}
						boolean selected = selectedName != null && selectedName.equals(tourInformation.getName());
						if (selected) {
							reloadSelectedTour(progress, tourInformation);
						}
					}
				}
				if (selectedName == null) {
					app.getSettings().setSettingsAPI(originalApi);
				}
			}
			toursIndexed = true;
		}
		this.tourPresent = tourPresent;
		if(!suggestToDownloadMap.isEmpty()) {
			final DownloadIndexFragment da = app.getDownloadActivity();
			if (da != null) {
				app.runInUIThread(new Runnable() {

					@Override
					public void run() {
						da.showDialogToDownloadMaps(suggestToDownloadMap);

					}
				});
			}
		}
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
		selectedStagePref = app.getSettings().registerIntPreference(SELECTED_STAGE, -1).makeGlobal();
		visitedStagesPref = app.getSettings().registerIntPreference(VISITED_STAGES, 0).makeGlobal();
		app.getSettings().OSMAND_THEME.set(OsmandSettings.OSMAND_LIGHT_THEME);
		selectedTour = tourInformation;
		selectNextAvailableStage(tourInformation);
	}

	public StageInformation selectNextAvailableStage(final TourInformation tourInformation) {
		if(selectedStagePref == null) {
			return selectedStage;
		}
		Integer it = selectedStagePref.get();
		while(it >= 0 && isStageVisited(it) ){
			it++;
		}
		if(it >= 0 && it < tourInformation.getStageInformation().size()) {
			selectedStage = tourInformation.getStageInformation().get(it);
		}
		return selectedStage;
	}
	
	public StageInformation getNextAvailableStage(final TourInformation tourInformation) {
		if(selectedStagePref == null){
			return null;
		}
		int it = selectedStagePref.get();
		while(it >= 0 && isStageVisited(it) ){
			it++;
		}
		if(it >= 0 && it < tourInformation.getStageInformation().size()) {
			return tourInformation.getStageInformation().get(it);
		}
		return null;
	}
	
	public boolean isStageVisited(int stageOrder) {
		if(visitedStagesPref == null) {
			return false;
		}
		Integer gi = visitedStagesPref.get();
		return (gi & (1 << stageOrder)) > 0;
	}
	
	public void markStageAsCompleted(StageInformation si) {
		if(visitedStagesPref == null) {
			return;
		}
		Integer gi = visitedStagesPref.get();
		gi |= (1 << si.getOrder());
		visitedStagesPref.set(gi);
		saveCurrentGPXTrack();
	}
	
	public void markStageAsNotCompleted(StageInformation si) {
		if(visitedStagesPref == null) {
			return;
		}
		Integer gi = visitedStagesPref.get();
		if((gi & (1 << si.getOrder())) > 0) {
			gi = gi - (1 << si.getOrder());
		}
		visitedStagesPref.set(gi);
	}

	protected void saveCurrentGPXTrack() {
		if(!Algorithms.isEmpty(saveGPXFolder.get())) {
			app.getSavingTrackHelper().saveDataToGpx(new File(saveGPXFolder.get()));
		}
	}
	
	public File getTracksDir() {
		if(!Algorithms.isEmpty(saveGPXFolder.get())) {
			return new File(saveGPXFolder.get());
		}
		return app.getAppPath(IndexConstants.GPX_RECORDED_INDEX_DIR);
	}
	
	
	public static class CompleteStageFragment extends DialogFragment {

		public static final String STAGE_PARAM = "STAGE";
		public static final String TOUR_PARAM = "TOUR";
		public static final String START_OVER = "START_OVER";

		public CompleteStageFragment() {
		}

		protected void extractArguments(Bundle args) {
			
		}
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			
			Bundle args = getArguments();
			OsmandApplication app = (OsmandApplication) getActivity().getApplication();
			
			Builder bld = new AlertDialog.Builder(getActivity());
			if(app.getSelectedGpxHelper().isShowingAnyGpxFiles()) {
				SelectedGPXFragment sgf = new SelectedGPXFragment();
				Bundle iargs = new Bundle();
				iargs.putBoolean(SelectedGPXFragment.ARG_TO_EXPAND_TRACK_INFO, true);
				iargs.putBoolean(SelectedGPXFragment.ARG_TO_FILTER_SHORT_TRACKS, true);
				iargs.putBoolean(SelectedGPXFragment.ARG_TO_HIDE_CONFIG_BTN, true);
				sgf.setArguments(iargs);
				sgf.onAttach(getActivity());
				bld.setView(sgf.onCreateView(getActivity().getLayoutInflater(), null, null));
			} else {
				bld.setMessage(R.string.stage_is_completed);
			}
			bld.setTitle(getString(R.string.stage_is_completed_short))
					.setPositiveButton(R.string.default_buttons_ok, null);
			if (args != null && args.getBoolean(START_OVER)) {
				String id = args.getString(TOUR_PARAM);
				TourInformation tours = null;
				final SherpafyCustomization sherpafy = (SherpafyCustomization) app.getAppCustomization();
				for (TourInformation ti : sherpafy.getTourInformations()) {
					if (ti.getId().equals(id)) {
						tours = ti;
						break;
					}
				}
				int k = args.getInt(STAGE_PARAM);
				StageInformation stage = null;
				if (tours != null && tours.getStageInformation().size() > k) {
					stage = tours.getStageInformation().get(k);
				}
				final StageInformation stageInformation = stage;
				final TourInformation tour = tours;
				if (stage != null) {
					bld.setNegativeButton(R.string.restart_stage, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							sherpafy.markStageAsNotCompleted(stageInformation);
							sherpafy.runStage(getActivity(), tour, stageInformation, true);
						}
					});
				}
			}
			return bld.create();
		}
	}
	
	protected void showCompleteStageFragment(final FragmentActivity activity, final StageInformation stage, final boolean showRestart) {
		File dir = getStageGpxRec(stage);
		final File[] fs = dir.listFiles();
		new AsyncTask<Void, Void, List<GPXUtilities.GPXFile>>() {

			@Override
			protected List<GPXFile> doInBackground(Void... params) {
				List<GPXUtilities.GPXFile> gpxs = new ArrayList<GPXUtilities.GPXFile>();
				if(fs != null) {
					for(File f : fs) {
						if(f.getName().endsWith(".gpx")) {
							gpxs.add(GPXUtilities.loadGPXFile(app, f));		
						}
					}
				}
				return gpxs;
			}
			
			protected void onPostExecute(java.util.List<GPXFile> result) {
				CompleteStageFragment csf = new CompleteStageFragment();
				Bundle bl = new Bundle();
				bl.putBoolean(CompleteStageFragment.START_OVER, showRestart);
				if (stage != null) {
					bl.putInt(CompleteStageFragment.STAGE_PARAM, stage.order);
					bl.putString(CompleteStageFragment.TOUR_PARAM, stage.tour.getId());
				}
				csf.setArguments(bl);
				app.getSelectedGpxHelper().clearAllGpxFileToShow();
				for(GPXFile g : result) {
					app.getSelectedGpxHelper().selectGpxFile(g, true, false);
				}
				activity.getSupportFragmentManager().beginTransaction().add(csf, "DialogFragment").commit();
			};
		}.execute((Void)null);
	}
	
	public StageInformation getSelectedStage() {
		return selectedStage;
	}

	public void selectStage(StageInformation stage, IProgress progress) {
		saveCurrentGPXTrack();
		if(stage == null) {
			selectedStagePref.set(-1);
			saveGPXFolder.set(null);
			selectedStage = null;
		} else {
			selectedStagePref.set(stage.getOrder());
			selectedStage = stage;
			File fl = getStageGpxRec(stage);
			fl.mkdirs();
			saveGPXFolder.set(fl.getAbsolutePath());
		}
		loadSelectedStage();
	}

	protected File getStageGpxRec(StageInformation stage) {
		return new File(stage.tour.getFolder(), "record" + stage.getOrder());
	}

	private void loadSelectedStage() {
		final StageInformation st = selectedStage;
		cachedFavorites = new ArrayList<StageFavorite>();
		for(Object o : st.favorites ) {
			if(o instanceof StageFavorite) {
				StageFavorite sf = (StageFavorite) o;
				cachedFavorites.add(sf);
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
			originalApi.edit(originalGlobal).putString(SELECTED_TOUR, null).commit();
		} else {
			originalApi.edit(originalGlobal).putString(SELECTED_TOUR, tour.getName()).commit();
		}
		selectedTour = tour;
		reloadSelectedTour(progress, tour);
	}

	@Override
	public void prepareLayerContextMenu(MapActivity activity, ContextMenuAdapter adapter) {
		filter(adapter, R.string.layer_poi, R.string.layer_amenity_label/*, R.string.layer_favorites*/);
	}
	
	@Override
	public void prepareLocationMenu(final MapActivity mapActivity, ContextMenuAdapter adapter) {
		filter(adapter, R.string.context_menu_item_directions_to,
				R.string.context_menu_item_destination_point, R.string.context_menu_item_search,
				R.string.context_menu_item_share_location/*, R.string.context_menu_item_add_favorite*/);
		MapActivityLayers layers = mapActivity.getMapLayers();
		if(layers.getContextMenuLayer().getFirstSelectedObject() instanceof StageFavorite) {
			final StageFavorite sf = ((StageFavorite)layers.getContextMenuLayer().getFirstSelectedObject());
			if(selectedStage != null) {
				adapter.item(R.string.show_waypoint_information).icons(R.drawable.ic_action_info_dark, R.drawable.ic_action_info_light ).position(0)
				.listen(new OnContextMenuClick() {
					@Override
					public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
						showFavoriteDialog(mapActivity, selectedStage, sf);
					}
				}).reg();
				
				
			}
		}
	}
	
	public void showFavoriteDialog(SherlockFragmentActivity mapActivity, StageInformation stage, StageFavorite sf) {
		Bundle bl = new Bundle();
		bl.putInt(SherpafyFavoriteFragment.STAGE_PARAM, stage.getOrder());
		bl.putString(SherpafyFavoriteFragment.TOUR_PARAM, stage.getTour().getId());
		bl.putInt(SherpafyFavoriteFragment.FAV_PARAM, sf.getOrder());
		FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
		final FavoriteDialogFragment ffd = new FavoriteDialogFragment();
		ffd.setArguments(bl);
		ffd.show(fragmentManager.beginTransaction(), "DialogFragment");
	}
	
	public static class FavoriteDialogFragment extends DialogFragment {
		
		@Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
			Bundle args = getArguments();
			SherpafyFavoriteFragment ssf = new SherpafyFavoriteFragment();
			ssf.setArguments(args);
			ssf.onAttach(getActivity());
            AlertDialog dlg = new AlertDialog.Builder(getActivity())
            		.setView(ssf.onCreateView(getActivity().getLayoutInflater(), null, savedInstanceState))
                    .setPositiveButton(R.string.default_buttons_ok, null)
                    .create();
            return dlg;
        }
    }

	@Override
	public void prepareOptionsMenu(final MapActivity mapActivity, ContextMenuAdapter adapter) {
		filter(adapter, R.string.menu_layers,
				R.string.pause_navigation, R.string.continue_navigation,  
				R.string.cancel_navigation, R.string.cancel_route, R.string.clear_destination,
				R.string.target_points,
				R.string.get_directions, 
				R.string.menu_mute_on, R.string.menu_mute_off,
				R.string.where_am_i, R.string.context_menu_item_share_location);
		final StageInformation stage = getSelectedStage();
		if (stage != null && !isStageVisited(stage.order)) {
			adapter.item(R.string.complete_stage)
					.icons(R.drawable.ic_action_finish_flag_dark, R.drawable.ic_action_finish_flag_light)
					.position(adapter.length() - 1).listen(new OnContextMenuClick() {
						@Override
						public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
							markStageAsCompleted(stage);
							showCompleteStageFragment(mapActivity, stage, false);
						}
					}).reg();
		}
		adapter.item(R.string.sherpafy_tour_info_txt).icons(R.drawable.ic_action_info_dark, R.drawable.ic_action_info_light).position(adapter.length() - 1)
				.listen(new OnContextMenuClick() {
					@Override
					public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
						Intent newIntent = new Intent(mapActivity, TourViewActivity.class);
						// newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						mapActivity.startActivity(newIntent);
					}
				}).reg();

		//share my location
		adapter.item(R.string.context_menu_item_share_location).icons(
				R.drawable.ic_action_gshare_dark, R.drawable.ic_action_gshare_light).listen(new OnContextMenuClick() {
			@Override
			public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
				if (app.getLocationProvider().getLastKnownLocation() != null) {
					new ShareLocation(mapActivity).run();
				} else {
					Toast.makeText(app, R.string.unknown_location, Toast.LENGTH_LONG).show();
				}
			}
		}).reg();

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
	public List<StageFavorite> getWaypoints() {
		return cachedFavorites;
	}
	
	@Override
	public String getIndexesUrl() {
		String s = "http://"+TOUR_SERVER+"/tours.php?gzip&" + Version.getVersionAsURLParam(app);
		if(!Algorithms.isEmpty(accessCodePref.get())) {
			s += "&code="+accessCodePref.get();
		}
		return s;
	}
	
	public void preDownloadActivity(final DownloadActivity da, final List<DownloadActivityType> downloadTypes, ActionBar actionBar) {
		actionBar.setTitle(TourDownloadType.TOUR.getString(da));
	}
	
	@Override
	public boolean showDownloadExtraActions() {
		return false;
	}
	
	public boolean saveGPXPoint(Location location) {
		return app.getRoutingHelper().isFollowingMode() && !Algorithms.isEmpty(saveGPXFolder.get());
	}
	
	@Override
	public void createLayers(OsmandMapTileView mapView, MapActivity activity) {
		mapView.addLayer(new StageFavoritesLayer(app, null), 4.1f);
	}
	
	public boolean isWaypointGroupVisible(int waypointType, RouteCalculationResult route) {
		return waypointType == WaypointHelper.WAYPOINTS || 
				waypointType == WaypointHelper.TARGETS || 
				waypointType == WaypointHelper.POI;
	}

	
	public void showLocationPoint(MapActivity ctx, LocationPoint locationPoint) {
		if(locationPoint instanceof StageFavorite && getSelectedStage() != null) {
			showFavoriteDialog(ctx, getSelectedStage(), (StageFavorite) locationPoint);
		}
	}
	
	public boolean onDestinationReached() {
		final MapActivity map = app.getMapActivity();
		if(map != null && getSelectedStage() != null) {
			app.runInUIThread(new Runnable() {
				
				@Override
				public void run() {
					showCompleteStageFragment(map, getSelectedStage(), false);
				}
			});
		}
		return true;
	}
	
	public void runStage(Activity a, TourInformation tour, StageInformation stage, boolean startOver) {
		WptPt point = null;
		GPXFile gpx = null;
		SherpafyCustomization customization = this;
		customization.selectTour(tour, IProgress.EMPTY_PROGRESS);

		customization.selectStage(stage, IProgress.EMPTY_PROGRESS);
		if (customization.getSelectedStage() != null) {
			gpx = customization.getSelectedStage().getGpx();
			List<SelectedGpxFile> sgpx = app.getSelectedGpxHelper().getSelectedGPXFiles();
			if (gpx == null){
				app.getSelectedGpxHelper().clearAllGpxFileToShow();
			} else if (sgpx.size() != 1 || sgpx.get(0).getGpxFile() != gpx) {
				app.getSelectedGpxHelper().clearAllGpxFileToShow();
				if (gpx != null && gpx.findPointToShow() != null) {
					point = gpx.findPointToShow();
					app.getSelectedGpxHelper().setGpxFileToDisplay(gpx);
				}
			}
		}
		if (gpx != null) {
			WptPt lp = gpx.getLastPoint();
			if (lp != null) {
				TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
				targetPointsHelper.navigateToPoint(new LatLon(lp.lat, lp.lon), true, -1, lp.name);
				app.getSettings().navigateDialog(true);
			}
		}
		String mode = stage != null ? stage.getMode() : tour.getMode();
		if (!Algorithms.isEmpty(mode)) {
			final ApplicationMode def = app.getSettings().getApplicationMode();
			ApplicationMode am = ApplicationMode.valueOfStringKey(mode, def);
			if (am != def) {
				app.getSettings().APPLICATION_MODE.set(am);
			}
		}
		if (startOver && point != null) {
			goToMap(a, new LatLon(point.lat, point.lon));
		} else {
			goToMap(a, stage.getStartPoint());
		}
	}
	
	public void goToMap(Activity a, LatLon location) {
		if (location != null) {
			app.getSettings().setMapLocationToShow(location.getLatitude(), location.getLongitude(), 16, null);
		}
		Intent newIntent = new Intent(a, getMapActivity());
		newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		a.startActivityForResult(newIntent, 0);
	}
	
	public int getWaypointSearchRadius(int searchDeviationRadius, int type) {
		if(type == WaypointHelper.WAYPOINTS) {
			return Math.min(2000, searchDeviationRadius);
		}
		return searchDeviationRadius;
	}

	@Override
	public boolean showNavigationControls() {
		return false;
	}
}
