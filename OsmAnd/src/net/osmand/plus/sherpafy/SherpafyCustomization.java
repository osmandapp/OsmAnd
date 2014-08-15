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
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
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
import net.osmand.plus.activities.SelectedGPXFragment;
import net.osmand.plus.api.FileSettingsAPIImpl;
import net.osmand.plus.api.SettingsAPI;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.sherpafy.TourInformation.StageFavorite;
import net.osmand.plus.sherpafy.TourInformation.StageInformation;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.Algorithms;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.Window;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;

public class SherpafyCustomization extends OsmAndAppCustomization {

	private static final String SELECTED_TOUR = "selected_tour";
	private static final String ACCESS_CODE = "access_code";
	private static final String SELECTED_STAGE = "selected_stage_int";
	private static final String VISITED_STAGES = "visited_stages_int";
	private CommonPreference<String> selectedTourPref;
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

	@Override
	public void setup(OsmandApplication app) {
		super.setup(app);
		originalApi = osmandSettings.getSettingsAPI();
		selectedTourPref = osmandSettings.registerStringPreference(SELECTED_TOUR, null).makeGlobal();
		saveGPXFolder = osmandSettings.registerStringPreference(SAVE_GPX_FOLDER, null).makeGlobal();
		if (osmandSettings.OSMAND_THEME.get() != OsmandSettings.OSMAND_LIGHT_THEME) {
			osmandSettings.OSMAND_THEME.set(OsmandSettings.OSMAND_LIGHT_THEME);
		}
		accessCodePref = osmandSettings.registerStringPreference(ACCESS_CODE, "").makeGlobal();
		toursFolder = new File(osmandSettings.getExternalStorageDirectory(), "osmand/tours");
	}

