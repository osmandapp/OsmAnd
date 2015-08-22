package net.osmand.plus.download;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StatFs;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.PopupMenu;
import android.text.method.LinkMovementMethod;
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
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.access.AccessibleToast;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.LocalIndexHelper;
import net.osmand.plus.activities.LocalIndexHelper.LocalIndexType;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.activities.OsmAndListFragment;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import net.osmand.plus.activities.OsmandExpandableListFragment;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.resources.IncrementalChangesManager;
import net.osmand.plus.resources.IncrementalChangesManager.IncrementalUpdate;
import net.osmand.plus.resources.IncrementalChangesManager.IncrementalUpdateList;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


public class NewLocalIndexesFragment extends OsmAndListFragment {

	private LoadLocalIndexTask asyncLoader;
	private BaseAdapter listAdapter;
	private AsyncTask<LocalIndexInfo, ?, ?> operationTask;

	private boolean selectionMode = false;
	private Set<LocalIndexInfo> selectedItems = new LinkedHashSet<LocalIndexInfo>();

	protected static int DELETE_OPERATION = 1;
	protected static int BACKUP_OPERATION = 2;
	protected static int RESTORE_OPERATION = 3;

	MessageFormat formatMb = new MessageFormat("{0, number,##.#} MB", Locale.US);
	MessageFormat formatGb = new MessageFormat("{0, number,#.##} GB", Locale.US);
	private ContextMenuAdapter optionsMenuAdapter;
	private ActionMode actionMode;

	private TextView descriptionText;
	private ProgressBar sizeProgress;

	Drawable backup;
	Drawable sdcard;
	Drawable planet;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.local_index_fragment, container, false);

		getDownloadActivity().setSupportProgressBarIndeterminateVisibility(false);

		ListView listView = (ListView)view.findViewById(android.R.id.list);
