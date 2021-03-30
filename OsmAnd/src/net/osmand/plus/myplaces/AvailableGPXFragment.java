package net.osmand.plus.myplaces;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import net.osmand.AndroidUtils;
import net.osmand.Collator;
import net.osmand.FileUtils;
import net.osmand.FileUtils.RenameCallback;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.GPXUtilities.Track;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.IndexConstants;
import net.osmand.OsmAndCollator;
import net.osmand.data.PointDescription;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.ItemClickListener;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GpxDbHelper.GpxDataItemCallback;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.SelectGpxTaskListener;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.base.OsmandExpandableListFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetType;
import net.osmand.plus.helpers.enums.TracksSortByMode;
import net.osmand.plus.mapmarkers.CoordinateInputDialogFragment;
import net.osmand.plus.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.myplaces.MoveGpxFileBottomSheet.OnTrackFileMoveListener;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.osmedit.oauth.OsmOAuthHelper.OsmAuthorizationListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.widgets.popup.PopUpMenuHelper;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.osmand.plus.GpxSelectionHelper.CURRENT_TRACK;
import static net.osmand.plus.myplaces.FavoritesActivity.GPX_TAB;
import static net.osmand.plus.myplaces.FavoritesActivity.TAB_ID;
import static net.osmand.plus.track.TrackMenuFragment.openTrack;
import static net.osmand.util.Algorithms.capitalizeFirstLetter;
import static net.osmand.util.Algorithms.formatDuration;
import static net.osmand.util.Algorithms.objectEquals;
import static net.osmand.util.Algorithms.removeAllFiles;

