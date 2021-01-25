package net.osmand;

import android.os.AsyncTask;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.Metadata;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.dialogs.RenameFileBottomSheet;
import net.osmand.util.Algorithms;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

public class FileUtils {

	public static final Pattern ILLEGAL_FILE_NAME_CHARACTERS = Pattern.compile("[?:\"*|/<>]");
	public static final Pattern ILLEGAL_PATH_NAME_CHARACTERS = Pattern.compile("[?:\"*|<>]");

	public static void renameFile(@NonNull FragmentActivity activity, @NonNull File file,
								  @Nullable Fragment target, boolean usedOnMap) {
		if (file.exists()) {
			FragmentManager fragmentManager = activity.getSupportFragmentManager();
			RenameFileBottomSheet.showInstance(fragmentManager, target, file, usedOnMap);
		}
	}

	public static File renameSQLiteFile(OsmandApplication ctx, File source, String newName,
										RenameCallback callback) {
		File dest = checkRenamePossibility(ctx, source, newName, false);
		if (dest == null) {
			return null;
		}
		if (!dest.getParentFile().exists()) {
			dest.getParentFile().mkdirs();
		}
		if (source.renameTo(dest)) {
			final String[] suffixes = new String[] {"-journal", "-wal", "-shm"};
			for (String s : suffixes) {
				File file = new File(ctx.getDatabasePath(source + s).toString());
				if (file.exists()) {
					file.renameTo(ctx.getDatabasePath(dest + s));
				}
			}
			if (callback != null) {
				callback.renamedTo(dest);
			}
			return dest;
		} else {
			Toast.makeText(ctx, R.string.file_can_not_be_renamed, Toast.LENGTH_LONG).show();
		}
		return null;
	}

	public static File renameGpxFile(@NonNull OsmandApplication app, @NonNull File source,
									 @NonNull String newName, boolean dirAllowed, @Nullable RenameCallback callback) {
		File dest = checkRenamePossibility(app, source, newName, dirAllowed);
		if (dest == null) {
			return null;
		}
		File res = renameGpxFile(app, source, dest);
		if (res != null) {
			if (callback != null) {
				callback.renamedTo(res);
			}
		} else {
			Toast.makeText(app, R.string.file_can_not_be_renamed, Toast.LENGTH_LONG).show();
		}
		return res;
	}

	public static File renameFile(@NonNull OsmandApplication app, @NonNull File source,
								  @NonNull String newName, boolean dirAllowed, RenameCallback callback) {
		File dest = checkRenamePossibility(app, source, newName, dirAllowed);
		if (dest == null) {
			return null;
		}
		if (!dest.getParentFile().exists()) {
			dest.getParentFile().mkdirs();
		}
		File res = source.renameTo(dest) ? dest : null;
		if (res != null) {
			if (callback != null) {
				callback.renamedTo(res);
			}
		} else {
			Toast.makeText(app, R.string.file_can_not_be_renamed, Toast.LENGTH_LONG).show();
		}
		return res;
	}

	public static File renameGpxFile(@NonNull OsmandApplication app, @NonNull File src, @NonNull File dest) {
		if (!dest.getParentFile().exists()) {
			dest.getParentFile().mkdirs();
		}
		if (src.renameTo(dest)) {
			GpxSelectionHelper helper = app.getSelectedGpxHelper();
			SelectedGpxFile selected = helper.getSelectedFileByPath(src.getAbsolutePath());
			app.getGpxDbHelper().rename(src, dest);
			if (selected != null && selected.getGpxFile() != null) {
				selected.resetSplitProcessed();
				selected.getGpxFile().path = dest.getAbsolutePath();
				helper.updateSelectedGpxFile(selected);
			}
			RenameGpxAsyncTask renameGpxAsyncTask = new RenameGpxAsyncTask(app, dest);
			renameGpxAsyncTask.execute();
			return dest;
		}
		return null;
	}