//		listAdapter = new LocalIndexesAdapter(getActivity());
//		listView.setAdapter(listAdapter);
//		setListView(listView);
		descriptionText = (TextView) view.findViewById(R.id.memory_size);
		sizeProgress = (ProgressBar) view.findViewById(R.id.memory_progress);
		updateDescriptionTextWithSize();
		colorDrawables();
		return view;
	}

	@SuppressWarnings({"unchecked","deprecation"})
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (asyncLoader == null || asyncLoader.getResult() == null) {
			// getLastNonConfigurationInstance method should be in onCreate() method
			// (onResume() doesn't work)
			Object indexes = getActivity().getLastNonConfigurationInstance();
			asyncLoader = new LoadLocalIndexTask();
			if (indexes instanceof List<?>) {
				asyncLoader.setResult((List<LocalIndexInfo>) indexes);
			}
		}
		setHasOptionsMenu(true);
	}

	private void colorDrawables(){
		boolean light = getMyApplication().getSettings().isLightContent();
		backup = getActivity().getResources().getDrawable(R.drawable.ic_type_archive);
		backup.mutate();
		if (light) {
			backup.setColorFilter(getResources().getColor(R.color.icon_color_light), PorterDuff.Mode.MULTIPLY);
		}
		sdcard = getActivity().getResources().getDrawable(R.drawable.ic_sdcard);
		sdcard.mutate();
		sdcard.setColorFilter(getActivity().getResources().getColor(R.color.color_distance), PorterDuff.Mode.MULTIPLY);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (asyncLoader == null || asyncLoader.getResult() == null) {
			reloadData();
		}
	}

	public void reloadData() {
		asyncLoader = new LoadLocalIndexTask();
		asyncLoader.execute(getActivity());
	}


	private void showContextMenu(final LocalIndexInfo info) {
		Builder builder = new Builder(getActivity());
		final ContextMenuAdapter adapter = new ContextMenuAdapter(getActivity());
		basicFileOperation(info, adapter);
		OsmandPlugin.onContextMenuActivity(getActivity(), null, info, adapter);

		String[] values = adapter.getItemNames();
		builder.setItems(values, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				OnContextMenuClick clk = adapter.getClickAdapter(which);
				if (clk != null) {
					clk.onContextMenuClick(null, adapter.getElementId(which), which, false);
				}
			}

		});
		builder.show();
	}


	private void basicFileOperation(final LocalIndexInfo info, ContextMenuAdapter adapter) {
		OnContextMenuClick listener = new OnContextMenuClick() {
			@Override
			public boolean onContextMenuClick(ArrayAdapter<?> adapter, int resId, int pos, boolean isChecked) {
				return performBasicOperation(resId, info);
			}
		};
		if(info.getType() == LocalIndexType.MAP_DATA || info.getType() == LocalIndexType.SRTM_DATA ||
				info.getType() == LocalIndexType.WIKI_DATA){
			if(!info.isBackupedData()){
				adapter.item(R.string.local_index_mi_backup).listen(listener).position( 1).reg();
			}
		}
		if(info.isBackupedData()){
			adapter.item(R.string.local_index_mi_restore).listen(listener).position(2).reg();
		}
		if(info.getType() != LocalIndexType.TTS_VOICE_DATA && info.getType() != LocalIndexType.VOICE_DATA){
			adapter.item(R.string.shared_string_rename).listen(listener).position(3).reg();
		}
		adapter.item(R.string.shared_string_delete).listen(listener).position(4).reg();
	}

	private boolean performBasicOperation(int resId, final LocalIndexInfo info) {
		if (resId == R.string.shared_string_rename) {
			renameFile(getActivity(), new File(info.getPathToData()), new Runnable() {

				@Override
				public void run() {
					reloadIndexes();
				}
			});
		} else if (resId == R.string.local_index_mi_restore) {
			new LocalIndexOperationTask(RESTORE_OPERATION).execute(info);
		} else if (resId == R.string.shared_string_delete) {
			Builder confirm = new Builder(getActivity());
			confirm.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					new LocalIndexOperationTask(DELETE_OPERATION).execute(info);
				}
			});
			confirm.setNegativeButton(R.string.shared_string_no, null);
			String fn = FileNameTranslationHelper.getFileName(getActivity(),
					getMyApplication().getResourceManager().getOsmandRegions(),
					info.getFileName());
			confirm.setMessage(getString(R.string.delete_confirmation_msg, fn));
			confirm.show();
		} else if (resId == R.string.local_index_mi_backup) {
			new LocalIndexOperationTask(BACKUP_OPERATION).execute(info);
		}
		return true;
	}

	public static void renameFile(final Activity a, final File f, final Runnable callback) {
		Builder b = new Builder(a);
		if(f.exists()){
			int xt = f.getName().lastIndexOf('.');
			final String ext = xt == -1 ? "" : f.getName().substring(xt);
			final String originalName = xt == -1 ? f.getName() : f.getName().substring(0, xt);
			final EditText editText = new EditText(a);
			editText.setText(originalName);
			b.setView(editText);
			b.setPositiveButton(R.string.shared_string_save, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					String newName = editText.getText().toString() + ext;
					File dest = new File(f.getParentFile(), newName);
					if (dest.exists()) {
						AccessibleToast.makeText(a, R.string.file_with_name_already_exists, Toast.LENGTH_LONG).show();
					} else {
						if(!dest.getParentFile().exists()) {
							dest.getParentFile().mkdirs();
						}
						if(f.renameTo(dest)){
							if(callback != null) {
								callback.run();
							}
						} else {
							AccessibleToast.makeText(a, R.string.file_can_not_be_renamed, Toast.LENGTH_LONG).show();
						}
					}

				}
			});
			b.setNegativeButton(R.string.shared_string_cancel, null);
			b.show();
		}
	}

	public class LoadLocalIndexTask extends AsyncTask<Activity, LocalIndexInfo, List<LocalIndexInfo>> {

		private List<LocalIndexInfo> result;

		@Override
		protected List<LocalIndexInfo> doInBackground(Activity... params) {
			LocalIndexHelper helper = new LocalIndexHelper(getMyApplication());
			return null; //helper.getLocalIndexData(this);
		}

		public void loadFile(LocalIndexInfo... loaded) {
			publishProgress(loaded);
		}

		@Override
		protected void onPreExecute() {
			getDownloadActivity().setSupportProgressBarIndeterminateVisibility(true);
//			listAdapter.clear();
		}

		@Override
		protected void onProgressUpdate(LocalIndexInfo... values) {
			for (LocalIndexInfo v : values) {
//				listAdapter.addLocalIndexInfo(v);
			}
			listAdapter.notifyDataSetChanged();
		}

		public void setResult(List<LocalIndexInfo> result) {
			this.result = result;
			if(result == null){
//				listAdapter.clear();
			} else {
				for (LocalIndexInfo v : result) {
//					listAdapter.addLocalIndexInfo(v);
				}
				listAdapter.notifyDataSetChanged();
				onPostExecute(result);
			}
		}

		@Override
		protected void onPostExecute(List<LocalIndexInfo> result) {
			this.result = result;
//			listAdapter.sortData();
			if (getDownloadActivity() != null){
				getDownloadActivity().setSupportProgressBarIndeterminateVisibility(false);
				getDownloadActivity().setLocalIndexInfos(result);
			}
		}

		public List<LocalIndexInfo> getResult() {
			return result;
		}

	}

	private File getFileToRestore(LocalIndexInfo i){
		if(i.isBackupedData()){
			File parent = new File(i.getPathToData()).getParentFile();
			if(i.getType() == LocalIndexType.SRTM_DATA){
				parent = getMyApplication().getAppPath(IndexConstants.SRTM_INDEX_DIR);
			} else if(i.getFileName().endsWith(IndexConstants.BINARY_ROAD_MAP_INDEX_EXT)){
				parent = getMyApplication().getAppPath(IndexConstants.ROADS_INDEX_DIR);
			} else if(i.getType() == LocalIndexType.WIKI_DATA){
				parent = getMyApplication().getAppPath(IndexConstants.WIKI_INDEX_DIR);
			} else if(i.getType() == LocalIndexType.MAP_DATA){
				parent = getMyApplication().getAppPath(IndexConstants.MAPS_PATH);
			} else if(i.getType() == LocalIndexType.TILES_DATA){
				parent = getMyApplication().getAppPath(IndexConstants.TILES_INDEX_DIR);
			} else if(i.getType() == LocalIndexType.VOICE_DATA){
				parent = getMyApplication().getAppPath(IndexConstants.VOICE_INDEX_DIR);
			} else if(i.getType() == LocalIndexType.TTS_VOICE_DATA){
				parent = getMyApplication().getAppPath(IndexConstants.VOICE_INDEX_DIR);
			}
			return new File(parent, i.getFileName());
		}
		return new File(i.getPathToData());
	}

	private File getFileToBackup(LocalIndexInfo i) {
		if(!i.isBackupedData()){
			return new File(getMyApplication().getAppPath(IndexConstants.BACKUP_INDEX_DIR), i.getFileName());
		}
		return new File(i.getPathToData());
	}

	private boolean move(File from, File to){
		if(!to.getParentFile().exists()){
			to.getParentFile().mkdirs();
		}
		return from.renameTo(to);
	}

	public class LocalIndexOperationTask extends AsyncTask<LocalIndexInfo, LocalIndexInfo, String> {

		private final int operation;

		public LocalIndexOperationTask(int operation){
			this.operation = operation;
		}


		@Override
		protected String doInBackground(LocalIndexInfo... params) {
			int count = 0;
			int total = 0;
			for(LocalIndexInfo info : params) {
				if(!isCancelled()){
					boolean successfull = false;
					if(operation == DELETE_OPERATION){
						File f = new File(info.getPathToData());
						successfull = Algorithms.removeAllFiles(f);
					} else if(operation == RESTORE_OPERATION){
						successfull = move(new File(info.getPathToData()), getFileToRestore(info));
						if(successfull){
							info.setBackupedData(false);
						}
					} else if(operation == BACKUP_OPERATION){
						successfull = move(new File(info.getPathToData()), getFileToBackup(info));
						if(successfull){
							info.setBackupedData(true);
							getMyApplication().getResourceManager().closeFile(info.getFileName());
						}
					}
					total ++;
					if(successfull){
						count++;
						publishProgress(info);
					}
				}
			}
			if(operation == DELETE_OPERATION){
				return getString(R.string.local_index_items_deleted, count, total);
			} else if(operation == BACKUP_OPERATION){
				return getString(R.string.local_index_items_backuped, count, total);
			} else if(operation == RESTORE_OPERATION){
				return getString(R.string.local_index_items_restored, count, total);
			}

			return "";
		}


		@Override
		protected void onProgressUpdate(LocalIndexInfo... values) {
			if(operation == DELETE_OPERATION){
//				listAdapter.delete(values);
			} else if(operation == BACKUP_OPERATION){
//				listAdapter.move(values, false);
			} else if(operation == RESTORE_OPERATION){
//				listAdapter.move(values, true);
			}

		}

		@Override
		protected void onPreExecute() {
			getDownloadActivity().setProgressBarIndeterminateVisibility(true);
		}

		@Override
		protected void onPostExecute(String result) {
			getDownloadActivity().setProgressBarIndeterminateVisibility(false);
			AccessibleToast.makeText(getDownloadActivity(), result, Toast.LENGTH_LONG).show();
			if (operation == RESTORE_OPERATION || operation == BACKUP_OPERATION){
//				listAdapter.clear();
				reloadIndexes();
			}
		}
	}




	public Set<LocalIndexInfo> getSelectedItems() {
		return selectedItems;
	}




	@Override
	public void onPause() {
		super.onPause();
		if(operationTask != null){
			operationTask.cancel(true);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		asyncLoader.cancel(true);
	}


	@SuppressWarnings("deprecation")
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if (!this.isAdded()) {
			return;
		}

		//fixes issue when local files not shown after switching tabs
		//Next line throws NPE in some circumstances when called from dashboard and listAdpater=null is not checked for. (Checking !this.isAdded above is not sufficient!)
		ActionBar actionBar = getDownloadActivity().getSupportActionBar();
		//hide action bar from downloadindexfragment
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		optionsMenuAdapter = new ContextMenuAdapter(getDownloadActivity());
		OnContextMenuClick listener = new OnContextMenuClick() {
			@Override
			public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
				localOptionsMenu(itemId);
				return true;
			}
		};
		optionsMenuAdapter.item(R.string.local_index_mi_reload)
				.icon(R.drawable.ic_action_refresh_dark)
				.listen(listener).position(1).reg();
		optionsMenuAdapter.item(R.string.shared_string_delete)
				.icon(R.drawable.ic_action_delete_dark)
				.listen(listener).position(2).reg();
		optionsMenuAdapter.item(R.string.local_index_mi_backup)
				.listen(listener).position(3).reg();
		optionsMenuAdapter.item(R.string.local_index_mi_restore)
				.listen(listener).position(4).reg();
		// doesn't work correctly
		//int max =  getResources().getInteger(R.integer.abs__max_action_buttons);
		int max = 3;
		SubMenu split = null;
		for (int j = 0; j < optionsMenuAdapter.length(); j++) {
			MenuItem item;
			if (j + 1 >= max && optionsMenuAdapter.length() > max) {
				if (split == null) {
					split = menu.addSubMenu(0, 1, j + 1, R.string.shared_string_more_actions);
					split.setIcon(R.drawable.ic_overflow_menu_white);
					split.getItem();
					MenuItemCompat.setShowAsAction(split.getItem(),MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
				}
				item = split.add(0, optionsMenuAdapter.getElementId(j), j + 1, optionsMenuAdapter.getItemName(j));
				MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS );
			} else {
				item = menu.add(0, optionsMenuAdapter.getElementId(j), j + 1, optionsMenuAdapter.getItemName(j));
				MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS );
			}
			OsmandApplication app = getMyApplication();

		}

		if(operationTask == null || operationTask.getStatus() == AsyncTask.Status.FINISHED){
			menu.setGroupVisible(0, true);
		} else {
			menu.setGroupVisible(0, false);
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

	public void doAction(int actionResId){
		if(actionResId == R.string.local_index_mi_backup){
			operationTask = new LocalIndexOperationTask(BACKUP_OPERATION);
		} else if(actionResId == R.string.shared_string_delete){
			operationTask = new LocalIndexOperationTask(DELETE_OPERATION);
		} else if(actionResId == R.string.local_index_mi_restore){
			operationTask = new LocalIndexOperationTask(RESTORE_OPERATION);
		} else {
			operationTask = null;
		}
		if(operationTask != null){
			operationTask.execute(selectedItems.toArray(new LocalIndexInfo[selectedItems.size()]));
		}
		if(actionMode != null) {
			actionMode.finish();
		}
	}

	private void openSelectionMode(final int actionResId, final int actionIconId,
			final DialogInterface.OnClickListener listener){
		String value = getString(actionResId);
		if (value.endsWith("...")) {
			value = value.substring(0, value.length() - 3);
		}
		final String actionButton = value;

		selectionMode = true;
		selectedItems.clear();
		//findViewById(R.id.DescriptionText).setVisibility(View.GONE);
		listAdapter.notifyDataSetChanged();
	}

	@SuppressWarnings("deprecation")
	private void updateDescriptionTextWithSize(){
		File dir = getMyApplication().getAppPath("").getParentFile();
		String size = formatGb.format(new Object[]{0});
		int percent = 0;
		if(dir.canRead()){
			StatFs fs = new StatFs(dir.getAbsolutePath());
			size = formatGb.format(new Object[]{(float) (fs.getAvailableBlocks()) * fs.getBlockSize() / (1 << 30) });
			percent = (int) (fs.getAvailableBlocks() * 100 / fs.getBlockCount());
		}
		sizeProgress.setProgress(percent);
		String text = getString(R.string.free, size);
		int l = text.indexOf('.');
		if(l == -1) {
			l = text.length();
		}
		descriptionText.setText(text);
		descriptionText.setMovementMethod(LinkMovementMethod.getInstance());
	}

	public void localOptionsMenu(final int itemId) {
		if (itemId == R.string.local_index_mi_reload) {
			reloadIndexes();
		} else if (itemId == R.string.shared_string_delete) {
			openSelectionMode(itemId, R.drawable.ic_action_delete_dark,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							doAction(itemId);
						}
					}, null, null);
		} else if (itemId == R.string.local_index_mi_backup) {
			openSelectionMode(itemId, R.drawable.ic_type_archive,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							doAction(itemId);
						}
					}, Boolean.FALSE, LocalIndexType.MAP_DATA);
		} else if (itemId == R.string.local_index_mi_restore) {
			openSelectionMode(itemId, R.drawable.ic_type_archive,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							doAction(itemId);
						}
					}, Boolean.TRUE, LocalIndexType.MAP_DATA);
		}
	}

	public void openSelectionMode(int stringRes, int darkIcon, DialogInterface.OnClickListener listener, Boolean backup,
			LocalIndexType filter) {
		openSelectionMode(stringRes, darkIcon, listener);
	}


	public void reloadIndexes() {
		asyncLoader = new LoadLocalIndexTask();
		AsyncTask<Void, String, List<String>> task = new AsyncTask<Void, String, List<String>>(){

			@Override
			protected void onPostExecute(List<String> warnings) {
				if ( getDownloadActivity() == null) {
					return;
				}
				getDownloadActivity().setSupportProgressBarIndeterminateVisibility(false);
				if (!warnings.isEmpty()) {
					final StringBuilder b = new StringBuilder();
					boolean f = true;
					for (String w : warnings) {
						if (f) {
							f = false;
						} else {
							b.append('\n');
						}
						b.append(w);
					}
					AccessibleToast.makeText(getDownloadActivity(), b.toString(), Toast.LENGTH_LONG).show();
				}
				if(asyncLoader.getStatus() == Status.PENDING) {
					asyncLoader.execute(getDownloadActivity());
				}
			}

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				getDownloadActivity().setSupportProgressBarIndeterminateVisibility(true);
			}
			@Override
			protected List<String> doInBackground(Void... params) {
				return getMyApplication().getResourceManager().reloadIndexes(IProgress.EMPTY_PROGRESS,
						new ArrayList<String>()
						);
			}

		};
		task.execute();

	}

	
	private void openPopUpMenu(View v, final LocalIndexInfo info) {
		IconsCache iconsCache = getMyApplication().getIconsCache();
		final PopupMenu optionsMenu = new PopupMenu(getActivity(), v);
		DirectionsDialogs.setupPopUpMenuIcon(optionsMenu);
		final boolean restore = info.isBackupedData();
		MenuItem item;
		if (info.getType() == LocalIndexType.MAP_DATA) {
			item = optionsMenu.getMenu().add(restore? R.string.local_index_mi_restore : R.string.local_index_mi_backup)
					.setIcon(backup);
			item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					performBasicOperation(restore ? R.string.local_index_mi_restore : R.string.local_index_mi_backup, info);
					return true;
				}
			});
		}

		item = optionsMenu.getMenu().add(R.string.shared_string_rename)
				.setIcon(iconsCache.getContentIcon(R.drawable.ic_action_edit_dark));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				performBasicOperation(R.string.shared_string_rename, info);
				return true;
			}
		});

		item = optionsMenu.getMenu().add(R.string.shared_string_delete)
				.setIcon(iconsCache.getContentIcon(R.drawable.ic_action_delete_dark));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				performBasicOperation(R.string.shared_string_delete, info);
				return true;
			}
		});
		if(getMyApplication().getSettings().BETA_TESTING_LIVE_UPDATES.get()) {
			item = optionsMenu.getMenu().add("Live updates")
					.setIcon(iconsCache.getContentIcon(R.drawable.ic_action_refresh_dark));
			item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					runLiveUpdate(info);
					return true;
				}
			});
		}
		
		optionsMenu.show();
	}
	
	private void runLiveUpdate(final LocalIndexInfo info) {
		final String fnExt = Algorithms.getFileNameWithoutExtension(new File(info.getFileName()));
		new AsyncTask<Object, Object, IncrementalUpdateList>() {

			protected void onPreExecute() {
				getDownloadActivity().setSupportProgressBarIndeterminateVisibility(true);

			};

			@Override
			protected IncrementalUpdateList doInBackground(Object... params) {
				IncrementalChangesManager cm = getMyApplication().getResourceManager().getChangesManager();
				return cm.getUpdatesByMonth(fnExt);
			}

			protected void onPostExecute(IncrementalUpdateList result) {
				getDownloadActivity().setSupportProgressBarIndeterminateVisibility(false);
				if (result.errorMessage != null) {
					Toast.makeText(getDownloadActivity(), result.errorMessage, Toast.LENGTH_SHORT).show();
				} else {
					List<IncrementalUpdate> ll = result.getItemsForUpdate();
					if(ll.isEmpty()) {
						Toast.makeText(getDownloadActivity(), R.string.no_updates_available, Toast.LENGTH_SHORT).show();
					} else {
						for (IncrementalUpdate iu : ll) {
							IndexItem ii = new IndexItem(iu.fileName, "Incremental update", iu.timestamp, iu.sizeText,
									iu.contentSize, iu.containerSize, DownloadActivityType.LIVE_UPDATES_FILE);
							getDownloadActivity().addToDownload(ii);
							getDownloadActivity().updateDownloadButton();
						}
					}
				}

			};

		}.execute(new Object[] { fnExt });
	}


	private DownloadActivity getDownloadActivity() {
		return (DownloadActivity) getActivity();
	}
}
