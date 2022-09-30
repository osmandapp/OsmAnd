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
import net.osmand.plus.settings.datastorage.StorageMigrationFragment;
import net.osmand.plus.settings.datastorage.StorageMigrationListener;
import net.osmand.plus.settings.datastorage.StorageMigrationRestartListener;
import net.osmand.plus.settings.datastorage.task.StorageMigrationAsyncTask.CopyFilesListener;
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

	private StorageMigrationListener migrationListener;
	private StorageMigrationRestartListener restartListener;

	private int copyProgress;
	private final Pair<Long, Long> filesSize;
	private final List<File> files;
	private final List<File> existingFiles = new ArrayList<>();

	public MoveFilesTask(@NonNull OsmandActionBarActivity activity,
	                     @NonNull StorageItem from,
	                     @NonNull StorageItem to,
	                     @NonNull List<File> files,
	                     @NonNull Pair<Long, Long> filesSize,
	                     @Nullable StorageMigrationRestartListener listener) {
		app = activity.getMyApplication();
		this.activity = new WeakReference<>(activity);
		this.context = new WeakReference<>(activity);
		this.from = from;
		this.to = to;
		this.filesSize = filesSize;
		this.files = files;
		this.restartListener = listener;
	}

	@Override
	protected void onPreExecute() {
		FragmentActivity fActivity = activity.get();
		if (AndroidUtils.isActivityNotDestroyed(fActivity)) {
			FragmentManager manager = fActivity.getSupportFragmentManager();
			migrationListener = StorageMigrationFragment.showInstance(manager, to, from, filesSize,
					copyProgress, files.size(), false, restartListener);
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
		Map<String, Pair<String, Long>> errors = new HashMap<>();
		CopyFilesListener copyFilesListener = getCopyFilesListener(filesSize.second / 1024);

		long remainingSize = filesSize.first;
		for (int i = 0; i < files.size(); i++) {
			File file = files.get(i);
			long fileLength = file.length();
			remainingSize -= fileLength;
			String fileName = file.getName();
			String filePathWithoutStorage = file.getAbsolutePath().replace(from.getDirectory(), "");

			copyFilesListener.onFileCopyStarted(fileName);
			File destFile = new File(to.getDirectory() + filePathWithoutStorage);
			if (!destFile.exists()) {
				String error = copyFile(from.getDirectory(), to.getDirectory(), filePathWithoutStorage, fileName, copyFilesListener);
				if (error != null) {
					errors.put(fileName, new Pair<>(error, fileLength));
				}
				copyFilesListener.onFileCopyFinished(fileName, FileUtils.APPROXIMATE_FILE_SIZE_BYTES / 1024);
			} else {
				existingFiles.add(destFile);
				int progress = (int) ((fileLength + FileUtils.APPROXIMATE_FILE_SIZE_BYTES) / 1024);
				copyFilesListener.onFileCopyFinished(fileName, progress);
			}
			file.delete();
			publishProgress(new Pair<>(files.size() - i, remainingSize));
		}
		return errors;
	}

	@Nullable
	private String copyFile(@NonNull String inputPath, @NonNull String outputPath, @NonNull String filePathWithoutStorage, @NonNull String fileName, @NonNull CopyFilesListener filesListener) {
		String error = null;
		InputStream stream = null;
		OutputStream outputStream = null;
		try {
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
		Context ctx = context.get();
		if (ctx != null && !errors.isEmpty()) {
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

		if (migrationListener != null) {
			migrationListener.onFilesCopyFinished(errors, existingFiles);
		}
	}

	@NonNull
	private IProgress getCopyProgress(@NonNull String fileName, @NonNull CopyFilesListener filesListener) {
		return new AbstractProgress() {

			private int progress;

			@Override
			public void progress(int deltaWork) {
				progress += deltaWork;
				filesListener.onFileCopyProgress(fileName, progress, deltaWork);
			}
		};
	}

	@NonNull
	private CopyFilesListener getCopyFilesListener(long size) {
		return new CopyFilesListener() {

			private ProgressHelper progressHelper;

			@Override
			public void onFileCopyStarted(@NonNull String fileName) {
				progressHelper = new ProgressHelper(() -> {
					copyProgress += progressHelper.getLastAddedDeltaProgress();
					publishProgress(copyProgress);
				});
				progressHelper.onStartWork((int) size);
				publishProgress(fileName);
			}

			@Override
			public void onFileCopyProgress(@NonNull String fileName, int p, int deltaWork) {
				progressHelper.onProgress(deltaWork);
			}

			@Override
			public void onFileCopyFinished(@NonNull String fileName, int deltaWork) {
				progressHelper.onProgress(deltaWork);
			}
		};
	}
}
