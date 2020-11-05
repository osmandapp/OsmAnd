package net.osmand.plus.settings.backend.backup;

import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static net.osmand.plus.settings.backend.backup.SettingsHelper.*;

class SettingsExporter {

	private Map<String, SettingsItem> items;
	private Map<String, String> additionalParams;
	private boolean exportItemsFiles;
	private boolean exportCancel;
	private final ExportProgress exportProgress;

	SettingsExporter(boolean exportItemsFiles, ExportProgress exportProgress) {
		this.exportItemsFiles = exportItemsFiles;
		this.exportProgress = exportProgress;
		items = new LinkedHashMap<>();
		additionalParams = new LinkedHashMap<>();
	}

	void addSettingsItem(SettingsItem item) throws IllegalArgumentException {
		if (items.containsKey(item.getName())) {
			throw new IllegalArgumentException("Already has such item: " + item.getName());
		}
		items.put(item.getName(), item);
	}

	public void setExportCancel(boolean exportCancel) {
		this.exportCancel = exportCancel;
	}

	void addAdditionalParam(String key, String value) {
		additionalParams.put(key, value);
	}

	void exportSettings(File file) throws JSONException, IOException {
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

	private void writeItemFiles(ZipOutputStream zos) throws IOException {
		int progress = 0;
		for (SettingsItem item : items.values()) {
			SettingsItemWriter<? extends SettingsItem> writer = item.getWriter();
			if (writer != null) {
				String fileName = item.getFileName();
				if (Algorithms.isEmpty(fileName)) {
					fileName = item.getDefaultFileName();
				}
				writer.writeEntry(fileName, zos);
			}
			if (exportCancel) {
				exportCancel = false;
				return;
			}
			if (item instanceof FileSettingsItem) {
				int size = (int) ((FileSettingsItem) item).getSize() / (1 << 20);
				progress += size;
				if (exportProgress != null) {
					exportProgress.setProgress(progress);
				}
			}
		}
	}

	private JSONObject createItemsJson() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("version", VERSION);
		for (Map.Entry<String, String> param : additionalParams.entrySet()) {
			json.put(param.getKey(), param.getValue());
		}
		JSONArray itemsJson = new JSONArray();
		for (SettingsItem item : items.values()) {
			itemsJson.put(new JSONObject(item.toJson()));
		}
		json.put("items", itemsJson);
		return json;
	}
}
