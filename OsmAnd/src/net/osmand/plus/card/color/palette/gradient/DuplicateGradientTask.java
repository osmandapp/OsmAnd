package net.osmand.plus.card.color.palette.gradient;

import static net.osmand.plus.helpers.ColorPaletteHelper.EXT;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.util.Algorithms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DuplicateGradientTask extends AsyncTask<Void, Void, Boolean> {

	private final OsmandApplication app;
	private final File fileToDuplicate;
	private final DuplicateGradientListener listener;

	public DuplicateGradientTask(@NonNull OsmandApplication app, @NonNull File fileToDuplicate, @NonNull DuplicateGradientListener listener) {
		this.app = app;
		this.fileToDuplicate = fileToDuplicate;
		this.listener = listener;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		if (!fileToDuplicate.exists()) {
			PlatformUtil.getLog(DuplicateGradientTask.class).error("File to duplicate doesn't exist");
			return false;
		}

		FileInputStream fis;
		byte[] data = new byte[(int) fileToDuplicate.length()];
		try {
			fis = new FileInputStream(fileToDuplicate);
			fis.read(data);
			fis.close();
		} catch (IOException e) {
			PlatformUtil.getLog(DuplicateGradientTask.class).error("Error copying data from file", e);
			return false;
		}

		int fileNumber = 1;

		String newFilename = fileToDuplicate.getName().replace(EXT, "");
		Matcher matcher = Pattern.compile("\\s\\(\\d+\\)$").matcher(newFilename);
		if (matcher.find()) {
			newFilename = newFilename.substring(0, matcher.start());
			String[] splitName = newFilename.split(" ");
			String lastSplitPart = splitName[splitName.length - 1].substring(1).substring(0, 1);
			int oldFileNumber = Algorithms.parseIntSilently(lastSplitPart, -1);
			if (oldFileNumber != -1) {
				fileNumber = oldFileNumber + 1;
			}
		}

		File newFile;
		do {
			newFile = new File(app.getAppPath(IndexConstants.CLR_PALETTE_DIR), newFilename + " (" + fileNumber + ")" + EXT);
			fileNumber++;
		} while (newFile.exists());

		FileOutputStream fos;
		try {
			fos = new FileOutputStream(newFile);
			fos.write(data);
			fos.close();
		} catch (IOException e) {
			PlatformUtil.getLog(DuplicateGradientTask.class).error("Error writing data to duplicated file", e);
			return false;
		}
		return true;
	}

	@Override
	protected void onPostExecute(Boolean successful) {
		listener.onDuplicatingFinished(successful);
	}

	public interface DuplicateGradientListener {
		void onDuplicatingFinished(boolean duplicated);
	}
}
