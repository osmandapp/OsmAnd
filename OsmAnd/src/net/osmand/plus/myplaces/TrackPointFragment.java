package net.osmand.plus.myplaces;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnActionExpandListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.AndroidUtils;
import net.osmand.Collator;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.OsmAndCollator;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import net.osmand.plus.activities.TrackActivity;
import net.osmand.plus.base.OsmandExpandableListFragment;
import net.osmand.plus.base.PointImageDrawable;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.mapmarkers.CoordinateInputDialogFragment;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.myplaces.DeletePointsTask.OnPointsDeleteListener;
import net.osmand.plus.myplaces.TrackBitmapDrawer.TrackBitmapDrawerListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.SaveGpxAsyncTask.SaveGpxListener;
import net.osmand.plus.track.TrackDisplayHelper;
import net.osmand.plus.track.TrackMenuFragment;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


public class TrackPointFragment extends OsmandExpandableListFragment implements TrackBitmapDrawerListener, OnPointsDeleteListener {

	public static final int SEARCH_ID = -1;
	public static final int DELETE_ID = 2;
	public static final int DELETE_ACTION_ID = 3;
	public static final int SHARE_ID = 4;
	public static final int SELECT_MAP_MARKERS_ID = 5;
	public static final int COORDINATE_INPUT_ID = 6;
	//public static final int SELECT_MAP_MARKERS_ACTION_MODE_ID = 6;
	public static final int SELECT_FAVORITES_ID = 7;
	public static final int SELECT_FAVORITES_ACTION_MODE_ID = 8;

	private static final int ROUTE_POINTS_LIMIT = 3;

	private OsmandApplication app;
	private TrackActivityFragmentAdapter fragmentAdapter;
	final private PointGPXAdapter adapter = new PointGPXAdapter();
	private GpxDisplayItemType[] filterTypes = new GpxDisplayItemType[] {GpxDisplayItemType.TRACK_POINTS, GpxDisplayItemType.TRACK_ROUTE_POINTS};
	private TrackDisplayHelper displayHelper;

	private boolean selectionMode;
	private LinkedHashMap<GpxDisplayItemType, Set<GpxDisplayItem>> selectedItems = new LinkedHashMap<>();
	private Set<Integer> selectedGroups = new LinkedHashSet<>();
	private ActionMode actionMode;
	private boolean updateEnable;
	private boolean routePointsExpanded;

	private View mainView;
	private Menu optionsMenu;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.app = getMyApplication();

		FragmentActivity activity = getActivity();
		if (activity instanceof TrackActivity) {
			displayHelper = ((TrackActivity) activity).getDisplayHelper();
		} else if (getTargetFragment() instanceof TrackMenuFragment) {
			displayHelper = ((TrackMenuFragment) getTargetFragment()).getDisplayHelper();
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (fragmentAdapter != null) {
			fragmentAdapter.onActivityCreated(savedInstanceState);
		}
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mainView = inflater.inflate(R.layout.track_points_tree, container, false);
		ExpandableListView listView = (ExpandableListView) mainView.findViewById(android.R.id.list);

		fragmentAdapter = new TrackActivityFragmentAdapter(app, this, listView, displayHelper, filterTypes);
		fragmentAdapter.setShowMapOnly(true);
		fragmentAdapter.setTrackBitmapSelectionSupported(false);
		fragmentAdapter.setShowDescriptionCard(true);
		fragmentAdapter.onCreateView(mainView);

		setContent(listView);
		setListView(listView);
		expandAllGroups();

		return mainView;
	}

	@Override
	public void onResume() {
		super.onResume();
		setUpdateEnable(true);
	}

