package net.osmand.plus.download.ui;

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
import java.util.regex.Pattern;

import net.osmand.AndroidUtils;
import net.osmand.Collator;
import net.osmand.IndexConstants;
import net.osmand.OsmAndCollator;
import net.osmand.map.ITileSource;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.ItemClickListener;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.LocalIndexHelper;
import net.osmand.plus.activities.LocalIndexHelper.LocalIndexType;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import net.osmand.plus.base.OsmandExpandableListFragment;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.resources.IncrementalChangesManager;
import net.osmand.util.Algorithms;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.PopupMenu;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


public class LocalIndexesFragment extends OsmandExpandableListFragment implements DownloadEvents {
	public static final Pattern ILLEGAL_FILE_NAME_CHARACTERS = Pattern.compile("[?:\"*|/<>]");
	public static final Pattern ILLEGAL_PATH_NAME_CHARACTERS = Pattern.compile("[?:\"*|<>]");

	private LoadLocalIndexTask asyncLoader;
	private Map<String, IndexItem> filesToUpdate = new HashMap<>();
	private LocalIndexesAdapter listAdapter;
	private AsyncTask<LocalIndexInfo, ?, ?> operationTask;

	private boolean selectionMode = false;
	private Set<LocalIndexInfo> selectedItems = new LinkedHashSet<>();

