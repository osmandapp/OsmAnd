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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.TextWatcher;
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
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.GPXUtilities.Track;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.IndexConstants;
import net.osmand.data.PointDescription;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.ItemClickListener;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GpxDbHelper.GpxDataItemCallback;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.activities.TrackActivity;
import net.osmand.plus.base.OsmandExpandableListFragment;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.download.ui.LocalIndexesFragment;
import net.osmand.plus.download.ui.LocalIndexesFragment.RenameCallback;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetType;
import net.osmand.plus.mapmarkers.CoordinateInputDialogFragment;
import net.osmand.plus.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.Collator;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static net.osmand.plus.myplaces.FavoritesActivity.GPX_TAB;
import static net.osmand.plus.myplaces.FavoritesActivity.TAB_ID;

public class AvailableGPXFragment extends OsmandExpandableListFragment implements
	FavoritesFragmentStateHolder {

	public static final Pattern ILLEGAL_PATH_NAME_CHARACTERS = Pattern.compile("[?:\"*|<>]");
	public static final int SEARCH_ID = -1;
	// public static final int ACTION_ID = 0;
	// protected static final int DELETE_ACTION_ID = 1;
	private boolean selectionMode = false;
	private List<GpxInfo> selectedItems = new ArrayList<>();
	private Set<Integer> selectedGroups = new LinkedHashSet<>();
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
	private View currentGpxView;
	private View footerView;
	private boolean importing = false;
	private View emptyView;

	@Override
	public void onAttach(Context activity) {
		super.onAttach(activity);
		this.app = (OsmandApplication) getActivity().getApplication();
		final Collator collator = Collator.getInstance();
		collator.setStrength(Collator.SECONDARY);
		currentRecording = new GpxInfo(getMyApplication().getSavingTrackHelper().getCurrentGpx(), getString(R.string.shared_string_currently_recording_track));
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
					Intent newIntent = new Intent(getActivity(), getMyApplication().getAppCustomization().getTrackActivity());
					newIntent.putExtra(TrackActivity.CURRENT_RECORDING, true);
					newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(newIntent);
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
		distanceI.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_small_distance));
		ImageView pointsI = (ImageView) currentGpxView.findViewById(R.id.points_icon);
		pointsI.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_small_point));
		updateCurrentTrack();
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

	public void reloadTracks() {
		asyncLoader = new LoadGpxTask();
		asyncLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getActivity());
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		MenuItem mi = createMenuItem(menu, SEARCH_ID, R.string.search_poi_filter, R.drawable.ic_action_search_dark, MenuItemCompat.SHOW_AS_ACTION_ALWAYS
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
		((FavoritesActivity) getActivity()).updateListViewFooter(footerView);

		// TODO Rewrite without ContextMenuAdapter
		optionsMenuAdapter = new ContextMenuAdapter();
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
			operationTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, selectedItems.toArray(new GpxInfo[selectedItems.size()]));
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
			ContextMenuItem contextMenuItem = optionsMenuAdapter.getItem(i);
			if (itemId == contextMenuItem.getTitleId()) {
				contextMenuItem.getItemClickListener().onContextMenuClick(null, itemId, i, false, null);
				return true;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	private void addTrack() {
		((FavoritesActivity) getActivity()).addTrack();
	}

	private void openCoordinatesInput() {
		CoordinateInputDialogFragment fragment = new CoordinateInputDialogFragment();
		fragment.setRetainInstance(true);
		fragment.show(getChildFragmentManager(), CoordinateInputDialogFragment.TAG);
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
				operationTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, originalSelectedItems.toArray(new GpxInfo[originalSelectedItems.size()]));
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
					Toast.makeText(getActivity(),
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

	private void collectDirs(File dir, List<File> dirs, File exclDir) {
		File[] listFiles = dir.listFiles();
		if (listFiles != null) {
			Arrays.sort(listFiles);
			for (File f : listFiles) {
				if (f.isDirectory()) {
					if (!exclDir.equals(f)) {
						dirs.add(f);
					}
					collectDirs(f, dirs, exclDir);
				}
			}
		}
	}

	private void moveGpx(final GpxInfo info) {

		final ContextMenuAdapter menuAdapter = new ContextMenuAdapter();
		ContextMenuItem.ItemBuilder itemBuilder = new ContextMenuItem.ItemBuilder();

		final List<File> dirs = new ArrayList<>();
		collectDirs(app.getAppPath(IndexConstants.GPX_INDEX_DIR), dirs, info.file.getParentFile());
		if (!info.file.getParentFile().equals(app.getAppPath(IndexConstants.GPX_INDEX_DIR))) {
			dirs.add(0, app.getAppPath(IndexConstants.GPX_INDEX_DIR));
		}
		String gpxDir = app.getAppPath(IndexConstants.GPX_INDEX_DIR).getPath();
		int i = 0;
		for (File dir : dirs) {
			String dirName = dir.getPath();
			if (dirName.startsWith(gpxDir)) {
				if (dirName.length() == gpxDir.length()) {
					dirName = dir.getName();
				} else {
					dirName = dirName.substring(gpxDir.length() + 1);
				}
			}
			menuAdapter.addItem(itemBuilder.setTitle(Algorithms.capitalizeFirstLetter(dirName))
					.setIcon(R.drawable.ic_action_folder_stroke).setTag(i).createItem());
			i++;
		}
		menuAdapter.addItem(itemBuilder.setTitleId(R.string.add_new_folder, app)
				.setIcon(R.drawable.map_zoom_in).setTag(-1).createItem());
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		final ArrayAdapter<ContextMenuItem> listAdapter =
				menuAdapter.createListAdapter(getActivity(), app.getSettings().isLightContent());
		builder.setTitle(R.string.select_gpx_folder);
		builder.setAdapter(listAdapter, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				ContextMenuItem item = menuAdapter.getItem(which);
				int index = item.getTag();
				if (index == -1) {
					Activity a = getActivity();
					AlertDialog.Builder b = new AlertDialog.Builder(a);
					b.setTitle(R.string.add_new_folder);
					final EditText editText = new EditText(a);
					editText.addTextChangedListener(new TextWatcher() {
						@Override
						public void beforeTextChanged(CharSequence s, int start, int count, int after) {
						}

						@Override
						public void onTextChanged(CharSequence s, int start, int before, int count) {
						}

						@Override
						public void afterTextChanged(Editable s) {
							Editable text = editText.getText();
							if (text.length() >= 1) {
								if (ILLEGAL_PATH_NAME_CHARACTERS.matcher(text).find()) {
									editText.setError(app.getString(R.string.file_name_containes_illegal_char));
								}
							}
						}
					});
					int leftPadding = AndroidUtils.dpToPx(a, 24f);
					int topPadding = AndroidUtils.dpToPx(a, 4f);
					b.setView(editText, leftPadding, topPadding, leftPadding, topPadding);
					// Behaviour will be overwritten later;
					b.setPositiveButton(R.string.shared_string_ok, null);
					b.setNegativeButton(R.string.shared_string_cancel, null);
					final AlertDialog alertDialog = b.create();
					alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
						@Override
						public void onShow(DialogInterface dialog) {
							alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
									new View.OnClickListener() {
										@Override
										public void onClick(View v) {
											String newName = editText.getText().toString();
											if (ILLEGAL_PATH_NAME_CHARACTERS.matcher(newName).find()) {
												Toast.makeText(app, R.string.file_name_containes_illegal_char,
														Toast.LENGTH_LONG).show();
												return;
											}
											File destFolder = new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), newName);
											if (destFolder.exists()) {
												Toast.makeText(app, R.string.file_with_name_already_exists,
														Toast.LENGTH_LONG).show();
												return;
											} else if (destFolder.mkdirs()) {
												File dest = new File(destFolder, info.fileName);
												if (info.file.renameTo(dest)) {
													app.getGpxDbHelper().rename(info.file, dest);
													asyncLoader = new LoadGpxTask();
													asyncLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getActivity());
												} else {
													Toast.makeText(app, R.string.file_can_not_be_moved, Toast.LENGTH_LONG).show();
												}

											} else {
												Toast.makeText(app, R.string.file_can_not_be_moved, Toast.LENGTH_LONG).show();
											}
											alertDialog.dismiss();
										}
									});
						}
					});
					alertDialog.show();
				} else {
					File dir = dirs.get(index);
					File dest = new File(dir, info.file.getName());
					if (dest.exists()) {
						Toast.makeText(app, R.string.file_with_name_already_exists, Toast.LENGTH_LONG).show();
					} else {
						if (info.file.renameTo(dest)) {
							app.getGpxDbHelper().rename(info.file, dest);
							asyncLoader = new LoadGpxTask();
							asyncLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getActivity());
						} else {
							Toast.makeText(app, R.string.file_can_not_be_moved, Toast.LENGTH_LONG).show();
						}
					}
				}
			}
		});
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.create().show();
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
			listView.setEmptyView(null);
			allGpxAdapter.clear();
		}

		@Override
		protected void onProgressUpdate(GpxInfo... values) {
			for (GpxInfo v : values) {
				allGpxAdapter.addLocalIndexInfo(v);
			}
			allGpxAdapter.sort();
			allGpxAdapter.notifyDataSetChanged();
		}

		public void setResult(List<GpxInfo> result) {
			this.result = result;
			allGpxAdapter.clear();
			if (result != null) {
				for (GpxInfo v : result) {
					allGpxAdapter.addLocalIndexInfo(v);
				}
				allGpxAdapter.sort();
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
				} else if (gpxFile.isFile() && gpxFile.getName().toLowerCase().endsWith(".gpx")) {
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
			Collections.sort(selected, new Comparator<GpxInfo>() {
				@Override
				public int compare(GpxInfo i1, GpxInfo i2) {
					return i1.getName().toLowerCase().compareTo(i2.getName().toLowerCase());
				}
			});
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

		public void sort() {
			Collections.sort(category, new Comparator<String>() {
				@Override
				public int compare(String lhs, String rhs) {
					return lhs.toLowerCase().compareTo(rhs.toLowerCase());
				}
			});
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
			String groupName = group.replaceAll("_", " ").replace(".gpx", "");
			if (groupName.length() == 0) {
				groupName = getString(R.string.shared_string_tracks);
			}
			t.append(Algorithms.capitalizeFirstLetter(groupName));
			boolean light = app.getSettings().isLightContent();

			if (selectionMode) {
				final CheckBox ch = (CheckBox) v.findViewById(R.id.toggle_item);
				// Issue 6187: No selection box for Visible group header
				//ch.setVisibility(View.VISIBLE);
				ch.setVisibility((selectionMode && !(groupPosition == 0 && isShowingSelection()))? View.VISIBLE : View.GONE);
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
					gpxItem.chartTypes = list.toArray(new GPXDataSetType[list.size()]);
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
		UiUtilities iconsCache = getMyApplication().getUIUtilities();
		final PopupMenu optionsMenu = new PopupMenu(getActivity(), v);
		DirectionsDialogs.setupPopUpMenuIcon(optionsMenu);

		MenuItem item = optionsMenu.getMenu().add(R.string.shared_string_show_on_map).setIcon(iconsCache.getThemedIcon(R.drawable.ic_show_on_map));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				showGpxOnMap(gpxInfo);
				return true;
			}
		});

		GPXTrackAnalysis analysis;
		if ((analysis = getGpxTrackAnalysis(gpxInfo, app, null)) != null) {
			if (analysis.totalDistance != 0 && !gpxInfo.currentlyRecordingTrack) {
				item = optionsMenu.getMenu().add(R.string.analyze_on_map).setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_info_dark));
				item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						new OpenGpxDetailsTask(gpxInfo).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
						return true;
					}
				});
			}
		}

		item = optionsMenu.getMenu().add(R.string.shared_string_move).setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_folder_stroke));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				moveGpx(gpxInfo);
				return true;
			}
		});

		item = optionsMenu.getMenu().add(R.string.shared_string_rename)
				.setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_edit_dark));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				LocalIndexesFragment.renameFile(getActivity(), gpxInfo.file, new RenameCallback() {
					@Override
					public void renamedTo(File file) {
						asyncLoader = new LoadGpxTask();
						asyncLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getActivity());
					}
				});
				return true;
			}
		});
		item = optionsMenu.getMenu().add(R.string.shared_string_share)
				.setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_gshare_dark));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				final Uri fileUri = AndroidUtils.getUriForFile(getMyApplication(), gpxInfo.file);
				final Intent sendIntent = new Intent(Intent.ACTION_SEND);
				sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
				sendIntent.setType("application/gpx+xml");
				sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
				startActivity(sendIntent);
				return true;
			}
		});

		final OsmEditingPlugin osmEditingPlugin = OsmandPlugin.getEnabledPlugin(OsmEditingPlugin.class);
		if (osmEditingPlugin != null && osmEditingPlugin.isActive()) {
			item = optionsMenu.getMenu().add(R.string.shared_string_export).setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_export));
			item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					osmEditingPlugin.sendGPXFiles(getActivity(), AvailableGPXFragment.this, gpxInfo);
					return true;
				}
			});

		}

		item = optionsMenu.getMenu().add(R.string.shared_string_delete)
				.setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_delete_dark));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
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
					app.getGpxDbHelper().remove(info.file);
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
			Toast.makeText(getActivity(), result, Toast.LENGTH_LONG).show();
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
						info.setGpx(GPXUtilities.loadGPXFile(info.file));
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
					allGpxAdapter.sort();
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
			distanceI.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_small_distance));
			ImageView pointsI = (ImageView) v.findViewById(R.id.points_icon);
			pointsI.setVisibility(View.VISIBLE);
			pointsI.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_small_point));
			ImageView timeI = (ImageView) v.findViewById(R.id.time_icon);
			timeI.setVisibility(View.VISIBLE);
			timeI.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_small_time));
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
				time.setText(Algorithms.formatDuration((int) (analysis.timeSpan / 1000), app.accessibilityEnabled()) + "");
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
		if (sgpx != null) {
			analysis = sgpx.getTrackAnalysis();
		} else if (gpxInfo.currentlyRecordingTrack) {
			analysis = app.getSavingTrackHelper().getCurrentTrack().getTrackAnalysis();
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
