package net.osmand.plus.utils;

import static net.osmand.IndexConstants.BACKUP_INDEX_DIR;
import static net.osmand.IndexConstants.DOWNLOAD_EXT;
import static net.osmand.IndexConstants.GEOTIFF_DIR;
import static net.osmand.IndexConstants.HEIGHTMAP_INDEX_DIR;
import static net.osmand.IndexConstants.LIVE_INDEX_DIR;
import static net.osmand.IndexConstants.MAPS_PATH;
import static net.osmand.IndexConstants.NAUTICAL_INDEX_DIR;
import static net.osmand.IndexConstants.OSMAND_SETTINGS_FILE_EXT;
import static net.osmand.IndexConstants.ROADS_INDEX_DIR;
import static net.osmand.IndexConstants.SRTM_INDEX_DIR;
import static net.osmand.IndexConstants.TEMP_DIR;
import static net.osmand.IndexConstants.WIKIVOYAGE_INDEX_DIR;
import static net.osmand.IndexConstants.WIKI_INDEX_DIR;
import static net.osmand.plus.plugins.development.OsmandDevelopmentPlugin.DOWNLOAD_BUILD_NAME;
import static net.osmand.util.Algorithms.XML_FILE_SIGNATURE;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.dialogs.RenameFileBottomSheet;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.helpers.GpxDisplayHelper;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class FileUtils {

	public static final int APPROXIMATE_FILE_SIZE_BYTES = 5 * 1024 * 1024;

	public static final Pattern ILLEGAL_FILE_NAME_CHARACTERS = Pattern.compile("[?:\"*|/<>]");
	public static final Pattern ILLEGAL_PATH_NAME_CHARACTERS = Pattern.compile("[?:\"*|<>]");

	public static void renameFile(@NonNull FragmentActivity activity, @NonNull File file,
	                              @Nullable Fragment target, boolean usedOnMap) {
		if (file.exists()) {
			FragmentManager fragmentManager = activity.getSupportFragmentManager();
			RenameFileBottomSheet.showInstance(fragmentManager, target, file, usedOnMap);
		}
	}

	@Nullable
	public static File renameSQLiteFile(OsmandApplication ctx, File source, String newName,
	                                    RenameCallback callback) {
		File dest = checkRenamePossibility(ctx, source, newName, false);
		if (dest == null) {
			return null;
		}
		File destDir = dest.getParentFile();
		if (!destDir.exists()) {
			destDir.mkdirs();
		}
		if (source.renameTo(dest)) {
			String[] suffixes = {"-journal", "-wal", "-shm"};
			for (String s : suffixes) {
				File file = new File(ctx.getDatabasePath(source + s).toString());
				if (file.exists()) {
					file.renameTo(ctx.getDatabasePath(dest + s));
				}
			}
			if (callback != null) {
				callback.fileRenamed(source, dest);
			}
			return dest;
		} else {
			Toast.makeText(ctx, R.string.file_can_not_be_renamed, Toast.LENGTH_LONG).show();
		}
		return null;
	}

	@Nullable
	public static File renameGpxFile(@NonNull OsmandApplication app, @NonNull File source,
	                                 @NonNull String newName, boolean dirAllowed, @Nullable RenameCallback callback) {
		File dest = checkRenamePossibility(app, source, newName, dirAllowed);
		if (dest == null) {
			return null;
		}
		File res = renameGpxFile(app, source, dest);
		if (res != null) {
			if (callback != null) {
				callback.fileRenamed(source, res);
			}
		} else {
			Toast.makeText(app, R.string.file_can_not_be_renamed, Toast.LENGTH_LONG).show();
		}
		return res;
	}

	@Nullable
	public static File renameFile(@NonNull OsmandApplication app, @NonNull File source,
	                              @NonNull String newName, boolean dirAllowed, RenameCallback callback) {
		File dest = checkRenamePossibility(app, source, newName, dirAllowed);
		if (dest == null) {
			return null;
		}
		File destDir = dest.getParentFile();
		if (!destDir.exists()) {
			destDir.mkdirs();
		}
		File res = source.renameTo(dest) ? dest : null;
		if (res != null) {
			if (callback != null) {
				callback.fileRenamed(source, res);
			}
		} else {
			Toast.makeText(app, R.string.file_can_not_be_renamed, Toast.LENGTH_LONG).show();
		}
		return res;
	}

	@Nullable
	public static File renameGpxFile(@NonNull OsmandApplication app, @NonNull File src, @NonNull File dest) {
		File destDir = dest.getParentFile();
		if (!destDir.exists()) {
			destDir.mkdirs();
		}
		if (src.renameTo(dest)) {
			updateRenamedGpx(app, src, dest);
			return dest;
		}
		return null;
	}

	public static void updateRenamedGpx(@NonNull OsmandApplication app, @NonNull File src, @NonNull File dest) {
		app.getGpxDbHelper().rename(src, dest);
		app.getQuickActionRegistry().onRenameGpxFile(src.getAbsolutePath(), dest.getAbsolutePath());

		GpxSelectionHelper gpxSelectionHelper = app.getSelectedGpxHelper();
		SelectedGpxFile selectedGpxFile = gpxSelectionHelper.getSelectedFileByPath(src.getAbsolutePath());
		if (selectedGpxFile != null) {
			selectedGpxFile.getGpxFile().path = dest.getAbsolutePath();
			gpxSelectionHelper.updateSelectedGpxFile(selectedGpxFile);
			GpxDisplayHelper gpxDisplayHelper = app.getGpxDisplayHelper();
			gpxDisplayHelper.updateDisplayGroupsNames(selectedGpxFile);
		}
	}

	public static void updateMovedGpxFiles(@NonNull OsmandApplication app, @NonNull List<File> files,
	                                       @NonNull File srcDir, @NonNull File destDir) {
		for (File srcFile : files) {
			String path = srcFile.getAbsolutePath();
			String newPath = path.replace(srcDir.getAbsolutePath(), destDir.getAbsolutePath());

			File destFile = new File(newPath);
			if (destFile.exists()) {
				updateRenamedGpx(app, srcFile, destFile);
			}
		}
	}

	public static boolean removeGpxFile(@NonNull OsmandApplication app, @NonNull File file) {
		if (file.exists()) {
			GpxSelectionHelper helper = app.getSelectedGpxHelper();
			SelectedGpxFile selected = helper.getSelectedFileByPath(file.getAbsolutePath());
			file.delete();
			if (selected != null) {
				GpxSelectionParams params = GpxSelectionParams.newInstance()
						.hideFromMap().syncGroup().saveSelection();
				helper.selectGpxFile(selected.getGpxFile(), params);
			}
			app.getGpxDbHelper().remove(file);
			return true;
		}
		return false;
	}

	public static File checkRenamePossibility(@NonNull OsmandApplication app, @NonNull File source,
	                                          @NonNull String newName, boolean dirAllowed) {
		if (Algorithms.isEmpty(newName)) {
			Toast.makeText(app, R.string.empty_filename, Toast.LENGTH_LONG).show();
			return null;
		}
		Pattern illegalCharactersPattern = dirAllowed ? ILLEGAL_PATH_NAME_CHARACTERS : ILLEGAL_FILE_NAME_CHARACTERS;
		if (illegalCharactersPattern.matcher(newName).find()) {
			Toast.makeText(app, R.string.file_name_containes_illegal_char, Toast.LENGTH_LONG).show();
			return null;
		}
		File dest = new File(source.getParentFile(), newName);
		if (dest.exists()) {
			Toast.makeText(app, R.string.file_with_name_already_exists, Toast.LENGTH_LONG).show();
			return null;
		}
		return dest;
	}

	public static boolean isValidFileName(@Nullable String name) {
		return name != null && !ILLEGAL_FILE_NAME_CHARACTERS.matcher(name).find();
	}

	public static boolean isValidDirName(@NonNull String name) {
		return !ILLEGAL_PATH_NAME_CHARACTERS.matcher(name).find();
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

	@NonNull
	public static File getTempDir(@NonNull OsmandApplication app) {
		return getExistingDir(app, TEMP_DIR);
	}

	@NonNull
	public static File getExistingDir(@NonNull OsmandApplication app, @NonNull String path) {
		File tempDir = app.getAppPath(path);
		if (!tempDir.exists()) {
			tempDir.mkdirs();
		}
		return tempDir;
	}

	public static boolean isWritable(@NonNull File dirToTest) {
		return isWritable(dirToTest, false);
	}

	public static boolean isWritable(@NonNull File dirToTest, boolean testWrite) {
		InputStream in = null;
		OutputStream out = null;
		boolean isWriteable;
		try {
			dirToTest.mkdirs();
			File writeTestFile = File.createTempFile("osmand_", ".tmp", dirToTest);
			isWriteable = writeTestFile.exists();

			if (isWriteable && testWrite) {
				out = new FileOutputStream(writeTestFile);
				Algorithms.writeInt(out, Integer.reverseBytes(XML_FILE_SIGNATURE));

				in = new FileInputStream(writeTestFile);
				int fileSignature = Algorithms.readInt(in);
				isWriteable = XML_FILE_SIGNATURE == fileSignature;
			}
			writeTestFile.delete();
		} catch (IOException e) {
			isWriteable = false;
		} finally {
			Algorithms.closeStream(in);
			Algorithms.closeStream(out);
		}
		return isWriteable;
	}

	public static boolean isTempFile(@NonNull OsmandApplication app, @Nullable String path) {
		return path != null && path.startsWith(getTempDir(app).getAbsolutePath());
	}

	@NonNull
	public static List<File> collectDirFiles(@NonNull File dir) {
		List<File> files = new ArrayList<>();
		collectDirFiles(dir, files);
		return files;
	}

	public static void collectDirFiles(@NonNull File file, @NonNull List<File> list) {
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null) {
				for (File subfolderFile : files) {
					collectDirFiles(subfolderFile, list);
				}
			}
		} else {
			list.add(file);
		}
	}

	@NonNull
	public static File getFileWithDownloadExtension(@NonNull File original) {
		File folder = original.getParentFile();
		String fileName = original.getName() + DOWNLOAD_EXT;
		return new File(folder, fileName);
	}

	public static void removeFilesWithExtensions(@NonNull File dir, boolean withSubdirs, @NonNull String... extensions) {
		File[] files = dir.listFiles(pathname -> pathname.isDirectory()
				? withSubdirs : CollectionUtils.endsWithAny(pathname.getName(), extensions));
		if (files == null) {
			return;
		}
		for (File file : files) {
			if (file.isDirectory()) {
				if (withSubdirs) {
					removeFilesWithExtensions(file, true, extensions);
				}
			} else {
				file.delete();
			}
		}
	}

	public static boolean replaceTargetFile(@NonNull File sourceFile, @NonNull File targetFile) {
		return replaceTargetFile(null, sourceFile, targetFile);
	}

	public static boolean replaceTargetFile(@Nullable ResourceManager manager,
	                                        @NonNull File sourceFile, @NonNull File targetFile) {
		boolean removed = Algorithms.removeAllFiles(targetFile);
		if (manager != null && removed) {
			manager.closeFile(targetFile.getName());
		}
		return sourceFile.renameTo(targetFile);
	}

	public static void removeUnnecessaryFiles(@NonNull OsmandApplication app) {
		Algorithms.removeAllFiles(app.getAppPath(TEMP_DIR));
		Algorithms.removeAllFiles(app.getAppPath(DOWNLOAD_BUILD_NAME));
		FileUtils.removeFilesWithExtensions(app.getAppPath(MAPS_PATH), false, DOWNLOAD_EXT);
		FileUtils.removeFilesWithExtensions(app.getAppPath(ROADS_INDEX_DIR), false, DOWNLOAD_EXT);
		FileUtils.removeFilesWithExtensions(app.getAppPath(LIVE_INDEX_DIR), false, DOWNLOAD_EXT);
		FileUtils.removeFilesWithExtensions(app.getAppPath(SRTM_INDEX_DIR), false, DOWNLOAD_EXT);
		FileUtils.removeFilesWithExtensions(app.getAppPath(NAUTICAL_INDEX_DIR), false, DOWNLOAD_EXT);
		FileUtils.removeFilesWithExtensions(app.getAppPath(WIKI_INDEX_DIR), false, DOWNLOAD_EXT);
		FileUtils.removeFilesWithExtensions(app.getAppPath(WIKIVOYAGE_INDEX_DIR), false, DOWNLOAD_EXT);
		FileUtils.removeFilesWithExtensions(app.getAppPath(HEIGHTMAP_INDEX_DIR), false, DOWNLOAD_EXT);
		FileUtils.removeFilesWithExtensions(app.getAppPath(GEOTIFF_DIR), false, DOWNLOAD_EXT);
	}

	public static boolean move(@NonNull File from, @NonNull File to) {
		File parent = to.getParentFile();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}
		return from.renameTo(to);
	}

	@NonNull
	public static File getBackupFileForCustomAppMode(@NonNull OsmandApplication app, @NonNull String appModeKey) {
		String fileName = appModeKey + OSMAND_SETTINGS_FILE_EXT;
		File backupDir = FileUtils.getExistingDir(app, BACKUP_INDEX_DIR);
		return new File(backupDir, fileName);
	}

	public interface RenameCallback {
		void fileRenamed(@NonNull File src, @NonNull File dest);
	}
}