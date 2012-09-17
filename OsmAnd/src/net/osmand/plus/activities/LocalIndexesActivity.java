package net.osmand.plus.activities;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.osmand.Algoritms;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.IProgress;
import net.osmand.access.AccessibleToast;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.activities.EnumAdapter.IEnumWithResource;
import net.osmand.plus.activities.LocalIndexHelper.LocalIndexInfo;
import net.osmand.plus.activities.LocalIndexHelper.LocalIndexType;
import net.osmand.plus.osmedit.OpenstreetmapRemoteUtil;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.StatFs;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class LocalIndexesActivity extends OsmandExpandableListActivity {

	private LoadLocalIndexTask asyncLoader;
	private LocalIndexesAdapter listAdapter;
	private LoadLocalIndexDescriptionTask descriptionLoader;
	private AsyncTask<LocalIndexInfo, ?, ?> operationTask;

	private boolean selectionMode = false;
	private Set<LocalIndexInfo> selectedItems = new LinkedHashSet<LocalIndexInfo>();
	private OsmandSettings settings;
	
	protected static int DELETE_OPERATION = 1;
	protected static int BACKUP_OPERATION = 2;
	protected static int RESTORE_OPERATION = 3;
	
	MessageFormat formatMb = new MessageFormat("{0, number,##.#} MB", Locale.US);
	MessageFormat formatGb = new MessageFormat("{0, number,#.##} GB", Locale.US);

	
	
	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		CustomTitleBar titleBar = new CustomTitleBar(this, R.string.local_index_descr_title, R.drawable.tab_download_screen_icon, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				asyncLoader.setResult(null);
				startActivity(new Intent(LocalIndexesActivity.this, DownloadIndexActivity.class));
			}
		});
		setContentView(R.layout.local_index);
		titleBar.afterSetContentView();
		
		
		settings = getMyApplication().getSettings();
		descriptionLoader = new LoadLocalIndexDescriptionTask();
		listAdapter = new LocalIndexesAdapter();
		
		
		/*findViewById(R.id.DownloadButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				asyncLoader.setResult(null);
				startActivity(new Intent(LocalIndexesActivity.this, DownloadIndexActivity.class));
			}
		});*/
		
		getExpandableListView().setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
				long packedPos = ((ExpandableListContextMenuInfo)menuInfo).packedPosition;
				int group = ExpandableListView.getPackedPositionGroup(packedPos);
				int child = ExpandableListView.getPackedPositionChild(packedPos);
				if (child >= 0 && group >= 0) {
					final LocalIndexInfo point = (LocalIndexInfo) listAdapter.getChild(group, child);
					showContextMenu(point);
				}
			}
		});
		
		setListAdapter(listAdapter);
		updateDescriptionTextWithSize();
		if (asyncLoader == null || asyncLoader.getResult() == null) {
			// getLastNonConfigurationInstance method should be in onCreate() method 
			// (onResume() doesn't work)
			Object indexes = getLastNonConfigurationInstance();
			asyncLoader = new LoadLocalIndexTask();
			if (indexes instanceof List<?>) {
				asyncLoader.setResult((List<LocalIndexInfo>) indexes);
			}
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (asyncLoader == null || asyncLoader.getResult() == null) {
			asyncLoader = new LoadLocalIndexTask();
			asyncLoader.execute(this);
		}
	}
	
	public boolean sendGPXFiles(final LocalIndexInfo... info){
		String name = settings.USER_NAME.get();
		String pwd = settings.USER_PASSWORD.get();
		if(Algoritms.isEmpty(name) || Algoritms.isEmpty(pwd)){
			AccessibleToast.makeText(this, R.string.validate_gpx_upload_name_pwd, Toast.LENGTH_LONG).show();
			return false;
		}
		Builder bldr = new AlertDialog.Builder(this);
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View view = inflater.inflate(R.layout.send_gpx_osm, null);
		final EditText descr = (EditText) view.findViewById(R.id.DescriptionText);
		if(info.length > 0 && info[0].getFileName() != null) {
			int dt = info[0].getFileName().indexOf('.');
			descr.setText(info[0].getFileName().substring(0, dt));
		}
		final EditText tags = (EditText) view.findViewById(R.id.TagsText);		
		final Spinner visibility = ((Spinner)view.findViewById(R.id.Visibility));
		EnumAdapter<UploadVisibility> adapter = new EnumAdapter<UploadVisibility>(this, R.layout.my_spinner_text, UploadVisibility.values());
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		visibility.setAdapter(adapter);
		visibility.setSelection(0);
		
		bldr.setView(view);
		bldr.setNegativeButton(R.string.default_buttons_no, null);
		bldr.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				new UploadGPXFilesTask(descr.getText().toString(), tags.getText().toString(), 
				 (UploadVisibility) visibility.getItemAtPosition(visibility.getSelectedItemPosition())
					).execute(info);
			}
		});
		bldr.show();
		return true;
	}
	
	
	private void showContextMenu(final LocalIndexInfo info) {
		Builder builder = new AlertDialog.Builder(this);
		final List<Integer> menu = new ArrayList<Integer>();
		if(info.getType() == LocalIndexType.GPX_DATA){
			menu.add(R.string.show_gpx_route);
//			if(OsmandPlugin.getEnabledPlugin(OsmandDevelopmentPlugin.class) instanceof OsmandDevelopmentPlugin) {
				menu.add(R.string.local_index_mi_upload_gpx);
//			}
			descriptionLoader = new LoadLocalIndexDescriptionTask();
			descriptionLoader.execute(info);
		}
		if(info.getType() == LocalIndexType.MAP_DATA || info.getType() == LocalIndexType.POI_DATA){
			if(!info.isBackupedData()){
				menu.add(R.string.local_index_mi_backup);
			}
		}
		if(info.isBackupedData()){
			menu.add(R.string.local_index_mi_restore);
		}
		menu.add(R.string.local_index_mi_rename);
		menu.add(R.string.local_index_mi_delete);
		if (!menu.isEmpty()) {
			String[] values = new String[menu.size()];
			for (int i = 0; i < values.length; i++) {
				values[i] = getString(menu.get(i));
			}
			builder.setItems(values, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					int resId = menu.get(which);
					if (resId == R.string.show_gpx_route) {
						if (info != null && info.getGpxFile() != null) {
							WptPt loc = info.getGpxFile().findPointToShow();
							if (loc != null) {
								settings.setMapLocationToShow(loc.lat, loc.lon, settings.getLastKnownMapZoom());
							}
							getMyApplication().setGpxFileToDisplay(info.getGpxFile(), false);
							MapActivity.launchMapActivityMoveToTop(LocalIndexesActivity.this);
						}
					} else if (resId == R.string.local_index_mi_rename) {
						renameFile(info);
					} else if (resId == R.string.local_index_mi_restore) {
						new LocalIndexOperationTask(RESTORE_OPERATION).execute(info);
					} else if (resId == R.string.local_index_mi_delete) {
						Builder confirm = new AlertDialog.Builder(LocalIndexesActivity.this);
						confirm.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								new LocalIndexOperationTask(DELETE_OPERATION).execute(info);	
							}
						});
						confirm.setNegativeButton(R.string.default_buttons_no, null);
						confirm.setMessage(getString(R.string.delete_confirmation_msg, info.getFileName()));
						confirm.show();
					} else if (resId == R.string.local_index_mi_backup) {
						new LocalIndexOperationTask(BACKUP_OPERATION).execute(info);
					} else if (resId == R.string.local_index_mi_upload_gpx) {
						sendGPXFiles(info);
					}
				}

			});
		}
		builder.show();
	}
	
	private void renameFile(LocalIndexInfo info) {
		final File f = new File(info.getPathToData());
		Builder b = new AlertDialog.Builder(this);
		if(f.exists()){
			final EditText editText = new EditText(this);
			editText.setText(f.getName());
			b.setView(editText);
			b.setPositiveButton(R.string.default_buttons_save, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String newName = editText.getText().toString();
					File dest = new File(f.getParentFile(), newName);
					if (dest.exists()) {
						AccessibleToast.makeText(LocalIndexesActivity.this, R.string.file_with_name_already_exists, Toast.LENGTH_LONG).show();
					} else {
						if(f.renameTo(dest)){
							reloadIndexes();
						} else {
							AccessibleToast.makeText(LocalIndexesActivity.this, R.string.file_can_not_be_renamed, Toast.LENGTH_LONG).show();
						}
					}
					
				}
			});
			b.setNegativeButton(R.string.default_buttons_cancel, null);
			b.show();
		}
	}

	public class LoadLocalIndexTask extends AsyncTask<Activity, LocalIndexInfo, List<LocalIndexInfo>> {

		private List<LocalIndexInfo> result;

		@Override
		protected List<LocalIndexInfo> doInBackground(Activity... params) {
			LocalIndexHelper helper = new LocalIndexHelper(getMyApplication());
			return helper.getAllLocalIndexData(this);
		}

		public void loadFile(LocalIndexInfo... loaded) {
			publishProgress(loaded);
		}

		@Override
		protected void onPreExecute() {
			findViewById(R.id.ProgressBar).setVisibility(View.VISIBLE);
		}
		
		@Override
		protected void onProgressUpdate(LocalIndexInfo... values) {
			for (LocalIndexInfo v : values) {
				listAdapter.addLocalIndexInfo(v);
			}
			listAdapter.notifyDataSetChanged();
		}
		
		public void setResult(List<LocalIndexInfo> result) {
			this.result = result;
			if(result == null){
				listAdapter.clear();
			} else {
				for (LocalIndexInfo v : result) {
					listAdapter.addLocalIndexInfo(v);
				}
				listAdapter.notifyDataSetChanged();
				onPostExecute(result);
			}
		}

		@Override
		protected void onPostExecute(List<LocalIndexInfo> result) {
			this.result = result;
			findViewById(R.id.ProgressBar).setVisibility(View.GONE);
		}
		
		public List<LocalIndexInfo> getResult() {
			return result;
		}

	}
	
	private File getFileToRestore(LocalIndexInfo i){
		if(i.isBackupedData()){
			File parent = new File(i.getPathToData()).getParentFile();
			if(i.getType() == LocalIndexType.GPX_DATA){
				parent = settings.extendOsmandPath(ResourceManager.GPX_PATH);
			} else if(i.getType() == LocalIndexType.MAP_DATA){
				parent = settings.extendOsmandPath(ResourceManager.MAPS_PATH);
			} else if(i.getType() == LocalIndexType.POI_DATA){
				parent = settings.extendOsmandPath(ResourceManager.POI_PATH);
			} else if(i.getType() == LocalIndexType.TILES_DATA){
				parent = settings.extendOsmandPath(ResourceManager.TILES_PATH);
			} else if(i.getType() == LocalIndexType.VOICE_DATA){
				parent = settings.extendOsmandPath(ResourceManager.VOICE_PATH);
			} else if(i.getType() == LocalIndexType.TTS_VOICE_DATA){
				parent = settings.extendOsmandPath(ResourceManager.VOICE_PATH);
			}
			return new File(parent, i.getFileName());
		}
		return new File(i.getPathToData());
	}
	
	private File getFileToBackup(LocalIndexInfo i) {
		if(!i.isBackupedData()){
			return new File(settings.extendOsmandPath(ResourceManager.BACKUP_PATH), i.getFileName());
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
						successfull = Algoritms.removeAllFiles(f);
					} else if(operation == RESTORE_OPERATION){
						successfull = move(new File(info.getPathToData()), getFileToRestore(info));
						if(successfull){
							info.setBackupedData(false);
						}
					} else if(operation == BACKUP_OPERATION){
						successfull = move(new File(info.getPathToData()), getFileToBackup(info));
						if(successfull){
							info.setBackupedData(true);
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
				listAdapter.delete(values);
			} else if(operation == BACKUP_OPERATION){
				listAdapter.move(values, false);
			} else if(operation == RESTORE_OPERATION){
				listAdapter.move(values, true);
			}
			
		}
		
		@Override
		protected void onPreExecute() {
			findViewById(R.id.ProgressBar).setVisibility(View.VISIBLE);
		}

		@Override
		protected void onPostExecute(String result) {
			findViewById(R.id.ProgressBar).setVisibility(View.GONE);
			AccessibleToast.makeText(LocalIndexesActivity.this, result, Toast.LENGTH_LONG).show();
			listAdapter.clear();
			reloadIndexes();
			
		}
	}
	
	public enum UploadVisibility implements IEnumWithResource {
		Public(R.string.gpxup_public),
		Identifiable(R.string.gpxup_identifiable),
		Trackable(R.string.gpxup_trackable),
		Private(R.string.gpxup_private);
		private final int resourceId;

		private UploadVisibility(int resourceId) {
			this.resourceId = resourceId;
		}
		public String asURLparam() {
			return name().toLowerCase();
		}
		@Override
		public int stringResource() {
			return resourceId;
		}
	}
	
	public class UploadGPXFilesTask extends AsyncTask<LocalIndexInfo, String, String> {
		
		private final String visibility;
		private final String description;
		private final String tagstring;

		public UploadGPXFilesTask(String description, String tagstring, UploadVisibility visibility){
			this.description = description;
			this.tagstring = tagstring;
			this.visibility = visibility != null ? visibility.asURLparam() : UploadVisibility.Private.asURLparam();
			
		}

		@Override
		protected String doInBackground(LocalIndexInfo... params) {
			int count = 0;
			int total = 0;
			for (LocalIndexInfo info : params) {
				if (!isCancelled()) {
					String warning = null;
					File file = new File(info.getPathToData());
					// TODO should be plugin functionality and do not use remote util directly
					warning = new OpenstreetmapRemoteUtil(LocalIndexesActivity.this, null).uploadGPXFile(tagstring, description, visibility, file);
					total++;
					if (warning == null) {
						count++;
					} else {
						publishProgress(warning);
					}
				}
			}
			return getString(R.string.local_index_items_uploaded, count, total);
		}


		@Override
		protected void onProgressUpdate(String... values) {
			if (values.length > 0) {
				StringBuilder b = new StringBuilder();
				for (int i=0; i<values.length; i++) {
					if(i > 0){
						b.append("\n");
					}
					b.append(values[i]);
				}
				AccessibleToast.makeText(LocalIndexesActivity.this, b.toString(), Toast.LENGTH_LONG).show();
			}
		}
		
		@Override
		protected void onPreExecute() {
			findViewById(R.id.ProgressBar).setVisibility(View.VISIBLE);
		}

		@Override
		protected void onPostExecute(String result) {
			findViewById(R.id.ProgressBar).setVisibility(View.GONE);
			AccessibleToast.makeText(LocalIndexesActivity.this, result, Toast.LENGTH_LONG).show();
		}

	}

	public class LoadLocalIndexDescriptionTask extends AsyncTask<LocalIndexInfo, LocalIndexInfo, LocalIndexInfo[]> {

		@Override
		protected LocalIndexInfo[] doInBackground(LocalIndexInfo... params) {
			LocalIndexHelper helper = new LocalIndexHelper(getMyApplication());
			for (LocalIndexInfo i : params) {
				helper.updateDescription(i);
			}
			return params;
		}

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected void onProgressUpdate(LocalIndexInfo... values) {
			listAdapter.notifyDataSetChanged();
		}

		@Override
		protected void onPostExecute(LocalIndexInfo[] result) {
			listAdapter.notifyDataSetChanged();
		}

	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		LocalIndexInfo item = listAdapter.getChild(groupPosition, childPosition);
		item.setExpanded(!item.isExpanded());
		if (item.isExpanded()) {
			descriptionLoader = new LoadLocalIndexDescriptionTask();
			descriptionLoader.execute(item);
		}
		if(selectionMode){
			selectedItems.add(item);
		}
		listAdapter.notifyDataSetInvalidated();
		return true;
	}
	
	


	@Override
	protected void onPause() {
		super.onPause();
		if(operationTask != null){
			operationTask.cancel(true);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		asyncLoader.cancel(true);
		descriptionLoader.cancel(true);
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		if(asyncLoader != null){
			return asyncLoader.getResult();
		}
		return super.onRetainNonConfigurationInstance();
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, R.string.local_index_mi_backup, 0, getString(R.string.local_index_mi_backup)+"...");
		menu.add(0, R.string.local_index_mi_restore, 1, getString(R.string.local_index_mi_restore)+"...");
		menu.add(0, R.string.local_index_mi_delete, 2, getString(R.string.local_index_mi_delete)+"...");
		menu.add(0, R.string.local_index_mi_reload, 3, R.string.local_index_mi_reload);
//		if(OsmandPlugin.getEnabledPlugin(OsmandDevelopmentPlugin.class) instanceof OsmandDevelopmentPlugin) {
			menu.add(0, R.string.local_index_mi_upload_gpx, 4, getString(R.string.local_index_mi_upload_gpx)+"...");
//		}
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if(operationTask == null || operationTask.getStatus() == Status.FINISHED){
			menu.setGroupVisible(0, true);
		} else {
			menu.setGroupVisible(0, false);
		}
		return true;
	}
	
	public void doAction(int actionResId){
		if(actionResId == R.string.local_index_mi_backup){
			operationTask = new LocalIndexOperationTask(BACKUP_OPERATION);
		} else if(actionResId == R.string.local_index_mi_delete){
			operationTask = new LocalIndexOperationTask(DELETE_OPERATION);
		} else if(actionResId == R.string.local_index_mi_restore){
			operationTask = new LocalIndexOperationTask(RESTORE_OPERATION);
		} else if(actionResId == R.string.local_index_mi_upload_gpx){
			sendGPXFiles(selectedItems.toArray(new LocalIndexInfo[selectedItems.size()]));
			operationTask = null;
		} else {
			operationTask = null;
		}
		if(operationTask != null){
			operationTask.execute(selectedItems.toArray(new LocalIndexInfo[selectedItems.size()]));
		}
		closeSelectionMode();
	}
	
	private void collapseAllGroups() {
		for (int i = 0; i < listAdapter.getGroupCount(); i++) {
			getExpandableListView().collapseGroup(i);
		}

	}
	
	private void openSelectionMode(final int actionResId){
		String value = getString(actionResId);
		if (value.endsWith("...")) {
			value = value.substring(0, value.length() - 3);
		}
		final String actionButton = value;
		if(listAdapter.getGroupCount() == 0){
			listAdapter.cancelFilter();
			AccessibleToast.makeText(LocalIndexesActivity.this, getString(R.string.local_index_no_items_to_do, actionButton.toLowerCase()), Toast.LENGTH_SHORT).show();
			return;
		}
		collapseAllGroups();
		selectionMode = true;
		selectedItems.clear();
		Button action = (Button) findViewById(R.id.ActionButton);
		action.setVisibility(View.VISIBLE);
		action.setText(actionButton);
		action.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(selectedItems.isEmpty()){
					AccessibleToast.makeText(LocalIndexesActivity.this, getString(R.string.local_index_no_items_to_do, actionButton.toLowerCase()), Toast.LENGTH_SHORT).show();
					return;
				}
				
				Builder builder = new AlertDialog.Builder(LocalIndexesActivity.this);
				builder.setMessage(getString(R.string.local_index_action_do, actionButton.toLowerCase(), selectedItems.size()));
				builder.setPositiveButton(actionButton, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						doAction(actionResId);
					}
				});
				builder.setNegativeButton(R.string.default_buttons_cancel, null);
				builder.show();
				
			}
		});
		Button cancel = (Button) findViewById(R.id.CancelButton);
		cancel.setVisibility(View.VISIBLE);
		cancel.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				closeSelectionMode();
			}
		});
