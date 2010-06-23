package com.osmand.activities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
			Map<String, String> indexFiles = DownloaderIndexFromGoogleCode.getIndexFiles(new String[] { IndexConstants.ADDRESS_INDEX_EXT,
					IndexConstants.POI_INDEX_EXT }, new String[] {
					IndexConstants.ADDRESS_TABLE_VERSION + "", IndexConstants.POI_TABLE_VERSION + "" }); //$NON-NLS-1$//$NON-NLS-2$
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
	
	
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		final Entry<String, String> e = ((DownloadIndexAdapter)getListAdapter()).getItem(position);
		Builder builder = new AlertDialog.Builder(this);
		int ls = e.getKey().lastIndexOf('_');
		final String regionName = e.getKey().substring(0, ls);
		builder.setMessage(MessageFormat.format(getString(R.string.download_question), regionName, e.getValue()));
		builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				downloadFile(e.getKey(), regionName);
			}
		});
		builder.setNegativeButton(R.string.default_buttons_no, null);
		builder.show();
	}
	
	private static final int BUFFER_SIZE = 32256; 
	
	protected void downloadFile(final String key, String regionName) {
		File parent = null;
		if(key.endsWith(IndexConstants.ADDRESS_INDEX_EXT)){
			parent = new File(Environment.getExternalStorageDirectory(), ResourceManager.ADDRESS_PATH);
			regionName += IndexConstants.ADDRESS_INDEX_EXT;
		} else if(key.endsWith(IndexConstants.POI_INDEX_EXT)){
			parent = new File(Environment.getExternalStorageDirectory(), ResourceManager.POI_PATH);
			regionName += IndexConstants.POI_INDEX_EXT;
		}
		if(parent != null){
			parent.mkdirs();
		}
		if(parent == null || !parent.exists()){
			Toast.makeText(DownloadIndexActivity.this, getString(R.string.download_sd_dir_not_accessible), Toast.LENGTH_LONG);
			return;
		}
		final File file = new File(parent, regionName);
		final ProgressDialog dlg = ProgressDialog.show(this, getString(R.string.downloading), getString(R.string.downloading_file), true, true);
		dlg.show();
		final ProgressDialogImplementation impl = new ProgressDialogImplementation(dlg, true);
		impl.setRunnable("DownloadIndex", new Runnable(){ //$NON-NLS-1$

			@Override
			public void run() {
				try {
					FileOutputStream out = new FileOutputStream(file);
					try {
						URL url = DownloaderIndexFromGoogleCode.getInputStreamToLoadIndex(key);
						URLConnection conn = url.openConnection();
						InputStream is = conn.getInputStream();
						impl.startTask(getString(R.string.downloading_file), conn.getContentLength());
						byte[] buffer = new byte[BUFFER_SIZE];
						int read = 0;
						while((read = is.read(buffer)) != -1){
							out.write(buffer, 0, read);
							impl.progress(read);
						}
						ArrayList<String> warnings = new ArrayList<String>();
						if(file.getName().endsWith(IndexConstants.ADDRESS_INDEX_EXT)){
							ResourceManager.getResourceManager().indexingAddress(impl, warnings, file);
						} else if(file.getName().endsWith(IndexConstants.POI_INDEX_EXT)){
							ResourceManager.getResourceManager().indexingPoi(impl, warnings, file);
						}
						if(warnings.isEmpty()){
							showWarning(getString(R.string.download_index_success));
						} else {
							showWarning(warnings.get(0));
						}
					} finally {
						out.close();
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
			String s = e.getKey().endsWith(IndexConstants.POI_INDEX_EXT) ? getString(R.string.poi) : getString(R.string.address);
			item.setText(s + "\n " + e.getKey().substring(0, l)); //$NON-NLS-1$
			description.setText(e.getValue().replace(':', '\n'));
			return row;
		}
	}
	

}
