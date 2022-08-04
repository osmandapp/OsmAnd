package net.osmand.plus.download.ui;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.FragmentActivity;

import net.osmand.Collator;
import net.osmand.IndexConstants;
import net.osmand.OsmAndCollator;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.OsmandBaseExpandableListAdapter;
import net.osmand.plus.base.OsmandExpandableListFragment;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.download.LocalIndexHelper;
import net.osmand.plus.download.LocalIndexHelper.LocalIndexType;
import net.osmand.plus.download.LocalIndexInfo;
import net.osmand.plus.download.SrtmDownloadItem;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.mapsource.EditMapSourceDialogFragment.OnMapSourceUpdateListener;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.resources.IncrementalChangesManager;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.FileUtils.RenameCallback;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class LocalIndexesFragment extends OsmandExpandableListFragment implements DownloadEvents,
		OnMapSourceUpdateListener, RenameCallback {

	private LoadLocalIndexTask asyncLoader;
	private final Map<String, IndexItem> filesToUpdate = new HashMap<>();
	private LocalIndexesAdapter listAdapter;
	private AsyncTask<LocalIndexInfo, ?, ?> operationTask;

	private boolean selectionMode;
	private final Set<LocalIndexInfo> selectedItems = new LinkedHashSet<>();

	private ContextMenuAdapter optionsMenuAdapter;
	private ActionMode actionMode;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.local_index, container, false);

		getDownloadActivity().setSupportProgressBarIndeterminateVisibility(false);
		getDownloadActivity().getAccessibilityAssistant().registerPage(view, DownloadActivity.LOCAL_TAB_NUMBER);

		ExpandableListView listView = view.findViewById(android.R.id.list);
		listAdapter = new LocalIndexesAdapter(getDownloadActivity());
		listView.setAdapter(listAdapter);
		expandAllGroups();
		setListView(listView);
		return view;
	}

	@SuppressWarnings({"unchecked", "deprecation"})
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (asyncLoader == null || asyncLoader.getResult() == null) {
			// getLastNonConfigurationInstance method should be in onCreate() method
			// (onResume() doesn't work)
			Object indexes = getActivity().getLastNonConfigurationInstance();
			if (indexes instanceof List<?>) {
				asyncLoader = new LoadLocalIndexTask();
				asyncLoader.setResult((List<LocalIndexInfo>) indexes);
			}
		}
		setHasOptionsMenu(true);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (asyncLoader == null || asyncLoader.getResult() == null) {
			reloadData();
		}
	}

	public void reloadData() {
		List<IndexItem> itemsToUpdate = getDownloadActivity().getDownloadThread().getIndexes().getItemsToUpdate();
		filesToUpdate.clear();
		for (IndexItem ii : itemsToUpdate) {
			filesToUpdate.put(ii.getTargetFileName(), ii);
		}
		LoadLocalIndexTask current = asyncLoader;
		if (current == null || current.getStatus() == AsyncTask.Status.FINISHED ||
				current.isCancelled() || current.getResult() != null) {
			asyncLoader = new LoadLocalIndexTask();
			asyncLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}

	private boolean performBasicOperation(int resId, LocalIndexInfo info) {
		if (resId == R.string.shared_string_rename) {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				File file = new File(info.getPathToData());
				FileUtils.renameFile(activity, file, this, false);
			}
		} else if (resId == R.string.clear_tile_data) {
			AlertDialog.Builder confirm = new AlertDialog.Builder(getActivity());
			confirm.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					new LocalIndexOperationTask(getDownloadActivity(), listAdapter, LocalIndexOperationTask.CLEAR_TILES_OPERATION).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, info);
				}
			});
			confirm.setNegativeButton(R.string.shared_string_no, null);
			String fn = FileNameTranslationHelper.getFileName(getActivity(),
					getMyApplication().getResourceManager().getOsmandRegions(),
					info.getFileName());
			confirm.setMessage(getString(R.string.clear_confirmation_msg, fn));
			confirm.show();
		} else if (resId == R.string.shared_string_edit) {
			OsmandRasterMapsPlugin.defineNewEditLayer(getDownloadActivity().getSupportFragmentManager(), this, info.getFileName());
		} else if (resId == R.string.local_index_mi_restore) {
			new LocalIndexOperationTask(getDownloadActivity(), listAdapter, LocalIndexOperationTask.RESTORE_OPERATION).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, info);
		} else if (resId == R.string.shared_string_delete) {
			AlertDialog.Builder confirm = new AlertDialog.Builder(getActivity());
			confirm.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					new LocalIndexOperationTask(getDownloadActivity(), listAdapter, LocalIndexOperationTask.DELETE_OPERATION).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, info);
				}
			});
			confirm.setNegativeButton(R.string.shared_string_no, null);
			String fn = FileNameTranslationHelper.getFileName(getActivity(),
					getMyApplication().getResourceManager().getOsmandRegions(),
					info.getFileName());
			confirm.setMessage(getString(R.string.delete_confirmation_msg, fn));
			confirm.show();
		} else if (resId == R.string.local_index_mi_backup) {
			new LocalIndexOperationTask(getDownloadActivity(), listAdapter, LocalIndexOperationTask.BACKUP_OPERATION).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, info);
		}
		return true;
	}

	@Override
	public void onMapSourceUpdated() {
		reloadLocalIndexes();
	}

	@Override
	public void renamedTo(File file) {
		reloadLocalIndexes();
	}

	private void reloadLocalIndexes() {
		DownloadActivity activity = getDownloadActivity();
		if (activity != null) {
			activity.reloadLocalIndexes();
		}
	}

	public class LoadLocalIndexTask extends AsyncTask<Void, LocalIndexInfo, List<LocalIndexInfo>>
			implements AbstractLoadLocalIndexTask {

		private boolean readFiles = true;
		private List<LocalIndexInfo> result;

		public LoadLocalIndexTask() {
		}

		public LoadLocalIndexTask(boolean readFiles) {
			this.readFiles = readFiles;
		}

		@Override
		protected List<LocalIndexInfo> doInBackground(Void... params) {
			LocalIndexHelper helper = new LocalIndexHelper(getMyApplication());
			return helper.getLocalIndexData(readFiles, true, this);
		}

		@Override
		public void loadFile(LocalIndexInfo... loaded) {
			if (!isCancelled()) {
				publishProgress(loaded);
			}
		}

		@Override
		protected void onPreExecute() {
			getDownloadActivity().setSupportProgressBarIndeterminateVisibility(true);
			listAdapter.clear();
		}

		@Override
		protected void onProgressUpdate(LocalIndexInfo... values) {
			boolean isDevPluginEnabled = OsmandPlugin.isEnabled(OsmandDevelopmentPlugin.class);
			for (LocalIndexInfo v : values) {
				if (v.getOriginalType() != LocalIndexType.TTS_VOICE_DATA || isDevPluginEnabled) {
					listAdapter.addLocalIndexInfo(v);
				}
			}
			listAdapter.notifyDataSetChanged();
			expandAllGroups();
		}

		public void setResult(List<LocalIndexInfo> result) {
			this.result = result;
			listAdapter.clear();
			if (result != null) {
				for (LocalIndexInfo v : result) {
					listAdapter.addLocalIndexInfo(v);
				}
				listAdapter.notifyDataSetChanged();
				expandAllGroups();
				onPostExecute(result);
			}
		}

		@Override
		protected void onPostExecute(List<LocalIndexInfo> result) {
			this.result = result;
			listAdapter.sortData();
			if (getDownloadActivity() != null) {
				getDownloadActivity().setSupportProgressBarIndeterminateVisibility(false);
				getDownloadActivity().setLocalIndexInfos(result);
			}
		}

		public List<LocalIndexInfo> getResult() {
			return result;
		}

	}


	public static class LocalIndexOperationTask extends AsyncTask<LocalIndexInfo, LocalIndexInfo, String> {
		protected static int DELETE_OPERATION = 1;
		protected static int BACKUP_OPERATION = 2;
		protected static int RESTORE_OPERATION = 3;
		protected static int CLEAR_TILES_OPERATION = 4;

		private final int operation;
		private final DownloadActivity a;
		private final LocalIndexesAdapter listAdapter;

		public LocalIndexOperationTask(DownloadActivity a, LocalIndexesAdapter listAdapter, int operation) {
			this.a = a;
			this.listAdapter = listAdapter;
			this.operation = operation;
		}

		private boolean move(File from, File to) {
			if (!to.getParentFile().exists()) {
				to.getParentFile().mkdirs();
			}
			return from.renameTo(to);
		}

		private File getFileToBackup(LocalIndexInfo i) {
			if (!i.isBackupedData()) {
				return new File(getMyApplication().getAppPath(IndexConstants.BACKUP_INDEX_DIR), i.getFileName());
			}
			return new File(i.getPathToData());
		}

		private OsmandApplication getMyApplication() {
			return (OsmandApplication) a.getApplication();
		}


		private File getFileToRestore(LocalIndexInfo i) {
			if (i.isBackupedData()) {
				File parent = new File(i.getPathToData()).getParentFile();
				if (i.getOriginalType() == LocalIndexType.MAP_DATA) {
					if (i.getFileName().endsWith(IndexConstants.BINARY_ROAD_MAP_INDEX_EXT)) {
						parent = getMyApplication().getAppPath(IndexConstants.ROADS_INDEX_DIR);
					} else {
						parent = getMyApplication().getAppPath(IndexConstants.MAPS_PATH);
					}
				} else if (i.getOriginalType() == LocalIndexType.TILES_DATA) {
					parent = getMyApplication().getAppPath(IndexConstants.TILES_INDEX_DIR);
				} else if (i.getOriginalType() == LocalIndexType.SRTM_DATA) {
					parent = getMyApplication().getAppPath(IndexConstants.SRTM_INDEX_DIR);
				} else if (i.getOriginalType() == LocalIndexType.WIKI_DATA) {
					parent = getMyApplication().getAppPath(IndexConstants.WIKI_INDEX_DIR);
				} else if (i.getOriginalType() == LocalIndexType.TRAVEL_DATA) {
					parent = getMyApplication().getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR);
				} else if (i.getOriginalType() == LocalIndexType.TTS_VOICE_DATA) {
					parent = getMyApplication().getAppPath(IndexConstants.VOICE_INDEX_DIR);
				} else if (i.getOriginalType() == LocalIndexType.VOICE_DATA) {
					parent = getMyApplication().getAppPath(IndexConstants.VOICE_INDEX_DIR);
				} else if (i.getOriginalType() == LocalIndexType.FONT_DATA) {
					parent = getMyApplication().getAppPath(IndexConstants.FONT_INDEX_DIR);
				}
				return new File(parent, i.getFileName());
			}
			return new File(i.getPathToData());
		}

		@Override
		protected String doInBackground(LocalIndexInfo... params) {
			int count = 0;
			int total = 0;
			for (LocalIndexInfo info : params) {
				if (!isCancelled()) {
					boolean successfull = false;
					if (operation == DELETE_OPERATION) {
						File f = new File(info.getPathToData());
						successfull = Algorithms.removeAllFiles(f);

						if (InAppPurchaseHelper.isSubscribedToLiveUpdates(getMyApplication())) {
							String fileNameWithoutExtension =
									Algorithms.getFileNameWithoutExtension(f);
							IncrementalChangesManager changesManager =
									getMyApplication().getResourceManager().getChangesManager();
							changesManager.deleteUpdates(fileNameWithoutExtension);
						}
						if (successfull) {
							getMyApplication().getResourceManager().closeFile(info.getFileName());
							File tShm = new File(f.getParentFile(), f.getName() + "-shm");
							File tWal = new File(f.getParentFile(), f.getName() + "-wal");
							if (tShm.exists()) {
								Algorithms.removeAllFiles(tShm);
							}
							if (tWal.exists()) {
								Algorithms.removeAllFiles(tWal);
							}
							clearMapillaryTiles(info);
						}
					} else if (operation == RESTORE_OPERATION) {
						successfull = move(new File(info.getPathToData()), getFileToRestore(info));
						if (successfull) {
							info.setBackupedData(false);
						}
					} else if (operation == BACKUP_OPERATION) {
						successfull = move(new File(info.getPathToData()), getFileToBackup(info));
						if (successfull) {
							info.setBackupedData(true);
							getMyApplication().getResourceManager().closeFile(info.getFileName());
						}
					} else if (operation == CLEAR_TILES_OPERATION) {
						ITileSource src = (ITileSource) info.getAttachedObject();
						if (src != null) {
							src.deleteTiles(info.getPathToData());
							clearMapillaryTiles(info);
						}
					}
					total++;
					if (successfull) {
						count++;
						publishProgress(info);
					}
				}
			}
			if (operation == DELETE_OPERATION) {
				a.getDownloadThread().updateLoadedFiles();
			}
			if (operation == DELETE_OPERATION) {
				return a.getString(R.string.local_index_items_deleted, count, total);
			} else if (operation == BACKUP_OPERATION) {
				return a.getString(R.string.local_index_items_backuped, count, total);
			} else if (operation == RESTORE_OPERATION) {
				return a.getString(R.string.local_index_items_restored, count, total);
			}

			return "";
		}


		@Override
		protected void onProgressUpdate(LocalIndexInfo... values) {
			if (listAdapter != null) {
				if (operation == DELETE_OPERATION) {
					listAdapter.delete(values);
				} else if (operation == BACKUP_OPERATION) {
					listAdapter.move(values, false);
				} else if (operation == RESTORE_OPERATION) {
					listAdapter.move(values, true);
				}
			}

		}

		@Override
		protected void onPreExecute() {
			a.setProgressBarIndeterminateVisibility(true);
		}

		@Override
		protected void onPostExecute(String result) {
			a.setProgressBarIndeterminateVisibility(false);
			if (result != null && result.length() > 0) {
				Toast.makeText(a, result, Toast.LENGTH_LONG).show();
			}

			if (operation == RESTORE_OPERATION || operation == BACKUP_OPERATION || operation == CLEAR_TILES_OPERATION) {
				a.reloadLocalIndexes();
			} else {
				a.onUpdatedIndexesList();
			}
		}

		// Clear tiles for both Mapillary sources together
		private void clearMapillaryTiles(LocalIndexInfo info) {
			ITileSource src = (ITileSource) info.getAttachedObject();
			ITileSource mapilaryCache = TileSourceManager.getMapillaryCacheSource();
			ITileSource mapilaryVector = TileSourceManager.getMapillaryVectorSource();
			if (mapilaryVector.getName().equals(src.getName()) || mapilaryCache.getName().equals(src.getName())) {
				File current = new File(info.getPathToData());
				File parent = current.getParentFile();
				if (parent == null) {
					return;
				}
				File[] list = parent.listFiles();
				if (list == null) {
					return;
				}
				for (File f : list) {
					String withoutExt = Algorithms.getFileNameWithoutExtension(f);
					String sqliteExt = IndexConstants.SQLITE_EXT.replace(".", "");
					ITileSource cache = null;
					if (withoutExt.equals(mapilaryCache.getName())) {
						cache = mapilaryCache;
					} else if (withoutExt.equals(mapilaryVector.getName())) {
						cache = mapilaryVector;
					}
					if (cache != null) {
						if (f.isDirectory()) {
							cache.deleteTiles(f.getPath());
						} else if (Algorithms.getFileExtension(f).equals(sqliteExt)) {
							SQLiteTileSource sqlTileSource = new SQLiteTileSource(getMyApplication(), f, TileSourceManager.getKnownSourceTemplates());
							sqlTileSource.deleteTiles(f.getPath());
						}
					}
				}
			}
		}
	}

	@Override
	public void onUpdatedIndexesList() {
		reloadData();
	}

	@Override
	public void downloadHasFinished() {
		reloadData();
	}

	@Override
	public void downloadInProgress() {
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		LocalIndexInfo child = listAdapter.getChild(groupPosition, childPosition);
		if (!selectionMode) {
			openPopUpMenu(v, child);
			return true;
		}
		selectedItems.add(child);
		listAdapter.notifyDataSetChanged();
		return true;
	}

	public Set<LocalIndexInfo> getSelectedItems() {
		return selectedItems;
	}


	@Override
	public void onPause() {
		super.onPause();
		if (operationTask != null) {
			operationTask.cancel(true);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (asyncLoader != null && asyncLoader.getStatus() == AsyncTask.Status.RUNNING) {
			asyncLoader.cancel(true);
		}
	}


	@SuppressWarnings("deprecation")
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if (!this.isAdded()) {
			return;
		}

		//fixes issue when local files not shown after switching tabs
		//Next line throws NPE in some circumstances when called from dashboard and listAdpater=null is not checked for. (Checking !this.isAdded above is not sufficient!)
		if (listAdapter != null && listAdapter.getGroupCount() == 0 && getDownloadActivity().getLocalIndexInfos().size() > 0) {
			for (LocalIndexInfo info : getDownloadActivity().getLocalIndexInfos()) {
				listAdapter.addLocalIndexInfo(info);
			}
			listAdapter.sortData();
			getExpandableListView().setAdapter(listAdapter);
			expandAllGroups();
		}
		ActionBar actionBar = getDownloadActivity().getSupportActionBar();
		//hide action bar from downloadindexfragment
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		boolean nightMode = !getMyApplication().getSettings().isLightContent();
		UiUtilities iconsCache = getMyApplication().getUIUtilities();
		int iconColorResId = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);
		optionsMenuAdapter = new ContextMenuAdapter(requireMyApplication());
		ItemClickListener listener = (uiAdapter, view, item, isChecked) -> {
			localOptionsMenu(item.getTitleId());
			return true;
		};
		optionsMenuAdapter.addItem(new ContextMenuItem(null)
				.setTitleId(R.string.shared_string_refresh, getContext())
				.setIcon(R.drawable.ic_action_refresh_dark)
				.setListener(listener)
				.setColor(getContext(), iconColorResId));
		optionsMenuAdapter.addItem(new ContextMenuItem(null)
				.setTitleId(R.string.shared_string_delete, getContext())
				.setIcon(R.drawable.ic_action_delete_dark)
				.setListener(listener)
				.setColor(getContext(), iconColorResId));
		optionsMenuAdapter.addItem(new ContextMenuItem(null)
				.setTitleId(R.string.local_index_mi_backup, getContext())
				.setListener(listener));
		optionsMenuAdapter.addItem(new ContextMenuItem(null)
				.setTitleId(R.string.local_index_mi_restore, getContext())
				.setListener(listener));
		// doesn't work correctly
		//int max =  getResources().getInteger(R.integer.abs__max_action_buttons);
		int max = 3;
		SubMenu split = null;
		for (int j = 0; j < optionsMenuAdapter.length(); j++) {
			MenuItem item;
			ContextMenuItem contextMenuItem = optionsMenuAdapter.getItem(j);
			if (j + 1 >= max && optionsMenuAdapter.length() > max) {
				if (split == null) {
					Drawable icOverflowMenu = iconsCache.getIcon(R.drawable.ic_overflow_menu_white, iconColorResId);
					split = menu.addSubMenu(0, 1, j + 1, R.string.shared_string_more_actions);
					split.setIcon(icOverflowMenu);
					split.getItem();
					MenuItemCompat.setShowAsAction(split.getItem(), MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
				}
				item = split.add(0, contextMenuItem.getTitleId(), j + 1, contextMenuItem.getTitle());
				MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
			} else {
				item = menu.add(0, contextMenuItem.getTitleId(), j + 1, contextMenuItem.getTitle());
				MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
			}
			if (contextMenuItem.getIcon() != -1) {
				Drawable icMenuItem = getMyApplication().getUIUtilities().getPaintedIcon(contextMenuItem.getIcon(), contextMenuItem.getColor());
				item.setIcon(icMenuItem);
			}

		}

		menu.setGroupVisible(0, operationTask == null || operationTask.getStatus() == AsyncTask.Status.FINISHED);
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (optionsMenuAdapter != null) {
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
		}
		return super.onOptionsItemSelected(item);
	}

	public void doAction(int actionResId) {
		if (actionResId == R.string.local_index_mi_backup) {
			operationTask = new LocalIndexOperationTask(getDownloadActivity(), listAdapter, LocalIndexOperationTask.BACKUP_OPERATION);
		} else if (actionResId == R.string.shared_string_delete) {
			operationTask = new LocalIndexOperationTask(getDownloadActivity(), listAdapter, LocalIndexOperationTask.DELETE_OPERATION);
		} else if (actionResId == R.string.local_index_mi_restore) {
			operationTask = new LocalIndexOperationTask(getDownloadActivity(), listAdapter, LocalIndexOperationTask.RESTORE_OPERATION);
		} else {
			operationTask = null;
		}
		if (operationTask != null) {
			operationTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, selectedItems.toArray(new LocalIndexInfo[0]));
		}
		if (actionMode != null) {
			actionMode.finish();
		}
	}


	private void expandAllGroups() {
		for (int i = 0; i < listAdapter.getGroupCount(); i++) {
			getExpandableListView().expandGroup(i);
		}
	}

	private void openSelectionMode(int actionResId, int actionIconId,
	                               DialogInterface.OnClickListener listener) {
		DownloadActivity downloadActivity = getDownloadActivity();
		if (downloadActivity == null) {
			return;
		}

		OsmandApplication app = requireMyApplication();
		int colorResId = ColorUtilities.getActiveButtonsAndLinksTextColorId(isNightMode(false));
		String value = getString(actionResId);
		if (value.endsWith("...")) {
			value = value.substring(0, value.length() - 3);
		}
		String actionButton = value;
		if (listAdapter.getGroupCount() == 0) {
			listAdapter.cancelFilter();
			expandAllGroups();
			listAdapter.notifyDataSetChanged();
			showNoItemsForActionsToast(actionButton);
			return;
		}

		expandAllGroups();

		selectionMode = true;
		selectedItems.clear();
		actionMode = downloadActivity.startSupportActionMode(new ActionMode.Callback() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				selectionMode = true;
				MenuItem it = menu.add(actionResId);
				if (actionIconId != 0) {
					Drawable icon = app.getUIUtilities().getIcon(actionIconId, colorResId);
					it.setIcon(icon);
				}
				it.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
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
					showNoItemsForActionsToast(actionButton);
					return true;
				}

				AlertDialog.Builder builder = new AlertDialog.Builder(getDownloadActivity());
				builder.setMessage(getString(R.string.local_index_action_do, actionButton.toLowerCase(), String.valueOf(selectedItems.size())));
				builder.setPositiveButton(actionButton, listener);
				builder.setNegativeButton(R.string.shared_string_cancel, null);
				builder.show();
				return true;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				selectionMode = false;
				listAdapter.cancelFilter();
				expandAllGroups();
				listAdapter.notifyDataSetChanged();
			}

		});
		listAdapter.notifyDataSetChanged();
	}

	private void showNoItemsForActionsToast(@NonNull String action) {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			String message = getString(R.string.local_index_no_items_to_do, action.toLowerCase());
			app.showShortToastMessage(Algorithms.capitalizeFirstLetter(message));
		}
	}

	public void localOptionsMenu(int itemId) {
		if (itemId == R.string.shared_string_refresh) {
			reloadLocalIndexes();
		} else if (itemId == R.string.shared_string_delete) {
			openSelectionMode(itemId, R.drawable.ic_action_delete_dark,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							doAction(itemId);
						}
					}, null);
		} else if (itemId == R.string.local_index_mi_backup) {
			openSelectionMode(itemId, R.drawable.ic_type_archive,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							doAction(itemId);
						}
					}, EnumSet.of(LocalIndexType.MAP_DATA, LocalIndexType.WIKI_DATA, LocalIndexType.SRTM_DATA));
		} else if (itemId == R.string.local_index_mi_restore) {
			openSelectionMode(itemId, R.drawable.ic_type_archive,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							doAction(itemId);
						}
					}, EnumSet.of(LocalIndexType.DEACTIVATED));
		}
	}

	public void openSelectionMode(int stringRes, int darkIcon, DialogInterface.OnClickListener listener,
	                              EnumSet<LocalIndexType> filter) {
		if (filter != null) {
			listAdapter.filterCategories(filter);
		}
		openSelectionMode(stringRes, darkIcon, listener);
	}


	protected class LocalIndexesAdapter extends OsmandBaseExpandableListAdapter {

		Map<LocalIndexInfo, List<LocalIndexInfo>> data = new LinkedHashMap<>();
		List<LocalIndexInfo> category = new ArrayList<>();
		List<LocalIndexInfo> filterCategory;
		int warningColor;
		int okColor;
		int corruptedColor;
		DownloadActivity ctx;

		public LocalIndexesAdapter(DownloadActivity ctx) {
			this.ctx = ctx;
			warningColor = ContextCompat.getColor(ctx, R.color.color_warning);
			boolean light = ctx.getMyApplication().getSettings().isLightContent();
			okColor = ColorUtilities.getPrimaryTextColor(ctx, !light);
			corruptedColor = ContextCompat.getColor(ctx, R.color.color_invalid);
		}

		public void clear() {
			data.clear();
			category.clear();
			filterCategory = null;
			notifyDataSetChanged();
		}

		public void sortData() {
			Collator cl = OsmAndCollator.primaryCollator();
			for (List<LocalIndexInfo> i : data.values()) {
				Collections.sort(i, new Comparator<LocalIndexInfo>() {
					@Override
					public int compare(LocalIndexInfo lhs, LocalIndexInfo rhs) {
						return cl.compare(getNameToDisplay(lhs), getNameToDisplay(rhs));
					}
				});
			}
		}

		public LocalIndexInfo findCategory(LocalIndexInfo val, boolean backuped) {
			for (LocalIndexInfo i : category) {
				if (i.isBackupedData() == backuped && val.getType() == i.getType() &&
						Algorithms.objectEquals(i.getSubfolder(), val.getSubfolder())) {
					return i;
				}
			}
			LocalIndexInfo newCat = new LocalIndexInfo(val.getType(), backuped, val.getSubfolder());
			category.add(newCat);
			data.put(newCat, new ArrayList<LocalIndexInfo>());
			return newCat;
		}

		public void delete(LocalIndexInfo[] values) {
			for (LocalIndexInfo i : values) {
				LocalIndexInfo c = findCategory(i, i.isBackupedData());
				if (c != null) {
					data.get(c).remove(i);
					if (data.get(c).size() == 0) {
						data.remove(c);
						category.remove(c);
					}
				}
			}
			notifyDataSetChanged();
		}

		public void move(LocalIndexInfo[] values, boolean oldBackupState) {
			for (LocalIndexInfo i : values) {
				LocalIndexInfo c = findCategory(i, oldBackupState);
				if (c != null) {
					data.get(c).remove(i);
				}
				c = findCategory(i, !oldBackupState);
				if (c != null) {
					data.get(c).add(i);
				}
			}
			notifyDataSetChanged();
			expandAllGroups();
		}

		public void cancelFilter() {
			filterCategory = null;
			notifyDataSetChanged();
		}

		public void filterCategories(EnumSet<LocalIndexType> types) {
			List<LocalIndexInfo> filter = new ArrayList<>();
			List<LocalIndexInfo> source = filterCategory == null ? category : filterCategory;
			for (LocalIndexInfo info : source) {
				if (types.contains(info.getType())) {
					filter.add(info);
				}
			}
			filterCategory = filter;
			notifyDataSetChanged();
		}

		public void filterCategories(boolean backup) {
			List<LocalIndexInfo> filter = new ArrayList<>();
			List<LocalIndexInfo> source = filterCategory == null ? category : filterCategory;
			for (LocalIndexInfo info : source) {
				if (info.isBackupedData() == backup) {
					filter.add(info);
				}
			}
			filterCategory = filter;
			notifyDataSetChanged();
		}

		public void addLocalIndexInfo(LocalIndexInfo info) {
			int found = -1;
			// search from end
			for (int i = category.size() - 1; i >= 0; i--) {
				LocalIndexInfo cat = category.get(i);
				if (cat.getType() == info.getType() && info.isBackupedData() == cat.isBackupedData() &&
						Algorithms.objectEquals(info.getSubfolder(), cat.getSubfolder())) {
					found = i;
					break;
				}
			}
			if (found == -1) {
				found = category.size();
				category.add(new LocalIndexInfo(info.getType(), info.isBackupedData(), info.getSubfolder()));
			}
			if (!data.containsKey(category.get(found))) {
				data.put(category.get(found), new ArrayList<LocalIndexInfo>());
			}
			data.get(category.get(found)).add(info);
		}

		@Override
		public LocalIndexInfo getChild(int groupPosition, int childPosition) {
			LocalIndexInfo cat = filterCategory != null ? filterCategory.get(groupPosition) : category.get(groupPosition);
			return data.get(cat).get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			// it would be unusable to have 10000 local indexes
			return groupPosition * 10000 + childPosition;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition,
		                         boolean isLastChild, View convertView, ViewGroup parent) {
			LocalIndexInfoViewHolder viewHolder;
			if (convertView == null) {
				LayoutInflater inflater = LayoutInflater.from(ctx);
				convertView = inflater.inflate(R.layout.local_index_list_item, parent, false);
				viewHolder = new LocalIndexInfoViewHolder(convertView);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (LocalIndexInfoViewHolder) convertView.getTag();
			}
			viewHolder.bindLocalIndexInfo(getChild(groupPosition, childPosition));
			return convertView;
		}


		private String getNameToDisplay(LocalIndexInfo child) {
			return child.getType() == LocalIndexType.VOICE_DATA
					? FileNameTranslationHelper.getVoiceName(ctx, child.getFileName())
					: FileNameTranslationHelper.getFileName(ctx,
					ctx.getMyApplication().getResourceManager().getOsmandRegions(),
					child.getFileName());
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			View v = convertView;
			LocalIndexInfo group = getGroup(groupPosition);
			if (v == null) {
				LayoutInflater inflater = LayoutInflater.from(ctx);
				v = inflater.inflate(R.layout.download_item_list_section, parent, false);
			}
			StringBuilder name = new StringBuilder(group.getType().getHumanString(ctx));
			if (group.getSubfolder() != null) {
				name.append(" ").append(group.getSubfolder());
			}
			TextView nameView = v.findViewById(R.id.title);
			TextView sizeView = v.findViewById(R.id.section_description);
			List<LocalIndexInfo> list = data.get(group);
			int size = 0;
			for (LocalIndexInfo aList : list) {
				int sz = aList.getSize();
				if (sz < 0) {
					size = 0;
					break;
				} else {
					size += sz;
				}
			}
			String sz = "";
			if (size > 0) {
				sz = AndroidUtils.formatSize(v.getContext(), size * 1024l);
			}
			sizeView.setText(sz);
			sizeView.setVisibility(View.VISIBLE);
			nameView.setText(name.toString());

			v.setOnClickListener(null);

			TypedValue typedValue = new TypedValue();
			Resources.Theme theme = ctx.getTheme();
			theme.resolveAttribute(R.attr.activity_background_color, typedValue, true);
			v.setBackgroundColor(typedValue.data);
			return v;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			LocalIndexInfo cat = filterCategory != null ? filterCategory.get(groupPosition) : category.get(groupPosition);
			return data.get(cat).size();
		}

		@Override
		public LocalIndexInfo getGroup(int groupPosition) {
			return filterCategory == null ? category.get(groupPosition) : filterCategory.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return filterCategory == null ? category.size() : filterCategory.size();
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


		private String getMapDescription(LocalIndexInfo child) {
			if (child.getType() == LocalIndexType.TILES_DATA) {
				return ctx.getString(R.string.online_map);
			} else if (child.getFileName().endsWith(IndexConstants.BINARY_ROAD_MAP_INDEX_EXT)) {
				return ctx.getString(R.string.download_roads_only_item);
			} else if (child.isBackupedData() && child.getFileName().endsWith(IndexConstants.BINARY_WIKI_MAP_INDEX_EXT)) {
				return ctx.getString(R.string.download_wikipedia_maps);
			} else if (child.isBackupedData() && (SrtmDownloadItem.isSrtmFile(child.getFileName()))) {
				return ctx.getString(R.string.download_srtm_maps);
			}
			return "";
		}

		private class LocalIndexInfoViewHolder {

			private final TextView nameTextView;
			private final ImageButton options;
			private final ImageView icon;
			private final TextView descriptionTextView;
			private final CheckBox checkbox;

			public LocalIndexInfoViewHolder(View view) {
				nameTextView = view.findViewById(R.id.nameTextView);
				options = view.findViewById(R.id.options);
				icon = view.findViewById(R.id.icon);
				descriptionTextView = view.findViewById(R.id.descriptionTextView);
				checkbox = view.findViewById(R.id.check_local_index);
			}

			public void bindLocalIndexInfo(LocalIndexInfo child) {
				options.setImageDrawable(ctx.getMyApplication().getUIUtilities()
						.getThemedIcon(R.drawable.ic_overflow_menu_white));
				options.setContentDescription(ctx.getString(R.string.shared_string_more));
				options.setOnClickListener(v -> openPopUpMenu(v, child));
				int colorId = filesToUpdate.containsKey(child.getFileName()) ? R.color.color_distance : R.color.color_ok;
				if (child.isBackupedData()) {
					colorId = R.color.color_unknown;
				}
				icon.setImageDrawable(getContentIcon(ctx, child.getType().getIconResource(), colorId));

				nameTextView.setText(getNameToDisplay(child));
				if (child.isNotSupported()) {
					nameTextView.setTextColor(warningColor);
				} else if (child.isCorrupted()) {
					nameTextView.setTextColor(corruptedColor);
				} else {
					nameTextView.setTextColor(okColor);
				}
				if (child.isBackupedData()) {
					nameTextView.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
				} else {
					nameTextView.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
				}
				StringBuilder builder = new StringBuilder();

				String mapDescription = getMapDescription(child);
				if (mapDescription.length() > 0) {
					builder.append(mapDescription);
				}

				if (child.getSize() >= 0) {
					if (builder.length() > 0) {
						builder.append(" • ");
					}
					builder.append(AndroidUtils.formatSize(ctx, child.getSize() * 1024L));
				}

				if (SrtmDownloadItem.isSRTMItem(child)) {
					builder.append(" ").append(SrtmDownloadItem.getAbbreviationInScopes(ctx, child));
				}

				if (!Algorithms.isEmpty(child.getDescription())) {
					if (builder.length() > 0) {
						builder.append(" • ");
					}
					builder.append(child.getDescription());
				}
				descriptionTextView.setText(builder.toString());
				checkbox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
				checkbox.setContentDescription(getNameToDisplay(child));
				if (selectionMode) {
					icon.setVisibility(View.GONE);
					options.setVisibility(View.GONE);
					checkbox.setChecked(selectedItems.contains(child));
					checkbox.setOnClickListener(v -> {
						if (checkbox.isChecked()) {
							selectedItems.add(child);
						} else {
							selectedItems.remove(child);
						}
					});

				} else {
					options.setVisibility(View.VISIBLE);
					icon.setVisibility(View.VISIBLE);
				}
			}

			private Drawable getContentIcon(DownloadActivity context, int resourceId, int colorId) {
				return context.getMyApplication().getUIUtilities().getIcon(resourceId, colorId);
			}
		}
	}

	private void openPopUpMenu(View v, LocalIndexInfo info) {
		UiUtilities iconsCache = getMyApplication().getUIUtilities();
		PopupMenu optionsMenu = new PopupMenu(getActivity(), v);
		DirectionsDialogs.setupPopUpMenuIcon(optionsMenu);
		boolean restore = info.isBackupedData();
		MenuItem item;
		if ((info.getType() == LocalIndexType.MAP_DATA) || (info.getType() == LocalIndexType.DEACTIVATED)) {
			item = optionsMenu.getMenu().add(restore ? R.string.local_index_mi_restore : R.string.local_index_mi_backup)
					.setIcon(iconsCache.getThemedIcon(R.drawable.ic_type_archive));
			item.setOnMenuItemClickListener(menuItem -> {
				performBasicOperation(restore ? R.string.local_index_mi_restore : R.string.local_index_mi_backup, info);
				return true;
			});
		}
		if (info.getType() != LocalIndexType.TILES_DATA) {
			item = optionsMenu.getMenu().add(R.string.shared_string_rename)
					.setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_edit_dark));
			item.setOnMenuItemClickListener(menuItem -> {
				performBasicOperation(R.string.shared_string_rename, info);
				return true;
			});
		}
		if (info.getType() == LocalIndexType.TILES_DATA
				&& ((info.getAttachedObject() instanceof TileSourceManager.TileSourceTemplate)
				|| ((info.getAttachedObject() instanceof SQLiteTileSource)
				&& ((SQLiteTileSource) info.getAttachedObject()).couldBeDownloadedFromInternet()))) {
			item = optionsMenu.getMenu().add(R.string.shared_string_edit)
					.setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_edit_dark));
			item.setOnMenuItemClickListener(menuItem -> {
				performBasicOperation(R.string.shared_string_edit, info);
				return true;
			});
		}
		if (info.getType() == LocalIndexType.TILES_DATA && (info.getAttachedObject() instanceof ITileSource)
				&& ((ITileSource) info.getAttachedObject()).couldBeDownloadedFromInternet()) {
			item = optionsMenu.getMenu().add(R.string.clear_tile_data)
					.setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_remove_dark));
			item.setOnMenuItemClickListener(menuItem -> {
				performBasicOperation(R.string.clear_tile_data, info);
				return true;
			});
		}
		IndexItem update = filesToUpdate.get(info.getFileName());
		if (update != null) {
			item = optionsMenu.getMenu().add(R.string.update_tile)
					.setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_import));
			item.setOnMenuItemClickListener(menuItem -> {
				DownloadActivity downloadActivity = getDownloadActivity();
				if (downloadActivity != null) {
					downloadActivity.startDownload(update);
				}
				return true;
			});
		}

		item = optionsMenu.getMenu().add(R.string.shared_string_delete)
				.setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_delete_dark));
		item.setOnMenuItemClickListener(menuItem -> {
			performBasicOperation(R.string.shared_string_delete, info);
			return true;
		});


		optionsMenu.show();
	}

	private DownloadActivity getDownloadActivity() {
		return (DownloadActivity) getActivity();
	}
}