	public static boolean removeGpxFile(@NonNull OsmandApplication app, @NonNull File file) {
		if (file.exists()) {
			GpxSelectionHelper helper = app.getSelectedGpxHelper();
			SelectedGpxFile selected = helper.getSelectedFileByPath(file.getAbsolutePath());
			file.delete();
			app.getGpxDbHelper().remove(file);
			if (selected != null && selected.getGpxFile() != null) {
				helper.selectGpxFile(selected.getGpxFile(), false, false);
			}
			return true;
		}
		return false;
	}

	public static File checkRenamePossibility(OsmandApplication ctx, File source, String newName, boolean dirAllowed) {
		if (Algorithms.isEmpty(newName)) {
			Toast.makeText(ctx, R.string.empty_filename, Toast.LENGTH_LONG).show();
			return null;
		}
		Pattern illegalCharactersPattern = dirAllowed ? ILLEGAL_PATH_NAME_CHARACTERS : ILLEGAL_FILE_NAME_CHARACTERS;
		if (illegalCharactersPattern.matcher(newName).find()) {
			Toast.makeText(ctx, R.string.file_name_containes_illegal_char, Toast.LENGTH_LONG).show();
			return null;
		}
		File dest = new File(source.getParentFile(), newName);
		if (dest.exists()) {
			Toast.makeText(ctx, R.string.file_with_name_already_exists, Toast.LENGTH_LONG).show();
			return null;
		}
		return dest;
	}

	public static String createUniqueFileName(@NonNull OsmandApplication app, String name, String dirName, String extension) {
		String uniqueFileName = name;
		File dir = app.getAppPath(dirName);
		File fout = new File(dir, name + extension);
		int ind = 0;
		while (fout.exists()) {
			uniqueFileName = name + "_" + (++ind);
			fout = new File(dir, uniqueFileName + extension);
		}
		return uniqueFileName;
	}

	public static File backupFile(@NonNull OsmandApplication app, @NonNull File src) {
		if (!src.exists()) {
			return null;
		}
		File tempDir = getTempDir(app);
		File dest = new File(tempDir, src.getName());
		try {
			Algorithms.fileCopy(src, dest);
		} catch (IOException e) {
			return null;
		}
		return dest;
	}

	public static File getTempDir(OsmandApplication app) {
		File tempDir = app.getAppPath(IndexConstants.TEMP_DIR);
		if (!tempDir.exists()) {
			tempDir.mkdirs();
		}
		return tempDir;
	}

	public static boolean isWritable(File dirToTest) {
		boolean isWriteable;
		try {
			dirToTest.mkdirs();
			File writeTestFile = File.createTempFile("osmand_", ".tmp", dirToTest);
			isWriteable = writeTestFile.exists();
			writeTestFile.delete();
		} catch (IOException e) {
			isWriteable = false;
		}
		return isWriteable;
	}

	public interface RenameCallback {
		void renamedTo(File file);
	}

	private static class RenameGpxAsyncTask extends AsyncTask<Void, Void, Exception> {

		private OsmandApplication app;
		private File file;

		private RenameGpxAsyncTask(@NonNull OsmandApplication app, @NonNull File file) {
			this.app = app;
			this.file = file;
		}

		@Override
		protected Exception doInBackground(Void... voids) {
			GpxSelectionHelper helper = app.getSelectedGpxHelper();
			SelectedGpxFile selected = helper.getSelectedFileByPath(file.getAbsolutePath());

			GPXFile gpxFile;
			if (selected != null && selected.getGpxFile() != null) {
				gpxFile = selected.getGpxFile();
			} else {
				gpxFile = GPXUtilities.loadGPXFile(file);
			}
			if (gpxFile.metadata == null) {
				gpxFile.metadata = new Metadata();
			}
			gpxFile.metadata.name = Algorithms.getFileNameWithoutExtension(file.getName());

			return GPXUtilities.writeGpxFile(file, gpxFile);
		}
	}
}
