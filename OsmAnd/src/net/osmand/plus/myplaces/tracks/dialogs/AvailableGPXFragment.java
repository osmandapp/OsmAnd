package net.osmand.plus.myplaces.tracks.dialogs;

import static net.osmand.plus.myplaces.MyPlacesActivity.GPX_TAB;
import static net.osmand.plus.myplaces.MyPlacesActivity.OPEN_GPX_DOCUMENT_REQUEST;
import static net.osmand.plus.myplaces.MyPlacesActivity.TAB_ID;
import static net.osmand.plus.track.helpers.GpxSelectionHelper.CURRENT_TRACK;
import static net.osmand.util.Algorithms.objectEquals;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.CallbackWithObject;
import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.OsmandBaseExpandableListAdapter;
import net.osmand.plus.base.OsmandExpandableListFragment;
import net.osmand.plus.base.SelectionBottomSheet.DialogStateListener;
import net.osmand.plus.base.SelectionBottomSheet.SelectableItem;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.myplaces.tracks.GpxActionsHelper;
import net.osmand.plus.mapmarkers.CoordinateInputDialogFragment;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.myplaces.favorites.dialogs.FragmentStateHolder;
import net.osmand.plus.myplaces.tracks.GpxSearchFilter;
import net.osmand.plus.myplaces.tracks.dialogs.MoveGpxFileBottomSheet.OnTrackFileMoveListener;
import net.osmand.plus.myplaces.tracks.tasks.DeleteGpxFilesTask.GpxFilesDeletionListener;
import net.osmand.plus.myplaces.tracks.tasks.LoadGpxInfosTask;
import net.osmand.plus.myplaces.tracks.tasks.LoadGpxInfosTask.LoadTracksListener;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.plugins.monitoring.SavingTrackHelper;
import net.osmand.plus.plugins.osmedit.asynctasks.UploadGPXFilesTask.UploadGpxListener;
import net.osmand.plus.plugins.osmedit.dialogs.UploadMultipleGPXBottomSheet;
import net.osmand.plus.plugins.osmedit.oauth.OsmOAuthHelper.OsmAuthorizationListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.TracksSortByMode;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.fragments.TrackMenuFragment.TrackMenuTab;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.track.helpers.GpxDbHelper.GpxDataItemCallback;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.track.helpers.SelectGpxTask.SelectGpxTaskListener;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FileUtils.RenameCallback;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AvailableGPXFragment extends OsmandExpandableListFragment implements
		FragmentStateHolder, OsmAuthorizationListener, OnTrackFileMoveListener,
		RenameCallback, UploadGpxListener, LoadTracksListener, GpxFilesDeletionListener {

	public static final String SELECTED_FOLDER_KEY = "selected_folder_key";

	private static final int SEARCH_ID = -1;

	private OsmandApplication app;
	private OsmandSettings settings;
	private GpxSelectionHelper selectedGpxHelper;
	private GpxActionsHelper gpxActionsHelper;

	private boolean selectionMode;
	private final List<GPXInfo> selectedItems = new ArrayList<>();
	private final Set<Integer> selectedGroups = new LinkedHashSet<>();
	private ActionMode actionMode;
	private LoadGpxInfosTask asyncLoader;
	private GpxIndexesAdapter allGpxAdapter;
	private ContextMenuAdapter optionsMenuAdapter;
	private boolean updateEnable;
	private GPXInfo currentRecording;
	private boolean showOnMapMode;
	private View currentGpxView;
	private View footerView;
	private boolean importing;
	private View emptyView;
	private SelectGpxTaskListener gpxTaskListener;
	private String selectedFolder;
	private boolean nightMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		gpxTaskListener = new SelectGpxTaskListener() {
			@Override
			public void gpxSelectionInProgress() {
				allGpxAdapter.notifyDataSetInvalidated();
			}

			@Override
			public void gpxSelectionStarted() {
				showProgressBar();
			}

			@Override
			public void gpxSelectionFinished() {
				hideProgressBar();
				allGpxAdapter.refreshSelected();
				allGpxAdapter.notifyDataSetChanged();
			}
		};
	}

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		app = (OsmandApplication) context.getApplicationContext();
		settings = app.getSettings();
		nightMode = !settings.isLightContent();
		currentRecording = new GPXInfo(getString(R.string.shared_string_currently_recording_track), null);
		currentRecording.setGpxFile(app.getSavingTrackHelper().getCurrentGpx());
		asyncLoader = new LoadGpxInfosTask(app, this);
		selectedGpxHelper = app.getSelectedGpxHelper();
		allGpxAdapter = new GpxIndexesAdapter();
		gpxActionsHelper = new GpxActionsHelper(requireActivity(), nightMode);
		gpxActionsHelper.setTargetFragment(this);
		gpxActionsHelper.setDeletionListener(this);
		setAdapter(allGpxAdapter);
	}

	public void startImport() {
		importing = true;
	}

	public void finishImport(boolean success) {
		if (success) {
			reloadTracks();
		}
		importing = false;
	}

	private void startHandler() {
		Handler updateCurrentRecordingTrack = new Handler();
		updateCurrentRecordingTrack.postDelayed(() -> {
			if (getView() != null && updateEnable) {
				updateCurrentTrack();
				if (selectedGpxHelper.getSelectedCurrentRecordingTrack() != null) {
					allGpxAdapter.notifyDataSetChanged();
				}
				startHandler();
			}
		}, 2000);
	}

	public List<GPXInfo> getSelectedItems() {
		return selectedItems;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!importing) {
			if (asyncLoader == null || asyncLoader.getGpxInfos() == null) {
				asyncLoader = new LoadGpxInfosTask(app, this);
				asyncLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else {
				allGpxAdapter.refreshSelected();
				allGpxAdapter.notifyDataSetChanged();
			}
		}
		updateCurrentTrack();

		updateEnable = true;
		startHandler();
		restoreState(getArguments());
	}

	@Override
	public void onPause() {
		super.onPause();
		updateEnable = false;
		if (actionMode != null) {
			actionMode.finish();
		}
	}

	public void updateCurrentTrack() {
		OsmandMonitoringPlugin plugin = PluginsHelper.getActivePlugin(OsmandMonitoringPlugin.class);
		if (currentGpxView == null || plugin == null) {
			return;
		}

		boolean isRecording = settings.SAVE_GLOBAL_TRACK_TO_GPX.get();

		ImageView icon = currentGpxView.findViewById(R.id.icon);
		icon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.monitoring_rec_big));
		icon.setVisibility(selectionMode && showOnMapMode ? View.GONE : View.VISIBLE);

		SavingTrackHelper sth = app.getSavingTrackHelper();

		int activeColorId = ColorUtilities.getActiveColorId(nightMode);
		Button stop = currentGpxView.findViewById(R.id.action_button);
		if (isRecording) {
			currentGpxView.findViewById(R.id.segment_time_div).setVisibility(View.VISIBLE);
			TextView segmentTime = currentGpxView.findViewById(R.id.segment_time);
			segmentTime.setText(OsmAndFormatter.getFormattedDurationShort((int) (sth.getDuration() / 1000)));
			segmentTime.setVisibility(View.VISIBLE);
			Drawable stopIcon = app.getUIUtilities().getIcon(R.drawable.ic_action_rec_stop, activeColorId);
			stop.setCompoundDrawablesWithIntrinsicBounds(stopIcon, null, null, null);
			stop.setText(app.getString(R.string.shared_string_control_stop));
			stop.setContentDescription(app.getString(R.string.gpx_monitoring_stop));
		} else {
			currentGpxView.findViewById(R.id.segment_time_div).setVisibility(View.GONE);
			currentGpxView.findViewById(R.id.segment_time).setVisibility(View.GONE);
			Drawable stopIcon = app.getUIUtilities().getIcon(R.drawable.ic_action_rec_start, activeColorId);
			stop.setCompoundDrawablesWithIntrinsicBounds(stopIcon, null, null, null);
			stop.setText(app.getString(R.string.shared_string_record));
			stop.setContentDescription(app.getString(R.string.gpx_monitoring_start));
		}
		stop.setOnClickListener(v -> {
			if (isRecording) {
				plugin.stopRecording();
				updateCurrentTrack();
			} else if (app.getLocationProvider().checkGPSEnabled(getActivity())) {
				plugin.startGPXMonitoring(getActivity());
				updateCurrentTrack();
			}
		});
		Button save = currentGpxView.findViewById(R.id.save_button);
		Drawable saveIcon = app.getUIUtilities().getIcon(R.drawable.ic_action_gsave_dark, activeColorId);
		save.setCompoundDrawablesWithIntrinsicBounds(saveIcon, null, null, null);
		save.setOnClickListener(v -> {
			plugin.saveCurrentTrack(() -> {
				if (isResumed()) {
					reloadTracks();
				}
			});
			updateCurrentTrack();
		});
		if (sth.getPoints() > 0 || sth.getDistance() > 0) {
			save.setVisibility(View.VISIBLE);
		} else {
			save.setVisibility(View.GONE);
		}
		save.setContentDescription(app.getString(R.string.save_current_track));

		((TextView) currentGpxView.findViewById(R.id.points_count)).setText(String.valueOf(sth.getPoints()));
		((TextView) currentGpxView.findViewById(R.id.distance))
				.setText(OsmAndFormatter.getFormattedDistance(sth.getDistance(), app));

		CheckBox checkbox = currentGpxView.findViewById(R.id.check_local_index);
		checkbox.setVisibility(selectionMode && showOnMapMode ? View.VISIBLE : View.GONE);
		if (selectionMode && showOnMapMode) {
			checkbox.setChecked(selectedItems.contains(currentRecording));
			checkbox.setOnClickListener(v -> {
				if (checkbox.isChecked()) {
					selectedItems.add(currentRecording);
				} else {
					selectedItems.remove(currentRecording);
				}
				updateSelectionMode(actionMode);
			});
		}

	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.available_gpx, container, false);
		listView = v.findViewById(android.R.id.list);
		setHasOptionsMenu(true);
		if (PluginsHelper.isActive(OsmandMonitoringPlugin.class)) {
			currentGpxView = inflater.inflate(R.layout.current_gpx_item, null, false);
			createCurrentTrackView();
			currentGpxView.findViewById(R.id.current_track_info).setOnClickListener(v1 -> {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					String name = getString(R.string.shared_string_tracks);
					boolean temporarySelected = selectedGpxHelper.getSelectedCurrentRecordingTrack() == null;
					TrackMenuFragment.openTrack(activity, null, storeState(), name, TrackMenuTab.OVERVIEW, temporarySelected);
				}
			});
			listView.addHeaderView(currentGpxView);
		}
		footerView = inflater.inflate(R.layout.list_shadow_footer, null, false);
		listView.addFooterView(footerView);
		emptyView = v.findViewById(android.R.id.empty);
		ImageView emptyImageView = emptyView.findViewById(R.id.empty_state_image_view);
		emptyImageView.setImageResource(nightMode ? R.drawable.ic_empty_state_trip_night : R.drawable.ic_empty_state_trip_day);
		Button importButton = emptyView.findViewById(R.id.import_button);
		importButton.setOnClickListener(view -> addTrack());
		if (adapter != null) {
			listView.setAdapter(adapter);
		}

		listView.setOnScrollListener(new AbsListView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView absListView, int i) {
				View currentFocus = getActivity().getCurrentFocus();
				if (currentFocus != null) {
					InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
				}
			}

			@Override
			public void onScroll(AbsListView absListView, int i, int i1, int i2) {

			}
		});

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		listView.setBackgroundColor(ColorUtilities.getActivityBgColor(app, nightMode));
	}

	public void createCurrentTrackView() {
		ImageView distanceI = currentGpxView.findViewById(R.id.distance_icon);
		distanceI.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_distance_16));
		ImageView pointsI = currentGpxView.findViewById(R.id.points_icon);
		pointsI.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_waypoint_16));
		updateCurrentTrack();
	}

	public void reloadTracks() {
		asyncLoader = new LoadGpxInfosTask(app, this);
		asyncLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, @NonNull MenuInflater inflater) {
		menu.clear();
		MenuItem mi = createMenuItem(menu, SEARCH_ID, R.string.search_poi_filter, R.drawable.ic_action_search_dark,
				MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		SearchView searchView = new SearchView(getActivity());
		updateSearchView(searchView);
		mi.setActionView(searchView);
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

			@Override
			public boolean onQueryTextSubmit(String query) {
				allGpxAdapter.getFilter().filter(query);
				return true;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				allGpxAdapter.getFilter().filter(newText);
				return true;
			}
		});
		mi.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
			@Override
			public boolean onMenuItemActionExpand(MenuItem item) {
				return true;
			}

			@Override
			public boolean onMenuItemActionCollapse(MenuItem item) {
				// Needed to hide intermediate progress bar after closing action mode
				new Handler().postDelayed(() -> hideProgressBar(), 100);
				return true;
			}
		});

		inflater.inflate(R.menu.track_sort_menu_item, menu);
		mi = menu.findItem(R.id.action_sort);
		mi.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		int iconColorId = ColorUtilities.getActiveButtonsAndLinksTextColorId(!isLightActionBar());
		mi.setIcon(getIcon(settings.TRACKS_SORT_BY_MODE.get().getIconId(), iconColorId));

		if (AndroidUiHelper.isOrientationPortrait(getActivity())) {
			menu = ((MyPlacesActivity) getActivity()).getClearToolbar(true).getMenu();
		} else {
			((MyPlacesActivity) getActivity()).getClearToolbar(false);
		}
		((MyPlacesActivity) getActivity()).updateListViewFooter(footerView);

		// To do Rewrite without ContextMenuAdapter
		optionsMenuAdapter = new ContextMenuAdapter(app);
		ItemClickListener listener = (uiAdapter, view, item, isChecked) -> {
			int itemId = item.getTitleId();
			if (itemId == R.string.shared_string_refresh) {
				reloadTracks();
			} else if (itemId == R.string.shared_string_show_on_map) {
				openShowOnMapMode();
			} else if (itemId == R.string.shared_string_delete) {
				openSelectionMode(R.string.shared_string_delete, R.drawable.ic_action_delete_dark, R.drawable.ic_action_delete_dark,
						items -> doAction(itemId));
			} else if (itemId == R.string.gpx_add_track) {
				addTrack();
			} else if (itemId == R.string.coordinate_input) {
				openCoordinatesInput();
			}
			return true;
		};
		optionsMenuAdapter.addItem(new ContextMenuItem(null)
				.setTitleId(R.string.gpx_add_track, getActivity())
				.setIcon(R.drawable.ic_action_plus)
				.setListener(listener));
		optionsMenuAdapter.addItem(new ContextMenuItem(null)
				.setTitleId(R.string.coordinate_input, getActivity())
				.setIcon(R.drawable.ic_action_coordinates_longitude)
				.setListener(listener));
		optionsMenuAdapter.addItem(new ContextMenuItem(null)
				.setTitleId(R.string.shared_string_show_on_map, getActivity())
				.setIcon(R.drawable.ic_show_on_map)
				.setListener(listener));
		optionsMenuAdapter.addItem(new ContextMenuItem(null)
				.setTitleId(R.string.shared_string_delete, getActivity())
				.setIcon(R.drawable.ic_action_delete_dark)
				.setListener(listener));
		optionsMenuAdapter.addItem(new ContextMenuItem(null)
				.setTitleId(R.string.shared_string_refresh, getActivity())
				.setIcon(R.drawable.ic_action_refresh_dark)
				.setListener(listener));
		PluginsHelper.onOptionsMenuActivity(getActivity(), this, optionsMenuAdapter);
		for (int j = 0; j < optionsMenuAdapter.length(); j++) {
			MenuItem item;
			ContextMenuItem contextMenuItem = optionsMenuAdapter.getItem(j);
			item = menu.add(0, contextMenuItem.getTitleId(), j + 1, contextMenuItem.getTitle());
			item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			if (AndroidUiHelper.isOrientationPortrait(getActivity())) {
				item.setOnMenuItemClickListener(menuItem -> {
					onOptionsItemSelected(item);
					return true;
				});
			}
			if (contextMenuItem.getIcon() != -1) {
				OsmandApplication app = requireMyApplication();
				int colorId = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);
				Drawable icMenuItem = app.getUIUtilities().getIcon(contextMenuItem.getIcon(), colorId);
				item.setIcon(icMenuItem);
			}
		}
	}

	private void updateSearchView(@NonNull SearchView searchView) {
		//do not ever do like this
		if (!app.getSettings().isLightContent()) {
			return;
		}
		try {
			ImageView cancelIcon = searchView.findViewById(R.id.search_close_btn);
			cancelIcon.setImageResource(R.drawable.ic_action_gremove_dark);
			//styling search hint icon and text
			SearchView.SearchAutoComplete searchEdit = searchView.findViewById(R.id.search_src_text);
			searchEdit.setTextColor(app.getColor(R.color.color_white));
			SpannableStringBuilder stopHint = new SpannableStringBuilder("   ");
			float rawTextSize = searchEdit.getTextSize();
			int textSize = (int) (rawTextSize * 1.25);

			//setting icon as spannable
			Drawable searchIcon = AppCompatResources.getDrawable(app, R.drawable.ic_action_search_dark);
			if (searchIcon != null) {
				searchIcon.setBounds(0, 0, textSize, textSize);
				stopHint.setSpan(new ImageSpan(searchIcon), 1, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				searchEdit.setHint(stopHint);
			}
		} catch (Exception e) {
			// ignore
		}
	}

	public void doAction(int actionResId) {
		if (actionResId == R.string.shared_string_delete) {
			gpxActionsHelper.deleteGpxFiles(selectedItems);
		}
		if (actionMode != null) {
			actionMode.finish();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		for (int i = 0; i < optionsMenuAdapter.length(); i++) {
			ContextMenuItem contextMenuItem = optionsMenuAdapter.getItem(i);
			if (itemId == contextMenuItem.getTitleId()) {
				ItemClickListener listener = contextMenuItem.getItemClickListener();
				if (listener != null) {
					listener.onContextMenuClick(null, null, contextMenuItem, false);
				}
				return true;
			}
		}
		if (itemId == R.id.action_sort) {
			Activity activity = getActivity();
			if (activity != null) {
				showSortPopUpMenu(activity.findViewById(R.id.action_sort), item);
			}
		}
		return super.onOptionsItemSelected(item);
	}

	private void showSortPopUpMenu(@NonNull View anchorView, @NonNull MenuItem item) {
		List<PopUpMenuItem> menuItems = new ArrayList<>();
		for (TracksSortByMode mode : TracksSortByMode.values()) {
			menuItems.add(new PopUpMenuItem.Builder(app)
					.setTitleId(mode.getNameId())
					.setIcon(getContentIcon(mode.getIconId()))
					.setOnClickListener(v -> {
						updateTracksSort(mode);
						int iconColorId = ColorUtilities.getActiveButtonsAndLinksTextColorId(!isLightActionBar());
						item.setIcon(getIcon(mode.getIconId(), iconColorId));
					})
					.setSelected(settings.TRACKS_SORT_BY_MODE.get() == mode)
					.create()
			);
		}
		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = anchorView;
		displayData.menuItems = menuItems;
		displayData.nightMode = nightMode;
		PopUpMenu.show(displayData);
	}

	private void addTrack() {
		Intent intent = ImportHelper.getImportTrackIntent();
		AndroidUtils.startActivityForResultIfSafe(this, intent, OPEN_GPX_DOCUMENT_REQUEST);
	}

	private void updateTracksSort(TracksSortByMode sortByMode) {
		settings.TRACKS_SORT_BY_MODE.set(sortByMode);
		reloadTracks();
	}

	private void openCoordinatesInput() {
		CoordinateInputDialogFragment fragment = new CoordinateInputDialogFragment();
		fragment.setRetainInstance(true);
		fragment.show(getChildFragmentManager(), CoordinateInputDialogFragment.TAG);
	}

	public void showProgressBar() {
		if (getActivity() != null) {
			((MyPlacesActivity) getActivity()).setSupportProgressBarIndeterminateVisibility(true);
		}
	}

	public void hideProgressBar() {
		if (getActivity() != null) {
			((MyPlacesActivity) getActivity()).setSupportProgressBarIndeterminateVisibility(false);
		}
	}

	private void updateSelectionMode(ActionMode m) {
		if (selectedItems.size() > 0) {
			m.setTitle(selectedItems.size() + " " + app.getString(R.string.shared_string_selected_lowercase));
		} else {
			m.setTitle("");
		}
	}

	private void enableSelectionMode(boolean selectionMode) {
		this.selectionMode = selectionMode;
		if (AndroidUiHelper.isOrientationPortrait(getActivity())) {
			((MyPlacesActivity) getActivity()).setToolbarVisibility(!selectionMode &&
					AndroidUiHelper.isOrientationPortrait(getActivity()));
			((MyPlacesActivity) getActivity()).updateListViewFooter(footerView);
		}
	}

	private void openShowOnMapMode() {
		enableSelectionMode(true);
		showOnMapMode = true;
		selectedItems.clear();
		selectedGroups.clear();
		Set<GPXInfo> originalSelectedItems = allGpxAdapter.getSelectedGpx();
		selectedItems.addAll(originalSelectedItems);

		actionMode = getActionBarActivity().startSupportActionMode(new ActionMode.Callback() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				enableSelectionMode(true);
				updateSelectionMode(mode);
				MenuItem it = menu.add(R.string.shared_string_show_on_map);
				it.setIcon(R.drawable.ic_action_done);
				it.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM
						| MenuItem.SHOW_AS_ACTION_WITH_TEXT);
				updateCurrentTrack();
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				runSelection(originalSelectedItems);
				actionMode.finish();
				allGpxAdapter.refreshSelected();
				allGpxAdapter.notifyDataSetChanged();
				return true;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				showOnMapMode = false;
				enableSelectionMode(false);
				updateCurrentTrack();
				allGpxAdapter.notifyDataSetChanged();
			}

		});
		allGpxAdapter.notifyDataSetChanged();
	}

	public void runSelection(Set<GPXInfo> originalSelectedItems) {
		HashMap<String, Boolean> selectedItemsFileNames = new HashMap<>();
		originalSelectedItems.addAll(selectedItems);
		for (GPXInfo gpxInfo : originalSelectedItems) {
			String path = gpxInfo.isCurrentRecordingTrack() ? CURRENT_TRACK : gpxInfo.getFile().getAbsolutePath();
			selectedItemsFileNames.put(path, selectedItems.contains(gpxInfo));
		}
		selectedGpxHelper.runSelection(selectedItemsFileNames, gpxTaskListener);
	}

	public void openSelectionMode(int actionResId, int darkIcon, int lightIcon,
	                              @Nullable SelectionModeListener listener) {
		int actionIconId = !isLightActionBar() ? darkIcon : lightIcon;
		String value = app.getString(actionResId);
		if (value.endsWith("...")) {
			value = value.substring(0, value.length() - 3);
		}
		String actionButton = value;
		if (allGpxAdapter.getGroupCount() == 0) {
			showNoItemsForActionsToast(actionButton);
			return;
		}

		enableSelectionMode(true);
		selectedItems.clear();
		selectedGroups.clear();
		actionMode = getActionBarActivity().startSupportActionMode(new ActionMode.Callback() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				enableSelectionMode(true);
				MenuItem it = menu.add(actionResId);
				if (actionIconId != 0) {
					it.setIcon(actionIconId);
				}
				it.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM
						| MenuItem.SHOW_AS_ACTION_WITH_TEXT);
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				if (selectedItems.isEmpty()) {
					showNoItemsForActionsToast(actionButton);
					return true;
				}
				FragmentActivity activity = getActivity();
				if (activity != null) {
					if (actionResId == R.string.shared_string_delete) {
						showDeleteConfirmationDialog(activity, actionButton, listener);
					} else if (actionResId == R.string.local_index_mi_upload_gpx) {
						showUploadConfirmationDialog(activity, actionButton, listener);
					}
				}
				return true;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				enableSelectionMode(false);
				allGpxAdapter.notifyDataSetChanged();
			}

		});
		allGpxAdapter.notifyDataSetChanged();
	}

	private void showNoItemsForActionsToast(@NonNull String action) {
		if (app != null) {
			String message = getString(R.string.local_index_no_items_to_do, action.toLowerCase());
			app.showShortToastMessage(Algorithms.capitalizeFirstLetter(message));
		}
	}

	private void showUploadConfirmationDialog(@NonNull FragmentActivity activity,
	                                          @NonNull String actionButton,
	                                          @Nullable SelectionModeListener listener) {
		long[] size = new long[1];
		List<SelectableItem<GPXInfo>> items = new ArrayList<>();
		for (GPXInfo gpxInfo : selectedItems) {
			SelectableItem<GPXInfo> item = new SelectableItem<>();
			item.setObject(gpxInfo);
			item.setTitle(gpxInfo.getName());
			item.setIconId(R.drawable.ic_notification_track);

			items.add(item);
			size[0] += gpxInfo.getIncreasedFileSize();
		}
		List<SelectableItem<GPXInfo>> selectedItems = new ArrayList<>(items);
		FragmentManager manager = activity.getSupportFragmentManager();
		UploadMultipleGPXBottomSheet dialog = UploadMultipleGPXBottomSheet.showInstance(manager, items, selectedItems);
		if (dialog != null) {
			dialog.setDialogStateListener(new DialogStateListener() {
				@Override
				public void onDialogCreated() {
					dialog.setTitle(actionButton);
					dialog.setApplyButtonTitle(getString(R.string.shared_string_continue));
					String total = getString(R.string.shared_string_total);
					dialog.setTitleDescription(getString(R.string.ltr_or_rtl_combine_via_colon, total,
							AndroidUtils.formatSize(app, size[0])));
				}

				@Override
				public void onCloseDialog() {
				}
			});
			dialog.setOnApplySelectionListener(selItems -> {
				List<GPXInfo> gpxInfos = new ArrayList<>();
				for (SelectableItem<GPXInfo> item : selItems) {
					gpxInfos.add(item.getObject());
				}
				if (listener != null) {
					listener.onItemsSelected(gpxInfos);
				}
			});
		}
	}

	private void showDeleteConfirmationDialog(@NonNull FragmentActivity activity,
	                                          @NonNull String actionButton,
	                                          @Nullable SelectionModeListener listener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setMessage(getString(R.string.local_index_action_do, actionButton.toLowerCase(),
				String.valueOf(selectedItems.size())));
		builder.setPositiveButton(actionButton, (dialog, which) -> {
			if (listener != null) {
				listener.onItemsSelected(selectedItems);
			}
		});
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.show();
	}

	@Override
	public Bundle storeState() {
		Bundle bundle = new Bundle();
		bundle.putInt(TAB_ID, GPX_TAB);
		return bundle;
	}

	@Override
	public void restoreState(Bundle bundle) {
		if (bundle != null && bundle.getInt(TAB_ID) == GPX_TAB) {
			selectedFolder = bundle.getString(SELECTED_FOLDER_KEY);
		}
	}

	@Override
	public void authorizationCompleted() {
		Bundle bundle = new Bundle();
		bundle.putInt(TAB_ID, GPX_TAB);

		Intent intent = new Intent(app, app.getAppCustomization().getMyPlacesActivity());
		intent.putExtra(MapActivity.INTENT_PARAMS, bundle);
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);

		app.startActivity(intent);
	}

	@Override
	public void onFileMove(@NonNull File src, @NonNull File dest) {
		File destFolder = dest.getParentFile();
		if (destFolder != null && !destFolder.exists() && !destFolder.mkdirs()) {
			app.showToastMessage(R.string.file_can_not_be_moved);
		} else if (dest.exists()) {
			app.showToastMessage(R.string.file_with_name_already_exists);
		} else if (src.renameTo(dest)) {
			app.getGpxDbHelper().rename(src, dest);
			asyncLoader = new LoadGpxInfosTask(app, this);
			asyncLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			app.showToastMessage(R.string.file_can_not_be_moved);
		}
	}

	public void renamedTo(File file) {
		reloadTracks();
	}

	@Override
	public void onGpxUploaded(String result) {
		if (actionMode != null) {
			actionMode.finish();
		}
	}

	@Override
	public void loadTracksStarted() {
		showProgressBar();
		listView.setEmptyView(null);
		allGpxAdapter.clear();
	}

	@Override
	public void loadTracksProgress(GPXInfo[] gpxInfos) {
		for (GPXInfo info : gpxInfos) {
			allGpxAdapter.addLocalIndexInfo(info);
		}
		allGpxAdapter.notifyDataSetChanged();
	}

	@Override
	public void loadTracksFinished() {
		allGpxAdapter.refreshSelected();
		hideProgressBar();
		listView.setEmptyView(emptyView);

		if (allGpxAdapter.getGroupCount() > 0) {
			if (allGpxAdapter.isShowingSelection()) {
				listView.expandGroup(0);
			}
			if (selectedFolder != null) {
				int index = allGpxAdapter.category.indexOf(selectedFolder);
				if (index != -1) {
					listView.expandGroup(index);
					listView.setSelection(index);
				}
				selectedFolder = null;
			}
		}
	}

	@Override
	public void onGpxFilesDeletionStarted() {
		showProgressBar();
	}

	@Override
	public void onGpxFilesDeleted(GPXInfo... values) {
		for (GPXInfo g : values) {
			allGpxAdapter.delete(g);
		}
		allGpxAdapter.notifyDataSetChanged();
	}

	@Override
	public void onGpxFilesDeletionFinished() {
		hideProgressBar();
		reloadTracks();
	}

	protected class GpxIndexesAdapter extends OsmandBaseExpandableListAdapter implements Filterable {

		private final Map<String, List<GPXInfo>> data = new LinkedHashMap<>();
		private final List<String> category = new ArrayList<>();
		private final List<GPXInfo> selected = new ArrayList<>();
		private GpxSearchFilter filter;

		private final GpxDataItemCallback updateGpxCallback = new GpxDataItemCallback() {

			private static final int UPDATE_GPX_ITEM_MSG_ID = OsmAndConstants.UI_HANDLER_LOCATION_SERVICE + 5;
			private static final long MIN_UPDATE_INTERVAL = 500;

			private long lastUpdateTime;

			private final Runnable updateItemsProc = () -> {
				if (updateEnable) {
					lastUpdateTime = System.currentTimeMillis();
					allGpxAdapter.notifyDataSetChanged();
				}
			};

			@Override
			public boolean isCancelled() {
				return !updateEnable;
			}

			@Override
			public void onGpxDataItemReady(@NonNull GpxDataItem item) {
				if (System.currentTimeMillis() - lastUpdateTime > MIN_UPDATE_INTERVAL) {
					updateItemsProc.run();
				}
				app.runMessageInUIThreadAndCancelPrevious(UPDATE_GPX_ITEM_MSG_ID, updateItemsProc, MIN_UPDATE_INTERVAL);
			}
		};

		public void refreshSelected() {
			selected.clear();
			selected.addAll(getSelectedGpx());
			TracksSortByMode sortByMode = settings.TRACKS_SORT_BY_MODE.get();
			Collator collator = OsmAndCollator.primaryCollator();
			Collections.sort(selected, (i1, i2) -> {
				if (sortByMode == TracksSortByMode.BY_NAME_ASCENDING) {
					return collator.compare(i1.getName(), i2.getName());
				} else if (sortByMode == TracksSortByMode.BY_NAME_DESCENDING) {
					return -collator.compare(i1.getName(), i2.getName());
				} else {
					if (i1.getFile() == null || i2.getFile() == null) {
						return collator.compare(i1.getName(), i2.getName());
					}
					long time1 = i1.getFile().lastModified();
					long time2 = i2.getFile().lastModified();
					if (time1 == time2) {
						return collator.compare(i1.getName(), i2.getName());
					}
					return -(Long.compare(time1, time2));
				}
			});
			notifyDataSetChanged();
		}

		public Set<GPXInfo> getSelectedGpx() {
			Set<GPXInfo> originalSelectedItems = new HashSet<>();
			SelectedGpxFile track = selectedGpxHelper.getSelectedCurrentRecordingTrack();
			if (track != null) {
				if (track.getGpxFile().showCurrentTrack) {
					originalSelectedItems.add(currentRecording);
				}
			}
			for (List<GPXInfo> gpxInfos : data.values()) {
				if (gpxInfos != null) {
					for (GPXInfo g : gpxInfos) {
						SelectedGpxFile sgpx = selectedGpxHelper.getSelectedFileByName(g.getFileName());
						if (sgpx != null) {
							g.setGpxFile(sgpx.getGpxFile());
							originalSelectedItems.add(g);
						}
					}
				}
			}
			return originalSelectedItems;
		}

		public void clear() {
			data.clear();
			category.clear();
			selected.clear();
			notifyDataSetChanged();
		}

		public void addLocalIndexInfo(@NonNull GPXInfo info) {
			LoadGpxInfosTask.addLocalIndexInfo(info, category, data);
		}

		@Override
		public GPXInfo getChild(int groupPosition, int childPosition) {
			if (isSelectedGroup(groupPosition)) {
				return selected.get(childPosition);
			}
			String cat = category.get(getGroupPosition(groupPosition));
			return data.get(cat).get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			// it would be unusable to have 10000 local indexes
			return groupPosition * 10000 + childPosition;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
		                         View convertView, ViewGroup parent) {
			View v = convertView;
			GPXInfo child = getChild(groupPosition, childPosition);
			if (v == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				v = inflater.inflate(R.layout.dash_gpx_track_item, parent, false);
			}
			GpxUiHelper.updateGpxInfoView(v, child, app, false, updateGpxCallback);

			ImageView icon = v.findViewById(R.id.icon);
			ImageButton options = v.findViewById(R.id.options);
			options.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_overflow_menu_white));
			options.setOnClickListener(view -> gpxActionsHelper.openPopUpMenu(view, child));

			CheckBox checkbox = v.findViewById(R.id.check_local_index);
			checkbox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
			if (selectionMode) {
				checkbox.setChecked(selectedItems.contains(child));
				checkbox.setOnClickListener(view -> {
					if (checkbox.isChecked()) {
						selectedItems.add(child);
					} else {
						selectedItems.remove(child);
					}
					updateSelectionMode(actionMode);
					// Issue 6187: Sync checkbox status between Visible group and rest of list
					allGpxAdapter.notifyDataSetInvalidated();
				});
				icon.setVisibility(View.GONE);
				//INVISIBLE instead of GONE avoids lines breaking differently in selection mode
				options.setVisibility(View.INVISIBLE);
			} else {
				icon.setVisibility(View.VISIBLE);
				options.setVisibility(View.VISIBLE);
			}

			CompoundButton checkItem = v.findViewById(R.id.toggle_item);
			if (isSelectedGroup(groupPosition)) {
				v.findViewById(R.id.check_item).setVisibility(selectionMode ? View.INVISIBLE : View.VISIBLE);
				v.findViewById(R.id.options).setVisibility(View.GONE);
			} else {
				v.findViewById(R.id.check_item).setVisibility(View.GONE);
			}


			boolean isChecked;
			if (child.isCurrentRecordingTrack()) {
				isChecked = selectedGpxHelper.getSelectedCurrentRecordingTrack() != null;
			} else {
				SelectedGpxFile selectedGpxFile = selectedGpxHelper.getSelectedFileByName(child.getFileName());
				isChecked = selectedGpxFile != null;
			}
			checkItem.setChecked(isChecked);
			checkItem.setOnClickListener(view -> {
				GpxSelectionParams params = GpxSelectionParams.newInstance().syncGroup().saveSelection();
				if (isChecked) {
					params.hideFromMap();
				} else {
					params.showOnMap().selectedByUser().addToMarkers().addToHistory();
				}
				selectedGpxHelper.selectGpxFile(child.getGpxFile(), params);
				notifyDataSetChanged();
			});

			v.setOnClickListener(view -> onChildClick(null, view, groupPosition, childPosition, 0));
			return v;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			View v = convertView;
			String group = getGroup(groupPosition);
			if (v == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				v = inflater.inflate(net.osmand.plus.R.layout.expandable_list_item_category, parent, false);
			}
			// Issue 6187: Always show visible group, also in selection mode
			//v.findViewById(R.id.group_divider).setVisibility(groupPosition == 0 ? View.GONE : View.VISIBLE);
			v.findViewById(R.id.group_divider).setVisibility(View.VISIBLE);

			if (selectionMode) {
				CheckBox ch = v.findViewById(R.id.toggle_item);
				// Issue 6187: No selection box for Visible group header
				//ch.setVisibility(View.VISIBLE);
				ch.setVisibility((selectionMode && !(groupPosition == 0 && isShowingSelection())) ? View.VISIBLE : View.GONE);
				ch.setChecked(selectedGroups.contains(groupPosition));

				ch.setOnClickListener(view -> {
					if (ch.isChecked()) {
						selectedItems.addAll(data.get(category.get(getGroupPosition(groupPosition))));
						selectedGroups.add(groupPosition);
					} else {
						selectedItems.removeAll(data.get(category.get(getGroupPosition(groupPosition))));
						selectedGroups.remove(groupPosition);
					}
					allGpxAdapter.notifyDataSetInvalidated();
					updateSelectionMode(actionMode);
				});
				v.findViewById(R.id.category_icon).setVisibility(View.GONE);
			} else {
				CheckBox ch = v.findViewById(R.id.toggle_item);
				ch.setVisibility(View.GONE);
				if (isSelectedGroup(groupPosition)) {
					setCategoryIcon(app.getUIUtilities().getIcon(R.drawable.ic_map, R.color.osmand_orange), v);
				} else {
					setCategoryIcon(app, 0, v, !nightMode);
				}
				v.findViewById(R.id.category_icon).setVisibility(View.VISIBLE);
			}

			adjustIndicator(app, groupPosition, isExpanded, v, !nightMode);

			String groupName = GpxUiHelper.getGpxDirTitle(group);
			TextView nameView = v.findViewById(R.id.category_name);
			nameView.setText(Algorithms.isEmpty(groupName) ? getString(R.string.shared_string_tracks) : groupName);

			return v;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			if (isSelectedGroup(groupPosition)) {
				return selected.size();
			}
			return data.get(category.get(getGroupPosition(groupPosition))).size();
		}

		private int getGroupPosition(int groupPosition) {
			return isShowingSelection() ? groupPosition - 1 : groupPosition;
		}

		private boolean isSelectedGroup(int groupPosition) {
			return isShowingSelection() && groupPosition == 0;
		}

		public boolean isShowingSelection() {
			// Issue 6187: Account for Visible group always being shown
			//return selected.size() > 0 && !selectionMode;
			return selected.size() > 0;
		}

		@Override
		public String getGroup(int groupPosition) {
			if (isSelectedGroup(groupPosition)) {
				return app.getString(R.string.shared_string_visible);
			}
			return category.get(getGroupPosition(groupPosition));
		}

		@Override
		public int getGroupCount() {
			if (isShowingSelection()) {
				return category.size() + 1;
			}
			return category.size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

		@Override
		public Filter getFilter() {
			if (filter == null) {
				filter = new GpxSearchFilter(getSearchFilterCallback());
			}
			filter.setGpxInfos(asyncLoader.getGpxInfos());
			return filter;
		}

		public void delete(GPXInfo g) {
			int found = -1;
			// search from end
			for (int i = category.size() - 1; i >= 0; i--) {
				String cat = category.get(i);
				if (objectEquals(g.subfolder, cat)) {
					found = i;
					break;
				}
			}
			if (found != -1) {
				data.get(category.get(found)).remove(g);
				selected.remove(g);
			}
		}
	}

	@NonNull
	private CallbackWithObject<Pair<CharSequence, List<GPXInfo>>> getSearchFilterCallback() {
		return result -> {
			updateFilteredList(result.first, result.second);
			return true;
		};
	}

	private void updateFilteredList(CharSequence constraint, List<GPXInfo> gpxInfos) {
		if (gpxInfos != null) {
			synchronized (allGpxAdapter) {
				allGpxAdapter.clear();
				for (GPXInfo i : gpxInfos) {
					allGpxAdapter.addLocalIndexInfo(i);
				}
				// disable sort
				// allGpxAdapter.sort();
				allGpxAdapter.refreshSelected();
			}
			allGpxAdapter.notifyDataSetChanged();
			if (constraint != null && constraint.length() > 3) {
				collapseTrees(10);
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (asyncLoader != null) {
			asyncLoader.cancel(true);
		}
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		GPXInfo gpxInfo = allGpxAdapter.getChild(groupPosition, childPosition);

		if (selectionMode) {
			if (selectedItems.contains(gpxInfo)) {
				selectedItems.remove(gpxInfo);
			} else {
				selectedItems.add(gpxInfo);
			}
			updateSelectionMode(actionMode);
		} else {
			gpxActionsHelper.openTrackOnMap(gpxInfo);
		}
		allGpxAdapter.notifyDataSetInvalidated();
		return true;
	}

	public interface SelectionModeListener {
		void onItemsSelected(List<GPXInfo> items);
	}
}