//		findViewById(R.id.DownloadButton).setVisibility(View.GONE);
		findViewById(R.id.FillLayoutStart).setVisibility(View.VISIBLE);
		findViewById(R.id.FillLayoutEnd).setVisibility(View.VISIBLE);
		findViewById(R.id.DescriptionText).setVisibility(View.GONE);
		if(R.string.local_index_mi_upload_gpx == actionResId){
			((TextView) findViewById(R.id.DescriptionTextTop)).setText(R.string.local_index_upload_gpx_description);
			((TextView) findViewById(R.id.DescriptionTextTop)).setVisibility(View.VISIBLE);
		}
		listAdapter.notifyDataSetChanged();
	}
	
	private void updateDescriptionTextWithSize(){
		File dir = getMyApplication().getSettings().extendOsmandPath("");
		String size = formatGb.format(new Object[]{0});
		if(dir.canRead()){
			StatFs fs = new StatFs(dir.getAbsolutePath());
			size = formatGb.format(new Object[]{(float) (fs.getAvailableBlocks()) * fs.getBlockSize() / (1 << 30) }); 
		}
		TextView ds = (TextView) findViewById(R.id.DescriptionText);
		String text = getString(R.string.download_link_and_local_description, size);
		int l = text.indexOf('.');
		if(l == -1) {
			l = text.length();
		}
		SpannableString content = new SpannableString(text);
		content.setSpan(new ClickableSpan() {
			@Override
			public void onClick(View widget) {
				asyncLoader.setResult(null);
				startActivity(new Intent(LocalIndexesActivity.this, DownloadIndexActivity.class));					
			}
			
			@Override
			public void updateDrawState(TextPaint ds) {
				super.updateDrawState(ds);
				ds.setColor(Color.GREEN);
			}
		}, 0, l, 0);
		ds.setText(content);
		ds.setMovementMethod(LinkMovementMethod.getInstance());
	}
	
	private void closeSelectionMode(){
		selectionMode = false;
//		findViewById(R.id.DownloadButton).setVisibility(View.VISIBLE);
		findViewById(R.id.DescriptionText).setVisibility(View.VISIBLE);
		findViewById(R.id.FillLayoutStart).setVisibility(View.GONE);
		findViewById(R.id.FillLayoutEnd).setVisibility(View.GONE);
		findViewById(R.id.CancelButton).setVisibility(View.GONE);
		findViewById(R.id.ActionButton).setVisibility(View.GONE);
		((TextView) findViewById(R.id.DescriptionTextTop)).setVisibility(View.GONE);
		
		
		;
		updateDescriptionTextWithSize();
		listAdapter.cancelFilter();
		collapseAllGroups();
		listAdapter.notifyDataSetChanged();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == R.string.local_index_mi_reload){
			reloadIndexes();
		} else if(item.getItemId() == R.string.local_index_mi_delete){
			openSelectionMode(R.string.local_index_mi_delete);
		} else if(item.getItemId() == R.string.local_index_mi_backup){
			listAdapter.filterCategories(false);
			listAdapter.filterCategories(LocalIndexType.MAP_DATA, LocalIndexType.POI_DATA);
			openSelectionMode(R.string.local_index_mi_backup);
		} else if(item.getItemId() == R.string.local_index_mi_restore){
			listAdapter.filterCategories(true);
			openSelectionMode(R.string.local_index_mi_restore);
		} else if(item.getItemId() == R.string.local_index_mi_upload_gpx){
			listAdapter.filterCategories(LocalIndexType.GPX_DATA);
			openSelectionMode(R.string.local_index_mi_upload_gpx);
		} else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}
	

	public void reloadIndexes() {
		listAdapter.clear();
		asyncLoader = new LoadLocalIndexTask();
		AsyncTask<Void, String, List<String>> task = new AsyncTask<Void, String, List<String>>(){

			@Override
			protected void onPostExecute(List<String> warnings) {
				findViewById(R.id.ProgressBar).setVisibility(View.GONE);
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
					AccessibleToast.makeText(LocalIndexesActivity.this, b.toString(), Toast.LENGTH_LONG).show();
				}
				if(asyncLoader.getStatus() == Status.PENDING) {
					asyncLoader.execute(LocalIndexesActivity.this);
				}
			}
			
			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				findViewById(R.id.ProgressBar).setVisibility(View.VISIBLE);
			}
			@Override
			protected List<String> doInBackground(Void... params) {
				return getMyApplication().getResourceManager().reloadIndexes(IProgress.EMPTY_PROGRESS);
			}
			
		};
		task.execute();
		
	}

	

	protected class LocalIndexesAdapter extends OsmandBaseExpandableListAdapter {
		
		Map<LocalIndexInfo, List<LocalIndexInfo>> data = new LinkedHashMap<LocalIndexInfo, List<LocalIndexInfo>>();
		List<LocalIndexInfo> category = new ArrayList<LocalIndexInfo>();
		List<LocalIndexInfo> filterCategory = null;
		

		public LocalIndexesAdapter() {
		}
		
		public void clear() {
			data.clear();
			category.clear();
			filterCategory = null;
			notifyDataSetChanged();
		}

		public LocalIndexInfo findCategory(LocalIndexInfo val, boolean backuped){
			for(LocalIndexInfo i : category){
				if(i.isBackupedData() == backuped && val.getType() == i.getType() ){
					return i;
				}
			}
			LocalIndexInfo newCat = new LocalIndexInfo(val.getType(), backuped);
			category.add(newCat);
			data.put(newCat, new ArrayList<LocalIndexInfo>());
			return newCat;
		}
		
		public void delete(LocalIndexInfo[] values) {
			for(LocalIndexInfo i : values){
				LocalIndexInfo c = findCategory(i, i.isBackupedData());
				if(c != null){
					data.get(c).remove(i);
				}
			}
			listAdapter.notifyDataSetChanged();
		}
		
		public void move(LocalIndexInfo[] values, boolean oldBackupState) {
			for(LocalIndexInfo i : values){
				LocalIndexInfo c = findCategory(i, oldBackupState);
				if(c != null){
					data.get(c).remove(i);
				}
				c = findCategory(i, !oldBackupState);
				if(c != null){
					data.get(c).add(i);
				}
			}
			listAdapter.notifyDataSetChanged();
		}

		public void cancelFilter(){
			filterCategory = null;
			notifyDataSetChanged();
		}
		
		public void filterCategories(LocalIndexType... types) {
			List<LocalIndexInfo> filter = new ArrayList<LocalIndexInfo>();
			List<LocalIndexInfo> source = filterCategory == null ? category : filterCategory;
			for (LocalIndexInfo info : source) {
				for (LocalIndexType ts : types) {
					if (info.getType() == ts) {
						filter.add(info);
					}
				}
			}
			filterCategory = filter;
			notifyDataSetChanged();
		}
		
		public void filterCategories(boolean backup) {
			List<LocalIndexInfo> filter = new ArrayList<LocalIndexInfo>();
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
				if (cat.getType() == info.getType() && info.isBackupedData() == cat.isBackupedData()) {
					found = i;
					break;
				}
			}
			if (found == -1) {
				found = category.size();
				category.add(new LocalIndexInfo(info.getType(), info.isBackupedData()));
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
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			View v = convertView;
			final LocalIndexInfo child = (LocalIndexInfo) getChild(groupPosition, childPosition);
			if (v == null ) {
				LayoutInflater inflater = getLayoutInflater();
				v = inflater.inflate(net.osmand.plus.R.layout.local_index_list_item, parent, false);
			}
			TextView viewName = ((TextView) v.findViewById(R.id.local_index_name));
			viewName.setText(child.getName());
			if (child.isNotSupported()) {
				viewName.setTextColor(getResources().getColor(R.color.localindex_notsupported));
			} else if (child.isCorrupted()) {
				viewName.setTextColor(getResources().getColor(R.color.localindex_iscorrupted));
			} else if (child.isLoaded()) {
				viewName.setTextColor(getResources().getColor(R.color.localindex_isloaded));
			} else {
				viewName.setTextColor(getResources().getColor(R.color.localindex_unknown));
			}
			if (child.getSize() >= 0) {
				String size;
				if (child.getSize() > 100) {
					size = formatMb.format(new Object[] { (float) child.getSize() / (1 << 10) });
				} else {
					size = child.getSize() + " Kb";
				}
				((TextView) v.findViewById(R.id.local_index_size)).setText(size);
			} else {
				((TextView) v.findViewById(R.id.local_index_size)).setText("");
			}
			TextView descr = ((TextView) v.findViewById(R.id.local_index_descr));
			if (child.isExpanded()) {
				descr.setVisibility(View.VISIBLE);
				descr.setText(child.getDescription());
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
						if(checkbox.isChecked()){
							selectedItems.add(child);
						} else {
							selectedItems.remove(child);
						}
					}
				});
			}
			

			return v;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			View v = convertView;
			LocalIndexInfo group = getGroup(groupPosition);
			if (v == null) {
				LayoutInflater inflater = getLayoutInflater();
				v = inflater.inflate(net.osmand.plus.R.layout.local_index_list_item_category, parent, false);
			}
			StringBuilder t = new StringBuilder(group.getType().getHumanString(LocalIndexesActivity.this));
			if (group.isBackupedData()) {
				t.append(" - ").append(getString(R.string.local_indexes_cat_backup));
			}
			adjustIndicator(groupPosition, isExpanded, v);
			TextView nameView = ((TextView) v.findViewById(R.id.local_index_category_name));
			List<LocalIndexInfo> list = data.get(group);
			int size = 0;
			for(int i=0; i<list.size(); i++){
				int sz = list.get(i).getSize();
				if(sz < 0){
					size = 0;
					break;
				} else {
					size += sz;
				}
			}
			size = size / (1 << 10);
			if(size > 0){
				t.append(" [").append(size).append(" MB]");
			}
			nameView.setText(t.toString());
			if (!group.isBackupedData()) {
				nameView.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
			} else {
				nameView.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
			}

			return v;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			LocalIndexInfo cat = filterCategory != null ? filterCategory.get(groupPosition) : category.get(groupPosition);
			return data.get(cat).size();
		}

		@Override
		public LocalIndexInfo getGroup(int groupPosition) {
			return filterCategory == null ?  category.get(groupPosition)  : filterCategory.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return filterCategory == null ?  category.size() : filterCategory.size();
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
	}
	
}
