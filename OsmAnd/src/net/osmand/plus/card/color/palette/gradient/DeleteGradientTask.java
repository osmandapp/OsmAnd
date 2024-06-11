package net.osmand.plus.card.color.palette.gradient;

import static net.osmand.plus.helpers.ColorPaletteHelper.EXT;
import static net.osmand.plus.helpers.ColorPaletteHelper.GRADIENT_ID_SPLITTER;
import static net.osmand.plus.helpers.ColorPaletteHelper.ROUTE_PREFIX;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;

import java.io.File;

public class DeleteGradientTask extends AsyncTask<Void, Void, Boolean> {
	private final File fileToDelete;
	private final DeleteGradientListener listener;

	public DeleteGradientTask(@NonNull File fileToDelete, @NonNull DeleteGradientListener listener) {
		this.fileToDelete = fileToDelete;
		this.listener = listener;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		if (!fileToDelete.exists()) {
			PlatformUtil.getLog(DuplicateGradientTask.class).error("File to delete doesn't exist");
			return false;
		}
		String fileName = fileToDelete.getName().replace(EXT, "");
		if (fileName.startsWith(ROUTE_PREFIX) && fileName.endsWith(GRADIENT_ID_SPLITTER + PaletteGradientColor.DEFAULT_NAME)) {
			return false;
		}
		return fileToDelete.delete();
	}

	@Override
	protected void onPostExecute(Boolean successful) {
		listener.onGradientDeleted(successful);
	}

	public interface DeleteGradientListener {
		void onGradientDeleted(boolean deleted);
	}
}