	@Override
	public void onPause() {
		super.onPause();
		setUpdateEnable(false);
		if (actionMode != null) {
			actionMode.finish();
		}
		if (optionsMenu != null) {
			optionsMenu.close();
		}
		if (fragmentAdapter != null && fragmentAdapter.colorListPopupWindow != null) {
			fragmentAdapter.colorListPopupWindow.dismiss();
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		fragmentAdapter = null;
	}

	public boolean isUpdateEnable() {
		return updateEnable;
	}

	public void setUpdateEnable(boolean updateEnable) {
		this.updateEnable = updateEnable;
		if (fragmentAdapter != null) {
			fragmentAdapter.setUpdateEnable(updateEnable);
		}
	}

	private int getSelectedItemsCount() {
		int count = 0;
		for (Set<GpxDisplayItem> set : selectedItems.values()) {
			if (set != null) {
				count += set.size();
			}
		}
		return count;
	}

	private Set<GpxDisplayItem> getSelectedItems() {
		Set<GpxDisplayItem> result = new LinkedHashSet<>();
		for (Set<GpxDisplayItem> set : selectedItems.values()) {
			if (set != null) {
				result.addAll(set);
			}
		}
		return result;
	}

	@Nullable
	private GPXFile getGpx() {
		return displayHelper.getGpx();
	}

	@Nullable
	private GpxDataItem getGpxDataItem() {
		return displayHelper.getGpxDataItem();
	}

	private void expandAllGroups() {
		for (int i = 0; i < adapter.getGroupCount(); i++) {
			getExpandableListView().expandGroup(i);
		}
	}

	private void setupListView(@NonNull ListView listView) {
		if (!adapter.isEmpty() && listView.getHeaderViewsCount() == 0) {
			listView.addHeaderView(getLayoutInflater().inflate(R.layout.list_shadow_header, null, false));
			listView.addFooterView(getLayoutInflater().inflate(R.layout.list_shadow_footer, null, false));
			View view = new View(getActivity());
			view.setLayoutParams(new AbsListView.LayoutParams(
					AbsListView.LayoutParams.MATCH_PARENT,
					AndroidUtils.dpToPx(listView.getContext(), 72)));
			listView.addFooterView(view);
		}
	}

	@Nullable
	private List<GpxDisplayGroup> getOriginalGroups() {
		return displayHelper.getOriginalGroups(filterTypes);
	}

	public void setContent() {
		setContent(listView);
		expandAllGroups();
	}

	public void setContent(ExpandableListView listView) {
		List<GpxDisplayGroup> groups = getOriginalGroups();
		if (groups != null) {
			adapter.synchronizeGroups(groups);
		}
		setupListView(listView);
		if (listView.getAdapter() == null) {
			listView.setAdapter(adapter);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == SELECT_MAP_MARKERS_ID) {
			selectMapMarkers();
			return true;
		} else if (item.getItemId() == SELECT_FAVORITES_ID) {
			selectFavorites();
			return true;
		} else if (item.getItemId() == SHARE_ID) {
			shareItems();
			return true;
		} else if (item.getItemId() == DELETE_ID) {
			enterDeleteMode();
			return true;
		} else if (item.getItemId() == COORDINATE_INPUT_ID) {
			openCoordinatesInput();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	private void selectMapMarkers() {
		if (getGpxDataItem() != null) {
			addOrRemoveMapMarkersSyncGroup();
		}
	}

	private void selectFavorites() {
		enterFavoritesMode();
	}

	private void shareItems() {
		final GPXFile gpxFile = getGpx();
		FragmentActivity activity = getActivity();
		if (gpxFile != null && activity != null) {
			if (Algorithms.isEmpty(gpxFile.path)) {
				SaveGpxListener saveGpxListener = new SaveGpxListener() {
					@Override
					public void gpxSavingStarted() {
						showProgressBar();
					}

					@Override
					public void gpxSavingFinished(Exception errorMessage) {
						if (isResumed()) {
							hideProgressBar();
							FragmentActivity activity = getActivity();
							if (activity != null) {
								GpxUiHelper.shareGpx(activity, new File(gpxFile.path));
							}
						}
					}
				};
				new SaveCurrentTrackTask(app, gpxFile, saveGpxListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else {
				GpxUiHelper.saveAndShareGpxWithAppearance(activity, gpxFile);
			}
		}
	}

	private void openCoordinatesInput() {
		FragmentManager fm = getFragmentManager();
		if (fm != null) {
			CoordinateInputDialogFragment fragment = new CoordinateInputDialogFragment();
			fragment.setRetainInstance(true);
			fragment.setListener(createOnPointsSavedListener());
			fragment.show(fm, CoordinateInputDialogFragment.TAG);
		}
	}

	private CoordinateInputDialogFragment.OnPointsSavedListener createOnPointsSavedListener() {
		return new CoordinateInputDialogFragment.OnPointsSavedListener() {
			@Override
			public void onPointsSaved() {
				setContent();
			}
		};
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		Activity activity = getActivity();
		if (activity == null) {
			return;
		}

		MenuItem mi = createMenuItem(menu, SEARCH_ID, R.string.search_poi_filter, R.drawable.ic_action_search_dark,
				MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		SearchView searchView = new SearchView(activity);
		FavoritesActivity.updateSearchView(activity, searchView);
		mi.setActionView(searchView);
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				adapter.getFilter().filter(query);
				return true;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				adapter.getFilter().filter(newText);
				return true;
			}
		});
		mi.setOnActionExpandListener(new OnActionExpandListener() {
			@Override
			public boolean onMenuItemActionExpand(MenuItem item) {
				return true;
			}

			@Override
			public boolean onMenuItemActionCollapse(MenuItem item) {
				adapter.setFilterResults(null);
				List<GpxDisplayGroup> groups = getOriginalGroups();
				if (groups != null) {
					adapter.synchronizeGroups(groups);
				}
				// Needed to hide intermediate progress bar after closing action mode
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						hideProgressBar();
					}
				}, 100);
				return true;
			}
		});

		if (!mi.isActionViewExpanded()) {

			createMenuItem(menu, SHARE_ID, R.string.shared_string_share, R.drawable.ic_action_gshare_dark, MenuItem.SHOW_AS_ACTION_NEVER, true);
			GPXFile gpxFile = getGpx();
			if (gpxFile != null && gpxFile.path != null) {
				final MapMarkersHelper markersHelper = app.getMapMarkersHelper();
				final boolean synced = markersHelper.getMarkersGroup(getGpx()) != null;
				createMenuItem(menu, SELECT_MAP_MARKERS_ID, synced ? R.string.remove_from_map_markers
						: R.string.shared_string_add_to_map_markers, R.drawable.ic_action_flag, MenuItem.SHOW_AS_ACTION_NEVER);
			}
			createMenuItem(menu, SELECT_FAVORITES_ID, R.string.shared_string_add_to_favorites, R.drawable.ic_action_favorite, MenuItem.SHOW_AS_ACTION_NEVER);
			createMenuItem(menu, DELETE_ID, R.string.shared_string_delete, R.drawable.ic_action_delete_dark, MenuItem.SHOW_AS_ACTION_NEVER);
			createMenuItem(menu, COORDINATE_INPUT_ID, R.string.coordinate_input, R.drawable.ic_action_coordinates_longitude, MenuItem.SHOW_AS_ACTION_NEVER);
		}
		this.optionsMenu = menu;
	}

