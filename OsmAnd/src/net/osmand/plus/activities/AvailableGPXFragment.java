package net.osmand.plus.activities;

import java.io.File;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.osmand.IndexConstants;
import net.osmand.access.AccessibleToast;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.util.Algorithms;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.ActionMode.Callback;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.actionbarsherlock.widget.SearchView.OnQueryTextListener;

public class AvailableGPXFragment extends OsmandExpandableListFragment {


	public static final int SEARCH_ID = -1;
	public static final int ACTION_ID = 0;
	protected static final int DELETE_ACTION_ID = 1;
	private boolean selectionMode = false;
	private List<GpxInfo> selectedItems = new ArrayList<GpxInfo>();
	private ActionMode actionMode;
	private SearchView searchView;
	private LoadGpxTask asyncLoader;
	private GpxIndexesAdapter listAdapter;
	MessageFormat formatMb = new MessageFormat("{0, number,##.#} MB", Locale.US);
	private LoadLocalIndexDescriptionTask descriptionLoader;
	private ContextMenuAdapter optionsMenuAdapter;
	private AsyncTask<GpxInfo, ?, ?> operationTask;
	private GpxSelectionHelper selectedGpxHelper;
	private SavingTrackHelper savingTrackHelper;
	private OsmandApplication app;


	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.app = (OsmandApplication) getActivity().getApplication();
		final Collator collator = Collator.getInstance();
		collator.setStrength(Collator.SECONDARY);
		asyncLoader = new LoadGpxTask();
		selectedGpxHelper = ((OsmandApplication) activity.getApplication()).getSelectedGpxHelper();
		savingTrackHelper = ((OsmandApplication) activity.getApplication()).getSavingTrackHelper();
		listAdapter = new GpxIndexesAdapter(getActivity());
		setAdapter(listAdapter);
	}

	public List<GpxInfo> getSelectedItems() {
		return selectedItems;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View vs = super.onCreateView(inflater, container, savedInstanceState);
		getExpandableListView().setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
				long packedPos = ((ExpandableListContextMenuInfo) menuInfo).packedPosition;
				int group = ExpandableListView.getPackedPositionGroup(packedPos);
				int child = ExpandableListView.getPackedPositionChild(packedPos);
				if (child >= 0 && group >= 0) {
					showContextMenu(listAdapter.getChild(group, child));
				}
			}
		});
		return vs;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (asyncLoader == null || asyncLoader.getResult() == null) {
			asyncLoader = new LoadGpxTask();
			asyncLoader.execute(getActivity());
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (operationTask != null) {
			operationTask.cancel(true);
		}
	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		MenuItem mi = createMenuItem(menu, SEARCH_ID, R.string.search_poi_filter, R.drawable.ic_action_search_light,
				R.drawable.ic_action_search_dark, MenuItem.SHOW_AS_ACTION_ALWAYS
						| MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		searchView = new com.actionbarsherlock.widget.SearchView(getActivity());
		mi.setActionView(searchView);
		searchView.setOnQueryTextListener(new OnQueryTextListener() {

			@Override
			public boolean onQueryTextSubmit(String query) {
				listAdapter.getFilter().filter(query);
				return true;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				listAdapter.getFilter().filter(newText);
				return true;
			}
		});

		optionsMenuAdapter = new ContextMenuAdapter(getActivity());
		OnContextMenuClick listener = new OnContextMenuClick() {
			@Override
			public void onContextMenuClick(final int itemId, int pos, boolean isChecked, DialogInterface dialog) {
				if (itemId == R.string.local_index_mi_reload) {
					asyncLoader = new LoadGpxTask();
					asyncLoader.execute(getActivity());
				} else if (itemId == R.string.show_gpx_route) {
					openShowOnMapMode();
				} else if (itemId == R.string.local_index_mi_delete) {
					openSelectionMode(itemId, R.drawable.ic_action_delete_dark, R.drawable.ic_action_delete_light,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									doAction(itemId);
								}
							});
				}
			}
		};
		optionsMenuAdapter.item(R.string.show_gpx_route)
				.icons(R.drawable.ic_action_map_marker_dark, R.drawable.ic_action_map_marker_light).listen(listener).reg();
		optionsMenuAdapter.item(R.string.local_index_mi_delete)
				.icons(R.drawable.ic_action_delete_dark, R.drawable.ic_action_delete_light).listen(listener).reg();
		optionsMenuAdapter.item(R.string.local_index_mi_reload)
				.icons(R.drawable.ic_action_refresh_dark, R.drawable.ic_action_refresh_light).listen(listener).reg();
		OsmandPlugin.onOptionsMenuActivity(getSherlockActivity(), this, optionsMenuAdapter);
		for (int j = 0; j < optionsMenuAdapter.length(); j++) {
			MenuItem item;
			item = menu.add(0, optionsMenuAdapter.getItemId(j), j + 1, optionsMenuAdapter.getItemName(j));
			item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM
			);
			if (optionsMenuAdapter.getImageId(j, isLightActionBar()) != 0) {
				item.setIcon(optionsMenuAdapter.getImageId(j, isLightActionBar()));
			}

		}
	}

	public void doAction(int actionResId) {
		if (actionResId == R.string.local_index_mi_delete) {
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
			if (itemId == optionsMenuAdapter.getItemId(i)) {
				optionsMenuAdapter.getClickAdapter(i).onContextMenuClick(itemId, i, false, null);
				return true;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	public void showProgressBar() {
		getSherlockActivity().setSupportProgressBarIndeterminateVisibility(true);
	}

	public void hideProgressBar() {
		if (getSherlockActivity() != null){
			getSherlockActivity().setSupportProgressBarIndeterminateVisibility(false);
		}
	}

	private void updateSelectionMode(ActionMode m) {
		if (selectedItems.size() > 0) {
			m.setTitle(selectedItems.size() + " " + app.getString(R.string.selected));
		} else {
			m.setTitle("");
		}
	}

	private void openShowOnMapMode() {
		selectionMode = true;
		selectedItems.clear();
		final Set<GpxInfo> originalSelectedItems = listAdapter.getSelectedGpx();
		selectedItems.addAll(originalSelectedItems);
		actionMode = getSherlockActivity().startActionMode(new Callback() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				selectionMode = true;
				updateSelectionMode(mode);
				MenuItem it = menu.add(R.string.show_gpx_route);
				it.setIcon(!isLightActionBar() ? R.drawable.ic_action_map_marker_dark : R.drawable.ic_action_map_marker_light);
				it.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM |
						MenuItem.SHOW_AS_ACTION_WITH_TEXT);
				return true;
			}


			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				if (selectedItems.isEmpty()) {
					return true;
				}
				runSelection(true);
				actionMode.finish();
				return true;
			}

			private void runSelection(boolean showOnMap) {
				operationTask = new SelectGpxTask(showOnMap);
				originalSelectedItems.addAll(selectedItems);
				operationTask.execute(originalSelectedItems.toArray(new GpxInfo[originalSelectedItems.size()]));
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				selectionMode = false;
				getView().findViewById(R.id.DescriptionText).setVisibility(View.GONE);
				runSelection(false);
				listAdapter.notifyDataSetChanged();
			}

		});
		listAdapter.notifyDataSetChanged();
	}


	public void openSelectionMode(final int actionResId, int darkIcon, int lightIcon,
								  final DialogInterface.OnClickListener listener) {
		final int actionIconId = !isLightActionBar() ? darkIcon : lightIcon;
		String value = app.getString(actionResId);
		if (value.endsWith("...")) {
			value = value.substring(0, value.length() - 3);
		}
		final String actionButton = value;
		if (listAdapter.getGroupCount() == 0) {
			AccessibleToast.makeText(getActivity(), app.getString(R.string.local_index_no_items_to_do, actionButton.toLowerCase()), Toast.LENGTH_SHORT).show();
			return;
		}

		selectionMode = true;
		selectedItems.clear();
		actionMode = getSherlockActivity().startActionMode(new Callback() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				selectionMode = true;
				MenuItem it = menu.add(actionResId);
				if (actionIconId != 0) {
					it.setIcon(actionIconId);
				}
				it.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM |
						MenuItem.SHOW_AS_ACTION_WITH_TEXT);
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
							app.getString(R.string.local_index_no_items_to_do, actionButton.toLowerCase()), Toast.LENGTH_SHORT).show();
					return true;
				}

				Builder builder = new AlertDialog.Builder(getActivity());
				builder.setMessage(getString(R.string.local_index_action_do, actionButton.toLowerCase(), selectedItems.size()));
				builder.setPositiveButton(actionButton, listener);
				builder.setNegativeButton(R.string.default_buttons_cancel, null);
				builder.show();
				return true;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				selectionMode = false;
				getView().findViewById(R.id.DescriptionText).setVisibility(View.GONE);
				listAdapter.notifyDataSetChanged();
			}

		});

		if (R.string.local_index_mi_upload_gpx == actionResId) {
			((TextView) getView().findViewById(R.id.DescriptionText)).setText(R.string.local_index_upload_gpx_description);
			((TextView) getView().findViewById(R.id.DescriptionText)).setVisibility(View.VISIBLE);
		}
		listAdapter.notifyDataSetChanged();
	}

	private void renameFile(GpxInfo info) {
		final File f = info.file;
		Builder b = new AlertDialog.Builder(getActivity());
		if (f.exists()) {
			final EditText editText = new EditText(getActivity());
			editText.setPadding(7, 3, 7, 3);
			editText.setText(f.getName());
			b.setView(editText);
			b.setPositiveButton(R.string.default_buttons_save, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					String newName = editText.getText().toString();
					File dest = new File(f.getParentFile(), newName);
					if (dest.exists()) {
						AccessibleToast.makeText(getActivity(), R.string.file_with_name_already_exists, Toast.LENGTH_LONG).show();
					} else {
						if (!f.getParentFile().exists()) {
							f.getParentFile().mkdirs();
						}
						if (f.renameTo(dest)) {
							asyncLoader = new LoadGpxTask();
							asyncLoader.execute(getActivity());
						} else {
							AccessibleToast.makeText(getActivity(), R.string.file_can_not_be_renamed, Toast.LENGTH_LONG).show();
						}
					}

				}
			});
			b.setNegativeButton(R.string.default_buttons_cancel, null);
			b.show();
		}
	}

	private void basicFileOperation(final GpxInfo info, ContextMenuAdapter adapter) {
		OnContextMenuClick listener = new OnContextMenuClick() {
			@Override
			public void onContextMenuClick(int resId, int pos, boolean isChecked, DialogInterface dialog) {
				if (resId == R.string.local_index_mi_rename) {
					renameFile(info);
				} else if (resId == R.string.local_index_unselect_gpx_file ||
						resId == R.string.local_index_select_gpx_file) {
					if (info.gpx == null) {
						loadGpxAsync(info, resId == R.string.local_index_select_gpx_file);
					} else {
						getMyApplication().getSelectedGpxHelper().selectGpxFile(info.gpx, resId == R.string.local_index_select_gpx_file, true);
						listAdapter.notifyDataSetChanged();
						selectedGpxHelper.runUiListeners();
					}
				} else if (resId == R.string.local_index_mi_delete) {
					Builder confirm = new AlertDialog.Builder(getActivity());
					confirm.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							new DeleteGpxTask().execute(info);
						}
					});
					confirm.setNegativeButton(R.string.default_buttons_no, null);
					confirm.setMessage(getString(R.string.delete_confirmation_msg, info.file.getName()));
					confirm.show();
				} else if (resId == R.string.local_index_mi_export) {
					final Uri fileUri = Uri.fromFile(info.file);
					final Intent sendIntent = new Intent(Intent.ACTION_SEND);
					sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
					sendIntent.setType("application/gpx+xml");
					startActivity(sendIntent);
				} else if (resId == R.string.show_gpx_route) {
					info.updateGpxInfo(getMyApplication());
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
			}
		};
		if (info.gpx != null && info.file == null) {
			GpxSelectionHelper.SelectedGpxFile selectedGpxFile = selectedGpxHelper.getSelectedCurrentRecordingTrack();
			if (selectedGpxFile != null && selectedGpxFile.getGpxFile() == info.gpx) {
				adapter.item(R.string.local_index_unselect_gpx_file).listen(listener).reg();
			} else {
				adapter.item(R.string.local_index_select_gpx_file).listen(listener).reg();
			}
		} else if (info.file != null) {
			if (getMyApplication().getSelectedGpxHelper().getSelectedFileByPath(info.file.getAbsolutePath()) == null) {
				adapter.item(R.string.local_index_select_gpx_file).listen(listener).reg();
			} else {
				adapter.item(R.string.local_index_unselect_gpx_file).listen(listener).reg();
			}
		}
		adapter.item(R.string.show_gpx_route).listen(listener).reg();
		if (info.file != null) {
			adapter.item(R.string.local_index_mi_rename).listen(listener).reg();
			adapter.item(R.string.local_index_mi_delete).listen(listener).reg();
			adapter.item(R.string.local_index_mi_export).listen(listener).reg();
		}
		OsmandPlugin.onContextMenuActivity(getSherlockActivity(), this, info, adapter);
	}

	private void showContextMenu(final GpxInfo info) {
		Builder builder = new AlertDialog.Builder(getActivity());
		final ContextMenuAdapter adapter = new ContextMenuAdapter(getActivity());
		basicFileOperation(info, adapter);

		String[] values = adapter.getItemNames();
		builder.setItems(values, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				OnContextMenuClick clk = adapter.getClickAdapter(which);
				if (clk != null) {
					clk.onContextMenuClick(adapter.getItemId(which), which, false, dialog);
				}
			}

		});
		builder.show();
	}


	public class LoadGpxTask extends AsyncTask<Activity, GpxInfo, List<GpxInfo>> {

		private List<GpxInfo> result;

		@Override
		protected List<GpxInfo> doInBackground(Activity... params) {
			List<GpxInfo> result = new ArrayList<GpxInfo>();
			if (!savingTrackHelper.getCurrentGpx().isEmpty()) {
				loadFile(new GpxInfo(savingTrackHelper.getCurrentGpx(),
						app.getString(R.string.gpx_available_current_track)));
			}
			loadGPXData(app.getAppPath(IndexConstants.GPX_INDEX_DIR), result, this);
			return result;
		}

		public void loadFile(GpxInfo... loaded) {
			publishProgress(loaded);
		}

		@Override
		protected void onPreExecute() {
			getSherlockActivity().setSupportProgressBarIndeterminateVisibility(true);
			listAdapter.clear();
		}

		@Override
		protected void onProgressUpdate(GpxInfo... values) {
			for (GpxInfo v : values) {
				listAdapter.addLocalIndexInfo(v);
			}
			listAdapter.notifyDataSetChanged();
		}

		public void setResult(List<GpxInfo> result) {
			this.result = result;
			listAdapter.clear();
			if (result != null) {
				for (GpxInfo v : result) {
					listAdapter.addLocalIndexInfo(v);
				}
				listAdapter.notifyDataSetChanged();
				onPostExecute(result);
			}
		}

		@Override
		protected void onPostExecute(List<GpxInfo> result) {
			this.result = result;
			if(getSherlockActivity() != null) {
				getSherlockActivity().setSupportProgressBarIndeterminateVisibility(false);
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
				List<GpxInfo> progress = new ArrayList<GpxInfo>();
				loadGPXFolder(mapPath, result, loadTask, progress, "");
				if (!progress.isEmpty()) {
					loadTask.loadFile(progress.toArray(new GpxInfo[progress.size()]));
				}
			}
		}

		private void loadGPXFolder(File mapPath, List<GpxInfo> result, LoadGpxTask loadTask,
								   List<GpxInfo> progress, String gpxSubfolder) {
			for (File gpxFile : listFilesSorted(mapPath)) {
				if (gpxFile.isDirectory()) {
					String sub = gpxSubfolder.length() == 0 ? gpxFile.getName() : gpxSubfolder + "/" + gpxFile.getName();
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

		Map<String, List<GpxInfo>> data = new LinkedHashMap<String, List<GpxInfo>>();
		List<String> category = new ArrayList<String>();
		int warningColor;
		int okColor;
		int defaultColor;
		int corruptedColor;
		private SearchFilter filter;


		public GpxIndexesAdapter(Context ctx) {
			warningColor = ctx.getResources().getColor(R.color.color_warning);
			okColor = ctx.getResources().getColor(R.color.color_ok);
			TypedArray ta = ctx.getTheme().obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary});
			defaultColor = ta.getColor(0, ctx.getResources().getColor(R.color.color_unknown));
			ta.recycle();
		}

		public Set<GpxInfo> getSelectedGpx() {
			Set<GpxInfo> originalSelectedItems = new HashSet<GpxInfo>();
			for (List<GpxInfo> l : data.values()) {
				if (l != null) {
					for (GpxInfo g : l) {
						boolean add = false;
						if (g.gpx != null && g.gpx.showCurrentTrack) {
							add = selectedGpxHelper.getSelectedCurrentRecordingTrack() != null;
						} else {
							add = selectedGpxHelper.getSelectedFileByName(g.getFileName()) != null;
						}
						if (add) {
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
			notifyDataSetChanged();
		}

		public void addLocalIndexInfo(GpxInfo info) {
			String catName;
			if (info.gpx != null && info.gpx.showCurrentTrack) {
				catName = info.name;
			} else {
				catName = app.getString(R.string.local_indexes_cat_gpx) + " " + info.subfolder;
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
			String cat = category.get(groupPosition);
			return data.get(cat).get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			// it would be unusable to have 10000 local indexes
			return groupPosition * 10000 + childPosition;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			View v = convertView;
			final GpxInfo child = (GpxInfo) getChild(groupPosition, childPosition);
			if (v == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				v = inflater.inflate(net.osmand.plus.R.layout.local_index_list_item, parent, false);
			}
			TextView viewName = ((TextView) v.findViewById(R.id.local_index_name));
			viewName.setText(child.getName());

			if (child.isCorrupted()) {
				viewName.setTextColor(corruptedColor);
				viewName.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
			} else if (selectedGpxHelper.getSelectedFileByName(child.getFileName()) != null) {
				viewName.setTextColor(okColor);
				viewName.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
			} else {
				viewName.setTextColor(defaultColor);
				viewName.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
			}
			if (child.getSize() >= 0) {
				String size;
				if (child.getSize() > 100) {
					size = formatMb.format(new Object[]{(float) child.getSize() / (1 << 10)});
				} else {
					size = child.getSize() + " kB";
				}
				((TextView) v.findViewById(R.id.local_index_size)).setText(size);
			} else {
				((TextView) v.findViewById(R.id.local_index_size)).setText("");
			}
			TextView descr = ((TextView) v.findViewById(R.id.local_index_descr));
			if (child.isExpanded()) {
				descr.setVisibility(View.VISIBLE);
				descr.setText(child.getHtmlDescription());
			} else {
				descr.setVisibility(View.GONE);
			}
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
			}


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
			adjustIndicator(groupPosition, isExpanded, v);
			TextView nameView = ((TextView) v.findViewById(R.id.category_name));
			List<GpxInfo> list = data.get(group);
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
			return data.get(category.get(groupPosition)).size();
		}

		@Override
		public String getGroup(int groupPosition) {
			return category.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
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
				if (Algorithms.objectEquals(getActivity().getString(R.string.local_indexes_cat_gpx) + " " + g.subfolder, cat)) {
					found = i;
					break;
				}
			}
			if (found != -1) {
				data.get(category.get(found)).remove(g);
			}
		}
	}

	public class LoadLocalIndexDescriptionTask extends AsyncTask<GpxInfo, GpxInfo, GpxInfo[]> {

		@Override
		protected GpxInfo[] doInBackground(GpxInfo... params) {
			for (GpxInfo i : params) {
				i.updateGpxInfo(getMyApplication());
			}
			return params;
		}

		@Override
		protected void onPreExecute() {
			showProgressBar();
		}

		@Override
		protected void onProgressUpdate(GpxInfo... values) {
			listAdapter.notifyDataSetChanged();
		}

		@Override
		protected void onPostExecute(GpxInfo[] result) {
			hideProgressBar();
			listAdapter.notifyDataSetChanged();

		}

	}

	public class DeleteGpxTask extends AsyncTask<GpxInfo, GpxInfo, String> {

		@Override
		protected String doInBackground(GpxInfo... params) {
			int count = 0;
			int total = 0;
			for (GpxInfo info : params) {
				if (!isCancelled() && (info.gpx == null || !info.gpx.showCurrentTrack)) {
					boolean successfull = false;
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
				listAdapter.delete(g);
			}
			listAdapter.notifyDataSetChanged();
		}

		@Override
		protected void onPreExecute() {
			getSherlockActivity().setProgressBarIndeterminateVisibility(true);
		}

		@Override
		protected void onPostExecute(String result) {
			getSherlockActivity().setProgressBarIndeterminateVisibility(false);
			AccessibleToast.makeText(getSherlockActivity(), result, Toast.LENGTH_LONG).show();
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
					info.updateGpxInfo(getMyApplication());
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
			listAdapter.notifyDataSetInvalidated();
		}

		@Override
		protected void onPreExecute() {
			getSherlockActivity().setProgressBarIndeterminateVisibility(true);
		}

		@Override
		protected void onPostExecute(String result) {
			selectedGpxHelper.runUiListeners();
			getSherlockActivity().setProgressBarIndeterminateVisibility(false);
			if (showOnMap && toShow != null) {
				getMyApplication().getSettings().setMapLocationToShow(toShow.lat, toShow.lon,
						getMyApplication().getSettings().getLastKnownMapZoom());
				MapActivity.launchMapActivityMoveToTop(getActivity());
			}
		}
	}

	private void loadGpxAsync(GpxInfo info, boolean isSelected){
		final boolean selected = isSelected;
		new AsyncTask<GpxInfo, Void, Void>() {
			GpxInfo info;

			@Override
			protected Void doInBackground(GpxInfo... params) {
				if (params == null){
					return null;
				}
				info = params[0];
				params[0].updateGpxInfo(getMyApplication());
				return null;
			}


			@Override
			protected void onProgressUpdate(Void... values) {
			}

			@Override
			protected void onPreExecute() {
				getSherlockActivity().setProgressBarIndeterminateVisibility(true);
			}

			@Override
			protected void onPostExecute(Void result) {
				if (getSherlockActivity() != null){
					getSherlockActivity().setProgressBarIndeterminateVisibility(false);
				}
				if (info.gpx != null){
					getMyApplication().getSelectedGpxHelper().selectGpxFile(info.gpx, selected, true);
					listAdapter.notifyDataSetChanged();
					selectedGpxHelper.runUiListeners();
				}
			}
		}.execute(info);
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
				String cs = constraint.toString().toLowerCase();
				List<GpxInfo> res = new ArrayList<GpxInfo>();
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
				synchronized (listAdapter) {
					listAdapter.clear();
					for (GpxInfo i : ((List<GpxInfo>) results.values)) {
						listAdapter.addLocalIndexInfo(i);
					}
				}
				listAdapter.notifyDataSetChanged();
				if (constraint != null && constraint.length() > 3) {
					collapseTrees(10);
				}
			}
		}

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (descriptionLoader != null) {
			descriptionLoader.cancel(true);
		}
		if (asyncLoader != null) {
			asyncLoader.cancel(true);
		}
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		GpxInfo item = listAdapter.getChild(groupPosition, childPosition);
		if (!selectionMode) {
			item.setExpanded(!item.isExpanded());
			if (item.isExpanded()) {
				descriptionLoader = new LoadLocalIndexDescriptionTask();
				descriptionLoader.execute(item);
			}
		} else {
			if (!selectedItems.contains(item)) {
				selectedItems.add(item);
			} else {
				selectedItems.remove(item);
			}
			updateSelectionMode(actionMode);
		}
		listAdapter.notifyDataSetInvalidated();
		return true;
	}


	public static class GpxInfo {
		public GPXFile gpx;
		public File file;
		public String subfolder;

		private String name = null;
		private int sz = -1;
		private String fileName = null;
		private String description;
		private boolean corrupted;
		private boolean expanded;
		private Spanned htmlDescription;

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
				sz = (int) (file.length() >> 10);
			}
			return sz;
		}

		public boolean isExpanded() {
			return expanded;
		}

		public void setExpanded(boolean expanded) {
			this.expanded = expanded;
		}

		public CharSequence getDescription() {
			if (description == null) {
				return "";
			}
			return description;
		}

		public Spanned getHtmlDescription() {
			if (htmlDescription != null) {
				return htmlDescription;
			}
			htmlDescription = Html.fromHtml(getDescription().toString().replace("\n", "<br/>"));
			return htmlDescription;
		}


		public void setGpx(GPXFile gpx) {
			this.gpx = gpx;
		}

		public void updateGpxInfo(OsmandApplication app) {
			if (gpx == null) {
				gpx = GPXUtilities.loadGPXFile(app, file);
			}
			if (gpx.warning != null) {
				corrupted = true;
				description = gpx.warning;
			} else {
				// 'Long-press for options' message
				description = GpxUiHelper.getDescription(app, gpx, file, true) +
						app.getString(R.string.local_index_gpx_info_show);
			}
			htmlDescription = null;
			getHtmlDescription();
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
}
