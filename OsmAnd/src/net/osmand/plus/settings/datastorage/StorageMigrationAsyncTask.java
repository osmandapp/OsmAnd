package net.osmand.plus.settings.datastorage;

import android.net.Uri;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.backup.AbstractProgress;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

class StorageMigrationAsyncTask extends AsyncTask<Void, Object, Void> {

	public final static Log log = PlatformUtil.getLog(StorageMigrationAsyncTask.class);

	private final OsmandApplication app;
	private final List<DocumentFile> documentFiles;
	private final WeakReference<FragmentActivity> activityRef;
	private StorageMigrationListener listener;

	private final long filesSize;
	private final boolean usedOnMap;
	private int generalProgress;

	public StorageMigrationAsyncTask(@NonNull FragmentActivity activity, @NonNull List<DocumentFile> documentFiles, boolean usedOnMap) {
		this.app = (OsmandApplication) activity.getApplication();
		this.usedOnMap = usedOnMap;
		this.documentFiles = documentFiles;
		filesSize = getFilesSize(documentFiles);
		activityRef = new WeakReference<>(activity);
	}

	@Override
	protected void onPreExecute() {
		FragmentActivity activity = activityRef.get();
		if (AndroidUtils.isActivityNotDestroyed(activity)) {
			FragmentManager manager = activity.getSupportFragmentManager();
			listener = StorageMigrationFragment.showInstance(manager, documentFiles, usedOnMap);
		}
	}

	@Override
	protected Void doInBackground(Void... params) {
		CopyFilesListener copyFilesListener = getCopyFilesListener(filesSize / 1024);
		List<DocumentFile> remainingFiles = new ArrayList<>(documentFiles);
		for (DocumentFile file : documentFiles) {
			copyFile(file, copyFilesListener);
			remainingFiles.remove(file);
			publishProgress(remainingFiles);
		}
		return null;
	}

	@Override
	protected void onProgressUpdate(Object... values) {
		if (listener != null) {
			for (Object object : values) {
				if (object instanceof String) {
					listener.onFileCopyStarted((String) object);
				} else if (object instanceof Integer) {
					generalProgress = (Integer) object;
					listener.onFilesCopyProgress(generalProgress);
				} else if (object instanceof List) {
					List<DocumentFile> remainingFiles = (List<DocumentFile>) object;
					listener.onRemainingFilesUpdate(remainingFiles);
				}
			}
		}
	}

	@Override
	protected void onPostExecute(Void aVoid) {
		if (listener != null) {
			listener.onFilesCopyFinished();
		}
	}

	private void copyFile(@NonNull DocumentFile documentFile, CopyFilesListener uploadListener) {
		try {
			Uri childUri = documentFile.getUri();
			String name = documentFile.getName();
			String fileName = name;
			String path = documentFile.getUri().getLastPathSegment();
			if (path != null) {
				int ps = path.indexOf("/");
				if (ps != -1 && ps < path.length() - 1) {
					fileName = path.substring(ps + 1);
				}
			}
			File testDir = new File(app.getAppPath(null), fileName);
			File parentFile = testDir.getParentFile();
			if (parentFile != null) {
				parentFile.mkdirs();
			}
			InputStream stream = app.getContentResolver().openInputStream(childUri);
			FileOutputStream outputStream = new FileOutputStream(testDir);

			IProgress progress = new AbstractProgress() {

				private int progress = 0;

				@Override
				public void startWork(int work) {
					uploadListener.onFileCopyStarted(name);
				}

				@Override
				public void progress(int deltaWork) {
					progress += deltaWork;
					uploadListener.onFileCopyProgress(name, progress, deltaWork);
				}
			};
			progress.startWork((int) documentFile.length() / 1024);

			Algorithms.streamCopy(stream, outputStream, progress, 1024);
			stream.close();
			outputStream.close();
			documentFile.delete();
		} catch (IOException e) {
			log.error(e);
		}
	}

	private CopyFilesListener getCopyFilesListener(long size) {
		return new CopyFilesListener() {

			private int deltaProgress = 0;

			@Override
			public void onFileCopyStarted(@NonNull String fileName) {
				publishProgress(fileName);
			}

			@Override
			public void onFileCopyProgress(@NonNull String fileName, int progress, int deltaWork) {
				deltaProgress += deltaWork;
				if ((deltaProgress > (size / 100)) || ((progress + deltaProgress) >= size)) {
					generalProgress += deltaProgress;
					publishProgress(generalProgress);
					deltaProgress = 0;
				}
			}
		};
	}

	public static long getFilesSize(@NonNull List<DocumentFile> documentFiles) {
		long size = 0;
		for (DocumentFile file : documentFiles) {
			size += file.length();
		}
		return size;
	}

	public interface CopyFilesListener {
		void onFileCopyStarted(@NonNull String fileName);

		void onFileCopyProgress(@NonNull String fileName, int progress, int deltaWork);
	}

	public interface StorageMigrationListener {

		void onFileCopyStarted(String fileName);

		void onFilesCopyProgress(int progress);

		void onFilesCopyFinished();

		void onRemainingFilesUpdate(List<DocumentFile> remainingFiles);

	}
}