public class AvailableGPXFragment extends OsmandExpandableListFragment implements
		FavoritesFragmentStateHolder, OsmAuthorizationListener, OnTrackFileMoveListener, RenameCallback {

	public static final int SEARCH_ID = -1;
	// public static final int ACTION_ID = 0;
	// protected static final int DELETE_ACTION_ID = 1;
	private boolean selectionMode = false;
	private List<GpxInfo> selectedItems = new ArrayList<>();
	private Set<Integer> selectedGroups = new LinkedHashSet<>();
	private ActionMode actionMode;
	private LoadGpxTask asyncLoader;
	private GpxIndexesAdapter allGpxAdapter;
	private ContextMenuAdapter optionsMenuAdapter;
	private AsyncTask<GpxInfo, ?, ?> operationTask;
	private GpxSelectionHelper selectedGpxHelper;
	private OsmandApplication app;
	private boolean updateEnable;
	private GpxInfo currentRecording;
	private boolean showOnMapMode;
	private View currentGpxView;
	private View footerView;
	private boolean importing = false;
	private View emptyView;
	private SelectGpxTaskListener gpxTaskListener;
	private TracksSortByMode sortByMode;

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
	public void onAttach(@NonNull Context activity) {
		super.onAttach(activity);
		this.app = (OsmandApplication) getActivity().getApplication();
		sortByMode = app.getSettings().TRACKS_SORT_BY_MODE.get();
		currentRecording = new GpxInfo(app.getSavingTrackHelper().getCurrentGpx(), getString(R.string.shared_string_currently_recording_track));
		currentRecording.currentlyRecordingTrack = true;
		asyncLoader = new LoadGpxTask();
		selectedGpxHelper = ((OsmandApplication) activity.getApplicationContext()).getSelectedGpxHelper();
		allGpxAdapter = new GpxIndexesAdapter(getActivity());
		setAdapter(allGpxAdapter);
	}

	public boolean isImporting() {
		return importing;
	}

	public void startImport() {
		this.importing = true;
	}

	public void finishImport(boolean success) {
		if (success) {
			reloadTracks();
		}
		this.importing = false;
	}

	private void startHandler() {
		Handler updateCurrentRecordingTrack = new Handler();
		updateCurrentRecordingTrack.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (getView() != null && updateEnable) {
					updateCurrentTrack();
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
		if (!importing) {
			if (asyncLoader == null || asyncLoader.getResult() == null) {
				asyncLoader = new LoadGpxTask();
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
		if (operationTask != null) {
			operationTask.cancel(true);
		}
		if (actionMode != null) {
			actionMode.finish();
		}
	}

	public void updateCurrentTrack() {
		final OsmandMonitoringPlugin plugin = OsmandPlugin.getEnabledPlugin(OsmandMonitoringPlugin.class);
		if (currentGpxView == null || plugin == null) {
			return;
		}

		final boolean isRecording = app.getSettings().SAVE_GLOBAL_TRACK_TO_GPX.get();

		ImageView icon = (ImageView) currentGpxView.findViewById(R.id.icon);
		icon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.monitoring_rec_big));
		icon.setVisibility(selectionMode && showOnMapMode ? View.GONE : View.VISIBLE);

		final boolean light = app.getSettings().isLightContent();
		SavingTrackHelper sth = app.getSavingTrackHelper();

		Button stop = (Button) currentGpxView.findViewById(R.id.action_button);
		if (isRecording) {
			currentGpxView.findViewById(R.id.segment_time_div).setVisibility(View.VISIBLE);
			TextView segmentTime = (TextView) currentGpxView.findViewById(R.id.segment_time);
			segmentTime.setText(OsmAndFormatter.getFormattedDurationShort((int)(sth.getDuration() / 1000)));
			segmentTime.setVisibility(View.VISIBLE);
			stop.setCompoundDrawablesWithIntrinsicBounds(app.getUIUtilities()
					.getIcon(R.drawable.ic_action_rec_stop, light ? R.color.active_color_primary_light : R.color.active_color_primary_dark), null, null, null);
			stop.setText(app.getString(R.string.shared_string_control_stop));
			stop.setContentDescription(app.getString(R.string.gpx_monitoring_stop));
		} else {
			currentGpxView.findViewById(R.id.segment_time_div).setVisibility(View.GONE);
			currentGpxView.findViewById(R.id.segment_time).setVisibility(View.GONE);
			stop.setCompoundDrawablesWithIntrinsicBounds(app.getUIUtilities()
					.getIcon(R.drawable.ic_action_rec_start, light ? R.color.active_color_primary_light : R.color.active_color_primary_dark), null, null, null);
			stop.setText(app.getString(R.string.shared_string_record));
			stop.setContentDescription(app.getString(R.string.gpx_monitoring_start));
		}
		stop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isRecording) {
					plugin.stopRecording();
					updateCurrentTrack();
				} else if (app.getLocationProvider().checkGPSEnabled(getActivity())) {
					plugin.startGPXMonitoring(getActivity());
					updateCurrentTrack();
				}
			}
		});
		Button save = (Button) currentGpxView.findViewById(R.id.save_button);
		save.setCompoundDrawablesWithIntrinsicBounds(app.getUIUtilities()
				.getIcon(R.drawable.ic_action_gsave_dark, light ? R.color.active_color_primary_light : R.color.active_color_primary_dark), null, null, null);
		save.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				plugin.saveCurrentTrack(new Runnable() {
					@Override
					public void run() {
						if (isResumed()) {
							reloadTracks();
						}
					}
				});
				updateCurrentTrack();
			}
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

		@SuppressWarnings("ConstantConditions")
		final CheckBox checkbox = (CheckBox) currentGpxView.findViewById(R.id.check_local_index);
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

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.available_gpx, container, false);
		listView = (ExpandableListView) v.findViewById(android.R.id.list);
		setHasOptionsMenu(true);
		if (OsmandPlugin.getEnabledPlugin(OsmandMonitoringPlugin.class) != null) {
			currentGpxView = inflater.inflate(R.layout.current_gpx_item, null, false);
			createCurrentTrackView();
			currentGpxView.findViewById(R.id.current_track_info).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						openTrack(activity, null, storeState(), getString(R.string.shared_string_tracks));
					}
				}
			});
			listView.addHeaderView(currentGpxView);
			/*
			currentTrackView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openTrack(getActivity(), null);
				}
			});
			*/
		}
		footerView = inflater.inflate(R.layout.list_shadow_footer, null, false);
		listView.addFooterView(footerView);
		emptyView = v.findViewById(android.R.id.empty);
		ImageView emptyImageView = (ImageView) emptyView.findViewById(R.id.empty_state_image_view);
		if (Build.VERSION.SDK_INT >= 18) {
			emptyImageView.setImageResource(app.getSettings().isLightContent() ? R.drawable.ic_empty_state_trip_day : R.drawable.ic_empty_state_trip_night);
		} else {
			emptyImageView.setVisibility(View.INVISIBLE);
		}
		Button importButton = (Button) emptyView.findViewById(R.id.import_button);
		importButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				addTrack();
			}
		});
		if (this.adapter != null) {
			listView.setAdapter(this.adapter);
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
		listView.setBackgroundColor(getResources().getColor(
				app.getSettings().isLightContent() ? R.color.activity_background_color_light
						: R.color.activity_background_color_dark));
	}

	public void createCurrentTrackView() {
		ImageView distanceI = (ImageView) currentGpxView.findViewById(R.id.distance_icon);
		distanceI.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_distance_16));
		ImageView pointsI = (ImageView) currentGpxView.findViewById(R.id.points_icon);
		pointsI.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_waypoint_16));
		updateCurrentTrack();
	}

	public void reloadTracks() {
		asyncLoader = new LoadGpxTask();
		asyncLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getActivity());
	}

	public void resetTracksLoader() {
		asyncLoader = null;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, @NonNull MenuInflater inflater) {
		menu.clear();
		MenuItem mi = createMenuItem(menu, SEARCH_ID, R.string.search_poi_filter, R.drawable.ic_action_search_dark,
				MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		SearchView searchView = new SearchView(getActivity());
		FavoritesActivity.updateSearchView(getActivity(), searchView);
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
				new Handler().postDelayed(new Runnable() {
					public void run() {
						hideProgressBar();
					}
				}, 100);
				return true;
			}
		});

		inflater.inflate(R.menu.track_sort_menu_item, menu);
		mi = menu.findItem(R.id.action_sort);
		mi.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		int iconColorId = isLightActionBar() ? R.color.active_buttons_and_links_text_light
				: R.color.active_buttons_and_links_text_dark;
		mi.setIcon(getIcon(sortByMode.getIconId(), iconColorId));

		if (AndroidUiHelper.isOrientationPortrait(getActivity())) {
			menu = ((FavoritesActivity) getActivity()).getClearToolbar(true).getMenu();
		} else {
			((FavoritesActivity) getActivity()).getClearToolbar(false);
		}
		((FavoritesActivity) getActivity()).updateListViewFooter(footerView);

		// To do Rewrite without ContextMenuAdapter
		optionsMenuAdapter = new ContextMenuAdapter(app);
		ItemClickListener listener = new ContextMenuAdapter.ItemClickListener() {
			@Override
			public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, final int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
				if (itemId == R.string.shared_string_refresh) {
					reloadTracks();
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
				} else if (itemId == R.string.gpx_add_track) {
					addTrack();
				}else if (itemId == R.string.coordinate_input) {
					openCoordinatesInput();
				}
				return true;
			}
		};
		optionsMenuAdapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.gpx_add_track, getActivity())
				.setIcon(R.drawable.ic_action_plus)
				.setListener(listener).createItem());
		optionsMenuAdapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.coordinate_input, getActivity())
				.setIcon(R.drawable.ic_action_coordinates_longitude)
				.setListener(listener).createItem());
		optionsMenuAdapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.shared_string_show_on_map, getActivity())
				.setIcon(R.drawable.ic_show_on_map)
				.setListener(listener).createItem());
		optionsMenuAdapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.shared_string_delete, getActivity())
				.setIcon(R.drawable.ic_action_delete_dark).setListener(listener).createItem());
		optionsMenuAdapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.shared_string_refresh, getActivity())
				.setIcon(R.drawable.ic_action_refresh_dark).setListener(listener).createItem());
		OsmandPlugin.onOptionsMenuActivity(getActivity(), this, optionsMenuAdapter);
		for (int j = 0; j < optionsMenuAdapter.length(); j++) {
			final MenuItem item;
			ContextMenuItem contextMenuItem = optionsMenuAdapter.getItem(j);
			item = menu.add(0, contextMenuItem.getTitleId(), j + 1, contextMenuItem.getTitle());
			item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			if (AndroidUiHelper.isOrientationPortrait(getActivity())) {
				item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem menuItem) {
						onOptionsItemSelected(item);
						return true;
					}
				});
			}
			if (contextMenuItem.getIcon() != -1) {
				int activeButtonsAndLinksTextColorResId = getMyApplication().getSettings().isLightContent() ?
						R.color.active_buttons_and_links_text_light : R.color.active_buttons_and_links_text_dark;
				Drawable icMenuItem = getMyApplication().getUIUtilities().getIcon(contextMenuItem.getIcon(), activeButtonsAndLinksTextColorResId);
				item.setIcon(icMenuItem);
			}
		}
	}

	public void doAction(int actionResId) {
		if (actionResId == R.string.shared_string_delete) {
			operationTask = new DeleteGpxTask();
			operationTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, selectedItems.toArray(new GpxInfo[0]));
		} else {
			operationTask = null;
		}
		if (actionMode != null) {
			actionMode.finish();
		}
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		int itemId = item.getItemId();
		for (int i = 0; i < optionsMenuAdapter.length(); i++) {
			ContextMenuItem contextMenuItem = optionsMenuAdapter.getItem(i);
			if (itemId == contextMenuItem.getTitleId()) {
				contextMenuItem.getItemClickListener().onContextMenuClick(null, itemId, i, false, null);
				return true;
			}
		}
		if (itemId == R.id.action_sort) {
			Activity activity = getActivity();
			if (activity != null) {
				boolean nightMode = app.getSettings().isLightContent();
				View menuSortItemView = getActivity().findViewById(R.id.action_sort);
				final List<PopUpMenuItem> items = new ArrayList<>();
				for (final TracksSortByMode mode : TracksSortByMode.values()) {
					items.add(new PopUpMenuItem.Builder(app)
							.setTitleId(mode.getNameId())
							.setIcon(app.getUIUtilities().getThemedIcon(mode.getIconId()))
							.setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									updateTracksSort(mode);
									int iconColorId = isLightActionBar() ? R.color.active_buttons_and_links_text_light
											: R.color.active_buttons_and_links_text_dark;
									item.setIcon(getIcon(mode.getIconId(), iconColorId));
								}
							})
							.setSelected(sortByMode == mode)
							.create()
					);
				}
				new PopUpMenuHelper.Builder(menuSortItemView, items, nightMode).show();
			}
		}
		return super.onOptionsItemSelected(item);
	}

	private void addTrack() {
		((FavoritesActivity) getActivity()).addTrack();
	}

	private void updateTracksSort(TracksSortByMode sortByMode) {
		this.sortByMode = sortByMode;
		app.getSettings().TRACKS_SORT_BY_MODE.set(sortByMode);
		reloadTracks();
	}

	private void openCoordinatesInput() {
		CoordinateInputDialogFragment fragment = new CoordinateInputDialogFragment();
		fragment.setRetainInstance(true);
		fragment.show(getChildFragmentManager(), CoordinateInputDialogFragment.TAG);
	}

	public void showProgressBar() {
		if (getActivity() != null) {
			((FavoritesActivity) getActivity()).setSupportProgressBarIndeterminateVisibility(true);
		}
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
			((FavoritesActivity) getActivity()).updateListViewFooter(footerView);
		}
	}

	private void openShowOnMapMode() {
		enableSelectionMode(true);
		showOnMapMode = true;
		selectedItems.clear();
		selectedGroups.clear();
		final Set<GpxInfo> originalSelectedItems = allGpxAdapter.getSelectedGpx();
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

	public void runSelection(Set<GpxInfo> originalSelectedItems) {
		HashMap<String, Boolean> selectedItemsFileNames = new HashMap<>();
		originalSelectedItems.addAll(selectedItems);
		for (GpxInfo gpxInfo : originalSelectedItems) {
			String path = gpxInfo.currentlyRecordingTrack ? CURRENT_TRACK : gpxInfo.file.getAbsolutePath();
			selectedItemsFileNames.put(path, selectedItems.contains(gpxInfo));
		}
		selectedGpxHelper.runSelection(selectedItemsFileNames, gpxTaskListener);
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
			Toast.makeText(getActivity(),
					app.getString(R.string.local_index_no_items_to_do, actionButton.toLowerCase()), Toast.LENGTH_SHORT)
					.show();
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
					Toast.makeText(getActivity(),
							app.getString(R.string.local_index_no_items_to_do, actionButton.toLowerCase()),
							Toast.LENGTH_SHORT).show();
					return true;
				}

				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setMessage(getString(R.string.local_index_action_do, actionButton.toLowerCase(),
						String.valueOf(selectedItems.size())));
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
		info.setGpx(GPXUtilities.loadGPXFile(info.file));
		boolean e = true;
		if (info.gpx != null) {
			WptPt loc = info.gpx.findPointToShow();
			OsmandApplication app = requireMyApplication();
			OsmandSettings settings = app.getSettings();
			if (loc != null) {
				settings.setMapLocationToShow(loc.lat, loc.lon, settings.getLastKnownMapZoom());
				e = false;
				app.getSelectedGpxHelper().setGpxFileToDisplay(info.gpx);
				MapActivity.launchMapActivityMoveToTop(getActivity(), storeState());
			}
		}
		if (e) {
			Toast.makeText(getActivity(), R.string.gpx_file_is_empty, Toast.LENGTH_LONG).show();
		}
	}

	private void moveGpx(final GpxInfo info) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			MoveGpxFileBottomSheet.showInstance(activity.getSupportFragmentManager(), this, info.file.getAbsolutePath(), false, false);
		}
	}

	@Override
	public Bundle storeState() {
		Bundle bundle = new Bundle();
		bundle.putInt(TAB_ID, GPX_TAB);
		return bundle;
	}

	@Override
	public void restoreState(Bundle bundle) {
	}

	@Override
	public void authorizationCompleted() {
		Bundle bundle = new Bundle();
		bundle.putInt(TAB_ID, GPX_TAB);

		Intent intent = new Intent(app, app.getAppCustomization().getFavoritesActivity());
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
			asyncLoader = new LoadGpxTask();
			asyncLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getActivity());
		} else {
			app.showToastMessage(R.string.file_can_not_be_moved);
		}
  }

	public void renamedTo(File file) {
		reloadTracks();
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
			showProgressBar();
			listView.setEmptyView(null);
			allGpxAdapter.clear();
		}

		@Override
		protected void onProgressUpdate(GpxInfo... values) {
			for (GpxInfo v : values) {
				allGpxAdapter.addLocalIndexInfo(v);
			}
			allGpxAdapter.notifyDataSetChanged();
		}

		@Override
		protected void onPostExecute(List<GpxInfo> result) {
			this.result = result;
			allGpxAdapter.refreshSelected();
			hideProgressBar();
			listView.setEmptyView(emptyView);
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
			// This file could be sorted in different way for folders
			// now folders are also sorted by last modified date
			final Collator collator = OsmAndCollator.primaryCollator();
			Arrays.sort(listFiles, new Comparator<File>() {
				@Override
				public int compare(File f1, File f2) {
					if (sortByMode == TracksSortByMode.BY_NAME_ASCENDING) {
						return collator.compare(f1.getName(), (f2.getName()));
					} else if (sortByMode == TracksSortByMode.BY_NAME_DESCENDING) {
						return -collator.compare(f1.getName(), (f2.getName()));
					} else {
						// here we could guess date from file name '2017-08-30 ...' - first part date
						if (f1.lastModified() == f2.lastModified()) {
							return -collator.compare(f1.getName(), (f2.getName()));
						}
						return -((f1.lastModified() < f2.lastModified()) ? -1 : ((f1.lastModified() == f2.lastModified()) ? 0 : 1));
					}
				}
			});
			return listFiles;
		}

		private void loadGPXData(File mapPath, List<GpxInfo> result, LoadGpxTask loadTask) {
			if (mapPath.canRead()) {
				List<GpxInfo> progress = new ArrayList<>();
				loadGPXFolder(mapPath, result, loadTask, progress, "");
				if (!progress.isEmpty()) {
					loadTask.loadFile(progress.toArray(new GpxInfo[0]));
				}
			}
		}

		private void loadGPXFolder(File mapPath, List<GpxInfo> result, LoadGpxTask loadTask, List<GpxInfo> progress,
		                           String gpxSubfolder) {
			File[] listFiles = listFilesSorted(mapPath);
			for (File gpxFile : listFiles) {
				if (gpxFile.isDirectory()) {
					String sub = gpxSubfolder.length() == 0 ? gpxFile.getName() : gpxSubfolder + "/"
							+ gpxFile.getName();
					loadGPXFolder(gpxFile, result, loadTask, progress, sub);
				} else if (gpxFile.isFile() && gpxFile.getName().toLowerCase().endsWith(IndexConstants.GPX_FILE_EXT)) {
					GpxInfo info = new GpxInfo();
					info.subfolder = gpxSubfolder;
					info.file = gpxFile;
					result.add(info);
					progress.add(info);
					if (progress.size() > 7) {
						loadTask.loadFile(progress.toArray(new GpxInfo[0]));
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

		private GpxInfoViewCallback updateGpxCallback = new GpxInfoViewCallback() {

			private static final int UPDATE_GPX_ITEM_MSG_ID = OsmAndConstants.UI_HANDLER_LOCATION_SERVICE + 5;
			private static final long MIN_UPDATE_INTERVAL = 500;

			private long lastUpdateTime;

			private Runnable updateItemsProc = new Runnable() {
				@Override
				public void run() {
					if (updateEnable) {
						lastUpdateTime = System.currentTimeMillis();
						allGpxAdapter.notifyDataSetChanged();
					}
				}
			};

			@Override
			public boolean isCancelled() {
				return !updateEnable;
			}

			@Override
			public void onGpxDataItemChanged(GpxDataItem item) {
				if (System.currentTimeMillis() - lastUpdateTime > MIN_UPDATE_INTERVAL) {
					updateItemsProc.run();
				}
				app.runMessageInUIThreadAndCancelPrevious(UPDATE_GPX_ITEM_MSG_ID, updateItemsProc, MIN_UPDATE_INTERVAL);
			}
		};

		public GpxIndexesAdapter(Context ctx) {
			warningColor = ContextCompat.getColor(ctx, R.color.color_warning);
			TypedArray ta = ctx.getTheme().obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary});
			defaultColor = ta.getColor(0, ContextCompat.getColor(ctx, R.color.color_unknown));
			ta.recycle();
		}

		public void refreshSelected() {
			selected.clear();
			selected.addAll(getSelectedGpx());
			final Collator collator = OsmAndCollator.primaryCollator();
			Collections.sort(selected, new Comparator<GpxInfo>() {
				@Override
				public int compare(GpxInfo i1, GpxInfo i2) {
					if (sortByMode == TracksSortByMode.BY_NAME_ASCENDING) {
						return collator.compare(i1.getName(), i2.getName());
					} else if (sortByMode == TracksSortByMode.BY_NAME_DESCENDING) {
						return -collator.compare(i1.getName(), i2.getName());
					} else {
						if (i1.file == null || i2.file == null) {
							return collator.compare(i1.getName(), i2.getName());
						}
						long time1 = i1.file.lastModified();
						long time2 = i2.file.lastModified();
						if (time1 == time2) {
							return collator.compare(i1.getName(), i2.getName());
						}
						return -((time1 < time2) ? -1 : ((time1 == time2) ? 0 : 1));
					}
				}
			});
			notifyDataSetChanged();
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
				if (objectEquals(catName, cat)) {
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
			updateGpxInfoView(v, child, app, false, updateGpxCallback);

			ImageView icon = (ImageView) v.findViewById(R.id.icon);
			ImageButton options = (ImageButton) v.findViewById(R.id.options);
			options.setImageDrawable(getMyApplication().getUIUtilities().getThemedIcon(R.drawable.ic_overflow_menu_white));
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
						// Issue 6187: Sync checkbox status between Visible group and rest of list
						allGpxAdapter.notifyDataSetInvalidated();
					}
				});
				icon.setVisibility(View.GONE);
				//INVISIBLE instead of GONE avoids lines breaking differently in selection mode
				options.setVisibility(View.INVISIBLE);
			} else {
				icon.setVisibility(View.VISIBLE);
				options.setVisibility(View.VISIBLE);
			}

			final CompoundButton checkItem = (CompoundButton) v.findViewById(R.id.toggle_item);
			if (isSelectedGroup(groupPosition)) {
				v.findViewById(R.id.check_item).setVisibility(selectionMode? View.INVISIBLE : View.VISIBLE);
				v.findViewById(R.id.options).setVisibility(View.GONE);
			} else {
				v.findViewById(R.id.check_item).setVisibility(View.GONE);
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
		public View getGroupView(final int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			View v = convertView;
			String group = getGroup(groupPosition);
			if (v == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				v = inflater.inflate(net.osmand.plus.R.layout.expandable_list_item_category, parent, false);
			}
			// Issue 6187: Always show visible group, also in selection mode
			//v.findViewById(R.id.group_divider).setVisibility(groupPosition == 0 ? View.GONE : View.VISIBLE);
			v.findViewById(R.id.group_divider).setVisibility(View.VISIBLE);

			StringBuilder t = new StringBuilder();
			String groupName = group.replaceAll("_", " ").replace(IndexConstants.GPX_FILE_EXT, "");
			if (groupName.length() == 0) {
				groupName = getString(R.string.shared_string_tracks);
			}
			t.append(capitalizeFirstLetter(groupName));
			boolean light = app.getSettings().isLightContent();

			if (selectionMode) {
				final CheckBox ch = (CheckBox) v.findViewById(R.id.toggle_item);
				// Issue 6187: No selection box for Visible group header
				//ch.setVisibility(View.VISIBLE);
				ch.setVisibility((selectionMode && !(groupPosition == 0 && isShowingSelection())) ? View.VISIBLE : View.GONE);
				ch.setChecked(selectedGroups.contains(groupPosition));

				ch.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (ch.isChecked()) {
							selectedItems.addAll(data.get(category.get(getGroupPosition(groupPosition))));
							selectedGroups.add(groupPosition);
						} else {
							selectedItems.removeAll(data.get(category.get(getGroupPosition(groupPosition))));
							selectedGroups.remove(groupPosition);
						}
						allGpxAdapter.notifyDataSetInvalidated();
						updateSelectionMode(actionMode);
					}
				});
				v.findViewById(R.id.category_icon).setVisibility(View.GONE);
			} else {
				final CheckBox ch = (CheckBox) v.findViewById(R.id.toggle_item);
				ch.setVisibility(View.GONE);
				if (isSelectedGroup(groupPosition)) {
					setCategoryIcon(app, app.getUIUtilities().getIcon(R.drawable.ic_map, R.color.osmand_orange), groupPosition, isExpanded, v, light);
				} else {
					setCategoryIcon(app, 0, groupPosition, isExpanded, v, light);
				}
				v.findViewById(R.id.category_icon).setVisibility(View.VISIBLE);
			}

			adjustIndicator(app, groupPosition, isExpanded, v, light);
			TextView nameView = ((TextView) v.findViewById(R.id.category_name));
			nameView.setText(t.toString());

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

	private class OpenGpxDetailsTask extends AsyncTask<Void, Void, GpxDisplayItem> {

		GpxInfo gpxInfo;
		ProgressDialog progressDialog;

		OpenGpxDetailsTask(GpxInfo gpxInfo) {
			this.gpxInfo = gpxInfo;
		}

		@Override
		protected void onPreExecute() {
			if (gpxInfo.gpx == null && gpxInfo.file != null) {
				progressDialog = new ProgressDialog(getActivity());
				progressDialog.setTitle("");
				progressDialog.setMessage(getActivity().getResources().getString(R.string.loading_data));
				progressDialog.setCancelable(false);
				progressDialog.show();
			}
		}

		@Override
		protected GpxDisplayItem doInBackground(Void... voids) {
			GpxDisplayGroup gpxDisplayGroup = null;
			GPXFile gpxFile = null;
			Track generalTrack = null;
			if (gpxInfo.gpx == null) {
				if (gpxInfo.file != null) {
					gpxFile = GPXUtilities.loadGPXFile(gpxInfo.file);
				}
			} else {
				gpxFile = gpxInfo.gpx;
			}
			if (gpxFile != null) {
				generalTrack = gpxFile.getGeneralTrack();
			}
			if (generalTrack != null) {
				gpxFile.addGeneralTrack();
				gpxDisplayGroup = selectedGpxHelper.buildGeneralGpxDisplayGroup(gpxFile, generalTrack);
			} else if (gpxFile != null && gpxFile.tracks.size() > 0) {
				gpxDisplayGroup = selectedGpxHelper.buildGeneralGpxDisplayGroup(gpxFile, gpxFile.tracks.get(0));
			}
			List<GpxDisplayItem> items = null;
			if (gpxDisplayGroup != null) {
				items = gpxDisplayGroup.getModifiableList();
			}
			if (items != null && items.size() > 0) {
				return items.get(0);
			}
			return null;
		}

		@Override
		protected void onPostExecute(GpxDisplayItem gpxItem) {
			if (progressDialog != null) {
				progressDialog.dismiss();
			}
			if (gpxItem != null && gpxItem.analysis != null) {
				ArrayList<GPXDataSetType> list = new ArrayList<>();
				if (gpxItem.analysis.hasElevationData) {
					list.add(GPXDataSetType.ALTITUDE);
				}
				if (gpxItem.analysis.hasSpeedData) {
					list.add(GPXDataSetType.SPEED);
				} else if (gpxItem.analysis.hasElevationData) {
					list.add(GPXDataSetType.SLOPE);
				}
				if (list.size() > 0) {
					gpxItem.chartTypes = list.toArray(new GPXDataSetType[0]);
				}
				final OsmandSettings settings = app.getSettings();
				settings.setMapLocationToShow(gpxItem.locationStart.lat, gpxItem.locationStart.lon,
						settings.getLastKnownMapZoom(),
						new PointDescription(PointDescription.POINT_TYPE_WPT, gpxItem.name),
						false,
						gpxItem);
				MapActivity.launchMapActivityMoveToTop(getActivity());
			}
		}
	}

	private void openPopUpMenu(View v, final GpxInfo gpxInfo) {
		boolean nightMode = app.getSettings().isLightContent();
		final List<PopUpMenuItem> items = new ArrayList<>();
		UiUtilities iconsCache = getMyApplication().getUIUtilities();

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_show_on_map)
				.setIcon(iconsCache.getThemedIcon(R.drawable.ic_show_on_map))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						showGpxOnMap(gpxInfo);
					}
				})
				.create()
		);

		GPXTrackAnalysis analysis;
		if ((analysis = getGpxTrackAnalysis(gpxInfo, app, null)) != null) {
			if (analysis.totalDistance != 0 && !gpxInfo.currentlyRecordingTrack) {
				items.add(new PopUpMenuItem.Builder(app)
						.setTitleId(R.string.analyze_on_map)
						.setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_info_dark))
						.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								new OpenGpxDetailsTask(gpxInfo).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
							}
						})
						.create()
				);
			}
		}

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_move)
				.setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_folder_stroke))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						moveGpx(gpxInfo);
					}
				})
				.create()
		);

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_rename)
				.setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_edit_dark))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						FragmentActivity activity = getActivity();
						if (activity != null) {
							FileUtils.renameFile(activity, gpxInfo.file, AvailableGPXFragment.this, false);
						}
					}
				})
				.create()
		);

		Drawable shareIcon = iconsCache.getThemedIcon((R.drawable.ic_action_gshare_dark));
		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_share)
				.setIcon(AndroidUtils.getDrawableForDirection(app, shareIcon))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						final Uri fileUri = AndroidUtils.getUriForFile(getMyApplication(), gpxInfo.file);
						final Intent sendIntent = new Intent(Intent.ACTION_SEND);
						sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
						sendIntent.setType("text/plain");
						sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
						startActivity(sendIntent);
					}
				})
				.create()
		);

		final OsmEditingPlugin osmEditingPlugin = OsmandPlugin.getEnabledPlugin(OsmEditingPlugin.class);
		if (osmEditingPlugin != null && osmEditingPlugin.isActive()) {
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_export)
					.setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_export))
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							osmEditingPlugin.sendGPXFiles(getActivity(), AvailableGPXFragment.this, gpxInfo);
						}
					})
					.create()
			);
		}

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_delete)
				.setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_delete_dark))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
						builder.setMessage(R.string.recording_delete_confirm);
						builder.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								operationTask = new DeleteGpxTask();
								operationTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, gpxInfo);
							}
						});
						builder.setNegativeButton(R.string.shared_string_cancel, null);
						builder.show();
					}
				})
				.create()
		);
		new PopUpMenuHelper.Builder(v, items, nightMode).show();
	}

	public class DeleteGpxTask extends AsyncTask<GpxInfo, GpxInfo, String> {

		@Override
		protected String doInBackground(GpxInfo... params) {
			int count = 0;
			int total = 0;
			for (GpxInfo info : params) {
				if (!isCancelled() && (info.gpx == null || !info.gpx.showCurrentTrack)) {
					boolean successful = FileUtils.removeGpxFile(app, info.file);
					total++;
					if (successful) {
						count++;
						publishProgress(info);
					}
				}
			}
			return getString(R.string.local_index_items_deleted, count, total);
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
			showProgressBar();
		}

		@Override
		protected void onPostExecute(String result) {
			hideProgressBar();
			Toast.makeText(getActivity(), result, Toast.LENGTH_LONG).show();
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
					if (r.getName().toLowerCase().contains(cs)) {
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
			openTrack(getActivity(), item.file, storeState(), getString(R.string.shared_string_tracks));
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

	public interface GpxInfoViewCallback {

		boolean isCancelled();

		void onGpxDataItemChanged(GpxDataItem item);
	}

	public static void updateGpxInfoView(View v, GpxInfo child, OsmandApplication app, boolean isDashItem, @Nullable GpxInfoViewCallback callback) {
		TextView viewName = ((TextView) v.findViewById(R.id.name));
		if (!isDashItem) {
			v.findViewById(R.id.divider_list).setVisibility(View.VISIBLE);
			v.findViewById(R.id.divider_dash).setVisibility(View.GONE);
		} else {
			v.findViewById(R.id.divider_dash).setVisibility(View.VISIBLE);
			v.findViewById(R.id.divider_list).setVisibility(View.GONE);
		}

		viewName.setText(child.getName());

		// ImageView icon = (ImageView) v.findViewById(!isDashItem? R.id.icon : R.id.show_on_map);
		ImageView icon = (ImageView) v.findViewById(R.id.icon);
		icon.setVisibility(View.VISIBLE);
		icon.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_polygom_dark));
		if (child.isCorrupted()) {
			viewName.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
		} else {
			viewName.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
		}
		if (getSelectedGpxFile(child, app) != null) {
			icon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_polygom_dark, R.color.color_distance));
		}
		GPXTrackAnalysis analysis = getGpxTrackAnalysis(child, app, callback);
		boolean sectionRead = analysis == null;
		if (sectionRead) {
			v.findViewById(R.id.read_section).setVisibility(View.GONE);
			v.findViewById(R.id.unknown_section).setVisibility(View.VISIBLE);
			String date = "";
			String size = "";

			if (child.getSize() >= 0) {
				size = AndroidUtils.formatSize(v.getContext(), child.getSize() * 1024l);
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
			distanceI.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_distance_16));
			ImageView pointsI = (ImageView) v.findViewById(R.id.points_icon);
			pointsI.setVisibility(View.VISIBLE);
			pointsI.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_waypoint_16));
			ImageView timeI = (ImageView) v.findViewById(R.id.time_icon);
			timeI.setVisibility(View.VISIBLE);
			timeI.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_time_16));
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
				time.setText(formatDuration((int) (analysis.timeSpan / 1000), app.accessibilityEnabled()) + "");
