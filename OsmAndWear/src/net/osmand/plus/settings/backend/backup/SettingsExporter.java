package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static net.osmand.plus.settings.backend.backup.SettingsHelper.BUFFER;
import static net.osmand.plus.settings.backend.backup.SettingsHelper.ExportProgressListener;

public class SettingsExporter extends Exporter {

	private final File file;
	private final boolean exportItemsFiles;

	public SettingsExporter(@NonNull File file,
							@Nullable ExportProgressListener progressListener,
							boolean exportItemsFiles) {
		super(progressListener);
		this.file = file;
		this.exportItemsFiles = exportItemsFiles;
	}

	@Override
	public void export() throws JSONException, IOException {
		exportSettings(file);
	}

	private void exportSettings(File file) throws JSONException, IOException {
		JSONObject json = createItemsJson();
		OutputStream os = new BufferedOutputStream(new FileOutputStream(file), BUFFER);
		ZipOutputStream zos = new ZipOutputStream(os);
		try {
			ZipEntry entry = new ZipEntry("items.json");
			zos.putNextEntry(entry);
			zos.write(json.toString(2).getBytes("UTF-8"));
			zos.closeEntry();
			if (exportItemsFiles) {
				writeItemFiles(zos);
			}
			zos.flush();
			zos.finish();
		} finally {
			Algorithms.closeStream(zos);
			Algorithms.closeStream(os);
		}
	}

	protected void writeItemFiles(ZipOutputStream zos) throws IOException {
		long[] progress = {0};
		ZipWriter zipWriter = new ZipWriter(zos, new AbstractProgress() {
			@Override
			public void progress(int deltaWork) {
				progress[0] += deltaWork;
				ExportProgressListener progressListener = getProgressListener();
				if (progressListener != null) {
					progressListener.updateProgress((int) progress[0] / (1 << 10));
				}
			}
		});
		writeItems(zipWriter);
	}
}
