package net.osmand.plus.settings.datastorage.task;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.ProgressHelper;
import net.osmand.plus.settings.backend.backup.AbstractProgress;
import net.osmand.plus.settings.datastorage.DataStorageHelper;
import net.osmand.plus.settings.datastorage.StorageMigrationFragment;
import net.osmand.plus.settings.datastorage.StorageMigrationListener;
import net.osmand.plus.settings.datastorage.item.StorageItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FileUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StorageMigrationAsyncTask extends AsyncTask<Void, Object, Map<String, Pair<String, Long>>> {

	public static final Log log = PlatformUtil.getLog(StorageMigrationAsyncTask.class);

	private final OsmandApplication app;
	private final StorageItem selectedStorage;
	private final Pair<Long, Long> filesSize;
	private final List<DocumentFile> documentFiles;
	private final List<File> existingFiles = new ArrayList<>();

	private final WeakReference<FragmentActivity> activityRef;
	private StorageMigrationListener migrationListener;
	private ProgressHelper progressHelper;

	private int generalProgress;
	private final boolean usedOnMap;

	public StorageMigrationAsyncTask(@NonNull FragmentActivity activity,
	                                 @NonNull List<DocumentFile> documentFiles,
	                                 @NonNull StorageItem selectedStorage,
	                                 @NonNull Pair<Long, Long> filesSize,
	                                 boolean usedOnMap) {
		activityRef = new WeakReference<>(activity);
		this.app = (OsmandApplication) activity.getApplication();
		this.usedOnMap = usedOnMap;
		this.filesSize = filesSize;
		this.documentFiles = documentFiles;
		this.selectedStorage = selectedStorage;

	}

	@Override
	protected void onPreExecute() {
		FragmentActivity activity = activityRef.get();
		if (AndroidUtils.isActivityNotDestroyed(activity)) {
			StorageItem sharedStorage;
			DataStorageHelper storageHelper = new DataStorageHelper(app);
			sharedStorage = storageHelper.getCurrentStorage();
			progressHelper = new ProgressHelper(() -> {
				generalProgress = progressHelper.getLastKnownProgress();
				publishProgress(generalProgress);
			});
			progressHelper.setTimeInterval(100); // update progress each 100 ms

			FragmentManager manager = activity.getSupportFragmentManager();
			migrationListener = StorageMigrationFragment.showInstance(manager, selectedStorage, sharedStorage, filesSize,
					generalProgress, documentFiles.size(), usedOnMap, null, null);
		}
	}

	@Override
	protected Map<String, Pair<String, Long>> doInBackground(Void... params) {
		Map<String, Pair<String, Long>> errors = new HashMap<>();
		FileCopyListener fileCopyListener = getCopyFilesListener();

		long remainingSize = filesSize.first;
		long totalSize = filesSize.second / 1024;
		progressHelper.onStartWork((int) totalSize);
		for (int i = 0; i < documentFiles.size(); i++) {
			DocumentFile file = documentFiles.get(i);
			long fileLength = file.length();
			remainingSize -= fileLength;

			Uri uri = file.getUri();
			String fileName = getFileName(uri, file.getName());
			if (fileName != null) {
				fileCopyListener.onFileCopyStarted(fileName);

				File destFile = new File(selectedStorage.getDirectory(), fileName);
				if (!destFile.exists()) {
					String error = copyFile(uri, fileName, fileCopyListener);
					if (error == null) {
						file.delete();
					} else {
						errors.put(fileName, new Pair<>(error, fileLength));
					}
					fileCopyListener.onFileCopyFinished(fileName, FileUtils.APPROXIMATE_FILE_SIZE_BYTES / 1024);
				} else {
					existingFiles.add(destFile);
					int progress = (int) ((fileLength + FileUtils.APPROXIMATE_FILE_SIZE_BYTES) / 1024);
					fileCopyListener.onFileCopyFinished(fileName, progress);
				}
			}
			publishProgress(new Pair<>(documentFiles.size() - i, remainingSize));
		}
		progressHelper.onFinishTask();
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
	protected void onPostExecute(Map<String, Pair<String, Long>> errors) {
		DataStorageHelper storageHelper = new DataStorageHelper(app);
		StorageItem currentStorage = storageHelper.getCurrentStorage();
		if (!Algorithms.stringsEqual(currentStorage.getKey(), selectedStorage.getKey())) {
			File dir = new File(selectedStorage.getDirectory());
			DataStorageHelper.saveFilesLocation(app, activityRef.get(), selectedStorage.getType(), dir);
		}
		if (migrationListener != null) {
			migrationListener.onFilesCopyFinished(errors, existingFiles);
		}
	}

	@Nullable
	private String copyFile(@NonNull Uri uri, @NonNull String fileName, @NonNull FileCopyListener filesListener) {
		String error = null;
		InputStream stream = null;
		OutputStream outputStream = null;
		try {
			File destFile = new File(selectedStorage.getDirectory(), fileName);
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
		} catch (Exception e) {
			log.error(e);
			error = e.getMessage();
		} finally {
			Algorithms.closeStream(stream);
			Algorithms.closeStream(outputStream);
		}
		return error;
	}

	@NonNull
	private IProgress getCopyProgress(@NonNull String fileName, @NonNull FileCopyListener filesListener) {
		return new AbstractProgress() {
			@Override
			public void progress(int deltaWork) {
				filesListener.onFileCopyProgress(fileName, deltaWork);
			}
		};
	}

	@Nullable
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

	@NonNull
	private FileCopyListener getCopyFilesListener() {
		return new FileCopyListener() {

			@Override
			public void onFileCopyStarted(@NonNull String fileName) {
				publishProgress(fileName);
			}

			@Override
			public void onFileCopyProgress(@NonNull String fileName, int deltaWork) {
				progressHelper.onProgress(deltaWork);
			}

			@Override
			public void onFileCopyFinished(@NonNull String fileName, int deltaWork) {
				progressHelper.onProgress(deltaWork);
			}
		};
	}

	public interface FileCopyListener {
		default void onFileCopyStarted(@NonNull String fileName) { }
		default void onFileCopyProgress(@NonNull String fileName, int deltaWork) { }
		default void onFileCopyFinished(@NonNull String fileName, int deltaWork) { }
	}
}