	private ContextMenuAdapter optionsMenuAdapter;
	private ActionMode actionMode;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.local_index, container, false);

		getDownloadActivity().setSupportProgressBarIndeterminateVisibility(false);
		getDownloadActivity().getAccessibilityAssistant().registerPage(view, DownloadActivity.LOCAL_TAB_NUMBER);

		ExpandableListView listView = (ExpandableListView) view.findViewById(android.R.id.list);
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

		getExpandableListView().setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
				long packedPos = ((ExpandableListContextMenuInfo) menuInfo).packedPosition;
				int group = ExpandableListView.getPackedPositionGroup(packedPos);
				int child = ExpandableListView.getPackedPositionChild(packedPos);
				if (child >= 0 && group >= 0) {
					final LocalIndexInfo point = listAdapter.getChild(group, child);
					showContextMenu(point);
				}
			}
		});
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

	private void showContextMenu(final LocalIndexInfo info) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		final ContextMenuAdapter adapter = new ContextMenuAdapter();
		basicFileOperation(info, adapter);
		OsmandPlugin.onContextMenuActivity(getActivity(), null, info, adapter);

		String[] values = adapter.getItemNames();
		builder.setItems(values, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ContextMenuItem item = adapter.getItem(which);
				if (item.getItemClickListener() != null) {
					item.getItemClickListener().onContextMenuClick(null,
							item.getTitleId(), which, false, null);
				}
			}

		});
		builder.show();
	}


	private void basicFileOperation(final LocalIndexInfo info, ContextMenuAdapter adapter) {
		ItemClickListener listener = new ItemClickListener() {
			@Override
			public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int resId, int pos, boolean isChecked, int[] viewCoordinates) {
				return performBasicOperation(resId, info);
			}
		};
		if (info.getType() == LocalIndexType.MAP_DATA || info.getType() == LocalIndexType.SRTM_DATA ||
				info.getType() == LocalIndexType.WIKI_DATA ) {
			if (!info.isBackupedData()) {
				adapter.addItem(new ContextMenuItem.ItemBuilder()
						.setTitleId(R.string.local_index_mi_backup, getContext())
						.setListener(listener)
						.setPosition(1).createItem());
			}
		}
		if (info.isBackupedData()) {
			adapter.addItem(new ContextMenuItem.ItemBuilder()
					.setTitleId(R.string.local_index_mi_restore, getContext())
					.setListener(listener)
					.setPosition(2).createItem());
		}
		if (info.getType() != LocalIndexType.TTS_VOICE_DATA && info.getType() != LocalIndexType.VOICE_DATA
				&& info.getType() != LocalIndexType.FONT_DATA) {
			adapter.addItem(new ContextMenuItem.ItemBuilder()
					.setTitleId(R.string.shared_string_rename, getContext())
					.setListener(listener)
					.setPosition(3).createItem());
		}
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.shared_string_delete, getContext())
				.setListener(listener)
				.setPosition(4).createItem());
	}

	private boolean performBasicOperation(int resId, final LocalIndexInfo info) {
		if (resId == R.string.shared_string_rename) {
			renameFile(getActivity(), new File(info.getPathToData()), new RenameCallback() {

				@Override
				public void renamedTo(File file) {
					getDownloadActivity().reloadLocalIndexes();
				}
			});
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

	public static void renameFile(final Activity a, final File f, final RenameCallback callback) {
		AlertDialog.Builder b = new AlertDialog.Builder(a);
		if (f.exists()) {
			int xt = f.getName().lastIndexOf('.');
			final String ext = xt == -1 ? "" : f.getName().substring(xt);
			final String originalName = xt == -1 ? f.getName() : f.getName().substring(0, xt);
			final EditText editText = new EditText(a);
			editText.setText(originalName);
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
						if (ILLEGAL_FILE_NAME_CHARACTERS.matcher(text).find()) {
							editText.setError(a.getString(R.string.file_name_containes_illegal_char));
						}
					}
				}
			});
			b.setTitle(R.string.shared_string_rename);
			int leftPadding = AndroidUtils.dpToPx(a, 24f);
			int topPadding = AndroidUtils.dpToPx(a, 4f);
			b.setView(editText, leftPadding, topPadding, leftPadding, topPadding);
			// Behaviour will be overwritten later;
			b.setPositiveButton(R.string.shared_string_save, null);
			b.setNegativeButton(R.string.shared_string_cancel, null);
			final AlertDialog alertDialog = b.create();
			alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
				@Override
				public void onShow(DialogInterface dialog) {
					alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
							new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									OsmandApplication app = (OsmandApplication) a.getApplication();
									if (renameGpxFile(app, f, editText.getText().toString() + ext, false, callback) != null) {
										alertDialog.dismiss();
									}
								}
							});
				}
			});
			alertDialog.show();
		}
	}

	public static File renameGpxFile(OsmandApplication ctx, File source, String newName, boolean dirAllowed, RenameCallback callback) {
		if (Algorithms.isEmpty(newName)) {
			Toast.makeText(ctx, R.string.empty_filename, Toast.LENGTH_LONG).show();
			return null;
		}
		Pattern illegalCharactersPattern = dirAllowed ? ILLEGAL_PATH_NAME_CHARACTERS : ILLEGAL_FILE_NAME_CHARACTERS;
		if (illegalCharactersPattern.matcher(newName).find()) {
			Toast.makeText(ctx, R.string.file_name_containes_illegal_char, Toast.LENGTH_LONG).show();
			return null;
		}
		File dest = new File(source.getParentFile(), newName);
		if (dest.exists()) {
			Toast.makeText(ctx, R.string.file_with_name_already_exists, Toast.LENGTH_LONG).show();
		} else {
			if (!dest.getParentFile().exists()) {
				dest.getParentFile().mkdirs();
			}
			if (source.renameTo(dest)) {
				ctx.getGpxDbHelper().rename(source, dest);
				if (callback != null) {
					callback.renamedTo(dest);
				}
				return dest;
			} else {
				Toast.makeText(ctx, R.string.file_can_not_be_renamed, Toast.LENGTH_LONG).show();
			}
		}
		return null;
	}


	public class LoadLocalIndexTask extends AsyncTask<Void, LocalIndexInfo, List<LocalIndexInfo>>
			implements AbstractLoadLocalIndexTask {

		private List<LocalIndexInfo> result;

		@Override
		protected List<LocalIndexInfo> doInBackground(Void... params) {
			LocalIndexHelper helper = new LocalIndexHelper(getMyApplication());
			return helper.getLocalIndexData(this);
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
			for (LocalIndexInfo v : values) {
				listAdapter.addLocalIndexInfo(v);
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
		private DownloadActivity a;
		private LocalIndexesAdapter listAdapter;

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
						ITileSource src =  (ITileSource) info.getAttachedObject();
						if(src != null) {
							src.deleteTiles(info.getPathToData());
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
			if(result != null && result.length() > 0) {
				Toast.makeText(a, result, Toast.LENGTH_LONG).show();
			}
			
			if (operation == RESTORE_OPERATION || operation == BACKUP_OPERATION || operation == CLEAR_TILES_OPERATION) {
				a.reloadLocalIndexes();
			} else {
				a.newDownloadIndexes();
			}
		}
	}

	@Override
	public void newDownloadIndexes() {
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
		int iconColorResId = getMyApplication().getSettings().isLightContent() ? R.color.active_buttons_and_links_text_light : R.color.active_buttons_and_links_text_dark;
		optionsMenuAdapter = new ContextMenuAdapter();
		ItemClickListener listener = new ContextMenuAdapter.ItemClickListener() {
			@Override
			public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter,
											  int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
				localOptionsMenu(itemId);
				return true;
			}
		};
		optionsMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.shared_string_refresh, getContext())
				.setIcon(R.drawable.ic_action_refresh_dark)
				.setListener(listener)
				.setColor(iconColorResId)
				.createItem());
		optionsMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.shared_string_delete, getContext())
				.setIcon(R.drawable.ic_action_delete_dark)
				.setListener(listener)
				.setColor(iconColorResId)
				.createItem());
		optionsMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.local_index_mi_backup, getContext())
				.setListener(listener)
				.createItem());
		optionsMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.local_index_mi_restore, getContext())
				.setListener(listener)
				.createItem());
		// doesn't work correctly
		//int max =  getResources().getInteger(R.integer.abs__max_action_buttons);
		int max = 3;
		SubMenu split = null;
		for (int j = 0; j < optionsMenuAdapter.length(); j++) {
			MenuItem item;
			ContextMenuItem contextMenuItem = optionsMenuAdapter.getItem(j);
			if (j + 1 >= max && optionsMenuAdapter.length() > max) {
				if (split == null) {
					split = menu.addSubMenu(0, 1, j + 1, R.string.shared_string_more_actions);
					split.setIcon(R.drawable.ic_overflow_menu_white);
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
				Drawable icMenuItem = getMyApplication().getUIUtilities().getIcon(contextMenuItem.getIcon(), contextMenuItem.getColorRes());
				item.setIcon(icMenuItem);
			}

		}

		if (operationTask == null || operationTask.getStatus() == AsyncTask.Status.FINISHED) {
			menu.setGroupVisible(0, true);
		} else {
			menu.setGroupVisible(0, false);
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
			operationTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, selectedItems.toArray(new LocalIndexInfo[selectedItems.size()]));
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

	private void openSelectionMode(final int actionResId, final int actionIconId,
								   final DialogInterface.OnClickListener listener) {
		final int colorResId = getMyApplication().getSettings().isLightContent() ? R.color.active_buttons_and_links_text_light : R.color.active_buttons_and_links_text_dark;
		String value = getString(actionResId);
		if (value.endsWith("...")) {
			value = value.substring(0, value.length() - 3);
		}
		final String actionButton = value;
		if (listAdapter.getGroupCount() == 0) {
			listAdapter.cancelFilter();
			expandAllGroups();
			listAdapter.notifyDataSetChanged();
			Toast.makeText(getDownloadActivity(), getString(R.string.local_index_no_items_to_do, actionButton.toLowerCase()), Toast.LENGTH_SHORT).show();
			return;
		}
		expandAllGroups();

		selectionMode = true;
		selectedItems.clear();
		actionMode = getDownloadActivity().startSupportActionMode(new ActionMode.Callback() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				selectionMode = true;
				MenuItem it = menu.add(actionResId);
				if (actionIconId != 0) {
					Drawable icon = getMyApplication().getUIUtilities().getIcon(actionIconId, colorResId);
					it.setIcon(icon);
				}
				MenuItemCompat.setShowAsAction(it, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM |
						MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				if (selectedItems.isEmpty()) {
					Toast.makeText(getDownloadActivity(),
							getString(R.string.local_index_no_items_to_do, actionButton.toLowerCase()), Toast.LENGTH_SHORT).show();
					return true;
				}

				AlertDialog.Builder builder = new AlertDialog.Builder(getDownloadActivity());
				builder.setMessage(getString(R.string.local_index_action_do, actionButton.toLowerCase(), selectedItems.size()));
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
		//findViewById(R.id.DescriptionText).setVisibility(View.GONE);
		listAdapter.notifyDataSetChanged();
	}

	public void localOptionsMenu(final int itemId) {
		if (itemId == R.string.shared_string_refresh) {
			getDownloadActivity().reloadLocalIndexes();
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
		List<LocalIndexInfo> filterCategory = null;
		int warningColor;
		int okColor;
		int corruptedColor;
		DownloadActivity ctx;

		public LocalIndexesAdapter(DownloadActivity ctx) {
			this.ctx = ctx;
			warningColor = ContextCompat.getColor(ctx, R.color.color_warning);
			boolean light = ctx.getMyApplication().getSettings().isLightContent();
			okColor = ContextCompat.getColor(ctx, light ? R.color.text_color_primary_light : R.color.text_color_primary_dark);
			corruptedColor = ContextCompat.getColor(ctx, R.color.color_invalid);
		}

		public void clear() {
			data.clear();
			category.clear();
			filterCategory = null;
			notifyDataSetChanged();
		}

		public void sortData() {
			final Collator cl = OsmAndCollator.primaryCollator();
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
		public View getChildView(final int groupPosition, final int childPosition,
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
			return child.getType() == LocalIndexType.VOICE_DATA ? FileNameTranslationHelper.getVoiceName(ctx, child.getFileName()) :
					FileNameTranslationHelper.getFileName(ctx,
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
			TextView nameView = ((TextView) v.findViewById(R.id.title));
			TextView sizeView = ((TextView) v.findViewById(R.id.section_description));
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
				if (size > 1 << 20) {
					sz = DownloadActivity.formatGb.format(new Object[]{(float) size / (1 << 20)});
				} else {
					sz = DownloadActivity.formatMb.format(new Object[]{(float) size / (1 << 10)});
				}
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
			} else if (child.isBackupedData() && child.getFileName().endsWith(IndexConstants.BINARY_SRTM_MAP_INDEX_EXT)) {
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
				nameTextView = ((TextView) view.findViewById(R.id.nameTextView));
				options = (ImageButton) view.findViewById(R.id.options);
				icon = (ImageView) view.findViewById(R.id.icon);
				descriptionTextView = (TextView) view.findViewById(R.id.descriptionTextView);
				checkbox = (CheckBox) view.findViewById(R.id.check_local_index);
			}

			public void bindLocalIndexInfo(final LocalIndexInfo child) {

				options.setImageDrawable(ctx.getMyApplication().getUIUtilities()
						.getThemedIcon(R.drawable.ic_overflow_menu_white));
				options.setContentDescription(ctx.getString(R.string.shared_string_more));
				options.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						openPopUpMenu(v, child);
					}
				});
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

				final String mapDescription = getMapDescription(child);
				if (mapDescription.length() > 0) {
					builder.append(mapDescription);
				}

				if (child.getSize() >= 0) {
					if (builder.length() > 0) {
						builder.append(" • ");
					}
					if (child.getSize() > 100) {
						builder.append(DownloadActivity.formatMb.format(new Object[]{(float) child.getSize() / (1 << 10)}));
					} else {
						builder.append(child.getSize()).append(" KB");
					}
				}

				if (!Algorithms.isEmpty(child.getDescription())) {
					if (builder.length() > 0) {
						builder.append(" • ");
					}
					builder.append(child.getDescription());
				}
				descriptionTextView.setText(builder.toString());
				checkbox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
				if (selectionMode) {
					icon.setVisibility(View.GONE);
					options.setVisibility(View.GONE);
					checkbox.setChecked(selectedItems.contains(child));
					checkbox.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(View v) {
							if (checkbox.isChecked()) {
								selectedItems.add(child);
							} else {
								selectedItems.remove(child);
							}
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

	private void openPopUpMenu(View v, final LocalIndexInfo info) {
		UiUtilities iconsCache = getMyApplication().getUIUtilities();
		final PopupMenu optionsMenu = new PopupMenu(getActivity(), v);
		DirectionsDialogs.setupPopUpMenuIcon(optionsMenu);
		final boolean restore = info.isBackupedData();
		MenuItem item;
		if ((info.getType() == LocalIndexType.MAP_DATA) || (info.getType() == LocalIndexType.DEACTIVATED)) {
			item = optionsMenu.getMenu().add(restore ? R.string.local_index_mi_restore : R.string.local_index_mi_backup)
					.setIcon(iconsCache.getThemedIcon(R.drawable.ic_type_archive));
			item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					performBasicOperation(restore ? R.string.local_index_mi_restore : R.string.local_index_mi_backup, info);
					return true;
				}
			});
		}

		item = optionsMenu.getMenu().add(R.string.shared_string_rename)
				.setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_edit_dark));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				performBasicOperation(R.string.shared_string_rename, info);
				return true;
			}
		});
		if (info.getType() == LocalIndexType.TILES_DATA && (info.getAttachedObject() instanceof ITileSource) &&
				((ITileSource)info.getAttachedObject()).couldBeDownloadedFromInternet()) {
			item = optionsMenu.getMenu().add(R.string.clear_tile_data)
					.setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_remove_dark));
			item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					performBasicOperation(R.string.clear_tile_data, info);
					return true;
				}
			});	
		}
		final IndexItem update = filesToUpdate.get(info.getFileName());
		if (update != null) {
			item = optionsMenu.getMenu().add(R.string.update_tile)
					.setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_import));
			item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					getDownloadActivity().startDownload(update);
					return true;
				}
			});
		}

		item = optionsMenu.getMenu().add(R.string.shared_string_delete)
				.setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_delete_dark));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				performBasicOperation(R.string.shared_string_delete, info);
				return true;
			}
		});


		optionsMenu.show();
	}

	private DownloadActivity getDownloadActivity() {
		return (DownloadActivity) getActivity();
	}

	public interface RenameCallback {

		public void renamedTo(File file);
	}
}
