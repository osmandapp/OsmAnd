package net.osmand;

import android.app.Activity;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.util.Algorithms;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.regex.Pattern;

public class FileUtils {

	public static final Pattern ILLEGAL_FILE_NAME_CHARACTERS = Pattern.compile("[?:\"*|/<>]");
	public static final Pattern ILLEGAL_PATH_NAME_CHARACTERS = Pattern.compile("[?:\"*|<>]");

	public static void renameFile(Activity a, final File f, final RenameCallback callback) {
		final WeakReference<Activity> weakActivity = new WeakReference<>(a);
		AlertDialog.Builder b = new AlertDialog.Builder(a);
		if (f.exists()) {
			int xt = f.getName().lastIndexOf('.');
			final String ext = xt == -1 ? "" : f.getName().substring(xt);
			final String originalName = xt == -1 ? f.getName() : f.getName().substring(0, xt);
			final EditText editText = new EditText(a);
			editText.setText(originalName);
			editText.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}

				@Override
				public void afterTextChanged(Editable s) {
					Editable text = editText.getText();
					if (text.length() >= 1) {
						Activity activity = weakActivity.get();
						if (ILLEGAL_FILE_NAME_CHARACTERS.matcher(text).find() && activity != null) {
							editText.setError(activity.getString(R.string.file_name_containes_illegal_char));
						}
					}
				}
			});
			b.setTitle(R.string.shared_string_rename);
			int leftPadding = AndroidUtils.dpToPx(a, 24f);
			int topPadding = AndroidUtils.dpToPx(a, 4f);
			b.setView(editText, leftPadding, topPadding, leftPadding, topPadding);
			// Behaviour will be overwritten later;
			b.setPositiveButton(R.string.shared_string_save, null);
			b.setNegativeButton(R.string.shared_string_cancel, null);
			final AlertDialog alertDialog = b.create();
			alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
				@Override
				public void onShow(DialogInterface dialog) {
					alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
							new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									Activity activity = weakActivity.get();
									if (activity != null) {
										OsmandApplication app = (OsmandApplication) activity.getApplication();
										if (ext.equals(SQLiteTileSource.EXT)) {
											if (renameSQLiteFile(app, f, editText.getText().toString() + ext,
													callback) != null) {
												alertDialog.dismiss();
											}
										} else {
											if (renameGpxFile(app, f, editText.getText().toString() + ext,
													false, callback) != null) {
												alertDialog.dismiss();
											}
										}
									}
								}
							});
				}
			});
			alertDialog.show();
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
			final String[] suffixes = new String[]{"-journal", "-wal", "-shm"};
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

	public static File renameGpxFile(OsmandApplication ctx, File source, String newName, boolean dirAllowed,
									 RenameCallback callback) {
		File dest = checkRenamePossibility(ctx, source, newName, dirAllowed);
		if (dest == null) {
			return null;
		}
		if (!dest.getParentFile().exists()) {
			dest.getParentFile().mkdirs();
		}
		if (source.renameTo(dest)) {
			GpxSelectionHelper helper = ctx.getSelectedGpxHelper();
			SelectedGpxFile selected = helper.getSelectedFileByPath(source.getAbsolutePath());
			ctx.getGpxDbHelper().rename(source, dest);
			if (selected != null && selected.getGpxFile() != null) {
				selected.getGpxFile().path = dest.getAbsolutePath();
				helper.updateSelectedGpxFile(selected);
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

	public interface RenameCallback {
		void renamedTo(File file);
	}
}
