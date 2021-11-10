package net.osmand.plus.settings.datastorage;

import android.net.Uri;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.backup.AbstractProgress;
import net.osmand.util.Algorithms;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

class CopyFilesAsyncTask extends AsyncTask<Void, Void, Void> {

	private final OsmandApplication app;
	private final DocumentFile folderFile;
	private final CopyFilesListener listener;

	public CopyFilesAsyncTask(@NonNull OsmandApplication app, @NonNull DocumentFile folderFile,
	                          @Nullable CopyFilesListener listener) {
		this.app = app;
		this.folderFile = folderFile;
		this.listener = listener;
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.onFilesCopyStarted();
		}
	}

	@Override
	protected Void doInBackground(Void... params) {
		copyFilesFromDir(folderFile);
		return null;
	}

	private void copyFilesFromDir(@NonNull DocumentFile documentFile) {
		if (documentFile.isDirectory()) {
			DocumentFile[] files = documentFile.listFiles();
			for (DocumentFile f : files) {
				copyFilesFromDir(f);
				//boolean deleted = f.delete();
				//Log.d("delete ", f.getName() + " deleted " + deleted);
			}
		} else {
			copyFile(documentFile);
		}
	}

	private void copyFile(@NonNull DocumentFile d) {
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
			Algorithms.streamCopy(stream, outputStream, new AbstractProgress() {
				private int work = 0;
				private int progress = 0;
				private int deltaProgress = 0;

				@Override
				public void startWork(int work) {
					if (listener != null) {
						this.work = work > 0 ? work : 1;
					}
				}

				@Override
				public void progress(int deltaWork) {
					if (listener != null) {
						deltaProgress += deltaWork;
						if ((deltaProgress > (work / 100)) || ((progress + deltaProgress) >= work)) {
							progress += deltaProgress;
							listener.onFilesCopyProgress(progress);
							deltaProgress = 0;
						}
					}
				}
			}, 1024);
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

		void onFilesCopyProgress(int progress);

		void onFilesCopyFinished();
	}
}
