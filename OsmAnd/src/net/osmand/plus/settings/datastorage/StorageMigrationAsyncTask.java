package net.osmand.plus.settings.datastorage;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.AbstractProgress;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class StorageMigrationAsyncTask extends AsyncTask<Void, Object, Map<String, String>> {

	public final static Log log = PlatformUtil.getLog(StorageMigrationAsyncTask.class);

	private final OsmandApplication app;
	private final List<DocumentFile> documentFiles;
	private final WeakReference<FragmentActivity> activityRef;
	private StorageMigrationListener migrationListener;

	protected int generalProgress;
	protected final long filesSize;
	protected final boolean usedOnMap;

	public StorageMigrationAsyncTask(@NonNull FragmentActivity activity,
	                                 @NonNull List<DocumentFile> documentFiles,
	                                 long filesSize,
	                                 boolean usedOnMap) {
		activityRef = new WeakReference<>(activity);
		this.app = (OsmandApplication) activity.getApplication();
		this.usedOnMap = usedOnMap;
		this.filesSize = filesSize;
		this.documentFiles = documentFiles;
	}

	@Override
	protected void onPreExecute() {
		FragmentActivity activity = activityRef.get();
		if (AndroidUtils.isActivityNotDestroyed(activity)) {
			FragmentManager manager = activity.getSupportFragmentManager();
			migrationListener = StorageMigrationFragment.showInstance(manager, generalProgress,
					documentFiles.size(), filesSize, usedOnMap);
		}
	}

	@Override
	protected Map<String, String> doInBackground(Void... params) {
		Map<String, String> errors = new HashMap<>();
		CopyFilesListener copyFilesListener = getCopyFilesListener(filesSize / 1024);

		long remainingSize = filesSize;
		for (int i = 0; i < documentFiles.size(); i++) {
			DocumentFile file = documentFiles.get(i);
			remainingSize -= file.length();

			Uri uri = file.getUri();
			String fileName = getFileName(uri, file.getName());
			String error = copyFile(uri, fileName, copyFilesListener);
			if (error == null) {
				file.delete();
			} else {
				errors.put(fileName, error);
			}
			publishProgress(new Pair<>(documentFiles.size() - i, remainingSize));
		}
		return errors;
	}

	@Override
	protected void onProgressUpdate(Object... values) {
		if (migrationListener != null) {
			for (Object object : values) {
				if (object instanceof String) {
					migrationListener.onFileCopyStarted((String) object);
				} else if (object instanceof Integer) {
					generalProgress = (Integer) object;
					migrationListener.onFilesCopyProgress(generalProgress);
				} else if (object instanceof Pair) {
					migrationListener.onRemainingFilesUpdate((Pair<Integer, Long>) object);
				}
			}
		}
	}

	@Override
	protected void onPostExecute(Map<String, String> errors) {
		if (migrationListener != null) {
			migrationListener.onFilesCopyFinished(errors);
		}
	}

	@Nullable
	private String copyFile(@NonNull Uri uri, @NonNull String fileName, @Nullable CopyFilesListener filesListener) {
		String error = null;
		InputStream stream = null;
		OutputStream outputStream = null;
		try {
			if (filesListener != null) {
				filesListener.onFileCopyStarted(fileName);
			}
			File destFile = app.getAppPath(fileName);
			if (destFile.exists()) {
				error = app.getString(R.string.file_with_name_already_exists);
			} else {
				File dir = destFile.getParentFile();
				if (dir != null) {
					dir.mkdirs();
				}
				stream = app.getContentResolver().openInputStream(uri);
				if (stream != null) {
					outputStream = new FileOutputStream(destFile);
					IProgress progress = getCopyProgress(fileName, filesListener);
					Algorithms.streamCopy(stream, outputStream, progress, 1024);
				}
			}
		} catch (Exception e) {
			log.error(e);
			error = e.getMessage();
		} finally {
			Algorithms.closeStream(stream);
			Algorithms.closeStream(outputStream);
		}
		return error;
	}

	private IProgress getCopyProgress(@NonNull String fileName, @Nullable CopyFilesListener filesListener) {
		return new AbstractProgress() {

			private int progress = 0;

			@Override
			public void progress(int deltaWork) {
				if (filesListener != null) {
					progress += deltaWork;
					filesListener.onFileCopyProgress(fileName, progress, deltaWork);
				}
			}
		};
	}

	private String getFileName(@NonNull Uri uri, @Nullable String fileName) {
		String path = uri.getLastPathSegment();
		if (path != null) {
			int ps = path.indexOf("/");
			if (ps != -1 && ps < path.length() - 1) {
				return path.substring(ps + 1);
			}
		}
		return fileName;
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

	public interface CopyFilesListener {
		void onFileCopyStarted(@NonNull String fileName);

		void onFileCopyProgress(@NonNull String fileName, int progress, int deltaWork);
	}

	public interface StorageMigrationListener {

		void onFileCopyStarted(@NonNull String path);

		void onFilesCopyProgress(int progress);

		void onFilesCopyFinished(@NonNull Map<String, String> errors);

		void onRemainingFilesUpdate(@NonNull Pair<Integer, Long> pair);

	}
}
