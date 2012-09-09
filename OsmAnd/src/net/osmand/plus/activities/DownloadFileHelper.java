package net.osmand.plus.activities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.osmand.IProgress;
import net.osmand.LogUtil;
import net.osmand.Version;
import net.osmand.data.IndexConstants;
import net.osmand.plus.R;

import org.apache.commons.logging.Log;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

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
			IProgress progress, boolean forceWifi) throws IOException, InterruptedException {
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
					conn.setRequestProperty("User-Agent", Version.getFullVersion(ctx)); //$NON-NLS-1$
					conn.setReadTimeout(30000);
					if (fileread > 0) {
						String range = "bytes="+fileread + "-" + (length -1); //$NON-NLS-1$ //$NON-NLS-2$
						conn.setRequestProperty("Range", range);  //$NON-NLS-1$
					}
					conn.setConnectTimeout(30000);
					log.info(conn.getResponseMessage() + " " + conn.getResponseCode()); //$NON-NLS-1$
					boolean wifiConnectionBroken = forceWifi && !isWifiConnected();
					if ((conn.getResponseCode() != HttpURLConnection.HTTP_PARTIAL  && 
							conn.getResponseCode() != HttpURLConnection.HTTP_OK ) || wifiConnectionBroken) {
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
						progress.startTask(taskName, length / 1024); //$NON-NLS-1$
					}

					first = false;
					while ((read = is.read(buffer)) != -1) {
						 if(interruptDownloading){
						 	throw new InterruptedException();
						 }
						out.write(buffer, 0, read);
						fileread += read;
						progress.remaining((length - fileread) / 1024);
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
	
	public boolean isWifiConnected(){
		ConnectivityManager mgr =  (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = mgr.getActiveNetworkInfo();
		return ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI;
	}
	
	protected boolean downloadFile(final String fileName, final File fileToDownload, final File fileToUnZip, final boolean unzipToDir,
			IProgress progress, Long dateModified, int parts, List<File> toReIndex, String indexOfAllFiles, 
			DownloadFileShowWarning showWarningCallback, boolean forceWifi ) throws InterruptedException {
		FileOutputStream out = null;
		try {

			out = new FileOutputStream(fileToDownload);
			try {
				if(parts == 1){
					URL url = new URL("http://download.osmand.net/download?event=2&file="+fileName + "&" + Version.getVersionAsURLParam(ctx));  //$NON-NLS-1$
					downloadFile(fileName, out, url, null, indexOfAllFiles, progress, forceWifi);
				} else {
					for(int i=1; i<=parts; i++){
						URL url = new URL("http://download.osmand.net/download?event=2&file="+fileName+"-"+i + "&" + Version.getVersionAsURLParam(ctx));  //$NON-NLS-1$
						downloadFile(fileName, out, url, " ["+i+"/"+parts+"]", indexOfAllFiles, progress, forceWifi);
					}
				}
			} finally {
				out.close();
				out = null;
			}

			if (fileToDownload.getName().endsWith(".zip")) { //$NON-NLS-1$
				if (unzipToDir) {
					fileToUnZip.mkdirs();
				}
				CountingInputStream fin = new CountingInputStream(new FileInputStream(fileToDownload));
				ZipInputStream zipIn = new ZipInputStream(fin);
				ZipEntry entry = null;
				boolean first = true;
				int len = (int) fileToDownload.length();
				progress.startTask(ctx.getString(R.string.unzipping_file), len / 1024);
				while ((entry = zipIn.getNextEntry()) != null) {
					if(entry.isDirectory() || entry.getName().endsWith(IndexConstants.GEN_LOG_EXT)){
						continue;
					}
					File fs;
					if (!unzipToDir) {
						if (first) {
							fs = fileToUnZip;
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
						}
					} else {
						fs = new File(fileToUnZip, entry.getName());
					}
					out = new FileOutputStream(fs);
					int read;
					byte[] buffer = new byte[BUFFER_SIZE];
					int remaining = len;
					while ((read = zipIn.read(buffer)) != -1) {
						out.write(buffer, 0, read);
						remaining -= fin.lastReadCount();
						progress.remaining(remaining / 1024);
					}
					out.close();
					
					if(dateModified != null){
						fs.setLastModified(dateModified);
					}
					toReIndex.add(fs);
				}
				zipIn.close();
				fileToDownload.delete(); // zip is no needed more
			}

			showWarningCallback.showWarning(ctx.getString(R.string.download_index_success));
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
	
	private static class CountingInputStream extends InputStream {

		private final InputStream delegate;
		private int count;

		public CountingInputStream(InputStream delegate) {
			this.delegate = delegate;
		}
		
		public int lastReadCount() {
			int last = count;
			count = 0;
			return last;
		}
		
		@Override
		public int available() throws IOException {
			return delegate.available();
		}

		@Override
		public void close() throws IOException {
			delegate.close();
		}

		@Override
		public boolean equals(Object o) {
			return delegate.equals(o);
		}

		@Override
		public int hashCode() {
			return delegate.hashCode();
		}

		@Override
		public void mark(int readlimit) {
			delegate.mark(readlimit);
		}

		@Override
		public boolean markSupported() {
			return delegate.markSupported();
		}

		@Override
		public int read() throws IOException {
			int read = delegate.read();
			if (read > 0) {
				this.count++;;
			}
			return read;
		}

		@Override
		public int read(byte[] buffer, int offset, int length)
				throws IOException {
			int read = delegate.read(buffer, offset, length);
			if (read > 0) {
				this.count += read;
			}
			return read;
		}

		@Override
		public int read(byte[] buffer) throws IOException {
			int read = delegate.read(buffer);
			if (read > 0) {
				this.count += read;
			}
			return read;
		}

		@Override
		public void reset() throws IOException {
			delegate.reset();
		}

		@Override
		public long skip(long byteCount) throws IOException {
			return delegate.skip(byteCount);
		}

		@Override
		public String toString() {
			return delegate.toString();
		}
	}
}
