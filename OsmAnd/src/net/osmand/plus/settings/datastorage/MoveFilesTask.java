package net.osmand.plus.settings.datastorage;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.ProgressImplementation;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.settings.datastorage.item.StorageItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

public class MoveFilesTask extends AsyncTask<Void, Object, Boolean> {

	protected WeakReference<OsmandActionBarActivity> activity;
	private final WeakReference<Context> context;
	private final StorageItem from;
	private final StorageItem to;
	protected ProgressImplementation progress;
	private Runnable runOnSuccess;
	private int movedCount;
	private long movedSize;
	private int copiedCount;
	private long copiedSize;
	private int failedCount;
	private long failedSize;
	private String exceptionMessage;

	private StorageMigrationListener migrationListener;

	private Pair<Long, Long> filesSize;
	private List<DocumentFile> documentFiles;
	private int copyProgress;

	public MoveFilesTask(OsmandActionBarActivity activity,
	                     StorageItem from,
	                     StorageItem to,
	                     @NonNull List<DocumentFile> documentFiles,
	                     @NonNull Pair<Long, Long> filesSize) {
		this.activity = new WeakReference<>(activity);
		this.context = new WeakReference<>(activity);
		this.from = from;
		this.to = to;
		this.filesSize = filesSize;
		this.documentFiles = documentFiles;
	}

	public void setRunOnSuccess(Runnable runOnSuccess) {
		this.runOnSuccess = runOnSuccess;
	}

	public int getMovedCount() {
		return movedCount;
	}

	public int getCopiedCount() {
		return copiedCount;
	}

	public int getFailedCount() {
		return failedCount;
	}

	public long getMovedSize() {
		return movedSize;
	}

	public long getCopiedSize() {
		return copiedSize;
	}

	public long getFailedSize() {
		return failedSize;
	}

	@Override
	protected void onPreExecute() {
		Context ctx = context.get();
		if (context == null) {
			return;
		}
		movedCount = 0;
		copiedCount = 0;
		failedCount = 0;
/*		progress = ProgressImplementation.createProgressDialog(
				ctx, ctx.getString(R.string.copying_osmand_files),
				ctx.getString(R.string.copying_osmand_files_descr, to.getPath()),
				ProgressDialog.STYLE_HORIZONTAL);*/

		FragmentActivity fActivity = activity.get();
		if (AndroidUtils.isActivityNotDestroyed(fActivity)) {
			FragmentManager manager = fActivity.getSupportFragmentManager();

			copyProgress = 0;
			migrationListener = StorageMigrationFragment.showInstance(manager, to, filesSize,
					copyProgress, documentFiles.size(), true);
		}
	}

	@Override
	protected void onPostExecute(Boolean result) {
		Context ctx = context.get();
		if (ctx == null) {
			return;
		}
		if (result != null) {
			if (result.booleanValue() && runOnSuccess != null) {
				runOnSuccess.run();
			} else if (!result.booleanValue()) {
				Toast.makeText(ctx, ctx.getString(R.string.shared_string_io_error) + ": " + exceptionMessage, Toast.LENGTH_LONG).show();
			}
		}
		try {
/*			if (progress.getDialog().isShowing()) {
				//progress.getDialog().dismiss();
			}*/
		} catch (Exception e) {
			//ignored
		}
	}

	private void movingFiles(File f, File t, int depth) throws IOException {
		Context ctx = context.get();
		if (ctx == null) {
			return;
		}
		if (depth <= 2) {
			//progress.startTask(ctx.getString(R.string.copying_osmand_one_file_descr, t.getName()), -1);
			//migrationListener.onFileCopyStarted(ctx.getString(R.string.copying_osmand_one_file_descr, t.getName()));
			publishProgress(t.getName());
		}
		if (f.isDirectory()) {
			t.mkdirs();
			File[] lf = f.listFiles();
			if (lf != null) {
				for (int i = 0; i < lf.length; i++) {
					if (lf[i] != null) {
						movingFiles(lf[i], new File(t, lf[i].getName()), depth + 1);
					}
				}
			}
			f.delete();
		} else if (f.isFile()) {
			if (t.exists()) {
				Algorithms.removeAllFiles(t);
			}
			boolean rnm = false;
			long fileSize = f.length();
			try {
				rnm = f.renameTo(t);
				movedCount++;
				movedSize += fileSize;
			} catch (RuntimeException e) {
			}
			if (!rnm) {
				FileInputStream fin = new FileInputStream(f);
				FileOutputStream fout = new FileOutputStream(t);
				try {
					//progress.startTask(ctx.getString(R.string.copying_osmand_one_file_descr, t.getName()), (int) (f.length() / 1024));
					//migrationListener.onFileCopyStarted(ctx.getString(R.string.copying_osmand_one_file_descr, t.getName()));
					publishProgress(t.getName());

					//Algorithms.streamCopy(fin, fout, progress, 1024);
					Algorithms.streamCopy(fin, fout);
					copiedCount++;
					copiedSize += fileSize;
				} catch (IOException e) {
					failedCount++;
					failedSize += fileSize;
				} finally {
					fin.close();
					fout.close();
				}
				f.delete();
			}
		}
		if (depth <= 2) {
			//progress.finishTask();
		}
	}

	@Override
	protected void onProgressUpdate(Object... values) {
		if (migrationListener != null) {
			for (Object object : values) {
				if (object instanceof String) {
					migrationListener.onFileCopyStarted((String) object);
				} /*else if (object instanceof Integer) {
					generalProgress = (Integer) object;
					migrationListener.onFilesCopyProgress(generalProgress);
				} else if (object instanceof Pair) {
					migrationListener.onRemainingFilesUpdate((Pair<Integer, Long>) object);
				}*/
			}
		}
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		File fromDirectory = new File(from.getDirectory());
		File toDirectory = new File(to.getDirectory());
		fromDirectory.mkdirs();
		try {
			movingFiles(fromDirectory, toDirectory, 0);
		} catch (IOException e) {
			exceptionMessage = e.getMessage();
			return false;
		}
		return true;
	}
}
