package net.osmand.activities;

import static net.osmand.data.index.IndexConstants.ADDRESS_INDEX_EXT;
import static net.osmand.data.index.IndexConstants.ADDRESS_INDEX_EXT_ZIP;
import static net.osmand.data.index.IndexConstants.ADDRESS_TABLE_VERSION;
import static net.osmand.data.index.IndexConstants.BINARY_MAP_INDEX_EXT;
import static net.osmand.data.index.IndexConstants.BINARY_MAP_INDEX_EXT_ZIP;
import static net.osmand.data.index.IndexConstants.BINARY_MAP_VERSION;
import static net.osmand.data.index.IndexConstants.POI_INDEX_EXT;
import static net.osmand.data.index.IndexConstants.POI_INDEX_EXT_ZIP;
import static net.osmand.data.index.IndexConstants.POI_TABLE_VERSION;
import static net.osmand.data.index.IndexConstants.TRANSPORT_INDEX_EXT;
import static net.osmand.data.index.IndexConstants.TRANSPORT_INDEX_EXT_ZIP;
import static net.osmand.data.index.IndexConstants.TRANSPORT_TABLE_VERSION;
import static net.osmand.data.index.IndexConstants.VOICE_INDEX_EXT_ZIP;
import static net.osmand.data.index.IndexConstants.VOICE_VERSION;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.osmand.IProgress;
import net.osmand.LogUtil;
import net.osmand.ProgressDialogImplementation;
import net.osmand.R;
import net.osmand.ResourceManager;
import net.osmand.data.index.DownloaderIndexFromGoogleCode;
import net.osmand.data.index.IndexConstants;

