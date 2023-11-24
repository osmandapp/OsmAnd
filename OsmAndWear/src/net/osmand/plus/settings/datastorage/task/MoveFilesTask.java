package net.osmand.plus.settings.datastorage.task;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.IProgress;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.base.ProgressHelper;
import net.osmand.plus.settings.backend.backup.AbstractProgress;
import net.osmand.plus.settings.datastorage.DataStorageHelper;
import net.osmand.plus.settings.datastorage.MoveFilesStopListener;
import net.osmand.plus.settings.datastorage.StorageMigrationFragment;
import net.osmand.plus.settings.datastorage.StorageMigrationListener;
import net.osmand.plus.settings.datastorage.StorageMigrationRestartListener;
import net.osmand.plus.settings.datastorage.task.StorageMigrationAsyncTask.FileCopyListener;
import net.osmand.plus.settings.datastorage.item.StorageItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FileUtils;
import net.osmand.util.Algorithms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoveFilesTask extends AsyncTask<Void, Object, Map<String, Pair<String, Long>>> {

	protected WeakReference<OsmandActionBarActivity> activity;
	private final OsmandApplication app;
	private final WeakReference<Context> context;
	private final StorageItem from;
	private final StorageItem to;

	private ProgressHelper progressHelper;
	private StorageMigrationListener migrationListener;
	private StorageMigrationRestartListener restartListener;
	private MoveFilesStopListener stopTaskListener;
	private int copyProgress;
	private final Pair<Long, Long> filesSize;
	private final List<File> files;
	private final List<File> existingFiles = new ArrayList<>();

	public MoveFilesTask(@NonNull OsmandActionBarActivity activity,
	                     @NonNull StorageItem from,
	                     @NonNull StorageItem to,
	                     @NonNull List<File> files,
	                     @NonNull Pair<Long, Long> filesSize,
	                     @Nullable StorageMigrationRestartListener listener,
	                     @Nullable MoveFilesStopListener stopTaskListener) {
		app = activity.getMyApplication();
		this.activity = new WeakReference<>(activity);
		this.context = new WeakReference<>(activity);
		this.from = from;
		this.to = to;
		this.filesSize = filesSize;
		this.files = files;
		this.restartListener = listener;
		this.stopTaskListener = stopTaskListener;
	}

	@Override
	protected void onPreExecute() {
		FragmentActivity fActivity = activity.get();
		if (AndroidUtils.isActivityNotDestroyed(fActivity)) {
			FragmentManager manager = fActivity.getSupportFragmentManager();
			progressHelper = new ProgressHelper(() -> {
				copyProgress = progressHelper.getLastKnownProgress();
				publishProgress(copyProgress);
			});
			progressHelper.setTimeInterval(100); // update progress each 100 ms
			migrationListener = StorageMigrationFragment.showInstance(manager, to, from, filesSize,
					copyProgress, files.size(), false, restartListener, stopTaskListener);
		}
	}

	@Override
	protected void onProgressUpdate(Object... values) {
		if (migrationListener != null) {
			for (Object object : values) {
				if (object instanceof String) {
					migrationListener.onFileCopyStarted((String) object);
				} else if (object instanceof Integer) {
					copyProgress = (Integer) object;
					migrationListener.onFilesCopyProgress(copyProgress);
				} else if (object instanceof Pair) {
					migrationListener.onRemainingFilesUpdate((Pair<Integer, Long>) object);
				}
			}
		}
	}

	@Override
	protected Map<String, Pair<String, Long>> doInBackground(Void... params) {
		File destinationDir = new File(to.getDirectory());
		Map<String, Pair<String, Long>> errors = new HashMap<>();
		if (!FileUtils.isWritable(destinationDir, true)) {
			return errors;
		}
		FileCopyListener fileCopyListener = getCopyFilesListener();

		long remainingSize = filesSize.first;
		long totalSize = filesSize.second / 1024;
		progressHelper.onStartWork((int) totalSize);
		for (int i = 0; i < files.size(); i++) {
			if (isCancelled()) {
				break;
			}
			File file = files.get(i);
			long fileLength = file.length();
			remainingSize -= fileLength;
			String fileName = file.getName();
			String filePathWithoutStorage = file.getAbsolutePath().replace(from.getDirectory(), "");

			fileCopyListener.onFileCopyStarted(fileName);
			File destFile = new File(to.getDirectory() + filePathWithoutStorage);
			if (!destFile.exists()) {
				String newDirectory = to.getDirectory() + filePathWithoutStorage.replace(fileName, "");
				File outputDirectory = new File(newDirectory);
				String error = copyFile(from.getDirectory(), to.getDirectory(), filePathWithoutStorage, fileName, outputDirectory, fileCopyListener);
				if (error != null) {
					errors.put(fileName, new Pair<>(error, fileLength));
				} else {
					file.delete();
				}
				fileCopyListener.onFileCopyFinished(fileName, FileUtils.APPROXIMATE_FILE_SIZE_BYTES / 1024);
			} else {
				existingFiles.add(destFile);
				int deltaProgress = (int) ((fileLength + FileUtils.APPROXIMATE_FILE_SIZE_BYTES) / 1024);
				fileCopyListener.onFileCopyFinished(fileName, deltaProgress);
			}
			publishProgress(new Pair<>(files.size() - i, remainingSize));
		}
		progressHelper.onFinishTask();
		return errors;
	}

	@Nullable
	private String copyFile(@NonNull String inputPath, @NonNull String outputPath,
	                        @NonNull String filePathWithoutStorage, @NonNull String fileName,
	                        @NonNull File outputDirectory, @NonNull FileCopyListener filesListener) {
		String error = null;
		InputStream stream = null;
		OutputStream outputStream = null;
		try {
			outputDirectory.mkdirs();
			stream = new FileInputStream(inputPath + filePathWithoutStorage);
			outputStream = new FileOutputStream(outputPath + filePathWithoutStorage);
			IProgress progress = getCopyProgress(fileName, filesListener);
			Algorithms.streamCopy(stream, outputStream, progress, 1024);
		} catch (Exception e) {
			error = e.getMessage();
		} finally {
			Algorithms.closeStream(stream);
			Algorithms.closeStream(outputStream);
		}
		return error;
	}

	@Override
	protected void onPostExecute(Map<String, Pair<String, Long>> errors) {
		changeStorage(errors);
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

	@Override
	protected void onCancelled() {
		changeStorage(null);
	}

	private void changeStorage(@Nullable Map<String, Pair<String, Long>> errors) {
		Context ctx = context.get();

		if (ctx != null && !Algorithms.isEmpty(errors)) {
			Toast.makeText(ctx, ctx.getString(R.string.shared_string_io_error), Toast.LENGTH_LONG).show();
		}

		DataStorageHelper storageHelper = new DataStorageHelper(app);
		StorageItem currentStorage = storageHelper.getCurrentStorage();
		if (!Algorithms.stringsEqual(currentStorage.getKey(), to.getKey())) {
			File dir = new File(to.getDirectory());
			DataStorageHelper.saveFilesLocation(app, activity.get(), to.getType(), dir);
		}
		app.getResourceManager().resetStoreDirectory();
		// immediately proceed with change (to not loose where maps are currently located)

		if (migrationListener != null && !isCancelled()) {
			migrationListener.onFilesCopyFinished(errors == null ? new HashMap<>() : errors, existingFiles);
		}
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
}
