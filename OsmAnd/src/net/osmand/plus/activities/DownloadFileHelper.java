package net.osmand.plus.activities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;

import android.app.Activity;

import net.osmand.IProgress;
import net.osmand.LogUtil;
import net.osmand.data.IndexConstants;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;

public class DownloadFileHelper {
	
	private final static Log log = LogUtil.getLog(DownloadFileHelper.class);
	private static final int BUFFER_SIZE = 32256;
	protected final int TRIES_TO_DOWNLOAD = 15;
	protected final long TIMEOUT_BETWEEN_DOWNLOADS = 8000;
	private final Activity ctx;
	private boolean interruptDownloading = false;
	
	public DownloadFileHelper(Activity ctx){
		this.ctx = ctx;
	}
	
	public interface DownloadFileShowWarning {
		
		public void showWarning(String warning);
	}
	
	protected void downloadFile(String fileName, FileOutputStream out, URL url, String part, String indexOfAllFiles, 
			IProgress progress) throws IOException, InterruptedException {
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
					if (conn.getResponseCode() != HttpURLConnection.HTTP_PARTIAL  && 
							conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
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
						String taskName = ctx.getString(R.string.downloading_file) + indexOfAllFiles +" " + fileName;
						if(part != null){
							taskName += part;
						}
						progress.startTask(taskName, length); //$NON-NLS-1$
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
		if(length != fileread || length == 0){
			throw new IOException("File was not fully read"); //$NON-NLS-1$
		}
		
	}
	
	protected boolean downloadFile(final String fileName, final File fileToDownload, final File fileToUnZip, final boolean unzipToDir,
			IProgress progress, Long dateModified, int parts, List<File> toReIndex, String indexOfAllFiles, 
			DownloadFileShowWarning showWarningCallback) throws InterruptedException {
		FileOutputStream out = null;
		try {

			out = new FileOutputStream(fileToDownload);
			
			try {
				if(parts == 1){
					URL url = new URL("http://download.osmand.net/download?file="+fileName);  //$NON-NLS-1$
					downloadFile(fileName, out, url, null, indexOfAllFiles, progress);
				} else {
					for(int i=1; i<=parts; i++){
						URL url = new URL("http://download.osmand.net/download?file="+fileName+"-"+i);  //$NON-NLS-1$
						downloadFile(fileName, out, url, " ["+i+"/"+parts+"]", indexOfAllFiles, progress);
					}
				}
			} finally {
				out.close();
				out = null;
			}

			File toIndex = fileToDownload;
			if (fileToDownload.getName().endsWith(".zip")) { //$NON-NLS-1$
				if (!unzipToDir) {
					toIndex = fileToUnZip;
				} else {
					fileToUnZip.mkdirs();
				}
				ZipInputStream zipIn = new ZipInputStream(new FileInputStream(fileToDownload));
				ZipEntry entry = null;
				boolean first = true;
				while ((entry = zipIn.getNextEntry()) != null) {
					int size = (int)entry.getSize();
					progress.startTask(ctx.getString(R.string.unzipping_file), size);
					File fs;
					if (!unzipToDir) {
						if (first) {
							fs = toIndex;
							first = false;
						} else {
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
							toIndex = fs;
						}
					} else {
						fs = new File(fileToUnZip, entry.getName());
					}
					out = new FileOutputStream(fs);
					int read;
					byte[] buffer = new byte[BUFFER_SIZE];
					while ((read = zipIn.read(buffer)) != -1) {
						out.write(buffer, 0, read);
						progress.progress(read);
					}
					out.close();
				}
				zipIn.close();
				fileToDownload.delete(); // zip is no needed more
			}

			ArrayList<String> warnings = new ArrayList<String>();
			ResourceManager manager = ((OsmandApplication) ctx.getApplicationContext()).getResourceManager();
			if(dateModified != null){
				toIndex.setLastModified(dateModified);
			}
			if (toIndex.getName().endsWith(IndexConstants.POI_INDEX_EXT)) {
				// update poi index immediately
				manager.indexingPoi(progress, warnings, toIndex);
			}
			if(dateModified != null){
				toIndex.setLastModified(dateModified);
				manager.updateIndexLastDateModified(toIndex);
			}
			toReIndex.add(toIndex);
			if (warnings.isEmpty()) {
				
				showWarningCallback.showWarning(ctx.getString(R.string.download_index_success));
			} else {
				showWarningCallback.showWarning(warnings.get(0));
			}
			return true;
		} catch (IOException e) {
			log.error("Exception ocurred", e); //$NON-NLS-1$
			showWarningCallback.showWarning(ctx.getString(R.string.error_io_error));
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
	
	
	public void setInterruptDownloading(boolean interruptDownloading) {
		this.interruptDownloading = interruptDownloading;
	}
	
	public boolean isInterruptDownloading() {
		return interruptDownloading;
	}
}
