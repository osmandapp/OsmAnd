package net.osmand.plus.download;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.download.IndexItem.DownloadEntry;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.plus.utils.FileUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DownloadFileHelper {
	
	private static final Log log = PlatformUtil.getLog(DownloadFileHelper.class);

	private static final int BUFFER_SIZE = 32256;
	protected static final int TRIES_TO_DOWNLOAD = 15;
	protected static final long TIMEOUT_BETWEEN_DOWNLOADS = 8000;

	private final OsmandApplication ctx;
	private boolean interruptDownloading;


	public DownloadFileHelper(OsmandApplication ctx){
		this.ctx = ctx;
	}
	
	public interface DownloadFileShowWarning {
		
		void showWarning(String warning);
	}
	
	public static boolean isInterruptedException(IOException e) {
		return e != null && e.getMessage().equals("Interrupted");
	}
	
	public InputStream getInputStreamToDownload(URL url, boolean forceWifi) throws IOException {
		InputStream cis = new InputStream() {
			final byte[] buffer = new byte[BUFFER_SIZE];
			int bufLen;
			int bufRead;
			int length;
			int fileread;
			int triesDownload = TRIES_TO_DOWNLOAD;
			boolean notFound;
			boolean first = true;
			private InputStream is;
			
			private void reconnect() throws IOException {
				while (triesDownload > 0) {
					try {
						if (!first) {
							log.info("Reconnecting"); //$NON-NLS-1$
							try {
								Thread.sleep(TIMEOUT_BETWEEN_DOWNLOADS);
							} catch (InterruptedException e) {
							}
						}
						HttpURLConnection conn = NetworkUtils.getHttpURLConnection(url);
						conn.setRequestProperty("User-Agent", Version.getFullVersion(ctx)); //$NON-NLS-1$
						conn.setReadTimeout(AndroidNetworkUtils.READ_TIMEOUT);
						if (fileread > 0) {
							String range = "bytes="+fileread + "-" + (length -1); //$NON-NLS-1$ //$NON-NLS-2$
							conn.setRequestProperty("Range", range);  //$NON-NLS-1$
						}
						conn.setConnectTimeout(AndroidNetworkUtils.CONNECT_TIMEOUT);
						log.info(conn.getResponseMessage() + " " + conn.getResponseCode()); //$NON-NLS-1$
						boolean wifiConnectionBroken = forceWifi && !isWifiConnected();
						if(conn.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND){
							notFound = true;
							break;
						}
						if ((conn.getResponseCode() != HttpURLConnection.HTTP_PARTIAL  && 
								conn.getResponseCode() != HttpURLConnection.HTTP_OK ) || wifiConnectionBroken) {
							conn.disconnect();
							triesDownload--;
							continue;
						}
						is = conn.getInputStream();
						if (first) {
							length = conn.getContentLength();
						}

						first = false;
						return;
					} catch (IOException e) {
						log.error("IOException", e); //$NON-NLS-1$
						triesDownload--;
					}
				}
				if(notFound) {
					throw new IOException("File not found "); //$NON-NLS-1$
				} else if(length == 0){
					throw new IOException("File was not fully read"); //$NON-NLS-1$
				} else if(triesDownload == 0 && length != fileread) {
					throw new IOException("File was not fully read"); //$NON-NLS-1$
				}
			}
			// use as prepare
			@Override
			public synchronized void reset() throws IOException {
				reconnect();
			}
			
			@Override
			public int read(byte[] buffer, int offset, int len) throws IOException {
				if (bufLen == -1) {
					return -1;
				}
				if (bufRead >= bufLen) {
					refillBuffer();
				}
				if (bufLen == -1) {
					return -1;
				}
				int av = bufLen - bufRead;
				int min = Math.min(len, av);
				System.arraycopy(this.buffer, bufRead, buffer, offset, min);
				bufRead += min;
				return min;
			}
			
			@Override
			public int read() throws IOException {
				int r = -1;
				if(bufLen == -1) {
					return -1;
				}
				refillBuffer();
				if(bufRead < bufLen) {
					byte b = buffer[bufRead++];
					return b >= 0 ? b : b + 256;
				}
				if (length <= fileread) {
					throw new IOException("File was not fully read"); //$NON-NLS-1$
				}
				return r;
			}
			private void refillBuffer() throws IOException {
				boolean readAgain = bufRead >= bufLen;
				while (readAgain) {
					if (is == null) {
						reconnect();
					}
					try {
						readAgain = false;
						bufRead = 0;
						if ((bufLen = is.read(buffer)) != -1) {
							fileread += bufLen;
							if (interruptDownloading) {
								break;
							}
						}
					} catch (IOException e) {
						if(interruptDownloading) 
						log.error("IOException", e); //$NON-NLS-1$
						triesDownload--;
						reconnect();
						readAgain = true;
					}
				}
				if (interruptDownloading) {
					throw new IOException("Interrupted");
				}
			}
			
			@Override
			public void close() throws IOException {
				if (is != null) {
					is.close();
				}
			}
			
			@Override
			public int available() throws IOException {
				if (is == null) {
					reconnect();
				}
				return length - fileread;
			}
		};
		cis.reset();
		return cis;
	}
	
	public boolean isWifiConnected(){
		return ctx.getSettings().isWifiConnected();
	}

	public boolean downloadFile(IndexItem.DownloadEntry de, IProgress progress,
								List<File> toReIndex, DownloadFileShowWarning showWarningCallback, boolean forceWifi) throws InterruptedException {
		try {
			List<InputStream> downloadInputStreams = new ArrayList<InputStream>();
			URL url = new URL(de.urlToDownload); //$NON-NLS-1$
			log.info("Url downloading " + de.urlToDownload);
			downloadInputStreams.add(getInputStreamToDownload(url, forceWifi));
			de.fileToDownload = de.targetFile;
			if (!de.unzipFolder) {
				de.fileToDownload = FileUtils.getFileWithDownloadExtension(de.targetFile);
			}
			unzipFile(de, progress, downloadInputStreams);
			if (!de.targetFile.getAbsolutePath().equals(de.fileToDownload.getAbsolutePath())) {
				ResourceManager rm = ctx.getResourceManager();
				boolean success = FileUtils.replaceTargetFile(rm, de.fileToDownload, de.targetFile);
				if (!success) {
					showWarningCallback.showWarning(ctx.getString(R.string.shared_string_io_error) + ": old file can't be deleted");
					return false;
				}
			}
			if (de.type == DownloadActivityType.SRTM_COUNTRY_FILE) {
				removePreviousSrtmFile(de);
			}
			toReIndex.add(de.targetFile);
			return true;
		} catch (IOException e) {
			log.error("Exception ocurred", e);
			showWarningCallback.showWarning(ctx.getString(R.string.shared_string_io_error) + ": " + e.getMessage());
			// Possibly file is corrupted
			Algorithms.removeAllFiles(de.fileToDownload);
			return false;
		}
	}

	private void removePreviousSrtmFile(DownloadEntry entry) {
		String meterExt = IndexConstants.BINARY_SRTM_MAP_INDEX_EXT;
		String feetExt = IndexConstants.BINARY_SRTM_FEET_MAP_INDEX_EXT;

		String fileName = entry.targetFile.getAbsolutePath();
		if (fileName.endsWith(meterExt)) {
			fileName = fileName.replace(meterExt, feetExt);
		} else if (fileName.endsWith(feetExt)) {
			fileName = fileName.replace(feetExt, meterExt);
		}

		File previous = new File(fileName);
		if (previous != null && previous.exists()) {
			boolean successful = Algorithms.removeAllFiles(previous);
			if (successful) {
				ctx.getResourceManager().closeFile(previous.getName());
			}
		}
	}

	private void unzipFile(IndexItem.DownloadEntry de, IProgress progress,  List<InputStream> is) throws IOException {
		CountingMultiInputStream fin = new CountingMultiInputStream(is);
		int len = fin.available();
		int mb = (int) (len / (1024f*1024f));
		if(mb == 0) {
			mb = 1;
		}
		StringBuilder taskName = new StringBuilder();
		//+ de.baseName /*+ " " + mb + " MB"*/;
		taskName.append(FileNameTranslationHelper.getFileName(ctx, ctx.getRegions(), de.baseName));
		if (de.type != null) {
			taskName.append(" ").append(de.type.getString(ctx));
		}
		progress.startTask(String.format(ctx.getString(R.string.shared_string_downloading_formatted), taskName), len / 1024);
		if (!de.zipStream) {
			copyFile(de, progress, fin, len, fin, de.fileToDownload);
		} else if(de.urlToDownload.contains(".gz")) {
			GZIPInputStream zipIn = new GZIPInputStream(fin);
			copyFile(de, progress, fin, len, zipIn, de.fileToDownload);
		} else {
			if (de.unzipFolder) {
				de.fileToDownload.mkdirs();
			} 
			ZipInputStream zipIn = new ZipInputStream(fin);
			ZipEntry entry = null;
			boolean first = true;
			while ((entry = zipIn.getNextEntry()) != null) {
				if (entry.isDirectory() || entry.getName().endsWith(IndexConstants.GEN_LOG_EXT)) {
					continue;
				}
				File fs;
				if (!de.unzipFolder) {
					if (first) {
						fs = de.fileToDownload;
						first = false;
					} else {
						String name = entry.getName();
						// small simplification
						int ind = name.lastIndexOf('_');
						if (ind > 0) {
							// cut version
							int i = name.indexOf('.', ind);
							if (i > 0) {
								name = name.substring(0, ind) + name.substring(i);
							}
						}
						fs = new File(de.fileToDownload.getParent(), name);
					}
				} else {
					fs = new File(de.fileToDownload, entry.getName());
				}
				copyFile(de, progress, fin, len, zipIn, fs);
			}
			zipIn.close();
		}
		fin.close();
	}

	private void copyFile(IndexItem.DownloadEntry de, IProgress progress, 
			CountingMultiInputStream countIS, int length, InputStream toRead, File targetFile)
			throws IOException {
		targetFile.getParentFile().mkdirs();
		FileOutputStream out = new FileOutputStream(targetFile);
		try {
			int read;
			byte[] buffer = new byte[BUFFER_SIZE];
			int remaining = length;
			while ((read = toRead.read(buffer)) != -1) {
				out.write(buffer, 0, read);
				remaining -= countIS.getAndClearReadCount();
				progress.remaining(remaining / 1024);
			}
		} finally {
			out.close();
		}
		targetFile.setLastModified(de.dateModified);
	}
	
	
	public void setInterruptDownloading(boolean interruptDownloading) {
		this.interruptDownloading = interruptDownloading;
	}
	
	public boolean isInterruptDownloading() {
		return interruptDownloading;
	}
	
	private static class CountingMultiInputStream extends InputStream {

		private final InputStream[] delegate;
		private int count;
		private int currentRead;

		public CountingMultiInputStream(List<InputStream> streams) {
			this.delegate = streams.toArray(new InputStream[0]);
		}
		
		@Override
		public int read(byte[] buffer, int offset, int length)
				throws IOException {
			int r = -1;
			while (r == -1 && currentRead < delegate.length) {
				r = delegate[currentRead].read(buffer, offset, length);
				if (r == -1) {
					delegate[currentRead].close();
					currentRead++;
				}
			}
			if (r > 0) {
				this.count += r;
			}
			return r;
		}
		
		@Override
		public int read() throws IOException {
			if (currentRead >= delegate.length) {
				return -1;
			}
			int r = -1;
			while (r == -1 && currentRead < delegate.length) {
				r = delegate[currentRead].read();
				if (r == -1) {
					delegate[currentRead].close();
					currentRead++;
				} else {
					this.count++;
				}
			}
			return r;
		}

		@Override
		public int available() throws IOException {
			int av = 0;
			for (int i = currentRead; i < delegate.length; i++) {
				av += delegate[i].available();
			}
			return av;
		}

		public int getAndClearReadCount() {
			int last = count;
			count = 0;
			return last;
		}
	}
}