//				}
			} else {
				time.setText("");
			}
		}

		TextView descr = ((TextView) v.findViewById(R.id.description));
		descr.setVisibility(View.GONE);

		v.findViewById(R.id.check_item).setVisibility(View.GONE);
	}

	private static SelectedGpxFile getSelectedGpxFile(GpxInfo gpxInfo, OsmandApplication app) {
		GpxSelectionHelper selectedGpxHelper = app.getSelectedGpxHelper();
		return gpxInfo.currentlyRecordingTrack ? selectedGpxHelper.getSelectedCurrentRecordingTrack() :
				selectedGpxHelper.getSelectedFileByName(gpxInfo.getFileName());
	}

	@Nullable
	private static GPXTrackAnalysis getGpxTrackAnalysis(GpxInfo gpxInfo, OsmandApplication app, @Nullable final GpxInfoViewCallback callback) {
		SelectedGpxFile sgpx = getSelectedGpxFile(gpxInfo, app);
		GPXTrackAnalysis analysis = null;
		if (sgpx != null && sgpx.isLoaded()) {
				analysis = sgpx.getTrackAnalysis(app);
		} else if (gpxInfo.currentlyRecordingTrack) {
			analysis = app.getSavingTrackHelper().getCurrentTrack().getTrackAnalysis(app);
		} else if (gpxInfo.file != null) {
			GpxDataItemCallback analyserCallback = null;
			if (callback != null) {
				analyserCallback = new GpxDataItemCallback() {
					@Override
					public boolean isCancelled() {
						return callback.isCancelled();
					}

					@Override
					public void onGpxDataItemReady(GpxDataItem item) {
						callback.onGpxDataItemChanged(item);
					}
				};
			}
			GpxDataItem dataItem = app.getGpxDbHelper().getItem(gpxInfo.file, analyserCallback);
			if (dataItem != null) {
				analysis = dataItem.getAnalysis();
			}
		}
		return analysis;
	}
}
