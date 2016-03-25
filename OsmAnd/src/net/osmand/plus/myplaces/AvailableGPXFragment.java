package net.osmand.plus.myplaces;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.IndexConstants;
import net.osmand.access.AccessibleToast;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.activities.TrackActivity;
import net.osmand.plus.base.OsmandExpandableListFragment;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.download.ui.LocalIndexesFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.Collator;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AvailableGPXFragment extends OsmandExpandableListFragment {

	public static final int SEARCH_ID = -1;
	// public static final int ACTION_ID = 0;
	// protected static final int DELETE_ACTION_ID = 1;
	private boolean selectionMode = false;
	private List<GpxInfo> selectedItems = new ArrayList<>();
	private ActionMode actionMode;
	private LoadGpxTask asyncLoader;
	private GpxIndexesAdapter allGpxAdapter;
	private static MessageFormat formatMb = new MessageFormat("{0, number,##.#} MB", Locale.US);
	private ContextMenuAdapter optionsMenuAdapter;
	private AsyncTask<GpxInfo, ?, ?> operationTask;
	private GpxSelectionHelper selectedGpxHelper;
	private OsmandApplication app;
	private boolean updateEnable;
	private GpxInfo currentRecording;
	private boolean showOnMapMode;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.app = (OsmandApplication) getActivity().getApplication();
		final Collator collator = Collator.getInstance();
		collator.setStrength(Collator.SECONDARY);
		currentRecording = new GpxInfo(getMyApplication().getSavingTrackHelper().getCurrentGpx(), getString(R.string.shared_string_currently_recording_track));
		currentRecording.currentlyRecordingTrack = true;
		asyncLoader = new LoadGpxTask();
		selectedGpxHelper = ((OsmandApplication) activity.getApplication()).getSelectedGpxHelper();
		allGpxAdapter = new GpxIndexesAdapter(getActivity());
		setAdapter(allGpxAdapter);
	}

	private void startHandler() {
		Handler updateCurrentRecordingTrack = new Handler();
		updateCurrentRecordingTrack.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (getView() != null && updateEnable) {
					updateCurrentTrack(getView(), getActivity(), app);
					if (selectedGpxHelper.getSelectedCurrentRecordingTrack() != null) {
						allGpxAdapter.notifyDataSetChanged();
					}
					startHandler();
				}
			}
		}, 2000);
	}

	public List<GpxInfo> getSelectedItems() {
		return selectedItems;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (asyncLoader == null || asyncLoader.getResult() == null) {
			asyncLoader = new LoadGpxTask();
			asyncLoader.execute(getActivity());
		} else {
			allGpxAdapter.refreshSelected();
			allGpxAdapter.notifyDataSetChanged();
		}
		updateCurrentTrack();

		updateEnable = true;
		startHandler();
	}

	@Override
	public void onPause() {
		super.onPause();
		updateEnable = false;
		if (operationTask != null) {
			operationTask.cancel(true);
		}
	}

	public void updateCurrentTrack() {
		if (OsmandPlugin.getEnabledPlugin(OsmandMonitoringPlugin.class) == null) {
			return;
		}
		updateCurrentTrack(getView(), getActivity(), app);
		@SuppressWarnings("ConstantConditions")
		final CheckBox checkbox = (CheckBox) getView().findViewById(R.id.check_local_index);
		checkbox.setVisibility(selectionMode && showOnMapMode ? View.VISIBLE : View.GONE);
		if (selectionMode && showOnMapMode) {
			checkbox.setChecked(selectedItems.contains(currentRecording));
			checkbox.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					if (checkbox.isChecked()) {
						selectedItems.add(currentRecording);
					} else {
						selectedItems.remove(currentRecording);
					}
					updateSelectionMode(actionMode);
				}
			});
		}

	}

	public static void updateCurrentTrack(View v, final Activity ctx, final OsmandApplication app) {
		final OsmandMonitoringPlugin plugin = OsmandPlugin.getEnabledPlugin(OsmandMonitoringPlugin.class);
		if (v == null || ctx == null || app == null || plugin == null) {
			return;
		}
		final boolean isRecording = app.getSettings().SAVE_GLOBAL_TRACK_TO_GPX.get();
		ImageButton stop = ((ImageButton) v.findViewById(R.id.stop));
		if (isRecording) {
			stop.setImageDrawable(app.getIconsCache().getContentIcon(R.drawable.ic_action_rec_stop));
		} else {
			stop.setImageDrawable(app.getIconsCache().getContentIcon(R.drawable.ic_action_rec_start));
		}
		stop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isRecording) {
					plugin.stopRecording();
				} else if(plugin != null){
					if (app.getLocationProvider().checkGPSEnabled(ctx)) {
						plugin.startGPXMonitoring(ctx);
					}
				}
			}
		});
		SavingTrackHelper sth = app.getSavingTrackHelper();
		ImageButton save = ((ImageButton) v.findViewById(R.id.show_on_map));
		save.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final OsmandMonitoringPlugin plugin = OsmandPlugin
						.getEnabledPlugin(OsmandMonitoringPlugin.class);
				plugin.saveCurrentTrack();
			}
		});
		if (sth.getPoints() > 0 || sth.getDistance() > 0) {
			save.setVisibility(View.VISIBLE);
		} else {
			save.setVisibility(View.GONE);
		}
		save.setImageDrawable(app.getIconsCache().getContentIcon(R.drawable.ic_action_gsave_dark));

		((TextView) v.findViewById(R.id.points_count)).setText(sth.getPoints() + "");
		((TextView) v.findViewById(R.id.distance))
				.setText(OsmAndFormatter.getFormattedDistance(sth.getDistance(), app));
		v.findViewById(R.id.points_icon).setVisibility(View.VISIBLE);
		ImageView distance = (ImageView) v.findViewById(R.id.distance_icon);
		distance.setVisibility(View.VISIBLE);
		distance.setImageDrawable(app.getIconsCache().getContentIcon(R.drawable.ic_small_distance));
		ImageView pointsCount = (ImageView) v.findViewById(R.id.points_icon);
		pointsCount.setVisibility(View.VISIBLE);
		pointsCount.setImageDrawable(app.getIconsCache().getContentIcon(R.drawable.ic_small_point));

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.available_gpx, container, false);
		listView = (ExpandableListView) v.findViewById(android.R.id.list);
		if (this.adapter != null) {
			listView.setAdapter(this.adapter);
		}
		setHasOptionsMenu(true);
		View currentTrackView = v.findViewById(R.id.current_track);
		createCurrentTrackView(v, getMyApplication());
		if (OsmandPlugin.getEnabledPlugin(OsmandMonitoringPlugin.class) == null) {
			currentTrackView.setVisibility(View.GONE);
		} else {
			currentTrackView.setVisibility(View.VISIBLE);
			currentTrackView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openTrack(getActivity(), null);
				}
			});
		}

		return v;
	}

	public static void openTrack(Activity a, final File f) {
		Intent newIntent = new Intent(a, ((OsmandApplication) a.getApplication()).getAppCustomization().getTrackActivity());
		// causes wrong position caching: newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		if (f == null) {
			newIntent.putExtra(TrackActivity.CURRENT_RECORDING, true);
		} else {
			newIntent.putExtra(TrackActivity.TRACK_FILE_NAME, f.getAbsolutePath());
		}
		newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		a.startActivity(newIntent);
	}

	public static void createCurrentTrackView(View v, final OsmandApplication app) {
		((TextView) v.findViewById(R.id.name)).setText(R.string.shared_string_currently_recording_track);
		v.findViewById(R.id.time_icon).setVisibility(View.GONE);
		v.findViewById(R.id.divider).setVisibility(View.GONE);
		v.findViewById(R.id.options).setVisibility(View.GONE);
		v.findViewById(R.id.stop).setVisibility(View.VISIBLE);
		v.findViewById(R.id.toggle_item).setVisibility(View.GONE);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		MenuItem mi = createMenuItem(menu, SEARCH_ID, R.string.search_poi_filter, R.drawable.ic_action_search_dark,
				R.drawable.ic_action_search_dark, MenuItemCompat.SHOW_AS_ACTION_ALWAYS
						| MenuItemCompat.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		SearchView searchView = new SearchView(getActivity());
		FavoritesActivity.updateSearchView(getActivity(), searchView);
		MenuItemCompat.setActionView(mi, searchView);
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
		MenuItemCompat.setOnActionExpandListener(mi, new MenuItemCompat.OnActionExpandListener() {
			@Override
			public boolean onMenuItemActionExpand(MenuItem item) {
				return true;
			}

			@Override
			public boolean onMenuItemActionCollapse(MenuItem item) {
				// Needed to hide intermediate progress bar after closing action mode
				new Handler().postDelayed(new Runnable() {
					public void run() {
						hideProgressBar();
					}
				}, 100);
				return true;
			}
		});

		if (AndroidUiHelper.isOrientationPortrait(getActivity())) {
			menu = ((FavoritesActivity) getActivity()).getClearToolbar(true).getMenu();
		} else {
			((FavoritesActivity) getActivity()).getClearToolbar(false);
		}

		optionsMenuAdapter = new ContextMenuAdapter(getActivity());
		OnContextMenuClick listener = new OnContextMenuClick() {
			@Override
			public boolean onContextMenuClick(ArrayAdapter<?> adapter, final int itemId, int pos, boolean isChecked) {
				if (itemId == R.string.local_index_mi_reload) {
					asyncLoader = new LoadGpxTask();
					asyncLoader.execute(getActivity());
				} else if (itemId == R.string.shared_string_show_on_map) {
					openShowOnMapMode();
				} else if (itemId == R.string.shared_string_delete) {
					openSelectionMode(itemId, R.drawable.ic_action_delete_dark, R.drawable.ic_action_delete_dark,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									doAction(itemId);
								}
							});
				}
				return true;
			}
		};
		optionsMenuAdapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.shared_string_show_on_map, getActivity())
				.setIcon(R.drawable.ic_show_on_map)
				.setListener(listener).createItem());
		optionsMenuAdapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.shared_string_delete, getActivity())
				.setIcon(R.drawable.ic_action_delete_dark).setListener(listener).createItem());
		optionsMenuAdapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.local_index_mi_reload, getActivity())
				.setIcon(R.drawable.ic_action_refresh_dark).setListener(listener).createItem());
		OsmandPlugin.onOptionsMenuActivity(getActivity(), this, optionsMenuAdapter);
		for (int j = 0; j < optionsMenuAdapter.length(); j++) {
			final MenuItem item;
			item = menu.add(0, optionsMenuAdapter.getElementId(j), j + 1, optionsMenuAdapter.getItemName(j));
			MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
			if (AndroidUiHelper.isOrientationPortrait(getActivity())) {
				item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem menuItem) {
						onOptionsItemSelected(item);
						return true;
					}
				});
			}
			OsmandApplication app = getMyApplication();
			if (optionsMenuAdapter.getImage(app, j, isLightActionBar()) != null) {
				item.setIcon(optionsMenuAdapter.getImage(app, j, isLightActionBar()));
			}

		}
	}

	public void doAction(int actionResId) {
		if (actionResId == R.string.shared_string_delete) {
			operationTask = new DeleteGpxTask();
			operationTask.execute(selectedItems.toArray(new GpxInfo[selectedItems.size()]));
		} else {
			operationTask = null;
		}
		if (actionMode != null) {
			actionMode.finish();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		for (int i = 0; i < optionsMenuAdapter.length(); i++) {
			if (itemId == optionsMenuAdapter.getElementId(i)) {
				optionsMenuAdapter.getClickAdapter(i).onContextMenuClick(null, itemId, i, false);
				return true;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	public void showProgressBar() {
		((FavoritesActivity) getActivity()).setSupportProgressBarIndeterminateVisibility(true);
	}

	public void hideProgressBar() {
		if (getActivity() != null) {
			((FavoritesActivity) getActivity()).setSupportProgressBarIndeterminateVisibility(false);
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
			((FavoritesActivity) getActivity()).setToolbarVisibility(!selectionMode &&
					AndroidUiHelper.isOrientationPortrait(getActivity()));
		}
	}

	private void openShowOnMapMode() {
		enableSelectionMode(true);
		showOnMapMode = true;
		selectedItems.clear();
		final Set<GpxInfo> originalSelectedItems = allGpxAdapter.getSelectedGpx();
		selectedItems.addAll(originalSelectedItems);
		actionMode = getActionBarActivity().startSupportActionMode(new ActionMode.Callback() {


			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				enableSelectionMode(true);
				updateSelectionMode(mode);
				MenuItem it = menu.add(R.string.shared_string_show_on_map);
				it.setIcon(R.drawable.ic_action_done);
				MenuItemCompat.setShowAsAction(it, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM
						| MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);
				updateCurrentTrack();
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				runSelection(false);
				actionMode.finish();
				allGpxAdapter.refreshSelected();
				allGpxAdapter.notifyDataSetChanged();
				return true;
			}

			private void runSelection(boolean showOnMap) {
				operationTask = new SelectGpxTask(showOnMap);
				originalSelectedItems.addAll(selectedItems);
				operationTask.execute(originalSelectedItems.toArray(new GpxInfo[originalSelectedItems.size()]));
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

	public void openSelectionMode(final int actionResId, int darkIcon, int lightIcon,
								  final DialogInterface.OnClickListener listener) {
		final int actionIconId = !isLightActionBar() ? darkIcon : lightIcon;
		String value = app.getString(actionResId);
		if (value.endsWith("...")) {
			value = value.substring(0, value.length() - 3);
		}
		final String actionButton = value;
		if (allGpxAdapter.getGroupCount() == 0) {
			AccessibleToast.makeText(getActivity(),
					app.getString(R.string.local_index_no_items_to_do, actionButton.toLowerCase()), Toast.LENGTH_SHORT)
					.show();
			return;
		}

		enableSelectionMode(true);
		selectedItems.clear();
		actionMode = getActionBarActivity().startSupportActionMode(new ActionMode.Callback() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				enableSelectionMode(true);
				MenuItem it = menu.add(actionResId);
				if (actionIconId != 0) {
					it.setIcon(actionIconId);
				}
				MenuItemCompat.setShowAsAction(it, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM
						| MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				if (selectedItems.isEmpty()) {
					AccessibleToast.makeText(getActivity(),
							app.getString(R.string.local_index_no_items_to_do, actionButton.toLowerCase()),
							Toast.LENGTH_SHORT).show();
					return true;
				}

				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setMessage(getString(R.string.local_index_action_do, actionButton.toLowerCase(),
						selectedItems.size()));
				builder.setPositiveButton(actionButton, listener);
				builder.setNegativeButton(R.string.shared_string_cancel, null);
				builder.show();
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

	private void showGpxOnMap(GpxInfo info) {
		info.setGpx(GPXUtilities.loadGPXFile(app, info.file));
		boolean e = true;
		if (info != null && info.gpx != null) {
			WptPt loc = info.gpx.findPointToShow();
			OsmandSettings settings = getMyApplication().getSettings();
			if (loc != null) {
				settings.setMapLocationToShow(loc.lat, loc.lon, settings.getLastKnownMapZoom());
				e = false;
				getMyApplication().getSelectedGpxHelper().setGpxFileToDisplay(info.gpx);
				MapActivity.launchMapActivityMoveToTop(getActivity());
			}
		}
		if (e) {
			AccessibleToast.makeText(getActivity(), R.string.gpx_file_is_empty, Toast.LENGTH_LONG).show();
		}
	}

	public class LoadGpxTask extends AsyncTask<Activity, GpxInfo, List<GpxInfo>> {

		private List<GpxInfo> result;

		@Override
		protected List<GpxInfo> doInBackground(Activity... params) {
			List<GpxInfo> result = new ArrayList<>();
			loadGPXData(app.getAppPath(IndexConstants.GPX_INDEX_DIR), result, this);
			return result;
		}

		public void loadFile(GpxInfo... loaded) {
			publishProgress(loaded);
		}

		@Override
		protected void onPreExecute() {
			((OsmandActionBarActivity) getActivity()).setSupportProgressBarIndeterminateVisibility(true);
			allGpxAdapter.clear();
		}

		@Override
		protected void onProgressUpdate(GpxInfo... values) {
			for (GpxInfo v : values) {
				allGpxAdapter.addLocalIndexInfo(v);
			}
			allGpxAdapter.notifyDataSetChanged();
		}

		public void setResult(List<GpxInfo> result) {
			this.result = result;
			allGpxAdapter.clear();
			if (result != null) {
				for (GpxInfo v : result) {
					allGpxAdapter.addLocalIndexInfo(v);
				}
				allGpxAdapter.refreshSelected();
				allGpxAdapter.notifyDataSetChanged();
				onPostExecute(result);
			}
		}

		@Override
		protected void onPostExecute(List<GpxInfo> result) {
			this.result = result;
			allGpxAdapter.refreshSelected();
			if (getActivity() != null) {
				((OsmandActionBarActivity) getActivity()).setSupportProgressBarIndeterminateVisibility(false);
			}
			if (allGpxAdapter.getGroupCount() > 0 &&
					allGpxAdapter.isShowingSelection()) {
				getExpandableListView().expandGroup(0);
			}
		}

		private File[] listFilesSorted(File dir) {
			File[] listFiles = dir.listFiles();
			if (listFiles == null) {
				return new File[0];
			}
			Arrays.sort(listFiles);
			return listFiles;
		}

		private void loadGPXData(File mapPath, List<GpxInfo> result, LoadGpxTask loadTask) {
			if (mapPath.canRead()) {
				List<GpxInfo> progress = new ArrayList<>();
				loadGPXFolder(mapPath, result, loadTask, progress, "");
				if (!progress.isEmpty()) {
					loadTask.loadFile(progress.toArray(new GpxInfo[progress.size()]));
				}
			}
		}

		private void loadGPXFolder(File mapPath, List<GpxInfo> result, LoadGpxTask loadTask, List<GpxInfo> progress,
								   String gpxSubfolder) {
			for (File gpxFile : listFilesSorted(mapPath)) {
				if (gpxFile.isDirectory()) {
					String sub = gpxSubfolder.length() == 0 ? gpxFile.getName() : gpxSubfolder + "/"
							+ gpxFile.getName();
					loadGPXFolder(gpxFile, result, loadTask, progress, sub);
				} else if (gpxFile.isFile() && gpxFile.getName().endsWith(".gpx")) {
					GpxInfo info = new GpxInfo();
					info.subfolder = gpxSubfolder;
					info.file = gpxFile;
					result.add(info);
					progress.add(info);
					if (progress.size() > 7) {
						loadTask.loadFile(progress.toArray(new GpxInfo[progress.size()]));
						progress.clear();
					}

				}
			}
		}

		public List<GpxInfo> getResult() {
			return result;
		}

	}

	protected class GpxIndexesAdapter extends OsmandBaseExpandableListAdapter implements Filterable {

		Map<String, List<GpxInfo>> data = new LinkedHashMap<>();
		List<String> category = new ArrayList<>();
		List<GpxInfo> selected = new ArrayList<>();
		int warningColor;
		int defaultColor;
		int corruptedColor;
		private SearchFilter filter;

		public GpxIndexesAdapter(Context ctx) {
			warningColor = ctx.getResources().getColor(R.color.color_warning);
			TypedArray ta = ctx.getTheme().obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary});
			defaultColor = ta.getColor(0, ctx.getResources().getColor(R.color.color_unknown));
			ta.recycle();
		}

		public void refreshSelected() {
			selected.clear();
			selected.addAll(getSelectedGpx());
		}

		public Set<GpxInfo> getSelectedGpx() {
			Set<GpxInfo> originalSelectedItems = new HashSet<>();
			SelectedGpxFile track = selectedGpxHelper.getSelectedCurrentRecordingTrack();
			if (track != null && track.getGpxFile() != null) {
				if (track.getGpxFile().showCurrentTrack) {
					originalSelectedItems.add(currentRecording);
				}
			}
			for (List<GpxInfo> l : data.values()) {
				if (l != null) {
					for (GpxInfo g : l) {
						SelectedGpxFile sgpx = selectedGpxHelper.getSelectedFileByName(g.getFileName());
						if (sgpx != null) {
							g.gpx = sgpx.getGpxFile();
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

		public void addLocalIndexInfo(GpxInfo info) {
			String catName;
			if (info.gpx != null && info.gpx.showCurrentTrack) {
				catName = info.name;
			} else {
				// local_indexes_cat_gpx now obsolete in new UI screen which shows only GPX data
				// catName = app.getString(R.string.local_indexes_cat_gpx) + " " + info.subfolder;
				catName = "" + info.subfolder;
			}
			int found = -1;
			// search from end
			for (int i = category.size() - 1; i >= 0; i--) {
				String cat = category.get(i);
				if (Algorithms.objectEquals(catName, cat)) {
					found = i;
					break;
				}
			}
			if (found == -1) {
				found = category.size();
				category.add(catName);
			}
			if (!data.containsKey(category.get(found))) {
				data.put(category.get(found), new ArrayList<GpxInfo>());
			}
			data.get(category.get(found)).add(info);
		}

		@Override
		public GpxInfo getChild(int groupPosition, int childPosition) {
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
		public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild,
								 View convertView, ViewGroup parent) {
			View v = convertView;
			final GpxInfo child = getChild(groupPosition, childPosition);
			if (v == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				v = inflater.inflate(R.layout.dash_gpx_track_item, parent, false);
			}
			udpateGpxInfoView(v, child, app, false);

			ImageView icon = (ImageView) v.findViewById(R.id.icon);
			ImageButton options = (ImageButton) v.findViewById(R.id.options);
			options.setImageDrawable(getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_overflow_menu_white));
			options.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openPopUpMenu(v, child);
				}
			});

			final CheckBox checkbox = (CheckBox) v.findViewById(R.id.check_local_index);
			checkbox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
			if (selectionMode) {
				checkbox.setChecked(selectedItems.contains(child));
				checkbox.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						if (checkbox.isChecked()) {
							selectedItems.add(child);
						} else {
							selectedItems.remove(child);
						}
						updateSelectionMode(actionMode);
					}
				});
				icon.setVisibility(View.GONE);
				options.setVisibility(View.GONE);
			} else {
				icon.setVisibility(View.VISIBLE);
				options.setVisibility(View.VISIBLE);
			}

			final CompoundButton checkItem = (CompoundButton) v.findViewById(R.id.toggle_item);
			if (isSelectedGroup(groupPosition)) {
				checkItem.setVisibility(View.VISIBLE);
				v.findViewById(R.id.options).setVisibility(View.GONE);
			} else {
				checkItem.setVisibility(View.GONE);
			}


			final boolean isChecked;
			if (child.currentlyRecordingTrack) {
				isChecked = selectedGpxHelper.getSelectedCurrentRecordingTrack() != null;
			} else {
				final SelectedGpxFile selectedGpxFile = selectedGpxHelper.getSelectedFileByName(child.getFileName());
				isChecked = selectedGpxFile != null;
			}
			checkItem.setChecked(isChecked);
			checkItem.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					selectedGpxHelper.selectGpxFile(child.gpx, !isChecked, false);
					notifyDataSetChanged();
				}
			});

			v.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onChildClick(null, v, groupPosition, childPosition, 0);
				}
			});
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
			StringBuilder t = new StringBuilder(group);
			adjustIndicator(groupPosition, isExpanded, v, getMyApplication().getSettings().isLightContent());
			TextView nameView = ((TextView) v.findViewById(R.id.category_name));
			List<GpxInfo> list = isSelectedGroup(groupPosition) ? selected : data.get(group);
			int size = 0;
			for (int i = 0; i < list.size(); i++) {
				int sz = list.get(i).getSize();
				if (sz < 0) {
					size = 0;
					break;
				} else {
					size += sz;
				}
			}
			size = size / (1 << 10);
			if (size > 0) {
				t.append(" [").append(size).append(" MB]");
			}
			nameView.setText(t.toString());
			nameView.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);

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
			return selected.size() > 0 && !selectionMode;
		}

		@Override
		public String getGroup(int groupPosition) {
			if (isSelectedGroup(groupPosition)) {
				return app.getString(R.string.shared_string_selected);
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
				filter = new SearchFilter();
			}
			return filter;
		}

		public void delete(GpxInfo g) {
			int found = -1;
			// search from end
			for (int i = category.size() - 1; i >= 0; i--) {
				String cat = category.get(i);
				// local_indexes_cat_gpx now obsolete in new UI screen which shows only GPX data
				// if (Algorithms.objectEquals(getActivity().getString(R.string.local_indexes_cat_gpx) + " " +
				// g.subfolder, cat)) {
				if (Algorithms.objectEquals("" + g.subfolder, cat)) {
					found = i;
					break;
				}
			}
			if (found != -1) {
				data.get(category.get(found)).remove(g);
			}
		}
	}

	private void openPopUpMenu(View v, final GpxInfo gpxInfo) {
		IconsCache iconsCache = getMyApplication().getIconsCache();
		final PopupMenu optionsMenu = new PopupMenu(getActivity(), v);
		DirectionsDialogs.setupPopUpMenuIcon(optionsMenu);

		MenuItem item = optionsMenu.getMenu().add(R.string.shared_string_show_on_map).setIcon(iconsCache.getContentIcon(R.drawable.ic_show_on_map));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				showGpxOnMap(gpxInfo);
				return true;
			}
		});

		item = optionsMenu.getMenu().add(R.string.shared_string_rename)
				.setIcon(iconsCache.getContentIcon(R.drawable.ic_action_edit_dark));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				LocalIndexesFragment.renameFile(getActivity(), gpxInfo.file, new Runnable() {

					@Override
					public void run() {
						asyncLoader = new LoadGpxTask();
						asyncLoader.execute(getActivity());
					}
				});
				return true;
			}
		});
		item = optionsMenu.getMenu().add(R.string.shared_string_share)
				.setIcon(iconsCache.getContentIcon(R.drawable.ic_action_gshare_dark));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				final Uri fileUri = Uri.fromFile(gpxInfo.file);
				final Intent sendIntent = new Intent(Intent.ACTION_SEND);
				sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
				sendIntent.setType("application/gpx+xml");
				startActivity(sendIntent);
				return true;
			}
		});

		final OsmEditingPlugin osmEditingPlugin = OsmandPlugin.getEnabledPlugin(OsmEditingPlugin.class);
		if (osmEditingPlugin != null && osmEditingPlugin.isActive()) {
			item = optionsMenu.getMenu().add(R.string.shared_string_export).setIcon(iconsCache.getContentIcon(R.drawable.ic_action_export));
			item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					osmEditingPlugin.sendGPXFiles(getActivity(), AvailableGPXFragment.this, gpxInfo);
					return true;
				}
			});

		}

		item = optionsMenu.getMenu().add(R.string.shared_string_delete)
				.setIcon(iconsCache.getContentIcon(R.drawable.ic_action_delete_dark));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setMessage(R.string.recording_delete_confirm);
				builder.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						operationTask = new DeleteGpxTask();
						operationTask.execute(gpxInfo);
					}
				});
				builder.setNegativeButton(R.string.shared_string_cancel, null);
				builder.show();
				return true;
			}
		});
		optionsMenu.show();

	}

	public class DeleteGpxTask extends AsyncTask<GpxInfo, GpxInfo, String> {

		@Override
		protected String doInBackground(GpxInfo... params) {
			int count = 0;
			int total = 0;
			for (GpxInfo info : params) {
				if (!isCancelled() && (info.gpx == null || !info.gpx.showCurrentTrack)) {
					boolean successfull;
					successfull = Algorithms.removeAllFiles(info.file);
					total++;
					if (successfull) {
						count++;
						publishProgress(info);
					}
				}
			}
			return app.getString(R.string.local_index_items_deleted, count, total);
		}

		@Override
		protected void onProgressUpdate(GpxInfo... values) {
			for (GpxInfo g : values) {
				allGpxAdapter.delete(g);
			}
			allGpxAdapter.notifyDataSetChanged();
		}

		@Override
		protected void onPreExecute() {
			getActivity().setProgressBarIndeterminateVisibility(true);
		}

		@Override
		protected void onPostExecute(String result) {
			getActivity().setProgressBarIndeterminateVisibility(false);
			AccessibleToast.makeText(getActivity(), result, Toast.LENGTH_LONG).show();
		}
	}

	public class SelectGpxTask extends AsyncTask<GpxInfo, GpxInfo, String> {

		private boolean showOnMap;
		private WptPt toShow;

		public SelectGpxTask(boolean showOnMap) {
			this.showOnMap = showOnMap;
		}

		@Override
		protected String doInBackground(GpxInfo... params) {
			for (GpxInfo info : params) {
				if (!isCancelled()) {
					if (!info.currentlyRecordingTrack) {
						info.setGpx(GPXUtilities.loadGPXFile(app, info.file));
					}
					publishProgress(info);
				}
			}
			return "";
		}

		@Override
		protected void onProgressUpdate(GpxInfo... values) {
			for (GpxInfo g : values) {
				final boolean visible = selectedItems.contains(g);
				selectedGpxHelper.selectGpxFile(g.gpx, visible, false);
				if (visible && toShow == null) {
					toShow = g.gpx.findPointToShow();
				}
			}
			allGpxAdapter.notifyDataSetInvalidated();
		}

		@Override
		protected void onPreExecute() {
			getActivity().setProgressBarIndeterminateVisibility(true);
		}

		@Override
		protected void onPostExecute(String result) {
			getActivity().setProgressBarIndeterminateVisibility(false);
			allGpxAdapter.refreshSelected();
			allGpxAdapter.notifyDataSetChanged();
			if (showOnMap && toShow != null) {
				getMyApplication().getSettings().setMapLocationToShow(toShow.lat, toShow.lon,
						getMyApplication().getSettings().getLastKnownMapZoom());
				MapActivity.launchMapActivityMoveToTop(getActivity());
			}
		}
	}


	private class SearchFilter extends Filter {

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults results = new FilterResults();
			final List<GpxInfo> raw = asyncLoader.getResult();
			if (constraint == null || constraint.length() == 0 || raw == null) {
				results.values = raw;
				results.count = 1;
			} else {
				String cs = constraint.toString();
				List<GpxInfo> res = new ArrayList<>();
				for (GpxInfo r : raw) {
					if (r.getName().toLowerCase().indexOf(cs) != -1) {
						res.add(r);
					}
				}
				results.values = res;
				results.count = res.size();
			}
			return results;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			if (results.values != null) {
				synchronized (allGpxAdapter) {
					allGpxAdapter.clear();
					for (GpxInfo i : ((List<GpxInfo>) results.values)) {
						allGpxAdapter.addLocalIndexInfo(i);
					}
					allGpxAdapter.refreshSelected();
				}
				allGpxAdapter.notifyDataSetChanged();
				if (constraint != null && constraint.length() > 3) {
					collapseTrees(10);
				}
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
		GpxInfo item = allGpxAdapter.getChild(groupPosition, childPosition);

		if (!selectionMode) {
			Intent newIntent = new Intent(getActivity(), getMyApplication().getAppCustomization().getTrackActivity());
			// causes wrong position caching: newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			if (item.currentlyRecordingTrack) {
				newIntent.putExtra(TrackActivity.CURRENT_RECORDING, true);
			} else {
				newIntent.putExtra(TrackActivity.TRACK_FILE_NAME, item.file.getAbsolutePath());
			}
			newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(newIntent);
			// item.setExpanded(!item.isExpanded());
			// if (item.isExpanded()) {
			// descriptionLoader = new LoadLocalIndexDescriptionTask();
			// descriptionLoader.execute(item);
			// }
		} else {
			if (!selectedItems.contains(item)) {
				selectedItems.add(item);
			} else {
				selectedItems.remove(item);
			}
			updateSelectionMode(actionMode);
		}
		allGpxAdapter.notifyDataSetInvalidated();
		return true;
	}

	public static class GpxInfo {
		public boolean currentlyRecordingTrack;
		public GPXFile gpx;
		public File file;
		public String subfolder;

		private String name = null;
		private int sz = -1;
		private String fileName = null;
		private boolean corrupted;

		public GpxInfo() {
		}

		public GpxInfo(GPXFile file, String name) {
			this.gpx = file;
			this.name = name;
		}

		public String getName() {
			if (name == null) {
				name = formatName(file.getName());
			}
			return name;
		}

		private String formatName(String name) {
			int ext = name.lastIndexOf('.');
			if (ext != -1) {
				name = name.substring(0, ext);
			}
			return name.replace('_', ' ');
		}

		public boolean isCorrupted() {
			return corrupted;
		}

		public int getSize() {
			if (sz == -1) {
				if (file == null) {
					return -1;
				}
				sz = (int) ((file.length() + 512) >> 10);
			}
			return sz;
		}

		public long getFileDate() {
			if (file == null) {
				return 0;
			}
			return file.lastModified();
		}

		public void setGpx(GPXFile gpx) {
			this.gpx = gpx;
		}


		public String getFileName() {
			if (fileName != null) {
				return fileName;
			}
			if (file == null) {
				return "";
			}
			return fileName = file.getName();
		}
	}

	public static void udpateGpxInfoView(View v, GpxInfo child, OsmandApplication app, boolean isDashItem) {
		TextView viewName = ((TextView) v.findViewById(R.id.name));
		if (!isDashItem) {
			v.findViewById(R.id.divider).setVisibility(View.GONE);
		} else {
			v.findViewById(R.id.divider).setVisibility(View.VISIBLE);
		}

		viewName.setText(child.getName());
		GpxSelectionHelper selectedGpxHelper = app.getSelectedGpxHelper();

		// ImageView icon = (ImageView) v.findViewById(!isDashItem? R.id.icon : R.id.show_on_map);
		ImageView icon = (ImageView) v.findViewById(R.id.icon);
		icon.setVisibility(View.VISIBLE);
		icon.setImageDrawable(app.getIconsCache().getContentIcon(R.drawable.ic_gpx_track));
		if (child.isCorrupted()) {
			viewName.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
		} else {
			viewName.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
		}
		SelectedGpxFile sgpx = child.currentlyRecordingTrack ? selectedGpxHelper.getSelectedCurrentRecordingTrack() :
				selectedGpxHelper.getSelectedFileByName(child.getFileName());
		GPXTrackAnalysis analysis = null;
		if (sgpx != null) {
			icon.setImageDrawable(app.getIconsCache().getIcon(R.drawable.ic_gpx_track, R.color.color_distance));
			analysis = sgpx.getTrackAnalysis();

		}
		boolean sectionRead = analysis == null;
		if (sectionRead) {
			v.findViewById(R.id.read_section).setVisibility(View.GONE);
			v.findViewById(R.id.unknown_section).setVisibility(View.VISIBLE);
			String date = "";
			String size = "";
			if (child.getSize() >= 0) {
				if (child.getSize() > 100) {
					size = formatMb.format(new Object[]{(float) child.getSize() / (1 << 10)});
				} else {
					size = child.getSize() + " kB";
				}
			}
			DateFormat df = app.getResourceManager().getDateFormat();
			long fd = child.getFileDate();
			if (fd > 0) {
				date = (df.format(new Date(fd)));
			}
			TextView sizeText = (TextView) v.findViewById(R.id.date_and_size_details);
			sizeText.setText(date + " \u2022 " + size);

		} else {
			v.findViewById(R.id.read_section).setVisibility(View.VISIBLE);
			v.findViewById(R.id.unknown_section).setVisibility(View.GONE);
			ImageView distanceI = (ImageView) v.findViewById(R.id.distance_icon);
			distanceI.setVisibility(View.VISIBLE);
			distanceI.setImageDrawable(app.getIconsCache().getContentIcon(R.drawable.ic_small_distance));
			ImageView pointsI = (ImageView) v.findViewById(R.id.points_icon);
			pointsI.setVisibility(View.VISIBLE);
			pointsI.setImageDrawable(app.getIconsCache().getContentIcon(R.drawable.ic_small_point));
			ImageView timeI = (ImageView) v.findViewById(R.id.time_icon);
			timeI.setVisibility(View.VISIBLE);
			timeI.setImageDrawable(app.getIconsCache().getContentIcon(R.drawable.ic_small_time));
			TextView time = (TextView) v.findViewById(R.id.time);
			TextView distance = (TextView) v.findViewById(R.id.distance);
			TextView pointsCount = (TextView) v.findViewById(R.id.points_count);
			pointsCount.setText(analysis.wptPoints + "");
//			if (analysis.totalDistanceMoving != 0) {
//				distance.setText(OsmAndFormatter.getFormattedDistance(analysis.totalDistanceMoving, app));
//			} else {
			distance.setText(OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));
//			}

			if (analysis.isTimeSpecified()) {
//				if (analysis.isTimeMoving()) {
//					time.setText(Algorithms.formatDuration((int) (analysis.timeMoving / 1000)) + "");
//				} else {
				time.setText(Algorithms.formatDuration((int) (analysis.timeSpan / 1000)) + "");
//				}
			} else {
				time.setText("");
			}
		}

		TextView descr = ((TextView) v.findViewById(R.id.description));
		descr.setVisibility(View.GONE);

		v.findViewById(R.id.toggle_item).setVisibility(View.GONE);
	}
}