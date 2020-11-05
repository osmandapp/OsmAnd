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
	private ExportProgressListener progressListener;

	private boolean cancelled;
	private boolean exportItemsFiles;

	SettingsExporter(ExportProgressListener progressListener, boolean exportItemsFiles) {
		this.progressListener = progressListener;
		this.exportItemsFiles = exportItemsFiles;
		items = new LinkedHashMap<>();
		additionalParams = new LinkedHashMap<>();
	}

	void addSettingsItem(SettingsItem item) throws IllegalArgumentException {
		if (items.containsKey(item.getName())) {
			throw new IllegalArgumentException("Already has such item: " + item.getName());
		}
		items.put(item.getName(), item);
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
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
			if (cancelled) {
				return;
			}
			if (item instanceof FileSettingsItem) {
				int size = (int) ((FileSettingsItem) item).getSize() / (1 << 20);
				progress += size;
				if (progressListener != null) {
					progressListener.updateProgress(progress);
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