	public void showProgressBar() {
		OsmandActionBarActivity activity = getActionBarActivity();
		if (activity != null) {
			activity.setSupportProgressBarIndeterminateVisibility(true);
		}
	}

	public void hideProgressBar() {
		OsmandActionBarActivity activity = getActionBarActivity();
		if (activity != null) {
			activity.setSupportProgressBarIndeterminateVisibility(false);
		}
	}

	private void setSelectionMode(boolean selectionMode) {
		this.selectionMode = selectionMode;
	}

	private void enterDeleteMode() {

		actionMode = getActionBarActivity().startSupportActionMode(new ActionMode.Callback() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				setSelectionMode(true);
				createMenuItem(menu, DELETE_ACTION_ID, R.string.shared_string_delete,
						R.drawable.ic_action_delete_dark,
						MenuItem.SHOW_AS_ACTION_IF_ROOM);
				selectedItems.clear();
				selectedGroups.clear();
				adapter.notifyDataSetInvalidated();
				updateSelectionMode(mode);
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				setSelectionMode(false);
				adapter.notifyDataSetInvalidated();
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				if (item.getItemId() == DELETE_ACTION_ID) {
					deleteItemsAction();
				}
				return true;
			}

		});

	}

	private void deleteItemsAction() {
		int size = getSelectedItemsCount();
		Activity activity = getActivity();
		if (size > 0 && activity != null) {
			AlertDialog.Builder b = new AlertDialog.Builder(activity);
			b.setMessage(getString(R.string.points_delete_multiple, size));
			b.setPositiveButton(R.string.shared_string_delete, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (actionMode != null) {
						actionMode.finish();
					}
					deleteItems();
				}
			});
			b.setNegativeButton(R.string.shared_string_cancel, null);
			b.show();
		}
	}

	private void deleteItems() {
		new DeletePointsTask(app, getGpx(), getSelectedItems(), this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void addOrRemoveMapMarkersSyncGroup() {
		final MapMarkersHelper markersHelper = app.getMapMarkersHelper();

		FragmentActivity activity = getActivity();
		if (activity == null) {
			return;
		}
		final GPXFile gpxFile = getGpx();
		MapMarkersGroup markersSearch = markersHelper.getMarkersGroup(gpxFile);
		final MapMarkersGroup markersGr;
		final boolean markersRemoved;
		if (markersSearch != null) {
			markersGr = markersSearch;
			markersHelper.removeMarkersGroup(markersGr);
			markersRemoved = true;
		} else if (gpxFile != null) {
			markersGr = markersHelper.addOrEnableGroup(gpxFile);
			markersRemoved = false;
		} else {
			markersRemoved = false;
			markersGr = null;
		}
		if (markersGr != null) {
			activity.invalidateOptionsMenu();
			if (gpxFile != null) {
				app.getSelectedGpxHelper().selectGpxFile(gpxFile, true, false, true, true, false);
			}
			if (fragmentAdapter != null) {
				fragmentAdapter.hideTransparentOverlay();
				fragmentAdapter.closeFabMenu(activity);
				fragmentAdapter.updateMenuFabVisibility(false);
			}
			Snackbar snackbar = Snackbar.make(mainView, markersRemoved ?
							R.string.waypoints_removed_from_map_markers : R.string.waypoints_added_to_map_markers,
					Snackbar.LENGTH_LONG)
					.setAction(R.string.shared_string_undo, new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							FragmentActivity trackActivity = getActivity();
							if (trackActivity != null) {
								if (markersRemoved) {
									if (gpxFile != null) {
										markersHelper.addOrEnableGroup(gpxFile);
									}
								} else {
									MapMarkersGroup group = markersHelper.getMarkersGroup(gpxFile);
									if (group != null) {
										markersHelper.removeMarkersGroup(group);
									}
								}
								trackActivity.invalidateOptionsMenu();
							}
						}
					});
			snackbar.addCallback(new Snackbar.Callback() {
				@Override
				public void onDismissed(Snackbar transientBottomBar, int event) {
					if (fragmentAdapter != null) {
						fragmentAdapter.updateMenuFabVisibility(true);
					}
					super.onDismissed(transientBottomBar, event);
				}
			});
			boolean nightMode = !app.getSettings().isLightContent();
			UiUtilities.setupSnackbar(snackbar, nightMode);
			snackbar.show();
		}
	}

	private void enterFavoritesMode() {
		actionMode = getActionBarActivity().startSupportActionMode(new ActionMode.Callback() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				setSelectionMode(true);
				createMenuItem(menu, SELECT_FAVORITES_ACTION_MODE_ID, R.string.shared_string_add_to_favorites,
						R.drawable.ic_action_favorite,
						MenuItem.SHOW_AS_ACTION_IF_ROOM);
				selectedItems.clear();
				selectedGroups.clear();
				adapter.notifyDataSetInvalidated();
				updateSelectionMode(mode);
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				setSelectionMode(false);
				adapter.notifyDataSetInvalidated();
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				if (item.getItemId() == SELECT_FAVORITES_ACTION_MODE_ID) {
					selectFavoritesImpl();
				}
				return true;
			}
		});

	}

	private void selectFavoritesImpl() {
		FragmentActivity activity = getActivity();
		if (activity != null && getSelectedItemsCount() > 0) {
			AlertDialog.Builder b = new AlertDialog.Builder(activity);
			final EditText editText = new EditText(activity);
			String name = getSelectedItems().iterator().next().group.getName();
			if (name.indexOf('\n') > 0) {
				name = name.substring(0, name.indexOf('\n'));
			}
			editText.setText(name);
			int leftMargin = AndroidUtils.dpToPx(activity, 16f);
			int topMargin = AndroidUtils.dpToPx(activity, 8f);
			editText.setPadding(leftMargin, topMargin, leftMargin, topMargin);
			b.setTitle(R.string.save_as_favorites_points);
			b.setView(editText);
			b.setPositiveButton(R.string.shared_string_save, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (actionMode != null) {
						actionMode.finish();
					}
					FavouritesDbHelper fdb = app.getFavorites();
					for (GpxDisplayItem i : getSelectedItems()) {
						if (i.locationStart != null) {
							FavouritePoint fp = FavouritePoint.fromWpt(
									i.locationStart, app, editText.getText().toString());
							if (!Algorithms.isEmpty(i.description)) {
								fp.setDescription(i.description);
							}
							fdb.addFavourite(fp, false);
						}
					}
					fdb.saveCurrentPointsIntoFile();
					MapActivity.launchMapActivityMoveToTop(getActivity());
				}
			});
			b.setNegativeButton(R.string.shared_string_cancel, null);
			b.show();
		}
	}

	private void updateSelectionMode(ActionMode m) {
		int size = getSelectedItemsCount();
		if (size > 0) {
			m.setTitle(size + " " + getMyApplication().getString(R.string.shared_string_selected_lowercase));
		} else {
			m.setTitle("");
		}
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		final GpxDisplayItem item = adapter.getChild(groupPosition, childPosition);
		if (item == null) {
			routePointsExpanded = true;
			adapter.notifyDataSetChanged();
		} else if (selectionMode) {
			CheckBox ch = (CheckBox) v.findViewById(R.id.toggle_item);
			ch.setChecked(!ch.isChecked());
			if (ch.isChecked()) {
				Set<GpxDisplayItem> set = selectedItems.get(item.group.getType());
				if (set != null) {
					set.add(item);
				} else {
					set = new LinkedHashSet<>();
					set.add(item);
					selectedItems.put(item.group.getType(), set);
				}
			} else {
				Set<GpxDisplayItem> set = selectedItems.get(item.group.getType());
				if (set != null) {
					set.remove(item);
				}
			}
			updateSelectionMode(actionMode);
		} else {
			GPXFile gpx = item.group.getGpx();
			if (gpx != null) {
				FragmentActivity activity = getActivity();
				if (activity != null && !TrackActivityFragmentAdapter.isGpxFileSelected(app, gpx)) {
					Intent intent = activity.getIntent();
					if (intent != null) {
						intent.putExtra(TrackActivity.SHOW_TEMPORARILY, true);
					}
				}
				app.getSelectedGpxHelper().setGpxFileToDisplay(gpx);
			}
			final OsmandSettings settings = app.getSettings();
			LatLon location = new LatLon(item.locationStart.lat, item.locationStart.lon);
			settings.setMapLocationToShow(location.getLatitude(), location.getLongitude(),
					settings.getLastKnownMapZoom(),
					new PointDescription(PointDescription.POINT_TYPE_WPT, item.name),
					false,
					item.locationStart);

			MapActivity.launchMapActivityMoveToTop(getActivity());
		}
		return true;
	}

	@Override
	public void onTrackBitmapDrawing() {
		if (fragmentAdapter != null) {
			fragmentAdapter.onTrackBitmapDrawing();
		}
	}

	@Override
	public void onTrackBitmapDrawn() {
		if (fragmentAdapter != null) {
			fragmentAdapter.onTrackBitmapDrawn();
		}
	}

	@Override
	public boolean isTrackBitmapSelectionSupported() {
		return fragmentAdapter != null && fragmentAdapter.isTrackBitmapSelectionSupported();
	}

	@Override
	public void drawTrackBitmap(Bitmap bitmap) {
		if (fragmentAdapter != null) {
			fragmentAdapter.drawTrackBitmap(bitmap);
		}
	}

	public void updateHeader() {
		if (fragmentAdapter != null) {
			fragmentAdapter.updateHeader(0);
		}
	}

	@Override
	public void onPointsDeletionStarted() {
		showProgressBar();
	}

	@Override
	public void onPointsDeleted() {
		selectedItems.clear();
		selectedGroups.clear();

		hideProgressBar();
		List<GpxDisplayGroup> groups = getOriginalGroups();
		if (groups != null) {
			adapter.synchronizeGroups(groups);
		}
	}

	class PointGPXAdapter extends OsmandBaseExpandableListAdapter implements Filterable {

		Map<GpxDisplayGroup, List<GpxDisplayItem>> itemGroups = new LinkedHashMap<>();
		List<GpxDisplayGroup> groups = new ArrayList<>();
		Filter myFilter;
		private Set<?> filter;
		Comparator<String> comparator;

		PointGPXAdapter() {
			final Collator collator = OsmAndCollator.primaryCollator();
			comparator = new Comparator<String>() {
				@Override
				public int compare(String s1, String s2) {
					return collator.compare(s1, s2);
				}
			};
		}

		public void synchronizeGroups(@NonNull List<GpxDisplayGroup> gs) {
			itemGroups.clear();
			groups.clear();
			Set<?> flt = filter;
			Collections.sort(gs, new Comparator<GpxDisplayGroup>() {
				@Override
				public int compare(GpxDisplayGroup g1, GpxDisplayGroup g2) {
					int i1 = g1.getType().ordinal();
					int i2 = g2.getType().ordinal();
					return i1 < i2 ? -1 : (i1 == i2 ? 0 : 1);
				}
			});
			for (GpxDisplayGroup g : gs) {
				if (g.getModifiableList().isEmpty()) {
					continue;
				}
				boolean empty = true;
				List<GpxDisplayItem> displayItems = g.getModifiableList();
				Map<String, List<GpxDisplayItem>> itemsMap = new HashMap<>();
				for (GpxDisplayItem item : displayItems) {
					String category;
					if (item.locationStart != null && g.getType() == GpxDisplayItemType.TRACK_POINTS) {
						category = item.locationStart.category;
						if (TextUtils.isEmpty(category)) {
							category = "";
						}
					} else {
						category = "";
					}
					List<GpxDisplayItem> items = itemsMap.get(category);
					if (items == null) {
						items = new ArrayList<>();
						itemsMap.put(category, items);
					}
					items.add(item);
				}
				if (flt == null) {
					empty = false;
				} else {
					Map<String, List<GpxDisplayItem>> itemsMapFiltered = new HashMap<>();
					for (Entry<String, List<GpxDisplayItem>> e : itemsMap.entrySet()) {
						String category = e.getKey();
						List<GpxDisplayItem> items = e.getValue();
						if (flt.contains(category)) {
							itemsMapFiltered.put(category, items);
							empty = false;
						} else {
							for (GpxDisplayItem i : items) {
								if (flt.contains(i)) {
									List<GpxDisplayItem> itemsFiltered = itemsMapFiltered.get(category);
									if (itemsFiltered == null) {
										itemsFiltered = new ArrayList<>();
										itemsMapFiltered.put(category, itemsFiltered);
									}
									itemsFiltered.add(i);
									empty = false;
								}
							}
						}
					}
					itemsMap = itemsMapFiltered;
				}
				if (!empty) {
					List<GpxDisplayItem> items = new ArrayList<>();
					List<String> categories = new ArrayList<>(itemsMap.keySet());
					Collections.sort(categories, comparator);
					if (g.getType() == GpxDisplayItemType.TRACK_POINTS) {
						itemGroups.put(g, items);
						groups.add(g);
					}
					for (String category : categories) {
						List<GpxDisplayItem> values = itemsMap.get(category);
						if (g.getType() == GpxDisplayItemType.TRACK_POINTS) {
							GpxDisplayGroup headerGroup = g.cloneInstance();
							headerGroup.setType(GpxDisplayItemType.TRACK_POINTS);
							headerGroup.setName(category);
							for (GpxDisplayItem i : values) {
								if (i.locationStart != null && i.locationStart.getColor() != 0) {
									headerGroup.setColor(i.locationStart.getColor(g.getColor()));
									break;
								}
							}
							List<GpxDisplayItem> headerGroupItems = headerGroup.getModifiableList();
							headerGroupItems.clear();
							headerGroupItems.addAll(values);
							itemGroups.put(headerGroup, values);
							groups.add(headerGroup);
						} else {
							items.addAll(values);
						}
					}
					if (items.size() > 0) {
						itemGroups.put(g, items);
						groups.add(g);
					}
				}
			}
			notifyDataSetChanged();
		}

		@Override
		public GpxDisplayItem getChild(int groupPosition, int childPosition) {
			final GpxDisplayGroup group = getGroup(groupPosition);
			int count = itemGroups.get(group).size();
			if (group.getType() == GpxDisplayItemType.TRACK_POINTS || routePointsExpanded
					|| count <= ROUTE_POINTS_LIMIT + 1 || childPosition < ROUTE_POINTS_LIMIT) {
				return itemGroups.get(groups.get(groupPosition)).get(childPosition);
			} else {
				return null;
			}
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return groupPosition * 10000 + childPosition;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			int count = itemGroups.get(groups.get(groupPosition)).size();
			final GpxDisplayGroup group = getGroup(groupPosition);
			if (group.getType() == GpxDisplayItemType.TRACK_POINTS || routePointsExpanded || count <= ROUTE_POINTS_LIMIT + 1) {
				return count;
			} else {
				return ROUTE_POINTS_LIMIT + 1;
			}
		}

		@Override
		public GpxDisplayGroup getGroup(int groupPosition) {
			return groups.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return groups.size();
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
		public View getGroupView(final int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			View row = convertView;
			final GpxDisplayGroup group = getGroup(groupPosition);
			UiUtilities iconsCache = app.getUIUtilities();
			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.wpt_list_item, parent, false);
				ImageView options = (ImageView) row.findViewById(R.id.options);
				options.setFocusable(false);
				options.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_overflow_menu_white));
			}
			ImageView icon = (ImageView) row.findViewById(R.id.icon);
			TextView groupTitle = (TextView) row.findViewById(R.id.bold_label);
			TextView title = (TextView) row.findViewById(R.id.label);
			TextViewEx button = (TextViewEx) row.findViewById(R.id.button);
			TextView description = (TextView) row.findViewById(R.id.description);
			ImageView expandImage = (ImageView) row.findViewById(R.id.expand_image);
			ImageView options = (ImageView) row.findViewById(R.id.options);

			button.setVisibility(View.GONE);

			if (groupPosition == 0 || group.getType() == GpxDisplayItemType.TRACK_ROUTE_POINTS) {
				icon.setVisibility(View.GONE);
				options.setVisibility(View.GONE);
				expandImage.setVisibility(View.GONE);
				title.setVisibility(View.GONE);
				groupTitle.setVisibility(View.VISIBLE);
				description.setVisibility(View.VISIBLE);
				row.findViewById(R.id.divider).setVisibility(View.GONE);
				row.findViewById(R.id.list_divider).setVisibility(View.GONE);
				row.setOnClickListener(null);
				if (group.getType() == GpxDisplayItemType.TRACK_POINTS) {
					groupTitle.setText(getString(R.string.shared_string_gpx_points));
					description.setText(getString(R.string.track_points_category_name));
				} else {
					groupTitle.setText(getString(R.string.route_points));
					description.setText(getString(R.string.route_points_category_name));
				}
			} else {
				icon.setVisibility(View.VISIBLE);
				boolean expanded = listView.isGroupExpanded(groupPosition);
				expandImage.setImageDrawable(iconsCache.getThemedIcon(
						expanded ? R.drawable.ic_action_arrow_up : R.drawable.ic_action_arrow_down));
				expandImage.setVisibility(View.VISIBLE);
				description.setVisibility(View.GONE);
				expandImage.setVisibility(View.VISIBLE);
				options.setVisibility(View.VISIBLE);
				row.findViewById(R.id.divider).setVisibility(View.VISIBLE);
				row.findViewById(R.id.list_divider).setVisibility(View.GONE);

				row.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (listView.isGroupExpanded(groupPosition)) {
							listView.collapseGroup(groupPosition);
						} else {
							listView.expandGroup(groupPosition);
						}
					}
				});

				title.setVisibility(View.VISIBLE);
				groupTitle.setVisibility(View.GONE);

				String categoryName = group.getName();
				if (TextUtils.isEmpty(categoryName)) {
					categoryName = getString(R.string.shared_string_waypoints);
				}
				SpannableStringBuilder text = new SpannableStringBuilder(categoryName).append(" (").append(String.valueOf(getChildrenCount(groupPosition))).append(")");
				text.setSpan(new ForegroundColorSpan(AndroidUtils.getColorFromAttr(mainView.getContext(), R.attr.wikivoyage_primary_text_color)),
						0, categoryName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				text.setSpan(new ForegroundColorSpan(ContextCompat.getColor(app, R.color.wikivoyage_secondary_text)),
						categoryName.length() + 1, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				title.setText(text);

				int groupColor = group.getColor();
				if (groupColor == 0) {
					groupColor = ContextCompat.getColor(app, R.color.gpx_color_point);
				}
				icon.setImageDrawable(app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_folder, groupColor | 0xff000000));
				options.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						FragmentActivity activity = getActivity();
						if (activity != null) {
							EditTrackGroupDialogFragment.showInstance(activity.getSupportFragmentManager(), group, null);
						}
					}
				});
			}
			row.findViewById(R.id.group_divider).setVisibility(group.getType() == GpxDisplayItemType.TRACK_ROUTE_POINTS ? View.VISIBLE : View.GONE);

			final CheckBox ch = (CheckBox) row.findViewById(R.id.toggle_item);
			if (selectionMode) {
				ch.setVisibility(View.VISIBLE);
				ch.setChecked(selectedGroups.contains(groupPosition));

				ch.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						List<GpxDisplayItem> items = itemGroups.get(group);
						if (ch.isChecked()) {
							if (groupPosition == 0 && groups.size() > 1) {
								setTrackPointsSelection(true);
							} else {
								setGroupSelection(items, groupPosition, true);
							}
						} else {
							if (groupPosition == 0) {
								setTrackPointsSelection(false);
							} else {
								setGroupSelection(items, groupPosition, false);
							}
						}
						adapter.notifyDataSetInvalidated();
						updateSelectionMode(actionMode);
					}
				});
			} else {
				ch.setVisibility(View.GONE);
			}
			return row;
		}

		private void setTrackPointsSelection(boolean select) {
			if (!groups.isEmpty()) {
				setGroupSelection(null, 0, select);
				for (int i = 1; i < groups.size(); i++) {
					GpxDisplayGroup g = groups.get(i);
					if (g.getType() == GpxDisplayItemType.TRACK_POINTS) {
						setGroupSelection(itemGroups.get(g), i, select);
					}
				}
			}
		}

		private void setGroupSelection(List<GpxDisplayItem> items, int groupPosition, boolean select) {
			GpxDisplayGroup group = groups.get(groupPosition);
			if (select) {
				selectedGroups.add(groupPosition);
				if (items != null) {
					Set<GpxDisplayItem> set = selectedItems.get(group.getType());
					if (set != null) {
						set.addAll(items);
					} else {
						set = new LinkedHashSet<>(items);
						selectedItems.put(group.getType(), set);
					}
				}
			} else {
				selectedGroups.remove(groupPosition);
				selectedItems.remove(group.getType());
			}
		}


		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
								 ViewGroup parent) {
			View row = convertView;
			UiUtilities iconsCache = getMyApplication().getUIUtilities();
			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.wpt_list_item, parent, false);
				ImageView options = (ImageView) row.findViewById(R.id.options);
				options.setFocusable(false);
				options.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_overflow_menu_white));
			}
			if (childPosition == 0) {
				row.findViewById(R.id.divider).setVisibility(View.GONE);
				row.findViewById(R.id.list_divider).setVisibility(View.GONE);
			} else {
				row.findViewById(R.id.divider).setVisibility(View.GONE);
				row.findViewById(R.id.list_divider).setVisibility(View.VISIBLE);
			}

			ImageView icon = (ImageView) row.findViewById(R.id.icon);
			TextView title = (TextView) row.findViewById(R.id.label);
			TextViewEx button = (TextViewEx) row.findViewById(R.id.button);
			TextView description = (TextView) row.findViewById(R.id.description);
			ImageView expandImage = (ImageView) row.findViewById(R.id.expand_image);
			ImageView options = (ImageView) row.findViewById(R.id.options);

			final GpxDisplayGroup group = getGroup(groupPosition);
			final GpxDisplayItem gpxItem = getChild(groupPosition, childPosition);
			final WptPt wpt = gpxItem != null ? gpxItem.locationStart : null;
			boolean isWaypoint = gpxItem != null && group.getType() == GpxDisplayItemType.TRACK_POINTS;
			if (isWaypoint) {
				int groupColor = group.getColor();
				if (wpt != null) {
					groupColor = wpt.getColor(groupColor);
				}
				if (groupColor == 0) {
					groupColor = ContextCompat.getColor(app, R.color.gpx_color_point);
				}

				title.setVisibility(View.VISIBLE);
				button.setVisibility(View.GONE);

				expandImage.setVisibility(View.GONE);
				options.setVisibility(View.GONE);
				options.setOnClickListener(null);

				title.setText(gpxItem.name);
				if (!Algorithms.isEmpty(gpxItem.description)) {
					description.setText(gpxItem.description);
					description.setVisibility(View.VISIBLE);
				} else {
					description.setVisibility(View.GONE);
				}
				icon.setImageDrawable(PointImageDrawable.getFromWpt(getActivity(), groupColor, false, wpt));

			} else {
				boolean showAll = gpxItem == null;
				title.setVisibility(showAll ? View.GONE : View.VISIBLE);
				description.setVisibility(showAll ? View.GONE : View.VISIBLE);
				button.setVisibility(!showAll ? View.GONE : View.VISIBLE);
				expandImage.setVisibility(View.GONE);
				options.setVisibility(View.GONE);

				if (showAll) {
					int count = itemGroups.get(groups.get(groupPosition)).size();
					button.setText(getString(R.string.shared_string_show_all) + " - " + count);
					icon.setImageDrawable(null);
				} else {
					title.setText(gpxItem.name);
					if (!Algorithms.isEmpty(gpxItem.description)) {
						description.setText(gpxItem.description);
						description.setVisibility(View.VISIBLE);
					} else {
						description.setVisibility(View.GONE);
					}
					icon.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_marker_dark));
				}
			}

			final CheckBox ch = (CheckBox) row.findViewById(R.id.toggle_item);
			if (selectionMode && gpxItem != null) {
				ch.setVisibility(View.VISIBLE);
				ch.setChecked(selectedItems.get(group.getType()) != null && selectedItems.get(group.getType()).contains(gpxItem));
				row.findViewById(R.id.icon).setVisibility(View.GONE);
				options.setVisibility(View.GONE);
				ch.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						if (ch.isChecked()) {
							Set<GpxDisplayItem> set = selectedItems.get(group.getType());
							if (set != null) {
								set.add(gpxItem);
							} else {
								set = new LinkedHashSet<>();
								set.add(gpxItem);
								selectedItems.put(group.getType(), set);
							}
						} else {
							Set<GpxDisplayItem> set = selectedItems.get(group.getType());
							if (set != null) {
								set.remove(gpxItem);
							}
						}
						updateSelectionMode(actionMode);
					}
				});
			} else {
				row.findViewById(R.id.icon).setVisibility(View.VISIBLE);
				ch.setVisibility(View.GONE);
			}
			return row;
		}

		@Override
		public Filter getFilter() {
			if (myFilter == null) {
				myFilter = new PointsFilter();
			}
			return myFilter;
		}

		public void setFilterResults(Set<?> values) {
			this.filter = values;
		}
	}

	public class PointsFilter extends Filter {

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults results = new FilterResults();
			if (constraint == null || constraint.length() == 0) {
				results.values = null;
				results.count = 1;
			} else {
				Set<Object> filter = new HashSet<>();
				String cs = constraint.toString().toLowerCase();
				List<GpxDisplayGroup> groups = getOriginalGroups();
				if (groups != null) {
					for (GpxDisplayGroup g : groups) {
						for (GpxDisplayItem i : g.getModifiableList()) {
							if (i.name.toLowerCase().contains(cs)) {
								filter.add(i);
							} else if (i.locationStart != null && !TextUtils.isEmpty(i.locationStart.category)
									&& i.locationStart.category.toLowerCase().contains(cs)) {
								filter.add(i.locationStart.category);
							}
						}
					}
				}
				results.values = filter;
				results.count = filter.size();
			}
			return results;
		}

		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			synchronized (adapter) {
				adapter.setFilterResults((Set<?>) results.values);
				List<GpxDisplayGroup> groups = getOriginalGroups();
				if (groups != null) {
					adapter.synchronizeGroups(groups);
				}
			}
			adapter.notifyDataSetChanged();
			expandAllGroups();
		}
	}
}