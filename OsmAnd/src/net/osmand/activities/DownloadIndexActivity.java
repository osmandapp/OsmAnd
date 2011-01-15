package net.osmand.activities;

import static net.osmand.data.index.IndexConstants.BINARY_MAP_INDEX_EXT;
import static net.osmand.data.index.IndexConstants.BINARY_MAP_INDEX_EXT_ZIP;
import static net.osmand.data.index.IndexConstants.BINARY_MAP_VERSION;
import static net.osmand.data.index.IndexConstants.POI_INDEX_EXT;
import static net.osmand.data.index.IndexConstants.POI_INDEX_EXT_ZIP;
import static net.osmand.data.index.IndexConstants.POI_TABLE_VERSION;
import static net.osmand.data.index.IndexConstants.VOICE_INDEX_EXT_ZIP;
import static net.osmand.data.index.IndexConstants.VOICE_VERSION;
import static net.osmand.data.index.IndexConstants.ADDRESS_INDEX_EXT;
import static net.osmand.data.index.IndexConstants.ADDRESS_INDEX_EXT_ZIP;
import static net.osmand.data.index.IndexConstants.ADDRESS_TABLE_VERSION;
import static net.osmand.data.index.IndexConstants.TRANSPORT_INDEX_EXT;
import static net.osmand.data.index.IndexConstants.TRANSPORT_INDEX_EXT_ZIP;
import static net.osmand.data.index.IndexConstants.TRANSPORT_TABLE_VERSION;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
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
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Color;
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
	private static final int SELECT_ALL_ID = 1;
	private static final int DESELECT_ALL_ID = 2;
	private static final int FILTER_EXISTING_REGIONS = 3;
	
	private static final boolean USE_DOWNLOAD_OSMAND_NET = true;
	private static DownloadIndexListThread downloadListIndexThread = new DownloadIndexListThread();

	private ProgressDialog progressFileDlg = null;
	private ProgressDialog progressListDlg = null;
	private Map<String, String> indexFileNames = null;
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
		
		indexFileNames = ((OsmandApplication)getApplication()).getResourceManager().getIndexFileNames();

	    EditText filterText = (EditText) findViewById(R.id.search_box);
		filterText.addTextChangedListener(textWatcher);

		if(downloadListIndexThread.getCachedIndexFiles() != null){
			setListAdapter(new DownloadIndexAdapter(downloadListIndexThread.getCachedIndexFiles()));
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
		/*MenuItem item = */menu.add(0, RELOAD_ID, 0, R.string.reload);
		// item.setIcon(R.drawable.ic_menu_refresh);
		/*item = */menu.add(0, SELECT_ALL_ID, 0, R.string.select_all);
		/*item = */menu.add(0, DESELECT_ALL_ID, 0, R.string.deselect_all);
		/*item = */menu.add(0, FILTER_EXISTING_REGIONS, 0, R.string.filter_existing_indexes);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == RELOAD_ID){
			//re-create the thread
			downloadListIndexThread = new DownloadIndexListThread();
			downloadIndexList();
		} else {
			final DownloadIndexAdapter listAdapter = (DownloadIndexAdapter)getListAdapter();
			if(item.getItemId() == SELECT_ALL_ID){
				int selected = 0;
				for (int i = 0; i < listAdapter.getCount(); i++) {
					IndexItem es = listAdapter.getItem(i);
					if(!entriesToDownload.containsKey(es.getFileName())){
						selected++;
						entriesToDownload.put(es.getFileName(), createDownloadEntry(es.getFileEntry()));
					}
				}
				Toast.makeText(this, MessageFormat.format(getString(R.string.items_were_selected), selected), Toast.LENGTH_SHORT).show();
				listAdapter.notifyDataSetInvalidated();
				if(selected > 0){
					findViewById(R.id.DownloadButton).setVisibility(View.VISIBLE);
				}
			} else if(item.getItemId() == FILTER_EXISTING_REGIONS){
				final List<String> listAlreadyDownloaded = listAlreadyDownloadedWithAlternatives();
				final List<IndexItem> filtered = new ArrayList<DownloadIndexActivity.IndexItem>();
				for(String file : listAlreadyDownloaded) {
					IndexItem fileItem = listAdapter.getIndexFiles().get(file);
					if (fileItem != null) {
						filtered.add(fileItem);
					}
				}
				listAdapter.clear();
				for (IndexItem fileItem : filtered) {
					listAdapter.add(fileItem);
				}
			} else if(item.getItemId() == DESELECT_ALL_ID){
				entriesToDownload.clear();
				listAdapter.notifyDataSetInvalidated();
				findViewById(R.id.DownloadButton).setVisibility(View.GONE);
			} else {
				return false;
			}
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
			indexFiles = downloadIndexesListFromInternet();
			if(uiActivity != null &&  uiActivity.progressListDlg != null){
				uiActivity.progressListDlg.dismiss();
				uiActivity.progressListDlg = null;
				uiActivity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (indexFiles != null) {
							uiActivity.setListAdapter(uiActivity.new DownloadIndexAdapter(indexFiles));
						} else {
							Toast.makeText(uiActivity, R.string.list_index_files_was_not_loaded, Toast.LENGTH_LONG).show();
						}
					}
				});
			}
		}
	}
	
	
	

	protected static Map<String, String> downloadIndexesListFromInternet(){
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
			if(USE_DOWNLOAD_OSMAND_NET){
				try {
					URL url = new URL("http://download.osmand.net/indexes.xml"); //$NON-NLS-1$
					XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
					parser.setInput(url.openStream(), "UTF-8"); //$NON-NLS-1$
					int next;
					while((next = parser.next()) != XmlPullParser.END_DOCUMENT) {
						if(next == XmlPullParser.START_TAG && parser.getName().equals("region")) { //$NON-NLS-1$
							String name = parser.getAttributeValue(null, "name"); //$NON-NLS-1$
							String size = parser.getAttributeValue(null, "size"); //$NON-NLS-1$
							String date = parser.getAttributeValue(null, "date"); //$NON-NLS-1$
							String description = parser.getAttributeValue(null, "description"); //$NON-NLS-1$
							indexFiles.put(name, description + " {"+date +" : " + size +" MB }");  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
						}
					}
				} catch (IOException e) {
					log.error("Error while loading indexes from repository", e); //$NON-NLS-1$
					return null;
				} catch (XmlPullParserException e) {
					log.error("Error while loading indexes from repository", e); //$NON-NLS-1$
					return null;
				}
			} else {
				String[] accepted = new String[] { POI_TABLE_VERSION + POI_INDEX_EXT, POI_TABLE_VERSION + POI_INDEX_EXT_ZIP,
						BINARY_MAP_VERSION + BINARY_MAP_INDEX_EXT, BINARY_MAP_VERSION + BINARY_MAP_INDEX_EXT_ZIP,
						VOICE_VERSION + VOICE_INDEX_EXT_ZIP };
				DownloaderIndexFromGoogleCode.getIndexFiles(indexFiles,
						accepted);
			}
			
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
		final IndexItem e = ((DownloadIndexAdapter)getListAdapter()).getItem(position);
		String key = e.getFileName();
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
		
		
		
		
		final DownloadEntry entry = createDownloadEntry(e.getFileEntry());
		if (entry != null) {
			// if(!fileToUnzip.exists()){
			// builder.setMessage(MessageFormat.format(getString(R.string.download_question), baseName, extractDateAndSize(e.getValue())));
			if (entry.fileToUnzip.exists()) {
				Builder builder = new AlertDialog.Builder(this);
				MessageFormat format;
				if (entry.fileToUnzip.isDirectory()) {
					format = new MessageFormat("{0,date,dd.MM.yyyy}", Locale.US); //$NON-NLS-1$
				} else {
					format = new MessageFormat("{0,date,dd.MM.yyyy} : {1, number,##.#} MB", Locale.US); //$NON-NLS-1$
				}
				String description = format.format(new Object[] { new Date(entry.fileToUnzip.lastModified()),
						((float) entry.fileToUnzip.length() / MB) });
				builder.setMessage(MessageFormat.format(getString(R.string.download_question_exist), entry.baseName, description,
						extractDateAndSize(e.getFileEntry().getValue())));

				builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						entriesToDownload.put(e.getFileName(), entry);
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
				entriesToDownload.put(e.getFileName(), entry);
				int x = getListView().getScrollX();
				int y = getListView().getScrollY();
				findViewById(R.id.DownloadButton).setVisibility(View.VISIBLE);
				getListView().scrollTo(x, y);
				ch.setChecked(!ch.isChecked());
			}

		}
		
	}

	private List<String> listAlreadyDownloadedWithAlternatives()
	{
		List<String> files = new ArrayList<String>();
		File externalStorageDirectory = Environment.getExternalStorageDirectory();
		files.addAll(listWithAlternatives(new File(externalStorageDirectory, ResourceManager.ADDRESS_PATH),ADDRESS_INDEX_EXT,ADDRESS_INDEX_EXT_ZIP,ADDRESS_TABLE_VERSION));
		files.addAll(listWithAlternatives(new File(externalStorageDirectory, ResourceManager.POI_PATH),POI_INDEX_EXT,POI_INDEX_EXT_ZIP,POI_TABLE_VERSION));
		files.addAll(listWithAlternatives(new File(externalStorageDirectory, ResourceManager.TRANSPORT_PATH),TRANSPORT_INDEX_EXT,TRANSPORT_INDEX_EXT_ZIP,TRANSPORT_TABLE_VERSION));
		files.addAll(listWithAlternatives(new File(externalStorageDirectory, ResourceManager.APP_DIR),BINARY_MAP_INDEX_EXT,BINARY_MAP_INDEX_EXT_ZIP,BINARY_MAP_VERSION));
		files.addAll(listWithAlternatives(new File(externalStorageDirectory, ResourceManager.VOICE_PATH),"",VOICE_INDEX_EXT_ZIP, VOICE_VERSION));
		return files;
	}
	
	private Collection<? extends String> listWithAlternatives(File file, final String ext, final String secondaryExt, final int version) {
		final List<String> files = new ArrayList<String>();
		if (file.isDirectory()) {
			file.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					if (filename.endsWith(ext)) {
						files.add(filename);
						files.add(filename.substring(0,filename.length()-ext.length())+"_"+version+ext);
						if (secondaryExt != null) {
							files.add(filename.substring(0,filename.length()-ext.length())+"_"+version+secondaryExt);
						}
					}
					return filename.endsWith(ext);
				}
			});

		}
		return files;
	}

	private DownloadEntry createDownloadEntry(final Entry<String, String> e) {
		String key = e.getKey();
		String value = e.getValue();
		File parent = null;
		String toSavePostfix = null;
		String toCheckPostfix = null;
		boolean unzipDir = false;
		File externalStorageDirectory = Environment.getExternalStorageDirectory();
		if(key.endsWith(ADDRESS_INDEX_EXT)){
			parent = new File(externalStorageDirectory, ResourceManager.ADDRESS_PATH);
			toSavePostfix = ADDRESS_INDEX_EXT;
			toCheckPostfix = ADDRESS_INDEX_EXT;
		} else if(key.endsWith(IndexConstants.ADDRESS_INDEX_EXT_ZIP)){
			parent = new File(externalStorageDirectory, ResourceManager.ADDRESS_PATH);
			toSavePostfix = ADDRESS_INDEX_EXT_ZIP;
			toCheckPostfix = ADDRESS_INDEX_EXT;
		} else if(key.endsWith(IndexConstants.POI_INDEX_EXT)){
			parent = new File(externalStorageDirectory, ResourceManager.POI_PATH);
			toSavePostfix = POI_INDEX_EXT;
			toCheckPostfix = POI_INDEX_EXT;
		} else if(key.endsWith(IndexConstants.POI_INDEX_EXT_ZIP)){
			parent = new File(externalStorageDirectory, ResourceManager.POI_PATH);
			toSavePostfix = POI_INDEX_EXT_ZIP;
			toCheckPostfix = POI_INDEX_EXT;
		} else if(key.endsWith(IndexConstants.TRANSPORT_INDEX_EXT)){
			parent = new File(externalStorageDirectory, ResourceManager.TRANSPORT_PATH);
			toSavePostfix = TRANSPORT_INDEX_EXT;
			toCheckPostfix = TRANSPORT_INDEX_EXT;
		} else if(key.endsWith(IndexConstants.TRANSPORT_INDEX_EXT_ZIP)){
			parent = new File(externalStorageDirectory, ResourceManager.TRANSPORT_PATH);
			toSavePostfix = TRANSPORT_INDEX_EXT_ZIP;
			toCheckPostfix = TRANSPORT_INDEX_EXT;
		} else if(key.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)){
			parent = new File(externalStorageDirectory, ResourceManager.APP_DIR);
			toSavePostfix = BINARY_MAP_INDEX_EXT;
			toCheckPostfix = BINARY_MAP_INDEX_EXT;
		} else if(key.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP)){
			parent = new File(externalStorageDirectory, ResourceManager.APP_DIR);
			toSavePostfix = BINARY_MAP_INDEX_EXT_ZIP;
			toCheckPostfix = BINARY_MAP_INDEX_EXT;
		} else if(key.endsWith(IndexConstants.VOICE_INDEX_EXT_ZIP)){
			parent = new File(externalStorageDirectory, ResourceManager.VOICE_PATH);
			toSavePostfix = VOICE_INDEX_EXT_ZIP;
			toCheckPostfix = ""; //$NON-NLS-1$
			unzipDir = true;
		}
		if(parent != null){
			parent.mkdirs();
		}
		final DownloadEntry entry;
		if(parent == null || !parent.exists()){
			Toast.makeText(DownloadIndexActivity.this, getString(R.string.sd_dir_not_accessible), Toast.LENGTH_LONG).show();
			entry = null;
		} else {
			entry = new DownloadEntry();
			int ls = key.lastIndexOf('_');
			entry.baseName = key.substring(0, ls);
			String extractDateAndSize = extractDateAndSize(value);
			entry.fileToSave = new File(parent, entry.baseName + toSavePostfix);
			entry.unzip = unzipDir;
			if(extractDateAndSize.length() > 10){
				SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy"); //$NON-NLS-1$
				try {
					Date d = format.parse(extractDateAndSize.substring(1, 11));
					entry.dateModified = d.getTime();
				} catch (ParseException e1) {
				}
			}
			int i = extractDateAndSize.lastIndexOf("MB"); //$NON-NLS-1$
			if(i > 12){
				String size = extractDateAndSize.substring(14, i - 1);
				try {
					entry.sizeMB = Double.parseDouble(size);
				} catch (NumberFormatException e1) {
				}
			}
			entry.fileToUnzip = new File(parent, entry.baseName + toCheckPostfix);
		}
		return entry;
	}
	
	protected void downloadFiles() {
		Builder builder = new AlertDialog.Builder(this);
		double sz = 0;
		for(DownloadEntry es : entriesToDownload.values()){
			sz += es.sizeMB;
		}
		builder.setMessage(MessageFormat.format(getString(R.string.download_files_question), entriesToDownload.size(), sz));
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
											if (downloadFile(s, entry.fileToSave, entry.fileToUnzip, entry.unzip, impl, entry.dateModified)) {
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
		public Long dateModified;
		public double sizeMB;
		public String baseName;
		
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
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setReadTimeout(30000);
					if (fileread > 0) {
						String range = "bytes="+fileread + "-" + (length -1); //$NON-NLS-1$ //$NON-NLS-2$
						conn.setRequestProperty("Range", range);  //$NON-NLS-1$
					}
					conn.setConnectTimeout(30000);
					log.info(conn.getResponseMessage() + " " + conn.getResponseCode()); //$NON-NLS-1$
					if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
						conn.disconnect();
						triesDownload--;
						continue;
					}
					is = conn.getInputStream();
//					long skipped = 0;
//					while (skipped < fileread) {
//						skipped += is.skip(fileread - skipped);
//					}
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
					if(length <= fileread){
						triesDownload = 0;
					}
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
			IProgress progress, Long dateModified) throws InterruptedException {
		FileOutputStream out = null;
		try {

			out = new FileOutputStream(fileToDownload);
			URL url = USE_DOWNLOAD_OSMAND_NET ? new URL("http://download.osmand.net/download?file="+key):  //$NON-NLS-1$
				DownloaderIndexFromGoogleCode.getInputStreamToLoadIndex(key);
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
					File fs;
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
						fs = new File(fileToUnZip.getParent(), name);
						
					} else {
						fs = new File(fileToUnZip, entry.getName());
					}
					out = new FileOutputStream(fs);
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
			if(dateModified != null){
				toIndex.setLastModified(dateModified);
			}
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
			if(dateModified != null){
				toIndex.setLastModified(dateModified);
				manager.updateIndexLastDateModified(toIndex);
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
	
	private static String extractDateAndSize(String m){
		return m.substring(m.indexOf('{'));
	}
	
	public void showWarning(final String messages){
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				Toast.makeText(DownloadIndexActivity.this, messages, Toast.LENGTH_LONG).show();
			}
			
		});
	}

	private String convertServerFileNameToLocal(String name){
		int l = name.lastIndexOf('_');
		String s;
		if(name.endsWith(IndexConstants.POI_INDEX_EXT) || name.endsWith(IndexConstants.POI_INDEX_EXT_ZIP)){
			s = IndexConstants.POI_INDEX_EXT;
		} else if(name.endsWith(IndexConstants.ADDRESS_INDEX_EXT) || name.endsWith(IndexConstants.ADDRESS_INDEX_EXT_ZIP)){
			s = IndexConstants.ADDRESS_INDEX_EXT;
		} else if(name.endsWith(IndexConstants.TRANSPORT_INDEX_EXT) || name.endsWith(IndexConstants.TRANSPORT_INDEX_EXT_ZIP)){
			s = IndexConstants.TRANSPORT_INDEX_EXT;
		} else if(name.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT) || name.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP)){
			s = IndexConstants.BINARY_MAP_INDEX_EXT;
		} else {
			s = ""; //$NON-NLS-1$
		}
		return name.substring(0, l) + s;
	}

	protected class IndexItem {
		private final Entry<String,String> fileEntry;
		private String key;
		private String description;
		private String date;
		
		public IndexItem(Entry<String,String> e) {
			fileEntry = e;
		}
		
		private void initialize()
		{
			if (key != null) {
				return;
			}
			Entry<String,String> e = fileEntry;
			int l = e.getKey().lastIndexOf('_');
			String s = ""; //$NON-NLS-1$
			if(e.getKey().endsWith(IndexConstants.POI_INDEX_EXT) || e.getKey().endsWith(IndexConstants.POI_INDEX_EXT_ZIP)){
				s = getString(R.string.poi);
			} else if(e.getKey().endsWith(IndexConstants.ADDRESS_INDEX_EXT) || e.getKey().endsWith(IndexConstants.ADDRESS_INDEX_EXT_ZIP)){
				s = getString(R.string.address);
			} else if(e.getKey().endsWith(IndexConstants.TRANSPORT_INDEX_EXT) || e.getKey().endsWith(IndexConstants.TRANSPORT_INDEX_EXT_ZIP)){
				s = getString(R.string.transport);
			} else if(e.getKey().endsWith(IndexConstants.BINARY_MAP_INDEX_EXT) || e.getKey().endsWith(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP)){
				boolean f = true;
				String lowerCase = e.getValue().toLowerCase();
				if (lowerCase.contains("map")) { //$NON-NLS-1$
					if (!f) {
						s += ", "; //$NON-NLS-1$
					} else {
						f = false;
					}
					s += getString(R.string.map_index);
				}
				if (lowerCase.contains("transport")) { //$NON-NLS-1$
					if (!f) {
						s += ", "; //$NON-NLS-1$
					} else {
						f = false;
					}
					s += getString(R.string.transport);
				}
				if (lowerCase.contains("address")) { //$NON-NLS-1$
					if (!f) {
						s += ", "; //$NON-NLS-1$
					} else {
						f = false;
					}
					s += getString(R.string.address);
				}
			} else if(e.getKey().endsWith(IndexConstants.VOICE_INDEX_EXT_ZIP)){
				s = getString(R.string.voice);
			}
			String name = e.getKey().substring(0, l < 0 ? e.getKey().length() : l).replace('_', ' ');
			if(e.getKey().endsWith(".zip")){ //$NON-NLS-1$
				name += " (zip)"; //$NON-NLS-1$
			}
			key = s.trim() + "\n" + name;
			description = extractDateAndSize(e.getValue()).replace(':', '\n').trim();
			if (description.length() > 10) {
				date = description.substring(1, 11);
			} else {
				date = null;
			}
		}
		public String getFileName() {
			return fileEntry.getKey();
		}
		public Entry<String, String> getFileEntry() {
			return fileEntry;
		}
		public String getName() {
			initialize();
			return key;
		}
		public String getDescription() {
			initialize();
			return description;
		}
		public Object getDate() {
			initialize();
			return date;
		}
		
	}
	protected class DownloadIndexAdapter extends ArrayAdapter<IndexItem> implements Filterable {
		
		private DownloadIndexFilter myFilter;
		private final Map<String, IndexItem> indexFiles;

		public DownloadIndexAdapter(Map<String, String> indexFiles) {
			super(DownloadIndexActivity.this, net.osmand.R.layout.download_index_list_item);
			this.indexFiles = new LinkedHashMap<String, DownloadIndexActivity.IndexItem>(indexFiles.size());
			for (Entry<String, String> entry : indexFiles.entrySet()) {
				IndexItem item = new IndexItem(entry);
				this.indexFiles.put(entry.getKey(), item);
				add(item);
			}
		}

		public Map<String, IndexItem> getIndexFiles() {
			return indexFiles;
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
			IndexItem e = getItem(position);
			item.setText(e.getName()); //$NON-NLS-1$
			description.setText(e.getDescription());
			
			CheckBox ch = (CheckBox) row.findViewById(R.id.check_download_item);
			ch.setChecked(entriesToDownload.containsKey(e.getFileName()));
			ch.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final CheckBox ch = (CheckBox) v.findViewById(R.id.check_download_item);
					ch.setChecked(!ch.isChecked());
					DownloadIndexActivity.this.onListItemClick(getListView(), row, position, getItemId(position));
				}
			});
			
			if(indexFileNames != null){
				String sfName = convertServerFileNameToLocal(e.getFileName());
				if(!indexFileNames.containsKey(sfName)){
					item.setTextColor(Color.WHITE);
				} else {
					if(e.getDate() != null){
						if(e.getDate().equals(indexFileNames.get(sfName))){
							item.setTextColor(Color.GREEN);
						} else {
							item.setTextColor(Color.BLUE);
						}
					} else {
						item.setTextColor(Color.GREEN);
					}
				}
			}
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
					results.values = indexFiles.values();
					results.count = indexFiles.size();
				} else {
					CharSequence lowerCase = constraint.toString()
							.toLowerCase();
					List<IndexItem> filter = new ArrayList<IndexItem>();
					for (IndexItem item : indexFiles.values()) {
						if (item.getName().toLowerCase().contains(lowerCase)) {
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
			protected void publishResults(CharSequence constraint, FilterResults results) {
				clear();
				for (IndexItem item : (Collection<IndexItem>) results.values) {
					add(item);
				}
			}
		}
	}
	

}
