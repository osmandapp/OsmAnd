package net.osmand.plus.settings.datastorage;

import android.net.Uri;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import net.osmand.IProgress;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.backup.AbstractProgress;
import net.osmand.util.Algorithms;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

class CopyFilesAsyncTask extends AsyncTask<Void, Object, Void> {

	private final OsmandApplication app;
	private final DocumentFile folderFile;
	private final CopyFilesListener listener;
	private final long size;

	public CopyFilesAsyncTask(@NonNull OsmandApplication app, @NonNull DocumentFile folderFile,
	                          @Nullable CopyFilesListener listener, long size) {
		this.app = app;
		this.folderFile = folderFile;
		this.listener = listener;
		this.size = size;
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.onFilesCopyStarted();
		}
	}

	@Override
	protected Void doInBackground(Void... params) {
		OnUploadItemListener uploadListener = getOnUploadItemListener(size);
		copyFilesFromDir(folderFile, uploadListener);
		return null;
	}

	private OnUploadItemListener getOnUploadItemListener(long size) {
		return new OnUploadItemListener() {

			private int progress = 0;
			private int deltaProgress = 0;

			@Override
			public void onItemUploadStarted(@NonNull String fileName) {
				publishProgress(fileName);
			}

			@Override
			public void onItemUploadProgress(@NonNull String fileName, int progress, int deltaWork) {
				deltaProgress += deltaWork;
				if ((deltaProgress > (size / 100)) || ((progress + deltaProgress) >= size)) {
					this.progress += deltaProgress;
					publishProgress(this.progress);
					deltaProgress = 0;
				}
			}
		};
	}

	@Override
	protected void onProgressUpdate(Object... values) {
		if (listener != null) {
			for (Object object : values) {
				if (object instanceof String) {
					listener.onFileCopyStarted((String) object);
				} else if (object instanceof Integer) {
					listener.onFilesCopyProgress((Integer) object);
				}
			}
		}
	}

	public interface OnUploadItemListener {
		void onItemUploadStarted(@NonNull String fileName);

		void onItemUploadProgress(@NonNull String fileName, int progress, int deltaWork);
	}

	private void copyFilesFromDir(@NonNull DocumentFile documentFile, OnUploadItemListener uploadListener) {
		if (documentFile.isDirectory()) {
			DocumentFile[] files = documentFile.listFiles();
			for (DocumentFile f : files) {
				copyFilesFromDir(f, uploadListener);
				//boolean deleted = f.delete();
				//Log.d("delete ", f.getName() + " deleted " + deleted);
			}
		} else {
			copyFile(documentFile, uploadListener);
		}
	}

	private void copyFile(@NonNull DocumentFile d, OnUploadItemListener uploadListener) {
		try {
			Uri childuri = d.getUri();
			String name = d.getName();
			String fileName = name;
			String path = d.getUri().getLastPathSegment();
			int ps = path.indexOf("/");
			if (ps != -1 && ps < path.length() - 1) {
				fileName = path.substring(ps + 1);
			}
			File testDir = new File(app.getAppPath(null), fileName);
			File parentFile = testDir.getParentFile();
			if (parentFile != null) {
				parentFile.mkdirs();
			}
			InputStream stream = app.getContentResolver().openInputStream(childuri);
			FileOutputStream outputStream = new FileOutputStream(testDir);

			IProgress progress = new AbstractProgress() {
				private int work = 0;
				private int progress = 0;
				private int deltaProgress = 0;

				@Override
				public void startWork(int work) {
					this.work = work > 0 ? work : 1;
					uploadListener.onItemUploadStarted(name);
				}

				@Override
				public void progress(int deltaWork) {
					deltaProgress += deltaWork;
					if ((deltaProgress > (work / 100)) || ((progress + deltaProgress) >= work)) {
						progress += deltaProgress;
						uploadListener.onItemUploadProgress(name, progress, deltaProgress);
						deltaProgress = 0;
					}
				}
			};
			progress.startWork((int) d.length());

			Algorithms.streamCopy(stream, outputStream, progress, 1024);
			stream.close();
			outputStream.close();

			//boolean deleted = d.delete();
			//Log.d("delete ", d.getName() + " deleted " + deleted);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onPostExecute(Void aVoid) {
		if (listener != null) {
			listener.onFilesCopyFinished();
		}
	}

	public interface CopyFilesListener {

		void onFilesCopyStarted();

		void onFileCopyStarted(String fileName);

		void onFilesCopyProgress(int progress);

		void onFilesCopyFinished();
	}
}
