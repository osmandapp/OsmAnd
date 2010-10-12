package net.osmand.activities;

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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
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
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class DownloadIndexActivity extends ListActivity {
	
	private final static Log log = LogUtil.getLog(DownloadIndexActivity.class);
	private ProgressDialog progressDlg = null; 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.download_index);
		
		progressDlg = ProgressDialog.show(this, getString(R.string.downloading), getString(R.string.downloading_list_indexes));
		progressDlg.setCancelable(true);
		
		new Thread(new Runnable(){
			@Override
			public void run() {
				final Map<String, String> indexFiles = downloadIndex();
				if(progressDlg != null){
					progressDlg.dismiss();
					progressDlg = null;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (indexFiles != null) {
								setListAdapter(new DownloadIndexAdapter(new ArrayList<Entry<String,String>>(indexFiles.entrySet())));
							} else {
								Toast.makeText(DownloadIndexActivity.this, R.string.list_index_files_was_not_loaded, Toast.LENGTH_LONG).show();
							}
						}
					});
				}
			}
		}, "DownloadIndexes").start(); //$NON-NLS-1$
		
	}
	
	@Override
	protected void onStop() {
		if(progressDlg != null){
			progressDlg.dismiss();
			progressDlg = null;
		}
		super.onStop();
	}
	
	protected Map<String, String> downloadIndex(){
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
			DownloaderIndexFromGoogleCode.getIndexFiles(new String[] { 
					IndexConstants.ADDRESS_INDEX_EXT, IndexConstants.POI_INDEX_EXT, IndexConstants.TRANSPORT_INDEX_EXT, 
					IndexConstants.ADDRESS_INDEX_EXT_ZIP, IndexConstants.POI_INDEX_EXT_ZIP, IndexConstants.TRANSPORT_INDEX_EXT_ZIP, 
					IndexConstants.MAP_INDEX_EXT, IndexConstants.MAP_INDEX_EXT_ZIP, IndexConstants.VOICE_INDEX_EXT_ZIP, }, 
					new String[] {
					IndexConstants.ADDRESS_TABLE_VERSION + "", IndexConstants.POI_TABLE_VERSION + "",//$NON-NLS-1$//$NON-NLS-2$
					IndexConstants.TRANSPORT_TABLE_VERSION + "", IndexConstants.ADDRESS_TABLE_VERSION + "", //$NON-NLS-1$ //$NON-NLS-2$
					IndexConstants.POI_TABLE_VERSION + "", IndexConstants.TRANSPORT_TABLE_VERSION + "", //$NON-NLS-1$//$NON-NLS-2$
					IndexConstants.MAP_TABLE_VERSION + "", IndexConstants.MAP_TABLE_VERSION + "", //$NON-NLS-1$//$NON-NLS-2$
					IndexConstants.VOICE_VERSION + "", }, //$NON-NLS-1$ 
					indexFiles);
			
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
		
		int ls = e.getKey().lastIndexOf('_');
		final String baseName = e.getKey().substring(0, ls);
		String key = e.getKey();
		
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
		} else if(key.endsWith(IndexConstants.MAP_INDEX_EXT)){
			parent = new File(Environment.getExternalStorageDirectory(), ResourceManager.APP_DIR);
			toSavePostfix = IndexConstants.MAP_INDEX_EXT;
			toCheckPostfix = IndexConstants.MAP_INDEX_EXT;
		} else if(key.endsWith(IndexConstants.MAP_INDEX_EXT_ZIP)){
			parent = new File(Environment.getExternalStorageDirectory(), ResourceManager.APP_DIR);
			toSavePostfix = IndexConstants.MAP_INDEX_EXT_ZIP;
			toCheckPostfix = IndexConstants.MAP_INDEX_EXT;
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
			final File fileToSave = new File(parent, baseName + toSavePostfix);
			final File fileToUnzip = new File(parent, baseName + toCheckPostfix);
				Builder builder = new AlertDialog.Builder(this);
				if(!fileToUnzip.exists()){
					builder.setMessage(MessageFormat.format(getString(R.string.download_question), baseName, e.getValue()));
				} else {
					MessageFormat format;
					if(fileToUnzip.isDirectory()){
						format = new MessageFormat("{0,date,dd.MM.yyyy}", Locale.US); //$NON-NLS-1$
					} else {
						format = new MessageFormat("{0,date,dd.MM.yyyy} : {1, number,##.#} MB", Locale.US); //$NON-NLS-1$
					}
					String description = format.format(new Object[]{new Date(fileToUnzip.lastModified()), ((float)fileToUnzip.length() / MB)});
					builder.setMessage(MessageFormat.format(getString(R.string.download_question_exist), baseName, description, e.getValue()));
					
				}
				final boolean toUnzip = unzipDir;
				builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						downloadFile(e.getKey(), fileToSave, fileToUnzip, toUnzip);
					}
				});
				builder.setNegativeButton(R.string.default_buttons_no, null);
				builder.show();
		}
		
	}
	
	private static final int BUFFER_SIZE = 32256; 
	
	protected void downloadFile(FileOutputStream out, URL url, IProgress progress) throws IOException{
		URLConnection conn = url.openConnection();
		conn.setReadTimeout(30000);
		conn.setConnectTimeout(30000);
		InputStream is = conn.getInputStream();
		int length = conn.getContentLength();
		progress.startTask(getString(R.string.downloading_file), length);
		byte[] buffer = new byte[BUFFER_SIZE];
		int read = 0;
		while((read = is.read(buffer)) != -1){
			out.write(buffer, 0, read);
			progress.progress(read);
			length -= read;
		}
		if(length > 0){
			throw new IOException("File was not fully read"); //$NON-NLS-1$
		}
		out.close();
		
	}
	
	protected void downloadFile(final String key, final File fileToDownload, final File fileToUnZip, final boolean unzipToDir) {
		
		progressDlg = ProgressDialog.show(this, getString(R.string.downloading), getString(R.string.downloading_file), true, true);
		progressDlg.show();
		final ProgressDialogImplementation impl = new ProgressDialogImplementation(progressDlg, true);
		impl.setRunnable("DownloadIndex", new Runnable(){ //$NON-NLS-1$

			@Override
			public void run() {
				try {
					FileOutputStream out = new FileOutputStream(fileToDownload);
					URL url = DownloaderIndexFromGoogleCode.getInputStreamToLoadIndex(key);
					downloadFile(out, url, impl);
						
					File toIndex = fileToDownload;
					if(fileToDownload.getName().endsWith(".zip")){ //$NON-NLS-1$
						impl.startTask(getString(R.string.unzipping_file), -1);
						if(!unzipToDir){
							toIndex = fileToUnZip;
						} else {
							fileToUnZip.mkdirs();
						}
						ZipInputStream zipIn = new ZipInputStream(new FileInputStream(fileToDownload));
						ZipEntry entry = null;
						while((entry = zipIn.getNextEntry()) != null) {
							if(!unzipToDir){
								String name = entry.getName();
								// small simplification
								int ind = name.lastIndexOf('_');
								if(ind > 0){
									// cut version
									int i = name.indexOf('.', ind);
									if(i > 0){
										name = name.substring(0, ind) + name.substring(i, name.length());
									}
								}
								out = new FileOutputStream(new File(fileToUnZip.getParent(), name));
							} else {
								out = new FileOutputStream(new File(fileToUnZip, entry.getName()));
							}
							int read;
							byte[] buffer = new byte[BUFFER_SIZE];
							while((read = zipIn.read(buffer)) != -1){
								out.write(buffer, 0, read);
							}
							out.close();
						} 
						zipIn.close();
						fileToDownload.delete(); // zip is no needed more
					}
						
					ArrayList<String> warnings = new ArrayList<String>();
					ResourceManager manager = ((OsmandApplication)getApplication()).getResourceManager();
					if(toIndex.getName().endsWith(IndexConstants.ADDRESS_INDEX_EXT)){
						manager.indexingAddress(impl, warnings, toIndex);
					} else if(toIndex.getName().endsWith(IndexConstants.POI_INDEX_EXT)){
						manager.indexingPoi(impl, warnings, toIndex);
					} else if(toIndex.getName().endsWith(IndexConstants.TRANSPORT_INDEX_EXT)){
						manager.indexingTransport(impl, warnings, toIndex);
					} else if(toIndex.getName().endsWith(IndexConstants.MAP_INDEX_EXT)){
						manager.indexingMaps(impl);
					} else if(toIndex.getName().endsWith(IndexConstants.VOICE_INDEX_EXT_ZIP)){
					}
					if(warnings.isEmpty()){
						showWarning(getString(R.string.download_index_success));
					} else {
						showWarning(warnings.get(0));
					}
				} catch (IOException e) {
					log.error("Exception ocurred", e); //$NON-NLS-1$
					showWarning(getString(R.string.error_io_error));
					// Possibly file is corrupted
					fileToDownload.delete();
				} finally {
					if(progressDlg != null){
						progressDlg.dismiss();
						progressDlg = null;
					}
				}
			}
		});
		impl.run();
	}
	
	public void showWarning(final String messages){
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				Toast.makeText(DownloadIndexActivity.this, messages, Toast.LENGTH_LONG).show();
			}
			
		});
	}


	private class DownloadIndexAdapter extends ArrayAdapter<Entry<String, String>> {

		public DownloadIndexAdapter(List<Entry<String, String>> array) {
			super(DownloadIndexActivity.this, net.osmand.R.layout.download_index_list_item, array);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(net.osmand.R.layout.download_index_list_item, parent, false);
			}
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
			} else if(e.getKey().endsWith(IndexConstants.MAP_INDEX_EXT) || e.getKey().endsWith(IndexConstants.MAP_INDEX_EXT_ZIP)){
				s = getString(R.string.map_index);
			} else if(e.getKey().endsWith(IndexConstants.VOICE_INDEX_EXT_ZIP)){
				s = getString(R.string.voice);
			}
			String name = e.getKey().substring(0, l).replace('_', ' ');
			if(e.getKey().endsWith(".zip")){ //$NON-NLS-1$
				name += " (zip)"; //$NON-NLS-1$
			}
			item.setText(s + "\n " + name); //$NON-NLS-1$
			description.setText(e.getValue().replace(':', '\n'));
			return row;
		}
	}
	

}