import org.apache.commons.logging.Log;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class DownloadIndexActivity extends ListActivity {
	
	private final static Log log = LogUtil.getLog(DownloadIndexActivity.class);
	private static final int RELOAD_ID = 0;
	private static DownloadIndexListThread downloadListIndexThread = new DownloadIndexListThread();

	private ProgressDialog progressFileDlg = null;
	private ProgressDialog progressListDlg = null;
	private LinkedHashMap<String, DownloadEntry> entriesToDownload = new LinkedHashMap<String, DownloadEntry>();
	private TextWatcher textWatcher = new TextWatcher() {

        public void afterTextChanged(Editable s) {
        }

        public void beforeTextChanged(CharSequence s, int start, int count,
                int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before,
                int count) {
        	DownloadIndexAdapter adapter = ((DownloadIndexAdapter)getListAdapter());
        	if(adapter != null){
        		adapter.getFilter().filter(s);
        	}
        }

    };
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// recreation upon rotation is prevented in manifest file
		setContentView(R.layout.download_index);
		findViewById(R.id.DownloadButton).setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View v) {
				downloadFiles();
			}
			
		});

	    EditText filterText = (EditText) findViewById(R.id.search_box);
		filterText.addTextChangedListener(textWatcher);

		if(downloadListIndexThread.getCachedIndexFiles() != null){
			setListAdapter(new DownloadIndexAdapter(new ArrayList<Entry<String,String>>(downloadListIndexThread.getCachedIndexFiles().entrySet())));
		} else {
			downloadIndexList();
		}
	}

	private void downloadIndexList() {
		progressListDlg = ProgressDialog.show(this, getString(R.string.downloading), getString(R.string.downloading_list_indexes));
		progressListDlg.setCancelable(true);
		downloadListIndexThread.setUiActivity(this);
		if(downloadListIndexThread.getState() == Thread.State.NEW){
			downloadListIndexThread.start();
		} else if(downloadListIndexThread.getState() == Thread.State.TERMINATED){
			// possibly exception occurred we don't have cache of files
			downloadListIndexThread = new DownloadIndexListThread();
			downloadListIndexThread.setUiActivity(this);
			downloadListIndexThread.start();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem item = menu.add(0, RELOAD_ID, 0, R.string.reload);
		item.setIcon(R.drawable.ic_menu_refresh);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == RELOAD_ID){
			//re-create the thread
			downloadListIndexThread = new DownloadIndexListThread();
			downloadIndexList();
		} else {
			return false;
		}
		return true;
	}
	
	private static class DownloadIndexListThread extends Thread {
		private DownloadIndexActivity uiActivity = null;
		private Map<String, String> indexFiles = null; 
		
		public DownloadIndexListThread(){
			super("DownloadIndexes"); //$NON-NLS-1$
		}
		public void setUiActivity(DownloadIndexActivity uiActivity) {
			this.uiActivity = uiActivity;
		}
		
		public Map<String, String> getCachedIndexFiles() {
			return indexFiles;
		}
		
		@Override
		public void run() {
			indexFiles = downloadIndex();
			if(uiActivity != null &&  uiActivity.progressListDlg != null){
				uiActivity.progressListDlg.dismiss();
				uiActivity.progressListDlg = null;
				uiActivity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (indexFiles != null) {
							uiActivity.setListAdapter(uiActivity.new DownloadIndexAdapter(new ArrayList<Entry<String,String>>(indexFiles.entrySet())));
						} else {
							Toast.makeText(uiActivity, R.string.list_index_files_was_not_loaded, Toast.LENGTH_LONG).show();
						}
					}
				});
			}
		}
	}
	
	
	

	protected static Map<String, String> downloadIndex(){
		try {
			log.debug("Start loading list of index files"); //$NON-NLS-1$
			TreeMap<String, String> indexFiles = new TreeMap<String, String>(new Comparator<String>(){
				private static final long serialVersionUID = 1L;

				@Override
				public int compare(String object1, String object2) {
					if(object1.endsWith(IndexConstants.VOICE_INDEX_EXT_ZIP)){
						if(object2.endsWith(IndexConstants.VOICE_INDEX_EXT_ZIP)){
							return object1.compareTo(object2);
						} else {
							return -1;
						}
					} else if(object2.endsWith(IndexConstants.VOICE_INDEX_EXT_ZIP)){
						return 1;
					}
					return object1.compareTo(object2);
				}
				
			});
			DownloaderIndexFromGoogleCode.getIndexFiles(indexFiles,
					ADDRESS_TABLE_VERSION + ADDRESS_INDEX_EXT,
					ADDRESS_TABLE_VERSION + ADDRESS_INDEX_EXT_ZIP,
					POI_TABLE_VERSION + POI_INDEX_EXT,
					POI_TABLE_VERSION + POI_INDEX_EXT_ZIP,
					TRANSPORT_TABLE_VERSION + TRANSPORT_INDEX_EXT,
					TRANSPORT_TABLE_VERSION + TRANSPORT_INDEX_EXT_ZIP,
					BINARY_MAP_VERSION + BINARY_MAP_INDEX_EXT,
					BINARY_MAP_VERSION + BINARY_MAP_INDEX_EXT_ZIP,
					VOICE_VERSION + VOICE_INDEX_EXT_ZIP);
			
			if (indexFiles != null && !indexFiles.isEmpty()) {
				return indexFiles;
			} else {
				return null;
			}
		} catch (RuntimeException e) {
			log.error("Error while loading indexes from repository", e); //$NON-NLS-1$
			return null;
		}
	}
	
	private final static int MB = 1 << 20;

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		final Entry<String, String> e = ((DownloadIndexAdapter)getListAdapter()).getItem(position);
		String key = e.getKey();
		final CheckBox ch = (CheckBox) v.findViewById(R.id.check_download_item);
		
		if(ch.isChecked()){
			ch.setChecked(!ch.isChecked());
			entriesToDownload.remove(key);
			if(entriesToDownload.isEmpty()){
				int x = getListView().getScrollX();
				int y = getListView().getScrollY();
				findViewById(R.id.DownloadButton).setVisibility(View.GONE);
				getListView().scrollTo(x, y);
			}
			return;
		}
		
		
		int ls = e.getKey().lastIndexOf('_');
		final String baseName = e.getKey().substring(0, ls);
		
		File parent = null;
		String toSavePostfix = null;
		String toCheckPostfix = null;
		boolean unzipDir = false;
		if(key.endsWith(IndexConstants.ADDRESS_INDEX_EXT)){
			parent = new File(Environment.getExternalStorageDirectory(), ResourceManager.ADDRESS_PATH);
			toSavePostfix = IndexConstants.ADDRESS_INDEX_EXT;
			toCheckPostfix = IndexConstants.ADDRESS_INDEX_EXT;
		} else if(key.endsWith(IndexConstants.ADDRESS_INDEX_EXT_ZIP)){
			parent = new File(Environment.getExternalStorageDirectory(), ResourceManager.ADDRESS_PATH);
			toSavePostfix = IndexConstants.ADDRESS_INDEX_EXT_ZIP;
			toCheckPostfix = IndexConstants.ADDRESS_INDEX_EXT;
		} else if(key.endsWith(IndexConstants.POI_INDEX_EXT)){
			parent = new File(Environment.getExternalStorageDirectory(), ResourceManager.POI_PATH);
			toSavePostfix = IndexConstants.POI_INDEX_EXT;
			toCheckPostfix = IndexConstants.POI_INDEX_EXT;
		} else if(key.endsWith(IndexConstants.POI_INDEX_EXT_ZIP)){
			parent = new File(Environment.getExternalStorageDirectory(), ResourceManager.POI_PATH);
			toSavePostfix = IndexConstants.POI_INDEX_EXT_ZIP;
			toCheckPostfix = IndexConstants.POI_INDEX_EXT;
		} else if(key.endsWith(IndexConstants.TRANSPORT_INDEX_EXT)){
			parent = new File(Environment.getExternalStorageDirectory(), ResourceManager.TRANSPORT_PATH);
			toSavePostfix = IndexConstants.TRANSPORT_INDEX_EXT;
			toCheckPostfix = IndexConstants.TRANSPORT_INDEX_EXT;
		} else if(key.endsWith(IndexConstants.TRANSPORT_INDEX_EXT_ZIP)){
			parent = new File(Environment.getExternalStorageDirectory(), ResourceManager.TRANSPORT_PATH);
			toSavePostfix = IndexConstants.TRANSPORT_INDEX_EXT_ZIP;
			toCheckPostfix = IndexConstants.TRANSPORT_INDEX_EXT;
		} else if(key.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)){
			parent = new File(Environment.getExternalStorageDirectory(), ResourceManager.APP_DIR);
			toSavePostfix = IndexConstants.BINARY_MAP_INDEX_EXT;
			toCheckPostfix = IndexConstants.BINARY_MAP_INDEX_EXT;
		} else if(key.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP)){
			parent = new File(Environment.getExternalStorageDirectory(), ResourceManager.APP_DIR);
			toSavePostfix = IndexConstants.BINARY_MAP_INDEX_EXT_ZIP;
			toCheckPostfix = IndexConstants.BINARY_MAP_INDEX_EXT;
		} else if(key.endsWith(IndexConstants.VOICE_INDEX_EXT_ZIP)){
			parent = new File(Environment.getExternalStorageDirectory(), ResourceManager.VOICE_PATH);
			toSavePostfix = IndexConstants.VOICE_INDEX_EXT_ZIP;
			toCheckPostfix = ""; //$NON-NLS-1$
			unzipDir = true;
		}
		if(parent != null){
			parent.mkdirs();
		}
		if(parent == null || !parent.exists()){
			Toast.makeText(DownloadIndexActivity.this, getString(R.string.sd_dir_not_accessible), Toast.LENGTH_LONG).show();
		} else {
			final DownloadEntry entry = new DownloadEntry();
			entry.fileToSave = new File(parent, baseName + toSavePostfix);
			entry.unzip = unzipDir;
			entry.fileToUnzip = new File(parent, baseName + toCheckPostfix);
			// if(!fileToUnzip.exists()){
			// builder.setMessage(MessageFormat.format(getString(R.string.download_question), baseName, e.getValue()));
			if (entry.fileToUnzip.exists()) {
				Builder builder = new AlertDialog.Builder(this);
				MessageFormat format;
				if (entry.fileToUnzip.isDirectory()) {
					format = new MessageFormat("{0,date,dd.MM.yyyy}", Locale.US); //$NON-NLS-1$
				} else {
					format = new MessageFormat("{0,date,dd.MM.yyyy} : {1, number,##.#} MB", Locale.US); //$NON-NLS-1$
				}
				String description = format
						.format(new Object[] { new Date(entry.fileToUnzip.lastModified()), ((float) entry.fileToUnzip.length() / MB) });
				builder.setMessage(MessageFormat.format(getString(R.string.download_question_exist), baseName, description, e.getValue()));
				
				builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						entriesToDownload.put(e.getKey(), entry);
						int x = getListView().getScrollX();
						int y = getListView().getScrollY();
						findViewById(R.id.DownloadButton).setVisibility(View.VISIBLE);
						getListView().scrollTo(x, y);
						ch.setChecked(!ch.isChecked());
					}
				});
				builder.setNegativeButton(R.string.default_buttons_no, null);
				builder.show();
			} else {
				entriesToDownload.put(e.getKey(), entry);
				int x = getListView().getScrollX();
				int y = getListView().getScrollY();
				findViewById(R.id.DownloadButton).setVisibility(View.VISIBLE);
				getListView().scrollTo(x, y);
				ch.setChecked(!ch.isChecked());
			}
				
		}
		
	}
	
	protected void downloadFiles() {
		Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(MessageFormat.format(getString(R.string.download_files_question), entriesToDownload.size()));
		builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				progressFileDlg = ProgressDialog.show(DownloadIndexActivity.this, getString(R.string.downloading),
						getString(R.string.downloading_file), true, true);
				interruptDownloading = false;
				progressFileDlg.show();
				final ProgressDialogImplementation impl = new ProgressDialogImplementation(progressFileDlg, true);
				progressFileDlg.setOnCancelListener(new DialogInterface.OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
						interruptDownloading = true;
					}
				});

				impl.setRunnable("DownloadIndex", new Runnable() { //$NON-NLS-1$
							@Override
							public void run() {
								try {
									for (String s : new ArrayList<String>(entriesToDownload.keySet())) {
										DownloadEntry entry = entriesToDownload.get(s);
										if (entry != null) {
											if (downloadFile(s, entry.fileToSave, entry.fileToUnzip, entry.unzip, impl)) {
												entriesToDownload.remove(s);
												runOnUiThread(new Runnable() {
													@Override
													public void run() {
														((DownloadIndexAdapter) getListAdapter()).notifyDataSetChanged();
														findViewById(R.id.DownloadButton).setVisibility(entriesToDownload.isEmpty() ? View.GONE : View.VISIBLE);
													}
												});
											}
										}
									}
								} catch (InterruptedException e) {
									// do not dismiss dialog
									progressFileDlg = null;
								} finally {
									if (progressFileDlg != null) {
										progressFileDlg.dismiss();
										progressFileDlg = null;
									}
								}
							}
						});
				impl.run();
			}
		});
		builder.setNegativeButton(R.string.default_buttons_no, null);
		builder.show();
	}
	
	private static final int BUFFER_SIZE = 32256; 
	
	private static class DownloadEntry {
		public File fileToSave;
		public File fileToUnzip;
		public boolean unzip;
		
	}
	
	protected final int TRIES_TO_DOWNLOAD = 20;
	protected final long TIMEOUT_BETWEEN_DOWNLOADS = 8000;
	private boolean interruptDownloading = false;
	
	protected void downloadFile(String fileName, FileOutputStream out, URL url, IProgress progress) throws IOException, InterruptedException {
		InputStream is = null;
		
		byte[] buffer = new byte[BUFFER_SIZE];
		int read = 0;
		int length = 0;
		int fileread = 0;
		int triesDownload = TRIES_TO_DOWNLOAD;
		boolean first = true;
		try {
			while (triesDownload > 0) {
				try {
					if (!first) {
						log.info("Reconnecting"); //$NON-NLS-1$
						try {
							Thread.sleep(TIMEOUT_BETWEEN_DOWNLOADS);
						} catch (InterruptedException e) {
						}
					}
					URLConnection conn = url.openConnection();
					conn.setReadTimeout(30000);
					conn.setConnectTimeout(30000);
					is = conn.getInputStream();
					long skipped = 0;
					while (skipped < fileread) {
						skipped += is.skip(fileread - skipped);
					}
					if (first) {
						length = conn.getContentLength();
						progress.startTask(getString(R.string.downloading_file) + " " + fileName, length); //$NON-NLS-1$
					}

					first = false;
					while ((read = is.read(buffer)) != -1) {
						 if(interruptDownloading){
						 	throw new InterruptedException();
						 }
						out.write(buffer, 0, read);
						progress.progress(read);
						fileread += read;
					}
					triesDownload = 0;
				} catch (IOException e) {
					log.error("IOException", e); //$NON-NLS-1$
					triesDownload--;
				}

			}
		} finally {
			if (is != null) {
				is.close();
			}
		}
		if(length != fileread){
			throw new IOException("File was not fully read"); //$NON-NLS-1$
		}
		
	}

	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(isFinishing()){
			interruptDownloading = true;
		}
	    if (textWatcher != null) {
	    	EditText filterText = (EditText) findViewById(R.id.search_box);
	    	filterText.removeTextChangedListener(textWatcher);
	    }
		downloadListIndexThread.setUiActivity(null);
		progressFileDlg = null;
	}
	
	protected boolean downloadFile(final String key, final File fileToDownload, final File fileToUnZip, final boolean unzipToDir,
			IProgress progress) throws InterruptedException {
		FileOutputStream out = null;
		try {

			out = new FileOutputStream(fileToDownload);
			URL url = DownloaderIndexFromGoogleCode.getInputStreamToLoadIndex(key);
			try {
				downloadFile(key, out, url, progress);
			} finally {
				out.close();
				out = null;
			}

			File toIndex = fileToDownload;
			if (fileToDownload.getName().endsWith(".zip")) { //$NON-NLS-1$
				progress.startTask(getString(R.string.unzipping_file), -1);
				if (!unzipToDir) {
					toIndex = fileToUnZip;
				} else {
					fileToUnZip.mkdirs();
				}
				ZipInputStream zipIn = new ZipInputStream(new FileInputStream(fileToDownload));
				ZipEntry entry = null;
				while ((entry = zipIn.getNextEntry()) != null) {
					if (!unzipToDir) {
						String name = entry.getName();
						// small simplification
						int ind = name.lastIndexOf('_');
						if (ind > 0) {
							// cut version
							int i = name.indexOf('.', ind);
							if (i > 0) {
								name = name.substring(0, ind) + name.substring(i, name.length());
							}
						}
						out = new FileOutputStream(new File(fileToUnZip.getParent(), name));
					} else {
						out = new FileOutputStream(new File(fileToUnZip, entry.getName()));
					}
					int read;
					byte[] buffer = new byte[BUFFER_SIZE];
					while ((read = zipIn.read(buffer)) != -1) {
						out.write(buffer, 0, read);
					}
					out.close();
				}
				zipIn.close();
				fileToDownload.delete(); // zip is no needed more
			}

			ArrayList<String> warnings = new ArrayList<String>();
			ResourceManager manager = ((OsmandApplication) getApplication()).getResourceManager();
			if (toIndex.getName().endsWith(IndexConstants.ADDRESS_INDEX_EXT)) {
				manager.indexingAddress(progress, warnings, toIndex);
			} else if (toIndex.getName().endsWith(IndexConstants.POI_INDEX_EXT)) {
				manager.indexingPoi(progress, warnings, toIndex);
			} else if (toIndex.getName().endsWith(IndexConstants.TRANSPORT_INDEX_EXT)) {
				manager.indexingTransport(progress, warnings, toIndex);
			} else if (toIndex.getName().endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)) {
				manager.indexingMaps(progress);
			} else if (toIndex.getName().endsWith(IndexConstants.VOICE_INDEX_EXT_ZIP)) {
			}
			if (warnings.isEmpty()) {
				showWarning(getString(R.string.download_index_success));
			} else {
				showWarning(warnings.get(0));
			}
			return true;
		} catch (IOException e) {
			log.error("Exception ocurred", e); //$NON-NLS-1$
			showWarning(getString(R.string.error_io_error));
			if(out != null){
				try {
					out.close();
				} catch (IOException e1) {
				}
			}
			// Possibly file is corrupted
			fileToDownload.delete();
			return false;
		} catch (InterruptedException e) {
			// Possibly file is corrupted
			fileToDownload.delete();
			throw e;
		}
	}
	
	public void showWarning(final String messages){
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				Toast.makeText(DownloadIndexActivity.this, messages, Toast.LENGTH_LONG).show();
			}
			
		});
	}


	protected class DownloadIndexAdapter extends ArrayAdapter<Entry<String, String>> implements Filterable {

		List<Entry<String, String>> origArray;
		private List<Entry<String, String>> origReference;
		private DownloadIndexFilter myFilter;

		public DownloadIndexAdapter(List<Entry<String, String>> array) {
			super(DownloadIndexActivity.this, net.osmand.R.layout.download_index_list_item, array);
			this.origArray = new ArrayList<Entry<String,String>>(array);
			this.origReference = array;
		}
		
		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater inflater = getLayoutInflater();
				v = inflater.inflate(net.osmand.R.layout.download_index_list_item, parent, false);
			}
			final View row = v; 
			TextView item = (TextView) row.findViewById(R.id.download_item);
			TextView description = (TextView) row.findViewById(R.id.download_descr);
			Entry<String, String> e = getItem(position);
			int l = e.getKey().lastIndexOf('_');
			String s = ""; //$NON-NLS-1$
			if(e.getKey().endsWith(IndexConstants.POI_INDEX_EXT) || e.getKey().endsWith(IndexConstants.POI_INDEX_EXT_ZIP)){
				s = getString(R.string.poi);
			} else if(e.getKey().endsWith(IndexConstants.ADDRESS_INDEX_EXT) || e.getKey().endsWith(IndexConstants.ADDRESS_INDEX_EXT_ZIP)){
				s = getString(R.string.address);
			} else if(e.getKey().endsWith(IndexConstants.TRANSPORT_INDEX_EXT) || e.getKey().endsWith(IndexConstants.TRANSPORT_INDEX_EXT_ZIP)){
				s = getString(R.string.transport);
			} else if(e.getKey().endsWith(IndexConstants.BINARY_MAP_INDEX_EXT) || e.getKey().endsWith(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP)){
				s = getString(R.string.map_index);
			} else if(e.getKey().endsWith(IndexConstants.VOICE_INDEX_EXT_ZIP)){
				s = getString(R.string.voice);
			}
			String name = e.getKey().substring(0, l).replace('_', ' ');
			if(e.getKey().endsWith(".zip")){ //$NON-NLS-1$
				name += " (zip)"; //$NON-NLS-1$
			}
			CheckBox ch = (CheckBox) row.findViewById(R.id.check_download_item);
			ch.setChecked(entriesToDownload.containsKey(e.getKey()));
			ch.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final CheckBox ch = (CheckBox) v.findViewById(R.id.check_download_item);
					ch.setChecked(!ch.isChecked());
					DownloadIndexActivity.this.onListItemClick(getListView(), row, position, getItemId(position));
				}
			});
			item.setText(s.trim() + "\n " + name); //$NON-NLS-1$
			description.setText(e.getValue().replace(':', '\n').trim());
			return row;
		}
		
		@Override
		public Filter getFilter() {
			if (myFilter == null) {
				myFilter = new DownloadIndexFilter();
			}
			return myFilter;
		}
		
		private final class DownloadIndexFilter extends Filter {
			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				FilterResults results = new FilterResults();
				if (constraint == null || constraint.length() == 0) {
					results.values = origArray;
					results.count = origArray.size();
				} else {
					CharSequence lowerCase = constraint.toString()
							.toLowerCase();
					List<Entry<String, String>> filter = new ArrayList<Entry<String, String>>();
					for (Entry<String, String> item : origArray) {
						if (item.getKey().toLowerCase().contains(lowerCase)) {
							filter.add(item);
						}
					}
					results.values = filter;
					results.count = filter.size();
				}
				return results;
			}

			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(CharSequence constraint,
					FilterResults results) {
				synchronized (origReference) {
					origReference.clear();
					origReference.addAll((List<Entry<String, String>>) results.values);
				}
				if (results.count > 0) {
					notifyDataSetChanged();
				} else {
					notifyDataSetInvalidated();
				}

			}
		}
	}
	

}
