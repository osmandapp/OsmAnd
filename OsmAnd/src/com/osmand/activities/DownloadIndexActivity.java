package com.osmand.activities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

import com.osmand.LogUtil;
import com.osmand.ProgressDialogImplementation;
import com.osmand.R;
import com.osmand.ResourceManager;
import com.osmand.data.index.DownloaderIndexFromGoogleCode;
import com.osmand.data.index.IndexConstants;

public class DownloadIndexActivity extends ListActivity {
	
	private final static Log log = LogUtil.getLog(DownloadIndexActivity.class);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.download_index);
		
		final ProgressDialog dlg = ProgressDialog.show(this, getString(R.string.downloading), getString(R.string.downloading_list_indexes));
		dlg.setCancelable(true);
		
		new Thread(new Runnable(){
			@Override
			public void run() {
				final Map<String, String> indexFiles = downloadIndex();
				dlg.dismiss();
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
		}, "DownloadIndexes").start(); //$NON-NLS-1$
		
	}
	
	protected Map<String, String> downloadIndex(){
		try {
			log.debug("Start loading list of index files"); //$NON-NLS-1$
			Map<String, String> indexFiles = DownloaderIndexFromGoogleCode.getIndexFiles(new String[] { 
					IndexConstants.ADDRESS_INDEX_EXT, IndexConstants.POI_INDEX_EXT, IndexConstants.TRANSPORT_INDEX_EXT, 
					IndexConstants.ADDRESS_INDEX_EXT_ZIP, IndexConstants.POI_INDEX_EXT_ZIP, IndexConstants.TRANSPORT_INDEX_EXT_ZIP, }, 
					new String[] {
					IndexConstants.ADDRESS_TABLE_VERSION + "", IndexConstants.POI_TABLE_VERSION + "",//$NON-NLS-1$//$NON-NLS-2$
					IndexConstants.TRANSPORT_TABLE_VERSION + "", //$NON-NLS-1$
					IndexConstants.ADDRESS_TABLE_VERSION + "", IndexConstants.POI_TABLE_VERSION + "",//$NON-NLS-1$//$NON-NLS-2$
					IndexConstants.TRANSPORT_TABLE_VERSION + ""});  //$NON-NLS-1$
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
		final String regionName = e.getKey().substring(0, ls);
		final File fileToSave = resolveFileName(e.getKey(), regionName);
		if (fileToSave != null) {
			Builder builder = new AlertDialog.Builder(this);
			File toCheck = new File(fileToSave.getParent(), fileToSave.getName().substring(0, fileToSave.getName().length() - 4) +".odb"); //$NON-NLS-1$
			if(!toCheck.exists()){
				builder.setMessage(MessageFormat.format(getString(R.string.download_question), regionName, e.getValue()));
			} else {
				MessageFormat format = new MessageFormat("{0,date,dd.MM.yyyy} : {1, number,##.#} MB", Locale.US); //$NON-NLS-1$
				String description = format.format(new Object[]{new Date(toCheck.lastModified()), ((float)toCheck.length() / MB)});
				builder.setMessage(MessageFormat.format(getString(R.string.download_question_exist), regionName, description, e.getValue()));
				
			}
			
			
			builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					downloadFile(e.getKey(), fileToSave);
				}
			});
			builder.setNegativeButton(R.string.default_buttons_no, null);
			builder.show();
		}
	}
	
	private static final int BUFFER_SIZE = 32256; 
	
	private File resolveFileName(String key, String regionName){
		File parent = null;
		if(key.endsWith(IndexConstants.ADDRESS_INDEX_EXT)){
			parent = new File(Environment.getExternalStorageDirectory(), ResourceManager.ADDRESS_PATH);
			regionName += IndexConstants.ADDRESS_INDEX_EXT;
		} else if(key.endsWith(IndexConstants.ADDRESS_INDEX_EXT_ZIP)){
			parent = new File(Environment.getExternalStorageDirectory(), ResourceManager.ADDRESS_PATH);
			regionName += IndexConstants.ADDRESS_INDEX_EXT_ZIP;
		} else if(key.endsWith(IndexConstants.POI_INDEX_EXT)){
			parent = new File(Environment.getExternalStorageDirectory(), ResourceManager.POI_PATH);
			regionName += IndexConstants.POI_INDEX_EXT;
		} else if(key.endsWith(IndexConstants.POI_INDEX_EXT_ZIP)){
			parent = new File(Environment.getExternalStorageDirectory(), ResourceManager.POI_PATH);
			regionName += IndexConstants.POI_INDEX_EXT_ZIP;
		} else if(key.endsWith(IndexConstants.TRANSPORT_INDEX_EXT)){
			parent = new File(Environment.getExternalStorageDirectory(), ResourceManager.TRANSPORT_PATH);
			regionName += IndexConstants.TRANSPORT_INDEX_EXT;
		} else if(key.endsWith(IndexConstants.TRANSPORT_INDEX_EXT_ZIP)){
			parent = new File(Environment.getExternalStorageDirectory(), ResourceManager.TRANSPORT_PATH);
			regionName += IndexConstants.TRANSPORT_INDEX_EXT_ZIP;
		}
		if(parent != null){
			parent.mkdirs();
		}
		if(parent == null || !parent.exists()){
			Toast.makeText(DownloadIndexActivity.this, getString(R.string.download_sd_dir_not_accessible), Toast.LENGTH_LONG).show();
			return null;
		}
		File file = new File(parent, regionName);
		return file;
	}
	
	protected void downloadFile(final String key, final File file) {
		
		final ProgressDialog dlg = ProgressDialog.show(this, getString(R.string.downloading), getString(R.string.downloading_file), true, true);
		dlg.show();
		final ProgressDialogImplementation impl = new ProgressDialogImplementation(dlg, true);
		impl.setRunnable("DownloadIndex", new Runnable(){ //$NON-NLS-1$

			@Override
			public void run() {
				try {
					FileOutputStream out = new FileOutputStream(file);
					URL url = DownloaderIndexFromGoogleCode.getInputStreamToLoadIndex(key);
					
					URLConnection conn = url.openConnection();
					conn.setReadTimeout(30000);
					conn.setConnectTimeout(30000);
					InputStream is = conn.getInputStream();
					int length = conn.getContentLength();
					impl.startTask(getString(R.string.downloading_file), length);
					byte[] buffer = new byte[BUFFER_SIZE];
					int read = 0;
					while((read = is.read(buffer)) != -1){
						out.write(buffer, 0, read);
						impl.progress(read);
						length -= read;
					}
					if(length > 0){
						throw new IOException("File was not fully read"); //$NON-NLS-1$
					}
					out.close();
						
					File toIndex = file;
					if(file.getName().endsWith(".zip")){ //$NON-NLS-1$
						impl.startTask(getString(R.string.unzipping_file), -1);
						toIndex = new File(file.getParentFile(), file.getName().substring(0, file.getName().length() - 3) + "odb"); //$NON-NLS-1$
						ZipInputStream zipIn = new ZipInputStream(new FileInputStream(file));
						ZipEntry entry = null;
						boolean found = false;
						while(!found) {
							if(entry != null){
								zipIn.closeEntry();
							}
							entry = zipIn.getNextEntry();
							found = entry == null || entry.getName().endsWith(".odb"); //$NON-NLS-1$
						} 
						if(entry != null){
							out = new FileOutputStream(toIndex);
							while((read = zipIn.read(buffer)) != -1){
								out.write(buffer, 0, read);
							}
							out.close();
						}
						zipIn.close();
						file.delete(); // zip is no needed more
					}
						
					ArrayList<String> warnings = new ArrayList<String>();
					if(toIndex.getName().endsWith(IndexConstants.ADDRESS_INDEX_EXT)){
						ResourceManager.getResourceManager().indexingAddress(impl, warnings, toIndex);
					} else if(toIndex.getName().endsWith(IndexConstants.POI_INDEX_EXT)){
						ResourceManager.getResourceManager().indexingPoi(impl, warnings, toIndex);
					} else if(toIndex.getName().endsWith(IndexConstants.TRANSPORT_INDEX_EXT)){
						ResourceManager.getResourceManager().indexingTransport(impl, warnings, toIndex);
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
					file.delete();
				} finally {
					dlg.dismiss();
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
			super(DownloadIndexActivity.this, com.osmand.R.layout.download_index_list_item, array);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(com.osmand.R.layout.download_index_list_item, parent, false);
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