	public boolean setAccessCode(String acCode) {
		acCode = acCode.toUpperCase();
		if (DownloadIndexActivity.downloadListIndexThread != null) {
			DownloadIndexActivity.downloadListIndexThread.clear();
		}
		if (validate(acCode) || Algorithms.isEmpty(acCode)) {
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

	public boolean isSettingsAvailable() {
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
		TextView v = (TextView) window.findViewById(R.id.AppName);
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

	public void updatedLoadedFiles(java.util.Map<String, String> indexFileNames, java.util.Map<String, String> indexActivatedFileNames) {
		DownloadIndexActivity.listWithAlternatives(app.getResourceManager().getDateFormat(),
				toursFolder, "", indexFileNames);
	}

	public List<String> onIndexingFiles(IProgress progress, Map<String, String> indexFileNames) {
		ArrayList<TourInformation> tourPresent = new ArrayList<TourInformation>();
		List<String> warns = new ArrayList<String>();
		selectedTour = null;
		final HashSet<String> suggestToDownloadMap = new HashSet<String>();
		if (toursFolder.exists()) {
			File[] availableTours = toursFolder.listFiles();
			if (availableTours != null) {
				String selectedName = selectedTourPref.get();
				for (File tr : availableTours) {
					if (tr.isDirectory()) {
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
						// check that tour was downloaded
						if (toursIndexed) {
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
		if (!suggestToDownloadMap.isEmpty()) {
			final DownloadIndexActivity da = app.getDownloadActivity();
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
		if (progress != null) {
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

	private void selectNextAvailableStage(final TourInformation tourInformation) {
		Integer it = selectedStagePref.get();
		while (it >= 0 && isStageVisited(it)) {
			it++;
		}
		if (it >= 0 && it < tourInformation.getStageInformation().size()) {
			selectedStage = tourInformation.getStageInformation().get(it);
		}
	}

	public boolean isStageVisited(int stageOrder) {
		Integer gi = visitedStagesPref.get();
		return (gi & (1 << stageOrder)) > 0;
	}

	public void markStageAsCompleted(StageInformation si) {
		Integer gi = visitedStagesPref.get();
		gi |= (1 << si.getOrder());
		visitedStagesPref.set(gi);
		saveCurrentGPXTrack();
		selectNextAvailableStage(si.tour);
	}

	protected void saveCurrentGPXTrack() {
		if (!Algorithms.isEmpty(saveGPXFolder.get())) {
			app.getSavingTrackHelper().saveDataToGpx(new File(saveGPXFolder.get()));
		}
	}

	public File getTracksDir() {
		if (!Algorithms.isEmpty(saveGPXFolder.get())) {
			return new File(saveGPXFolder.get());
		}
		return app.getAppPath(IndexConstants.GPX_RECORDED_INDEX_DIR);
	}

	@Override
	public boolean onDestinationReached(MapActivity mapActivity) {
		showCompleteStageFragment(mapActivity, selectedStage);
		selectNextAvailableStage(selectedTour);
		return false;
	}

	public static class CompleteStageFragment extends DialogFragment {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			Bundle args = getArguments();
			SelectedGPXFragment sgf = new SelectedGPXFragment();
			sgf.setArguments(args);
			sgf.onAttach(getActivity());
			AlertDialog dlg = new AlertDialog.Builder(getActivity())
            		.setView(sgf.onCreateView(getActivity().getLayoutInflater(), null, savedInstanceState))
					.setMessage("Stage is completed TODO")
					.setPositiveButton(R.string.default_buttons_ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
								}
							}
					)
					.create();
			return dlg;
		}
	}

	protected void showCompleteStageFragment(MapActivity mapActivity, StageInformation stage) {
		File file = getStageGpxRec(stage);
		GPXUtilities.GPXFile gpx = GPXUtilities.loadGPXFile(app, file);
		if (gpx == null) {
			return;
		}
		CompleteStageFragment csf = new CompleteStageFragment();
		app.getSelectedGpxHelper().clearAllGpxFileToShow();
		app.getSelectedGpxHelper().selectGpxFile(gpx, true, false);
		csf.show(mapActivity.getSupportFragmentManager().beginTransaction(), "DialogFragment");

	}

	public StageInformation getSelectedStage() {
		return selectedStage;
	}

	public void selectStage(StageInformation stage, IProgress progress) {
		saveCurrentGPXTrack();
		if (stage == null) {
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
		for (Object o : st.favorites) {
			if (o instanceof StageFavorite) {
				StageFavorite sf = (StageFavorite) o;
				cachedFavorites.add(sf);
			}
		}
		if (st != null && st.gpxFile != null) {
			if (st.gpx == null) {
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
		filter(adapter, R.string.layer_poi, R.string.layer_amenity_label/*, R.string.layer_favorites*/);
	}

	@Override
	public void prepareLocationMenu(final MapActivity mapActivity, ContextMenuAdapter adapter) {
		filter(adapter, R.string.context_menu_item_directions_to,
				R.string.context_menu_item_destination_point, R.string.context_menu_item_search,
				R.string.context_menu_item_share_location/*, R.string.context_menu_item_add_favorite*/);
		MapActivityLayers layers = mapActivity.getMapLayers();
		if (layers.getContextMenuLayer().getFirstSelectedObject() instanceof StageFavorite) {
			final StageFavorite sf = ((StageFavorite) layers.getContextMenuLayer().getFirstSelectedObject());
			if (selectedStage != null) {
				adapter.item(R.string.show_waypoint_information).icons(R.drawable.ic_action_info_dark, R.drawable.ic_action_info_light).position(0)
						.listen(new OnContextMenuClick() {
							@Override
							public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
								showFavoriteDialog(mapActivity, selectedStage, sf);
							}
						}).reg();


			}
		}
	}

	public void showFavoriteDialog(MapActivity mapActivity, StageInformation stage, StageFavorite sf) {
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
					.setPositiveButton(R.string.default_buttons_ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
								}
							}
					)
					.create();
			return dlg;
		}
	}

	@Override
	public void prepareOptionsMenu(final MapActivity mapActivity, ContextMenuAdapter adapter) {

		filter(adapter, R.string.exit_Button, R.string.menu_layers,
				R.string.pause_navigation, R.string.continue_navigation,
				R.string.cancel_navigation, R.string.cancel_route, R.string.clear_destination,
				R.string.get_directions,
				R.string.menu_mute_on, R.string.menu_mute_off,
				R.string.where_am_i);
		final StageInformation stage = getSelectedStage();
		if (stage != null && !isStageVisited(stage.order)) {
			adapter.item(R.string.complete_stage)
					.icons(R.drawable.ic_action_flage_dark, R.drawable.ic_action_flage_light)
					.position(adapter.length() - 1).listen(new OnContextMenuClick() {
				@Override
				public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
					markStageAsCompleted(stage);
					showCompleteStageFragment(mapActivity, stage);
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
	}


	public void filter(ContextMenuAdapter a, Integer... ids) {
		if (isSettingsAvailable()) {
			return;
		}
		TreeSet<Integer> set = new TreeSet<Integer>(Arrays.asList(ids));
		for (int i = 0; i < a.length(); ) {
			int itemId = a.getItemId(i);
			if (set.contains(itemId)) {
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
		String s = "http://" + TOUR_SERVER + "/tours.php?gzip&" + Version.getVersionAsURLParam(app);
		if (!Algorithms.isEmpty(accessCodePref.get())) {
			s += "&code=" + accessCodePref.get();
		}
		return s;
	}

	public void preDownloadActivity(final DownloadIndexActivity da, final List<DownloadActivityType> downloadTypes, ActionBar actionBar) {
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
		mapView.addLayer(new StageFavoritesLayer(this), 4.1f);
	}
}
